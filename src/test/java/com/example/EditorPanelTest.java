package com.example;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EditorPanelTest {

    @Test
    void editorPanelBuildsItsSurfaceThroughTheControllerBoundary() {
        ActionRegistry actions = new ActionRegistry();
        actions.register(ActionRegistry.Id.RUN_CODE, "Run Code", null, () -> {});

        EditorPanel panel = new EditorPanel(actions, new EditorPanel.Controller() {
            @Override public AppSettings settings() { return AppSettings.defaults("Python 3"); }
            @Override public AppThemePalette palette() { return AppThemePalette.dark(); }
            @Override public void disableFocus(Component component) { component.setFocusable(false); }
            @Override public void applyEditorPreferences(RSyntaxTextArea editor, int fontSize, String colorScheme, boolean useTabsAsSpaces, int tabSpacing) {}
            @Override public void installEditorAutoPairs(RSyntaxTextArea editor) {}
            @Override public void saveCurrentProgramToCache(String language) {}
            @Override public void updateExecutionAvailability() {}
            @Override public boolean problemStatementLoaded() { return false; }
            @Override public void applyLanguageTemplateOrCachedProgram() {}
            @Override public void setActiveEditorZoomTarget() {}
            @Override public void zoomEditorIn() {}
            @Override public void zoomEditorOut() {}
            @Override public void applyEditorZoom(RSyntaxTextArea editor) {}
            @Override public void onRuntimeSupportClicked() {}
            @Override public Action runAction() { return actions.action(ActionRegistry.Id.RUN_CODE); }
        });

        assertNotNull(panel.editor());
        assertNotNull(panel.scrollPane());
        assertNotNull(panel.languageDropdown());
        assertNotNull(panel.runButton());
        assertEquals("Python 3", panel.languageDropdown().getSelectedItem());
    }
}
