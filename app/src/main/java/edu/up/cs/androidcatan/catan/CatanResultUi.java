package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/** Build 014 winner's podium and full results, without changing career data. */
public final class CatanResultUi {
    private static final int[] PLAYER_COLORS = {
        0xFFD35D4C, 0xFF5188BA, 0xFF66A76B, 0xFFD5A84A,
        0xFF926EBD, 0xFFD17E4E, 0xFF63AAA9
    };
    private CatanResultUi() { }

    private static String duration(long milliseconds) {
        long min = milliseconds / 60_000L;
        return min >= 60L ? min / 60L + "h " + min % 60L + "m" : min + " min";
    }

    private static String difficulty(int[] types) {
        if (types.length < 2) return "Unknown";
        int type = types[1];
        for (int i = 2; i < types.length; i++) if (types[i] != type) return "Mixed";
        String[] modes = {"Human", "Easy", "Normal", "Hard", "Expert"};
        return type >= 1 && type < modes.length ? modes[type] : "Custom";
    }

    private static int sum(int[] nums) {
        int total = 0;
        for (int n : nums) total += n;
        return total;
    }

    private static int production(int[][] amounts, int player) {
        return sum(amounts[player]);
    }

    private static void addLabel(Activity a, LinearLayout dest, String title, String value) {
        LinearLayout pair = new LinearLayout(a);
        pair.setOrientation(LinearLayout.VERTICAL);
        pair.setPadding(CatanPolishUi.dp(a, 3), 0, CatanPolishUi.dp(a, 3), 0);
        TextView big = CatanPolishUi.text(a, value, 19, CatanPolishUi.FOREST, true);
        big.setGravity(Gravity.CENTER_HORIZONTAL);
        pair.addView(big);
        TextView small = CatanPolishUi.text(a, title, 11, CatanPolishUi.MUTED, true);
        small.setGravity(Gravity.CENTER_HORIZONTAL);
        pair.addView(small);
        dest.addView(pair, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    public static AlertDialog show(Activity a, CatanCareerRecord match) {
        final String[] names = match.names();
        final int[] scores = match.finalScores();
        final boolean victory = match.winner == 0;
        final CatanPolishUi.Panel panel = CatanPolishUi.panel(
                a, "★  CATAN  •  FINAL RESULTS",
                victory ? "VICTORY!" : "MATCH COMPLETE",
                names[match.winner] + " claimed the island",
                true, true);

        // Podium hero — makes the outcome immediately obvious.
        LinearLayout hero = CatanPolishUi.card(a, CatanPolishUi.PALE);
        TextView trophy = CatanPolishUi.text(a,
                victory ? "★  ISLAND CONQUERED  ★" : "★  THE WINNING SETTLEMENT  ★",
                12, CatanPolishUi.GOLD, true);
        trophy.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(trophy);
        TextView winner = CatanPolishUi.text(a, names[match.winner], 23,
                CatanPolishUi.FOREST, true);
        winner.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(winner);
        TextView score = CatanPolishUi.text(a, scores[match.winner]
                + " VICTORY POINTS", 15, CatanPolishUi.INK, true);
        score.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(score);
        panel.body.addView(hero);
        CatanPolishUi.gap(a, panel.body, 7);

        LinearLayout stats = CatanPolishUi.card(a, 0xFFFFF9ED);
        LinearLayout overview = new LinearLayout(a);
        overview.setGravity(Gravity.CENTER);
        overview.setOrientation(LinearLayout.HORIZONTAL);
        addLabel(a, overview, "ROUNDS", String.valueOf(match.rounds));
        addLabel(a, overview, "PLAY TIME", duration(match.activeMillis));
        addLabel(a, overview, "OPPONENTS", String.valueOf(names.length - 1));
        addLabel(a, overview, "DIFFICULTY", difficulty(match.computerTypes()));
        stats.addView(overview);
        panel.body.addView(stats);

        CatanPolishUi.section(a, panel.body, "FINAL STANDINGS");
        Integer[] order = new Integer[names.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (x, y) -> {
            int diff = Integer.compare(scores[y], scores[x]);
            return diff == 0 ? Integer.compare(x, y) : diff;
        });
        for (int i = 0; i < order.length; i++) {
            int p = order[i];
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int inset = CatanPolishUi.dp(a, 7);
            row.setPadding(inset, CatanPolishUi.dp(a, 5),
                    inset, CatanPolishUi.dp(a, 5));
            row.setBackground(CatanPolishUi.background(p == match.winner
                            ? 0xFFFFE7B5 : 0xFFFFFAED,
                    p == match.winner ? CatanPolishUi.GOLD : CatanPolishUi.DIVIDER,
                    CatanPolishUi.dp(a, 8), CatanPolishUi.dp(a, 1)));
            TextView rank = CatanPolishUi.text(a,
                    p == match.winner ? "★" : String.valueOf(i + 1), 15,
                    p == match.winner ? CatanPolishUi.GOLD : CatanPolishUi.MUTED, true);
            rank.setGravity(Gravity.CENTER);
            row.addView(rank, new LinearLayout.LayoutParams(CatanPolishUi.dp(a, 30), -2));
            View color = new View(a);
            color.setBackground(CatanPolishUi.background(PLAYER_COLORS[p], PLAYER_COLORS[p],
                    CatanPolishUi.dp(a, 8), 0));
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
                    CatanPolishUi.dp(a, 10), CatanPolishUi.dp(a, 10));
            colorParams.rightMargin = CatanPolishUi.dp(a, 10);
            row.addView(color, colorParams);
            TextView name = CatanPolishUi.text(a, names[p] + (p == 0 ? "  (You)" : ""),
                    14, CatanPolishUi.INK, p == match.winner);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView value = CatanPolishUi.text(a, scores[p] + " VP",
                    16, CatanPolishUi.FOREST, true);
            row.addView(value);
            panel.body.addView(row);
            CatanPolishUi.gap(a, panel.body, 3);
        }

        CatanPolishUi.section(a, panel.body, "MATCH HIGHLIGHTS");
        LinearLayout highlights = CatanPolishUi.card(a, 0xFFFFF9ED);
        int[] trades = match.playerTrades(), roads = match.roads();
        int[] settlers = match.settlements(), cities = match.cities();
        int[] development = match.developmentCards();
        int[][] resources = match.production();
        addStat(a, highlights, "Your player trades", trades[0]
                + " successful deals");
        addStat(a, highlights, "Your expansion", roads[0] + " roads · "
                + settlers[0] + " settlements · " + cities[0] + " cities");
        addStat(a, highlights, "Resources produced", String.valueOf(production(resources, 0)));
        addStat(a, highlights, "Development cards bought", String.valueOf(development[0]));
        panel.body.addView(highlights);

        TextView date = CatanPolishUi.text(a, match.island + " island  •  "
                + DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.UK)
                .format(new Date(match.finishedAt)),
                11, CatanPolishUi.MUTED, false);
        panel.body.addView(date);
        if (match.partialStatistics) {
            TextView partial = CatanPolishUi.text(a,
                    "Some statistics are partial because this match began in an earlier build.",
                    11, CatanPolishUi.MUTED, false);
            panel.body.addView(partial);
        }

        LinearLayout details = new LinearLayout(a);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);
        CatanPolishUi.section(a, details, "DICE & RESOURCE REPORT");
        addDice(a, details, match.dice());
        addResources(a, details, names, resources);
        CatanPolishUi.section(a, details, "BUILDING & TRADING");
        for (int p = 0; p < names.length; p++) {
            addStat(a, details, names[p], trades[p] + " player trades, "
                    + match.bankTrades()[p] + " bank trades, " + roads[p]
                    + " roads, " + settlers[p] + " settlements, "
                    + cities[p] + " cities");
        }
        CatanPolishUi.section(a, details, "SCORE PROGRESSION");
        details.addView(new CatanCareerGraph(a,
                Arrays.asList(match.publicScoreHistory()), names),
                new LinearLayout.LayoutParams(-1, CatanPolishUi.dp(a, 164)));
        panel.body.addView(details);

