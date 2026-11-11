package dev.defaultchat.command;

import dev.defaultchat.DefaultChatPlugin;
import dev.defaultchat.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GlobalChatCommand implements CommandExecutor {

    private final DefaultChatPlugin plugin;

    public GlobalChatCommand(DefaultChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.player-only");
            return true;
        }
        if (args.length == 0) {
            messages.send(sender, "command.global-usage");
            return true;
        }
        plugin.chat().handle(player, String.join(" ", args), true);
        return true;
    }
}
