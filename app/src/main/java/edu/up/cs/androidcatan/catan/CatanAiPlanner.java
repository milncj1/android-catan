package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;
import java.util.List;

import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

/**
 * Offline, legal-move strategy evaluator for Hard and Expert AI.
 * Never reads opponents' private resource/development hands or changes game rules.
 * Existing random Easy and balanced Normal computer behaviour is retained.
 */
public final class CatanAiPlanner {
    private CatanAiPlanner() { }
    private static final int[] PIPS = {0, 0, 1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1};

    static double scoreSettlement(CatanGameState game, int vertex, int playerId, boolean expert) {
        Board board = game.getBoard();
        if (vertex < 0 || vertex >= board.getBuildings().length) return -100000;
        double score = CatanStrategy.scoreIntersection(game, vertex, playerId);
        int[] ownedPips = new int[5];
        int[] vertexPips = new int[5];
        int[] numberMask = new int[13];
        for (int v = 0; v < board.getBuildings().length; v++) {
            Building owned = board.getBuildings()[v];
            if (owned == null || owned.getOwnerId() != playerId) continue;
            for (Integer tileId : board.getIntToHexIdMap().get(v)) {
                Hexagon hex = board.getHexagonFromId(tileId);
                int r = hex.getResourceId();
                if (r >= 0 && r < 5) ownedPips[r] += PIPS[hex.getChitValue()];
            }
        }
        for (Integer tileId : board.getIntToHexIdMap().get(vertex)) {
            Hexagon hex = board.getHexagonFromId(tileId);
            int resource = hex.getResourceId();
            if (resource < 0 || resource >= 5) continue;
            int pips = PIPS[hex.getChitValue()];
            vertexPips[resource] += pips;
            numberMask[hex.getChitValue()] = 1;
            if (expert) {
                score += pips * (resource == 1 || resource == 3 ? 2.5 : 1.5);
            } else {
                score += pips * 1.0;
            }
        }
        for (int r = 0; r < 5; r++) {
            if (vertexPips[r] == 0) continue;
            if (ownedPips[r] == 0) score += expert ? 14.0 : 8.0;
            if (expert && ownedPips[r] < 4) score += 4.0;
        }
        if (expert) {
            // Spread probability over different dice outcomes for resilience.
            for (int number = 2; number <= 12; number++)
                if (numberMask[number] > 0) score += 2.0;
            // A 2:1 harbour is valuable only with some matching production.
            for (Port port : board.getPortList()) {
                if (port.getIntersectionA() != vertex && port.getIntersectionB() != vertex)
                    continue;
                int r = port.getResourceId();
                if (r >= 0 && r < 5 && ownedPips[r] + vertexPips[r] >= 5)
                    score += 14.0;
                else if (r < 0) score += 6.0;
            }
        }
        return score;
    }

    static double scoreRoad(CatanGameState game, int vertex, int playerId, boolean expert) {
        Board board = game.getBoard();
        double best = -100000;
        if (board.validBuildingLocation(playerId, true, vertex))
            best = scoreSettlement(game, vertex, playerId, expert);
        for (int neighbour : board.getIntersectionAdjacencyList().get(vertex)) {
            if (board.validBuildingLocation(playerId, true, neighbour))
                best = Math.max(best, scoreSettlement(game, neighbour, playerId, expert)
                        * (expert ? .91 : .76));
        }
        return best < -1000 ? -100 : best;
    }

    static int chooseRobberHex(CatanGameState game, int playerId, boolean expert) {
        Board board = game.getBoard();
        int existing = board.getRobber().getHexagonId();
        int chosen = -1;
        double best = Double.NEGATIVE_INFINITY;
        for (Hexagon tile : board.getHexagons()) {
            int hexId = tile.getHexagonId();
            if (hexId == existing || tile.getResourceId() == 5) continue;
            int enemyBuildings = 0;
            int ownBuildings = 0;
            double enemyThreat = 0;
            for (int vertex : board.getHexToIntIdMap().get(hexId)) {
                Building building = board.getBuildingAtIntersection(vertex);
                if (building == null) continue;
                if (building.getOwnerId() == playerId) ownBuildings++;
                else {
                    enemyBuildings++;
                    Player opponent = game.getPlayerList().get(building.getOwnerId());
                    enemyThreat += 1.0 + opponent.getVictoryPoints()
                            * (expert ? 0.35 : 0.15);
                }
            }
            if (enemyBuildings == 0) continue;
            double score = PIPS[tile.getChitValue()] * (expert ? 2.5 : 1.5)
                    + enemyThreat - ownBuildings * 9;
            if (score > best) {
                best = score;
                chosen = hexId;
            }
        }
        if (chosen >= 0) return chosen;
        for (Hexagon tile : board.getHexagons()) {
            if (tile.getHexagonId() != existing && tile.getResourceId() != 5)
                return tile.getHexagonId();
        }
        return -1;
    }

