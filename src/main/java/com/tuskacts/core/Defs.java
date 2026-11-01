package com.tuskacts.core;

/**
 * The 24 attacks: {energy cost, cooldown ticks, stand pose, pose ticks}. Index [act - 1][slot].
 * Stand poses: 0 idle, 1 aiming one arm, 2 rush (alternating fists in front), 3 heavy punch, 4 uppercut, 5 slam, 6 grab.
 */
public final class Defs {
    private Defs() {}

    public static final int[][][] SKILLS = {
            // ACT 1: nail shooter
            {{6, 14, 1, 8}, {8, 24, 1, 10}, {14, 50, 1, 12}, {14, 80, 1, 12}, {10, 60, 1, 10}, {22, 140, 2, 26}},
            // ACT 2: drilling nails and chasing holes
            {{10, 20, 1, 8}, {16, 70, 1, 10}, {18, 90, 1, 20}, {20, 140, 5, 12}, {22, 100, 5, 20}, {38, 220, 1, 30}},
            // ACT 3: wormholes
            {{20, 120, 1, 14}, {24, 90, 1, 14}, {26, 160, 5, 14}, {22, 160, 6, 30}, {28, 180, 1, 14}, {30, 200, 1, 14}},
            // ACT 4: physical fighting
            {{24, 100, 2, 44}, {30, 120, 3, 28}, {28, 110, 4, 22}, {40, 200, 6, 16}, {34, 150, 5, 22}, {32, 170, 6, 40}},
    };

    public static int cost(int act, int slot) { return SKILLS[act - 1][slot][0]; }
    public static int cooldown(int act, int slot) { return SKILLS[act - 1][slot][1]; }
    public static int pose(int act, int slot) { return SKILLS[act - 1][slot][2]; }
    public static int poseTicks(int act, int slot) { return SKILLS[act - 1][slot][3]; }
}
