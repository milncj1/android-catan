package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

import static org.junit.Assert.*;

/** JVM tests for the content of a saved match, no Android emulator needed. */
public class CatanSaveStoreTest {
    private static final String[] NAMES = {"You", "Ada", "Marie", "Alan"};
    private static final int[] TYPES = {0, 2, 1, 2};

    private CatanSaveStore.Snapshot roundTrip(CatanSaveStore.Snapshot original) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        Object value;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            value = input.readObject();
        }
        assertTrue(value instanceof CatanSaveStore.Snapshot);
        return (CatanSaveStore.Snapshot) value;
    }

    @Test public void snapshotPreservesBoardPlayerResourcesAndTurn() throws Exception {
        CatanGameState current = new CatanGameState();
        current.setSetupPhase(false);
        current.setActionPhase(true);
        current.setCurrentPlayerId(2);
        current.getPlayerList().get(0).addResourceCard(0, 4);
        current.getPlayerList().get(2).addResourceCard(1, 3);
        current.getBoard().addBuilding(4, new Settlement(0));

        CatanSaveStore.Snapshot restored = roundTrip(
                new CatanSaveStore.Snapshot(current, NAMES, TYPES));
        assertTrue(restored.isValid());
        assertArrayEquals(NAMES, restored.getNames());
        assertArrayEquals(TYPES, restored.getTypes());

        CatanGameState game = restored.getState();
        assertEquals(2, game.getCurrentPlayerId());
        assertFalse(game.isSetupPhase());
        assertTrue(game.isActionPhase());
        assertEquals(4, game.getPlayerList().get(0).getResourceCards()[0]);
        assertEquals(3, game.getPlayerList().get(2).getResourceCards()[1]);
        assertEquals(0, game.getBoard().getBuildingAtIntersection(4).getOwnerId());

        for (int i = 0; i < 19; i++) {
            Hexagon before = current.getBoard().getHexagons().get(i);
            Hexagon after = game.getBoard().getHexagons().get(i);
            assertEquals(before.getResourceId(), after.getResourceId());
            assertEquals(before.getChitValue(), after.getChitValue());
        }
        for (int i = 0; i < 9; i++) {
            Port before = current.getBoard().getPortList().get(i);
            Port after = game.getBoard().getPortList().get(i);
            assertEquals(before.getTradeRatio(), after.getTradeRatio());
            assertEquals(before.getResourceId(), after.getResourceId());
        }
    }

    @Test public void saveContainsOwnIndependentCopyOfMatchState() {
        CatanGameState source = new CatanGameState();
        CatanSaveStore.Snapshot snap = new CatanSaveStore.Snapshot(source, NAMES, TYPES);
        source.getPlayerList().get(0).addResourceCard(0, 10);
        assertEquals(0, snap.getState().getPlayerList().get(0).getResourceCards()[0]);
    }

    @Test public void savedRoadsAreRestoredIndependentlyOfOriginalBoard() throws Exception {
        CatanGameState original = new CatanGameState();
        original.getBoard().addRoad(0, 0, 1);
        CatanSaveStore.Snapshot saved = roundTrip(
                new CatanSaveStore.Snapshot(original, NAMES, TYPES));

        CatanGameState restored = saved.getState();
        assertEquals(1, restored.getBoard().getRoads().size());
        assertEquals(0, restored.getBoard().getRoadMatrix()[0][1].getOwnerId());

        // A mutation to a restored copy must not alter another copy or the
        // existing match. Previously Board copied shared road-matrix rows.
        restored.getBoard().getRoadMatrix()[0][1].setOwnerId(1);
        assertEquals(0, original.getBoard().getRoadMatrix()[0][1].getOwnerId());
        assertEquals(0, saved.getState().getBoard().getRoadMatrix()[0][1].getOwnerId());
    }

    @Test public void cpuCanResumeAnOpeningRoadFromSavedSettlement() throws Exception {
        CatanGameState state = new CatanGameState();
        state.setCurrentPlayerId(2);
        state.setCurrentSetupSettlement(7);
        state.setSetupRoadBuiltThisTurn(false);
        state.getBoard().addBuilding(7, new Settlement(2));

        CatanGameState recovered = roundTrip(
                new CatanSaveStore.Snapshot(state, NAMES, TYPES)).getState();
        assertEquals(2, recovered.getCurrentPlayerId());
        assertEquals(7, recovered.getCurrentSetupSettlement());
        assertFalse(recovered.isSetupRoadBuiltThisTurn());
        assertEquals(2, recovered.getBoard().getBuildingAtIntersection(7).getOwnerId());
    }

    @Test public void invalidPlayerConfigurationsCannotBeRestored() {
        assertFalse(new CatanSaveStore.Snapshot(new CatanGameState(),
                NAMES, new int[]{2, 2, 2, 2}).isValid());
        assertFalse(new CatanSaveStore.Snapshot(new CatanGameState(),
                NAMES, new int[]{0, 2, 0, 2}).isValid());
        assertFalse(new CatanSaveStore.Snapshot(new CatanGameState(),
                new String[]{"", "A", "B", "C"}, TYPES).isValid());
    }

    @Test public void saveKeepsRobberAndPhaseProgress() throws Exception {
        CatanGameState game = new CatanGameState();
        game.setSetupPhase(false);
        game.setCurrentPlayerId(3);
        game.setRobberPhase(true);
        game.getRobberPlayerListHasDiscarded()[0] = true;
        game.getRobberPlayerListHasDiscarded()[1] = true;
        int robber = game.getBoard().getRobber().getHexagonId();

        CatanGameState restored = roundTrip(
                new CatanSaveStore.Snapshot(game, NAMES, TYPES)).getState();
        assertEquals(3, restored.getCurrentPlayerId());
        assertTrue(restored.isRobberPhase());
        assertEquals(robber, restored.getBoard().getRobber().getHexagonId());
        assertTrue(restored.getRobberPlayerListHasDiscarded()[0]);
        assertFalse(restored.getRobberPlayerListHasDiscarded()[2]);
    }
}
