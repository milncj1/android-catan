package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

/**
 * Evaluate offers from one computer's OWN resource cards and public board
 * information only. No access to the offering human's undisclosed hand.
 * Personality varies independently of difficulty.
 */
public final class CatanNegotiationAI {
    private CatanNegotiationAI() { }
    public enum Personality { MERCHANT, BUILDER, HOARDER, OPPORTUNIST }

    public static Personality personality(int cpuPlayerId) {
        return Personality.values()[Math.floorMod(cpuPlayerId - 1,
                Personality.values().length)];
    }

    public static CatanTradeResponsesInfo.Response evaluate(
            CatanGameState publicState, int cpuId, int difficultyType,
            int[] humanGives, int[] humanWants) {
        Player cpu = publicState.getPlayerList().get(cpuId);
        if (!CatanTradingRules.validBundle(humanGives)
                || !CatanTradingRules.validBundle(humanWants)
                || cpuId == publicState.getCurrentPlayerId()
                || !cpu.hasResourceBundle(humanWants))
            return new CatanTradeResponsesInfo.Response(cpuId,
                    CatanTradeResponsesInfo.DECLINE, humanGives, humanWants);

        int[] hand = cpu.getResourceCards();
        double[] valuations = valueCards(hand);
        double incoming = 0, outgoing = 0;
        for (int r = 0; r < 5; r++) {
            incoming += humanGives[r] * valuations[r];
            outgoing += humanWants[r] * valuations[r];
        }
        Personality personality = personality(cpuId);
        double threshold;
        switch (Math.max(1, Math.min(4, difficultyType))) {
            case 1: threshold = -0.70; break;
            case 2: threshold = -0.10; break;
            case 3: threshold = 0.18; break;
            default: threshold = 0.45; break;
        }
        switch (personality) {
            case MERCHANT: threshold -= 0.22; break;
            case BUILDER: threshold -= 0.08; break;
            case HOARDER: threshold += 0.30; break;
            case OPPORTUNIST: threshold += 0.20; break;
        }
        if (publicState.getMarketState().getActiveEvent()
                == CatanMarketState.Event.TRADING_FAIR) threshold -= 0.22;

        // Only public victory points, never the human's resource inventory.
        int publicLead = publicState.getPlayerList()
                .get(publicState.getCurrentPlayerId()).getVictoryPoints();
        if (difficultyType >= 3 && publicLead >= 8) threshold += 0.4;
        if (difficultyType >= 4 && publicLead >= 9) threshold += 1.1;

        if (incoming - outgoing >= threshold)
            return new CatanTradeResponsesInfo.Response(cpuId,
                    CatanTradeResponsesInfo.ACCEPT, humanGives, humanWants);

        // One practical counter: ask for one more of the OFFERED resource.
        // This never claims knowledge of how many cards the human actually has.
        int extra = -1;
        for (int r = 0; r < 5; r++)
            if (humanGives[r] > 0 && (extra < 0 || valuations[r] > valuations[extra]))
                extra = r;
        if (extra >= 0 && humanGives[extra] < 99
                && incoming + valuations[extra] - outgoing >= threshold - 0.25) {
            int[] counter = humanGives.clone();
            counter[extra]++;
            return new CatanTradeResponsesInfo.Response(cpuId,
                    CatanTradeResponsesInfo.COUNTER, counter, humanWants);
        }
        // Or keep the offered bundle, but surrender one fewer card when
        // the original request contains at least two of that resource.
        for (int r = 0; r < 5; r++) {
            if (humanWants[r] <= 1) continue;
            if (incoming - outgoing + valuations[r] >= threshold - 0.25) {
                int[] counterWant = humanWants.clone();
                counterWant[r]--;
                return new CatanTradeResponsesInfo.Response(cpuId,
                        CatanTradeResponsesInfo.COUNTER, humanGives, counterWant);
            }
        }
        return new CatanTradeResponsesInfo.Response(cpuId,
                CatanTradeResponsesInfo.DECLINE, humanGives, humanWants);
    }

    private static double[] valueCards(int[] hand) {
        // Look towards the closest meaningful affordable build. Its missing
        // ingredients are more valuable than resources held in surplus.
        int[][] targets = { Settlement.resourceCost, City.resourceCost,
                Road.resourceCost, new int[]{0, 1, 0, 1, 1} };
        int[] next = targets[0];
        int fewestMissing = Integer.MAX_VALUE;
        for (int[] cost : targets) {
            int missing = 0;
            for (int r = 0; r < 5; r++)
                missing += Math.max(0, cost[r] - hand[r]);
            if (missing > 0 && missing < fewestMissing) {
                fewestMissing = missing;
                next = cost;
            }
        }
        double[] result = new double[5];
        for (int r = 0; r < 5; r++) {
            result[r] = hand[r] == 0 ? 1.5
                    : hand[r] == 1 ? 1.22
                    : hand[r] == 2 ? 1.0
                    : hand[r] <= 3 ? 0.76 : 0.5;
            if (hand[r] < next[r]) result[r] += 0.7;
            if (hand[r] == next[r] && next[r] > 0) result[r] += 0.22;
        }
        return result;
    }
}
