package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NightWatchIntegrationContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void serverTickAdvancesCycleBeforeOrdinarySocialBehavior() throws IOException {
        String source = Files.readString(ENTITY);
        int cycleCall = source.indexOf("tickNightWatchCycle()");
        int socialCall = source.indexOf("tickSocialBehavior();", cycleCall);

        assertTrue(cycleCall >= 0);
        assertTrue(socialCall > cycleCall);
        assertTrue(source.substring(cycleCall, socialCall).contains("!level().isClientSide"));
    }

    @Test
    void completionDiscoversJournalAndUsesRelationshipMemoryWithoutTrustReward() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("private boolean tickNightWatchCycle()");
        int end = source.indexOf("private boolean isNightWatchHardBlocked", start);
        String method = source.substring(start, end);

        assertTrue(method.contains("FadedNightWatchPolicy.advance("));
        assertTrue(method.contains("JournalMemory.Discovery.BEHAVIOR_NIGHT_WATCH"));
        assertTrue(method.contains("FadedRelationshipDialoguePolicy.Context.NIGHT_WATCH"));
        assertTrue(method.contains("FadedDialogue.relationshipMemory(selection)"));
        assertFalse(method.contains("addTrust("));
        assertFalse(method.contains("modifyTrust("));
    }

    @Test
    void hardBlockerCoversSafetyCommandsAndInterruptions() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("private boolean isNightWatchHardBlocked");
        int end = source.indexOf("private void tickSocialBehavior()", start);
        String method = source.substring(start, end);

        assertTrue(method.contains("distanceToSqr(friend) > 400.0D"));
        assertTrue(method.contains("CompanionCommand.REST"));
        assertTrue(method.contains("CompanionCommand.HOME"));
        assertTrue(method.contains("isFriendRescueActive()"));
        assertTrue(method.contains("isInWaterOrBubble()"));
        assertTrue(method.contains("isOnFire()"));
        assertTrue(method.contains("level().isRainingAt(blockPosition())"));
        assertTrue(method.contains("animalCarryTarget != null"));
    }

    @Test
    void sleepingIntentUsesDedicatedAnimationAction() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("if (decision.intent() == FadedSocialController.Intent.WATCH_SLEEPING)");
        int end = source.indexOf("if (decision.intent() == FadedSocialController.Intent.NIGHT_CLOSE)", start);
        String handler = source.substring(start, end);

        assertTrue(handler.contains("setSocialAction(SocialAction.WATCH_SLEEPING)"));
        assertFalse(handler.contains("setSocialAction(SocialAction.GUARD)"));
    }
}
