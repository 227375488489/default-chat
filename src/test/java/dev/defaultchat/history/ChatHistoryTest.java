package dev.defaultchat.history;

import dev.defaultchat.chat.ChatChannel;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryTest {

    @Test
    void keepsOnlyTheNewestEntries() {
        ChatHistory history = new ChatHistory(3);
        for (int i = 1; i <= 4; i++) {
            history.add(entry(i));
        }

        List<ChatHistory.Entry> recent = history.recent(10);
        assertEquals(3, recent.size());
        assertEquals("player4", recent.get(2).player());
        assertEquals(2, history.recent(2).size());
        assertEquals("player4", history.recent(2).get(1).player());
    }

    @Test
    void disabledHistoryStoresNothing() {
        ChatHistory history = new ChatHistory(0);
        assertFalse(history.enabled());
        history.add(entry(1));
        assertTrue(history.isEmpty());
    }

    private static ChatHistory.Entry entry(int index) {
        return new ChatHistory.Entry(
                Instant.ofEpochSecond(index),
                ChatChannel.LOCAL,
                "player" + index,
                Component.text("message"),
                "message"
        );
    }
}
