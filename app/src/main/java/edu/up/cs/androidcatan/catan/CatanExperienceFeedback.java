package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.SoundEffectConstants;
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
            if (important && CatanExperienceSettings.soundEffects(activity))
                banner.playSoundEffect(SoundEffectConstants.CLICK);
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
        return text.contains("MARKET EVENT") || text.contains("built a road")
                || text.contains("placed a settlement")
                || text.contains("upgraded a settlement") || text.contains("traded with")
                || text.contains("bought a development card")
                || text.contains("played Knight") || text.contains("robber");
    }
}
