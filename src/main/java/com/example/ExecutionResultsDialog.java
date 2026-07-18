package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

        JPanel footer = buildFooter(dialog, language, report, theme);
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel buildFooter(JDialog owner, String language,
                                      CodeExecutionService.ExecutionReport report,
                                      AppThemePalette theme) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(theme.panelBackground());
        footer.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> owner.dispose());
        owner.getRootPane().registerKeyboardAction(
                e -> owner.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton copyButton = new JButton("Copy Report");
        copyButton.addActionListener(e -> {
            String text = ExecutionResultFormatter.buildResultsText(language, report);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        });
        JButton saveButton = new JButton("Save Report…");
        saveButton.addActionListener(e -> saveReportToFile(owner, language, report));
        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(copyButton);
        buttonRow.add(saveButton);
        buttonRow.add(closeButton);
        footer.add(buttonRow, BorderLayout.EAST);
        return footer;
    }

    private static void saveReportToFile(JDialog owner, String language,
                                        CodeExecutionService.ExecutionReport report) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save execution report");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        chooser.setSelectedFile(new java.io.File("cp-ally-report.txt"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(target,
                    ExecutionResultFormatter.buildResultsText(language, report),
                    StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            javax.swing.JOptionPane.showMessageDialog(owner,
                    "Failed to save report:\n" + ioe.getMessage(),
                    "Save error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
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

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        footer.add(close);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // Suppress unused-import warning for Frame in case future
    // enhancements need it.
    @SuppressWarnings("unused")
    private static final Class<?> UNUSED = java.awt.Frame.class;
}
