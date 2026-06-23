package com.ankh.compat;

import com.ankh.AnkhResurrection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.Map;

public final class AccessoriesCompat {

    private AccessoriesCompat() {}

    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("accessories");
    private static boolean disabled = false;

    private static Method capGet;
    private static Method getContainers;
    private static Method getAccessories;
    private static Method getCosmeticAccessories;
    private static boolean reflectionReady = false;

    public static boolean isActive() {
        return PRESENT && !disabled;
    }

    private static synchronized void ensureReflection() {
        if (reflectionReady || disabled) return;
        try {
            Class<?> capClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Class<?> containerClass = Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
            capGet = capClass.getMethod("get", LivingEntity.class);
            getContainers = capClass.getMethod("getContainers");
            getAccessories = containerClass.getMethod("getAccessories");
            try {
                getCosmeticAccessories = containerClass.getMethod("getCosmeticAccessories");
            } catch (NoSuchMethodException ignored) {
                getCosmeticAccessories = null;
            }
            reflectionReady = true;
        } catch (Throwable t) {
            disabled = true;
            AnkhResurrection.LOGGER.warn("[Ankh] Accessories present but API not as expected; integration disabled.", t);
        }
    }

    private static Map<?, ?> containersFor(ServerPlayerEntity player) throws Exception {
        Object cap = capGet.invoke(null, player);
        if (cap == null) return null;
        Object containers = getContainers.invoke(cap);
        return (containers instanceof Map<?, ?> map) ? map : null;
    }

    private static Inventory inventoryOf(Object container, boolean cosmetic) throws Exception {
        Method m = cosmetic ? getCosmeticAccessories : getAccessories;
        if (m == null) return null;
        Object inv = m.invoke(container);
        return (inv instanceof Inventory i) ? i : null;
    }

    public static NbtList snapshot(ServerPlayerEntity player) {
        NbtList out = new NbtList();
        if (!isActive()) return out;
        ensureReflection();
        if (!reflectionReady) return out;
        try {
            Map<?, ?> containers = containersFor(player);
            if (containers == null) return out;
            RegistryWrapper.WrapperLookup reg = player.getRegistryManager();
            for (Map.Entry<?, ?> e : containers.entrySet()) {
                String name = String.valueOf(e.getKey());
                serialize(out, name, false, inventoryOf(e.getValue(), false), reg);
                serialize(out, name, true, inventoryOf(e.getValue(), true), reg);
            }
        } catch (Throwable t) {
            disabled = true;
            AnkhResurrection.LOGGER.warn("[Ankh] Accessories snapshot failed; integration disabled.", t);
        }
        return out;
    }

    private static void serialize(NbtList out, String container, boolean cosmetic,
                                  Inventory inv, RegistryWrapper.WrapperLookup reg) {
        if (inv == null) return;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            NbtCompound entry = new NbtCompound();
            entry.putString("Container", container);
            entry.putInt("Slot", i);
            entry.putBoolean("Cosmetic", cosmetic);
            entry.put("Item", stack.encode(reg));
            out.add(entry);
        }
    }

    public static void restore(ServerPlayerEntity player, NbtList data) {
        if (!isActive() || data == null || data.isEmpty()) return;
        ensureReflection();
        if (!reflectionReady) return;
        try {
            Map<?, ?> containers = containersFor(player);
            if (containers == null) return;
            RegistryWrapper.WrapperLookup reg = player.getRegistryManager();
            for (int k = 0; k < data.size(); k++) {
                NbtCompound entry = data.getCompound(k);
                Object container = containers.get(entry.getString("Container"));
                if (container == null) continue;
                Inventory inv = inventoryOf(container, entry.getBoolean("Cosmetic"));
                if (inv == null) continue;
                int slot = entry.getInt("Slot");
                if (slot < 0 || slot >= inv.size()) continue;
                ItemStack stack = ItemStack.fromNbt(reg, entry.getCompound("Item")).orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) continue;
                if (inv.getStack(slot).isEmpty()) {
                    inv.setStack(slot, stack);
                } else {
                    player.getInventory().offerOrDrop(stack);
                }
            }
        } catch (Throwable t) {
            AnkhResurrection.LOGGER.warn("[Ankh] Accessories restore failed (items may be lost).", t);
        }
    }

    public static void clear(ServerPlayerEntity player) {
        if (!isActive()) return;
        ensureReflection();
        if (!reflectionReady) return;
        try {
            Map<?, ?> containers = containersFor(player);
            if (containers == null) return;
            for (Map.Entry<?, ?> e : containers.entrySet()) {
                clearInv(inventoryOf(e.getValue(), false));
                clearInv(inventoryOf(e.getValue(), true));
            }
        } catch (Throwable t) {
            AnkhResurrection.LOGGER.warn("[Ankh] Accessories clear failed.", t);
        }
    }

    private static void clearInv(Inventory inv) {
        if (inv == null) return;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) inv.setStack(i, ItemStack.EMPTY);
        }
    }
}
