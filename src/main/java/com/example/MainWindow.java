package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainWindow {

    private static final String APP_NAME = "Competitive Programming Ally";
    private static final String PROBLEM_PLACEHOLDER = "Enter problem code (eg: 2208A)";
    private static final Pattern PROBLEM_CODE_PATTERN = Pattern.compile("^(\\d{1,6})([A-Za-z][A-Za-z0-9]{0,2})$");
    private static final String DEFAULT_LANGUAGE = "Python 3";
    private static final String DEFAULT_EDITOR_THEME = "Eclipse Dark";
    private static final String DEFAULT_APP_THEME = "Dark";
    private static final String EMPTY_PROBLEM_CODE = "__EMPTY_PROBLEM__";
    private static final String SETTINGS_DIR_NAME = "CompetitiveProgrammingAlly";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final String CURRENT_APP_VERSION = "0.2.0";
    private static final String VERSION_SOURCE_URL = "https://pastebin.com/raw/uzU8MUWs";
    private static final String RELEASES_URL = "https://github.com/0xPolybit/cp-ally-ide/releases";
    private static final Pattern SEMVER_PATTERN = Pattern.compile("\\b(\\d+\\.\\d+\\.\\d+)\\b");
    private static final int LEFT_FIELD_WIDTH = 280;
    private static final int LEFT_FIELD_HEIGHT = 32;
    private static final int MIN_WINDOW_WIDTH = 1000;
    private static final int MIN_WINDOW_HEIGHT = 680;
    private static final int MIN_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 420;
    private static final int RUN_ICON_SIZE = 24;

    private final SettingsRepository settingsRepository = new SettingsRepository(
            SETTINGS_DIR_NAME,
            SETTINGS_FILE_NAME,
            DEFAULT_LANGUAGE);
        private final ProgramCacheRepository programCacheRepository = new ProgramCacheRepository(settingsRepository.getAppDataDirectory());
    private CodeforcesService codeforcesService;
    private final CodeExecutionService codeExecutionService = new CodeExecutionService();

    private ProblemHtmlRenderer problemHtmlRenderer;
    private JButton initialFocusButton;
    private JComboBox<String> languageDropdown;
    private JLabel runtimeSupportLabel;
    private JLabel executionStateLabel;
    private AppSettings appSettings;
    private JFrame mainFrame;
    private JTextField problemCodeInput;
    private JButton fetchProblemButton;
    private JLabel fetchStatusLabel;
    private JButton runButton;
    private RSyntaxTextArea codeEditor;
    private RTextScrollPane codeScrollPane;
    private JPanel leftPanelContainer;
    private JPanel problemEntryPanel;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private JSplitPane contentSplitPane;
    private JSplitPane statementTestCasesSplitPane;
    private TestCasesPanel testCasesPanel;
    private boolean problemStatementLoaded;
    private boolean currentProblemIsEmpty;
    private String currentProblemCode;
    private AppThemePalette appThemePalette = AppThemePalette.dark();
    private java.util.concurrent.ScheduledExecutorService autosaveExecutor;
    private volatile String lastAutosavedSource = null;
    private double editorZoomFactor = 1.0;
    private double problemZoomFactor = 1.0;
    private static final double ZOOM_MIN = 0.25;
    private static final double ZOOM_MAX = 4.0;
    private static final double ZOOM_STEP = 0.05;
    private javax.swing.JEditorPane problemPane;
    private ProblemDetails currentProblemDetails;
    private javax.swing.JLabel zoomPercentLabel;
    private enum ZoomTarget { EDITOR, PROBLEM }
    private ZoomTarget activeZoomTarget = ZoomTarget.EDITOR;

    public void showWindow() {
        appSettings = settingsRepository.load();
        appThemePalette = AppThemePalette.fromName(appSettings != null ? appSettings.appTheme() : DEFAULT_APP_THEME);

        JFrame.setDefaultLookAndFeelDecorated(true);
        applyAppTheme(appThemePalette);

        Path appDataDir = settingsRepository.getAppDataDirectory();
        problemHtmlRenderer = new ProblemHtmlRenderer(appDataDir);
        problemHtmlRenderer.setTheme(appThemePalette);
        codeforcesService = new CodeforcesService(appDataDir);

        JFrame frame = new JFrame(APP_NAME + " v" + CURRENT_APP_VERSION);
        UiIconLoader.applyWindowIcon(frame, "/assets/logo.png");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        frame.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", true);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground", appThemePalette.titleBarBackground());
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground", appThemePalette.titleBarForeground());
        frame.setJMenuBar(createEmbeddedTitleBar());
        mainFrame = frame;
        testCasesPanel = new TestCasesPanel(mainFrame, appThemePalette);

        frame.add(createContentSplit(), BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAutosave();
                saveCurrentProgramToCache();
                persistSettings(frame);
            }
        });

        applyWindowSettings(frame, appSettings);
        frame.setVisible(true);

        if (contentSplitPane != null && appSettings.dividerLocation() > 0) {
            SwingUtilities.invokeLater(() -> contentSplitPane.setDividerLocation(appSettings.dividerLocation()));
        }

        if (statementTestCasesSplitPane != null && appSettings.testCasesDividerLocation() > 0) {
            SwingUtilities.invokeLater(() -> statementTestCasesSplitPane.setDividerLocation(appSettings.testCasesDividerLocation()));
        }

        if (appSettings.maximized()) {
            SwingUtilities.invokeLater(() -> frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH));
        }

        if (initialFocusButton != null) {
            SwingUtilities.invokeLater(() -> initialFocusButton.requestFocusInWindow());
        }

        // Start autosave according to settings
        startAutosaveIfNeeded();

        checkForAppUpdatesAsync();
    }

    private void checkForAppUpdatesAsync() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return fetchLatestVersion();
            }

            @Override
            protected void done() {
                try {
                    String latestVersion = get();
                    if (latestVersion == null || latestVersion.isBlank()) {
                        return;
                    }
                    if (!CURRENT_APP_VERSION.equals(latestVersion)) {
                        showUpdateAvailableDialog(latestVersion);
                    }
                } catch (Exception ignored) {
                    // Ignore update-check failures to avoid interrupting normal app usage.
                }
            }
        };
        worker.execute();
    }

    private String fetchLatestVersion() {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(VERSION_SOURCE_URL).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "cp-ally-ide");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
                return extractVersion(content.toString());
            }
        } catch (IOException ignored) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractVersion(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        Matcher matcher = SEMVER_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private void showUpdateAvailableDialog(String latestVersion) {
        Object[] options = {"Open Releases", "Later"};
        int selection = JOptionPane.showOptionDialog(
                mainFrame,
                "A new version of Competitive Programming Ally is available.\n\n"
                        + "Current version: " + CURRENT_APP_VERSION + "\n"
                        + "Latest version: " + latestVersion,
                "Update Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (selection == JOptionPane.YES_OPTION) {
            openExternalUrl(RELEASES_URL);
        }
    }

    private void openExternalUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || url == null || url.isBlank()) {
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
            // Silently ignore browser launch issues.
        }
    }

    private void applyAppTheme(AppThemePalette palette) {
        AppThemePalette safePalette = palette != null ? palette : AppThemePalette.dark();
        if (safePalette.lightTheme()) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }

        UIManager.put("Component.accentColor", safePalette.accentColor());
        UIManager.put("Component.focusColor", safePalette.accentColor());

        UIManager.put("Panel.background", safePalette.frameBackground());
        UIManager.put("RootPane.background", safePalette.frameBackground());
        UIManager.put("Label.foreground", safePalette.textColor());

        UIManager.put("ToolBar.background", safePalette.panelBackground());
        UIManager.put("ToolBar.borderColor", safePalette.borderColor());
        UIManager.put("ToolBar.dockingBackground", safePalette.panelBackground());
        UIManager.put("ToolBar.overflowBackground", safePalette.panelBackground());

        UIManager.put("Button.background", safePalette.surfaceBackground());
        UIManager.put("Button.foreground", safePalette.textColor());
        UIManager.put("Button.hoverBackground", safePalette.surfaceRaised());
        UIManager.put("Button.default.background", safePalette.accentColor());
        UIManager.put("Button.default.foreground", safePalette.accentForeground());

        UIManager.put("TextField.background", safePalette.inputBackground());
        UIManager.put("TextField.foreground", safePalette.inputForeground());
        UIManager.put("TextField.caretForeground", safePalette.accentColor());
        UIManager.put("TextField.selectionBackground", safePalette.selectionBackground());
        UIManager.put("TextField.selectionForeground", safePalette.textColor());

        UIManager.put("SplitPane.background", safePalette.frameBackground());
        UIManager.put("SplitPaneDivider.background", safePalette.panelBackground());
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPaneDivider.gripColor", safePalette.mutedTextColor());
        UIManager.put("SplitPaneDivider.draggingColor", safePalette.accentColor());

        UIManager.put("ScrollBar.background", safePalette.frameBackground());
        UIManager.put("ScrollBar.track", safePalette.scrollbarTrack());
        UIManager.put("ScrollBar.thumb", safePalette.scrollbarThumb());
        UIManager.put("ScrollBar.thumbHover", safePalette.scrollbarThumbHover());
        UIManager.put("ScrollBar.thumbPressed", safePalette.scrollbarThumbPressed());

        UIManager.put("MenuBar.background", safePalette.panelBackground());
        UIManager.put("MenuBar.foreground", safePalette.textColor());
        UIManager.put("Menu.background", safePalette.panelBackground());
        UIManager.put("Menu.foreground", safePalette.textColor());
        UIManager.put("MenuItem.background", safePalette.panelBackground());
        UIManager.put("MenuItem.foreground", safePalette.textColor());
        UIManager.put("MenuItem.selectionBackground", safePalette.surfaceRaised());
        UIManager.put("MenuItem.selectionForeground", safePalette.textColor());
    }

    private AppThemePalette currentThemePalette() {
        if (appSettings == null) {
            return appThemePalette != null ? appThemePalette : AppThemePalette.dark();
        }
        appThemePalette = AppThemePalette.fromName(appSettings.appTheme());
        return appThemePalette;
    }

    private JMenuBar createEmbeddedTitleBar() {
        AppThemePalette palette = currentThemePalette();
        JMenuBar titleBar = new JMenuBar();
        titleBar.setOpaque(true);
        titleBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        titleBar.setBackground(palette.titleBarBackground());

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem preferencesItem = new JMenuItem("Preferences");
        preferencesItem.addActionListener(e -> onPreferencesClicked());
        preferencesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem clearCacheItem = new JMenuItem("Clear All Cache");
        clearCacheItem.addActionListener(e -> onClearAllCacheClicked());
        JMenuItem chooseDifferentProblemItem = new JMenuItem("Choose Problem");
        chooseDifferentProblemItem.addActionListener(e -> promptForDifferentProblem());
        chooseDifferentProblemItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem openEmptyProblemItem = new JMenuItem("Open Empty");
        openEmptyProblemItem.addActionListener(e -> promptOpenEmptyProblem());
        openEmptyProblemItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(preferencesItem);
        fileMenu.addSeparator();
        fileMenu.add(chooseDifferentProblemItem);
        fileMenu.add(openEmptyProblemItem);
        fileMenu.addSeparator();
        fileMenu.add(clearCacheItem);
        fileMenu.add(exitItem);
        titleBar.add(fileMenu);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> {
            if (codeEditor != null && codeEditor.hasFocus()) {
                codeEditor.selectAll();
            }
        });
        editMenu.add(selectAllItem);
        titleBar.add(editMenu);

        // Run Menu
        JMenu runMenu = new JMenu("Run");
        JMenuItem runCodeItem = new JMenuItem("Run Code");
        runCodeItem.addActionListener(e -> onRunButtonClicked());
        runCodeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        runMenu.add(runCodeItem);
        titleBar.add(runMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem documentationItem = new JMenuItem("Documentation");
        documentationItem.addActionListener(e -> openExternalUrl("https://github.com/0xPolybit/cp-ally-ide#readme"));
        JMenuItem checkUpdatesItem = new JMenuItem("Check for Updates");
        checkUpdatesItem.addActionListener(e -> onFetchLatestVersionClicked());
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(documentationItem);
        helpMenu.add(checkUpdatesItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
        titleBar.add(helpMenu);
        // Right-aligned zoom controls
        titleBar.add(Box.createHorizontalGlue());
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoomPanel.setOpaque(false);
        javax.swing.JButton zoomOutBtn = new javax.swing.JButton("-");
        zoomOutBtn.setFocusable(false);
        zoomOutBtn.setToolTipText("Zoom out active region (Ctrl+Wheel or touch pinch)");
        zoomOutBtn.addActionListener(e -> zoomOut(activeZoomTarget));
        javax.swing.JButton zoomInBtn = new javax.swing.JButton("+");
        zoomInBtn.setFocusable(false);
        zoomInBtn.setToolTipText("Zoom in active region (Ctrl+Wheel or touch pinch)");
        zoomInBtn.addActionListener(e -> zoomIn(activeZoomTarget));
        zoomPercentLabel = new JLabel(zoomLabelText());
        zoomPercentLabel.setForeground(palette.textColor());
        zoomPanel.add(zoomOutBtn);
        zoomPanel.add(zoomPercentLabel);
        zoomPanel.add(zoomInBtn);
        titleBar.add(zoomPanel);
        return titleBar;
    }

    private void onClearAllCacheClicked() {
        int response = JOptionPane.showConfirmDialog(
                mainFrame,
                "Clear all cached data? This includes programming solutions and problem statements.\nThis action cannot be undone.",
                "Confirm Clear All Cache",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            programCacheRepository.clearAll();
            codeforcesService.clearProblemCache();
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "All cache cleared successfully.",
                    "Cache Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onFetchLatestVersionClicked() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return fetchLatestVersion();
            }

            @Override
            protected void done() {
                try {
                    String latestVersion = get();
                    String message = (latestVersion == null || latestVersion.isBlank())
                            ? "Unable to fetch latest version right now."
                            : "Latest version: " + latestVersion;
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            message,
                            "Latest Version",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Unable to fetch latest version right now.",
                            "Latest Version",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onPreferencesClicked() {
        try {
            String currentAppTheme = appSettings != null ? appSettings.appTheme() : DEFAULT_APP_THEME;
            PreferencesDialog.PreferencesSelection initialSelection = new PreferencesDialog.PreferencesSelection(
                    appSettings != null ? appSettings.editorFontSize() : 14,
                    appSettings != null ? appSettings.editorColorScheme() : DEFAULT_EDITOR_THEME,
                    currentAppTheme,
                    appSettings != null && appSettings.useTabsAsSpaces(),
                    appSettings != null ? appSettings.tabSpacing() : 4,
                    appSettings != null ? appSettings.autosaveEnabled() : true,
                    appSettings != null ? appSettings.autosaveIntervalSeconds() : 10);
            PreferencesDialog.PreferencesSelection selection = PreferencesDialog.showDialog(mainFrame, initialSelection, currentThemePalette());
            if (selection != null) {
                boolean appThemeChanged = !selection.appTheme().equalsIgnoreCase(currentAppTheme);
                appSettings = replaceEditorPreferences(appSettings, selection.editorFontSize(), selection.editorColorScheme(), selection.appTheme(), selection.useTabsAsSpaces(), selection.tabSpacing(), selection.autosaveEnabled(), selection.autosaveIntervalSeconds());
                appThemePalette = AppThemePalette.fromName(selection.appTheme());
                settingsRepository.save(appSettings);
                // Restart autosave executor if autosave settings changed
                restartAutosaveIfNeeded();
                if (appThemeChanged) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "The app theme has been updated. Please reopen the program to reflect all changes.",
                            APP_NAME,
                            JOptionPane.INFORMATION_MESSAGE);
                    if (mainFrame != null) {
                        mainFrame.dispose();
                    }
                    System.exit(0);
                    return;
                }
                if (codeEditor != null) {
                    applyEditorPreferences(codeEditor, selection.editorFontSize(), selection.editorColorScheme(), selection.useTabsAsSpaces(), selection.tabSpacing());
                }
                refreshThemeAwareUi();
                if (mainFrame != null) {
                    SwingUtilities.updateComponentTreeUI(mainFrame);
                    mainFrame.revalidate();
                    mainFrame.repaint();
                }
            }
        } catch (Exception ex) {
            System.err.println("[MainWindow] Failed to construct/show Preferences dialog: " + ex.getMessage());
        }
    }

    private void showAboutDialog() {
        AppThemePalette palette = currentThemePalette();
        String aboutText = "<html>" +
                "<div style='text-align: center; font-family: Arial; padding: 10px;'>" +
                "<h2 style='color: " + toHex(palette.accentColor()) + ";'>" + APP_NAME + "</h2>" +
                "<p><strong>Version:</strong> " + CURRENT_APP_VERSION + "</p>" +
                "<p>A competitive programming IDE for CodeForces integration.</p>" +
                "<p>Write, test, and submit solutions directly from the editor.</p>" +
                "<br>" +
                "<p><em>Built with Java Swing & RSyntaxTextArea</em></p>" +
                "<p><a href='https://github.com/0xPolybit/cp-ally-ide'>GitHub Repository</a></p>" +
                "</div>" +
                "</html>";
        JOptionPane.showMessageDialog(
                mainFrame,
                aboutText,
                "About " + APP_NAME,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JSplitPane createContentSplit() {
        JPanel leftPanel = createProblemStatementPanel();
        JPanel rightPanel = createEditorPanel();

        leftPanel.setMinimumSize(new Dimension(MIN_LEFT_PANEL_WIDTH, 0));
        rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_PANEL_WIDTH, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerLocation(appSettings != null ? appSettings.dividerLocation() : 420);
        splitPane.setDividerSize(14);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        disableFocus(splitPane);
        contentSplitPane = splitPane;

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> clampDivider(splitPane));
        splitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                clampDivider(splitPane);
            }
        });

        return splitPane;
    }

    private void clampDivider(JSplitPane splitPane) {
        int width = splitPane.getWidth();
        if (width <= 0) {
            return;
        }

        int maxLeft = Math.max(MIN_LEFT_PANEL_WIDTH, width / 2);
        int minLeft = MIN_LEFT_PANEL_WIDTH;
        int current = splitPane.getDividerLocation();
        int clamped = Math.max(minLeft, Math.min(current, maxLeft));

        int rightLimited = width - MIN_RIGHT_PANEL_WIDTH;
        if (rightLimited > minLeft) {
            clamped = Math.min(clamped, rightLimited);
        }

        if (clamped != current) {
            splitPane.setDividerLocation(clamped);
        }
    }

    private JPanel createProblemStatementPanel() {
        AppThemePalette palette = currentThemePalette();
        leftPanelContainer = new JPanel(new BorderLayout());
        leftPanelContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 10));
        leftPanelContainer.setBackground(palette.frameBackground());

        problemEntryPanel = createProblemEntryPanel();
        leftPanelContainer.add(problemEntryPanel, BorderLayout.CENTER);
        return leftPanelContainer;
    }

    private JPanel createProblemEntryPanel() {
        AppThemePalette palette = currentThemePalette();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(true);
        form.setBackground(palette.panelBackground());
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(palette.borderColor()),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        problemCodeInput = createPlaceholderField(PROBLEM_PLACEHOLDER);
        problemCodeInput.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setHorizontalAlignment(JTextField.CENTER);
        problemCodeInput.setAlignmentX(Component.CENTER_ALIGNMENT);
        problemCodeInput.addActionListener(e -> fetchProblemButton.doClick());

        fetchProblemButton = new JButton("Fetch from CodeForces");
        fetchProblemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        fetchProblemButton.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchProblemButton.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchProblemButton.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        ImageIcon codeforcesIcon = UiIconLoader.loadScaledClasspathIcon("/assets/codeforces.png", 16, 16);
        if (codeforcesIcon != null) {
            fetchProblemButton.setIcon(codeforcesIcon);
            fetchProblemButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            fetchProblemButton.setIconTextGap(8);
        }
        fetchProblemButton.addActionListener(e -> onFetchProblemClicked());
        initialFocusButton = fetchProblemButton;

        JPanel actionStrip = new JPanel(new BorderLayout());
        actionStrip.setOpaque(false);
        actionStrip.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel connectivityLabel = new JLabel("Checking CodeForces...");
        connectivityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectivityLabel.setForeground(palette.mutedTextColor());
        connectivityLabel.setFont(connectivityLabel.getFont().deriveFont(Font.PLAIN, 12f));

        fetchStatusLabel = new JLabel(" ");
        fetchStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fetchStatusLabel.setForeground(palette.errorColor());
        fetchStatusLabel.setFont(fetchStatusLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JLabel logoLabel = new JLabel();
        ImageIcon logoIcon = UiIconLoader.loadScaledClasspathIcon("/assets/logo.png", 64, 64);
        if (logoIcon != null) {
            logoLabel.setIcon(logoIcon);
        }
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        form.add(logoLabel);

        form.add(problemCodeInput);
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        form.add(fetchProblemButton);
        form.add(Box.createRigidArea(new Dimension(0, 14)));
        form.add(connectivityLabel);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(fetchStatusLabel);

        checkCodeforcesStatusAsync(connectivityLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel formHolder = new JPanel(new GridBagLayout());
        formHolder.setOpaque(false);
        formHolder.add(form, gbc);

        panel.add(actionStrip, BorderLayout.NORTH);
        panel.add(formHolder, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditorPanel() {
        AppThemePalette palette = currentThemePalette();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 16));
        panel.setBackground(palette.frameBackground());

        JToolBar editorToolbar = new JToolBar();
        editorToolbar.setFloatable(false);
        editorToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        editorToolbar.setOpaque(false);
        disableFocus(editorToolbar);

        runButton = createToolbarButton("Run");
        runButton.setEnabled(false);
        applyRunButtonIcons();
        runButton.addActionListener(e -> onRunButtonClicked());
        editorToolbar.add(runButton);
        editorToolbar.add(Box.createHorizontalGlue());
        editorToolbar.add(Box.createHorizontalStrut(20));

        runtimeSupportLabel = new JLabel("checking...");
        runtimeSupportLabel.setForeground(palette.mutedTextColor());
        runtimeSupportLabel.setFont(runtimeSupportLabel.getFont().deriveFont(Font.PLAIN, 12f));
        runtimeSupportLabel.setMaximumSize(new Dimension(40, 20));
        runtimeSupportLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        runtimeSupportLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onRuntimeSupportLabelClicked();
            }
        });
        editorToolbar.add(runtimeSupportLabel);
        editorToolbar.add(Box.createHorizontalStrut(2));

        JLabel hintIconLabel = createHintIconLabel();
        editorToolbar.add(hintIconLabel);
        editorToolbar.add(Box.createHorizontalStrut(10));

        executionStateLabel = new JLabel("Status: Idle");
        executionStateLabel.setForeground(palette.mutedTextColor());
        executionStateLabel.setFont(executionStateLabel.getFont().deriveFont(Font.PLAIN, 12f));
        editorToolbar.add(executionStateLabel);
        editorToolbar.add(Box.createHorizontalStrut(10));

        languageDropdown = new JComboBox<>(new String[] {
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
        });

        String preferredLanguage = appSettings != null ? appSettings.lastLanguage() : DEFAULT_LANGUAGE;
        languageDropdown.setSelectedItem(preferredLanguage);
        if (languageDropdown.getSelectedItem() == null) {
            languageDropdown.setSelectedItem(DEFAULT_LANGUAGE);
        }
        languageDropdown.setPreferredSize(new Dimension(190, LEFT_FIELD_HEIGHT));
        languageDropdown.setMaximumSize(new Dimension(220, LEFT_FIELD_HEIGHT));
        languageDropdown.setBackground(palette.inputBackground());
        languageDropdown.setForeground(palette.inputForeground());
        languageDropdown.setFocusable(false);
        languageDropdown.setRequestFocusEnabled(false);
        languageDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                saveCurrentProgramToCache(e.getItem() != null ? e.getItem().toString() : null);
                return;
            }

            updateExecutionAvailability();
            if (problemStatementLoaded) {
                applyLanguageTemplateOrCachedProgram();
            }
        });
        editorToolbar.add(languageDropdown);
        updateExecutionAvailability();

        codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeEditor.setTabSize(appSettings != null ? appSettings.tabSpacing() : 4);
        codeEditor.setCodeFoldingEnabled(false);
        codeEditor.setEditable(false);
        codeEditor.setFocusable(false);
        codeEditor.setRequestFocusEnabled(false);
        applyEditorPreferences(
            codeEditor,
            appSettings != null ? appSettings.editorFontSize() : 14,
            appSettings != null ? appSettings.editorColorScheme() : DEFAULT_EDITOR_THEME,
            appSettings != null && appSettings.useTabsAsSpaces(),
            appSettings != null ? appSettings.tabSpacing() : 4);
        installEditorAutoPairs(codeEditor);
        codeEditor.setText("Select a problem to get started...");
        codeEditor.setCaretPosition(0);
        codeEditor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                activeZoomTarget = ZoomTarget.EDITOR;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                activeZoomTarget = ZoomTarget.EDITOR;
            }
        });
        // Support Ctrl+wheel / pinch-to-zoom on the editor
        // Mouse wheel handling for the editor is attached to the scroll pane below so
        // wheel events reliably control the scrollbar even when the editor isn't focusable.
        // Editor zoom (font size) keybindings: Ctrl + Plus / Ctrl + Equals / NumpadAdd to increase,
        // Ctrl + Minus / NumpadSubtract to decrease by 2pt.
        javax.swing.Action zoomInAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomIn(ZoomTarget.EDITOR);
            }
         };

        javax.swing.Action zoomOutAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomOut(ZoomTarget.EDITOR);
            }
        };

        // Bind multiple keystrokes for plus (since '+' often requires Shift)
        javax.swing.InputMap im = codeEditor.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = codeEditor.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ADD, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        am.put("zoomIn", zoomInAction);
        am.put("zoomOut", zoomOutAction);
        RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);
        // Ensure wheel scrolling is enabled on the scroll pane (some platforms/custom components
        // can prevent wheel events from reaching the scrollbar). Attach a listener to the
        // scroll pane to support Ctrl+wheel zooming while forwarding normal wheel events
        // to the scrollbar explicitly for consistent behavior.
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.addMouseWheelListener(e -> {
            try {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) zoomIn(ZoomTarget.EDITOR); else zoomOut(ZoomTarget.EDITOR);
                    e.consume();
                    return;
                }
                JScrollBar vsb = scrollPane.getVerticalScrollBar();
                int units = e.getUnitsToScroll();
                int increment = Math.max(1, vsb.getUnitIncrement());
                vsb.setValue(vsb.getValue() + units * increment);
                e.consume();
            } catch (Exception ignored) {
            }
        });
        // Also add a listener on the editor itself to forward wheel events to the
        // scroll pane. This helps when the editor component intercepts wheel events
        // and the scroll pane doesn't receive them.
        codeEditor.addMouseWheelListener(e -> {
            try {
                if (e.isControlDown()) {
                    // let scrollPane's handler manage Ctrl+wheel zoom
                    return;
                }
                if (codeScrollPane != null) {
                    JScrollBar vsb = codeScrollPane.getVerticalScrollBar();
                    int units = e.getUnitsToScroll();
                    int increment = Math.max(1, vsb.getUnitIncrement());
                    vsb.setValue(vsb.getValue() + units * increment);
                    e.consume();
                }
            } catch (Exception ignored) {
            }
        });
        codeScrollPane = scrollPane;
        applyEditorFontSize(codeEditor, appSettings != null ? appSettings.editorFontSize() : 14);

        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setBorder(BorderFactory.createLineBorder(palette.borderColor()));
        scrollPane.getGutter().setBackground(palette.gutterBackground());
        scrollPane.getGutter().setLineNumberColor(palette.mutedTextColor());
        scrollPane.getGutter().setBorderColor(palette.borderColor());
        scrollPane.getVerticalScrollBar().setBackground(palette.frameBackground());
        scrollPane.getVerticalScrollBar().setForeground(palette.scrollbarThumb());
        scrollPane.getHorizontalScrollBar().setBackground(palette.frameBackground());
        scrollPane.getHorizontalScrollBar().setForeground(palette.scrollbarThumb());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setBackground(palette.panelBackground());

        panel.add(editorToolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void applyEditorPreferences(RSyntaxTextArea editor, int fontSize, String colorScheme, boolean useTabsAsSpaces, int tabSpacing) {
        applyEditorTheme(editor, colorScheme);
        applyEditorFontSize(editor, fontSize);
        applyEditorTabSettings(editor, useTabsAsSpaces, tabSpacing);
    }

    private void applyEditorFontSize(RSyntaxTextArea editor, int fontSize) {
        int appliedSize = Math.max(8, Math.min(32, fontSize));
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, appliedSize);
        editor.setFont(font);

        SyntaxScheme scheme = editor.getSyntaxScheme();
        if (scheme != null) {
            for (int i = 0; i < scheme.getStyleCount(); i++) {
                Style style = scheme.getStyle(i);
                if (style != null && style.font != null) {
                    style.font = style.font.deriveFont((float) appliedSize);
                }
            }
            editor.setSyntaxScheme(scheme);
        }

        if (codeScrollPane != null) {
            try {
                codeScrollPane.getGutter().setLineNumberFont(new Font(Font.MONOSPACED, Font.PLAIN, appliedSize));
            } catch (Exception ignored) {
            }
        }

        editor.revalidate();
        editor.repaint();
    }

    private void applyEditorTabSettings(RSyntaxTextArea editor, boolean useTabsAsSpaces, int tabSpacing) {
        int validTabSpacing = Math.max(2, Math.min(8, tabSpacing));
        editor.setTabsEmulated(useTabsAsSpaces);
        editor.setTabSize(validTabSpacing);
    }

    private void applyEditorTheme(RSyntaxTextArea editor, String colorScheme) {
        EditorTheme theme = editorThemeFor(colorScheme);
        Color editorBackground = theme.background();

        editor.setBackground(editorBackground);
        editor.setForeground(theme.foreground());
        editor.setCaretColor(theme.caret());
        editor.setCurrentLineHighlightColor(theme.currentLine());
        editor.setSelectionColor(theme.selection());
        editor.setMatchedBracketBGColor(theme.matchedBracketBackground());
        editor.setMatchedBracketBorderColor(theme.matchedBracketBorder());
        editor.setAnimateBracketMatching(false);
        editor.setPaintMatchedBracketPair(true);

        SyntaxScheme scheme = editor.getSyntaxScheme();
        setTokenStyle(scheme, Token.RESERVED_WORD, theme.keyword(), true, false);
        setTokenStyle(scheme, Token.RESERVED_WORD_2, theme.typeColor(), true, false);
        setTokenStyle(scheme, Token.DATA_TYPE, theme.typeColor(), false, false);
        setTokenStyle(scheme, Token.FUNCTION, theme.functionColor(), false, false);
        setTokenStyle(scheme, Token.LITERAL_STRING_DOUBLE_QUOTE, theme.stringColor(), false, false);
        setTokenStyle(scheme, Token.LITERAL_CHAR, theme.stringColor(), false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_DECIMAL_INT, theme.numberColor(), false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_FLOAT, theme.numberColor(), false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_HEXADECIMAL, theme.numberColor(), false, false);
        setTokenStyle(scheme, Token.COMMENT_EOL, theme.commentColor(), false, true);
        setTokenStyle(scheme, Token.COMMENT_MULTILINE, theme.commentColor(), false, true);
        setTokenStyle(scheme, Token.COMMENT_DOCUMENTATION, theme.commentColor(), false, true);
        setTokenStyle(scheme, Token.OPERATOR, theme.operatorColor(), false, false);
        setTokenStyle(scheme, Token.SEPARATOR, theme.operatorColor(), false, false);
        setTokenStyle(scheme, Token.IDENTIFIER, theme.foreground(), false, false);

        if (codeScrollPane != null) {
            codeScrollPane.setBackground(editorBackground);
            try {
                codeScrollPane.getViewport().setBackground(editorBackground);
            } catch (Exception ignored) {
            }
        }

        editor.revalidate();
        editor.repaint();
    }

    private EditorTheme editorThemeFor(String colorScheme) {
        return switch (normalizeColorScheme(colorScheme)) {
            case "eclipse light" -> EditorTheme.eclipseLight();
            case "monokai" -> EditorTheme.monokaiDark();
            case "monokai dark" -> EditorTheme.monokaiDark();
            case "monokai light" -> EditorTheme.monokaiLight();
            case "solarized dark" -> EditorTheme.solarizedDark();
            case "solarized light" -> EditorTheme.solarizedLight();
            case "dracula" -> EditorTheme.draculaDark();
            case "dracula dark" -> EditorTheme.draculaDark();
            case "dracula light" -> EditorTheme.draculaLight();
            case "codeforces modern" -> EditorTheme.codeforcesDark();
            case "codeforces dark" -> EditorTheme.codeforcesDark();
            case "codeforces light" -> EditorTheme.codeforcesLight();
            default -> EditorTheme.eclipseDark();
        };
    }

    private String normalizeColorScheme(String colorScheme) {
        return colorScheme == null ? DEFAULT_EDITOR_THEME : colorScheme.trim().toLowerCase();
    }

    private AppSettings replaceEditorPreferences(AppSettings current, int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing, boolean autosaveEnabled, int autosaveIntervalSeconds) {
        if (current == null) {
            return new AppSettings(-1, -1, 1200, 760, 420, 420, false, DEFAULT_LANGUAGE, editorFontSize, editorColorScheme, appTheme, useTabsAsSpaces, tabSpacing, autosaveEnabled, autosaveIntervalSeconds);
        }
        return new AppSettings(
                current.x(),
                current.y(),
                current.width(),
                current.height(),
                current.dividerLocation(),
                current.testCasesDividerLocation(),
                current.maximized(),
                current.lastLanguage(),
                editorFontSize,
                editorColorScheme,
                appTheme,
                useTabsAsSpaces,
                tabSpacing,
                autosaveEnabled,
                autosaveIntervalSeconds);
    }

    private record EditorTheme(
            Color background,
            Color foreground,
            Color caret,
            Color currentLine,
            Color selection,
            Color matchedBracketBackground,
            Color matchedBracketBorder,
            Color keyword,
            Color typeColor,
            Color commentColor,
            Color stringColor,
            Color numberColor,
            Color functionColor,
            Color operatorColor) {

        static EditorTheme eclipseDark() {
            return new EditorTheme(
                    new Color(30, 31, 34),
                    new Color(187, 187, 187),
                    new Color(240, 240, 240),
                    new Color(50, 54, 60),
                    new Color(55, 247, 19, 58),
                    new Color(58, 63, 70),
                    new Color(55, 247, 19, 140),
                    new Color(204, 120, 50),
                    new Color(152, 118, 170),
                    new Color(128, 128, 128),
                    new Color(106, 135, 89),
                    new Color(104, 151, 187),
                    new Color(255, 198, 109),
                    new Color(169, 183, 198));
        }

                static EditorTheme eclipseLight() {
                    return new EditorTheme(
                        new Color(250, 251, 253),
                        new Color(35, 40, 48),
                        new Color(35, 40, 48),
                        new Color(238, 242, 247),
                        new Color(32, 142, 98, 34),
                        new Color(200, 208, 219),
                        new Color(32, 142, 98, 120),
                        new Color(204, 120, 50),
                        new Color(152, 118, 170),
                        new Color(110, 118, 130),
                        new Color(106, 135, 89),
                        new Color(82, 121, 180),
                        new Color(196, 143, 61),
                        new Color(90, 99, 112));
                }

        static EditorTheme monokai() {
            return new EditorTheme(
                    new Color(39, 40, 34),
                    new Color(248, 248, 242),
                    new Color(248, 248, 242),
                    new Color(49, 50, 44),
                    new Color(73, 72, 62, 80),
                    new Color(102, 217, 239),
                    new Color(102, 217, 239, 140),
                    new Color(249, 38, 114),
                    new Color(166, 226, 46),
                    new Color(117, 113, 94),
                    new Color(230, 219, 116),
                    new Color(174, 129, 255),
                    new Color(102, 217, 239),
                    new Color(248, 248, 242));
        }

        static EditorTheme monokaiDark() {
            return monokai();
        }

        static EditorTheme monokaiLight() {
            return new EditorTheme(
                    new Color(250, 251, 253),
                    new Color(34, 38, 46),
                    new Color(34, 38, 46),
                    new Color(236, 241, 246),
                    new Color(32, 142, 98, 34),
                    new Color(205, 213, 223),
                    new Color(32, 142, 98, 120),
                    new Color(249, 38, 114),
                    new Color(120, 158, 37),
                    new Color(110, 118, 130),
                    new Color(175, 120, 52),
                    new Color(119, 90, 191),
                    new Color(32, 142, 98),
                    new Color(68, 75, 88));
        }

        static EditorTheme solarizedDark() {
            return new EditorTheme(
                    new Color(0x00, 0x2b, 0x36),
                    new Color(0x93, 0xa1, 0xa1),
                    new Color(0xfd, 0xf6, 0xe3),
                    new Color(0x07, 0x36, 0x42),
                    new Color(0x58, 0x6e, 0x75, 80),
                    new Color(0x26, 0x8b, 0xd2),
                    new Color(0x26, 0x8b, 0xd2, 140),
                    new Color(0xCB, 0x4B, 0x16),
                    new Color(0xB5, 0x89, 0x00),
                    new Color(0x58, 0x6E, 0x75),
                    new Color(0x2A, 0xA1, 0x98),
                    new Color(0xD3, 0x36, 0x82),
                    new Color(0xB5, 0x89, 0x00),
                    new Color(0x93, 0xA1, 0xA1));
        }

                static EditorTheme solarizedLight() {
                    return new EditorTheme(
                        new Color(0xFD, 0xF6, 0xE3),
                        new Color(0x58, 0x6E, 0x75),
                        new Color(0x58, 0x6E, 0x75),
                        new Color(0xEE, 0xE8, 0xD5),
                        new Color(0x2A, 0xA1, 0x98, 45),
                        new Color(0xD8, 0xDE, 0xE3),
                        new Color(0x2A, 0xA1, 0x98, 120),
                        new Color(0xCB, 0x4B, 0x16),
                        new Color(0xB5, 0x89, 0x00),
                        new Color(0x93, 0xA1, 0xA1),
                        new Color(0x2A, 0xA1, 0x98),
                        new Color(0xD3, 0x36, 0x82),
                        new Color(0xB5, 0x89, 0x00),
                        new Color(0x00, 0x2B, 0x36));
                }

        static EditorTheme dracula() {
            return new EditorTheme(
                    new Color(40, 42, 54),
                    new Color(248, 248, 242),
                    new Color(248, 248, 242),
                    new Color(68, 71, 90),
                    new Color(98, 114, 164, 85),
                    new Color(80, 250, 123),
                    new Color(80, 250, 123, 140),
                    new Color(189, 147, 249),
                    new Color(139, 233, 253),
                    new Color(98, 114, 164),
                    new Color(255, 184, 108),
                    new Color(241, 250, 140),
                    new Color(80, 250, 123),
                    new Color(248, 248, 242));
        }

        static EditorTheme draculaDark() {
            return dracula();
        }

        static EditorTheme draculaLight() {
            return new EditorTheme(
                    new Color(250, 251, 253),
                    new Color(40, 42, 54),
                    new Color(40, 42, 54),
                    new Color(235, 240, 247),
                    new Color(98, 114, 164, 38),
                    new Color(214, 220, 229),
                    new Color(98, 114, 164, 120),
                    new Color(189, 147, 249),
                    new Color(139, 233, 253),
                    new Color(98, 114, 164),
                    new Color(255, 184, 108),
                    new Color(122, 120, 216),
                    new Color(80, 250, 123),
                    new Color(68, 71, 90));
        }

        static EditorTheme codeforcesModern() {
            return new EditorTheme(
                    new Color(28, 30, 34),
                    new Color(219, 223, 229),
                    new Color(240, 240, 240),
                    new Color(36, 39, 45),
                    new Color(64, 81, 100, 90),
                    new Color(82, 143, 255),
                    new Color(82, 143, 255, 140),
                    new Color(86, 156, 214),
                    new Color(78, 201, 176),
                    new Color(122, 126, 130),
                    new Color(206, 145, 120),
                    new Color(181, 206, 168),
                    new Color(220, 220, 170),
                    new Color(212, 212, 212));
        }

        static EditorTheme codeforcesDark() {
            return codeforcesModern();
        }

        static EditorTheme codeforcesLight() {
            return new EditorTheme(
                    new Color(250, 251, 253),
                    new Color(35, 40, 48),
                    new Color(35, 40, 48),
                    new Color(235, 240, 246),
                    new Color(32, 142, 98, 34),
                    new Color(205, 213, 223),
                    new Color(32, 142, 98, 120),
                    new Color(86, 156, 214),
                    new Color(78, 201, 176),
                    new Color(122, 126, 130),
                    new Color(206, 145, 120),
                    new Color(181, 206, 168),
                    new Color(220, 220, 170),
                    new Color(92, 99, 110));
        }
    }

    private void setTokenStyle(SyntaxScheme scheme, int token, Color color, boolean bold, boolean italic) {
        Style current = scheme.getStyle(token);
        Font baseFont = current != null && current.font != null ? current.font : new Font(Font.MONOSPACED, Font.PLAIN, 14);

        int fontStyle = Font.PLAIN;
        if (bold) {
            fontStyle |= Font.BOLD;
        }
        if (italic) {
            fontStyle |= Font.ITALIC;
        }

        Style style = new Style(color, null, baseFont.deriveFont(fontStyle));
        scheme.setStyle(token, style);
    }

    private JTextField createPlaceholderField(String placeholder) {
        AppThemePalette palette = currentThemePalette();
        JTextField field = new JTextField(placeholder);
        field.setBackground(palette.inputBackground());
        field.setForeground(palette.mutedTextColor());
        field.setCaretColor(palette.inputForeground());
        field.setFocusable(true);
        field.setRequestFocusEnabled(true);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (placeholder.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(palette.inputForeground());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(palette.mutedTextColor());
                }
            }
        });
        return field;
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        disableFocus(button);
        return button;
    }

    private void checkCodeforcesStatusAsync(JLabel statusLabel) {
        AppThemePalette palette = currentThemePalette();
        statusLabel.setText("Checking CodeForces...");
        statusLabel.setForeground(palette.mutedTextColor());

        SwingWorker<ConnectivityResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ConnectivityResult doInBackground() {
                return codeforcesService.evaluateConnectivity();
            }

            @Override
            protected void done() {
                try {
                    ConnectivityResult result = get();
                    statusLabel.setText(result.message());
                    statusLabel.setForeground(result.color());
                } catch (Exception ignored) {
                    statusLabel.setText("CodeForces unresponsive");
                    statusLabel.setForeground(palette.errorColor());
                }
            }
        };
        worker.execute();
    }

    private void onFetchProblemClicked() {
        String rawCode = problemCodeInput.getText() != null ? problemCodeInput.getText().trim() : "";
        fetchProblemByCode(rawCode, true);
    }

    private void fetchProblemByCode(String rawCode, boolean showWarnings) {
        if (rawCode.isEmpty() || PROBLEM_PLACEHOLDER.equals(rawCode)) {
            if (showWarnings) {
                showFetchWarning("Enter a problem code, for example 2208A.");
            }
            return;
        }

        saveCurrentProgramToCache();

        Matcher matcher = PROBLEM_CODE_PATTERN.matcher(rawCode);
        if (!matcher.matches()) {
            if (showWarnings) {
                showFetchWarning("Invalid problem code. Use format like 2208A.");
            }
            return;
        }

        String contestId = matcher.group(1);
        String index = matcher.group(2).toUpperCase();

        showLeftPanelLoading(contestId + index);

        final ProblemDetails[] fetched = new ProblemDetails[1];
        SwingWorker<RenderedProblemView[], Void> worker = new SwingWorker<>() {
            @Override
            protected RenderedProblemView[] doInBackground() throws Exception {
                ProblemDetails details = codeforcesService.fetchProblemDetails(contestId, index);
                fetched[0] = details;
                RenderedProblemView full = problemHtmlRenderer.render(details);
                RenderedProblemView statementOnly = problemHtmlRenderer.renderStatementOnly(details);
                return new RenderedProblemView[]{statementOnly, full};
            }

            @Override
            protected void done() {
                try {
                    RenderedProblemView[] renders = get();
                    currentProblemDetails = fetched[0];
                    showCodeforcesProblemView(contestId + index, renders[0], renders[1]);
                } catch (Exception ex) {
                    System.err.println("[MainWindow] Failed to fetch CodeForces problem " + rawCode + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    restoreProblemEntryPanelWithError("Could not fetch that problem.");
                    JOptionPane.showMessageDialog(
                            null,
                            "Could not fetch the specified CodeForces problem. Please verify the code and try again.",
                            APP_NAME,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void promptForDifferentProblem() {
        String initialValue = "";
        if (problemCodeInput != null && problemCodeInput.getText() != null) {
            String current = problemCodeInput.getText().trim();
            if (!current.isEmpty() && !PROBLEM_PLACEHOLDER.equals(current)) {
                initialValue = current;
            }
        }

        String entered = (String) JOptionPane.showInputDialog(
                mainFrame,
                "Enter problem code (e.g. 2208A):",
                "Choose Different Problem",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                initialValue);

        if (entered == null) {
            return;
        }

        String rawCode = entered.trim();
        if (problemCodeInput != null) {
            problemCodeInput.setText(rawCode.isEmpty() ? PROBLEM_PLACEHOLDER : rawCode);
        }
        fetchProblemByCode(rawCode, true);
    }

    private void showFetchWarning(String message) {
        fetchStatusLabel.setForeground(currentThemePalette().errorColor());
        fetchStatusLabel.setText(message);
        JOptionPane.showMessageDialog(null, message, APP_NAME, JOptionPane.WARNING_MESSAGE);
    }

    private JLabel createLoadingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(currentThemePalette().mutedTextColor());
        label.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        return label;
    }

    private void showLeftPanelLoading(String problemCode) {
        fetchProblemButton.setEnabled(false);
        fetchStatusLabel.setForeground(currentThemePalette().mutedTextColor());
        fetchStatusLabel.setText("Fetching problem " + problemCode + "...");

        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(createLoadingLabel("Loading statement and sample tests..."));

        leftPanelContainer.removeAll();
        leftPanelContainer.add(loadingPanel, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private void restoreProblemEntryPanelWithError(String message) {
        fetchProblemButton.setEnabled(true);
        fetchStatusLabel.setForeground(currentThemePalette().errorColor());
        fetchStatusLabel.setText(message);

        leftPanelContainer.removeAll();
        leftPanelContainer.add(problemEntryPanel, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private void showCodeforcesProblemView(String problemCode, RenderedProblemView statementOnly, RenderedProblemView full) {
        renderProblemView(problemCode, statementOnly, full, false);
    }

    private void showEmptyProblemView() {
        saveCurrentProgramToCache();
        currentProblemDetails = null;
        RenderedProblemView empty = problemHtmlRenderer.renderEmptyProblem();
        renderProblemView(EMPTY_PROBLEM_CODE, empty, empty, true);
    }

    private void renderProblemView(String problemCode, RenderedProblemView statementOnly, RenderedProblemView full, boolean emptyProblem) {
        AppThemePalette palette = currentThemePalette();
        currentProblemIsEmpty = emptyProblem;
        copyPayloads.clear();
        copyPayloads.putAll(full.copyPayloads());
        if (testCasesPanel != null) {
            testCasesPanel.setSamplePayloads(full.copyPayloads());
        }

        // Create or reuse persistent problemPane so we can re-render on zoom changes without rebuilding UI
        if (problemPane == null) {
            problemPane = new JEditorPane();
            problemPane.setContentType("text/html");
            problemPane.setEditable(false);
            problemPane.setFocusable(false);
            problemPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    activeZoomTarget = ZoomTarget.PROBLEM;
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    activeZoomTarget = ZoomTarget.PROBLEM;
                }
            });
            problemPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            problemPane.addHyperlinkListener(event -> {
                if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED || event.getDescription() == null) {
                    return;
                }
                String description = event.getDescription();
                if (description.startsWith("copy:")) {
                    String key = description.substring("copy:".length());
                    String payload = copyPayloads.get(key);
                    if (payload != null) {
                        copyToClipboard(payload);
                    }
                    return;
                }

                if (description.startsWith("http://") || description.startsWith("https://")) {
                    openExternalUrl(description);
                }
            });
        }
        problemPane.setText(statementOnly.html());
        problemPane.setCaretPosition(0);
        problemPane.setBackground(palette.frameBackground());

        JScrollPane scrollPane = new JScrollPane(problemPane);
        // Ensure wheel scrolling works for the problem pane and support Ctrl+wheel / pinch-to-zoom.
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.addMouseWheelListener(e -> {
            try {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) zoomIn(ZoomTarget.PROBLEM); else zoomOut(ZoomTarget.PROBLEM);
                    e.consume();
                }
            } catch (Exception ignored) {
            }
        });
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(palette.frameBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel statementPanel = new JPanel(new BorderLayout());
        statementPanel.setOpaque(false);
        statementPanel.add(topBar, BorderLayout.NORTH);
        statementPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel testCasesSection = testCasesPanel != null ? testCasesPanel.createPanel() : new JPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, testCasesSection);
        splitPane.setResizeWeight(0.5);

        int minTestCaseHeight = MIN_WINDOW_HEIGHT / 3;
        int preferredDivider = appSettings != null && appSettings.testCasesDividerLocation() > 0
            ? appSettings.testCasesDividerLocation()
            : mainFrame.getHeight() - minTestCaseHeight - 10;
        splitPane.setDividerLocation(preferredDivider);

        statementTestCasesSplitPane = splitPane;

        leftPanelContainer.removeAll();
        leftPanelContainer.add(splitPane, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();

        currentProblemCode = problemCode;
        problemStatementLoaded = true;
        enableEditorForProblem();
    }

    private void promptOpenEmptyProblem() {
        int selection = JOptionPane.showConfirmDialog(
                mainFrame,
                "Open an empty problem?\n\nYour current code will be saved before switching.",
                "Open Empty Problem",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (selection != JOptionPane.YES_OPTION) {
            return;
        }

        saveCurrentProgramToCache();
        showEmptyProblemView();
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void enableEditorForProblem() {
        if (codeEditor != null) {
            codeEditor.setEditable(true);
            codeEditor.setFocusable(true);
            codeEditor.setRequestFocusEnabled(true);
        }

        applyLanguageTemplateOrCachedProgram();
        updateExecutionAvailability();
        // Ensure editor zoom reflects current zoomFactor
        applyZoomToEditor();
    }

    private void applyLanguageTemplateOrCachedProgram() {
        if (codeEditor == null || languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        if (currentProblemIsEmpty) {
            applyLanguageTemplate();
            return;
        }

        String language = languageDropdown.getSelectedItem().toString();
        String cachedProgram = currentProblemCode != null
                ? programCacheRepository.loadLatestSource(currentProblemCode, language)
                : null;

        if (cachedProgram != null) {
            codeEditor.setSyntaxEditingStyle(resolveSyntaxStyle(language));
            codeEditor.setText(cachedProgram);
            codeEditor.setCaretPosition(Math.min(codeEditor.getText().length(), cachedProgram.length()));
            lastAutosavedSource = codeEditor.getText();
            applyZoomToEditor();
            return;
        }

        applyLanguageTemplate();
    }

    private void applyLanguageTemplate() {
        if (codeEditor == null || languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        String language = languageDropdown.getSelectedItem().toString();
        codeEditor.setSyntaxEditingStyle(resolveSyntaxStyle(language));
        String boilerplate = boilerplateFor(language);
        codeEditor.setText(boilerplate);

        int cursor = boilerplate.indexOf("// code goes here...");
        if (cursor < 0) {
            cursor = boilerplate.indexOf("# code goes here...");
        }
        codeEditor.setCaretPosition(Math.max(0, cursor));
        lastAutosavedSource = codeEditor.getText();
        applyZoomToEditor();
    }

    private void saveCurrentProgramToCache() {
        if (languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        saveCurrentProgramToCache(languageDropdown.getSelectedItem().toString());
    }

    private void saveCurrentProgramToCache(String language) {
        if (!problemStatementLoaded || currentProblemCode == null || codeEditor == null || language == null || language.isBlank()) {
            return;
        }

        String sourceCode = codeEditor.getText();
        if (sourceCode == null) {
            return;
        }

        // Do not persist code for the special empty problem slot — always treat empty problem as ephemeral
        if (currentProblemIsEmpty || EMPTY_PROBLEM_CODE.equals(currentProblemCode)) {
            return;
        }

        programCacheRepository.save(currentProblemCode, language, sourceCode);
    }

    private void updateExecutionAvailability() {
        if (languageDropdown == null || runtimeSupportLabel == null || runButton == null) {
            return;
        }

        String language = selectedLanguage();
        CodeExecutionService.LanguageSupport support = codeExecutionService.detectSupport(language);
        boolean hasTestCases = testCasesPanel != null && !testCasesPanel.getExecutionTestCases().isEmpty();
        boolean ready = problemStatementLoaded && support.supported() && (hasTestCases || currentProblemIsEmpty);

        runtimeSupportLabel.setText("<html><span style='color:"
                + (support.supported() ? "#61d66e" : "#f65656")
                + ";'>"
                + (support.supported() ? "Yes" : "No")
                + "</span></html>");
        runtimeSupportLabel.setToolTipText(support.message());
        runButton.setEnabled(ready);
        runButton.setToolTipText(ready
            ? (currentProblemIsEmpty && !hasTestCases ? "Run the current code with empty input." : "Run the sample test cases locally")
            : (hasTestCases ? support.message() : "No test cases available to run."));
    }

    private String selectedLanguage() {
        if (languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return DEFAULT_LANGUAGE;
        }
        return languageDropdown.getSelectedItem().toString();
    }

    private void onRunButtonClicked() {
        if (!problemStatementLoaded || codeEditor == null) {
            return;
        }

        String language = selectedLanguage();
        CodeExecutionService.LanguageSupport support = codeExecutionService.detectSupport(language);
        if (!support.supported()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    support.message(),
                    "Execution unavailable",
                    JOptionPane.WARNING_MESSAGE);
            updateExecutionAvailability();
            return;
        }

        List<CodeExecutionService.TestCaseSpec> testCases = testCasesPanel != null
            ? testCasesPanel.getExecutionTestCases()
            : SampleTestCaseCollector.collect(copyPayloads);
        boolean emptyProblemRun = currentProblemIsEmpty && testCases.isEmpty();
        if (testCases.isEmpty() && !emptyProblemRun) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "No test cases were found for this problem.",
                    "Nothing to run",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String sourceCode = codeEditor.getText();
        runButton.setEnabled(false);
        runButton.setToolTipText("Running sample test cases...");
        setExecutionRunningState(true);

        SwingWorker<CodeExecutionService.ExecutionReport, Void> worker = new SwingWorker<>() {
            @Override
            protected CodeExecutionService.ExecutionReport doInBackground() throws Exception {
                return emptyProblemRun
                        ? codeExecutionService.runProgramOnce(language, sourceCode)
                        : codeExecutionService.runSampleTests(language, sourceCode, testCases);
            }

            @Override
            protected void done() {
                setExecutionRunningState(false);
                updateExecutionAvailability();
                try {
                    showExecutionResultsDialog(language, get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Failed to run the selected language locally.\n\n" + ex.getMessage(),
                            "Execution error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setExecutionRunningState(boolean running) {
        if (executionStateLabel == null) {
            return;
        }

        if (running) {
            executionStateLabel.setText("Status: Running");
            executionStateLabel.setForeground(new Color(247, 215, 26));
        } else {
            executionStateLabel.setText("Status: Idle");
            executionStateLabel.setForeground(new Color(169, 176, 188));
        }
    }

    private void onRuntimeSupportLabelClicked() {
        try {
            String language = selectedLanguage();
            String info = codeExecutionService.getDetailedSupportInfo(language);
            SupportDialogs.showRuntimeSupportDialog(mainFrame, language, info, currentThemePalette());
        } catch (Exception ex) {
            System.err.println("[MainWindow] Failed to construct/show runtime support dialog: " + ex.getMessage());
        }
    }

    private JLabel createHintIconLabel() {
        JLabel hintLabel = new JLabel();
        try {
            ImageIcon hintIcon = UiIconLoader.loadThemedClasspathIcon("hint.png", currentThemePalette(), 16, 16);
            if (hintIcon != null) {
                hintLabel.setIcon(hintIcon);
            }
        } catch (Exception ignored) {
            System.err.println("[MainWindow] Failed to load hint icon: " + ignored.getMessage());
        }
        
        hintLabel.setMaximumSize(new Dimension(20, 20));
        hintLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        hintLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onRuntimeSupportLabelClicked();
            }
        });
        return hintLabel;
    }

    private void installEditorAutoPairs(RSyntaxTextArea editor) {
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!editor.isEditable() || e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                if (editor.getSelectionStart() != editor.getSelectionEnd()) {
                    return;
                }

                int caret = editor.getCaretPosition();
                int length = editor.getDocument().getLength();
                if (caret <= 0 || caret >= length) {
                    return;
                }

                try {
                    String left = editor.getDocument().getText(caret - 1, 1);
                    String right = editor.getDocument().getText(caret, 1);
                    if (left.length() == 1 && right.length() == 1) {
                        char opening = left.charAt(0);
                        char closing = right.charAt(0);
                        char expected = switch (opening) {
                            case '(' -> ')';
                            case '[' -> ']';
                            case '{' -> '}';
                            default -> '\0';
                        };

                        if (expected != '\0' && closing == expected) {
                            editor.getDocument().remove(caret - 1, 2);
                            editor.setCaretPosition(caret - 1);
                            e.consume();
                        }
                    }
                } catch (Exception ignored) {
                    // Keep default backspace behavior if paired deletion fails.
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!editor.isEditable()) {
                    return;
                }

                Character closing = switch (e.getKeyChar()) {
                    case '(' -> ')';
                    case '[' -> ']';
                    case '{' -> '}';
                    default -> null;
                };

                if (closing == null) {
                    return;
                }

                int caret = editor.getCaretPosition();
                if (caret < 0 || caret > editor.getDocument().getLength()) {
                    return;
                }

                try {
                    editor.getDocument().insertString(caret, String.valueOf(closing), null);
                    editor.setCaretPosition(caret);
                } catch (Exception ignored) {
                    // Keep default typing behavior if insertion fails.
                }
            }
        });
    }

    private void showExecutionResultsDialog(String language, CodeExecutionService.ExecutionReport report) {
        ExecutionResultsDialog.show(mainFrame, language, report, currentThemePalette());
    }

    private void applyRunButtonIcons() {
        if (runButton == null) {
            return;
        }

        AppThemePalette palette = currentThemePalette();
        ImageIcon normal = UiIconLoader.loadThemedClasspathIcon("run.png", palette, RUN_ICON_SIZE, RUN_ICON_SIZE);
        ImageIcon hover = UiIconLoader.loadScaledClasspathIcon("/assets/run-hover.png", RUN_ICON_SIZE, RUN_ICON_SIZE);
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
        runButton.setPreferredSize(new Dimension(RUN_ICON_SIZE + 14, RUN_ICON_SIZE + 10));
    }

    private String resolveSyntaxStyle(String language) {
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return SyntaxConstants.SYNTAX_STYLE_PYTHON;
        }
        if (language.startsWith("GNU G++") || language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        }
        if (language.startsWith("Java ")) {
            return SyntaxConstants.SYNTAX_STYLE_JAVA;
        }
        if (language.startsWith("Kotlin")) {
            return SyntaxConstants.SYNTAX_STYLE_KOTLIN;
        }
        if (language.startsWith("C#")) {
            return SyntaxConstants.SYNTAX_STYLE_CSHARP;
        }
        if (language.startsWith("Go")) {
            return SyntaxConstants.SYNTAX_STYLE_GO;
        }
        if (language.startsWith("Rust")) {
            return SyntaxConstants.SYNTAX_STYLE_RUST;
        }
        if (language.startsWith("Node.js") || language.startsWith("JavaScript")) {
            return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
        }
        if (language.startsWith("PHP")) {
            return SyntaxConstants.SYNTAX_STYLE_PHP;
        }
        if (language.startsWith("Ruby")) {
            return SyntaxConstants.SYNTAX_STYLE_RUBY;
        }
        if (language.startsWith("Perl")) {
            return SyntaxConstants.SYNTAX_STYLE_PERL;
        }
        if (language.startsWith("Scala")) {
            return SyntaxConstants.SYNTAX_STYLE_SCALA;
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    private String boilerplateFor(String language) {
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return "import sys\n"
                + "\n"
                + "def main():\n"
                + "\t# code goes here...\n"
                + "\n"
                + "if __name__ == \"__main__\":\n"
                + "\tmain()\n";
        }
        if (language.startsWith("GNU G++") || language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return "#include <bits/stdc++.h>\n"
                    + "using namespace std;\n"
                    + "\n"
                    + "int main() {\n"
                    + "\t// code goes here...\n"
                    + "\treturn 0;\n"
                    + "}\n";
        }
        if (language.startsWith("Java ")) {
            return "import java.io.*;\n"
                    + "import java.util.*;\n"
                    + "\n"
                    + "public class Main {\n"
                    + "\n"
                    + "\tpublic static void main(String[] args) throws Exception {\n"
                    + "\t\tScanner sc = new Scanner(System.in);\n"
                    + "\t\t// code goes here...\n"
                    + "\t\tsc.close();\n"
                    + "\t}\n"
                    + "\n"
                    + "}\n";
        }
        if (language.startsWith("Kotlin")) {
            return "fun main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("C#")) {
            return "using System;\n"
                    + "\n"
                    + "public class Program {\n"
                    + "\tpublic static void Main() {\n"
                    + "\t\t// code goes here...\n"
                    + "\t}\n"
                    + "}\n";
        }
        if (language.startsWith("Go")) {
            return "package main\n"
                    + "\n"
                    + "func main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("Rust")) {
            return "fn main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("Node.js") || language.startsWith("JavaScript")) {
            return "function main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n"
                    + "\n"
                    + "main();\n";
        }
        if (language.startsWith("PHP")) {
            return "<?php\n"
                    + "// code goes here...\n";
        }
        if (language.startsWith("Ruby")) {
            return "def main\n"
                    + "\t# code goes here...\n"
                    + "end\n"
                    + "\n"
                    + "main\n";
        }
        if (language.startsWith("Perl")) {
            return "use strict;\n"
                    + "use warnings;\n"
                    + "\n"
                    + "# code goes here...\n";
        }
        if (language.startsWith("Haskell")) {
            return "main :: IO ()\n"
                    + "main = do\n"
                    + "\t-- code goes here...\n";
        }
        if (language.startsWith("OCaml")) {
            return "let () =\n"
                    + "\t(* code goes here... *)\n"
                    + "\t()\n";
        }
        if (language.startsWith("Scala")) {
            return "object Main {\n"
                    + "\tdef main(args: Array[String]): Unit = {\n"
                    + "\t\t// code goes here...\n"
                    + "\t}\n"
                    + "}\n";
        }
        if (language.startsWith("Pascal")) {
            return "program Main;\n"
                    + "begin\n"
                    + "\t// code goes here...\n"
                    + "end.\n";
        }
        return "// code goes here...\n";
    }

    private void persistSettings(JFrame frame) {
        int state = frame.getExtendedState();
        boolean maximized = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;

        String language = DEFAULT_LANGUAGE;
        if (languageDropdown != null && languageDropdown.getSelectedItem() != null) {
            language = languageDropdown.getSelectedItem().toString();
        }

        int dividerLocation = contentSplitPane != null ? contentSplitPane.getDividerLocation() : 420;
        int testCasesDividerLocation = statementTestCasesSplitPane != null ? statementTestCasesSplitPane.getDividerLocation() : 420;

        AppSettings settings = new AppSettings(
            frame.getX(),
            frame.getY(),
            frame.getWidth(),
            frame.getHeight(),
            dividerLocation,
            testCasesDividerLocation,
            maximized,
            language,
            appSettings != null ? appSettings.editorFontSize() : 14,
            appSettings != null ? appSettings.editorColorScheme() : DEFAULT_EDITOR_THEME,
            appSettings != null ? appSettings.appTheme() : DEFAULT_APP_THEME,
            appSettings != null && appSettings.useTabsAsSpaces(),
            appSettings != null ? appSettings.tabSpacing() : 4,
            appSettings != null ? appSettings.autosaveEnabled() : true,
            appSettings != null ? appSettings.autosaveIntervalSeconds() : 10);

        settingsRepository.save(settings);
    }

    // Zoom helpers
    private void zoomIn(ZoomTarget target) {
        adjustZoom(target, ZOOM_STEP);
    }

    private void zoomOut(ZoomTarget target) {
        adjustZoom(target, -ZOOM_STEP);
    }

    private void adjustZoom(ZoomTarget target, double delta) {
        if (target == ZoomTarget.PROBLEM) {
            setZoomFactor(ZoomTarget.PROBLEM, problemZoomFactor + delta);
        } else {
            setZoomFactor(ZoomTarget.EDITOR, editorZoomFactor + delta);
        }
    }

    private double clampZoom(double z) {
        if (z < ZOOM_MIN) return ZOOM_MIN;
        if (z > ZOOM_MAX) return ZOOM_MAX;
        // snap to step
        double steps = Math.round(z / ZOOM_STEP);
        return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, steps * ZOOM_STEP));
    }

    private void setZoomFactor(ZoomTarget target, double newZoom) {
        double clampedZoom = clampZoom(newZoom);
        if (target == ZoomTarget.PROBLEM) {
            problemZoomFactor = clampedZoom;
            if (problemHtmlRenderer != null) {
                problemHtmlRenderer.setZoomFactor(problemZoomFactor);
            }
            rerenderProblemStatement();
        } else {
            editorZoomFactor = clampedZoom;
            applyZoomToEditor();
        }
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText(zoomLabelText());
        }
    }

    private void applyZoomToEditor() {
        if (codeEditor == null) return;
        int base = appSettings != null ? appSettings.editorFontSize() : 14;
        int newSize = Math.max(8, (int) Math.round(base * editorZoomFactor));
        applyEditorFontSize(codeEditor, newSize);
    }

    private void rerenderProblemStatement() {
        try {
            if (!problemStatementLoaded || problemPane == null || problemHtmlRenderer == null) {
                return;
            }
            if (currentProblemDetails != null) {
                RenderedProblemView full = problemHtmlRenderer.render(currentProblemDetails);
                RenderedProblemView statementOnly = problemHtmlRenderer.renderStatementOnly(currentProblemDetails);
                copyPayloads.clear();
                copyPayloads.putAll(full.copyPayloads());
                if (testCasesPanel != null) {
                    testCasesPanel.setSamplePayloads(full.copyPayloads());
                }
                problemPane.setText(statementOnly.html());
                problemPane.setCaretPosition(0);
            } else if (currentProblemIsEmpty) {
                RenderedProblemView empty = problemHtmlRenderer.renderEmptyProblem();
                problemPane.setText(empty.html());
                problemPane.setCaretPosition(0);
            }
        } catch (Exception ignored) {
        }
    }

    private String zoomLabelText() {
        return "E " + (int) Math.round(editorZoomFactor * 100) + "% | P " + (int) Math.round(problemZoomFactor * 100) + "%";
    }

    private void startAutosaveIfNeeded() {
        stopAutosave();
        if (appSettings == null || !appSettings.autosaveEnabled()) {
            return;
        }
        int interval = Math.max(1, appSettings.autosaveIntervalSeconds());
        startAutosave(interval);
    }

    private void restartAutosaveIfNeeded() {
        // Called after preferences change
        startAutosaveIfNeeded();
    }

    private void startAutosave(int intervalSeconds) {
        try {
            autosaveExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cpa-autosave");
                t.setDaemon(true);
                return t;
            });

            autosaveExecutor.scheduleAtFixedRate(() -> {
                try {
                    if (appSettings == null || !appSettings.autosaveEnabled()) return;
                    if (!problemStatementLoaded || currentProblemCode == null || codeEditor == null) return;
                    // Never autosave the ephemeral empty problem slot
                    if (currentProblemIsEmpty || EMPTY_PROBLEM_CODE.equals(currentProblemCode)) return;
                    String current = codeEditor.getText();
                    if (current == null) return;
                    if (!current.equals(lastAutosavedSource)) {
                        String language = languageDropdown != null && languageDropdown.getSelectedItem() != null
                                ? languageDropdown.getSelectedItem().toString()
                                : DEFAULT_LANGUAGE;
                        programCacheRepository.save(currentProblemCode, language, current);
                        lastAutosavedSource = current;
                    }
                } catch (Exception ignored) {
                }
            }, intervalSeconds, intervalSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private void stopAutosave() {
        try {
            if (autosaveExecutor != null) {
                autosaveExecutor.shutdownNow();
                autosaveExecutor = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void refreshThemeAwareUi() {
        AppThemePalette palette = currentThemePalette();
        if (mainFrame != null) {
            mainFrame.getRootPane().putClientProperty("JRootPane.titleBarBackground", palette.titleBarBackground());
            mainFrame.getRootPane().putClientProperty("JRootPane.titleBarForeground", palette.titleBarForeground());
        }
        if (leftPanelContainer != null) {
            leftPanelContainer.setBackground(palette.frameBackground());
        }
        if (problemEntryPanel != null) {
            problemEntryPanel.setBackground(palette.frameBackground());
        }
        if (runtimeSupportLabel != null) {
            runtimeSupportLabel.setForeground(palette.mutedTextColor());
        }
        if (executionStateLabel != null) {
            executionStateLabel.setForeground(palette.mutedTextColor());
        }
        if (languageDropdown != null) {
            languageDropdown.setBackground(palette.inputBackground());
            languageDropdown.setForeground(palette.inputForeground());
        }
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setForeground(palette.textColor());
        }
        if (codeScrollPane != null) {
            Color editorBackground = editorThemeFor(appSettings != null ? appSettings.editorColorScheme() : DEFAULT_EDITOR_THEME).background();
            codeScrollPane.setBackground(editorBackground);
            try {
                codeScrollPane.getViewport().setBackground(editorBackground);
            } catch (Exception ignored) {
            }
            try {
                codeScrollPane.getGutter().setBackground(palette.gutterBackground());
                codeScrollPane.getGutter().setLineNumberColor(palette.mutedTextColor());
                codeScrollPane.getGutter().setBorderColor(palette.borderColor());
            } catch (Exception ignored) {
            }
        }
        if (mainFrame != null) {
            SwingUtilities.updateComponentTreeUI(mainFrame);
            mainFrame.revalidate();
            mainFrame.repaint();
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void applyWindowSettings(JFrame frame, AppSettings settings) {
        int width = Math.max(MIN_WINDOW_WIDTH, settings.width());
        int height = Math.max(MIN_WINDOW_HEIGHT, settings.height());
        frame.setSize(width, height);

        if (settings.x() >= 0 && settings.y() >= 0) {
            frame.setLocation(settings.x(), settings.y());
        } else {
            frame.setLocationRelativeTo(null);
        }
    }

    private void disableFocus(Component component) {
        component.setFocusable(false);
        if (component instanceof JComponent jComponent) {
            jComponent.setRequestFocusEnabled(false);
        }
    }
}
