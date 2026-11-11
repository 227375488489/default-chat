package dev.defaultchat.chat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plays the mention sound. State is per player and bounded by the number of online players. */
public final class MentionPinger {

    private final Map<UUID, Long> lastPing = new HashMap<>();
    private final Key sound;
    private final float volume;
    private final float pitch;
    private final long cooldownMs;

    public MentionPinger(Key sound, float volume, float pitch, long cooldownMs) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.cooldownMs = cooldownMs;
    }

    public void ping(Player target) {
        long now = System.currentTimeMillis();
        Long last = lastPing.get(target.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastPing.put(target.getUniqueId(), now);
        target.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch));
    }

    public void forget(UUID id) {
        lastPing.remove(id);
    }
}
