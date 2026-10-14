package edu.up.cs.androidcatan.catan;

import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;

/**
 * One rules authority for human, CPU and market exchanges. No UI knowledge.
 * Trading must be called by the LocalGame on its game thread.
 */
public final class CatanTradingRules {
    private CatanTradingRules() { }

    public static final int INVALID_RATE = -1;
    private static final int RESOURCES = 5;

    public static boolean ownsAnyHarbour(CatanGameState game, int playerId) {
        if (game == null || game.getBoard() == null) return false;
        for (Port port : game.getBoard().getPortList())
            if (ownsHarbour(game, playerId, port)) return true;
        return false;
    }

    public static boolean ownsHarbour(CatanGameState game, int playerId, Port port) {
        if (game == null || port == null || playerId < 0
                || playerId >= game.getPlayerList().size()) return false;
        // Only ports on the actual board count, never untrusted action data.
        for (Port actual : game.getBoard().getPortList()) {
            if (actual.getTradeRatio() != port.getTradeRatio()
                    || actual.getResourceId() != port.getResourceId()) continue;
            int a = actual.getIntersectionA(), b = actual.getIntersectionB();
            boolean same = (a == port.getIntersectionA() && b == port.getIntersectionB())
                    || (a == port.getIntersectionB() && b == port.getIntersectionA());
            if (!same) continue;
            Building[] buildings = game.getBoard().getBuildings();
            for (int vertex : new int[]{a, b})
                if (vertex >= 0 && vertex < buildings.length
                        && buildings[vertex] != null
                        && buildings[vertex].getOwnerId() == playerId) return true;
        }
        return false;
    }

    /** Cheapest ORIGINAL bank or owned harbour exchange, ignoring events. */
    public static int standardRate(CatanGameState game, int playerId, int giving) {
        if (game == null || !validResource(giving) || playerId < 0
                || playerId >= game.getPlayerList().size()) return INVALID_RATE;
        int best = 4;
        for (Port port : game.getBoard().getPortList()) {
            if (!ownsHarbour(game, playerId, port)) continue;
            if (port.getResourceId() == -1 || port.getResourceId() == giving)
                best = Math.min(best, port.getTradeRatio());
        }
        return best;
    }

    public static int bestRate(CatanGameState game, int playerId,
                               int giving, int receiving) {
        if (game == null || !validResource(giving) || !validResource(receiving)
                || giving == receiving || game.getBankResourceCount(receiving) < 1)
            return INVALID_RATE;
        CatanMarketState market = game.getMarketState();
        if (market.blocksBankReceipt(receiving)) return INVALID_RATE;
        int rate = standardRate(game, playerId, giving);
        if (rate < 1) return INVALID_RATE;
        if (market.getActiveEvent() == null || market.hasUsedSpecial(playerId)) return rate;
        switch (market.getActiveEvent()) {
            case MARKET_DAY:
                rate = Math.min(rate, 3);
                break;
            case HARBOUR_FESTIVAL:
                if (ownsAnyHarbour(game, playerId)) rate = Math.min(rate, 2);
                break;
            case MERCHANT_CARAVAN:
                if (market.getSelectedResource() == giving) rate = Math.min(rate, 2);
                break;
            default:
                break;
        }
        return rate;
    }

    /** One bank exchange. Both sides are validated BEFORE changing resources. */
    public static int exchangeWithBank(CatanGameState game, int playerId,
                                       int giving, int receiving) {
        if (game == null || game.isSetupPhase() || !game.isActionPhase()
                || game.isRobberPhase() || game.getCurrentPlayerId() != playerId)
            return INVALID_RATE;
        int rate = bestRate(game, playerId, giving, receiving);
        if (rate < 1) return INVALID_RATE;
        Player player = game.getPlayerList().get(playerId);
        if (!player.checkResourceCard(giving, rate)) return INVALID_RATE;
        int original = standardRate(game, playerId, giving);
        // A used market allowance must never be spent when the player's
        // normal harbour already provides this rate or a better one.
        boolean special = rate < original;
        if (!player.removeResourceCard(giving, rate)) return INVALID_RATE;
        player.addResourceCard(receiving, 1);
        if (special) game.getMarketState().markUsed(playerId);
        return rate;
    }

    /**
     * Transaction primitive for a separately AUTHORIZED player negotiation.
     * This is deliberately not exposed as a GameAction until explicit
     * accept/counteroffer authority and turn checks are implemented.
     */
    static boolean exchangeBetweenPlayers(CatanGameState game, int activePlayer,
                                          int partnerId, int[] giving, int[] receiving) {
        if (game == null || !game.isActionPhase() || game.isSetupPhase()
                || game.isRobberPhase() || activePlayer != game.getCurrentPlayerId()
                || partnerId < 0 || partnerId >= game.getPlayerList().size()
                || activePlayer == partnerId || !validBundle(giving)
                || !validBundle(receiving) || overlaps(giving, receiving)) return false;
        Player actor = game.getPlayerList().get(activePlayer);
        Player partner = game.getPlayerList().get(partnerId);
        if (!actor.hasResourceBundle(giving) || !partner.hasResourceBundle(receiving))
            return false;
        // There is no thread interleaving within a LocalGame move. All checks
        // have completed, so neither removal can fail due to a short hand.
        if (!actor.removeResourceBundle(giving) || !partner.removeResourceBundle(receiving))
            throw new IllegalStateException("Validated Catan trade failed to debit");
        for (int resource = 0; resource < RESOURCES; resource++) {
            actor.addResourceCard(resource, receiving[resource]);
            partner.addResourceCard(resource, giving[resource]);
        }
        return true;
    }

    /**
     * Catan does not allow giving and receiving the same resource type in one
     * player trade. Apply it to ORIGINAL and COUNTER offers, not just the UI.
     */
    public static boolean overlaps(int[] giving, int[] receiving) {
        if (giving == null || receiving == null
                || giving.length != RESOURCES || receiving.length != RESOURCES)
            return true;
        for (int r = 0; r < RESOURCES; r++)
            if (giving[r] > 0 && receiving[r] > 0) return true;
        return false;
    }

    public static boolean validBundle(int[] bundle) {
        if (bundle == null || bundle.length != RESOURCES) return false;
        boolean hasCards = false;
        for (int quantity : bundle) {
            if (quantity < 0 || quantity > 100) return false;
            hasCards |= quantity > 0;
        }
        return hasCards;
    }

    private static boolean validResource(int resource) {
        return resource >= 0 && resource < RESOURCES;
    }
}
