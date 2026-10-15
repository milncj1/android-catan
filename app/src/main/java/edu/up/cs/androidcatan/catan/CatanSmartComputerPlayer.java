package edu.up.cs.androidcatan.catan;

import android.util.Log;

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
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithAutoBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeOfferAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseMonopolyCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseRoadBuildingCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseVictoryPointCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;
import edu.up.cs.androidcatan.game.GameComputerPlayer;
import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * https://github.com/alexweininger/android-catan
 **/

public class CatanSmartComputerPlayer extends GameComputerPlayer {
    private static final String TAG = "CatanSmartComputerPlayer";

    private int[] robberResourcesDiscard = new int[]{0, 0, 0, 0, 0};
    private int hexId;
    int playerWithMostVPs;
    int playerWithMostVPsIntersection;
    boolean foundBuilding;
    ArrayList<Integer> buildingsBuiltOnThisTurn = new ArrayList<>();
    private int lastSettlementIntersectionId = -1;
    private final CatanAiDifficulty difficulty;
    private CatanGameState lastTradeSnapshot;

    CatanSmartComputerPlayer(String name) {
        this(name, CatanAiDifficulty.NORMAL);
    }

    CatanSmartComputerPlayer(String name, CatanAiDifficulty difficulty) {
        super(name);
        this.difficulty = difficulty == null ? CatanAiDifficulty.NORMAL : difficulty;
    }

