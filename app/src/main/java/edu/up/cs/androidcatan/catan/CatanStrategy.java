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
