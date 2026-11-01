package com.tuskacts.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tuskacts.TuskActs;
import com.tuskacts.core.Defs;
import com.tuskacts.core.PData;
import com.tuskacts.entity.ModEntities;
import com.tuskacts.entity.TuskStand;
import com.tuskacts.net.CdPayload;
import com.tuskacts.net.StatePayload;
import com.tuskacts.particle.Fx;
import com.tuskacts.util.Snd;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps each player's Tusk: unlocking acts, the visible stand, spin energy, cooldowns and the 24 attacks. */
public final class Manager {
    private Manager() {}

    public static final AttachmentType<PData> DATA = AttachmentRegistry.<PData>builder()
            .persistent(PData.CODEC).copyOnDeath().buildAndRegister(TuskActs.id("tusk"));

    private static final Map<UUID, long[]> CD = new HashMap<>();        // [(act-1)*6 + slot] -> tick the attack is ready
    private static final Map<UUID, Integer> STANDS = new HashMap<>();   // owner -> stand entity id
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    public static PData get(ServerPlayerEntity p) {
        return ((AttachmentTarget) p).getAttachedOrElse(DATA, PData.DEFAULT);
    }

    public static void set(ServerPlayerEntity p, PData d) {
        ((AttachmentTarget) p).setAttached(DATA, d);
    }

    private static ServerWorld world(ServerPlayerEntity p) {
        return (ServerWorld) p.getWorld();
    }

