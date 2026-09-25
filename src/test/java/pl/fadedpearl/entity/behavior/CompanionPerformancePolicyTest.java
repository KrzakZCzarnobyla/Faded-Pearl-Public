package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionPerformancePolicyTest {
    @Test
    void expensiveScansHaveExplicitBoundedCadences() {
        assertEquals(5, CompanionPerformancePolicy.PROTECTOR_HOSTILE_SCAN_TICKS);
        assertEquals(20, CompanionPerformancePolicy.AMBIENT_MOB_SCAN_TICKS);
        assertEquals(20, CompanionPerformancePolicy.CURIOSITY_IDLE_SCAN_TICKS);
        assertEquals(100, CompanionPerformancePolicy.LIGHT_SCAN_TICKS);
        assertEquals(100, CompanionPerformancePolicy.NAMED_PET_SCAN_TICKS);
        assertEquals(100, CompanionPerformancePolicy.FADE_MEETING_MISS_COOLDOWN_TICKS);
    }

    @Test
    void entityIdsDephaseTheSameScanAcrossTicks() {
        assertTrue(CompanionPerformancePolicy.isCadenceTick(19, 1, 20));
        assertFalse(CompanionPerformancePolicy.isCadenceTick(19, 2, 20));
        assertTrue(CompanionPerformancePolicy.isCadenceTick(18, 2, 20));
        assertTrue(CompanionPerformancePolicy.isCadenceTick(1, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> CompanionPerformancePolicy.isCadenceTick(0, 0, 0));
    }
}
