package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import static org.junit.Assert.*;

/** CPU negotiations must be fair, deterministic and unable to guess hidden hands. */
public class CatanNegotiationAITest {
    private static CatanGameState game() {
        CatanGameState game = new CatanGameState(4, CatanBoardTopology.Island.CLASSIC);
        game.setSetupPhase(false);
        game.setActionPhase(true);
        game.setCurrentPlayerId(0);
        return game;
    }
    @Test public void cpuDeclinesWhenItDoesNotOwnTheRequestedCards() {
        CatanGameState state = game();
        CatanTradeResponsesInfo.Response reply = CatanNegotiationAI.evaluate(
                state, 1, 4, new int[]{1, 0, 0, 0, 0},
                new int[]{0, 0, 0, 1, 0});
        assertEquals(CatanTradeResponsesInfo.DECLINE, reply.getVerdict());
    }

    @Test public void easyCanAcceptButExpertCanCounterSameDeal() {
        CatanGameState state = game();
        state.getPlayerList().get(1).addResourceCard(3, 1);
        state.getPlayerList().get(1).addResourceCard(0, 2); // brick is less scarce to this CPU
        int[] brick = {1, 0, 0, 0, 0};
        int[] ore = {0, 0, 0, 1, 0};
        CatanTradeResponsesInfo.Response easy =
                CatanNegotiationAI.evaluate(state, 1, 1, brick, ore);
        CatanTradeResponsesInfo.Response expert =
                CatanNegotiationAI.evaluate(state, 1, 4, brick, ore);
        assertEquals(CatanTradeResponsesInfo.ACCEPT, easy.getVerdict());
        assertEquals(CatanTradeResponsesInfo.COUNTER, expert.getVerdict());
        assertEquals(2, expert.getGive()[0]); // asks for an extra brick
    }

    @Test public void cpuNegotiationCannotReadUndisclosedHumanHand() {
        CatanGameState state = game();
        state.getPlayerList().get(2).addResourceCard(3, 1);
        int[] offered = {1, 0, 0, 0, 0};
        int[] wanted = {0, 0, 0, 1, 0};
        int first = CatanNegotiationAI.evaluate(
                state, 2, 3, offered, wanted).getVerdict();
        // The only changed state is the human's undisclosed inventory.
        state.getPlayerList().get(0).addResourceCard(0, 8);
        state.getPlayerList().get(0).addResourceCard(4, 9);
        assertEquals(first, CatanNegotiationAI.evaluate(
                state, 2, 3, offered, wanted).getVerdict());
    }

    @Test public void computersHaveDiverseStableTradingPersonalities() {
        assertEquals(CatanNegotiationAI.Personality.MERCHANT,
                CatanNegotiationAI.personality(1));
        assertEquals(CatanNegotiationAI.Personality.BUILDER,
                CatanNegotiationAI.personality(2));
        assertEquals(CatanNegotiationAI.Personality.HOARDER,
                CatanNegotiationAI.personality(3));
        assertEquals(CatanNegotiationAI.Personality.OPPORTUNIST,
                CatanNegotiationAI.personality(4));
        assertEquals(CatanNegotiationAI.Personality.MERCHANT,
                CatanNegotiationAI.personality(5));
    }

    @Test public void responseBundlesAreDefensivelyCopied() {
        int[] give = {1,0,0,0,0}, want = {0,0,0,1,0};
        CatanTradeResponsesInfo.Response result = new CatanTradeResponsesInfo.Response(
                2, CatanTradeResponsesInfo.ACCEPT, give, want);
        give[0] = 100;
        result.getWant()[3] = 40;
        assertEquals(1, result.getGive()[0]);
        assertEquals(1, result.getWant()[3]);
    }
}
