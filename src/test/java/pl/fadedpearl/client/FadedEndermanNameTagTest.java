package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedEndermanNameTagTest {
    @Test
    void onlyOrdinaryNameTagReceivesExtraHeight() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/FadedEndermanGeoRenderer.java"));
        int methodStart = source.indexOf("protected void renderNameTag(");
        String method = source.substring(methodStart);
        assertTrue(source.contains("private static final double NAME_TAG_EXTRA_HEIGHT = 0.65D;"));
        String statusRender = "super.renderNameTag(enderman, status, poseStack, buffers, packedLight);";
        int firstStatus = method.indexOf(statusRender);
        int secondStatus = method.indexOf(statusRender, firstStatus + 1);
        int namePose = method.indexOf("poseStack.pushPose();");
        assertTrue(firstStatus >= 0 && secondStatus < 0 && namePose > firstStatus);
        assertTrue(!method.contains("status.faded_pearl.regenerating"));
        assertTrue(method.contains("poseStack.pushPose();"));
        assertTrue(method.contains("poseStack.translate(0.0D, NAME_TAG_EXTRA_HEIGHT, 0.0D);"));
        assertTrue(method.contains("super.renderNameTag(enderman, name, poseStack, buffers, packedLight);"));
        assertTrue(method.contains("poseStack.popPose();"));
    }
}
