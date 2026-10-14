package edu.up.cs.androidcatan.catan.gamestate;

import java.util.ArrayList;
import java.util.List;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;

/**
 * Longest Road follows a trail of unique road EDGES, not unique vertices.
 * An opponent's settlement or city cuts the trail at that intersection.
 *
 * Pure Java to allow unit tests without an Android device.
 */
public final class LongestRoadCalculator {
    private LongestRoadCalculator() { }

    public static int longestLength(List<Road> allRoads, Building[] buildings, int owner) {
        if (allRoads == null || allRoads.isEmpty()) return 0;
        ArrayList<Road> own = new ArrayList<>();
        for (Road road : allRoads) {
            if (road != null && road.getOwnerId() == owner) own.add(road);
        }
        if (own.isEmpty()) return 0;
        boolean[] used = new boolean[own.size()];
        int best = 0;
        for (Road road : own) {
            best = Math.max(best, traverse(own, buildings, road.getIntersectionAId(), 0, used, owner));
            best = Math.max(best, traverse(own, buildings, road.getIntersectionBId(), 0, used, owner));
        }
        return best;
    }

    private static int traverse(List<Road> own, Building[] buildings, int current, int count,
                                boolean[] used, int owner) {
        // You may END at an opponent's intersection, but cannot pass through.
        if (count > 0 && buildings != null && current >= 0 && current < buildings.length
                && buildings[current] != null && buildings[current].getOwnerId() != owner) {
            return count;
        }
        int best = count;
        for (int i = 0; i < own.size(); i++) {
            if (used[i]) continue;
            Road road = own.get(i);
            int other;
            if (road.getIntersectionAId() == current) other = road.getIntersectionBId();
            else if (road.getIntersectionBId() == current) other = road.getIntersectionAId();
            else continue;

            used[i] = true;
            best = Math.max(best, traverse(own, buildings, other, count + 1, used, owner));
            used[i] = false;
        }
        return best;
    }

    /**
     * Holder must have 5+ continuous roads. The current holder retains the
     * award on a tie; without a current eligible holder, a tie leaves it unclaimed.
     */
    public static int findHolder(List<Road> roads, Building[] buildings, int previousHolder) {
        int totalPlayers = 4;
        if (roads != null) for (Road r : roads)
            if (r != null) totalPlayers = Math.max(totalPlayers, r.getOwnerId() + 1);
        if (buildings != null) for (Building b : buildings)
            if (b != null) totalPlayers = Math.max(totalPlayers, b.getOwnerId() + 1);
        totalPlayers = Math.max(totalPlayers, previousHolder + 1);
        int[] lengths = new int[Math.max(4, Math.min(7, totalPlayers))];
        int best = 4;
        int bestPlayer = -1;
        int countBest = 0;
        for (int p = 0; p < lengths.length; p++) {
            lengths[p] = longestLength(roads, buildings, p);
            if (lengths[p] > best) {
                best = lengths[p];
                bestPlayer = p;
                countBest = 1;
            } else if (lengths[p] == best && best > 4) {
                countBest++;
            }
        }
        if (best <= 4) return -1;
        if (previousHolder >= 0 && previousHolder < lengths.length && lengths[previousHolder] == best) {
            return previousHolder;
        }
        return countBest == 1 ? bestPlayer : -1;
    }
}
