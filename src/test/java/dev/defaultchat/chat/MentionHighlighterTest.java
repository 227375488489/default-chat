package dev.defaultchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentionHighlighterTest {

    private static final Style HIGHLIGHT = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);

    @Test
    void highlightsEveryMention() {
        Component input = Component.text("hello steve and steve");
        Component output = MentionHighlighter.highlight(input, "steve"::equals, HIGHLIGHT);

        assertEquals("hello steve and steve", plain(output));
        assertEquals("stevesteve", highlightedText(output));
    }

    @Test
    void keepsExistingColorsWhenNotMentioned() {
        Component input = MiniMessage.miniMessage().deserialize("<gradient:#00ff88:#00aaff>hey steve</gradient>");
        Component output = MentionHighlighter.highlight(input, "steve"::equals, HIGHLIGHT);

        assertEquals("hey steve", plain(output));
        assertEquals("steve", highlightedText(output));
    }

    @Test
    void ignoresPartialMatches() {
        Component input = Component.text("mysteve");
        Component output = MentionHighlighter.highlight(input, "steve"::equals, HIGHLIGHT);
        assertEquals("mysteve", plain(output));
        assertEquals("", highlightedText(output));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String highlightedText(Component component) {
        StringBuilder builder = new StringBuilder();
        for (Component each : flatten(component)) {
            if (each instanceof TextComponent text
                    && text.style().color() == NamedTextColor.GOLD
                    && text.style().decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE) {
                builder.append(text.content());
            }
        }
        return builder.toString();
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        out.add(component);
        for (Component child : component.children()) {
            out.addAll(flatten(child));
        }
        return out;
    }
}
