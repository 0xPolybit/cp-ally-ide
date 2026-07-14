package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Owns application-level FlatLaf setup and shared UIManager defaults.
 *
 * Components still receive an AppThemePalette when they need document/editor
 * colors, but global Look and Feel configuration belongs here rather than in
 * the main window orchestration class.
 */
final class ThemeManager {

    private AppThemePalette currentPalette;

    ThemeManager() {
        this(AppThemePalette.dark());
    }

    ThemeManager(AppThemePalette initialPalette) {
        this.currentPalette = safePalette(initialPalette);
    }

    AppThemePalette currentPalette() {
        return currentPalette;
    }

    /**
     * Sets up FlatLaf and applies all application-level defaults. Call on the
     * EDT before constructing the main component tree.
     */
    void apply(AppThemePalette palette) {
        currentPalette = safePalette(palette);
        if (currentPalette.lightTheme()) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        applyDefaults(currentPalette);
    }

    /** Refreshes an existing window after a Look and Feel/defaults update. */
    void refresh(JFrame frame) {
        if (frame == null) {
            return;
        }
        Runnable refreshAction = () -> {
            SwingUtilities.updateComponentTreeUI(frame);
            frame.revalidate();
            frame.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            refreshAction.run();
        } else {
            SwingUtilities.invokeLater(refreshAction);
        }
    }

    private void applyDefaults(AppThemePalette palette) {
        UIManager.put("Component.accentColor", palette.accentColor());
        UIManager.put("Component.focusColor", palette.focusColor());

        UIManager.put("Panel.background", palette.frameBackground());
        UIManager.put("RootPane.background", palette.frameBackground());
        UIManager.put("Label.foreground", palette.textColor());

        UIManager.put("ToolBar.background", palette.panelBackground());
        UIManager.put("ToolBar.borderColor", palette.borderColor());
        UIManager.put("ToolBar.dockingBackground", palette.panelBackground());
        UIManager.put("ToolBar.overflowBackground", palette.panelBackground());

        UIManager.put("Button.background", palette.surfaceBackground());
        UIManager.put("Button.foreground", palette.textColor());
        UIManager.put("Button.hoverBackground", palette.surfaceRaised());
        UIManager.put("Button.default.background", palette.accentColor());
        UIManager.put("Button.default.foreground", palette.accentForeground());

        UIManager.put("TextField.background", palette.inputBackground());
        UIManager.put("TextField.foreground", palette.inputForeground());
        UIManager.put("TextField.caretForeground", palette.accentColor());
        UIManager.put("TextField.selectionBackground", palette.selectionBackground());
        UIManager.put("TextField.selectionForeground", palette.textColor());

        UIManager.put("SplitPane.background", palette.frameBackground());
        UIManager.put("SplitPaneDivider.background", palette.panelBackground());
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPaneDivider.gripColor", palette.mutedTextColor());
        UIManager.put("SplitPaneDivider.draggingColor", palette.accentColor());

        UIManager.put("ScrollBar.background", palette.frameBackground());
        UIManager.put("ScrollBar.track", palette.scrollbarTrack());
        UIManager.put("ScrollBar.thumb", palette.scrollbarThumb());
        UIManager.put("ScrollBar.thumbHover", palette.scrollbarThumbHover());
        UIManager.put("ScrollBar.thumbPressed", palette.scrollbarThumbPressed());

        UIManager.put("MenuBar.background", palette.panelBackground());
        UIManager.put("MenuBar.foreground", palette.textColor());
        UIManager.put("Menu.background", palette.panelBackground());
        UIManager.put("Menu.foreground", palette.textColor());
        UIManager.put("MenuItem.background", palette.panelBackground());
        UIManager.put("MenuItem.foreground", palette.textColor());
        UIManager.put("MenuItem.selectionBackground", palette.surfaceRaised());
        UIManager.put("MenuItem.selectionForeground", palette.textColor());
    }

    private static AppThemePalette safePalette(AppThemePalette palette) {
        return palette != null ? palette : AppThemePalette.dark();
    }
}
