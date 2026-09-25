package pl.fadedpearl.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldBalanceResourceContractTest {
    private static final Path DATA = Path.of("src/main/resources/data/faded_pearl");
    private static final Path ASSETS = Path.of("src/main/resources/assets/faded_pearl");

    @Test
    void anchorRecipeUsesOneEchoShardAndHasARecipeUnlock() throws IOException {
        JsonObject recipe = json(DATA.resolve("recipes/resonating_anchor.json"));
        String pattern = recipe.getAsJsonArray("pattern").toString();
        assertEquals(1, pattern.chars().filter(value -> value == 'E').count());
        JsonObject advancement = json(DATA.resolve("advancements/recipes/misc/resonating_anchor.json"));
        assertTrue(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .toString().contains("faded_pearl:resonating_anchor"));
    }

    @Test
    void pearlStepsHaveGuidanceAndTheFilledPearlUnlocksItsRecipe() throws IOException {
        JsonObject english = json(ASSETS.resolve("lang/en_us.json"));
        JsonObject polish = json(ASSETS.resolve("lang/pl_pl.json"));
        for (String suffix : new String[]{"empty_pearl.hint", "water_filled_pearl.hint", "pulsating_pearl.hint"}) {
            assertTrue(english.has("tooltip.faded_pearl." + suffix));
            assertTrue(polish.has("tooltip.faded_pearl." + suffix));
        }
        JsonObject advancement = json(DATA.resolve("advancements/recipes/misc/pulsating_pearl.json"));
        assertTrue(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .toString().contains("faded_pearl:pulsating_pearl"));
    }

    @Test
    void anchorExplainsHomeRecallAndDimensionLimitInBothLocales() throws IOException {
        JsonObject english = json(ASSETS.resolve("lang/en_us.json"));
        JsonObject polish = json(ASSETS.resolve("lang/pl_pl.json"));
        for (String suffix : new String[]{"home", "recall", "dimension"}) {
            String key = "tooltip.faded_pearl.resonating_anchor." + suffix;
            assertTrue(english.has(key));
            assertTrue(polish.has(key));
        }
    }

    @Test
    void evasionPearlHasRecipeUnlockModelAndLocalizedGuidance() throws IOException {
        JsonObject recipe = json(DATA.resolve("recipes/escape_pearl.json"));
        assertEquals("faded_pearl:escape_pearl",
                recipe.getAsJsonObject("result").get("item").getAsString());
        String pattern = recipe.getAsJsonArray("pattern").toString();
        assertEquals("[\"MEM\",\"APA\",\"MCM\"]", pattern);
        assertEquals("minecraft:echo_shard", recipe.getAsJsonObject("key")
                .getAsJsonObject("E").get("item").getAsString());
        assertEquals("minecraft:crying_obsidian", recipe.getAsJsonObject("key")
                .getAsJsonObject("C").get("item").getAsString());
        JsonObject advancement = json(DATA.resolve("advancements/recipes/misc/escape_pearl.json"));
        assertTrue(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .toString().contains("faded_pearl:escape_pearl"));
        assertTrue(Files.exists(ASSETS.resolve("models/item/escape_pearl.json")));
        var texture = ImageIO.read(ASSETS.resolve("textures/item/escape_pearl.png").toFile());
        assertEquals(32, texture.getWidth());
        assertEquals(32, texture.getHeight());
        for (Path locale : new Path[]{ASSETS.resolve("lang/en_us.json"), ASSETS.resolve("lang/pl_pl.json")}) {
            JsonObject language = json(locale);
            assertTrue(language.has("item.faded_pearl.escape_pearl"));
            assertTrue(language.has("tooltip.faded_pearl.escape_pearl"));
            assertTrue(language.has("tooltip.faded_pearl.escape_pearl.use"));
            assertTrue(language.has("message.faded_pearl.escape_pearl.armed"));
            assertTrue(language.has("message.faded_pearl.escape_pearl.already_armed"));
            assertTrue(language.has("message.faded_pearl.escape_pearl.triggered"));
        }
    }

    @Test
    void healingPearlProgressionUsesDistinctTransparent32PixelTextures() throws IOException {
        Path[] textures = {
                ASSETS.resolve("textures/item/empty_pearl.png"),
                ASSETS.resolve("textures/item/water_filled_pearl.png"),
                ASSETS.resolve("textures/item/pulsating_pearl.png")
        };
        for (Path path : textures) {
            var image = ImageIO.read(path.toFile());
            assertEquals(32, image.getWidth());
            assertEquals(32, image.getHeight());
            boolean transparent = false;
            boolean visible = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    transparent |= alpha == 0;
                    visible |= alpha > 0;
                }
            }
            assertTrue(transparent);
            assertTrue(visible);
        }
        assertTrue(Files.mismatch(textures[0], textures[1]) >= 0);
        assertTrue(Files.mismatch(textures[1], textures[2]) >= 0);
        assertTrue(Files.mismatch(textures[0], textures[2]) >= 0);
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
