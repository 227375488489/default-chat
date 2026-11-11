package dev.defaultchat.chat;

import dev.defaultchat.DefaultChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Builds the final chat line from the language template. */
public final class ChatFormatter {

    private final DefaultChatPlugin plugin;

    public ChatFormatter(DefaultChatPlugin plugin) {
        this.plugin = plugin;
    }

    public Component format(Player player, ChatChannel channel, Component message) {
        String template = plugin.messages().raw("chat.format." + channel.key());
        if (template == null || template.isBlank()) {
            template = "<player><dark_gray> › <message>";
        }

        Component name = Component.text(player.getName());
        if (plugin.config().clickableNames()) {
            name = name
                    .clickEvent(ClickEvent.suggestCommand("/msg " + player.getName()))
                    .hoverEvent(HoverEvent.showText(plugin.messages().parse(
                            "chat.name-hover",
                            Placeholder.unparsed("player", player.getName())
                    )));
        }

        return plugin.messages().parseRaw(
                template,
                Placeholder.component("player", name),
                Placeholder.component("message", message)
        );
    }
}
