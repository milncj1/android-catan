package edu.up.cs.androidcatan.catan;

import java.util.Arrays;

/**
 * One live negotiation on the game thread. No save file contains a pending
 * agreement; restoring a match safely discards unaccepted offers.
 */
final class CatanTradeNegotiation {
    static final int NOT_INVITED = -1, WAITING = 0, DECLINED = 1,
            ACCEPTED = 2, COUNTERED = 3;
    final long id;
    final int proposer;
    final int[] giving, receiving;
    private final int[] status;
    private final int[][] counterGiving, counterReceiving;
    private boolean executed;

    CatanTradeNegotiation(long id, CatanGameState game, int proposer,
                          int[] targets, int[] giving, int[] receiving) {
        if (id < 1 || game == null || game.getCurrentPlayerId() != proposer
                || game.isSetupPhase() || game.isRobberPhase() || !game.isActionPhase()
                || !CatanTradingRules.validBundle(giving)
                || !CatanTradingRules.validBundle(receiving)
                || targets == null || targets.length == 0
                || proposer < 0 || proposer >= game.getPlayerList().size()
                || !game.getPlayerList().get(proposer).hasResourceBundle(giving))
            throw new IllegalArgumentException("Invalid trade proposal");

        this.id = id;
        this.proposer = proposer;
        this.giving = giving.clone();
        this.receiving = receiving.clone();
        int playerCount = game.getPlayerList().size();
        status = new int[playerCount];
        Arrays.fill(status, NOT_INVITED);
        counterGiving = new int[playerCount][];
        counterReceiving = new int[playerCount][];
        for (int target : targets) {
            if (target < 0 || target >= playerCount || target == proposer
                    || status[target] != NOT_INVITED)
                throw new IllegalArgumentException("Invalid or duplicate trade partner");
            status[target] = WAITING;
        }
    }

    boolean invited(int player) { return player >= 0 && player < status.length
            && status[player] != NOT_INVITED; }
    boolean waitingFor(int player) { return invited(player) && status[player] == WAITING; }
    boolean complete() {
        for (int state : status) if (state == WAITING) return false;
        return true;
    }

    /** A counter is expressed in terms of the original proposer's hand. */
    boolean respond(CatanGameState game, int partner, int response,
                    int[] revisedGiving, int[] revisedReceiving) {
        if (!waitingFor(partner) || game.getCurrentPlayerId() != proposer) return false;
        if (response == DECLINED) {
            status[partner] = DECLINED;
            return true;
        }
        if (response == ACCEPTED) {
            // Never advertise acceptance if the target cannot pay today.
            if (!game.getPlayerList().get(partner).hasResourceBundle(receiving))
                return false;
            status[partner] = ACCEPTED;
            return true;
        }
        if (response != COUNTERED || !CatanTradingRules.validBundle(revisedGiving)
                || !CatanTradingRules.validBundle(revisedReceiving)
                || !game.getPlayerList().get(partner).hasResourceBundle(revisedReceiving))
            return false;
        status[partner] = COUNTERED;
        counterGiving[partner] = revisedGiving.clone();
        counterReceiving[partner] = revisedReceiving.clone();
        return true;
    }

    boolean choose(CatanGameState game, int proposerId, int partner) {
        if (executed || proposerId != proposer || partner < 0 || !invited(partner)
                || !complete() || (status[partner] != ACCEPTED
                && status[partner] != COUNTERED)) return false;
        boolean succeeded = CatanTradingRules.exchangeBetweenPlayers(
                game, proposer, partner, actualGiving(partner), actualReceiving(partner));
        if (succeeded) executed = true;
        return succeeded;
    }

    int[] actualGiving(int partner) {
        return status[partner] == COUNTERED ? counterGiving[partner].clone() : giving.clone();
    }

    int[] actualReceiving(int partner) {
        return status[partner] == COUNTERED ? counterReceiving[partner].clone() : receiving.clone();
    }

    CatanTradeInfo requestInfo() {
        return new CatanTradeInfo(CatanTradeInfo.REQUEST, id, proposer, -1,
                giving, receiving, null, null, null, "");
    }

    CatanTradeInfo resultsInfo() {
        return new CatanTradeInfo(CatanTradeInfo.RESULTS, id, proposer, -1,
                giving, receiving, status, counterGiving, counterReceiving, "");
    }

    int[] getStatus() { return status.clone(); }
}
