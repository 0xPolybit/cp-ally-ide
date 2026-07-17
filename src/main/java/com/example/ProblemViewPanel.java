package com.example;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stable problem workspace surface. It owns statement presentation and view
 * transitions while leaving fetching, rendering, and test data orchestration
 * to MainWindow and the existing services.
 */
final class ProblemViewPanel extends JPanel {

    private final AppThemePalette palette;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private JPanel metadataPanel;
    private JEditorPane problemPane;
    private JScrollPane problemScrollPane;
    private JLabel submissionStatusLabel;
    private InlineNotice latexNotice;
    private JSplitPane statementTestCasesSplitPane;
    private int cachedScrollPosition;
    private int cachedScrollMaximum;

    ProblemViewPanel(AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_4, UiTokens.SPACE_4, UiTokens.SPACE_4, UiTokens.SPACE_3));
        setBackground(this.palette.frameBackground());
        contentPanel.setOpaque(false);
        SectionHeader header = new SectionHeader(
                "Problem workspace",
                "Statement and test cases",
                this.palette);
        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Problem workspace");
    }

    void showEntry(java.awt.Component component) {
        showComponent(component);
    }

    void showLoading(java.awt.Component component) {
        showComponent(component);
    }

    void showError(java.awt.Component component) {
        showComponent(component);
    }

    void showLoaded() {
        if (statementTestCasesSplitPane != null) {
            showComponent(statementTestCasesSplitPane);
        }
    }

    void initializeDocumentSurface(
            Runnable zoomIn,
            Runnable zoomOut,
            Runnable activateZoomTarget,
            Consumer<String> hyperlinkHandler) {
        if (problemPane != null) {
            return;
        }

        problemPane = new JEditorPane() {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                if (graphics instanceof java.awt.Graphics2D graphics2D) {
                    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                }
                super.paintComponent(graphics);
            }
        };
        problemPane.setContentType("text/html");
        problemPane.setEditable(false);
        problemPane.setFocusable(false);
        problemPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        problemPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                activateZoomTarget.run();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                activateZoomTarget.run();
            }
        });
        problemPane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                    && event.getDescription() != null) {
                hyperlinkHandler.accept(event.getDescription());
            }
        });

        problemScrollPane = new JScrollPane(problemPane);
        problemScrollPane.setWheelScrollingEnabled(true);
        problemScrollPane.addMouseWheelListener(event -> {
            try {
                if (event.isControlDown()) {
                    if (event.getWheelRotation() < 0) zoomIn.run();
                    else zoomOut.run();
                    event.consume();
                }
            } catch (Exception ignored) {
            }
        });
        problemScrollPane.setBorder(BorderFactory.createEmptyBorder());
        problemScrollPane.getVerticalScrollBar().setUnitIncrement(14);
        problemScrollPane.getViewport().setBackground(palette.frameBackground());
    }

    void setDocumentHtml(String html) {
        if (problemPane == null) {
            throw new IllegalStateException("Document surface has not been initialized");
        }
        problemPane.setText(html == null ? "" : html);
        problemPane.setCaretPosition(0);
        problemPane.setBackground(palette.frameBackground());
        if (problemScrollPane != null) {
            problemScrollPane.getViewport().setBackground(palette.frameBackground());
        }
    }

    JSplitPane createStatementTestCasesSurface(java.awt.Component testCases, int preferredDivider) {
        if (statementTestCasesSplitPane != null) {
            return statementTestCasesSplitPane;
        }
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_3, 0));

        submissionStatusLabel = new JLabel("");
        submissionStatusLabel.setFont(submissionStatusLabel.getFont().deriveFont(
                java.awt.Font.BOLD, UiTokens.CAPTION_FONT_SIZE));
        submissionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, UiTokens.SPACE_3, 0, UiTokens.SPACE_1));
        submissionStatusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        submissionStatusLabel.setVisible(false);
        topBar.add(submissionStatusLabel, BorderLayout.EAST);

        JPanel statementPanel = new JPanel(new BorderLayout());
        statementPanel.setOpaque(false);
        statementPanel.add(topBar, BorderLayout.NORTH);
        statementPanel.add(problemScrollPane, BorderLayout.CENTER);

        int minTestCaseHeight = UiTokens.MIN_WINDOW_HEIGHT / 3;
        int divider = preferredDivider > 0
                ? preferredDivider
                : Math.max(160, getHeight() - minTestCaseHeight - UiTokens.SPACE_2);
        statementTestCasesSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, statementPanel, testCases);
        statementTestCasesSplitPane.setResizeWeight(0.5);
        statementTestCasesSplitPane.setDividerSize(UiTokens.DIVIDER_SIZE);
        statementTestCasesSplitPane.setBorder(BorderFactory.createEmptyBorder());
        statementTestCasesSplitPane.setDividerLocation(divider);
        return statementTestCasesSplitPane;
    }

    void setMetadata(ProblemStatementMetadata metadata) {
        if (metadataPanel == null) {
            metadataPanel = new JPanel(new GridLayout(0, 1, 0, UiTokens.SPACE_1));
            metadataPanel.setOpaque(true);
            metadataPanel.setBackground(palette.surfaceBackground());
            metadataPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(palette.subtleBorderColor()),
                    BorderFactory.createEmptyBorder(
                            UiTokens.SPACE_2, UiTokens.SPACE_3, UiTokens.SPACE_2, UiTokens.SPACE_3)));
            add(metadataPanel, BorderLayout.EAST);
        }
        metadataPanel.removeAll();
        if (metadata == null) {
            revalidate();
            repaint();
            return;
        }
        addRow(metadata.problemCode + "  ·  " + safe(metadata.title));
        if (!metadata.timeLimit.isBlank()) {
            addRow("Time: " + metadata.timeLimit);
        }
        if (!metadata.memoryLimit.isBlank()) {
            addRow("Memory: " + metadata.memoryLimit);
        }
        if (!metadata.ioMode.isBlank()) {
            addRow("I/O: " + metadata.ioMode);
        }
        if (!metadata.difficulty.isBlank()) {
            addRow("Difficulty: " + metadata.difficulty);
        }
        if (metadata.tags != null && !metadata.tags.isEmpty()) {
            addRow("Tags: " + String.join(", ", metadata.tags));
        }
        revalidate();
        repaint();
    }

    void setLatexNotice(InlineNotice notice) {
        if (latexNotice != null) {
            remove(latexNotice);
            latexNotice = null;
        }
        if (notice != null) {
            latexNotice = notice;
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_3, 0));
            wrapper.add(notice, BorderLayout.CENTER);
            add(wrapper, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
    }

    void rememberScrollPosition() {
        if (problemScrollPane == null) {
            return;
        }
        JScrollBar vertical = problemScrollPane.getVerticalScrollBar();
        cachedScrollPosition = vertical.getValue();
        cachedScrollMaximum = vertical.getMaximum();
    }

    void restoreScrollPosition() {
        if (problemScrollPane == null) {
            return;
        }
        JScrollBar vertical = problemScrollPane.getVerticalScrollBar();
        if (cachedScrollMaximum == 0) {
            vertical.setValue(0);
            return;
        }
        int proportional = Math.max(0, Math.min(
                vertical.getMaximum(),
                (int) Math.round((double) cachedScrollPosition * vertical.getMaximum() / cachedScrollMaximum)));
        vertical.setValue(proportional);
    }

    JEditorPane documentPane() {
        return problemPane;
    }

    JScrollPane documentScrollPane() {
        return problemScrollPane;
    }

    JLabel submissionStatusLabel() {
        return submissionStatusLabel;
    }

    JSplitPane statementTestCasesSplitPane() {
        return statementTestCasesSplitPane;
    }

    void setDocumentBackground(Color color) {
        if (problemPane != null) {
            problemPane.setBackground(color);
        }
        if (problemScrollPane != null) {
            problemScrollPane.getViewport().setBackground(color);
        }
    }

    private void addRow(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(palette.textColor());
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
        metadataPanel.add(label);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showComponent(java.awt.Component component) {
        contentPanel.removeAll();
        if (component != null) {
            contentPanel.add(component, BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
