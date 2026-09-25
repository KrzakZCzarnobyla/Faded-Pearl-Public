package pl.fadedpearl.entity.dialogue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedDialogueCallSiteTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");
    private static final Pattern CATEGORY_CALL = Pattern.compile("FadedDialogue\\.Category\\.([A-Z_]+)");

    @Test
    void randomDialogueCallSitesUseTheMemoryApi() throws IOException {
        String source = Files.readString(ENTITY);
        Matcher matcher = CATEGORY_CALL.matcher(source);
        Set<String> categories = new HashSet<>();
        int calls = 0;
        while (matcher.find()) {
            calls++;
            categories.add(matcher.group(1));
        }

        assertEquals(45, calls);
        assertEquals(45, categories.size());
        assertFalse(source.contains("FadedDialogue.variantKey("));
        assertFalse(source.contains("FadedDialogue.sendRandom"));
    }

    @Test
    void everyCategoryRetainsItsExistingKeysInBothLocales() throws IOException {
        JsonObject english = locale("en_us");
        JsonObject polish = locale("pl_pl");

        for (FadedDialogue.Category category : FadedDialogue.Category.values()) {
            for (int index = 0; index < category.variants(); index++) {
                String key = category.baseKey() + (category.firstIndex() + index);
                assertTrue(english.has(key), "Missing EN key: " + key);
                assertTrue(polish.has(key), "Missing PL key: " + key);
            }
            assertEquals(category.name().startsWith("COMMAND_"), category.delivery().overlay(), category.name());
        }
    }

    @Test
    void allFrequentWorldReactionsHaveDedicatedFiveLinePools() {
        String[] reactions = {"flower_seen", "container", "fire", "water", "ore", "crafting",
                "cave", "stars", "rain", "animal", "villager", "held_item"};
        for (String reaction : reactions)
            assertEquals(5, FadedDialogue.worldReaction(reaction).variants(), reaction);
    }

    @Test
    void persistenceHooksCoverOrdinaryAndRecoverySavePaths() throws IOException {
        String entity = Files.readString(ENTITY);
        String codec = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/persistence/FadedPersistenceCodec.java"));

        assertEquals(1, occurrences(entity, "dialogueMemory.write(tag)"));
        assertEquals(1, occurrences(entity, "dialogueMemory.read(tag)"));
        assertTrue(codec.contains("FadedDialogue.MEMORY_NBT_KEY"));
    }

    private static JsonObject locale(String locale) throws IOException {
        Path path = Path.of("src/main/resources/assets/faded_pearl/lang/" + locale + ".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
