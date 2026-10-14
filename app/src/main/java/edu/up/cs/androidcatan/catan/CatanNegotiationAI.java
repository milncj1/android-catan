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

    /**
     * Called only by the CPU on its OWN turn. Proposes a resource needed for
     * the nearest build in exchange for its OWN surplus cards. No inspection
     * of the human inventory is possible here.
     *
     * Returns {cpuGives, cpuWants}, or null if no sensible offer exists.
     */
    public static int[][] proposeOnOwnTurn(CatanGameState game, int cpuId) {
        if (game == null || game.getCurrentPlayerId() != cpuId
                || !game.isActionPhase() || game.isRobberPhase()) return null;
        int[] cards = game.getPlayerList().get(cpuId).getResourceCards();
        int[][] targets = { Settlement.resourceCost, City.resourceCost,
                Road.resourceCost, new int[]{0,1,0,1,1} };
        int[] bestTarget = null;
        int bestMissing = Integer.MAX_VALUE;
        for (int[] cost : targets) {
            int missing = 0;
            for (int r = 0; r < 5; r++)
                missing += Math.max(0, cost[r] - cards[r]);
            if (missing > 0 && missing < bestMissing) {
                bestMissing = missing;
                bestTarget = cost;
            }
        }
        if (bestTarget == null || bestMissing > 3) return null;
        int needed = -1, surplus = -1, largestSpare = 0;
        for (int r = 0; r < 5; r++) {
            if (cards[r] < bestTarget[r] && needed < 0) needed = r;
            int spare = cards[r] - bestTarget[r];
            if (spare > largestSpare) {
                largestSpare = spare;
                surplus = r;
            }
        }
        if (needed < 0 || surplus < 0 || needed == surplus) return null;
        int[] give = new int[5], want = new int[5];
        // A 1:1 offer is fair; the AI may offer two surplus cards if it has many.
        give[surplus] = largestSpare >= 4 ? 2 : 1;
        want[needed] = 1;
        return new int[][] {give, want};
    }

    /** Choose another CPU using public production, never its hidden hand. */
    public static int bestComputerTradingPartner(CatanGameState game,
                                                 int self, int resourceNeeded) {
        int players = game.getPlayerList().size();
        if (players < 3 || resourceNeeded < 0 || resourceNeeded > 4) return -1;
        int[] pips = {0, 0, 1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1};
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int cpu = 1; cpu < players; cpu++) {
            if (cpu == self) continue;
            double score = 0;
            for (int vertex = 0; vertex < game.getBoard().getBuildings().length;
                    vertex++) {
                edu.up.cs.androidcatan.catan.gamestate.buildings.Building house =
                        game.getBoard().getBuildings()[vertex];
                if (house == null || house.getOwnerId() != cpu) continue;
                for (Integer hexId : game.getBoard().getIntToHexIdMap().get(vertex)) {
                    edu.up.cs.androidcatan.catan.gamestate.Hexagon hex =
                            game.getBoard().getHexagonFromId(hexId);
                    if (hex.getResourceId() == resourceNeeded
                            && hex.getChitValue() >= 0 && hex.getChitValue() < pips.length)
                        score += pips[hex.getChitValue()]
                                * (house instanceof City ? 2 : 1);
                }
            }
            // Avoid handing the win to an opponent based on public points.
            if (game.getPlayerList().get(cpu).getVictoryPoints() >= 9) score -= 30;
            score += ((cpu + game.getMarketState().getCompletedRounds()) % 5) * .01;
            if (score > bestScore) { bestScore = score; best = cpu; }
        }
        return best;
    }

    /** A single CPU reply to a human's counteroffer while CPU remains active. */
    public static boolean acceptsCounterOnOwnTurn(CatanGameState game, int cpuId,
            int difficultyType, int[] cpuGives, int[] cpuReceives) {
        if (!CatanTradingRules.validBundle(cpuGives)
                || !CatanTradingRules.validBundle(cpuReceives)
                || !game.getPlayerList().get(cpuId).hasResourceBundle(cpuGives))
            return false;
        int[] ownHand = game.getPlayerList().get(cpuId).getResourceCards();
        double[] values = valueCards(ownHand);
        double delta = 0;
        for (int r = 0; r < 5; r++)
            delta += cpuReceives[r] * values[r] - cpuGives[r] * values[r];
        double minAdvantage = difficultyType <= 1 ? -0.65
                : difficultyType == 2 ? -0.10 : difficultyType == 3 ? 0.15 : 0.35;
        if (personality(cpuId) == Personality.MERCHANT) minAdvantage -= 0.25;
        if (personality(cpuId) == Personality.HOARDER) minAdvantage += 0.25;
        if (game.getMarketState().getActiveEvent()
                == CatanMarketState.Event.TRADING_FAIR) minAdvantage -= 0.20;
        return delta >= minAdvantage;
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
