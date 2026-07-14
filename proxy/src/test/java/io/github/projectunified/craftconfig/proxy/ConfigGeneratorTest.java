package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.StickyValue;
import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.configurate.ConfigurateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigGeneratorTest {

    @TempDir
    Path tempDir;

    private ConfigurateConfig config;

    @BeforeEach
    void setUp() {
        config = new ConfigurateConfig(tempDir.resolve("test.yml").toFile(), YamlConfigurationLoader.builder());
        config.setup();
    }

    @Test
    void newInstanceReturnsProxyOfCorrectType() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertNotNull(proxy);
        assertTrue(proxy instanceof MyConfig);
    }

    @Test
    void getterWithConfigPathReturnsDefaultValue() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertEquals("default", proxy.name());
        assertEquals(42, proxy.port());
        assertTrue(proxy.enabled());
    }

    @Test
    void getterWithPrefixConventionReturnsDefaultValue() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertEquals("default", proxy.getName());
        assertTrue(proxy.isEnabled());
    }

    @Test
    void getterReturnsConfigValueWhenSet() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        config.node("name").set("custom");
        config.node("port").set(8080);
        config.node("enabled").set(false);

        assertEquals("custom", proxy.name());
        assertEquals(8080, proxy.port());
        assertFalse(proxy.enabled());
    }

    @Test
    void prefixGetterReturnsConfigValueWhenSet() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        config.node("name").set("custom");
        config.node("enabled").set(false);

        assertEquals("custom", proxy.getName());
        assertFalse(proxy.isEnabled());
    }

    @Test
    void setterSetsValueAndSaves() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        proxy.name("newValue");
        assertEquals("newValue", config.node("name").get(String.class));
    }

    @Test
    void prefixSetterSetsValueAndSaves() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        proxy.setName("newValue");
        assertEquals("newValue", config.node("name").get(String.class));
    }

    @Test
    void setterClearsCache() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config, true, true);
        proxy.name();
        config.node("name").set("cached");
        proxy.name("newValue");
        assertEquals("newValue", proxy.name());
    }

    @Test
    void getConfigReturnsUnderlyingConfig() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        Config returnedConfig = proxy.getConfig();
        assertSame(config, returnedConfig);
    }

    @Test
    void reloadConfigReloadsAndClearsCaches() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config, true, true);
        proxy.name();
        config.node("name").set("reloaded");
        config.save();
        proxy.reloadConfig();
        assertEquals("reloaded", proxy.name());
    }

    @Test
    void toStringReturnsClassName() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertEquals(MyConfig.class.toString(), proxy.toString());
    }

    @Test
    void hashCodeReturnsClassHashCode() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertEquals(MyConfig.class.hashCode(), proxy.hashCode());
    }

    @Test
    void equalsReturnsTrueForSameProxy() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertTrue(proxy.equals(proxy));
    }

    @Test
    void equalsReturnsFalseForDifferentProxy() {
        MyConfig proxy1 = ConfigGenerator.newInstance(MyConfig.class, config);
        MyConfig proxy2 = ConfigGenerator.newInstance(MyConfig.class, config);
        assertFalse(proxy1.equals(proxy2));
    }

    @Test
    void addDefaultSetsDefaultsInConfig() {
        ConfigGenerator.newInstance(MyConfig.class, config, true, false, true);
        assertEquals("default", config.node("name").get(String.class));
        assertEquals(42, config.node("port").get(Integer.class));
        assertEquals(true, config.node("enabled").get(Boolean.class));
    }

    @Test
    void commentAnnotationSetsCommentOnFirstSetup() {
        ConfigGenerator.newInstance(CommentedConfig.class, config, true, false, true);
        List<String> comment = config.node("name").getComment();
        assertEquals(Arrays.asList("This is the name"), comment);
    }

    @Test
    void addDefaultSetsDefaultsInNestedSubConfig() {
        config.node("middle", "value").set("defaultMiddle");
        config.node("middle", "inner", "value").set("defaultInner");

        ConfigGenerator.newInstance(DeepConfig.class, config, false, false, true);

        assertEquals("defaultMiddle", config.node("middle", "value").get(String.class));
        assertEquals("defaultInner", config.node("middle", "inner", "value").get(String.class));
    }

    @Test
    void nestedPathWorksCorrectly() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config, true, false, true);
        assertEquals("nestedDefault", proxy.nestedValue());
        config.node("server", "name").set("customNested");
        assertEquals("customNested", proxy.nestedValue());
    }

    @Test
    void defaultVoidMethodWithNonConfigPathExecutesDefaultBody() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        assertDoesNotThrow(proxy::unknownMethod);
    }

    @Test
    void stickyValueReturnsCachedValue() {
        StickyConfig proxy = ConfigGenerator.newInstance(StickyConfig.class, config, true, true, true);
        assertEquals("sticky", proxy.stickyValue());
        config.node("value").set("changed");
        assertEquals("sticky", proxy.stickyValue());
    }

    @Test
    void nonStickyValueReturnsLatestValue() {
        NonStickyConfig proxy = ConfigGenerator.newInstance(NonStickyConfig.class, config, true, false, true);
        assertEquals("nonSticky", proxy.nonStickyValue());
        config.node("value").set("changed");
        assertEquals("changed", proxy.nonStickyValue());
    }

    @Test
    void reloadClearsStickyCache() {
        StickyConfig proxy = ConfigGenerator.newInstance(StickyConfig.class, config, true, true, true);
        assertEquals("sticky", proxy.stickyValue());
        config.node("value").set("changed");
        config.save();
        proxy.reloadConfig();
        assertEquals("changed", proxy.stickyValue());
    }

    @Test
    void setupConfigIsCalledWhenTrue() {
        File file = tempDir.resolve("setup" + System.nanoTime() + ".yml").toFile();
        ConfigurateConfig newConfig = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        ConfigGenerator.newInstance(MyConfig.class, newConfig, true);
        assertTrue(file.exists());
    }

    @Test
    void subConfigReturnsProxiedChildNode() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config, true, false, true);
        ServerConfig server = proxy.server();

        assertNotNull(server);
        assertTrue(server instanceof ServerConfig);

        assertEquals("localhost", server.host());
        assertEquals("A Server", server.motd());
    }

    @Test
    void subConfigReadsFromCorrectPath() {
        config.node("server", "host").set("192.168.1.1");
        config.node("server", "motd").set("Custom Server");

        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config, false);
        ServerConfig server = proxy.server();

        assertEquals("192.168.1.1", server.host());
        assertEquals("Custom Server", server.motd());
    }

    @Test
    void subConfigSetWritesToCorrectPath() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        ServerConfig server = proxy.server();

        server.host("10.0.0.1");
        server.motd("My Server");

        assertEquals("10.0.0.1", config.node("server", "host").get(String.class));
        assertEquals("My Server", config.node("server", "motd").get(String.class));
    }

    @Test
    void subConfigReturnsSameProxyInstance() {
        MyConfig proxy = ConfigGenerator.newInstance(MyConfig.class, config);
        ServerConfig server1 = proxy.server();
        ServerConfig server2 = proxy.server();
        assertSame(server1, server2);
    }

    @Test
    void nestedSubConfigWorks() {
        config.node("middle", "value").set("defaultMiddle");
        config.node("middle", "inner", "value").set("defaultInner");

        DeepConfig proxy = ConfigGenerator.newInstance(DeepConfig.class, config, false, false, true);
        MiddleConfig middle = proxy.middle();

        assertNotNull(middle);
        assertEquals("defaultMiddle", middle.value());

        InnerConfig inner = middle.inner();
        assertNotNull(inner);
        assertEquals("defaultInner", inner.value());
    }

    @Test
    void nestedSubConfigReadsFromCorrectPath() {
        config.node("middle", "inner", "value").set("deepValue");

        DeepConfig proxy = ConfigGenerator.newInstance(DeepConfig.class, config, false);
        MiddleConfig middle = proxy.middle();
        InnerConfig inner = middle.inner();

        assertEquals("deepValue", inner.value());
    }

    @Test
    void nestedSubConfigSetWritesToCorrectPath() {
        config.node("middle", "inner", "value").set("initial");

        DeepConfig proxy = ConfigGenerator.newInstance(DeepConfig.class, config, false);
        MiddleConfig middle = proxy.middle();
        InnerConfig inner = middle.inner();

        inner.value("newValue");

        assertEquals("newValue", config.node("middle", "inner", "value").get(String.class));
    }

    // === Interface Inheritance Tests ===

    @Test
    void childConfigInheritsParentDefaults() {
        ChildConfig proxy = ConfigGenerator.newInstance(ChildConfig.class, config, true, false, true);
        assertEquals("localhost", proxy.host());
        assertEquals(25565, proxy.port());
        assertEquals("A Server", proxy.motd());
    }

    @Test
    void childConfigReadsParentValuesFromConfig() {
        config.node("host").set("10.0.0.1");
        config.node("port").set(8080);
        config.node("motd").set("Custom Server");

        ChildConfig proxy = ConfigGenerator.newInstance(ChildConfig.class, config, false);
        assertEquals("10.0.0.1", proxy.host());
        assertEquals(8080, proxy.port());
        assertEquals("Custom Server", proxy.motd());
    }

    @Test
    void childConfigSetsParentValues() {
        ChildConfig proxy = ConfigGenerator.newInstance(ChildConfig.class, config);
        proxy.host("example.com");
        proxy.port(9090);
        proxy.motd("Hello");

        assertEquals("example.com", config.node("host").get(String.class));
        assertEquals(9090, config.node("port").get(Integer.class));
        assertEquals("Hello", config.node("motd").get(String.class));
    }

    @Test
    void childConfigPrefixGettersWorkForInheritedMethods() {
        ChildConfig proxy = ConfigGenerator.newInstance(ChildConfig.class, config);
        assertEquals("localhost", proxy.getHost());
        assertEquals(25565, proxy.getPort());
    }

    @Test
    void childConfigPrefixSettersWorkForInheritedMethods() {
        ChildConfig proxy = ConfigGenerator.newInstance(ChildConfig.class, config);
        proxy.setHost("newhost");
        proxy.setPort(1234);
        assertEquals("newhost", config.node("host").get(String.class));
        assertEquals(1234, config.node("port").get(Integer.class));
    }

    @Test
    void childConfigOverridesParentDefault() {
        OverridingChildConfig proxy = ConfigGenerator.newInstance(OverridingChildConfig.class, config, true, false, true);
        assertEquals("overridden-host", proxy.host());
        assertEquals("Overridden Server", proxy.motd());
    }

    @Test
    void childConfigOverrideDefaultWrittenToConfig() {
        OverridingChildConfig proxy = ConfigGenerator.newInstance(OverridingChildConfig.class, config, true, false, true);
        assertEquals("overridden-host", config.node("host").get(String.class));
        assertEquals("Overridden Server", config.node("motd").get(String.class));
    }

    @Test
    void childConfigOverrideDefaultDoesNotOverwriteExistingValue() {
        config.node("host").set("existing-host");
        OverridingChildConfig proxy = ConfigGenerator.newInstance(OverridingChildConfig.class, config, false, false, true);
        assertEquals("existing-host", proxy.host());
        assertEquals("existing-host", config.node("host").get(String.class));
    }

    // === Default Method Calling Sub-Config Tests ===

    @Test
    void defaultMethodCanCallSubConfigGetter() {
        WithSubConfig proxy = ConfigGenerator.newInstance(WithSubConfig.class, config, true, false, true);
        assertEquals("localhost", proxy.resolvedHost());
        assertEquals("A Server", proxy.resolvedMotd());
    }

    @Test
    void defaultMethodCallSubConfigGetterReadsConfigValues() {
        config.node("server", "host").set("10.0.0.1");
        config.node("server", "motd").set("Custom Server");

        WithSubConfig proxy = ConfigGenerator.newInstance(WithSubConfig.class, config, false);
        assertEquals("10.0.0.1", proxy.resolvedHost());
        assertEquals("Custom Server", proxy.resolvedMotd());
    }

    @Test
    void defaultMethodCallSubConfigGetterSetsValues() {
        WithSubConfig proxy = ConfigGenerator.newInstance(WithSubConfig.class, config);
        proxy.server().host("example.com");
        proxy.server().motd("Hello");

        assertEquals("example.com", config.node("server", "host").get(String.class));
        assertEquals("Hello", config.node("server", "motd").get(String.class));
        assertEquals("example.com", proxy.resolvedHost());
        assertEquals("Hello", proxy.resolvedMotd());
    }

    @Test
    void converterConvertsValueOnGet() {
        config.node("number").set(42);
        ConverterConfig proxy = ConfigGenerator.newInstance(ConverterConfig.class, config);
        assertEquals(42.0, proxy.number().doubleValue(), 0.001);
    }

    // === Converter Tests ===

    @Test
    void converterConvertsValueOnSet() {
        ConverterConfig proxy = ConfigGenerator.newInstance(ConverterConfig.class, config);
        proxy.number(99);
        assertEquals(99, config.node("number").get(Integer.class));
    }

    @Test
    void converterWithDefaultValue() {
        ConverterConfig proxy = ConfigGenerator.newInstance(ConverterConfig.class, config, true, false, true);
        assertEquals(42.0, proxy.number().doubleValue(), 0.001);
    }

    @Test
    void converterSetAndGetRoundTrip() {
        ConverterConfig proxy = ConfigGenerator.newInstance(ConverterConfig.class, config);
        proxy.number(99);
        assertEquals(99.0, proxy.number().doubleValue(), 0.001);
    }

    // === Priority Tests ===

    @Test
    void priorityOrdersDefaultsByPriorityValue() {
        ConfigGenerator.newInstance(PriorityConfig.class, config, true, false, true);

        // All defaults should be written regardless of priority
        assertEquals("z", config.node("zebra").get(String.class));
        assertEquals("a", config.node("alpha").get(String.class));
        assertEquals("m", config.node("middle").get(String.class));

        // Verify ordering: alpha (10) before middle (20) before zebra (30)
        List<String> keys = new ArrayList<>(config.node().getChildren().keySet());
        int alphaIdx = keys.indexOf("alpha");
        int middleIdx = keys.indexOf("middle");
        int zebraIdx = keys.indexOf("zebra");
        assertTrue(alphaIdx < middleIdx, "alpha should come before middle");
        assertTrue(middleIdx < zebraIdx, "middle should come before zebra");
    }

    @Test
    void covariantReturnTypeUsesChildAnnotation() {
        CovariantChildConfig proxy = ConfigGenerator.newInstance(CovariantChildConfig.class, config, true, false, true);
        assertNotNull(proxy.settings());
        assertEquals("childValue", proxy.settings().childSetting());
        assertEquals("defaultValue", proxy.settings().parentSetting());
    }

    @Test
    void covariantReturnTypeWritesCorrectPath() {
        CovariantChildConfig proxy = ConfigGenerator.newInstance(CovariantChildConfig.class, config, true, false, true);
        assertEquals("childValue", config.node("Settings", "childSetting").get(String.class));
        assertEquals("defaultValue", config.node("Settings", "parentSetting").get(String.class));
    }

    @Test
    void covariantReturnTypeReadsExistingValues() {
        config.node("Settings", "childSetting").set("customChild");
        config.node("Settings", "parentSetting").set("customParent");

        CovariantChildConfig proxy = ConfigGenerator.newInstance(CovariantChildConfig.class, config, false);
        assertEquals("customChild", proxy.settings().childSetting());
        assertEquals("customParent", proxy.settings().parentSetting());
    }

    @Test
    void covariantReturnTypeSetsValues() {
        CovariantChildConfig proxy = ConfigGenerator.newInstance(CovariantChildConfig.class, config);
        CovariantChildSettingsConfig settings = proxy.settings();
        settings.childSetting("newChild");
        settings.parentSetting("newParent");

        assertEquals("newChild", config.node("Settings", "childSetting").get(String.class));
        assertEquals("newParent", config.node("Settings", "parentSetting").get(String.class));
    }

    @ConfigNode
    public interface PriorityConfig {
        @ConfigPath(value = "zebra", priority = 30)
        default String zebra() {
            return "z";
        }

        @ConfigPath(value = "alpha", priority = 10)
        default String alpha() {
            return "a";
        }

        @ConfigPath(value = "middle", priority = 20)
        default String middle() {
            return "m";
        }
    }

    @ConfigNode
    public interface WithSubConfig {
        @ConfigPath("server")
        ServerConfig server();

        @ConfigPath("name")
        default String name() {
            return "default";
        }

        default String resolvedHost() {
            return server().host();
        }

        default String resolvedMotd() {
            return server().motd();
        }
    }

    @ConfigNode
    public interface CommentedConfig {
        @ConfigPath("name")
        @Comment("This is the name")
        default String name() {
            return "default";
        }

        void name(String value);
    }

    @ConfigNode
    public interface MyConfig {
        @ConfigPath("name")
        default String name() {
            return "default";
        }

        void name(String value);

        @ConfigPath("port")
        default int port() {
            return 42;
        }

        @ConfigPath("enabled")
        default boolean enabled() {
            return true;
        }

        @ConfigPath("name")
        default String getName() {
            return "default";
        }

        void setName(String name);

        @ConfigPath("enabled")
        default boolean isEnabled() {
            return true;
        }

        @ConfigPath({"server", "name"})
        default String nestedValue() {
            return "nestedDefault";
        }

        @ConfigPath("server")
        ServerConfig server();

        Config getConfig();

        void reloadConfig();

        default void unknownMethod() {
        }
    }

    @ConfigNode
    public interface ServerConfig {
        @ConfigPath("host")
        default String host() {
            return "localhost";
        }

        void host(String value);

        @ConfigPath("motd")
        default String motd() {
            return "A Server";
        }

        void motd(String value);
    }

    @ConfigNode
    public interface StickyConfig {
        @ConfigPath("value")
        @StickyValue
        default String stickyValue() {
            return "sticky";
        }

        void stickyValue(String value);

        void reloadConfig();
    }

    @ConfigNode
    public interface NonStickyConfig {
        @ConfigPath("value")
        default String nonStickyValue() {
            return "nonSticky";
        }

        void nonStickyValue(String value);
    }

    @ConfigNode
    public interface DeepConfig {
        @ConfigPath("middle")
        MiddleConfig middle();
    }

    @ConfigNode
    public interface MiddleConfig {
        @ConfigPath("value")
        default String value() {
            return "defaultMiddle";
        }

        void value(String value);

        @ConfigPath("inner")
        InnerConfig inner();
    }

    @ConfigNode
    public interface InnerConfig {
        @ConfigPath("value")
        default String value() {
            return "defaultInner";
        }

        void value(String value);
    }

    // === Covariant Return Type Tests ===

    @ConfigNode
    public interface ParentConfig {
        @ConfigPath("host")
        default String host() {
            return "localhost";
        }

        void host(String value);

        @ConfigPath("host")
        default String getHost() {
            return "localhost";
        }

        void setHost(String value);

        @ConfigPath("port")
        default int port() {
            return 25565;
        }

        void port(int value);

        @ConfigPath("port")
        default int getPort() {
            return 25565;
        }

        void setPort(int value);
    }

    @ConfigNode
    public interface ChildConfig extends ParentConfig {
        @ConfigPath("motd")
        default String motd() {
            return "A Server";
        }

        void motd(String value);
    }

    @ConfigNode
    public interface OverridingChildConfig extends ParentConfig {
        @Override
        @ConfigPath("host")
        default String host() {
            return "overridden-host";
        }

        @ConfigPath("motd")
        default String motd() {
            return "Overridden Server";
        }

        void motd(String value);
    }

    @ConfigNode
    public interface ConverterConfig {
        @ConfigPath(value = "number", converter = StringToNumberConverter.class)
        default Number number() {
            return 42;
        }

        void number(Number value);
    }

    @ConfigNode
    public interface CovariantParentConfig {
        @ConfigPath("Settings")
        CovariantParentSettingsConfig settings();
    }

    @ConfigNode
    public interface CovariantParentSettingsConfig {
        @ConfigPath("parentSetting")
        default String parentSetting() {
            return "defaultValue";
        }

        void parentSetting(String value);
    }

    @ConfigNode
    public interface CovariantChildConfig extends CovariantParentConfig {
        @Override
        @ConfigPath("Settings")
        CovariantChildSettingsConfig settings();
    }

    @ConfigNode
    public interface CovariantChildSettingsConfig extends CovariantParentSettingsConfig {
        @ConfigPath("childSetting")
        default String childSetting() {
            return "childValue";
        }

        void childSetting(String value);
    }

    public static class StringToNumberConverter implements Converter {
        @Override
        public Object convert(Object raw) {
            if (raw instanceof String) {
                try {
                    return Double.parseDouble((String) raw);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (raw instanceof Number) {
                return raw;
            }
            return null;
        }

        @Override
        public Object convertToRaw(Object value) {
            if (value instanceof Number) {
                return String.valueOf(value);
            }
            return value;
        }
    }
}
