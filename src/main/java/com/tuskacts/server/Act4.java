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

/** ACT 4: the big humanoid Tusk. Physical fighting: a rush of punches, a heavy punch, an uppercut and slam, the infinite spin touch, a ground rift, a grab and throw. */
public final class Act4 {
    private Act4() {}

    private static final Random R = Fx.R;

    static boolean run(int slot, ServerWorld w, ServerPlayerEntity p) {
        return switch (slot) {
            case 0 -> rush(w, p);
            case 1 -> heavy(w, p);
            case 2 -> uppercut(w, p);
            case 3 -> spinTouch(w, p);
            case 4 -> rift(w, p);
            default -> grab(w, p);
        };
    }

    private static List<LivingEntity> cone(ServerWorld w, ServerPlayerEntity p, double range, double minDot) {
        Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : Ops.targets(w, new Box(eye, eye).expand(range), p)) {
            Vec3d to = Ops.mid(e).subtract(eye);
            if (to.length() <= range + e.getWidth() / 2 && (to.length() < 1.0 || to.normalize().dotProduct(look) >= minDot)) out.add(e);
        }
        return out;
    }

    // 1. ORA ORA ORA: twenty quick punches, then a closing blow
    private static boolean rush(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        Effects.add(t -> {
            if (t > 42 || !p.isAlive()) return false;
            Vec3d look = p.getRotationVec(1f).normalize();
            Vec3d right = Ops.rightOf(look);
            Vec3d front = p.getEyePos().add(look.multiply(2.6)).add(0, -0.4, 0);
            if (t <= 40 && t % 2 == 0) {
                double side = (t / 2) % 2 == 0 ? 1 : -1;
                Vec3d fist = front.add(right.multiply(side * 0.55)).add(Fx.jitter(0.15));
                Ops.line(w, "t_gold", p.getEyePos().add(look.multiply(1.1)).add(right.multiply(side * 0.5)).add(0, -0.5, 0), fist, 0.3, look.multiply(0.15));
                Fx.burst(w, "t_star", fist, 3, 0.12, 0.1);
                Fx.burst(w, "t_spark", fist, 4, 0.2, 0.1);
                for (LivingEntity e : cone(w, p, 4.4, 0.55)) {
                    Ops.hit(w, e, p, 1.7f);
                    Vec3d pull = front.subtract(Ops.mid(e));
                    e.addVelocity(pull.x * 0.05, 0, pull.z * 0.05);
                    e.velocityModified = true;
                }
                if (t % 4 == 0) Snd.play(w, front, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 0.9f + R.nextFloat() * 0.5f);
            }
            if (t == 42) {
                Snd.play(w, front, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.9f, 1.3f);
                Fx.burst(w, "t_gold", front, 40, 0.5, 0.2);
                Fx.burst(w, "t_star", front, 6, 0.2, 0.2);
                for (LivingEntity e : cone(w, p, 5.0, 0.5)) {
                    Ops.hurt(w, e, p, 8f);
                    Ops.push(e, look, 1.5, 0.4);
                }
            }
            return true;
        });
        return true;
    }

    // 2. a slow wind-up and one enormous punch
    private static boolean heavy(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.7f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 14, 3, true, false, false));
        Effects.add(t -> {
            if (t > 14 || !p.isAlive()) return false;
            Vec3d look = p.getRotationVec(1f).normalize();
            Vec3d c = p.getEyePos().add(look.multiply(1.6)).add(0, -0.5, 0);
            if (t <= 12) {
                for (int i = 0; i < 5; i++) {
                    Vec3d off = Fx.jitter(1.2);
                    Fx.vel(w, "t_gold", c.add(off), off.multiply(-0.1));
                }
                return true;
            }
            if (t == 13) {
                Snd.play(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.3f, 0.7f);
                Snd.play(w, c, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 1.5f, 0.6f);
                Ops.line(w, "t_gold", p.getEyePos().add(0, -0.5, 0), p.getEyePos().add(look.multiply(5.5)).add(0, -0.5, 0), 0.2, Vec3d.ZERO);
                for (int k = 1; k <= 5; k++) Fx.circle(w, "t_star", p.getEyePos().add(look.multiply(k)).add(0, -0.5, 0), 0.5 + k * 0.28, 12, k, 0);
                Fx.burst(w, "t_spark", c.add(look.multiply(2.5)), 40, 0.6, 0.2);
                Ops.ring(w, Ops.ground(w, p.getPos().add(look.multiply(2.5)), p).add(0, 0.1, 0), 4, 10);
                for (LivingEntity e : cone(w, p, 5.2, 0.6)) {
                    Ops.hurt(w, e, p, 20f);
                    Ops.push(e, look, 2.1, 0.55);
                    Fx.burst(w, "t_star", Ops.mid(e), 12, 0.3, 0.3);
                }
            }
            return true;
        });
        return true;
    }

    // 3. an uppercut that throws the enemy into the air, then a downward smash
    private static boolean uppercut(ServerWorld w, ServerPlayerEntity p) {
        LivingEntity target = Ops.aimEntity(w, p, 4.8, 0.5);
        if (target == null) return false;
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
        Ops.hurt(w, target, p, 8f);
        target.setVelocity(0, 1.5, 0);
        target.velocityModified = true;
        Fx.burst(w, "t_gold", target.getPos().add(0, 0.5, 0), 30, 0.2, 0.9);
        Effects.add(t -> {
            if (t > 24 || !p.isAlive() || !target.isAlive()) return false;
            if (t <= 11) {
                Fx.vel(w, "t_gold", Ops.mid(target).add(Fx.jitter(0.25)), Vec3d.ZERO);
                Fx.vel(w, "t_star", Ops.mid(target).add(Fx.jitter(0.25)), Vec3d.ZERO);
                return true;
            }
            if (t == 12) {
                Snd.play(w, Ops.mid(target), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.3f, 0.6f);
                Ops.hurt(w, target, p, 12f);
                target.setVelocity(0, -2.3, 0);
                target.velocityModified = true;
                Ops.line(w, "t_gold", Ops.mid(target).add(0, 3, 0), Ops.mid(target), 0.25, Vec3d.ZERO);
            }
            if (t > 13 && (target.isOnGround() || t == 24)) {
                Vec3d g = target.getPos();
                Snd.play(w, g, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                Fx.burst(w, "t_dust", g.add(0, 0.2, 0), 30, 0.4, 0.5);
                Ops.ring(w, g.add(0, 0.1, 0), 4, 10);
                Ops.blast(w, g, p, 3.4, 6f, 0.8, 0.35, null);
                return false;
            }
            return true;
        });
        return true;
    }

    // 4. the touch of infinite rotation: the enemy spins, is shredded and finally sent away
    private static boolean spinTouch(ServerWorld w, ServerPlayerEntity p) {
        LivingEntity target = Ops.aimEntity(w, p, 4.4, 0.7);
        if (target == null) return false;
        Snd.play(w, Ops.mid(target), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.3f, 1.8f);
        Fx.burst(w, "t_gold", Ops.mid(target), 30, 0.3, 0.3);
        Effects.add(t -> {
            if (t > 82 || !p.isAlive() || !target.isAlive()) return false;
            if (t <= 80) {
                if (target instanceof ServerPlayerEntity) {
                    if (t % 20 == 1) target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
                } else {
                    target.setYaw(target.getYaw() + 48f);
                    target.setHeadYaw(target.getYaw());
                    target.setBodyYaw(target.getYaw());
                }
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 4, true, false, false));
                target.setVelocity(target.getVelocity().multiply(0.3, 1, 0.3));
                target.velocityModified = true;
                for (int i = 0; i < 6; i++) {
                    double a = t * 0.8 + i * 1.05, y = ((t * 0.1 + i * 0.3) % Math.max(0.6, target.getHeight()));
                    Vec3d at = target.getPos().add(Math.cos(a) * 0.8, y, Math.sin(a) * 0.8);
                    Fx.vel(w, i % 2 == 0 ? "t_gold" : "t_spin", at, new Vec3d(-Math.sin(a) * 0.1, 0.02, Math.cos(a) * 0.1));
                }
                if (t % 4 == 0) Ops.hit(w, target, p, 1.5f);
            }
            if (t == 81) {
                Snd.play(w, Ops.mid(target), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
                Fx.burst(w, "t_hole", Ops.mid(target), 12, 0.4, 0.3);
                Fx.burst(w, "t_gold", Ops.mid(target), 50, 0.6, 0.3);
                Ops.hurt(w, target, p, 10f);
            }
            return true;
        });
        return true;
    }

    // 5. a fist into the ground: a jagged crack tears forward and throws everything on it
    private static boolean rift(ServerWorld w, ServerPlayerEntity p) {
        Vec3d dir = Ops.flat(p.getRotationVec(1f));
        Vec3d side = Ops.rightOf(dir);
        Vec3d start = p.getPos();
        Set<Integer> hit = new HashSet<>();
        Snd.play(w, start, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        Effects.add(t -> {
            if (t > 14 || !p.isAlive()) return false;
            Vec3d c = start.add(dir.multiply(1.2 + t * 1.0)).add(side.multiply(Math.sin(t * 1.4) * 0.8));
            Vec3d g = Ops.ground(w, c, p);
            Fx.burst(w, "t_dust", g.add(0, 0.2, 0), 10, 0.25, 0.5);
            for (int i = 0; i < 4; i++) Fx.vel(w, "t_hole", g.add(Fx.jitter(0.45)).add(0, 0.1, 0), Vec3d.ZERO);
            Fx.vel(w, "t_gold", g.add(side.multiply(1.3)).add(0, 0.3, 0), new Vec3d(0, 0.12, 0));
            Fx.vel(w, "t_gold", g.subtract(side.multiply(1.3)).add(0, 0.3, 0), new Vec3d(0, 0.12, 0));
            if (t % 3 == 0) Snd.play(w, g, SoundEvents.BLOCK_STONE_BREAK, 1.0f, 0.6f);
            for (LivingEntity e : Ops.around(w, g.add(0, 0.8, 0), 1.9, p)) {
                if (!hit.add(e.getId())) continue;
                Ops.hurt(w, e, p, 9f);
                Ops.push(e, e.getPos().subtract(g), 0.5, 0.8);
                Ops.impact(w, Ops.mid(e), 12);
            }
            if (t == 14) {
                Fx.burst(w, "t_gold", g.add(0, 0.5, 0), 30, 0.5, 0.6);
                Ops.ring(w, g.add(0, 0.1, 0), 4, 10);
            }
            return true;
        });
        return true;
    }

    // 6. grab the enemy, lift and spin it, then throw it away; it explodes on landing
    private static boolean grab(ServerWorld w, ServerPlayerEntity p) {
        LivingEntity target = Ops.aimEntity(w, p, 4.4, 0.6);
        if (target == null) return false;
        Snd.play(w, Ops.mid(target), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.7f);
        boolean[] thrown = {false};
        Effects.add(t -> {
            if (t > 60 || !p.isAlive() || !target.isAlive()) return false;
            Vec3d look = p.getRotationVec(1f).normalize();
            if (t <= 18) {
                double a = t * 0.5;
                Vec3d hold = p.getPos().add(Ops.flat(look).multiply(1.5)).add(0, 0.3 + t * 0.12, 0);
                Ops.tp(target, hold.add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6));
                target.setVelocity(Vec3d.ZERO);
                target.fallDistance = 0;
                target.velocityModified = true;
                Fx.circle(w, "t_gold", p.getPos().add(Ops.flat(look).multiply(1.5)).add(0, 0.2 + t * 0.1, 0), 0.9, 6, a, 0.05);
                Fx.vel(w, "t_star", Ops.mid(target).add(Fx.jitter(0.3)), Vec3d.ZERO);
                return true;
            }
            if (t == 19) {
                thrown[0] = true;
                Snd.play(w, Ops.mid(target), SoundEvents.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);
                target.setVelocity(look.x * 2.3, 0.35, look.z * 2.3);
                target.velocityModified = true;
                Ops.hurt(w, target, p, 6f);
            }
            Fx.vel(w, "t_gold", Ops.mid(target), Vec3d.ZERO);
            Fx.vel(w, "t_spin", Ops.mid(target).add(Fx.jitter(0.2)), Vec3d.ZERO);
            if (thrown[0] && t > 22 && (target.isOnGround() || t >= 58 || target.horizontalCollision)) {
                Vec3d g = target.getPos();
                Snd.play(w, g, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);
                Fx.burst(w, "t_dust", g.add(0, 0.2, 0), 36, 0.5, 0.5);
                Fx.burst(w, "t_star", g.add(0, 0.5, 0), 8, 0.2, 0.3);
                Ops.ring(w, g.add(0, 0.1, 0), 4, 10);
                Ops.blast(w, g.add(0, 0.5, 0), p, 3.4, 14f, 1.0, 0.5, null);
                return false;
            }
            return true;
        });
        return true;
    }
}
