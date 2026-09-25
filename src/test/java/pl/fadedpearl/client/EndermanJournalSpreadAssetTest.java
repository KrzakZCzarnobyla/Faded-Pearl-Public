package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class EndermanJournalSpreadAssetTest {
    private static final Path TEXTURE = Path.of(
            "src/main/resources/assets/faded_pearl/textures/gui/enderman_journal_spread.png");

    @Test
    void spreadHasTwoReadablePagesAndRealTransparency() throws Exception {
        BufferedImage spread = ImageIO.read(TEXTURE.toFile());
        assertNotNull(spread);
        assertEquals(292, spread.getWidth());
        assertEquals(180, spread.getHeight());
        assertEquals(0, spread.getRGB(0, 0) >>> 24);
        assertEquals(0, spread.getRGB(291, 179) >>> 24);
        assertEquals(255, spread.getRGB(20, 20) >>> 24);
        assertEquals(255, spread.getRGB(160, 20) >>> 24);
        assertTrue((spread.getRGB(20, 20) >> 16 & 0xFF) > 200);
        assertTrue((spread.getRGB(160, 20) >> 16 & 0xFF) > 200);
    }

    @Test
    void screenDrawsTheWholeAssetWithoutMirroredGeometry() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/pl/fadedpearl/client/EndermanJournalScreen.java"));
        assertTrue(screen.contains("graphics.blit(BOOK_SPREAD_TEXTURE, bookLeft, bookTop"));
        assertFalse(screen.contains("graphics.pose().scale(-1.0F"));
    }
}
