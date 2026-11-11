package dev.defaultchat.history;

import dev.defaultchat.chat.ChatChannel;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Bounded in-memory ring buffer of the most recent chat messages. */
public final class ChatHistory {

    public record Entry(Instant time, ChatChannel channel, String player, Component message, String plain) {
    }

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public ChatHistory(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public synchronized void add(Entry entry) {
        if (capacity == 0) {
            return;
        }
        entries.addLast(entry);
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    public synchronized List<Entry> recent(int count) {
        int limit = Math.min(Math.max(1, count), entries.size());
        if (limit == 0) {
            return List.of();
        }
        List<Entry> all = new ArrayList<>(entries);
        return List.copyOf(all.subList(all.size() - limit, all.size()));
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean enabled() {
        return capacity > 0;
    }

    public synchronized void clear() {
        entries.clear();
    }
}
