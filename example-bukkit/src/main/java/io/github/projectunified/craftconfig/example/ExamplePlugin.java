package io.github.projectunified.craftconfig.example;

import io.github.projectunified.craftconfig.bukkit.BukkitConfig;
import io.github.projectunified.craftconfig.common.ConfigNode;
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
    }

    private void demonstrateBasicUsage() {
        getLogger().info("=== Basic Usage ===");

        config.node("name").set("MyServer");
        config.node("port").set(25565);
        config.node("debug").set(false);

        getLogger().info("Name: " + config.node("name").get(String.class));
        getLogger().info("Port: " + config.node("port").get(Integer.class));
        getLogger().info("Debug: " + config.node("debug").get(Boolean.class));

        config.save();
    }

    private void demonstrateNodeNavigation() {
        getLogger().info("=== Node Navigation ===");

        config.node("database", "host").set("localhost");
        config.node("database", "port").set(3306);
        config.node("database", "name").set("mydb");

        ConfigNode dbNode = config.node("database");
        getLogger().info("Database host: " + dbNode.node("host").get(String.class));
        getLogger().info("Database port: " + dbNode.node("port").get(Integer.class));

        config.save();
    }

    private void demonstrateChainedNodes() {
        getLogger().info("=== Chained Nodes ===");

        config.node("game", "world", "spawn", "x").set(0);
        config.node("game", "world", "spawn", "y").set(64);
        config.node("game", "world", "spawn", "z").set(0);
        config.node("game", "world", "name").set("world_nether");
        config.node("game", "world", "difficulty").set("normal");

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
        config.node("permissions", "admin", "ban").set(true);
        config.node("permissions", "admin", "kick").set(true);
        config.node("permissions", "user", "level").set(10);
        config.node("permissions", "user", "chat").set(true);

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
        config.node("items", "pickaxe").set("iron");
        config.node("items", "axe").set("wood");

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
        config.node("settings", "difficulty").setIfAbsent("normal");
        config.node("settings", "pvp").setIfAbsent(true);

        config.node("settings", "language").setIfAbsent("de");
        getLogger().info("Language (should be en): " + config.node("settings", "language").get(String.class));

        config.save();
    }

    private void demonstrateNodeOperations() {
        getLogger().info("=== Node Operations ===");

        config.node("test", "path").set("exists");

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
}
