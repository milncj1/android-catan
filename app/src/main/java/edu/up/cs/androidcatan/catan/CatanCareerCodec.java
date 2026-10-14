package edu.up.cs.androidcatan.catan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Explicit, versioned data format; NEVER deserialize arbitrary Java objects
 * from a user-chosen import. The same codec drives local career storage and
 * manual offline exports. Merge is deterministic and idempotent.
 */
public final class CatanCareerCodec {
    private static final int MAGIC = 0x43544331; // CTC1
    private static final int VERSION = 1;
    public static final int MAX_BYTES = 8_000_000;
    private static final int MAX_MATCHES = 20000;
    private CatanCareerCodec() { }

    public static byte[] encode(List<CatanCareerRecord> matches) throws IOException {
        if (matches == null || matches.size() > MAX_MATCHES)
            throw new IOException("Too many matches");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(matches.size());
        for (CatanCareerRecord match : matches) writeRecord(out, match);
        out.flush();
        byte[] body = buffer.toByteArray();
        if (body.length > MAX_BYTES - 8) throw new IOException("Backup too large");
        CRC32 crc = new CRC32();
        crc.update(body);
        out.writeLong(crc.getValue());
        out.flush();
        return buffer.toByteArray();
    }

    public static ArrayList<CatanCareerRecord> decode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 20 || bytes.length > MAX_BYTES)
            throw new IOException("Invalid backup length");
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length - 8);
        DataInputStream check = new DataInputStream(new ByteArrayInputStream(
                bytes, bytes.length - 8, 8));
        if (check.readLong() != crc.getValue()) throw new IOException("Backup checksum mismatch");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(
                bytes, 0, bytes.length - 8));
        if (in.readInt() != MAGIC || in.readInt() != VERSION)
            throw new IOException("Unsupported Catan career backup");
        int count = in.readInt();
        if (count < 0 || count > MAX_MATCHES) throw new IOException("Invalid match count");
        ArrayList<CatanCareerRecord> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(readRecord(in));
        if (in.available() != 0) throw new IOException("Unexpected backup contents");
        // An imported file must never contain contradictory versions of the
        // same match; reject it before presenting a confirmation dialog.
        Preview internal = preview(Collections.<CatanCareerRecord>emptyList(), result);
        if (internal.conflicts > 0)
            throw new IOException("Backup contains conflicting match IDs");
        return result;
    }

    private static void writeRecord(DataOutputStream out, CatanCareerRecord r)
            throws IOException {
        out.writeUTF(r.id);
        out.writeLong(r.startedAt);
        out.writeLong(r.finishedAt);
        out.writeLong(r.activeMillis);
        out.writeBoolean(r.partialStatistics);
        out.writeUTF(r.island);
        out.writeInt(r.winner);
        out.writeInt(r.rounds);
        writeStrings(out, r.names());
        writeInts(out, r.computerTypes());
        writeInts(out, r.finalScores());
        writeInts(out, r.dice());
        writeRows(out, r.production());
        writeInts(out, r.playerTrades());
        writeInts(out, r.bankTrades());
        writeInts(out, r.roads());
        writeInts(out, r.settlements());
        writeInts(out, r.cities());
        writeInts(out, r.developmentCards());
        writeRows(out, r.publicScoreHistory());
    }

    private static CatanCareerRecord readRecord(DataInputStream in) throws IOException {
        try {
            String id = in.readUTF();
            long started = in.readLong(), finished = in.readLong(), active = in.readLong();
            boolean partial = in.readBoolean();
            String island = in.readUTF();
            int winner = in.readInt(), rounds = in.readInt();
            String[] names = readStrings(in, 7);
            int[] types = readInts(in, 7);
            int[] scores = readInts(in, 7);
            int[] dice = readInts(in, 13);
            int[][] production = readRows(in, 7, 5);
            int[] trades = readInts(in, 7);
            int[] bank = readInts(in, 7);
            int[] roads = readInts(in, 7);
            int[] settlements = readInts(in, 7);
            int[] cities = readInts(in, 7);
            int[] dev = readInts(in, 7);
            int[][] progress = readRows(in, 100000, 7);
            return new CatanCareerRecord(id, started, finished, active, partial,
                    island, winner, rounds, names, types, scores, dice, production,
                    trades, bank, roads, settlements, cities, dev, progress);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid career record", e);
        }
    }

    private static void writeStrings(DataOutputStream out, String[] data) throws IOException {
        out.writeInt(data.length);
        for (String value : data) out.writeUTF(value);
    }

    private static String[] readStrings(DataInputStream in, int max) throws IOException {
        int n = in.readInt();
        if (n < 2 || n > max) throw new IOException("Invalid player names");
        String[] result = new String[n];
        for (int i = 0; i < n; i++) result[i] = in.readUTF();
        return result;
    }

    private static void writeInts(DataOutputStream out, int[] values) throws IOException {
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static int[] readInts(DataInputStream in, int max) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > max) throw new IOException("Bad array length");
        int[] result = new int[n];
        for (int i = 0; i < n; i++) result[i] = in.readInt();
        return result;
    }

    private static void writeRows(DataOutputStream out, int[][] rows) throws IOException {
        out.writeInt(rows.length);
        for (int[] row : rows) writeInts(out, row);
    }

    private static int[][] readRows(DataInputStream in, int maxRows, int maxCols)
            throws IOException {
        int n = in.readInt();
        if (n < 0 || n > maxRows) throw new IOException("Bad row count");
        int[][] result = new int[n][];
        for (int i = 0; i < n; i++) result[i] = readInts(in, maxCols);
        return result;
    }

    public static final class Preview {
        public final int total, added, duplicates, conflicts;
        Preview(int total, int added, int duplicates, int conflicts) {
            this.total = total;
            this.added = added;
            this.duplicates = duplicates;
            this.conflicts = conflicts;
        }
    }

    /**
     * ID collisions with DIFFERENT payloads abort the entire import.
     * Byte-for-byte equivalent duplicates are ignored.
     */
    public static Preview preview(List<CatanCareerRecord> current,
                                  List<CatanCareerRecord> incoming) throws IOException {
        Map<String, byte[]> known = new HashMap<>();
        for (CatanCareerRecord r : current) {
            byte[] identity = oneRecord(r);
            byte[] previous = known.put(r.id, identity);
            if (previous != null && !java.util.Arrays.equals(previous, identity))
                throw new IOException("Conflicting local match ID");
        }
        int added = 0, duplicates = 0, conflicts = 0;
        for (CatanCareerRecord r : incoming) {
            byte[] data = oneRecord(r);
            byte[] old = known.get(r.id);
            if (old == null) { known.put(r.id, data); added++; }
            else if (java.util.Arrays.equals(old, data)) duplicates++;
            else conflicts++;
        }
        return new Preview(incoming.size(), added, duplicates, conflicts);
    }

    public static ArrayList<CatanCareerRecord> merge(List<CatanCareerRecord> current,
            List<CatanCareerRecord> incoming) throws IOException {
        Preview result = preview(current, incoming);
        if (result.conflicts > 0) throw new IOException(
                "Conflicting match IDs. Nothing was imported.");
        ArrayList<CatanCareerRecord> merged = new ArrayList<>(current);
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (CatanCareerRecord record : current) ids.add(record.id);
        for (CatanCareerRecord record : incoming)
            if (ids.add(record.id)) merged.add(record);
        Collections.sort(merged, (a, b) -> Long.compare(b.finishedAt, a.finishedAt));
        return merged;
    }

    private static byte[] oneRecord(CatanCareerRecord r) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeRecord(new DataOutputStream(bytes), r);
        return bytes.toByteArray();
    }
}