    // ================================================================== tick
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTicks();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                PData d = get(p);
                if (d.unlocked() == 0 || !p.isAlive()) continue;
                if (tick % 5 == 0 && d.energy() < PData.MAX) {                       // nails grow back; mint tea (sneaking) is faster
                    d = d.withEnergy(d.energy() + (p.isSneaking() ? 2 : 1));
                    set(p, d);
                }
                keepStand(p, d);
                int stamp = d.energy() * 100 + d.act() * 10 + d.unlocked() + (d.out() ? 7 : 0);
                Integer last = LAST_SENT.get(p.getUuid());
                if (last == null || last != stamp || tick % 40 == 0) {
                    sendState(p);
                    LAST_SENT.put(p.getUuid(), stamp);
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SENT.remove(handler.getPlayer().getUuid());
            STANDS.remove(handler.getPlayer().getUuid());
            sendState(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            CD.remove(id);
            STANDS.remove(id);
            LAST_SENT.remove(id);
        });
        registerCommands();
    }

    /** Makes sure exactly one stand exists while the player has it summoned. */
    private static void keepStand(ServerPlayerEntity p, PData d) {
        ServerWorld w = world(p);
        Integer id = STANDS.get(p.getUuid());
        TuskStand s = id == null ? null : w.getEntityById(id) instanceof TuskStand t && t.isAlive() ? t : null;
        if (d.out() && s == null) {
            TuskStand n = ModEntities.STAND.create(w);
            if (n == null) return;
            n.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), 0f, 0f);
            n.setOwnerId(p.getId());
            n.setAct(d.act());
            w.spawnEntity(n);
            STANDS.put(p.getUuid(), n.getId());
        } else if (!d.out() && s != null) {
            s.discard();
            STANDS.remove(p.getUuid());
        }
    }

    public static TuskStand stand(ServerPlayerEntity p) {
        Integer id = STANDS.get(p.getUuid());
        return id != null && world(p).getEntityById(id) instanceof TuskStand t ? t : null;
    }

    public static void sendState(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, StatePayload.ID)) return;
        PData d = get(p);
        ServerPlayNetworking.send(p, new StatePayload(d.unlocked(), d.act(), d.out(), d.energy(), PData.MAX));
    }

    // ================================================================== actions from the keys
    public static void action(ServerPlayerEntity p, int kind, int slot) {
        PData d = get(p);
        if (d.unlocked() == 0 || !p.isAlive()) return;
        switch (kind) {
            case 0 -> use(p, slot);
            case 1 -> {
                set(p, d.withOut(!d.out()));
                Snd.play(world(p), p.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, d.out() ? 0.8f : 1.6f);
                sendState(p);
            }
            case 2 -> {
                int next = d.act() >= d.unlocked() ? 1 : d.act() + 1;
                set(p, d.withAct(next));
                p.sendMessage(Text.translatable("message.tuskacts.act", next).formatted(Formatting.LIGHT_PURPLE), true);
                Snd.play(world(p), p.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f + next * 0.15f);
                sendState(p);
            }
            default -> { }
        }
    }

    private static void use(ServerPlayerEntity p, int slot) {
        if (slot < 0 || slot > 5) return;
        PData d = get(p);
        int act = d.act();
        ServerWorld w = world(p);
        long[] cd = CD.computeIfAbsent(p.getUuid(), k -> new long[24]);
        int ci = (act - 1) * 6 + slot;
        if (w.getTime() < cd[ci]) return;
        int cost = Defs.cost(act, slot);
        if (d.energy() < cost) {
            p.sendMessage(Text.translatable("message.tuskacts.no_energy").formatted(Formatting.RED), true);
            return;
        }
        if (!d.out()) {
            d = d.withOut(true);
            set(p, d);
            keepStand(p, d);
        }
        boolean ok = switch (act) {
            case 1 -> Act1.run(slot, w, p);
            case 2 -> Act2.run(slot, w, p);
            case 3 -> Act3.run(slot, w, p);
            default -> Act4.run(slot, w, p);
        };
        if (!ok) {
            p.sendMessage(Text.translatable("message.tuskacts.no_target").formatted(Formatting.GRAY), true);
            return;
        }
        set(p, get(p).withEnergy(get(p).energy() - cost));
        cd[ci] = w.getTime() + Defs.cooldown(act, slot);
        TuskStand s = stand(p);
        if (s != null) s.setStance(Defs.pose(act, slot), Defs.poseTicks(act, slot));
        p.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        if (ServerPlayNetworking.canSend(p, CdPayload.ID)) ServerPlayNetworking.send(p, new CdPayload(act, slot, Defs.cooldown(act, slot)));
        sendState(p);
    }

    // ================================================================== items and commands
    /** The Golden Rectangle: gives Tusk, and each new use evolves it by one act. */
    public static boolean evolve(ServerPlayerEntity p) {
        PData d = get(p);
        if (d.unlocked() >= 4) {
            p.sendMessage(Text.translatable("message.tuskacts.max").formatted(Formatting.GRAY), true);
            return false;
        }
        unlock(p, d.unlocked() + 1);
        return true;
    }

    public static void unlock(ServerPlayerEntity p, int act) {
        PData d = get(p).withUnlocked(Math.max(get(p).unlocked(), act)).withAct(act).withOut(true).withEnergy(PData.MAX);
        set(p, d);
        ServerWorld w = world(p);
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 20));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("message.tuskacts.unlock_sub." + act).formatted(Formatting.GRAY)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("message.tuskacts.unlock", act).formatted(Formatting.BOLD, Formatting.LIGHT_PURPLE)));
        Snd.play(w, p.getPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Vec3d c = p.getPos().add(0, 1, 0);
        Fx.burst(w, "t_gold", c, 60, 0.45, 0.4);
        Fx.burst(w, "t_star", c, 8, 0.2, 0.3);
        Fx.burst(w, "t_spin", c, 40, 0.3, 0.3);
        Ops.ring(w, p.getPos().add(0, 0.1, 0), 4, 12);
        keepStand(p, d);
        sendState(p);
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("tusk")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("act").then(CommandManager.argument("n", IntegerArgumentType.integer(1, 4)).executes(ctx -> {
                            unlock(ctx.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(ctx, "n"));
                            return 1;
                        })))
                        .then(CommandManager.literal("energy").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            set(p, get(p).withEnergy(PData.MAX));
                            sendState(p);
                            return 1;
                        }))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            set(p, PData.DEFAULT);
                            CD.remove(p.getUuid());
                            sendState(p);
                            return 1;
                        }))));
    }
}
