package com.tuskacts.server;

import com.tuskacts.particle.Fx;
import com.tuskacts.util.Effects;
import com.tuskacts.util.Snd;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** ACT 3: humanoid Tusk with the wormhole: dive through a hole, strike from inside one, a black hole, a hole shield, twin portals, orbiting holes. */
public final class Act3 {
    private Act3() {}

    private static final Random R = Fx.R;
    private static final Vec3d Y = new Vec3d(0, 1, 0);

    static boolean run(int slot, ServerWorld w, ServerPlayerEntity p) {
        return switch (slot) {
            case 0 -> dive(w, p);
            case 1 -> inside(w, p);
            case 2 -> blackHole(w, p);
            case 3 -> shield(w, p);
            case 4 -> twin(w, p);
            default -> orbit(w, p);
        };
    }

    // 1. Johnny shoots himself: he sinks into a hole under his feet and comes out far ahead
    private static boolean dive(ServerWorld w, ServerPlayerEntity p) {
        Vec3d start = p.getPos();
        Vec3d exit = Ops.walkFree(w, start, start.add(Ops.flat(p.getRotationVec(1f)).multiply(12)));
        if (exit.distanceTo(start) < 2.5) return false;
        Snd.play(w, start, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 24, 0, true, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 26, 4, true, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 22, 6, true, false, false));
        Effects.add(t -> {
            if (t > 24 || !p.isAlive()) return false;
            p.fallDistance = 0;
            if (t == 1) {
                Ops.disc(w, start.add(0, 0.08, 0), Ops.X, Ops.Z, 1.3, 30);
                for (LivingEntity e : Ops.around(w, start.add(0, 0.5, 0), 1.9, p)) Ops.hurt(w, e, p, 6f);
            }
            if (t < 22 && t % 2 == 0) {
                Ops.disc(w, start.add(0, 0.08, 0), Ops.X, Ops.Z, 1.2, 8);
                Fx.vel(w, "t_spin", start.add(Fx.jitter(0.5)).add(0, 0.6, 0), new Vec3d(0, -0.08, 0));
            }
            if (t == 22) {
                p.requestTeleport(exit.x, exit.y, exit.z);
                Snd.play(w, exit, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
                Ops.disc(w, exit.add(0, 0.08, 0), Ops.X, Ops.Z, 1.4, 34);
                Fx.burst(w, "t_spin", exit.add(0, 0.5, 0), 24, 0.35, 0.5);
                for (LivingEntity e : Ops.around(w, exit.add(0, 0.5, 0), 2.3, p)) {
                    Ops.hurt(w, e, p, 7f);
                    Ops.push(e, e.getPos().subtract(exit), 0.7, 0.5);
                }
            }
            return true;
        });
        return true;
    }

