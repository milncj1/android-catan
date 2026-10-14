package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.view.Gravity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lightweight landscape dashboards; no permanent board overlays. */
public final class CatanCareerUi {
    public static final int EXPORT_REQUEST = 1401, IMPORT_REQUEST = 1402;
    private static final String[] RESOURCES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
    private static final String[] LEVELS = {"All difficulties", "Easy", "Normal", "Hard", "Expert"};
    private static final String[] ISLANDS = {"All islands", "CLASSIC", "EXPANDED", "GRAND"};
    private static final String[] OPPONENTS = {"All opponents", "1", "2", "3", "4", "5", "6"};
    private static final String[] DATES = {"All dates", "Last 30 days", "Last 90 days", "This year"};
    private CatanCareerUi() { }

    private static int dp(Activity a, int x) {
        return (int) (x * a.getResources().getDisplayMetrics().density + .5f);
    }
    private static LinearLayout column(Activity a) {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dp(a, 12);
        layout.setPadding(p, p / 2, p, p);
        return layout;
    }
    private static TextView text(Activity a, String value, int size, boolean heading) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(size);
        if (heading) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(a, heading ? 9 : 3), 0, dp(a, 3));
        return t;
    }
    private static void line(Activity a, LinearLayout parent, String label) {
        parent.addView(text(a, label, 12, false));
    }
    private static void title(Activity a, LinearLayout parent, String label) {
        parent.addView(text(a, label, 16, true));
    }
    private static AlertDialog dialog(Activity a, String title, LinearLayout body) {
        ScrollView scroll = new ScrollView(a);
        scroll.addView(body);
        AlertDialog d = new AlertDialog.Builder(a).setTitle(title)
                .setView(scroll).setPositiveButton("Close", null).create();
        d.setOnShowListener(ignored -> {
            if (d.getWindow() != null) d.getWindow().setLayout(
                    (int) (a.getResources().getDisplayMetrics().widthPixels * .90f),
                    (int) (a.getResources().getDisplayMetrics().heightPixels * .87f));
        });
        d.show();
        return d;
    }
    private static void bars(Activity a, LinearLayout parent, String[] names,
                             int[] values) {
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        for (int i = 0; i < values.length; i++) {
            String name = names != null && i < names.length ? names[i] : String.valueOf(i);
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(a, name + "  " + values[i], 11, false);
            row.addView(label, new LinearLayout.LayoutParams(dp(a, 105), -2));
            ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(max);
            bar.setProgress(Math.max(0, values[i]));
            row.addView(bar, new LinearLayout.LayoutParams(0, dp(a, 8), 1f));
            parent.addView(row);
        }
    }
    private static String formattedDate(long ms) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                Locale.UK).format(new Date(ms));
    }
    private static String duration(long ms) {
        long min = Math.max(0, ms / 60_000L);
        return min >= 60 ? (min / 60) + "h " + (min % 60) + "m" : min + " min";
    }
    private static int sum(int[] values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }
    private static String[] rollLabels() {
        String[] numbers = new String[11];
        for (int n = 2; n <= 12; n++) numbers[n - 2] = Integer.toString(n);
        return numbers;
    }
    private static int[] diceValues(int[] dice) {
        int[] values = new int[11];
        for (int n = 2; n <= 12; n++) values[n - 2] = dice[n];
        return values;
    }
    private static void resourceRows(Activity a, LinearLayout root, int[][] amounts,
                                     String[] names) {
        for (int p = 0; p < amounts.length; p++) {
            title(a, root, names[p]);
            line(a, root, "Total produced: " + sum(amounts[p]));
            bars(a, root, RESOURCES, amounts[p]);
        }
    }

    public static void showCurrent(Activity a, CatanGameState game, String[] names) {
        if (game == null) return;
        CatanMatchStats stats = game.getMatchStats();
        LinearLayout root = column(a);
        title(a, root, "Current Match");
        line(a, root, game.getBoard().getIsland().name() + " · "
                + (stats.getPlayerCount() - 1) + " opponents · "
                + stats.getCompletedRounds() + " complete rounds");
        if (stats.isPartialHistory())
            line(a, root, "Statistics began when this earlier saved match was resumed.");
        title(a, root, "Dice distribution");
        bars(a, root, rollLabels(), diceValues(stats.getDiceRolls()));
        title(a, root, "Resources produced");
        resourceRows(a, root, stats.getProduction(), names);
        title(a, root, "Trading and building");
        int[] trades = stats.getPlayerTrades(), bank = stats.getBankTrades();
        int[] roads = stats.getRoads(), settlements = stats.getSettlements();
        int[] cities = stats.getCities(), development = stats.getDevelopmentCards();
        for (int p = 0; p < stats.getPlayerCount(); p++)
            line(a, root, names[p] + ": " + trades[p] + " player trades, "
                    + bank[p] + " bank/harbour, " + roads[p] + " roads, "
                    + settlements[p] + " settlements, " + cities[p] + " cities, "
                    + development[p] + " dev cards bought");
        title(a, root, "Public victory-point progression");
        root.addView(new CatanCareerGraph(a, stats.getPublicScoreHistory(), names),
                new LinearLayout.LayoutParams(-1, dp(a, 164)));
        java.util.ArrayList<int[]> rounds = stats.getPublicScoreHistory();
        // A years-long unfinished match must not create thousands of Android
        // TextViews in one dialog. The graph above still covers every round.
        int first = Math.max(0, rounds.size() - 40);
        if (first > 0) line(a, root, "Showing the latest 40 of " + rounds.size()
                + " recorded rounds; graph includes the complete history.");
        for (int index = first; index < rounds.size(); index++) {
            int[] row = rounds.get(index);
            StringBuilder points = new StringBuilder("Round " + (index + 1) + ": ");
            for (int p = 0; p < row.length; p++) {
                if (p > 0) points.append(" · ");
                points.append(names[p]).append(" ").append(row[p]);
            }
            line(a, root, points.toString());
        }
        line(a, root, "Opponents' hidden development-card points are never disclosed during play.");
        dialog(a, "Match Statistics", root);
    }

    public static void showRecord(Activity a, CatanCareerRecord record) {
        LinearLayout root = column(a);
        String[] names = record.names();
        title(a, root, "Winner: " + names[record.winner]);
        line(a, root, formattedDate(record.finishedAt) + " · "
                + record.island + " · " + (record.players() - 1) + " opponents");
        line(a, root, record.rounds + " recorded rounds · " + duration(record.activeMillis));
        if (record.partialStatistics)
            line(a, root, "Partial statistics: this game began in an older version.");
        title(a, root, "Final standings");
        int[] scores = record.finalScores();
        for (int p = 0; p < names.length; p++)
            line(a, root, names[p] + " — " + scores[p] + " VP"
                    + (p == record.winner ? "  ★ Winner" : ""));
        title(a, root, "Dice distribution");
        bars(a, root, rollLabels(), diceValues(record.dice()));
        title(a, root, "Resource production");
        resourceRows(a, root, record.production(), names);
        title(a, root, "Trading and building");
        int[] trades = record.playerTrades(), bank = record.bankTrades();
        int[] roads = record.roads(), settlers = record.settlements();
        int[] cities = record.cities(), dev = record.developmentCards();
        for (int p = 0; p < names.length; p++)
            line(a, root, names[p] + ": " + trades[p] + " player trades, "
                    + bank[p] + " bank/harbour, " + roads[p] + " roads, "
                    + settlers[p] + " settlements, " + cities[p] + " cities, "
                    + dev[p] + " dev cards");
        title(a, root, "Public points by round");
        root.addView(new CatanCareerGraph(a,
                java.util.Arrays.asList(record.publicScoreHistory()), names),
                new LinearLayout.LayoutParams(-1, dp(a, 164)));
        int[][] fullHistory = record.publicScoreHistory();
        int firstRound = Math.max(0, fullHistory.length - 40);
        if (firstRound > 0)
            line(a, root, "Showing the latest 40 of " + fullHistory.length
                    + " rounds; the graph includes the full match.");
        for (int round = firstRound; round < fullHistory.length; round++) {
            int[] score = fullHistory[round];
            StringBuilder line = new StringBuilder("Round " + (round + 1) + ": ");
            for (int p = 0; p < score.length; p++)
                line.append(p > 0 ? " · " : "").append(names[p]).append(" ").append(score[p]);
            line(a, root, line.toString());
        }
        dialog(a, "Completed Match", root);
    }

    public static void showLatest(Activity a, String id) {
        try {
            for (CatanCareerRecord match : CatanCareerStore.read(a))
                if (match.id.equals(id)) { showRecord(a, match); return; }
        } catch (IOException ignored) { }
        new AlertDialog.Builder(a).setTitle("Match finished")
                .setMessage("Your match has finished. Open My Catan Career to review its result.")
                .setPositiveButton("OK", null).show();
    }

    private static Spinner spinner(Activity a, String[] values) {
        Spinner control = new Spinner(a);
        ArrayAdapter<String> choices = new ArrayAdapter<>(
                a, android.R.layout.simple_spinner_item, values);
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        control.setAdapter(choices);
        return control;
    }

    public static void showCareer(Activity a) {
        ArrayList<CatanCareerRecord> all;
        try { all = CatanCareerStore.read(a); }
        catch (IOException error) {
            new AlertDialog.Builder(a).setTitle("Career records unavailable")
                    .setMessage("The archive could not be read. Existing records have not been changed. "
                            + error.getMessage()).setPositiveButton("OK", null).show();
            return;
        }
        LinearLayout root = column(a);
        title(a, root, "My Catan Career");
        LinearLayout buttons = new LinearLayout(a);
        Button export = new Button(a), restore = new Button(a);
        export.setText("Export backup");
        restore.setText("Import backup");
        buttons.addView(export, new LinearLayout.LayoutParams(0, dp(a, 42), 1));
        buttons.addView(restore, new LinearLayout.LayoutParams(0, dp(a, 42), 1));
        root.addView(buttons);
        export.setOnClickListener(v -> {
            Intent target = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            target.setType("application/octet-stream");
            target.addCategory(Intent.CATEGORY_OPENABLE);
            target.putExtra(Intent.EXTRA_TITLE, "Catan_Career_Backup.ctc");
            a.startActivityForResult(target, EXPORT_REQUEST);
        });
        restore.setOnClickListener(v -> {
            Intent source = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            source.setType("*/*");
            source.addCategory(Intent.CATEGORY_OPENABLE);
            a.startActivityForResult(source, IMPORT_REQUEST);
        });

        LinearLayout filters = new LinearLayout(a);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        Spinner level = spinner(a, LEVELS), board = spinner(a, ISLANDS);
        Spinner rivals = spinner(a, OPPONENTS), date = spinner(a, DATES);
        filters.addView(level, new LinearLayout.LayoutParams(0, dp(a, 40), 1));
        filters.addView(board, new LinearLayout.LayoutParams(0, dp(a, 40), 1));
        filters.addView(rivals, new LinearLayout.LayoutParams(0, dp(a, 40), 1));
        root.addView(filters);
        root.addView(date, new LinearLayout.LayoutParams(-1, dp(a, 38)));

        LinearLayout summary = new LinearLayout(a), history = column(a);
        summary.setOrientation(LinearLayout.VERTICAL);
        root.addView(summary);
        title(a, root, "Completed matches");
        root.addView(history);
        final int[] showing = {0};
        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            summary.removeAllViews();
            history.removeAllViews();
            ArrayList<CatanCareerRecord> selected = new ArrayList<>();
            long now = System.currentTimeMillis();
            java.util.Calendar localCalendar = java.util.Calendar.getInstance();
            localCalendar.setTimeInMillis(now);
            int currentYear = localCalendar.get(java.util.Calendar.YEAR);
            for (CatanCareerRecord match : all) {
                if (level.getSelectedItemPosition() > 0
                        && (match.computerTypes().length < 2
                        || match.computerTypes()[1] != level.getSelectedItemPosition())) continue;
                if (board.getSelectedItemPosition() > 0
                        && !match.island.equals(ISLANDS[board.getSelectedItemPosition()])) continue;
                if (rivals.getSelectedItemPosition() > 0
                        && match.players() - 1 != rivals.getSelectedItemPosition()) continue;
                int when = date.getSelectedItemPosition();
                if (when == 1 && (match.finishedAt > now
                        || now - match.finishedAt > 30L * 86400000L)) continue;
                if (when == 2 && (match.finishedAt > now
                        || now - match.finishedAt > 90L * 86400000L)) continue;
                if (when == 3) {
                    localCalendar.setTimeInMillis(match.finishedAt);
                    if (localCalendar.get(java.util.Calendar.YEAR) != currentYear)
                        continue;
                }
                selected.add(match);
            }
            int wins = 0, streak = 0, best = 0;
            long totalPlayMillis = 0L, fastestWin = Long.MAX_VALUE;
            int mostTrades = 0, highestScore = 0;
            for (CatanCareerRecord match : selected) {
                totalPlayMillis += match.activeMillis;
                mostTrades = Math.max(mostTrades, match.playerTrades()[0]);
                highestScore = Math.max(highestScore, match.finalScores()[0]);
                if (match.winner == 0 && match.activeMillis > 0)
                    fastestWin = Math.min(fastestWin, match.activeMillis);
            }
            ArrayList<CatanCareerRecord> order = new ArrayList<>(selected);
            Collections.sort(order, (x, y) -> Long.compare(x.finishedAt, y.finishedAt));
            for (CatanCareerRecord match : order) {
                if (match.winner == 0) { wins++; streak++; best = Math.max(best, streak); }
                else streak = 0;
            }
            title(a, summary, selected.size() + " matches · " + wins + " wins · "
                    + (selected.isEmpty() ? "0" : String.format(Locale.UK, "%.1f",
                    (100.0 * wins) / selected.size())) + "% win rate");
            line(a, summary, "Best streak: " + best + " · Current streak: " + streak);
            if (!selected.isEmpty()) {
                line(a, summary, "Average active match: "
                        + duration(totalPlayMillis / selected.size())
                        + " · Fastest victory: "
                        + (fastestWin == Long.MAX_VALUE ? "—" : duration(fastestWin)));
                line(a, summary, "Most trades in one game: " + mostTrades
                        + " · Highest finishing score: " + highestScore + " VP");
            }
            if (selected.isEmpty()) {
                line(a, history, "No completed matches for these filters yet.");
                return;
            }
            int count = Math.min(selected.size(), showing[0] + 25);
            for (int i = 0; i < count; i++) {
                final CatanCareerRecord match = selected.get(i);
                Button item = new Button(a);
                item.setText((match.winner == 0 ? "★ Victory" : "Defeat")
                        + " · " + formattedDate(match.finishedAt)
                        + " · " + match.island + " · " + match.rounds + " rounds");
                item.setTextSize(12);
                item.setAllCaps(false);
                history.addView(item, new LinearLayout.LayoutParams(-1, dp(a, 42)));
                item.setOnClickListener(v -> new AlertDialog.Builder(a)
                        .setTitle("Completed match")
                        .setItems(new String[]{"View details", "Delete this record"},
                                (dlg, choice) -> {
                                    if (choice == 0) showRecord(a, match);
                                    else new AlertDialog.Builder(a)
                                            .setTitle("Delete this completed match?")
                                            .setMessage("The match will be removed from career statistics. Your unfinished game is unaffected.")
                                            .setNegativeButton("Cancel", null)
                                            .setPositiveButton("Delete", (confirm, which) -> {
                                                try {
                                                    CatanCareerStore.delete(a, match.id);
                                                    all.remove(match);
                                                    refresh[0].run();
                                                } catch (IOException error) {
                                                    new AlertDialog.Builder(a)
                                                            .setMessage(error.getMessage())
                                                            .setPositiveButton("OK", null).show();
                                                }
                                            }).show();
                                }).show());
            }
            if (count < selected.size()) {
                Button more = new Button(a);
                more.setText("Show older matches");
                history.addView(more);
                more.setOnClickListener(v -> { showing[0] += 25; refresh[0].run(); });
            }
        };
        AdapterView.OnItemSelectedListener changed = new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, android.view.View view,
                    int position, long id) { showing[0] = 0; refresh[0].run(); }
        };
        level.setOnItemSelectedListener(changed);
        board.setOnItemSelectedListener(changed);
        rivals.setOnItemSelectedListener(changed);
        date.setOnItemSelectedListener(changed);
        refresh[0].run();
        dialog(a, "My Catan Career", root);
    }
}
