package edu.up.cs.androidcatan.catan.gamestate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure Java land topology for selectable island sizes.
 *
 * Coordinates are integer scaled axial hex coordinates. Adjacent hexes share
 * exactly the same two corner positions, so intersections and roads can be
 * generated rather than entered manually. This class does not replace the
 * current 19-hex Board yet: existing serialized match IDs remain untouched.
 */
public final class CatanBoardTopology {

    public enum Island {
        CLASSIC("Classic", new int[]{3, 4, 5, 4, 3}),
        EXPANDED("Expanded", new int[]{3, 4, 5, 6, 5, 4, 3}),
        GRAND("Grand", new int[]{4, 5, 6, 7, 6, 5, 4});

        private final String title;
        private final int[] rowLengths;

        Island(String title, int[] rowLengths) {
            this.title = title;
            this.rowLengths = rowLengths.clone();
        }

        public String getTitle() { return title; }
        public int[] getRowLengths() { return rowLengths.clone(); }
        public int getHexCount() {
            int total = 0;
            for (int n : rowLengths) total += n;
            return total;
        }
    }

    public static final class Hex {
        public final int id;
        public final int q;
        public final int r;
        private final int[] corners;
        private final int[] edges;

        private Hex(int id, int q, int r, int[] corners, int[] edges) {
            this.id = id;
            this.q = q;
            this.r = r;
            this.corners = corners.clone();
            this.edges = edges.clone();
        }

        public int[] getCorners() { return corners.clone(); }
        public int[] getEdges() { return edges.clone(); }
    }

    public static final class Vertex {
        public final int id;
        /** Unit X represents sqrt(3)/2 physical hex-radius. */
        public final int x;
        /** Unit Y represents 1/2 physical hex-radius. */
        public final int y;

        private Vertex(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    public static final class Edge {
        public final int id;
        public final int from;
        public final int to;
        /** First adjacent land hex, or -1 for none. */
        public final int firstHex;
        /** Second adjacent land hex, or -1 when this is a coastal edge. */
        public final int secondHex;

        private Edge(int id, int from, int to, int firstHex, int secondHex) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.firstHex = firstHex;
            this.secondHex = secondHex;
        }

        public boolean isCoastal() { return secondHex < 0; }
    }

    private static final int[][] CORNERS = {
            {0, -2}, {1, -1}, {1, 1}, {0, 2}, {-1, 1}, {-1, -1}
    };

    private final Island island;
    private final List<Hex> hexes;
    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private final List<List<Integer>> vertexNeighbours;
    private final List<List<Integer>> hexNeighbours;
    private final List<Integer> coastalEdges;

    private static final class PendingEdge {
        final int id;
        final int from;
        final int to;
        final int firstHex;
        int secondHex = -1;

        PendingEdge(int id, int from, int to, int firstHex) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.firstHex = firstHex;
        }
    }

    public CatanBoardTopology(Island island) {
        if (island == null) throw new IllegalArgumentException("Island is required");
        this.island = island;

        Map<Long, Integer> vertexIds = new LinkedHashMap<>();
        Map<Long, PendingEdge> edgeIds = new LinkedHashMap<>();
        List<Vertex> generatedVertices = new ArrayList<>();
        List<Hex> generatedHexes = new ArrayList<>();
        int[] rows = island.getRowLengths();

        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            int r = rowIndex - rows.length / 2;
            int qStart = -(rows[rowIndex] - 1 + r) / 2;
            for (int column = 0; column < rows[rowIndex]; column++) {
                int q = qStart + column;
                int centreX = 2 * q + r;
                int centreY = 3 * r;
                int hexId = generatedHexes.size();
                int[] vertexNumbers = new int[6];

                for (int i = 0; i < 6; i++) {
                    int x = centreX + CORNERS[i][0];
                    int y = centreY + CORNERS[i][1];
                    long coordinate = key(x, y);
                    Integer known = vertexIds.get(coordinate);
                    if (known == null) {
                        known = generatedVertices.size();
                        vertexIds.put(coordinate, known);
                        generatedVertices.add(new Vertex(known, x, y));
                    }
                    vertexNumbers[i] = known;
                }

                int[] edgeNumbers = new int[6];
                for (int i = 0; i < 6; i++) {
                    int a = vertexNumbers[i];
                    int b = vertexNumbers[(i + 1) % 6];
                    int from = Math.min(a, b);
                    int to = Math.max(a, b);
                    long edgeKey = key(from, to);
                    PendingEdge edge = edgeIds.get(edgeKey);
                    if (edge == null) {
                        edge = new PendingEdge(edgeIds.size(), from, to, hexId);
                        edgeIds.put(edgeKey, edge);
                    } else {
                        if (edge.secondHex != -1)
                            throw new IllegalStateException("More than two hexes share a road");
                        edge.secondHex = hexId;
                    }
                    edgeNumbers[i] = edge.id;
                }
                generatedHexes.add(new Hex(hexId, q, r, vertexNumbers, edgeNumbers));
            }
        }

        List<Edge> generatedEdges = new ArrayList<>();
        List<Integer> generatedCoast = new ArrayList<>();
        List<List<Integer>> intersections = new ArrayList<>();
        List<List<Integer>> hexAdjacency = new ArrayList<>();
        for (int i = 0; i < generatedVertices.size(); i++)
            intersections.add(new ArrayList<Integer>());
        for (int i = 0; i < generatedHexes.size(); i++)
            hexAdjacency.add(new ArrayList<Integer>());

        for (PendingEdge pending : edgeIds.values()) {
            Edge edge = new Edge(pending.id, pending.from, pending.to,
                    pending.firstHex, pending.secondHex);
            generatedEdges.add(edge);
            intersections.get(edge.from).add(edge.to);
            intersections.get(edge.to).add(edge.from);
            if (edge.isCoastal()) {
                generatedCoast.add(edge.id);
            } else {
                hexAdjacency.get(edge.firstHex).add(edge.secondHex);
                hexAdjacency.get(edge.secondHex).add(edge.firstHex);
            }
        }

        this.hexes = Collections.unmodifiableList(generatedHexes);
        this.vertices = Collections.unmodifiableList(generatedVertices);
        this.edges = Collections.unmodifiableList(generatedEdges);
        this.vertexNeighbours = freeze(intersections);
        this.hexNeighbours = freeze(hexAdjacency);
        this.coastalEdges = Collections.unmodifiableList(generatedCoast);
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    private static List<List<Integer>> freeze(List<List<Integer>> source) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> values : source)
            copy.add(Collections.unmodifiableList(new ArrayList<>(values)));
        return Collections.unmodifiableList(copy);
    }

    public Island getIsland() { return island; }
    public List<Hex> getHexes() { return hexes; }
    public List<Vertex> getVertices() { return vertices; }
    public List<Edge> getEdges() { return edges; }
    public List<Integer> getCoastalEdges() { return coastalEdges; }
    public List<List<Integer>> getVertexNeighbours() { return vertexNeighbours; }
    public List<List<Integer>> getHexNeighbours() { return hexNeighbours; }
}
