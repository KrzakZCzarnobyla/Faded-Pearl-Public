package pl.fadedpearl.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FadedPearlSavedData extends SavedData {
    private static final String NAME = "faded_pearl_world";
    private UUID endermanId;
    private boolean dead;
    private boolean healed;
    private BlockPos homePos;
    private ResourceKey<Level> homeDimension;
    private BlockPos lastPos;
    private ResourceKey<Level> lastDimension;
    private CompoundTag companionSnapshot;
    private ResourceKey<Level> snapshotDimension;
    private BlockPos snapshotPos;
    private long snapshotGameTime;
    private long recoveryMissingSince;
    private int recoveryEpoch;
    private final ListTag pendingCuriosityReturns = new ListTag();
    private CompanionOwnershipLedger ownership = new CompanionOwnershipLedger();
    private CompanionRecoveryLedger companionRecovery = new CompanionRecoveryLedger();
    private CaveEncounterLedger caveEncounters = new CaveEncounterLedger();

    public static FadedPearlSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FadedPearlSavedData::load, FadedPearlSavedData::new, NAME);
    }

    public static FadedPearlSavedData load(CompoundTag tag) {
        FadedPearlSavedData data = new FadedPearlSavedData();
        if (tag.hasUUID("Enderman")) data.endermanId = tag.getUUID("Enderman");
        data.dead = tag.getBoolean("Dead");
        data.healed = tag.getBoolean("Healed");
        if (tag.contains("HomePos", Tag.TAG_LONG) && tag.contains("HomeDimension", Tag.TAG_STRING)) {
            Optional<ResourceKey<Level>> dimension = dimensionKey(tag.getString("HomeDimension"));
            if (dimension.isPresent()) {
                data.homePos = BlockPos.of(tag.getLong("HomePos"));
                data.homeDimension = dimension.get();
            }
        }
        if (tag.contains("LastPos", Tag.TAG_LONG) && tag.contains("LastDimension", Tag.TAG_STRING)) {
            Optional<ResourceKey<Level>> dimension = dimensionKey(tag.getString("LastDimension"));
            if (dimension.isPresent()) {
                data.lastPos = BlockPos.of(tag.getLong("LastPos"));
                data.lastDimension = dimension.get();
            }
        }
        if (tag.contains("CompanionSnapshot", Tag.TAG_COMPOUND))
            data.companionSnapshot = tag.getCompound("CompanionSnapshot").copy();
        if (tag.contains("SnapshotDimension", Tag.TAG_STRING) && tag.contains("SnapshotPos", Tag.TAG_LONG)) {
            Optional<ResourceKey<Level>> dimension = dimensionKey(tag.getString("SnapshotDimension"));
            if (dimension.isPresent()) {
                data.snapshotDimension = dimension.get();
                data.snapshotPos = BlockPos.of(tag.getLong("SnapshotPos"));
            }
        }
        data.snapshotGameTime = tag.getLong("SnapshotGameTime");
        data.recoveryMissingSince = tag.getLong("RecoveryMissingSince");
        data.recoveryEpoch = Math.max(0, tag.getInt("RecoveryEpoch"));
        if (tag.contains("PendingCuriosityReturns", Tag.TAG_LIST))
            data.pendingCuriosityReturns.addAll(tag.getList("PendingCuriosityReturns", Tag.TAG_COMPOUND).copy());
        else if (tag.contains("PendingCuriosityReturn", Tag.TAG_COMPOUND))
            data.pendingCuriosityReturns.add(tag.getCompound("PendingCuriosityReturn").copy());
        if (tag.contains("CompanionOwnership", Tag.TAG_COMPOUND))
            data.ownership = CompanionOwnershipLedger.load(tag.getCompound("CompanionOwnership"));
        else if (data.endermanId != null) {
            CompoundTag snapshot = data.companionSnapshot;
            if (data.healed && snapshot != null && snapshot.hasUUID("Friend"))
                data.ownership.importLegacyOwner(snapshot.getUUID("Friend"), data.endermanId);
            else if (!data.healed && !data.dead) data.ownership.registerUnclaimed(data.endermanId);
            // A healed legacy companion without a verified Friend stays in the old fields.
            // The live entity must supply its owner before per-player recovery can take over.
            data.setDirty();
        }
        // Early 1.7 saves may have an empty ownership index but a valid legacy snapshot.
        if (data.endermanId != null && data.healed && data.companionSnapshot != null
                && data.companionSnapshot.hasUUID("Friend")
                && data.ownership.ownerOf(data.endermanId).isEmpty()
                && data.ownership.importLegacyOwner(data.companionSnapshot.getUUID("Friend"), data.endermanId))
            data.setDirty();
        if (data.endermanId != null && data.dead && data.ownership.removeUnclaimed(data.endermanId))
            data.setDirty();
        if (tag.contains("CompanionRecovery", Tag.TAG_LIST))
            data.companionRecovery = CompanionRecoveryLedger.load(tag.getList("CompanionRecovery", Tag.TAG_COMPOUND));
        if (data.endermanId != null && data.companionRecovery.importLegacy(data.endermanId, tag)) data.setDirty();
        if (tag.contains("CaveEncounters", Tag.TAG_LIST))
            data.caveEncounters = CaveEncounterLedger.load(tag.getList("CaveEncounters", Tag.TAG_COMPOUND));
        if (data.endermanId != null && !data.caveEncounters.contains(data.endermanId)) {
            ResourceKey<Level> dimension = data.snapshotDimension != null ? data.snapshotDimension : data.lastDimension;
            BlockPos pos = data.snapshotPos != null ? data.snapshotPos : data.lastPos;
            if (dimension != null && pos != null
                    && data.caveEncounters.importLegacy(data.endermanId, dimension.location().toString(), pos))
                data.setDirty();
        }
        return data;
    }

    public Optional<UUID> endermanId() { return Optional.ofNullable(endermanId); }
    public boolean hasSpawned() { return endermanId != null; }
    public boolean isDead() { return dead; }
    public boolean isHealed() { return healed; }
    public Optional<BlockPos> homePos() { return Optional.ofNullable(homePos); }
    public Optional<ResourceKey<Level>> homeDimension() { return Optional.ofNullable(homeDimension); }
    public Optional<BlockPos> lastPos() { return Optional.ofNullable(lastPos); }
    public Optional<ResourceKey<Level>> lastDimension() { return Optional.ofNullable(lastDimension); }
    public Optional<CompoundTag> companionSnapshot() { return Optional.ofNullable(companionSnapshot).map(CompoundTag::copy); }
    public Optional<ResourceKey<Level>> snapshotDimension() { return Optional.ofNullable(snapshotDimension); }
    public Optional<BlockPos> snapshotPos() { return Optional.ofNullable(snapshotPos); }
    public long snapshotGameTime() { return snapshotGameTime; }
    public long recoveryMissingSince() { return recoveryMissingSince; }
    public int recoveryEpoch() { return recoveryEpoch; }
    public Optional<CompoundTag> pendingCuriosityReturn() {
        return pendingCuriosityReturns.isEmpty() ? Optional.empty()
                : Optional.of(pendingCuriosityReturns.getCompound(0).copy());
    }
    public List<CompoundTag> pendingCuriosityReturns() {
        List<CompoundTag> values = new ArrayList<>(pendingCuriosityReturns.size());
        for (int i = 0; i < pendingCuriosityReturns.size(); i++)
            values.add(pendingCuriosityReturns.getCompound(i).copy());
        return List.copyOf(values);
    }

    public Optional<UUID> companionOf(UUID owner) { return ownership.companionOf(owner); }
    public Optional<UUID> ownerOf(UUID companion) { return ownership.ownerOf(companion); }
    /** Reconciles only the original live companion when an old save had no ownership record. */
    public boolean reconcileLegacyOwner(UUID companion, UUID owner) {
        if (!companion.equals(endermanId) || !healed) return false;
        boolean imported = ownership.importLegacyOwner(owner, companion);
        if (imported) setDirty();
        return imported;
    }
    /** Includes the legacy global companion until all old-world data has been reconciled. */
    public List<UUID> recoveryCompanionIds() {
        Set<UUID> ids = new HashSet<>(ownership.ownedCompanionIds());
        if (endermanId != null) ids.add(endermanId);
        return ids.stream().sorted().toList();
    }
    public boolean isUnclaimedCompanion(UUID companion) { return ownership.isUnclaimed(companion); }
    public boolean hasUnclaimedCompanion() { return ownership.hasUnclaimed(); }
    public int unclaimedCompanionCount() { return ownership.unclaimedCount(); }
    public boolean hasUnlocatedLegacyEncounter() {
        return endermanId != null && !dead && !caveEncounters.contains(endermanId);
    }
    public boolean hasUnresolvedLegacyOwner() {
        return endermanId != null && healed && !dead && ownership.ownerOf(endermanId).isEmpty();
    }
    public List<BlockPos> caveEncounterPositions(ResourceKey<Level> dimension) {
        return caveEncounters.sitesIn(dimension.location().toString()).stream()
                .map(CaveEncounterLedger.Site::pos).toList();
    }
    public boolean canPlaceCaveEncounter(ResourceKey<Level> dimension, BlockPos pos) {
        return caveEncounters.canPlace(dimension.location().toString(), pos);
    }
    public boolean canPlaceCaveEncounter(ResourceKey<Level> dimension, BlockPos pos, int minimumHorizontalSpacing) {
        return caveEncounters.canPlace(dimension.location().toString(), pos, minimumHorizontalSpacing);
    }
    public boolean reserveCaveEncounter(UUID companion, ResourceKey<Level> dimension, BlockPos pos) {
        if (!caveEncounters.register(companion, dimension.location().toString(), pos)) return false;
        if (!ownership.registerUnclaimed(companion)) {
            caveEncounters.remove(companion);
            return false;
        }
        setDirty();
        return true;
    }
    public boolean reserveCaveEncounter(UUID companion, ResourceKey<Level> dimension, BlockPos pos,
                                        int minimumHorizontalSpacing) {
        if (!caveEncounters.register(companion, dimension.location().toString(), pos,
                minimumHorizontalSpacing)) return false;
        if (!ownership.registerUnclaimed(companion)) {
            caveEncounters.remove(companion);
            return false;
        }
        setDirty();
        return true;
    }
    public void rollbackCaveEncounter(UUID companion) {
        boolean unclaimedRemoved = ownership.removeUnclaimed(companion);
        boolean siteRemoved = caveEncounters.remove(companion);
        if (unclaimedRemoved || siteRemoved) setDirty();
    }
    public Optional<CompoundTag> companionSnapshot(UUID companion) { return companionRecovery.snapshot(companion); }
    public Optional<ResourceKey<Level>> snapshotDimension(UUID companion) {
        return companionRecovery.snapshotDimension(companion).flatMap(FadedPearlSavedData::dimensionKey);
    }
    public Optional<BlockPos> snapshotPos(UUID companion) { return companionRecovery.snapshotPos(companion); }
    public Optional<ResourceKey<Level>> lastDimension(UUID companion) {
        return companionRecovery.lastDimension(companion).flatMap(FadedPearlSavedData::dimensionKey);
    }
    public Optional<BlockPos> lastPos(UUID companion) { return companionRecovery.lastPos(companion); }
    public long snapshotGameTime(UUID companion) { return companionRecovery.snapshotGameTime(companion); }
    public long recoveryMissingSince(UUID companion) { return companionRecovery.missingSince(companion); }
    public int recoveryEpoch(UUID companion) { return companionRecovery.epoch(companion); }
    public boolean isDead(UUID companion) { return companionRecovery.isDead(companion); }
    public boolean isHealed(UUID companion) { return companionRecovery.isHealed(companion); }
    public Optional<BlockPos> homePos(UUID companion) { return companionRecovery.homePos(companion); }
    public Optional<ResourceKey<Level>> homeDimension(UUID companion) {
        return companionRecovery.homeDimension(companion).flatMap(FadedPearlSavedData::dimensionKey);
    }
    public boolean registerUnclaimedCompanion(UUID companion) {
        boolean added = ownership.registerUnclaimed(companion);
        if (added) setDirty();
        return added;
    }
    public boolean claimCompanion(UUID owner, UUID companion) {
        CompanionOwnershipLedger.ClaimResult result = ownership.claim(owner, companion);
        if (result == CompanionOwnershipLedger.ClaimResult.CLAIMED) setDirty();
        return result == CompanionOwnershipLedger.ClaimResult.CLAIMED
                || result == CompanionOwnershipLedger.ClaimResult.ALREADY_OWNED;
    }

    /** The first successful healer atomically claims one unowned companion on the server thread. */
    public synchronized boolean claimForHealing(UUID owner, UUID companion) {
        // A healed legacy record without a verified Friend must not become a free companion.
        if (companion.equals(endermanId) && healed && ownership.ownerOf(companion).isEmpty()) return false;
        if (ownership.ownerOf(companion).isEmpty() && !ownership.isUnclaimed(companion))
            ownership.registerUnclaimed(companion);
        CompanionOwnershipLedger.ClaimResult result = ownership.claim(owner, companion);
        if (result != CompanionOwnershipLedger.ClaimResult.CLAIMED
                && result != CompanionOwnershipLedger.ClaimResult.ALREADY_OWNED) return false;
        if (endermanId == null) endermanId = companion;
        setDirty();
        return true;
    }

    public void markSpawned(UUID id) { endermanId = id; ownership.registerUnclaimed(id); setDirty(); }
    public boolean claimEnderman(UUID id) {
        if (endermanId == null) {
            endermanId = id;
            setDirty();
            return true;
        }
        return endermanId.equals(id);
    }
    public void markDead() {
        dead = true;
        if (endermanId != null) {
            companionRecovery.markDead(endermanId);
            ownership.removeUnclaimed(endermanId);
        }
        setDirty();
    }
    public void markDead(UUID companion) {
        if (companion.equals(endermanId)) { markDead(); return; }
        companionRecovery.markDead(companion);
        ownership.removeUnclaimed(companion);
        setDirty();
    }
    public void markHealed() {
        healed = true;
        if (endermanId != null) companionRecovery.markHealed(endermanId);
        setDirty();
    }
    public void markHealed(UUID companion) {
        if (companion.equals(endermanId)) { markHealed(); return; }
        companionRecovery.markHealed(companion);
        setDirty();
    }
    public void setHome(ResourceKey<Level> dimension, BlockPos pos) {
        homeDimension = dimension; homePos = pos.immutable(); setDirty();
        if (endermanId != null) companionRecovery.setHome(endermanId, dimension.location().toString(), pos);
    }
    public void setHome(UUID companion, ResourceKey<Level> dimension, BlockPos pos) {
        if (companion.equals(endermanId)) { setHome(dimension, pos); return; }
        companionRecovery.setHome(companion, dimension.location().toString(), pos);
        setDirty();
    }
    public void updateLastKnown(ResourceKey<Level> dimension, BlockPos pos) {
        if (lastPos == null || !lastPos.closerThan(pos, 4.0D) || lastDimension == null || !lastDimension.equals(dimension)) {
            lastDimension = dimension; lastPos = pos.immutable(); setDirty();
        }
        if (endermanId != null && companionRecovery.updateLastKnown(endermanId, dimension.location().toString(), pos)) setDirty();
        if (endermanId != null && caveEncounters.importLegacy(endermanId, dimension.location().toString(), pos)) setDirty();
    }
    public void updateLastKnown(UUID companion, ResourceKey<Level> dimension, BlockPos pos) {
        if (companion.equals(endermanId)) { updateLastKnown(dimension, pos); return; }
        if (companionRecovery.updateLastKnown(companion, dimension.location().toString(), pos)) setDirty();
    }
    public void updateCompanionSnapshot(ResourceKey<Level> dimension, BlockPos pos, long gameTime,
                                        CompoundTag snapshot, int epoch) {
        companionSnapshot = snapshot.copy(); snapshotDimension = dimension; snapshotPos = pos.immutable();
        snapshotGameTime = gameTime; recoveryEpoch = Math.max(recoveryEpoch, epoch); recoveryMissingSince = 0L;
        updateLastKnown(dimension, pos); setDirty();
        if (endermanId != null) companionRecovery.updateSnapshot(endermanId, dimension.location().toString(), pos,
                gameTime, snapshot, epoch);
    }
    public void updateCompanionSnapshot(UUID companion, ResourceKey<Level> dimension, BlockPos pos,
                                        long gameTime, CompoundTag snapshot, int epoch) {
        if (companion.equals(endermanId)) { updateCompanionSnapshot(dimension, pos, gameTime, snapshot, epoch); return; }
        companionRecovery.updateSnapshot(companion, dimension.location().toString(), pos, gameTime, snapshot, epoch);
        setDirty();
    }
    public void setRecoveryMissingSince(long value) {
        if (recoveryMissingSince != value) { recoveryMissingSince = value; setDirty(); }
        if (endermanId != null && companionRecovery.setMissingSince(endermanId, value)) setDirty();
    }
    public void setRecoveryMissingSince(UUID companion, long value) {
        if (companion.equals(endermanId)) { setRecoveryMissingSince(value); return; }
        if (companionRecovery.setMissingSince(companion, value)) setDirty();
    }
    public void commitRecoveryEpoch(int value) {
        recoveryEpoch = Math.max(recoveryEpoch, value);
        if (endermanId != null) companionRecovery.commitEpoch(endermanId, value);
        setDirty();
    }
    public void commitRecoveryEpoch(UUID companion, int value) {
        if (companion.equals(endermanId)) { commitRecoveryEpoch(value); return; }
        if (companionRecovery.commitEpoch(companion, value)) setDirty();
    }
    public boolean escrowCuriosityReturn(CompoundTag value, int entityEpoch) {
        if (entityEpoch < recoveryEpoch) return false;
        return queueCuriosityReturn(value);
    }
    public boolean escrowCuriosityReturn(UUID companion, CompoundTag value, int entityEpoch) {
        if (!value.hasUUID("Owner") || !companion.equals(value.getUUID("Owner"))
                || !value.hasUUID("Friend")
                || ownerOf(companion).filter(value.getUUID("Friend")::equals).isEmpty()
                || entityEpoch < recoveryEpoch(companion)) return false;
        return queueCuriosityReturn(value);
    }
    private boolean queueCuriosityReturn(CompoundTag value) {
        for (int i = 0; i < pendingCuriosityReturns.size(); i++)
            if (pendingCuriosityReturns.getCompound(i).equals(value)) return true;
        pendingCuriosityReturns.add(value.copy());
        setDirty();
        return true;
    }
    public void clearPendingCuriosityReturn() {
        if (!pendingCuriosityReturns.isEmpty()) { pendingCuriosityReturns.remove(0); setDirty(); }
    }
    public void clearPendingCuriosityReturn(CompoundTag value) {
        for (int i = 0; i < pendingCuriosityReturns.size(); i++)
            if (pendingCuriosityReturns.getCompound(i).equals(value)) {
                pendingCuriosityReturns.remove(i);
                setDirty();
                return;
            }
    }

    private static Optional<ResourceKey<Level>> dimensionKey(String value) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        return location == null ? Optional.empty() : Optional.of(ResourceKey.create(Registries.DIMENSION, location));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (endermanId != null) tag.putUUID("Enderman", endermanId);
        tag.putBoolean("Dead", dead);
        tag.putBoolean("Healed", healed);
        if (homePos != null && homeDimension != null) {
            tag.putLong("HomePos", homePos.asLong());
            tag.putString("HomeDimension", homeDimension.location().toString());
        }
        if (lastPos != null && lastDimension != null) {
            tag.putLong("LastPos", lastPos.asLong());
            tag.putString("LastDimension", lastDimension.location().toString());
        }
        if (companionSnapshot != null) tag.put("CompanionSnapshot", companionSnapshot.copy());
        if (snapshotDimension != null && snapshotPos != null) {
            tag.putString("SnapshotDimension", snapshotDimension.location().toString());
            tag.putLong("SnapshotPos", snapshotPos.asLong());
        }
        tag.putLong("SnapshotGameTime", snapshotGameTime);
        tag.putLong("RecoveryMissingSince", recoveryMissingSince);
        tag.putInt("RecoveryEpoch", recoveryEpoch);
        if (!pendingCuriosityReturns.isEmpty()) tag.put("PendingCuriosityReturns", pendingCuriosityReturns.copy());
        tag.put("CompanionOwnership", ownership.save());
        tag.put("CompanionRecovery", companionRecovery.save());
        tag.put("CaveEncounters", caveEncounters.save());
        return tag;
    }
}
