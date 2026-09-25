package pl.fadedpearl.entity.curiosity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.world.FadedPearlSavedData;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CuriosityEscrowDeliveryTest {
    @Test void failedInventoryAndFailedSpawnKeepExactStackInPersistentEscrow() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        CompoundTag escrow = escrow("minecraft:diamond", (byte) 1);
        assertTrue(data.escrowCuriosityReturn(escrow, 0));

        assertEquals(CuriosityEscrowDelivery.Result.RETAIN_ESCROW,
                CuriosityEscrowDelivery.execute(() -> false, () -> false, data::clearPendingCuriosityReturn));
        assertEquals(escrow, data.pendingCuriosityReturn().orElseThrow());
    }

    @Test void adapterClearsOnlyAfterConfirmedInventoryOrDropMaterialisation() {
        assertEquals(CuriosityEscrowDelivery.Result.CLEAR_ESCROW,
                CuriosityEscrowDelivery.resolve(true, false));
        assertEquals(CuriosityEscrowDelivery.Result.CLEAR_ESCROW,
                CuriosityEscrowDelivery.resolve(false, true));
        java.util.concurrent.atomic.AtomicBoolean cleared = new java.util.concurrent.atomic.AtomicBoolean();
        assertEquals(CuriosityEscrowDelivery.Result.CLEAR_ESCROW,
                CuriosityEscrowDelivery.execute(() -> false, () -> true, () -> cleared.set(true)));
        assertTrue(cleared.get());
    }

    @Test void successfulInventorySkipsDropAndClearsEscrowExactlyOnce() {
        java.util.concurrent.atomic.AtomicInteger inventoryAttempts = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger dropAttempts = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger clearCalls = new java.util.concurrent.atomic.AtomicInteger();

        assertEquals(CuriosityEscrowDelivery.Result.CLEAR_ESCROW,
                CuriosityEscrowDelivery.execute(
                        () -> inventoryAttempts.incrementAndGet() == 1,
                        () -> { dropAttempts.incrementAndGet(); return true; },
                        clearCalls::incrementAndGet));
        assertEquals(1, inventoryAttempts.get());
        assertEquals(0, dropAttempts.get());
        assertEquals(1, clearCalls.get());
    }

    @Test void escrowQueuesConflictsIdempotentlyAndSurvivesSavedDataRoundTrip() {
        FadedPearlSavedData data = new FadedPearlSavedData();
        CompoundTag first = escrow("minecraft:diamond", (byte) 1);
        assertTrue(data.escrowCuriosityReturn(first, 0));
        assertTrue(data.escrowCuriosityReturn(first, 0));
        assertTrue(data.escrowCuriosityReturn(escrow("minecraft:emerald", (byte) 1), 0));
        CompoundTag disk = data.save(new CompoundTag());
        assertEquals(first, FadedPearlSavedData.load(disk).pendingCuriosityReturn().orElseThrow());
    }

    @Test void legacySingleEscrowEntryMigratesWithoutLosingTheExactStack() {
        CompoundTag legacyEscrow = escrow("minecraft:diamond_sword", (byte) 64);
        CompoundTag exactItemTag = new CompoundTag();
        exactItemTag.putString("CustomName", "legacy");
        exactItemTag.putInt("Damage", 73);
        net.minecraft.nbt.ListTag enchantments = new net.minecraft.nbt.ListTag();
        CompoundTag enchantment = new CompoundTag();
        enchantment.putString("id", "minecraft:unbreaking");
        enchantment.putShort("lvl", (short) 3);
        enchantments.add(enchantment);
        exactItemTag.put("Enchantments", enchantments);
        legacyEscrow.getCompound("Stack").put("tag", exactItemTag);
        CompoundTag oldWorld = new CompoundTag();
        oldWorld.put("PendingCuriosityReturn", legacyEscrow);

        FadedPearlSavedData migrated = FadedPearlSavedData.load(oldWorld);
        CompoundTag rewritten = migrated.save(new CompoundTag());
        FadedPearlSavedData reloaded = FadedPearlSavedData.load(rewritten);

        assertEquals(java.util.List.of(legacyEscrow), reloaded.pendingCuriosityReturns());
        assertTrue(rewritten.contains("PendingCuriosityReturns"));
        assertFalse(rewritten.contains("PendingCuriosityReturn"));
        assertEquals((byte) 64, reloaded.pendingCuriosityReturns().get(0).getCompound("Stack").getByte("Count"));
        assertEquals(exactItemTag,
                reloaded.pendingCuriosityReturns().get(0).getCompound("Stack").getCompound("tag"));
    }

    @Test void separateCompanionsKeepTheirOwnEscrowAndEpoch() {
        UUID first = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bea = UUID.fromString("00000000-0000-0000-0000-000000000002");
        FadedPearlSavedData data = new FadedPearlSavedData();
        data.markSpawned(first);
        assertTrue(data.claimForHealing(alice, first));
        data.markHealed(first);
        assertTrue(data.registerUnclaimedCompanion(second));
        assertTrue(data.claimCompanion(bea, second));
        data.markHealed(second);
        data.commitRecoveryEpoch(first, 3);
        data.commitRecoveryEpoch(second, 8);

        CompoundTag aliceItem = escrow("minecraft:diamond", (byte) 1);
        aliceItem.putUUID("Owner", first);
        CompoundTag beaItem = escrow("minecraft:emerald", (byte) 1);
        beaItem.putUUID("Owner", second);
        beaItem.putUUID("Friend", bea);
        assertFalse(data.escrowCuriosityReturn(first, aliceItem, 2));
        assertTrue(data.escrowCuriosityReturn(first, aliceItem, 3));
        assertFalse(data.escrowCuriosityReturn(first, beaItem, 8));
        assertTrue(data.escrowCuriosityReturn(second, beaItem, 8));

        FadedPearlSavedData loaded = FadedPearlSavedData.load(data.save(new CompoundTag()));
        assertEquals(java.util.List.of(aliceItem, beaItem), loaded.pendingCuriosityReturns());
        loaded.clearPendingCuriosityReturn(beaItem);
        assertEquals(java.util.List.of(aliceItem), loaded.pendingCuriosityReturns());
    }

    private static CompoundTag escrow(String id, byte count) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id);
        stack.putByte("Count", count);
        CompoundTag escrow = new CompoundTag();
        escrow.put("Stack", stack);
        escrow.putUUID("Friend", java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return escrow;
    }
}