        Button close = CatanPolishUi.button(a, "Close", false);
        Button fullStats = CatanPolishUi.button(a, "Full stats ↓", false);
        panel.addAction(close, 1f);
        panel.addAction(fullStats, 1.15f);
        final boolean onBoard = a instanceof MainActivity
                && a.findViewById(edu.up.cs.androidcatan.R.id.board) != null;
        if (onBoard) {
            Button home = CatanPolishUi.button(a, "Main Menu", true);
            panel.addAction(home, 1.3f);
            home.setOnClickListener(v -> {
                panel.dialog.dismiss();
                ((MainActivity) a).confirmReturnToMainMenu();
            });
        }
        close.setOnClickListener(v -> panel.dialog.dismiss());
        fullStats.setOnClickListener(v -> {
            boolean expand = details.getVisibility() != View.VISIBLE;
            details.setVisibility(expand ? View.VISIBLE : View.GONE);
            fullStats.setText(expand ? "Hide stats ↑" : "Full stats ↓");
            if (expand) panel.scroll.post(() -> panel.scroll.smoothScrollTo(
                    0, details.getTop()));
        });
        return panel.show();
    }

    private static void addStat(Activity a, LinearLayout list, String title, String value) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, CatanPolishUi.dp(a, 4), 0, CatanPolishUi.dp(a, 4));
        TextView heading = CatanPolishUi.text(a, title, 12, CatanPolishUi.MUTED, false);
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView count = CatanPolishUi.text(a, value, 12, CatanPolishUi.INK, true);
        count.setGravity(Gravity.END);
        row.addView(count);
        list.addView(row);
    }

    private static void addDice(Activity a, LinearLayout root, int[] dice) {
        int high = 1;
        for (int n = 2; n <= 12; n++) high = Math.max(high, dice[n]);
        for (int n = 2; n <= 12; n++) {
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = CatanPolishUi.text(a, String.valueOf(n), 11,
                    CatanPolishUi.INK, true);
            row.addView(label, new LinearLayout.LayoutParams(CatanPolishUi.dp(a, 30), -2));
            ProgressBar bar = new ProgressBar(a, null,
                    android.R.attr.progressBarStyleHorizontal);
            bar.setMax(high); bar.setProgress(dice[n]);
            row.addView(bar, new LinearLayout.LayoutParams(0,
                    CatanPolishUi.dp(a, 8), 1f));
            TextView value = CatanPolishUi.text(a, " " + dice[n],
                    11, CatanPolishUi.MUTED, false);
            row.addView(value);
            root.addView(row);
        }
    }

    private static void addResources(Activity a, LinearLayout root, String[] names,
                                     int[][] production) {
        for (int p = 0; p < production.length; p++) {
            LinearLayout card = CatanPolishUi.card(a, 0xFFFFF9ED);
            TextView heading = CatanPolishUi.text(a, names[p] + " — "
                    + sum(production[p]) + " produced", 12, CatanPolishUi.FOREST, true);
            card.addView(heading);
            final String[] types = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
            for (int i = 0; i < types.length; i++)
                addStat(a, card, types[i], String.valueOf(production[p][i]));
            root.addView(card);
        }
    }
}
