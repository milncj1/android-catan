package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.util.Random;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import static org.junit.Assert.*;

/** Regression tests for CPU-initiated offers without disrupting turns. */
public class CatanProactiveTradingTest {
    @Test public void onlyThreeRotatingCpuPlayersMayOfferInASevenPlayerRound() {
        CatanGameState game = new CatanGameState(7, CatanBoardTopology.Island.GRAND);
        CatanMarketState market = game.getMarketState();
        assertTrue(market.mayComputerOffer(1, 7));
        assertTrue(market.mayComputerOffer(2, 7));
        assertTrue(market.mayComputerOffer(3, 7));
        assertFalse(market.mayComputerOffer(4, 7));

        market.recordProactiveOffer();
        market.recordProactiveOffer();
        market.recordProactiveOffer();
        for (int id = 1; id <= 6; id++)
            assertFalse(market.mayComputerOffer(id, 7));

        // All players completed a round; offers reset even with events OFF.
        market.onRoundCompleted(game, new Random(1));
        assertEquals(0, market.getProactiveOffersThisRound());
        assertTrue(market.mayComputerOffer(4, 7));
        assertTrue(market.mayComputerOffer(5, 7));
        assertTrue(market.mayComputerOffer(6, 7));
        assertFalse(market.mayComputerOffer(1, 7));
    }

    @Test public void cpuOffersResourcesItActuallyOwnsAndRequestsUsefulMissingCard() {
        CatanGameState game = new CatanGameState(3, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(1);
        Player cpu = game.getPlayerList().get(1);
        cpu.addResourceCard(0, 1); // brick
        cpu.addResourceCard(1, 1); // grain
        cpu.addResourceCard(4, 5); // plenty of spare wool
        int[][] offer = CatanNegotiationAI.proposeOnOwnTurn(game, 1);
        assertNotNull(offer);
        assertTrue(CatanTradingRules.validBundle(offer[0]));
        assertTrue(CatanTradingRules.validBundle(offer[1]));
        assertTrue(cpu.hasResourceBundle(offer[0]));
        assertEquals(0, offer[0][2]); // CPU cannot offer missing lumber
        assertEquals(1, offer[1][2]); // request lumber to build settlement
        assertTrue(offer[0][4] >= 1);
    }

    @Test public void proactiveOfferDoesNotInspectTheHumanSecretHand() {
        CatanGameState game = new CatanGameState(3, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(1);
        Player cpu = game.getPlayerList().get(1);
        cpu.addResourceCard(0, 1);
        cpu.addResourceCard(1, 1);
        cpu.addResourceCard(4, 5);
        int[][] first = CatanNegotiationAI.proposeOnOwnTurn(game, 1);
        game.getPlayerList().get(0).addResourceCard(1, 10);
        game.getPlayerList().get(0).addResourceCard(2, 12);
        int[][] second = CatanNegotiationAI.proposeOnOwnTurn(game, 1);
        assertNotNull(first);
        assertArrayEquals(first[0], second[0]);
        assertArrayEquals(first[1], second[1]);
    }

    @Test public void cpuCannotOfferOutsideItsOwnPostRollActionPhase() {
        CatanGameState game = new CatanGameState();
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        assertNull(CatanNegotiationAI.proposeOnOwnTurn(game, 1));
        game.setCurrentPlayerId(1);
        game.setActionPhase(false);
        assertNull(CatanNegotiationAI.proposeOnOwnTurn(game, 1));
    }
}
