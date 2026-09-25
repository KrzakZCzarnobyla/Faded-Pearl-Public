package pl.fadedpearl.entity.curiosity;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import pl.fadedpearl.FadedPearl;

/** Conservative eligibility gate: exchange only stacks whose exact, self-contained copy is safe. */
public final class ItemCuriosityPolicy {
    public static final int MIN_TRUST = 20;
    public static final int POINT_TICKS = 40;
    public static final int HAND_OFFER_TICKS = 200;
    public static final int INSPECT_TICKS = 80;
    /** Runtime feedback: keep spontaneous pointing rare enough to remain noteworthy. */
    public static final int COOLDOWN_TICKS = 3600;
    public static final int GROUND_APPROACH_TIMEOUT_TICKS = 160;
    public static final int GROUND_INSPECT_TICKS = 80;
    public static final double GROUND_NOTICE_RANGE = 8.0D;
    public static final double GROUND_STOP_DISTANCE = 2.25D;
    public static final int HINT_DELAY_TICKS = 10;

    private ItemCuriosityPolicy() {}

    public static boolean isEligible(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ItemTags.SMALL_FLOWERS)) return false;
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || FadedPearl.MOD_ID.equals(key.getNamespace())) return false;
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BaseEntityBlock) return false;
        if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return false;
        // Vanilla/container conventions plus Forge item-handler capability cover nested inventories;
        // harmless identity data (name, enchantments, damage) remains eligible and is returned byte-for-byte.
        return !stack.hasTag() || (!stack.getTag().contains("Items")
                && !stack.getTag().contains("BlockEntityTag")
                && !stack.getTag().contains("Inventory"));
    }
}
