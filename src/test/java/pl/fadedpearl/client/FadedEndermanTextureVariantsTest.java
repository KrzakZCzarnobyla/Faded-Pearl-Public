package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FadedEndermanTextureVariantsTest {
    @Test
    void selectsEveryExistingColorVariant() {
        Map<Integer, String> variants = Map.ofEntries(
                Map.entry(0x466BDE, "466bde"),
                Map.entry(0x4B3158, "4b3158"),
                Map.entry(0x4EBFFF, "4ebfff"),
                Map.entry(0xB870D6, "b870d6"),
                Map.entry(0xD83B3B, "d83b3b"),
                Map.entry(0xE33A4E, "e33a4e"),
                Map.entry(0xE8EEF2, "e8eef2"),
                Map.entry(0xF28C28, "f28c28"),
                Map.entry(0xF28FB8, "f28fb8"),
                Map.entry(0xF4F0EA, "f4f0ea"),
                Map.entry(0xF5F5F5, "f5f5f5"),
                Map.entry(0xFF8A26, "ff8a26"),
                Map.entry(0xFFD83D, "ffd83d"),
                Map.entry(0xFFF4B0, "fff4b0")
        );

        variants.forEach((color, suffix) -> assertEquals(
                "healed_enderman_" + suffix + ".png",
                FadedEndermanTextureVariants.healedTextureName(color)
        ));
    }

    @Test
    void fallsBackToBaseTextureForUnknownColor() {
        assertEquals("healed_enderman.png", FadedEndermanTextureVariants.healedTextureName(0xFF00FF));
    }

    @Test
    void ignoresAlphaBitsWhenSelectingKnownVariant() {
        assertEquals("healed_enderman_f28fb8.png", FadedEndermanTextureVariants.healedTextureName(0x7FF28FB8));
    }
}
