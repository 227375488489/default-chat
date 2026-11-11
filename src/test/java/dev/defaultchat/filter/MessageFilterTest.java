package dev.defaultchat.filter;

import dev.defaultchat.config.PluginConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFilterTest {

    @Test
    void masksBlockedWordsCaseInsensitively() {
        MessageFilter filter = new MessageFilter(filters(List.of("badword"), FilterAction.MASK, false, List.of()));
        MessageFilter.Result result = filter.apply("you BadWord here");
        assertFalse(result.blocked());
        assertEquals("you **** here", result.text());
    }

    @Test
    void blocksTheWholeMessageWhenConfigured() {
        MessageFilter filter = new MessageFilter(filters(List.of("badword"), FilterAction.BLOCK, false, List.of()));
        MessageFilter.Result result = filter.apply("you badword here");
        assertTrue(result.blocked());
        assertEquals("you badword here", result.text());
    }

    @Test
    void blocksLinksUnlessAllowlisted() {
        MessageFilter filter = new MessageFilter(filters(List.of(), FilterAction.MASK, true, List.of("example.com")));
        assertTrue(filter.apply("visit https://spam.example.org").blocked());
        assertFalse(filter.apply("visit https://example.com/page").blocked());
        assertFalse(filter.apply("no links here").blocked());
    }

    @Test
    void detectsShouting() {
        assertTrue(MessageFilter.isShouting("HELLO WORLD", 6, 0.7));
        assertFalse(MessageFilter.isShouting("Hello World", 6, 0.7));
        assertFalse(MessageFilter.isShouting("HELLO", 6, 0.7));
    }

    private static PluginConfig.Filters filters(List<String> words, FilterAction action,
                                                boolean blockLinks, List<String> allowlist) {
        return new PluginConfig.Filters(words, "****", action, blockLinks, allowlist);
    }
}
