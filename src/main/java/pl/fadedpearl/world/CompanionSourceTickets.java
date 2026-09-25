package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Shared source-chunk tickets for anchor recall and missing-companion recovery. Server-thread only. */
final class CompanionSourceTickets {
    private static final int RADIUS = 1;
    private static final int LEVEL = 2;
    private static final ReferenceCounter<Key> USERS = new ReferenceCounter<>();

    static void acquire(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos center, BlockPos id) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;
        Key key = new Key(dimension, center, id.immutable());
        if (!USERS.acquire(key)) return;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, LEVEL, key.id);
        }
    }

    static void release(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos center, BlockPos id) {
        Key key = new Key(dimension, center, id);
        if (!USERS.release(key)) return;
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
            level.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk, LEVEL, key.id);
        }
    }

    static final class ReferenceCounter<K> {
        private final java.util.Map<K, Integer> users = new java.util.HashMap<>();

        /** Returns true only when the caller must create the shared external resource. */
        boolean acquire(K key) {
            return users.merge(key, 1, Integer::sum) == 1;
        }

        /** Returns true only when the caller must remove the shared external resource. */
        boolean release(K key) {
            Integer count = users.get(key);
            if (count == null) return false;
            if (count > 1) {
                users.put(key, count - 1);
                return false;
            }
            users.remove(key);
            return true;
        }

        int users(K key) {
            return users.getOrDefault(key, 0);
        }
    }

    private record Key(ResourceKey<Level> dimension, ChunkPos center, BlockPos id) {}
    private CompanionSourceTickets() {}
}
