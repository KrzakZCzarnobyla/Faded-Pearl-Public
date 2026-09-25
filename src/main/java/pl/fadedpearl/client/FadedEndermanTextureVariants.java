package pl.fadedpearl.client;

import java.util.Set;

final class FadedEndermanTextureVariants {
    private static final Set<Integer> SUPPORTED_COLORS = Set.of(
            0x466BDE,
            0x4B3158,
            0x4EBFFF,
            0xB870D6,
            0xD83B3B,
            0xE33A4E,
            0xE8EEF2,
            0xF28C28,
            0xF28FB8,
            0xF4F0EA,
            0xF5F5F5,
            0xFF8A26,
            0xFFD83D,
            0xFFF4B0
    );

    private FadedEndermanTextureVariants() {
    }

    static String healedTextureName(int color) {
        int rgb = color & 0xFFFFFF;
        if (!SUPPORTED_COLORS.contains(rgb)) {
            return "healed_enderman.png";
        }

        return "healed_enderman_" + String.format("%06x", rgb) + ".png";
    }
}
