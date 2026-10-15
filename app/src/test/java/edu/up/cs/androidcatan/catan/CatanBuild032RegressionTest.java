package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Random;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberMoveAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberStealAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import static org.junit.Assert.*;

/** Build 32: exercise the real engine rather than changing AI/game rules. */
public class CatanBuild032RegressionTest {
    private static CatanGameState actualState(CatanLocalGame game) throws Exception {
        Field field = CatanLocalGame.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CatanGameState) field.get(game);
    }

    @Test public void knightMustFinishRobberBeforeCpuCanEndTurn() throws Exception {
        CatanGameState starting = new CatanGameState();
        starting.setSetupPhase(false);
        starting.setActionPhase(true);
        starting.setCurrentPlayerId(1);
        starting.getPlayerList().get(1).getDevelopmentCards().add(0);
        CatanLocalGame game = new CatanLocalGame(starting);
        CatanGameState now = actualState(game);

        assertTrue(game.makeMove(new CatanUseKnightCardAction(null)));
        assertTrue(now.isRobberPhase());
        assertTrue(now.allPlayersHaveDiscarded());
        assertFalse("End Turn must be rejected until robber is resolved",
                game.makeMove(new CatanEndTurnAction(null)));

        int destination = now.getBoard().getRobber().getHexagonId() == 0 ? 1 : 0;
        assertTrue(game.makeMove(new CatanRobberMoveAction(null, 1, destination)));
        assertFalse("Moving is not the same as choosing a victim",
                game.makeMove(new CatanEndTurnAction(null)));
        // Empty opening board: nobody is next to the robber, so -1 is legal.
        assertTrue(game.makeMove(new CatanRobberStealAction(null, 1, -1)));
        assertFalse(now.isRobberPhase());
        assertTrue(game.makeMove(new CatanEndTurnAction(null)));
        assertEquals(2, now.getCurrentPlayerId());
    }

    @Test public void strategiesHandleAllIslandsAndOpponentCounts() {
        int[] counts = {2, 4, 5, 7};
        CatanBoardTopology.Island[] islands = {
                CatanBoardTopology.Island.CLASSIC,
                CatanBoardTopology.Island.CLASSIC,
                CatanBoardTopology.Island.EXPANDED,
                CatanBoardTopology.Island.GRAND
        };
        for (int match = 0; match < counts.length; match++) {
            CatanGameState state = new CatanGameState(counts[match], islands[match]);
            int sites = state.getBoard().getBuildings().length;
            assertTrue(sites >= 54);
            for (int difficulty = 1; difficulty <= 4; difficulty++) {
                CatanAiDifficulty level = CatanAiDifficulty.fromPlayerType(difficulty);
                assertEquals(difficulty, level.playerType);
                // Rules/strategy must not mutate inventories or award free cards.
                int ownedBefore = state.getPlayerList().get(1)
                        .getTotalResourceCardCount();
                for (int vertex = 0; vertex < sites; vertex++) {
                    if (!state.getBoard().validBuildingLocation(1, true, vertex)) continue;
                    double value = CatanStrategy.scoreIntersection(state, vertex, 1, level);
                    assertTrue("Finite legal intersection value: " + match + "/" + vertex,
                            !Double.isNaN(value) && !Double.isInfinite(value));
                }
                assertEquals(ownedBefore, state.getPlayerList().get(1)
                        .getTotalResourceCardCount());
                assertNull("Cannot build roads without a road/settlement",
                        CatanAiPlanner.bestRoad(state, 1, level == CatanAiDifficulty.EXPERT));
            }
        }
    }

    @Test public void repeatedSeededMarketRoundsRemainInBoundsAndDoNotRepeat() {
        CatanGameState state = new CatanGameState();
        state.configureMarketEvents(true);
        CatanMarketState market = state.getMarketState();
        assertTrue(market.isEnabled());
        CatanMarketState.Event last = null;
        int events = 0;
        int disruptions = 0;
        Random seed = new Random(32031L);
        // Exercise many deterministic full-round boundaries, including all
        // special-used reset behaviour; no device/emulator required.
        for (int round = 0; round < 360; round++) {
            market.onRoundCompleted(state, seed);
            CatanMarketState.Event active = market.getActiveEvent();
            if (active != null) {
                assertNotEquals("No identical consecutive events", last, active);
                last = active;
                events++;
                if (active == CatanMarketState.Event.MERCHANT_SHORTAGE) {
                    disruptions++;
                    int resource = market.getSelectedResource();
                    assertTrue(resource >= 0 && resource < 5);
                    assertTrue(market.blocksBankReceipt(resource));
                }
            }
            assertTrue("Balanced human interruption cap", market.getProactiveOffersThisRound() <= 3);
        }
        assertTrue("Expected a useful sample of events", events >= 45);
        assertTrue("Expected rare disruptions", disruptions > 0);
    }

    @Test public void aiTradingDecisionDoesNotMutateOtherPlayersCards() {
        CatanGameState game = new CatanGameState();
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        game.getPlayerList().get(1).addResourceCard(3, 3);
        int before = game.getPlayerList().get(1).getResourceCards()[3];
        int[] given = {1,0,0,0,0};
        int[] wanted = {0,0,0,1,0};
        for (int difficulty = 1; difficulty <= 4; difficulty++)
            assertNotNull(CatanNegotiationAI.evaluate(game, 1, difficulty, given, wanted));
        assertEquals(before, game.getPlayerList().get(1).getResourceCards()[3]);
        assertEquals(0, game.getPlayerList().get(0).getResourceCards()[3]);
    }
}
