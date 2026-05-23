package com.example;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.awt.Image;
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

            ImageIcon icon = new ImageIcon(iconUrl);
            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                return null;
            }
            return icon;
        } catch (Exception ignored) {
            return null;
        }
    }

    static ImageIcon loadScaledClasspathIcon(String resourcePath, int width, int height) {
        ImageIcon icon = loadClasspathIcon(resourcePath);
        if (icon == null) {
            return null;
        }

        if (icon.getIconWidth() == width && icon.getIconHeight() == height) {
            return icon;
        }

        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
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