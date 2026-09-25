package pl.fadedpearl.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.registry.ModRecipes;

import java.util.Map;

public final class PulsatingPearlRecipe extends CustomRecipe {
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("poppy", 0xE33A4E), Map.entry("blue_orchid", 0x4EBFFF),
            Map.entry("dandelion", 0xFFD83D), Map.entry("allium", 0xB870D6),
            Map.entry("azure_bluet", 0xE8EEF2), Map.entry("red_tulip", 0xD83B3B),
            Map.entry("orange_tulip", 0xF28C28), Map.entry("white_tulip", 0xF4F0EA),
            Map.entry("pink_tulip", 0xF28FB8), Map.entry("oxeye_daisy", 0xFFF4B0),
            Map.entry("cornflower", 0x466BDE), Map.entry("lily_of_the_valley", 0xF5F5F5),
            Map.entry("wither_rose", 0x4B3158), Map.entry("torchflower", 0xFF8A26));

    public PulsatingPearlRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int pearl = 0, flower = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.WATER_FILLED_PEARL.get())) pearl++;
            else if (stack.is(ItemTags.SMALL_FLOWERS)) flower++;
            else return false;
        }
        return pearl == 1 && flower == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        ItemStack result = new ItemStack(ModItems.PULSATING_PEARL.get());
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.is(ItemTags.SMALL_FLOWERS)) continue;
            Item item = stack.getItem();
            String path = ForgeRegistries.ITEMS.getKey(item).getPath();
            PulsatingPearlItem.setColor(result, COLORS.getOrDefault(path, PulsatingPearlItem.DEFAULT_COLOR));
            break;
        }
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.WATER_FILLED_PEARL.get()));
        ingredients.add(Ingredient.of(ItemTags.SMALL_FLOWERS));
        return ingredients;
    }
    @Override public ItemStack getResultItem(RegistryAccess access) {
        return new ItemStack(ModItems.PULSATING_PEARL.get());
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.PULSATING_PEARL.get(); }
}
