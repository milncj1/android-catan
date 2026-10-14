package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;
import java.util.List;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;

/** UI-only robber step calculation from authoritative, persisted game state. */
public final class CatanRobberSteps {
    public enum Step { NONE, DISCARD, WAIT_FOR_DISCARDS, MOVE_TILE, PICK_VICTIM }
    private CatanRobberSteps() { }

    public static int discardCount(int cardsInHand) {
        return cardsInHand > 7 ? cardsInHand / 2 : 0;
    }

    public static Step current(CatanGameState state, int player) {
        if (state == null || !state.isRobberPhase() || player < 0
                || player >= state.getPlayerList().size()) return Step.NONE;
        if (!state.getRobberPlayerListHasDiscarded()[player]) {
            return discardCount(state.getPlayerList().get(player)
                    .getTotalResourceCardCount()) > 0 ? Step.DISCARD : Step.WAIT_FOR_DISCARDS;
        }
        if (state.getCurrentPlayerId() != player || !state.allPlayersHaveDiscarded())
            return Step.WAIT_FOR_DISCARDS;
        return state.getHasMovedRobber() ? Step.PICK_VICTIM : Step.MOVE_TILE;
    }

    /** Unique eligible players beside the robber, not intersections. */
    public static List<Integer> victims(CatanGameState state, int actor) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (state == null || actor < 0 || actor >= state.getPlayerList().size()) return ids;
        for (int opponent = 0; opponent < state.getPlayerList().size(); opponent++)
            if (state.isRobberVictim(actor, opponent)) ids.add(opponent);
        return ids;
    }

    public static int selectedTotal(int[] selections) {
        if (selections == null || selections.length != 5) return -1;
        int sum = 0;
        for (int x : selections) { if (x < 0) return -1; sum += x; }
        return sum;
    }

    /** True polygon test; selecting a robber hex must not snap to its junctions. */
    public static boolean containsHexPoint(float x, float y, int[][] vertices) {
        if (vertices == null || vertices.length < 3) return false;
        boolean inside = false;
        for (int i = 0, j = vertices.length - 1; i < vertices.length; j = i++) {
            if (vertices[i] == null || vertices[j] == null
                    || vertices[i].length < 2 || vertices[j].length < 2)
                return false;
            float xi = vertices[i][0], yi = vertices[i][1];
            float xj = vertices[j][0], yj = vertices[j][1];
            if ((yi > y) != (yj > y)
                    && x < (xj - xi) * (y - yi) / (yj - yi) + xi)
                inside = !inside;
        }
        return inside;
    }

    public static boolean discardReady(int[] selected, int[] available, int required) {
        if (available == null || available.length != 5
                || selectedTotal(selected) != required) return false;
        for (int i = 0; i < 5; i++)
            if (selected[i] > available[i]) return false;
        return true;
    }
}
