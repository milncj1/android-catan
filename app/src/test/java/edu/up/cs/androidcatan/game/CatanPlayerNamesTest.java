package edu.up.cs.androidcatan.game;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Regression for a hidden/blank legacy player row blocking Start New Game. */
public class CatanPlayerNamesTest {
    @Test public void visibleHumanNameIsUsed() {
        assertEquals("Chris", CatanPlayerNames.human(" Chris "));
    }

    @Test public void blankOrMissingHumanNameCannotBlockGame() {
        assertEquals("You", CatanPlayerNames.human(""));
        assertEquals("You", CatanPlayerNames.human("   "));
        assertEquals("You", CatanPlayerNames.human(null));
    }

    @Test public void hiddenComputerNamesAlsoAlwaysHaveDefaults() {
        assertEquals("Computer 1", CatanPlayerNames.computer("", 1));
        assertEquals("Computer 6", CatanPlayerNames.computer(null, 6));
        assertEquals("CPU George", CatanPlayerNames.computer(" CPU George ", 2));
    }
}
