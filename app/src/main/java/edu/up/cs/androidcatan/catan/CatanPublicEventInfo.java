package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

/**
 * An accepted, public move notification for the human turn feed.
 * Never includes another player's private cards or resource inventory.
 * This is not part of the serialized save-game schema.
 */
public final class CatanPublicEventInfo extends GameInfo {
    private static final long serialVersionUID = 20261013007L;
    private final String description;
    private final boolean diceRoll;

    public CatanPublicEventInfo(String description, boolean diceRoll) {
        this.description = description;
        this.diceRoll = diceRoll;
    }

    public String getDescription() { return description; }
    public boolean isDiceRoll() { return diceRoll; }
}
