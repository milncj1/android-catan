package edu.up.cs.androidcatan.catan.actions;

import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/**
 * Target's reply to an outstanding server-issued offer ID. In a counteroffer,
 * giving/receiving always describe resources given/received by the PROPOSER.
 */
public final class CatanTradeReplyAction extends GameAction {
    private static final long serialVersionUID = 20261014014L;
    public static final int DECLINE = 0, ACCEPT = 1, COUNTER = 2;
    private final long offerId;
    private final int response;
    private final int[] counterGiving, counterReceiving;
    public CatanTradeReplyAction(GamePlayer player, long offerId, int response,
                                 int[] counterGiving, int[] counterReceiving) {
        super(player);
        this.offerId = offerId;
        this.response = response;
        this.counterGiving = counterGiving == null ? null : counterGiving.clone();
        this.counterReceiving = counterReceiving == null ? null : counterReceiving.clone();
    }
    public long getOfferId() { return offerId; }
    public int getResponse() { return response; }
    public int[] getCounterGiving() {
        return counterGiving == null ? null : counterGiving.clone();
    }
    public int[] getCounterReceiving() {
        return counterReceiving == null ? null : counterReceiving.clone();
    }
}
