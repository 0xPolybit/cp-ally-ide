package com.example;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Font;

/** Bottom status surface for connection, save, execution, and zoom state. */
final class ApplicationStatusBar extends JPanel {

    private final StatusBadge connectivityBadge;
    private final StatusBadge saveBadge;
    private final StatusBadge executionBadge;
    private final JLabel zoomLabel = new JLabel();

    ApplicationStatusBar(AppThemePalette palette) {
        AppThemePalette theme = palette != null ? palette : AppThemePalette.dark();
        setLayout(new FlowLayout(FlowLayout.LEFT, UiTokens.SPACE_2, UiTokens.SPACE_1));
        setOpaque(true);
        setBackground(theme.panelBackground());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.subtleBorderColor()));

        connectivityBadge = new StatusBadge("Checking Codeforces", StatusBadge.Kind.INFO, theme);
        saveBadge = new StatusBadge("Not saved", StatusBadge.Kind.NEUTRAL, theme);
        executionBadge = new StatusBadge("Idle", StatusBadge.Kind.NEUTRAL, theme);
        add(connectivityBadge);
        add(saveBadge);
        add(executionBadge);

        zoomLabel.setForeground(theme.mutedTextColor());
        zoomLabel.setFont(zoomLabel.getFont().deriveFont(Font.PLAIN, UiTokens.SMALL_CAPTION_FONT_SIZE));
        add(zoomLabel);
        getAccessibleContext().setAccessibleName("Application status");
    }

    void setConnectivity(ConnectivityState state, String message) {
        StatusBadge.Kind kind = switch (state == null ? ConnectivityState.CHECKING : state) {
            case ONLINE -> StatusBadge.Kind.SUCCESS;
            case DEGRADED -> StatusBadge.Kind.WARNING;
            case OFFLINE -> StatusBadge.Kind.ERROR;
            case CHECKING -> StatusBadge.Kind.INFO;
        };
        connectivityBadge.setStatus(message == null || message.isBlank() ? "Checking Codeforces" : message, kind);
    }

    void setSaveState(SaveState state) {
        SaveState safe = state == null ? SaveState.DISABLED : state;
        StatusBadge.Kind kind = switch (safe) {
            case CLEAN -> StatusBadge.Kind.SUCCESS;
            case DIRTY, SAVING -> StatusBadge.Kind.WARNING;
            case DISABLED -> StatusBadge.Kind.NEUTRAL;
        };
        String text = switch (safe) {
            case CLEAN -> "Saved";
            case DIRTY -> "Unsaved changes";
            case SAVING -> "Saving…";
            case DISABLED -> "Save disabled";
        };
        saveBadge.setStatus(text, kind);
    }

    void setExecutionState(ExecutionState state) {
        ExecutionState safe = state == null ? ExecutionState.IDLE : state;
        StatusBadge.Kind kind = switch (safe) {
            case RUNNING -> StatusBadge.Kind.WARNING;
            case COMPLETE -> StatusBadge.Kind.SUCCESS;
            case FAILED -> StatusBadge.Kind.ERROR;
            case IDLE -> StatusBadge.Kind.NEUTRAL;
        };
        String text = switch (safe) {
            case RUNNING -> "Running";
            case COMPLETE -> "Complete";
            case FAILED -> "Failed";
            case IDLE -> "Idle";
        };
        executionBadge.setStatus(text, kind);
    }

    void setZoomText(String text) {
        zoomLabel.setText(text == null ? "" : text);
    }
}
