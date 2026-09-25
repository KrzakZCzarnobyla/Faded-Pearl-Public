package pl.fadedpearl.entity.protection;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EscapePearlContractTest {
    private static final Path MAIN = Path.of("src/main/java/pl/fadedpearl");

    @Test
    void entityAdapterCancelsOwnerDamageAndTrustLossWithoutConsumingProtection() throws IOException {
        String source = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String hurt = source.substring(source.indexOf("public boolean hurt(DamageSource source"),
                source.indexOf("private void tickExpansionDetectors()"));
        assertTrue(source.contains("source.getEntity() instanceof Player player"));
        assertTrue(source.contains("EscapePearlPolicy.decide(escapePearlArmed"));
        assertTrue(hurt.contains("tryEscapePearlTeleport(player);"));
        assertFalse(hurt.contains("escapePearlArmed = false;"));
        assertTrue(hurt.indexOf("return false;") < hurt.indexOf("modifyTrust(FadedTrustManager.PLAYER_ATTACK)"));
    }

    @Test
    void armedStateIsSavedLoadedAndIncludedInRecovery() throws IOException {
        String entity = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String codec = Files.readString(MAIN.resolve("entity/persistence/FadedPersistenceCodec.java"));
        assertTrue(entity.contains("tag.putBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED, escapePearlArmed)"));
        assertTrue(entity.contains("escapePearlArmed = tag.getBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED)"));
        assertTrue(codec.contains("ANIMAL_SAME_COOLDOWN, ANIMAL_LAST_ID, ESCAPE_PEARL_ARMED"));
    }
}
