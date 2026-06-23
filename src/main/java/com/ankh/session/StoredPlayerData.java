package com.ankh.session;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

public final class StoredPlayerData {

    private NbtList inventoryNbt;
    private NbtList accessoriesNbt = new NbtList();
    private int experienceLevel;
    private float experienceProgress;
    private int totalExperience;

    private StoredPlayerData() {}

    public static StoredPlayerData capture(PlayerEntity player) {
        StoredPlayerData data = new StoredPlayerData();

        data.inventoryNbt = player.getInventory().writeNbt(new NbtList());
        if (player instanceof ServerPlayerEntity sp) {
            data.accessoriesNbt = com.ankh.compat.AccessoriesCompat.snapshot(sp);
        }
        data.experienceLevel = player.experienceLevel;
        data.experienceProgress = player.experienceProgress;
        data.totalExperience = player.totalExperience;
        return data;
    }

    public void restoreOnto(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.readNbt(inventoryNbt);

        player.experienceLevel = experienceLevel;
        player.experienceProgress = experienceProgress;
        player.totalExperience = totalExperience;

        player.setExperienceLevel(experienceLevel);

        com.ankh.compat.AccessoriesCompat.restore(player, accessoriesNbt);
    }

    public void addOnto(ServerPlayerEntity player) {

        PlayerInventory temp = new PlayerInventory(player);
        temp.readNbt(inventoryNbt);
        giveAll(player, temp.main);
        giveAll(player, temp.armor);
        giveAll(player, temp.offHand);
        temp.clear();

        com.ankh.compat.AccessoriesCompat.restore(player, accessoriesNbt);

        player.addExperience(totalExperience);
    }

    private static void giveAll(ServerPlayerEntity player,
                                net.minecraft.util.collection.DefaultedList<net.minecraft.item.ItemStack> list) {
        for (net.minecraft.item.ItemStack stack : list) {
            if (!stack.isEmpty()) {
                player.giveItemStack(stack.copy());
            }
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("Inventory", inventoryNbt);
        nbt.put("Accessories", accessoriesNbt);
        nbt.putInt("XpLevel", experienceLevel);
        nbt.putFloat("XpProgress", experienceProgress);
        nbt.putInt("XpTotal", totalExperience);
        return nbt;
    }

    public static StoredPlayerData fromNbt(NbtCompound nbt) {
        StoredPlayerData data = new StoredPlayerData();
        data.inventoryNbt = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
        data.accessoriesNbt = nbt.getList("Accessories", NbtElement.COMPOUND_TYPE);
        data.experienceLevel = nbt.getInt("XpLevel");
        data.experienceProgress = nbt.getFloat("XpProgress");
        data.totalExperience = nbt.getInt("XpTotal");
        return data;
    }

    public NbtList getInventoryNbt() {
        return inventoryNbt;
    }

    public int extractItemAndCount(String itemId) {
        int total = 0;
        for (int i = inventoryNbt.size() - 1; i >= 0; i--) {
            NbtCompound entry = inventoryNbt.getCompound(i);
            String id = entry.getString("id");
            if (id.equals(itemId)) {
                total += Math.max(1, entry.getInt("count"));
                inventoryNbt.remove(i);
            }
        }
        return total;
    }
}
