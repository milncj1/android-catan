package edu.up.cs.androidcatan.catan;

/**
 * Four genuine AI levels using the same rules, resource supply and dice.
 *
 * Important: the original stored player type indexes must keep their meaning.
 * Human = 0, original random CPU = 1, original strategic CPU = 2,
 * stronger strategic CPU = 3, expert strategic CPU = 4.
 */
public enum CatanAiDifficulty {
    EASY(1, "Easy"),
    NORMAL(2, "Normal"),
    HARD(3, "Hard"),
    EXPERT(4, "Expert");

    public final int playerType;
    public final String label;

    CatanAiDifficulty(int playerType, String label) {
        this.playerType = playerType;
        this.label = label;
    }

    public static CatanAiDifficulty fromPlayerType(int playerType) {
        for (CatanAiDifficulty value : values())
            if (value.playerType == playerType) return value;
        return NORMAL;
    }

    public boolean looksAhead() { return this == HARD || this == EXPERT; }
    public boolean plansOpponentBlocking() { return this == EXPERT; }
}
