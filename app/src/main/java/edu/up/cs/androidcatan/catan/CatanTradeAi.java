package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

/**
 * Respond using ONLY this computer's own resources, public victory points and
 * the disclosed proposal. Does not inspect another player's hidden hand.
 */
final class CatanTradeAi {
    static final class Decision {
        final int response;
        final int[] giving, receiving;
        Decision(int response, int[] giving, int[] receiving) {
            this.response = response;
            this.giving = giving;
            this.receiving = receiving;
        }
    }
    private CatanTradeAi() { }

    static Decision evaluate(CatanGameState game, int self, CatanAiDifficulty difficulty,
                             CatanTradeInfo proposal) {
        int[] hand = game.getPlayerList().get(self).getResourceCards();
        int[] toGet = proposal.getGiving(); // proposer pays, CPU receives
        int[] toGive = proposal.getReceiving(); // CPU pays, proposer receives
        if (!game.getPlayerList().get(self).hasResourceBundle(toGive))
            return new Decision(CatanTradeReplyActionStatus.DECLINE, null, null);

        // Expert uses public victory-point totals, not the proposer's
        // hidden development cards or resources.
        if (difficulty == CatanAiDifficulty.EXPERT
                && game.getPlayerList().get(proposal.getProposerId()).getVictoryPoints() >= 9)
            return new Decision(CatanTradeReplyActionStatus.DECLINE, null, null);

        double gain = 0, loss = 0;
        for (int res = 0; res < 5; res++) {
            double value = marginalValue(hand, res);
            gain += toGet[res] * value;
            // Going below one of each resource tends to be more damaging.
            loss += toGive[res] * (value + (hand[res] - toGive[res] == 0 ? 0.35 : 0));
        }
        double tolerance = difficulty == CatanAiDifficulty.EASY ? 0.78
                : difficulty == CatanAiDifficulty.NORMAL ? 0.96
                : difficulty == CatanAiDifficulty.HARD ? 1.07 : 1.15;

        if (gain >= loss * tolerance)
            return new Decision(CatanTradeReplyActionStatus.ACCEPT, null, null);

        if (difficulty == CatanAiDifficulty.EASY || gain < loss * 0.48)
            return new Decision(CatanTradeReplyActionStatus.DECLINE, null, null);

        // Counter with one EXTRA resource in the CPU's preferred direction.
        // The computer does not assume that the other player actually has it.
        int desirable = 0;
        for (int r = 1; r < 5; r++)
            if (marginalValue(hand, r) > marginalValue(hand, desirable)) desirable = r;
        int[] counterGive = toGet.clone();   // from original proposer
        int[] counterReceive = toGive.clone();
        counterGive[desirable] += 1;
        return new Decision(CatanTradeReplyActionStatus.COUNTER, counterGive, counterReceive);
    }

    private static double marginalValue(int[] hand, int resource) {
        int[][] goal = {Settlement.resourceCost, City.resourceCost,
                Road.resourceCost, DevelopmentCard.resourceCost};
        double needed = 0;
        for (int[] bundle : goal)
            if (hand[resource] < bundle[resource]) needed += .3;
        return 1.0 + needed + (hand[resource] == 0 ? .45 : 0)
                - (hand[resource] > 4 ? .3 : 0);
    }

    /** Local response constants without linking this evaluator to UI classes. */
    static final class CatanTradeReplyActionStatus {
        static final int DECLINE = 0, ACCEPT = 1, COUNTER = 2;
    }
}
