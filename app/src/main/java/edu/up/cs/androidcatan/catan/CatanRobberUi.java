package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;
import edu.up.cs.androidcatan.R;

/**
 * Unified Build 014 robber presentation. Only the styling/layout changed;
 * engine validates all discard, movement and theft actions as before.
 */
public final class CatanRobberUi {
    public interface DiscardCallback { void submit(int[] discarded); }
    public interface VictimCallback { void choose(int opponent); }
    private static final String[] RESOURCES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
    private static final int[] ICONS = {
            R.drawable.brick_icon_25x25, R.drawable.grain_icon_25x25,
            R.drawable.lumber_icon_25x25, R.drawable.ore_icon_25x25,
            R.drawable.wool_icon_25x25
    };
    private static final int[] PLAYER_COLORS = {
            0xFFD35D4C, 0xFF5188BA, 0xFF66A76B, 0xFFD5A84A,
            0xFF926EBD, 0xFFD17E4E, 0xFF63AAA9
    };
    private CatanRobberUi() { }

    private static void addMainMenu(Activity a, CatanPolishUi.Panel panel) {
        if (!(a instanceof MainActivity)) return;
        Button home = CatanPolishUi.button(a, "Main Menu", false);
        panel.addAction(home, 1f);
        home.setOnClickListener(v -> ((MainActivity) a).confirmReturnToMainMenu());
    }

