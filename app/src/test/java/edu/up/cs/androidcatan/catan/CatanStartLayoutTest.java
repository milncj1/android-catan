package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/**
 * Prevent the original landscape clipping regression: name/options were
 * fixed-height siblings above the action footer, while an invisible legacy
 * setup table was allocated all the scrollable space.
 */
public class CatanStartLayoutTest {
    private static Document layout() throws Exception {
        File source = new File("src/main/res/layout/game_config_main.xml");
        if (!source.isFile())
            source = new File("app/src/main/res/layout/game_config_main.xml");
        assertTrue("Catan start layout source must be available", source.isFile());
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
    }

    private static Element id(Document doc, String name) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            if (("@+id/" + name).equals(e.getAttribute("android:id"))) return e;
        }
        fail("Missing setup control: " + name);
        return null;
    }

    private static boolean childOf(Node child, Node ancestor) {
        for (Node current = child.getParentNode(); current != null;
             current = current.getParentNode()) {
            if (current == ancestor) return true;
        }
        return false;
    }

    @Test public void actionsCannotBeScrolledOffTheBottom() throws Exception {
        Document doc = layout();
        Element options = id(doc, "new_game_options_scroll");
        assertEquals("ScrollView", options.getTagName());
        assertEquals("0dp", options.getAttribute("android:layout_height"));
        assertEquals("1", options.getAttribute("android:layout_weight"));
        assertTrue(childOf(id(doc, "new_game_player_name"), options));
        assertTrue(childOf(id(doc, "new_game_island"), options));
        assertTrue(childOf(id(doc, "new_game_opponents"), options));
        assertTrue(childOf(id(doc, "new_game_difficulty"), options));
        assertTrue(childOf(id(doc, "new_game_market_events"), options));
        assertFalse(childOf(id(doc, "continueGameButton"), options));
        assertFalse(childOf(id(doc, "saveConfigButton"), options));
        assertFalse(childOf(id(doc, "playGameButton"), options));
    }

    @Test public void invisibleLegacySetupDoesNotTakeLayoutHeight() throws Exception {
        Document doc = layout();
        NodeList includes = doc.getElementsByTagName("include");
        boolean legacyFound = false;
        for (int i = 0; i < includes.getLength(); i++) {
            Element element = (Element) includes.item(i);
            if ("@layout/game_config_tabhost".equals(
                    element.getAttribute("layout"))) {
                legacyFound = true;
                assertEquals("gone", element.getAttribute("android:visibility"));
                assertFalse(childOf(element, id(doc, "new_game_options_scroll")));
            }
        }
        assertTrue("Framework legacy table must remain mounted", legacyFound);
    }
}
