package pl.fadedpearl.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LoreItem extends Item {
    private final List<String> tooltipKeys;

    public LoreItem(Properties properties, String... tooltipKeys) {
        super(properties);
        this.tooltipKeys = List.of(tooltipKeys);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (String tooltipKey : tooltipKeys)
            tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
