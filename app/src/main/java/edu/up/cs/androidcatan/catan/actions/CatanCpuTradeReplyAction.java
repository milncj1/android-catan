package edu.up.cs.androidcatan.catan.actions;

import java.io.Serializable;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/** Human's answer to a CPU proposal; counters are evaluated once, then closed. */
public final class CatanCpuTradeReplyAction extends GameAction implements Serializable {
    private static final long serialVersionUID = 20261014017L;
    public static final int DECLINE = 0, ACCEPT = 1, COUNTER = 2;
    private final long offerId;
    private final int answer;
    private final int[] humanGives, humanWants;
    public CatanCpuTradeReplyAction(GamePlayer human, long offerId, int answer,
                                    int[] humanGives, int[] humanWants) {
        super(human);
        this.offerId = offerId;
        this.answer = answer;
        this.humanGives = humanGives == null ? null : humanGives.clone();
        this.humanWants = humanWants == null ? null : humanWants.clone();
    }
    public long getOfferId() { return offerId; }
    public int getAnswer() { return answer; }
    public int[] getHumanGives() { return humanGives == null ? null : humanGives.clone(); }
    public int[] getHumanWants() { return humanWants == null ? null : humanWants.clone(); }
}
