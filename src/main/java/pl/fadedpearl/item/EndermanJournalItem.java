package pl.fadedpearl.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pl.fadedpearl.entity.journal.EndermanJournalAccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EndermanJournalItem extends Item {
    public static final String COMPANION_ID_TAG = "Companion";

    public EndermanJournalItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, UUID companionId) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putUUID(COMPANION_ID_TAG, companionId);
        return stack;
    }

    public static Optional<UUID> companionId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(COMPANION_ID_TAG)
                ? Optional.of(tag.getUUID(COMPANION_ID_TAG)) : Optional.empty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer serverPlayer && EndermanJournalAccess.open(serverPlayer, stack))
            return InteractionResultHolder.consume(stack);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.faded_pearl.enderman_journal")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(companionId(stack).isPresent()
                ? "tooltip.faded_pearl.enderman_journal.bound"
                : "tooltip.faded_pearl.enderman_journal.unbound").withStyle(ChatFormatting.GRAY));
    }
}
