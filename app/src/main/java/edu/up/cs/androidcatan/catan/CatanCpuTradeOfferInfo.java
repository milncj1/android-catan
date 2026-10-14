package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

/** Private, temporary CPU trade invitation. Never saved or broadcast. */
public final class CatanCpuTradeOfferInfo extends GameInfo {
    private static final long serialVersionUID = 20261014018L;
    private final int cpuPlayerId;
    private final long offerId;
    private final int[] youGive, youReceive;
    private final String outcome; // nonempty on completion, otherwise offer invitation
    public CatanCpuTradeOfferInfo(int cpuPlayerId, long offerId,
                                  int[] youGive, int[] youReceive) {
        this(cpuPlayerId, offerId, youGive, youReceive, "");
    }
    private CatanCpuTradeOfferInfo(int cpuPlayerId, long offerId,
                                   int[] youGive, int[] youReceive, String outcome) {
        this.cpuPlayerId = cpuPlayerId;
        this.offerId = offerId;
        this.youGive = youGive.clone();
        this.youReceive = youReceive.clone();
        this.outcome = outcome;
    }
    public static CatanCpuTradeOfferInfo outcome(String text) {
        return new CatanCpuTradeOfferInfo(-1, 0L,
                new int[5], new int[5], text);
    }
    public boolean isOutcome() { return !outcome.isEmpty(); }
    public String getOutcome() { return outcome; }
    public long getOfferId() { return offerId; }
    public int getCpuPlayerId() { return cpuPlayerId; }
    public int[] getYouGive() { return youGive.clone(); }
    public int[] getYouReceive() { return youReceive.clone(); }
}
