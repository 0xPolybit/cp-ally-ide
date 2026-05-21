package com.example;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;

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

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.panelBackground());
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Local execution for " + language);
        title.setForeground(theme.textColor());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        header.add(title, BorderLayout.NORTH);

        JLabel summary = new JLabel(ExecutionResultFormatter.summary(report));
        summary.setForeground(theme.mutedTextColor());
        header.add(summary, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setText(ExecutionResultFormatter.buildResultsHtml(language, report, theme));
        pane.setCaretPosition(0);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setBackground(theme.frameBackground());
        pane.setForeground(theme.textColor());

        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(theme.frameBackground());
        dialog.add(scrollPane, BorderLayout.CENTER);

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

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.panelBackground());
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Local execution for " + language);
        title.setForeground(theme.textColor());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        header.add(title, BorderLayout.NORTH);

        JLabel summary = new JLabel("Compilation failed.");
        summary.setForeground(theme.errorColor());
        header.add(summary, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);

        JEditorPane area = new JEditorPane();
        area.setEditable(false);
        area.setContentType("text/plain");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(theme.frameBackground());
        area.setForeground(theme.textColor());
        area.setCaretColor(theme.textColor());
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
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
