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

/** ACT 2: sturdier Tusk. Drilling nails, holes that chase their prey, spiral nails, mines and a huge drill. */
public final class Act2 {
    private Act2() {}

    private static final Random R = Fx.R;

    static boolean run(int slot, ServerWorld w, ServerPlayerEntity p) {
        return switch (slot) {
            case 0 -> drill(w, p);
            case 1 -> hole(w, p);
            case 2 -> spiral(w, p);
            case 3 -> mines(w, p);
            case 4 -> trench(w, p);
            default -> overdrill(w, p);
        };
    }

    // 1. a drilling nail that blasts a crater
    private static boolean drill(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        Ops.bolt(w, p, Ops.hand(p), p.getRotationVec(1f), 2.0, 10, 0.5, true,
                (pos, dir, t) -> {
                    Ops.nailTrail(w, pos, dir, t, 0.25);
                    Ops.nailTrail(w, pos.subtract(dir.normalize().multiply(0.4)), dir, t + 3, 0.18);
                },
                (pos, target) -> {
                    Snd.play(w, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.9f, 1.4f);
                    Fx.burst(w, "t_dust", pos, 28, 0.35, 0.5);
                    Fx.burst(w, "t_star", pos, 4, 0.1, 0.2);
                    Ops.ring(w, pos.add(0, 0.1, 0), 3, 8);
                    Ops.blast(w, pos, p, 2.8, 9f, 0.9, 0.35, null);
                });
        return true;
    }

