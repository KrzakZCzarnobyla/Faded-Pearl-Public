package pl.fadedpearl.world;

import java.util.Optional;
import java.util.UUID;

/** Pure recovery identity and duplicate-selection rules shared by the server adapter and tests. */
final class CompanionRecoveryGuard {
    enum DuplicateDecision { KEEP_CURRENT, REPLACE_CURRENT, DEFER_EQUAL_DURING_TRANSITION }

    static boolean matchesOwner(Optional<UUID> authoritativeOwner, Optional<UUID> snapshotFriend) {
        return authoritativeOwner.isEmpty()
                || snapshotFriend.filter(authoritativeOwner.orElseThrow()::equals).isPresent();
    }

    static DuplicateDecision chooseDuplicate(int currentEpoch, int candidateEpoch,
                                             boolean transitionGraceActive) {
        if (candidateEpoch > currentEpoch) return DuplicateDecision.REPLACE_CURRENT;
        if (candidateEpoch == currentEpoch && transitionGraceActive)
            return DuplicateDecision.DEFER_EQUAL_DURING_TRANSITION;
        return DuplicateDecision.KEEP_CURRENT;
    }

    private CompanionRecoveryGuard() {}
}
