package pl.fadedpearl.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarryWaterEscapeCorrectionTest {
    private static final Path SOURCE = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void carryEscapeSelectsPassengerSafeLandingAndUsesSynchronizedTeleport() throws IOException {
        String source = Files.readString(SOURCE);
        String method = between(source, "private void escapeWaterToDryLand()",
                "private boolean teleportNearGroundedFriend(");

        assertTrue(method.contains("getControllingPassenger() instanceof ServerPlayer rider && isFriend(rider)"));
        assertTrue(method.contains("findNearestSafePassengerLanding(rider, blockPosition(), 16, 12)"));
        assertTrue(method.contains("trySafeTeleportWithPassenger(rider, passengerLanding)"));
        assertTrue(method.indexOf("return;") < method.indexOf("findNearestSafeLanding(blockPosition(), 16, 12)"));
        assertFalse(method.contains("setCarriedBlinkCooldown"));
    }

    @Test
    void passengerLandingAndRollbackValidateBothDryCollisionBoxesBeforeCorrections() throws IOException {
        String source = Files.readString(SOURCE);
        String landing = between(source, "private boolean isSafePassengerLanding(",
                "private BlockPos findNearestSafePassengerLanding(");
        String teleport = between(source, "private boolean trySafeTeleportWithPassenger(",
                "private void teleportNearFriend(");

        assertTrue(landing.contains("level().noCollision(this, vehicleBox)"));
        assertTrue(landing.contains("level().containsAnyLiquid(vehicleBox)"));
        assertTrue(landing.contains("level().noCollision(rider, riderBox)"));
        assertTrue(landing.contains("level().containsAnyLiquid(riderBox)"));

        int rollback = teleport.indexOf("teleportTo(oldX, oldY, oldZ);");
        int vehicleCorrection = teleport.indexOf("new ClientboundMoveVehiclePacket(this)");
        int playerCorrection = teleport.indexOf("rider.connection.teleport(");
        int particles = teleport.indexOf("sendParticles(");
        int sound = teleport.indexOf("playSound(");
        assertTrue(rollback >= 0 && rollback < vehicleCorrection);
        assertTrue(vehicleCorrection < playerCorrection);
        assertTrue(playerCorrection < particles);
        assertTrue(particles < sound);
    }

    @Test
    void soloEscapeKeepsExistingGenericTeleportPath() throws IOException {
        String source = Files.readString(SOURCE);
        String method = between(source, "private void escapeWaterToDryLand()",
                "private boolean teleportNearGroundedFriend(");

        assertTrue(method.contains("findNearestSafeLanding(blockPosition(), 16, 12)"));
        assertTrue(method.contains("trySafeTeleport(landing.getX() + .5D,"));
        assertTrue(method.contains("selfRescueSucceededSinceLastDecision = true"));
    }

    private static String between(String source, String start, String end) {
        return source.substring(source.indexOf(start), source.indexOf(end, source.indexOf(start)));
    }
}
