package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import java.util.ArrayList;

/**
 * Small, offline, deterministic strategy evaluator for legal Catan moves.
 * Inspired by general pip-count and resource-diversity strategies.
 *
 * This scores decisions only: the original game engine remains responsible
 * for move legality, resources, and turn order.
 */
public final class CatanStrategy {
    private static final int[] PIPS = {0, 0, 1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1};

    private CatanStrategy() { }

    /** Expected production and useful resource mix at one intersection. */
    public static double scoreIntersection(CatanGameState game, int vertex, int playerId) {
        Board board = game.getBoard();
        if (vertex < 0 || vertex >= board.getBuildings().length) return -100000.0;

        ArrayList<Integer> hexIds = board.getIntToHexIdMap().get(vertex);
        if (hexIds == null || hexIds.isEmpty()) return -100000.0;

        int[] production = new int[5];
        int[] existing = new int[5];
        for (int point = 0; point < board.getBuildings().length; point++) {
            Building own = board.getBuildings()[point];
            if (own != null && own.getOwnerId() == playerId) {
                for (Integer hid : board.getIntToHexIdMap().get(point)) {
                    Hexagon tile = board.getHexagonFromId(hid);
                    int resource = tile.getResourceId();
                    if (resource >= 0 && resource < 5) {
                        existing[resource] += PIPS[tile.getChitValue()];
                    }
                }
            }
        }

        double score = 0.0;
        int different = 0;
        for (Integer hid : hexIds) {
            Hexagon tile = board.getHexagonFromId(hid);
            if (tile == null) continue;
            int resource = tile.getResourceId();
            if (resource < 0 || resource >= 5) continue;
            int pips = PIPS[tile.getChitValue()];
            production[resource] += pips;
            score += pips * 5.0;
        }

        for (int resource = 0; resource < 5; resource++) {
            if (production[resource] <= 0) continue;
            different++;
            if (existing[resource] == 0) {
                score += 6.0; // collect a new type rather than duplicate another 8
            } else {
                score += 2.0;
            }
        }
        score += different * 3.0;
        // Road materials are useful early; still favour total production.
        score += production[0] * 0.6 + production[2] * 0.6;
        return score;
    }

    /**
     * Hard/Expert evaluate the same legal board using additional public facts.
     * Neither mode receives extra cards, production or secret opponent hands.
     */
    public static double scoreIntersection(CatanGameState game, int vertex,
                                           int playerId, CatanAiDifficulty level) {
        double base = scoreIntersection(game, vertex, playerId);
        if (level == null || !level.looksAhead() || base < -90000) return base;

        Board board = game.getBoard();
        int[] ownedProduction = new int[5];
        int[] candidateProduction = new int[5];
        for (int point = 0; point < board.getBuildings().length; point++) {
            Building own = board.getBuildings()[point];
            if (own == null || own.getOwnerId() != playerId) continue;
            for (Integer tileId : board.getIntToHexIdMap().get(point)) {
                Hexagon tile = board.getHexagonFromId(tileId);
                if (tile.getResourceId() < 5)
                    ownedProduction[tile.getResourceId()] += PIPS[tile.getChitValue()];
            }
        }
        for (Integer tileId : board.getIntToHexIdMap().get(vertex)) {
            Hexagon tile = board.getHexagonFromId(tileId);
            if (tile.getResourceId() < 5)
                candidateProduction[tile.getResourceId()] += PIPS[tile.getChitValue()];
        }

        // Filling holes in the production mix helps afford complete builds
        // instead of simply accumulating a single strong number.
        double balanced = 0;
        for (int resource = 0; resource < 5; resource++) {
            int production = candidateProduction[resource];
            if (production == 0) continue;
            if (ownedProduction[resource] == 0) balanced += production * 1.7;
            else if (ownedProduction[resource] < 5) balanced += production * 0.55;
        }
        base += balanced;

        // Harbours are particularly valuable when production is concentrated.
        for (edu.up.cs.androidcatan.catan.gamestate.Port harbour : board.getPortList()) {
            if (harbour.getIntersectionA() != vertex
                    && harbour.getIntersectionB() != vertex) continue;
            if (harbour.getResourceId() < 0) base += 5;
            else base += 3 + candidateProduction[harbour.getResourceId()] * 1.7
                    + ownedProduction[harbour.getResourceId()] * 0.35;
        }

        if (level.plansOpponentBlocking()) {
            // Expert values a strong contested junction more highly than a
            // similar uncontested one: taking it can block a rival's roads.
            for (edu.up.cs.androidcatan.catan.gamestate.buildings.Road road
                    : board.getRoads()) {
                if (road.getOwnerId() != playerId
                        && (road.getIntersectionAId() == vertex
                            || road.getIntersectionBId() == vertex)) {
                    base += 8;
                    break;
                }
            }
            // Reward three-resource intersections with multiple reliable numbers.
            int diversity = 0;
            for (int pips : candidateProduction) if (pips > 0) diversity++;
            if (diversity >= 3) base += 6;
        }
        return base;
    }

    /**
     * Hard looks one settlement ahead; Expert additionally considers the
     * strongest site two new road segments away.
     */
    public static double scoreRoadDestination(CatanGameState game, int vertex,
                                              int playerId, CatanAiDifficulty level) {
        if (level == null || !level.looksAhead())
            return scoreRoadDestination(game, vertex, playerId);
        Board board = game.getBoard();
        double best = -100000;
        if (board.validBuildingLocation(playerId, true, vertex))
            best = scoreIntersection(game, vertex, playerId, level) + 10;
        for (Integer neighbour : board.getIntersectionAdjacencyList().get(vertex)) {
            if (board.validBuildingLocation(playerId, true, neighbour))
                best = Math.max(best,
                        scoreIntersection(game, neighbour, playerId, level) * 0.82);
            if (level.plansOpponentBlocking()) {
                for (Integer next : board.getIntersectionAdjacencyList().get(neighbour)) {
                    if (next == vertex) continue;
                    if (board.validBuildingLocation(playerId, true, next))
                        best = Math.max(best,
                                scoreIntersection(game, next, playerId, level) * 0.53);
                }
            }
        }
        return best;
    }

    /** Choose a road end with room to expand to valuable future sites. */
    public static double scoreRoadDestination(CatanGameState game, int vertex, int playerId) {
        Board board = game.getBoard();
        double score = 0.0;
        if (board.validBuildingLocation(playerId, true, vertex)) {
            score = scoreIntersection(game, vertex, playerId) + 10.0;
        }
        for (Integer neighbour : board.getIntersectionAdjacencyList().get(vertex)) {
            if (board.validBuildingLocation(playerId, true, neighbour)) {
                score = Math.max(score, scoreIntersection(game, neighbour, playerId) * 0.8);
            }
        }
        return score;
    }
}
