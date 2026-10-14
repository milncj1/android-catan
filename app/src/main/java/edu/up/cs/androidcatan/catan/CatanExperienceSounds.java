package edu.up.cs.androidcatan.catan;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiny generated offline cues. No music or sound is emitted unless explicitly
 * enabled, and silent/vibrate mode and music-volume zero suppress playback.
 * Generation and playback are on a background thread, never the game loop.
 */
public final class CatanExperienceSounds {
    public enum Cue { DICE, BUILD, TRADE, MARKET, ACHIEVEMENT, VICTORY }
    private static final int RATE = 22050;
    private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "catan-game-cues");
        thread.setDaemon(true);
        return thread;
    });
    private static long lastCueStarted;
    private CatanExperienceSounds() { }

    public static void play(Context context, Cue cue) {
        if (context == null || cue == null ||
                !CatanExperienceSettings.soundEffects(context)) return;
        Context app = context.getApplicationContext();
        AudioManager manager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (manager == null || manager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
                || manager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return;
        synchronized (CatanExperienceSounds.class) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastCueStarted < 150L) return;
            lastCueStarted = now;
        }
        AUDIO.execute(() -> {
            if (!CatanExperienceSettings.soundEffects(app)) return;
            playCue(cue);
        });
    }

    private static void playCue(Cue cue) {
        double[] notes;
        int millis;
        switch (cue) {
            case DICE: notes = new double[]{420, 510, 350}; millis = 180; break;
            case BUILD: notes = new double[]{300, 420}; millis = 160; break;
            case TRADE: notes = new double[]{510, 640}; millis = 170; break;
            case MARKET: notes = new double[]{490, 630, 760}; millis = 260; break;
            case ACHIEVEMENT: notes = new double[]{480, 620, 800}; millis = 280; break;
            default: notes = new double[]{440, 550, 660, 880}; millis = 360; break;
        }
        int samples = RATE * millis / 1000;
        short[] pcm = new short[samples];
        int noteSamples = Math.max(1, samples / notes.length);
        for (int i = 0; i < samples; i++) {
            int note = Math.min(notes.length - 1, i / noteSamples);
            double t = i / (double) RATE;
            double progress = (i % noteSamples) / (double) noteSamples;
            // Quick attack, gentle fading harmonics; intentionally restrained.
            double envelope = Math.min(1, progress * 18) * Math.pow(1 - progress, 1.8);
            double wave = Math.sin(2 * Math.PI * notes[note] * t)
                    + 0.18 * Math.sin(4 * Math.PI * notes[note] * t);
            pcm[i] = (short) (Math.max(-1, Math.min(1, wave * envelope * .13)) * 32767);
        }
        AudioTrack track = null;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            if (track.getState() != AudioTrack.STATE_INITIALIZED) return;
            if (track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING) < 0) return;
            track.play();
            Thread.sleep(millis + 30L);
            track.stop();
        } catch (RuntimeException | InterruptedException ignored) {
            // Optional sound must never affect the match or display.
            if (ignored instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally {
            if (track != null) track.release();
        }
    }
}
