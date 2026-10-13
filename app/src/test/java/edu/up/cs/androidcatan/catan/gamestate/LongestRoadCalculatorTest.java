package edu.up.cs.androidcatan.catan.gamestate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

public class LongestRoadCalculatorTest {
    private List<Road> roads(int owner, int... nodes) {
        List<Road> list = new ArrayList<>();
        for (int i = 1; i < nodes.length; i++) {
            list.add(new Road(owner, nodes[i - 1], nodes[i]));
        }
        return list;
    }

    @Test public void fiveStraightRoadsQualify() {
        List<Road> rs = roads(0, 0, 1, 2, 3, 4, 5);
        assertEquals(5, LongestRoadCalculator.longestLength(rs, new Building[54], 0));
        assertEquals(0, LongestRoadCalculator.findHolder(rs, new Building[54], -1));
    }

    @Test public void branchDoesNotCountAllEdgesTogether() {
        List<Road> rs = roads(0, 0, 1, 2, 3, 4);
        rs.addAll(roads(0, 2, 5, 6));
        assertEquals(4, LongestRoadCalculator.longestLength(rs, new Building[54], 0));
    }

    @Test public void fullSixEdgeLoopCountsEveryEdgeOnce() {
        List<Road> rs = roads(1, 0, 1, 2, 3, 4, 5, 0);
        assertEquals(6, LongestRoadCalculator.longestLength(rs, new Building[54], 1));
    }

    @Test public void rivalSettlementBreaksRoadAtVertex() {
        List<Road> rs = roads(0, 0, 1, 2, 3, 4, 5);
        Building[] sites = new Building[54];
        sites[2] = new Settlement(1);
        assertEquals(3, LongestRoadCalculator.longestLength(rs, sites, 0));
    }

    @Test public void existingHolderKeepsAwardOnEqualLength() {
        List<Road> rs = roads(0, 0, 1, 2, 3, 4, 5);
        rs.addAll(roads(1, 10, 11, 12, 13, 14, 15));
        assertEquals(0, LongestRoadCalculator.findHolder(rs, new Building[54], 0));
        assertEquals(-1, LongestRoadCalculator.findHolder(rs, new Building[54], -1));
    }

    @Test public void challengerTakesAwardByExceedingExistingHolder() {
        List<Road> rs = roads(0, 0, 1, 2, 3, 4, 5);
        rs.addAll(roads(1, 10, 11, 12, 13, 14, 15, 16));
        assertEquals(1, LongestRoadCalculator.findHolder(rs, new Building[54], 0));
    }
}
