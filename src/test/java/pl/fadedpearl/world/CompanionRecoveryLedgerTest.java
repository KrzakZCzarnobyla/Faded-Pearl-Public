package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionRecoveryLedgerTest {
    private static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void twoSnapshotsAndEpochsRemainSeparateAcrossSaveQuit() {
        CompanionRecoveryLedger data = new CompanionRecoveryLedger();
        CompoundTag first = new CompoundTag();
        first.putString("Name", "first");
        CompoundTag second = new CompoundTag();
        second.putString("Name", "second");
        BlockPos firstPos = new BlockPos(10, 30, 20);
        BlockPos secondPos = new BlockPos(200, 40, 300);

        data.updateSnapshot(FIRST, "minecraft:overworld", firstPos, 100L, first, 3);
        data.updateSnapshot(SECOND, "minecraft:the_nether", secondPos, 200L, second, 9);
        data.setMissingSince(FIRST, 111L);
        data.setMissingSince(SECOND, 222L);
        data.commitEpoch(FIRST, 4);
        data.commitEpoch(SECOND, 10);
        CompanionRecoveryLedger loaded = CompanionRecoveryLedger.load(data.save());

        assertAll(
                () -> assertEquals("first", loaded.snapshot(FIRST).orElseThrow().getString("Name")),
                () -> assertEquals("second", loaded.snapshot(SECOND).orElseThrow().getString("Name")),
                () -> assertEquals(firstPos, loaded.snapshotPos(FIRST).orElseThrow()),
                () -> assertEquals(secondPos, loaded.snapshotPos(SECOND).orElseThrow()),
                () -> assertEquals("minecraft:overworld", loaded.snapshotDimension(FIRST).orElseThrow()),
                () -> assertEquals("minecraft:the_nether", loaded.snapshotDimension(SECOND).orElseThrow()),
                () -> assertEquals(111L, loaded.missingSince(FIRST)),
                () -> assertEquals(222L, loaded.missingSince(SECOND)),
                () -> assertEquals(4, loaded.epoch(FIRST)),
                () -> assertEquals(10, loaded.epoch(SECOND)));
    }

    @Test
    void legacyRecoveryFieldsMigrateWithoutLosingTheOriginal() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        old.putBoolean("Healed", true);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Name", "legacy");
        old.put("CompanionSnapshot", snapshot);
        old.putInt("RecoveryEpoch", 7);
        FadedPearlSavedData data = FadedPearlSavedData.load(old);

        assertEquals("legacy", data.companionSnapshot(FIRST).orElseThrow().getString("Name"));
        assertEquals(7, data.recoveryEpoch(FIRST));
        assertEquals("legacy", data.companionSnapshot().orElseThrow().getString("Name"));
        assertEquals(7, FadedPearlSavedData.load(data.save(new CompoundTag())).recoveryEpoch(FIRST));
    }

    @Test
    void legacyGlobalIdentitySnapshotAndEpochRemainSingleAfterRepeatedMigration() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        old.putBoolean("Healed", true);
        old.putBoolean("Dead", false);
        old.putLong("SnapshotGameTime", 1234L);
        old.putLong("RecoveryMissingSince", 1200L);
        old.putInt("RecoveryEpoch", 9);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Friend", owner);
        snapshot.putInt("Trust", 42);
        old.put("CompanionSnapshot", snapshot);

        FadedPearlSavedData once = FadedPearlSavedData.load(old);
        CompoundTag firstSave = once.save(new CompoundTag());
        FadedPearlSavedData twice = FadedPearlSavedData.load(firstSave);

        assertAll(
                () -> assertEquals(FIRST, twice.endermanId().orElseThrow()),
                () -> assertEquals(java.util.List.of(FIRST), twice.recoveryCompanionIds()),
                () -> assertEquals(FIRST, twice.companionOf(owner).orElseThrow()),
                () -> assertEquals(owner, twice.ownerOf(FIRST).orElseThrow()),
                () -> assertFalse(twice.isUnclaimedCompanion(FIRST)),
                () -> assertEquals(9, twice.recoveryEpoch()),
                () -> assertEquals(9, twice.recoveryEpoch(FIRST)),
                () -> assertEquals(1234L, twice.snapshotGameTime(FIRST)),
                () -> assertEquals(1200L, twice.recoveryMissingSince(FIRST)),
                () -> assertEquals(42, twice.companionSnapshot(FIRST).orElseThrow().getInt("Trust")),
                () -> assertEquals(firstSave, twice.save(new CompoundTag())));
    }

    @Test
    void snapshotsAreDefensivelyCopiedAndUnknownReadsDoNotCreateRecords() {
        CompanionRecoveryLedger ledger = new CompanionRecoveryLedger();
        assertEquals(0, ledger.epoch(FIRST));
        assertTrue(ledger.snapshot(FIRST).isEmpty());
        assertEquals(0, ledger.save().size());

        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Name", "original");
        ledger.updateSnapshot(FIRST, "minecraft:overworld", BlockPos.ZERO, 1L, snapshot, 5);
        snapshot.putString("Name", "changed");
        CompoundTag returned = ledger.snapshot(FIRST).orElseThrow();
        returned.putString("Name", "also changed");
        assertEquals("original", ledger.snapshot(FIRST).orElseThrow().getString("Name"));
        assertFalse(ledger.commitEpoch(FIRST, 4));
        assertEquals(5, ledger.epoch(FIRST));
    }

    @Test
    void deathHealingAndHomesAreIndependent() {
        CompanionRecoveryLedger ledger = new CompanionRecoveryLedger();
        BlockPos firstHome = new BlockPos(4, 70, 8);
        BlockPos secondHome = new BlockPos(150, 40, 200);
        ledger.markHealed(FIRST);
        ledger.markHealed(SECOND);
        ledger.setHome(FIRST, "minecraft:overworld", firstHome);
        ledger.setHome(SECOND, "minecraft:the_nether", secondHome);
        ledger.markDead(FIRST);
        CompanionRecoveryLedger loaded = CompanionRecoveryLedger.load(ledger.save());

        assertTrue(loaded.isDead(FIRST));
        assertFalse(loaded.isDead(SECOND));
        assertTrue(loaded.isHealed(FIRST));
        assertTrue(loaded.isHealed(SECOND));
        assertEquals(firstHome, loaded.homePos(FIRST).orElseThrow());
        assertEquals(secondHome, loaded.homePos(SECOND).orElseThrow());
        assertEquals("minecraft:overworld", loaded.homeDimension(FIRST).orElseThrow());
        assertEquals("minecraft:the_nether", loaded.homeDimension(SECOND).orElseThrow());
    }

    @Test
    void legacyLifeAndHomeMigrateToMatchingCompanionOnly() {
        CompoundTag old = new CompoundTag();
        old.putBoolean("Healed", true);
        old.putBoolean("Dead", false);
        old.putLong("HomePos", new BlockPos(11, 64, 22).asLong());
        old.putString("HomeDimension", "minecraft:overworld");
        CompanionRecoveryLedger ledger = new CompanionRecoveryLedger();
        ledger.updateSnapshot(FIRST, "minecraft:overworld", BlockPos.ZERO, 5L, new CompoundTag(), 2);
        assertTrue(ledger.importLegacy(FIRST, old));
        ListTag firstSave = ledger.save();
        CompanionRecoveryLedger loaded = CompanionRecoveryLedger.load(firstSave);
        assertFalse(loaded.importLegacy(FIRST, old));

        assertTrue(loaded.isHealed(FIRST));
        assertFalse(loaded.isHealed(SECOND));
        assertEquals(new BlockPos(11, 64, 22), loaded.homePos(FIRST).orElseThrow());
        assertTrue(loaded.homePos(SECOND).isEmpty());
        assertEquals(firstSave, loaded.save());
    }

    @Test
    void missingLegacyFieldsUseSafeDefaultsAcrossRepeatedSaveLoad() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);

        FadedPearlSavedData once = FadedPearlSavedData.load(old);
        FadedPearlSavedData twice = FadedPearlSavedData.load(once.save(new CompoundTag()));

        assertAll(
                () -> assertFalse(twice.isDead(FIRST)),
                () -> assertFalse(twice.isHealed(FIRST)),
                () -> assertEquals(0, twice.recoveryEpoch(FIRST)),
                () -> assertEquals(0L, twice.snapshotGameTime(FIRST)),
                () -> assertEquals(0L, twice.recoveryMissingSince(FIRST)),
                () -> assertTrue(twice.companionSnapshot(FIRST).isEmpty()),
                () -> assertTrue(twice.homePos(FIRST).isEmpty()),
                () -> assertTrue(twice.homeDimension(FIRST).isEmpty()));
    }

    @Test
    void malformedDimensionsFallBackToEmptyInsteadOfAbortingWorldLoad() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        old.putLong("HomePos", BlockPos.ZERO.asLong());
        old.putString("HomeDimension", "not a dimension");
        old.putLong("LastPos", BlockPos.ZERO.asLong());
        old.putString("LastDimension", "also invalid");
        old.putLong("SnapshotPos", BlockPos.ZERO.asLong());
        old.putString("SnapshotDimension", "still invalid");

        FadedPearlSavedData loaded = assertDoesNotThrow(() -> FadedPearlSavedData.load(old));

        assertAll(
                () -> assertTrue(loaded.homePos().isEmpty()),
                () -> assertTrue(loaded.homeDimension().isEmpty()),
                () -> assertTrue(loaded.lastPos().isEmpty()),
                () -> assertTrue(loaded.lastDimension().isEmpty()),
                () -> assertTrue(loaded.snapshotPos().isEmpty()),
                () -> assertTrue(loaded.snapshotDimension().isEmpty()));
    }

    @Test
    void validLegacyFieldsRepairWrongTypedFieldsInAnEarlyRecoveryRecord() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        old.putBoolean("Healed", true);
        old.putBoolean("Dead", false);
        old.putInt("RecoveryEpoch", 12);
        old.putLong("HomePos", new BlockPos(8, 70, 9).asLong());
        old.putString("HomeDimension", "minecraft:overworld");
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Name", "legacy-valid");
        old.put("CompanionSnapshot", snapshot);

        CompoundTag broken = new CompoundTag();
        broken.putUUID("Companion", FIRST);
        broken.putString("Healed", "broken");
        broken.putString("Dead", "broken");
        broken.putString("RecoveryEpoch", "broken");
        broken.putInt("HomePos", 1);
        broken.putInt("HomeDimension", 1);
        broken.putString("CompanionSnapshot", "broken");
        ListTag recovery = new ListTag();
        recovery.add(broken);
        old.put("CompanionRecovery", recovery);

        CompanionRecoveryLedger loaded = CompanionRecoveryLedger.load(recovery);
        assertTrue(loaded.importLegacy(FIRST, old));

        assertAll(
                () -> assertTrue(loaded.isHealed(FIRST)),
                () -> assertFalse(loaded.isDead(FIRST)),
                () -> assertEquals(12, loaded.epoch(FIRST)),
                () -> assertEquals(new BlockPos(8, 70, 9), loaded.homePos(FIRST).orElseThrow()),
                () -> assertEquals("minecraft:overworld", loaded.homeDimension(FIRST).orElseThrow()),
                () -> assertEquals("legacy-valid", loaded.snapshot(FIRST).orElseThrow().getString("Name")));
    }

    @Test
    void duplicateAndForeignRecoveryRowsCannotReplaceCanonicalData() {
        ListTag raw = new ListTag();
        raw.add(recoveryRecord(FIRST, "canonical", 4));
        raw.add(recoveryRecord(FIRST, "foreign-duplicate", 99));
        raw.add(new CompoundTag());
        raw.add(recoveryRecord(SECOND, "second", 7));

        CompanionRecoveryLedger loaded = CompanionRecoveryLedger.load(raw);
        ListTag firstSave = loaded.save();
        ListTag secondSave = CompanionRecoveryLedger.load(firstSave).save();

        assertAll(
                () -> assertEquals("canonical", loaded.snapshot(FIRST).orElseThrow().getString("Name")),
                () -> assertEquals(4, loaded.epoch(FIRST)),
                () -> assertEquals("second", loaded.snapshot(SECOND).orElseThrow().getString("Name")),
                () -> assertEquals(7, loaded.epoch(SECOND)),
                () -> assertEquals(2, firstSave.size()),
                () -> assertEquals(firstSave, secondSave));
    }

    private static CompoundTag recoveryRecord(UUID companion, String name, int epoch) {
        CompoundTag record = new CompoundTag();
        record.putUUID("Companion", companion);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("Name", name);
        record.put("CompanionSnapshot", snapshot);
        record.putInt("RecoveryEpoch", epoch);
        return record;
    }
}
