package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import edu.up.cs.androidcatan.catan.actions.CatanBuildCityAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement;
import edu.up.cs.androidcatan.game.GameMainActivity;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.infoMsg.GameInfo;

import static org.junit.Assert.*;

/** Classic base-game rules, runnable without an Android phone or a network. */
public class CatanClassicRulesTest {
    private final GamePlayer actingHuman = new GamePlayer() {
        @Override public void gameSetAsGui(GameMainActivity activity) { }
        @Override public void setAsGui(GameMainActivity activity) { }
        @Override public void sendInfo(GameInfo info) { }
        @Override public void start() { }
        @Override public boolean requiresGui() { return true; }
        @Override public boolean supportsGui() { return true; }
    };

    private static CatanGameState normalTurn() {
        CatanGameState state = new CatanGameState();
        state.setSetupPhase(false);
        state.setActionPhase(true);
        state.setCurrentPlayerId(0);
        return state;
    }

    private static Hexagon oneProducingTile(CatanGameState state) {
        Hexagon chosen = null;
        for (Hexagon tile : state.getBoard().getHexagons()) {
            tile.setChitValue(0);
            if (tile.getResourceId() < 5 &&
                    tile.getHexagonId() != state.getBoard().getRobber().getHexagonId())
                chosen = tile;
        }
        assertNotNull(chosen);
        chosen.setChitValue(6);
        state.setActionPhase(false);
        return chosen;
    }

    @Test public void finiteBankAwardsItsLastCardToTheOnlyRecipient() {
        CatanGameState state = normalTurn();
        Hexagon hex = oneProducingTile(state);
        int resource = hex.getResourceId();
        int point = state.getBoard().getHexToIntIdMap().get(hex.getHexagonId()).get(0);
        state.getBoard().addBuilding(point, new City(0));
        state.getPlayerList().get(1).addResourceCard(resource, 18);
        assertEquals(1, state.getBankResourceCount(resource));

        state.produceResources(6);

        assertEquals(1, state.getPlayerList().get(0).getResourceCards()[resource]);
        assertEquals(0, state.getBankResourceCount(resource));
    }

    @Test public void finiteBankPaysNobodyWhenTwoPlayersAreShortOfSameResource() {
        CatanGameState state = normalTurn();
        Hexagon hex = oneProducingTile(state);
        int resource = hex.getResourceId();
        int a = state.getBoard().getHexToIntIdMap().get(hex.getHexagonId()).get(0);
        int b = state.getBoard().getHexToIntIdMap().get(hex.getHexagonId()).get(1);
        state.getBoard().addBuilding(a, new Settlement(0));
        state.getBoard().addBuilding(b, new Settlement(1));
        state.getPlayerList().get(2).addResourceCard(resource, 18);

        state.produceResources(6);

        assertEquals(0, state.getPlayerList().get(0).getResourceCards()[resource]);
        assertEquals(0, state.getPlayerList().get(1).getResourceCards()[resource]);
        assertEquals(1, state.getBankResourceCount(resource));
    }

    @Test public void anEmptyBankCannotAcceptATradeOrTakePayment() {
        CatanGameState state = normalTurn();
        state.getPlayerList().get(0).addResourceCard(0, 4);
        state.getPlayerList().get(1).addResourceCard(1, 19);
        CatanLocalGame game = new CatanLocalGame(state);

        assertFalse(game.makeMove(new CatanTradeWithBankAction(actingHuman, 0, 1)));
        assertEquals(4, state.getPlayerList().get(0).getResourceCards()[0]);
    }

    @Test public void yearOfPlentyCannotCreateResourcesBeyondTheBank() {
        CatanGameState state = normalTurn();
        state.getPlayerList().get(0).getDevelopmentCards().add(2);
        state.getPlayerList().get(1).addResourceCard(1, 18);
        CatanLocalGame game = new CatanLocalGame(state);

        assertTrue(game.makeMove(new CatanUseYearOfPlentyCardAction(actingHuman, 1, 1)));
        // Inspect the authoritative game state without starting the Android threads.
        try {
            java.lang.reflect.Field field = CatanLocalGame.class.getDeclaredField("state");
            field.setAccessible(true);
            CatanGameState result = (CatanGameState) field.get(game);
            assertEquals(1, result.getPlayerList().get(0).getResourceCards()[1]);
            assertEquals(0, result.getBankResourceCount(1));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test public void robberMustBeResolvedBeforeUpgradingCity() {
        CatanGameState state = normalTurn();
        state.setRobberPhase(true);
        state.getBoard().addBuilding(0, new Settlement(0));
        CatanLocalGame game = new CatanLocalGame(state);

        assertFalse(game.makeMove(new CatanBuildCityAction(actingHuman, false, 0, 0)));
    }

    @Test public void robberMustBeResolvedBeforeBuildingSettlement() {
        CatanGameState state = normalTurn();
        state.setRobberPhase(true);
        int to = state.getBoard().getIntersectionAdjacencyList().get(0).get(0);
        state.getBoard().addRoad(0, 0, to);
        assertTrue(state.getBoard().validBuildingLocation(0, false, 0));
        CatanLocalGame game = new CatanLocalGame(state);

        assertFalse(game.makeMove(new CatanBuildSettlementAction(actingHuman, false, 0, 0)));
    }
}
