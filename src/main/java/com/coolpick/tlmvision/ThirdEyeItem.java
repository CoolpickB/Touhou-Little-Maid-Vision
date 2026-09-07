package com.coolpick.tlmvision;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class ThirdEyeItem extends Item {
    public ThirdEyeItem() { super(new Properties().stacksTo(1)); }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.GOLD);
    }
}
