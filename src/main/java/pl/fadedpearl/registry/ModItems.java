package pl.fadedpearl.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.item.EndermanTearItem;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.item.LoreItem;
import pl.fadedpearl.item.EndermanJournalItem;
import pl.fadedpearl.item.LoreBlockItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FadedPearl.MOD_ID);

    public static final RegistryObject<Item> EMPTY_PEARL = ITEMS.register("empty_pearl",
            () -> new LoreItem(new Item.Properties().stacksTo(1), "tooltip.faded_pearl.empty_pearl",
                    "tooltip.faded_pearl.empty_pearl.hint"));
    public static final RegistryObject<Item> WATER_FILLED_PEARL = ITEMS.register("water_filled_pearl",
            () -> new LoreItem(new Item.Properties().stacksTo(1), "tooltip.faded_pearl.water_filled_pearl",
                    "tooltip.faded_pearl.water_filled_pearl.hint"));
    public static final RegistryObject<Item> PULSATING_PEARL = ITEMS.register("pulsating_pearl",
            () -> new PulsatingPearlItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ESCAPE_PEARL = ITEMS.register("escape_pearl",
            () -> new LoreItem(new Item.Properties().stacksTo(16), "tooltip.faded_pearl.escape_pearl",
                    "tooltip.faded_pearl.escape_pearl.use"));
    public static final RegistryObject<Item> ENDERMAN_TEAR = ITEMS.register("enderman_tear",
            () -> new EndermanTearItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RESONATING_ANCHOR = ITEMS.register("resonating_anchor",
            () -> new LoreBlockItem(ModBlocks.RESONATING_ANCHOR.get(), new Item.Properties().stacksTo(1),
                    "tooltip.faded_pearl.resonating_anchor.home",
                    "tooltip.faded_pearl.resonating_anchor.recall",
                    "tooltip.faded_pearl.resonating_anchor.dimension"));
    public static final RegistryObject<Item> ENDERMAN_JOURNAL = ITEMS.register("enderman_journal",
            () -> new EndermanJournalItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
