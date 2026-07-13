package io.github.projectunified.craftconfig.configurate;

import io.github.projectunified.craftconfig.common.CommentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurateConfigTest {

    @TempDir
    Path tempDir;

    private ConfigurateConfig createConfig(String fileName) {
        File file = tempDir.resolve(fileName).toFile();
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        return config;
    }

    @Test
    void getOriginalReturnsConfigurationNode() {
        ConfigurateConfig config = createConfig("test.yml");
        Object original = config.getOriginal();
        assertInstanceOf(ConfigurationNode.class, original);
    }

    @Test
    void setAndGetStringValue() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("key").set("hello");
        assertEquals("hello", config.node("key").get());
    }

    @Test
    void setAndGetNestedPath() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("a", "b", "c").set("world");
        assertEquals("world", config.node("a", "b", "c").get());
    }

    @Test
    void getWithDefaultReturnsDefaultWhenMissing() {
        ConfigurateConfig config = createConfig("test.yml");
        assertEquals("fallback", config.node("missing").get("fallback"));
    }

    @Test
    void containsTrueWhenSet() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("existing").set("value");
        assertTrue(config.node("existing").exists());
    }

    @Test
    void containsFalseForMissing() {
        ConfigurateConfig config = createConfig("test.yml");
        assertFalse(config.node("nonexistent").exists());
    }

    @Test
    void removeDeletesEntry() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("toRemove").set("value");
        assertTrue(config.node("toRemove").exists());
        config.node("toRemove").remove();
        assertFalse(config.node("toRemove").exists());
    }

    @Test
    void clearRemovesAll() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("key1").set("a");
        config.node("key2").set("b");
        config.remove();
        assertFalse(config.node("key1").exists());
        assertFalse(config.node("key2").exists());
    }

    @Test
    void getNameReturnsFileName() {
        ConfigurateConfig config = createConfig("myfile.yml");
        assertEquals("myfile.yml", config.getName());
    }

    @Test
    void getChildrenShallowReturnsDirectChildren() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("a", "b").set("v1");
        config.node("a", "c").set("v2");

        assertEquals(1, config.getChildren().size());
    }

    @Test
    void getChildrenDeepRecurses() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("a", "b").set("v1");
        config.node("a", "c").set("v2");

        assertFalse(config.getChildren().isEmpty());
    }

    @Test
    void setupCreatesFileIfNotExists() {
        File file = tempDir.resolve("newfile.yml").toFile();
        assertFalse(file.exists());
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        assertTrue(file.exists());
    }

    @Test
    void setupLoadsExistingContent() throws IOException {
        File file = tempDir.resolve("existing.yml").toFile();
        Files.writeString(file.toPath(), "key: value\n");
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        assertEquals("value", config.node("key").get());
    }

    @Test
    void reloadReadsLatestFromFile() throws IOException {
        ConfigurateConfig config = createConfig("reload.yml");
        config.node("data").set("original");
        config.save();

        Files.writeString(tempDir.resolve("reload.yml"), "updated: fresh\n");
        config.reload();
        assertEquals("fresh", config.node("updated").get());
        assertNull(config.node("original").get());
    }

    @Test
    void saveWritesToFile() throws IOException {
        ConfigurateConfig config = createConfig("save.yml");
        config.node("data").set("saved");
        config.save();

        File file = tempDir.resolve("save.yml").toFile();
        String content = Files.readString(file.toPath());
        assertTrue(content.contains("saved"));
        assertTrue(content.contains("data"));
    }

    @Test
    void normalizeConfigurationNodeListReturnsList() throws Exception {
        ConfigurateConfig config = createConfig("test.yml");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        root.node("list").appendListNode().set("a");
        root.node("list").appendListNode().set("b");

        ConfigurationNode listNode = root.node("list");
        Object normalized = config.normalize(listNode);
        assertInstanceOf(List.class, normalized);
        assertEquals(2, ((List<?>) normalized).size());
    }

    @Test
    void normalizeConfigurationNodeMapReturnsMap() throws Exception {
        ConfigurateConfig config = createConfig("test.yml");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        root.node("mapKey", "child1").set("val1");
        root.node("mapKey", "child2").set("val2");
        ConfigurationNode mapNode = root.node("mapKey");

        Object normalized = config.normalize(mapNode);
        assertInstanceOf(java.util.Map.class, normalized);
    }

    @Test
    void normalizeConfigurationNodeScalarReturnsRawValue() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("testPath").set("scalar");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        ConfigurationNode scalarNode = root.node("testPath");

        Object normalized = config.normalize(scalarNode);
        assertEquals("scalar", normalized);
    }

    @Test
    void isNormalizableConfigurationNodeReturnsTrue() {
        ConfigurateConfig config = createConfig("test.yml");
        assertTrue(config.isNormalizable(config.getOriginal()));
    }

    @Test
    void isNormalizableOtherTypeReturnsFalse() {
        ConfigurateConfig config = createConfig("test.yml");
        assertFalse(config.isNormalizable("not a node"));
        assertFalse(config.isNormalizable(42));
        assertFalse(config.isNormalizable(null));
    }

    @Test
    void getCommentBlockTypeReturnsComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("path").set("value");
        config.node("path").setComment(Arrays.asList("line1", "line2"));

        List<String> comment = config.node("path").getComment(CommentType.BLOCK);
        assertEquals(Arrays.asList("line1", "line2"), comment);
    }

    @Test
    void setCommentBlockTypeSetsComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("key").set("value");
        config.node("key").setComment(CommentType.BLOCK, List.of("comment line"));

        List<String> comment = config.node("key").getComment(CommentType.BLOCK);
        assertEquals(1, comment.size());
        assertEquals("comment line", comment.get(0));
    }

    @Test
    void setCommentNullRemovesComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("key").set("value");
        config.node("key").setComment(List.of("to remove"));
        assertFalse(config.node("key").getComment(CommentType.BLOCK).isEmpty());

        config.node("key").setComment(null);
        assertTrue(config.node("key").getComment(CommentType.BLOCK).isEmpty());
    }

    @Test
    void getCommentNonBlockTypeReturnsEmpty() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("key").set("value");
        config.node("key").setComment(List.of("side comment"));

        List<String> comment = config.node("key").getComment(CommentType.SIDE);
        assertTrue(comment.isEmpty());
    }

    @Test
    void getChildrenNonMapThrows() {
        ConfigurateConfig config = createConfig("test.yml");
        config.node("leaf").set("scalar");
        assertThrows(IllegalStateException.class, () -> config.node("leaf").getChildren());
    }
}
