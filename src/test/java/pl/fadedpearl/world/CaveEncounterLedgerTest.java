package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CaveEncounterLedgerTest {
    private static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void spacingIsHorizontalAndPersistsAcrossSaveQuit() {
        CaveEncounterLedger ledger = new CaveEncounterLedger();
        assertTrue(ledger.register(FIRST, "minecraft:overworld", new BlockPos(0, -20, 0)));
        assertFalse(ledger.register(SECOND, "minecraft:overworld", new BlockPos(99, 80, 0)));
        assertTrue(ledger.canPlace("minecraft:overworld", new BlockPos(100, -20, 0)));
        assertTrue(ledger.register(SECOND, "minecraft:overworld", new BlockPos(100, -20, 0)));
        CaveEncounterLedger restored = CaveEncounterLedger.load(ledger.save());
        assertEquals(2, restored.sitesIn("minecraft:overworld").size());
        assertFalse(restored.canPlace("minecraft:overworld", new BlockPos(50, -20, 0)));
        assertTrue(restored.canPlace("minecraft:the_nether", new BlockPos(0, -20, 0)));
    }

    @Test
    void removedReservationLeavesNoGhostEncounter() {
        CaveEncounterLedger ledger = new CaveEncounterLedger();
        assertTrue(ledger.register(FIRST, "minecraft:overworld", BlockPos.ZERO));
        assertFalse(ledger.register(SECOND, "minecraft:overworld", new BlockPos(50, 0, 0)));
        assertTrue(ledger.remove(FIRST));
        assertTrue(ledger.register(SECOND, "minecraft:overworld", new BlockPos(50, 0, 0)));
        CaveEncounterLedger loaded = CaveEncounterLedger.load(ledger.save());
        assertEquals(java.util.List.of(new BlockPos(50, 0, 0)),
                loaded.sitesIn("minecraft:overworld").stream().map(CaveEncounterLedger.Site::pos).toList());
    }

    @Test
    void configurableSpacingChangesPlacementWithoutChangingStoredSites() {
        CaveEncounterLedger ledger = new CaveEncounterLedger();
        assertTrue(ledger.register(FIRST, "minecraft:overworld", BlockPos.ZERO, 160));
        assertFalse(ledger.canPlace("minecraft:overworld", new BlockPos(159, 80, 0), 160));
        assertTrue(ledger.canPlace("minecraft:overworld", new BlockPos(160, -20, 0), 160));
        CaveEncounterLedger restored = CaveEncounterLedger.load(ledger.save());
        assertFalse(restored.canPlace("minecraft:overworld", new BlockPos(159, 0, 0), 160));
    }

    @Test
    void connectedPassageIsSameCaveAndBlockedOriginIsConservativelyUnknown() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos target = new BlockPos(120, 0, 0);
        assertEquals(CaveConnectivity.Result.SAME,
                CaveConnectivity.survey(start, target, ignored -> true,
                        pos -> pos.getY() == 0 && pos.getZ() == 0 && pos.getX() >= 0 && pos.getX() <= 120));
        assertEquals(CaveConnectivity.Result.DISTINCT,
                CaveConnectivity.survey(start, target, ignored -> true,
                        pos -> pos.getY() == 0 && pos.getZ() == 0 && pos.getX() != 60
                                && pos.getX() >= 0 && pos.getX() <= 120));
        assertEquals(CaveConnectivity.Result.INDETERMINATE,
                CaveConnectivity.survey(start, target, ignored -> true,
                        pos -> !pos.equals(target)));
    }

    @Test
    void unloadedCorridorAndOversizedCaveRejectNewEncounterConservatively() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos target = new BlockPos(120, 0, 0);
        assertEquals(CaveConnectivity.Result.INDETERMINATE,
                CaveConnectivity.survey(start, target, pos -> pos.getX() != 60,
                        pos -> pos.getY() == 0 && pos.getZ() == 0
                                && pos.getX() >= 0 && pos.getX() <= 120));
        assertEquals(CaveConnectivity.Result.INDETERMINATE,
                CaveConnectivity.survey(start, target, ignored -> true, ignored -> true));
    }
}
