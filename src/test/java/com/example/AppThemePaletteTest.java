package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppThemePaletteTest {

    @Test
    void applicationAccentIsDistinctFromSemanticSuccess() {
        for (AppThemePalette palette : new AppThemePalette[]{
                AppThemePalette.light(),
                AppThemePalette.dark(),
                AppThemePalette.ultraDark(),
                AppThemePalette.lightContrast(),
                AppThemePalette.darkContrast()}) {
            assertNotEquals(palette.accentColor(), palette.successColor(), palette.name());
            assertEquals(palette.accentColor(), palette.linkColor());
            assertEquals(palette.accentColor(), palette.focusColor());
            assertNotNull(palette.infoColor());
            assertNotNull(palette.disabledTextColor());
            assertNotNull(palette.subtleBorderColor());
        }
    }

    @Test
    void themeLookupKeepsSupportedNamesAndDefaultsSafely() {
        assertEquals("Light", AppThemePalette.fromName("light").name());
        assertEquals("Ultra Dark", AppThemePalette.fromName(" ULTRA DARK ").name());
        assertEquals("Dark", AppThemePalette.fromName("unknown").name());
        assertEquals("Dark", AppThemePalette.fromName(null).name());
    }
}
