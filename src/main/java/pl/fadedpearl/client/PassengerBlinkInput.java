package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.network.PassengerBlinkPacket;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PassengerBlinkInput {
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().isEmpty()
                || !(player.getVehicle() instanceof FadedEnderman companion)
                || companion.getControllingPassenger() != player) return;
        PassengerBlinkPacket.send();
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private PassengerBlinkInput() {}
}
