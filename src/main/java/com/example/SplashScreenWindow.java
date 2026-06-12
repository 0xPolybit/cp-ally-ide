package com.example;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

final class SplashScreenWindow {

    private final JWindow window;
    private final long startedAtMillis;

    SplashScreenWindow() {
        startedAtMillis = System.currentTimeMillis();
        window = new JWindow();
        window.setBackground(new Color(0, 0, 0, 0));
        window.setAlwaysOnTop(true);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder());

        ImageIcon logoIcon = loadLogoIcon();
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);
        content.add(logoLabel, BorderLayout.CENTER);

        window.setContentPane(content);
        Dimension targetSize = getTargetSize(logoIcon);
        window.setSize(targetSize);
        centerWindowOnScreen(targetSize);
    }

    void showSplash() {
        window.setAlwaysOnTop(true);
        window.setVisible(true);
        window.toFront();
        window.requestFocus();
    }

    void closeSplash() {
        Runnable closeAction = () -> {
            if (window.isDisplayable()) {
                window.setVisible(false);
                window.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            closeAction.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(closeAction);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ignored) {
            // Ignore close failures so startup can continue.
        }
    }

    void sleepUntilMinimumDuration(long minimumDurationMillis) {
        long elapsedMillis = System.currentTimeMillis() - startedAtMillis;
        long remainingMillis = minimumDurationMillis - elapsedMillis;
        sleepSilently(Math.max(0L, remainingMillis));
    }

    void sleepSilently(long delayMillis) {
        if (delayMillis <= 0L) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private ImageIcon loadLogoIcon() {
        java.net.URL logoUrl = SplashScreenWindow.class.getResource("/assets/logo.png");
        if (logoUrl == null) {
            Path logoPath = Path.of("assets", "logo.png");
            if (!Files.exists(logoPath)) {
                return new ImageIcon();
            }
            try {
                logoUrl = logoPath.toUri().toURL();
            } catch (Exception ignored) {
                return new ImageIcon();
            }
        }

        try {
            BufferedImage logoImage = ImageIO.read(logoUrl);
            if (logoImage == null) {
                return new ImageIcon();
            }

            Dimension targetSize = getTargetSize(logoImage.getWidth(), logoImage.getHeight());
            BufferedImage scaled = new BufferedImage(targetSize.width, targetSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, targetSize.width, targetSize.height);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(logoImage, 0, 0, targetSize.width, targetSize.height, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (IOException ignored) {
            return new ImageIcon();
        }
    }

    private Dimension getTargetSize(ImageIcon logoIcon) {
        int iconWidth = Math.max(1, logoIcon.getIconWidth());
        int iconHeight = Math.max(1, logoIcon.getIconHeight());
        return getTargetSize(iconWidth, iconHeight);
    }

    private Dimension getTargetSize(int originalWidth, int originalHeight) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int targetHeight = Math.max(1, (int) Math.round(screenSize.height * 0.25d));
        double scale = targetHeight / (double) Math.max(1, originalHeight);
        int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        return new Dimension(targetWidth, targetHeight);
    }

    private void centerWindowOnScreen(Dimension size) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = Math.max(0, (screenSize.width - size.width) / 2);
        int y = Math.max(0, (screenSize.height - size.height) / 2);
        window.setLocation(x, y);
    }
}