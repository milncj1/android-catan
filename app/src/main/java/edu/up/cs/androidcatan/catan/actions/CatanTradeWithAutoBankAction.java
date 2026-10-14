package edu.up.cs.androidcatan.catan.actions;

import java.io.Serializable;

import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/**
 * Ask the game engine to use the best LEGAL bank/owned-harbour/market rate.
 * The actor supplies no arbitrary rate or harbour object; the game decides.
 */
public final class CatanTradeWithAutoBankAction extends GameAction implements Serializable {
    private static final long serialVersionUID = 20261014012L;
    private final int giving;
    private final int receiving;
    private transient int executedRate = -1;

    public CatanTradeWithAutoBankAction(GamePlayer player, int giving, int receiving) {
        super(player);
        this.giving = giving;
        this.receiving = receiving;
    }
    public int getGiving() { return giving; }
    public int getReceiving() { return receiving; }
    public int getExecutedRate() { return executedRate; }
    public void setExecutedRate(int rate) { executedRate = rate; }
}
