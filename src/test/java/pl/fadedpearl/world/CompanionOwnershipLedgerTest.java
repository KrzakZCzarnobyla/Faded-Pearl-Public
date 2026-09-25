package pl.fadedpearl.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionOwnershipLedgerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void firstSuccessfulHealerOwnsUnclaimedCompanion() {
        CompanionOwnershipLedger ledger = new CompanionOwnershipLedger();
        assertTrue(ledger.registerUnclaimed(FIRST));
        assertEquals(CompanionOwnershipLedger.ClaimResult.CLAIMED, ledger.claim(A, FIRST));
        assertEquals(CompanionOwnershipLedger.ClaimResult.COMPANION_OWNED, ledger.claim(B, FIRST));
        assertEquals(A, ledger.ownerOf(FIRST).orElseThrow());
        assertFalse(ledger.isUnclaimed(FIRST));
    }

    @Test
    void oneCompanionPerOwnerAndIndependentRelationsSurviveSave() {
        CompanionOwnershipLedger ledger = new CompanionOwnershipLedger();
        ledger.registerUnclaimed(FIRST);
        ledger.registerUnclaimed(SECOND);
        assertEquals(CompanionOwnershipLedger.ClaimResult.CLAIMED, ledger.claim(A, FIRST));
        assertEquals(CompanionOwnershipLedger.ClaimResult.OWNER_HAS_COMPANION, ledger.claim(A, SECOND));
        assertEquals(CompanionOwnershipLedger.ClaimResult.CLAIMED, ledger.claim(B, SECOND));
        CompanionOwnershipLedger loaded = CompanionOwnershipLedger.load(ledger.save());
        assertEquals(FIRST, loaded.companionOf(A).orElseThrow());
        assertEquals(SECOND, loaded.companionOf(B).orElseThrow());
        assertEquals(A, loaded.ownerOf(FIRST).orElseThrow());
        assertEquals(B, loaded.ownerOf(SECOND).orElseThrow());
    }

    @Test
    void legacyHealedSnapshotKeepsOriginalOwnerAndLegacyWoundedStaysUnclaimed() {
        CompoundTag healed = new CompoundTag();
        healed.putUUID("Enderman", FIRST);
        healed.putBoolean("Healed", true);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Friend", A);
        healed.put("CompanionSnapshot", snapshot);
        FadedPearlSavedData migrated = FadedPearlSavedData.load(healed);
        assertEquals(FIRST, migrated.companionOf(A).orElseThrow());
        assertEquals(A, FadedPearlSavedData.load(migrated.save(new CompoundTag()))
                .ownerOf(FIRST).orElseThrow());

        CompoundTag wounded = new CompoundTag();
        wounded.putUUID("Enderman", SECOND);
        assertTrue(FadedPearlSavedData.load(wounded).isUnclaimedCompanion(SECOND));
    }

    @Test
    void legacyHealedWithoutVerifiedOwnerCannotBeClaimedAsUnowned() {
        CompoundTag healed = new CompoundTag();
        healed.putUUID("Enderman", FIRST);
        healed.putBoolean("Healed", true);
        FadedPearlSavedData migrated = FadedPearlSavedData.load(healed);
        assertFalse(migrated.isUnclaimedCompanion(FIRST));
        assertFalse(migrated.claimCompanion(B, FIRST));
        assertEquals(FIRST, migrated.endermanId().orElseThrow());
    }

    @Test
    void healingRecordsFirstOwnerAndRejectsSecondWithoutChangingLegacyIdentity() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        data.markSpawned(FIRST);
        assertTrue(data.claimForHealing(A, FIRST));
        assertFalse(data.claimForHealing(B, FIRST));
        assertFalse(data.claimForHealing(A, SECOND));
        assertEquals(A, data.ownerOf(FIRST).orElseThrow());
        assertEquals(FIRST, data.endermanId().orElseThrow());
        assertEquals(FIRST, FadedPearlSavedData.load(data.save(new CompoundTag()))
                .companionOf(A).orElseThrow());
    }

    @Test
    void healingExistingUnclaimedLegacyEntityAndNewlySummonedEntityBothWork() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        FadedPearlSavedData legacy = FadedPearlSavedData.load(old);
        assertTrue(legacy.claimForHealing(A, FIRST));
        assertEquals(A, legacy.ownerOf(FIRST).orElseThrow());

        FadedPearlSavedData summoned = new FadedPearlSavedData();
        assertTrue(summoned.claimForHealing(B, SECOND));
        assertEquals(SECOND, summoned.companionOf(B).orElseThrow());
    }

    @Test
    void unresolvedHealedLegacyRecordCannotBeReassignedDuringHealing() {
        CompoundTag old = new CompoundTag();
        old.putUUID("Enderman", FIRST);
        old.putBoolean("Healed", true);
        FadedPearlSavedData data = FadedPearlSavedData.load(old);
        assertFalse(data.claimForHealing(B, FIRST));
        assertTrue(data.ownerOf(FIRST).isEmpty());
    }

    @Test
    void recoveryTracksBothOwnedCompanionsAndKeepsLegacyIdentityOnce() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        data.markSpawned(FIRST);
        assertTrue(data.claimForHealing(A, FIRST));
        data.markHealed(FIRST);
        assertTrue(data.registerUnclaimedCompanion(SECOND));
        assertTrue(data.claimCompanion(B, SECOND));
        data.markHealed(SECOND);

        assertEquals(java.util.List.of(FIRST, SECOND), data.recoveryCompanionIds());
        FadedPearlSavedData loaded = FadedPearlSavedData.load(data.save(new CompoundTag()));
        assertEquals(java.util.List.of(FIRST, SECOND), loaded.recoveryCompanionIds());
        assertEquals(FIRST, loaded.companionOf(A).orElseThrow());
        assertEquals(SECOND, loaded.companionOf(B).orElseThrow());
    }

    @Test
    void earlySaveWithEmptyOwnershipIndexRecoversVerifiedLegacyFriend() {
        CompoundTag early = new CompoundTag();
        early.putUUID("Enderman", FIRST);
        early.putBoolean("Healed", true);
        early.put("CompanionOwnership", new CompoundTag());
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Friend", A);
        early.put("CompanionSnapshot", snapshot);

        FadedPearlSavedData loaded = FadedPearlSavedData.load(early);
        assertEquals(A, loaded.ownerOf(FIRST).orElseThrow());
        assertEquals(FIRST, loaded.companionOf(A).orElseThrow());
    }

    @Test
    void liveLegacyReconciliationCannotStealAnotherCompanion() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        data.markSpawned(FIRST);
        data.markHealed(FIRST);
        assertTrue(data.reconcileLegacyOwner(FIRST, A));
        assertFalse(data.reconcileLegacyOwner(FIRST, B));
        assertFalse(data.reconcileLegacyOwner(SECOND, B));
        assertEquals(A, data.ownerOf(FIRST).orElseThrow());
    }

    @Test
    void twoPlayersCanHealDifferentUnclaimedFadeWithoutReplacingFirst() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        data.markSpawned(FIRST);
        data.registerUnclaimedCompanion(SECOND);
        assertTrue(data.claimForHealing(A, FIRST));
        data.markHealed(FIRST);
        assertTrue(data.claimForHealing(B, SECOND));
        data.markHealed(SECOND);
        assertFalse(data.claimForHealing(A, SECOND));
        assertFalse(data.claimForHealing(B, FIRST));
        assertEquals(FIRST, data.endermanId().orElseThrow());
        assertEquals(FIRST, data.companionOf(A).orElseThrow());
        assertEquals(SECOND, data.companionOf(B).orElseThrow());
        FadedPearlSavedData loaded = FadedPearlSavedData.load(data.save(new CompoundTag()));
        assertEquals(SECOND, loaded.companionOf(B).orElseThrow());
    }

    @Test
    void unclaimedSupplyCanBeBoundedByPlayersStillWithoutCompanions() {
        CompanionOwnershipLedger ledger = new CompanionOwnershipLedger();
        assertEquals(0, ledger.unclaimedCount());
        assertTrue(ledger.registerUnclaimed(FIRST));
        assertTrue(ledger.registerUnclaimed(SECOND));
        assertEquals(2, ledger.unclaimedCount());
        assertEquals(CompanionOwnershipLedger.ClaimResult.CLAIMED, ledger.claim(A, FIRST));
        assertEquals(1, ledger.unclaimedCount());
        assertTrue(ledger.removeUnclaimed(SECOND));
        assertEquals(0, ledger.unclaimedCount());
    }

    @Test
    void malformedAndConflictingRowsCannotCreateASecondCompanionForOneOwner() {
        CompoundTag raw = new CompoundTag();
        ListTag owned = new ListTag();
        owned.add(owned(A, FIRST));
        owned.add(owned(A, SECOND));
        owned.add(owned(B, FIRST));
        owned.add(new CompoundTag());
        raw.put("Owned", owned);

        ListTag unclaimed = new ListTag();
        CompoundTag alreadyOwned = new CompoundTag();
        alreadyOwned.putUUID("Companion", FIRST);
        unclaimed.add(alreadyOwned);
        CompoundTag available = new CompoundTag();
        available.putUUID("Companion", SECOND);
        unclaimed.add(available);
        raw.put("Unclaimed", unclaimed);

        CompanionOwnershipLedger loaded = CompanionOwnershipLedger.load(raw);
        CompoundTag firstSave = loaded.save();

        assertAll(
                () -> assertEquals(FIRST, loaded.companionOf(A).orElseThrow()),
                () -> assertEquals(A, loaded.ownerOf(FIRST).orElseThrow()),
                () -> assertTrue(loaded.companionOf(B).isEmpty()),
                () -> assertTrue(loaded.ownerOf(SECOND).isEmpty()),
                () -> assertFalse(loaded.isUnclaimed(FIRST)),
                () -> assertTrue(loaded.isUnclaimed(SECOND)),
                () -> assertEquals(firstSave, CompanionOwnershipLedger.load(firstSave).save()));
    }

    @Test
    void existingMultipleRelationsSurviveLegacyMigrationWithoutBeingReassigned() {
        CompoundTag world = new CompoundTag();
        world.putUUID("Enderman", FIRST);
        world.putBoolean("Healed", true);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putUUID("Friend", A);
        world.put("CompanionSnapshot", snapshot);

        CompoundTag ownership = new CompoundTag();
        ListTag owned = new ListTag();
        owned.add(owned(A, FIRST));
        owned.add(owned(B, SECOND));
        ownership.put("Owned", owned);
        ownership.put("Unclaimed", new ListTag());
        world.put("CompanionOwnership", ownership);

        FadedPearlSavedData loaded = FadedPearlSavedData.load(world);
        FadedPearlSavedData reloaded = FadedPearlSavedData.load(loaded.save(new CompoundTag()));

        assertAll(
                () -> assertEquals(FIRST, reloaded.companionOf(A).orElseThrow()),
                () -> assertEquals(SECOND, reloaded.companionOf(B).orElseThrow()),
                () -> assertEquals(A, reloaded.ownerOf(FIRST).orElseThrow()),
                () -> assertEquals(B, reloaded.ownerOf(SECOND).orElseThrow()),
                () -> assertFalse(reloaded.claimCompanion(A, SECOND)));
    }

    private static CompoundTag owned(UUID owner, UUID companion) {
        CompoundTag entry = new CompoundTag();
        entry.putUUID("Owner", owner);
        entry.putUUID("Companion", companion);
        return entry;
    }
}
