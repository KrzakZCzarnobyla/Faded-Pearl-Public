package pl.fadedpearl.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResonatingAnchorVisualContractTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/faded_pearl");
    private static final Path INACTIVE_MODEL = ASSETS.resolve("models/block/resonating_anchor.json");
    private static final Path ACTIVE_MODEL = ASSETS.resolve("models/block/resonating_anchor_active.json");

    @Test
    void blockstateUsesDistinctInactiveAndActiveModels() throws IOException {
        JsonObject variants = json(ASSETS.resolve("blockstates/resonating_anchor.json"))
                .getAsJsonObject("variants");
        String inactive = variants.getAsJsonObject("active=false").get("model").getAsString();
        String active = variants.getAsJsonObject("active=true").get("model").getAsString();

        assertEquals("faded_pearl:block/resonating_anchor", inactive);
        assertEquals("faded_pearl:block/resonating_anchor_active", active);
        assertNotEquals(inactive, active);
    }

    @Test
    void bothModelsArePedestalsWithFourPillarsAndNoFullCube() throws IOException {
        for (Path model : new Path[]{INACTIVE_MODEL, ACTIVE_MODEL}) {
            JsonArray elements = json(model).getAsJsonArray("elements");
            assertEquals(12, elements.size(), model.toString());
            for (String pillar : new String[]{
                    "north_west_pillar", "north_east_pillar",
                    "south_west_pillar", "south_east_pillar"}) {
                assertTrue(hasNamedElement(elements, pillar), model + " missing " + pillar);
            }
            assertFalse(elements.asList().stream().anyMatch(element -> {
                JsonObject object = element.getAsJsonObject();
                return coordinates(object.getAsJsonArray("from"), 0, 0, 0)
                        && coordinates(object.getAsJsonArray("to"), 16, 16, 16);
            }), model + " must not contain a full cube");
        }
    }

    @Test
    void activePearlIsRaisedAndItemUsesTheActivePresentation() throws IOException {
        JsonObject resting = namedElement(json(INACTIVE_MODEL).getAsJsonArray("elements"), "resting_pearl");
        JsonObject raised = namedElement(json(ACTIVE_MODEL).getAsJsonArray("elements"), "raised_pearl");

        assertEquals(4, resting.getAsJsonArray("from").get(1).getAsInt());
        assertEquals(8, raised.getAsJsonArray("from").get(1).getAsInt());
        assertEquals(14, raised.getAsJsonArray("to").get(1).getAsInt());
        assertFalse(raised.get("shade").getAsBoolean());
        assertEquals("faded_pearl:block/resonating_anchor_active",
                json(ASSETS.resolve("models/item/resonating_anchor.json")).get("parent").getAsString());
    }

    @Test
    void blockUsesStateDependentNonFullShapesAndKeepsInteractionCode() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/block/ResonatingAnchorBlock.java"));

        assertTrue(source.contains("private static final VoxelShape PEDESTAL"));
        assertTrue(source.contains("private static final VoxelShape INACTIVE_SHAPE"));
        assertTrue(source.contains("private static final VoxelShape ACTIVE_SHAPE"));
        assertTrue(source.contains("public VoxelShape getShape("));
        assertTrue(source.contains("public VoxelShape getCollisionShape("));
        assertTrue(source.contains("state.getValue(ACTIVE) ? ACTIVE_SHAPE : INACTIVE_SHAPE"));
        assertTrue(source.contains("AnchorRecallService.request(server, pos, (ServerPlayer) player)"));
        assertTrue(source.contains("data.setHome(id, server.dimension(), pos)"));
    }

    private static boolean hasNamedElement(JsonArray elements, String name) {
        return elements.asList().stream()
                .map(element -> element.getAsJsonObject())
                .anyMatch(element -> name.equals(element.get("name").getAsString()));
    }

    private static JsonObject namedElement(JsonArray elements, String name) {
        return elements.asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(element -> name.equals(element.get("name").getAsString()))
                .findFirst()
                .orElseThrow();
    }

    private static boolean coordinates(JsonArray array, int x, int y, int z) {
        return array.get(0).getAsInt() == x && array.get(1).getAsInt() == y
                && array.get(2).getAsInt() == z;
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
