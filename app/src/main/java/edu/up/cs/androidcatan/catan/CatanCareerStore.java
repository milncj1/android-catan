package edu.up.cs.androidcatan.catan;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline career archive, separate from the current-match autosave.
 * All mutations are atomic and imports are merge-only.
 */
public final class CatanCareerStore {
    private static final String FILE = "catan-career-v1.bin";
    private static final String TAG = "CatanCareer";
    private CatanCareerStore() { }

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getApplicationContext().getFilesDir(), FILE));
    }

    public static synchronized ArrayList<CatanCareerRecord> read(Context context)
            throws IOException {
        AtomicFile target = file(context);
        File base = target.getBaseFile();
        if (!base.exists() && !new File(base.getPath() + ".bak").exists())
            return new ArrayList<>();
        try (InputStream in = target.openRead()) {
            return CatanCareerCodec.decode(readLimited(in));
        }
    }

    private static byte[] readLimited(InputStream source) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count;
        while ((count = source.read(chunk)) != -1) {
            if (buffer.size() + count > CatanCareerCodec.MAX_BYTES)
                throw new IOException("Career backup too large");
            buffer.write(chunk, 0, count);
        }
        return buffer.toByteArray();
    }

    private static void save(Context context, List<CatanCareerRecord> entries)
            throws IOException {
        byte[] bytes = CatanCareerCodec.encode(entries);
        AtomicFile target = file(context);
        FileOutputStream out = null;
        try {
            out = target.startWrite();
            out.write(bytes);
            target.finishWrite(out);
        } catch (IOException | RuntimeException error) {
            if (out != null) target.failWrite(out);
            throw error;
        }
    }

    /** Records match completion once even if Android repeats a callback. */
    public static synchronized boolean archive(Context context, CatanGameState game,
            String[] names, int[] types, int winner) {
        if (context == null || game == null) return false;
        try {
            ArrayList<CatanCareerRecord> current = read(context);
            CatanCareerRecord next = CatanCareerRecord.finished(game, names, types, winner);
            ArrayList<CatanCareerRecord> merged = CatanCareerCodec.merge(
                    current, Collections.singletonList(next));
            if (merged.size() != current.size()) save(context, merged);
            return true;
        } catch (IOException | RuntimeException error) {
            Log.e(TAG, "Failed to archive completed match; existing data kept", error);
            return false;
        }
    }

    public static synchronized void exportTo(Context context, Uri destination)
            throws IOException {
        byte[] bytes = CatanCareerCodec.encode(read(context));
        try (OutputStream out = context.getContentResolver().openOutputStream(destination)) {
            if (out == null) throw new IOException("Cannot open selected backup location");
            out.write(bytes);
            out.flush();
        }
    }

    public static final class ImportPlan {
        public final byte[] bytes;
        public final CatanCareerCodec.Preview preview;
        private ImportPlan(byte[] bytes, CatanCareerCodec.Preview preview) {
            this.bytes = bytes.clone();
            this.preview = preview;
        }
    }

    /** Read/validate only, without modifying the phone. */
    public static synchronized ImportPlan previewImport(Context context, Uri source)
            throws IOException {
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            if (in == null) throw new IOException("Cannot open selected backup");
            bytes = readLimited(in);
        }
        ArrayList<CatanCareerRecord> incoming = CatanCareerCodec.decode(bytes);
        CatanCareerCodec.Preview preview =
                CatanCareerCodec.preview(read(context), incoming);
        return new ImportPlan(bytes, preview);
    }

    /** Repeat safety checks at commit time, then save once. */
    public static synchronized CatanCareerCodec.Preview commitImport(
            Context context, ImportPlan plan) throws IOException {
        if (plan == null) throw new IOException("No import to confirm");
        ArrayList<CatanCareerRecord> incoming = CatanCareerCodec.decode(plan.bytes);
        ArrayList<CatanCareerRecord> current = read(context);
        CatanCareerCodec.Preview preview = CatanCareerCodec.preview(current, incoming);
        if (preview.conflicts > 0)
            throw new IOException("Conflicting match IDs. Your records were not changed.");
        if (preview.added > 0) save(context, CatanCareerCodec.merge(current, incoming));
        return preview;
    }

    public static synchronized boolean delete(Context context, String matchId)
            throws IOException {
        ArrayList<CatanCareerRecord> records = read(context);
        for (int i = 0; i < records.size(); i++)
            if (records.get(i).id.equals(matchId)) {
                records.remove(i);
                save(context, records);
                return true;
            }
        return false;
    }
}
