package edu.up.cs.androidcatan.game;

/**
 * Simple defensive names for the offline Catan setup. The Android form and
 * old player-table widgets may temporarily disagree during a layout rebuild;
 * a missing hidden value must never make Start New Game unusable.
 */
public final class CatanPlayerNames {
    private CatanPlayerNames() { }

    public static String human(String entered) {
        String name = entered == null ? "" : entered.trim();
        return name.isEmpty() ? "You" : name;
    }

    public static String computer(String entered, int playerNumber) {
        String name = entered == null ? "" : entered.trim();
        return name.isEmpty() ? "Computer " + playerNumber : name;
    }
}