    private static LinearLayout resourceRow(Activity a, int resource, int available,
            int[] selected, int required, TextView amount, Button minus, Button plus) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(CatanPolishUi.dp(a, 8), CatanPolishUi.dp(a, 2),
                CatanPolishUi.dp(a, 8), CatanPolishUi.dp(a, 2));
        row.setBackground(CatanPolishUi.background(
                resource % 2 == 0 ? 0xFFFFFAEB : 0xFFFFEBCD,
                CatanPolishUi.DIVIDER, CatanPolishUi.dp(a, 8),
                CatanPolishUi.dp(a, 1)));
        ImageView icon = new ImageView(a);
        icon.setImageResource(ICONS[resource]);
        icon.setContentDescription(RESOURCES[resource]);
        row.addView(icon, new LinearLayout.LayoutParams(
                CatanPolishUi.dp(a, 30), CatanPolishUi.dp(a, 30)));
        TextView label = CatanPolishUi.text(a, "  " + RESOURCES[resource] + "  ·  " + available,
                14, CatanPolishUi.INK, true);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(minus, new LinearLayout.LayoutParams(
                CatanPolishUi.dp(a, 42), CatanPolishUi.dp(a, 40)));
        amount.setGravity(Gravity.CENTER);
        row.addView(amount, new LinearLayout.LayoutParams(
                CatanPolishUi.dp(a, 40), -2));
        row.addView(plus, new LinearLayout.LayoutParams(
                CatanPolishUi.dp(a, 42), CatanPolishUi.dp(a, 40)));
        return row;
    }

    public static AlertDialog discard(Activity a, int[] hand, DiscardCallback callback) {
        if (a == null || hand == null || hand.length != 5)
            throw new IllegalArgumentException("Invalid resource hand");
        final int[] held = hand.clone();
        int total = 0;
        for (int n : held) total += n;
        final int required = CatanRobberSteps.discardCount(total);
        if (required == 0) throw new IllegalArgumentException("No discard necessary");

        CatanPolishUi.Panel panel = CatanPolishUi.panel(a,
                "⚠  THE ROBBER  •  STEP 1 OF 3",
                "Discard resources",
                "A seven was rolled. Choose half your cards to discard.",
                false, false);

        LinearLayout summary = CatanPolishUi.card(a, CatanPolishUi.PALE);
        TextView count = CatanPolishUi.text(a,
                "0 / " + required + " CARDS SELECTED", 17, CatanPolishUi.FOREST, true);
        count.setGravity(Gravity.CENTER_HORIZONTAL);
        summary.addView(count);
        ProgressBar progress = new ProgressBar(a, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(required);
        progress.setProgress(0);
        summary.addView(progress, new LinearLayout.LayoutParams(
                -1, CatanPolishUi.dp(a, 10)));
        panel.body.addView(summary);
        CatanPolishUi.gap(a, panel.body, 8);

        final int[] chosen = new int[5];
        TextView[] amounts = new TextView[5];
        Button[] minusButtons = new Button[5], plusButtons = new Button[5];

        Button confirm = CatanPolishUi.button(a, "Discard " + required + " cards", true);
        panel.addAction(confirm, 1.55f);
        addMainMenu(a, panel);

        Runnable refresh = () -> {
            int countSelected = CatanRobberSteps.selectedTotal(chosen);
            count.setText(countSelected + " / " + required + " CARDS SELECTED");
            progress.setProgress(countSelected);
            confirm.setEnabled(CatanRobberSteps.discardReady(chosen, held, required));
            confirm.setAlpha(confirm.isEnabled() ? 1f : .5f);
            for (int i = 0; i < 5; i++) {
                amounts[i].setText(String.valueOf(chosen[i]));
                minusButtons[i].setEnabled(chosen[i] > 0);
                plusButtons[i].setEnabled(held[i] > chosen[i]
                        && countSelected < required);
                minusButtons[i].setAlpha(minusButtons[i].isEnabled() ? 1f : .45f);
                plusButtons[i].setAlpha(plusButtons[i].isEnabled() ? 1f : .45f);
            }
        };

        for (int res = 0; res < 5; res++) {
            final int i = res;
            Button minus = CatanPolishUi.button(a, "−", false);
            minus.setTextSize(17);
            minus.setContentDescription("Discard one fewer " + RESOURCES[i]);
            Button plus = CatanPolishUi.button(a, "+", false);
            plus.setTextSize(17);
            plus.setContentDescription("Discard one more " + RESOURCES[i]);
            TextView amount = CatanPolishUi.text(a, "0", 16, CatanPolishUi.FOREST, true);
            amounts[i] = amount;
            minusButtons[i] = minus;
            plusButtons[i] = plus;
            minus.setOnClickListener(v -> {
                if (chosen[i] > 0) chosen[i]--;
                refresh.run();
            });
            plus.setOnClickListener(v -> {
                if (chosen[i] < held[i] && CatanRobberSteps.selectedTotal(chosen) < required)
                    chosen[i]++;
                refresh.run();
            });
            panel.body.addView(resourceRow(a, i, held[i], chosen, required,
                    amount, minus, plus));
            CatanPolishUi.gap(a, panel.body, 4);
        }

        confirm.setOnClickListener(v -> {
            if (!CatanRobberSteps.discardReady(chosen, held, required)) return;
            confirm.setEnabled(false);
            confirm.setText("Discarding…");
            callback.submit(chosen.clone());
        });
        refresh.run();
        return panel.show();
    }

    /** Select an opponent, never an awkward or ambiguous building junction. */
    public static AlertDialog victims(Activity a, List<Integer> eligible, String[] names,
                                      int[] publicHandSizes, VictimCallback callback) {
        if (a == null || eligible == null || eligible.isEmpty()
                || names == null || publicHandSizes == null)
            throw new IllegalArgumentException("No valid targets");
        CatanPolishUi.Panel panel = CatanPolishUi.panel(a,
                "⚠  THE ROBBER  •  STEP 3 OF 3",
                "Choose an opponent",
                "Steal one random card from someone beside this hex.",
                false, false);

        TextView caption = CatanPolishUi.text(a,
                "Only opponents beside the robber are shown. Their card counts are public.",
                12, CatanPolishUi.MUTED, false);
        panel.body.addView(caption);
        CatanPolishUi.gap(a, panel.body, 8);

        final boolean[] submitted = {false};
        for (int id : eligible) {
            if (id < 0 || id >= names.length || id >= publicHandSizes.length) continue;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(CatanPolishUi.dp(a, 10), CatanPolishUi.dp(a, 5),
                    CatanPolishUi.dp(a, 10), CatanPolishUi.dp(a, 5));
            row.setBackground(CatanPolishUi.background(
                    0xFFFFE9C3, CatanPolishUi.GOLD,
                    CatanPolishUi.dp(a, 10), CatanPolishUi.dp(a, 1)));
            View dot = new View(a);
            dot.setBackground(CatanPolishUi.background(
                    PLAYER_COLORS[Math.min(id, PLAYER_COLORS.length - 1)],
                    PLAYER_COLORS[Math.min(id, PLAYER_COLORS.length - 1)],
                    CatanPolishUi.dp(a, 10), 0));
            row.addView(dot, new LinearLayout.LayoutParams(
                    CatanPolishUi.dp(a, 14), CatanPolishUi.dp(a, 14)));
            LinearLayout textStack = new LinearLayout(a);
            textStack.setOrientation(LinearLayout.VERTICAL);
            textStack.setPadding(CatanPolishUi.dp(a, 10), 0, 0, 0);
            TextView name = CatanPolishUi.text(a, names[id], 16,
                    CatanPolishUi.FOREST, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textStack.addView(name);
            int held = publicHandSizes[id];
            textStack.addView(CatanPolishUi.text(a,
                    held + (held == 1 ? " resource card" : " resource cards"),
                    11, CatanPolishUi.MUTED, false));
            row.addView(textStack, new LinearLayout.LayoutParams(0, -2, 1f));
            Button steal = CatanPolishUi.button(a, "Steal →", true);
            row.addView(steal, new LinearLayout.LayoutParams(
                    CatanPolishUi.dp(a, 94), CatanPolishUi.dp(a, 42)));
            steal.setOnClickListener(v -> {
                if (submitted[0]) return;
                submitted[0] = true;
                steal.setEnabled(false);
                steal.setText("Stealing…");
                callback.choose(id);
            });
            row.setOnClickListener(v -> steal.performClick());
            panel.body.addView(row);
            CatanPolishUi.gap(a, panel.body, 8);
        }
        addMainMenu(a, panel);
        return panel.show();
    }
}
