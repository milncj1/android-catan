package edu.up.cs.androidcatan.catan;

/**
 * Pure board-touch policy. Only a genuinely visible modal panel blocks taps;
 * never use a sticky flag that survives rolling seven, robber discards,
 * cancelled trades or ending a turn.
 */
public final class CatanBoardTapGuard {
    private CatanBoardTapGuard() { }

    public static boolean acceptsTap(boolean humanTurn, boolean discardOpen,
                                     boolean tradeOpen, boolean resourcePickerOpen) {
        return humanTurn && !discardOpen && !tradeOpen && !resourcePickerOpen;
    }
}
