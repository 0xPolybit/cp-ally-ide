package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.awt.Frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogShellTest {

    @Test
    void shellAppliesHeaderAndContentAndSupportsActions() {
        DialogShell shell = new DialogShell(null, "Dialog title", AppThemePalette.dark(), true);
        JTextArea body = new JTextArea("Body");
        shell.setContentAndHeader(body, "Dialog title", "Subtitle");
        shell.addAction("OK", () -> {});
        shell.addCancel();

        assertNotNull(shell.dialog().getContentPane());
        assertEquals("Dialog title", shell.dialog().getTitle());
        assertTrue(shell.dialog().getRootPane().getActionMap().size() > 0);
    }

    @Test
    void emptyDialogStillExposesHeader() {
        DialogShell shell = new DialogShell((Frame) null, "Header only", AppThemePalette.light(), false);
        shell.setHeader("Header only", "Hint text");

        assertNotNull(shell.dialog().getContentPane());
        assertEquals("Header only", shell.dialog().getTitle());
    }
}
