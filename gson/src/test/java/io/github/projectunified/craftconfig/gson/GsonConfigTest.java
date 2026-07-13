package io.github.projectunified.craftconfig.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.projectunified.craftconfig.common.ConfigNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GsonConfigTest {

    @TempDir
    Path tempDir;

    private File getTestFile(String name) {
        return tempDir.resolve(name).toFile();
    }

    @Test
    void getOriginalReturnsJsonObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        Object original = config.getOriginal();
        assertInstanceOf(JsonObject.class, original);
    }

    @Test
    void setAndGetStringValue() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("key").set("hello");
        Object value = config.node("key").get();
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("hello", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void setAndGetNestedPath() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("a", "b", "c").set("val");
        Object value = config.node("a", "b", "c").get();
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("val", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void setNullRemovesEntry() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("key").set("hello");
        assertNotNull(config.node("key").get());
        config.node("key").set(null);
        assertNull(config.node("key").get());
    }

    @Test
    void containsTrueWhenSet() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("existing").set("value");
        assertTrue(config.node("existing").exists());
    }

    @Test
    void containsFalseForMissing() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertFalse(config.node("missing").exists());
    }

    @Test
    void removeDeletesEntry() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("toRemove").set("value");
        assertTrue(config.node("toRemove").exists());
        config.node("toRemove").remove();
        assertFalse(config.node("toRemove").exists());
    }

    @Test
    void clearRemovesAll() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("key1").set("val1");
        config.node("key2").set("val2");
        config.remove();
        assertFalse(config.node("key1").exists());
        assertFalse(config.node("key2").exists());
    }

    @Test
    void getNameReturnsFileName() {
        GsonConfig config = new GsonConfig(getTestFile("myfile.json"));
        assertEquals("myfile.json", config.getName());
    }

    @Test
    void getChildrenShallowReturnsDirectChildren() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("top1").set("v1");
        config.node("top2").set("v2");
        config.node("child", "value").set("nested");

        assertEquals(3, config.getChildren().size());
    }

    @Test
    void getChildrenDeepRecurses() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("top").set("v1");
        config.node("child", "value").set("nested");

        assertTrue(config.getChildren().size() >= 2);
    }

    @Test
    void setupCreatesFileIfNotExists() {
        File file = getTestFile("newfile.json");
        assertFalse(file.exists());
        GsonConfig config = new GsonConfig(file);
        config.setup();
        assertTrue(file.exists());
    }

    @Test
    void setupLoadsExistingContent() throws IOException {
        File file = getTestFile("existing.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"loaded\":\"true\"}");
        }

        GsonConfig config = new GsonConfig(file);
        config.setup();

        Object value = config.node("loaded").get();
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("true", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void saveWritesToFile() throws IOException {
        File file = getTestFile("savetest.json");
        GsonConfig config = new GsonConfig(file);
        config.node("value").set("saved");
        config.save();

        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("saved"));
        assertTrue(content.contains("value"));
    }

    @Test
    void reloadReadsLatestFromFile() throws IOException {
        File file = getTestFile("reloadtest.json");
        GsonConfig config = new GsonConfig(file);
        config.setup();

        // Write directly to file to simulate external change
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"reloaded\":\"yes\"}");
        }

        config.reload();
        Object value = config.node("reloaded").get();
        assertNotNull(value);
        assertEquals("yes", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void normalizeJsonElementReturnsNormalizedObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        JsonPrimitive primitive = new JsonPrimitive("hello");
        Object result = config.normalize(primitive);
        assertEquals("hello", result);
    }

    @Test
    void normalizeNonJsonElementReturnsSameObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        String input = "just a string";
        Object result = config.normalize(input);
        assertEquals(input, result);
    }

    @Test
    void isNormalizableJsonElementReturnsTrue() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertTrue(config.isNormalizable(new JsonObject()));
        assertTrue(config.isNormalizable(new JsonPrimitive("test")));
        assertTrue(config.isNormalizable(new JsonArray()));
    }

    @Test
    void isNormalizableStringReturnsFalse() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertFalse(config.isNormalizable("not a json element"));
        assertFalse(config.isNormalizable(42));
    }

    @Test
    void getWithDefaultReturnsDefaultWhenMissing() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        Object def = "defaultVal";
        Object result = config.node("missing").get(def);
        assertNotNull(result);
        assertEquals("defaultVal", result);
    }

    @Test
    void setupHandlesEmptyFile() {
        File file = getTestFile("empty.json");
        try {
            file.createNewFile();
        } catch (IOException e) {
            fail("Could not create test file");
        }
        GsonConfig config = new GsonConfig(file);
        assertDoesNotThrow(config::setup);
    }

    @Test
    void nestedNodeGetSet() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("a", "b", "c").set("deep");

        assertEquals("deep", config.node("a", "b", "c").get(String.class));
        assertNotNull(config.node("a").get());
        assertNotNull(config.node("a", "b").get());
    }

    @Test
    void nestedNodeGetChildren() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("parent", "child1").set("v1");
        config.node("parent", "child2").set("v2");

        ConfigNode parent = config.node("parent");
        assertTrue(parent.hasChild());
        Map<String, ConfigNode> children = parent.getChildren();
        assertEquals(2, children.size());
        assertTrue(children.containsKey("child1"));
        assertTrue(children.containsKey("child2"));
        assertEquals("v1", children.get("child1").get(String.class));
        assertEquals("v2", children.get("child2").get(String.class));
    }

    @Test
    void nodeReturnsSameForEmptyPath() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        ConfigNode sameNode = config.node();
        assertSame(config, sameNode);
    }

    @Test
    void childNodeReturnsSameForEmptyPath() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("key").set("value");
        ConfigNode node = config.node("key");
        assertSame(node, node.node());
    }

    @Test
    void nodeHasChildReturnsTrueForObjects() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("child", "key").set("value");
        assertTrue(config.node("child").hasChild());
        assertFalse(config.node("child", "key").hasChild());
    }

    @Test
    void chainedNodeNavigation() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("a", "b", "c", "d").set("deep");

        ConfigNode nodeA = config.node("a");
        ConfigNode nodeB = nodeA.node("b");
        ConfigNode nodeC = nodeB.node("c");
        ConfigNode nodeD = nodeC.node("d");

        assertEquals("deep", nodeD.get(String.class));
        assertEquals("deep", nodeC.node("d").get(String.class));
        assertEquals("deep", nodeB.node("c", "d").get(String.class));
        assertEquals("deep", nodeA.node("b", "c", "d").get(String.class));
    }

    @Test
    void nodeGetParentAndPath() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("level1", "level2", "level3").set("value");

        ConfigNode node = config.node("level1", "level2", "level3");
        assertArrayEquals(new String[]{"level1", "level2", "level3"}, node.getPath());
        assertNotNull(node.getParent());
    }

    @Test
    void nodeGetConfig() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("key").set("value");

        ConfigNode node = config.node("key");
        assertSame(config, node.getConfig());
    }

    @Test
    void nodeGetChildrenChained() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("server", "world", "spawn").set("0,0,0");
        config.node("server", "world", "name").set("world_nether");

        ConfigNode serverNode = config.node("server");
        ConfigNode worldNode = serverNode.node("world");

        assertTrue(worldNode.hasChild());
        Map<String, ConfigNode> worldChildren = worldNode.getChildren();
        assertEquals(2, worldChildren.size());
        assertEquals("0,0,0", worldChildren.get("spawn").get(String.class));
    }

    @Test
    void nodeSetAndGetChained() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));

        config.node("game", "mode").set("survival");
        config.node("game", "difficulty").set("hard");
        config.node("game", "rules", "doDaylightCycle").set(false);

        assertEquals("survival", config.node("game", "mode").get(String.class));
        assertEquals("hard", config.node("game", "difficulty").get(String.class));
        assertFalse(config.node("game", "rules", "doDaylightCycle").get(Boolean.class));
    }

    @Test
    void nodeRemoveChained() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("a", "b", "c").set("value");
        assertNotNull(config.node("a", "b", "c").get());

        config.node("a", "b").node("c").remove();
        assertNull(config.node("a", "b", "c").get());
    }

    @Test
    void nodeExistsChained() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("exists", "path").set("yes");

        assertTrue(config.node("exists", "path").exists());
        assertFalse(config.node("not", "exists").exists());
    }

    @Test
    void nodeNormalizeChained() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.node("nested", "value").set("test");

        Object normalized = config.node("nested", "value").getNormalized();
        assertNotNull(normalized);
    }
}
