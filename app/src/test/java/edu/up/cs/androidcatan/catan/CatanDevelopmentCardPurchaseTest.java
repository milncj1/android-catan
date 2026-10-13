package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanBuyDevCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;

import static org.junit.Assert.*;

/** Gameplay contract used by the private illustrated development-card hand. */
public class CatanDevelopmentCardPurchaseTest {
    private static CatanGameState authoritative(CatanLocalGame game) throws Exception {
        java.lang.reflect.Field field = CatanLocalGame.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CatanGameState) field.get(game);
    }

    @Test public void classicDeckHasExactly25Cards() {
        CatanGameState game = new CatanGameState();
        assertEquals(25, game.getDevelopmentCards().size());
        int[] counts = new int[5];
        for (Integer card : game.getDevelopmentCards()) counts[card]++;
        assertArrayEquals(new int[]{14, 5, 2, 2, 2}, counts);
    }

    @Test public void cardCannotBeBoughtDuringSetupOrWithoutResources() throws Exception {
        CatanLocalGame game = new CatanLocalGame();
        CatanGameState state = authoritative(game);
        state.getCurrentPlayer().addResourceCard(1, 1);
        state.getCurrentPlayer().addResourceCard(3, 1);
        state.getCurrentPlayer().addResourceCard(4, 1);
        assertFalse(game.makeMove(new CatanBuyDevCardAction(null)));

        state.setSetupPhase(false);
        state.setActionPhase(true);
        state.getCurrentPlayer().removeResourceCard(3, 1);
        assertFalse(game.makeMove(new CatanBuyDevCardAction(null)));
        assertEquals(25, state.getDevelopmentCards().size());
        assertTrue(state.getCurrentPlayer().getDevelopmentCards().isEmpty());
    }

    @Test public void acceptedPurchasePaysExactlyOnceAndAddsOneSecretCard() throws Exception {
        CatanLocalGame game = new CatanLocalGame();
        CatanGameState state = authoritative(game);
        state.setSetupPhase(false);
        state.setActionPhase(true);

        Player human = state.getPlayerList().get(0);
        human.addResourceCard(1, 1); // grain
        human.addResourceCard(3, 1); // ore
        human.addResourceCard(4, 1); // wool

        assertTrue(game.makeMove(new CatanBuyDevCardAction(null)));
        assertEquals(24, state.getDevelopmentCards().size());
        assertEquals(1, human.getDevelopmentCards().size());
        for (int resource : new int[]{1, 3, 4})
            assertEquals(0, human.getResourceCards()[resource]);

        int drawn = human.getDevelopmentCards().get(0);
        assertTrue(drawn >= 0 && drawn <= 4);
        if (drawn != 1) {
            assertFalse("New action card must wait until the next turn",
                    human.getPlayableDevCards().contains(drawn));
        }
        assertFalse("Without additional resources a duplicate buy must fail",
                game.makeMove(new CatanBuyDevCardAction(null)));
        assertEquals(1, human.getDevelopmentCards().size());

        assertTrue(game.makeMove(new CatanEndTurnAction(null)));
        if (drawn != 1)
            assertTrue("Action card becomes available after buyer's turn ends",
                    human.getPlayableDevCards().contains(drawn));
    }
}
