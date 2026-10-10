package com.wellfeed.gui;

/** One short message shown near the hotbar. */
public final class Toasts {
    private static String text;
    private static long until;

    private Toasts() {
    }

    public static void push(String message) {
        text = message;
        until = System.currentTimeMillis() + 1600;
    }

    /** The message to show right now, or null. */
    public static String current() {
        return System.currentTimeMillis() < until ? text : null;
    }
}
