package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class WorldKnowledgeInterestContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void integrationUsesDryNavigationSafeLandingAndMovementArbitration() throws Exception {
        String source = Files.readString(ENTITY);
        String implementation = source.substring(source.indexOf("private void tickWorldInterest"),
                source.indexOf("private void tickGroundCuriosity"));
        assertAll(
                () -> assertTrue(implementation.contains("FadedWorldInterestPolicy.decide")),
                () -> assertTrue(implementation.contains("isSafeHomeLanding(serverLevel, feet)")),
                () -> assertTrue(implementation.contains("createDryPathTo(feet)")),
                () -> assertTrue(implementation.contains("primaryRouteBudget(fallbackAvailable)")),
                () -> assertTrue(implementation.contains("MAX_ROUTE_CANDIDATES")),
                () -> assertTrue(implementation.contains(
                        "FadedMovementCoordinator.Locomotion.CURIOSITY_APPROACH")),
                () -> assertFalse(implementation.contains("teleport")),
                () -> assertFalse(implementation.contains("setBlock")),
                () -> assertFalse(implementation.contains("modifyTrust")),
                () -> assertFalse(implementation.contains("addTrust")));
    }

    @Test
    void itemCuriosityAndEveryOwnerClickCanInterruptAnActiveSession() throws Exception {
        String source = Files.readString(ENTITY);
        String tickCalls = source.substring(source.indexOf("protected void customServerAiStep"),
                source.indexOf("private void tickCarryingState"));
        String interaction = source.substring(source.indexOf("protected InteractionResult mobInteract"),
                source.indexOf("private FadedInteractionHandler.HeldItem classifyHeldItem"));
        String implementation = source.substring(source.indexOf("private void tickWorldInterest"),
                source.indexOf("private void tickGroundCuriosity"));
        assertAll(
                () -> assertTrue(tickCalls.contains("if (!level().isClientSide && isHealed()) tickWorldInterest()")),
                () -> assertTrue(interaction.contains("worldInterestOwnerInteraction = true")),
                () -> assertTrue(implementation.contains("curiosityPhase != CuriosityPhase.NONE")),
                () -> assertTrue(implementation.contains("worldInterestOwnerInteraction = false")),
                () -> assertTrue(implementation.contains("isWorldInterestTargetVisibleFrom(worldInterestDestination")));
    }

    @Test
    void discoveryOccursOnlyAfterArrivalAndTransientStateIsNotPersisted() throws Exception {
        String source = Files.readString(ENTITY);
        String implementation = source.substring(source.indexOf("private void tickWorldInterest"),
                source.indexOf("private void tickGroundCuriosity"));
        int arrival = implementation.indexOf(
                "position().distanceToSqr(Vec3.atBottomCenterOf(worldInterestDestination)) <= 1.0D");
        int discovery = implementation.indexOf("journalMemory.discover(knowledgeInterest");
        int relationshipDialogue = implementation.indexOf("FadedRelationshipDialoguePolicy.select(");
        String persistence = source.substring(source.indexOf("public void addAdditionalSaveData"),
                source.indexOf("public void readAdditionalSaveData"));
        assertAll(
                () -> assertTrue(arrival >= 0 && discovery > arrival),
                () -> assertTrue(relationshipDialogue > discovery),
                () -> assertTrue(implementation.contains("setSocialAction(SocialAction.ITEM_INSPECT)")),
                () -> assertFalse(persistence.contains("worldInterest")),
                () -> assertFalse(persistence.contains("WorldInterest")));
    }

    @Test
    void knowledgeCatalogIsBoundedAndDoesNotIncludeContainers() throws Exception {
        String source = Files.readString(ENTITY);
        String implementation = source.substring(source.indexOf("private boolean isKnowledgeInterestBlock"),
                source.indexOf("private boolean isCraftsmanshipInterestBlock"));
        assertAll(
                () -> assertTrue(implementation.contains("Blocks.BOOKSHELF")),
                () -> assertTrue(implementation.contains("Blocks.CHISELED_BOOKSHELF")),
                () -> assertTrue(implementation.contains("Blocks.LECTERN")),
                () -> assertTrue(implementation.contains("Blocks.ENCHANTING_TABLE")),
                () -> assertFalse(implementation.contains("Blocks.CHEST")),
                () -> assertFalse(implementation.contains("Blocks.BARREL")));
    }

    @Test
    void craftsmanshipSharesTheSameScanAndHasABoundedCatalog() throws Exception {
        String source = Files.readString(ENTITY);
        String scan = source.substring(source.indexOf("private WorldInterestRoute findWorldInterestRoute"),
                source.indexOf("private DryRoute findWorldInterestRouteTo"));
        String catalog = source.substring(source.indexOf("private boolean isCraftsmanshipInterestBlock"),
                source.indexOf("private FadedWorldInterestCatalog.Category classifyWorldInterest"));
        assertAll(
                () -> assertEquals(1, occurrences(scan, "BlockPos.betweenClosed")),
                () -> assertTrue(scan.contains("primaryRouteBudget(fallbackAvailable)")),
                () -> assertTrue(catalog.contains("Blocks.CRAFTING_TABLE")),
                () -> assertTrue(catalog.contains("Blocks.FURNACE")),
                () -> assertTrue(catalog.contains("Blocks.ANVIL")),
                () -> assertTrue(catalog.contains("Blocks.SMITHING_TABLE")),
                () -> assertTrue(catalog.contains("Blocks.STONECUTTER")),
                () -> assertTrue(catalog.contains("Blocks.BREWING_STAND")),
                () -> assertFalse(catalog.contains("Blocks.CHEST")),
                () -> assertFalse(catalog.contains("Blocks.BARREL")));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }
}
