package com.example;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        // Initialize the logger BEFORE any networking so startup failures are captured.
        try {
            SettingsRepository earlySettings = new SettingsRepository(
                    "CompetitiveProgrammingAlly", "settings.properties", "Python 3");
            DiagnosticLogger.initialize(earlySettings.getAppDataDirectory());
        } catch (Exception ignored) {
            // Logger stays file-less; messages still go to stderr.
        }

        DiagnosticLogger.info("[App] Launched. args.length=" + args.length
                + (args.length > 0 ? ", args[0]=" + args[0] : ""));

        String pendingUrl = parseCpallyUrl(args);
        DiagnosticLogger.info("[App] pendingUrl=" + pendingUrl);

        InstanceServer instanceServer = new InstanceServer();
        DiagnosticLogger.info("[App] Calling tryBind...");
        boolean isPrimary = instanceServer.tryBind();
        DiagnosticLogger.info("[App] tryBind returned isPrimary=" + isPrimary);

        if (!isPrimary) {
            if (pendingUrl != null) {
                DiagnosticLogger.info("[App] Secondary instance. Forwarding URL: " + pendingUrl);
                boolean forwarded = instanceServer.tryForward(pendingUrl);
                DiagnosticLogger.info("[App] tryForward returned " + forwarded);
                if (!forwarded) {
                    // Primary may have just exited — try to become primary ourselves.
                    isPrimary = instanceServer.tryBind();
                    DiagnosticLogger.info("[App] Retry tryBind returned isPrimary=" + isPrimary);
                    if (!isPrimary) {
                        DiagnosticLogger.info("[App] Could not become primary. Exiting.");
                        System.exit(0);
                    }
                    // Fall through as primary below.
                } else {
                    DiagnosticLogger.info("[App] URL forwarded. Exiting secondary instance.");
                    System.exit(0);
                }
            } else {
                DiagnosticLogger.info("[App] Secondary instance with no URL. Exiting.");
                System.exit(0);
            }
        }

        DiagnosticLogger.info("[App] This is the primary instance. Starting UI.");
        Runtime.getRuntime().addShutdownHook(new Thread(instanceServer::close, "cpally-shutdown"));

        final InstanceServer primaryServer = instanceServer;
        final String urlToOpen = pendingUrl;

        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreenWindow = new SplashScreenWindow();
            splashScreenWindow.showSplash();

            Thread startupThread = new Thread(() -> {
                try {
                    DiagnosticLogger.info("[App] Startup thread running.");

                    MainWindow mainWindow = new MainWindow();

                    // Register IPC handler before showing the window so that any
                    // hot-handoff URL that arrives during startup is queued on the EDT
                    // and processed after showWindow() completes.
                    primaryServer.startListening(url -> mainWindow.openFromUrl(url));
                    DiagnosticLogger.info("[App] IPC listener started.");

                    // Keep the splash from flashing on very fast starts, but do
                    // not impose the former three-second startup delay.
                    splashScreenWindow.sleepUntilMinimumDuration(350L);
                    splashScreenWindow.closeSplash();
                    splashScreenWindow.sleepSilently(100L);

                    SwingUtilities.invokeLater(() -> {
                        mainWindow.showWindow();
                        if (urlToOpen != null) {
                            DiagnosticLogger.info("[App] Cold-launch URL: " + urlToOpen);
                            mainWindow.openFromUrl(urlToOpen);
                        }
                    });
                } catch (Throwable t) {
                    DiagnosticLogger.error("[App] Fatal exception during startup", t);
                }
            }, "main-window-startup");
            startupThread.start();
        });
    }

    private static String parseCpallyUrl(String[] args) {
        if (args == null || args.length == 0) return null;
        // Strip surrounding quotes that some shells or launchers may add.
        String arg = args[0].trim();
        if (arg.startsWith("\"") && arg.endsWith("\"") && arg.length() > 1) {
            arg = arg.substring(1, arg.length() - 1).trim();
        }
        return arg.toLowerCase().startsWith("cpally://") ? arg : null;
    }
}
