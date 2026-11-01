package com.tuskacts;

import com.tuskacts.entity.ModEntities;
import com.tuskacts.item.ModItems;
import com.tuskacts.net.ActionPayload;
import com.tuskacts.net.CdPayload;
import com.tuskacts.net.StatePayload;
import com.tuskacts.particle.ModParticles;
import com.tuskacts.server.Manager;
import com.tuskacts.util.Effects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TuskActs implements ModInitializer {
    public static final String ID = "tuskacts";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    public static Identifier id(String path) {
        return Identifier.of(ID, path);
    }

    @Override
    public void onInitialize() {
        ModParticles.init();
        Effects.init();
        ModEntities.init();
        ModItems.init();
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CdPayload.ID, CdPayload.CODEC);
        Manager.init();
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) ->
                Manager.action(context.player(), payload.kind(), payload.slot()));
        LOG.info("Tusk: Act 1 to 4 ready.");
    }
}
