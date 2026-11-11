package dev.defaultchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

/**
 * Parses player input. Only a safe subset of MiniMessage is exposed to players: no click, hover,
 * font, keybind, selector, score, nbt or translatable tags, so a message can never inject actions.
 */
public final class ChatInput {

    private static final MiniMessage SAFE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.transition())
                    .resolver(StandardTags.shadowColor())
                    .resolver(StandardTags.sprite())
                    .resolver(StandardTags.reset())
                    .build())
            .build();

    private ChatInput() {
    }

    public static Component parse(String text, boolean allowFormatting) {
        if (!allowFormatting) {
            return Component.text(text);
        }
        try {
            return SAFE.deserialize(text);
        } catch (RuntimeException e) {
            return Component.text(text);
        }
    }

    /** Plain text used for filtering, mention matching and logging. */
    public static String plain(String text, boolean allowFormatting) {
        if (!allowFormatting) {
            return text;
        }
        try {
            return MiniMessage.miniMessage().stripTags(text);
        } catch (RuntimeException e) {
            return text;
        }
    }
}