    // 2. a hole opens behind the enemy and the spinning claws come out of it
    private static boolean inside(ServerWorld w, ServerPlayerEntity p) {
        LivingEntity target = Ops.aimEntity(w, p, 14, 0.8);
        if (target == null) return false;
        Vec3d away = Ops.flat(target.getPos().subtract(p.getPos()));
        Vec3d side = Ops.rightOf(away);
        Vec3d hole = Ops.mid(target).add(away.multiply(1.4));
        Snd.play(w, hole, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        Effects.add(t -> {
            if (t > 15 || !p.isAlive() || !target.isAlive()) return false;
            if (t < 11) {
                Ops.disc(w, hole, side, Y, 0.2 + 0.07 * t, 6 + t * 2);
                return true;
            }
            if (t == 11) {
                Vec3d to = Ops.mid(target);
                Snd.play(w, to, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.7f);
                for (int k = -1; k <= 1; k++) {
                    Vec3d off = side.multiply(k * 0.4);
                    Ops.line(w, "t_spin", hole.add(off), to.add(off).subtract(away.multiply(0.2)), 0.2, Vec3d.ZERO);
                    Ops.line(w, "t_nail", hole.add(off), to.add(off), 0.35, Vec3d.ZERO);
                }
                Fx.burst(w, "t_star", to, 10, 0.2, 0.2);
                Ops.hurt(w, target, p, 13f);
                Ops.push(target, away.multiply(-1), 1.0, 0.25);
            }
            Ops.disc(w, hole, side, Y, 0.9, 10);
            return true;
        });
        return true;
    }

    // 3. a black hole on the ground: it drags everything in, then collapses
    private static boolean blackHole(ServerWorld w, ServerPlayerEntity p) {
        Vec3d c = Ops.ground(w, Ops.aim(w, p, 8), p);
        Snd.play(w, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 0.5f);
        Effects.add(t -> {
            if (t > 70 || !p.isAlive()) return false;
            if (t % 2 == 0) Ops.disc(w, c.add(0, 0.08, 0), Ops.X, Ops.Z, 1.6, 8);
            for (int i = 0; i < 5; i++) {
                double a = t * 0.4 + i * 1.26, r = 4.2 * (1.0 - ((t + i * 3) % 14) / 14.0);
                Fx.vel(w, "t_spin", c.add(Math.cos(a) * r, 0.15, Math.sin(a) * r), new Vec3d(0, 0.01, 0));
            }
            for (LivingEntity e : Ops.around(w, c.add(0, 0.8, 0), 4.6, p)) {
                Vec3d in = Ops.flat(c.subtract(e.getPos()));
                double d = e.getPos().distanceTo(c);
                e.addVelocity(in.x * 0.075, 0, in.z * 0.075);
                e.velocityModified = true;
                if (d < 1.4 && t % 8 == 0) {
                    Ops.hit(w, e, p, 3f);
                    Fx.burst(w, "t_spark", Ops.mid(e), 6, 0.2, 0.2);
                }
            }
            if (t == 70) {
                Snd.play(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                Fx.burst(w, "t_hole", c.add(0, 0.5, 0), 20, 0.5, 0.4);
                Fx.burst(w, "t_spin", c.add(0, 0.5, 0), 40, 0.5, 0.4);
                Ops.ring(w, c.add(0, 0.1, 0), 5, 10);
                Ops.blast(w, c.add(0, 0.5, 0), p, 4.0, 6f, 0.9, 0.5, null);
            }
            return true;
        });
        return true;
    }

    // 4. a standing hole in front of you swallows projectiles and cuts anything that touches it
    private static boolean shield(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 36, 2, true, false, false));
        Effects.add(t -> {
            if (t > 30 || !p.isAlive()) return false;
            Vec3d front = Ops.flat(p.getRotationVec(1f));
            Vec3d c = p.getPos().add(0, 1.1, 0).add(front.multiply(1.9));
            Vec3d right = Ops.rightOf(front);
            Ops.disc(w, c, right, Y, 1.5, 12);
            Fx.vel(w, "t_gold", c.add(right.multiply(Math.cos(t) * 1.5)).add(0, Math.sin(t) * 1.5, 0), Vec3d.ZERO);
            for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, new Box(c, c).expand(3.4), x -> x.getOwner() != p)) {
                Fx.burst(w, "t_spark", pr.getPos(), 10, 0.2, 0.2);
                pr.discard();
            }
            if (t % 5 == 0) {
                for (LivingEntity e : Ops.around(w, c, 2.8, p)) {
                    if (Ops.mid(e).subtract(p.getPos()).normalize().dotProduct(front) < 0.2) continue;
                    Ops.hit(w, e, p, 2.5f);
                    Ops.push(e, front, 0.7, 0.15);
                    Fx.burst(w, "t_spark", Ops.mid(e), 6, 0.2, 0.2);
                }
            }
            return true;
        });
        return true;
    }

    // 5. two linked holes: whoever steps into the first comes out of the second
    private static boolean twin(ServerWorld w, ServerPlayerEntity p) {
        Vec3d a = Ops.ground(w, Ops.aim(w, p, 6), p);
        Vec3d b = Ops.ground(w, Ops.aim(w, p, 20), p);
        if (a.distanceTo(b) < 5) b = Ops.ground(w, a.add(Ops.flat(p.getRotationVec(1f)).multiply(9)), p);
        Vec3d exit = b;
        Snd.play(w, a, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.9f);
        Map<Integer, Integer> last = new HashMap<>();
        Effects.add(t -> {
            if (t > 100 || !p.isAlive()) return false;
            if (t % 2 == 0) {
                Ops.disc(w, a.add(0, 0.08, 0), Ops.X, Ops.Z, 1.3, 10);
                Ops.disc(w, exit.add(0, 0.08, 0), Ops.X, Ops.Z, 1.3, 10);
                Fx.circle(w, "t_gold", exit.add(0, 0.1, 0), 1.5, 8, t * 0.3, 0.03);
                Fx.circle(w, "t_heart", a.add(0, 0.1, 0), 1.5, 8, -t * 0.3, 0.03);
            }
            for (LivingEntity e : Ops.around(w, a.add(0, 0.6, 0), 1.5, p)) {
                if (t - last.getOrDefault(e.getId(), -99) < 20) continue;
                last.put(e.getId(), t);
                Fx.burst(w, "t_spin", Ops.mid(e), 16, 0.3, 0.3);
                Ops.hurt(w, e, p, 5f);
                Ops.tp(e, exit.add(0, 0.3, 0));
                e.fallDistance = 0;
                Fx.burst(w, "t_spin", exit.add(0, 1, 0), 16, 0.3, 0.3);
                Snd.play(w, exit, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.3f);
            }
            return true;
        });
        return true;
    }

    // 6. three small holes circle you and throw away anything they touch
    private static boolean orbit(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
        Map<Integer, Integer> last = new HashMap<>();
        Effects.add(t -> {
            if (t > 120 || !p.isAlive()) return false;
            Vec3d c = p.getPos().add(0, 1.0, 0);
            for (int k = 0; k < 3; k++) {
                double a = t * 0.22 + k * Math.PI * 2 / 3;
                Vec3d pos = c.add(Math.cos(a) * 2.6, 0.3 * Math.sin(t * 0.2 + k), Math.sin(a) * 2.6);
                for (int i = 0; i < 2; i++) Fx.vel(w, "t_hole", pos.add(Fx.jitter(0.12)), Vec3d.ZERO);
                for (int i = 0; i < 4; i++) {
                    double b = t * 0.6 + i * Math.PI / 2;
                    Fx.vel(w, "t_spin", pos.add(Math.cos(b) * 0.55, Math.sin(b) * 0.55, 0), Vec3d.ZERO);
                }
                for (LivingEntity e : Ops.around(w, pos, 1.2, p)) {
                    if (t - last.getOrDefault(e.getId(), -99) < 15) continue;
                    last.put(e.getId(), t);
                    Ops.hit(w, e, p, 5f);
                    Ops.push(e, e.getPos().subtract(p.getPos()), 0.9, 0.3);
                    Fx.burst(w, "t_spark", Ops.mid(e), 10, 0.25, 0.2);
                }
            }
            return true;
        });
        return true;
    }
}
