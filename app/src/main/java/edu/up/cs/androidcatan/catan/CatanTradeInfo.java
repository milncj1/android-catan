package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

/**
 * Targeted negotiation messages; contains a PROPOSAL only, never a player's
 * complete inventory. Results go ONLY to the player initiating the offer.
 */
public final class CatanTradeInfo extends GameInfo {
    private static final long serialVersionUID = 20261014016L;
    public static final int REQUEST = 1, RESULTS = 2, COMPLETE = 3, CANCELLED = 4;
    private final int kind, proposerId, partnerId;
    private final long offerId;
    private final int[] giving, receiving, status;
    private final int[][] counterGiving, counterReceiving;
    private final String message;

    public CatanTradeInfo(int kind, long offerId, int proposerId, int partnerId,
                          int[] giving, int[] receiving, int[] status,
                          int[][] counterGiving, int[][] counterReceiving,
                          String message) {
        this.kind = kind;
        this.offerId = offerId;
        this.proposerId = proposerId;
        this.partnerId = partnerId;
        this.giving = giving == null ? null : giving.clone();
        this.receiving = receiving == null ? null : receiving.clone();
        this.status = status == null ? null : status.clone();
        this.counterGiving = copy(counterGiving);
        this.counterReceiving = copy(counterReceiving);
        this.message = message == null ? "" : message;
    }
    private static int[][] copy(int[][] items) {
        if (items == null) return null;
        int[][] result = new int[items.length][];
        for (int i = 0; i < items.length; i++)
            result[i] = items[i] == null ? null : items[i].clone();
        return result;
    }
    public int getKind() { return kind; }
    public long getOfferId() { return offerId; }
    public int getProposerId() { return proposerId; }
    public int getPartnerId() { return partnerId; }
    public int[] getGiving() { return giving == null ? null : giving.clone(); }
    public int[] getReceiving() { return receiving == null ? null : receiving.clone(); }
    public int[] getStatus() { return status == null ? null : status.clone(); }
    public int[][] getCounterGiving() { return copy(counterGiving); }
    public int[][] getCounterReceiving() { return copy(counterReceiving); }
    public String getMessage() { return message; }
}
