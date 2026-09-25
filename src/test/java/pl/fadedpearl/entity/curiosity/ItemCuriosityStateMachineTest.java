package pl.fadedpearl.entity.curiosity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.curiosity.ItemCuriosityStateMachine.*;

class ItemCuriosityStateMachineTest {
    @Test void pointingIsExactlyFortyTicksAndTimeoutStartsApprovedCooldown() {
        State state = ItemCuriosityStateMachine.pointHand();
        for (int i = 0; i < 39; i++) state = tickPointing(state, true);
        assertEquals(new State(Phase.POINTING_HAND, 1, 0, false), state);
        assertEquals(new State(Phase.NONE, 0, ItemCuriosityPolicy.COOLDOWN_TICKS, false),
                tickPointing(state, true));
        assertEquals(ItemCuriosityPolicy.COOLDOWN_TICKS,
                advanceCooldown(ItemCuriosityPolicy.COOLDOWN_TICKS, true));
        int cooldown = ItemCuriosityPolicy.COOLDOWN_TICKS;
        for (int i = 0; i < ItemCuriosityPolicy.COOLDOWN_TICKS; i++)
            cooldown = advanceCooldown(cooldown, false);
        assertEquals(0, cooldown);
    }

    @Test void mismatchCancelsWithCooldownAndHandoffOnlyWorksInsideWindow() {
        assertEquals(ItemCuriosityPolicy.COOLDOWN_TICKS,
                tickPointing(ItemCuriosityStateMachine.pointHand(), false).cooldown());
        State inspecting = acceptHandoff(ItemCuriosityStateMachine.pointHand());
        assertEquals(new State(Phase.INSPECTING, 80, 0, true), inspecting);
        assertThrows(IllegalStateException.class,
                () -> acceptHandoff(new State(Phase.NONE, 0, ItemCuriosityPolicy.COOLDOWN_TICKS, false)));
    }

    @Test void offlineLoadDefersReturnWithoutReplayingInspectionOrGrantingTrust() {
        State restored = restoreOwnedStack();
        assertEquals(Phase.RETURN_PENDING, restored.phase());
        assertTrue(restored.ownsStack());
        assertEquals(restored, afterReturn(restored, ReturnResult.FAILED));
        assertFalse(grantsTrust(false, false));
    }

    @Test void fullInventoryFallsBackToDropAndDropFailureRetainsOwnership() {
        State pending = restoreOwnedStack();
        assertFalse(afterReturn(pending, ReturnResult.DROP_ACCEPTED).ownsStack());
        State failed = afterReturn(pending, ReturnResult.FAILED);
        assertTrue(failed.ownsStack());
        assertEquals(Phase.RETURN_PENDING, failed.phase());
    }

    @Test void staleRecoveryDiscardNeverMaterialisesSnapshotStack() {
        assertEquals(OwnershipAction.KEEP,
                removalAction(Removal.DISCARDED, 4, 5, true));
        assertEquals(OwnershipAction.KEEP,
                removalAction(Removal.DIMENSION_TRANSFER, 5, 5, true));
        assertEquals(OwnershipAction.KEEP,
                removalAction(Removal.UNLOADED, 5, 5, true));
        assertEquals(OwnershipAction.DROP,
                removalAction(Removal.KILLED, 5, 5, true));
    }

    @Test void everyRequiredHigherPriorityActivityInterrupts() {
        assertTrue(shouldInterrupt(new Interruption(true, false, false, false, false, false, false, false, false, false)));
        assertTrue(shouldInterrupt(new Interruption(false, true, false, false, false, false, false, false, false, false)));
        assertTrue(shouldInterrupt(new Interruption(false, false, true, false, false, false, false, false, false, false)));
        assertTrue(shouldInterrupt(new Interruption(false, false, false, true, false, false, false, false, false, false)));
        assertTrue(shouldInterrupt(new Interruption(false, false, false, false, true, false, false, false, false, false)));
        assertFalse(shouldInterrupt(new Interruption(false, false, false, false, false, false, false, false, false, false)));
    }

    @Test void trustIsOncePerItemIdAndOnlyAfterSuccessfulReturn() {
        assertTrue(grantsTrust(false, true));
        assertFalse(grantsTrust(true, true));
        assertFalse(grantsTrust(false, false));
    }
}
