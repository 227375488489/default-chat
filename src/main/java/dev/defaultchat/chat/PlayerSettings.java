package dev.defaultchat.chat;

import java.util.Objects;
import java.util.UUID;

/** Per-player preferences, loaded lazily and persisted in playerdata.yml. */
public final class PlayerSettings {

    private final UUID id;
    private boolean mentionPings = true;
    private ChatChannel channel;

    public PlayerSettings(UUID id, ChatChannel channel) {
        this.id = Objects.requireNonNull(id, "id");
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public UUID id() {
        return id;
    }

    public boolean mentionPings() {
        return mentionPings;
    }

    public void mentionPings(boolean value) {
        this.mentionPings = value;
    }

    public ChatChannel channel() {
        return channel;
    }

    public void channel(ChatChannel value) {
        this.channel = Objects.requireNonNull(value, "channel");
    }
}
