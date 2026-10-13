package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class EchoNeedleLocatorTest {
    private static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void guidesOnlyUndergroundPlayersWithoutAnExistingCompanion() {
        assertTrue(EchoNeedleLocator.canGuide(false, false, false));
        assertFalse(EchoNeedleLocator.canGuide(true, false, false));
        assertFalse(EchoNeedleLocator.canGuide(false, true, false));
        assertFalse(EchoNeedleLocator.canGuide(false, false, true));
    }

    @Test
    void choosesNearestEligiblePersistentEncounterWithoutNeedingAnEntity() {
        var candidates = List.of(
                candidate(FIRST, "minecraft:overworld", new BlockPos(40, 0, 0), true, false, true),
                candidate(SECOND, "minecraft:overworld", new BlockPos(20, 0, 0), true, false, true));

        var result = EchoNeedleLocator.locate(candidates, "minecraft:overworld", .5D, .5D, .5D, 0.0F);

        assertEquals(new EchoNeedleLocator.Reading(12, EchoNeedleLocator.Height.SIMILAR), result.orElseThrow());
    }

    @Test
    void rejectsDeadHealedClaimedOtherDimensionAndOutOfRangeEntries() {
        var candidates = List.of(
                candidate(FIRST, "minecraft:overworld", new BlockPos(10, 0, 0), false, false, true),
                candidate(SECOND, "minecraft:overworld", new BlockPos(11, 0, 0), true, true, true),
                candidate(UUID.randomUUID(), "minecraft:overworld", new BlockPos(12, 0, 0), true, false, false),
                candidate(UUID.randomUUID(), "minecraft:the_nether", new BlockPos(13, 0, 0), true, false, true),
                candidate(UUID.randomUUID(), "minecraft:overworld", new BlockPos(97, 0, 0), true, false, true));

        assertTrue(EchoNeedleLocator.locate(candidates, "minecraft:overworld",
                .5D, .5D, .5D, 0.0F).isEmpty());
    }

    @Test
    void rangeIsSphericalAndInclusiveAtNinetySixBlocks() {
        var boundary = candidate(FIRST, "minecraft:overworld", new BlockPos(96, 0, 0), true, false, true);
        var outside = candidate(SECOND, "minecraft:overworld", new BlockPos(97, 0, 0), true, false, true);
        assertTrue(EchoNeedleLocator.locate(List.of(boundary), "minecraft:overworld",
                .5D, .5D, .5D, 0.0F).isPresent());
        assertTrue(EchoNeedleLocator.locate(List.of(outside), "minecraft:overworld",
                .5D, .5D, .5D, 0.0F).isEmpty());
        assertTrue(EchoNeedleLocator.locate(List.of(candidate(SECOND, "minecraft:overworld",
                        new BlockPos(80, 60, 0), true, false, true)), "minecraft:overworld",
                .5D, .5D, .5D, 0.0F).isEmpty());
    }

    @Test
    void directionUsesSixteenWrappedRelativeSectors() {
        for (int sector = 0; sector < 16; sector++)
            assertEquals(sector, EchoNeedleLocator.sectorFromRelativeYaw(sector * 22.5D));
        assertEquals(0, EchoNeedleLocator.sectorFromRelativeYaw(11.249D));
        assertEquals(1, EchoNeedleLocator.sectorFromRelativeYaw(11.25D));
        assertEquals(15, EchoNeedleLocator.sectorFromRelativeYaw(-11.251D));
        assertEquals(0, EchoNeedleLocator.sectorFromRelativeYaw(360.0D));
    }

    @Test
    void heightUsesFourBlockSimilarBandWithStrictOuterCategories() {
        assertEquals(EchoNeedleLocator.Height.BELOW, EchoNeedleLocator.height(20.0D, 15.999D));
        assertEquals(EchoNeedleLocator.Height.SIMILAR, EchoNeedleLocator.height(20.0D, 16.0D));
        assertEquals(EchoNeedleLocator.Height.SIMILAR, EchoNeedleLocator.height(20.0D, 24.0D));
        assertEquals(EchoNeedleLocator.Height.ABOVE, EchoNeedleLocator.height(20.0D, 24.001D));
    }

    @Test
    void savedDataProjectsPersistentEncounterAndOwnershipRecoveryStatus() {
        CaveEncounterLedger encounters = new CaveEncounterLedger();
        assertTrue(encounters.register(FIRST, "minecraft:overworld", new BlockPos(8, -20, 8)));
        CompanionOwnershipLedger ownership = new CompanionOwnershipLedger();
        assertTrue(ownership.registerUnclaimed(FIRST));
        CompoundTag saved = new CompoundTag();
        saved.put("CaveEncounters", encounters.save());
        saved.put("CompanionOwnership", ownership.save());
        FadedPearlSavedData restored = FadedPearlSavedData.load(saved);
        var candidate = restored.echoNeedleCandidates("minecraft:overworld").get(0);
        assertTrue(candidate.alive());
        assertFalse(candidate.healed());
        assertTrue(candidate.unclaimed());

        assertTrue(restored.claimForHealing(UUID.randomUUID(), FIRST));
        restored.markHealed(FIRST);
        candidate = restored.echoNeedleCandidates("minecraft:overworld").get(0);
        assertTrue(candidate.alive());
        assertTrue(candidate.healed());
        assertFalse(candidate.unclaimed());
        restored.markDead(FIRST);
        assertFalse(restored.echoNeedleCandidates("minecraft:overworld").get(0).alive());
    }

    private static EchoNeedleLocator.Candidate candidate(UUID id, String dimension, BlockPos pos,
                                                          boolean alive, boolean healed, boolean unclaimed) {
        return new EchoNeedleLocator.Candidate(id, dimension, pos, alive, healed, unclaimed);
    }
}