    /**
     * callback method--game's state has changed
     *
     * @param info the information (presumably containing the game's state)
     */
    @Override
    protected void receiveInfo(GameInfo info) {
        Log.i(TAG, "receiveInfo() of player " + this.playerNum + " called.");

        //Make sure we are dealing with Catan game
        if (info instanceof CatanTradeInfo) {
            CatanTradeAi.onTradeInfo((CatanTradeInfo) info, lastTradeSnapshot,
                    this, game, playerNum, difficulty);
            return;
        }
        if (!(info instanceof CatanGameState)) return;
        lastTradeSnapshot = (CatanGameState) info;
        if (lastTradeSnapshot.isTradePending()) return;
        CatanGameState gs = (CatanGameState) info;
        Log.d(TAG, "receiveInfo: game state current player: " + gs.getCurrentPlayerId() + " this.playerNum: " + this.playerNum);

        //Random initialized player
        Random random = new Random();

        Log.i(TAG, "receiveInfo() of player " + this.playerNum + " called.");

        Log.d(TAG, "receiveInfo: game state current player: " + gs.getCurrentPlayerId() + " this.playerNum: " + this.playerNum);

        //Prevents computer from playing if it is not their turn OR if the robber has been played for discard purposes
        if (this.playerNum != gs.getCurrentPlayerId() && !gs.isRobberPhase()) {
            Log.w(TAG, "receiveInfo: not my turn and not the robber phase, returning playerNum=" + this.playerNum + " current player=" + gs.getCurrentPlayerId());
            return;
        }

        //Keep count of settlements and buildings
        int settlementCount = 0;
        int roadCount = 0;

        // Get the number of settlements the player has built.
        for (Building building : gs.getBoard().getBuildings()) {
            if (building != null && building.getOwnerId() == this.playerNum) {
                settlementCount++;
            }
        }

        // Get the number of roads the player has built.
        for (Road road : gs.getBoard().getRoads()) {
            if (road.getOwnerId() == this.playerNum) {
                roadCount++;
            }
        }

        Log.i(TAG, "receiveInfo: roadCount: " + roadCount + " settlementCount: " + settlementCount);

        /*------------------------------------CPUs Setup Phase Actions-----------------------------------------*/

        // if it is the setup phase and the players turn
        if (gs.isSetupPhase() && this.playerNum == gs.getCurrentPlayerId()) {
            // These flags come from the authoritative (and autosaved) game.
            // Do not use temporary CPU fields, which reset after Continue Game.
            int placedSettlement = gs.getCurrentSetupSettlement();
            if (placedSettlement < 0) {
                int best = -1;
                double bestValue = Double.NEGATIVE_INFINITY;
                for (int vertex = 0; vertex < gs.getBoard().getBuildings().length; vertex++) {
                    if (!gs.getBoard().validBuildingLocation(playerNum, true, vertex)) continue;
                    double value = (difficulty.looksAhead()
                            ? CatanAiPlanner.scoreSettlement(gs, vertex, playerNum,
                                    difficulty == CatanAiDifficulty.EXPERT)
                            : CatanStrategy.scoreIntersection(gs, vertex, playerNum))
                            + random.nextDouble() * (difficulty == CatanAiDifficulty.EXPERT ? 0.005 : 0.1);
                    if (value > bestValue) {
                        best = vertex;
                        bestValue = value;
                    }
                }
                if (best >= 0) {
                    game.sendAction(new CatanBuildSettlementAction(this, true, playerNum, best));
                } else Log.e(TAG, "No legal initial settlement for player " + playerNum);
                return;
            }
            if (!gs.isSetupRoadBuiltThisTurn()) {
                int bestTo = -1;
                double bestValue = Double.NEGATIVE_INFINITY;
                for (Integer to : gs.getBoard().getIntersectionAdjacencyList().get(placedSettlement)) {
                    if (!gs.getBoard().validRoadPlacement(playerNum, true,
                            placedSettlement, to, placedSettlement)) continue;
                    double score = (difficulty.looksAhead()
                            ? CatanAiPlanner.scoreRoad(gs, to, playerNum,
                                    difficulty == CatanAiDifficulty.EXPERT)
                            : CatanStrategy.scoreRoadDestination(gs, to, playerNum))
                            + random.nextDouble() * (difficulty == CatanAiDifficulty.EXPERT ? .005 : .1);
                    if (score > bestValue) {
                        bestValue = score;
                        bestTo = to;
                    }
                }
                if (bestTo >= 0) {
                    game.sendAction(new CatanBuildRoadAction(this, true, playerNum,
                            placedSettlement, bestTo));
                } else Log.e(TAG, "No legal initial road from " + placedSettlement);
                return;
            }
            game.sendAction(new CatanEndTurnAction(this));
            return;
        } // setup phase if statement END

        /*------------------------------Setup Phase End------------------------------------------*/


        /*-------------------------------CPUs Roll Dice Action--------------------------------------*/

        //If the player has not yet rolled yet AND it is this players turn, make them roll the dice
        if (!gs.isSetupPhase() && !gs.isActionPhase() && !gs.isRobberPhase() && gs.getCurrentPlayerId() == playerNum) {
            sleep(CatanExperienceSettings.pauseMillis(300));
            game.sendAction(new CatanRollDiceAction(this));
            sleep(CatanExperienceSettings.pauseMillis(300));
            return;
        }
        /*-------------------------------CPUs Robber Actions--------------------------------------*/

        //Robber phase accessible for any turn, due to them possibly needing to discard
        if (gs.isRobberPhase()) {
            Log.i(TAG, "receiveInfo: Computer has reached the Robber Phase");
            sleep(CatanExperienceSettings.pauseMillis(500));
            /*--------------------Discard Phase--------------------*/

            //1. Check if this player has discarded
            if (!gs.getRobberPlayerListHasDiscarded()[playerNum]) {
                int[] held = gs.getPlayerList().get(playerNum).getResourceCards().clone();
                int total = 0;
                for (int count : held) total += count;
                int required = total > 7 ? total / 2 : 0;
                int[] discarded = new int[5];
                // Preserve scarce resource types where possible.
                for (int n = 0; n < required; n++) {
                    int mostCommon = -1;
                    for (int res = 0; res < held.length; res++) {
                        if (held[res] > 0 && (mostCommon < 0 || held[res] > held[mostCommon])) {
                            mostCommon = res;
                        }
                    }
                    if (mostCommon < 0) return;
                    held[mostCommon]--;
                    discarded[mostCommon]++;
                }
                game.sendAction(new CatanRobberDiscardAction(this, playerNum, discarded));
                return;
            }//End of discard phase

            //5. Wait until all players have completed their discard phase
            if (!gs.allPlayersHaveDiscarded() && gs.getCurrentPlayerId() == playerNum) {
                Log.d(TAG, "receiveInfo: Not all players have discarded!!!!");
                return;
            }

            Log.i(TAG, "receiveInfo: Robber Phase --> Move Robber Phase");

            /*----------------------Move Robber Phase----------------*/

            //6. If it is this players turn, continue to rest of robber phase; otherwise player is done
            if (gs.getCurrentPlayerId() == playerNum) {
                Log.i(TAG, "receiveInfo: Computer is moving robber");

                //7. Check if player has move robber; if not move to random, VALID hexagon
                if (!gs.getHasMovedRobber()) {
                    if (difficulty.looksAhead()) {
                        int hex = CatanAiPlanner.chooseRobberHex(gs, playerNum,
                                difficulty == CatanAiDifficulty.EXPERT);
                        if (hex >= 0) {
                            game.sendAction(new CatanRobberMoveAction(this, playerNum, hex));
                            return;
                        }
                    }

                    //8.Check Player who has most victory points and get player ID (Cannot be this player)
                    playerWithMostVPs = gs.getPlayerWithMostVPsExcludingCurrentPlayer(playerNum);

                    //What intersection contains the building; also used as intersection to steal from
                    playerWithMostVPsIntersection = 0;

                    //A building has been found that contains the intersection of player with most VPs
                    foundBuilding = false;
                    //9. Iterate through each Hexagon and find one that has the playersId at one of the adjacent intersections
                    for (Hexagon hex : gs.getBoard().getHexagons()) {
                        Log.i(TAG, "receiveInfo: Checking hexagon " + hex.getHexagonId() + " for player " + playerWithMostVPs);
                        if (gs.getBoard().getRobber().getHexagonId() != hex.getHexagonId()) {
                            for (Integer intersection : gs.getBoard().getHexToIntIdMap().get(hex.getHexagonId())) {
                                if (gs.getBoard().hasBuilding(intersection) && gs.getBoard().getBuildingAtIntersection(intersection).getOwnerId() == playerWithMostVPs && tryMoveRobber(hex.getHexagonId(), gs)) {
                                    Log.i(TAG, "receiveInfo: Found player at hex " + hex.getHexagonId() + ".");
                                    hexId = hex.getHexagonId();
                                    playerWithMostVPsIntersection = intersection;
                                    foundBuilding = true;
                                }
                            }
                        }

                        //We've found our hex and building, stop iteration of loop
                        if (foundBuilding) {
                            break;
                        }
                    }

                    //10. Send the action to move the robber; information has been saved to also steal with the robber

                    // If the leading opponent has no eligible settlement, move
                    // the robber to another tile rather than repeating an invalid move.
                    if (!foundBuilding) {
                        for (Hexagon candidate : gs.getBoard().getHexagons()) {
                            if (candidate.getHexagonId() != gs.getBoard().getRobber().getHexagonId()) {
                                hexId = candidate.getHexagonId();
                                break;
                            }
                        }
                    }
                    sleep(CatanExperienceSettings.pauseMillis(250));
                    game.sendAction(new CatanRobberMoveAction(this, playerNum, hexId));
                    return;

                }//End of Move Robber Phase

                /*----------------Steal Resource Phase--------------*/

                //11. Now Steal from the selected intersection
                sleep(CatanExperienceSettings.pauseMillis(250));
                if (difficulty.looksAhead())
                    playerWithMostVPs = gs.getPlayerWithMostVPsExcludingCurrentPlayer(playerNum);
                int victim = -1;
                if (gs.isRobberVictim(playerNum, playerWithMostVPs)) {
                    victim = playerWithMostVPs;
                } else {
                    for (int other = 0; other < gs.getPlayerList().size(); other++) {
                        if (gs.isRobberVictim(playerNum, other)) {
                            victim = other;
                            break;
                        }
                    }
                }
                game.sendAction(new CatanRobberStealAction(this, playerNum, victim));
                sleep(CatanExperienceSettings.pauseMillis(500));
                return;
            }//End of Robber Phases after Discard phase
        }//End of Robber Phase

        // not setup phase if statement END

        /* ----------------------------------- CPUs Normal Action Phase ------------------------------------ */

        //Player can take calculated actions
        if (!gs.isSetupPhase() && gs.isActionPhase() && gs.getCurrentPlayerId() == this.playerNum && !gs.isRobberPhase()) {

            //Checking if they have actual buildings
            int settlementIntersection = getBuildingOfPlayer(gs);
            Log.d(TAG, "receiveInfo: settlementIntersection = " + settlementIntersection);
            if (settlementIntersection == -1) {
                Log.d(TAG, "receiveInfo: There is no settlementIntersection");
                return;
            }

            sleep(CatanExperienceSettings.pauseMillis(200));

            // A played Road Building card grants two free road placements.
            // Place them before considering any paid action or ending the turn.
            if (gs.getFreeRoadsRemaining() > 0) {
                if (buildBestRoad(gs)) return;
                game.sendAction(new CatanEndTurnAction(this));
                return;
            }

            // Initiate one useful player trade on this CPU's turn, at most.
            // Do not interrupt free-road placement or exceed the per-round cap.
            if (!gs.hasCpuOfferedThisTurn()) {
                int[][] proposal = CatanNegotiationAI.proposeOnOwnTurn(gs, playerNum);
                if (proposal != null) {
                    int need = 0;
                    for (int r = 0; r < 5; r++)
                        if (proposal[1][r] > 0) { need = r; break; }
                    boolean mayInviteHuman = gs.getMarketState()
                            .mayComputerOffer(playerNum, gs.getPlayerList().size());
                    // Occasionally choose a CPU rival even when the human is
                    // eligible. Those quiet exchanges don't interrupt play.
                    boolean preferHuman = mayInviteHuman
                            && (gs.getPlayerList().size() == 2
                                || (gs.getMarketState().getCompletedRounds()
                                    + playerNum) % 3 != 0);
                    int partner = preferHuman ? 0
                            : CatanNegotiationAI.bestComputerTradingPartner(
                                    gs, playerNum, need);
                    if (partner < 0 && mayInviteHuman) partner = 0;
                    if (partner >= 0) {
                        game.sendAction(new CatanTradeOfferAction(this,
                                new int[]{partner}, proposal[0], proposal[1]));
                        return;
                    }
                }
            }

            // The harder levels combine legal moves within one turn.
            // The old Normal AI remains unchanged as the default.
            if (difficulty.looksAhead()) {
                if (takeAdvancedAction(gs)) return;
                game.sendAction(new CatanEndTurnAction(this));
                return;
            }

            /*****Looks to trade so they can potentially build a road******/
            int brickCount = gs.getPlayerList().get(this.playerNum).getResourceCards()[0];
            int grainCount = gs.getPlayerList().get(this.playerNum).getResourceCards()[1];
            int lumberCount = gs.getPlayerList().get(this.playerNum).getResourceCards()[2];
            int oreCount = gs.getPlayerList().get(this.playerNum).getResourceCards()[3];
            int woolCount = gs.getPlayerList().get(this.playerNum).getResourceCards()[4];
            int tradeResourceId;
            if (brickCount == 0) {
                tradeResourceId = 0;
            } else {
                tradeResourceId = 2;
            }

            /******Looks to build a settlement****/
            if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Settlement.resourceCost)) {
                int bestSite = -1;
                double bestSiteScore = Double.NEGATIVE_INFINITY;
                for (Integer candidate : getPlayerRoadIntersection(getPlayerRoads(gs))) {
                    if (!gs.getBoard().validBuildingLocation(this.playerNum, false, candidate)) continue;
                    double score = CatanStrategy.scoreIntersection(gs, candidate, this.playerNum, difficulty);
                    if (score > bestSiteScore) {
                        bestSiteScore = score;
                        bestSite = candidate;
                    }
                }
                if (bestSite >= 0) {
                    game.sendAction(new CatanBuildSettlementAction(this, false, this.playerNum, bestSite));
                    // Advanced players can build again if they still have resources.
                    // The authoritative game callback decides what is legal next.
                    if (!difficulty.looksAhead()) game.sendAction(new CatanEndTurnAction(this));
                    sleep(CatanExperienceSettings.pauseMillis(350));
                    return;
                }
            }

