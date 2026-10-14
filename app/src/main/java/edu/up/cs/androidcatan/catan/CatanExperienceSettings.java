package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/** Simple, offline Build 014 presentation preferences; never changes rules. */
public final class CatanExperienceSettings {
    private static final String PREFS = "catan-014-experience";
    private static final String SPEED = "speed", ANIMATIONS = "animations";
    private static final String EFFECTS = "effects", MUSIC = "music";
    private static volatile int currentSpeed = 1; // Relaxed=0, Normal=1, Fast=2
    private CatanExperienceSettings() { }

    private static SharedPreferences store(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void load(Context ctx) {
        currentSpeed = Math.max(0, Math.min(2, store(ctx).getInt(SPEED, 1)));
    }

    public static int speed(Context ctx) { load(ctx); return currentSpeed; }
    public static boolean animations(Context ctx) { return store(ctx).getBoolean(ANIMATIONS, true); }
    public static boolean soundEffects(Context ctx) { return store(ctx).getBoolean(EFFECTS, false); }
    public static boolean music(Context ctx) { return store(ctx).getBoolean(MUSIC, false); }

    /** CPU strategy is unaffected: ONLY existing display/thinking pauses scale. */
    public static int pauseMillis(int normalMillis) {
        if (normalMillis <= 0) return 0;
        if (currentSpeed == 0) return (int) Math.min(2500, normalMillis * 1.4);
        if (currentSpeed == 2) return Math.max(20, (int) (normalMillis * .20));
        return normalMillis;
    }

    public static int animationMillis(Context ctx, int normalMillis) {
        if (!animations(ctx)) return 0;
        int speed = speed(ctx);
        if (speed == 0) return (int) (normalMillis * 1.35);
        if (speed == 2) return Math.max(70, normalMillis / 4);
        return normalMillis;
    }

    public static void show(Activity activity, Runnable onAudioChange) {
        load(activity);
        int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad / 2, pad, pad / 2);

        TextView title = new TextView(activity);
        title.setText("GAMEPLAY SPEED");
        title.setTextSize(12);
        content.addView(title);
        RadioGroup group = new RadioGroup(activity);
        group.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Relaxed", "Normal", "Fast"};
        RadioButton[] buttons = new RadioButton[3];
        for (int i = 0; i < labels.length; i++) {
            final int selected = i;
            RadioButton button = new RadioButton(activity);
            button.setText(labels[i]);
            button.setTextSize(12);
            button.setPadding(0, 0, pad / 2, 0);
            group.addView(button);
            buttons[i] = button;
            button.setOnClickListener(v -> {
                currentSpeed = selected;
                store(activity).edit().putInt(SPEED, selected).apply();
            });
        }
        buttons[currentSpeed].setChecked(true);
        content.addView(group);

        CheckBox animation = new CheckBox(activity);
        animation.setText("Animations and visual feedback");
        animation.setTextSize(13);
        animation.setChecked(animations(activity));
        animation.setOnCheckedChangeListener((v, checked) ->
                store(activity).edit().putBoolean(ANIMATIONS, checked).apply());
        content.addView(animation);

        CheckBox sounds = new CheckBox(activity);
        sounds.setText("Sound effects (off by default)");
        sounds.setTextSize(13);
        sounds.setChecked(soundEffects(activity));
        sounds.setOnCheckedChangeListener((v, checked) -> {
            store(activity).edit().putBoolean(EFFECTS, checked).apply();
            if (onAudioChange != null) onAudioChange.run();
        });
        content.addView(sounds);

        CheckBox music = new CheckBox(activity);
        music.setText("Background music (off by default)");
        music.setTextSize(13);
        music.setChecked(music(activity));
        music.setOnCheckedChangeListener((v, checked) -> {
            store(activity).edit().putBoolean(MUSIC, checked).apply();
            if (onAudioChange != null) onAudioChange.run();
        });
        content.addView(music);

        TextView hint = new TextView(activity);
        hint.setText("Speed changes presentation only. Computer intelligence and dice remain unchanged.");
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(0, pad, 0, 0);
        content.addView(hint);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Gameplay Settings")
                .setView(scroll)
                .setPositiveButton("Done", null)
                .create();
        dialog.setOnShowListener(v -> {
            if (dialog.getWindow() != null)
                dialog.getWindow().setLayout(
                        (int) (activity.getResources().getDisplayMetrics().widthPixels * .82f),
                        (int) (activity.getResources().getDisplayMetrics().heightPixels * .85f));
        });
        dialog.show();
    }
}
