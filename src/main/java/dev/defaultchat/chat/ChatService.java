package dev.defaultchat.chat;

import dev.defaultchat.DefaultChatPlugin;
import dev.defaultchat.api.DefaultChatMessageEvent;
import dev.defaultchat.config.Messages;
import dev.defaultchat.config.PluginConfig;
import dev.defaultchat.filter.CapsAction;
import dev.defaultchat.filter.MessageFilter;
import dev.defaultchat.filter.SpamGuard;
import dev.defaultchat.history.ChatHistory;
import dev.defaultchat.log.ChatLogWriter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * All chat delivery runs on the main (global region) thread: filters, recipients, history, sounds and
 * the API event. The only asynchronous parts are the chat log writer and playerdata persistence.
 */
public final class ChatService {

    private final DefaultChatPlugin plugin;

    private ChatHistory history;
    private ChatLogWriter logWriter;
    private PlayerSettingsStore settings;
    private SpamGuard spamGuard;
    private MessageFilter filter;
    private MentionPinger pinger;
    private ChatFormatter formatter;

    public ChatService(DefaultChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.history = new ChatHistory(plugin.config().history().size());
        this.settings = new PlayerSettingsStore(plugin);
        rebuild();
    }

    public void reload() {
        rebuild();
    }

    public void shutdown() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
        if (settings != null) {
            settings.saveNow();
        }
        if (history != null) {
            history.clear();
        }
    }

    private void rebuild() {
        PluginConfig config = plugin.config();
        if (logWriter != null) {
            logWriter.close();
        }
        this.logWriter = new ChatLogWriter(plugin);
        this.spamGuard = new SpamGuard(config.spam());
        this.filter = new MessageFilter(config.filters());
        this.pinger = new MentionPinger(
                config.mentions().sound(),
                config.mentions().volume(),
                config.mentions().pitch(),
                config.mentions().cooldownMs()
        );
        this.formatter = new ChatFormatter(plugin);
    }

    public PlayerSettingsStore settings() {
        return settings;
    }

    public ChatHistory history() {
        return history;
    }

    public void saveSettings() {
        settings.saveAsync();
    }

    public void forget(UUID id) {
        settings.unload(id);
        if (spamGuard != null) {
            spamGuard.reset(id);
        }
        if (pinger != null) {
            pinger.forget(id);
        }
    }

    /**
     * Handles one chat message. Must be called on the main/global region thread.
     *
     * @param forceGlobal true for /globalchat, which bypasses the trigger prefix
     */
    public void handle(Player player, String input, boolean forceGlobal) {
        PluginConfig config = plugin.config();
        Messages messages = plugin.messages();

        if (!player.hasPermission("defaultchat.use")) {
            messages.send(player, "error.no-permission");
            return;
        }

        String text = input == null ? "" : input.strip();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > config.maxLength()) {
            messages.send(player, "error.too-long", Placeholder.parsed("max", Integer.toString(config.maxLength())));
            return;
        }

        boolean global = forceGlobal;
        if (!global) {
            String trigger = config.global().trigger();
            if (!trigger.isEmpty() && text.startsWith(trigger) && text.length() > trigger.length()) {
                if (canUseGlobal(player, config)) {
                    global = true;
                    text = text.substring(trigger.length()).strip();
                }
            }
        }

        ChatChannel channel = global ? ChatChannel.GLOBAL : settings.get(player.getUniqueId()).channel();
        if (channel == ChatChannel.GLOBAL && !canUseGlobal(player, config)) {
            if (forceGlobal) {
                messages.send(player, "channel.no-permission");
                return;
            }
            channel = ChatChannel.LOCAL;
        }

        boolean formatting = config.playerMiniMessage() && player.hasPermission("defaultchat.format");
        String plain = ChatInput.plain(text, formatting);

        if (!player.hasPermission("defaultchat.bypass.spam")
                && config.caps().action() == CapsAction.BLOCK
                && MessageFilter.isShouting(plain, config.caps().minLength(), config.caps().maxRatio())) {
            messages.send(player, "caps.blocked");
            return;
        }

        if (!player.hasPermission("defaultchat.bypass.spam")) {
            SpamGuard.Decision decision = spamGuard.check(player.getUniqueId(), plain, System.currentTimeMillis());
            if (!decision.allowed()) {
                String key = switch (decision.reason()) {
                    case COOLDOWN -> "spam.cooldown";
                    case DUPLICATE -> "spam.duplicate";
                    case RATE_LIMIT -> "spam.rate-limit";
                    case NONE -> null;
                };
                if (key != null) {
                    feedback(player, key, decision.retryAfterMs());
                }
                return;
            }
        }

        if (!player.hasPermission("defaultchat.bypass.filter")) {
            MessageFilter.Result result = filter.apply(text);
            if (result.blocked()) {
                messages.send(player, "error.filtered");
                return;
            }
            if (!result.text().equals(text)) {
                text = result.text();
                plain = ChatInput.plain(text, formatting);
            }
        }

        List<Player> recipients = recipients(player, channel, config);
        Set<String> mentioned = Set.of();
        if (config.mentions().enabled() && player.hasPermission("defaultchat.mention")) {
            mentioned = Mentions.find(
                    plain,
                    recipients.stream().map(Player::getName).toList(),
                    config.mentions().minLength()
            );
        }

        Component body = ChatInput.parse(text, formatting);
        if (!mentioned.isEmpty()) {
            Set<String> names = mentioned;
            body = MentionHighlighter.highlight(body, names::contains, config.mentions().highlight());
        }

        Component formatted = formatter.format(player, channel, body);
        DefaultChatMessageEvent event = new DefaultChatMessageEvent(
                player,
                channel,
                formatted,
                plain,
                new LinkedHashSet<>(recipients)
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        for (Audience viewer : event.viewers()) {
            viewer.sendMessage(event.message());
        }
        Bukkit.getConsoleSender().sendMessage(event.message());

        history.add(new ChatHistory.Entry(Instant.now(), channel, player.getName(), event.message(), plain));
        logWriter.log(player.getName(), channel, plain);

        if (!mentioned.isEmpty()) {
            for (Player target : recipients) {
                if (target.equals(player) || !event.viewers().contains(target)) {
                    continue;
                }
                if (!mentioned.contains(target.getName().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (settings.get(target.getUniqueId()).mentionPings()) {
                    pinger.ping(target);
                }
            }
        }
    }

    private void feedback(Player player, String key, long retryAfterMs) {
        long seconds = Math.max(1L, (retryAfterMs + 999L) / 1000L);
        plugin.messages().send(player, key, Placeholder.parsed("seconds", Long.toString(seconds)));
    }

    private boolean canUseGlobal(Player player, PluginConfig config) {
        return !config.global().requirePermission() || player.hasPermission("defaultchat.channel.global");
    }

    private List<Player> recipients(Player sender, ChatChannel channel, PluginConfig config) {
        List<Player> out = new ArrayList<>();
        if (channel == ChatChannel.GLOBAL || !config.local().enabled()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (visible(sender, online, config)) {
                    out.add(online);
                }
            }
        } else {
            World world = sender.getWorld();
            int radius = config.local().radiusFor(world.getName().toLowerCase(Locale.ROOT));
            if (radius <= 0) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (visible(sender, online, config)) {
                        out.add(online);
                    }
                }
            } else {
                double max = (double) radius * radius;
                Location origin = sender.getLocation();
                for (Player online : world.getPlayers()) {
                    if (visible(sender, online, config) && origin.distanceSquared(online.getLocation()) <= max) {
                        out.add(online);
                    }
                }
            }
        }
        if (!out.contains(sender)) {
            out.add(sender);
        }
        return out;
    }

    private boolean visible(Player sender, Player viewer, PluginConfig config) {
        if (viewer.equals(sender) || !config.respectVanish()) {
            return true;
        }
        return viewer.canSee(sender);
    }
}
