package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelationshipEventMemoryContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void villageUsesOnePendingEventPerOutsideToInsideTransition() throws IOException {
        String source = Files.readString(ENTITY);
        String awareness = awarenessImplementation(source);

        assertTrue(source.contains("private boolean friendWasInVillage;"));
        assertTrue(source.contains("private boolean villageEntryPending;"));
        assertTrue(awareness.contains("if (!friendInVillage)"));
        assertTrue(awareness.contains("else if (!friendWasInVillage)"));
        assertTrue(awareness.contains("villageEntryPending = true;"));
        assertTrue(awareness.contains("if (villageEntryPending)"));
        assertTrue(awareness.contains("villageEntryPending = false;"));
        assertTrue(awareness.contains("FadedRelationshipDialoguePolicy.Context.VILLAGE"));
    }

    @Test
    void playerDiamondUsesTheExistingNoDiamondToDiamondEdge() throws IOException {
        String awareness = awarenessImplementation(Files.readString(ENTITY));

        assertTrue(awareness.contains("if (!friendHadDiamond && hasDiamond)"));
        assertTrue(awareness.contains("FadedRelationshipDialoguePolicy.Context.PLAYER_DIAMOND"));
        assertTrue(awareness.contains("friendHadDiamond = hasDiamond;"));
    }

    @Test
    void fadeOnlyReactsWhenTheVisibleOrePositionChanges() throws IOException {
        String source = Files.readString(ENTITY);
        String awareness = awarenessImplementation(source);
        String scan = source.substring(source.indexOf("private BlockPos findVisibleDiamondOre()"),
                source.indexOf("public void observeTamedAnimal"));

        assertTrue(source.contains("private BlockPos lastVisibleDiamondOre;"));
        assertTrue(awareness.contains("!visibleDiamondOre.equals(lastVisibleDiamondOre)"));
        assertTrue(awareness.indexOf("if (newlyVisibleDiamondOre)")
                < awareness.indexOf("lastVisibleDiamondOre = visibleDiamondOre;"));
        assertTrue(awareness.contains("FadedRelationshipDialoguePolicy.Context.ENDERMAN_DIAMOND"));
        assertFalse(scan.contains("queueLookAt("), "The scan itself must not repeatedly animate the same ore");
    }

    @Test
    void transientEdgeStateIsResetOnLoadAndNeverPersisted() throws IOException {
        String source = Files.readString(ENTITY);
        String write = source.substring(source.indexOf("public void addAdditionalSaveData"),
                source.indexOf("public void readAdditionalSaveData"));
        String read = source.substring(source.indexOf("public void readAdditionalSaveData"));

        assertFalse(write.contains("friendWasInVillage"));
        assertFalse(write.contains("villageEntryPending"));
        assertFalse(write.contains("lastVisibleDiamondOre"));
        assertTrue(read.contains("friendWasInVillage = false;"));
        assertTrue(read.contains("villageEntryPending = false;"));
        assertTrue(read.contains("lastVisibleDiamondOre = null;"));
    }

    @Test
    void socialActionOnlyStartsAfterDialogueSelectionSucceeds() throws IOException {
        String source = Files.readString(ENTITY);
        String adapter = source.substring(source.indexOf("private boolean observeRelationshipAwareness"),
                source.indexOf("private void applyWeatherState"));

        int discovery = adapter.indexOf("worldAwarenessMemory.markFirst(milestone)");
        int selection = adapter.indexOf("FadedRelationshipDialoguePolicy.select(");
        int noneGuard = adapter.indexOf("selection == FadedRelationshipDialoguePolicy.Selection.NONE");
        int action = adapter.indexOf("setSocialAction(action)");
        assertTrue(discovery >= 0 && selection > discovery && noneGuard > selection && action > noneGuard);
    }

    private static String awarenessImplementation(String source) {
        return source.substring(source.indexOf("private void tickWorldAwareness"),
                source.indexOf("private boolean inventoryContains"));
    }
}
