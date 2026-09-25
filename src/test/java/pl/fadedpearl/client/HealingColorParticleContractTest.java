package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class HealingColorParticleContractTest {
    @Test
    void healedFadeEmitsFrequentParticlesUsingItsSyncedHealingColor() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String particles = source.substring(source.indexOf("private void spawnEmotionalParticles()"),
                source.indexOf("private Player getFriendPlayer()"));
        assertTrue(particles.contains("random.nextInt(4) == 0"));
        assertTrue(particles.contains("int color = getHealingColor()"));
        assertTrue(particles.contains("new DustParticleOptions(rgb, 0.55F)"));
    }
}
