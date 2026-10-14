package edu.up.cs.androidcatan.catan.gamestate;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/** Geometry tests for the optional island sizes; no Android UI or saved game is touched. */
public class CatanBoardTopologyTest {
    @Test public void classicMatchesCurrentNineteenHexIsland() {
        check(CatanBoardTopology.Island.CLASSIC, 19, 54, 72, 30);
    }

    @Test public void expandedIslandHasThirtyHexes() {
        check(CatanBoardTopology.Island.EXPANDED, 30, 81, 110, 40);
    }

    @Test public void customSevenPlayerIslandHasThirtySevenHexes() {
        check(CatanBoardTopology.Island.GRAND, 37, 96, 132, 42);
    }

    private static void check(CatanBoardTopology.Island island, int tiles,
                              int intersections, int roads, int coast) {
        CatanBoardTopology map = new CatanBoardTopology(island);
        assertEquals(tiles, map.getHexes().size());
        assertEquals(intersections, map.getVertices().size());
        assertEquals(roads, map.getEdges().size());
        assertEquals(coast, map.getCoastalEdges().size());
        assertEquals(tiles, island.getHexCount());

        int[] edgeUseCounts = new int[roads];
        for (CatanBoardTopology.Hex hex : map.getHexes()) {
            assertEquals(6, hex.getCorners().length);
            assertEquals(6, hex.getEdges().length);
            Set<Integer> distinctCorners = new HashSet<>();
            for (int vertex : hex.getCorners()) {
                assertTrue(vertex >= 0 && vertex < intersections);
                distinctCorners.add(vertex);
            }
            assertEquals("No duplicated corner inside a hex", 6, distinctCorners.size());
            for (int edge : hex.getEdges()) {
                assertTrue(edge >= 0 && edge < roads);
                edgeUseCounts[edge]++;
            }
        }

        Set<Long> undirectedEdges = new HashSet<>();
        for (CatanBoardTopology.Edge edge : map.getEdges()) {
            assertTrue(edge.from >= 0 && edge.from < intersections);
            assertTrue(edge.to >= 0 && edge.to < intersections);
            assertNotEquals("No zero-length road", edge.from, edge.to);
            assertTrue("No duplicate road", undirectedEdges.add(
                    ((long) edge.from << 32) | (edge.to & 0xffffffffL)));
            assertEquals(edge.isCoastal() ? 1 : 2, edgeUseCounts[edge.id]);
            assertTrue(map.getVertexNeighbours().get(edge.from).contains(edge.to));
            assertTrue(map.getVertexNeighbours().get(edge.to).contains(edge.from));
            if (!edge.isCoastal()) {
                assertTrue(map.getHexNeighbours().get(edge.firstHex).contains(edge.secondHex));
                assertTrue(map.getHexNeighbours().get(edge.secondHex).contains(edge.firstHex));
            }
        }

        int degreeSum = 0;
        for (List<Integer> neighbours : map.getVertexNeighbours()) {
            assertTrue("No impossible 4-way land intersection", neighbours.size() <= 3);
            degreeSum += neighbours.size();
        }
        assertEquals("Every road joins exactly two vertices", roads * 2, degreeSum);
    }
}
