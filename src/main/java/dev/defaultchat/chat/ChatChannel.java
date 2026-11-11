package dev.defaultchat.chat;

import java.util.Locale;

public enum ChatChannel {

    LOCAL,
    GLOBAL;

    public static ChatChannel parse(String value) {
        return value != null && value.equalsIgnoreCase("global") ? GLOBAL : LOCAL;
    }

    public ChatChannel opposite() {
        return this == LOCAL ? GLOBAL : LOCAL;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
