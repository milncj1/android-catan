package edu.up.cs.androidcatan.catan;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import edu.up.cs.androidcatan.catan.actions.CatanBuildCityAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberDiscardAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberMoveAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberStealAction;
import edu.up.cs.androidcatan.catan.actions.CatanRollDiceAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuyDevCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseMonopolyCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseRoadBuildingCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
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
public class CatanDumbComputerPlayer extends GameComputerPlayer implements Serializable {
    private static final String TAG = "CatanDumbComputerPlayer";

    private int lastSettlementIntersectionId = -1;
    private int[] robberResourcesDiscard = new int[]{0, 0, 0, 0, 0};
    private int hexId;
    ArrayList<Integer> buildingsBuiltOnThisTurn = new ArrayList<>();
    private boolean movedRobber;

    /**
     * callback method--game's state has changed
     *
     * @param info the information (presumably containing the game's state)
     */
    @Override
    protected void receiveInfo(GameInfo info) {
        Log.i(TAG, "receiveInfo() of player " + this.playerNum + " called.");

        if (!(info instanceof CatanGameState)) return; // must do this check at start of method!

        //creates a new game state object
        CatanGameState gs = (CatanGameState) info;
        Log.d(TAG, "receiveInfo: game state current player: " + gs.getCurrentPlayerId() + " this.playerNum: " + this.playerNum);
        if (this.playerNum != gs.getCurrentPlayerId() && !gs.isRobberPhase()) {
            Log.w(TAG, "receiveInfo: not my turn and not the robber phase, returning playerNum=" + this.playerNum + " current player=" + gs.getCurrentPlayerId());
            return;
        }
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

        Random random = new Random();

        /*------------------------------------CPUs Setup Phase Actions-----------------------------------------*/

        // if it is the setup phase and the players turn
        if (gs.isSetupPhase() && this.playerNum == gs.getCurrentPlayerId()) {
            // Opening placements are restored from the saved rules state.
            int start = gs.getCurrentSetupSettlement();
            if (start < 0) {
                ArrayList<Integer> legal = new ArrayList<>();
                for (int i = 0; i < gs.getBoard().getBuildings().length; i++) {
                    if (gs.getBoard().validBuildingLocation(playerNum, true, i)) legal.add(i);
                }
                if (!legal.isEmpty()) game.sendAction(new CatanBuildSettlementAction(
                        this, true, playerNum, legal.get(random.nextInt(legal.size()))));
                return;
            }
            if (!gs.isSetupRoadBuiltThisTurn()) {
                ArrayList<Integer> legalRoadEnds = new ArrayList<>();
                for (Integer to : gs.getBoard().getIntersectionAdjacencyList().get(start)) {
                    if (gs.getBoard().validRoadPlacement(playerNum, true, start, to, start))
                        legalRoadEnds.add(to);
                }
                if (!legalRoadEnds.isEmpty()) game.sendAction(new CatanBuildRoadAction(
                        this, true, playerNum, start,
                        legalRoadEnds.get(random.nextInt(legalRoadEnds.size()))));
                return;
            }
            game.sendAction(new CatanEndTurnAction(this));
            return;
        } // setup phase if statement END

        /*------------------------------Setup Phase End------------------------------------------*/


        /*-------------------------------CPUs Roll Dice Action--------------------------------------*/
        if (!gs.isSetupPhase() && !gs.isActionPhase() && !gs.isRobberPhase()
                && gs.getCurrentPlayerId() == playerNum) {
            sleep(1000);
            Log.i(TAG, "receiveInfo: RollDiceAction by DumbComputerPlayer " + this.playerNum);
            game.sendAction(new CatanRollDiceAction(this));
            sleep(500);
            return;
        }

        // Easy is intentionally less strategic, but never attempts an
        // illegal road or loops on missing options.
        if (!gs.isSetupPhase() && gs.isActionPhase()
                && gs.getCurrentPlayerId() == playerNum && !gs.isRobberPhase()) {
            playEasyTurn(gs, random);
            return;
        }

        /*-------------------------------CPUs Robber Actions--------------------------------------*/

        //Carry out robber phase
        if (gs.isRobberPhase()) {
            if (!gs.getRobberPlayerListHasDiscarded()[playerNum]) {
                int[] remaining = gs.getPlayerList().get(playerNum).getResourceCards().clone();
                int sum = 0;
                for (int count : remaining) sum += count;
                int required = sum > 7 ? sum / 2 : 0;
                int[] discard = new int[5];
                for (int i = 0; i < required; i++) {
                    int held = 0;
                    for (int value : remaining) held += value;
                    if (held <= 0) return;
                    int target = random.nextInt(held);
                    for (int resource = 0; resource < 5; resource++) {
                        if (target < remaining[resource]) {
                            remaining[resource]--;
                            discard[resource]++;
                            break;
                        }
                        target -= remaining[resource];
                    }
                }
                game.sendAction(new CatanRobberDiscardAction(this, playerNum, discard));
                return;
            }
            if (gs.getCurrentPlayerId() != playerNum || !gs.allPlayersHaveDiscarded()) return;
            if (!gs.getHasMovedRobber()) {
                ArrayList<Integer> destinations = new ArrayList<>();
                for (int tile = 0; tile < gs.getBoard().getHexagons().size(); tile++)
                    if (tile != gs.getBoard().getRobber().getHexagonId()
                            && gs.getBoard().getHexagonFromId(tile).getResourceId() != 5)
                        destinations.add(tile);
                if (!destinations.isEmpty())
                    game.sendAction(new CatanRobberMoveAction(this, playerNum,
                            destinations.get(random.nextInt(destinations.size()))));
                return;
            }
            // Read the destination from saved state, not this CPU's former
            // transient hexId field. No victim is legal on an empty tile.
            ArrayList<Integer> eligible = new ArrayList<>();
            for (int p = 0; p < gs.getPlayerList().size(); p++) {
                if (gs.isRobberVictim(playerNum, p)) eligible.add(p);
            }
            int victim = eligible.isEmpty() ? -1 : eligible.get(random.nextInt(eligible.size()));
            game.sendAction(new CatanRobberStealAction(this, playerNum, victim));
            return;
        }

    }// receiveInfo() END

