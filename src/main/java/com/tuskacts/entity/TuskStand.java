package com.tuskacts.entity;

import com.tuskacts.core.PData;
import com.tuskacts.server.Manager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** The visible Stand. The client draws it next to its owner; the server only keeps it alive and tells it the act and the pose. */
public class TuskStand extends Entity {
    public static final TrackedData<Integer> OWNER = DataTracker.registerData(TuskStand.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ACT = DataTracker.registerData(TuskStand.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> STANCE = DataTracker.registerData(TuskStand.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> STANCE_START = DataTracker.registerData(TuskStand.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> STANCE_DUR = DataTracker.registerData(TuskStand.class, TrackedDataHandlerRegistry.INTEGER);

    public TuskStand(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER, -1);
        builder.add(ACT, 1);
        builder.add(STANCE, 0);
        builder.add(STANCE_START, 0);
        builder.add(STANCE_DUR, 0);
    }

    public int ownerId() { return this.dataTracker.get(OWNER); }
    public int act() { return this.dataTracker.get(ACT); }
    public int stance() { return this.dataTracker.get(STANCE); }
    public int stanceStart() { return this.dataTracker.get(STANCE_START); }
    public int stanceDur() { return this.dataTracker.get(STANCE_DUR); }

    public void setOwnerId(int id) { this.dataTracker.set(OWNER, id); }
    public void setAct(int a) { if (a != act()) this.dataTracker.set(ACT, a); }

    public void setStance(int stance, int ticks) {
        this.dataTracker.set(STANCE, stance);
        this.dataTracker.set(STANCE_START, (int) getWorld().getTime());
        this.dataTracker.set(STANCE_DUR, Math.max(1, ticks));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        Entity owner = getWorld().getEntityById(ownerId());
        if (!(owner instanceof ServerPlayerEntity p) || !p.isAlive() || !Manager.get(p).out()) {
            discard();
            return;
        }
        setPosition(p.getX(), p.getY(), p.getZ());
        setAct(Manager.get(p).act());
        if (stance() != 0 && getWorld().getTime() - stanceStart() > stanceDur()) this.dataTracker.set(STANCE, 0);
    }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public boolean shouldSave() { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) { }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) { }
}
