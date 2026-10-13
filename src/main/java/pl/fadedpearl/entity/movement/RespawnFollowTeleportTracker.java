package pl.fadedpearl.entity.movement;

import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Transient, server-authoritative state for the short FOLLOW teleport pause after a death respawn.
 * This deliberately has no persistence: logout, server shutdown and ordinary dimension travel do
 * not create or preserve a respawn pause.
 */
public final class RespawnFollowTeleportTracker {
    public static final int BLOCK_TICKS = 60;
    static final int ARRIVAL_WINDOW_TICKS = 200;

    private static final Map<MinecraftServer, Map<UUID, RespawnState>> STATES = new WeakHashMap<>();

    public static synchronized void markDeathRespawn(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        STATES.computeIfAbsent(server, ignored -> new HashMap<>())
                .put(playerId, new RespawnState(server.getTickCount()));
    }

    public static synchronized boolean blocksDistanceFollowTeleport(MinecraftServer server, UUID playerId) {
        RespawnState state = state(server, playerId);
        if (state == null) return false;
        int elapsed = elapsedTicks(state.respawnTick(), server.getTickCount());
        if (elapsed > ARRIVAL_WINDOW_TICKS) {
            removeState(server, playerId);
            return false;
        }
        return elapsed < BLOCK_TICKS;
    }

    public static synchronized boolean consumeArrivalPresentation(MinecraftServer server, UUID playerId) {
        RespawnState state = state(server, playerId);
        if (state == null) return false;
        int elapsed = elapsedTicks(state.respawnTick(), server.getTickCount());
        if (elapsed < BLOCK_TICKS) return false;
        removeState(server, playerId);
        return elapsed <= ARRIVAL_WINDOW_TICKS;
    }

    public static synchronized void clearPlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        Map<UUID, RespawnState> serverStates = STATES.get(server);
        if (serverStates == null) return;
        serverStates.remove(playerId);
        if (serverStates.isEmpty()) STATES.remove(server);
    }

    public static synchronized void clear(MinecraftServer server) {
        if (server != null) STATES.remove(server);
    }

    static boolean isBlockedAt(int respawnTick, int currentTick) {
        return elapsedTicks(respawnTick, currentTick) < BLOCK_TICKS;
    }

    static boolean isPresentationCurrentAt(int respawnTick, int currentTick) {
        int elapsed = elapsedTicks(respawnTick, currentTick);
        return elapsed >= BLOCK_TICKS && elapsed <= ARRIVAL_WINDOW_TICKS;
    }

    private static int elapsedTicks(int respawnTick, int currentTick) {
        return Math.max(0, currentTick - respawnTick);
    }

    private static void removeState(MinecraftServer server, UUID playerId) {
        Map<UUID, RespawnState> serverStates = STATES.get(server);
        if (serverStates == null) return;
        serverStates.remove(playerId);
        if (serverStates.isEmpty()) STATES.remove(server);
    }

    private static RespawnState state(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return null;
        Map<UUID, RespawnState> serverStates = STATES.get(server);
        return serverStates == null ? null : serverStates.get(playerId);
    }

    private record RespawnState(int respawnTick) {}

    private RespawnFollowTeleportTracker() {}
}
