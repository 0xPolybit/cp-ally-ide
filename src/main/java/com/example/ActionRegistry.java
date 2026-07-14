package com.example;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared Swing actions for menus, toolbar controls, and keyboard shortcuts.
 * One action instance owns the label, accelerator, enabled state, and handler.
 */
final class ActionRegistry {

    enum Id {
        FETCH_PROBLEM,
        PREFERENCES,
        CLEAR_ALL_CACHE,
        CHOOSE_PROBLEM,
        OPEN_EMPTY_PROBLEM,
        REFRESH_PROBLEM,
        EXIT,
        RUN_CODE,
        ADD_TEST_CASE
    }

    private final Map<Id, Action> actions = new EnumMap<>(Id.class);

    Action register(Id id, String name, KeyStroke accelerator, Runnable handler) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(handler, "handler");

        AbstractAction action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
                handler.run();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, name);
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        actions.put(id, action);
        return action;
    }

    Action action(Id id) {
        Action action = actions.get(id);
        if (action == null) {
            throw new IllegalStateException("Action has not been registered: " + id);
        }
        return action;
    }

    void setEnabled(Id id, boolean enabled) {
        action(id).setEnabled(enabled);
    }

    boolean isEnabled(Id id) {
        return action(id).isEnabled();
    }
}
