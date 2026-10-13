package edu.up.cs.androidcatan.catan;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Small private play-by-play diary. Stored separately from CatanGameState so
 * Build 005 match files and Java serialVersionUIDs remain compatible.
 */
public final class CatanMatchHistory {
    private static final String PREFS = "catan-match-history-v1";
    private static final String KEY = "events";
    private static final int MAX_ENTRIES = 60;

    private CatanMatchHistory() { }

    public static void clear(Context context) {
        if (context != null)
            context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().remove(KEY).commit();
    }

    public static void append(Context context, String event) {
        if (context == null || event == null || event.trim().isEmpty()) return;
        SharedPreferences store = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String previous = store.getString(KEY, "");
        // Events are single-line descriptions; never store player hand arrays.
        String safe = event.replace('\n', ' ').replace('\r', ' ').trim();
        String[] lines = previous.isEmpty() ? new String[0] : previous.split("\\n");
        StringBuilder result = new StringBuilder();
        int first = Math.max(0, lines.length - MAX_ENTRIES + 1);
        for (int i = first; i < lines.length; i++) {
            if (!lines[i].isEmpty()) result.append(lines[i]).append('\n');
        }
        result.append(safe);
        store.edit().putString(KEY, result.toString()).commit();
    }

    public static String history(Context context) {
        if (context == null) return "";
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    /** Last publicly announced roll, with the player's name and both dice. */
    public static String lastRoll(Context context) {
        String all = history(context);
        if (all.isEmpty()) return "";
        String[] entries = all.split("\\n");
        for (int i = entries.length - 1; i >= 0; i--) {
            if (entries[i].contains(" rolled ")) return entries[i];
        }
        return "";
    }

    public static String latest(Context context) {
        String history = history(context);
        if (history.isEmpty()) return "Every player's rolls and actions appear here.";
        int lastBreak = history.lastIndexOf('\n');
        return history.substring(lastBreak + 1);
    }
}
