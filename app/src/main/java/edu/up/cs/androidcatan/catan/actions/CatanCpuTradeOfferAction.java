package edu.up.cs.androidcatan.catan.actions;

import java.io.Serializable;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/** Computer proposes a trade on ITS OWN turn. The engine pauses safely. */
public final class CatanCpuTradeOfferAction extends GameAction implements Serializable {
    private static final long serialVersionUID = 20261014016L;
    private final int[] giving, wanting; // from the CPU's viewpoint
    public CatanCpuTradeOfferAction(GamePlayer cpu, int[] giving, int[] wanting) {
        super(cpu);
        this.giving = giving.clone();
        this.wanting = wanting.clone();
    }
    public int[] getGiving() { return giving.clone(); }
    public int[] getWanting() { return wanting.clone(); }
}
