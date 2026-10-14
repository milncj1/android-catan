package edu.up.cs.androidcatan.catan.actions;

import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.actionMsg.GameAction;

/** The ORIGINAL active proposer chooses one accepted proposal or cancels. */
public final class CatanTradeChooseAction extends GameAction {
    private static final long serialVersionUID = 20261014015L;
    private final long offerId;
    private final int selectedPartner; // -1 cancels; otherwise exact respondent
    public CatanTradeChooseAction(GamePlayer player, long offerId, int selectedPartner) {
        super(player);
        this.offerId = offerId;
        this.selectedPartner = selectedPartner;
    }
    public long getOfferId() { return offerId; }
    public int getSelectedPartner() { return selectedPartner; }
}
