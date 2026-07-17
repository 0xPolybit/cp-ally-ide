package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

/** Native execution results surface backed by an {@link CodeExecutionService.ExecutionReport}. */
final class ExecutionResultsView {

    private final AppThemePalette palette;
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final JList<CodeExecutionService.TestCaseResult> resultList = new JList<>();
    private final JTextArea detailArea = new JTextArea();
    private final JLabel summaryLabel = new JLabel(" ");

    ExecutionResultsView(AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        rootPanel.setBackground(palette.frameBackground());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3));

        summaryLabel.setForeground(palette.mutedTextColor());
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_2, 0));
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        rootPanel.add(summaryPanel, BorderLayout.NORTH);

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
                renderResult(value, isSelected));
        resultList.setBackground(palette.surfaceBackground());
        resultList.setForeground(palette.textColor());
        resultList.addListSelectionListener(this::handleSelection);

        JScrollPane listScroll = new JScrollPane(resultList);
        listScroll.setBorder(BorderFactory.createLineBorder(palette.subtleBorderColor()));
        listScroll.setPreferredSize(new java.awt.Dimension(220, 0));

        detailArea.setEditable(false);
        detailArea.setFocusable(false);
        detailArea.setBackground(palette.surfaceBackground());
        detailArea.setForeground(palette.textColor());
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) UiTokens.CAPTION_FONT_SIZE));
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(false);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(palette.subtleBorderColor()));
        detailScroll.getVerticalScrollBar().setUnitIncrement(10);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        split.setResizeWeight(0.4);
        split.setDividerSize(UiTokens.DIVIDER_SIZE);
        split.setBorder(BorderFactory.createEmptyBorder());
        rootPanel.add(split, BorderLayout.CENTER);
        rootPanel.getAccessibleContext().setAccessibleName("Execution results");
    }

    JComponent component() {
        return rootPanel;
    }

    JSplitPane resultSplit() {
        return (JSplitPane) rootPanel.getComponent(1);
    }

    JList<CodeExecutionService.TestCaseResult> resultList() {
        return resultList;
    }

    void setReport(CodeExecutionService.ExecutionReport report) {
        summaryLabel.setText(ExecutionResultFormatter.summary(report));
        List<CodeExecutionService.TestCaseResult> results = report.results();
        resultList.setListData(results.toArray(new CodeExecutionService.TestCaseResult[0]));
        if (!results.isEmpty()) {
            int firstFailure = indexOfFirstFailure(results);
            resultList.setSelectedIndex(Math.max(0, firstFailure));
        } else {
            detailArea.setText("");
        }
    }

    private void handleSelection(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        CodeExecutionService.TestCaseResult result = resultList.getSelectedValue();
        detailArea.setText(result == null ? "" : renderDetail(result));
        detailArea.setCaretPosition(0);
    }

    private static int indexOfFirstFailure(List<CodeExecutionService.TestCaseResult> results) {
        for (int i = 0; i < results.size(); i++) {
            CodeExecutionService.TestCaseResult result = results.get(i);
            if (result.timedOut() || result.unknown() || !result.passed()) {
                return i;
            }
        }
        return 0;
    }

    private static String renderDetail(CodeExecutionService.TestCaseResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Input\n");
        builder.append(result.input() == null ? "" : result.input());
        builder.append("\n\nOutput\n");
        builder.append(result.actualOutput() == null ? "" : result.actualOutput());
        if (!result.passed() || result.timedOut()) {
            builder.append("\n\nExpected Output\n");
            builder.append(result.expectedOutput() == null ? "" : result.expectedOutput());
        }
        if (result.stderrOutput() != null && !result.stderrOutput().isBlank()) {
            builder.append("\n\nError Output\n");
            builder.append(result.stderrOutput());
        }
        if (result.note() != null && !result.note().isBlank()) {
            builder.append("\n\nNote\n");
            builder.append(result.note());
        }
        return builder.toString();
    }

    private JLabel renderResult(CodeExecutionService.TestCaseResult result, boolean isSelected) {
        JLabel label = new JLabel(formatRowLabel(result));
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        Color background;
        Color foreground;
        if (isSelected) {
            background = palette.surfaceRaised();
            foreground = palette.textColor();
        } else {
            background = palette.surfaceBackground();
            foreground = palette.mutedTextColor();
        }
        label.setBackground(background);
        label.setForeground(foreground);
        return label;
    }

    private static String formatRowLabel(CodeExecutionService.TestCaseResult result) {
        String name = result.displayName() == null ? "" : result.displayName();
        String status;
        ColorKind color;
        if (result.timedOut()) {
            status = "Time limit exceeded";
            color = ColorKind.WARNING;
        } else if (result.unknown()) {
            status = "No expected output";
            color = ColorKind.WARNING;
        } else if (result.passed()) {
            status = "Passed";
            color = ColorKind.SUCCESS;
        } else {
            status = "Failed";
            color = ColorKind.ERROR;
        }
        return name + "  ·  " + status + "  ·  " + result.durationMillis() + " ms";
    }

    private enum ColorKind { SUCCESS, WARNING, ERROR, NEUTRAL }
}
