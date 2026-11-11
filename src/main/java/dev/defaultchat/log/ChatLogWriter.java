package dev.defaultchat.log;

import dev.defaultchat.DefaultChatPlugin;
import dev.defaultchat.chat.ChatChannel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Appends chat messages to a file on a single virtual thread. Writes are dropped when the queue is
 * saturated instead of growing without a bound or blocking the server thread.
 */
public final class ChatLogWriter implements AutoCloseable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PENDING = 512;

    private final DefaultChatPlugin plugin;
    private final boolean enabled;
    private final Path file;
    private final ExecutorService executor;
    private final AtomicInteger pending = new AtomicInteger();

    public ChatLogWriter(DefaultChatPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.config().logging().enabled();
        this.file = plugin.getDataFolder().toPath().resolve(plugin.config().logging().file());
        this.executor = enabled
                ? Executors.newSingleThreadExecutor(Thread.ofVirtual().name("DefaultChat-Log-", 0).factory())
                : null;
        if (enabled) {
            Path folder = file.getParent();
            if (folder != null) {
                try {
                    Files.createDirectories(folder);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not create the chat log folder: " + e.getMessage());
                }
            }
        }
    }

    public void log(String player, ChatChannel channel, String message) {
        if (!enabled || executor == null) {
            return;
        }
        if (pending.incrementAndGet() > MAX_PENDING) {
            pending.decrementAndGet();
            return;
        }
        String line = "[" + TIME.format(ZonedDateTime.now()) + "] [" + channel.key() + "] " + player + ": " + message
                + System.lineSeparator();
        executor.execute(() -> {
            try {
                Files.writeString(file, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not write to the chat log", e);
            } finally {
                pending.decrementAndGet();
            }
        });
    }

    @Override
    public void close() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
