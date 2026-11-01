package com.tuskacts.server;

import com.tuskacts.particle.Fx;
import com.tuskacts.util.Effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.Random;

/** Small helpers shared by every attack. */
public final class Ops {
    private Ops() {}

    public static final Random R = Fx.R;

    public interface Trail { void at(Vec3d pos, Vec3d dir, int t); }
    public interface Impact { void hit(Vec3d pos, LivingEntity target); }

    public static Vec3d rightOf(Vec3d look) {
        Vec3d r = look.crossProduct(new Vec3d(0, 1, 0));
        return r.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : r.normalize();
    }

    public static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0, v.z);
        return f.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : f.normalize();
    }

    public static Vec3d mid(LivingEntity e) {
        return e.getPos().add(0, e.getHeight() / 2, 0);
    }

    public static Vec3d hand(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f);
        return p.getEyePos().add(look.multiply(0.9)).add(rightOf(look).multiply(0.4)).add(0, -0.3, 0);
    }

    public static List<LivingEntity> targets(ServerWorld w, Box box, ServerPlayerEntity owner) {
        return w.getEntitiesByClass(LivingEntity.class, box, e -> e != owner && e.isAlive() && !e.isSpectator() && !e.isTeammate(owner));
    }

    public static List<LivingEntity> around(ServerWorld w, Vec3d c, double r, ServerPlayerEntity owner) {
        return targets(w, new Box(c, c).expand(r), owner).stream().filter(e -> mid(e).distanceTo(c) <= r + e.getWidth() / 2).toList();
    }

    public static void hurt(ServerWorld w, LivingEntity t, ServerPlayerEntity p, float amount) {
        t.damage(w.getDamageSources().playerAttack(p), amount);
    }

    /** Damage that ignores the invulnerability frames, for multi-hit attacks. */
    public static void hit(ServerWorld w, LivingEntity t, ServerPlayerEntity p, float amount) {
        t.timeUntilRegen = 0;
        hurt(w, t, p, amount);
    }

    public static void push(LivingEntity e, Vec3d dir, double horiz, double up) {
        Vec3d f = flat(dir);
        e.addVelocity(f.x * horiz, up, f.z * horiz);
        e.velocityModified = true;
    }

    public static void impact(ServerWorld w, Vec3d at, int n) {
        Fx.burst(w, "t_spark", at, n, 0.3, 0.3);
        Fx.burst(w, "t_star", at, Math.max(2, n / 4), 0.12, 0.2);
    }

    public static void line(ServerWorld w, String type, Vec3d a, Vec3d b, double step, Vec3d vel) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) return;
        Vec3d u = d.multiply(1.0 / len);
        for (double t = 0; t <= len; t += step) Fx.vel(w, type, a.add(u.multiply(t)), vel);
    }

    /** Heart outline of cubes in the plane (right, up) around c. */
    public static void heart(ServerWorld w, String type, Vec3d c, Vec3d right, Vec3d up, double scale, Vec3d vel) {
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28;
            double x = 16 * Math.pow(Math.sin(a), 3);
            double y = 13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a) - Math.cos(4 * a);
            Fx.vel(w, type, c.add(right.multiply(x * scale)).add(up.multiply(y * scale)), vel);
        }
    }

    public static Vec3d aim(ServerWorld w, ServerPlayerEntity p, double range) {
        Vec3d eye = p.getEyePos(), end = eye.add(p.getRotationVec(1f).multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        return bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
    }

    public static Vec3d ground(ServerWorld w, Vec3d at, ServerPlayerEntity p) {
        BlockHitResult bh = w.raycast(new RaycastContext(at.add(0, 2, 0), at.add(0, -14, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        return bh.getType() == HitResult.Type.MISS ? at : bh.getPos();
    }

    /** Nearest living entity inside a cone in front of the player. */
    public static LivingEntity aimEntity(ServerWorld w, ServerPlayerEntity p, double range, double minDot) {
        Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
        LivingEntity best = null;
        double bd = 1e9;
        for (LivingEntity e : targets(w, new Box(eye, eye.add(look.multiply(range))).expand(3), p)) {
            Vec3d to = mid(e).subtract(eye);
            double d = to.length();
            if (d < 0.3 || d > range || to.normalize().dotProduct(look) < minDot) continue;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    public static boolean freeAt(ServerWorld w, Vec3d feet) {
        BlockPos a = BlockPos.ofFloored(feet), b = a.up();
        return w.getBlockState(a).getCollisionShape(w, a).isEmpty() && w.getBlockState(b).getCollisionShape(w, b).isEmpty();
    }

    public static Vec3d walkFree(ServerWorld w, Vec3d start, Vec3d end) {
        int steps = (int) Math.max(1, Math.ceil(start.distanceTo(end) / 0.5));
        Vec3d last = start;
        for (int i = 1; i <= steps; i++) {
            Vec3d q = start.add(end.subtract(start).multiply((double) i / steps));
            if (!freeAt(w, q)) break;
            last = q;
        }
        return last;
    }

    public static void tp(LivingEntity e, Vec3d at) {
        if (e instanceof ServerPlayerEntity sp) sp.requestTeleport(at.x, at.y, at.z);
        else e.refreshPositionAfterTeleport(at.x, at.y, at.z);
    }

    /** Flies a bolt in a straight line; stops on a block or the first target and calls impact. */
    public static void bolt(ServerWorld w, ServerPlayerEntity owner, Vec3d start, Vec3d dir, double speed, int maxTicks,
                            double radius, boolean endImpact, Trail trail, Impact impact) {
        Vec3d step = dir.normalize().multiply(speed);
        Vec3d[] pos = {start};
        Effects.add(t -> {
            if (t > maxTicks || !owner.isAlive()) {
                if (endImpact) impact.hit(pos[0], null);
                return false;
            }
            Vec3d old = pos[0], next = old.add(step);
            BlockHitResult bh = w.raycast(new RaycastContext(old, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, owner));
            boolean block = bh.getType() != HitResult.Type.MISS;
            Vec3d at = block ? bh.getPos().subtract(step.normalize().multiply(0.3)) : next;
            List<LivingEntity> near = targets(w, new Box(old, next).expand(radius), owner);
            if (block || !near.isEmpty()) {
                impact.hit(at, near.isEmpty() ? null : near.get(0));
                return false;
            }
            pos[0] = next;
            trail.at(next, step, t);
            return true;
        });
    }

    /** The spinning nail: a bright core with a small helix of pink cubes turning around it. */
    public static void nailTrail(ServerWorld w, Vec3d pos, Vec3d dir, int t, double radius) {
        Vec3d d = dir.normalize();
        Vec3d r = rightOf(d), u = r.crossProduct(d).normalize();
        double a = t * 1.7;
        Fx.vel(w, "t_nail", pos, Vec3d.ZERO);
        Fx.vel(w, "t_spin", pos.add(r.multiply(Math.cos(a) * radius)).add(u.multiply(Math.sin(a) * radius)), Vec3d.ZERO);
        Fx.vel(w, "t_spin", pos.add(r.multiply(-Math.cos(a) * radius)).add(u.multiply(-Math.sin(a) * radius)), Vec3d.ZERO);
    }

    public static int blast(ServerWorld w, Vec3d c, ServerPlayerEntity owner, double radius, float dmg, double force, double up,
                            java.util.function.Consumer<LivingEntity> extra) {
        int n = 0;
        for (LivingEntity e : around(w, c, radius, owner)) {
            double d = mid(e).distanceTo(c);
            float k = (float) (1.0 - 0.5 * d / radius);
            hurt(w, e, owner, dmg * k);
            Vec3d away = e.getPos().subtract(c);
            push(e, away, force * k, up * k);
            if (extra != null) extra.accept(e);
            n++;
        }
        return n;
    }

    /** Flat ring of cubes on the ground that spreads out. */
    public static void ring(ServerWorld w, Vec3d c, double r, int ticks) {
        Effects.groundRing(w, c, r, ticks, "t_ring", "t_spark");
    }

    /** A dark hole: a disc of cubes lying on the ground (or standing, using the given plane axes). */
    public static void disc(ServerWorld w, Vec3d c, Vec3d ax1, Vec3d ax2, double r, int count) {
        for (int i = 0; i < count; i++) {
            double a = R.nextDouble() * Math.PI * 2, d = Math.sqrt(R.nextDouble()) * r;
            Fx.vel(w, "t_hole", c.add(ax1.multiply(Math.cos(a) * d)).add(ax2.multiply(Math.sin(a) * d)), Vec3d.ZERO);
        }
        for (int i = 0; i < Math.max(8, (int) (r * 12)); i++) {
            double a = Math.PI * 2 * i / Math.max(8, (int) (r * 12));
            Fx.vel(w, "t_spin", c.add(ax1.multiply(Math.cos(a) * r)).add(ax2.multiply(Math.sin(a) * r)), Vec3d.ZERO);
        }
    }

    public static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
}
