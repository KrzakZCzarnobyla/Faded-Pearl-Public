package pl.fadedpearl.client;

import com.mojang.math.Axis;
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
        if (!(player.getVehicle() instanceof FadedEnderman)) return;

        // Rotate the rendered body into the cradle pose without rotating the player's camera.
        event.getPoseStack().translate(0.0D, 0.95D, 0.0D);
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(92.0F));
        event.getPoseStack().translate(0.0D, -0.95D, 0.0D);
    }

    private CarryRenderEvents() {}
}
