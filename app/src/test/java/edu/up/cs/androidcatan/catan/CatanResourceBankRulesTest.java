package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;

import static org.junit.Assert.*;

/**
 * Classic CATAN resource supply: 19 cards per type; no serialized bank fields.
 * These JVM tests do not need an emulator or a GitHub Actions build.
 */
public class CatanResourceBankRulesTest {
    private static Hexagon firstProducingTile(Board board) {
        for (Hexagon hex : board.getHexagons()) {
            if (hex.getResourceId() >= 0 && hex.getResourceId() < 5
                    && hex.getChitValue() >= 2 && hex.getChitValue() <= 12) return hex;
        }
        throw new AssertionError("No producing terrain found");
    }

    @Test public void bankStockTracksHandsAndReturnedCards() {
        CatanGameState state = new CatanGameState();
        assertEquals(19, state.getBankResourceCount(0));
        state.getPlayerList().get(0).addResourceCard(0, 19);
        assertEquals(0, state.getBankResourceCount(0));
        assertTrue(state.getPlayerList().get(0).removeResourceCard(0, 4));
        assertEquals(4, state.getBankResourceCount(0));
        assertEquals(0, state.getBankResourceCount(5));
    }

    @Test public void shortageForTwoPlayersPaysNeither() {
        CatanGameState state = new CatanGameState();
        Board board = state.getBoard();
        Hexagon tile = firstProducingTile(board);
        int resource = tile.getResourceId();
        int a = board.getHexToIntIdMap().get(tile.getHexagonId()).get(0);
        int b = board.getHexToIntIdMap().get(tile.getHexagonId()).get(2);
        assertTrue(board.addBuilding(a, new Settlement(0)));
        assertTrue(board.addBuilding(b, new Settlement(1)));
        state.getPlayerList().get(0).addResourceCard(resource, 17);
        state.getPlayerList().get(1).addResourceCard(resource, 1);
        state.setSetupPhase(false);

        state.produceResources(tile.getChitValue());
        assertEquals(17, state.getPlayerList().get(0).getResourceCards()[resource]);
        assertEquals(1, state.getPlayerList().get(1).getResourceCards()[resource]);
        assertEquals(1, state.getBankResourceCount(resource));
    }

    @Test public void shortageForOnePlayerPaysRemainingCards() {
        CatanGameState state = new CatanGameState();
        Board board = state.getBoard();
        Hexagon tile = firstProducingTile(board);
        int resource = tile.getResourceId();
        int point = board.getHexToIntIdMap().get(tile.getHexagonId()).get(0);
        assertTrue(board.addBuilding(point, new City(0)));
        state.getPlayerList().get(0).addResourceCard(resource, 18);
        state.setSetupPhase(false);

        state.produceResources(tile.getChitValue());
        assertEquals(19, state.getPlayerList().get(0).getResourceCards()[resource]);
        assertEquals(0, state.getBankResourceCount(resource));
    }

    @Test public void bankTradeRefusesUnavailableResource() {
        CatanGameState state = new CatanGameState();
        state.setSetupPhase(false);
        state.setActionPhase(true);
        state.getPlayerList().get(0).addResourceCard(0, 4);
        state.getPlayerList().get(1).addResourceCard(1, 19);
        CatanLocalGame game = new CatanLocalGame(state);
        assertFalse(game.makeMove(new CatanTradeWithBankAction(null, 0, 1)));
    }

    @Test public void yearOfPlentyCannotCreateExtraCards() {
        CatanGameState state = new CatanGameState();
        state.setSetupPhase(false);
        state.getPlayerList().get(0).getDevelopmentCards().add(2);
        state.getPlayerList().get(1).addResourceCard(0, 18);
        CatanLocalGame game = new CatanLocalGame(state);
        assertTrue(game.makeMove(new CatanUseYearOfPlentyCardAction(null, 0, 0)));

        try {
            java.lang.reflect.Field field = CatanLocalGame.class.getDeclaredField("state");
            field.setAccessible(true);
            CatanGameState after = (CatanGameState) field.get(game);
            assertEquals(1, after.getPlayerList().get(0).getResourceCards()[0]);
            assertEquals(0, after.getBankResourceCount(0));
            assertFalse(after.getPlayerList().get(0).getDevelopmentCards().contains(2));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
