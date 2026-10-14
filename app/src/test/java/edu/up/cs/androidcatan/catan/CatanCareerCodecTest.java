package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

/** Build 014 pure-Java regression tests: no emulator or network needed. */
public class CatanCareerCodecTest {
    private static CatanCareerRecord example(String id, int winner) {
        int[] dice = new int[13]; dice[7] = 5; dice[8] = 9;
        int[][] prod = new int[][] {{2,1,0,3,0},{0,4,1,0,2}};
        return new CatanCareerRecord(id, 1000L, 99000L, 78000L, false,
                "CLASSIC", winner, 8,
                new String[]{"You","CPU 1"}, new int[]{0,2},
                new int[]{10,8}, dice, prod, new int[]{3,3},
                new int[]{2,1}, new int[]{4,6}, new int[]{2,2},
                new int[]{1,1}, new int[]{3,2},
                new int[][] {{2,2},{4,3},{8,6}});
    }

    @Test public void backupRoundTripsWithoutJavaDeserialization() throws Exception {
        CatanCareerRecord original = example("match-aabbcc", 0);
        byte[] encoded = CatanCareerCodec.encode(Collections.singletonList(original));
        ArrayList<CatanCareerRecord> result = CatanCareerCodec.decode(encoded);
        assertEquals(1, result.size());
        assertEquals(original.id, result.get(0).id);
        assertEquals("CPU 1", result.get(0).names()[1]);
        assertEquals(9, result.get(0).dice()[8]);
        assertEquals(3, result.get(0).production()[0][3]);
        assertEquals(8, result.get(0).rounds);
        assertEquals(78000L, result.get(0).activeMillis);
    }

    @Test public void invalidChecksumCannotOverwriteCareer() throws Exception {
        byte[] corrupted = CatanCareerCodec.encode(
                Collections.singletonList(example("match-aabbcc", 0)));
        corrupted[corrupted.length - 11] ^= 42;
        try {
            CatanCareerCodec.decode(corrupted);
            fail("Corrupt backup must fail");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("checksum"));
        }
    }

    @Test public void smartMergeSkipsDuplicatesAndRemainsIdempotent() throws Exception {
        CatanCareerRecord a = example("match-aabbcc", 0);
        CatanCareerRecord b = example("match-aabbdd", 1);
        ArrayList<CatanCareerRecord> first = CatanCareerCodec.merge(
                Collections.singletonList(a), Arrays.asList(a, b));
        assertEquals(2, first.size());
        CatanCareerCodec.Preview preview = CatanCareerCodec.preview(first,
                Arrays.asList(a, b));
        assertEquals(0, preview.added);
        assertEquals(2, preview.duplicates);
        assertEquals(2, CatanCareerCodec.merge(first, Arrays.asList(a, b)).size());
    }

    @Test public void conflictingMatchIdRejectsWholeImport() throws Exception {
        CatanCareerRecord a = example("match-aabbcc", 0);
        CatanCareerRecord conflicting = example("match-aabbcc", 1);
        assertEquals(1, CatanCareerCodec.preview(Collections.singletonList(a),
                Collections.singletonList(conflicting)).conflicts);
        try {
            CatanCareerCodec.merge(Collections.singletonList(a),
                    Collections.singletonList(conflicting));
            fail("No Replace All, even when IDs collide");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Conflicting"));
        }
    }

    @Test public void defensiveCopiesPreventCareerMutation() {
        CatanCareerRecord record = example("match-aabbcc", 0);
        record.names()[0] = "CORRUPTED";
        record.dice()[8] = 0;
        record.production()[0][3] = 999;
        record.publicScoreHistory()[0][0] = 500;
        assertEquals("You", record.names()[0]);
        assertEquals(9, record.dice()[8]);
        assertEquals(3, record.production()[0][3]);
        assertEquals(2, record.publicScoreHistory()[0][0]);
    }

    @Test public void ledgerCountsAcceptedActionsAndOneFullRoundOnly() {
        CatanGameState game = new CatanGameState();
        game.setSetupPhase(false);
        game.setActionPhase(true);
        CatanMatchStats stats = game.getMatchStats();
        int[][] before = new int[4][5];
        game.getPlayerList().get(0).addResourceCard(0, 2);
        game.getPlayerList().get(1).addResourceCard(2, 1);
        stats.recordRoll(8, before, game);
        stats.recordBuild(0, CatanMatchStats.ROAD);
        stats.recordBankTrade(0);
        stats.recordPlayerTrade(0, 1);
        game.getMarketState().onRoundCompleted(game, new Random(7));
        stats.recordRound(game);
        stats.recordRound(game);
        assertEquals(1, stats.getDiceRolls()[8]);
        assertEquals(2, stats.getProduction()[0][0]);
        assertEquals(1, stats.getPlayerTrades()[0]);
        assertEquals(1, stats.getPlayerTrades()[1]);
        assertEquals(1, stats.getBankTrades()[0]);
        assertEquals(1, stats.getRoads()[0]);
        assertEquals(1, stats.getCompletedRounds());
    }

    @Test public void liveStatsSurviveCopyAndSerialization() throws Exception {
        CatanGameState game = new CatanGameState();
        game.getMatchStats().recordBuild(0, CatanMatchStats.CITY);
        CatanGameState copied = new CatanGameState(game);
        assertEquals(game.getMatchStats().getMatchId(), copied.getMatchStats().getMatchId());
        copied.getMatchStats().recordBuild(0, CatanMatchStats.ROAD);
        assertEquals(0, game.getMatchStats().getRoads()[0]);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(game); }
        CatanGameState restored;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (CatanGameState) in.readObject();
        }
        assertEquals(1, restored.getMatchStats().getCities()[0]);
        assertEquals(game.getMatchStats().getMatchId(), restored.getMatchStats().getMatchId());
    }

    @Test public void legacySaveWithNullStatisticsStartsPartialHistory() throws Exception {
        CatanGameState old = new CatanGameState();
        java.lang.reflect.Field ledger = CatanGameState.class.getDeclaredField("matchStats");
        ledger.setAccessible(true);
        ledger.set(old, null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(old); }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            CatanGameState resumed = (CatanGameState) in.readObject();
            assertTrue(resumed.getMatchStats().isPartialHistory());
            assertEquals(0, resumed.getMatchStats().getCompletedRounds());
        }
    }
}
