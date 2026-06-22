package com.ankh.session;

import com.ankh.AnkhResurrection;
import com.ankh.entity.CorpseEntity;
import com.ankh.entity.ResurrectionZombieEntity;
import com.ankh.network.ModNetworking;
import com.ankh.registry.ModEntities;
import com.ankh.registry.ModItems;
import com.ankh.util.InventoryNbtUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.UUID;

public final class ResurrectionManager {

    private ResurrectionManager() {}

    public static ServerWorld worldFor(MinecraftServer server, ResurrectionSession session) {
        Identifier dimId = Identifier.tryParse(session.getDimensionId());
        if (dimId != null) {
            RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, dimId);
            ServerWorld world = server.getWorld(key);
            if (world != null) return world;
        }
        return server.getOverworld();
    }

    public static Entity findEntity(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            Entity e = world.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    public static void retrackEntity(ServerWorld world, Entity entity) {
        try {
            net.minecraft.server.world.ServerChunkLoadingManager clm =
                    world.getChunkManager().chunkLoadingManager;
            it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> trackers =
                    ((com.ankh.mixin.ServerChunkLoadingManagerAccessor) clm).ankh$getEntityTrackers();
            Object tracker = trackers.get(entity.getId());
            if (tracker instanceof com.ankh.mixin.EntityTrackerInvoker inv) {

                for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                    if (p.getId() == entity.getId()) continue;
                    inv.ankh$stopTracking(p);
                    inv.ankh$updateTrackedStatus(p);
                }
                AnkhResurrection.LOGGER.info("[Ankh] re-tracked entity id={} for {} viewers",
                        entity.getId(), world.getServer().getPlayerManager().getPlayerList().size());
            } else {
                AnkhResurrection.LOGGER.warn("[Ankh] re-track: no tracker for entity id={}", entity.getId());
            }
        } catch (Exception e) {
            AnkhResurrection.LOGGER.error("[Ankh] re-track failed", e);
        }
    }

    public static void spawnZombie(MinecraftServer server, ResurrectionSession session, String ownerName) {
        ServerWorld world = worldFor(server, session);
        BlockPos pos = session.getDeathPos();

        BlockPos groundPos = groundBelow(world, pos);

        session.setGroundedPos(groundPos);

        ResurrectionZombieEntity zombie = ModEntities.RESURRECTION_ZOMBIE.create(world);
        if (zombie == null) return;

        zombie.refreshPositionAndAngles(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5, 0f, 0f);
        zombie.initialize(session.getOwner(), ownerName, groundPos);
        zombie.setPersistent();

        world.spawnEntity(zombie);
        try {
            equipFromSnapshot(zombie, session.getStoredData().getInventoryNbt(), server);
        } catch (Exception e) {
            AnkhResurrection.LOGGER.error("[Ankh] Failed to equip Resurrection Zombie", e);
        }
        session.setZombieUuid(zombie.getUuid());

        try {

            BlockPos lampBase = groundPos;
            com.ankh.entity.LampEntity lamp = ModEntities.LAMP.create(world);
            if (lamp != null) {
                lamp.refreshPositionAndAngles(lampBase.getX() + 0.5, lampBase.getY(), lampBase.getZ() + 0.5, 0f, 0f);
                world.spawnEntity(lamp);
                AnkhResurrection.LOGGER.info("[Ankh] Death lamp spawned (uuid={}) at {},{},{}",
                        lamp.getUuid(), lampBase.getX(), lampBase.getY(), lampBase.getZ());
            } else {
                AnkhResurrection.LOGGER.warn("[Ankh] LampEntity.create returned null -> no death lamp spawned.");
            }
            BlockPos lightPos = lampBase.up(3);
            if (world.getBlockState(lightPos).isAir()) {
                world.setBlockState(lightPos, net.minecraft.block.Blocks.LIGHT.getDefaultState()
                        .with(net.minecraft.block.LightBlock.LEVEL_15, 15));
            }
            zombie.setLampPos(lampBase);
        } catch (Exception e) {
            AnkhResurrection.LOGGER.error("[Ankh] Failed to place death lamp", e);
        }

        AnkhResurrection.LOGGER.info("[Ankh] Spawned Resurrection Zombie for {} at {}", ownerName, pos);
    }

    private static void equipFromSnapshot(ResurrectionZombieEntity zombie, NbtList inv, MinecraftServer server) {
        var registries = server.getRegistryManager();

        ItemStack head = InventoryNbtUtil.bestArmorForSlot(inv, EquipmentSlot.HEAD, registries);
        ItemStack chest = InventoryNbtUtil.bestArmorForSlot(inv, EquipmentSlot.CHEST, registries);
        ItemStack legs = InventoryNbtUtil.bestArmorForSlot(inv, EquipmentSlot.LEGS, registries);
        ItemStack feet = InventoryNbtUtil.bestArmorForSlot(inv, EquipmentSlot.FEET, registries);
        ItemStack offhand = InventoryNbtUtil.stackAtSlot(inv, InventoryNbtUtil.SLOT_OFFHAND, registries);
        ItemStack main = InventoryNbtUtil.highestDamageStack(inv, registries);

        zombie.equipStack(EquipmentSlot.HEAD, head.copy());
        zombie.equipStack(EquipmentSlot.CHEST, chest.copy());
        zombie.equipStack(EquipmentSlot.LEGS, legs.copy());
        zombie.equipStack(EquipmentSlot.FEET, feet.copy());
        zombie.equipStack(EquipmentSlot.MAINHAND, main.copy());
        zombie.equipStack(EquipmentSlot.OFFHAND, offhand.copy());

        ItemStack ranged = InventoryNbtUtil.firstRangedWeapon(inv, registries);
        zombie.setRangedWeapon(ranged, main);
        AnkhResurrection.LOGGER.info("[Ankh] Guardian equipped: melee={}, ranged={}",
                main.getItem(), ranged.isEmpty() ? "none" : ranged.getItem());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setEquipmentDropChance(slot, 0.0f);
        }
    }

    public static void onZombieDeath(MinecraftServer server, ResurrectionSession session, Vec3d at, String ownerName) {
        ServerWorld world = worldFor(server, session);

        CorpseEntity corpse = ModEntities.CORPSE.create(world);
        if (corpse == null) return;

        corpse.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
        corpse.initialize(session.getOwner(), ownerName);
        corpse.setLampPos(session.getGroundedPos());
        corpse.setPersistent();

        world.spawnEntity(corpse);

        session.setCorpseUuid(corpse.getUuid());
        session.setPhase(ResurrectionSession.Phase.CORPSE_PRESENT);
        session.setZombieUuid(null);
        ResurrectionState.get(server).markDirty();
        AnkhResurrection.LOGGER.info("[Ankh] Zombie died -> spawned Corpse for {}", ownerName);
    }

    public static boolean resurrect(MinecraftServer server, ResurrectionSession session, ServerPlayerEntity ankhUser) {
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(session.getOwner());
        if (owner == null) {
            AnkhResurrection.LOGGER.warn("[Ankh] resurrect aborted: owner offline");
            return false;
        }

        AnkhResurrection.LOGGER.info("[Ankh] Resurrecting {} ({})",
                owner.getName().getString(), session.isSingleplayer() ? "singleplayer" : "multiplayer");

        session.setWaiting(false);

        consumeOneAnkh(ankhUser);
        despawn(server, session.getZombieUuid());
        despawn(server, session.getCorpseUuid());
        removeDeathLamp(server, session);

        if (session.isSingleplayer()) {

            session.getStoredData().addOnto(owner);
            ModNetworking.sendAnkhResurrectVfx(owner);
            ModNetworking.clearTimer(owner);
            ResurrectionState.get(server).remove(session.getOwner());
            AnkhResurrection.LOGGER.info("[Ankh] resurrected (SP) {}", owner.getName().getString());
            return true;
        }

        session.setPendingResurrect(true);
        prepareResurrectSpawn(server, session, owner, ankhUser);

        owner.changeGameMode(GameMode.SURVIVAL);
        owner.setInvulnerable(false);
        owner.getInventory().clear();
        net.minecraft.world.GameRules rules = server.getGameRules();
        boolean showDeaths = rules.getBoolean(net.minecraft.world.GameRules.SHOW_DEATH_MESSAGES);
        rules.get(net.minecraft.world.GameRules.SHOW_DEATH_MESSAGES).set(false, server);
        owner.setHealth(owner.getMaxHealth());
        owner.damage(owner.getDamageSources().genericKill(), Float.MAX_VALUE);
        rules.get(net.minecraft.world.GameRules.SHOW_DEATH_MESSAGES).set(showDeaths, server);

        ServerPlayerEntity newOwner = server.getPlayerManager().respawnPlayer(owner, false,
                net.minecraft.entity.Entity.RemovalReason.KILLED);
        newOwner.networkHandler.player = newOwner;
        return true;
    }

    public static void prepareResurrectSpawn(MinecraftServer server, ResurrectionSession session,
                                             ServerPlayerEntity owner, ServerPlayerEntity ankhUser) {
        ServerWorld world = (ServerWorld) ankhUser.getWorld();
        session.stashSpawn(owner.getSpawnPointDimension(), owner.getSpawnPointPosition(),
                owner.getSpawnAngle(), owner.isSpawnForced());
        owner.setSpawnPoint(world.getRegistryKey(), ankhUser.getBlockPos(), ankhUser.getYaw(), true, false);
    }

    public static void finalizeResurrect(MinecraftServer server, ResurrectionSession session,
                                         ServerPlayerEntity player) {
        session.getStoredData().restoreOnto(player);

        player.clearStatusEffects();
        player.setFireTicks(0);
        player.setFrozenTicks(0);
        player.setAir(player.getMaxAir());
        player.fallDistance = 0.0f;
        player.extinguish();
        com.ankh.config.AnkhConfig cfg = com.ankh.config.AnkhConfig.get();
        player.setHealth(player.getMaxHealth());
        if (cfg.resurrectionAbsorptionTicks() > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                    cfg.resurrectionAbsorptionTicks(), 4, false, false, true));
            player.setAbsorptionAmount(20.0f);
        }

        GameMode previous = parseGameMode(session.getPreviousGameMode());
        player.changeGameMode(previous);
        player.setInvulnerable(false);
        if (previous != GameMode.CREATIVE) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
        }
        player.sendAbilitiesUpdate();

        if (session.isSpawnStashed()) {
            RegistryKey<World> dim = session.getStashSpawnDim();
            if (dim == null) dim = World.OVERWORLD;
            player.setSpawnPoint(dim, session.getStashSpawnPos(), session.getStashSpawnAngle(),
                    session.getStashSpawnForced(), false);
            session.clearStashSpawn();
        }

        ModNetworking.sendAnkhResurrectVfx(player);
        ModNetworking.clearTimer(player);
        session.setPendingResurrect(false);
        session.setWaiting(false);
        ResurrectionState.get(server).remove(session.getOwner());

        AnkhResurrection.LOGGER.info("[Ankh] {} resurrected onto the corpse at {}.",
                player.getName().getString(), player.getBlockPos());
    }

    private static void consumeOneAnkh(ServerPlayerEntity user) {
        if (user.getMainHandStack().isOf(ModItems.ANKH)) {
            user.getMainHandStack().decrement(1);
        } else if (user.getOffHandStack().isOf(ModItems.ANKH)) {
            user.getOffHandStack().decrement(1);
        } else {

            for (int i = 0; i < user.getInventory().size(); i++) {
                ItemStack s = user.getInventory().getStack(i);
                if (s.isOf(ModItems.ANKH)) {
                    s.decrement(1);
                    break;
                }
            }
        }
    }

    public static void cleanup(MinecraftServer server, ResurrectionSession session) {
        despawn(server, session.getZombieUuid());
        despawn(server, session.getCorpseUuid());

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(session.getOwner());
        if (owner != null && session.isWaiting()) {
            GameMode previous = parseGameMode(session.getPreviousGameMode());
            owner.changeGameMode(previous);
            ModNetworking.clearTimer(owner);
        } else if (owner != null) {
            ModNetworking.clearTimer(owner);
        }

        removeDeathLamp(server, session);

        ResurrectionState.get(server).remove(session.getOwner());
    }

    private static void removeDeathLamp(MinecraftServer server, ResurrectionSession session) {
        try {
            ServerWorld w = worldFor(server, session);
            BlockPos dp = session.getDeathPos();
            if (w != null && dp != null) {

                net.minecraft.util.math.Box area = new net.minecraft.util.math.Box(
                        dp.getX() - 2, dp.getY() - 64, dp.getZ() - 2,
                        dp.getX() + 3, dp.getY() + 6, dp.getZ() + 3);
                for (com.ankh.entity.LampEntity lamp :
                        w.getEntitiesByClass(com.ankh.entity.LampEntity.class, area, e -> true)) {
                    BlockPos lightPos = lamp.getBlockPos().up(3);
                    if (w.getBlockState(lightPos).isOf(net.minecraft.block.Blocks.LIGHT)) {
                        w.setBlockState(lightPos, net.minecraft.block.Blocks.AIR.getDefaultState());
                    }
                    lamp.discard();
                }
            }
        } catch (Exception e) {
            AnkhResurrection.LOGGER.error("[Ankh] Failed to remove death lamp", e);
        }
    }

    private static BlockPos groundBelow(ServerWorld w, BlockPos start) {
        int x = start.getX(), z = start.getZ();
        int minY = w.getBottomY() + 1;
        net.minecraft.util.math.BlockPos.Mutable m = new net.minecraft.util.math.BlockPos.Mutable();
        for (int y = start.getY(); y >= minY; y--) {
            net.minecraft.block.BlockState bs = w.getBlockState(m.set(x, y - 1, z));
            boolean floorBelow = !bs.getCollisionShape(w, m).isEmpty() || !bs.getFluidState().isEmpty();
            if (floorBelow) {
                return new BlockPos(x, y, z);
            }
        }
        return start;
    }

    public static void despawn(MinecraftServer server, UUID uuid) {        Entity e = findEntity(server, uuid);
        if (e != null) {
            e.discard();
        }
    }

    public static GameMode parseGameMode(String id) {
        GameMode mode = GameMode.byName(id, GameMode.SURVIVAL);
        return mode == GameMode.SPECTATOR ? GameMode.SURVIVAL : mode;
    }

    public static void prepareObserverSpawn(MinecraftServer server, ResurrectionSession session,
                                            ServerPlayerEntity player) {
        ServerWorld deathWorld = worldFor(server, session);
        RegistryKey<World> deathDim = deathWorld.getRegistryKey();

        session.stashSpawn(player.getSpawnPointDimension(), player.getSpawnPointPosition(),
                player.getSpawnAngle(), player.isSpawnForced());

        player.setSpawnPoint(deathDim, session.getGroundedPos(), 0.0f, true, false);
    }

    public static void enterWaitingState(MinecraftServer server, ResurrectionSession session, ServerPlayerEntity player) {
        applyObserverMode(player);

        if (session.isSpawnStashed()) {
            RegistryKey<World> dim = session.getStashSpawnDim();
            if (dim == null) dim = World.OVERWORLD;
            player.setSpawnPoint(dim, session.getStashSpawnPos(), session.getStashSpawnAngle(),
                    session.getStashSpawnForced(), false);
            session.clearStashSpawn();
        }
    }

    public static void applyObserverMode(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SPECTATOR);
        player.setInvulnerable(true);
    }

    public static void exitWaitingState(ServerPlayerEntity player, ResurrectionSession session) {
        GameMode previous = parseGameMode(session.getPreviousGameMode());
        player.changeGameMode(previous);
        player.setInvulnerable(false);
        if (previous != GameMode.CREATIVE) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
        }
        player.sendAbilitiesUpdate();
        session.setWaiting(false);
    }

}
