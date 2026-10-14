package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.util.Random;

import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

import static org.junit.Assert.*;

/** Each optional event must honor the agreed one-round, one-use market rules. */
public final class CatanMarketRulesRegressionTest {
    private static CatanGameState game() {
        CatanGameState game = new CatanGameState(4, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        game.configureMarketEvents(true);
        return game;
    }

    /** Choose a market event deterministically, keeping the tests repeatable. */
    private static Random marketDice(final int disruption, final int choice,
                                     final int resource) {
        return new Random(7L) {
            int callsForFive;
            @Override public int nextInt(int bound) {
                if (bound == 5) return callsForFive++ == 0 ? disruption : resource;
                if (bound == 4) return Math.min(choice, 3);
                if (bound == 3) return Math.min(choice, 2);
                return 0;
            }
        };
    }

    private static void startFirstEvent(CatanGameState game, Random dice) {
        for (int i = 0; i < 3; i++)
            game.getMarketState().onRoundCompleted(game, dice);
    }

    @Test public void marketDayGivesOneThreeToOneDiscountOnly() {
        CatanGameState game = game();
        startFirstEvent(game, marketDice(4, 0, 0));
        assertEquals(CatanMarketState.Event.MARKET_DAY,
                game.getMarketState().getActiveEvent());
        game.getPlayerList().get(0).addResourceCard(0, 8);
        assertEquals(3, CatanTradingRules.bestRate(game, 0, 0, 3));
        assertEquals(3, CatanTradingRules.exchangeWithBank(game, 0, 0, 3));
        assertTrue(game.getMarketState().hasUsedSpecial(0));
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 0, 3));
        assertEquals(5, game.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(1, game.getPlayerList().get(0).getResourceCards()[3]);
        CatanGameState restoredCopy = new CatanGameState(game);
        assertEquals(4, CatanTradingRules.bestRate(restoredCopy, 0, 0, 3));
        game.getMarketState().onRoundCompleted(game, marketDice(4, 0, 0));
        assertNull(game.getMarketState().getActiveEvent());
    }

    @Test public void caravanOnlyDiscountsGivingTheAnnouncedResource() {
        CatanGameState game = game();
        startFirstEvent(game, marketDice(4, 1, 2));
        assertEquals(CatanMarketState.Event.MERCHANT_CARAVAN,
                game.getMarketState().getActiveEvent());
        assertEquals(2, game.getMarketState().getSelectedResource());
        game.getPlayerList().get(0).addResourceCard(2, 4);
        assertEquals(2, CatanTradingRules.bestRate(game, 0, 2, 3));
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 0, 2));
        assertEquals(2, CatanTradingRules.exchangeWithBank(game, 0, 2, 3));
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 2, 3));
    }

    @Test public void shortageBlocksBankReceiptButNotDirectPlayerExchange() {
        CatanGameState game = game();
        startFirstEvent(game, marketDice(0, 0, 3));
        assertEquals(CatanMarketState.Event.MERCHANT_SHORTAGE,
                game.getMarketState().getActiveEvent());
        assertTrue(game.getMarketState().blocksBankReceipt(3));
        assertEquals(CatanTradingRules.INVALID_RATE,
                CatanTradingRules.bestRate(game, 0, 0, 3));
        game.getPlayerList().get(0).addResourceCard(0, 1);
        game.getPlayerList().get(1).addResourceCard(3, 1);
        assertTrue(CatanTradingRules.exchangeBetweenPlayers(game, 0, 1,
                new int[]{1,0,0,0,0}, new int[]{0,0,0,1,0}));
        assertEquals(1, game.getPlayerList().get(0).getResourceCards()[3]);
        assertEquals(1, game.getPlayerList().get(1).getResourceCards()[0]);
    }

    @Test public void harbourFestivalRequiresOwnedHarbourAndHonoursTwoToOne() {
        CatanGameState game = game();
        Port specialist = null;
        for (Port port : game.getBoard().getPortList())
            if (port.getTradeRatio() == 2) { specialist = port; break; }
        assertNotNull(specialist);
        assertTrue(game.getBoard().addBuilding(
                specialist.getIntersectionA(), new Settlement(0)));
        assertTrue(CatanTradingRules.ownsAnyHarbour(game, 0));
        int giving = (specialist.getResourceId() + 1) % 5;
        int receiving = (giving + 1) % 5;
        // Festival eligibility depends on a harbour, not whether its
        // specialist resource matches the resource being offered.
        startFirstEvent(game, marketDice(4, 3, 0));
        assertEquals(CatanMarketState.Event.HARBOUR_FESTIVAL,
                game.getMarketState().getActiveEvent());
        assertEquals(2, CatanTradingRules.bestRate(game, 0, giving, receiving));
        assertEquals(4, CatanTradingRules.bestRate(game, 1, giving, receiving));
    }

    @Test public void shortageCannotOccurTwiceBackToBack() {
        CatanGameState game = game();
        Random alwaysShortage = marketDice(0, 0, 2);
        startFirstEvent(game, alwaysShortage);
        assertEquals(CatanMarketState.Event.MERCHANT_SHORTAGE,
                game.getMarketState().getActiveEvent());
        // First event begins round 4, next event begins round 7.
        for (int i = 0; i < 3; i++)
            game.getMarketState().onRoundCompleted(game, alwaysShortage);
        assertNotEquals(CatanMarketState.Event.MERCHANT_SHORTAGE,
                game.getMarketState().getActiveEvent());
    }

    @Test public void marketOffNeverSchedulesAnEventAndOffersRemainPermitted() {
        CatanGameState game = game();
        game.configureMarketEvents(false);
        for (int i = 0; i < 20; i++)
            game.getMarketState().onRoundCompleted(game, marketDice(0, 0, 0));
        assertNull(game.getMarketState().getActiveEvent());
        assertEquals(4, CatanTradingRules.bestRate(game, 0, 0, 3));
        assertTrue(game.getMarketState().mayComputerOffer(1, 4));
    }
}
