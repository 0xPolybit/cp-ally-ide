package com.example;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;

final class UiIconLoader {

    private UiIconLoader() {
    }

    static ImageIcon loadClasspathIcon(String resourcePath) {
        try {
            URL iconUrl = UiIconLoader.class.getResource(resourcePath);
            if (iconUrl == null) {
                return null;
            }
            BufferedImage img = ImageIO.read(iconUrl);
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
                return null;
            }
            return new ImageIcon(img);
        } catch (Exception ignored) {
            return null;
        }
    }

    static ImageIcon loadScaledClasspathIcon(String resourcePath, int width, int height) {
        try {
            URL iconUrl = UiIconLoader.class.getResource(resourcePath);
            if (iconUrl == null) {
                return null;
            }
            BufferedImage source = ImageIO.read(iconUrl);
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                return null;
            }
            if (source.getWidth() == width && source.getHeight() == height) {
                return new ImageIcon(source);
            }
            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = target.createGraphics();
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, width, height);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(source, 0, 0, width, height, null);
            g2.dispose();
            return new ImageIcon(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    static ImageIcon loadThemedClasspathIcon(String assetBaseName, AppThemePalette theme) {
        return loadThemedClasspathIcon(assetBaseName, theme, -1, -1);
    }

    static ImageIcon loadThemedClasspathIcon(String assetBaseName, AppThemePalette theme, int width, int height) {
        String resourcePath = themedAssetPath(assetBaseName, theme);
        if (width > 0 && height > 0) {
            return loadScaledClasspathIcon(resourcePath, width, height);
        }
        return loadClasspathIcon(resourcePath);
    }

    static String themedAssetPath(String assetBaseName, AppThemePalette theme) {
        String normalized = assetBaseName == null ? "" : assetBaseName.trim();
        if (normalized.isEmpty()) {
            return "/assets/";
        }

        String name = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext = dot >= 0 ? name.substring(dot) : ".png";
        boolean lightTheme = theme != null && theme.lightTheme();
        return "/assets/" + base + (lightTheme ? "-light" : "-dark") + ext;
    }

    static void applyWindowIcon(JFrame frame, String resourcePath) {
        if (frame == null) {
            return;
        }

        ImageIcon icon = loadClasspathIcon(resourcePath);
        if (icon != null) {
            frame.setIconImage(icon.getImage());
        }
    }
}