package dev.defaultchat.listener;

import dev.defaultchat.chat.ChatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final ChatService chat;

    public PlayerListener(ChatService chat) {
        this.chat = chat;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chat.forget(event.getPlayer().getUniqueId());
    }
}
