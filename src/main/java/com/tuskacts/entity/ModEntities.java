package com.tuskacts.entity;

import com.tuskacts.TuskActs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<TuskStand> STAND = Registry.register(Registries.ENTITY_TYPE, TuskActs.id("tusk_stand"),
            EntityType.Builder.<TuskStand>create(TuskStand::new, SpawnGroup.MISC).dimensions(0.6f, 1.0f)
                    .maxTrackingRange(10).trackingTickInterval(2).build("tusk_stand"));

    public static void init() { }
}
