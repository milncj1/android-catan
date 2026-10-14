package edu.up.cs.androidcatan.catan.actions;

import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/** Active player starts a proposed resource exchange, with one or more opponents. */
public final class CatanTradeOfferAction extends GameAction {
    private static final long serialVersionUID = 20261014013L;
    private final int[] targets, giving, receiving;
    public CatanTradeOfferAction(GamePlayer proposer, int[] targets, int[] giving, int[] receiving) {
        super(proposer);
        this.targets = targets == null ? null : targets.clone();
        this.giving = giving == null ? null : giving.clone();
        this.receiving = receiving == null ? null : receiving.clone();
    }
    public int[] getTargets() { return targets == null ? null : targets.clone(); }
    public int[] getGiving() { return giving == null ? null : giving.clone(); }
    public int[] getReceiving() { return receiving == null ? null : receiving.clone(); }
}
