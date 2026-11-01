package com.tuskacts.particle;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Server-side helpers that spawn the 3D cube particles. */
public final class Fx {
    private Fx() {}

    public static final Random R = new Random();

    /** Particle density from the config (0.2 .. 2.0): lets slow computers thin the effects out. */
    public static double density() {
        return 1.0;
    }

    /** True most of the time at density 1; skips some particles at lower density. */
    public static boolean keep() {
        double d = 1.0;
        return d >= 1.0 || R.nextDouble() < d;
    }

    /** One cube with an exact velocity. */
    public static void vel(ServerWorld w, String type, double x, double y, double z, double vx, double vy, double vz) {
        if (!keep()) return;
        w.spawnParticles(ModParticles.get(type), x, y, z, 0, vx, vy, vz, 1.0);
    }

    public static void vel(ServerWorld w, String type, Vec3d p, Vec3d v) {
        vel(w, type, p.x, p.y, p.z, v.x, v.y, v.z);
    }

    public static Vec3d jitter(double s) {
        return new Vec3d(R.nextGaussian() * s, R.nextGaussian() * s, R.nextGaussian() * s);
    }

    /** Cubes flying out from a point in every direction (up = upward lean, 0..1). */
    public static void burst(ServerWorld w, String type, Vec3d c, int count, double speed, double up) {
        for (int i = 0; i < count; i++) {
            double a = R.nextDouble() * Math.PI * 2;
            double elev = (R.nextDouble() * 2 - 1 + up) * 0.7;
            double sp = speed * (0.3 + R.nextDouble() * 0.9);
            vel(w, type, c.x, c.y, c.z, Math.cos(a) * Math.cos(elev) * sp, Math.sin(elev) * sp, Math.sin(a) * Math.cos(elev) * sp);
        }
    }

    /** Cubes on the surface of a sphere (static shell). */
    public static void sphere(ServerWorld w, String type, Vec3d c, double r, int count, Vec3d drift) {
        for (int i = 0; i < count; i++) {
            double u = R.nextDouble() * 2 - 1, a = R.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
            vel(w, type, c.x + r * s * Math.cos(a), c.y + r * u, c.z + r * s * Math.sin(a), drift.x, drift.y, drift.z);
        }
    }

    /** Horizontal circle of cubes. */
    public static void circle(ServerWorld w, String type, Vec3d c, double r, int count, double phase, double vUp) {
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2 * i / count;
            vel(w, type, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, vUp, 0);
        }
    }
}
