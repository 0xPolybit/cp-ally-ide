package com.example;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Owns editor-local controls, toolbar state, and the RSyntaxTextArea surface. */
final class EditorPanel extends JPanel {

    interface Controller {
        AppSettings settings();
        AppThemePalette palette();
        void disableFocus(Component component);
        void applyEditorPreferences(RSyntaxTextArea editor, int fontSize, String colorScheme,
                                    boolean useTabsAsSpaces, int tabSpacing);
        void installEditorAutoPairs(RSyntaxTextArea editor);
        void saveCurrentProgramToCache(String language);
        void updateExecutionAvailability();
        boolean problemStatementLoaded();
        void applyLanguageTemplateOrCachedProgram();
        void setActiveEditorZoomTarget();
        void zoomEditorIn();
        void zoomEditorOut();
        void applyEditorZoom(RSyntaxTextArea editor);
        void onRuntimeSupportClicked();
        Action runAction();
    }

    private static final String[] LANGUAGES = {
            "Python 3",
            "GNU G++17 7.3.0",
            "GNU G++20 13.2",
            "GNU C11 5.1.0",
            "GNU G11 5.1.0",
            "Java 21",
            "Kotlin 1.9",
            "C# 8",
            "Go 1.22",
            "Rust 2021",
            "Node.js 20",
            "PHP 8.2",
            "Ruby 3.2",
            "Perl 5",
            "Haskell GHC 8.10",
            "OCaml 4.02",
            "Scala 2.12",
            "Pascal 3.0",
            "JavaScript V8",
            "PyPy 3"
    };

    private final Controller controller;
    private final AppThemePalette palette;
    private final JComboBox<String> languageDropdown;
    private final RSyntaxTextArea codeEditor;
    private final RTextScrollPane codeScrollPane;
    private final javax.swing.JButton runButton;
    private final JLabel runtimeSupportLabel;
    private final JLabel executionStateLabel;

