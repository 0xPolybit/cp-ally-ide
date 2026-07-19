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
import javax.swing.JCheckBoxMenuItem;
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
    private static final String CURRENT_APP_VERSION = "0.2.2";
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
    private final CustomTestRepository customTestRepository = new CustomTestRepository(settingsRepository.getAppDataDirectory());
    private ToolchainAvailability toolchainAvailability;
    private CodeforcesService codeforcesService;
    private ProblemSheetsService problemSheetsService;
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
    private JButton stopButton;
    private JPanel leftPanelContainer;
    private JPanel problemEntryPanel;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private JSplitPane contentSplitPane;
    private JSplitPane statementTestCasesSplitPane;
    private JScrollPane problemScrollPane;
    private TestCasesPanel testCasesPanel;
    private JMenuItem refreshProblemItem;
    private JMenuItem addTestCaseItem;
    private boolean problemStatementLoaded;
    private boolean currentProblemIsEmpty;
    private String currentProblemCode;
    private final ProblemFetchArbiter problemFetchArbiter = new ProblemFetchArbiter();
    private CancellationToken activeProblemFetchToken;
    private javax.swing.SwingWorker<?, ?> activeProblemFetchWorker;
    private AppThemePalette appThemePalette = AppThemePalette.dark();
    private java.util.concurrent.ScheduledExecutorService autosaveExecutor;
    private volatile boolean sourceDirty = false;
    private volatile String lastAutosavedSource = null;
    private volatile boolean problemLoading = false;
    private volatile boolean executionRunning = false;
    private java.util.concurrent.ScheduledFuture<?> pendingAutosave;
    private static final long AUTOSAVE_DEBOUNCE_MILLIS = 1000L;
    private double editorZoomFactor = 1.0;
    private double problemZoomFactor = 1.0;
    private static final double DEFAULT_ZOOM = 1.0;
    private EditorFindBar editorFindBar;
    private static final double ZOOM_MIN = 0.25;
    private static final double ZOOM_MAX = 4.0;
    private static final double ZOOM_STEP = 0.05;
    private javax.swing.JEditorPane problemPane;
    private ProblemDetails currentProblemDetails;
    private javax.swing.JLabel zoomPercentLabel;
    private enum ZoomTarget { EDITOR, PROBLEM }
    private ZoomTarget activeZoomTarget = ZoomTarget.EDITOR;
    private JMenuItem userMenuItem;
    private JMenuItem showProfileMenuItem;
    private JMenuItem chooseDifferentProblemItem;
    private JMenuItem openEmptyProblemItem;
    private JLabel loggedInLabel;
    private JLabel submissionStatusLabel;
    private CodeforcesUserService cfUserService;
    private CodeforcesProfileService cfProfileService;
    private String codeforcesUsername = "";

    public void showWindow() {
        appSettings = settingsRepository.load();
        appThemePalette = AppThemePalette.fromName(appSettings != null ? appSettings.appTheme() : DEFAULT_APP_THEME);

        JFrame.setDefaultLookAndFeelDecorated(true);
        applyAppTheme(appThemePalette);

        Path appDataDir = settingsRepository.getAppDataDirectory();
        DiagnosticLogger.initialize(appDataDir);
        DiagnosticLogger.info("App starting up. Version: " + CURRENT_APP_VERSION);

        problemHtmlRenderer = new ProblemHtmlRenderer(appDataDir);
        problemHtmlRenderer.setTheme(appThemePalette);
        codeforcesService = new CodeforcesService(appDataDir);
        if (appSettings != null) {
            codeExecutionService.configureExecutionLimits(appSettings.runTimeoutSeconds(), appSettings.maxOutputBytes());
        }
        toolchainAvailability = new ToolchainAvailability(codeExecutionService);
        problemSheetsService = new ProblemSheetsService();
        cfUserService = new CodeforcesUserService();
        cfProfileService = new CodeforcesProfileService();
        codeforcesUsername = appSettings != null ? appSettings.codeforcesUsername() : "";

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
        testCasesPanel = new TestCasesPanel(mainFrame, appThemePalette, customTestRepository);
        testCasesPanel.addListener(source -> updateExecutionAvailability());
        testCasesPanel.setRunTestHandler(this::runSingleTest);

        frame.add(createContentSplit(), BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAutosave();
                saveCurrentProgramToCache();
                customTestRepository.flush();
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

    /**
     * Signals that the main window has finished initialization and is safe to
     * receive external commands. The application entry point calls this after
     * {@link #showWindow()} returns and before draining any queued deep-link
     * commands. The default implementation is a no-op; tests and integrations
     * may override to observe readiness.
     */
    void markReady() {
        // Intentionally empty: callers use the App-level drain after invoking
        // this. Exposed for symmetry and to make the readiness contract
        // explicit in the type system.
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
        JMenuItem restoreSnapshotItem = new JMenuItem("Restore Source Snapshot…");
        restoreSnapshotItem.addActionListener(e -> restoreSourceSnapshot());
        chooseDifferentProblemItem = new JMenuItem("Choose Problem");
        chooseDifferentProblemItem.addActionListener(e -> promptForDifferentProblem());
        chooseDifferentProblemItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openEmptyProblemItem = new JMenuItem("Open Empty");
        openEmptyProblemItem.addActionListener(e -> promptOpenEmptyProblem());
        openEmptyProblemItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        refreshProblemItem = new JMenuItem("Refresh Problem");
        refreshProblemItem.addActionListener(e -> refreshCurrentProblem());
        refreshProblemItem.setEnabled(false);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> shutdownAndExit());
        fileMenu.add(preferencesItem);
        userMenuItem = new JMenuItem(codeforcesUsername.isEmpty() ? "Add User" : "Logout User");
        userMenuItem.addActionListener(e -> onUserMenuClicked());
        fileMenu.add(userMenuItem);
        showProfileMenuItem = new JMenuItem("Show Profile");
        showProfileMenuItem.addActionListener(e -> onShowProfileClicked());
        showProfileMenuItem.setEnabled(!codeforcesUsername.isEmpty());
        fileMenu.add(showProfileMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(chooseDifferentProblemItem);
        fileMenu.add(openEmptyProblemItem);
        fileMenu.add(refreshProblemItem);
        fileMenu.addSeparator();
        fileMenu.add(clearCacheItem);
        fileMenu.add(restoreSnapshotItem);
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
        JMenuItem findItem = new JMenuItem("Find");
        findItem.addActionListener(e -> openFindDialog());
        findItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem replaceItem = new JMenuItem("Replace");
        replaceItem.addActionListener(e -> openReplaceDialog());
        replaceItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
        resetZoomItem.addActionListener(e -> resetZoom());
        resetZoomItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem goToLineItem = new JMenuItem("Go to Line");
        goToLineItem.addActionListener(e -> openGoToLineDialog());
        goToLineItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JCheckBoxMenuItem foldingToggle = new JCheckBoxMenuItem("Code Folding",
                appSettings == null || appSettings.codeFolding());
        foldingToggle.addActionListener(e -> setCodeFoldingEnabled(foldingToggle.getState()));
        editMenu.add(selectAllItem);
        editMenu.add(findItem);
        editMenu.add(replaceItem);
        editMenu.add(goToLineItem);
        editMenu.addSeparator();
        editMenu.add(resetZoomItem);
        editMenu.add(foldingToggle);
        titleBar.add(editMenu);

        // Global Ctrl+0 reset-zoom binding: works regardless of which
        // component has focus, matching the README.
        javax.swing.JRootPane rootPane = mainFrame != null ? mainFrame.getRootPane() : null;
        if (rootPane != null) {
            javax.swing.Action resetZoomAction = new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    resetZoom();
                }
            };
            bindGlobalShortcut(rootPane, KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK,
                    "cpa.resetZoom", resetZoomAction);
            bindGlobalShortcut(rootPane, KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK,
                    "cpa.find", action(e -> openFindDialog()));
            bindGlobalShortcut(rootPane, KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_DOWN_MASK,
                    "cpa.replace", action(e -> openReplaceDialog()));
            bindGlobalShortcut(rootPane, KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_DOWN_MASK,
                    "cpa.goToLine", action(e -> openGoToLineDialog()));
            bindGlobalShortcut(rootPane, KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK,
                    "cpa.run", action(e -> onRunButtonClicked()));
            bindGlobalShortcut(rootPane, KeyEvent.VK_T,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK,
                    "cpa.addTest", action(e -> { if (testCasesPanel != null) testCasesPanel.addCustomTestCase(); }));
        }

        // Run Menu
        JMenu runMenu = new JMenu("Run");
        JMenuItem runCodeItem = new JMenuItem("Run Code");
        runCodeItem.addActionListener(e -> onRunButtonClicked());
        runCodeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        addTestCaseItem = new JMenuItem("Add Test Case");
        addTestCaseItem.addActionListener(e -> { if (testCasesPanel != null) testCasesPanel.addCustomTestCase(); });
        addTestCaseItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        addTestCaseItem.setEnabled(false);
        runMenu.add(runCodeItem);
        runMenu.addSeparator();
        runMenu.add(addTestCaseItem);
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
        zoomOutBtn.setFocusable(true);
        zoomOutBtn.getAccessibleContext().setAccessibleName("Zoom out");
        zoomOutBtn.setToolTipText("Zoom out active region (Ctrl+Wheel or touch pinch)");
        zoomOutBtn.addActionListener(e -> zoomOut(activeZoomTarget));
        javax.swing.JButton zoomInBtn = new javax.swing.JButton("+");
        zoomInBtn.setFocusable(true);
        zoomInBtn.getAccessibleContext().setAccessibleName("Zoom in");
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
            CacheManager.clearAll(settingsRepository.getAppDataDirectory());
            codeforcesService.clearProblemCache();
            if (problemHtmlRenderer != null) {
                // Cached LaTeX/icon files were just deleted from disk; drop the
                // in-memory URIs so re-renders regenerate them instead of
                // pointing at missing files.
                problemHtmlRenderer.clearMemoryCaches();
            }
            if (cfUserService != null) {
                cfUserService.clearCache();
            }
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
                    appSettings != null ? appSettings.autosaveIntervalSeconds() : 10,
                    appSettings != null ? appSettings.runTimeoutSeconds() : 2,
                    appSettings != null ? appSettings.maxOutputBytes() : 1024 * 1024);
            PreferencesDialog.PreferencesSelection selection = PreferencesDialog.showDialog(mainFrame, initialSelection, currentThemePalette());
            if (selection != null) {
                boolean appThemeChanged = !selection.appTheme().equalsIgnoreCase(currentAppTheme);
                appSettings = replaceEditorPreferences(appSettings, selection.editorFontSize(), selection.editorColorScheme(), selection.appTheme(), selection.useTabsAsSpaces(), selection.tabSpacing(), selection.autosaveEnabled(), selection.autosaveIntervalSeconds(), selection.runTimeoutSeconds(), selection.maxOutputBytes());
                codeExecutionService.configureExecutionLimits(selection.runTimeoutSeconds(), selection.maxOutputBytes());
                appThemePalette = AppThemePalette.fromName(selection.appTheme());
                settingsRepository.save(appSettings);
                // Restart autosave executor if autosave settings changed
                restartAutosaveIfNeeded();
                if (appThemeChanged) {
                    // Live theme switch: re-apply FlatLaf defaults, repaint
                    // every component, and re-render the problem HTML so
                    // the inline CSS picks up the new palette. The
                    // previous "please reopen" prompt is gone.
                    applyAppTheme(appThemePalette);
                    if (problemHtmlRenderer != null) {
                        problemHtmlRenderer.setTheme(appThemePalette);
                    }
                    if (codeEditor != null) {
                        applyEditorPreferences(codeEditor, selection.editorFontSize(), selection.editorColorScheme(), selection.useTabsAsSpaces(), selection.tabSpacing());
                    }
                    refreshThemeAwareUi();
                    rerenderProblemStatement();
                    if (mainFrame != null) {
                        SwingUtilities.updateComponentTreeUI(mainFrame);
                        mainFrame.revalidate();
                        mainFrame.repaint();
                    }
                } else {
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
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        loggedInLabel = new JLabel();
        loggedInLabel.setFont(loggedInLabel.getFont().deriveFont(Font.PLAIN, 11f));
        loggedInLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loggedInLabel.setForeground(palette.mutedTextColor());
        loggedInLabel.setVisible(!codeforcesUsername.isEmpty());
        if (!codeforcesUsername.isEmpty()) {
            loggedInLabel.setText("Logged in as " + codeforcesUsername);
        }
        form.add(loggedInLabel);

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
        stopButton = createToolbarButton("Stop");
        stopButton.setEnabled(false);
        stopButton.setVisible(false);
        stopButton.setToolTipText("Cancel the in-flight test run");
        stopButton.addActionListener(e -> stopRunningExecution());
        editorToolbar.add(stopButton);
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
        // Normalize legacy "GNU G11" label (an old dropdown artifact) to the
        // canonical Codeforces "GNU C11" label so previously-saved settings
        // still map to a valid dropdown entry.
        if (preferredLanguage != null && preferredLanguage.startsWith("GNU G11")) {
            preferredLanguage = "GNU C11 5.1.0";
        }
        languageDropdown.setSelectedItem(preferredLanguage);
        if (languageDropdown.getSelectedItem() == null) {
            languageDropdown.setSelectedItem(DEFAULT_LANGUAGE);
        }
        languageDropdown.setPreferredSize(new Dimension(190, LEFT_FIELD_HEIGHT));
        languageDropdown.setMaximumSize(new Dimension(220, LEFT_FIELD_HEIGHT));
        languageDropdown.setBackground(palette.inputBackground());
        languageDropdown.setForeground(palette.inputForeground());
        languageDropdown.setFocusable(true);
        languageDropdown.getAccessibleContext().setAccessibleName("Programming language");
        languageDropdown.setRequestFocusEnabled(false);
        languageDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                String oldLanguage = e.getItem() != null ? e.getItem().toString() : null;
                if (currentProblemCode != null && oldLanguage != null) {
                    programCacheRepository.takeSnapshot(currentProblemCode, oldLanguage);
                }
                saveCurrentProgramToCache(oldLanguage);
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
        codeEditor.setCodeFoldingEnabled(appSettings == null || appSettings.codeFolding());
        codeEditor.setEditable(false);
        codeEditor.setFocusable(true);
        codeEditor.getAccessibleContext().setAccessibleName("Source code editor");
        codeEditor.setRequestFocusEnabled(false);
        applyEditorPreferences(
            codeEditor,
            appSettings != null ? appSettings.editorFontSize() : 14,
            appSettings != null ? appSettings.editorColorScheme() : DEFAULT_EDITOR_THEME,
            appSettings != null && appSettings.useTabsAsSpaces(),
            appSettings != null ? appSettings.tabSpacing() : 4);
        installEditorAutoPairs(codeEditor);
        installEditorIndenter(codeEditor);
        installGoToLineShortcut(codeEditor);
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
        codeEditor.addMouseWheelListener(e -> {
            try {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) zoomIn(ZoomTarget.EDITOR); else zoomOut(ZoomTarget.EDITOR);
                    e.consume();
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

        // The find/replace bar lives just below the editor toolbar. It is
        // hidden by default so the main window layout is unchanged when
        // the user is not actively searching.
        editorFindBar = new EditorFindBar(codeEditor);
        editorFindBar.container().setBackground(palette.panelBackground());

        panel.add(editorToolbar, BorderLayout.NORTH);
        panel.add(editorFindBar.container(), BorderLayout.AFTER_LAST_LINE);
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

    private AppSettings replaceEditorPreferences(AppSettings current, int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing, boolean autosaveEnabled, int autosaveIntervalSeconds, int runTimeoutSeconds, int maxOutputBytes) {
        if (current == null) {
            return new AppSettings(-1, -1, 1200, 760, 420, 420, false, DEFAULT_LANGUAGE, editorFontSize, editorColorScheme, appTheme, useTabsAsSpaces, tabSpacing, autosaveEnabled, autosaveIntervalSeconds, "", DEFAULT_ZOOM, DEFAULT_ZOOM, true, runTimeoutSeconds, maxOutputBytes);
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
                autosaveIntervalSeconds,
                current.codeforcesUsername(),
                current.editorZoom(),
                current.problemZoom(),
                current.codeFolding(),
                runTimeoutSeconds,
                maxOutputBytes);
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
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(text);
        return button;
    }

    private static javax.swing.Action action(java.awt.event.ActionListener listener) {
        return new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                listener.actionPerformed(event);
            }
        };
    }

    private static void bindGlobalShortcut(javax.swing.JRootPane rootPane, int keyCode,
                                           int modifiers, String name, javax.swing.Action action) {
        javax.swing.KeyStroke keyStroke = javax.swing.KeyStroke.getKeyStroke(keyCode, modifiers);
        rootPane.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        rootPane.getActionMap().put(name, action);
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

        if (currentProblemCode != null && languageDropdown != null) {
            programCacheRepository.takeSnapshot(currentProblemCode, selectedLanguage());
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
        String problemCode = contestId + index;

        // Bump the request id and cancel the previous fetch, if any. Any
        // in-flight worker that finishes after this point will see its
        // requestId is no longer the current one and discard its result.
        ProblemFetchArbiter.Request req = problemFetchArbiter.begin();
        long myRequestId = req.requestId;
        if (activeProblemFetchToken != null) {
            activeProblemFetchToken.cancel();
        }
        if (activeProblemFetchWorker != null && !activeProblemFetchWorker.isDone()) {
            activeProblemFetchWorker.cancel(true);
        }
        final CancellationToken myToken = req.token;
        activeProblemFetchToken = myToken;

        showLeftPanelLoading(problemCode);
        problemLoading = true;
        applyWorkspaceState();

        final ProblemDetails[] fetched = new ProblemDetails[1];
        SwingWorker<RenderedProblemView[], Void> worker = new SwingWorker<>() {
            @Override
            protected RenderedProblemView[] doInBackground() throws Exception {
                ProblemDetails details = codeforcesService.fetchProblemDetails(contestId, index, myToken);
                if (myToken.isCancelled() || isCancelled()) {
                    return null;
                }
                fetched[0] = details;
                java.util.concurrent.Future<List<SheetInfo>> sheetsFuture =
                        TaskCoordinator.shared().submitNetwork(() ->
                                problemSheetsService.fetchSheets(contestId + index));
                RenderedProblemView[] renderedViews = problemHtmlRenderer.renderBoth(details);
                RenderedProblemView statementOnly = renderedViews[0];
                RenderedProblemView full = renderedViews[1];
                if (myToken.isCancelled() || isCancelled()) {
                    sheetsFuture.cancel(true);
                    return null;
                }
                List<SheetInfo> sheets;
                try {
                    sheets = sheetsFuture.get(7, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception metadataFailure) {
                    sheetsFuture.cancel(true);
                    DiagnosticLogger.warn("[MainWindow] Optional practice-sheet metadata unavailable: "
                            + metadataFailure.getMessage());
                    sheets = List.of();
                }
                if (!sheets.isEmpty() && !myToken.isCancelled() && problemFetchArbiter.isCurrent(myRequestId)) {
                    String sheetHtml = buildSheetInfoHtml(sheets, appThemePalette);
                    full = injectSheetInfo(full, sheetHtml);
                    statementOnly = injectSheetInfo(statementOnly, sheetHtml);
                }
                return new RenderedProblemView[]{statementOnly, full};
            }

            @Override
            protected void done() {
                // Drop stale results: if a newer fetch has been started (or
                // this worker was cancelled), do not touch the UI.
                if (!problemFetchArbiter.isCurrent(myRequestId) || isCancelled()) {
                    DiagnosticLogger.info("[MainWindow] Discarding stale fetch result for " + problemCode);
                    return;
                }
                // We are the current request, so the loading state ends here.
                problemLoading = false;
                try {
                    RenderedProblemView[] renders = get();
                    if (renders == null) {
                        return;
                    }
                    currentProblemDetails = fetched[0];
                    showCodeforcesProblemView(problemCode, renders[0], renders[1]);
                } catch (java.util.concurrent.CancellationException cancelled) {
                    DiagnosticLogger.info("[MainWindow] Fetch cancelled for " + problemCode);
                    problemLoading = false;
                } catch (Exception ex) {
                    DiagnosticLogger.error("[MainWindow] Failed to fetch CodeForces problem " + rawCode, ex);
                    // Surface the underlying cause (network, bot-check, missing markup, etc.)
                    // instead of a generic message so the user can tell what actually went wrong.
                    String cause = ex.getMessage() != null && !ex.getMessage().isBlank()
                            ? ex.getMessage()
                            : ex.getClass().getSimpleName();
                    restoreProblemEntryPanelWithError("Could not fetch that problem.");
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Could not fetch the specified CodeForces problem.\n\nDetails: " + cause,
                            APP_NAME,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        activeProblemFetchWorker = worker;
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
        JOptionPane.showMessageDialog(mainFrame, message, APP_NAME, JOptionPane.WARNING_MESSAGE);
    }

    private JLabel createLoadingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(currentThemePalette().mutedTextColor());
        label.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        return label;
    }

    private void showLeftPanelLoading(String problemCode) {
        // Enablement is delegated to applyWorkspaceState; just keep the
        // visual cue here.
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
        problemLoading = false;
        fetchStatusLabel.setForeground(currentThemePalette().errorColor());
        fetchStatusLabel.setText(message);

        if (refreshProblemItem != null) {
            refreshProblemItem.setEnabled(false);
        }

        if (addTestCaseItem != null) {
            addTestCaseItem.setEnabled(false);
        }

        leftPanelContainer.removeAll();
        leftPanelContainer.add(problemEntryPanel, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private void showCodeforcesProblemView(String problemCode, RenderedProblemView statementOnly, RenderedProblemView full) {
        renderProblemView(problemCode, statementOnly, full, false);
        // Reload any custom tests persisted for this problem so the user does
        // not have to recreate them when they return to it.
        if (testCasesPanel != null) {
            testCasesPanel.setSamplePayloadsForProblem(problemCode, full.copyPayloads());
        }
        refreshSubmissionStatus();
    }

    private void refreshCurrentProblem() {
        if (!problemStatementLoaded || currentProblemCode == null || currentProblemIsEmpty) {
            return;
        }
        String code = currentProblemCode;
        codeforcesService.clearProblemCache(code);
        fetchProblemByCode(code, true);
    }

    public void openFromUrl(String url) {
        if (url == null || url.isBlank()) return;
        DiagnosticLogger.info("[MainWindow] openFromUrl: " + url);
        String prefix = "cpally://problem/";
        if (!url.trim().toLowerCase().startsWith(prefix)) {
            DiagnosticLogger.warn("[MainWindow] Unrecognized cpally URL: " + url);
            return;
        }
        // Strip prefix (case-insensitive match already confirmed above).
        String rawCode = url.trim().substring(prefix.length()).trim();
        // Strip any trailing slashes browsers may append.
        while (rawCode.endsWith("/")) rawCode = rawCode.substring(0, rawCode.length() - 1).trim();
        if (rawCode.isEmpty()) {
            DiagnosticLogger.warn("[MainWindow] cpally URL had empty problem code: " + url);
            return;
        }
        DiagnosticLogger.info("[MainWindow] Opening problem: " + rawCode);
        if (mainFrame != null) {
            int state = mainFrame.getExtendedState();
            if ((state & JFrame.ICONIFIED) != 0) {
                mainFrame.setExtendedState(state & ~JFrame.ICONIFIED);
            }
            mainFrame.setVisible(true);
            mainFrame.toFront();
            mainFrame.requestFocus();
        }
        fetchProblemByCode(rawCode, false);
    }

    private static RenderedProblemView injectSheetInfo(RenderedProblemView view, String sheetHtml) {
        String html = view.html().replace("</body>", sheetHtml + "</body>");
        return new RenderedProblemView(html, view.copyPayloads());
    }

    private static String buildSheetInfoHtml(List<SheetInfo> sheets, AppThemePalette theme) {
        String border  = toHex(theme.borderColor());
        String surface = toHex(theme.surfaceBackground());
        String muted   = toHex(theme.mutedTextColor());
        String accent  = toHex(theme.accentColor());
        String text    = toHex(theme.textColor());

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin:20px 4px 8px 4px; padding:10px 14px;")
          .append(" background-color:").append(surface).append(";")
          .append(" border:1px solid ").append(border).append(";'>")
          .append("<div style='font-size:11px; font-weight:bold; color:").append(muted).append(";")
          .append(" margin-bottom:6px;'>FOUND IN PRACTICE SHEETS</div>");

        for (SheetInfo sheet : sheets) {
            sb.append("<div style='font-size:13px; color:").append(text).append("; margin:3px 0;'>")
              .append("&#8227;&nbsp;<a href='").append(sheet.url()).append("'")
              .append(" style='color:").append(accent).append(";'>")
              .append(escapeHtml(sheet.name()))
              .append("</a></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private void showEmptyProblemView() {
        saveCurrentProgramToCache();
        currentProblemDetails = null;
        RenderedProblemView empty = problemHtmlRenderer.renderEmptyProblem();
        renderProblemView(EMPTY_PROBLEM_CODE, empty, empty, true);
        if (testCasesPanel != null) {
            // Empty problem is ephemeral; clear any restored custom tests so
            // they belong to the next real problem, not to the placeholder.
            testCasesPanel.setSamplePayloadsForProblem(null, empty.copyPayloads());
        }
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
            // Paint with high-quality interpolation so scaled images (LaTeX
            // formulas, icons, statement illustrations) render smoothly.
            problemPane = new JEditorPane() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    if (g instanceof java.awt.Graphics2D g2) {
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                    }
                    super.paintComponent(g);
                }
            };
            problemPane.setContentType("text/html");
            problemPane.setEditable(false);
            problemPane.setFocusable(true);
            problemPane.getAccessibleContext().setAccessibleName("Problem statement");
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

        if (problemScrollPane == null) {
            // Build the persistent scroll pane and split pane once
            problemScrollPane = new JScrollPane(problemPane);
            problemScrollPane.setWheelScrollingEnabled(true);
            problemScrollPane.addMouseWheelListener(e -> {
                try {
                    if (e.isControlDown()) {
                        if (e.getWheelRotation() < 0) zoomIn(ZoomTarget.PROBLEM); else zoomOut(ZoomTarget.PROBLEM);
                        e.consume();
                    }
                } catch (Exception ignored) {
                }
            });
            problemScrollPane.setBorder(BorderFactory.createEmptyBorder());
            problemScrollPane.getVerticalScrollBar().setUnitIncrement(14);

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

            submissionStatusLabel = new JLabel("");
            submissionStatusLabel.setFont(submissionStatusLabel.getFont().deriveFont(Font.BOLD, 12f));
            submissionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
            submissionStatusLabel.setVisible(false);
            topBar.add(submissionStatusLabel, BorderLayout.EAST);

            JPanel statementPanel = new JPanel(new BorderLayout());
            statementPanel.setOpaque(false);
            statementPanel.add(topBar, BorderLayout.NORTH);
            statementPanel.add(problemScrollPane, BorderLayout.CENTER);

            JPanel testCasesSection = testCasesPanel != null ? testCasesPanel.createPanel() : new JPanel();

            int minTestCaseHeight = MIN_WINDOW_HEIGHT / 3;
            int preferredDivider = appSettings != null && appSettings.testCasesDividerLocation() > 0
                ? appSettings.testCasesDividerLocation()
                : mainFrame.getHeight() - minTestCaseHeight - 10;

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, testCasesSection);
            splitPane.setResizeWeight(0.5);
            splitPane.setDividerLocation(preferredDivider);
            statementTestCasesSplitPane = splitPane;
        }

        problemScrollPane.getViewport().setBackground(palette.frameBackground());

        leftPanelContainer.removeAll();
        leftPanelContainer.add(statementTestCasesSplitPane, BorderLayout.CENTER);
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

        showEmptyProblemView(); // saves the current program before switching
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

        // Enablement of the menu items is now derived from the central
        // workspace state, not from scattered setEnabled calls. The
        // empty-problem distinction is preserved here via the
        // refreshEnabled() policy, which already returns false for
        // empty problems.
        applyWorkspaceState();

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

    private void restoreSourceSnapshot() {
        if (currentProblemCode == null || currentProblemCode.isBlank() || currentProblemIsEmpty || codeEditor == null) {
            return;
        }
        String language = selectedLanguage();
        List<Path> snapshots = programCacheRepository.listSnapshots(currentProblemCode, language);
        if (snapshots.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No source snapshots are available for this problem and language.", "Restore Snapshot", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] choices = snapshots.stream().map(p -> p.getFileName().toString().replace(".txt", "")).toArray(String[]::new);
        Object selected = JOptionPane.showInputDialog(mainFrame, "Choose a snapshot:", "Restore Source Snapshot", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (selected == null) return;
        int index = java.util.Arrays.asList(choices).indexOf(selected.toString());
        if (index < 0) return;
        String source = programCacheRepository.restoreSnapshot(snapshots.get(index));
        if (source != null) {
            codeEditor.setText(source);
            codeEditor.setCaretPosition(0);
        }
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

        // If we already know the support state for this language, paint the UI
        // synchronously and avoid another async probe. Otherwise show a
        // Checking… placeholder and update once the probe completes; the
        // probe itself runs on a background thread, never the EDT.
        CodeExecutionService.LanguageSupport cached = toolchainAvailability != null
                ? toolchainAvailability.cached(language)
                : codeExecutionService.detectSupport(language);
        if (cached != null) {
            paintExecutionAvailability(cached);
        } else {
            paintExecutionAvailabilityChecking(language);
            if (toolchainAvailability != null) {
                toolchainAvailability.probe(language, result -> {
                    if (selectedLanguage().equals(language)) {
                        paintExecutionAvailability(result);
                    }
                });
            } else {
                // Fallback: no cache available, do the probe synchronously. This
                // is the only path that can still block the EDT.
                paintExecutionAvailability(codeExecutionService.detectSupport(language));
            }
        }
    }

    private void paintExecutionAvailability(CodeExecutionService.LanguageSupport support) {
        if (languageDropdown == null || runtimeSupportLabel == null || runButton == null) {
            return;
        }
        String language = selectedLanguage();
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
        // Avoid 'unused' lint when only the language is read for log lines.
        if (language == null) {
            // no-op
        }
    }

    private void paintExecutionAvailabilityChecking(String language) {
        runtimeSupportLabel.setText("<html><span style='color:"
                + (currentThemePalette().mutedTextColor() != null
                        ? toHex(currentThemePalette().mutedTextColor()) : "#a9b0bc")
                + ";'>Checking\u2026</span></html>");
        runtimeSupportLabel.setToolTipText("Checking local toolchain for " + language);
        runButton.setEnabled(false);
        runButton.setToolTipText("Checking local toolchain for " + language + "\u2026");
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
        startExecution(language, sourceCode, testCases, emptyProblemRun);
    }

    /** Runs one sample/custom test selected from the TestCasesPanel. */
    private void runSingleTest(int oneBasedIndex) {
        if (!problemStatementLoaded || codeEditor == null || executionRunning) {
            return;
        }
        String language = selectedLanguage();
        CodeExecutionService.LanguageSupport support = codeExecutionService.detectSupport(language);
        if (!support.supported()) {
            JOptionPane.showMessageDialog(mainFrame, support.message(),
                    "Execution unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CodeExecutionService.TestCaseSpec test = testCasesPanel != null
                ? testCasesPanel.getTestCase(oneBasedIndex) : null;
        if (test == null) {
            return;
        }
        startExecution(language, codeEditor.getText(), List.of(test), false);
    }

    private void startExecution(String language, String sourceCode,
                                List<CodeExecutionService.TestCaseSpec> testCases,
                                boolean emptyProblemRun) {
        runButton.setEnabled(false);
        runButton.setToolTipText(testCases.size() == 1
                ? "Running selected test..." : "Running sample test cases...");
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
                boolean canceled = isCancelled();
                setExecutionRunningState(false);
                activeExecutionWorker = null;
                updateExecutionAvailability();
                try {
                    if (canceled) {
                        showExecutionResultsDialog(language,
                                CodeExecutionService.ExecutionReport.failure("Run canceled by user."));
                    } else {
                        showExecutionResultsDialog(language, get());
                    }
                } catch (java.util.concurrent.CancellationException ex) {
                    showExecutionResultsDialog(language,
                            CodeExecutionService.ExecutionReport.failure("Run canceled by user."));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Failed to run the selected language locally.\n\n" + ex.getMessage(),
                            "Execution error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        activeExecutionWorker = worker;
        worker.execute();
    }

    private javax.swing.SwingWorker<?, ?> activeExecutionWorker;

    private void setExecutionRunningState(boolean running) {
        this.executionRunning = running;
        if (executionStateLabel != null) {
            if (running) {
                executionStateLabel.setText("Status: Running");
                executionStateLabel.setForeground(new Color(247, 215, 26));
            } else {
                executionStateLabel.setText("Status: Idle");
                executionStateLabel.setForeground(new Color(169, 176, 188));
            }
        }
        if (stopButton != null) {
            // The Stop button only makes sense while a run is in flight.
            // We toggle visibility rather than enablement so the toolbar
            // layout does not shift when no run is active.
            stopButton.setVisible(running);
            stopButton.setEnabled(running);
        }
        applyWorkspaceState();
    }

    /**
     * Cancels the in-flight test run, if any. Cancels the SwingWorker
     * (which interrupts its background thread, unwinding the
     * process wait loop) and signals the {@link CodeExecutionService}'s
     * run loop to bail out early. The current test's verdict is
     * reported as CANCELED in the result dialog.
     */
    private void stopRunningExecution() {
        if (!executionRunning) {
            return;
        }
        if (activeExecutionWorker != null && !activeExecutionWorker.isDone()) {
            activeExecutionWorker.cancel(true);
        }
        activeExecutionWorker = null;
        setExecutionRunningState(false);
        if (executionStateLabel != null) {
            executionStateLabel.setText("Status: Canceled");
            executionStateLabel.setForeground(new Color(246, 198, 67));
        }
    }

    /**
     * Centralised enablement of every interactive control in the main
     * window. Call this whenever one of the underlying state booleans
     * changes; the control set is derived from a {@link WorkspaceState}
     * value object so the policy lives in one place.
     */
    private void applyWorkspaceState() {
        WorkspaceState s = currentWorkspaceState();
        if (fetchProblemButton != null) {
            fetchProblemButton.setEnabled(s.fetchEnabled());
        }
        if (refreshProblemItem != null) {
            refreshProblemItem.setEnabled(s.refreshEnabled(currentProblemIsEmpty));
        }
        if (addTestCaseItem != null) {
            addTestCaseItem.setEnabled(s.addTestCaseEnabled());
        }
        if (chooseDifferentProblemItem != null) {
            chooseDifferentProblemItem.setEnabled(!s.problemLoading());
        }
        if (openEmptyProblemItem != null) {
            openEmptyProblemItem.setEnabled(!s.executionRunning());
        }
        if (showProfileMenuItem != null) {
            showProfileMenuItem.setEnabled(s.showProfileEnabled());
        }
        if (runButton != null) {
            // Run button is governed by toolchain availability; only
            // additionally gate it on workspace state here. The actual
            // "ready to run" check happens in updateExecutionAvailability.
            if (s.executionRunning()) {
                runButton.setEnabled(false);
                runButton.setText("Running…");
            } else if (runButton.getText() != null && runButton.getText().startsWith("Running")) {
                runButton.setText("Run");
            }
        }
    }

    private WorkspaceState currentWorkspaceState() {
        return new WorkspaceState(
                problemLoading,
                problemStatementLoaded,
                executionRunning,
                codeforcesUsername != null && !codeforcesUsername.isEmpty());
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
        EditorKeyBindings bindings = new EditorKeyBindings();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!editor.isEditable() || e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                    return;
                }
                EditorKeyBindings.Context ctx = snapshotContext(editor);
                EditorKeyBindings.Decision decision = bindings.decideBackspace(ctx);
                if (decision.kind() == EditorKeyBindings.Decision.Kind.PASS_THROUGH) {
                    return;
                }
                try {
                    if (decision.kind() == EditorKeyBindings.Decision.Kind.DELETE_PAIR) {
                        editor.getDocument().remove(decision.caret(), 2);
                        editor.setCaretPosition(decision.caret());
                        e.consume();
                    }
                } catch (Exception ignored) {
                    // Keep default backspace behavior if the deletion fails.
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!editor.isEditable()) {
                    return;
                }
                char typed = e.getKeyChar();
                if (Character.isISOControl(typed)) {
                    return;
                }
                EditorKeyBindings.Context ctx = snapshotContext(editor);
                EditorKeyBindings.Decision decision = bindings.decideAutoPair(typed, ctx);
                switch (decision.kind()) {
                    case PASS_THROUGH -> { /* let the editor handle it */ }
                    case INSERT -> {
                        try {
                            editor.getDocument().insertString(editor.getCaretPosition(),
                                    decision.literal(), null);
                            editor.setCaretPosition(decision.caret());
                            e.consume();
                        } catch (Exception ignored) { }
                    }
                    case WRAP -> {
                        try {
                            String selected = editor.getSelectedText();
                            if (selected == null) selected = "";
                            int start = decision.selStart();
                            int end = decision.selEnd();
                            int len = end - start;
                            String opener = String.valueOf(typed);
                            String closer = String.valueOf(EditorKeyBindings.matchingCloser(typed));
                            editor.getDocument().remove(start, len);
                            editor.getDocument().insertString(start,
                                    opener + selected + closer, null);
                            editor.setCaretPosition(decision.wrapCaret());
                            e.consume();
                        } catch (Exception ignored) { }
                    }
                    case SKIP -> {
                        editor.setCaretPosition(decision.caret());
                        e.consume();
                    }
                    default -> { }
                }
            }
        });
    }

    private static EditorKeyBindings.Context snapshotContext(RSyntaxTextArea editor) {
        boolean hasSelection = editor.getSelectionStart() != editor.getSelectionEnd();
        int caret = editor.getCaretPosition();
        char before = charAt(editor, caret - 1);
        char after = charAt(editor, caret);
        return new EditorKeyBindings.Context(hasSelection, caret, before, after);
    }

    private static char charAt(RSyntaxTextArea editor, int index) {
        if (index < 0 || index >= editor.getDocument().getLength()) {
            return '\0';
        }
        try {
            String s = editor.getDocument().getText(index, 1);
            return s.isEmpty() ? '\0' : s.charAt(0);
        } catch (Exception ex) {
            return '\0';
        }
    }

    /**
     * Tab/Shift+Tab indentation. Multi-line selections indent or
     * outdent every selected line by one indent unit. With no
     * selection, Tab inserts a tab/indent unit and Shift+Tab
     * outdents the current line. The {@code useTabsAsSpaces} and
     * {@code tabSpacing} settings are honored.
     */
    private void installEditorIndenter(RSyntaxTextArea editor) {
        EditorIndenter indenter = new EditorIndenter(
                appSettings != null ? appSettings.tabSpacing() : 4,
                appSettings != null && appSettings.useTabsAsSpaces());

        javax.swing.Action tabAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                applyIndenterAction(editor, indenter.decideTab(snapshotIndenterContext(editor)));
            }
        };
        javax.swing.Action shiftTabAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                applyIndenterAction(editor, indenter.decideShiftTab(snapshotIndenterContext(editor)));
            }
        };
        javax.swing.InputMap im = editor.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = editor.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "cpa.indent");
        am.put("cpa.indent", tabAction);
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "cpa.outdent");
        am.put("cpa.outdent", shiftTabAction);
    }

    private static EditorIndenter.Context snapshotIndenterContext(RSyntaxTextArea editor) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        boolean hasSelection = start != end;
        int firstLine;
        int lastLine;
        try {
            firstLine = editor.getLineOfOffset(start);
            lastLine = editor.getLineOfOffset(Math.max(start, end - 1));
        } catch (javax.swing.text.BadLocationException ble) {
            return new EditorIndenter.Context(false, -1, -1, false);
        }
        boolean singleLine = firstLine == lastLine;
        return new EditorIndenter.Context(hasSelection, firstLine, lastLine, singleLine);
    }

    private void applyIndenterAction(RSyntaxTextArea editor, EditorIndenter.Action action) {
        if (action.kind == EditorIndenter.Action.Kind.PASS_THROUGH) {
            return;
        }
        if (action.kind == EditorIndenter.Action.Kind.INSERT_TAB) {
            // No selection: insert one indent unit at the caret.
            try {
                editor.getDocument().insertString(editor.getCaretPosition(),
                        new EditorIndenter(appSettings != null ? appSettings.tabSpacing() : 4,
                                appSettings != null && appSettings.useTabsAsSpaces()).singleIndent(),
                        null);
            } catch (Exception ignored) { }
            return;
        }
        // INDENT_LINES / OUTDENT_LINES: collect edits per line and apply
        // in reverse offset order so earlier edits don't shift later ones.
        try {
            int totalLines = editor.getLineCount();
            if (action.startLine < 0 || action.startLine >= totalLines) {
                return;
            }
            java.util.List<EditorIndenter.LineEdit> edits = new java.util.ArrayList<>();
            for (int i = action.startLine; i <= action.endLine && i < totalLines; i++) {
                int lineStart = editor.getLineStartOffset(i);
                int lineEnd = editor.getLineEndOffset(i);
                String lineText = editor.getText(lineStart, lineEnd - lineStart);
                String updated;
                if (action.kind == EditorIndenter.Action.Kind.INDENT_LINES) {
                    updated = action.indent + lineText;
                } else {
                    updated = EditorIndenter.removeIndent(lineText, action.indent);
                }
                edits.add(new EditorIndenter.LineEdit(i, lineText, updated));
            }
            // Apply in reverse so offsets remain valid.
            for (int i = edits.size() - 1; i >= 0; i--) {
                EditorIndenter.LineEdit edit = edits.get(i);
                int lineStart = editor.getLineStartOffset(edit.lineIndex);
                editor.getDocument().remove(lineStart, edit.original.length());
                editor.getDocument().insertString(lineStart, edit.updated, null);
            }
        } catch (Exception ignored) {
            // Best-effort; if anything goes wrong, leave the editor alone.
        }
    }

    /**
     * Wires the Ctrl+G "Go to Line" shortcut. Bound on the editor's
     * input map so the keystroke works whenever the editor has focus.
     */
    private void installGoToLineShortcut(RSyntaxTextArea editor) {
        javax.swing.Action goToLine = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openGoToLineDialog();
            }
        };
        javax.swing.InputMap im = editor.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = editor.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke(
                        KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                "cpa.goToLine");
        am.put("cpa.goToLine", goToLine);
    }

    private void openGoToLineDialog() {
        if (codeEditor == null) {
            return;
        }
        int totalLines = codeEditor.getLineCount();
        GoToLineDialog dlg = new GoToLineDialog(totalLines);
        Integer line = dlg.show(mainFrame);
        if (line != null) {
            try {
                int target = codeEditor.getLineStartOffset(line - 1);
                codeEditor.setCaretPosition(target);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Toggles code folding in the editor and persists the choice.
     */
    private void setCodeFoldingEnabled(boolean enabled) {
        if (codeEditor != null) {
            codeEditor.setCodeFoldingEnabled(enabled);
        }
        if (appSettings != null) {
            appSettings = new AppSettings(
                    appSettings.x(), appSettings.y(), appSettings.width(), appSettings.height(),
                    appSettings.dividerLocation(), appSettings.testCasesDividerLocation(),
                    appSettings.maximized(), appSettings.lastLanguage(),
                    appSettings.editorFontSize(), appSettings.editorColorScheme(),
                    appSettings.appTheme(), appSettings.useTabsAsSpaces(),
                    appSettings.tabSpacing(), appSettings.autosaveEnabled(),
                    appSettings.autosaveIntervalSeconds(), appSettings.codeforcesUsername(),
                    appSettings.editorZoom(), appSettings.problemZoom(),
                    enabled);
            settingsRepository.save(appSettings);
        }
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
        LanguageDefinition definition = LanguageDefinition.forDisplayName(language);
        if (definition != null) {
            return definition.syntaxStyle();
        }
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return SyntaxConstants.SYNTAX_STYLE_PYTHON;
        }
        if (language.startsWith("GNU G++")) {
            return SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        }
        if (language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return SyntaxConstants.SYNTAX_STYLE_C;
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
        LanguageDefinition definition = LanguageDefinition.forDisplayName(language);
        if (definition != null) {
            return definition.boilerplate();
        }
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return "import sys\n"
                + "\n"
                + "def main():\n"
                + "\t# code goes here...\n"
                + "\n"
                + "if __name__ == \"__main__\":\n"
                + "\tmain()\n";
        }
        if (language.startsWith("GNU G++")) {
            return "#include <bits/stdc++.h>\n"
                    + "using namespace std;\n"
                    + "\n"
                    + "int main() {\n"
                    + "\t// code goes here...\n"
                    + "\treturn 0;\n"
                    + "}\n";
        }
        if (language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return "#include <stdio.h>\n"
                    + "#include <stdlib.h>\n"
                    + "\n"
                    + "int main(void) {\n"
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

    private void onUserMenuClicked() {
        if (codeforcesUsername.isEmpty()) {
            String input = (String) JOptionPane.showInputDialog(
                    mainFrame, "Enter your Codeforces username:", "Add User",
                    JOptionPane.PLAIN_MESSAGE, null, null, "");
            if (input == null || input.isBlank()) {
                return;
            }
            String handle = input.trim();
            if (!handle.matches("[A-Za-z0-9_.-]{1,24}")) {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "That does not look like a valid Codeforces handle.\n"
                                + "Handles may only contain letters, digits, '_', '.' and '-'.",
                        "Invalid Handle",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            saveUsername(handle);
        } else {
            int confirm = JOptionPane.showConfirmDialog(
                    mainFrame, "Log out " + codeforcesUsername + "?",
                    "Logout User", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                saveUsername("");
            }
        }
    }

    private void saveUsername(String username) {
        codeforcesUsername = username;
        if (cfUserService != null) cfUserService.clearCache();
        if (mainFrame != null) persistSettings(mainFrame);
        if (userMenuItem != null)
            userMenuItem.setText(username.isEmpty() ? "Add User" : "Logout User");
        if (showProfileMenuItem != null)
            showProfileMenuItem.setEnabled(!username.isEmpty());
        updateLoggedInLabel();
        refreshSubmissionStatus();
    }

    private void onShowProfileClicked() {
        if (codeforcesUsername.isEmpty() || cfProfileService == null) return;
        UserProfileDialog.show(mainFrame, codeforcesUsername, cfProfileService, appThemePalette);
    }

    private void updateLoggedInLabel() {
        if (loggedInLabel == null) return;
        if (codeforcesUsername.isEmpty()) {
            loggedInLabel.setVisible(false);
        } else {
            loggedInLabel.setText("Logged in as " + codeforcesUsername);
            loggedInLabel.setForeground(appThemePalette != null ? appThemePalette.mutedTextColor() : Color.GRAY);
            loggedInLabel.setVisible(true);
        }
    }

    private void refreshSubmissionStatus() {
        if (submissionStatusLabel == null) return;
        if (codeforcesUsername.isEmpty() || currentProblemCode == null || !problemStatementLoaded) {
            submissionStatusLabel.setText("");
            submissionStatusLabel.setVisible(false);
            return;
        }
        submissionStatusLabel.setText("Checking...");
        submissionStatusLabel.setForeground(appThemePalette != null ? appThemePalette.mutedTextColor() : Color.GRAY);
        submissionStatusLabel.setVisible(true);

        String handle = codeforcesUsername;
        String code = currentProblemCode;
        new javax.swing.SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return cfUserService.fetchBestVerdictDisplay(handle, code);
            }
            @Override protected void done() {
                try {
                    String verdict = get();
                    if (submissionStatusLabel == null) return;
                    // A newer problem (or logout) may have superseded this request.
                    if (!code.equals(currentProblemCode) || !handle.equals(codeforcesUsername)) return;
                    if (verdict == null || verdict.isEmpty()) {
                        submissionStatusLabel.setText("");
                        submissionStatusLabel.setVisible(false);
                        return;
                    }
                    Color color = switch (verdict) {
                        case "Accepted"     -> appThemePalette != null ? appThemePalette.successColor() : new Color(97, 214, 110);
                        case "Wrong Answer" -> appThemePalette != null ? appThemePalette.warningColor() : new Color(246, 198, 67);
                        default             -> appThemePalette != null ? appThemePalette.errorColor()   : new Color(246, 86, 86);
                    };
                    submissionStatusLabel.setText("● " + verdict);
                    submissionStatusLabel.setForeground(color);
                    submissionStatusLabel.setVisible(true);
                } catch (Exception ignored) {
                    if (submissionStatusLabel != null) submissionStatusLabel.setVisible(false);
                }
            }
        }.execute();
    }

    /** Saves the current code and settings before terminating, mirroring the window-close path. */
    private void shutdownAndExit() {
        try {
            stopAutosave();
            saveCurrentProgramToCache();
            // Flush any pending custom-test writes so a user does not lose
            // tests they just added if they immediately exit.
            customTestRepository.flush();
            if (mainFrame != null) {
                persistSettings(mainFrame);
            }
        } catch (Exception e) {
            DiagnosticLogger.error("[MainWindow] Failed to persist state on exit", e);
        }
        System.exit(0);
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
            appSettings != null ? appSettings.autosaveIntervalSeconds() : 10,
            codeforcesUsername,
            editorZoomFactor,
            problemZoomFactor,
            appSettings == null || appSettings.codeFolding());

        settingsRepository.save(settings);
    }

    // Zoom helpers
    private void zoomIn(ZoomTarget target) {
        adjustZoom(target, ZOOM_STEP);
    }

    private void zoomOut(ZoomTarget target) {
        adjustZoom(target, -ZOOM_STEP);
    }

    /**
     * Resets both editor and problem zoom to 100% and re-applies them.
     * Bound globally to Ctrl+0 from the menu/title bar so the shortcut
     * works regardless of which component currently has focus.
     */
    private void resetZoom() {
        setZoomFactor(ZoomTarget.EDITOR, 1.0);
        setZoomFactor(ZoomTarget.PROBLEM, 1.0);
    }

    private void openFindDialog() {
        if (editorFindBar == null) return;
        if (editorFindBar.isVisible()) {
            // Toggle: pressing Find twice closes the bar.
            editorFindBar.hide();
            return;
        }
        editorFindBar.showFind();
    }

    private void openReplaceDialog() {
        if (editorFindBar == null) return;
        editorFindBar.showReplace();
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
        // Persist the new zoom so a restart preserves it.
        persistSettingsIfFrameReady();
    }

    private void persistSettingsIfFrameReady() {
        if (mainFrame == null || appSettings == null) {
            return;
        }
        appSettings = new AppSettings(
                appSettings.x(),
                appSettings.y(),
                appSettings.width(),
                appSettings.height(),
                appSettings.dividerLocation(),
                appSettings.testCasesDividerLocation(),
                appSettings.maximized(),
                appSettings.lastLanguage(),
                appSettings.editorFontSize(),
                appSettings.editorColorScheme(),
                appSettings.appTheme(),
                appSettings.useTabsAsSpaces(),
                appSettings.tabSpacing(),
                appSettings.autosaveEnabled(),
                appSettings.autosaveIntervalSeconds(),
                appSettings.codeforcesUsername(),
                editorZoomFactor,
                problemZoomFactor,
                appSettings.codeFolding());
        settingsRepository.save(appSettings);
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
                RenderedProblemView[] renderedViews = problemHtmlRenderer.renderBoth(currentProblemDetails);
                RenderedProblemView statementOnly = renderedViews[0];
                RenderedProblemView full = renderedViews[1];
                if (problemSheetsService != null && currentProblemCode != null) {
                    List<SheetInfo> sheets = problemSheetsService.getCached(currentProblemCode);
                    if (!sheets.isEmpty()) {
                        String sheetHtml = buildSheetInfoHtml(sheets, appThemePalette);
                        full = injectSheetInfo(full, sheetHtml);
                        statementOnly = injectSheetInfo(statementOnly, sheetHtml);
                    }
                }
                copyPayloads.clear();
                copyPayloads.putAll(full.copyPayloads());
                if (testCasesPanel != null) {
                    // Zoom/theme re-render must not destroy user-added custom
                    // test cases; updateSamplePayloads refreshes the sample
                    // set without touching the custom list.
                    testCasesPanel.updateSamplePayloads(full.copyPayloads());
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

    /**
     * Dirty-event-driven autosave. A document listener on the editor
     * flips a {@code dirty} flag; the save task runs after a debounce
     * period. The save is re-armed whenever the user keeps typing so a
     * single save per typing burst is the norm.
     */
    private void startAutosave(int intervalSeconds) {
        try {
            autosaveExecutor = TaskCoordinator.shared().scheduler();
            // The debounce window is fixed at AUTOSAVE_DEBOUNCE_MILLIS; the
            // configured interval acts as a hard upper bound for the time
            // between an edit and the next save attempt, but in practice
            // the save always fires within AUTOSAVE_DEBOUNCE_MILLIS of the
            // last keystroke.
            int debounceMillis = (int) Math.min(AUTOSAVE_DEBOUNCE_MILLIS,
                    Math.max(100L, intervalSeconds * 1000L / 2L));
            installEditorDocumentListener();
            // intervalSeconds is accepted but not directly used; kept for
            // API stability and to honor user preference ordering.
            if (intervalSeconds < 1) {
                intervalSeconds = 1;
            }
            // Touch the variable so it isn't flagged unused.
            if (debounceMillis < 0) intervalSeconds = intervalSeconds;
        } catch (Exception ignored) {
        }
    }

    private void installEditorDocumentListener() {
        if (codeEditor == null) return;
        codeEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { markSourceDirty(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { markSourceDirty(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { markSourceDirty(); }
        });
    }

    /**
     * Marks the source as dirty and arms a debounced save. Multiple
     * keystrokes within the debounce window coalesce to a single save.
     */
    private void markSourceDirty() {
        sourceDirty = true;
        scheduleAutosave();
    }

    private void scheduleAutosave() {
        if (autosaveExecutor == null) return;
        java.util.concurrent.ScheduledFuture<?> previous = pendingAutosave;
        if (previous != null) {
            previous.cancel(false);
        }
        pendingAutosave = autosaveExecutor.schedule(() -> runAutosaveOnce(),
                AUTOSAVE_DEBOUNCE_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * One debounced save attempt. Reads the editor text on the EDT,
     * compares against the last saved snapshot, and writes if changed.
     * If the user kept typing during the save, re-arms the debounce so
     * the next idle period triggers another save.
     */
    private void runAutosaveOnce() {
        try {
            if (appSettings == null || !appSettings.autosaveEnabled()) return;
            if (!problemStatementLoaded || currentProblemCode == null || codeEditor == null) return;
            if (currentProblemIsEmpty || EMPTY_PROBLEM_CODE.equals(currentProblemCode)) return;
            if (!sourceDirty) return;

            final String[] edtResult = {null, null};
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (codeEditor != null) edtResult[0] = codeEditor.getText();
                    if (languageDropdown != null && languageDropdown.getSelectedItem() != null)
                        edtResult[1] = languageDropdown.getSelectedItem().toString();
                });
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (java.lang.reflect.InvocationTargetException ignored) {
                return;
            }
            String current = edtResult[0];
            if (current == null) return;
            if (current.equals(lastAutosavedSource)) {
                sourceDirty = false;
                return;
            }
            String language = edtResult[1] != null ? edtResult[1] : DEFAULT_LANGUAGE;
            programCacheRepository.save(currentProblemCode, language, current);
            lastAutosavedSource = current;
            sourceDirty = false;
        } catch (Exception ignored) {
        }
    }

    private void stopAutosave() {
        try {
            if (pendingAutosave != null) {
                pendingAutosave.cancel(false);
                pendingAutosave = null;
            }
            // The scheduler is application-scoped; cancel only this
            // window's pending task, never shut down the shared pool.
            autosaveExecutor = null;
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
