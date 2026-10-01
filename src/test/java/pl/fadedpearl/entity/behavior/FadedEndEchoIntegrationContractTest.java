package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedEndEchoIntegrationContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void adapterUsesServerFightHistoryInsteadOfDragonEntityPresence() throws IOException {
        String source = Files.readString(ENTITY);
        String dragonState = source.substring(source.indexOf("private FadedEndEchoPolicy.DragonState authoritativeDragonState"),
                source.indexOf("private BlockPos findVisibleEndThreshold"));

        assertTrue(dragonState.contains("serverLevel.getDragonFight()"));
        assertTrue(dragonState.contains("hasPreviouslyKilledDragon()"));
        assertFalse(dragonState.contains("EnderDragon"));
        assertFalse(dragonState.contains("getDragonUUID"));
        assertFalse(dragonState.contains("getEntitiesOfClass"));
    }

    @Test
    void portalScanIsBoundedVisibleAndNeverSearchesChunks() throws IOException {
        String source = Files.readString(ENTITY);
        String scan = source.substring(source.indexOf("private BlockPos findVisibleEndThreshold"),
                source.indexOf("private boolean isEndEchoReactionSafe"));

        assertTrue(scan.contains("origin.offset(-8, -4, -8)"));
        assertTrue(scan.contains("origin.offset(8, 4, 8)"));
        assertTrue(scan.contains("level().hasChunkAt(candidate)"));
        assertTrue(scan.contains("Blocks.END_PORTAL_FRAME"));
        assertTrue(scan.contains("Blocks.END_PORTAL"));
        assertTrue(scan.contains("ClipContext.Block.COLLIDER"));
        assertFalse(scan.contains("getChunk"));
    }

    @Test
    void portalScanRunsOnlyForAnUndiscoveredOverworldThreshold() throws IOException {
        String source = Files.readString(ENTITY);
        String tick = source.substring(source.indexOf("private boolean tickEndEchoes"),
                source.indexOf("private FadedEndEchoPolicy.Observation endEchoObservation"));

        int scanPolicy = tick.indexOf("FadedEndEchoPolicy.shouldScanEndPortal(");
        int observationCall = tick.indexOf("endEchoObservation(");
        assertTrue(scanPolicy >= 0);
        assertTrue(observationCall > scanPolicy);
        assertTrue(tick.substring(scanPolicy, observationCall).contains(
                "serverLevel.dimension() == Level.OVERWORLD"));
    }

    @Test
    void pendingReactionIsRevalidatedBeforeItsMilestoneIsWritten() throws IOException {
        String source = Files.readString(ENTITY);
        String tick = source.substring(source.indexOf("private boolean tickEndEchoes"),
                source.indexOf("private FadedEndEchoPolicy.Observation endEchoObservation"));

        int cheapContext = tick.indexOf("FadedEndEchoPolicy.contextPresent(");
        int completionContext = tick.indexOf("FadedEndEchoPolicy.stillRelevant(");
        int discovery = tick.indexOf("worldAwarenessMemory.markFirst(FadedEndEchoPolicy.milestone(reaction))");
        assertTrue(cheapContext >= 0);
        assertTrue(completionContext > cheapContext);
        assertTrue(discovery > completionContext);
        assertTrue(tick.contains("clearPendingEndEcho(200)"));
    }

    @Test
    void reactionsUseExistingPresentationAndNeverChangeTrust() throws IOException {
        String source = Files.readString(ENTITY);
        String adapter = source.substring(source.indexOf("private boolean tickEndEchoes"),
                source.indexOf("private boolean tickNightWatchCycle"));

        assertTrue(adapter.contains("SocialAction.WORRIED"));
        assertTrue(adapter.contains("SocialAction.LOOK_AROUND"));
        assertTrue(adapter.contains("SocialAction.AFFECTION"));
        assertTrue(adapter.contains("pendingBarrierRecoveryOutcome == FadedMovementCoordinator.RecoveryOutcome.NONE"));
        assertTrue(adapter.contains("isFriendRescueActive()"));
        assertTrue(adapter.contains("getCarryAction() == CarryAction.NONE"));
        assertFalse(adapter.contains("modifyTrust("));
        assertFalse(adapter.contains("addTrust("));
    }
}
