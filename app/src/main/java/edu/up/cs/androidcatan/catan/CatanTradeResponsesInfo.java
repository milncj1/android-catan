package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;
import java.util.List;

import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

/** Private offer results delivered ONLY to the human who initiated them. */
public final class CatanTradeResponsesInfo extends GameInfo {
    private static final long serialVersionUID = 20261014014L;
    public static final int ACCEPT = 1, COUNTER = 2, DECLINE = 3;
    public static final class Response implements java.io.Serializable {
        private static final long serialVersionUID = 20261014015L;
        private final int playerId;
        private final int verdict;
        private final int[] give, want; // active human's perspective
        public Response(int playerId, int verdict, int[] give, int[] want) {
            this.playerId = playerId;
            this.verdict = verdict;
            this.give = give.clone();
            this.want = want.clone();
        }
        public int getPlayerId() { return playerId; }
        public int getVerdict() { return verdict; }
        public int[] getGive() { return give.clone(); }
        public int[] getWant() { return want.clone(); }
    }

    private final long offerId;
    private final ArrayList<Response> responses;
    private final boolean confirmation;
    public CatanTradeResponsesInfo(long offerId, List<Response> responses) {
        this(offerId, responses, false);
    }
    private CatanTradeResponsesInfo(long offerId, List<Response> responses,
                                     boolean confirmation) {
        this.offerId = offerId;
        this.responses = new ArrayList<>(responses);
        this.confirmation = confirmation;
    }
    public static CatanTradeResponsesInfo completed() {
        return new CatanTradeResponsesInfo(0, new ArrayList<Response>(), true);
    }
    /** Adapt engine-approved private replies for the existing comparison panel. */
    public static CatanTradeResponsesInfo fromTradeInfo(CatanTradeInfo info) {
        ArrayList<Response> translated = new ArrayList<>();
        int[] status = info.getStatus();
        int[][] counterGiven = info.getCounterGiving();
        int[][] counterWanted = info.getCounterReceiving();
        if (status != null) for (int id = 0; id < status.length; id++) {
            if (status[id] == CatanTradeNegotiation.NOT_INVITED) continue;
            int verdict = status[id] == CatanTradeNegotiation.ACCEPTED ? ACCEPT
                    : status[id] == CatanTradeNegotiation.COUNTERED ? COUNTER : DECLINE;
            int[] offered = verdict == COUNTER ? counterGiven[id] : info.getGiving();
            int[] requested = verdict == COUNTER ? counterWanted[id] : info.getReceiving();
            translated.add(new Response(id, verdict, offered, requested));
        }
        return new CatanTradeResponsesInfo(info.getOfferId(), translated);
    }

    public long getOfferId() { return offerId; }
    public List<Response> getResponses() { return new ArrayList<>(responses); }
    public boolean isConfirmation() { return confirmation; }
}
