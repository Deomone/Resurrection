package com.ankh.event;

import com.ankh.AnkhResurrection;
import com.ankh.config.AnkhConfig;
import com.ankh.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class LootInjection {

    private LootInjection() {}

    private static final Identifier SUSPICIOUS_SAND =
            Identifier.ofVanilla("archaeology/desert_pyramid");
    private static final Identifier PYRAMID_CHEST =
            Identifier.ofVanilla("chests/desert_pyramid");

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {

            if (!source.isBuiltin()) {
                return;
            }

            Identifier id = key.getValue();
            AnkhConfig config = AnkhConfig.get();

            if (SUSPICIOUS_SAND.equals(id)) {
                tableBuilder.pool(ankhPool(config.suspiciousSandChance));
                AnkhResurrection.LOGGER.info("[Ankh] Injected Ankh into suspicious sand ({}%).",
                        config.suspiciousSandChance * 100f);
            } else if (PYRAMID_CHEST.equals(id)) {
                tableBuilder.pool(ankhPool(config.pyramidChestChance));
                AnkhResurrection.LOGGER.info("[Ankh] Injected Ankh into desert pyramid chests ({}%).",
                        config.pyramidChestChance * 100f);
            }
        });
    }

    private static LootPool.Builder ankhPool(float chance) {
        return LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance))
                .with(ItemEntry.builder(ModItems.ANKH));
    }
}
