package pl.fadedpearl.release;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseArtifactContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/faded_pearl");
    private static final Set<String> REQUIRED_RUNTIME_DEPENDENCIES =
            Set.of("forge", "minecraft", "geckolib", "smartbrainlib");
    private static final Pattern SEMANTIC_VERSION = Pattern.compile(
            "\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?");

    @Test
    void releaseMetadataUsesOneVersionAndDependencyContract() throws Exception {
        Properties properties = properties();
        for (String key : List.of(
                "minecraft_version", "forge_version", "loader_version_range",
                "minecraft_version_range", "forge_version_range", "geckolib_version",
                "geckolib_version_range", "smartbrainlib_version",
                "smartbrainlib_version_range", "smartbrainlib_curse_file_id",
                "mod_id", "mod_name", "mod_version", "mod_group_id")) {
            assertFalse(required(properties, key).isBlank(), key + " must not be blank");
        }
        assertTrue(SEMANTIC_VERSION.matcher(required(properties, "mod_version")).matches(),
                "mod_version must be a SemVer-compatible release identifier");

        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("version = mod_version"));
        assertTrue(build.contains("base { archivesName = mod_id }"));
        assertTrue(build.contains("$" + "{geckolib_version}"));
        assertTrue(build.contains("$" + "{smartbrainlib_curse_file_id}"));
        assertTrue(build.contains("dependsOn 'reobfJar'"),
                "artifact contract must inspect the reobfuscated JAR");

        String sourceToml = Files.readString(RESOURCES.resolve("META-INF/mods.toml"));
        for (String placeholder : List.of(
                "loader_version_range", "mod_id", "mod_name", "mod_version",
                "forge_version_range", "minecraft_version_range",
                "geckolib_version_range", "smartbrainlib_version_range")) {
            assertTrue(sourceToml.contains("$" + "{" + placeholder + "}"),
                    "mods.toml must use a placeholder for " + placeholder);
        }

        String expandedToml = expand(sourceToml, properties);
        assertEquals(required(properties, "mod_id"), quotedValue(modBlock(expandedToml), "modId"));
        assertEquals(required(properties, "mod_name"), quotedValue(modBlock(expandedToml), "displayName"));
        assertEquals(required(properties, "mod_version"), quotedValue(modBlock(expandedToml), "version"));
        assertEquals(required(properties, "forge_version"),
                lowerBound(required(properties, "forge_version_range")));
        assertEquals(required(properties, "minecraft_version"),
                lowerBound(required(properties, "minecraft_version_range")));
        assertEquals(required(properties, "geckolib_version"),
                lowerBound(required(properties, "geckolib_version_range")));
        assertEquals(required(properties, "smartbrainlib_version"),
                lowerBound(required(properties, "smartbrainlib_version_range")));
        assertEquals(required(properties, "forge_version").split("\\.")[0],
                lowerBound(required(properties, "loader_version_range")));

        String mainClass = Files.readString(Path.of("src/main/java/pl/fadedpearl/FadedPearl.java"));
        assertTrue(mainClass.contains("MOD_ID = \"" + required(properties, "mod_id") + "\""),
                "Java mod id must match gradle.properties");
    }

    @Test
    void metadataCannotMakeAnOptionalIntegrationMandatory() throws Exception {
        Properties properties = properties();
        String expandedToml = expand(
                Files.readString(RESOURCES.resolve("META-INF/mods.toml")), properties);
        Map<String, Dependency> dependencies = dependencies(expandedToml, required(properties, "mod_id"));

        assertTrue(dependencies.keySet().containsAll(REQUIRED_RUNTIME_DEPENDENCIES),
                "required Forge/runtime libraries must be declared");
        for (String modId : REQUIRED_RUNTIME_DEPENDENCIES) {
            assertTrue(dependencies.get(modId).mandatory(),
                    modId + " must remain a mandatory runtime dependency");
        }
        for (Dependency dependency : dependencies.values()) {
            if (dependency.mandatory()) {
                assertTrue(REQUIRED_RUNTIME_DEPENDENCIES.contains(dependency.modId()),
                        dependency.modId() + " is an optional integration and cannot be mandatory");
            }
        }
        assertEquals(required(properties, "forge_version_range"), dependencies.get("forge").versionRange());
        assertEquals(required(properties, "minecraft_version_range"),
                dependencies.get("minecraft").versionRange());
        assertEquals(required(properties, "geckolib_version_range"),
                dependencies.get("geckolib").versionRange());
        assertEquals(required(properties, "smartbrainlib_version_range"),
                dependencies.get("smartbrainlib").versionRange());
    }

    @Test
    void requiredResourcesAndTheirLocalReferencesExist() throws Exception {
        List<String> required = new ArrayList<>(List.of(
                "META-INF/mods.toml",
                "pack.mcmeta",
                "assets/faded_pearl/lang/en_us.json",
                "assets/faded_pearl/lang/pl_pl.json",
                "assets/faded_pearl/geo/faded_enderman.geo.json",
                "assets/faded_pearl/animations/faded_enderman.animation.json",
                "assets/faded_pearl/sounds.json",
                "assets/faded_pearl/blockstates/resonating_anchor.json",
                "assets/faded_pearl/models/block/resonating_anchor.json",
                "assets/faded_pearl/models/item/empty_pearl.json",
                "assets/faded_pearl/models/item/water_filled_pearl.json",
                "assets/faded_pearl/models/item/pulsating_pearl.json",
                "assets/faded_pearl/models/item/escape_pearl.json",
                "assets/faded_pearl/models/item/enderman_tear.json",
                "assets/faded_pearl/models/item/enderman_journal.json",
                "assets/faded_pearl/models/item/resonating_anchor.json",
                "assets/faded_pearl/textures/entity/faded_enderman_final_wounded.png",
                "assets/faded_pearl/textures/entity/faded_enderman_glowmask.png",
                "assets/faded_pearl/textures/gui/enderman_journal_spread.png",
                "data/faded_pearl/recipes/pulsating_pearl.json",
                "data/faded_pearl/recipes/resonating_anchor.json",
                "data/faded_pearl/recipes/escape_pearl.json",
                "data/faded_pearl/loot_tables/entities/faded_enderman.json",
                "data/faded_pearl/loot_tables/blocks/resonating_anchor.json"));
        for (int trust = 0; trust <= 7; trust++) {
            required.add("assets/faded_pearl/textures/entity/faded_enderman_final_healed_trust_"
                    + trust + ".png");
        }
        for (String relative : required) {
            Path path = RESOURCES.resolve(relative);
            assertTrue(Files.isRegularFile(path), "missing required resource: " + relative);
            assertTrue(Files.size(path) > 0, "empty required resource: " + relative);
        }

        try (Stream<Path> files = Files.walk(RESOURCES)) {
            for (Path json : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (var reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
                    assertNotNull(JsonParser.parseReader(reader), "invalid JSON: " + json);
                }
            }
        }
        assertModelReferencesResolve();
        assertSoundReferencesResolve();

        JsonObject pack = JsonParser.parseString(Files.readString(RESOURCES.resolve("pack.mcmeta")))
                .getAsJsonObject();
        assertEquals("1.20.1", required(properties(), "minecraft_version"),
                "update the pack-format contract when changing Minecraft");
        assertEquals(15, pack.getAsJsonObject("pack").get("pack_format").getAsInt(),
                "Minecraft 1.20.1 requires resource pack format 15");
    }

    @Test
    void reobfuscatedJarIsACompleteReleaseArtifact() throws Exception {
        Properties properties = properties();
        String configuredPath = System.getProperty("fadedPearl.releaseArtifact");
        assertNotNull(configuredPath, "Gradle must provide the release artifact path");
        Path artifact = Path.of(configuredPath);
        assertTrue(Files.isRegularFile(artifact), "missing release artifact: " + artifact);
        assertEquals(required(properties, "mod_id") + "-" + required(properties, "mod_version") + ".jar",
                artifact.getFileName().toString());

        try (JarFile jar = new JarFile(artifact.toFile())) {
            Set<String> entries = new HashSet<>();
            Enumeration<JarEntry> enumeration = jar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                assertTrue(entries.add(entry.getName()), "duplicate JAR entry: " + entry.getName());
                assertFalse(entry.getName().startsWith("/") || entry.getName().contains("..")
                                || entry.getName().contains("\\"),
                        "unsafe JAR entry: " + entry.getName());
                if (!entry.isDirectory()) {
                    assertTrue(entry.getSize() > 0, "empty JAR entry: " + entry.getName());
                }
            }

            assertTrue(entries.contains("pl/fadedpearl/FadedPearl.class"), "missing mod entrypoint class");
            assertFalse(entries.stream().anyMatch(name -> name.startsWith("pl/fadedpearl/")
                            && name.endsWith("Test.class")),
                    "test classes must not be packaged");
            assertFalse(entries.stream().anyMatch(name -> name.endsWith(".java")),
                    "source files must not be packaged");

            try (Stream<Path> sourceResources = Files.walk(RESOURCES)) {
                for (Path resource : sourceResources.filter(Files::isRegularFile).toList()) {
                    String relative = RESOURCES.relativize(resource).toString().replace('\\', '/');
                    assertTrue(entries.contains(relative), "resource missing from JAR: " + relative);
                }
            }

            String artifactToml = entryText(jar, "META-INF/mods.toml");
            assertFalse(artifactToml.contains("$" + "{"), "unexpanded placeholder in JAR mods.toml");
            String artifactPack = entryText(jar, "pack.mcmeta");
            assertFalse(artifactPack.contains("$" + "{"), "unexpanded placeholder in JAR pack.mcmeta");
            assertEquals(required(properties, "mod_version"), quotedValue(modBlock(artifactToml), "version"));
            assertEquals(required(properties, "mod_id"), quotedValue(modBlock(artifactToml), "modId"));

            Attributes manifest = jar.getManifest().getMainAttributes();
            assertEquals(required(properties, "mod_name"), manifest.getValue("Implementation-Title"));
            assertEquals(required(properties, "mod_version"), manifest.getValue("Implementation-Version"));
            assertEquals("Faded Pearl", manifest.getValue("Implementation-Vendor"));
        }
    }

    private static void assertModelReferencesResolve() throws IOException {
        Path models = ASSETS.resolve("models");
        try (Stream<Path> files = Files.walk(models)) {
            for (Path model : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
                if (json.has("parent")) {
                    assertLocalResource(json.get("parent").getAsString(), "models", ".json", model);
                }
                if (json.has("textures")) {
                    for (var texture : json.getAsJsonObject("textures").entrySet()) {
                        String reference = texture.getValue().getAsString();
                        if (!reference.startsWith("#")) {
                            assertLocalResource(reference, "textures", ".png", model);
                        }
                    }
                }
            }
        }
        Path blockstatePath = ASSETS.resolve("blockstates/resonating_anchor.json");
        JsonObject blockstate = JsonParser.parseString(Files.readString(blockstatePath)).getAsJsonObject();
        for (var variant : blockstate.getAsJsonObject("variants").entrySet()) {
            assertLocalResource(variant.getValue().getAsJsonObject().get("model").getAsString(),
                    "models", ".json", blockstatePath);
        }
    }

    private static void assertSoundReferencesResolve() throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json")))
                .getAsJsonObject();
        for (var event : sounds.entrySet()) {
            for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String reference = sound.isJsonObject()
                        ? sound.getAsJsonObject().get("name").getAsString()
                        : sound.getAsString();
                assertLocalResource(reference, "sounds", ".ogg", ASSETS.resolve("sounds.json"));
            }
        }
    }

    private static void assertLocalResource(
            String reference, String folder, String suffix, Path referringFile) {
        String namespace = "minecraft";
        String path = reference;
        int separator = reference.indexOf(':');
        if (separator >= 0) {
            namespace = reference.substring(0, separator);
            path = reference.substring(separator + 1);
        }
        if (!"faded_pearl".equals(namespace)) {
            return;
        }
        Path target = ASSETS.resolve(folder).resolve(path + suffix);
        assertTrue(Files.isRegularFile(target),
                referringFile + " references missing local resource " + reference);
    }

    private static Properties properties() throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        assertNotNull(value, "missing gradle property: " + key);
        return value.trim();
    }

    private static String expand(String template, Properties properties) {
        String expanded = template;
        for (String key : properties.stringPropertyNames()) {
            expanded = expanded.replace("$" + "{" + key + "}", properties.getProperty(key));
        }
        assertFalse(expanded.contains("$" + "{"), "unexpanded metadata placeholder: " + expanded);
        return expanded;
    }

    private static String modBlock(String toml) {
        int start = toml.indexOf("[[mods]]");
        int end = toml.indexOf("[[dependencies.", start);
        assertTrue(start >= 0 && end > start, "mods.toml must contain one mod block before dependencies");
        return toml.substring(start, end);
    }

    private static String quotedValue(String block, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]+)\"")
                .matcher(block);
        assertTrue(matcher.find(), "missing quoted TOML key: " + key);
        return matcher.group(1);
    }

    private static boolean booleanValue(String block, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*(true|false)\\s*$")
                .matcher(block);
        assertTrue(matcher.find(), "missing boolean TOML key: " + key);
        return Boolean.parseBoolean(matcher.group(1));
    }

    private static Map<String, Dependency> dependencies(String toml, String modId) {
        Pattern blocks = Pattern.compile("(?s)\\[\\[dependencies\\." + Pattern.quote(modId)
                + "]]\\s*(.*?)(?=\\R\\[\\[|\\z)");
        Matcher matcher = blocks.matcher(toml);
        Map<String, Dependency> result = new HashMap<>();
        while (matcher.find()) {
            String block = matcher.group(1);
            Dependency dependency = new Dependency(
                    quotedValue(block, "modId"),
                    booleanValue(block, "mandatory"),
                    quotedValue(block, "versionRange"));
            assertFalse(result.containsKey(dependency.modId()),
                    "duplicate dependency metadata: " + dependency.modId());
            result.put(dependency.modId(), dependency);
        }
        assertFalse(result.isEmpty(), "no dependency blocks parsed from mods.toml");
        return result;
    }

    private static String lowerBound(String range) {
        assertTrue(range.startsWith("[") || range.startsWith("("), "invalid version range: " + range);
        int comma = range.indexOf(',');
        assertTrue(comma > 1, "version range needs a lower bound: " + range);
        return range.substring(1, comma);
    }

    private static String entryText(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        assertNotNull(entry, "missing JAR entry: " + name);
        try (InputStream stream = jar.getInputStream(entry)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Dependency(String modId, boolean mandatory, String versionRange) {
    }
}
