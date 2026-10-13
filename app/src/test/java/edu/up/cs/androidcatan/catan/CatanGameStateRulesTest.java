package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

/**
 * Regression checks for classic rules, runnable as local JVM JUnit tests.
 */
public class CatanGameStateRulesTest {
    @Test public void newBoardsHaveValidRandomClassicIslandAndPorts() {
        java.util.HashSet<String> layouts = new java.util.HashSet<>();
        for (int run = 0; run < 5; run++) {
            Board board = new Board();
            assertEquals(19, board.getHexagons().size());
            int[] terrain = new int[6];
            int[] numbers = new int[13];
            StringBuilder signature = new StringBuilder();
            int desertId = -1;
            for (Hexagon tile : board.getHexagons()) {
                int type = tile.getResourceId(), chit = tile.getChitValue();
                assertTrue(type >= 0 && type <= 5);
                assertTrue(chit >= 0 && chit < numbers.length);
                terrain[type]++;
                if (type == 5) {
                    assertEquals(0, chit);
                    desertId = tile.getHexagonId();
                } else numbers[chit]++;
                signature.append(type).append(':').append(chit).append(',');
            }
            assertArrayEquals(new int[]{3, 4, 4, 3, 4, 1}, terrain);
            assertArrayEquals(new int[]{0, 0, 1, 2, 2, 2, 2, 0, 2, 2, 2, 2, 1}, numbers);
            assertEquals(desertId, board.getRobber().getHexagonId());
            assertTrue(board.checkChitRule());

            assertEquals(9, board.getPortList().size());
            int[] ports = new int[6]; // five distinct 2:1 resource ports + four 3:1
            for (edu.up.cs.androidcatan.catan.gamestate.Port port : board.getPortList()) {
                if (port.getResourceId() == -1) {
                    assertEquals(3, port.getTradeRatio());
                    ports[5]++;
                } else {
                    assertEquals(2, port.getTradeRatio());
                    ports[port.getResourceId()]++;
                }
                signature.append(port.getResourceId()).append(';');
            }
            assertArrayEquals(new int[]{1, 1, 1, 1, 1, 4}, ports);
            Board copy = new Board(board);
            for (int p = 0; p < 9; p++) {
                assertEquals(board.getPortList().get(p).getResourceId(),
                        copy.getPortList().get(p).getResourceId());
                assertEquals(board.getPortList().get(p).getTradeRatio(),
                        copy.getPortList().get(p).getTradeRatio());
            }
            layouts.add(signature.toString());
        }
        assertTrue("Every new game should not reuse the same island", layouts.size() > 1);
    }

    @Test public void developmentDeckHasExactlyTwentyFiveCardsAndNeverRefills() {
        CatanGameState game = new CatanGameState();
        int[] cards = new int[5];
        for (int n = 0; n < 25; n++) {
            assertTrue(game.hasDevelopmentCardsAvailable());
            int card = game.getRandomDevCard();
            assertTrue(card >= 0 && card < cards.length);
            cards[card]++;
        }
        assertArrayEquals(new int[]{14, 5, 2, 2, 2}, cards);
        assertFalse(game.hasDevelopmentCardsAvailable());
        assertEquals(-1, game.getRandomDevCard());
    }

    @Test public void robberRequiresExactlyHalfOfCardsAndRejectsNegativeAmounts() {
        CatanGameState game = new CatanGameState();
        game.setRobberPhase(true);
        Player player = game.getPlayerList().get(0);
        player.addResourceCard(0, 8);
        assertFalse(game.validDiscard(0, new int[]{-1, 0, 0, 0, 0}));
        assertFalse(game.validDiscard(0, new int[]{3, 0, 0, 0, 0}));
        assertFalse(game.validDiscard(0, new int[]{5, 0, 0, 0, 0}));
        assertTrue(game.validDiscard(0, new int[]{4, 0, 0, 0, 0}));
        assertFalse(game.discardResources(0, new int[]{5, 0, 0, 0, 0}));
        assertEquals(8, player.getResourceCards()[0]);
        assertTrue(game.discardResources(0, new int[]{4, 0, 0, 0, 0}));
        assertEquals(4, player.getResourceCards()[0]);
        assertFalse(game.discardResources(0, new int[]{0, 0, 0, 0, 0}));
    }

    @Test public void roadPlacementRejectsInvalidVertexIdsWithoutCrashing() {
        Board board = new Board();
        assertFalse(board.validRoadPlacement(0, false, -1, 1));
        assertFalse(board.validRoadPlacement(0, false, 0, -1));
        assertFalse(board.validRoadPlacement(0, false, 54, 1));
        assertFalse(board.validRoadPlacement(0, false, 1, 1));
        assertFalse(board.validRoadPlacement(-1, false, 0, 1));
    }

    @Test public void robberBlocksResourceProductionOnOccupiedTile() {
        CatanGameState original = new CatanGameState();
        Board board = original.getBoard();
        Hexagon chosen = null;
        for (Hexagon tile : board.getHexagons()) {
            if (tile.getResourceId() >= 0 && tile.getResourceId() < 5
                    && tile.getChitValue() >= 2 && tile.getChitValue() <= 12
                    && tile.getHexagonId() != board.getRobber().getHexagonId()) {
                chosen = tile;
                break;
            }
        }
        assertNotNull(chosen);
        int point = board.getHexToIntIdMap().get(chosen.getHexagonId()).get(0);
        assertTrue(board.addBuilding(point, new Settlement(0)));
        original.setSetupPhase(false);
        CatanGameState blocked = new CatanGameState(original);
        assertTrue(blocked.getBoard().moveRobber(chosen.getHexagonId()));
        CatanGameState open = new CatanGameState(original);

        blocked.produceResources(chosen.getChitValue());
        open.produceResources(chosen.getChitValue());
        int type = chosen.getResourceId();
        assertTrue(open.getPlayerList().get(0).getResourceCards()[type]
                > blocked.getPlayerList().get(0).getResourceCards()[type]);
    }

    @Test public void openingTurnStateSurvivesDeepCopy() {
        CatanGameState game = new CatanGameState();
        game.setCurrentSetupSettlement(8);
        game.setSetupRoadBuiltThisTurn(true);
        CatanGameState copy = new CatanGameState(game);
        assertEquals(8, copy.getCurrentSetupSettlement());
        assertTrue(copy.isSetupRoadBuiltThisTurn());
        copy.setSetupRoadBuiltThisTurn(false);
        assertTrue(game.isSetupRoadBuiltThisTurn());
    }

    @Test public void stolenResourcesComeOnlyFromHeldCardTypes() {
        Player player = new Player(0);
        player.addResourceCard(0, 5);
        for (int n = 0; n < 50; n++) assertEquals(0, player.getRandomCard());
        assertEquals(5, player.getTotalResourceCardCount());
    }
}
