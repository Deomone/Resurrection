package com.ankh.item;

import com.ankh.entity.CorpseEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.List;

public class AnkhItem extends Item {

    public AnkhItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof CorpseEntity corpse) {
            return corpse.interact(user, hand);
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.ankh.ankh.tooltip.1").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("item.ankh.ankh.tooltip.2").formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
