package com.ankh.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class LampEntity extends Entity {

    public LampEntity(EntityType<? extends LampEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}

    @Override
    public boolean isImmuneToExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }
}
