package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Regression cases for the complete 7/knight/discard/move/steal UI flow. */
public class CatanRobberStepsTest {
    @Test public void discardOnlyAboveSevenCards() {
        assertEquals(0, CatanRobberSteps.discardCount(0));
        assertEquals(0, CatanRobberSteps.discardCount(7));
        assertEquals(4, CatanRobberSteps.discardCount(8));
        assertEquals(4, CatanRobberSteps.discardCount(9));
        assertEquals(5, CatanRobberSteps.discardCount(10));
    }

    @Test public void exactDiscardAndInventoryLimits() {
        int[] hand = {3,2,1,1,1};
        assertTrue(CatanRobberSteps.discardReady(new int[]{2,1,1,0,0}, hand, 4));
        assertFalse(CatanRobberSteps.discardReady(new int[]{3,2,0,0,0}, hand, 4));
        assertFalse(CatanRobberSteps.discardReady(new int[]{4,0,0,0,0}, hand, 4));
        assertFalse(CatanRobberSteps.discardReady(new int[]{-1,2,2,1,0}, hand, 4));
    }

    @Test public void robberStagesSurviveAuthoritativeStateCopy() {
        CatanGameState state = new CatanGameState();
        state.setRobberPhase(true);
        state.setCurrentPlayerId(0);
        state.getPlayerList().get(0).addResourceCard(0, 8);
        assertEquals(CatanRobberSteps.Step.DISCARD, CatanRobberSteps.current(state, 0));

        state.getRobberPlayerListHasDiscarded()[0] = true;
        assertEquals(CatanRobberSteps.Step.WAIT_FOR_DISCARDS,
                CatanRobberSteps.current(state, 0));
        Arrays.fill(state.getRobberPlayerListHasDiscarded(), true);
        assertEquals(CatanRobberSteps.Step.MOVE_TILE, CatanRobberSteps.current(state, 0));
        CatanGameState restored = new CatanGameState(state);
        assertEquals(CatanRobberSteps.Step.MOVE_TILE, CatanRobberSteps.current(restored, 0));

        restored.setHasMovedRobber(true);
        assertEquals(CatanRobberSteps.Step.PICK_VICTIM,
                CatanRobberSteps.current(restored, 0));
        restored.setRobberPhase(false);
        assertEquals(CatanRobberSteps.Step.NONE, CatanRobberSteps.current(restored, 0));
    }

    @Test public void knightSkipsDiscardStage() {
        CatanGameState state = new CatanGameState();
        state.setRobberPhase(true);
        Arrays.fill(state.getRobberPlayerListHasDiscarded(), true);
        state.setCurrentPlayerId(0);
        assertEquals(CatanRobberSteps.Step.MOVE_TILE, CatanRobberSteps.current(state, 0));
    }

    @Test public void cpuRobberCanStillMakeHumanDiscard() {
        CatanGameState state = new CatanGameState();
        state.setRobberPhase(true);
        state.setCurrentPlayerId(1);
        state.getPlayerList().get(0).addResourceCard(0, 8);
        assertEquals(CatanRobberSteps.Step.DISCARD, CatanRobberSteps.current(state, 0));
        state.getRobberPlayerListHasDiscarded()[0] = true;
        assertEquals(CatanRobberSteps.Step.WAIT_FOR_DISCARDS,
                CatanRobberSteps.current(state, 0));
    }

    @Test public void exactHexPolygonHitTest() {
        int[][] square = {{0,0},{10,0},{10,10},{0,10}};
        assertTrue(CatanRobberSteps.containsHexPoint(5,5,square));
        assertFalse(CatanRobberSteps.containsHexPoint(12,5,square));
        assertFalse(CatanRobberSteps.containsHexPoint(2,11,square));
        assertFalse(CatanRobberSteps.containsHexPoint(3,3,null));
    }
}
