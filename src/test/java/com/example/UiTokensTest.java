package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTokensTest {

    @Test
    void commonControlDimensionsUseTheSharedScale() {
        assertEquals(UiTokens.PROBLEM_FIELD_WIDTH, UiTokens.controlSize().width);
        assertEquals(UiTokens.CONTROL_HEIGHT, UiTokens.controlSize().height);
        assertEquals(UiTokens.ICON_NORMAL + UiTokens.SPACE_3,
                UiTokens.compactIconButtonSize().width);
        assertEquals(UiTokens.ICON_NORMAL + UiTokens.SPACE_2,
                UiTokens.compactIconButtonSize().height);
    }

    @Test
    void spacingAndControlValuesArePositive() {
        assertTrue(UiTokens.SPACE_1 < UiTokens.SPACE_2);
        assertTrue(UiTokens.SPACE_2 < UiTokens.SPACE_3);
        assertTrue(UiTokens.CONTROL_HEIGHT > 0);
        assertTrue(UiTokens.DIVIDER_SIZE > 0);
    }
}
