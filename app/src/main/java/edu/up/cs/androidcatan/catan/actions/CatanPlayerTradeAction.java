package edu.up.cs.androidcatan.catan.actions;

import java.io.Serializable;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/**
 * Requests CPU trade responses or confirms ONE of the engine-issued replies.
 * Bundles describe the HUMAN ACTIVE PLAYER perspective. No resources are
 * transferred until the engine validates the saved response and both hands.
 */
public final class CatanPlayerTradeAction extends GameAction implements Serializable {
    private static final long serialVersionUID = 20261014013L;
    public static final int REQUEST = 1;
    public static final int ACCEPT_RESPONSE = 2;
    public static final int CANCEL = 3;

    private final int kind;
    private final int target; // -1 means all opponents for REQUEST only
    private final long offerId;
    private final int[] give;
    private final int[] want;

    private CatanPlayerTradeAction(GamePlayer actor, int kind, int target,
                                   long offerId, int[] give, int[] want) {
        super(actor);
        this.kind = kind;
        this.target = target;
        this.offerId = offerId;
        this.give = give == null ? null : give.clone();
        this.want = want == null ? null : want.clone();
    }

    public static CatanPlayerTradeAction request(GamePlayer actor, int recipient,
                                                   int[] offer, int[] request) {
        return new CatanPlayerTradeAction(actor, REQUEST, recipient, 0L, offer, request);
    }
    public static CatanPlayerTradeAction accept(GamePlayer actor, long offerId, int partner) {
        return new CatanPlayerTradeAction(actor, ACCEPT_RESPONSE, partner, offerId, null, null);
    }
    public static CatanPlayerTradeAction cancel(GamePlayer actor, long offerId) {
        return new CatanPlayerTradeAction(actor, CANCEL, -1, offerId, null, null);
    }
    public int getKind() { return kind; }
    public int getTarget() { return target; }
    public long getOfferId() { return offerId; }
    public int[] getGive() { return give == null ? null : give.clone(); }
    public int[] getWant() { return want == null ? null : want.clone(); }
}
