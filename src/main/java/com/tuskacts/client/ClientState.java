package com.tuskacts.client;

/** What the server last told this client, for the HUD. */
public final class ClientState {
    private ClientState() {}

    public static int unlocked, act = 1, energy, max = 100;
    public static boolean out;
    public static final long[] cdEnd = new long[24];
    public static final int[] cdTotal = new int[24];
    public static long clock;
    public static float shown;
}
