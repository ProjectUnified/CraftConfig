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
        demonstrateComments();
        demonstrateTypedAccess();
    }

    private void demonstrateBasicUsage() {
        getLogger().info("=== Basic Usage ===");

        config.node("name").setIfAbsent("MyServer");
        config.node("port").setIfAbsent(25565);
        config.node("debug").setIfAbsent(false);

        getLogger().info("Name: " + config.node("name").get());
        getLogger().info("Port: " + config.node("port").get());
        getLogger().info("Debug: " + config.node("debug").get());

        config.save();
    }

    private void demonstrateNodeNavigation() {
        getLogger().info("=== Node Navigation ===");

        config.node("database", "host").setIfAbsent("localhost");
        config.node("database", "port").setIfAbsent(3306);
        config.node("database", "name").setIfAbsent("mydb");

        ConfigNode dbNode = config.node("database");
        getLogger().info("Has children: " + dbNode.hasChild());

        Map<String, ConfigNode> children = dbNode.getChildren();
        for (Map.Entry<String, ConfigNode> entry : children.entrySet()) {
            getLogger().info("  " + entry.getKey() + " = " + entry.getValue().get());
        }

        config.save();
    }

    private void demonstrateComments() {
        getLogger().info("=== Comments ===");

        config.node("welcome").setIfAbsent("Hello World!");
        config.node("welcome").setComment(Arrays.asList("Welcome message", "Displayed to all players"));

        config.node("max-players").setIfAbsent(20);
        config.node("max-players").setComment(Arrays.asList("Maximum number of players", "Default: 20"));

        config.save();
    }

    private void demonstrateTypedAccess() {
        getLogger().info("=== Typed Access ===");

        String name = config.node("name").get(String.class);
        int port = config.node("port").get(Integer.class, 25565);
        boolean debug = config.node("debug").get(Boolean.class, false);

        getLogger().info("Name (String): " + name);
        getLogger().info("Port (int): " + port);
        getLogger().info("Debug (boolean): " + debug);
    }
}