            /*****Looks to build a city from a settlement****/
            // Upgrade the settlement that doubles the most expected production.
            if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(City.resourceCost)) {
                int bestCitySite = -1;
                double bestCityScore = Double.NEGATIVE_INFINITY;
                for (int n = 0; n < gs.getBoard().getBuildings().length; n++) {
                    Building building = gs.getBoard().getBuildings()[n];
                    if (!(building instanceof Settlement) || building.getOwnerId() != this.playerNum) continue;
                    double score = CatanStrategy.scoreIntersection(gs, n, this.playerNum, difficulty);
                    if (score > bestCityScore) {
                        bestCityScore = score;
                        bestCitySite = n;
                    }
                }
                if (bestCitySite >= 0) {
                    game.sendAction(new CatanBuildCityAction(this, false, this.playerNum, bestCitySite));
                    if (!difficulty.looksAhead()) game.sendAction(new CatanEndTurnAction(this));
                    sleep(CatanExperienceSettings.pauseMillis(350));
                    return;
                }
            }

            /*****Looks to use a dev card*******/
            if (!gs.hasPlayedDevelopmentCardThisTurn()
                    && !gs.getPlayerList().get(this.playerNum).getPlayableDevCards().isEmpty()) {
                Log.d(TAG, "receiveInfo: Player " + this.playerNum + " has a playable development card");
                for (int n = 0; n < gs.getPlayerList().get(this.playerNum).getPlayableDevCards().size(); n++) {

                    // Victory Point cards stay hidden and score automatically.
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 1) continue;

                    //if they have a knight card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 0) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using knight card");
                        // A Knight opens the mandatory robber phase. Do not
                        // queue End Turn now: the engine must first process
                        // discards, robber movement and an eligible steal.
                        // The next authoritative state callback resumes this CPU.
                        game.sendAction(new CatanUseKnightCardAction(this));
                        return;
                    }

                    //if they have a year of plenty card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 2) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using year of plenty card");
                        // Pick a resource which is actually present in the finite bank.
                        // An illegal development-card action would otherwise leave the
                        // computer waiting forever for a fresh game-state callback.
                        int selected = -1;
                        int fewestHeld = Integer.MAX_VALUE;
                        for (int res = 0; res < 5; res++) {
                            if (gs.getBankResourceCount(res) > 0
                                    && gs.getPlayerList().get(playerNum).getResourceCards()[res] < fewestHeld) {
                                selected = res;
                                fewestHeld = gs.getPlayerList().get(playerNum).getResourceCards()[res];
                            }
                        }
                        if (selected < 0) continue;
                        game.sendAction(new CatanUseYearOfPlentyCardAction(this, selected));
                        game.sendAction(new CatanEndTurnAction(this));
                        return;
                    }

                    //if they have a monopoly card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 3) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using monopoly card");
                        game.sendAction(new CatanUseMonopolyCardAction(this, difficulty.looksAhead()
                                ? mostProductiveOpponentResource(gs) : playerLeastResource(gs))); //change chosenResource
                        game.sendAction(new CatanEndTurnAction(this));
                        sleep(CatanExperienceSettings.pauseMillis(350));
                        Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                        return;
                    }

                    //if they have a road card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 4) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using road dev card");
                        if (getPlayerRoads(gs).size() >= 15) continue;
                        game.sendAction(new CatanUseRoadBuildingCardAction(this));
                        return;
                    }
                }
            }

            /******Looks to buy a dev card*******/
            if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(DevelopmentCard.resourceCost) && gs.hasDevelopmentCardsAvailable()) {
                Log.d(TAG, "receiveInfo: Player " + this.playerNum + " purchased dev card");
                game.sendAction(new CatanBuyDevCardAction(this));
                Log.d(TAG, "receiveInfo: CatanBuyDevCardAction sent");
                if (!difficulty.looksAhead()) game.sendAction(new CatanEndTurnAction(this));
                sleep(CatanExperienceSettings.pauseMillis(350));
                return;
            }

            /*****Normal makes a basic, legal bank trade towards a build.*****/
            int[] normalTrade = CatanAiPlanner.bestBankTrade(gs, this.playerNum, false);
            if (normalTrade != null) {
                game.sendAction(new CatanTradeWithAutoBankAction(this,
                        normalTrade[0], normalTrade[1]));
                game.sendAction(new CatanEndTurnAction(this));
                return;
            }

            /******Looks to extend towards the next useful settlement*****/
            if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Road.resourceCost)
                    && getPlayerRoads(gs).size() < 15 && buildBestRoad(gs)) {
                game.sendAction(new CatanEndTurnAction(this));
                return;
            }

            //End turn if no other action can be taken
            game.sendAction(new CatanEndTurnAction(this));
            Log.d(TAG, "receiveInfo: CatanEndTurnAction sent: Nothing was built");
            sleep(CatanExperienceSettings.pauseMillis(350));
            return;
            //not setup phase if statement END
        }
    }// receiveInfo() END

    /**
     * One accepted action per state update. Continues building/playing until
     * no legal improvements remain, then the caller ends the turn.
     */
    private boolean takeAdvancedAction(CatanGameState gs) {
        Player me = gs.getPlayerList().get(playerNum);
        boolean expert = difficulty == CatanAiDifficulty.EXPERT;
        int ownSettlements = 0, ownCities = 0, ownRoads = 0;
        for (Building piece : gs.getBoard().getBuildings()) {
            if (piece == null || piece.getOwnerId() != playerNum) continue;
            if (piece instanceof Settlement) ownSettlements++;
            else if (piece instanceof City) ownCities++;
        }
        for (Road road : gs.getBoard().getRoads())
            if (road.getOwnerId() == playerNum) ownRoads++;

        int settlement = ownSettlements < 5
                ? CatanAiPlanner.bestNewSettlement(gs, playerNum, expert) : -1;
        int city = ownCities < 4 ? CatanAiPlanner.bestCity(gs, playerNum, expert) : -1;
        boolean canSettle = settlement >= 0
                && me.hasResourceBundle(Settlement.resourceCost);
        boolean canUpgrade = city >= 0 && me.hasResourceBundle(City.resourceCost);
        int points = me.getVictoryPoints() + me.getVictoryPointsPrivate()
                + (gs.getCurrentLongestRoadPlayerId() == playerNum ? 2 : 0)
                + (gs.getCurrentLargestArmyPlayerId() == playerNum ? 2 : 0);

        // Secure an immediate win instead of buying development cards.
        if (points >= 9) {
            if (canSettle) {
                game.sendAction(new CatanBuildSettlementAction(this, false, playerNum, settlement));
                return true;
            }
            if (canUpgrade) {
                game.sendAction(new CatanBuildCityAction(this, false, playerNum, city));
                return true;
            }
        }
        if (expert && canUpgrade) {
            game.sendAction(new CatanBuildCityAction(this, false, playerNum, city));
            return true;
        }
        if (canSettle) {
            game.sendAction(new CatanBuildSettlementAction(this, false, playerNum, settlement));
            return true;
        }
        if (canUpgrade) {
            game.sendAction(new CatanBuildCityAction(this, false, playerNum, city));
            return true;
        }

        if (!gs.hasPlayedDevelopmentCardThisTurn()) {
            ArrayList<Integer> playable = me.getPlayableDevCards();
            if (ownRoads < 14 && playable.contains(4)
                    && CatanAiPlanner.bestRoad(gs, playerNum, expert) != null) {
                game.sendAction(new CatanUseRoadBuildingCardAction(this));
                return true;
            }
            if (playable.contains(2)) {
                int resource = leastAvailableResource(gs);
                if (resource >= 0) {
                    game.sendAction(new CatanUseYearOfPlentyCardAction(this, resource));
                    return true;
                }
            }
            if (playable.contains(0) && (expert || ownRoads >= 4)) {
                game.sendAction(new CatanUseKnightCardAction(this));
                return true;
            }
            if (playable.contains(3) && ownRoads >= 4) {
                game.sendAction(new CatanUseMonopolyCardAction(this,
                        CatanAiPlanner.chooseMonopolyResource(gs, playerNum)));
                return true;
            }
        }

        // Expand into reachable high-quality sites.
        if (ownRoads < 15 && me.hasResourceBundle(Road.resourceCost)
                && buildBestRoad(gs)) return true;

        // A targeted bank trade can unlock a specific next build.
        int[] trade = CatanAiPlanner.bestBankTrade(gs, playerNum, expert);
        if (trade != null) {
            game.sendAction(new CatanTradeWithAutoBankAction(this, trade[0], trade[1]));
            return true;
        }

        if (gs.hasDevelopmentCardsAvailable()
                && me.hasResourceBundle(DevelopmentCard.resourceCost)) {
            game.sendAction(new CatanBuyDevCardAction(this));
            return true;
        }
        return false;
    }

    private int leastAvailableResource(CatanGameState gs) {
        int[] hand = gs.getPlayerList().get(playerNum).getResourceCards();
        int result = -1;
        for (int r = 0; r < 5; r++) {
            if (gs.getBankResourceCount(r) <= 0) continue;
            if (result == -1 || hand[r] < hand[result]) result = r;
        }
        return result;
    }

    /**
     * Choose a legal road with the highest future expansion value.
     * Only place the road here; the next game-state callback chooses whether
     * to place another free road or finish the turn.
     */
    private boolean buildBestRoad(CatanGameState gs) {
        int roadsUsed = 0;
        for (Road road : gs.getBoard().getRoads())
            if (road.getOwnerId() == playerNum) roadsUsed++;
        if (roadsUsed >= 15) return false;
        if (difficulty.looksAhead()) {
            int[] choice = CatanAiPlanner.bestRoad(gs, playerNum,
                    difficulty == CatanAiDifficulty.EXPERT);
            if (choice == null) return false;
            game.sendAction(new CatanBuildRoadAction(this, false, playerNum,
                    choice[0], choice[1]));
            return true;
        }
        Random random = new Random();
        int bestFrom = -1, bestTo = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Integer from : getPlayerRoadIntersection(getPlayerRoads(gs))) {
            for (Integer to : gs.getBoard().getIntersectionAdjacencyList().get(from)) {
                if (!gs.getBoard().validRoadPlacement(playerNum, false, from, to)) continue;
                double score = CatanStrategy.scoreRoadDestination(gs, to, playerNum, difficulty)
                        + random.nextDouble() * 0.2;
                if (score > bestScore) {
                    bestScore = score;
                    bestFrom = from;
                    bestTo = to;
                }
            }
        }
        if (bestFrom < 0) return false;
        game.sendAction(new CatanBuildRoadAction(this, false, playerNum, bestFrom, bestTo));
        return true;
    }

    /**
     * Attempts the move the robber if possible
     *
     * @param hexId the ID of the tile to attempt the move the robber to
     * @param gs    the ame state
     * @return true or false depending on if the move was possible
     */
    private boolean tryMoveRobber(int hexId, CatanGameState gs) {

        //Checks hex has been selected
        if (hexId == -1) {
            Log.d(TAG, "tryMoveRobber: Invalid hex ID from CPU");
            return false;
        }

        //Checks if it is a different hex from the same spot
        if (hexId == gs.getBoard().getRobber().getHexagonId()) {
            Log.d(TAG, "tryMoveRobber: Same hexId as robber");
            return false;
        }

        //Checks if player has selected the desert tile
        if (gs.getBoard().getHexagons().get(hexId).getResourceId() == 5) {
            Log.d(TAG, "tryMoveRobber: Desert tile selected; invalid.");
            return false;
        }

        //Checking to make sure there is an opponent building adjacent to the selected hex
        ArrayList<Integer> intersections = gs.getBoard().getHexToIntIdMap().get(hexId);

        for (Integer intersection : intersections) {
            if (gs.getBoard().getBuildings()[intersection] != null) {
                if (gs.getBoard().getBuildings()[intersection].getOwnerId() != playerNum) {
                    return true;
                }
            }
        }

        //Invalid Hex
        Log.d(TAG, "tryMoveRobber: ");
        return false;
    }

    /**
     * @param gs CatanGameState object to get the buildings on the board
     * @return gets the first building wth the owner's id and returns its intersection location
     */
    private int getBuildingOfPlayer(CatanGameState gs) {
        for (int n = 0; n < gs.getBoard().getBuildings().length; n++) {
            if (gs.getBoard().getBuildings()[n] != null && gs.getBoard().getBuildings()[n].getOwnerId() == this.playerNum) {
                return n;
            }
        }
        return -1;
    }

    /**
     * @param gs CatanGameState object to get the roads on the board
     * @return ArrayList of roads that that player owns
     */
    private ArrayList<Road> getPlayerRoads(CatanGameState gs) {
        ArrayList<Road> playerRoads = new ArrayList<>();
        for (int n = 0; n < gs.getBoard().getRoads().size(); n++) {
            if (gs.getBoard().getRoads().get(n).getOwnerId() == this.playerNum) {
                playerRoads.add(gs.getBoard().getRoads().get(n));
            }
        }
        return playerRoads;
    }

    /**
     * @param playerRoads takes a list of a players Road
     * @return returns an arrayList of intersections along the player's roads
     */
    private ArrayList<Integer> getPlayerRoadIntersection(ArrayList<Road> playerRoads) {
        ArrayList<Integer> intersections = new ArrayList<>();
        for (int n = 0; n < playerRoads.size(); n++) {
            intersections.add(playerRoads.get(n).getIntersectionAId());
            intersections.add(playerRoads.get(n).getIntersectionBId());
        }

        Log.d(TAG, "With repeat Intersections: " + intersections.toString());

        return intersections;
    }

    /**
     * @param intersectionId intersection you are checking to see it's resource
     * @param gs
     * @return true if the id is for brick or lumber, false if it isn't
     */
    private boolean checkIntersectionResource(int intersectionId, CatanGameState gs) {
        Log.d(TAG, "checkIntersectionResource() called with: intersectionId = [" + intersectionId + "], gs = [" + gs + "]");
        ArrayList<Integer> adjHexIds = gs.getBoard().getIntToHexIdMap().get(intersectionId);
        for (Integer adjHexId : adjHexIds) {
            Log.d(TAG, "checkIntersectionResource: the length of getBuildings is: " + gs.getBoard().getBuildings().length);

            //change back to 0 and 2 for building a road
            if (gs.getBoard().getHexagonFromId(adjHexId).getResourceId() == 0 || gs.getBoard().getHexagonFromId(adjHexId).getResourceId() == 2) {
                Log.d(TAG, "checkIntersectionResource() returned: " + true);
                return true;
            }
        }

        Log.d(TAG, "checkIntersectionResource() returned: " + false);
        return false;
    }

    /**
     * Choose Monopoly using only visible buildings and their production
     * probabilities. The CPU must not peek at hidden opponent hands.
     */
    private int mostProductiveOpponentResource(CatanGameState gs) {
        double[] demand = new double[5];
        for (int point = 0; point < gs.getBoard().getBuildings().length; point++) {
            Building b = gs.getBoard().getBuildings()[point];
            if (b == null || b.getOwnerId() == playerNum) continue;
            for (Integer hexId : gs.getBoard().getIntToHexIdMap().get(point)) {
                Hexagon h = gs.getBoard().getHexagonFromId(hexId);
                int resource = h.getResourceId();
                int number = h.getChitValue();
                if (resource < 0 || resource >= 5 || number < 2 || number > 12 || number == 7)
                    continue;
                double pips = 6 - Math.abs(7 - number);
                demand[resource] += pips * b.getVictoryPoints();
            }
        }
        int best = 0;
        for (int resource = 1; resource < 5; resource++)
            if (demand[resource] > demand[best]) best = resource;
        return best;
    }

    private int playerLeastResource(CatanGameState gs) {
        int[] cards = gs.getPlayerList().get(this.playerNum).getResourceCards();
        int leastIndex = 0;
        for (int i = 1; i < cards.length; i++) {
            if (cards[i] < cards[leastIndex]) leastIndex = i;
        }
        return leastIndex;
    }

    /**
     * @param gs
     * @return true if below 10 roads built, false otherwise
     */
    private boolean playerRoadCount(CatanGameState gs) {
        int count = 0;
        for (int n = 0; n < gs.getBoard().getRoads().size(); n++) {
            if (gs.getBoard().getRoads().get(n).getOwnerId() == this.playerNum) {
                count++;
            }
        }
        Log.d(TAG, "playerRoadCount: Count of player " + this.playerNum + " roads is at " + count);
        return count < 10;
    }

    /**
     * @param intersection to check how many roads it has coming off of it
     * @return true if road intersection is less than 24, false otherwise
     */
    private boolean checkSetupPhaseIntersection(int intersection) {
        return intersection <= 23;
    }
} // CatanSmartComputerPlayer class END


