package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThemeManagerTest {

    @Test
    void applyStoresPaletteAndPublishesSharedDefaults() {
        ThemeManager manager = new ThemeManager();
        AppThemePalette palette = AppThemePalette.light();

        manager.apply(palette);

        assertSame(palette, manager.currentPalette());
        assertEquals(palette.accentColor(), UIManager.getColor("Component.accentColor"));
        assertEquals(palette.focusColor(), UIManager.getColor("Component.focusColor"));
        assertEquals(palette.frameBackground(), UIManager.getColor("Panel.background"));
        assertEquals(palette.surfaceBackground(), UIManager.getColor("Button.background"));
    }

    @Test
    void nullPaletteFallsBackToDark() {
        ThemeManager manager = new ThemeManager(null);

        assertEquals("Dark", manager.currentPalette().name());

        manager.apply(null);

        assertEquals("Dark", manager.currentPalette().name());
    }
}
