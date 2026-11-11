package dev.defaultchat.config;

import dev.defaultchat.chat.ChatChannel;
import dev.defaultchat.filter.CapsAction;
import dev.defaultchat.filter.FilterAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Immutable snapshot of config.yml. A new snapshot is created on every reload, so services never
 * observe a half-updated configuration.
 */
public record PluginConfig(
        String language,
        ChatChannel defaultChannel,
        int maxLength,
        boolean playerMiniMessage,
        boolean clickableNames,
        Local local,
        Global global,
        Spam spam,
        Caps caps,
        Mentions mentions,
        Filters filters,
        History history,
        Logging logging,
        boolean respectVanish,
        int saveIntervalSeconds
) {

    private static final Pattern LANGUAGE = Pattern.compile("[a-z0-9_-]{2,16}");
    private static final Key DEFAULT_SOUND = Key.key("minecraft", "entity.experience_orb.pickup");

    public record Local(boolean enabled, int radius, Map<String, Integer> worldRadius) {
        public int radiusFor(String world) {
            return worldRadius.getOrDefault(world, radius);
        }
    }

    public record Global(String trigger, boolean requirePermission) {
    }

    public record Spam(long cooldownMs, long duplicateWindowMs, long rateWindowMs, int rateMessages) {
    }

    public record Caps(int minLength, double maxRatio, CapsAction action) {
    }

    public record Mentions(boolean enabled, int minLength, Key sound, float volume, float pitch,
                           long cooldownMs, Style highlight) {
    }

    public record Filters(List<String> words, String mask, FilterAction wordAction,
                          boolean blockLinks, List<String> linkAllowlist) {
    }

    public record History(boolean enabled, int size) {
    }

    public record Logging(boolean enabled, String file) {
    }

    public static PluginConfig load(FileConfiguration config) {
        String language = config.getString("language", "en").toLowerCase(Locale.ROOT);
        if (!LANGUAGE.matcher(language).matches()) {
            throw new IllegalArgumentException("language must be a language code such as 'en' or 'ru'");
        }

        ChatChannel defaultChannel = ChatChannel.parse(config.getString("chat.default-channel", "local"));
        int maxLength = Math.clamp(config.getInt("chat.max-length", 256), 16, 2000);

        Map<String, Integer> worldRadius = new HashMap<>();
        ConfigurationSection radiusSection = config.getConfigurationSection("chat.local.world-radius");
        if (radiusSection != null) {
            for (String world : radiusSection.getKeys(false)) {
                worldRadius.put(world.toLowerCase(Locale.ROOT), radiusSection.getInt(world, config.getInt("chat.local.radius", 100)));
            }
        }
        Local local = new Local(
                config.getBoolean("chat.local.enabled", true),
                config.getInt("chat.local.radius", 100),
                Map.copyOf(worldRadius)
        );

        Global global = new Global(
                config.getString("chat.global.trigger", "!"),
                config.getBoolean("chat.global.require-permission", false)
        );

        Spam spam = new Spam(
                Math.max(0L, config.getLong("chat.cooldown-ms", 750L)),
                Math.max(0L, config.getLong("chat.duplicate-window-seconds", 30L)) * 1000L,
                Math.max(0L, config.getLong("chat.rate-limit.seconds", 5L)) * 1000L,
                Math.max(0, config.getInt("chat.rate-limit.messages", 5))
        );

        Caps caps = new Caps(
                Math.max(0, config.getInt("chat.caps.min-length", 6)),
                Math.clamp(config.getDouble("chat.caps.max-ratio", 0.7), 0.0D, 1.0D),
                CapsAction.parse(config.getString("chat.caps.action", "BLOCK"))
        );

        Mentions mentions = new Mentions(
                config.getBoolean("mentions.enabled", true),
                Math.max(1, config.getInt("mentions.min-length", 3)),
                soundKey(config.getString("mentions.sound", DEFAULT_SOUND.asString())),
                (float) Math.clamp(config.getDouble("mentions.volume", 0.6), 0.0D, 4.0D),
                (float) Math.clamp(config.getDouble("mentions.pitch", 1.4), 0.5D, 2.0D),
                Math.max(0L, config.getLong("mentions.cooldown-seconds", 2L)) * 1000L,
                highlightStyle(config.getString("mentions.highlight", "<#FFD166><bold>"))
        );

        Filters filters = new Filters(
                lower(config.getStringList("filters.words")),
                config.getString("filters.word-mask", "****"),
                FilterAction.parse(config.getString("filters.word-action", "MASK")),
                config.getBoolean("filters.block-links", false),
                lower(config.getStringList("filters.link-allowlist"))
        );

        History history = new History(
                config.getBoolean("history.enabled", true),
                Math.clamp(config.getInt("history.size", 100), 0, 1000)
        );

        Logging logging = new Logging(
                config.getBoolean("logging.enabled", true),
                logFile(config.getString("logging.file", "chat.log"))
        );

        return new PluginConfig(
                language,
                defaultChannel,
                maxLength,
                config.getBoolean("chat.player-minimessage", true),
                config.getBoolean("chat.clickable-names", true),
                local,
                global,
                spam,
                caps,
                mentions,
                filters,
                history,
                logging,
                config.getBoolean("privacy.respect-vanish", true),
                Math.clamp(config.getInt("storage.save-interval-seconds", 300), 30, 3600)
        );
    }

    private static List<String> lower(List<String> values) {
        List<String> out = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(out);
    }

    private static Key soundKey(String raw) {
        try {
            return Key.key(raw);
        } catch (RuntimeException e) {
            return DEFAULT_SOUND;
        }
    }

    private static Style highlightStyle(String raw) {
        try {
            Style style = MiniMessage.miniMessage().deserialize(raw).style();
            if (style.color() != null || !style.decorations().isEmpty()) {
                return style;
            }
        } catch (RuntimeException ignored) {
            // Fall through to the built-in highlight so a broken value never disables chat.
        }
        return Style.style(TextColor.color(0xFFD166), TextDecoration.BOLD);
    }

    private static String logFile(String raw) {
        try {
            Path path = Path.of(raw);
            if (path.isAbsolute() || path.normalize().startsWith("..")) {
                return "chat.log";
            }
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            return "chat.log";
        }
    }
}
