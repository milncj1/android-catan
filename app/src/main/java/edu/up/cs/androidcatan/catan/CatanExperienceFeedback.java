package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import edu.up.cs.androidcatan.R;

/**
 * Optional, nonblocking presentation only. Invoked after accepted engine
 * messages, never from dice or move validation. Replaces older notices instead
 * of queuing them when six CPUs play rapidly.
 */
public final class CatanExperienceFeedback {
    private CatanExperienceFeedback() { }

    public static void notice(Activity activity, String message, boolean important) {
        if (activity == null || message == null || message.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            TextView banner = activity.findViewById(R.id.experience_notice);
            if (banner == null || activity.isFinishing()) return;
            Object token = new Object();
            banner.setTag(token);
            banner.setText(message.length() > 105 ? message.substring(0, 102) + "…" : message);
            banner.setVisibility(View.VISIBLE);
            banner.animate().cancel();
            banner.setAlpha(CatanExperienceSettings.animations(activity) ? 0.15f : 1f);
            int transition = CatanExperienceSettings.animationMillis(activity, 200);
            if (transition > 0)
                banner.animate().alpha(1f).setDuration(transition).start();
            if (important) {
                CatanExperienceSounds.Cue cue = message.toLowerCase().contains("market")
                        ? CatanExperienceSounds.Cue.MARKET
                        : message.toLowerCase().contains("traded")
                        ? CatanExperienceSounds.Cue.TRADE
                        : message.toLowerCase().contains("longest road")
                        || message.toLowerCase().contains("largest army")
                        ? CatanExperienceSounds.Cue.ACHIEVEMENT
                        : CatanExperienceSounds.Cue.BUILD;
                CatanExperienceSounds.play(activity, cue);
            }
            int speed = CatanExperienceSettings.speed(activity);
            int linger = speed == 0 ? 3200 : speed == 2 ? 1000 : 2000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (banner.getTag() == token) {
                    banner.animate().cancel();
                    banner.setVisibility(View.GONE);
                    banner.setAlpha(1f);
                }
            }, linger);
        });
    }

    /**
     * Decorative dice tumble. The result is supplied by the accepted game
     * event; these changing faces NEVER roll or influence the engine.
     * Tokens prevent stale CPU animations from overwriting newer results.
     */
    public static void dice(Activity activity, TextView display, String result) {
        if (activity == null || display == null || result == null) return;
        if (!CatanExperienceSettings.animations(activity)) {
            pulse(activity, display, 160);
            return;
        }
        activity.runOnUiThread(() -> {
            if (activity.isFinishing()) return;
            final Object token = new Object();
            display.setTag(token);
            final int duration = CatanExperienceSettings.animationMillis(activity, 650);
            final String[] faces = {"⚃ ⚅", "⚁ ⚄", "⚂ ⚀"};
            Handler handler = new Handler(Looper.getMainLooper());
            for (int n = 0; n < faces.length; n++) {
                final int frame = n;
                handler.postDelayed(() -> {
                    if (display.getTag() == token) display.setText(faces[frame]);
                }, (duration * n) / 4L);
            }
            handler.postDelayed(() -> {
                if (display.getTag() == token) {
                    display.setText(result);
                    pulse(activity, display, 220);
                }
            }, duration);
            CatanExperienceSounds.play(activity, CatanExperienceSounds.Cue.DICE);
        });
    }

    /** Small polish effect on an already-updated score, roll or control. */
    public static void pulse(Activity activity, View view, int normalDuration) {
        if (activity == null || view == null
                || !CatanExperienceSettings.animations(activity)) return;
        activity.runOnUiThread(() -> {
            int duration = CatanExperienceSettings.animationMillis(activity, normalDuration);
            view.animate().cancel();
            view.setAlpha(0.60f);
            view.animate().alpha(1f).setDuration(Math.max(70, duration)).start();
        });
    }

    /** Keep ordinary CPU traffic in history; surface meaningful actions only. */
    public static boolean isImportant(String text) {
        if (text == null) return false;
        String event = text.toLowerCase(java.util.Locale.ROOT);
        // Roads and routine dev-card purchases stay in History and on-board
        // effects so a 7-player game never floods the screen with pop-ups.
        return event.contains("market event") || event.contains("built a city")
                || event.contains("upgraded a settlement")
                || event.contains("traded with") || event.contains("longest road")
                || event.contains("largest army") || event.contains("robber");
    }
}
