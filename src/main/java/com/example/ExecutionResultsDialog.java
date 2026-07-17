package com.example;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Frame;

/** Dialog hosting the native {@link ExecutionResultsView}. */
final class ExecutionResultsDialog {

    private ExecutionResultsDialog() {
    }

    static void show(Frame owner, String language, CodeExecutionService.ExecutionReport report, AppThemePalette palette) {
        if (report == null) {
            return;
        }

        AppThemePalette theme = palette != null ? palette : AppThemePalette.dark();
        if (!report.success()) {
            showCompilationErrorDialog(owner, language, report.failureMessage(), theme);
            return;
        }

        JDialog dialog = new JDialog(owner, "Execution Results", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(theme.frameBackground());

        SectionHeader header = new SectionHeader(
                "Local execution for " + language,
                ExecutionResultFormatter.summary(report),
                theme);
        header.setOpaque(true);
        header.setBackground(theme.panelBackground());
        dialog.add(header, BorderLayout.NORTH);

        ExecutionResultsView view = new ExecutionResultsView(theme);
        view.setReport(report);
        dialog.add(view.component(), BorderLayout.CENTER);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    static void showCompilationError(Frame owner, String language, String failureMessage, AppThemePalette palette) {
        showCompilationErrorDialog(owner, language, failureMessage, palette != null ? palette : AppThemePalette.dark());
    }

    private static void showCompilationErrorDialog(Frame owner, String language, String failureMessage, AppThemePalette theme) {
        JDialog dialog = new JDialog(owner, "Execution Results", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(theme.frameBackground());

        SectionHeader header = new SectionHeader(
                "Local execution for " + language,
                "Compilation failed",
                theme);
        header.setOpaque(true);
        header.setBackground(theme.panelBackground());
        dialog.add(header, BorderLayout.NORTH);

        javax.swing.JEditorPane area = new javax.swing.JEditorPane();
        area.setEditable(false);
        area.setContentType("text/plain");
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, (int) UiTokens.CAPTION_FONT_SIZE));
        area.setBackground(theme.frameBackground());
        area.setForeground(theme.textColor());
        area.setCaretColor(theme.textColor());
        area.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3));
        area.setText(failureMessage == null || failureMessage.isBlank() ? "Compilation failed." : failureMessage);
        area.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(theme.frameBackground());
        dialog.add(scrollPane, BorderLayout.CENTER);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
