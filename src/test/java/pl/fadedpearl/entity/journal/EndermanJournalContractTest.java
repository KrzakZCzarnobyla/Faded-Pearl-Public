package pl.fadedpearl.entity.journal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class EndermanJournalContractTest {
    private static final Path MAIN = Path.of("src/main/java/pl/fadedpearl");
    private static final Path ASSETS = Path.of("src/main/resources/assets/faded_pearl");

    @Test
    void acquisitionIsMainHandServerOnlyFriendHealedAndDownedSafe() throws Exception {
        String entity = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String method = entity.substring(entity.indexOf("protected InteractionResult mobInteract"),
                entity.indexOf("private FadedInteractionHandler.HeldItem classifyHeldItem"));
        String issuance = method.substring(method.indexOf("private InteractionResult issueEndermanJournal"));
        assertAll(
                () -> assertTrue(method.contains("mainHand && player.getItemInHand(hand).is(Items.BOOK)")),
                () -> assertTrue(method.contains("serverPlayerPresent && isHealed() && !isDowned() && isFriend(player)")),
                () -> assertTrue(method.indexOf("issueEndermanJournal") < method.indexOf("curiosityPhase == CuriosityPhase.POINT_HAND")),
                () -> assertTrue(method.contains("if (!delivered) return InteractionResult.PASS")),
                () -> assertTrue(method.indexOf("if (!delivered) return") < method.indexOf("book.shrink(1)")),
                () -> assertTrue(method.contains("if (!player.getAbilities().instabuild) book.shrink(1)")),
                () -> assertTrue(method.contains("new ItemEntity(player.serverLevel(), drop.x, drop.y + 0.5D, drop.z, journal)")),
                () -> assertFalse(issuance.contains("addTrust(")),
                () -> assertFalse(issuance.contains("modifyTrust(")));
    }

    @Test
    void openingUsesOnlyServerCanonicalOrRecoveryState() throws Exception {
        String item = Files.readString(MAIN.resolve("item/EndermanJournalItem.java"));
        String access = Files.readString(MAIN.resolve("entity/journal/EndermanJournalAccess.java"));
        String packet = Files.readString(MAIN.resolve("network/OpenEndermanJournalPacket.java"));
        assertAll(
                () -> assertTrue(item.contains("EndermanJournalAccess.open(serverPlayer, stack)")),
                () -> assertTrue(access.contains("data.companionOf(player.getUUID()).filter(companionId::equals)")),
                () -> assertTrue(access.contains("player.getUUID().equals(tag.getUUID(FadedPersistenceCodec.FRIEND))")),
                () -> assertTrue(access.contains("loaded.createJournalSnapshot(player.getUUID())")),
                () -> assertTrue(access.contains("loaded.getRecoveryEpoch() < data.recoveryEpoch(companionId)")),
                () -> assertTrue(access.contains("data.companionSnapshot(companionId)")),
                () -> assertFalse(access.contains("snapshot.getCompound(\"tag\")")),
                () -> assertTrue(packet.contains("validDays")),
                () -> assertTrue(packet.contains("validBehaviors")),
                () -> assertTrue(packet.contains("buffer.writeVarInt(packet.trust)")),
                () -> assertTrue(packet.contains("NameLearningMemory.sanitizeName")));
    }

    @Test
    void itemAndModelUseApprovedContract() throws Exception {
        String registry = Files.readString(MAIN.resolve("registry/ModItems.java"));
        JsonObject model = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item/enderman_journal.json")))
                .getAsJsonObject();
        BufferedImage icon = ImageIO.read(ASSETS.resolve("textures/item/enderman_journal_pearl.png").toFile());
        assertAll(() -> assertTrue(registry.contains("new EndermanJournalItem(new Item.Properties().stacksTo(1))")),
                () -> assertEquals("minecraft:item/generated", model.get("parent").getAsString()),
                () -> assertEquals("faded_pearl:item/enderman_journal_pearl",
                        model.getAsJsonObject("textures").get("layer0").getAsString()),
                () -> assertNotNull(icon),
                () -> assertEquals(32, icon.getWidth()),
                () -> assertEquals(32, icon.getHeight()),
                () -> assertEquals(0, icon.getRGB(0, 0) >>> 24));
    }

    @Test
    void localesHaveExactlyTheSameJournalKeys() throws Exception {
        JsonObject english = JsonParser.parseString(Files.readString(ASSETS.resolve("lang/en_us.json"))).getAsJsonObject();
        JsonObject polish = JsonParser.parseString(Files.readString(ASSETS.resolve("lang/pl_pl.json"))).getAsJsonObject();
        Set<String> englishKeys = english.keySet().stream().filter(EndermanJournalContractTest::journalKey)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> polishKeys = polish.keySet().stream().filter(EndermanJournalContractTest::journalKey)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(englishKeys, polishKeys);
        assertEquals(91, englishKeys.size());
        for (EndermanJournalSnapshot.BehaviorEntry entry : EndermanJournalSnapshot.BehaviorEntry.values())
            assertTrue(englishKeys.contains("journal.faded_pearl.behavior."
                    + entry.name().toLowerCase(java.util.Locale.ROOT) + ".use"));
        assertTrue(englishKeys.contains("journal.faded_pearl.behavior.social_reposition.text"));
        assertTrue(englishKeys.contains("journal.faded_pearl.behavior.animal_carry.text"));
        assertTrue(englishKeys.contains("journal.faded_pearl.basic.trust_value"));
        assertTrue(englishKeys.contains("journal.faded_pearl.basic.origin.fade_story"));
        assertTrue(englishKeys.contains("toast.faded_pearl.journal.title"));
        assertTrue(englishKeys.contains("toast.faded_pearl.journal.message"));
    }

    @Test
    void fadeOriginRequiresObservedHighTrustCommandAndStaysServerOwned() throws Exception {
        String entity = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String method = entity.substring(entity.indexOf("public void respondToCommand"),
                entity.indexOf("private boolean returnCuriosityStack"));
        String screen = Files.readString(MAIN.resolve("client/EndermanJournalScreen.java"));
        assertTrue(method.contains("if (trust >= 60) journalMemory.discover(JournalMemory.Discovery.BASIC_FADE_ORIGIN)"));
        assertTrue(screen.contains("if (snapshot.fadeOriginKnown())"));
        assertTrue(screen.contains("journal.faded_pearl.basic.origin.fade_story"));
    }

    @Test
    void animalCarryDiscoveryRequiresSuccessfulMount() throws Exception {
        String entity = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String carry = entity.substring(entity.indexOf("private void tickSmallAnimalCarry"),
                entity.indexOf("private boolean isEligibleSmallAnimal"));
        int mountSuccess = carry.indexOf("if (mounted) {");
        int discovery = carry.indexOf("journalMemory.discover(JournalMemory.Discovery.BEHAVIOR_ANIMAL_CARRY)");
        int mountFailure = carry.indexOf("else cancelAnimalCarryAttempt()", mountSuccess);
        assertTrue(mountSuccess >= 0 && discovery > mountSuccess && discovery < mountFailure);
    }

    @Test
    void hiddenBehaviorsDoNotRevealTrustRequirements() throws Exception {
        String screen = Files.readString(MAIN.resolve("client/EndermanJournalScreen.java"));
        assertTrue(screen.contains("if (unlocked) EndermanJournalTrustGuide.requiredTrust(entry)"));
        assertTrue(screen.indexOf("if (unlocked) {") < screen.indexOf("journal.faded_pearl.behavior.how_to"));
        assertTrue(screen.contains("snapshot.trust()"));
    }

    @Test
    void escapePearlDiscoveryAndBothCraftingDiagramsUseTheJournalContract() throws Exception {
        String entity = Files.readString(MAIN.resolve("entity/FadedEnderman.java"));
        String policy = Files.readString(MAIN.resolve("entity/journal/EndermanJournalPolicy.java"));
        String screen = Files.readString(MAIN.resolve("client/EndermanJournalScreen.java"));
        JsonObject escapeRecipe = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/faded_pearl/recipes/escape_pearl.json"))).getAsJsonObject();
        assertAll(
                () -> assertTrue(entity.contains("JournalMemory.Discovery.ESCAPE_PEARL_PLAYER_HIT")),
                () -> assertTrue(entity.contains("JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD")),
                () -> assertTrue(policy.contains("&& facts.journal().contains(JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD)")),
                () -> assertEquals("[\"MEM\",\"APA\",\"MCM\"]",
                        escapeRecipe.getAsJsonArray("pattern").toString()),
                () -> assertTrue(screen.contains("addRecipeRows(content, \"mem\", \"apa\", \"mcm\")")),
                () -> assertTrue(screen.contains("addRecipeRows(content, \"apa\", \"aca\", \"aea\")")),
                () -> assertTrue(screen.contains("BehaviorEntry.ESCAPE_PEARL")),
                () -> assertTrue(screen.contains("BehaviorEntry.HOME")));
    }

    private static boolean journalKey(String key) {
        return key.contains("enderman_journal") || key.contains("faded_pearl.journal")
                || key.startsWith("journal.faded_pearl.");
    }
}
