package pl.fadedpearl.client;

import net.minecraft.client.Minecraft;
import pl.fadedpearl.network.EchoNeedlePacket;
import pl.fadedpearl.world.EchoNeedleLocator;

import java.util.Optional;

/** Client cache with a defensive timeout so missed clear packets cannot leave stale guidance. */
public final class EchoNeedleClientState {
    public static final long TIMEOUT_TICKS = 50L;
    private static EchoNeedleLocator.Reading reading;
    private static long receivedAt = Long.MIN_VALUE;

    public static void accept(EchoNeedlePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        accept(packet, now);
    }

    static void accept(EchoNeedlePacket packet, long now) {
        if (!packet.active()) {
            clear();
            return;
        }
        reading = new EchoNeedleLocator.Reading(packet.sector(), packet.height());
        receivedAt = now;
    }

    public static Optional<EchoNeedleLocator.Reading> current(long now) {
        if (reading == null) return Optional.empty();
        if (now < receivedAt || now - receivedAt > TIMEOUT_TICKS) {
            clear();
            return Optional.empty();
        }
        return Optional.of(reading);
    }

    public static void clear() {
        reading = null;
        receivedAt = Long.MIN_VALUE;
    }

    private EchoNeedleClientState() {}
}
