package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.up.cs.androidcatan.catan.gamestate.Board;

/**
 * Regression checks for classic rules, runnable as local JVM JUnit tests.
 */
public class CatanGameStateRulesTest {
    @Test public void developmentDeckHasExactlyTwentyFiveCardsAndNeverRefills() {
        CatanGameState game = new CatanGameState();
        int[] cards = new int[5];
        for (int n = 0; n < 25; n++) {
            assertTrue(game.hasDevelopmentCardsAvailable());
            int card = game.getRandomDevCard();
            assertTrue(card >= 0 && card < cards.length);
            cards[card]++;
        }
        assertArrayEquals(new int[]{14, 5, 2, 2, 2}, cards);
        assertFalse(game.hasDevelopmentCardsAvailable());
        assertEquals(-1, game.getRandomDevCard());
    }

    @Test public void robberRequiresExactlyHalfOfCardsAndRejectsNegativeAmounts() {
        CatanGameState game = new CatanGameState();
        game.setRobberPhase(true);
        Player player = game.getPlayerList().get(0);
        player.addResourceCard(0, 8);
        assertFalse(game.validDiscard(0, new int[]{-1, 0, 0, 0, 0}));
        assertFalse(game.validDiscard(0, new int[]{3, 0, 0, 0, 0}));
        assertFalse(game.validDiscard(0, new int[]{5, 0, 0, 0, 0}));
        assertTrue(game.validDiscard(0, new int[]{4, 0, 0, 0, 0}));
        assertFalse(game.discardResources(0, new int[]{5, 0, 0, 0, 0}));
        assertEquals(8, player.getResourceCards()[0]);
        assertTrue(game.discardResources(0, new int[]{4, 0, 0, 0, 0}));
        assertEquals(4, player.getResourceCards()[0]);
        assertFalse(game.discardResources(0, new int[]{0, 0, 0, 0, 0}));
    }

    @Test public void roadPlacementRejectsInvalidVertexIdsWithoutCrashing() {
        Board board = new Board();
        assertFalse(board.validRoadPlacement(0, false, -1, 1));
        assertFalse(board.validRoadPlacement(0, false, 0, -1));
        assertFalse(board.validRoadPlacement(0, false, 54, 1));
        assertFalse(board.validRoadPlacement(0, false, 1, 1));
        assertFalse(board.validRoadPlacement(-1, false, 0, 1));
    }

    @Test public void openingTurnStateSurvivesDeepCopy() {
        CatanGameState game = new CatanGameState();
        game.setCurrentSetupSettlement(8);
        game.setSetupRoadBuiltThisTurn(true);
        CatanGameState copy = new CatanGameState(game);
        assertEquals(8, copy.getCurrentSetupSettlement());
        assertTrue(copy.isSetupRoadBuiltThisTurn());
        copy.setSetupRoadBuiltThisTurn(false);
        assertTrue(game.isSetupRoadBuiltThisTurn());
    }

    @Test public void stolenResourcesComeOnlyFromHeldCardTypes() {
        Player player = new Player(0);
        player.addResourceCard(0, 5);
        for (int n = 0; n < 50; n++) assertEquals(0, player.getRandomCard());
        assertEquals(5, player.getTotalResourceCardCount());
    }
}
