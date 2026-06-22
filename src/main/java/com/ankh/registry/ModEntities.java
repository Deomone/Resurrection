package com.ankh.registry;

import com.ankh.AnkhResurrection;
import com.ankh.entity.CorpseEntity;
import com.ankh.entity.ResurrectionZombieEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {

    private ModEntities() {}

    public static final RegistryKey<EntityType<?>> RESURRECTION_ZOMBIE_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, AnkhResurrection.id("resurrection_zombie"));

    public static final RegistryKey<EntityType<?>> CORPSE_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, AnkhResurrection.id("corpse"));

    public static final EntityType<ResurrectionZombieEntity> RESURRECTION_ZOMBIE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    RESURRECTION_ZOMBIE_KEY,
                    EntityType.Builder.create(ResurrectionZombieEntity::new, SpawnGroup.MISC)
                            .dimensions(0.6f, 1.95f)
                            .maxTrackingRange(48)
                            .trackingTickInterval(2)
                            .makeFireImmune()
                            .build("resurrection_zombie")
            );

    public static final EntityType<CorpseEntity> CORPSE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    CORPSE_KEY,
                    EntityType.Builder.create(CorpseEntity::new, SpawnGroup.MISC)
                            .dimensions(0.6f, 0.55f)
                            .maxTrackingRange(48)
                            .trackingTickInterval(3)
                            .makeFireImmune()
                            .build("corpse")
            );

    public static final RegistryKey<EntityType<?>> LAMP_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, AnkhResurrection.id("lamp"));

    public static final EntityType<com.ankh.entity.LampEntity> LAMP =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    LAMP_KEY,
                    EntityType.Builder.<com.ankh.entity.LampEntity>create(com.ankh.entity.LampEntity::new, SpawnGroup.MISC)
                            .dimensions(0.5f, 3.25f)
                            .maxTrackingRange(64)
                            .trackingTickInterval(3)
                            .makeFireImmune()
                            .build("lamp")
            );

    public static void register() {

        FabricDefaultAttributeRegistry.register(
                RESURRECTION_ZOMBIE,
                ZombieEntity.createZombieAttributes()
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
        );

        FabricDefaultAttributeRegistry.register(
                CORPSE,
                net.minecraft.entity.LivingEntity.createLivingAttributes()
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH, 1024.0)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                        .add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
        );

        AnkhResurrection.LOGGER.info("[Ankh Resurrection] Entities registered.");
    }
}