    /**
     * Weak but fair: randomly chooses from LEGAL, AFFORDABLE actions.
     * It may pass despite having an opportunity (unlike Normal/Hard), but
     * never submits impossible coordinates or keeps replaying failed moves.
     */
    private void playEasyTurn(CatanGameState gs, Random random) {
        Player me = gs.getPlayerList().get(playerNum);
        ArrayList<int[]> legal = new ArrayList<>();
        int settlementCount = 0, cityCount = 0, roadCount = 0;
        for (Building building : gs.getBoard().getBuildings()) {
            if (building == null || building.getOwnerId() != playerNum) continue;
            if (building instanceof Settlement) settlementCount++;
            if (building instanceof City) cityCount++;
        }
        for (Road road : gs.getBoard().getRoads())
            if (road.getOwnerId() == playerNum) roadCount++;
        if (cityCount < 4 && me.hasResourceBundle(City.resourceCost)) {
            for (int site = 0; site < gs.getBoard().getBuildings().length; site++)
                if (gs.getBoard().validCityLocation(playerNum, site))
                    legal.add(new int[]{0, site, -1});
        }
        if (settlementCount < 5 && me.hasResourceBundle(Settlement.resourceCost)) {
            for (int site = 0; site < gs.getBoard().getBuildings().length; site++)
                if (gs.getBoard().validBuildingLocation(playerNum, false, site))
                    legal.add(new int[]{1, site, -1});
        }
        if (roadCount < 15 && (gs.getFreeRoadsRemaining() > 0
                || me.hasResourceBundle(Road.resourceCost))) {
            for (int from = 0; from < gs.getBoard().getBuildings().length; from++)
                for (Integer to : gs.getBoard().getIntersectionAdjacencyList().get(from))
                    if (from < to && gs.getBoard().validRoadPlacement(playerNum, false, from, to))
                        legal.add(new int[]{2, from, to});
        }
        // Never waste Road Building's second free road by ending the turn.
        if (gs.getFreeRoadsRemaining() > 0) {
            ArrayList<int[]> freeRoads = new ArrayList<>();
            for (int[] move : legal) if (move[0] == 2) freeRoads.add(move);
            if (!freeRoads.isEmpty()) {
                int[] chosen = freeRoads.get(random.nextInt(freeRoads.size()));
                game.sendAction(new CatanBuildRoadAction(this, false, playerNum,
                        chosen[1], chosen[2]));
                return;
            }
            game.sendAction(new CatanEndTurnAction(this));
            return;
        }

        if (!gs.hasPlayedDevelopmentCardThisTurn() && random.nextInt(3) == 0) {
            ArrayList<Integer> playable = new ArrayList<>();
            for (Integer card : me.getPlayableDevCards()) {
                if (card == 0 || card == 3) playable.add(card);
                else if (card == 2) {
                    for (int r = 0; r < 5; r++)
                        if (gs.getBankResourceCount(r) > 0) { playable.add(card); break; }
                } else if (card == 4 && roadCount < 15
                        && CatanAiPlanner.bestRoad(gs, playerNum, false) != null) {
                    // A Road Building card is free: it must also work when
                    // the player cannot afford to buy ordinary roads.
                    playable.add(card);
                }
            }
            if (!playable.isEmpty()) {
                int card = playable.get(random.nextInt(playable.size()));
                if (card == 0) game.sendAction(new CatanUseKnightCardAction(this));
                else if (card == 3) {
                    game.sendAction(new CatanUseMonopolyCardAction(this, random.nextInt(5)));
                } else if (card == 2) {
                    ArrayList<Integer> available = new ArrayList<>();
                    for (int r = 0; r < 5; r++)
                        if (gs.getBankResourceCount(r) > 0) available.add(r);
                    int resource = available.get(random.nextInt(available.size()));
                    game.sendAction(new CatanUseYearOfPlentyCardAction(this, resource));
                } else if (card == 4) {
                    game.sendAction(new CatanUseRoadBuildingCardAction(this));
                }
                return;
            }
        }

        // Easy occasionally wastes a turn, but usually uses opportunities.
        if (!legal.isEmpty() && random.nextInt(5) != 0) {
            int[] move = legal.get(random.nextInt(legal.size()));
            if (move[0] == 0)
                game.sendAction(new CatanBuildCityAction(this, false, playerNum, move[1]));
            else if (move[0] == 1)
                game.sendAction(new CatanBuildSettlementAction(this, false, playerNum, move[1]));
            else
                game.sendAction(new CatanBuildRoadAction(this, false, playerNum,
                        move[1], move[2]));
            // One action at most per Easy turn keeps it approachable.
            // An accepted move precedes End Turn on the same game queue.
            game.sendAction(new CatanEndTurnAction(this));
            return;
        }
        // Development cards and simple bank exchanges prevent Easy from
        // passing indefinitely while holding resources it cannot use.
        if (me.hasResourceBundle(DevelopmentCard.resourceCost)
                && gs.hasDevelopmentCardsAvailable() && random.nextInt(3) == 0) {
            game.sendAction(new CatanBuyDevCardAction(this));
            game.sendAction(new CatanEndTurnAction(this));
            return;
        }
        int[] hand = me.getResourceCards();
        int spare = -1, missing = -1;
        for (int resource = 0; resource < 5; resource++) {
            if (hand[resource] >= 4 && (spare < 0 || hand[resource] > hand[spare]))
                spare = resource;
            if (gs.getBankResourceCount(resource) > 0
                    && (missing < 0 || hand[resource] < hand[missing]))
                missing = resource;
        }
        if (legal.isEmpty() && spare >= 0 && missing >= 0 && spare != missing
                && random.nextInt(2) == 0) {
            game.sendAction(new CatanTradeWithBankAction(this, spare, missing));
        }
        game.sendAction(new CatanEndTurnAction(this));
    }

