package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.lang.reflect.Field;
import edu.up.cs.androidcatan.catan.actions.*;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.buildings.*;
import static org.junit.Assert.*;

/**
 * Headless, deterministic-policy ENGINE stress (not a substitute for running
 * actual Android AI handler threads on a device). Test real legal GameActions
 * with 2/4/5/7 player configurations and every island footprint.
 */
public class CatanBuild032TurnStressTest {
    private static CatanGameState state(CatanLocalGame game) throws Exception {
        Field f = CatanLocalGame.class.getDeclaredField("state");
        f.setAccessible(true);
        return (CatanGameState) f.get(game);
    }

    @Test public void fullSetupAndDozensOfRoundsStayLegalOnAllIslands() throws Exception {
        int[] playerCounts = {2,4,5,7};
        CatanBoardTopology.Island[] islands = {
                CatanBoardTopology.Island.CLASSIC,
                CatanBoardTopology.Island.CLASSIC,
                CatanBoardTopology.Island.EXPANDED,
                CatanBoardTopology.Island.GRAND
        };
        for (int match = 0; match < playerCounts.length; match++) {
            int players = playerCounts[match];
            CatanLocalGame engine = new CatanLocalGame(players, islands[match]);
            CatanGameState game = state(engine);
            Board board = game.getBoard();

            // Use the actual authoritative snake setup action sequence.
            for (int turn = 0; turn < players * 2; turn++) {
                int current = game.getCurrentPlayerId();
                int vertex = -1, end = -1;
                for (int v = 0; v < board.getBuildings().length && vertex == -1; v++) {
                    if (!board.validBuildingLocation(current, true, v)) continue;
                    for (int next : board.getIntersectionAdjacencyList().get(v)) {
                        if (board.getRoadMatrix()[v][next] != null
                                && board.getRoadMatrix()[v][next].getOwnerId() == -1) {
                            vertex = v;
                            end = next;
                            break;
                        }
                    }
                }
                assertTrue("Opening settlement exists for " + players
                        + " players at step " + turn, vertex >= 0);
                assertTrue(engine.makeMove(new CatanBuildSettlementAction(
                        null, true, current, vertex)));
                assertTrue("Opening road should touch placed settlement",
                        engine.makeMove(new CatanBuildRoadAction(null, true,
                                current, vertex, end)));
                assertTrue(engine.makeMove(new CatanEndTurnAction(null)));
            }
            assertFalse(game.isSetupPhase());

            // Real dice, finite bank, robber and incremental build actions.
            // We never give the computers bonus resources or peek into hands
            // to transfer anything between them.
            for (int turn = 0; turn < 30 * players; turn++) {
                int actor = game.getCurrentPlayerId();
                assertTrue("Roll on turn " + turn, engine.makeMove(
                        new CatanRollDiceAction(null)));
                if (game.isRobberPhase()) {
                    for (int p = 0; p < players; p++) {
                        if (game.getRobberPlayerListHasDiscarded()[p]) continue;
                        int[] hand = game.getPlayerList().get(p).getResourceCards();
                        int sum = 0;
                        for (int qty : hand) sum += qty;
                        int mustDiscard = sum > 7 ? sum / 2 : 0;
                        int[] selected = new int[5];
                        for (int r = 0; r < 5; r++) {
                            selected[r] = Math.min(hand[r], mustDiscard);
                            mustDiscard -= selected[r];
                        }
                        assertEquals(0, mustDiscard);
                        assertTrue("All CPU discards must be legal",
                                engine.makeMove(new CatanRobberDiscardAction(
                                        null, p, selected)));
                    }
                    assertTrue(game.allPlayersHaveDiscarded());
                    int from = game.getBoard().getRobber().getHexagonId();
                    int next = (from + 1) % board.getHexagons().size();
                    assertTrue(engine.makeMove(new CatanRobberMoveAction(null, actor, next)));
                    int victim = -1;
                    for (int p = 0; p < players; p++)
                        if (game.isRobberVictim(actor, p)) { victim = p; break; }
                    assertTrue(engine.makeMove(new CatanRobberStealAction(null, actor, victim)));
                }

                // Each simulated player follows one legal, affordable action
                // per turn; this tests the action engine, not AI win rates.
                Player player = game.getCurrentPlayer();
                int site = CatanAiPlanner.bestNewSettlement(game, actor, true);
                int city = CatanAiPlanner.bestCity(game, actor, true);
                int roads = 0, cities = 0, settlements = 0;
                for (Road road : board.getRoads())
                    if (road.getOwnerId() == actor) roads++;
                for (Building b : board.getBuildings()) {
                    if (b == null || b.getOwnerId() != actor) continue;
                    if (b instanceof City) cities++;
                    else if (b instanceof Settlement) settlements++;
                }
                if (site >= 0 && settlements < 5
                        && player.hasResourceBundle(Settlement.resourceCost))
                    assertTrue(engine.makeMove(new CatanBuildSettlementAction(
                            null, false, actor, site)));
                else if (city >= 0 && cities < 4
                        && player.hasResourceBundle(City.resourceCost))
                    assertTrue(engine.makeMove(new CatanBuildCityAction(
                            null, false, actor, city)));
                else if (roads < 15 && player.hasResourceBundle(Road.resourceCost)) {
                    int[] road = CatanAiPlanner.bestRoad(game, actor, true);
                    if (road != null) assertTrue(engine.makeMove(
                            new CatanBuildRoadAction(null, false, actor,
                                    road[0], road[1])));
                }
                assertTrue(engine.makeMove(new CatanEndTurnAction(null)));
                for (int resource = 0; resource < 5; resource++)
                    assertTrue("Finite bank cannot become negative",
                            game.getBankResourceCount(resource) >= 0);
                for (Player p : game.getPlayerList())
                    for (int held : p.getResourceCards())
                        assertTrue("No resource count may become negative", held >= 0);
            }
        }
    }
}
