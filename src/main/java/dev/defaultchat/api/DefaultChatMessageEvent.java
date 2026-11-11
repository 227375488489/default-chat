package dev.defaultchat.api;

import dev.defaultchat.chat.ChatChannel;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

/**
 * Fired on the main (global region) thread right before a message is delivered. Listeners can cancel
 * the message, replace its component or change the viewer set. The viewer set is mutable.
 */
public final class DefaultChatMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ChatChannel channel;
    private final String plainMessage;
    private final Set<Audience> viewers;
    private Component message;
    private boolean cancelled;

    public DefaultChatMessageEvent(Player player, ChatChannel channel, Component message,
                                   String plainMessage, Set<Audience> viewers) {
        this.player = player;
        this.channel = channel;
        this.message = message;
        this.plainMessage = plainMessage;
        this.viewers = viewers;
    }

    public Player player() {
        return player;
    }

    public ChatChannel channel() {
        return channel;
    }

    public Component message() {
        return message;
    }

    public void message(Component message) {
        this.message = message;
    }

    /** Message without formatting, after filtering and masking. */
    public String plainMessage() {
        return plainMessage;
    }

    public Set<Audience> viewers() {
        return viewers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
