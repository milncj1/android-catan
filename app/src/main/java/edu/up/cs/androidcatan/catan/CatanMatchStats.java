package edu.up.cs.androidcatan.catan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Build 014 live match ledger. Mutated ONLY after the game engine accepts an
 * action. Lives in CatanGameState, so atomic Continue Game saves include it.
 * A null ledger in an older save creates a new, explicitly partial history.
 *
 * Only public points are kept in the in-progress chart. Hidden VP cards are
 * never disclosed to opponents through this object.
 */
public final class CatanMatchStats implements Serializable {
    private static final long serialVersionUID = 20261014014L;
    private final String matchId;
    private final long startedAt;
    private final boolean partialHistory;
    private final int playerCount;
    private final int[] diceRolls = new int[13];
    private final int[][] resourceProduction;
    private final int[] playerTrades, bankTrades, roads, settlements, cities, developmentCards;
    private final ArrayList<int[]> publicScoreHistory = new ArrayList<>();
    private int completedRounds;
    private int lastGlobalRound;
    private long activeMillis;
    private boolean completed;

    public CatanMatchStats(int players, boolean partial, int initialGlobalRound) {
        if (players < 2 || players > 7) throw new IllegalArgumentException("players");
        matchId = UUID.randomUUID().toString();
        startedAt = System.currentTimeMillis();
        playerCount = players;
        partialHistory = partial;
        lastGlobalRound = Math.max(0, initialGlobalRound);
        resourceProduction = new int[players][5];
        playerTrades = new int[players];
        bankTrades = new int[players];
        roads = new int[players];
        settlements = new int[players];
        cities = new int[players];
        developmentCards = new int[players];
    }

    public CatanMatchStats(CatanMatchStats source) {
        matchId = source.matchId;
        startedAt = source.startedAt;
        partialHistory = source.partialHistory;
        playerCount = source.playerCount;
        System.arraycopy(source.diceRolls, 0, diceRolls, 0, diceRolls.length);
        resourceProduction = new int[playerCount][5];
        for (int p = 0; p < playerCount; p++)
            System.arraycopy(source.resourceProduction[p], 0, resourceProduction[p], 0, 5);
        playerTrades = source.playerTrades.clone();
        bankTrades = source.bankTrades.clone();
        roads = source.roads.clone();
        settlements = source.settlements.clone();
        cities = source.cities.clone();
        developmentCards = source.developmentCards.clone();
        for (int[] round : source.publicScoreHistory)
            publicScoreHistory.add(round.clone());
        completedRounds = source.completedRounds;
        lastGlobalRound = source.lastGlobalRound;
        activeMillis = source.activeMillis;
        completed = source.completed;
    }

    public String getMatchId() { return matchId; }
    public long getStartedAt() { return startedAt; }
    public boolean isPartialHistory() { return partialHistory; }
    public int getPlayerCount() { return playerCount; }
    public int getCompletedRounds() { return completedRounds; }
    public long getActiveMillis() { return activeMillis; }
    public boolean isCompleted() { return completed; }
    public int[] getDiceRolls() { return diceRolls.clone(); }
    public int[][] getProduction() {
        int[][] copy = new int[playerCount][5];
        for (int p = 0; p < playerCount; p++)
            copy[p] = resourceProduction[p].clone();
        return copy;
    }
    public int[] getPlayerTrades() { return playerTrades.clone(); }
    public int[] getBankTrades() { return bankTrades.clone(); }
    public int[] getRoads() { return roads.clone(); }
    public int[] getSettlements() { return settlements.clone(); }
    public int[] getCities() { return cities.clone(); }
    public int[] getDevelopmentCards() { return developmentCards.clone(); }
    public ArrayList<int[]> getPublicScoreHistory() {
        ArrayList<int[]> copy = new ArrayList<>();
        for (int[] line : publicScoreHistory) copy.add(line.clone());
        return copy;
    }

    /** Count production ONLY from an accepted roll; trades are not production. */
    void recordRoll(int sum, int[][] handsBefore, CatanGameState state) {
        if (completed || sum < 2 || sum > 12 || handsBefore == null
                || handsBefore.length != playerCount) return;
        diceRolls[sum]++;
        if (sum == 7) return;
        for (int p = 0; p < playerCount; p++) {
            if (handsBefore[p] == null || handsBefore[p].length != 5) continue;
            int[] after = state.getPlayerList().get(p).getResourceCards();
            for (int r = 0; r < 5; r++)
                resourceProduction[p][r] += Math.max(0, after[r] - handsBefore[p][r]);
        }
    }

    void recordPlayerTrade(int proposer, int partner) {
        if (completed) return;
        if (validPlayer(proposer)) playerTrades[proposer]++;
        if (validPlayer(partner)) playerTrades[partner]++;
    }

    void recordBankTrade(int actor) {
        if (!completed && validPlayer(actor)) bankTrades[actor]++;
    }

    static final int ROAD = 0, SETTLEMENT = 1, CITY = 2, DEVELOPMENT = 3;
    void recordBuild(int actor, int kind) {
        if (completed || !validPlayer(actor)) return;
        switch (kind) {
            case ROAD: roads[actor]++; break;
            case SETTLEMENT: settlements[actor]++; break;
            case CITY: cities[actor]++; break;
            case DEVELOPMENT: developmentCards[actor]++; break;
            default: break;
        }
    }

    /** Called after a completed round, not once per player turn. */
    void recordRound(CatanGameState state) {
        if (completed) return;
        int globalRound = state.getMarketState().getCompletedRounds();
        if (globalRound <= lastGlobalRound) return;
        lastGlobalRound = globalRound;
        completedRounds++;
        int[] publicScores = new int[playerCount];
        for (int p = 0; p < playerCount; p++) {
            Player player = state.getPlayerList().get(p);
            publicScores[p] = player.getVictoryPoints()
                    + (state.getCurrentLongestRoadPlayerId() == p ? 2 : 0)
                    + (state.getCurrentLargestArmyPlayerId() == p ? 2 : 0);
        }
        publicScoreHistory.add(publicScores);
    }

    void addActiveMillis(long duration) {
        if (!completed && duration > 0 && duration <= 60_000)
            activeMillis += duration;
    }

    void markCompleted() { completed = true; }

    private boolean validPlayer(int p) { return p >= 0 && p < playerCount; }
}
