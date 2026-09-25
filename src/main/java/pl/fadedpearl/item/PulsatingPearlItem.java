package pl.fadedpearl.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class PulsatingPearlItem extends Item {
    public static final String COLOR_TAG = "HealingColor";
    public static final int DEFAULT_COLOR = 0xE33A4E;

    public PulsatingPearlItem(Properties properties) { super(properties); }

    public static int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(COLOR_TAG) ? tag.getInt(COLOR_TAG) : DEFAULT_COLOR;
    }

    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateTag().putInt(COLOR_TAG, color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.faded_pearl.pulsating_pearl")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.faded_pearl.pulsating_pearl.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