    /**
     * Constructor for the computer player
     *
     * @param name name of the player to be used on the soreboard anf whenever the player is referenced
     */
    CatanDumbComputerPlayer(String name) {
        super(name);
    }

    /**
     * The method that attempts to move the robber
     *
     * @param hexId the ID of the tile to move attempt moving the robber to
     * @param gs    the current game state
     * @return either true or false depending if moving the robber was valid
     */
    private boolean tryMoveRobber(int hexId, CatanGameState gs) {
        Log.d(TAG, "tryMoveRobber() called with: hexId = [" + hexId + "], gs = [" + gs + "]");

        //Robber already moved
        if (gs.getHasMovedRobber()) {
            Log.d(TAG, "tryMoveRobber() returned: " + false + " because the robber has already been moved.");
            return false;
        }

        //Hex not selected
        if (hexId == -1) {
            Log.d(TAG, "tryMoveRobber: Invalid hex ID from CPU");
            return false;
        }

        //HexId not different from Robber's current position
        if (hexId == gs.getBoard().getRobber().getHexagonId()) {
            Log.d(TAG, "tryMoveRobber: Same hexId as robber");
            return false;
        }

        //Robber cannot be moved to desert tile
        if (gs.getBoard().getHexagons().get(hexId).getResourceId() == 5) {
            Log.d(TAG, "tryMoveRobber: Desert tile selected; invalid.");
            return false;
        }

        ArrayList<Integer> intersections = gs.getBoard().getHexToIntIdMap().get(hexId);

        //Make sure hex has enemy buildings adjacent to it; if not, then it's not a valid hex
        for (Integer intersection : intersections) {
            if (gs.getBoard().getBuildings()[intersection] != null) {
                if (gs.getBoard().getBuildings()[intersection].getOwnerId() != playerNum) {
                    CatanRobberMoveAction action = new CatanRobberMoveAction(this, playerNum, hexId);
                    game.sendAction(action);
                    gs.setHasMovedRobber(true);
                    return true;
                }
            }
        }
        Log.d(TAG, "tryMoveRobber: returned " + false);
        return false;
    }

