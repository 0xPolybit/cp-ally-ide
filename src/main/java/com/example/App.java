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

        // Single application-wide command queue. Any deep-link delivered to the
        // primary instance before the main window is ready is buffered here and
        // applied once the window reports readiness. This eliminates the race
        // where an IPC-arriving URL could call MainWindow.openFromUrl() while
        // the UI fields are still null.
        PendingCommands pendingCommands = new PendingCommands(16);
        if (pendingUrl != null) {
            pendingCommands.submit(pendingUrl);
        }

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
                        pendingCommands.discard();
                        DiagnosticLogger.info("[App] Could not become primary. Exiting.");
                        System.exit(0);
                    }
                    // Fall through as primary below.
                } else {
                    pendingCommands.discard();
                    DiagnosticLogger.info("[App] URL forwarded. Exiting secondary instance.");
                    System.exit(0);
                }
            } else {
                pendingCommands.discard();
                DiagnosticLogger.info("[App] Secondary instance with no URL. Exiting.");
                System.exit(0);
            }
        }

        DiagnosticLogger.info("[App] This is the primary instance. Starting UI.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            instanceServer.close();
            TaskCoordinator.shared().shutdownNow();
        }, "cpally-shutdown"));

        final InstanceServer primaryServer = instanceServer;
        final PendingCommands pending = pendingCommands;

        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreenWindow = new SplashScreenWindow();
            splashScreenWindow.showSplash();

            Thread startupThread = new Thread(() -> {
                try {
                    DiagnosticLogger.info("[App] Startup thread running.");

                    MainWindow mainWindow = new MainWindow();

                    // Register IPC handler. Any URL that arrives while the window
                    // is still initializing is queued on the EDT and delivered
                    // only after the window signals readiness, eliminating the
                    // pre-showWindow race.
                    primaryServer.startListening(pending::submit);
                    DiagnosticLogger.info("[App] IPC listener started.");

                    splashScreenWindow.sleepUntilMinimumDuration(3000L);
                    splashScreenWindow.closeSplash();
                    splashScreenWindow.sleepSilently(500L);

                    SwingUtilities.invokeLater(() -> {
                        try {
                            mainWindow.showWindow();
                            mainWindow.markReady();
                            java.util.Map<String, String> drained = pending.markReady();
                            if (!drained.isEmpty()) {
                                DiagnosticLogger.info("[App] Draining " + drained.size()
                                        + " queued command(s) after readiness.");
                                // The queue is keyed by problem code and the values
                                // are the original commands; deliver each in the
                                // submission order. openFromUrl is idempotent for
                                // repeated URLs of the same problem.
                                drained.values().forEach(mainWindow::openFromUrl);
                            }
                        } catch (Throwable t) {
                            DiagnosticLogger.error("[App] Fatal exception during UI startup", t);
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
