package com.tuskacts.server;

import com.tuskacts.particle.Fx;
import com.tuskacts.util.Effects;
import com.tuskacts.util.Snd;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

/** ACT 1: small floating Tusk. Weak, quick nail attacks: single shot, claws, fan, hovering propellers, a jump and a cute rush. */
public final class Act1 {
    private Act1() {}

    private static final Random R = Fx.R;

    static boolean run(int slot, ServerWorld w, ServerPlayerEntity p) {
        return switch (slot) {
            case 0 -> shot(w, p);
            case 1 -> cutter(w, p);
            case 2 -> volley(w, p);
            case 3 -> propeller(w, p);
            case 4 -> jump(w, p);
            default -> chumimi(w, p);
        };
    }

    // 1. one fast spinning nail
    private static boolean shot(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 0.8f, 1.9f);
        Ops.bolt(w, p, Ops.hand(p), p.getRotationVec(1f), 1.8, 6, 0.4, true,
                (pos, dir, t) -> Ops.nailTrail(w, pos, dir, t, 0.1),
                (pos, target) -> {
                    Ops.impact(w, pos, 10);
                    if (target != null) Ops.hurt(w, target, p, 5.5f);
                });
        return true;
    }

    // 2. Finger Cutter: three claw cuts sweeping downwards in front of you
    private static boolean cutter(ServerWorld w, ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f).normalize();
        Vec3d right = Ops.rightOf(look), up = right.crossProduct(look).normalize();
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.9f);
        Effects.add(t -> {
            if (t > 3 || !p.isAlive()) return false;
            Vec3d eye = p.getEyePos().add(0, -0.2, 0);
            for (int k = -1; k <= 1; k++) {
                Vec3d lat = right.multiply(k * 0.55);
                double drop = 0.7 - t * 0.45;
                Vec3d a = eye.add(look.multiply(0.8)).add(lat).add(up.multiply(drop + 0.5));
                Vec3d b = eye.add(look.multiply(3.6)).add(lat.multiply(1.6)).add(up.multiply(drop - 0.9));
                Ops.line(w, "t_spin", a, b, 0.22, Vec3d.ZERO);
                Fx.vel(w, "t_nail", b, Vec3d.ZERO);
            }
            return true;
        });
        for (LivingEntity e : Ops.targets(w, new Box(p.getEyePos(), p.getEyePos()).expand(4), p)) {
            Vec3d to = Ops.mid(e).subtract(p.getEyePos());
            if (to.length() > 3.9 || to.normalize().dotProduct(look) < 0.45) continue;
            Ops.hurt(w, e, p, 7f);
            Ops.push(e, to, 0.4, 0.1);
            Ops.impact(w, Ops.mid(e), 10);
        }
        return true;
    }

    // 3. five nails in a flat fan
    private static boolean volley(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
        Vec3d look = p.getRotationVec(1f);
        for (int i = -2; i <= 2; i++) {
            Vec3d dir = look.rotateY((float) Math.toRadians(i * 12.0)).add(0, (R.nextDouble() - 0.5) * 0.04, 0);
            Ops.bolt(w, p, Ops.hand(p), dir, 1.6, 7, 0.35, true,
                    (pos, d, t) -> Ops.nailTrail(w, pos, d, t, 0.08),
                    (pos, target) -> {
                        Ops.impact(w, pos, 6);
                        if (target != null) Ops.hurt(w, target, p, 3.5f);
                    });
        }
        return true;
    }

    // 4. two nails hover in place like propellers, cutting whatever comes near
    private static boolean propeller(ServerWorld w, ServerPlayerEntity p) {
        Vec3d c = Ops.aim(w, p, 7).subtract(p.getRotationVec(1f).multiply(0.4));
        Snd.play(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
        Map<Integer, Integer> last = new HashMap<>();
        Effects.add(t -> {
            if (t > 44 || !p.isAlive()) return false;
            for (int level = 0; level < 2; level++) {
                double h = 0.5 + level * 0.7;
                double a = t * (0.9 + level * 0.25) + level;
                for (int b = 0; b < 2; b++) {
                    Vec3d dir = new Vec3d(Math.cos(a + b * Math.PI), 0, Math.sin(a + b * Math.PI));
                    Ops.line(w, level == 0 ? "t_spin" : "t_nail", c.add(0, h, 0).subtract(dir.multiply(1.3)), c.add(0, h, 0).add(dir.multiply(1.3)), 0.3, Vec3d.ZERO);
                }
            }
            Fx.vel(w, "t_gold", c.add(0, 0.9, 0), Fx.jitter(0.03));
            for (LivingEntity e : Ops.around(w, c.add(0, 0.9, 0), 1.9, p)) {
                Vec3d in = c.subtract(e.getPos());
                e.addVelocity(in.x * 0.04, 0.0, in.z * 0.04);
                e.velocityModified = true;
                if (t - last.getOrDefault(e.getId(), -99) >= 6) {
                    last.put(e.getId(), t);
                    Ops.hit(w, e, p, 2.5f);
                    Ops.impact(w, Ops.mid(e), 5);
                }
            }
            return true;
        });
        return true;
    }

    // 5. Nails pushed against the ground throw Johnny forward and up
    private static boolean jump(ServerWorld w, ServerPlayerEntity p) {
        Vec3d f = Ops.flat(p.getRotationVec(1f));
        Vec3d at = p.getPos();
        Snd.play(w, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5f, 1.8f);
        p.setVelocity(f.x * 0.95, 0.65, f.z * 0.95);
        p.velocityModified = true;
        Fx.burst(w, "t_dust", at.add(0, 0.2, 0), 24, 0.25, 0.4);
        Ops.ring(w, at.add(0, 0.1, 0), 3, 8);
        for (LivingEntity e : Ops.around(w, at, 2.6, p)) {
            Ops.hurt(w, e, p, 4f);
            Ops.push(e, e.getPos().subtract(at), 0.6, 0.35);
        }
        Effects.add(t -> {
            if (t > 18 || !p.isAlive()) return false;
            p.fallDistance = 0;
            Vec3d b = p.getPos().add(0, 0.3, 0);
            for (int i = 0; i < 4; i++) Fx.vel(w, i % 2 == 0 ? "t_spin" : "t_nail", b.add(Fx.jitter(0.25)), new Vec3d(0, -0.05, 0));
            return true;
        });
        return true;
    }

    // 6. "Chumimi~n!": Tusk pokes the target again and again, then a heart pops
    private static boolean chumimi(ServerWorld w, ServerPlayerEntity p) {
        LivingEntity t0 = Ops.aimEntity(w, p, 14, 0.75);
        if (t0 == null) return false;
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.5f, 1.8f);
        Vec3d look = p.getRotationVec(1f).normalize();
        Vec3d right = Ops.rightOf(look), up = right.crossProduct(look).normalize();
        Effects.add(t -> {
            if (t > 26 || !p.isAlive() || !t0.isAlive()) return false;
            Vec3d m = Ops.mid(t0);
            if (t % 3 == 0 && t < 24) {
                Vec3d spot = m.add(Fx.jitter(0.3));
                Ops.line(w, "t_nail", Ops.hand(p), spot, 0.35, Vec3d.ZERO);
                Fx.burst(w, "t_star", spot, 5, 0.12, 0.1);
                Ops.hit(w, t0, p, 2.5f);
                Snd.play(w, spot, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.6f + R.nextFloat() * 0.4f);
            }
            if (t == 26) {
                Ops.heart(w, "t_heart", m.add(0, 0.6, 0), right, up, 0.07, Fx.jitter(0.01));
                Fx.burst(w, "t_heart", m, 14, 0.25, 0.2);
                Ops.hurt(w, t0, p, 5f);
                Ops.push(t0, m.subtract(p.getPos()), 0.8, 0.3);
            }
            return true;
        });
        return true;
    }
}
