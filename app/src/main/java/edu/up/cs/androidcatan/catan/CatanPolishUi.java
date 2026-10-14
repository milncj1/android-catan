package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shared visual shell for Build 014's results and robber stages.
 * Flexible middle scroll area and FIXED footer for narrow landscape devices.
 * It is presentation-only: no rules, resources or saved data here.
 */
public final class CatanPolishUi {
    public static final int FOREST = 0xFF173E36;
    public static final int FOREST_DARK = 0xFF0F302B;
    public static final int GOLD = 0xFFB78436;
    public static final int GOLD_LIGHT = 0xFFFFE4A3;
    public static final int PAPER = 0xFFFFF5DC;
    public static final int INK = 0xFF23473C;
    public static final int MUTED = 0xFF72654E;
    public static final int PALE = 0xFFFFE9BF;
    public static final int DIVIDER = 0xFFE1C99C;

    private CatanPolishUi() { }

    public static int dp(Activity a, float n) {
        return Math.round(a.getResources().getDisplayMetrics().density * n);
    }

    public static GradientDrawable background(int fill, int border, float radius, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        if (stroke > 0) bg.setStroke(stroke, border);
        return bg;
    }

    public static TextView text(Activity a, String label, int sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(label);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setIncludeFontPadding(true);
        return t;
    }

    public static Button button(Activity a, String title, boolean primary) {
        Button b = new Button(a);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(primary ? GOLD_LIGHT : FOREST);
        b.setBackground(background(primary ? FOREST : PALE,
                primary ? GOLD : GOLD, dp(a, 9), dp(a, 1)));
        b.setMinimumHeight(dp(a, 44));
        b.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        return b;
    }

    public static LinearLayout card(Activity a, int fill) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10));
        c.setBackground(background(fill, DIVIDER, dp(a, 10), dp(a, 1)));
        return c;
    }

    public static void gap(Activity a, LinearLayout container, int heightDp) {
        View spacer = new View(a);
        container.addView(spacer, new LinearLayout.LayoutParams(1, dp(a, heightDp)));
    }

    public static void section(Activity a, LinearLayout container, String title) {
        gap(a, container, 11);
        container.addView(text(a, title, 15, FOREST, true));
        gap(a, container, 6);
    }

    /** Created first, populated next, shown only once all controls exist. */
    public static final class Panel {
        public final LinearLayout body, footer;
        public final ScrollView scroll;
        public final AlertDialog dialog;
        private final LinearLayout shell;
        private final Activity activity;
        private final boolean wide;

        private Panel(Activity a, String kicker, String heading, String subtitle,
                      boolean wide, boolean cancelable) {
            this.activity = a;
            this.wide = wide;
            shell = new LinearLayout(a);
            shell.setOrientation(LinearLayout.VERTICAL);
            shell.setBackground(background(PAPER, GOLD, dp(a, 16), dp(a, 2)));
            shell.setClipToPadding(false);

            LinearLayout header = new LinearLayout(a);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setPadding(dp(a, 17), dp(a, 10), dp(a, 17), dp(a, 10));
            header.setBackground(background(FOREST, GOLD, dp(a, 14), dp(a, 1)));
            TextView eyebrow = text(a, kicker, 11, GOLD_LIGHT, true);
            eyebrow.setLetterSpacing(0.09f);
            header.addView(eyebrow);
            TextView headline = text(a, heading, wide ? 25 : 19, GOLD_LIGHT, true);
            header.addView(headline);
            if (subtitle != null && !subtitle.isEmpty()) {
                TextView detail = text(a, subtitle, 12, 0xFFECE1C9, false);
                header.addView(detail);
            }
            shell.addView(header, new LinearLayout.LayoutParams(-1, -2));

            scroll = new ScrollView(a);
            scroll.setFillViewport(false);
            scroll.setVerticalScrollBarEnabled(true);
            scroll.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
            scroll.setClipToPadding(false);
            body = new LinearLayout(a);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(a, 4), 0, dp(a, 4), dp(a, 4));
            scroll.addView(body);
            shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

            footer = new LinearLayout(a);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.CENTER_VERTICAL);
            footer.setPadding(dp(a, 13), dp(a, 7), dp(a, 13), dp(a, 8));
            footer.setBackground(background(0xFFFFE8BB, DIVIDER, dp(a, 10), dp(a, 1)));
            shell.addView(footer, new LinearLayout.LayoutParams(-1, -2));

            dialog = new AlertDialog.Builder(a).setView(shell).create();
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(false);
        }

        public void addAction(Button action, float weight) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dp(activity, 44), weight);
            params.setMargins(dp(activity, 3), 0, dp(activity, 3), 0);
            footer.addView(action, params);
        }

        public AlertDialog show() {
            dialog.show();
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawableResource(android.R.color.transparent);
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams attrs = w.getAttributes();
                attrs.dimAmount = .67f;
                w.setAttributes(attrs);
                int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
                int maxWidth = dp(activity, wide ? 900 : 570);
                int width = Math.min(maxWidth, (int) (screenWidth * (wide ? .88f : .77f)));
                int height = (int) (screenHeight * .85f);
                w.setLayout(width, height);
            }
            return dialog;
        }
    }

    public static Panel panel(Activity a, String kicker, String heading,
                              String subtitle, boolean wide, boolean cancelable) {
        return new Panel(a, kicker, heading, subtitle, wide, cancelable);
    }
}
