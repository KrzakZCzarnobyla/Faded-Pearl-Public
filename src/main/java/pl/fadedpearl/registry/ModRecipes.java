package pl.fadedpearl.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.recipe.PulsatingPearlRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FadedPearl.MOD_ID);
    public static final RegistryObject<RecipeSerializer<PulsatingPearlRecipe>> PULSATING_PEARL =
            SERIALIZERS.register("pulsating_pearl", () -> new SimpleCraftingRecipeSerializer<>(PulsatingPearlRecipe::new));
    private ModRecipes() {}
}
