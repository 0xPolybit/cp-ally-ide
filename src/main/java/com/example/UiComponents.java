package com.example;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;

/** Shared button construction and accessible action styling. */
final class UiComponents {

    private UiComponents() {
    }

    static JButton primaryButton(String text, Runnable action) {
        return createButton(text, action, "default");
    }

    static JButton secondaryButton(String text, Runnable action) {
        return createButton(text, action, "button");
    }

    static JButton quietButton(String text, Runnable action) {
        return createButton(text, action, "borderless");
    }

    static JButton iconButton(String accessibleName, Runnable action) {
        JButton button = createButton("", action, "toolBarButton");
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        return button;
    }

    private static JButton createButton(String text, Runnable action, String buttonType) {
        JButton button = new JButton(text == null ? "" : text);
        button.putClientProperty("JButton.buttonType", buttonType);
        button.setFocusable(true);
        button.setRequestFocusEnabled(true);
        if (action != null) {
            button.addActionListener((ActionEvent ignored) -> action.run());
        }
        if (text != null && !text.isBlank()) {
            button.getAccessibleContext().setAccessibleName(text);
        }
        return button;
    }
}
