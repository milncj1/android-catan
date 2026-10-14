package edu.up.cs.androidcatan.catan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Building;

/**
 * Saved, deterministic state for optional surprise market events. This class
 * owns timing and once-per-player benefits; it never creates resource cards.
 * Old saved matches have a null CatanGameState.marketState and remain classic.
 */
public final class CatanMarketState implements Serializable {
    private static final long serialVersionUID = 20261014011L;

    public enum Event {
        MARKET_DAY("Market Day"),
        HARBOUR_FESTIVAL("Harbour Festival"),
        MERCHANT_CARAVAN("Merchant Caravan"),
        TRADING_FAIR("Trading Fair"),
        MERCHANT_SHORTAGE("Merchant Shortage");

        public final String title;
        Event(String title) { this.title = title; }
    }

    private boolean enabled;
    private int completedRounds;
    private int nextEventRound = 4; // One-indexed: after all players finish round 3.
    private int activeFromRound;
    private Event activeEvent;
    private Event lastEvent;
    private int selectedResource = -1;
    private boolean[] specialUsed = new boolean[0];
    private int proactiveOffersThisRound;

    public CatanMarketState() { }

    public CatanMarketState(CatanMarketState other) {
        enabled = other.enabled;
        completedRounds = other.completedRounds;
        nextEventRound = other.nextEventRound;
        activeFromRound = other.activeFromRound;
        activeEvent = other.activeEvent;
        lastEvent = other.lastEvent;
        proactiveOffersThisRound = other.proactiveOffersThisRound;
        selectedResource = other.selectedResource;
        specialUsed = other.specialUsed == null ? new boolean[0]
                : other.specialUsed.clone();
    }

    /** Configure only when creating a new match, never during a resumed turn. */
    public void enable(boolean newValue) {
        if (enabled == newValue) return;
        enabled = newValue;
        if (!enabled) {
            activeEvent = null;
            specialUsed = new boolean[0];
            selectedResource = -1;
        }
    }

    public boolean isEnabled() { return enabled; }
    public int getCompletedRounds() { return completedRounds; }
    public int getNextEventRound() { return nextEventRound; }
    public Event getActiveEvent() { return enabled ? activeEvent : null; }
    public int getSelectedResource() { return getActiveEvent() == null ? -1 : selectedResource; }
    public int getActiveFromRound() { return activeFromRound; }

    public boolean hasUsedSpecial(int playerId) {
        return playerId < 0 || playerId >= specialUsed.length || specialUsed[playerId];
    }

    void markUsed(int playerId) {
        if (enabled && activeEvent != null && playerId >= 0
                && playerId < specialUsed.length) specialUsed[playerId] = true;
    }

    /**
     * Balanced frequency: up to three CPU-to-human interruptions per round.
     * Rotate which opponents have priority, so the first three cannot always
     * use the whole allowance in 4–6-opponent matches.
     */
    public boolean mayComputerOffer(int cpuId, int totalPlayers) {
        int computers = totalPlayers - 1;
        if (computers < 1 || cpuId < 1 || cpuId > computers
                || proactiveOffersThisRound >= 3) return false;
        int starting = (completedRounds * 3) % computers;
        int slots = Math.min(3, computers);
        for (int step = 0; step < slots; step++)
            if (cpuId - 1 == (starting + step) % computers) return true;
        return false;
    }

    void recordProactiveOffer() { proactiveOffersThisRound++; }
    public int getProactiveOffersThisRound() { return proactiveOffersThisRound; }

    /** Called exactly once as the last numbered player ends a normal turn. */
    public void onRoundCompleted(CatanGameState match, Random random) {
        completedRounds++;
        proactiveOffersThisRound = 0;
        if (!enabled) return;
        // Expire the previous event even when no new event starts.
        activeEvent = null;
        selectedResource = -1;
        specialUsed = new boolean[0];

        int beginningRound = completedRounds + 1;
        if (beginningRound < nextEventRound) return;
        ArrayList<Event> choices = new ArrayList<>(Arrays.asList(
                Event.MARKET_DAY, Event.MERCHANT_CARAVAN, Event.TRADING_FAIR));
        if (anyHarbourOwned(match)) choices.add(Event.HARBOUR_FESTIVAL);
        boolean disruption = lastEvent != Event.MERCHANT_SHORTAGE
                && random.nextInt(5) == 0; // 20% target
        if (disruption) {
            choices.clear();
            choices.add(Event.MERCHANT_SHORTAGE);
        }
        if (choices.size() > 1) choices.remove(lastEvent);
        if (choices.isEmpty()) choices.add(Event.MARKET_DAY);
        activeEvent = choices.get(random.nextInt(choices.size()));
        lastEvent = activeEvent;
        activeFromRound = beginningRound;
        specialUsed = new boolean[match.getPlayerList().size()];
        if (activeEvent == Event.MERCHANT_CARAVAN
                || activeEvent == Event.MERCHANT_SHORTAGE)
            selectedResource = random.nextInt(5);
        nextEventRound = beginningRound + 3 + random.nextInt(3);
    }

    private static boolean anyHarbourOwned(CatanGameState match) {
        for (Port port : match.getBoard().getPortList()) {
            int a = port.getIntersectionA(), b = port.getIntersectionB();
            Building[] buildings = match.getBoard().getBuildings();
            if (a >= 0 && a < buildings.length && buildings[a] != null) return true;
            if (b >= 0 && b < buildings.length && buildings[b] != null) return true;
        }
        return false;
    }

    /** A shortage affects exchanges RECEIVING a resource, not production/cards. */
    public boolean blocksBankReceipt(int resource) {
        return getActiveEvent() == Event.MERCHANT_SHORTAGE
                && selectedResource == resource;
    }

    private static String resourceName(int resource) {
        String[] names = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
        return resource >= 0 && resource < names.length ? names[resource] : "Unknown";
    }

    public String announcement() {
        if (getActiveEvent() == null) return "";
        switch (activeEvent) {
            case MARKET_DAY: return "Market Day! One 3:1 bank trade per player this round.";
            case HARBOUR_FESTIVAL: return "Harbour Festival! Harbour owners get one 2:1 trade with any resource.";
            case MERCHANT_CARAVAN: return "Merchant Caravan! Each player gets one 2:1 trade giving "
                    + resourceName(selectedResource) + ".";
            case TRADING_FAIR: return "Trading Fair! Rivals are more open to negotiations this round.";
            case MERCHANT_SHORTAGE: return "Merchant Shortage! " + resourceName(selectedResource)
                    + " cannot be received from the bank or harbours this round.";
            default: return "";
        }
    }
}
