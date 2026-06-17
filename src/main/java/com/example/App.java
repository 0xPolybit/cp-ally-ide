package com.example;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        String pendingUrl = parseCpallyUrl(args);

        InstanceServer instanceServer = new InstanceServer();
        boolean isPrimary = instanceServer.tryBind();

        if (!isPrimary) {
            if (pendingUrl != null) {
                boolean forwarded = instanceServer.tryForward(pendingUrl);
                if (!forwarded) {
                    // Primary may have just exited — try to become primary ourselves.
                    isPrimary = instanceServer.tryBind();
                    if (!isPrimary) {
                        // Another race; give up and exit.
                        System.exit(0);
                    }
                    // Fall through as primary below.
                } else {
                    System.exit(0);
                }
            } else {
                // No URL to forward; don't steal focus from the running instance.
                System.exit(0);
            }
        }

        final InstanceServer primaryServer = instanceServer;
        final String urlToOpen = pendingUrl;

        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreenWindow = new SplashScreenWindow();
            splashScreenWindow.showSplash();

            Thread startupThread = new Thread(() -> {
                try {
                    SettingsRepository tempSettings = new SettingsRepository("CompetitiveProgrammingAlly", "settings.properties", "Python 3");
                    DiagnosticLogger.initialize(tempSettings.getAppDataDirectory());
                    DiagnosticLogger.info("App starting up...");

                    MainWindow mainWindow = new MainWindow();

                    // Register IPC handler before showing the window so that any
                    // hot-handoff URL that arrives during startup is queued on the EDT
                    // and processed after showWindow() completes.
                    primaryServer.startListening(url -> mainWindow.openFromUrl(url));

                    splashScreenWindow.sleepUntilMinimumDuration(3000L);
                    splashScreenWindow.closeSplash();
                    splashScreenWindow.sleepSilently(500L);

                    SwingUtilities.invokeLater(() -> {
                        mainWindow.showWindow();
                        if (urlToOpen != null) {
                            mainWindow.openFromUrl(urlToOpen);
                        }
                    });
                } catch (Throwable t) {
                    DiagnosticLogger.error("Fatal exception during startup", t);
                }
            }, "main-window-startup");
            startupThread.start();
        });
    }

    private static String parseCpallyUrl(String[] args) {
        if (args == null || args.length == 0) return null;
        String arg = args[0].trim();
        return arg.toLowerCase().startsWith("cpally://") ? arg : null;
    }
}