    // 2. the nail leaves a hole that hunts the nearest enemy for eight seconds
    private static boolean hole(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 0.9f);
        Ops.bolt(w, p, Ops.hand(p), p.getRotationVec(1f), 2.0, 9, 0.5, true,
                (pos, dir, t) -> Ops.nailTrail(w, pos, dir, t, 0.2),
                (pos, target) -> {
                    if (target != null) Ops.hurt(w, target, p, 6f);
                    Fx.burst(w, "t_hole", pos, 6, 0.15, 0.1);
                    chase(w, p, pos);
                });
        return true;
    }

    private static void chase(ServerWorld w, ServerPlayerEntity p, Vec3d start) {
        Vec3d[] h = {start};
        LivingEntity[] tgt = {null};
        Effects.add(t -> {
            if (t > 160 || !p.isAlive()) return false;
            if (tgt[0] == null || !tgt[0].isAlive() || tgt[0].getPos().distanceTo(h[0]) > 24) {
                tgt[0] = null;
                double bd = 18;
                for (LivingEntity e : Ops.targets(w, new Box(h[0], h[0]).expand(18), p)) {
                    double d = Ops.mid(e).distanceTo(h[0]);
                    if (d < bd) {
                        bd = d;
                        tgt[0] = e;
                    }
                }
            }
            double size = t > 120 ? 1.0 - (t - 120) / 40.0 : 1.0;
            for (int i = 0; i < 2; i++) Fx.vel(w, "t_hole", h[0].add(Fx.jitter(0.14 * size)), Vec3d.ZERO);
            double a = t * 0.5;
            Fx.vel(w, "t_spin", h[0].add(Math.cos(a) * 0.55 * size, Math.sin(a) * 0.55 * size, Math.sin(a * 0.7) * 0.3), Vec3d.ZERO);
            Fx.vel(w, "t_spin", h[0].add(-Math.cos(a) * 0.55 * size, -Math.sin(a) * 0.55 * size, 0), Vec3d.ZERO);
            if (tgt[0] != null) {
                Vec3d to = Ops.mid(tgt[0]).subtract(h[0]);
                double d = to.length();
                if (d < 1.2) {
                    if (t % 8 == 0) {
                        Ops.hit(w, tgt[0], p, 3.5f);
                        tgt[0].addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0));
                        Fx.burst(w, "t_heart", Ops.mid(tgt[0]), 6, 0.15, 0.1);
                    }
                } else {
                    Vec3d old = h[0];
                    h[0] = old.add(to.multiply(Math.min(0.38, d) / d));
                    Ops.line(w, "t_hole", old, h[0], 0.3, Vec3d.ZERO);
                }
            }
            return true;
        });
    }

    // 3. three nails fly one after the other along a corkscrew path
    private static boolean spiral(ServerWorld w, ServerPlayerEntity p) {
        Effects.add(t -> {
            if (t > 14 || !p.isAlive()) return false;
            if (t == 1 || t == 6 || t == 11) helix(w, p);
            return true;
        });
        return true;
    }

    private static void helix(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 0.9f, 1.4f);
        Vec3d dir = p.getRotationVec(1f).normalize();
        Vec3d right = Ops.rightOf(dir), up = right.crossProduct(dir).normalize();
        Vec3d[] pos = {Ops.hand(p)};
        Set<Integer> hit = new HashSet<>();
        Effects.add(t -> {
            if (t > 14 || !p.isAlive()) return false;
            Vec3d next = pos[0].add(dir.multiply(1.4));
            BlockHitResult bh = w.raycast(new RaycastContext(pos[0], next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (bh.getType() != HitResult.Type.MISS) {
                Ops.impact(w, bh.getPos(), 14);
                return false;
            }
            pos[0] = next;
            for (int k = 0; k < 3; k++) {
                double a = t * 0.9 - k * 0.45;
                Vec3d at = next.add(right.multiply(Math.cos(a) * 0.9)).add(up.multiply(Math.sin(a) * 0.9));
                Fx.vel(w, k == 0 ? "t_nail" : "t_spin", at, Vec3d.ZERO);
            }
            Fx.vel(w, "t_gold", next, Vec3d.ZERO);
            for (LivingEntity e : Ops.targets(w, new Box(next, next).expand(1.3), p)) {
                if (!hit.add(e.getId())) continue;
                Ops.hit(w, e, p, 7f);
                Ops.push(e, dir, 0.5, 0.2);
                Ops.impact(w, Ops.mid(e), 10);
            }
            return true;
        });
    }

    // 4. four nails planted in the ground; whoever steps near is lifted by a drilling pillar
    private static boolean mines(ServerWorld w, ServerPlayerEntity p) {
        Vec3d f = Ops.flat(p.getRotationVec(1f));
        Snd.play(w, p.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, 1.2f, 0.7f);
        for (int k = 0; k < 4; k++) {
            Vec3d dir = f.rotateY((float) Math.toRadians((k - 1.5) * 26.0));
            Vec3d g = Ops.ground(w, p.getPos().add(dir.multiply(5.0)), p);
            boolean[] gone = {false};
            Effects.add(t -> {
                if (t > 200 || gone[0] || !p.isAlive()) return false;
                if (t % 4 == 0) {
                    Fx.vel(w, "t_gold", g.add(0, 0.25 + 0.2 * Math.sin(t * 0.3), 0), Vec3d.ZERO);
                    Fx.circle(w, "t_spin", g.add(0, 0.1, 0), 0.45, 5, t * 0.5, 0);
                }
                List<LivingEntity> near = Ops.around(w, g.add(0, 0.5, 0), 1.9, p);
                if (near.isEmpty()) return true;
                gone[0] = true;
                Snd.play(w, g, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                for (double h = 0; h <= 4.2; h += 0.35) {
                    Fx.circle(w, "t_spin", g.add(0, h, 0), 0.5 + 0.08 * h, 6, h * 2, 0.02);
                    Fx.vel(w, "t_gold", g.add(0, h, 0), Vec3d.ZERO);
                }
                Fx.burst(w, "t_dust", g.add(0, 0.2, 0), 22, 0.3, 0.5);
                Ops.blast(w, g.add(0, 0.5, 0), p, 2.4, 8f, 0.3, 0.9, null);
                return false;
            });
        }
        return true;
    }

    // 5. a drill that bores a winding trench forward
    private static boolean trench(ServerWorld w, ServerPlayerEntity p) {
        Vec3d dir = Ops.flat(p.getRotationVec(1f));
        Vec3d side = Ops.rightOf(dir);
        Vec3d start = p.getPos();
        Set<Integer> hit = new HashSet<>();
        Snd.play(w, start, SoundEvents.BLOCK_GRAVEL_BREAK, 1.5f, 0.5f);
        Effects.add(t -> {
            if (t > 11 || !p.isAlive()) return false;
            Vec3d c = start.add(dir.multiply(1.5 + t * 0.9)).add(side.multiply(Math.sin(t * 0.9) * 0.5));
            Vec3d g = Ops.ground(w, c, p);
            Fx.burst(w, "t_dust", g.add(0, 0.2, 0), 8, 0.2, 0.35);
            Fx.circle(w, "t_hole", g.add(0, 0.05, 0), 0.6, 8, t, 0);
            Fx.vel(w, "t_spin", g.add(0, 0.4, 0), new Vec3d(0, 0.1, 0));
            for (LivingEntity e : Ops.around(w, g.add(0, 0.8, 0), 1.6, p)) {
                if (!hit.add(e.getId())) continue;
                Ops.hurt(w, e, p, 7f);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2));
                e.addVelocity(0, -0.2, 0);
                Ops.impact(w, Ops.mid(e), 10);
            }
            return true;
        });
        return true;
    }

    // 6. a long charge, then a huge drill nail that bores through everything
    private static boolean overdrill(ServerWorld w, ServerPlayerEntity p) {
        Snd.play(w, p.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.6f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 16, 3, true, false, false));
        Set<Integer> hit = new HashSet<>();
        Vec3d[] pos = {null};
        Vec3d[] dirHold = {null};
        Effects.add(t -> {
            if (t > 32 || !p.isAlive()) return false;
            if (t <= 12) {
                Vec3d c = Ops.hand(p).add(p.getRotationVec(1f).multiply(0.8));
                for (int i = 0; i < 4; i++) {
                    Vec3d off = Fx.jitter(1.0);
                    Fx.vel(w, i % 2 == 0 ? "t_gold" : "t_spin", c.add(off), off.multiply(-0.12));
                }
                return true;
            }
            if (pos[0] == null) {
                pos[0] = Ops.hand(p);
                dirHold[0] = p.getRotationVec(1f).normalize();
                Snd.play(w, pos[0], SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 1.4f);
            }
            Vec3d dir = dirHold[0];
            Vec3d right = Ops.rightOf(dir), up = right.crossProduct(dir).normalize();
            Vec3d next = pos[0].add(dir.multiply(2.4));
            BlockHitResult bh = w.raycast(new RaycastContext(pos[0], next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (bh.getType() != HitResult.Type.MISS) {
                Vec3d at = bh.getPos();
                Snd.play(w, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);
                Fx.burst(w, "t_dust", at, 50, 0.5, 0.6);
                Fx.burst(w, "t_star", at, 8, 0.2, 0.3);
                Ops.ring(w, at.add(0, 0.1, 0), 5, 12);
                Ops.blast(w, at, p, 5.0, 12f, 1.4, 0.5, null);
                return false;
            }
            pos[0] = next;
            for (int k = 0; k < 3; k++) {
                double a = t * 1.1 + k * Math.PI * 2 / 3;
                Fx.vel(w, "t_spin", next.add(right.multiply(Math.cos(a) * 0.8)).add(up.multiply(Math.sin(a) * 0.8)), Vec3d.ZERO);
                Fx.vel(w, "t_gold", next.subtract(dir.multiply(1.0)).add(right.multiply(Math.cos(a + 1) * 0.5)).add(up.multiply(Math.sin(a + 1) * 0.5)), Vec3d.ZERO);
            }
            Fx.vel(w, "t_star", next, Vec3d.ZERO);
            for (LivingEntity e : Ops.targets(w, new Box(pos[0].subtract(dir.multiply(2.4)), next).expand(1.8), p)) {
                if (!hit.add(e.getId())) continue;
                Ops.hurt(w, e, p, 16f);
                Ops.push(e, dir, 1.0, 0.3);
                Ops.impact(w, Ops.mid(e), 16);
            }
            return true;
        });
        return true;
    }
}
