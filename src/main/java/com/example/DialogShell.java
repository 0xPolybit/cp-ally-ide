package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Reusable modal/modeless dialog shell with consistent layout and behavior. */
final class DialogShell {

    private final JDialog dialog;
    private final JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.SPACE_2, 0));
    private final AppThemePalette palette;
    private JButton defaultButton;

    DialogShell(java.awt.Frame owner, String title, AppThemePalette palette, boolean modal) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        dialog = new JDialog(owner, title, modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
        dialog.getContentPane().setBackground(this.palette.frameBackground());
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        installEscapeBehavior();
    }

    void setContent(JComponent body) {
        dialog.getContentPane().removeAll();
        dialog.getContentPane().add(body, BorderLayout.CENTER);
        dialog.getContentPane().add(buildFooterPanel(), BorderLayout.SOUTH);
        dialog.getContentPane().revalidate();
        dialog.getContentPane().repaint();
    }

    void setHeader(String title, String subtitle) {
        dialog.setTitle(title);
        dialog.getContentPane().removeAll();
        SectionHeader header = new SectionHeader(title, subtitle, palette);
        header.setOpaque(true);
        header.setBackground(palette.panelBackground());
        dialog.getContentPane().add(header, BorderLayout.NORTH);
        dialog.getContentPane().add(buildFooterPanel(), BorderLayout.SOUTH);
        dialog.getContentPane().revalidate();
        dialog.getContentPane().repaint();
    }

    void setContentAndHeader(JComponent body, String title, String subtitle) {
        dialog.getContentPane().removeAll();
        SectionHeader header = new SectionHeader(title, subtitle, palette);
        header.setOpaque(true);
        header.setBackground(palette.panelBackground());
        dialog.getContentPane().add(header, BorderLayout.NORTH);
        JPanel bodyHolder = new JPanel(new BorderLayout());
        bodyHolder.setBackground(palette.frameBackground());
        bodyHolder.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3));
        bodyHolder.add(body, BorderLayout.CENTER);
        dialog.getContentPane().add(bodyHolder, BorderLayout.CENTER);
        dialog.getContentPane().add(buildFooterPanel(), BorderLayout.SOUTH);
        dialog.getContentPane().revalidate();
        dialog.getContentPane().repaint();
    }

    void addAction(String label, Runnable action) {
        JButton button = UiComponents.secondaryButton(label, action);
        footerPanel.add(button);
        dialog.getRootPane().setDefaultButton(button);
        defaultButton = button;
        revalidateDialog();
    }

    void addCancel() {
        JButton button = UiComponents.quietButton("Close", () -> dialog.dispose());
        button.getAccessibleContext().setAccessibleName("Close dialog");
        footerPanel.add(button);
        revalidateDialog();
    }

    void setSize(int width, int height) {
        dialog.setSize(width, height);
        ensureOnScreen();
    }

    void setMinimumSize(Dimension size) {
        dialog.setMinimumSize(size);
    }

    void setVisible(boolean visible) {
        if (visible) {
            ensureOnScreen();
        }
        dialog.setVisible(visible);
    }

    void dispose() {
        dialog.dispose();
    }

    JDialog dialog() {
        return dialog;
    }

    private JPanel buildFooterPanel() {
        footerPanel.setOpaque(true);
        footerPanel.setBackground(palette.frameBackground());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_2, UiTokens.SPACE_3, UiTokens.SPACE_2, UiTokens.SPACE_3));
        return footerPanel;
    }

    private void revalidateDialog() {
        footerPanel.revalidate();
        footerPanel.repaint();
        dialog.getContentPane().revalidate();
        dialog.getContentPane().repaint();
    }

    private void installEscapeBehavior() {
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                if (defaultButton != null) {
                    defaultButton.requestFocusInWindow();
                }
            }
        });
    }

    private void ensureOnScreen() {
        java.awt.Rectangle bounds = dialog.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }
        java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (bounds.x + bounds.width > screen.width || bounds.y + bounds.height > screen.height
                || bounds.x < 0 || bounds.y < 0) {
            dialog.setLocationRelativeTo(getOwner(dialog));
        }
    }

    private static Component getOwner(JDialog dialog) {
        try {
            return dialog.getOwner();
        } catch (Exception ignored) {
            return null;
        }
    }
}
