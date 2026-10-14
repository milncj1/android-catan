package edu.up.cs.androidcatan.catan;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Structural checks because the original results dialog clipped its scroll
 * and the robber dialogs had their only action buried below five resource rows.
 */
public class CatanPolishLayoutTest {
    private String source(String file) throws Exception {
        File f = new File("src/main/java/edu/up/cs/androidcatan/catan/" + file);
        if (!f.isFile()) f = new File(
                "app/src/main/java/edu/up/cs/androidcatan/catan/" + file);
        assertTrue(file + " should exist", f.isFile());
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void visualDialogsHaveScrollableBodyAndPinnedActions() throws Exception {
        String common = source("CatanPolishUi.java");
        assertTrue(common.contains("shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f))"));
        assertTrue(common.contains("shell.addView(footer, new LinearLayout.LayoutParams(-1, -2))"));
        assertTrue(common.contains("WindowManager.LayoutParams.FLAG_DIM_BEHIND"));
        assertTrue(common.contains("screenHeight * .85f"));
    }

    @Test public void matchSummaryHasAlwaysVisibleHomeAndExpandableStats() throws Exception {
        String result = source("CatanResultUi.java");
        assertTrue(result.contains("FINAL STANDINGS"));
        assertTrue(result.contains("MATCH HIGHLIGHTS"));
        assertTrue(result.contains("Full stats ↓"));
        assertTrue(result.contains("panel.addAction(home, 1.3f)"));
        assertTrue(result.contains("panel.dialog.dismiss()"));
    }

    @Test public void robberUiRetainsLegalStepActions() throws Exception {
        String robber = source("CatanRobberUi.java");
        assertTrue(robber.contains("CatanRobberSteps.discardReady"));
        assertTrue(robber.contains("callback.submit(chosen.clone())"));
        assertTrue(robber.contains("callback.choose(id)"));
        assertTrue(robber.contains("panel.addAction(confirm, 1.55f)"));
        assertTrue(robber.contains("R.drawable.brick_icon_25x25"));
        assertTrue(robber.contains("addMainMenu(a, panel)"));
    }
}
