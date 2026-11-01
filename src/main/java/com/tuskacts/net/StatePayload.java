package com.tuskacts.net;

import com.tuskacts.TuskActs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server -> owner: unlocked acts (0..4), current act, summoned, energy and its maximum. */
public record StatePayload(int unlocked, int act, boolean out, int energy, int max) implements CustomPayload {
    public static final CustomPayload.Id<StatePayload> ID = new CustomPayload.Id<>(TuskActs.id("state"));
    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StatePayload::unlocked,
            PacketCodecs.VAR_INT, StatePayload::act,
            PacketCodecs.BOOL, StatePayload::out,
            PacketCodecs.VAR_INT, StatePayload::energy,
            PacketCodecs.VAR_INT, StatePayload::max,
            StatePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
