package com.tuskacts.net;

import com.tuskacts.TuskActs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Player -> server. kind 0: use attack slot 0..5, kind 1: summon / dismiss the stand, kind 2: next act. */
public record ActionPayload(int kind, int slot) implements CustomPayload {
    public static final CustomPayload.Id<ActionPayload> ID = new CustomPayload.Id<>(TuskActs.id("action"));
    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ActionPayload::kind,
            PacketCodecs.VAR_INT, ActionPayload::slot,
            ActionPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
