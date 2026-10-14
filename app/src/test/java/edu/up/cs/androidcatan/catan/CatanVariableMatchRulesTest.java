package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;

import static org.junit.Assert.*;

/** Complete snake opening on configurable maps and opponent counts. */
public class CatanVariableMatchRulesTest {
    private static CatanGameState state(CatanLocalGame game) throws Exception {
        java.lang.reflect.Field field = CatanLocalGame.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CatanGameState) field.get(game);
    }

    @Test public void twoPlayerDuelCompletesBothPlacements() throws Exception {
        check(2, CatanBoardTopology.Island.CLASSIC, 19, 9);
    }

    @Test public void threePlayerClassicCompletesBothPlacements() throws Exception {
        check(3, CatanBoardTopology.Island.CLASSIC, 19, 9);
    }

    @Test public void fourPlayerExpandedLeavesMoreSpace() throws Exception {
        check(4, CatanBoardTopology.Island.EXPANDED, 30, 11);
    }

    @Test public void sixPlayerExpandedCompletesTwelvePlacements() throws Exception {
        check(6, CatanBoardTopology.Island.EXPANDED, 30, 11);
    }

    @Test public void sevenPlayerGrandCompletesFourteenPlacements() throws Exception {
        check(7, CatanBoardTopology.Island.GRAND, 37, 13);
    }

    private void check(int totalPlayers, CatanBoardTopology.Island island,
                       int tiles, int expectedPorts) throws Exception {
        CatanLocalGame game = new CatanLocalGame(totalPlayers, island);
        CatanGameState state = state(game);
        Board board = state.getBoard();
        assertEquals(totalPlayers, state.getPlayerList().size());
        assertEquals(tiles, board.getHexagons().size());
        assertEquals(expectedPorts, board.getPortList().size());
        assertEquals(totalPlayers * 2, state.getOpeningPlacementCount());
        assertEquals(0, state.getOpeningPlayer(0));
        assertEquals(totalPlayers - 1, state.getOpeningPlayer(totalPlayers - 1));
        assertEquals(totalPlayers - 1, state.getOpeningPlayer(totalPlayers));
        assertEquals(0, state.getOpeningPlayer(totalPlayers * 2 - 1));

        for (int step = 0; step < state.getOpeningPlacementCount(); step++) {
            int player = state.getOpeningPlayer(step);
            assertTrue("Still in opening phase at step " + step, state.isSetupPhase());
            assertEquals(player, state.getCurrentPlayerId());
            int from = -1, to = -1;
            for (int candidate = 0; candidate < board.getBuildings().length; candidate++) {
                if (!board.validBuildingLocation(player, true, candidate)) continue;
                for (Integer next : board.getIntersectionAdjacencyList().get(candidate)) {
                    if (board.getRoadMatrix()[candidate][next].getOwnerId() == -1) {
                        from = candidate;
                        to = next;
                        break;
                    }
                }
                if (from >= 0) break;
            }
            assertTrue("Settlement space exists on step " + step, from >= 0);
            assertTrue("Accept free opening settlement " + step,
                    game.makeMove(new CatanBuildSettlementAction(null, true, player, from)));
            assertTrue("Accept attached free opening road " + step,
                    game.makeMove(new CatanBuildRoadAction(null, true, player, from, to)));
            assertTrue("Advance snake order after step " + step,
                    game.makeMove(new CatanEndTurnAction(null)));
        }
        assertFalse(state.isSetupPhase());
        assertEquals(0, state.getCurrentPlayerId());
        assertEquals(totalPlayers * 2, board.getRoads().size());
        int buildings = 0;
        for (Building b : board.getBuildings()) if (b != null) buildings++;
        assertEquals(totalPlayers * 2, buildings);
        for (Player player : state.getPlayerList())
            assertEquals("Every participant owns two initial settlements",
                    2, player.getVictoryPoints());

        CatanGameState restored = new CatanGameState(state);
        assertEquals(totalPlayers, restored.getPlayerList().size());
        assertEquals(tiles, restored.getBoard().getHexagons().size());
        assertEquals(totalPlayers * 2, restored.getBoard().getRoads().size());
    }
}
