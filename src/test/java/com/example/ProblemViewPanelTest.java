package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProblemViewPanelTest {

    @Test
    void problemPanelOwnsDocumentAndLoadedSplitSurface() {
        ProblemViewPanel panel = new ProblemViewPanel(AppThemePalette.dark());
        panel.initializeDocumentSurface(() -> {}, () -> {}, () -> {}, description -> {});
        panel.setDocumentHtml("<html><body>Statement</body></html>");
        var split = panel.createStatementTestCasesSurface(new JPanel(), 120);
        panel.showLoaded();

        assertNotNull(panel.documentPane());
        assertNotNull(panel.documentScrollPane());
        assertNotNull(panel.submissionStatusLabel());
        assertEquals(120, split.getDividerLocation());
        assertNotNull(panel.getAccessibleContext().getAccessibleName());
    }

    @Test
    void problemPanelCanSwitchStableEntryLoadingAndErrorViews() {
        ProblemViewPanel panel = new ProblemViewPanel(AppThemePalette.light());
        JLabel entry = new JLabel("Entry");
        JLabel loading = new JLabel("Loading");
        JLabel error = new JLabel("Error");

        panel.showEntry(entry);
        panel.showLoading(loading);
        panel.showError(error);

        assertEquals(2, panel.getComponentCount());
    }
}
