package pl.fadedpearl;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import pl.fadedpearl.registry.ModEntities;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.registry.ModSounds;
import pl.fadedpearl.registry.ModBlocks;
import pl.fadedpearl.network.ModNetwork;
import pl.fadedpearl.registry.ModRecipes;
import software.bernie.geckolib.GeckoLib;
import pl.fadedpearl.config.FadedServerConfig;

@Mod(FadedPearl.MOD_ID)
public final class FadedPearl {
    public static final String MOD_ID = "faded_pearl";

    public FadedPearl() {
        GeckoLib.initialize();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FadedServerConfig.SPEC);
        ModNetwork.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
