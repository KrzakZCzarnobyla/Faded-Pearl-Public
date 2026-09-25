package pl.fadedpearl.world;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import pl.fadedpearl.entity.FadedEnderman;
import pl.fadedpearl.registry.ModEntities;
import pl.fadedpearl.entity.curiosity.CuriosityEscrowDelivery;
import pl.fadedpearl.config.FadedServerConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CompanionRecoveryService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SOURCE_RADIUS = 1;
    private static final Map<UUID, Long> TRANSITION_GRACE = new HashMap<>();
    private static final Map<UUID, Long> LEGACY_RECONCILE_RETRY = new HashMap<>();
    private static final Map<UUID, SourceTickets> SOURCE_TICKETS = new HashMap<>();
    private static long serverReadyAfter;

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (serverReadyAfter == 0L) serverReadyAfter = now + FadedServerConfig.recoveryGraceTicks();
        if (now % 20L != 0L) return;
        FadedPearlSavedData data = FadedPearlSavedData.get(server.overworld());
        deliverPendingCuriosityReturn(server, data);
        reconcileUnresolvedLegacy(server, data, now);
        var companions = data.recoveryCompanionIds();
        Set<UUID> tracked = new HashSet<>(companions);
        for (UUID id : Set.copyOf(SOURCE_TICKETS.keySet()))
            if (!tracked.contains(id)) releaseTickets(server, id);
        for (UUID id : companions) tickCompanion(server, data, id, now);
    }

    private static void reconcileUnresolvedLegacy(MinecraftServer server, FadedPearlSavedData data, long now) {
        if (!data.hasUnresolvedLegacyOwner()) return;
        UUID id = data.endermanId().orElseThrow();
        if (now < LEGACY_RECONCILE_RETRY.getOrDefault(id, 0L)) return;
        LEGACY_RECONCILE_RETRY.put(id, now + FadedServerConfig.recoveryGraceTicks());
        FadedEnderman live = findCanonical(server, data, id, data.recoveryEpoch(id));
        if (live == null) {
            loadLastKnown(server, data, id);
            live = findCanonical(server, data, id, data.recoveryEpoch(id));
        }
        if (live != null && live.isHealed())
            live.getFriendId().ifPresent(friend -> data.reconcileLegacyOwner(id, friend));
        releaseTickets(server, id);
    }

    private static void tickCompanion(MinecraftServer server, FadedPearlSavedData data, UUID id, long now) {
        if (data.isDead(id) || !data.isHealed(id)) { releaseTickets(server, id); return; }

        FadedEnderman canonical = findCanonical(server, data, id, data.recoveryEpoch(id));
        if (canonical != null) {
            releaseTickets(server, id);
            snapshotLiveCompanion(data, canonical, id, now);
            return;
        }

        UUID ownerId = data.ownerOf(id).orElse(null);
        if (ownerId == null || server.getPlayerList().getPlayer(ownerId) == null) {
            releaseTickets(server, id);
            return;
        }

        loadLastKnown(server, data, id);
        canonical = findCanonical(server, data, id, data.recoveryEpoch(id));
        if (canonical != null) {
            releaseTickets(server, id);
            snapshotLiveCompanion(data, canonical, id, now);
            return;
        }

        long missingSince = data.recoveryMissingSince(id);
        if (missingSince == 0L) { data.setRecoveryMissingSince(id, now); return; }
        long transitionUntil = TRANSITION_GRACE.getOrDefault(id, 0L);
        if (now < serverReadyAfter || now - missingSince < FadedServerConfig.recoveryGraceTicks()
                || now < transitionUntil) return;
        restore(server, data, id, now);
    }

    private static void snapshotLiveCompanion(FadedPearlSavedData data, FadedEnderman canonical, UUID id, long now) {
        CompoundTag snapshot = canonical.createRecoverySnapshot();
        if (snapshot.hasUUID("Friend") && data.ownerOf(id).isEmpty())
            data.reconcileLegacyOwner(id, snapshot.getUUID("Friend"));
        Optional<UUID> snapshotFriend = snapshot.hasUUID("Friend")
                ? Optional.of(snapshot.getUUID("Friend")) : Optional.empty();
        if (!CompanionRecoveryGuard.matchesOwner(data.ownerOf(id), snapshotFriend)) {
            LOGGER.error("Refusing recovery snapshot for companion {}: live Friend disagrees with owner ledger", id);
            return;
        }
        data.updateCompanionSnapshot(id, ((ServerLevel) canonical.level()).dimension(), canonical.blockPosition(), now,
                snapshot, canonical.getRecoveryEpoch());
    }

    public static void noteTransition(MinecraftServer server, FadedEnderman companion) {
        long now = server.overworld().getGameTime();
        TRANSITION_GRACE.put(companion.getUUID(), now + FadedServerConfig.recoveryGraceTicks());
        FadedPearlSavedData.get(server.overworld()).setRecoveryMissingSince(companion.getUUID(), 0L);
        releaseTickets(server, companion.getUUID());
    }

    public static void requestRecovery(ServerPlayer player) {
        FadedPearlSavedData data = FadedPearlSavedData.get(player.serverLevel());
        data.companionOf(player.getUUID()).ifPresent(id -> {
            if (data.recoveryMissingSince(id) == 0L)
                data.setRecoveryMissingSince(id, player.serverLevel().getServer().overworld().getGameTime());
        });
    }

    public static void clear(MinecraftServer server) {
        for (UUID id : Set.copyOf(SOURCE_TICKETS.keySet())) releaseTickets(server, id);
        TRANSITION_GRACE.clear();
        LEGACY_RECONCILE_RETRY.clear();
        serverReadyAfter = 0L;
    }

    private static FadedEnderman findCanonical(MinecraftServer server, FadedPearlSavedData data,
                                                UUID id, int authoritativeEpoch) {
        FadedEnderman canonical = null;
        Optional<UUID> owner = data.ownerOf(id);
        boolean transitionGraceActive = server.overworld().getGameTime()
                < TRANSITION_GRACE.getOrDefault(id, 0L);
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof FadedEnderman candidate)) continue;
            if (!CompanionRecoveryGuard.matchesOwner(owner, candidate.getFriendId())) {
                LOGGER.error("Discarding companion copy {}: live Friend disagrees with owner ledger", id);
                candidate.discard();
                continue;
            }
            if (candidate.getRecoveryEpoch() < authoritativeEpoch) {
                LOGGER.warn("Discarding stale recovered companion copy {} with epoch {} (authoritative {})",
                        id, candidate.getRecoveryEpoch(), authoritativeEpoch);
                candidate.discard();
                continue;
            }
            if (canonical == null) {
                canonical = candidate;
            } else {
                switch (CompanionRecoveryGuard.chooseDuplicate(canonical.getRecoveryEpoch(),
                        candidate.getRecoveryEpoch(), transitionGraceActive)) {
                    case REPLACE_CURRENT -> {
                        LOGGER.warn("Discarding duplicate companion {} with epoch {} in favor of epoch {}",
                                id, canonical.getRecoveryEpoch(), candidate.getRecoveryEpoch());
                        canonical.discard();
                        canonical = candidate;
                    }
                    case DEFER_EQUAL_DURING_TRANSITION -> {
                        // A dimension transfer may expose both sides briefly. Let vanilla finish removing
                        // the source before treating an equal-epoch copy as a persistent duplicate.
                    }
                    case KEEP_CURRENT -> {
                        LOGGER.warn("Discarding duplicate companion {} with epoch {} while canonical epoch is {}",
                                id, candidate.getRecoveryEpoch(), canonical.getRecoveryEpoch());
                        candidate.discard();
                    }
                }
            }
        }
        return canonical;
    }

    private static void loadLastKnown(MinecraftServer server, FadedPearlSavedData data, UUID id) {
        Optional<ResourceKey<Level>> dimension = data.snapshotDimension(id).isPresent()
                ? data.snapshotDimension(id) : data.lastDimension(id);
        Optional<BlockPos> position = data.snapshotPos(id).isPresent() ? data.snapshotPos(id) : data.lastPos(id);
        if (dimension.isEmpty() || position.isEmpty()) return;
        ServerLevel level = server.getLevel(dimension.get());
        if (level == null) return;
        ChunkPos center = new ChunkPos(position.get());
        ensureTickets(server, id, dimension.get(), center, position.get());
        for (int dx = -SOURCE_RADIUS; dx <= SOURCE_RADIUS; dx++)
            for (int dz = -SOURCE_RADIUS; dz <= SOURCE_RADIUS; dz++) level.getChunk(center.x + dx, center.z + dz);
    }

    private static void restore(MinecraftServer server, FadedPearlSavedData data, UUID id, long now) {
        Optional<net.minecraft.nbt.CompoundTag> snapshot = data.companionSnapshot(id);
        Optional<UUID> owner = data.ownerOf(id);
        Optional<UUID> snapshotFriend = snapshot.filter(value -> value.hasUUID("Friend"))
                .map(value -> value.getUUID("Friend"));
        if (snapshot.isEmpty() || owner.isEmpty()
                || !CompanionRecoveryGuard.matchesOwner(owner, snapshotFriend)) return;
        UUID friendId = snapshotFriend.orElseThrow();
        ServerPlayer friend = server.getPlayerList().getPlayer(friendId);
        if (friend == null) return;
        ServerLevel target = friend.serverLevel();
        BlockPos landing = findSafeLanding(target, friend.blockPosition(), 8, 12);
        if (landing == null || findCanonical(server, data, id, data.recoveryEpoch(id)) != null) return;

        FadedEnderman recovered = ModEntities.FADED_ENDERMAN.get().create(target);
        if (recovered == null) return;
        int nextEpoch = data.recoveryEpoch(id) + 1;
        recovered.applyRecoverySnapshot(snapshot.get(), nextEpoch);
        recovered.setUUID(id);
        recovered.moveTo(Vec3.atBottomCenterOf(landing));
        if (!target.noCollision(recovered) || target.containsAnyLiquid(recovered.getBoundingBox())) return;
        if (!target.addFreshEntity(recovered)) return;
        data.commitRecoveryEpoch(id, nextEpoch);
        data.updateCompanionSnapshot(id, target.dimension(), landing, now, recovered.createRecoverySnapshot(), nextEpoch);
        releaseTickets(server, id);
        LOGGER.warn("Recovered missing Faded Enderman {} near friend {} with epoch {}", id, friend.getUUID(), nextEpoch);
    }

    private static void deliverPendingCuriosityReturn(MinecraftServer server, FadedPearlSavedData data) {
        for (CompoundTag escrow : data.pendingCuriosityReturns()) {
            if (!escrow.hasUUID("Friend") || !escrow.contains("Stack")) continue;
            UUID friendId = escrow.getUUID("Friend");
            if (escrow.hasUUID("Owner")
                    && data.ownerOf(escrow.getUUID("Owner")).filter(friendId::equals).isEmpty()) continue;
            ServerPlayer friend = server.getPlayerList().getPlayer(friendId);
            if (friend == null) continue;
            ItemStack exact = ItemStack.of(escrow.getCompound("Stack"));
            if (exact.isEmpty()) { data.clearPendingCuriosityReturn(escrow); continue; }
            ItemStack attempt = exact.copy();
            Vec3 pos = friend.position();
            CuriosityEscrowDelivery.execute(
                    () -> friend.getInventory().add(attempt),
                    () -> friend.serverLevel().addFreshEntity(
                            new ItemEntity(friend.serverLevel(), pos.x, pos.y + 0.5D, pos.z, attempt)),
                    () -> data.clearPendingCuriosityReturn(escrow));
        }
    }

    private static BlockPos findSafeLanding(ServerLevel level, BlockPos origin, int radius, int verticalRange) {
        for (int r = 1; r <= radius; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
            for (int dy = 2; dy >= -verticalRange; dy--) {
                BlockPos feet = origin.offset(dx, dy, dz); BlockPos support = feet.below();
                var supportState = level.getBlockState(support);
                if (!supportState.isFaceSturdy(level, support, Direction.UP) || supportState.is(Blocks.LAVA)
                        || supportState.is(Blocks.WATER) || !level.getFluidState(support).isEmpty()) continue;
                if (!level.isEmptyBlock(feet) || !level.isEmptyBlock(feet.above()) || !level.isEmptyBlock(feet.above(2))
                        || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                return feet.immutable();
            }
        }
        return null;
    }

    private static void ensureTickets(MinecraftServer server, UUID companion, ResourceKey<Level> dimension, ChunkPos center, BlockPos position) {
        SourceTickets current = SOURCE_TICKETS.get(companion);
        if (current != null && current.matches(dimension, center, position)) return;
        releaseTickets(server, companion);
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;
        SourceTickets tickets = new SourceTickets(dimension, center, position.immutable());
        SOURCE_TICKETS.put(companion, tickets);
        CompanionSourceTickets.acquire(server, dimension, center, tickets.id);
    }

    private static void releaseTickets(MinecraftServer server, UUID companion) {
        SourceTickets tickets = SOURCE_TICKETS.remove(companion);
        if (tickets == null) return;
        CompanionSourceTickets.release(server, tickets.dimension, tickets.center, tickets.id);
    }

    private record SourceTickets(ResourceKey<Level> dimension, ChunkPos center, BlockPos id) {
        private boolean matches(ResourceKey<Level> otherDimension, ChunkPos otherCenter, BlockPos otherId) {
            return dimension.equals(otherDimension) && center.equals(otherCenter) && id.equals(otherId);
        }
    }

    private CompanionRecoveryService() {}
}
