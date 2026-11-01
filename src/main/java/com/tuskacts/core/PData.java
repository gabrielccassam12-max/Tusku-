package com.tuskacts.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Saved with the player: highest unlocked act (0 none .. 4), act in use, stand summoned, spin energy. */
public record PData(int unlocked, int act, boolean out, int energy) {
    public static final PData DEFAULT = new PData(0, 1, false, 100);
    public static final int MAX = 100;

    public static final Codec<PData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("unlocked").forGetter(PData::unlocked),
            Codec.INT.fieldOf("act").forGetter(PData::act),
            Codec.BOOL.fieldOf("out").forGetter(PData::out),
            Codec.INT.fieldOf("energy").forGetter(PData::energy)
    ).apply(i, PData::new));

    public PData withUnlocked(int v) { return new PData(Math.max(0, Math.min(4, v)), act, out, energy); }
    public PData withAct(int v) { return new PData(unlocked, Math.max(1, Math.min(4, v)), out, energy); }
    public PData withOut(boolean v) { return new PData(unlocked, act, v, energy); }
    public PData withEnergy(int v) { return new PData(unlocked, act, out, Math.max(0, Math.min(MAX, v))); }
}
