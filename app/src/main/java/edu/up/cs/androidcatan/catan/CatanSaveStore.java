package edu.up.cs.androidcatan.catan;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * One private, offline, atomic autosave for the current match.
 * No account, network permission, external storage, or backup dependency.
 *
 * Every write replaces the last known-good snapshot atomically. Android can
 * restore the previous complete version if the process dies during a write.
 */
public final class CatanSaveStore {
    private static final String TAG = "CatanSaveStore";
    private static final String FILE_NAME = "catan-current-match-v1.dat";
    private static final long MAX_SAVE_BYTES = 2_000_000L;
    private static final int FORMAT = 1;

    private CatanSaveStore() { }

    /** Data only: never serialise Activity, View, Handler or CPU threads. */
    public static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 20261013001L;
        private final int format;
        private final String[] names;
        private final int[] types;
        private final CatanGameState state;
        private final long savedAt;

        public Snapshot(CatanGameState state, String[] names, int[] types) {
            this.format = FORMAT;
            this.state = state == null ? null : new CatanGameState(state);
            this.names = names == null ? null : names.clone();
            this.types = types == null ? null : types.clone();
            this.savedAt = System.currentTimeMillis();
        }

        public boolean isValid() {
            if (format != FORMAT || state == null || state.getBoard() == null
                    || state.getPlayerList() == null || state.getPlayerList().size() != 4
                    || names == null || names.length != 4 || types == null || types.length != 4
                    || state.getCurrentPlayerId() < 0 || state.getCurrentPlayerId() > 3
                    || types[0] != 0) return false;
            for (int i = 0; i < 4; i++) {
                if (names[i] == null || names[i].trim().isEmpty()
                        || names[i].length() > 120) return false;
                if (i > 0 && types[i] != 1 && types[i] != 2) return false;
            }
            return true;
        }

        public CatanGameState getState() { return new CatanGameState(state); }
        public String[] getNames() { return names.clone(); }
        public int[] getTypes() { return types.clone(); }
        public long getSavedAt() { return savedAt; }
    }

    private static AtomicFile atomic(Context context) {
        File data = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        return new AtomicFile(data);
    }

    public static synchronized boolean save(Context context, CatanGameState state,
                                            String[] names, int[] types) {
        if (context == null) return false;
        Snapshot snapshot = new Snapshot(state, names, types);
        if (!snapshot.isValid()) return false;

        AtomicFile file = atomic(context);
        OutputStream out = null;
        try {
            out = file.startWrite();
            ObjectOutputStream objectStream = new ObjectOutputStream(out);
            objectStream.writeObject(snapshot);
            objectStream.flush();
            file.finishWrite(out);
            return true;
        } catch (IOException | RuntimeException ex) {
            Log.e(TAG, "Couldn't write autosave", ex);
            if (out != null) file.failWrite(out);
            return false;
        }
    }

    /** Corrupt/incompatible saves are kept untouched; never delete user data on read failure. */
    public static synchronized Snapshot load(Context context) {
        if (context == null) return null;
        AtomicFile file = atomic(context);
        if (!file.getBaseFile().exists()) return null;
        if (file.getBaseFile().length() > MAX_SAVE_BYTES) {
            Log.w(TAG, "Save file exceeds safe size limit");
            return null;
        }
        try (ObjectInputStream input = new ObjectInputStream(file.openRead())) {
            Object read = input.readObject();
            if (!(read instanceof Snapshot)) return null;
            Snapshot snapshot = (Snapshot) read;
            return snapshot.isValid() ? snapshot : null;
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            Log.w(TAG, "Saved game is unavailable or incompatible", ex);
            return null;
        }
    }

    public static synchronized void clear(Context context) {
        if (context != null) atomic(context).delete();
    }
}
