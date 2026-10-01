package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.fadedpearl.entity.behavior.FadedNightWatchPolicy.CompletionDecision.NONE;
import static pl.fadedpearl.entity.behavior.FadedNightWatchPolicy.CompletionDecision.WATCH_COMPLETED;
import static pl.fadedpearl.entity.behavior.FadedNightWatchPolicy.Phase.ABORTED_SESSION;
import static pl.fadedpearl.entity.behavior.FadedNightWatchPolicy.Phase.WATCHING_SESSION;

final class FadedNightWatchPolicyTest {
    @Test
    void requiresOneHundredActualWatchingTicks() {
        FadedNightWatchPolicy.State ninetyNine = watch(FadedNightWatchPolicy.State.initial(), 99);
        FadedNightWatchPolicy.Transition earlyWake = wake(ninetyNine, true);
        FadedNightWatchPolicy.State oneHundred = watch(FadedNightWatchPolicy.State.initial(), 100);
        FadedNightWatchPolicy.Transition qualifiedWake = wake(oneHundred, true);

        assertAll(
                () -> assertEquals(99, ninetyNine.watchTicks()),
                () -> assertEquals(NONE, earlyWake.decision()),
                () -> assertEquals(100, oneHundred.watchTicks()),
                () -> assertEquals(WATCH_COMPLETED, qualifiedWake.decision()));
    }

    @Test
    void approachPausesWithoutCountingOrAbortingTheSession() {
        FadedNightWatchPolicy.State state = watch(FadedNightWatchPolicy.State.initial(), 40);
        for (int tick = 0; tick < 250; tick++) {
            state = sleepTick(state, false, false);
        }
        FadedNightWatchPolicy.State paused = state;
        state = watch(state, 60);
        FadedNightWatchPolicy.State qualified = state;

        assertAll(
                () -> assertEquals(WATCHING_SESSION, paused.phase()),
                () -> assertEquals(40, paused.watchTicks()),
                () -> assertEquals(WATCH_COMPLETED, wake(qualified, true).decision()));
    }

    @Test
    void hardBlockBeforeQualificationAbortsTheWholeSession() {
        FadedNightWatchPolicy.State state = watch(FadedNightWatchPolicy.State.initial(), 99);
        state = sleepTick(state, false, true);
        FadedNightWatchPolicy.State aborted = watch(state, 100);

        assertAll(
                () -> assertEquals(ABORTED_SESSION, aborted.phase()),
                () -> assertEquals(99, aborted.watchTicks()),
                () -> assertEquals(NONE, wake(aborted, true).decision()));
    }

    @Test
    void hardBlockAfterQualificationStillAbortsTheWholeSession() {
        FadedNightWatchPolicy.State qualified = watch(FadedNightWatchPolicy.State.initial(), 100);
        FadedNightWatchPolicy.State aborted = sleepTick(qualified, false, true);

        assertAll(
                () -> assertEquals(ABORTED_SESSION, aborted.phase()),
                () -> assertEquals(100, aborted.watchTicks()),
                () -> assertEquals(NONE, wake(aborted, true).decision()));
    }

    @Test
    void qualifiedWakeEmitsExactlyOnceAndResets() {
        FadedNightWatchPolicy.Transition completion = wake(
                watch(FadedNightWatchPolicy.State.initial(), 100), true);
        FadedNightWatchPolicy.Transition repeatedWake = wake(completion.state(), true);

        assertAll(
                () -> assertEquals(WATCH_COMPLETED, completion.decision()),
                () -> assertEquals(FadedNightWatchPolicy.State.initial(), completion.state()),
                () -> assertEquals(NONE, repeatedWake.decision()),
                () -> assertEquals(FadedNightWatchPolicy.State.initial(), repeatedWake.state()));
    }

    @Test
    void qualifiedWakeWithoutReactionPermissionIsConsumedWithoutCompletion() {
        FadedNightWatchPolicy.Transition noOwnerReaction = wake(
                watch(FadedNightWatchPolicy.State.initial(), 100), false);

        assertAll(
                () -> assertEquals(NONE, noOwnerReaction.decision()),
                () -> assertEquals(FadedNightWatchPolicy.State.initial(), noOwnerReaction.state()),
                () -> assertEquals(NONE, wake(noOwnerReaction.state(), true).decision()));
    }

    @Test
    void freshSleepCanSucceedAfterAnAbortedSleepEnds() {
        FadedNightWatchPolicy.State firstSleep = sleepTick(
                FadedNightWatchPolicy.State.initial(), false, true);
        FadedNightWatchPolicy.Transition interruptedWake = wake(firstSleep, true);
        FadedNightWatchPolicy.Transition laterWake = wake(watch(interruptedWake.state(), 100), true);

        assertAll(
                () -> assertEquals(ABORTED_SESSION, firstSleep.phase()),
                () -> assertEquals(NONE, interruptedWake.decision()),
                () -> assertEquals(WATCH_COMPLETED, laterWake.decision()));
    }

    @Test
    void watchingTicksSaturateWithoutOverflow() {
        FadedNightWatchPolicy.State maximum = new FadedNightWatchPolicy.State(
                WATCHING_SESSION, Integer.MAX_VALUE);
        FadedNightWatchPolicy.State advanced = sleepTick(maximum, true, false);

        assertAll(
                () -> assertEquals(Integer.MAX_VALUE, advanced.watchTicks()),
                () -> assertEquals(WATCH_COMPLETED, wake(advanced, true).decision()));
    }

    @Test
    void idleWakeAndRapidSleepDoNotComplete() {
        FadedNightWatchPolicy.Transition idle = wake(FadedNightWatchPolicy.State.initial(), true);
        FadedNightWatchPolicy.Transition rapid = wake(
                sleepTick(FadedNightWatchPolicy.State.initial(), true, false), true);

        assertAll(
                () -> assertEquals(FadedNightWatchPolicy.State.initial(), idle.state()),
                () -> assertEquals(NONE, idle.decision()),
                () -> assertEquals(NONE, rapid.decision()),
                () -> assertEquals(FadedNightWatchPolicy.State.initial(), rapid.state()));
    }

    private static FadedNightWatchPolicy.State watch(FadedNightWatchPolicy.State state, int ticks) {
        FadedNightWatchPolicy.State current = state;
        for (int tick = 0; tick < ticks; tick++) {
            current = sleepTick(current, true, false);
        }
        return current;
    }

    private static FadedNightWatchPolicy.State sleepTick(
            FadedNightWatchPolicy.State state, boolean watching, boolean hardBlocked) {
        return FadedNightWatchPolicy.advance(state,
                new FadedNightWatchPolicy.Snapshot(true, watching, hardBlocked, false)).state();
    }

    private static FadedNightWatchPolicy.Transition wake(
            FadedNightWatchPolicy.State state, boolean canReact) {
        return FadedNightWatchPolicy.advance(state,
                new FadedNightWatchPolicy.Snapshot(false, false, false, canReact));
    }
}
