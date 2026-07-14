package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiPrimitivesTest {

    @Test
    void statusBadgeUsesTextAndAccessibleState() {
        StatusBadge badge = new StatusBadge(
                "Ready", StatusBadge.Kind.SUCCESS, AppThemePalette.dark());

        assertEquals("Ready", badge.text());
        assertEquals("Ready status", badge.getAccessibleContext().getAccessibleName());
        assertNotNull(badge.getAccessibleContext().getAccessibleDescription());

        badge.setStatus("Unavailable", StatusBadge.Kind.ERROR);

        assertEquals("Unavailable", badge.text());
        assertEquals("Unavailable status", badge.getAccessibleContext().getAccessibleName());
    }

    @Test
    void noticesAndEmptyStatesCanExposeActions() {
        AtomicBoolean invoked = new AtomicBoolean();
        InlineNotice notice = new InlineNotice(
                "Connection problem", "Try again.", StatusBadge.Kind.ERROR, AppThemePalette.dark());
        notice.setAction("Retry", () -> invoked.set(true));
        EmptyStatePanel empty = new EmptyStatePanel(
                "No test cases", "Add input to begin.", AppThemePalette.dark());
        empty.setAction("Add", () -> invoked.set(true));

        assertTrue(notice.isVisible());
        assertTrue(empty.isVisible());
        assertEquals("Connection problem", notice.getAccessibleContext().getAccessibleName());
        assertEquals("No test cases", empty.getAccessibleContext().getAccessibleName());
        assertFalse(notice.getComponentCount() == 0);
        assertFalse(empty.getComponentCount() == 0);
    }

    @Test
    void buttonFactoryCreatesAccessibleActionButtons() {
        AtomicBoolean invoked = new AtomicBoolean();
        JButton button = UiComponents.primaryButton("Run", () -> invoked.set(true));

        assertEquals("Run", button.getText());
        assertEquals("Run", button.getAccessibleContext().getAccessibleName());
        button.doClick();
        assertTrue(invoked.get());
    }

    @Test
    void sectionHeaderHasAccessibleTitleAndTrailingComponentSlot() {
        SectionHeader header = new SectionHeader(
                "Execution Results", "1 passed, 0 failed", AppThemePalette.light());
        JButton action = new JButton("Close");

        header.setTrailingComponent(action);

        assertEquals("Execution Results", header.getAccessibleContext().getAccessibleName());
        assertTrue(header.getComponentCount() >= 2);
    }
}
