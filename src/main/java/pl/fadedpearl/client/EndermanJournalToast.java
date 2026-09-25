package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

/** Independent upper-right toast; it never replaces the Enderman's dialogue overlay. */
public final class EndermanJournalToast implements Toast {
    private static final Object TOKEN = new Object();
    private long lastShown;
    private boolean refreshed = true;

    public static void show() {
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        EndermanJournalToast current = toasts.getToast(EndermanJournalToast.class, TOKEN);
        if (current == null) toasts.addToast(new EndermanJournalToast());
        else current.refreshed = true;
    }

    @Override
    public Object getToken() { return TOKEN; }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent component, long time) {
        if (refreshed) {
            lastShown = time;
            refreshed = false;
        }
        graphics.blit(TEXTURE, 0, 0, 0, 64, 160, 32);
        graphics.drawString(component.getMinecraft().font,
                Component.translatable("toast.faded_pearl.journal.title"), 18, 7, 0xFFEAA6, false);
        graphics.drawString(component.getMinecraft().font,
                Component.translatable("toast.faded_pearl.journal.message"), 18, 18, 0xFFFFFF, false);
        return time - lastShown < 4000L * component.getNotificationDisplayTimeMultiplier()
                ? Visibility.SHOW : Visibility.HIDE;
    }
}
