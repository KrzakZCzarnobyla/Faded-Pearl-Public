package pl.fadedpearl.entity.curiosity;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ItemCuriosityContractTest {
    @Test void approvedTimingAndTrustContractIsFrozen() {
        assertAll(
                () -> assertEquals(20, ItemCuriosityPolicy.MIN_TRUST),
                () -> assertEquals(40, ItemCuriosityPolicy.POINT_TICKS),
                () -> assertEquals(200, ItemCuriosityPolicy.HAND_OFFER_TICKS),
                () -> assertEquals(80, ItemCuriosityPolicy.INSPECT_TICKS),
                () -> assertEquals(3600, ItemCuriosityPolicy.COOLDOWN_TICKS),
                () -> assertEquals(160, ItemCuriosityPolicy.GROUND_APPROACH_TIMEOUT_TICKS),
                () -> assertEquals(80, ItemCuriosityPolicy.GROUND_INSPECT_TICKS),
                () -> assertEquals(8.0D, ItemCuriosityPolicy.GROUND_NOTICE_RANGE),
                () -> assertEquals(2.25D, ItemCuriosityPolicy.GROUND_STOP_DISTANCE),
                () -> assertEquals(10, ItemCuriosityPolicy.HINT_DELAY_TICKS));
    }

    @Test void entityUsesCoordinatorAndOneItemTransfer() throws Exception {
        String source = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        assertTrue(source.contains("copyWithCount(1)"));
        assertTrue(source.contains("held.shrink(1)"));
        assertTrue(source.contains("queueStop(FadedMovementCoordinator.Locomotion.EXTERNAL_INTERACTION_STOP"));
        assertTrue(source.contains("queueLocomotion(FadedMovementCoordinator.Locomotion.CURIOSITY_APPROACH"));
        assertTrue(source.contains("queueLookAt(friend"));
        assertTrue(source.contains("createDryPathTo"));
        assertTrue(source.contains("hasLineOfSight(item)"));
        assertTrue(source.contains("isSafeHomeLanding(serverLevel, feet)"));
        assertTrue(source.contains(".limit(6).toList()"));
        assertTrue(source.contains("friend.isInWaterOrBubble() || friend.isOnFire()"));
        assertFalse(source.contains("getNavigation().stop(); // curiosity"));
    }

    @Test void groundCuriosityNeverTakesOwnershipOrTeleports() throws Exception {
        String source = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String ground = source.substring(source.indexOf("private void tickGroundCuriosity"),
                source.indexOf("private DryRoute findGroundCuriosityRoute"));
        assertFalse(ground.contains("curiosityStack ="));
        assertFalse(ground.contains("shrink("));
        assertFalse(ground.contains("teleport"));
        assertFalse(ground.contains("addTrust("));
        assertFalse(ground.contains("modifyTrust("));
    }

    @Test void hintIsLocalizedDeferredAndRateLimitedWithoutNbt() throws Exception {
        String source = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        assertTrue(source.contains("hint.faded_pearl.item_curiosity.offer"));
        assertTrue(source.contains("lastDialogueTick == level().getGameTime()"));
        assertTrue(source.contains("lastThoughtTick == level().getGameTime()"));
        assertTrue(source.contains("deferredDialogues.add"));
        assertTrue(source.contains("flushDeferredDialogues();"));
        assertTrue(source.contains("ItemCuriosityPolicy.HAND_OFFER_TICKS"));
        assertFalse(source.contains("curiosityHintCooldown"));
        String codec = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/persistence/FadedPersistenceCodec.java"));
        assertFalse(codec.contains("curiosity_hint"));
    }

    @Test void recoveryWhitelistIncludesOwnedStackAndMemory() throws Exception {
        String codec = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/persistence/FadedPersistenceCodec.java"));
        assertTrue(codec.contains("CURIOSITY_STACK, CURIOSITY_SEEN, CURIOSITY_COOLDOWN"));
    }
}
