package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;

/** Immutable completed-match record. Does not include hidden card inventories. */
public final class CatanCareerRecord {
    public final String id;
    public final long startedAt, finishedAt, activeMillis;
    public final boolean partialStatistics;
    public final String island;
    public final int winner, rounds;
    private final String[] names;
    private final int[] computerTypes, finalScores, dice;
    private final int[][] production, scoreHistory;
    private final int[] playerTrades, bankTrades, roads, settlements, cities, developmentCards;

    public CatanCareerRecord(String id, long startedAt, long finishedAt, long activeMillis,
            boolean partialStatistics, String island, int winner, int rounds,
            String[] names, int[] computerTypes, int[] finalScores, int[] dice,
            int[][] production, int[] playerTrades, int[] bankTrades, int[] roads,
            int[] settlements, int[] cities, int[] developmentCards,
            int[][] scoreHistory) {
        if (id == null || id.length() < 8 || id.length() > 90
                || !id.matches("[a-zA-Z0-9-]+")
                || names == null || names.length < 2 || names.length > 7
                || computerTypes == null || computerTypes.length != names.length
                || finalScores == null || finalScores.length != names.length
                || dice == null || dice.length != 13
                || production == null || production.length != names.length
                || winner < 0 || winner >= names.length || rounds < 0 || rounds > 100000
                || startedAt < 0 || finishedAt < startedAt || activeMillis < 0
                || island == null || island.length() > 60
                || scoreHistory == null || scoreHistory.length > 100000)
            throw new IllegalArgumentException("Invalid Catan career record");
        for (String name : names) if (name == null || name.length() < 1
                || name.length() > 120) throw new IllegalArgumentException("Invalid name");
        for (int[] row : production) if (row == null || row.length != 5)
            throw new IllegalArgumentException("Invalid production");
        for (int[] row : scoreHistory) if (row == null || row.length != names.length)
            throw new IllegalArgumentException("Invalid score history");
        checkCounts(dice);
        checkCounts(finalScores);
        for (int[] row : production) checkCounts(row);
        for (int[] row : scoreHistory) checkCounts(row);
        checkPlayerCounts(playerTrades, names.length);
        checkPlayerCounts(bankTrades, names.length);
        checkPlayerCounts(roads, names.length);
        checkPlayerCounts(settlements, names.length);
        checkPlayerCounts(cities, names.length);
        checkPlayerCounts(developmentCards, names.length);
        this.id = id;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.activeMillis = activeMillis;
        this.partialStatistics = partialStatistics;
        this.island = island;
        this.winner = winner;
        this.rounds = rounds;
        this.names = names.clone();
        this.computerTypes = computerTypes.clone();
        this.finalScores = finalScores.clone();
        this.dice = dice.clone();
        this.production = cloneRows(production);
        this.playerTrades = playerTrades.clone();
        this.bankTrades = bankTrades.clone();
        this.roads = roads.clone();
        this.settlements = settlements.clone();
        this.cities = cities.clone();
        this.developmentCards = developmentCards.clone();
        this.scoreHistory = cloneRows(scoreHistory);
    }

    private static void checkCounts(int[] data) {
        for (int value : data) if (value < 0 || value > 1_000_000)
            throw new IllegalArgumentException("Invalid count");
    }

    private static void checkPlayerCounts(int[] data, int length) {
        if (data == null || data.length != length)
            throw new IllegalArgumentException("Invalid player counts");
        checkCounts(data);
    }

    private static int[][] cloneRows(int[][] rows) {
        int[][] result = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) result[i] = rows[i].clone();
        return result;
    }

    public static CatanCareerRecord finished(CatanGameState game,
                                                String[] names, int[] types, int winner) {
        CatanMatchStats stats = game.getMatchStats();
        int players = game.getPlayerList().size();
        int[] scores = new int[players];
        for (int i = 0; i < players; i++) {
            Player player = game.getPlayerList().get(i);
            scores[i] = player.getVictoryPoints() + player.getVictoryPointsPrivate()
                    + (game.getCurrentLongestRoadPlayerId() == i ? 2 : 0)
                    + (game.getCurrentLargestArmyPlayerId() == i ? 2 : 0);
        }
        ArrayList<int[]> publicHistory = stats.getPublicScoreHistory();
        return new CatanCareerRecord(stats.getMatchId(), stats.getStartedAt(),
                Math.max(System.currentTimeMillis(), stats.getStartedAt()),
                stats.getActiveMillis(), stats.isPartialHistory(),
                game.getBoard().getIsland().name(), winner, stats.getCompletedRounds(),
                names, types, scores, stats.getDiceRolls(), stats.getProduction(),
                stats.getPlayerTrades(), stats.getBankTrades(), stats.getRoads(),
                stats.getSettlements(), stats.getCities(), stats.getDevelopmentCards(),
                publicHistory.toArray(new int[publicHistory.size()][]));
    }

    public int players() { return names.length; }
    public String[] names() { return names.clone(); }
    public int[] computerTypes() { return computerTypes.clone(); }
    public int[] finalScores() { return finalScores.clone(); }
    public int[] dice() { return dice.clone(); }
    public int[][] production() { return cloneRows(production); }
    public int[] playerTrades() { return playerTrades.clone(); }
    public int[] bankTrades() { return bankTrades.clone(); }
    public int[] roads() { return roads.clone(); }
    public int[] settlements() { return settlements.clone(); }
    public int[] cities() { return cities.clone(); }
    public int[] developmentCards() { return developmentCards.clone(); }
    public int[][] publicScoreHistory() { return cloneRows(scoreHistory); }
}
