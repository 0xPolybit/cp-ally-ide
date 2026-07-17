package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/** Collapsible Tests/Results bottom tool window. */
final class BottomToolWindow {

    private final AppThemePalette palette;
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final JTabbedPane tabs = new JTabbedPane();
    private final JToggleButton collapseButton = new JToggleButton();
    private final JButton closeButton = new JButton("×");
    private boolean collapsed;
    private int expandedHeight;
    private final JComponent testCasesComponent;
    private final JComponent resultsComponent;
    private int preferredHeight = 280;

    BottomToolWindow(JComponent testCasesComponent, JComponent resultsComponent, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        this.testCasesComponent = testCasesComponent;
        this.resultsComponent = resultsComponent;
        this.expandedHeight = preferredHeight;
        build();
    }

    private void build() {
        tabs.setBackground(palette.panelBackground());
        tabs.setForeground(palette.textColor());
        tabs.addTab("Tests", testCasesComponent);
        tabs.addTab("Results", resultsComponent);
        rootPanel.setBackground(palette.panelBackground());
        rootPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, palette.subtleBorderColor()));
        rootPanel.add(tabs, BorderLayout.CENTER);
        rootPanel.getAccessibleContext().setAccessibleName("Bottom tool window");
    }

    JComponent component() {
        return rootPanel;
    }

    int preferredExpandedHeight() {
        return Math.max(180, preferredHeight);
    }

    void setPreferredHeight(int height) {
        this.preferredHeight = Math.max(120, height);
    }

    void setExpanded(boolean expanded) {
        this.collapsed = !expanded;
        rootPanel.setVisible(expanded);
    }

    boolean isExpanded() {
        return !collapsed;
    }

    void selectResults() {
        tabs.setSelectedIndex(1);
    }

    void selectTests() {
        tabs.setSelectedIndex(0);
    }

    void setSelectedTab(String tabKey) {
        if ("results".equalsIgnoreCase(tabKey)) {
            selectResults();
        } else {
            selectTests();
        }
    }

    String selectedTabKey() {
        return tabs.getSelectedIndex() == 1 ? "results" : "tests";
    }

    void setExpandedHeight(int height) {
        this.expandedHeight = Math.max(180, height);
    }

    int expandedHeight() {
        return Math.max(180, expandedHeight);
    }
}
