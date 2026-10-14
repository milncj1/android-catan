package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Robber-specific responsive dialogs, replacing the enormous 2018 image menus.
 * No network, no rule changes, no view references in saved game state.
 */
public final class CatanRobberUi {
    public interface DiscardCallback { void submit(int[] discarded); }
    public interface VictimCallback { void choose(int opponent); }
    private static final String[] RESOURCES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
    private CatanRobberUi() { }

    private static int dp(Activity a, float value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private static TextView text(Activity a, String content, int size, boolean bold) {
        TextView t = new TextView(a);
        t.setText(content);
        t.setTextSize(size);
        t.setTextColor(0xFF294B41);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static LinearLayout layout(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(a, 14);
        l.setPadding(padding, dp(a, 9), padding, padding);
        l.setBackgroundColor(0xFFFFF7E7);
        return l;
    }

    private static AlertDialog show(Activity a, String heading, LinearLayout form,
                                    boolean cancelable) {
        ScrollView scroller = new ScrollView(a);
        scroller.setFillViewport(false);
        scroller.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(heading)
                .setView(scroller)
                .create();
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    Math.min(dp(a, 510), (int)(a.getResources()
                            .getDisplayMetrics().widthPixels * .77f)),
                    (int)(a.getResources().getDisplayMetrics().heightPixels * .86f));
        }
        return dialog;
    }

    /** Allow returning to a saved match even from non-cancelable robber stages. */
    private static void addMainMenuAction(Activity a, LinearLayout content) {
        if (!(a instanceof MainActivity)) return;
        Button home = new Button(a);
        home.setAllCaps(false);
        home.setText("Main Menu");
        home.setContentDescription("Save game and return to main menu");
        content.addView(home, new LinearLayout.LayoutParams(-1, dp(a, 42)));
        home.setOnClickListener(v ->
                ((MainActivity) a).confirmReturnToMainMenu());
    }

    /** Discard exactly half only if holding eight or more cards. */
    public static AlertDialog discard(Activity a, int[] hand, DiscardCallback callback) {
        if (a == null || hand == null || hand.length != 5)
            throw new IllegalArgumentException("Invalid resource hand");
        final int[] held = hand.clone();
        int handTotal = 0;
        for (int x : held) handTotal += x;
        final int required = CatanRobberSteps.discardCount(handTotal);
        if (required == 0) throw new IllegalArgumentException("No discard necessary");

        LinearLayout content = layout(a);
        TextView instructions = text(a, "You have " + handTotal
                + " resource cards. Choose exactly " + required
                + " to discard, then confirm.", 14, false);
        content.addView(instructions);
        TextView count = text(a, "0 of " + required + " selected", 15, true);
        count.setPadding(0, dp(a, 8), 0, dp(a, 6));
        content.addView(count);
        int[] selected = new int[5];
        TextView[] amounts = new TextView[5];
        Button[] minusButtons = new Button[5], plusButtons = new Button[5];
        final Button discard = new Button(a);
        discard.setAllCaps(false);
        discard.setText("Discard " + required + " cards");
        Runnable refresh = () -> {
            int chosen = CatanRobberSteps.selectedTotal(selected);
            count.setText(chosen + " of " + required + " selected");
            discard.setEnabled(CatanRobberSteps.discardReady(selected, held, required));
            for (int i = 0; i < selected.length; i++) {
                amounts[i].setText(Integer.toString(selected[i]));
                plusButtons[i].setEnabled(held[i] > selected[i] && chosen < required);
                minusButtons[i].setEnabled(selected[i] > 0);
            }
        };
        for (int i = 0; i < RESOURCES.length; i++) {
            final int res = i;
            LinearLayout line = new LinearLayout(a);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(a, RESOURCES[i] + "  (" + held[i] + ")", 14, true);
            line.addView(name, new LinearLayout.LayoutParams(0, dp(a, 42), 1f));
            Button minus = new Button(a);
            minus.setText("−");
            minus.setContentDescription("Discard one fewer " + RESOURCES[i]);
            line.addView(minus, new LinearLayout.LayoutParams(dp(a, 58), dp(a, 42)));
            TextView amount = text(a, "0", 17, true);
            amount.setGravity(Gravity.CENTER);
            line.addView(amount, new LinearLayout.LayoutParams(dp(a, 42), dp(a, 42)));
            Button plus = new Button(a);
            plus.setText("+");
            plus.setContentDescription("Discard one more " + RESOURCES[i]);
            line.addView(plus, new LinearLayout.LayoutParams(dp(a, 58), dp(a, 42)));
            amounts[i] = amount;
            plusButtons[i] = plus;
            minusButtons[i] = minus;
            minus.setOnClickListener(v -> {
                if (selected[res] > 0) selected[res]--;
                refresh.run();
            });
            plus.setOnClickListener(v -> {
                if (selected[res] < held[res]
                        && CatanRobberSteps.selectedTotal(selected) < required)
                    selected[res]++;
                refresh.run();
            });
            content.addView(line);
        }
        discard.setPadding(dp(a, 8), 0, dp(a, 8), 0);
        content.addView(discard);
        TextView note = text(a, "All selected cards will be discarded. You cannot "
                + "skip this step when you hold more than seven cards.", 11, false);
        note.setPadding(0, dp(a, 5), 0, 0);
        content.addView(note);
        addMainMenuAction(a, content);
        AlertDialog dialog = show(a, "ROBBER  ·  1/3  Discard", content, false);
        discard.setOnClickListener(v -> {
            if (!CatanRobberSteps.discardReady(selected, held, required)) return;
            discard.setEnabled(false);
            discard.setText("Discarding…");
            callback.submit(selected.clone());
        });
        refresh.run();
        return dialog;
    }

    /** Choose an adjacent PLAYER directly, not an unreliable junction hitbox. */
    public static AlertDialog victims(Activity a, List<Integer> eligible, String[] names,
                                      int[] publicHandSizes, VictimCallback callback) {
        if (a == null || eligible == null || eligible.isEmpty())
            throw new IllegalArgumentException("No robbery targets");
        LinearLayout content = layout(a);
        TextView instructions = text(a, "Choose an opponent beside the robber's new "
                + "hexagon. One random resource card will be stolen if they hold any.", 14, false);
        content.addView(instructions);
        AlertDialog dialog = show(a, "ROBBER  ·  3/3  Choose opponent", content, false);
        final boolean[] clicked = {false};
        for (int id : eligible) {
            if (id < 0 || id >= names.length) continue;
            String label = names[id] + "  ·  " + publicHandSizes[id]
                    + (publicHandSizes[id] == 1 ? " card" : " cards");
            Button button = new Button(a);
            button.setAllCaps(false);
            button.setText(label);
            button.setTextSize(14);
            content.addView(button, new LinearLayout.LayoutParams(-1, dp(a, 45)));
            button.setOnClickListener(v -> {
                if (clicked[0]) return;
                clicked[0] = true;
                button.setEnabled(false);
                callback.choose(id);
            });
        }
        addMainMenuAction(a, content);
        return dialog;
    }
}
