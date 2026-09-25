package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import pl.fadedpearl.block.ResonatingAnchorBlock;
import pl.fadedpearl.entity.FadedEnderman;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AnchorRecallService {
    private static final int MAX_ATTEMPTS = 8;
    private static final int RETRY_DELAY_TICKS = 10;
    private static final int SOURCE_CHUNK_RADIUS = 1;
    private static final Map<UUID, PendingRecall> PENDING = new HashMap<>();

    /** A loaded pre-1.7 companion can repair an old save that lacks a verified snapshot. */
    public static Optional<UUID> companionIdFor(ServerPlayer player, FadedPearlSavedData data) {
        Optional<UUID> owned = data.companionOf(player.getUUID());
        if (owned.isPresent()) return owned;
        UUID legacy = data.endermanId().orElse(null);
        FadedEnderman loaded = findLoaded(player.getServer(), legacy);
        if (loaded == null && legacy != null && data.lastDimension(legacy).isPresent()
                && data.lastPos(legacy).isPresent()) {
            ServerLevel source = player.getServer().getLevel(data.lastDimension(legacy).orElseThrow());
            if (source != null) {
                source.getChunk(data.lastPos(legacy).orElseThrow());
                loaded = findLoaded(player.getServer(), legacy);
            }
        }
        if (loaded != null && loaded.isHealed() && loaded.isFriend(player)
                && data.reconcileLegacyOwner(legacy, player.getUUID())) return Optional.of(legacy);
        return Optional.empty();
    }

    public static void request(ServerLevel anchorLevel, BlockPos anchorPos, ServerPlayer player) {
        FadedPearlSavedData data = FadedPearlSavedData.get(anchorLevel);
        UUID companionId = companionIdFor(player, data).orElse(null);
        if (companionId == null || data.isDead(companionId) || !data.isHealed(companionId)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.faded_pearl.anchor.silent"));
            return;
        }
        CompanionRecoveryService.requestRecovery(player);
        PendingRecall previous = PENDING.put(player.getUUID(),
                new PendingRecall(companionId, anchorLevel.dimension(), anchorPos.immutable(), anchorLevel.getServer().getTickCount() + 1));
        if (previous != null) previous.releaseTickets(anchorLevel.getServer());
    }

    public static void cancel(MinecraftServer server, UUID playerId) {
        PendingRecall pending = PENDING.remove(playerId);
        if (pending != null) pending.releaseTickets(server);
    }

    public static void clear(MinecraftServer server) {
        PENDING.values().forEach(pending -> pending.releaseTickets(server));
        PENDING.clear();
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<Map.Entry<UUID, PendingRecall>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRecall> entry = iterator.next();
            PendingRecall pending = entry.getValue();
            if (now < pending.nextAttemptTick) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || attempt(server, player, pending)) {
                pending.releaseTickets(server);
                iterator.remove();
            } else if (++pending.attempts >= MAX_ATTEMPTS) {
                sendDirection(server, player, pending);
                pending.releaseTickets(server);
                iterator.remove();
            } else {
                pending.nextAttemptTick = now + RETRY_DELAY_TICKS;
            }
        }
    }

    private static boolean attempt(MinecraftServer server, ServerPlayer player, PendingRecall pending) {
        ServerLevel anchorLevel = server.getLevel(pending.anchorDimension);
        if (anchorLevel == null) return true;
        FadedPearlSavedData data = FadedPearlSavedData.get(anchorLevel);
        if (!data.companionOf(player.getUUID()).filter(pending.companionId::equals).isPresent()
                || data.isDead(pending.companionId) || !data.isHealed(pending.companionId)) return true;
        FadedEnderman companion = findLoaded(server, pending.companionId);
        if (companion == null) companion = loadTracked(server, data, pending);
        if (companion == null) return false;
        if (companion.getRecoveryEpoch() < data.recoveryEpoch(pending.companionId)) return false;
        if (!companion.isHealed() || !companion.isFriend(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.faded_pearl.anchor.silent"));
            return true;
        }
        if (companion.level() != anchorLevel) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.faded_pearl.anchor.other_dimension"));
            return true;
        }
        companion.setHome(anchorLevel.dimension(), pending.anchorPos);
        var state = anchorLevel.getBlockState(pending.anchorPos);
        if (state.hasProperty(ResonatingAnchorBlock.ACTIVE)) {
            anchorLevel.setBlock(pending.anchorPos, state.setValue(ResonatingAnchorBlock.ACTIVE, true), 3);
        }
        companion.recallTo(anchorLevel, pending.anchorPos.above());
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.faded_pearl.anchor.recalled"));
        return true;
    }

    private static FadedEnderman findLoaded(MinecraftServer server, UUID id) {
        if (id == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof FadedEnderman companion) return companion;
        }
        return null;
    }

    private static FadedEnderman loadTracked(MinecraftServer server, FadedPearlSavedData data, PendingRecall pending) {
        Optional<ResourceKey<Level>> dimension = data.lastDimension(pending.companionId);
        Optional<BlockPos> position = data.lastPos(pending.companionId);
        if (dimension.isEmpty() || position.isEmpty()) return null;
        ServerLevel level = server.getLevel(dimension.get());
        if (level == null) return null;
        ChunkPos center = new ChunkPos(position.get());
        pending.ensureTickets(server, dimension.get(), center, position.get());
        for (int dx = -SOURCE_CHUNK_RADIUS; dx <= SOURCE_CHUNK_RADIUS; dx++) {
            for (int dz = -SOURCE_CHUNK_RADIUS; dz <= SOURCE_CHUNK_RADIUS; dz++) {
                level.getChunk(center.x + dx, center.z + dz);
            }
        }
        return findLoaded(server, pending.companionId);
    }

    private static void sendDirection(MinecraftServer server, ServerPlayer player, PendingRecall pending) {
        ServerLevel anchorLevel = server.getLevel(pending.anchorDimension);
        if (anchorLevel == null) return;
        BlockPos target = FadedPearlSavedData.get(anchorLevel).lastPos(pending.companionId).orElse(pending.anchorPos);
        int dx = target.getX() - pending.anchorPos.getX();
        int dz = target.getZ() - pending.anchorPos.getZ();
        String direction = Math.abs(dx) > Math.abs(dz) ? (dx >= 0 ? "E" : "W") : (dz >= 0 ? "S" : "N");
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.faded_pearl.anchor.direction", direction));
    }

    private static final class PendingRecall {
        private final UUID companionId;
        private final ResourceKey<Level> anchorDimension;
        private final BlockPos anchorPos;
        private int attempts;
        private int nextAttemptTick;
        private ResourceKey<Level> ticketDimension;
        private ChunkPos ticketCenter;
        private BlockPos ticketId;

        private PendingRecall(UUID companionId, ResourceKey<Level> anchorDimension, BlockPos anchorPos, int nextAttemptTick) {
            this.companionId = companionId;
            this.anchorDimension = anchorDimension;
            this.anchorPos = anchorPos;
            this.nextAttemptTick = nextAttemptTick;
        }

        private void ensureTickets(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos center, BlockPos id) {
            if (dimension.equals(ticketDimension) && center.equals(ticketCenter) && id.equals(ticketId)) return;
            releaseTickets(server);
            ServerLevel level = server.getLevel(dimension);
            if (level == null) return;
            ticketDimension = dimension;
            ticketCenter = center;
            ticketId = id.immutable();
            CompanionSourceTickets.acquire(server, ticketDimension, ticketCenter, ticketId);
        }

        private void releaseTickets(MinecraftServer server) {
            if (ticketDimension == null || ticketCenter == null || ticketId == null) return;
            CompanionSourceTickets.release(server, ticketDimension, ticketCenter, ticketId);
            ticketDimension = null;
            ticketCenter = null;
            ticketId = null;
        }
    }

    private AnchorRecallService() {}
}