    EditorPanel(ActionRegistry actions, Controller controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.palette = controller.palette() != null ? controller.palette() : AppThemePalette.dark();
        setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_4, UiTokens.SPACE_3, UiTokens.SPACE_4, UiTokens.SPACE_4));
        setBackground(this.palette.frameBackground());

        JToolBar editorToolbar = new JToolBar();
        editorToolbar.setFloatable(false);
        editorToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_3, 0));
        editorToolbar.setOpaque(false);
        controller.disableFocus(editorToolbar);

        runButton = new javax.swing.JButton(actions.action(ActionRegistry.Id.RUN_CODE));
        runButton.setEnabled(false);
        applyRunButtonIcons();
        editorToolbar.add(runButton);
        editorToolbar.add(Box.createHorizontalGlue());
        editorToolbar.add(Box.createHorizontalStrut(UiTokens.SPACE_5));

        runtimeSupportLabel = new JLabel("Checking");
        runtimeSupportLabel.setForeground(palette.mutedTextColor());
        runtimeSupportLabel.setFont(runtimeSupportLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
        runtimeSupportLabel.setMaximumSize(new Dimension(100, UiTokens.CONTROL_HEIGHT));
        runtimeSupportLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runtimeSupportLabel.getAccessibleContext().setAccessibleName("Runtime support details");
        runtimeSupportLabel.setToolTipText("Show runtime support details");
        runtimeSupportLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                controller.onRuntimeSupportClicked();
            }
        });
        editorToolbar.add(runtimeSupportLabel);
        editorToolbar.add(Box.createHorizontalStrut(UiTokens.SPACE_1));

        JLabel hintIconLabel = createHintIconLabel();
        editorToolbar.add(hintIconLabel);
        editorToolbar.add(Box.createHorizontalStrut(UiTokens.SPACE_3));

        executionStateLabel = new JLabel("Status: Idle");
        executionStateLabel.setForeground(palette.mutedTextColor());
        executionStateLabel.setFont(executionStateLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
        executionStateLabel.getAccessibleContext().setAccessibleName("Execution status");
        editorToolbar.add(executionStateLabel);
        editorToolbar.add(Box.createHorizontalStrut(UiTokens.SPACE_3));

        languageDropdown = new JComboBox<>(LANGUAGES);
        AppSettings settings = controller.settings();
        String preferredLanguage = settings != null ? settings.lastLanguage() : "Python 3";
        languageDropdown.setSelectedItem(preferredLanguage);
        if (languageDropdown.getSelectedItem() == null) {
            languageDropdown.setSelectedItem("Python 3");
        }
        languageDropdown.setPreferredSize(new Dimension(190, UiTokens.CONTROL_HEIGHT));
        languageDropdown.setMaximumSize(new Dimension(220, UiTokens.CONTROL_HEIGHT));
        languageDropdown.setBackground(palette.inputBackground());
        languageDropdown.setForeground(palette.inputForeground());
        languageDropdown.setFocusable(false);
        languageDropdown.setRequestFocusEnabled(false);
        languageDropdown.getAccessibleContext().setAccessibleName("Programming language");
        languageDropdown.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.DESELECTED) {
                controller.saveCurrentProgramToCache(event.getItem() != null ? event.getItem().toString() : null);
                return;
            }
            controller.updateExecutionAvailability();
            if (controller.problemStatementLoaded()) {
                controller.applyLanguageTemplateOrCachedProgram();
            }
        });
        editorToolbar.add(languageDropdown);

        codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeEditor.setTabSize(settings != null ? settings.tabSpacing() : 4);
        codeEditor.setCodeFoldingEnabled(false);
        codeEditor.setEditable(false);
        codeEditor.setFocusable(false);
        codeEditor.setRequestFocusEnabled(false);
        controller.applyEditorPreferences(
                codeEditor,
                settings != null ? settings.editorFontSize() : 14,
                settings != null ? settings.editorColorScheme() : "Eclipse Dark",
                settings != null && settings.useTabsAsSpaces(),
                settings != null ? settings.tabSpacing() : 4);
        controller.installEditorAutoPairs(codeEditor);
        codeEditor.setText("Select a problem to get started...");
        codeEditor.setCaretPosition(0);
        installZoomAndMouseBehavior();

        codeScrollPane = new RTextScrollPane(codeEditor);
        installScrollBehavior();
        codeScrollPane.setFoldIndicatorEnabled(true);
        codeScrollPane.setBorder(BorderFactory.createLineBorder(palette.borderColor()));
        codeScrollPane.getGutter().setBackground(palette.gutterBackground());
        codeScrollPane.getGutter().setLineNumberColor(palette.mutedTextColor());
        codeScrollPane.getGutter().setBorderColor(palette.borderColor());
        codeScrollPane.getVerticalScrollBar().setBackground(palette.frameBackground());
        codeScrollPane.getVerticalScrollBar().setForeground(palette.scrollbarThumb());
        codeScrollPane.getHorizontalScrollBar().setBackground(palette.frameBackground());
        codeScrollPane.getHorizontalScrollBar().setForeground(palette.scrollbarThumb());
        codeScrollPane.getVerticalScrollBar().setUnitIncrement(14);
        codeScrollPane.setBackground(palette.panelBackground());
        controller.applyEditorZoom(codeEditor);

        add(editorToolbar, BorderLayout.NORTH);
        add(codeScrollPane, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Code editor");
    }

    RSyntaxTextArea editor() {
        return codeEditor;
    }

    RTextScrollPane scrollPane() {
        return codeScrollPane;
    }

    JComboBox<String> languageDropdown() {
        return languageDropdown;
    }

    javax.swing.JButton runButton() {
        return runButton;
    }

    JLabel runtimeSupportLabel() {
        return runtimeSupportLabel;
    }

    JLabel executionStateLabel() {
        return executionStateLabel;
    }

    private void installZoomAndMouseBehavior() {
        codeEditor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                controller.setActiveEditorZoomTarget();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                controller.setActiveEditorZoomTarget();
            }
        });

        javax.swing.Action zoomInAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                controller.zoomEditorIn();
            }
        };
        javax.swing.Action zoomOutAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                controller.zoomEditorOut();
            }
        };

        javax.swing.InputMap inputMap = codeEditor.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = codeEditor.getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ADD, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        actionMap.put("zoomIn", zoomInAction);
        actionMap.put("zoomOut", zoomOutAction);
    }

    private void installScrollBehavior() {
        codeScrollPane.setWheelScrollingEnabled(true);
        codeScrollPane.addMouseWheelListener(event -> {
            try {
                if (event.isControlDown()) {
                    if (event.getWheelRotation() < 0) controller.zoomEditorIn();
                    else controller.zoomEditorOut();
                    event.consume();
                    return;
                }
                JScrollBar vertical = codeScrollPane.getVerticalScrollBar();
                int increment = Math.max(1, vertical.getUnitIncrement());
                vertical.setValue(vertical.getValue() + event.getUnitsToScroll() * increment);
                event.consume();
            } catch (Exception ignored) {
            }
        });
        codeEditor.addMouseWheelListener(event -> {
            try {
                if (event.isControlDown()) {
                    if (event.getWheelRotation() < 0) controller.zoomEditorIn();
                    else controller.zoomEditorOut();
                    event.consume();
                    return;
                }
                JScrollBar vertical = codeScrollPane.getVerticalScrollBar();
                int increment = Math.max(1, vertical.getUnitIncrement());
                vertical.setValue(vertical.getValue() + event.getUnitsToScroll() * increment);
                event.consume();
            } catch (Exception ignored) {
            }
        });
    }

    private JLabel createHintIconLabel() {
        JLabel label = new JLabel();
        javax.swing.ImageIcon icon = UiIconLoader.loadThemedClasspathIcon(
                "hint.png", palette, UiTokens.ICON_SMALL, UiTokens.ICON_SMALL);
        if (icon != null) {
            label.setIcon(icon);
        }
        label.setMaximumSize(new Dimension(UiTokens.ICON_NORMAL, UiTokens.ICON_NORMAL));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setToolTipText("Show runtime support details");
        label.getAccessibleContext().setAccessibleName("Runtime support details");
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                controller.onRuntimeSupportClicked();
            }
        });
        return label;
    }

    private void applyRunButtonIcons() {
        javax.swing.ImageIcon normal = UiIconLoader.loadThemedClasspathIcon(
                "run.png", palette, UiTokens.ICON_LARGE, UiTokens.ICON_LARGE);
        javax.swing.ImageIcon hover = UiIconLoader.loadScaledClasspathIcon(
                "/assets/run-hover.png", UiTokens.ICON_LARGE, UiTokens.ICON_LARGE);
        if (normal == null) {
            return;
        }
        runButton.setText("");
        runButton.setIcon(normal);
        runButton.setDisabledIcon(normal);
        runButton.setRolloverEnabled(hover != null);
        if (hover != null) {
            runButton.setRolloverIcon(hover);
        }
        runButton.setPreferredSize(UiTokens.compactIconButtonSize());
        runButton.setToolTipText("Run code (Ctrl+R)");
        runButton.getAccessibleContext().setAccessibleName("Run code");
    }
}
