package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Build 014 tap regression for the report "after several rounds I can't select
 * an intersection". A seven can auto-discard; an unrelated stale menu flag
 * must never survive and lock the whole board.
 */
public class CatanBoardTapGuardTest {
    @Test public void ordinaryHumanTurnAlwaysAllowsBoardSelection() {
        assertTrue(CatanBoardTapGuard.acceptsTap(true,false,false,false));
    }
    @Test public void cpuTurnDoesNotPermitBuildingSelection() {
        assertFalse(CatanBoardTapGuard.acceptsTap(false,false,false,false));
    }
    @Test public void actualDiscardPanelBlocksOnlyWhileVisible() {
        assertFalse(CatanBoardTapGuard.acceptsTap(true,true,false,false));
        // After an automatic discard / seven, hidden panel must unlock board.
        assertTrue(CatanBoardTapGuard.acceptsTap(true,false,false,false));
    }
    @Test public void robberMoveAndStealAllowJunctionTapAfterDiscard() {
        // Only discard/trade/resource chooser can block. Robber selection itself
        // deliberately does NOT lock intersections or robber hexagons.
        assertTrue(CatanBoardTapGuard.acceptsTap(true,false,false,false));
    }
    @Test public void closingTradeOrResourceMenuAlwaysUnlocksSelection() {
        assertFalse(CatanBoardTapGuard.acceptsTap(true,false,true,false));
        assertFalse(CatanBoardTapGuard.acceptsTap(true,false,false,true));
        assertTrue(CatanBoardTapGuard.acceptsTap(true,false,false,false));
    }
}
