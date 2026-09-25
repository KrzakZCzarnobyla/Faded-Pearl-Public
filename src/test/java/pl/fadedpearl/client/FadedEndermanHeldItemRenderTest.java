package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadedEndermanHeldItemRenderTest {
    private static final Path RENDERER = Path.of(
            "src/main/java/pl/fadedpearl/client/FadedEndermanGeoRenderer.java");

    @Test
    void heldItemUsesGeckoLibBoneAttachmentLayer() throws Exception {
        String source = Files.readString(RENDERER);

        assertTrue(source.contains("new BlockAndItemGeoLayer<FadedEnderman>(this)"));
        assertTrue(source.contains("protected ItemStack getStackForBone(GeoBone bone, FadedEnderman enderman)"));
        assertTrue(source.contains("\"right_hand\".equals(bone.getName())"));
        assertTrue(source.contains("super.renderStackForBone(poseStack, bone, stack, enderman, buffers,"));
        assertFalse(source.contains("new GeoRenderLayer<FadedEnderman>(this)"));
    }

    @Test
    void heldItemKeepsExactSyncedStackAndRightHandTransform() throws Exception {
        String source = Files.readString(RENDERER);

        assertTrue(source.contains("ItemStack stack = enderman.getCuriosityDisplayStack();"));
        assertTrue(source.contains("return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;"));
        assertTrue(source.contains("poseStack.scale(0.65F, 0.65F, 0.65F);"));
    }
}
