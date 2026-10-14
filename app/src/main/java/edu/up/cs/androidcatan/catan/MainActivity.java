package edu.up.cs.androidcatan.catan;

import java.util.ArrayList;
import edu.up.cs.androidcatan.catan.gamestate.CatanBoardTopology;

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
    private boolean resumeSessionPending;
    private CatanBoardTopology.Island chosenIsland = CatanBoardTopology.Island.CLASSIC;
    private boolean suppressBoardSelection;
    private boolean manuallyChosenBoard;
    private boolean newGameMarketEvents = true;
    private static final String MARKET_PREFERENCES = "catan_011_options";
    private static final String MARKET_ENABLED_KEY = "market_events_enabled";


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
        for (int index = 0; index < names.length; index++) {
            result.addPlayer(names[index], types[index]);
        }
        pendingResume = saved;
        return result;
    }

    @Override
    protected void onNewMatchRequested() {
        pendingResume = null;
        resumeSessionPending = false;
    }

    @Override
    protected void prepareLocalMatch(LocalGame localGame, GameConfig options) {
        if (!(localGame instanceof CatanLocalGame)) return;
        CatanLocalGame catan = (CatanLocalGame) localGame;
        int[] types = new int[options.getNumPlayers()];
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
        if (pendingResume == null) {
            // The game instance exists already but no player threads are running yet.
            // This replaces its default state only for a NEW game.
            catan.setState(new CatanGameState(options.getNumPlayers(), chosenIsland));
            catan.configureMarketEvents(newGameMarketEvents);
        }
        catan.configureAutosave(getApplicationContext(), types);
        // createLocalGame() has already installed the restored position.
        resumeSessionPending = pendingResume != null;
        pendingResume = null;
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
        // Append rather than renumber old type IDs: existing saved matches
        // store type 1 for Easy and type 2 for the original smart opponent.
        playerTypes.add(new GamePlayerType("Hard Computer") {
            public GamePlayer createPlayer(String name) {
                return new CatanSmartComputerPlayer(name, CatanAiDifficulty.HARD);
            }
        });
        playerTypes.add(new GamePlayerType("Expert Computer") {
            public GamePlayer createPlayer(String name) {
                return new CatanSmartComputerPlayer(name, CatanAiDifficulty.EXPERT);
            }
        });

        GameConfig defaultConfig = new GameConfig(playerTypes, 2, 7, "Settlers of Catan", PORT_NUMBER);
        defaultConfig.addPlayer("Human", 0); // player 1: a human player
        defaultConfig.addPlayer("Computer 1", 2); // offline smart computer opponent
        defaultConfig.addPlayer("Computer 2", 2); // offline smart computer opponent
        defaultConfig.addPlayer("Computer 3", 2); // offline smart computer opponent
        defaultConfig.setRemoteData("Remote Human Player", "", 0);

        return defaultConfig;
    }


    @Override
    protected void initStarterGui() {
        super.initStarterGui();
        final android.widget.Spinner island = findViewById(edu.up.cs.androidcatan.R.id.new_game_island);
        final android.widget.Spinner opponents = findViewById(edu.up.cs.androidcatan.R.id.new_game_opponents);
        final android.widget.TextView hint = findViewById(edu.up.cs.androidcatan.R.id.new_game_island_hint);
        if (island == null || opponents == null) return;

        android.widget.ArrayAdapter<String> islandChoices = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Classic · 19", "Expanded · 30", "Grand · 37"});
        islandChoices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        island.setAdapter(islandChoices);
        android.widget.ArrayAdapter<String> computerChoices = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"1 computer", "2 computers", "3 computers",
                        "4 computers", "5 computers", "6 computers"});
        computerChoices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        opponents.setAdapter(computerChoices);

        final android.widget.Switch events =
                findViewById(edu.up.cs.androidcatan.R.id.new_game_market_events);
        if (events != null) {
            // Default on for the chosen 011 mode, but remember future choices.
            newGameMarketEvents = getSharedPreferences(MARKET_PREFERENCES, MODE_PRIVATE)
                    .getBoolean(MARKET_ENABLED_KEY, true);
            events.setChecked(newGameMarketEvents);
            events.setOnCheckedChangeListener((button, enabled) -> {
                newGameMarketEvents = enabled;
                getSharedPreferences(MARKET_PREFERENCES, MODE_PRIVATE).edit()
                        .putBoolean(MARKET_ENABLED_KEY, enabled).apply();
            });
        }

        final android.widget.Spinner difficulty =
                findViewById(edu.up.cs.androidcatan.R.id.new_game_difficulty);
        final android.widget.TextView difficultyHint =
                findViewById(edu.up.cs.androidcatan.R.id.new_game_difficulty_hint);
        if (difficulty != null) {
            android.widget.ArrayAdapter<String> levels = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item,
                    new String[]{"Easy", "Normal", "Hard", "Expert"});
            levels.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            difficulty.setAdapter(levels);
            difficulty.setSelection(1); // Keep the current smart CPU as default.
            difficulty.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                android.view.View view, int position, long id) {
                    CatanAiDifficulty selected = CatanAiDifficulty.values()[position];
                    configureComputerDifficulty(selected.playerType);
                    if (difficultyHint != null) {
                        String description = position == 0
                                ? "Easy: forgiving, less predictable random moves"
                                : position == 1
                                    ? "Normal: sensible resources and expansion"
                                    : position == 2
                                        ? "Hard: resource balance, harbours and longer turns"
                                        : "Expert: more planning, contested sites and blocking";
                        difficultyHint.setText(description + " · same resources and dice");
                    }
                }
            });
        }

        int count = currentComputerOpponentRows();
        chosenIsland = suggestedIsland(count);
        suppressBoardSelection = true;
        island.setSelection(chosenIsland.ordinal());
        suppressBoardSelection = false;
        opponents.setSelection(count - 1);

        island.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                        android.view.View view, int position, long id) {
                chosenIsland = CatanBoardTopology.Island.values()[position];
                if (!suppressBoardSelection) manuallyChosenBoard = true;
                updateIslandHint(hint, opponents.getSelectedItemPosition() + 1);
            }
        });
        opponents.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                        android.view.View view, int position, long id) {
                int count = position + 1;
                configureComputerOpponentRows(count);
                if (!manuallyChosenBoard) {
                    suppressBoardSelection = true;
                    chosenIsland = suggestedIsland(count);
                    island.setSelection(chosenIsland.ordinal());
                    suppressBoardSelection = false;
                }
                updateIslandHint(hint, count);
            }
        });
        updateIslandHint(hint, count);
    }

    private CatanBoardTopology.Island suggestedIsland(int computerCount) {
        return computerCount >= 6 ? CatanBoardTopology.Island.GRAND
                : computerCount >= 4 ? CatanBoardTopology.Island.EXPANDED
                    : CatanBoardTopology.Island.CLASSIC;
    }

    private void updateIslandHint(android.widget.TextView view, int opponents) {
        if (view == null) return;
        String status = chosenIsland == CatanBoardTopology.Island.CLASSIC
                ? "Official 3–4-player island; 2 or 5+ players are house rules"
                : chosenIsland == CatanBoardTopology.Island.EXPANDED
                    ? "Official 5–6-player island; fewer players give extra space"
                    : "Custom Grand island for up to 7 total players";
        view.setText(chosenIsland.getTitle() + " · "
                + chosenIsland.getRowLengths().length + " rows · "
                + chosenIsland.getHexCount() + " tiles · " + status);
    }

    @Override
    public LocalGame createLocalGame() {
        // Never generate an unrelated random board before loading a match.
        return pendingResume == null
                ? new CatanLocalGame()
                : new CatanLocalGame(pendingResume.getState());
    }

    @Override
    protected void onLocalMatchLaunched(LocalGame launched) {
        if (!resumeSessionPending || !(launched instanceof CatanLocalGame)) return;
        resumeSessionPending = false;
        final CatanLocalGame resumed = (CatanLocalGame) launched;
        final android.os.Handler watcher = new android.os.Handler(android.os.Looper.getMainLooper());
        final long deadline = android.os.SystemClock.uptimeMillis() + 10000L;
        watcher.postDelayed(new Runnable() {
            @Override public void run() {
                if (isFinishing() || isDestroyed()) return;
                GamePlayer gui = getActiveGuiPlayer();
                boolean boardDisplayed = gui instanceof CatanHumanPlayer
                        && ((CatanHumanPlayer) gui).isInitialStateDisplayed();
                if (resumed.isSessionReady() && boardDisplayed) {
                    // Only now may the resumed game replace the last known-good save.
                    resumed.confirmResumeDisplayed();
                    return;
                }
                if (android.os.SystemClock.uptimeMillis() < deadline) {
                    watcher.postDelayed(this, 250L);
                    return;
                }
                String stage = resumed.describeStartup();
                android.util.Log.e("CatanResume", "Incomplete restore: "
                        + stage + ", boardDisplayed=" + boardDisplayed);
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Unable to finish restoring")
                        .setMessage("Your original saved game has been kept. "
                                + "Catan did not finish reconnecting the match. "
                                + "Close and reopen the app to try again.\n\n"
                                + "Diagnostic: " + stage + "; board=" + boardDisplayed)
                        .setPositiveButton("OK", null)
                        .show();
            }
        }, 250L);
    }
}
