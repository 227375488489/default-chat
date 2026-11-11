package dev.defaultchat.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MiniMessage templates loaded from lang/&lt;language&gt;.yml. English is always loaded first, so a
 * translation can be partial without breaking anything.
 */
public final class Messages {

    private final JavaPlugin plugin;
    private final MiniMessage parser = MiniMessage.miniMessage();
    private final Map<String, String> values;
    private final Component prefix;
    private final Set<String> warned = new HashSet<>();

    private Messages(JavaPlugin plugin, Map<String, String> values) {
        this.plugin = plugin;
        this.values = values;
        this.prefix = parseRaw(values.getOrDefault("prefix", ""));
    }

    public static Messages load(JavaPlugin plugin, String language) {
        extract(plugin, "en");
        extract(plugin, "ru");
        extract(plugin, language);
        Map<String, String> defaults = read(plugin, "en");
        if (defaults.isEmpty()) {
            throw new IllegalStateException("lang/en.yml is missing from the plugin jar");
        }
        Map<String, String> values = new HashMap<>(defaults);
        if (!"en".equals(language)) {
            values.putAll(read(plugin, language));
        }
        return new Messages(plugin, Map.copyOf(values));
    }

    /** Copies a bundled language file into the data folder so it can be edited. */
    private static void extract(JavaPlugin plugin, String language) {
        File target = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (target.isFile()) {
            return;
        }
        try {
            plugin.saveResource("lang/" + language + ".yml", false);
        } catch (IllegalArgumentException ignored) {
            // No such bundled language; the caller falls back to English.
        }
    }

    private static Map<String, String> read(JavaPlugin plugin, String language) {
        String resource = "lang/" + language + ".yml";
        String content = content(plugin, resource);
        if (content == null) {
            return Map.of();
        }
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(content);
            Map<String, String> out = new HashMap<>();
            for (String key : yaml.getKeys(true)) {
                if (yaml.isString(key)) {
                    out.put(key, yaml.getString(key, ""));
                }
            }
            return out;
        } catch (InvalidConfigurationException e) {
            throw new IllegalStateException("Could not read " + resource, e);
        }
    }

    private static String content(JavaPlugin plugin, String resource) {
        Path file = plugin.getDataFolder().toPath().resolve(resource);
        if (Files.isRegularFile(file)) {
            try {
                return Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + file, e);
            }
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + resource + " from the jar", e);
        }
    }

    /** Raw MiniMessage template, or null when the key is unknown. */
    public String raw(String key) {
        return values.get(key);
    }

    public Component parse(String key, TagResolver... resolvers) {
        String raw = values.get(key);
        if (raw == null) {
            return Component.text(key);
        }
        return parseRaw(raw, resolvers);
    }

    public Component parseRaw(String raw, TagResolver... resolvers) {
        try {
            return parser.deserialize(raw, resolvers);
        } catch (RuntimeException e) {
            warnOnce(raw, e);
            return Component.text(raw);
        }
    }

    public Component prefix() {
        return prefix;
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(prefix.append(parse(key, resolvers)));
    }

    private void warnOnce(String raw, RuntimeException error) {
        if (warned.add(raw)) {
            plugin.getLogger().warning("Invalid MiniMessage template [" + raw + "]: " + error.getMessage());
        }
    }
}
