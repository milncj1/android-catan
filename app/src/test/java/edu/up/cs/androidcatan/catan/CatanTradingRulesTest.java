package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.util.Random;

import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

import static org.junit.Assert.*;

/** Build 011: no free cards, no hijacking, market allowances survive deep copy. */
public class CatanTradingRulesTest {
    private CatanGameState actionGame() {
        CatanGameState game = new CatanGameState(4, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        return game;
    }

    /** Choose predictable market events without a chance-based/flaky test. */
    private static Random picks(final int shortageChance, final int eventChoice) {
        return new Random(5) {
            @Override public int nextInt(int bound) {
                if (bound == 5) return shortageChance;
                if (bound == 3 || bound == 4) return Math.min(eventChoice, bound - 1);
                return 0;
            }
        };
    }

    @Test public void bankAutomaticallyRespectsAvailableResourcesAndDoesNotMintOnFailure() {
        CatanGameState game = actionGame();
        Player player = game.getPlayerList().get(0);
        player.addResourceCard(0, 4);
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 0, 3));
        assertEquals(-1, CatanTradingRules.bestRate(game, 0, 0, 0));
        assertEquals(-1, CatanTradingRules.exchangeWithBank(game, 0, 1, 3));
        assertEquals(4, player.getResourceCards()[0]);
        assertEquals(4, CatanTradingRules.exchangeWithBank(game, 0, 0, 3));
        assertEquals(0, player.getResourceCards()[0]);
        assertEquals(1, player.getResourceCards()[3]);
        assertEquals(-1, CatanTradingRules.exchangeWithBank(game, 0, 0, 3));
        assertEquals(1, player.getResourceCards()[3]);
    }

    @Test public void eachOwnedHarbourAppliesAutomaticallyWithoutSelection() {
        CatanGameState game = actionGame();
        Port general = null;
        Port specific = null;
        for (Port port : game.getBoard().getPortList()) {
            if (general == null && port.getTradeRatio() == 3) general = port;
            if (specific == null && port.getTradeRatio() == 2) specific = port;
        }
        assertNotNull(general);
        assertNotNull(specific);
        game.getBoard().addBuilding(general.getIntersectionA(), new Settlement(0));
        assertEquals(3, CatanTradingRules.standardRate(game, 0, 1));
        game.getBoard().addBuilding(specific.getIntersectionA(), new Settlement(0));
        assertEquals(2, CatanTradingRules.standardRate(game, 0, specific.getResourceId()));
        assertEquals(4, CatanTradingRules.standardRate(game, 1, 1));
    }

    @Test public void atomicPlayerTradesCheckBothHandsAndNeverGiftResources() {
        CatanGameState game = actionGame();
        Player human = game.getPlayerList().get(0);
        Player rival = game.getPlayerList().get(1);
        human.addResourceCard(0, 2);
        rival.addResourceCard(3, 1);
        assertFalse(CatanTradingRules.exchangeBetweenPlayers(game, 0, 1,
                new int[]{2,0,0,0,0}, new int[]{0,0,0,2,0}));
        assertEquals(2, human.getResourceCards()[0]);
        assertEquals(1, rival.getResourceCards()[3]);
        assertFalse(CatanTradingRules.exchangeBetweenPlayers(game, 0, 1,
                new int[]{2,0,0,0,0}, new int[]{0,0,0,0,0}));
        assertFalse(CatanTradingRules.exchangeBetweenPlayers(game, 0, 0,
                new int[]{2,0,0,0,0}, new int[]{0,0,0,1,0}));
        assertTrue(CatanTradingRules.exchangeBetweenPlayers(game, 0, 1,
                new int[]{2,0,0,0,0}, new int[]{0,0,0,1,0}));
        assertEquals(0, human.getResourceCards()[0]);
        assertEquals(1, human.getResourceCards()[3]);
        assertEquals(2, rival.getResourceCards()[0]);
        assertEquals(0, rival.getResourceCards()[3]);
    }

    @Test public void firstEventBeginsInRoundFourAndExpiresAfterEveryonePlays() {
        CatanGameState game = actionGame();
        CatanMarketState market = game.getMarketState();
        market.enable(true);
        Random predictable = picks(1, 0); // positive, Market Day
        market.onRoundCompleted(game, predictable);
        market.onRoundCompleted(game, predictable);
        assertNull(market.getActiveEvent());
        market.onRoundCompleted(game, predictable);
        assertEquals(CatanMarketState.Event.MARKET_DAY, market.getActiveEvent());
        assertEquals(4, market.getActiveFromRound());
        assertTrue(market.getNextEventRound() >= 7);
        assertTrue(market.getNextEventRound() <= 9);
        market.onRoundCompleted(game, predictable);
        assertNull(market.getActiveEvent());
    }

    @Test public void marketDayDiscountIsOneUseAndCopiedForResume() {
        CatanGameState game = actionGame();
        game.configureMarketEvents(true);
        for (int i = 0; i < 3; i++)
            game.getMarketState().onRoundCompleted(game, picks(1, 0));
        assertEquals(CatanMarketState.Event.MARKET_DAY,
                game.getMarketState().getActiveEvent());
        Player human = game.getPlayerList().get(0);
        human.addResourceCard(0, 7);
        assertEquals(3, CatanTradingRules.bestRate(game, 0, 0, 3));
        assertEquals(3, CatanTradingRules.exchangeWithBank(game, 0, 0, 3));
        assertTrue(game.getMarketState().hasUsedSpecial(0));
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 0, 3));
        CatanGameState resumed = new CatanGameState(game);
        assertTrue(resumed.getMarketState().hasUsedSpecial(0));
        assertEquals(4, CatanTradingRules.bestRate(resumed, 0, 0, 3));
    }

    @Test public void shortageRestrictsReceivingFromBankNotPlayerTrades() {
        CatanGameState game = actionGame();
        game.configureMarketEvents(true);
        for (int i = 0; i < 3; i++)
            game.getMarketState().onRoundCompleted(game, picks(0, 0));
        assertEquals(CatanMarketState.Event.MERCHANT_SHORTAGE,
                game.getMarketState().getActiveEvent());
        int restricted = game.getMarketState().getSelectedResource();
        assertTrue(game.getMarketState().blocksBankReceipt(restricted));
        assertEquals(-1, CatanTradingRules.bestRate(game, 0,
                (restricted + 1) % 5, restricted));
        game.getPlayerList().get(0).addResourceCard(1, 1);
        game.getPlayerList().get(1).addResourceCard(restricted, 1);
        int[] giving = new int[5], receiving = new int[5];
        giving[1] = 1;
        receiving[restricted] = 1;
        // If the shortage resource is Grain (1), exchange one different card.
        if (restricted == 1) { giving[1] = 0; giving[2] = 1;
            game.getPlayerList().get(0).addResourceCard(2, 1); }
        assertTrue(CatanTradingRules.exchangeBetweenPlayers(game, 0, 1,
                giving, receiving));
    }

    @Test public void oldGamesStartWithClassicTradingAndNoMarketEvent() {
        CatanGameState game = actionGame();
        assertFalse(game.getMarketState().isEnabled());
        assertNull(game.getMarketState().getActiveEvent());
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 1, 0));
    }
}
