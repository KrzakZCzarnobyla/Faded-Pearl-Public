package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EchoNeedleHudContractTest {
    @Test
    void polishedHudUsesTheExistingPearlFrameAndDistinctNeedleHalves() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/EchoNeedleHud.java"));
        assertAll(
                () -> assertTrue(source.contains("ModItems.EMPTY_PEARL")),
                () -> assertTrue(source.contains("graphics.renderItem(frameItem()")),
                () -> assertTrue(source.contains("NEEDLE_OUTLINE = 0xFF05070A")),
                () -> assertTrue(source.contains("graphics.fill(-2, -8, 2, -5, NEEDLE_OUTLINE)")),
                () -> assertTrue(source.contains("NEEDLE_TIP")),
                () -> assertTrue(source.contains("NEEDLE_CORE")),
                () -> assertTrue(source.contains("NEEDLE_TAIL")),
                () -> assertTrue(source.contains("renderHeightBadge")),
                () -> assertTrue(source.contains("EchoNeedleLocator.Height.ABOVE")),
                () -> assertTrue(source.contains("EchoNeedleLocator.Height.BELOW")));
    }
}
