package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import pl.fadedpearl.entity.trust.TrustLossNotice;

/** Red, short-lived loss notice, separate from the Enderman's dialogue line. */
public final class TrustLossToast implements Toast {
    private static final Object TOKEN = new Object();
    private int points;
    private TrustLossNotice.Reason reason;
    private long lastShown;
    private boolean refreshed = true;

    private TrustLossToast(int points, TrustLossNotice.Reason reason) {
        this.points = points;
        this.reason = reason;
    }

    public static void show(int points, TrustLossNotice.Reason reason) {
        if (points <= 0) return;
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        TrustLossToast current = toasts.getToast(TrustLossToast.class, TOKEN);
        if (current == null) toasts.addToast(new TrustLossToast(points, reason));
        else {
            current.points = points;
            current.reason = reason;
            current.refreshed = true;
        }
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
                Component.translatable("toast.faded_pearl.trust_loss.title", points), 12, 7, 0xFF5A5A, false);
        graphics.drawString(component.getMinecraft().font,
                Component.translatable("toast.faded_pearl.trust_loss." + reason.name().toLowerCase(java.util.Locale.ROOT)),
                12, 18, 0xFFFFFF, false);
        return time - lastShown < 3500L * component.getNotificationDisplayTimeMultiplier()
                ? Visibility.SHOW : Visibility.HIDE;
    }
}
