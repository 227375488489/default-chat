package dev.defaultchat.chat;

import dev.defaultchat.DefaultChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Keeps player preferences in memory and writes playerdata.yml off the main thread. Values are
 * loaded once per session; quitting players are dropped from memory so the cache stays small.
 */
public final class PlayerSettingsStore {

    private final DefaultChatPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();
    private final YamlConfiguration data = new YamlConfiguration();

    public PlayerSettingsStore(DefaultChatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (file.exists()) {
            try {
                data.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getLogger().warning("Could not read playerdata.yml: " + e.getMessage());
            }
        }
    }

    public PlayerSettings get(UUID id) {
        return cache.computeIfAbsent(id, key -> {
            PlayerSettings settings = new PlayerSettings(key, plugin.config().defaultChannel());
            String base = "players." + key;
            if (data.isConfigurationSection(base)) {
                settings.mentionPings(data.getBoolean(base + ".mentions", true));
                settings.channel(ChatChannel.parse(data.getString(base + ".channel", plugin.config().defaultChannel().key())));
            }
            return settings;
        });
    }

    public void persist(PlayerSettings settings) {
        String base = "players." + settings.id();
        data.set(base + ".mentions", settings.mentionPings());
        data.set(base + ".channel", settings.channel().key());
    }

    public void saveAsync() {
        if (!data.isConfigurationSection("players")) {
            return;
        }
        String snapshot = data.saveToString();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> write(snapshot));
    }

    public void saveNow() {
        if (!data.isConfigurationSection("players")) {
            return;
        }
        write(data.saveToString());
    }

    public void unload(UUID id) {
        cache.remove(id);
    }

    private void write(String content) {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                return;
            }
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml: " + e.getMessage());
        }
    }
}
