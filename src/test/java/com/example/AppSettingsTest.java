package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsTest {

    @Test
    void defaultsUseTheExpectedFirstRunConfiguration() {
        AppSettings settings = AppSettings.defaults("Python 3");

        assertEquals(-1, settings.x());
        assertEquals(-1, settings.y());
        assertEquals(1200, settings.width());
        assertEquals(760, settings.height());
        assertEquals(420, settings.dividerLocation());
        assertEquals(420, settings.testCasesDividerLocation());
        assertFalse(settings.maximized());
        assertEquals("Python 3", settings.lastLanguage());
        assertEquals(14, settings.editorFontSize());
        assertEquals("Eclipse Dark", settings.editorColorScheme());
        assertEquals("Dark", settings.appTheme());
        assertFalse(settings.useTabsAsSpaces());
        assertEquals(4, settings.tabSpacing());
        assertTrue(settings.autosaveEnabled());
        assertEquals(10, settings.autosaveIntervalSeconds());
        assertEquals("", settings.codeforcesUsername());
    }
}
