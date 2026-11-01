package com.tuskacts.particle;

import com.tuskacts.TuskActs;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cube particles of Tusk. Names match assets/tuskacts/particles/<name>.json. */
public final class ModParticles {
    private ModParticles() {}

    public static final Map<String, CubeProfile> PROFILES = new LinkedHashMap<>();
    public static final Map<String, SimpleParticleType> TYPES = new LinkedHashMap<>();

    static {
        //                                    young     middle    old       size   var    life var grav   drag   shrink spin  flat  alpha inner
        PROFILES.put("t_nail",  new CubeProfile(0xFFFFFF, 0xFFE9A0, 0xE0A030, 0.14f, 0.03f, 8,  2, 0.0f,   0.88f, 0.920f, 10f, 0.35f, 1.00f, true));
        PROFILES.put("t_spin",  new CubeProfile(0xFFE0F0, 0xFF7AB8, 0xB02070, 0.16f, 0.04f, 10, 3, 0.0f,   0.90f, 0.930f, 30f, 1f,    0.90f, true));
        PROFILES.put("t_star",  new CubeProfile(0xFFFFFF, 0xFFF08A, 0xFFC42A, 0.22f, 0.08f, 12, 4, 0.0f,   0.90f, 0.940f, 16f, 1f,    0.95f, true));
        PROFILES.put("t_hole",  new CubeProfile(0x3A1250, 0x1A0626, 0x050008, 0.48f, 0.14f, 10, 3, 0.0f,   0.92f, 0.950f, 8f,  1f,    0.90f, false));
        PROFILES.put("t_spark", new CubeProfile(0xFFFFFF, 0xFFB8DC, 0xFF5AA0, 0.08f, 0.03f, 14, 5, 0.02f,  0.92f, 0.970f, 40f, 1f,    1.00f, false));
        PROFILES.put("t_dust",  new CubeProfile(0xC8B79A, 0x8A7B62, 0x4A4034, 0.20f, 0.08f, 16, 5, 0.010f, 0.93f, 0.960f, 20f, 1f,    0.95f, false));
        PROFILES.put("t_ring",  new CubeProfile(0xFFE0F0, 0xFF8AC0, 0xB03070, 0.15f, 0.03f, 5,  1, 0.0f,   1.00f, 0.900f, 0f,  0.25f, 0.90f, false));
        PROFILES.put("t_gold",  new CubeProfile(0xFFFFFF, 0xFFE680, 0xE0A800, 0.12f, 0.03f, 9,  3, 0.0f,   0.92f, 0.930f, 24f, 1f,    1.00f, true));
        PROFILES.put("t_heart", new CubeProfile(0xFF9AB0, 0xC01048, 0x600020, 0.20f, 0.05f, 12, 3, 0.0f,   0.90f, 0.945f, 14f, 1f,    0.95f, true));
    }

    public static SimpleParticleType get(String name) {
        return TYPES.get(name);
    }

    public static void init() {
        for (String name : PROFILES.keySet()) {
            SimpleParticleType t = FabricParticleTypes.simple(true);
            Registry.register(Registries.PARTICLE_TYPE, TuskActs.id(name), t);
            TYPES.put(name, t);
        }
    }
}
