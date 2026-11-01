package com.tuskacts.client;

import com.tuskacts.entity.ModEntities;
import com.tuskacts.net.ActionPayload;
import com.tuskacts.net.CdPayload;
import com.tuskacts.net.StatePayload;
import com.tuskacts.particle.CubeProfile;
import com.tuskacts.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class TuskClient implements ClientModInitializer {
    private static final String CAT = "key.categories.tuskacts";
    private static final KeyBinding[] SLOT = new KeyBinding[6];
    private static final boolean[] down = new boolean[6];
    private static KeyBinding SUMMON, ACT;
    private static final int[] ACT_COLOR = {0, 0xFF8AC0, 0xFFB454, 0xB58CFF, 0xFFE27A};

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tuskacts." + name, InputUtil.Type.KEYSYM, code, CAT));
    }

    @Override
    public void onInitializeClient() {
        for (Map.Entry<String, SimpleParticleType> en : ModParticles.TYPES.entrySet()) {
            CubeProfile pf = ModParticles.PROFILES.get(en.getKey());
            ParticleFactoryRegistry.getInstance().register(en.getValue(), sprites ->
                    (ParticleFactory<SimpleParticleType>) (type, world, x, y, z, vx, vy, vz) ->
                            new CubeParticle(world, x, y, z, vx, vy, vz, sprites, pf));
        }
        EntityRendererRegistry.register(ModEntities.STAND, StandRenderer::new);

        int[] codes = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N};
        for (int i = 0; i < SLOT.length; i++) SLOT[i] = key("attack" + (i + 1), codes[i]);
        SUMMON = key("summon", GLFW.GLFW_KEY_G);
        ACT = key("act", GLFW.GLFW_KEY_H);

        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (p, ctx) -> {
            ClientState.unlocked = p.unlocked();
            ClientState.act = p.act();
            ClientState.out = p.out();
            ClientState.energy = p.energy();
            ClientState.max = p.max();
        });
        ClientPlayNetworking.registerGlobalReceiver(CdPayload.ID, (p, ctx) -> {
            int i = (p.act() - 1) * 6 + p.slot();
            if (i >= 0 && i < 24) {
                ClientState.cdEnd[i] = ClientState.clock + p.ticks();
                ClientState.cdTotal[i] = Math.max(1, p.ticks());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientState.clock++;
            if (client.player == null) return;
            boolean free = client.currentScreen == null;
            for (int i = 0; i < SLOT.length; i++) {
                boolean now = free && SLOT[i].isPressed();
                if (now && !down[i]) ClientPlayNetworking.send(new ActionPayload(0, i));
                down[i] = now;
                while (SLOT[i].wasPressed()) { }
            }
            while (SUMMON.wasPressed()) if (free) ClientPlayNetworking.send(new ActionPayload(1, 0));
            while (ACT.wasPressed()) if (free) ClientPlayNetworking.send(new ActionPayload(2, 0));
        });

        HudRenderCallback.EVENT.register(TuskClient::hud);
    }

    private static int argb(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void hud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || ClientState.unlocked <= 0) return;
        int act = Math.max(1, Math.min(4, ClientState.act));
        int col = ACT_COLOR[act];
        int w = 130, x = g.getScaledWindowWidth() - w - 10, rows = 6;
        int y = g.getScaledWindowHeight() - 30 - rows * 12 - 30;
        g.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.tuskacts.act", act).append(ClientState.out ? Text.empty() : Text.translatable("hud.tuskacts.away")), x, y, argb(col, 255));
        ClientState.shown += (ClientState.energy - ClientState.shown) * 0.25f;
        float pct = Math.max(0f, Math.min(1f, ClientState.shown / Math.max(1, ClientState.max)));
        int by = y + 12;
        g.fill(x - 3, by - 2, x + w + 3, by + 8, argb(0x0B0F14, 0xB4));
        g.drawBorder(x - 3, by - 2, w + 6, 10, argb(col, 255));
        g.fill(x, by, x + (int) (w * pct), by + 6, argb(col, 255));
        String en = Text.translatable("hud.tuskacts.spin").getString() + " " + ClientState.energy + "/" + ClientState.max;
        g.drawTextWithShadow(mc.textRenderer, en, x, by + 10, 0xFFC8D8E8);
        int ay = by + 24;
        for (int i = 0; i < rows; i++) {
            int ry = ay + i * 12;
            int ci = (act - 1) * 6 + i;
            long left = Math.max(0, ClientState.cdEnd[ci] - ClientState.clock);
            int cost = com.tuskacts.core.Defs.cost(act, i);
            boolean afford = ClientState.energy >= cost;
            g.fill(x - 3, ry - 1, x + w + 3, ry + 10, argb(0x0B0F14, 0x88));
            int tcol = left > 0 ? 0xFF8A94A0 : afford ? 0xFFFFFFFF : 0xFFFF7777;
            g.drawTextWithShadow(mc.textRenderer, Text.literal("[").append(SLOT[i].getBoundKeyLocalizedText()).append("] ").append(Text.translatable("skill.tuskacts.a" + act + "_" + i)), x, ry + 1, tcol);
            String right = left > 0 ? String.format("%.1f", left / 20.0) : String.valueOf(cost);
            g.drawTextWithShadow(mc.textRenderer, right, x + w - mc.textRenderer.getWidth(right), ry + 1, left > 0 ? 0xFFFF8888 : argb(col, 255));
            if (left > 0) {
                int cw = (int) (w * (1f - (float) left / Math.max(1, ClientState.cdTotal[ci])));
                g.fill(x, ry + 9, x + cw, ry + 10, argb(col, 255));
            }
        }
    }
}
