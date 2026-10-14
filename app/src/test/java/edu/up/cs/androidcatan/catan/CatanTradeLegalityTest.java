package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import static org.junit.Assert.*;

/** Prevent illegitimate Catan offers and keep unfinished negotiations harmless. */
public class CatanTradeLegalityTest {
    private static CatanGameState readyGame() {
        CatanGameState game = new CatanGameState(4, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        game.getPlayerList().get(0).addResourceCard(0, 3);
        game.getPlayerList().get(0).addResourceCard(3, 2);
        game.getPlayerList().get(1).addResourceCard(0, 2);
        game.getPlayerList().get(1).addResourceCard(4, 2);
        return game;
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalOverlappingOfferIsRejectedBeforeInvitations() {
        CatanGameState game = readyGame();
        new CatanTradeNegotiation(7, game, 0, new int[]{1},
                new int[]{2,0,0,1,0}, new int[]{1,0,0,0,1});
    }

    @Test public void invalidCounterCannotForceAcceptOrMoveResources() {
        CatanGameState game = readyGame();
        CatanTradeNegotiation trade = new CatanTradeNegotiation(9, game, 0,
                new int[]{1}, new int[]{1,0,0,0,0}, new int[]{0,0,0,0,1});
        assertFalse(trade.respond(game, 1, CatanTradeNegotiation.COUNTERED,
                new int[]{1,0,0,0,0}, new int[]{1,0,0,0,1}));
        assertTrue(trade.waitingFor(1));
        assertFalse(trade.complete());
        assertEquals(3, game.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(2, game.getPlayerList().get(1).getResourceCards()[4]);
        assertTrue(trade.respond(game, 1, CatanTradeNegotiation.DECLINED,
                null, null));
        assertTrue(trade.complete());
        assertFalse(trade.choose(game, 0, 1));
    }

    @Test public void declinedOrUnfinishedOfferCannotChangeInventory() {
        CatanGameState game = readyGame();
        CatanTradeNegotiation trade = new CatanTradeNegotiation(10, game, 0,
                new int[]{1,2}, new int[]{1,0,0,0,0},
                new int[]{0,0,0,0,1});
        assertTrue(trade.respond(game, 1, CatanTradeNegotiation.ACCEPTED,
                null, null));
        assertFalse(trade.complete());
        assertFalse(trade.choose(game, 0, 1));
        assertEquals(3, game.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(2, game.getPlayerList().get(1).getResourceCards()[4]);
    }
}
