package pl.fadedpearl.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.registry.ModEntities;
import pl.fadedpearl.item.PulsatingPearlItem;
import pl.fadedpearl.registry.ModItems;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EntityRenderers.register(ModEntities.FADED_ENDERMAN.get(), FadedEndermanGeoRenderer::new));
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? PulsatingPearlItem.getColor(stack) : 0xFFFFFF,
                ModItems.PULSATING_PEARL.get());
    }

    private ClientEvents() {}
}
