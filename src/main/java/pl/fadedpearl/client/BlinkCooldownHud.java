package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.entity.FadedEnderman;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlinkCooldownHud {
    private static final ItemStack PEARL_ICON = new ItemStack(Items.ENDER_PEARL);
    private static final int BAR_WIDTH = 79;
    private static final int BAR_HEIGHT = 5;

    @SubscribeEvent
    public static void renderAfterFood(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.options.hideGui || player == null
                || !(player.getVehicle() instanceof FadedEnderman companion)
                || companion.getControllingPassenger() != player) return;

        render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), companion.getCarriedBlinkCooldown());
    }

    private static void render(GuiGraphics graphics, int screenWidth, int screenHeight, int cooldownTicks) {
        int right = screenWidth / 2 + 91;
        int left = right - BAR_WIDTH;
        int top = screenHeight - 57;
        int innerWidth = BAR_WIDTH - 2;
        int fill = BlinkCooldownHudState.filledPixels(cooldownTicks, innerWidth);
        int fillColor = BlinkCooldownHudState.fillColor(cooldownTicks);
        int borderColor = BlinkCooldownHudState.borderColor(cooldownTicks);

        graphics.renderItem(PEARL_ICON, left - 5, top - 9);
        graphics.fill(left, top, right, top + BAR_HEIGHT, 0xCC100B18);
        graphics.fill(left + 1, top + 1, left + 1 + fill, top + BAR_HEIGHT - 1, fillColor);
        graphics.fill(left, top, right, top + 1, borderColor);
        graphics.fill(left, top + BAR_HEIGHT - 1, right, top + BAR_HEIGHT, borderColor);
        graphics.fill(left, top, left + 1, top + BAR_HEIGHT, borderColor);
        graphics.fill(right - 1, top, right, top + BAR_HEIGHT, borderColor);
    }

    private BlinkCooldownHud() {
    }
}
