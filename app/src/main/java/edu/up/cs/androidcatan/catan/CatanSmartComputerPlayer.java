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
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
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

    CatanSmartComputerPlayer(String name) {
        super(name);
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
        if (!(info instanceof CatanGameState)) return;
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
                    double value = CatanStrategy.scoreIntersection(gs, vertex, playerNum)
                            + random.nextDouble() * 0.1;
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
                    double score = CatanStrategy.scoreRoadDestination(gs, to, playerNum)
                            + random.nextDouble() * 0.1;
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
            sleep(300);
            game.sendAction(new CatanRollDiceAction(this));
            sleep(300);
            return;
        }
        /*-------------------------------CPUs Robber Actions--------------------------------------*/

        //Robber phase accessible for any turn, due to them possibly needing to discard
        if (gs.isRobberPhase()) {
            Log.i(TAG, "receiveInfo: Computer has reached the Robber Phase");
            sleep(500);
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
                    sleep(250);
                    game.sendAction(new CatanRobberMoveAction(this, playerNum, hexId));
                    return;

                }//End of Move Robber Phase

                /*----------------Steal Resource Phase--------------*/

                //11. Now Steal from the selected intersection
                sleep(250);
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
                sleep(500);
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

            sleep(200);

            // A played Road Building card grants two free road placements.
            // Place them before considering any paid action or ending the turn.
            if (gs.getFreeRoadsRemaining() > 0) {
                if (buildBestRoad(gs)) return;
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
                    double score = CatanStrategy.scoreIntersection(gs, candidate, this.playerNum);
                    if (score > bestSiteScore) {
                        bestSiteScore = score;
                        bestSite = candidate;
                    }
                }
                if (bestSite >= 0) {
                    game.sendAction(new CatanBuildSettlementAction(this, false, this.playerNum, bestSite));
                    game.sendAction(new CatanEndTurnAction(this));
                    sleep(350);
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
                    double score = CatanStrategy.scoreIntersection(gs, n, this.playerNum);
                    if (score > bestCityScore) {
                        bestCityScore = score;
                        bestCitySite = n;
                    }
                }
                if (bestCitySite >= 0) {
                    game.sendAction(new CatanBuildCityAction(this, false, this.playerNum, bestCitySite));
                    game.sendAction(new CatanEndTurnAction(this));
                    sleep(350);
                    return;
                }
            }

            /******Looks to buy a dev card*******/
            if (gs.getPlayerList().get(this.playerNum).hasResourceBundle(DevelopmentCard.resourceCost) && gs.hasDevelopmentCardsAvailable()) {
                Log.d(TAG, "receiveInfo: Player " + this.playerNum + " purchased dev card");
                game.sendAction(new CatanBuyDevCardAction(this));
                Log.d(TAG, "receiveInfo: CatanBuyDevCardAction sent");
                game.sendAction(new CatanEndTurnAction(this));
                sleep(350);
                Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                return;
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
                        game.sendAction(new CatanUseKnightCardAction(this));
                        Log.d(TAG, "receiveInfo: CatanUseKnightCardAction sent");
                        game.sendAction(new CatanEndTurnAction(this));
                        sleep(350);
                        Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                        return;
                    }

                    //if they have a year of plenty card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 2) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using year of plenty card");
                        game.sendAction(new CatanUseYearOfPlentyCardAction(this, playerLeastResource(gs))); //change chosenResource
                        Log.d(TAG, "receiveInfo: Used year of plenty card");
                        game.sendAction(new CatanEndTurnAction(this));
                        sleep(350);
                        Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                        return;
                    }

                    //if they have a monopoly card
                    if (gs.getPlayerList().get(this.playerNum).getPlayableDevCards().get(n) == 3) {
                        Log.d(TAG, "receiveInfo: Player " + this.playerNum + " using monopoly card");
                        game.sendAction(new CatanUseMonopolyCardAction(this, playerLeastResource(gs))); //change chosenResource
                        game.sendAction(new CatanEndTurnAction(this));
                        sleep(350);
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

            /*****Looks to trade so they can potentially build a road******/
            if (brickCount >= 5 && lumberCount == 0) {
                Log.d(TAG, "receiveInfo: Brick count: " + brickCount + " and lumberCount: " + lumberCount + " caused a trade");
                game.sendAction(new CatanTradeWithBankAction(this, 0, 2));
                game.sendAction(new CatanEndTurnAction(this));
                sleep(350);
                return;
            }
            if (lumberCount >= 5 && brickCount == 0) {
                Log.d(TAG, "receiveInfo: Lumber count: " + lumberCount + " and brickCount: " + brickCount + " caused a trade");
                game.sendAction(new CatanTradeWithBankAction(this, 2, 0));
                game.sendAction(new CatanEndTurnAction(this));
                sleep(350);
                return;
            }

            if (grainCount >= 4) {
                if (!gs.getPlayerList().get(this.playerNum).hasResourceBundle(Road.resourceCost) && !gs.getPlayerList().get(this.playerNum).hasResourceBundle(Settlement.resourceCost) && !gs.getPlayerList().get(this.playerNum).hasResourceBundle(City.resourceCost)) {
                    Log.d(TAG, "receiveInfo: Trade happening: player + " + this.playerNum + "traded grain for brick");
                    game.sendAction(new CatanTradeWithBankAction(this, 1, playerLeastResource(gs)));
                    Log.d(TAG, "receiveInfo: CatanTradeWithBankAction sent");
                    game.sendAction(new CatanEndTurnAction(this));
                    sleep(350);
                    Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                    return;
                }
            }

            if (woolCount >= 4) {
                if (!gs.getPlayerList().get(this.playerNum).hasResourceBundle(Road.resourceCost) && !gs.getPlayerList().get(this.playerNum).hasResourceBundle(Settlement.resourceCost) && !gs.getPlayerList().get(this.playerNum).hasResourceBundle(City.resourceCost)) {
                    Log.d(TAG, "receiveInfo: Trade happening: wool for brick");
                    game.sendAction(new CatanTradeWithBankAction(this, 4, playerLeastResource(gs)));
                    Log.d(TAG, "receiveInfo: CatanTradeWithBankAction sent");
                    game.sendAction(new CatanEndTurnAction(this));
                    sleep(350);
                    Log.d(TAG, "receiveInfo: CatanEndTurnAction sent");
                    return;
                }
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
            sleep(350);
            return;
            //not setup phase if statement END
        }
    }// receiveInfo() END

    /**
     * Choose a legal road with the highest future expansion value.
     * Only place the road here; the next game-state callback chooses whether
     * to place another free road or finish the turn.
     */
    private boolean buildBestRoad(CatanGameState gs) {
        Random random = new Random();
        int bestFrom = -1, bestTo = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Integer from : getPlayerRoadIntersection(getPlayerRoads(gs))) {
            for (Integer to : gs.getBoard().getIntersectionAdjacencyList().get(from)) {
                if (!gs.getBoard().validRoadPlacement(playerNum, false, from, to)) continue;
                double score = CatanStrategy.scoreRoadDestination(gs, to, playerNum)
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


