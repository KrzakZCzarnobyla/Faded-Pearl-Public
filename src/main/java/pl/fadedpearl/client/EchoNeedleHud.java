package pl.fadedpearl.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import pl.fadedpearl.FadedPearl;
import pl.fadedpearl.registry.ModItems;
import pl.fadedpearl.world.EchoNeedleLocator;

@Mod.EventBusSubscriber(modid = FadedPearl.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EchoNeedleHud {
    private static final int NEEDLE_OUTLINE = 0xFF05070A;
    private static final int NEEDLE_TIP = 0xFFF0FAFF;
    private static final int NEEDLE_CORE = 0xFF79C9E8;
    private static final int NEEDLE_TAIL = 0xFF78539D;
    private static final int BADGE_BACKGROUND = 0xD9141828;
    private static final int BADGE_BORDER = 0xDD5A7699;
    private static final int HEIGHT_COLOR = 0xFFE1F7FF;
    private static ItemStack frameItem;

    @SubscribeEvent
    public static void renderAfterExperience(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) return;
        EchoNeedleClientState.current(minecraft.level.getGameTime()).ifPresent(reading ->
                render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(),
                        event.getWindow().getGuiScaledHeight(), reading));
    }

    @SubscribeEvent
    public static void clearOnLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        EchoNeedleClientState.clear();
    }

    static void render(GuiGraphics graphics, int screenWidth, int screenHeight,
                       EchoNeedleLocator.Reading reading) {
        int centerX = screenWidth / 2 - 101;
        int centerY = screenHeight - 29;
        graphics.renderItem(frameItem(), centerX - 8, centerY - 8);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 200.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(reading.sector() * 22.5F));
        graphics.fill(-2, -8, 2, -5, NEEDLE_OUTLINE);
        graphics.fill(-3, -6, 3, -2, NEEDLE_OUTLINE);
        graphics.fill(-2, -3, 2, 7, NEEDLE_OUTLINE);
        graphics.fill(-3, 3, 3, 7, NEEDLE_OUTLINE);
        graphics.fill(-1, -6, 1, -1, NEEDLE_CORE);
        graphics.fill(-2, -5, 2, -3, NEEDLE_CORE);
        graphics.fill(-1, -7, 1, -5, NEEDLE_TIP);
        graphics.fill(-1, 1, 1, 6, NEEDLE_TAIL);
        graphics.fill(-2, 4, 2, 6, NEEDLE_TAIL);
        graphics.fill(-1, -1, 1, 2, NEEDLE_TIP);
        graphics.pose().popPose();
        renderHeightBadge(graphics, centerX + 12, centerY, reading.height());
    }

    private static void renderHeightBadge(GuiGraphics graphics, int centerX, int centerY,
                                          EchoNeedleLocator.Height height) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.fill(centerX - 4, centerY - 5, centerX + 5, centerY + 5, BADGE_BACKGROUND);
        graphics.fill(centerX - 3, centerY - 4, centerX + 4, centerY - 3, BADGE_BORDER);
        graphics.fill(centerX - 3, centerY + 3, centerX + 4, centerY + 4, BADGE_BORDER);
        graphics.fill(centerX - 4, centerY - 4, centerX - 3, centerY + 4, BADGE_BORDER);
        graphics.fill(centerX + 4, centerY - 4, centerX + 5, centerY + 4, BADGE_BORDER);
        if (height == EchoNeedleLocator.Height.ABOVE) {
            graphics.fill(centerX, centerY - 3, centerX + 1, centerY + 3, HEIGHT_COLOR);
            graphics.fill(centerX - 1, centerY - 2, centerX + 2, centerY - 1, HEIGHT_COLOR);
            graphics.fill(centerX - 2, centerY - 1, centerX + 3, centerY, HEIGHT_COLOR);
        }
        else if (height == EchoNeedleLocator.Height.BELOW) {
            graphics.fill(centerX, centerY - 2, centerX + 1, centerY + 4, HEIGHT_COLOR);
            graphics.fill(centerX - 1, centerY + 1, centerX + 2, centerY + 2, HEIGHT_COLOR);
            graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, HEIGHT_COLOR);
        }
        else {
            graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, HEIGHT_COLOR);
            graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, HEIGHT_COLOR);
        }
        graphics.pose().popPose();
    }

    private static ItemStack frameItem() {
        if (frameItem == null) frameItem = new ItemStack(ModItems.EMPTY_PEARL.get());
        return frameItem;
    }

    public static void showFirstHint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(Component.translatable("hint.faded_pearl.echo_needle"), true);
    }

    private EchoNeedleHud() {}
}
