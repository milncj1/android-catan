package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import static org.junit.Assert.*;

/** No cards change until a specific invited partner's reply is chosen. */
public final class CatanTradeNegotiationTest {
    private CatanGameState match() {
        CatanGameState gs = new CatanGameState();
        gs.setSetupPhase(false);
        gs.setActionPhase(true);
        gs.setCurrentPlayerId(0);
        gs.getPlayerList().get(0).addResourceCard(0, 3); // Brick
        gs.getPlayerList().get(1).addResourceCard(3, 2); // Ore
        gs.getPlayerList().get(3).addResourceCard(3, 2); // Ore
        return gs;
    }

    private CatanTradeNegotiation offer(CatanGameState game) {
        return new CatanTradeNegotiation(5, game, 0, new int[]{1, 2, 3},
                new int[]{2, 0, 0, 0, 0}, new int[]{0, 0, 0, 1, 0});
    }

    @Test public void allInvitedPartnersRespondBeforeAnyCardsMove() {
        CatanGameState match = match();
        CatanTradeNegotiation trade = offer(match);
        assertFalse(trade.complete());
        assertTrue(trade.respond(match, 1, CatanTradeNegotiation.ACCEPTED, null, null));
        assertFalse(trade.complete());
        assertFalse(trade.choose(match, 0, 1));
        assertEquals(3, match.getPlayerList().get(0).getResourceCards()[0]);
        assertTrue(trade.respond(match, 2, CatanTradeNegotiation.DECLINED, null, null));
        assertTrue(trade.respond(match, 3, CatanTradeNegotiation.ACCEPTED, null, null));
        assertTrue(trade.complete());
        assertTrue(trade.choose(match, 0, 3));
        assertEquals(1, match.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(1, match.getPlayerList().get(0).getResourceCards()[3]);
        assertEquals(2, match.getPlayerList().get(3).getResourceCards()[0]);
        assertEquals(1, match.getPlayerList().get(3).getResourceCards()[3]);
        assertEquals(0, match.getPlayerList().get(1).getResourceCards()[0]);
    }

    @Test public void noUninvitedRepeatOrSpoofedReplies() {
        CatanGameState game = match();
        CatanTradeNegotiation trade = offer(game);
        assertFalse(trade.respond(game, 0, CatanTradeNegotiation.ACCEPTED, null, null));
        assertFalse(trade.respond(game, 4, CatanTradeNegotiation.ACCEPTED, null, null));
        assertTrue(trade.respond(game, 1, CatanTradeNegotiation.ACCEPTED, null, null));
        assertFalse(trade.respond(game, 1, CatanTradeNegotiation.DECLINED, null, null));
        assertFalse(trade.choose(game, 1, 1));
        assertFalse(trade.choose(game, 0, 0));
    }

    @Test public void playerCannotAcceptAnOfferBeyondTheirHand() {
        CatanGameState game = match();
        CatanTradeNegotiation trade = offer(game);
        assertFalse(trade.respond(game, 2, CatanTradeNegotiation.ACCEPTED, null, null));
        assertTrue(trade.waitingFor(2));
        assertTrue(trade.respond(game, 2, CatanTradeNegotiation.DECLINED, null, null));
    }

    @Test public void onlyChosenCounterofferTransfersItsExactResources() {
        CatanGameState game = match();
        CatanTradeNegotiation trade = offer(game);
        assertTrue(trade.respond(game, 1, CatanTradeNegotiation.COUNTERED,
                new int[]{3, 0, 0, 0, 0}, new int[]{0, 0, 0, 1, 0}));
        assertTrue(trade.respond(game, 2, CatanTradeNegotiation.DECLINED, null, null));
        assertTrue(trade.respond(game, 3, CatanTradeNegotiation.DECLINED, null, null));
        assertEquals(CatanTradeNegotiation.COUNTERED, trade.getStatus()[1]);
        assertTrue(trade.choose(game, 0, 1));
        assertEquals(0, game.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(1, game.getPlayerList().get(0).getResourceCards()[3]);
    }

    @Test public void proposalRejectsFreeGiftsAndDuplicateInvitations() {
        CatanGameState game = match();
        boolean rejectedGift = false, rejectedDuplicate = false;
        try {
            new CatanTradeNegotiation(11, game, 0, new int[]{1},
                    new int[]{1, 0, 0, 0, 0}, new int[5]);
        } catch (IllegalArgumentException expected) { rejectedGift = true; }
        try {
            new CatanTradeNegotiation(11, game, 0, new int[]{1,1},
                    new int[]{1,0,0,0,0}, new int[]{0,0,0,1,0});
        } catch (IllegalArgumentException expected) { rejectedDuplicate = true; }
        assertTrue(rejectedGift);
        assertTrue(rejectedDuplicate);
    }

    @Test public void resumedGameDoesNotRetainPendingAgreement() throws Exception {
        CatanGameState game = match();
        game.setTradePending(true);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream serializer = new java.io.ObjectOutputStream(out);
        serializer.writeObject(game);
        serializer.close();
        java.io.ObjectInputStream deserializer = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(out.toByteArray()));
        CatanGameState resumed = (CatanGameState) deserializer.readObject();
        assertFalse("Unfinished offers must be cancelled after restart", resumed.isTradePending());
        assertEquals(3, resumed.getPlayerList().get(0).getResourceCards()[0]);
    }

    @Test public void balancedHumanOfferCapRotatesFairlyOnSixOpponents() {
        CatanGameState game = match();
        CatanMarketState market = game.getMarketState();
        assertTrue(market.mayComputerOffer(1, 7));
        assertTrue(market.mayComputerOffer(2, 7));
        assertTrue(market.mayComputerOffer(3, 7));
        assertFalse(market.mayComputerOffer(4, 7));
        market.recordProactiveOffer();
        market.recordProactiveOffer();
        market.recordProactiveOffer();
        assertFalse(market.mayComputerOffer(1, 7));
        market.onRoundCompleted(game, new java.util.Random(1));
        assertFalse("Next round rotates away from first three", market.mayComputerOffer(1, 7));
        assertTrue(market.mayComputerOffer(4, 7));
    }
}
