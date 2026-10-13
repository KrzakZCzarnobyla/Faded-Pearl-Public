package pl.fadedpearl.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryPlayerSafetyContractTest {
    @Test
    void carriedPlayerCannotAccumulateOrReceiveFallDamage() throws Exception {
        String entity = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String events = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/event/CommonEvents.java"));

        String ridden = between(entity, "protected void tickRidden(", "protected Vec3 getRiddenInput(");
        String position = between(entity, "protected void positionRider(", "private Vec3 animalCarryPosition(");
        String removal = between(entity, "protected void removePassenger(", "private Vec3 animalCarryPosition(");

        assertTrue(ridden.contains("player.fallDistance = 0.0F"));
        assertTrue(position.contains("player.fallDistance = 0.0F"));
        assertTrue(removal.contains("passenger.fallDistance = 0.0F"));
        assertTrue(events.contains("protectCarriedPlayerFromFall(LivingFallEvent event)"));
        assertTrue(events.contains("companion.getControllingPassenger() == player"));
        assertTrue(events.contains("event.setCanceled(true)"));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        return source.substring(from, to);
    }
}
