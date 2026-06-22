package com.ankh.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;

public final class InventoryNbtUtil {

    private InventoryNbtUtil() {}

    public static final int SLOT_FEET = 100;
    public static final int SLOT_LEGS = 101;
    public static final int SLOT_CHEST = 102;
    public static final int SLOT_HEAD = 103;
    public static final int SLOT_OFFHAND = -106;

    public static ItemStack stackAtSlot(NbtList inventory, int slot, RegistryWrapper.WrapperLookup registries) {
        for (int i = 0; i < inventory.size(); i++) {
            NbtCompound entry = inventory.getCompound(i);
            int s = entry.getByte("Slot") & 255;

            if (s == slot || entry.getByte("Slot") == slot) {
                return ItemStack.fromNbt(registries, entry).orElse(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack firstHotbarStack(NbtList inventory, RegistryWrapper.WrapperLookup registries) {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = stackAtSlot(inventory, slot, registries);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack highestDamageStack(NbtList inventory, RegistryWrapper.WrapperLookup registries) {
        ItemStack best = ItemStack.EMPTY;
        double bestDmg = 0.0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = ItemStack.fromNbt(registries, inventory.getCompound(i)).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            double dmg = attackDamageOf(stack);
            if (dmg > bestDmg) {
                bestDmg = dmg;
                best = stack;
            }
        }
        return best.isEmpty() ? firstHotbarStack(inventory, registries) : best;
    }

    public static ItemStack bestArmorForSlot(NbtList inventory, net.minecraft.entity.EquipmentSlot slot,
                                             RegistryWrapper.WrapperLookup registries) {
        ItemStack best = ItemStack.EMPTY;
        double bestScore = -1.0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = ItemStack.fromNbt(registries, inventory.getCompound(i)).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof net.minecraft.item.ArmorItem armor)) continue;
            if (armor.getSlotType() != slot) continue;
            double toughness = 0.0;
            try { toughness = armor.getMaterial().value().toughness(); } catch (Exception ignored) {}
            double score = armor.getProtection() * 100.0 + toughness;
            if (score > bestScore) {
                bestScore = score;
                best = stack;
            }
        }
        return best;
    }

    public static ItemStack firstRangedWeapon(NbtList inventory, RegistryWrapper.WrapperLookup registries) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = ItemStack.fromNbt(registries, inventory.getCompound(i)).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof net.minecraft.item.BowItem
                    || stack.getItem() instanceof net.minecraft.item.CrossbowItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static double attackDamageOf(ItemStack stack) {
        AttributeModifiersComponent comp =
                stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        final double[] sum = {0.0};
        comp.applyModifiers(AttributeModifierSlot.MAINHAND, (attr, mod) -> {
            if (attr.value() == EntityAttributes.GENERIC_ATTACK_DAMAGE.value()) {
                sum[0] += mod.value();
            }
        });
        return sum[0];
    }
}
