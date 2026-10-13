package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;

import edu.up.cs.androidcatan.game.GameMainActivity;
import edu.up.cs.androidcatan.game.GamePlayer;
import edu.up.cs.androidcatan.game.LocalGame;
import edu.up.cs.androidcatan.game.config.GameConfig;
import edu.up.cs.androidcatan.game.config.GamePlayerType;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * https://github.com/alexweininger/android-catan
 **/

public class MainActivity extends GameMainActivity {

    // the port number that this game will use when playing over the network
    private static final int PORT_NUMBER = 2278;

    private CatanSaveStore.Snapshot pendingResume;

    @Override
    protected boolean hasSavedMatch() {
        return CatanSaveStore.load(getApplicationContext()) != null;
    }

    @Override
    protected GameConfig savedMatchConfiguration() {
        CatanSaveStore.Snapshot saved = CatanSaveStore.load(getApplicationContext());
        pendingResume = null;
        if (saved == null) return null;

        GameConfig result = createDefaultConfig().copyWithoutPlayers();
        result.setLocal(true);
        String[] names = saved.getNames();
        int[] types = saved.getTypes();
        for (int index = 0; index < 4; index++) {
            result.addPlayer(names[index], types[index]);
        }
        pendingResume = saved;
        return result;
    }

    @Override
    protected void onNewMatchRequested() {
        pendingResume = null;
    }

    @Override
    protected void prepareLocalMatch(LocalGame localGame, GameConfig options) {
        if (!(localGame instanceof CatanLocalGame)) return;
        CatanLocalGame catan = (CatanLocalGame) localGame;
        int[] types = new int[4];
        GamePlayerType[] available = options.getAvailTypes();
        for (int i = 0; i < types.length; i++) {
            String label = options.getSelType(i).getTypeName();
            types[i] = -1;
            for (int choice = 0; choice < available.length; choice++) {
                if (label.equals(available[choice].getTypeName())) {
                    types[i] = choice;
                    break;
                }
            }
        }
        catan.configureAutosave(getApplicationContext(), types);
        if (pendingResume != null) {
            catan.setState(pendingResume.getState());
            pendingResume = null;
        }
    }

    // default game configuration
    @Override
    public GameConfig createDefaultConfig() {
        // Define the allowed player types
        ArrayList<GamePlayerType> playerTypes = new ArrayList<GamePlayerType>();

        // Catan has three player types:  human, smart cpu, dumb cpu
        playerTypes.add(new GamePlayerType("Local Human Player") {
            public GamePlayer createPlayer(String name) {
                return new CatanHumanPlayer(name);
            }
        });
        playerTypes.add(new GamePlayerType("Dumb Computer") {
            public GamePlayer createPlayer(String name) {
                return new CatanDumbComputerPlayer(name);
            }
        });
        playerTypes.add(new GamePlayerType("Smart Computer") {
            public GamePlayer createPlayer(String name) {
                return new CatanSmartComputerPlayer(name);
            }
        });

        GameConfig defaultConfig = new GameConfig(playerTypes, 4, 4, "Settlers of Catan", PORT_NUMBER);
        defaultConfig.addPlayer("Human", 0); // player 1: a human player
        defaultConfig.addPlayer("Computer 1", 2); // offline smart computer opponent
        defaultConfig.addPlayer("Computer 2", 2); // offline smart computer opponent
        defaultConfig.addPlayer("Computer 3", 2); // offline smart computer opponent
        defaultConfig.setRemoteData("Remote Human Player", "", 0);

        return defaultConfig;
    }

    @Override
    public LocalGame createLocalGame() {
        return new CatanLocalGame();
    }
}
