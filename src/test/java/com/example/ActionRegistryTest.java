package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionRegistryTest {

    @Test
    void oneActionCarriesNameAcceleratorEnabledStateAndHandler() {
        ActionRegistry registry = new ActionRegistry();
        AtomicBoolean invoked = new AtomicBoolean();
        KeyStroke shortcut = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK);

        Action action = registry.register(
                ActionRegistry.Id.RUN_CODE, "Run Code", shortcut, () -> invoked.set(true));

        assertEquals("Run Code", action.getValue(Action.NAME));
        assertEquals(shortcut, action.getValue(Action.ACCELERATOR_KEY));
        assertTrue(action.isEnabled());

        action.actionPerformed(null);

        assertTrue(invoked.get());
    }

    @Test
    void enabledStateIsSharedByEveryConsumerOfTheAction() {
        ActionRegistry registry = new ActionRegistry();
        registry.register(ActionRegistry.Id.ADD_TEST_CASE, "Add Test Case", null, () -> {});

        registry.setEnabled(ActionRegistry.Id.ADD_TEST_CASE, false);

        assertFalse(registry.isEnabled(ActionRegistry.Id.ADD_TEST_CASE));
        assertFalse(registry.action(ActionRegistry.Id.ADD_TEST_CASE).isEnabled());
    }
}
