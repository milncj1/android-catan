package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.catan.actions.CatanTradeReplyAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeChooseAction;
import edu.up.cs.androidcatan.game.Game;
import edu.up.cs.androidcatan.game.GamePlayer;

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
        // Expert respects public victory threats without knowing private hands.
        if (difficulty == CatanAiDifficulty.EXPERT
                && game.getPlayerList().get(proposal.getProposerId())
                        .getVictoryPoints() >= 9)
            return new Decision(CatanTradeReplyActionStatus.DECLINE, null, null);

        // Use the SAME goal-based valuation and independent personality in
        // unsolicited offers, responses and counteroffers.
        CatanTradeResponsesInfo.Response answer = CatanNegotiationAI.evaluate(
                game, self, difficulty.playerType,
                proposal.getGiving(), proposal.getReceiving());
        if (answer.getVerdict() == CatanTradeResponsesInfo.ACCEPT)
            return new Decision(CatanTradeReplyActionStatus.ACCEPT, null, null);
        if (answer.getVerdict() == CatanTradeResponsesInfo.COUNTER)
            return new Decision(CatanTradeReplyActionStatus.COUNTER,
                    answer.getGive(), answer.getWant());
        return new Decision(CatanTradeReplyActionStatus.DECLINE, null, null);
    }

    /** Route private trade requests on each CPU's own player thread. */
    static void onTradeInfo(CatanTradeInfo info, CatanGameState lastSeen,
                            GamePlayer computer, Game game, int playerId,
                            CatanAiDifficulty difficulty) {
        if (lastSeen == null || game == null) return;
        if (info.getKind() == CatanTradeInfo.REQUEST) {
            if (info.getProposerId() == playerId
                    || lastSeen.getCurrentPlayerId() != info.getProposerId()) return;
            Decision choice = evaluate(lastSeen, playerId, difficulty, info);
            game.sendAction(new CatanTradeReplyAction(computer, info.getOfferId(),
                    choice.response, choice.giving, choice.receiving));
        } else if (info.getKind() == CatanTradeInfo.RESULTS
                && info.getProposerId() == playerId) {
            int best = -1;
            int[] statuses = info.getStatus();
            if (statuses != null) {
                for (int i = 0; i < statuses.length; i++)
                    if (statuses[i] == CatanTradeNegotiation.ACCEPTED) {
                        best = i;
                        break;
                    }
                if (best < 0) {
                    int[][] counterGiving = info.getCounterGiving();
                    int[][] counterReceiving = info.getCounterReceiving();
                    for (int i = 0; i < statuses.length; i++) {
                        if (statuses[i] != CatanTradeNegotiation.COUNTERED
                                || counterGiving == null || counterReceiving == null
                                || !CatanTradingRules.validBundle(counterGiving[i])
                                || !CatanTradingRules.validBundle(counterReceiving[i])
                                || !lastSeen.getPlayerList().get(playerId)
                                        .hasResourceBundle(counterGiving[i])
                                || !CatanNegotiationAI.acceptsCounterOnOwnTurn(
                                        lastSeen, playerId, difficulty.playerType,
                                        counterGiving[i], counterReceiving[i])) continue;
                        best = i;
                        break;
                    }
                }
            }
            game.sendAction(new CatanTradeChooseAction(computer, info.getOfferId(), best));
        }
    }

    /** Local response constants without linking this evaluator to UI classes. */
    static final class CatanTradeReplyActionStatus {
        static final int DECLINE = 0, ACCEPT = 1, COUNTER = 2;
    }
}
