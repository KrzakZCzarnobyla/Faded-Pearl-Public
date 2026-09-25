package pl.fadedpearl.world;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionRecoveryServiceContractTest {
    @Test
    void foreignFriendIsRejectedBeforeEpochCanReplaceTheCanonicalCompanion() throws Exception {
        String method = method("private static FadedEnderman findCanonical",
                "private static void loadLastKnown");

        int ownerGuard = method.indexOf("CompanionRecoveryGuard.matchesOwner");
        int epochSelection = method.indexOf("CompanionRecoveryGuard.chooseDuplicate");
        assertTrue(ownerGuard >= 0, "canonical lookup must validate the owner ledger");
        assertTrue(epochSelection > ownerGuard,
                "a foreign Friend must be discarded before its epoch can win duplicate selection");
    }

    @Test
    void deadUnhealedAndOfflineOwnersCannotReachRestore() throws Exception {
        String tick = method("private static void tickCompanion", "private static void snapshotLiveCompanion");

        int lifeGate = tick.indexOf("data.isDead(id) || !data.isHealed(id)");
        int ownerGate = tick.indexOf("ownerId == null || server.getPlayerList().getPlayer(ownerId) == null");
        int restore = tick.indexOf("restore(server, data, id, now)");
        assertAll(
                () -> assertTrue(lifeGate >= 0, "dead or unhealed companions must stop immediately"),
                () -> assertTrue(ownerGate > lifeGate, "missing companions require an online owner"),
                () -> assertTrue(restore > ownerGate, "restore must stay behind both gates"));
    }

    @Test
    void restoreRequiresSnapshotOwnerAndMatchingFriend() throws Exception {
        String restore = method("private static void restore", "private static void deliverPendingCuriosityReturn");

        assertAll(
                () -> assertTrue(restore.contains("snapshot.isEmpty() || owner.isEmpty()")),
                () -> assertTrue(restore.contains("CompanionRecoveryGuard.matchesOwner(owner, snapshotFriend)")),
                () -> assertTrue(restore.contains("ServerPlayer friend = server.getPlayerList().getPlayer(friendId)")),
                () -> assertTrue(restore.contains("if (friend == null) return")));
    }

    @Test
    void repeatedTicksKeepOneMissingTimestampAndOneTicketSet() throws Exception {
        String tick = method("private static void tickCompanion", "private static void snapshotLiveCompanion");
        String tickets = method("private static void ensureTickets", "private static void releaseTickets");

        assertAll(
                () -> assertTrue(tick.contains("if (missingSince == 0L)")),
                () -> assertTrue(tick.contains("data.setRecoveryMissingSince(id, now); return;")),
                () -> assertTrue(tickets.contains("current.matches(dimension, center, position)) return")),
                () -> assertTrue(tickets.indexOf("current.matches") < tickets.indexOf("releaseTickets(server, companion)")));
    }

    @Test
    void transitionGraceDefersOnlyEqualEpochDuplicatesAndExpiresByGameTime() throws Exception {
        String canonical = method("private static FadedEnderman findCanonical", "private static void loadLastKnown");
        String transition = method("public static void noteTransition", "public static void requestRecovery");

        assertAll(
                () -> assertTrue(canonical.contains("server.overworld().getGameTime()")),
                () -> assertTrue(canonical.contains("< TRANSITION_GRACE.getOrDefault(id, 0L)")),
                () -> assertTrue(canonical.contains("CompanionRecoveryGuard.chooseDuplicate")),
                () -> assertTrue(transition.contains("now + FadedServerConfig.recoveryGraceTicks()")),
                () -> assertTrue(transition.contains("setRecoveryMissingSince(companion.getUUID(), 0L)")));
    }

    private static String method(String start, String end) throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/world/CompanionRecoveryService.java"));
        int startIndex = service.indexOf(start);
        int endIndex = service.indexOf(end, startIndex);
        assertTrue(startIndex >= 0, "missing method start: " + start);
        assertTrue(endIndex > startIndex, "missing method end: " + end);
        return service.substring(startIndex, endIndex);
    }
}
