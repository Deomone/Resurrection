package com.ankh.registry;

import com.ankh.AnkhResurrection;
import com.ankh.item.AnkhItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {

    private ModItems() {}

    public static final Identifier ANKH_ID = AnkhResurrection.id("ankh");

    public static final Item ANKH = register(
            "ankh",
            new AnkhItem(new Item.Settings()
                    .maxCount(16)
                    .rarity(net.minecraft.util.Rarity.EPIC))
    );

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, AnkhResurrection.id(path), item);
    }

    public static void register() {

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries ->
                entries.addAfter(Items.TOTEM_OF_UNDYING, ANKH));

        AnkhResurrection.LOGGER.info("[Ankh Resurrection] Items registered.");
    }
}
