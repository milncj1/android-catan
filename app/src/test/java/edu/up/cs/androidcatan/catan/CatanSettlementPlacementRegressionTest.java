package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

import static org.junit.Assert.*;

/** Regression coverage for confusing settlement placement and false success. */
public class CatanSettlementPlacementRegressionTest {
    private static CatanGameState engineState(CatanLocalGame engine) throws Exception {
        java.lang.reflect.Field f = CatanLocalGame.class.getDeclaredField("state");
        f.setAccessible(true);
        return (CatanGameState) f.get(engine);
    }

    private static int threeWayJunction(Board board) {
        for (int i = 0; i < board.getBuildings().length; i++) {
            if (board.getIntersectionAdjacencyList().get(i).size() == 3) return i;
        }
        throw new AssertionError("Expected 3-road junction on standard island");
    }

    @Test public void aSettlementIsLegalAtThreeOwnedRoadsMeeting() {
        Board board = new Board();
        int junction = threeWayJunction(board);
        for (Integer neighbour : board.getIntersectionAdjacencyList().get(junction))
            board.addRoad(0, junction, neighbour);

        assertEquals(3, board.getRoadsAtIntersection(junction).size());
        assertTrue("Three connected roads do not forbid a settlement",
                board.validBuildingLocation(0, false, junction));
        assertTrue(board.addBuilding(junction, new Settlement(0)));
        assertFalse("Cannot place a second settlement over one already built",
                board.validBuildingLocation(0, false, junction));
    }

    @Test public void unpaidBuildIsRejectedWithoutChangingBoardOrScore() throws Exception {
        CatanGameState start = new CatanGameState();
        start.setSetupPhase(false);
        start.setActionPhase(true);
        Board board = start.getBoard();
        int junction = threeWayJunction(board);
        int neighbour = board.getIntersectionAdjacencyList().get(junction).get(0);
        board.addRoad(0, junction, neighbour);

        CatanLocalGame game = new CatanLocalGame(start);
        CatanGameState actual = engineState(game);
        int originalScore = actual.getCurrentPlayer().getVictoryPoints();
        CatanBuildSettlementAction action =
                new CatanBuildSettlementAction(null, false, 0, junction);
        assertFalse("No resources, no settlement", game.makeMove(action));
        assertNull(actual.getBoard().getBuildingAtIntersection(junction));
        assertEquals(originalScore, actual.getCurrentPlayer().getVictoryPoints());

        for (int resource : new int[]{0, 1, 2, 4})
            actual.getCurrentPlayer().addResourceCard(resource, 1);
        assertTrue("Exact standard costs should allow building", game.makeMove(action));
        assertNotNull(actual.getBoard().getBuildingAtIntersection(junction));
        assertEquals(originalScore + 1, actual.getCurrentPlayer().getVictoryPoints());
        for (int resource : new int[]{0, 1, 2, 4})
            assertEquals(0, actual.getCurrentPlayer().getResourceCards()[resource]);
    }

    @Test public void immediatelyAdjacentJunctionStillFailsDistanceRule() {
        Board board = new Board();
        int junction = threeWayJunction(board);
        int neighbour = board.getIntersectionAdjacencyList().get(junction).get(0);
        board.addBuilding(neighbour, new Settlement(1));
        board.addRoad(0, junction, neighbour);
        assertFalse("Settlement distance rule survives three-way junction",
                board.validBuildingLocation(0, false, junction));
    }
}
