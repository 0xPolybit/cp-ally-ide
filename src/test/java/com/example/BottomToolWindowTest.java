package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BottomToolWindowTest {

    @Test
    void tabsStartVisibleAndTrackSelection() {
        BottomToolWindow tool = new BottomToolWindow(new JPanel(), new JPanel(), AppThemePalette.dark());

        assertTrue(tool.isExpanded());
        assertEquals("tests", tool.selectedTabKey());

        tool.selectResults();
        assertEquals("results", tool.selectedTabKey());

        tool.selectTests();
        assertEquals("tests", tool.selectedTabKey());
    }

    @Test
    void collapsingHidesTheWindowAndPersistsSelection() {
        BottomToolWindow tool = new BottomToolWindow(new JPanel(), new JPanel(), AppThemePalette.light());
        tool.selectResults();
        tool.setExpanded(false);

        assertFalse(tool.isExpanded());
        assertEquals("results", tool.selectedTabKey());
    }
}
