package edu.up.cs.androidcatan.catan;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/**
 * Guards the visible in-match escape route. Saving and orderly shutdown
 * also receive an on-device end-to-end test because they use Android loops.
 */
public class CatanMainMenuNavigationTest {
    @Test public void gameBoardHasDedicatedMainMenuButton() throws Exception {
        File file = new File("src/main/res/layout/catan_main_activity.xml");
        if (!file.isFile()) file = new File("app/src/main/res/layout/catan_main_activity.xml");
        assertTrue("Board layout source must exist", file.isFile());
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(file);
        NodeList buttons = doc.getElementsByTagName("Button");
        boolean hasHome = false, hasHelp = false;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element element = (Element) buttons.item(i);
            String id = element.getAttribute("android:id");
            if ("@+id/match_main_menu_button".equals(id)) {
                hasHome = true;
                assertEquals("Main Menu", element.getAttribute("android:text"));
            }
            if ("@+id/sidebar_button_menu".equals(id)) {
                hasHelp = true;
                assertEquals("Help", element.getAttribute("android:text"));
            }
        }
        assertTrue("Dedicated main menu button must stay visible", hasHome);
        assertTrue("Old help button should not mislead players", hasHelp);
    }
}
