package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class FadedEndermanTrustTextureTest {
    private static final Path DIRECTORY = Path.of(
            "src/main/resources/assets/faded_pearl/textures/entity");

    @Test
    void modelReferencesEveryTrustVariant() throws Exception {
        String model = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/FadedEndermanGeoModel.java"));
        for (int stage = 0; stage < 8; stage++) {
            assertTrue(model.contains("faded_enderman_final_healed_trust_" + stage + ".png"));
        }
        assertTrue(model.contains("FadedTrustManager.stageIndex(entity.getTrust())"));
    }

    @Test
    void renderTrustIsSyncedFromServerOnHealingChangesAndSaveLoad() throws Exception {
        String entity = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        assertTrue(entity.contains("entityData.define(SYNCED_TRUST, 0)"));
        assertTrue(entity.contains("level().isClientSide ? entityData.get(SYNCED_TRUST) : trust"));
        assertTrue(entity.contains("entityData.set(SYNCED_TRUST, trust)"));
        assertTrue(entity.contains("setTrustValue(FadedTrustManager.applyModifier(trust, appliedAmount))"));
        assertTrue(entity.contains("setTrustValue(state.trust())"));
        assertTrue(entity.contains("setTrustValue(0)"));
    }

    @Test
    void allEightVariantsPreserveDimensionsAlphaAndGainBlueLight() throws Exception {
        BufferedImage base = ImageIO.read(DIRECTORY.resolve("faded_enderman_final_healed.png").toFile());
        long previousBlue = -1;
        long previousLuminance = -1;
        for (int stage = 0; stage < 8; stage++) {
            Path path = DIRECTORY.resolve("faded_enderman_final_healed_trust_" + stage + ".png");
            assertTrue(Files.isRegularFile(path), path.toString());
            BufferedImage image = ImageIO.read(path.toFile());
            assertEquals(base.getWidth(), image.getWidth());
            assertEquals(base.getHeight(), image.getHeight());

            long blue = 0;
            long luminance = 0;
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    int original = base.getRGB(x, y);
                    int variant = image.getRGB(x, y);
                    assertEquals(original >>> 24, variant >>> 24, "alpha at " + x + "," + y);
                    if ((variant >>> 24) == 0) continue;
                    int red = (variant >>> 16) & 255;
                    int green = (variant >>> 8) & 255;
                    int pixelBlue = variant & 255;
                    blue += pixelBlue;
                    luminance += red * 2126L + green * 7152L + pixelBlue * 722L;
                }
            }
            if (stage > 0) {
                assertTrue(blue > previousBlue, "blue must rise at stage " + stage);
                assertTrue(luminance > previousLuminance, "luminance must rise at stage " + stage);
            }
            previousBlue = blue;
            previousLuminance = luminance;
        }
    }

    @Test
    void stageZeroIsPixelIdenticalAndWarmDetailsRemainUntouched() throws Exception {
        BufferedImage base = ImageIO.read(DIRECTORY.resolve("faded_enderman_final_healed.png").toFile());
        BufferedImage zero = ImageIO.read(DIRECTORY.resolve("faded_enderman_final_healed_trust_0.png").toFile());
        BufferedImage seven = ImageIO.read(DIRECTORY.resolve("faded_enderman_final_healed_trust_7.png").toFile());
        int warmPixels = 0;
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int original = base.getRGB(x, y);
                assertEquals(original, zero.getRGB(x, y), "stage zero at " + x + "," + y);
                int red = (original >>> 16) & 255;
                int green = (original >>> 8) & 255;
                int blue = original & 255;
                if ((original >>> 24) != 0 && red > blue && red > green) {
                    assertEquals(original, seven.getRGB(x, y), "warm detail at " + x + "," + y);
                    warmPixels++;
                }
            }
        }
        assertTrue(warmPixels > 0);
    }
}
