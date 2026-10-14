package edu.up.cs.androidcatan.catan;


import android.util.Log;
import android.content.Context;

import java.util.ArrayList;
import java.util.Random;

import edu.up.cs.androidcatan.catan.actions.CatanBuildCityAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuyDevCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberDiscardAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberMoveAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberStealAction;
import edu.up.cs.androidcatan.catan.actions.CatanRollDiceAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithAutoBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeOfferAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeReplyAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeChooseAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithCustomPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseMonopolyCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseRoadBuildingCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseVictoryPointCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.LongestRoadCalculator;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.LocalGame;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * https://github.com/alexweininger/android-catan
 **/

public class CatanLocalGame extends LocalGame {

    private final static String TAG = "CatanLocalGame";

    private CatanGameState state;
    private GameAction lastAcceptedCandidate;
    private CatanTradeNegotiation pendingTrade;
    private long offerSequence;
    private String lastCompletedTradeDescription;

    // Per-roll snapshots are transient and never added to saved-game data.
    private int[][] lastRollHandsBefore;
    private static final String[] RESOURCE_NAMES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};

    private Context autosaveContext;
    private int[] autosaveTypes;
    private final boolean resumedFromSave;
    private boolean restoredBoardDisplayed;

    /** Called before starting new/resumed match; never persists UI objects. */
    public void configureAutosave(Context context, int[] playerTypes) {
        this.autosaveContext = context == null ? null : context.getApplicationContext();
        this.autosaveTypes = playerTypes == null ? null : playerTypes.clone();
    }

    private void persistMatch() {
        if (autosaveContext != null && autosaveTypes != null && playerNames != null) {
            if (!CatanSaveStore.save(autosaveContext, state, playerNames, autosaveTypes)) {
                Log.w(TAG, "Autosave unsuccessful; previous complete save retained");
            }
        }
    }

    @Override protected void onMatchReady() {
        sessionReady = true;
        // Start a fresh turn diary only for a NEW match. Continue leaves it intact.
        if (!resumedFromSave) {
            CatanMatchHistory.clear(autosaveContext);
            CatanMatchHistory.append(autosaveContext,
                    "New match started — choose your opening settlement and road");
            persistMatch();
        }
    }

    @Override protected void onAcceptedMove() {
        publishAcceptedMove(lastAcceptedCandidate);
        lastRollHandsBefore = null;
        // Resumed CPUs may act before the UI is connected. Keep the original
        // snapshot intact until the human can see the board again.
        if (!resumedFromSave || restoredBoardDisplayed) persistMatch();
    }

    /** Called after the resumed board and score display have been populated. */
    public void confirmResumeDisplayed() {
        if (!resumedFromSave) return;
        postToGameThread(new Runnable() {
            @Override public void run() {
                if (restoredBoardDisplayed) return;
                restoredBoardDisplayed = true;
                // Persist any CPU actions taken while the restored GUI initialized.
                persistMatch();
            }
        });
    }

    @Override protected void onMatchFinished() {
        // Never discard the original snapshot for a resumed game that failed
        // to reach the human display, even if CPUs reached the victory condition.
        if (resumedFromSave && !restoredBoardDisplayed) {
            Log.w(TAG, "Resumed game ended before GUI restoration; keeping original save");
            return;
        }
        if (autosaveContext != null) CatanSaveStore.clear(autosaveContext);
    }

    /**
     * constructor for CatanLocalGame
     */
    public CatanLocalGame() {
        super();
        resumedFromSave = false;
        state = new CatanGameState();
    }

    /** New chosen configuration; the zero-argument constructor remains classic. */
    public CatanLocalGame(int playerCount, CatanBoardTopology.Island island) {
        super();
        resumedFromSave = false;
        state = new CatanGameState(playerCount, island);
    }

    /** Configure optional events on a new match before starting any turns. */
    public void configureMarketEvents(boolean enabled) {
        if (!resumedFromSave) state.configureMarketEvents(enabled);
    }

    /**
     * Restoring a match must never create a fresh random island first.
     * Start directly from the saved rules state, before player threads start.
     */
    public CatanLocalGame(CatanGameState savedState) {
        super();
        if (savedState == null || savedState.getBoard() == null)
            throw new IllegalArgumentException("Cannot restore an empty match");
        resumedFromSave = true;
        state = new CatanGameState(savedState);
    }

    private volatile boolean sessionReady;

    public boolean isSessionReady() {
        return sessionReady;
    }

    /**
     * Publish only information all players may legitimately see. Buying a
     * development card must NOT disclose which card another player drew.
     * Private inventories remain on their owners' UI only.
     */
    private void publishAcceptedMove(GameAction action) {
        if (action == null || players == null || playerNames == null) return;
        int who = getPlayerIdx(action.getPlayer());
        if (who < 0 || who >= playerNames.length) return;
        String actor = playerNames[who];
        String description = null;
        boolean roll = false;
        if (action instanceof CatanRollDiceAction) {
            int[] dice = state.getDice().getDiceValues();
            description = actor + " rolled " + dice[0] + " + " + dice[1]
                    + " = " + state.getCurrentDiceSum()
                    + (state.getCurrentDiceSum() == 7 ? " — robber activated" : "");
            roll = true;
            if (state.getCurrentDiceSum() != 7 && lastRollHandsBefore != null) {
                StringBuilder yields = new StringBuilder();
                for (int p = 0; p < state.getPlayerList().size(); p++) {
                    StringBuilder gained = new StringBuilder();
                    int[] now = state.getPlayerList().get(p).getResourceCards();
                    for (int res = 0; res < 5; res++) {
                        int delta = now[res] - lastRollHandsBefore[p][res];
                        if (delta <= 0) continue;
                        if (gained.length() > 0) gained.append(", ");
                        gained.append("+").append(delta).append(" ").append(RESOURCE_NAMES[res]);
                    }
                    if (gained.length() > 0) {
                        if (yields.length() > 0) yields.append("; ");
                        yields.append(playerNames[p]).append(" ").append(gained);
                    }
                }
                description += yields.length() > 0
                        ? " — production: " + yields
                        : " — no resources produced";
            }
        } else if (action instanceof CatanBuildSettlementAction) {
            description = actor + " placed a settlement";
        } else if (action instanceof CatanBuildRoadAction) {
            description = actor + " built a road";
        } else if (action instanceof CatanBuildCityAction) {
            description = actor + " upgraded a settlement to a city";
        } else if (action instanceof CatanBuyDevCardAction) {
            description = actor + " bought a development card";
        } else if (action instanceof CatanUseKnightCardAction) {
            description = actor + " played Knight";
        } else if (action instanceof CatanUseMonopolyCardAction) {
            int id = ((CatanUseMonopolyCardAction) action).getChosenResource();
            description = actor + " played Monopoly: " + RESOURCE_NAMES[id];
        } else if (action instanceof CatanUseYearOfPlentyCardAction) {
            CatanUseYearOfPlentyCardAction card = (CatanUseYearOfPlentyCardAction) action;
            description = actor + " played Year of Plenty: "
                    + RESOURCE_NAMES[card.getChosenResource()] + " and "
                    + RESOURCE_NAMES[card.getSecondChosenResource()];
        } else if (action instanceof CatanUseRoadBuildingCardAction) {
            description = actor + " played Road Building";
        } else if (action instanceof CatanRobberDiscardAction) {
            description = actor + " finished robber discards";
        } else if (action instanceof CatanRobberMoveAction) {
            description = actor + " moved the robber";
        } else if (action instanceof CatanRobberStealAction) {
            int from = ((CatanRobberStealAction) action).getStealingFromPlayerId();
            description = from < 0 ? actor + " found nobody to steal from"
                    : actor + " stole from " + playerNames[from];
        } else if (action instanceof CatanTradeChooseAction) {
            description = lastCompletedTradeDescription;
            lastCompletedTradeDescription = null;
        } else if (action instanceof CatanTradeWithAutoBankAction) {
            CatanTradeWithAutoBankAction exchange = (CatanTradeWithAutoBankAction) action;
            description = actor + " traded " + exchange.getExecutedRate() + " "
                    + RESOURCE_NAMES[exchange.getGiving()] + " for 1 "
                    + RESOURCE_NAMES[exchange.getReceiving()] + " (bank/harbour)";
        } else if (action instanceof CatanTradeWithBankAction) {
            CatanTradeWithBankAction trade = (CatanTradeWithBankAction) action;
            description = actor + " traded 4 " + RESOURCE_NAMES[trade.getResourceIdGiving()]
                    + " for 1 " + RESOURCE_NAMES[trade.getResourceIdRec()] + " (bank)";
        } else if (action instanceof CatanTradeWithPortAction) {
            description = actor + " traded at a 2:1 harbour";
        } else if (action instanceof CatanTradeWithCustomPortAction) {
            description = actor + " traded at a 3:1 harbour";
        } else if (action instanceof CatanEndTurnAction) {
            description = actor + " finished their turn — " + playerNames[state.getCurrentPlayerId()]
                    + " is next";
            if (state.getCurrentPlayerId() == 0 && !state.isSetupPhase()
                    && state.getMarketState().getActiveEvent() != null
                    && state.getMarketState().getActiveFromRound()
                        == state.getMarketState().getCompletedRounds() + 1)
                description += " — MARKET EVENT: " + state.getMarketState().announcement();
        }
        if (description != null) {
            CatanPublicEventInfo notice = new CatanPublicEventInfo(description, roll);
            for (GamePlayer p : players) {
                if (p instanceof CatanHumanPlayer) p.sendInfo(notice);
            }
        }
    }

    /*--------------------------------------- Action Methods -------------------------------------------*/

    /**
     * Tell whether the given player is allowed to make a move at the
     * present point in the game.
     *
     * @param playerIdx the player's player-number (ID)
     * @return true iff the player is allowed to move
     */
    @Override
    public boolean canMove(int playerIdx) {
        Log.d(TAG, "canMove() called with: playerIdx = [" + playerIdx + "] currentPlayerId(): " + this.state.getCurrentPlayerId());
        if (playerIdx < 0 || playerIdx >= state.getPlayerList().size()) return false;
        if (pendingTrade != null) {
            // Invited parties may ONLY reply. The makeMove guard below rejects
            // every other out-of-turn command, including roll/build/bank trade.
            return playerIdx == pendingTrade.proposer || pendingTrade.waitingFor(playerIdx);
        }

        // if it is the robber phase
        if (state.isRobberPhase()) {
            // if the player has discarded already
            if (state.getRobberPlayerListHasDiscarded()[playerIdx]) {
                if (state.getCurrentPlayerId() == playerIdx) {
                    // if the player has discarded, and it is their turn, return true
                    Log.d(TAG, "canMove() the player has discarded, and it is their turn, returned " + true);
                    return true;
                } else {
                    // return false if it is the robber phase and they have discarded, and it is not their turn
                    Log.d(TAG, "canMove() it is the robber phase and they have discarded, and it is not their turn returned " + false);
                    return false;
                }
            } else {
                // return true if it is the robber phase and they have not discarded
                Log.d(TAG, "canMove() it is the robber phase and they have not discarded returned " + true);
                return true;
            }
        }

        if (playerIdx < 0 || playerIdx >= state.getPlayerList().size()) Log.e(TAG, "canMove: Invalid playerIds: " + playerIdx);

        Log.d(TAG, "canMove() returned: " + (playerIdx == this.state.getCurrentPlayerId()));
        return playerIdx == this.state.getCurrentPlayerId();
    }

    /**
     * Initiates action based on what kind of GameAction object received.
     *
     * @param action The move that the player has sent to the game
     * @return Tells whether the move was a legal one.
     */
    @Override
    protected synchronized boolean makeMove(GameAction action) {
        lastCompletedTradeDescription = null;
        lastAcceptedCandidate = action;
        lastRollHandsBefore = null;
        if (action instanceof CatanRollDiceAction) {
            lastRollHandsBefore = new int[state.getPlayerList().size()][5];
            for (int p = 0; p < state.getPlayerList().size(); p++) {
                int[] resources = state.getPlayerList().get(p).getResourceCards();
                System.arraycopy(resources, 0, lastRollHandsBefore[p], 0, 5);
            }
        }
        Log.d(TAG, "makeMove() called with: action = [" + action + "]");
        Log.d(TAG, "makeMove: state: " + state.toString());
        int actingPlayer = players == null ? -1 : getPlayerIdx(action.getPlayer());
        if (actingPlayer < 0) return false;

        // Live negotiation has its own restricted action protocol. The active
        // player cannot roll, build or end the turn while awaiting replies.
        if (pendingTrade != null) {
            if (action instanceof CatanTradeReplyAction) {
                CatanTradeReplyAction reply = (CatanTradeReplyAction) action;
                if (reply.getOfferId() != pendingTrade.id
                        || !pendingTrade.waitingFor(actingPlayer)) return false;
                int response = reply.getResponse() == CatanTradeReplyAction.DECLINE
                        ? CatanTradeNegotiation.DECLINED
                        : reply.getResponse() == CatanTradeReplyAction.ACCEPT
                            ? CatanTradeNegotiation.ACCEPTED
                            : reply.getResponse() == CatanTradeReplyAction.COUNTER
                                ? CatanTradeNegotiation.COUNTERED : -1;
                if (response < 0 || !pendingTrade.respond(state, actingPlayer, response,
                        reply.getCounterGiving(), reply.getCounterReceiving()))
                    return false;
                if (pendingTrade.complete())
                    players[pendingTrade.proposer].sendInfo(pendingTrade.resultsInfo());
                return true;
            }
            if (action instanceof CatanTradeChooseAction
                    && actingPlayer == pendingTrade.proposer) {
                CatanTradeChooseAction choose = (CatanTradeChooseAction) action;
                if (choose.getOfferId() != pendingTrade.id) return false;
                int chosen = choose.getSelectedPartner();
                if (chosen < 0) {
                    clearTradeSession("Negotiation cancelled");
                    return true;
                }
                if (!pendingTrade.complete()) return false;
                if (!pendingTrade.choose(state, actingPlayer, chosen)) {
                    // If a hand changed before approval, cancel safely.
                    clearTradeSession("Trade expired — resource amounts changed");
                    return true;
                }
                String giver = playerNames[actingPlayer];
                String receiver = playerNames[chosen];
                lastCompletedTradeDescription = giver + " traded resources with " + receiver;
                players[chosen].sendInfo(new CatanTradeInfo(
                        CatanTradeInfo.COMPLETE, pendingTrade.id, actingPlayer,
                        chosen, null, null, null, null, null,
                        "Trade completed with " + giver));
                pendingTrade = null;
                state.setTradePending(false);
                return true;
            }
            return false;
        }

        // When there is no active negotiation, replies and selections are stale.
        if (action instanceof CatanTradeReplyAction
                || action instanceof CatanTradeChooseAction) return false;
        if (actingPlayer != state.getCurrentPlayerId()
                && !(action instanceof CatanRobberDiscardAction)) return false;

        if (action instanceof CatanTradeOfferAction) {
            CatanTradeOfferAction offer = (CatanTradeOfferAction) action;
            CatanTradeNegotiation proposed;
            try {
                proposed = new CatanTradeNegotiation(++offerSequence, state,
                        actingPlayer, offer.getTargets(), offer.getGiving(), offer.getReceiving());
            } catch (IllegalArgumentException invalid) {
                return false;
            }
            pendingTrade = proposed;
            state.setTradePending(true);
            for (int id = 0; id < players.length; id++)
                if (proposed.waitingFor(id)) players[id].sendInfo(proposed.requestInfo());
            return true;
        }

        /* --------------------------- Turn Actions --------------------------------------- */

        //Player is rolling the dice
        if (action instanceof CatanRollDiceAction) {
            if (state.isSetupPhase() || state.isActionPhase() || state.isRobberPhase()) return false;
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            //Set the gamestates dice value to what the player rolled
            this.state.setCurrentDiceSum(this.state.getDice().roll());
            Log.i(TAG, "rollDice: Player " + this.state.getCurrentPlayerId() + " rolled a " + this.state.getCurrentDiceSum());

            //If Robber is rolled, activate robber phase
            if (state.getCurrentDiceSum() == 7) { // if the robber is rolled
                Log.i(TAG, "rollDice: The robber has been activated.");
                state.setRobberPhase(true);
            } else { //Produce Resources for any other roll
                // produce resources for the roll
                Log.d(TAG, "makeMove: calling produce resources");
                state.produceResources(state.getCurrentDiceSum());
            }

            //Action Phase: Player can now do any action other than roll
            state.setActionPhase(true); // set the action phase to true
            return true;
        }

        //Player is ending their turn
        if (action instanceof CatanEndTurnAction) {
            if (state.isSetupPhase()
                    && (state.getCurrentSetupSettlement() < 0 || !state.isSetupRoadBuiltThisTurn()))
                return false;
            if (!state.isSetupPhase() && (!state.isActionPhase() || state.isRobberPhase())) return false;
            Log.d(TAG, "makeMove() Player " + state.getCurrentPlayerId() + " is ending their turn.");

            // A purchased development card becomes playable next turn.
            // Clear the cards bought by the player whose turn is ENDING,
            // before switching to the next player.
            state.getCurrentPlayer().setDevCardsBuiltThisTurn(new ArrayList<Integer>());

            // if it is still the setup phase
            if (this.state.isSetupPhase()) {
                // increment setup phase turn counter
                if (this.state.getSetupPhaseTurnCounter() < state.getOpeningPlacementCount() - 1) {
                    this.state.setSetupPhaseTurnCounter(this.state.getSetupPhaseTurnCounter() + 1);
                    this.state.setCurrentPlayerId(state.getOpeningPlayer(state.getSetupPhaseTurnCounter()));
                } else {
                    // if it is the last turn of the setup phase
                    this.state.setCurrentPlayerId(this.state.getCurrentPlayerId());
                }
            } else { // if it is not the setup phase
                // increment the current turn
                if (this.state.getCurrentPlayerId() == state.getPlayerList().size() - 1) {
                    this.state.setCurrentPlayerId(0);
                    // All players have had exactly one turn. An event may
                    // start ONLY now, with an equal chance for every player.
                    this.state.getMarketState().onRoundCompleted(state, new Random());
                } else {
                    this.state.setCurrentPlayerId(this.state.getCurrentPlayerId() + 1);
                }
            }
            // update the setup phase
            if (state.isSetupPhase()) this.state.setSetupPhase(this.state.updateSetupPhase());

            state.setActionPhase(false); // set action phase to false
            state.setFreeRoadsRemaining(0); // free roads expire at turn end
            state.setDevelopmentCardPlayedThisTurn(false);
            state.setCurrentSetupSettlement(-1);
            state.setSetupRoadBuiltThisTurn(false);
            Log.i(TAG, "makeMove: It is now " + state.getCurrentPlayerId() + "'s turn.");
            return true;
        }

        /* --------------------------- Build Actions --------------------------------------- */


        // The authoritative game engine enforces placement legality, turn
        // ownership, costs and finite piece counts, including for CPU moves.
        if (action instanceof CatanBuildRoadAction) {
            CatanBuildRoadAction build = (CatanBuildRoadAction) action;
            int owner = build.getOwnerId(), from = build.getIntAId(), to = build.getIntBid();
            if (owner != state.getCurrentPlayerId() || build.isSetupPhase() != state.isSetupPhase()) return false;
            if (ownedRoads(owner) >= 15) return false;
            if (state.isSetupPhase()
                    && (state.getCurrentSetupSettlement() < 0 || state.isSetupRoadBuiltThisTurn()))
                return false;
            if (!state.getBoard().validRoadPlacement(owner, state.isSetupPhase(), from, to,
                    state.getCurrentSetupSettlement())) return false;
            boolean free = !state.isSetupPhase() && state.getFreeRoadsRemaining() > 0;
            if (!state.isSetupPhase() && (!state.isActionPhase() && !free || state.isRobberPhase()))
                return false;
            if (!free && !state.isSetupPhase()
                    && !state.getCurrentPlayer().removeResourceBundle(Road.resourceCost)) return false;

            state.getBoard().addRoad(owner, from, to);
            if (state.isSetupPhase()) state.setSetupRoadBuiltThisTurn(true);
            if (free) state.setFreeRoadsRemaining(state.getFreeRoadsRemaining() - 1);
            if (!state.isSetupPhase()) updateLongestRoadHolder();
            return true;
        }

        if (action instanceof CatanBuildSettlementAction) {
            CatanBuildSettlementAction build = (CatanBuildSettlementAction) action;
            int owner = build.getOwnerId(), point = build.getIntersectionId();
            if (owner != state.getCurrentPlayerId() || build.isSetupPhase() != state.isSetupPhase()) return false;
            if (ownedBuildings(owner, Settlement.class) >= 5) return false;
            if (!state.getBoard().validBuildingLocation(owner, state.isSetupPhase(), point)) return false;
            if (!state.isSetupPhase() && (!state.isActionPhase() || state.isRobberPhase())) return false;
            if (state.isSetupPhase() && state.getCurrentSetupSettlement() >= 0) return false;
            if (!state.isSetupPhase()
                    && !state.getCurrentPlayer().removeResourceBundle(Settlement.resourceCost)) return false;

            state.getBoard().addBuilding(point, new Settlement(owner));
            if (state.isSetupPhase()) {
                state.setCurrentSetupSettlement(point);
                if (state.getSetupPhaseTurnCounter() >= state.getPlayerList().size()) {
                    for (Integer hexagon : state.getBoard().getIntToHexIdMap().get(point)) {
                        int resource = state.getBoard().getHexagonFromId(hexagon).getResourceId();
                        if (resource >= 0 && resource < 5 && state.getBankResourceCount(resource) > 0)
                            state.getCurrentPlayer().addResourceCard(resource, 1);
                    }
                }
            }
            state.getCurrentPlayer().addVictoryPoints(1);
            updateLongestRoadHolder();
            return true;
        }

        if (action instanceof CatanBuildCityAction) {
            CatanBuildCityAction build = (CatanBuildCityAction) action;
            int owner = build.getOwnerId(), point = build.getIntersectionId();
            if (owner != state.getCurrentPlayerId() || state.isSetupPhase()
                    || !state.isActionPhase() || state.isRobberPhase()) return false;
            if (ownedBuildings(owner, City.class) >= 4) return false;
            if (!state.getBoard().validCityLocation(owner, point)) return false;
            if (!state.getCurrentPlayer().removeResourceBundle(City.resourceCost)) return false;

            state.getBoard().addBuilding(point, new City(owner));
            state.getCurrentPlayer().addVictoryPoints(1);
            return true;
        }

        /*------------------------------- Development Card Actions -------------------------------*/

        //Player would like to buy a development card
        if (action instanceof CatanBuyDevCardAction) {
            if (state.isSetupPhase() || !state.isActionPhase() || state.isRobberPhase()) return false;
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            // There are exactly 25 cards in classic Catan. Reject the
            // purchase before spending resources if none remain.
            if (!state.hasDevelopmentCardsAvailable()) return false;
            Player player = state.getCurrentPlayer();
            if (!player.removeResourceBundle(DevelopmentCard.resourceCost)) return false;
            int devCard = state.getRandomDevCard();
            player.getDevelopmentCards().add(devCard);
            if (devCard == 1) {
                player.addPrivateVictoryPoints(1); // Secret VP is awarded when drawn.
            } else {
                player.addDevCardsBuiltThisTurn(devCard);
            }

            return true;
        }

        //Player would like to player a Knight Dev Card
        if (action instanceof CatanUseKnightCardAction) {
            if (!canPlayDevelopmentCard(0)) return false;
            state.setDevelopmentCardPlayedThisTurn(true);
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            //Remove the dev card from their hand
            state.getCurrentPlayer().removeDevCard(0);

            //Check and update the army size to see if they can claim the "Largest Army" trophy
            state.checkArmySize(state.getCurrentPlayerId());

            //Activate Robber phase
            state.setRobberPhase(true);

            //Skip the discarding phase and move to the move robber phase
            boolean[] allResolved = new boolean[state.getPlayerList().size()];
            java.util.Arrays.fill(allResolved, true);
            state.setRobberPlayerListHasDiscarded(allResolved);

            //Return
            return true;
        }

        //Player would like to use the Victory Points Dev Card
        if (action instanceof CatanUseVictoryPointCardAction) {
            // Victory Point cards are hidden and count from the moment purchased.
            // They are not spent/played as an ordinary development card.
            return false;
        }

        //Player would like to use Catan Year of Plenty Dev Card
        if (action instanceof CatanUseYearOfPlentyCardAction) {
            if (!canPlayDevelopmentCard(2)) return false;
            CatanUseYearOfPlentyCardAction card = (CatanUseYearOfPlentyCardAction) action;
            int first = card.getChosenResource(), second = card.getSecondChosenResource();
            if (first < 0 || first > 4 || second < 0 || second > 4) return false;

            // Year of Plenty draws from the bank, not unlimited cards.
            // If a stack is depleted, take only the cards still available.
            int firstAvailable = state.getBankResourceCount(first);
            int secondAvailable = state.getBankResourceCount(second);
            int firstAward = Math.min(1, firstAvailable);
            int secondAward = Math.min(1, secondAvailable - (first == second ? firstAward : 0));
            if (firstAward == 0 && secondAward == 0) return false;
            state.getCurrentPlayer().removeDevCard(2);
            state.setDevelopmentCardPlayedThisTurn(true);
            if (firstAward > 0) state.getCurrentPlayer().addResourceCard(first, firstAward);
            if (secondAward > 0) state.getCurrentPlayer().addResourceCard(second, secondAward);
            return true;
        }

        //Player would like to use the Monopoly Dev Card
        if (action instanceof CatanUseMonopolyCardAction) {
            if (!canPlayDevelopmentCard(3)) return false;
            int chosen = ((CatanUseMonopolyCardAction) action).getChosenResource();
            if (chosen < 0 || chosen > 4) return false;
            state.setDevelopmentCardPlayedThisTurn(true);
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            //Take this resource from the OTHER players, never from the bank.
            int totalResources = 0;
            int resourceId = ((CatanUseMonopolyCardAction) action).getChosenResource();

            //Iterate through all players and remove the specified resource, adding the the totalResources
            for (Player player : state.getPlayerList()) {
                if (player.getPlayerId() == state.getCurrentPlayerId()) continue;
                int resCount = player.getResourceCards()[resourceId];
                player.removeResourceCard(resourceId, resCount);
                totalResources += resCount;
            }

            //Add the totalResources of the specified resource then remove the dev card
            state.getCurrentPlayer().addResourceCard(resourceId, totalResources);
            state.getCurrentPlayer().removeDevCard(3);
            return true;
        }

        //Player would like to use the Road Building Dev Card
        if (action instanceof CatanUseRoadBuildingCardAction) {
            if (!canPlayDevelopmentCard(4)) return false;
            // Two roads at no resource cost, not four free resource cards.
            state.getCurrentPlayer().removeDevCard(4);
            state.setDevelopmentCardPlayedThisTurn(true);
            state.setFreeRoadsRemaining(2);
            return true;
        }

        /*---------------------------------- Robber Actions --------------------------------------*/

        //Player is Discarding Cards during Robber Discard Phase
        if (action instanceof CatanRobberDiscardAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "], playerId=" + ((CatanRobberDiscardAction) action).getPlayerId());

            //Discard specified resources
            CatanRobberDiscardAction discard = (CatanRobberDiscardAction) action;
            // The sender may discard only their own cards.
            if (players != null && getPlayerIdx(action.getPlayer()) != discard.getPlayerId())
                return false;
            return state.discardResources(discard.getPlayerId(), discard.getRobberDiscardedResources());
        }

        //Player is Moving robber during Move Robber Phase
        if (action instanceof CatanRobberMoveAction) {
            if (!state.isRobberPhase() || !state.allPlayersHaveDiscarded()
                    || ((CatanRobberMoveAction) action).getPlayerId() != state.getCurrentPlayerId())
                return false;
            Log.d(TAG, "makeMove() called with: action = [" + action + "]. playerId=" + ((CatanRobberMoveAction) action).getPlayerId());

            //Don't move if they've already moved the robber
            if (state.getHasMovedRobber()) {
                Log.d(TAG, "makeMove: the robber has already been moved");
                return false;
            }

            //Move the robber if valid; then set hasMovedRobber to true
            if (state.getBoard().moveRobber(((CatanRobberMoveAction) action).getHexagonId())) {
                Log.e(TAG, "makeMove() move robber: Player " + ((CatanRobberMoveAction) action).getPlayerId() + " moved the Robber to Hexagon " + ((CatanRobberMoveAction) action).getHexagonId());
                this.state.setHasMovedRobber(true);
                return true;
            }
            Log.e(TAG, "makeMove: moving the robber failed returning false.");
            return false;
        }

        //Player is Stealing from another player during Steal Phase
        if (action instanceof CatanRobberStealAction) {
            Log.d(TAG, "makeMove() called with: action = [" + action + "]");

            //Steal a random resource from a specified player
            return state.robberSteal(((CatanRobberStealAction) action).getPlayerId(), ((CatanRobberStealAction) action).getStealingFromPlayerId());
        }

        /*---------------------------------- Trade Actions ---------------------------------------*/

        // One action for automatic 4:1 / 3:1 / 2:1 and market discounts.
        // Market use is charged only after the entire exchange succeeds.
        if (action instanceof CatanTradeWithAutoBankAction) {
            if (!canTradeNow()) return false;
            CatanTradeWithAutoBankAction request = (CatanTradeWithAutoBankAction) action;
            int rate = CatanTradingRules.exchangeWithBank(state,
                    state.getCurrentPlayerId(), request.getGiving(), request.getReceiving());
            if (rate == CatanTradingRules.INVALID_RATE) return false;
            request.setExecutedRate(rate);
            return true;
        }


        // Trade with bank or an occupied port only after rolling the dice.
        if (action instanceof CatanTradeWithBankAction) {
            if (!canTradeNow()) return false;
            CatanTradeWithBankAction trade = (CatanTradeWithBankAction) action;
            int give = trade.getResourceIdGiving(), receive = trade.getResourceIdRec();
            if (!validResource(give) || !validResource(receive) || give == receive
                    || state.getBankResourceCount(receive) < 1
                    || state.getMarketState().blocksBankReceipt(receive)) return false;
            if (!state.getCurrentPlayer().removeResourceCard(give, 4)) return false;
            state.getCurrentPlayer().addResourceCard(receive, 1);
            return true;
        }

        if (action instanceof CatanTradeWithPortAction) {
            if (!canTradeNow()) return false;
            CatanTradeWithPortAction trade = (CatanTradeWithPortAction) action;
            Port port = trade.getPort();
            int received = trade.getResourceRecId();
            if (port == null || !validResource(received) || port.getTradeRatio() != 2
                    || !validResource(port.getResourceId())
                    || received == port.getResourceId() || !ownsPort(port, state.getCurrentPlayerId())
                    || state.getBankResourceCount(received) < 1
                    || state.getMarketState().blocksBankReceipt(received))
                return false;
            if (!state.getCurrentPlayer().removeResourceCard(port.getResourceId(), 2)) return false;
            state.getCurrentPlayer().addResourceCard(received, 1);
            return true;
        }

        if (action instanceof CatanTradeWithCustomPortAction) {
            if (!canTradeNow()) return false;
            CatanTradeWithCustomPortAction trade = (CatanTradeWithCustomPortAction) action;
            int give = trade.getResourceGiveId(), receive = trade.getResourceRecId();
            if (!validResource(give) || !validResource(receive) || give == receive) return false;
            boolean hasGeneralPort = false;
            for (Port port : state.getBoard().getPortList()) {
                if (port.getTradeRatio() == 3 && port.getResourceId() == -1
                        && ownsPort(port, state.getCurrentPlayerId())) {
                    hasGeneralPort = true;
                    break;
                }
            }
            if (!hasGeneralPort || state.getBankResourceCount(receive) < 1
                    || state.getMarketState().blocksBankReceipt(receive)
                    || !state.getCurrentPlayer().removeResourceCard(give, 3))
                return false;
            state.getCurrentPlayer().addResourceCard(receive, 1);
            return true;
        }

        // if we reach here, the GameAction object we received is not one that we recognize
        Log.e(TAG, "makeMove: FATAL ERROR: GameAction action was not and instance of an action class that we recognize.");
        return false;
    }

    /*---------------------- Methods for checking the Game State and updating it ------------------------------------*/

    /**
     * Notify the given player that its state has changed. This should involve sending
     * a GameInfo object to the player. If the game is not a perfect-information game
     * this method should remove any information from the game that the player is not
     * allowed to know.
     *
     * @param p the player to notify
     */
    @Override
    protected void sendUpdatedStateTo(GamePlayer p) {
        Log.d(TAG, "sendUpdatedStateTo() called with: p = [" + p + "]");
        Log.i(TAG, "sendUpdatedStateTo: state.toSting():" + this.state.toString());
        Log.i(TAG, "sendUpdatedStateTo: board.toString(): " + this.state.getBoard().toString());
        CatanGameState copy = new CatanGameState(state);
        Log.i(TAG, "sendUpdatedStateTo: board.toString(): " + copy.getBoard().toString());

        p.sendInfo(copy);
    }

    /**
     * Check if the game is over. It is over, return a string that tells
     * who the winner(s), if any, are. If the game is not over, return null;
     *
     * @return a message that tells who has won the game, or null if the
     * game is not over
     */
    private static String formatBundle(int[] bundle) {
        StringBuilder description = new StringBuilder();
        for (int resource = 0; resource < 5; resource++) {
            if (bundle[resource] < 1) continue;
            if (description.length() > 0) description.append(" + ");
            description.append(bundle[resource]).append(" ")
                    .append(RESOURCE_NAMES[resource]);
        }
        return description.toString();
    }

    private void clearTradeSession(String reason) {
        if (pendingTrade != null && players != null) {
            CatanTradeNegotiation old = pendingTrade;
            CatanTradeInfo cancelled = new CatanTradeInfo(
                    CatanTradeInfo.CANCELLED, old.id, old.proposer, -1,
                    null, null, null, null, null, reason);
            players[old.proposer].sendInfo(cancelled);
            for (int id = 0; id < players.length; id++)
                if (old.invited(id)) players[id].sendInfo(cancelled);
        }
        pendingTrade = null;
        state.setTradePending(false);
    }

    private boolean canTradeNow() {
        return !state.isSetupPhase() && state.isActionPhase() && !state.isRobberPhase();
    }

    private static boolean validResource(int resourceId) {
        return resourceId >= 0 && resourceId < 5;
    }

    private boolean ownsPort(Port candidate, int owner) {
        for (Port actual : state.getBoard().getPortList()) {
            if (actual.getTradeRatio() != candidate.getTradeRatio()
                    || actual.getResourceId() != candidate.getResourceId()) continue;
            boolean sameEnds = actual.getIntersectionA() == candidate.getIntersectionA()
                    && actual.getIntersectionB() == candidate.getIntersectionB();
            boolean flipped = actual.getIntersectionA() == candidate.getIntersectionB()
                    && actual.getIntersectionB() == candidate.getIntersectionA();
            if (!sameEnds && !flipped) continue;
            for (int point : new int[]{actual.getIntersectionA(), actual.getIntersectionB()}) {
                if (state.getBoard().hasBuilding(point)
                        && state.getBoard().getBuildingAtIntersection(point).getOwnerId() == owner)
                    return true;
            }
        }
        return false;
    }

    private int ownedRoads(int owner) {
        int count = 0;
        for (Road road : state.getBoard().getRoads()) {
            if (road.getOwnerId() == owner) count++;
        }
        return count;
    }

    private int ownedBuildings(int owner, Class<?> type) {
        int count = 0;
        for (edu.up.cs.androidcatan.catan.gamestate.buildings.Building building :
                state.getBoard().getBuildings()) {
            if (building != null && building.getOwnerId() == owner && type.isInstance(building)) count++;
        }
        return count;
    }

    private boolean canPlayDevelopmentCard(int type) {
        // Action development cards may be used before or after rolling.
        return !state.isSetupPhase() && !state.isRobberPhase()
                && !state.hasPlayedDevelopmentCardThisTurn()
                && state.getCurrentPlayer().getPlayableDevCards().contains(type);
    }

    private void updateLongestRoadHolder() {
        state.setCurrentLongestRoadPlayerId(
                LongestRoadCalculator.findHolder(state.getBoard().getRoads(),
                        state.getBoard().getBuildings(), state.getCurrentLongestRoadPlayerId()));
    }

    @Override
    public String checkIfGameOver() {
        Log.d(TAG, "checkIfGameOver() called");

        //NULL check
        if (playerNames == null) {
            Log.e(TAG, "checkIfGameOver: player names is null");
            return null;
        }

        // In classic Catan, only the player whose turn it is may win.
        // Victory Point development cards count automatically while held.
        int i = state.getCurrentPlayerId();
        Player current = state.getCurrentPlayer();
        int lr = (state.getCurrentLongestRoadPlayerId() == i) ? 2 : 0;
        int la = (state.getCurrentLargestArmyPlayerId() == i) ? 2 : 0;
        // Buying a Victory Point card already increases private VP by one.
        int total = current.getVictoryPoints() + current.getVictoryPointsPrivate() + lr + la;
        return total >= 10 ? playerNames[i] + " wins!" : null;
    }

    /**
     * Starts the game. Creates initial game state.
     *
     * @param players The list of players in the game.
     */
    @Override
    public void start(GamePlayer[] players) {
        // Restore the saved rules state BEFORE LocalGame starts player threads
        // and sends their initial BindGameInfo/StartGameInfo messages.
        super.start(players);
    }

    public void setState(CatanGameState state) {
        if (state == null || state.getBoard() == null) {
            throw new IllegalArgumentException("Cannot resume empty Catan game");
        }
        this.state = new CatanGameState(state);
    }

}
