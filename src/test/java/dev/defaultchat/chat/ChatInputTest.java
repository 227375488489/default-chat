package dev.defaultchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatInputTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void parsesSafeFormatting() {
        Component component = ChatInput.parse("<red>hello", true);
        assertEquals("hello", plain(component));
        assertEquals(NamedTextColor.RED, component.style().color());
    }

    @Test
    void supportsSprites() {
        String raw = "<gray>hi <sprite:\"minecraft:blocks/stone\">";
        Component component = ChatInput.parse(raw, true);
        assertNotEquals(raw, plain(component));
        assertFalse(component.style().clickEvent() != null);
    }

    @Test
    void rejectsInteractiveTags() {
        String raw = "<click:run_command:'/op steve'>click me</click>";
        Component component = ChatInput.parse(raw, true);
        assertEquals(raw, plain(component));
        assertNull(component.style().clickEvent());
    }

    @Test
    void fallsBackToLiteralTextWhenFormattingIsDisabled() {
        assertEquals("<red>hello", plain(ChatInput.parse("<red>hello", false)));
        assertEquals("<red>hello", ChatInput.plain("<red>hello", false));
        assertEquals("hello", ChatInput.plain("<red>hello", true));
    }
}
