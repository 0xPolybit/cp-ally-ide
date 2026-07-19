package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageDefinitionTest {
    @Test
    void registryHasStableDefinitions() {
        assertTrue(LanguageDefinition.all().size() >= 15);
        assertNotNull(LanguageDefinition.forId("python3"));
        assertNotNull(LanguageDefinition.forId("cpp17"));
    }

    @Test
    void displayNamesResolveToStableDefinitions() {
        LanguageDefinition c = LanguageDefinition.forDisplayName("GNU C11 5.1.0");
        assertEquals("c11", c.id());
        assertEquals("Main.c", c.sourceFileName());
        assertEquals("text/c", c.syntaxStyle());
    }

    @Test
    void legacyG11MapsToC11() {
        assertEquals("c11", LanguageDefinition.forDisplayName("GNU G11 5.1.0").id());
    }

    @Test
    void templatesAreNonEmpty() {
        for (LanguageDefinition definition : LanguageDefinition.all()) {
            assertTrue(definition.boilerplate() != null && !definition.boilerplate().isBlank(), definition.id());
            assertNotNull(definition.syntaxStyle(), definition.id());
        }
    }
}
