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

        /*----------------------------------Build Actions------------------------------------------*/

        //Check if player can actually build
        if (!gs.isSetupPhase() && gs.isActionPhase() && gs.getCurrentPlayerId() == this.playerNum && !gs.isRobberPhase()) {
            sleep(1000);
            Building building;
            int action = random.nextInt(4);
            if (action == 0) //build  City
            {
                //Check if player has resources to build
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(City.resourceCost)) {
                    Log.d(TAG, "receiveInfo: Valid amount of resources to build city");
                    for (int n = 0; n < gs.getBoard().getBuildings().length; n++) {

                        //Check if location is valid to build a city on
                        if (gs.getBoard().getBuildings()[n] == null) {
                            Log.d(TAG, "receiveInfo: Nothing at this location on board");
                        } else if (gs.getBoard().getBuildings()[n].getOwnerId() == this.playerNum) {
                            Log.d(TAG, "receiveInfo: valid owner id");
                            building = gs.getBoard().getBuildings()[n];
                            if (building instanceof Settlement) {
                                game.sendAction(new CatanBuildCityAction(this, false, this.playerNum, n));
                                Log.d(TAG, "receiveInfo: CatanBuildCityAction sent");
                                game.sendAction(new CatanEndTurnAction(this));
                                Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                                return;
                            }
                        }
                    }
                }
            } else if (action == 1) //build a settlement
            {
                Log.d(TAG, "Dumb AI randomly tried to build a settlement");
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Settlement.resourceCost)) {
                    for (int n = 0; n < getPlayerRoadIntersection(getPlayerRoads(gs)).size(); n++) {
                        //cycling through the amount, not the proper value at the intersection
                        if (gs.getBoard().validBuildingLocation(this.playerNum, false, getPlayerRoadIntersection(getPlayerRoads(gs)).get(n))) {
                            Log.d(TAG, "receiveInfo: validBuildingLocation for a settlement");
                            game.sendAction(new CatanBuildSettlementAction(this, false, this.playerNum, getPlayerRoadIntersection(getPlayerRoads(gs)).get(n)));
                            Log.d(TAG, "receiveInfo: CatanBuildSettlementAction sent");
                            game.sendAction(new CatanEndTurnAction(this));
                            Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                            return;
                        }
                    }
                }

            } else if (action == 2)// build a Road
            {
                Log.d(TAG, "Dumb AI randomly tried to build a road");
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Road.resourceCost)) {

                    // get road endpoints for players roads
                    ArrayList<Integer> individualRoads = getPlayerRoadIntersection(getPlayerRoads(gs));
                    int randIntersection = random.nextInt(individualRoads.size());

                    // get random intersection from those road intersections
                    int roadCoordinate = individualRoads.get(randIntersection);

                    // get all adjacent intersections
                    ArrayList<Integer> intersectionsToChooseFrom = gs.getBoard().getIntersectionAdjacencyList().get(roadCoordinate);
                    Log.d(TAG, "IntersectionsToChooseFrom for coordinate: " + roadCoordinate + " for the following cords: " + intersectionsToChooseFrom.toString());

                    //int randomRoadIntersection = random.nextInt(intersectionsToChooseFrom.size());
                    for (int n = 0; n < intersectionsToChooseFrom.size(); n++) {
                        if (gs.getBoard().validRoadPlacement(this.playerNum, false, roadCoordinate, intersectionsToChooseFrom.get(n))) {
                            game.sendAction(new CatanBuildRoadAction(this, false, this.playerNum, roadCoordinate, intersectionsToChooseFrom.get(n)));
                            //was random road intersection
                            Log.d(TAG, "receiveInfo: CatanBuildRoadAction sent");

                            game.sendAction(new CatanEndTurnAction(this));

                            Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                            return;
                        }
                    }
                    Log.d(TAG, "receiveInfo: Problem with building a road");
                    game.sendAction(new CatanEndTurnAction(this));
                    return;
                }
            } else {
                Log.d(TAG, "Dumb AI randomly chose to do nothing");
            }
        }


        /*------------------------------Build Actions End------------------------------------------*/

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
                int current = gs.getBoard().getRobber().getHexagonId();
                int size = gs.getBoard().getHexagons().size();
                if (size < 2) return;
                int dest = random.nextInt(size - 1);
                if (dest >= current) dest++;
                game.sendAction(new CatanRobberMoveAction(this, playerNum, dest));
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

        /*----------------------------------Build Actions------------------------------------------*/

        //Check if it is valid to try and take action to build
        if (!gs.isSetupPhase() && gs.isActionPhase() && gs.getCurrentPlayerId() == this.playerNum && !gs.isRobberPhase()) {
            sleep(1000);
            Building building;
            int action = random.nextInt(4);

            //Build a city
            if (action == 0) {
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(City.resourceCost)) {
                    Log.d(TAG, "receiveInfo: Valid amount of resources to build city");
                    for (int n = 0; n < gs.getBoard().getBuildings().length; n++) {
                        if (gs.getBoard().getBuildings()[n] == null) {
                            Log.d(TAG, "receiveInfo: Nothing at this location on board");
                        } else if (gs.getBoard().getBuildings()[n].getOwnerId() == this.playerNum) {
                            Log.d(TAG, "receiveInfo: valid owner id");
                            building = gs.getBoard().getBuildings()[n];
                            if (building instanceof Settlement) {
                                game.sendAction(new CatanBuildCityAction(this, false, this.playerNum, n));
                                Log.d(TAG, "receiveInfo: CatanBuildCityAction sent");
                                return;
                            }
                        }
                    }
                }
            } else if (action == 1) { //Build a settlement
                Log.d(TAG, "Dumb AI randomly tried to build a settlement");
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Settlement.resourceCost)) {
                    Log.d(TAG, "receiveInfo: Valid amount of resources to building");
                    for (int n = 0; n < getPlayerRoadIntersection(getPlayerRoads(gs)).size(); n++) {
                        if (gs.getBoard().validBuildingLocation(this.playerNum, false, n)) {
                            Log.d(TAG, "receiveInfo: validBuildingLocation for a settlement");
                            game.sendAction(new CatanBuildSettlementAction(this, false, this.playerNum, n));
                            Log.d(TAG, "receiveInfo: CatanBuildSettlementAction sent");
                        }
                    }
                }

            } else if (action == 2) { //Build a road
                Log.d(TAG, "Dumb AI randomly tried to build a road");
                if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(Road.resourceCost)) {

                    // get road endpoints for players roads
                    ArrayList<Integer> individualRoads = getPlayerRoadIntersection(getPlayerRoads(gs));
                    int randIntersection = random.nextInt(individualRoads.size());

                    // get random intersection from those road intersections
                    int roadCoordinate = individualRoads.get(randIntersection);

                    // get all adjacent intersections
                    ArrayList<Integer> intersectionsToChooseFrom = gs.getBoard().getIntersectionAdjacencyList().get(roadCoordinate);

                    int randomRoadIntersection = random.nextInt(intersectionsToChooseFrom.size());
                    for (int n = 0; n < intersectionsToChooseFrom.size(); n++) {
                        if (gs.getBoard().validRoadPlacement(this.playerNum, false, roadCoordinate, intersectionsToChooseFrom.get(n))) {
                            game.sendAction(new CatanBuildRoadAction(this, false, this.playerNum, roadCoordinate, intersectionsToChooseFrom.get(randomRoadIntersection)));
                            Log.d(TAG, "receiveInfo: CatanBuildRoadAction sent");
                            return;
                        }
                    }
                    Log.d(TAG, "receiveInfo: Problem with building a road");
                }
            } else { //Do nothing and end turn
                Log.d(TAG, "Dumb AI randomly chose to do nothing");
                game.sendAction(new CatanEndTurnAction(this));
                Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                return;
            }
        }

        /*------------------------------Build Actions End------------------------------------------*/


        /* ----------------------------------- CPUs Normal Action Phase ------------------------------------ */
        //End turn if action taken
        if (!gs.isRobberPhase() && this.playerNum == gs.getCurrentPlayerId()) {
            Log.e(TAG, "receiveInfo: returning a CatanEndTurnAction because it is not the robber phase and it is my turn playerId=" + playerNum);
            game.sendAction(new CatanEndTurnAction(this));
            return;
        }
    }// receiveInfo() END

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
