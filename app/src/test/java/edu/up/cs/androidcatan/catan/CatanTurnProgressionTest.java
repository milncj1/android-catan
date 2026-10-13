package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;

import static org.junit.Assert.*;

/** Core engine actions without Android GUI or CPU threads. */
public class CatanTurnProgressionTest {
    private static CatanGameState rulesState(CatanLocalGame game) throws Exception {
        java.lang.reflect.Field field = CatanLocalGame.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CatanGameState) field.get(game);
    }

    @Test public void eightOpeningPlacementsReturnPlayToHuman() throws Exception {
        CatanLocalGame game = new CatanLocalGame();
        CatanGameState state = rulesState(game);
        Board board = state.getBoard();
        int[] expectedOrder = {0, 1, 2, 3, 3, 2, 1, 0};

        for (int step = 0; step < expectedOrder.length; step++) {
            int player = expectedOrder[step];
            assertTrue("Opening turn " + step + " is still setup", state.isSetupPhase());
            assertEquals("Opening player order", player, state.getCurrentPlayerId());
            // A human only places one settlement in the first round and
            // comes back for another at the end of the reverse round.
            if (step == 7) {
                assertEquals("Human gets second opening placement", 0, player);
                assertEquals("Only first human settlement exists so far",
                        1, state.getPlayerList().get(0).getVictoryPoints());
            }
            int selected = -1;
            int roadEnd = -1;
            for (int intersection = 0; intersection < board.getBuildings().length; intersection++) {
                if (!board.validBuildingLocation(player, true, intersection)) continue;
                for (Integer next : board.getIntersectionAdjacencyList().get(intersection)) {
                    // At this point the new settlement does not yet exist.
                    if (board.getRoadMatrix()[intersection][next] != null
                            && board.getRoadMatrix()[intersection][next].getOwnerId() == -1
                            && board.getRoadsAtIntersection(intersection).size() < 3
                            && board.getRoadsAtIntersection(next).size() < 3) {
                        selected = intersection;
                        roadEnd = next;
                        break;
                    }
                }
                if (selected >= 0) break;
            }
            assertTrue("Legal settlement exists on placement " + step, selected >= 0);
            assertTrue(game.makeMove(new CatanBuildSettlementAction(null, true, player, selected)));
            assertEquals(selected, state.getCurrentSetupSettlement());
            assertFalse(state.isSetupRoadBuiltThisTurn());
            assertTrue("Road for new settlement",
                    game.makeMove(new CatanBuildRoadAction(null, true, player, selected, roadEnd)));
            assertTrue(state.isSetupRoadBuiltThisTurn());
            if (step == 0) {
                assertFalse("Human cannot immediately place both opening settlements",
                        game.makeMove(new CatanBuildSettlementAction(null, true, 0, selected)));
            }
            assertTrue(game.makeMove(new CatanEndTurnAction(null)));
        }

        assertFalse("Opening phase complete", state.isSetupPhase());
        assertEquals("Human starts first normal turn", 0, state.getCurrentPlayerId());
        assertEquals(8, board.getRoads().size());
        int buildings = 0;
        for (Building building : board.getBuildings()) if (building != null) buildings++;
        assertEquals(8, buildings);
        for (Player player : state.getPlayerList()) assertEquals(2, player.getVictoryPoints());
    }

    @Test public void regularTurnCannotEndBeforeRolling() throws Exception {
        CatanLocalGame game = new CatanLocalGame();
        CatanGameState state = rulesState(game);
        state.setSetupPhase(false);
        assertFalse(game.makeMove(new CatanEndTurnAction(null)));
        state.setActionPhase(true);
        assertTrue(game.makeMove(new CatanEndTurnAction(null)));
        assertEquals(1, state.getCurrentPlayerId());
        assertFalse(state.isActionPhase());
    }
}
