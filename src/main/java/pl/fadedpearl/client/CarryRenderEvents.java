package pl.fadedpearl.client;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, value = Dist.CLIENT)
public final class CarryRenderEvents {
    @SubscribeEvent
    public static void renderCarriedPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof FadedEnderman companion)) return;

        // Keep the rendered body upright and aligned with the carrier without touching camera yaw.
        float carriedYaw = companion.yBodyRot;
        player.setYBodyRot(carriedYaw);
        player.yBodyRotO = carriedYaw;
    }

    private CarryRenderEvents() {}
}
