package dev.defaultchat.filter;

import java.util.Locale;

public enum CapsAction {

    BLOCK,
    IGNORE;

    public static CapsAction parse(String value) {
        if (value == null) {
            return BLOCK;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BLOCK;
        }
    }
}
