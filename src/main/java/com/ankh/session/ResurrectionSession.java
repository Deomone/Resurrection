package com.ankh.session;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class ResurrectionSession {

    public static int totalTicks() { return com.ankh.config.AnkhConfig.get().sessionTicks(); }

    public enum Phase {

        ZOMBIE_ALIVE,

        CORPSE_PRESENT
    }

    private final UUID owner;
    private final boolean singleplayer;

    private final String dimensionId;
    private final BlockPos deathPos;

    private Phase phase = Phase.ZOMBIE_ALIVE;
    private int ticksRemaining = totalTicks();

    private UUID zombieUuid;
    private UUID corpseUuid;

    private final StoredPlayerData storedData;

    private int preservedAnkhCount = 0;

    private boolean ankhReturned = false;

    private boolean waiting = false;
    private String previousGameMode = "survival";

    private transient int observerPlaceDelay = -1;

    private BlockPos groundedPos;

    private transient BlockPos stashSpawnPos;
    private transient net.minecraft.registry.RegistryKey<net.minecraft.world.World> stashSpawnDim;
    private transient float stashSpawnAngle;
    private transient boolean stashSpawnForced;
    private transient boolean spawnStashed;

    private transient boolean pendingResurrect = false;

    public ResurrectionSession(UUID owner, boolean singleplayer, String dimensionId,
                               BlockPos deathPos, StoredPlayerData storedData) {
        this.owner = owner;
        this.singleplayer = singleplayer;
        this.dimensionId = dimensionId;
        this.deathPos = deathPos.toImmutable();
        this.storedData = storedData;
    }

    public UUID getOwner() { return owner; }
    public boolean isSingleplayer() { return singleplayer; }
    public String getDimensionId() { return dimensionId; }
    public BlockPos getDeathPos() { return deathPos; }
    public Phase getPhase() { return phase; }
    public int getTicksRemaining() { return ticksRemaining; }
    public UUID getZombieUuid() { return zombieUuid; }
    public UUID getCorpseUuid() { return corpseUuid; }
    public StoredPlayerData getStoredData() { return storedData; }
    public int getPreservedAnkhCount() { return preservedAnkhCount; }
    public boolean isAnkhReturned() { return ankhReturned; }
    public boolean isWaiting() { return waiting; }
    public String getPreviousGameMode() { return previousGameMode; }

    public void setPhase(Phase phase) { this.phase = phase; }
    public void setZombieUuid(UUID uuid) { this.zombieUuid = uuid; }
    public void setCorpseUuid(UUID uuid) { this.corpseUuid = uuid; }
    public void setPreservedAnkhCount(int count) { this.preservedAnkhCount = count; }
    public void setAnkhReturned(boolean returned) { this.ankhReturned = returned; }
    public void setWaiting(boolean waiting) { this.waiting = waiting; }
    public boolean isPendingResurrect() { return pendingResurrect; }
    public void setPendingResurrect(boolean v) { this.pendingResurrect = v; }
    public void setPreviousGameMode(String mode) { this.previousGameMode = mode; }

    public BlockPos getGroundedPos() { return groundedPos != null ? groundedPos : deathPos; }
    public void setGroundedPos(BlockPos pos) { this.groundedPos = pos == null ? null : pos.toImmutable(); }

    public void stashSpawn(net.minecraft.registry.RegistryKey<net.minecraft.world.World> dim,
                           BlockPos pos, float angle, boolean forced) {
        this.stashSpawnDim = dim;
        this.stashSpawnPos = pos;
        this.stashSpawnAngle = angle;
        this.stashSpawnForced = forced;
        this.spawnStashed = true;
    }
    public boolean isSpawnStashed() { return spawnStashed; }
    public net.minecraft.registry.RegistryKey<net.minecraft.world.World> getStashSpawnDim() { return stashSpawnDim; }
    public BlockPos getStashSpawnPos() { return stashSpawnPos; }
    public float getStashSpawnAngle() { return stashSpawnAngle; }
    public boolean getStashSpawnForced() { return stashSpawnForced; }
    public void clearStashSpawn() { this.spawnStashed = false; this.stashSpawnPos = null; this.stashSpawnDim = null; }

    public void requestObserverPlacement(int ticks) { this.observerPlaceDelay = ticks; }

    public boolean observerPlacementDue() {
        if (observerPlaceDelay < 0) return false;
        if (observerPlaceDelay == 0) { observerPlaceDelay = -1; return true; }
        observerPlaceDelay--;
        return false;
    }

    public boolean tickAndCheckExpired() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
        return ticksRemaining <= 0;
    }

    public int getSecondsRemaining() {
        return Math.max(0, ticksRemaining / 20);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Owner", owner);
        nbt.putBoolean("Singleplayer", singleplayer);
        nbt.putString("Dimension", dimensionId);
        nbt.putInt("DeathX", deathPos.getX());
        nbt.putInt("DeathY", deathPos.getY());
        nbt.putInt("DeathZ", deathPos.getZ());
        if (groundedPos != null) {
            nbt.putInt("GroundX", groundedPos.getX());
            nbt.putInt("GroundY", groundedPos.getY());
            nbt.putInt("GroundZ", groundedPos.getZ());
        }
        nbt.putString("Phase", phase.name());
        nbt.putInt("TicksRemaining", ticksRemaining);
        nbt.putInt("PreservedAnkh", preservedAnkhCount);
        nbt.putBoolean("AnkhReturned", ankhReturned);
        nbt.putBoolean("Waiting", waiting);
        nbt.putString("PrevGameMode", previousGameMode);
        if (zombieUuid != null) nbt.putUuid("Zombie", zombieUuid);
        if (corpseUuid != null) nbt.putUuid("Corpse", corpseUuid);
        nbt.put("Data", storedData.toNbt());
        return nbt;
    }

    public static ResurrectionSession fromNbt(NbtCompound nbt) {
        UUID owner = nbt.getUuid("Owner");
        boolean sp = nbt.getBoolean("Singleplayer");
        String dim = nbt.getString("Dimension");
        BlockPos pos = new BlockPos(nbt.getInt("DeathX"), nbt.getInt("DeathY"), nbt.getInt("DeathZ"));
        StoredPlayerData data = StoredPlayerData.fromNbt(nbt.getCompound("Data"));

        ResurrectionSession session = new ResurrectionSession(owner, sp, dim, pos, data);
        session.phase = Phase.valueOf(nbt.getString("Phase"));
        session.ticksRemaining = nbt.getInt("TicksRemaining");
        session.preservedAnkhCount = nbt.getInt("PreservedAnkh");
        session.ankhReturned = nbt.getBoolean("AnkhReturned");
        session.waiting = nbt.getBoolean("Waiting");
        if (nbt.contains("PrevGameMode")) session.previousGameMode = nbt.getString("PrevGameMode");
        if (nbt.contains("GroundX")) {
            session.groundedPos = new BlockPos(nbt.getInt("GroundX"), nbt.getInt("GroundY"), nbt.getInt("GroundZ"));
        }
        if (nbt.containsUuid("Zombie")) session.zombieUuid = nbt.getUuid("Zombie");
        if (nbt.containsUuid("Corpse")) session.corpseUuid = nbt.getUuid("Corpse");
        return session;
    }
}
