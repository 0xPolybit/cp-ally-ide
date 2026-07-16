package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/** Persistent global workspace controls below the application menu. */
final class WorkspaceBar extends JPanel {

    private final AppThemePalette palette;
    private final JLabel problemLabel = new JLabel("No problem selected");
    private final StatusBadge connectivityBadge;

    WorkspaceBar(ActionRegistry actions, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        setLayout(new BorderLayout(UiTokens.SPACE_4, 0));
        setOpaque(true);
        setBackground(this.palette.panelBackground());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, this.palette.subtleBorderColor()),
                BorderFactory.createEmptyBorder(UiTokens.SPACE_2, UiTokens.SPACE_4, UiTokens.SPACE_2, UiTokens.SPACE_4)));

        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.SPACE_2, 0));
        identity.setOpaque(false);
        JLabel appLabel = new JLabel("CP Ally");
        appLabel.setForeground(this.palette.textColor());
        appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, UiTokens.BODY_FONT_SIZE));
        identity.add(appLabel);

        problemLabel.setForeground(this.palette.mutedTextColor());
        problemLabel.setFont(problemLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
        identity.add(problemLabel);
        add(identity, BorderLayout.WEST);

        connectivityBadge = new StatusBadge(
                "Checking Codeforces", StatusBadge.Kind.INFO, this.palette);
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.SPACE_2, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(connectivityBadge);
        actionsPanel.add(new JButton(actions.action(ActionRegistry.Id.CHOOSE_PROBLEM)));
        actionsPanel.add(new JButton(actions.action(ActionRegistry.Id.OPEN_EMPTY_PROBLEM)));
        actionsPanel.add(new JButton(actions.action(ActionRegistry.Id.REFRESH_PROBLEM)));
        actionsPanel.add(new JButton(actions.action(ActionRegistry.Id.PREFERENCES)));
        add(actionsPanel, BorderLayout.EAST);

        getAccessibleContext().setAccessibleName("Workspace controls");
    }

    void setProblemState(ProblemViewState state) {
        if (state == null) {
            problemLabel.setText("No problem selected");
            return;
        }
        String code = state.problemCode();
        String displayCode = code == null || code.isBlank() ? "No problem selected" : code;
        String suffix = switch (state.state()) {
            case EMPTY -> "Empty workspace";
            case LOADING -> "Loading…";
            case LOADED -> "Ready";
            case ERROR -> "Error";
        };
        problemLabel.setText(displayCode + " · " + suffix);
        problemLabel.setForeground(state.state() == ProblemLoadState.ERROR
                ? palette.errorColor()
                : palette.mutedTextColor());
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
}
