package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestCasesPanel {

    private static final Color SURFACE = new Color(30, 31, 34);
    private static final Color PANEL = new Color(43, 45, 48);
    private static final Color BORDER = new Color(67, 71, 76);
    private static final Color TEXT = new Color(223, 225, 229);
    private static final Color CODE = new Color(24, 26, 29);

    private final Frame owner;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private final List<CodeExecutionService.TestCaseSpec> customTestCases = new ArrayList<>();
    private final JTabbedPane testCasesTabs = new JTabbedPane();
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private List<CodeExecutionService.TestCaseSpec> sampleTestCases = List.of();

    TestCasesPanel(Frame owner) {
        this.owner = owner;
        testCasesTabs.setBackground(PANEL);
        testCasesTabs.setForeground(TEXT);

        JButton addTestCaseButton = new JButton("Add Test Case");
        addTestCaseButton.setFocusable(false);
        addTestCaseButton.setRequestFocusEnabled(false);
        addTestCaseButton.setPreferredSize(new Dimension(160, 32));
        addTestCaseButton.addActionListener(e -> showAddCustomTestCaseDialog());

        JLabel sectionLabel = new JLabel("Test Cases");
        sectionLabel.setForeground(TEXT);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topBar.add(sectionLabel, BorderLayout.WEST);
        topBar.add(addTestCaseButton, BorderLayout.EAST);

        rootPanel.setOpaque(false);
        rootPanel.add(topBar, BorderLayout.NORTH);
        rootPanel.add(testCasesTabs, BorderLayout.CENTER);

        refreshTabs(-1);
    }

    JPanel createPanel() {
        return rootPanel;
    }

    void setSamplePayloads(Map<String, String> payloads) {
        copyPayloads.clear();
        if (payloads != null) {
            copyPayloads.putAll(payloads);
        }
        customTestCases.clear();
        refreshTabs(-1);
    }

    List<CodeExecutionService.TestCaseSpec> getExecutionTestCases() {
        List<CodeExecutionService.TestCaseSpec> testCases = new ArrayList<>(sampleTestCases);
        testCases.addAll(customTestCases);
        return testCases;
    }

    private void refreshTabs(int selectedIndex) {
        sampleTestCases = SampleTestCaseCollector.collect(copyPayloads);
        testCasesTabs.removeAll();

        if (sampleTestCases.isEmpty() && customTestCases.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(SURFACE);
            testCasesTabs.addTab("No Test Cases", emptyPanel);
            testCasesTabs.setSelectedIndex(0);
            return;
        }

        for (int i = 0; i < sampleTestCases.size(); i++) {
            testCasesTabs.addTab("Test " + (i + 1), createTestCasePanel(sampleTestCases.get(i)));
        }

        for (int i = 0; i < customTestCases.size(); i++) {
            int customIndex = i;
            String title = "Custom Test Case " + (i + 1);
            testCasesTabs.addTab(title, createTestCasePanel(customTestCases.get(i)));
            testCasesTabs.setTabComponentAt(sampleTestCases.size() + i, createClosableTabHeader(title, () -> {
                if (customIndex >= 0 && customIndex < customTestCases.size()) {
                    customTestCases.remove(customIndex);
                    refreshTabs(Math.max(0, sampleTestCases.size() + customIndex - 1));
                }
            }));
        }

        if (selectedIndex >= 0 && selectedIndex < testCasesTabs.getTabCount()) {
            testCasesTabs.setSelectedIndex(selectedIndex);
        } else if (testCasesTabs.getTabCount() > 0) {
            testCasesTabs.setSelectedIndex(0);
        }
    }

    private JPanel createClosableTabHeader(String title, Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setForeground(TEXT);

        JButton closeButton = new JButton("x");
        closeButton.setFocusable(false);
        closeButton.setRequestFocusEnabled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        closeButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
        closeButton.addActionListener(e -> onClose.run());

        header.add(label, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createTestCasePanel(CodeExecutionService.TestCaseSpec testCase) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(SURFACE);
        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(createTestDataPanel("Input", testCase.input()));
        String expectedOutput = testCase.expectedOutputProvided() ? testCase.expectedOutput() : "Not provided";
        splitPane.setRightComponent(createTestDataPanel("Expected Output", expectedOutput));

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTestDataPanel(String title, String data) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        JTextArea textArea = new JTextArea(data);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBackground(CODE);
        textArea.setForeground(new Color(217, 221, 228));
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setMargin(new java.awt.Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.setBackground(SURFACE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void showAddCustomTestCaseDialog() {
        JDialog dialog = new JDialog(owner, "Add Test Case", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SURFACE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel inputLabel = new JLabel("Input (required)");
        inputLabel.setForeground(TEXT);
        content.add(inputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea inputArea = new JTextArea(8, 40);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputArea.setBackground(CODE);
        inputArea.setForeground(TEXT);
        inputArea.setCaretColor(TEXT);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        inputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(inputScroll);
        content.add(Box.createVerticalStrut(12));

        JLabel outputLabel = new JLabel("Expected Output (optional)");
        outputLabel.setForeground(TEXT);
        content.add(outputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea outputArea = new JTextArea(8, 40);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(CODE);
        outputArea.setForeground(TEXT);
        outputArea.setCaretColor(TEXT);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        outputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(outputScroll);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton saveButton = new JButton("Add");
        saveButton.setFocusable(false);
        saveButton.addActionListener(e -> {
            String input = inputArea.getText();
            if (input == null || input.isBlank()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Input is required.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String expectedOutput = outputArea.getText();
            boolean hasExpectedOutput = expectedOutput != null && !expectedOutput.isBlank();
                String normalizedInput = ensureTrailingNewline(input);
                String normalizedExpectedOutput = hasExpectedOutput ? ensureTrailingNewline(expectedOutput) : "";
            customTestCases.add(new CodeExecutionService.TestCaseSpec(
                    normalizedInput,
                    normalizedExpectedOutput,
                    true,
                    hasExpectedOutput,
                    "Custom Test Case " + (customTestCases.size() + 1)));
            refreshTabs(sampleTestCases.size() + customTestCases.size() - 1);
            dialog.dispose();
        });

        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        actions.add(buttonRow, BorderLayout.EAST);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(640, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private String ensureTrailingNewline(String value) {
        if (value == null || value.isEmpty()) {
            return "\n";
        }
        return value.endsWith("\n") ? value : value + "\n";
    }
}