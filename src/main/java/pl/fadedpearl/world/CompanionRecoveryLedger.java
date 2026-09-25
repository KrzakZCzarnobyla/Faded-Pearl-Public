package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Per-companion recovery state. The old world-wide fields remain readable during 1.7 migration. */
final class CompanionRecoveryLedger {
    private static final String COMPANION = "Companion";
    private static final String SNAPSHOT = "CompanionSnapshot";
    private static final String SNAPSHOT_DIMENSION = "SnapshotDimension";
    private static final String SNAPSHOT_POS = "SnapshotPos";
    private static final String SNAPSHOT_GAME_TIME = "SnapshotGameTime";
    private static final String LAST_DIMENSION = "LastDimension";
    private static final String LAST_POS = "LastPos";
    private static final String MISSING_SINCE = "RecoveryMissingSince";
    private static final String EPOCH = "RecoveryEpoch";
    private static final String DEAD = "Dead";
    private static final String HEALED = "Healed";
    private static final String HOME_DIMENSION = "HomeDimension";
    private static final String HOME_POS = "HomePos";
    private static final String[] LEGACY_KEYS = {SNAPSHOT, SNAPSHOT_DIMENSION, SNAPSHOT_POS,
            SNAPSHOT_GAME_TIME, LAST_DIMENSION, LAST_POS, MISSING_SINCE, EPOCH,
            DEAD, HEALED, HOME_DIMENSION, HOME_POS};

    private final Map<UUID, CompoundTag> records = new HashMap<>();

    boolean hasRecord(UUID companion) { return records.containsKey(companion); }

    boolean importLegacy(UUID companion, CompoundTag worldTag) {
        CompoundTag record = records.get(companion);
        boolean changed = false;
        if (record == null) {
            record = new CompoundTag();
            records.put(companion, record);
            changed = true;
        }
        for (String key : LEGACY_KEYS) {
            int expectedType = expectedType(key);
            Tag value = worldTag.get(key);
            if (value != null && value.getId() == expectedType && !record.contains(key, expectedType)) {
                record.put(key, value.copy());
                changed = true;
            }
        }
        return changed;
    }

    private static int expectedType(String key) {
        return switch (key) {
            case SNAPSHOT -> Tag.TAG_COMPOUND;
            case SNAPSHOT_DIMENSION, LAST_DIMENSION, HOME_DIMENSION -> Tag.TAG_STRING;
            case SNAPSHOT_POS, SNAPSHOT_GAME_TIME, LAST_POS, MISSING_SINCE, HOME_POS -> Tag.TAG_LONG;
            case EPOCH -> Tag.TAG_INT;
            case DEAD, HEALED -> Tag.TAG_BYTE;
            default -> Tag.TAG_END;
        };
    }

    Optional<CompoundTag> snapshot(UUID companion) {
        CompoundTag record = records.get(companion);
        return record == null || !record.contains(SNAPSHOT, Tag.TAG_COMPOUND) ? Optional.empty()
                : Optional.of(record.getCompound(SNAPSHOT).copy());
    }

    Optional<String> snapshotDimension(UUID companion) {
        return dimension(companion, SNAPSHOT_DIMENSION);
    }

    Optional<BlockPos> snapshotPos(UUID companion) { return position(companion, SNAPSHOT_POS); }
    Optional<String> lastDimension(UUID companion) { return dimension(companion, LAST_DIMENSION); }
    Optional<BlockPos> lastPos(UUID companion) { return position(companion, LAST_POS); }
    long snapshotGameTime(UUID companion) { return read(companion).getLong(SNAPSHOT_GAME_TIME); }
    long missingSince(UUID companion) { return read(companion).getLong(MISSING_SINCE); }
    int epoch(UUID companion) { return Math.max(0, read(companion).getInt(EPOCH)); }
    boolean isDead(UUID companion) { return read(companion).getBoolean(DEAD); }
    boolean isHealed(UUID companion) { return read(companion).getBoolean(HEALED); }
    Optional<String> homeDimension(UUID companion) { return dimension(companion, HOME_DIMENSION); }
    Optional<BlockPos> homePos(UUID companion) { return position(companion, HOME_POS); }

    void markDead(UUID companion) { mutable(companion).putBoolean(DEAD, true); }
    void markHealed(UUID companion) { mutable(companion).putBoolean(HEALED, true); }
    void setHome(UUID companion, String dimension, BlockPos pos) {
        CompoundTag record = mutable(companion);
        record.putString(HOME_DIMENSION, dimension);
        record.putLong(HOME_POS, pos.asLong());
    }

    boolean updateLastKnown(UUID companion, String dimension, BlockPos pos) {
        CompoundTag record = mutable(companion);
        Optional<BlockPos> previous = lastPos(companion);
        if (previous.isPresent() && previous.get().closerThan(pos, 4.0D)
                && lastDimension(companion).filter(dimension::equals).isPresent()) return false;
        record.putString(LAST_DIMENSION, dimension);
        record.putLong(LAST_POS, pos.asLong());
        return true;
    }

    void updateSnapshot(UUID companion, String dimension, BlockPos pos, long gameTime,
                        CompoundTag snapshot, int epoch) {
        CompoundTag record = mutable(companion);
        record.put(SNAPSHOT, snapshot.copy());
        record.putString(SNAPSHOT_DIMENSION, dimension);
        record.putLong(SNAPSHOT_POS, pos.asLong());
        record.putLong(SNAPSHOT_GAME_TIME, gameTime);
        record.putInt(EPOCH, Math.max(epoch(companion), epoch));
        record.putLong(MISSING_SINCE, 0L);
        updateLastKnown(companion, dimension, pos);
    }

    boolean setMissingSince(UUID companion, long value) {
        if (!records.containsKey(companion) && value == 0L) return false;
        CompoundTag record = mutable(companion);
        if (record.getLong(MISSING_SINCE) == value) return false;
        record.putLong(MISSING_SINCE, value);
        return true;
    }

    boolean commitEpoch(UUID companion, int value) {
        if (value <= epoch(companion)) return false;
        mutable(companion).putInt(EPOCH, value);
        return true;
    }

    static CompanionRecoveryLedger load(ListTag list) {
        CompanionRecoveryLedger ledger = new CompanionRecoveryLedger();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag record = list.getCompound(i);
            if (!record.hasUUID(COMPANION)) continue;
            UUID companion = record.getUUID(COMPANION);
            if (ledger.records.containsKey(companion)) continue;
            CompoundTag copy = record.copy();
            copy.remove(COMPANION);
            ledger.records.put(companion, copy);
        }
        return ledger;
    }

    ListTag save() {
        ListTag list = new ListTag();
        records.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag record = entry.getValue().copy();
            record.putUUID(COMPANION, entry.getKey());
            list.add(record);
        });
        return list;
    }

    private CompoundTag read(UUID companion) { return records.getOrDefault(companion, new CompoundTag()); }
    private CompoundTag mutable(UUID companion) { return records.computeIfAbsent(companion, ignored -> new CompoundTag()); }

    private Optional<BlockPos> position(UUID companion, String key) {
        CompoundTag record = records.get(companion);
        return record == null || !record.contains(key, Tag.TAG_LONG) ? Optional.empty()
                : Optional.of(BlockPos.of(record.getLong(key)));
    }

    private Optional<String> dimension(UUID companion, String key) {
        CompoundTag record = records.get(companion);
        if (record == null || !record.contains(key, Tag.TAG_STRING)) return Optional.empty();
        String value = record.getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
