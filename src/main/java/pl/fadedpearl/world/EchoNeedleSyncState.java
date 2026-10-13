package pl.fadedpearl.world;

import java.util.Optional;

/** Change-or-heartbeat policy kept independent from Forge networking for deterministic tests. */
final class EchoNeedleSyncState {
    static final long HEARTBEAT_TICKS = 40L;

    private EchoNeedleLocator.Reading lastReading;
    private long lastSentTick = Long.MIN_VALUE;

    Optional<Update> update(Optional<EchoNeedleLocator.Reading> current, long gameTime) {
        EchoNeedleLocator.Reading next = current.orElse(null);
        boolean changed = !java.util.Objects.equals(lastReading, next);
        boolean heartbeat = next != null && elapsed(gameTime, lastSentTick) >= HEARTBEAT_TICKS;
        if (!changed && !heartbeat) return Optional.empty();
        if (next == null && lastReading == null) return Optional.empty();
        lastReading = next;
        lastSentTick = gameTime;
        return Optional.of(next == null ? Update.clear() : Update.active(next));
    }

    private static long elapsed(long now, long previous) {
        if (previous == Long.MIN_VALUE || now < previous) return Long.MAX_VALUE;
        return now - previous;
    }

    record Update(boolean active, int sector, EchoNeedleLocator.Height height) {
        static Update active(EchoNeedleLocator.Reading reading) {
            return new Update(true, reading.sector(), reading.height());
        }

        static Update clear() {
            return new Update(false, 0, EchoNeedleLocator.Height.SIMILAR);
        }
    }
}
