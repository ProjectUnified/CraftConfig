package io.github.projectunified.craftconfig.example;

import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.bukkit.BukkitConfig;
import io.github.projectunified.craftconfig.common.ConfigNode;
import io.github.projectunified.craftconfig.proxy.ConfigGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

public class ExamplePlugin extends JavaPlugin {
    private BukkitConfig config;

    @Override
    public void onEnable() {
        config = new BukkitConfig(this, "config.yml");
        config.setup();

        demonstrateBasicUsage();
        demonstrateNodeNavigation();
        demonstrateChainedNodes();
        demonstrateNestedNodes();
        demonstrateChildren();
        demonstrateSetIfAbsent();
        demonstrateNodeOperations();
        demonstrateComments();
        demonstrateTypedAccess();
        demonstrateProxyConfig();
    }

    private void demonstrateBasicUsage() {
        getLogger().info("=== Basic Usage ===");

        config.node("name").set("MyServer");
        config.node("name").setComment(Arrays.asList("Server name displayed in the server list"));
        config.node("port").set(25565);
        config.node("port").setComment(Arrays.asList("The port the server listens on"));
        config.node("debug").set(false);
        config.node("debug").setComment(Arrays.asList("Enable debug mode for verbose logging"));

        getLogger().info("Name: " + config.node("name").get(String.class));
        getLogger().info("Port: " + config.node("port").get(Integer.class));
        getLogger().info("Debug: " + config.node("debug").get(Boolean.class));

        config.save();
    }

    private void demonstrateNodeNavigation() {
        getLogger().info("=== Node Navigation ===");

        config.node("database", "host").set("localhost");
        config.node("database", "host").setComment(Arrays.asList("Database server hostname"));
        config.node("database", "port").set(3306);
        config.node("database", "port").setComment(Arrays.asList("Database server port"));
        config.node("database", "name").set("mydb");
        config.node("database", "name").setComment(Arrays.asList("Database name"));

        ConfigNode dbNode = config.node("database");
        getLogger().info("Database host: " + dbNode.node("host").get(String.class));
        getLogger().info("Database port: " + dbNode.node("port").get(Integer.class));

        config.save();
    }

    private void demonstrateChainedNodes() {
        getLogger().info("=== Chained Nodes ===");

        config.node("game", "world", "spawn", "x").set(0);
        config.node("game", "world", "spawn", "x").setComment(Arrays.asList("Spawn X coordinate"));
        config.node("game", "world", "spawn", "y").set(64);
        config.node("game", "world", "spawn", "y").setComment(Arrays.asList("Spawn Y coordinate"));
        config.node("game", "world", "spawn", "z").set(0);
        config.node("game", "world", "spawn", "z").setComment(Arrays.asList("Spawn Z coordinate"));
        config.node("game", "world", "name").set("world_nether");
        config.node("game", "world", "name").setComment(Arrays.asList("World name"));
        config.node("game", "world", "difficulty").set("normal");
        config.node("game", "world", "difficulty").setComment(Arrays.asList("World difficulty", "Options: peaceful, easy, normal, hard"));

        ConfigNode gameNode = config.node("game");
        ConfigNode worldNode = gameNode.node("world");
        ConfigNode spawnNode = worldNode.node("spawn");

        getLogger().info("Game world name: " + worldNode.node("name").get(String.class));
        getLogger().info("Spawn X: " + spawnNode.node("x").get(Integer.class));
        getLogger().info("Spawn Y: " + spawnNode.node("y").get(Integer.class));
        getLogger().info("Spawn Z: " + spawnNode.node("z").get(Integer.class));

        config.save();
    }

    private void demonstrateNestedNodes() {
        getLogger().info("=== Nested Nodes ===");

        config.node("permissions", "admin", "level").set(100);
        config.node("permissions", "admin", "level").setComment(Arrays.asList("Admin permission level"));
        config.node("permissions", "admin", "ban").set(true);
        config.node("permissions", "admin", "ban").setComment(Arrays.asList("Can ban players"));
        config.node("permissions", "admin", "kick").set(true);
        config.node("permissions", "admin", "kick").setComment(Arrays.asList("Can kick players"));
        config.node("permissions", "user", "level").set(10);
        config.node("permissions", "user", "level").setComment(Arrays.asList("User permission level"));
        config.node("permissions", "user", "chat").set(true);
        config.node("permissions", "user", "chat").setComment(Arrays.asList("Can use chat"));

        ConfigNode permNode = config.node("permissions");
        ConfigNode adminNode = permNode.node("admin");
        ConfigNode userNode = permNode.node("user");

        getLogger().info("Admin level: " + adminNode.node("level").get(Integer.class));
        getLogger().info("Admin can ban: " + adminNode.node("ban").get(Boolean.class));
        getLogger().info("User level: " + userNode.node("level").get(Integer.class));
        getLogger().info("User can chat: " + userNode.node("chat").get(Boolean.class));

        config.save();
    }

