package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.network.SetCompanionCommandPacket;

public final class CommandWheelScreen extends Screen {
    private final int entityId;
    private final int active;

    private CommandWheelScreen(int entityId, int active) {
        super(Component.translatable("screen.faded_pearl.commands"));
        this.entityId = entityId;
        this.active = active;
    }

    public static void open(int entityId, int active) {
        Minecraft.getInstance().setScreen(new CommandWheelScreen(entityId, active));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2, cy = height / 2;
        graphics.fill(cx - 44, cy - 44, cx + 44, cy + 44, 0xB8141C26);
        graphics.fill(cx - 4, cy - 64, cx + 4, cy + 64, 0xAA34475A);
        graphics.fill(cx - 64, cy - 4, cx + 64, cy + 4, 0xAA34475A);
        String[] keys = {"follow", "stay", "rest", "home"};
        int[][] points = {{0,-31},{31,0},{0,31},{-31,0}};
        for (int i = 0; i < keys.length; i++) {
            Component text = Component.translatable("command.faded_pearl." + keys[i]);
            int color = i == active ? 0xFF77E7FF : 0xFFFFFFFF;
            graphics.drawCenteredString(font, text, cx + points[i][0], cy + points[i][1] - 4, color);
        }
        graphics.drawCenteredString(font, title, cx, cy - 82, 0xFFE7F8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        double dx = mouseX - width / 2.0D, dy = mouseY - height / 2.0D;
        int selected;
        if (Math.abs(dx) > Math.abs(dy)) selected = dx > 0 ? 1 : 3;
        else selected = dy > 0 ? 2 : 0;
        SetCompanionCommandPacket.send(entityId, selected);
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
