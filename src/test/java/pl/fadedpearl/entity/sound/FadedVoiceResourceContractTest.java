package pl.fadedpearl.entity.sound;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FadedVoiceResourceContractTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/faded_pearl");
    private static final List<String> EVENTS = List.of(
            "faded_enderman_wounded_notice",
            "faded_enderman_cave_cry",
            "faded_enderman_ambient_1",
            "faded_enderman_ambient_2",
            "faded_enderman_ambient_3");

    @Test
    void everyVoiceEventIsRegisteredAndBackedByOneValidVorbisResource() throws Exception {
        String registry = Files.readString(Path.of("src/main/java/pl/fadedpearl/registry/ModSounds.java"));
        String sounds = Files.readString(ASSETS.resolve("sounds.json"));
        for (String event : EVENTS) {
            assertTrue(registry.contains("register(\"" + event + "\")"), event);
            assertEquals(1, occurrences(sounds, "\"" + event + "\": {"), event);
            assertEquals(1, occurrences(sounds, "\"name\": \"faded_pearl:entity/" + event + "\""), event);

            byte[] ogg = Files.readAllBytes(ASSETS.resolve("sounds/entity/" + event + ".ogg"));
            assertTrue(ogg.length > 4, event);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, new byte[]{ogg[0], ogg[1], ogg[2], ogg[3]}, event);
            assertTrue(new String(ogg, StandardCharsets.ISO_8859_1).contains("vorbis"), event);
        }
    }

    @Test
    void subtitleKeysExistInBothLanguages() throws Exception {
        String English = Files.readString(ASSETS.resolve("lang/en_us.json"));
        String Polish = Files.readString(ASSETS.resolve("lang/pl_pl.json"));
        for (String key : List.of(
                "subtitles.faded_pearl.faded_enderman_wounded_notice",
                "subtitles.faded_pearl.faded_enderman_cave_cry",
                "subtitles.faded_pearl.faded_enderman_ambient")) {
            assertTrue(English.contains("\"" + key + "\""), key);
            assertTrue(Polish.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void entityPairsEachAmbientEventWithAControllerPoseWithoutTextDialogueOrNbt() throws Exception {
        String entity = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String ambient = between(entity, "private void tickAmbientVoice()", "private void spawnEmotionalParticles()");
        assertEquals(1, occurrences(ambient, "level().playSound("));
        assertEquals(3, occurrences(ambient, "ModSounds.FADED_ENDERMAN_AMBIENT_"));
        assertFalse(ambient.contains("FadedDialogue"));
        assertFalse(ambient.contains("modifyTrust"));
        assertFalse(ambient.contains("addTrust"));

        String codec = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/persistence/FadedPersistenceCodec.java"));
        assertFalse(codec.contains("ambientVoiceCooldown"));
        assertFalse(codec.contains("AMBIENT_VOICE"));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue(from >= 0 && to > from, start);
        return source.substring(from, to);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }
}
