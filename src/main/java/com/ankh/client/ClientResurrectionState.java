package com.ankh.client;

public final class ClientResurrectionState {

    private ClientResurrectionState() {}

    private static boolean active = false;
    private static int secondsRemaining = 0;
    private static boolean singleplayer = false;

    private static boolean ankhVfxArmed = false;

    public static void setTimer(int seconds, boolean sp) {
        active = true;
        secondsRemaining = seconds;
        singleplayer = sp;
    }

    public static void clear() {
        active = false;
        secondsRemaining = 0;
    }

    public static void reset() {
        active = false;
        secondsRemaining = 0;
        singleplayer = false;
        ankhVfxArmed = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static int getSecondsRemaining() {
        return secondsRemaining;
    }

    public static boolean isSingleplayer() {
        return singleplayer;
    }

    public static void armAnkhVfx() {
        ankhVfxArmed = true;
    }

    public static boolean consumeAnkhVfx() {
        boolean was = ankhVfxArmed;
        ankhVfxArmed = false;
        return was;
    }

    public static boolean isAnkhVfxArmed() {
        return ankhVfxArmed;
    }
}
