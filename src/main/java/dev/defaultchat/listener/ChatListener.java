package dev.defaultchat.listener;

import dev.defaultchat.DefaultChatPlugin;
import dev.defaultchat.chat.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Captures the message on the netty thread and cancels the event, then delivers everything on the
 * main/global region thread where Bukkit state is safe to touch.
 */
public final class ChatListener implements Listener {

    private final DefaultChatPlugin plugin;
    private final ChatService chat;

    public ChatListener(DefaultChatPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        event.setCancelled(true);
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                chat.handle(player, plain, false);
            }
        });
    }
}
