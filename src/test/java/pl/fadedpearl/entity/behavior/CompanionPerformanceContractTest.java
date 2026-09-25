package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionPerformanceContractTest {
    @Test
    void entityRoutesExpensiveAmbientQueriesThroughExplicitCadences() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        assertTrue(source.contains("CompanionPerformancePolicy.LIGHT_SCAN_TICKS"));
        assertTrue(source.contains("CompanionPerformancePolicy.NAMED_PET_SCAN_TICKS"));
        assertTrue(source.contains("CompanionPerformancePolicy.PROTECTOR_HOSTILE_SCAN_TICKS"));
        assertTrue(source.contains("CompanionPerformancePolicy.AMBIENT_MOB_SCAN_TICKS"));
        assertTrue(source.contains("CompanionPerformancePolicy.CURIOSITY_IDLE_SCAN_TICKS"));
        assertTrue(source.contains("CompanionPerformancePolicy.FADE_MEETING_MISS_COOLDOWN_TICKS"));
    }
}
