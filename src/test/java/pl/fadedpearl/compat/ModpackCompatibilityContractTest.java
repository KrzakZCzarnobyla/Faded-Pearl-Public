package pl.fadedpearl.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ModpackCompatibilityContractTest {
    @Test
    void fadedEndermanCannotBecomePassengerOfExternalTransport() throws Exception {
        String events = Files.readString(Path.of("src/main/java/pl/fadedpearl/event/CommonEvents.java"));
        assertTrue(events.contains("EntityMountEvent"));
        assertTrue(events.contains("event.getEntityMounting() instanceof FadedEnderman"));
        assertTrue(events.contains("event.setCanceled(true)"));
    }

    @Test
    void dynamicPearlRecipeExposesGenericViewerMetadata() throws Exception {
        String recipe = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/recipe/PulsatingPearlRecipe.java"));
        assertTrue(recipe.contains("Ingredient.of(ModItems.WATER_FILLED_PEARL.get())"));
        assertTrue(recipe.contains("Ingredient.of(ItemTags.SMALL_FLOWERS)"));
        assertTrue(recipe.contains("getResultItem(RegistryAccess access)"));
    }

    @Test
    void recoveryDiscardsEveryAdditionalCopyIncludingEqualEpoch() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/world/CompanionRecoveryService.java"));
        String method = service.substring(service.indexOf("private static FadedEnderman findCanonical"),
                service.indexOf("private static void loadLastKnown"));
        assertTrue(method.contains("canonical.discard()"));
        assertTrue(method.contains("candidate.discard()"));
        assertTrue(method.contains("transitionGraceActive"));
        assertFalse(method.contains("candidate.getRecoveryEpoch() > canonical.getRecoveryEpoch()) canonical = candidate"));
    }
}
