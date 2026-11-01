package com.tuskacts.net;

import com.tuskacts.TuskActs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server -> player: attack slot of the given act is on cooldown for N ticks. */
public record CdPayload(int act, int slot, int ticks) implements CustomPayload {
    public static final CustomPayload.Id<CdPayload> ID = new CustomPayload.Id<>(TuskActs.id("cd"));
    public static final PacketCodec<RegistryByteBuf, CdPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CdPayload::act,
            PacketCodecs.VAR_INT, CdPayload::slot,
            PacketCodecs.VAR_INT, CdPayload::ticks,
            CdPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
