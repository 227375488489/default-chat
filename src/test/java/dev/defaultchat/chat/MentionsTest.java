package dev.defaultchat.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentionsTest {

    @Test
    void findsNamesCaseInsensitively() {
        Set<String> found = Mentions.find("Hello Steve, how are you?", List.of("Steve"), 3);
        assertEquals(Set.of("steve"), found);
    }

    @Test
    void supportsAtPrefix() {
        assertTrue(Mentions.find("@Steve hi", List.of("Steve"), 3).contains("steve"));
    }

    @Test
    void ignoresNamesInsideOtherWords() {
        assertTrue(Mentions.find("mysteve hi", List.of("Steve"), 3).isEmpty());
        assertTrue(Mentions.find("steve_ hi", List.of("Steve"), 3).isEmpty());
    }

    @Test
    void respectsMinimumLength() {
        assertTrue(Mentions.find("hi Al", List.of("Al"), 3).isEmpty());
        assertEquals(Set.of("al"), Mentions.find("hi Al", List.of("Al"), 1));
    }

    @Test
    void keepsMessageOrder() {
        Set<String> found = Mentions.find("bob then alice", List.of("Alice", "Bob"), 3);
        assertEquals(List.of("bob", "alice"), List.copyOf(found));
    }
}
