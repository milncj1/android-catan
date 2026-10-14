package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;
import edu.up.cs.androidcatan.catan.gamestate.Board;

import static org.junit.Assert.*;

/** All difficulty tiers share the same underlying dice, costs and move legality. */
public class CatanAiDifficultyTest {

    @Test public void oldSavedAiTypeNumbersKeepTheirOriginalMeaning() {
        assertEquals(CatanAiDifficulty.EASY, CatanAiDifficulty.fromPlayerType(1));
        assertEquals(CatanAiDifficulty.NORMAL, CatanAiDifficulty.fromPlayerType(2));
        assertEquals(CatanAiDifficulty.HARD, CatanAiDifficulty.fromPlayerType(3));
        assertEquals(CatanAiDifficulty.EXPERT, CatanAiDifficulty.fromPlayerType(4));
        assertEquals(CatanAiDifficulty.NORMAL, CatanAiDifficulty.fromPlayerType(-1));
        assertFalse(CatanAiDifficulty.NORMAL.looksAhead());
        assertTrue(CatanAiDifficulty.HARD.looksAhead());
        assertTrue(CatanAiDifficulty.EXPERT.plansOpponentBlocking());
        assertFalse(CatanAiDifficulty.HARD.plansOpponentBlocking());
    }

    @Test public void hardAndExpertUseExtraPublicBoardAnalysisWithoutChangingResources() {
        CatanGameState game = new CatanGameState(4, CatanBoardTopology.Island.CLASSIC);
        Board board = game.getBoard();
        int goodJunction = -1;
        for (int i = 0; i < board.getBuildings().length; i++) {
            if (board.validBuildingLocation(0, true, i)
                    && CatanStrategy.scoreIntersection(game, i, 0) > 25) {
                goodJunction = i;
                break;
            }
        }
        assertTrue(goodJunction >= 0);
        int before = game.getPlayerList().get(0).getTotalResourceCardCount();
        double normal = CatanStrategy.scoreIntersection(game, goodJunction, 0);
        double hard = CatanStrategy.scoreIntersection(game, goodJunction, 0,
                CatanAiDifficulty.HARD);
        double expert = CatanStrategy.scoreIntersection(game, goodJunction, 0,
                CatanAiDifficulty.EXPERT);

        assertTrue("Hard rewards balanced future production", hard > normal);
        assertTrue("Expert considers at least the Hard factors", expert >= hard);
        assertEquals(before, game.getPlayerList().get(0).getTotalResourceCardCount());
        assertEquals(0, game.getBoard().getRoads().size());
        assertEquals("Scoring cannot add settlements", null,
                game.getBoard().getBuildings()[goodJunction]);
    }

    @Test public void allFourLevelsApplyAcrossAllNewBoardSizes() {
        for (CatanBoardTopology.Island island : CatanBoardTopology.Island.values()) {
            CatanGameState game = new CatanGameState(4, island);
            int spot = 0;
            for (CatanAiDifficulty difficulty : CatanAiDifficulty.values()) {
                double score = CatanStrategy.scoreIntersection(game, spot, 1,
                        difficulty);
                assertFalse(Double.isNaN(score));
                assertFalse(Double.isInfinite(score));
            }
        }
    }
}