    /**
     * looks through the list of buildings that have been built then find which ones are own by
     * the player add adds them to an arrayList
     *
     * @param gs the current game state
     * @return arraylist of Road objects that the player owns
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
     * finds the intersections that the roads the player owns
     *
     * @param playerRoads arrayList of Road Objects that the player owns
     * @return and arrayList of Integers of the intersections that the roads the player owns are on
     */
    private ArrayList<Integer> getPlayerRoadIntersection(ArrayList<Road> playerRoads) {
        ArrayList<Integer> intersections = new ArrayList<>();
        for (int n = 0; n < playerRoads.size(); n++) {
            intersections.add(playerRoads.get(n).getIntersectionAId());
            intersections.add(playerRoads.get(n).getIntersectionBId());
        }
        ArrayList<Integer> noRepeatIntersections = new ArrayList<>();
        for (int n = 0; n < intersections.size(); n++) {
            for (int j = n + 1; j < intersections.size(); j++) {
                if (!Objects.equals(intersections.get(n), intersections.get(j))) {
                    noRepeatIntersections.add(n);
                }
            }
            Log.d(TAG, "With repeat Intersections: " + intersections.toString());
            Log.d(TAG, "No repeat Intersections: " + noRepeatIntersections.toString());
        }
        return intersections;
    }
} // CatanDumbComputerPlayer class END
