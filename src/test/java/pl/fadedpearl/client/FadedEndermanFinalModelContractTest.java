package pl.fadedpearl.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadedEndermanFinalModelContractTest {
    private static final Path GEOMETRY = Path.of(
            "src/main/resources/assets/faded_pearl/geo/faded_enderman.geo.json");
    private static final Path MODEL = Path.of(
            "src/main/java/pl/fadedpearl/client/FadedEndermanGeoModel.java");
    private static final Path RENDERER = Path.of(
            "src/main/java/pl/fadedpearl/client/FadedEndermanGeoRenderer.java");

    @Test
    void exportedRigKeepsRuntimeBoneContract() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(GEOMETRY)).getAsJsonObject();
        JsonArray bones = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones");
        Set<String> names = new HashSet<>();
        bones.forEach(value -> names.add(value.getAsJsonObject().get("name").getAsString()));

        assertTrue(names.containsAll(Set.of("root", "body", "head_tracking", "head",
                "right_arm", "right_hand", "left_arm", "left_forearm", "left_hand",
                "right_leg", "left_leg", "pearl", "eyes", "jaw", "wound")));
        assertFalse(names.contains("heart_core"));
        assertFalse(names.contains("right_forearm"));
        assertFalse(names.contains("right_thigh"));
        assertFalse(names.contains("right_shin"));
        assertEquals(names.size(), bones.size(), "runtime bone names must be unique");

        JsonObject animations = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/faded_pearl/animations/faded_enderman.animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        Set<String> animatedBones = new HashSet<>();
        animations.entrySet().forEach(animation -> {
            JsonObject clip = animation.getValue().getAsJsonObject();
            if (clip.has("bones")) clip.getAsJsonObject("bones").keySet().forEach(animatedBones::add);
        });
        assertTrue(names.containsAll(animatedBones), "every animated bone must exist in the final rig");

        String entitySource = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        Matcher matcher = Pattern.compile("animation\\.faded_enderman\\.[a-z_]+").matcher(entitySource);
        Set<String> requiredClips = new HashSet<>();
        while (matcher.find()) requiredClips.add(matcher.group());
        assertTrue(animations.keySet().containsAll(requiredClips),
                "every animation requested by FadedEnderman must exist");
    }

    @Test
    void exportedGeometryUsesOfficialBedrockAxesAndVisibleBounds() throws Exception {
        JsonObject definition = JsonParser.parseString(Files.readString(GEOMETRY)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonArray bones = definition.getAsJsonArray("bones");
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (var boneValue : bones) {
            JsonObject bone = boneValue.getAsJsonObject();
            if (!bone.has("cubes")) continue;
            for (var cubeValue : bone.getAsJsonArray("cubes")) {
                JsonObject cube = cubeValue.getAsJsonObject();
                JsonArray origin = cube.getAsJsonArray("origin");
                JsonArray size = cube.getAsJsonArray("size");
                minX = Math.min(minX, origin.get(0).getAsDouble());
                minY = Math.min(minY, origin.get(1).getAsDouble());
                minZ = Math.min(minZ, origin.get(2).getAsDouble());
                maxX = Math.max(maxX, origin.get(0).getAsDouble() + size.get(0).getAsDouble());
                maxY = Math.max(maxY, origin.get(1).getAsDouble() + size.get(1).getAsDouble());
                maxZ = Math.max(maxZ, origin.get(2).getAsDouble() + size.get(2).getAsDouble());
            }
        }
        assertTrue(minX >= -11 && maxX <= 10, "model must remain centered on X");
        assertTrue(minY >= 0 && maxY > 55 && maxY < 57, "feet/head must occupy the visible Y range");
        assertTrue(minZ >= -2 && maxZ <= 9, "model must remain centered on Z");

        JsonObject pearl = null;
        for (var boneValue : bones) {
            if (boneValue.getAsJsonObject().get("name").getAsString().equals("pearl")) {
                pearl = boneValue.getAsJsonObject();
                break;
            }
        }
        JsonObject rotatedCube = pearl.getAsJsonArray("cubes").get(1).getAsJsonObject();
        assertEquals(-2.25, rotatedCube.getAsJsonArray("origin").get(0).getAsDouble(), 0.00001);
        assertEquals(36.46299, rotatedCube.getAsJsonArray("origin").get(1).getAsDouble(), 0.00001);
        assertEquals(-90.0, rotatedCube.getAsJsonArray("rotation").get(0).getAsDouble(), 0.00001);
    }

    @Test
    void exportedAnimationsKeepAuthoredGeckoLibEasing() throws Exception {
        String animations = Files.readString(Path.of(
                "src/main/resources/assets/faded_pearl/animations/faded_enderman.animation.json"));

        assertTrue(animations.contains("\"easing\": \"easeInOutSine\""));
        assertTrue(animations.contains("\"easing\": \"easeInQuad\""));
        assertTrue(animations.contains("\"easing\": \"easeOutQuad\""));
    }

    @Test
    void fingerTracksStayOnTheValidatedAttachmentSet() throws Exception {
        JsonObject animations = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/faded_pearl/animations/faded_enderman.animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        Set<String> fingerBones = Set.of("bone10", "bone11", "bone12", "bone13");
        Set<String> clipsWithFingerMotion = Set.of(
                "animation.faded_enderman.held_item",
                "animation.faded_enderman.item_inspect",
                "animation.faded_enderman.item_point",
                "animation.faded_enderman.hug",
                "animation.faded_enderman.carry_idle",
                "animation.faded_enderman.embrace_ready",
                "animation.faded_enderman.snow_catch",
                "animation.faded_enderman.pick_up_player",
                "animation.faded_enderman.put_down_player",
                "animation.faded_enderman.carry_walk");
        int animatedFingerBones = 0;
        int fingerKeyframes = 0;

        for (var animation : animations.entrySet()) {
            JsonObject clip = animation.getValue().getAsJsonObject();
            if (!clip.has("bones")) continue;
            JsonObject bones = clip.getAsJsonObject("bones");
            for (String fingerBone : fingerBones) {
                if (!bones.has(fingerBone)) continue;
                assertTrue(clipsWithFingerMotion.contains(animation.getKey()),
                        "unvalidated finger motion in " + animation.getKey() + "/" + fingerBone);
                animatedFingerBones++;
                JsonObject channels = bones.getAsJsonObject(fingerBone);
                for (String channel : Set.of("position", "rotation", "scale")) {
                    if (channels.has(channel)) {
                        fingerKeyframes += channels.getAsJsonObject(channel).size();
                    }
                }
            }
        }

        assertEquals(40, animatedFingerBones,
                "all four fingers must use the validated tracks in exactly ten clips");
        assertEquals(1484, fingerKeyframes,
                "validated finger attachment tracks must not be replaced by editor offsets");
    }

    @Test
    void articulatedBonesUseConnectedModelSpaceJointPivots() throws Exception {
        JsonArray bones = JsonParser.parseString(Files.readString(GEOMETRY)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones");
        Map<String, JsonObject> byName = new java.util.HashMap<>();
        bones.forEach(value -> byName.put(
                value.getAsJsonObject().get("name").getAsString(), value.getAsJsonObject()));

        assertParentAndPivot(byName, "head_tracking", "body", -1.0, 46.35, 3.5);
        assertParentAndPivot(byName, "head", "head_tracking", -1.0, 46.35, 3.5);
        assertParentAndPivot(byName, "left_arm", "body", 6.5, 45.75, 3.0);
        assertParentAndPivot(byName, "left_hand", "left_forearm", 6.75, 16.75, 3.0);
        assertParentAndPivot(byName, "right_arm", "body", -8.0, 45.75, 3.0);
        assertParentAndPivot(byName, "bone14", "right_arm", -8.5, 31.75, 2.25);
        assertParentAndPivot(byName, "right_hand", "bone14", -8.75, 16.75, 3.0);
        assertParentAndPivot(byName, "left_leg", "body", 2.0, 29.0, 3.0);
        assertParentAndPivot(byName, "right_leg", "body", -4.5, 29.0, 3.0);
        assertParentAndPivot(byName, "jaw", "head", -1.0, 46.35, -0.5);
    }

    private static void assertParentAndPivot(Map<String, JsonObject> bones, String name,
                                             String parent, double x, double y, double z) {
        JsonObject bone = bones.get(name);
        assertEquals(parent, bone.get("parent").getAsString(), name + " parent");
        JsonArray pivot = bone.getAsJsonArray("pivot");
        assertEquals(x, pivot.get(0).getAsDouble(), 0.00001, name + " pivot X");
        assertEquals(y, pivot.get(1).getAsDouble(), 0.00001, name + " pivot Y");
        assertEquals(z, pivot.get(2).getAsDouble(), 0.00001, name + " pivot Z");
    }

    @Test
    void healedTextureDropsLayerThreeAndTintIsBoneScoped() throws Exception {
        String model = Files.readString(MODEL);
        String renderer = Files.readString(RENDERER);

        assertTrue(model.contains("faded_enderman_final_wounded.png"));
        assertTrue(model.contains("faded_enderman_final_healed_trust_0.png"));
        assertTrue(model.contains("FadedTrustManager.stageIndex(entity.getTrust())"));
        assertTrue(renderer.contains("enderman.getHealingColor()"));
        assertTrue(renderer.contains("\"pearl\".equals(bone.getName()) || \"eyes\".equals(bone.getName())"));
        assertTrue(renderer.contains("packedLight = 0xF000F0;"));
        assertTrue(renderer.contains("red = green = blue = 1.0F;"));

        BufferedImage wounded = ImageIO.read(Path.of("src/main/resources/assets/faded_pearl/textures/entity",
                "faded_enderman_final_wounded.png").toFile());
        BufferedImage healed = ImageIO.read(Path.of("src/main/resources/assets/faded_pearl/textures/entity",
                "faded_enderman_final_healed.png").toFile());
        assertEquals(256, wounded.getWidth());
        assertEquals(256, wounded.getHeight());
        assertEquals(256, healed.getWidth());
        assertEquals(256, healed.getHeight());

        int changedPixels = 0;
        for (int y = 0; y < wounded.getHeight(); y++) {
            for (int x = 0; x < wounded.getWidth(); x++) {
                if (wounded.getRGB(x, y) != healed.getRGB(x, y)) changedPixels++;
            }
        }
        assertTrue(changedPixels > 0, "removing layer #3 must visibly change the healed texture");
    }
}
