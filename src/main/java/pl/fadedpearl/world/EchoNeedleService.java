package pl.fadedpearl.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import pl.fadedpearl.network.EchoNeedlePacket;
import pl.fadedpearl.network.ModNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative locator driven solely by persistent cave-encounter indices. */
public final class EchoNeedleService {
    public static final String HINT_SHOWN_KEY = "faded_pearl:echo_needle_hint_shown";
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<UUID, EchoNeedleSyncState> STATES = new HashMap<>();

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % UPDATE_INTERVAL_TICKS != 0L) return;
        FadedPearlSavedData data = FadedPearlSavedData.get(server.overworld());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String dimension = player.serverLevel().dimension().location().toString();
            boolean canGuide = EchoNeedleLocator.canGuide(player.isSpectator(),
                    data.companionOf(player.getUUID()).isPresent(),
                    player.serverLevel().canSeeSky(player.blockPosition()));
            Optional<EchoNeedleLocator.Reading> reading = canGuide
                    ? EchoNeedleLocator.locate(data.echoNeedleCandidates(player.serverLevel().dimension()),
                    dimension, player.getX(), player.getY(), player.getZ(), player.getYRot())
                    : Optional.empty();
            EchoNeedleSyncState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new EchoNeedleSyncState());
            state.update(reading, now).ifPresent(update -> {
                ModNetwork.updateEchoNeedle(player, new EchoNeedlePacket(
                        update.active(), update.sector(), update.height()));
                if (update.active() && !player.getPersistentData().getBoolean(HINT_SHOWN_KEY)) {
                    player.getPersistentData().putBoolean(HINT_SHOWN_KEY, true);
                    ModNetwork.showEchoNeedleHint(player);
                }
            });
        }
    }

    public static void copyHintProgress(Player original, Player clone) {
        if (original.getPersistentData().getBoolean(HINT_SHOWN_KEY))
            clone.getPersistentData().putBoolean(HINT_SHOWN_KEY, true);
    }

    public static void remove(UUID player) {
        STATES.remove(player);
    }

    public static void clear() {
        STATES.clear();
    }

    private EchoNeedleService() {}
}