    private void demonstrateChildren() {
        getLogger().info("=== Children ===");

        config.node("items", "sword").set("diamond");
        config.node("items", "sword").setComment(Arrays.asList("Sword material"));
        config.node("items", "pickaxe").set("iron");
        config.node("items", "pickaxe").setComment(Arrays.asList("Pickaxe material"));
        config.node("items", "axe").set("wood");
        config.node("items", "axe").setComment(Arrays.asList("Axe material"));

        ConfigNode itemsNode = config.node("items");
        Map<String, ConfigNode> items = itemsNode.getChildren();

        getLogger().info("Items count: " + items.size());
        for (Map.Entry<String, ConfigNode> entry : items.entrySet()) {
            getLogger().info("  " + entry.getKey() + ": " + entry.getValue().get(String.class));
        }

        config.save();
    }

    private void demonstrateSetIfAbsent() {
        getLogger().info("=== Set If Absent ===");

        config.node("settings", "language").setIfAbsent("en");
        config.node("settings", "language").setComment(Arrays.asList("Server language", "Default: en"));
        config.node("settings", "difficulty").setIfAbsent("normal");
        config.node("settings", "difficulty").setComment(Arrays.asList("Game difficulty"));
        config.node("settings", "pvp").setIfAbsent(true);
        config.node("settings", "pvp").setComment(Arrays.asList("Allow player vs player combat"));

        config.node("settings", "language").setIfAbsent("de");
        getLogger().info("Language (should be en): " + config.node("settings", "language").get(String.class));

        config.save();
    }

    private void demonstrateNodeOperations() {
        getLogger().info("=== Node Operations ===");

        config.node("test", "path").set("exists");
        config.node("test", "path").setComment(Arrays.asList("Test path for demonstration"));

        ConfigNode testNode = config.node("test", "path");
        getLogger().info("Path exists: " + testNode.exists());
        getLogger().info("Has child: " + testNode.hasChild());
        getLogger().info("Config reference: " + (testNode.getConfig() == config));

        ConfigNode parent = testNode;
        getLogger().info("Parent exists: " + parent.getParent().exists());

        config.node("test").node("path").remove();
        getLogger().info("After remove - exists: " + config.node("test", "path").exists());

        config.save();
    }

    private void demonstrateComments() {
        getLogger().info("=== Comments ===");

        config.node("welcome").set("Hello World!");
        config.node("welcome").setComment(Arrays.asList("Welcome message", "Displayed to all players"));

        config.node("max-players").set(20);
        config.node("max-players").setComment(Arrays.asList("Maximum number of players", "Default: 20"));

        config.save();
    }

    private void demonstrateTypedAccess() {
        getLogger().info("=== Typed Access ===");

        String name = config.node("name").get(String.class);
        int port = config.node("port").get(Integer.class, 25565);
        boolean debug = config.node("debug").get(Boolean.class, false);
        double version = config.node("settings", "version").get(Double.class, 1.0);

        getLogger().info("Name (String): " + name);
        getLogger().info("Port (int): " + port);
        getLogger().info("Debug (boolean): " + debug);
        getLogger().info("Version (double): " + version);
    }

    private void demonstrateProxyConfig() {
        getLogger().info("=== Proxy Config ===");

        ServerProxy serverProxy = ConfigGenerator.newInstance(ServerProxy.class, config, false);

        getLogger().info("Server host: " + serverProxy.host());
        getLogger().info("Server port: " + serverProxy.port());
        getLogger().info("Server motd: " + serverProxy.meta().motd());
        getLogger().info("Max players: " + serverProxy.meta().maxPlayers());

        serverProxy.host("play.example.com");
        serverProxy.port(25565);
        serverProxy.meta().motd("Welcome to Example Server!");
        serverProxy.meta().maxPlayers(100);

        getLogger().info("Set back to original");

        config.save();
    }

    @io.github.projectunified.craftconfig.annotation.ConfigNode
    public interface ServerProxy {
        @ConfigPath({"server", "host"})
        default String host() {
            return "localhost";
        }

        void host(String value);

        @ConfigPath({"server", "port"})
        default int port() {
            return 25565;
        }

        void port(int value);

        @ConfigPath({"server", "meta"})
        ServerMetaConfig meta();

        @io.github.projectunified.craftconfig.annotation.ConfigNode
        interface ServerMetaConfig {
            @ConfigPath("motd")
            default String motd() {
                return "A Minecraft Server";
            }

            void motd(String value);

            @ConfigPath("max-players")
            default int maxPlayers() {
                return 20;
            }

            void maxPlayers(int value);
        }
    }
}
