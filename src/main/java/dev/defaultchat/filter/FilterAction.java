package dev.defaultchat.filter;

import java.util.Locale;

public enum FilterAction {

    MASK,
    BLOCK;

    public static FilterAction parse(String value) {
        if (value == null) {
            return MASK;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MASK;
        }
    }
}