    /** Resource selection uses public tile production, never secret CPU hands. */
    static int chooseMonopolyResource(CatanGameState game, int playerId) {
        int[] opponentProduction = new int[5];
        Board board = game.getBoard();
        for (int vertex = 0; vertex < board.getBuildings().length; vertex++) {
            Building building = board.getBuildings()[vertex];
            if (building == null || building.getOwnerId() == playerId) continue;
            for (int hexId : board.getIntToHexIdMap().get(vertex)) {
                Hexagon tile = board.getHexagonFromId(hexId);
                if (tile.getResourceId() >= 0 && tile.getResourceId() < 5)
                    opponentProduction[tile.getResourceId()] +=
                            PIPS[tile.getChitValue()] * building.getVictoryPoints();
            }
        }
        int best = 1;
        for (int r = 0; r < 5; r++)
            if (opponentProduction[r] > opponentProduction[best]) best = r;
        return best;
    }

    /** Returns a legal road to construct or null when expansion is blocked. */
    static int[] bestRoad(CatanGameState game, int playerId, boolean expert) {
        Board board = game.getBoard();
        int[] best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int from = 0; from < board.getBuildings().length; from++) {
            Building home = board.getBuildings()[from];
            boolean connected = home != null && home.getOwnerId() == playerId;
            if (!connected) {
                for (Road road : board.getRoads()) {
                    if (road.getOwnerId() == playerId
                            && (road.getIntersectionAId() == from
                                || road.getIntersectionBId() == from)) {
                        connected = true;
                        break;
                    }
                }
            }
            if (!connected) continue;
            for (Integer to : board.getIntersectionAdjacencyList().get(from)) {
                if (!board.validRoadPlacement(playerId, false, from, to)) continue;
                double score = scoreRoad(game, to, playerId, expert);
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{from, to};
                }
            }
        }
        return best;
    }

    static int bestNewSettlement(CatanGameState game, int playerId, boolean expert) {
        Board board = game.getBoard();
        int chosen = -1;
        double best = Double.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < board.getBuildings().length; vertex++) {
            if (!board.validBuildingLocation(playerId, false, vertex)) continue;
            double score = scoreSettlement(game, vertex, playerId, expert);
            if (score > best) { best = score; chosen = vertex; }
        }
        return chosen;
    }

    static int bestCity(CatanGameState game, int playerId, boolean expert) {
        Board board = game.getBoard();
        double best = Double.NEGATIVE_INFINITY;
        int chosen = -1;
        for (int vertex = 0; vertex < board.getBuildings().length; vertex++) {
            if (!board.validCityLocation(playerId, vertex)) continue;
            double score = scoreSettlement(game, vertex, playerId, expert);
            if (score > best) { best = score; chosen = vertex; }
        }
        return chosen;
    }

    /** Trade only a genuine surplus for a missing piece required for a build. */
    static int[] bestBankTrade(CatanGameState game, int playerId, boolean expert) {
        int[] cards = game.getPlayerList().get(playerId).getResourceCards();
        int[][] targets = expert
                ? new int[][] {City.resourceCost, Settlement.resourceCost, Road.resourceCost}
                : new int[][] {Settlement.resourceCost, City.resourceCost, Road.resourceCost};
        for (int[] cost : targets) {
            int needed = -1;
            for (int i = 0; i < 5; i++) {
                if (cards[i] < cost[i]) {
                    if (needed >= 0) { needed = -1; break; }
                    needed = i;
                }
            }
            if (needed < 0 || game.getBankResourceCount(needed) <= 0) continue;
            for (int giving = 0; giving < 5; giving++) {
                if (giving == needed) continue;
                // Never trade away resources needed to complete this build.
                int exchangeRate = CatanTradingRules.bestRate(
                        game, playerId, giving, needed);
                if (exchangeRate > 0 && cards[giving] - cost[giving] >= exchangeRate)
                    return new int[]{giving, needed};
            }
        }
        return null;
    }
}
