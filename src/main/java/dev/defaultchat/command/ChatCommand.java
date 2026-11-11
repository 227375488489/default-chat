package dev.defaultchat.command;

import dev.defaultchat.DefaultChatPlugin;
import dev.defaultchat.chat.ChatChannel;
import dev.defaultchat.chat.PlayerSettings;
import dev.defaultchat.config.Messages;
import dev.defaultchat.history.ChatHistory;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("local", "global", "toggle", "ping", "history", "reload");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final DefaultChatPlugin plugin;

    public ChatCommand(DefaultChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();
        if (args.length == 0) {
            messages.send(sender, "command.usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "local" -> switchChannel(sender, ChatChannel.LOCAL);
            case "global" -> switchChannel(sender, ChatChannel.GLOBAL);
            case "toggle" -> toggle(sender);
            case "ping" -> togglePing(sender);
            case "history" -> showHistory(sender, args);
            case "reload" -> reload(sender);
            default -> messages.send(sender, "command.usage");
        }
        return true;
    }

    private void toggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "error.player-only");
            return;
        }
        ChatChannel current = plugin.chat().settings().get(player.getUniqueId()).channel();
        switchChannel(sender, current.opposite());
    }

    private void switchChannel(CommandSender sender, ChatChannel channel) {
        Messages messages = plugin.messages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.player-only");
            return;
        }
        if (channel == ChatChannel.GLOBAL && !player.hasPermission("defaultchat.channel.global")) {
            messages.send(sender, "channel.no-permission");
            return;
        }
        PlayerSettings settings = plugin.chat().settings().get(player.getUniqueId());
        settings.channel(channel);
        plugin.chat().settings().persist(settings);
        messages.send(sender, "channel." + channel.key());
    }

    private void togglePing(CommandSender sender) {
        Messages messages = plugin.messages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.player-only");
            return;
        }
        PlayerSettings settings = plugin.chat().settings().get(player.getUniqueId());
        settings.mentionPings(!settings.mentionPings());
        plugin.chat().settings().persist(settings);
        messages.send(sender, settings.mentionPings() ? "ping.enabled" : "ping.disabled");
    }

    private void showHistory(CommandSender sender, String[] args) {
        Messages messages = plugin.messages();
        if (!sender.hasPermission("defaultchat.history")) {
            messages.send(sender, "error.no-permission");
            return;
        }
        ChatHistory history = plugin.chat().history();
        if (!history.enabled()) {
            messages.send(sender, "history.disabled");
            return;
        }
        int count = 10;
        if (args.length > 1) {
            try {
                count = Math.clamp(Integer.parseInt(args[1]), 1, 50);
            } catch (NumberFormatException e) {
                messages.send(sender, "command.usage");
                return;
            }
        }
        List<ChatHistory.Entry> entries = history.recent(count);
        if (entries.isEmpty()) {
            messages.send(sender, "history.empty");
            return;
        }
        messages.send(sender, "history.header");
        for (ChatHistory.Entry entry : entries) {
            sender.sendMessage(messages.parse(
                    "history.line",
                    Placeholder.unparsed("time", TIME.format(entry.time())),
                    Placeholder.component("channel", messages.parse("chat.tag." + entry.channel().key())),
                    Placeholder.unparsed("player", entry.player()),
                    Placeholder.unparsed("message", entry.plain())
            ));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("defaultchat.reload")) {
            plugin.messages().send(sender, "error.no-permission");
            return;
        }
        if (plugin.reloadPlugin()) {
            plugin.messages().send(sender, "reload.success");
        } else {
            plugin.messages().send(sender, "reload.failed");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(prefix)) {
                out.add(sub);
            }
        }
        return out;
    }
}
