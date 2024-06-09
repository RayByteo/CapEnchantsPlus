package com.kleidion.capenchants;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class CapEnchants extends JavaPlugin implements Listener, TabExecutor {

    private final Map<Enchantment, Integer> enchantmentCaps = new HashMap<>();
    private File capsFile;
    private FileConfiguration capsConfig;

    @Override
    public void onEnable() {
        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register the command executor and tab completer
        if (getCommand("cap") == null) {
            getLogger().severe("Command /cap is not registered. Check your plugin.yml.");
            return;
        }
        getCommand("cap").setExecutor(this);
        getCommand("cap").setTabCompleter(this);

        // Load the enchantment caps from the file
        loadCaps();
    }

    @Override
    public void onDisable() {
        // Save the enchantment caps to the file
        saveCaps();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cap")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /cap <enchantment> <level>");
                return false;
            }

            Enchantment enchantment = Enchantment.getByName(args[0].toUpperCase());
            if (enchantment == null) {
                sender.sendMessage("Unknown enchantment: " + args[0]);
                return false;
            }

            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid level: " + args[1]);
                return false;
            }

            enchantmentCaps.put(enchantment, level);
            sender.sendMessage("Capped " + enchantment.getKey().getKey() + " at level " + level);
            return true;
        }
        return false;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("cap")) {
            if (args.length == 1) {
                // Provide enchantment suggestions
                java.util.List<String> completions = new java.util.ArrayList<>();
                for (Enchantment enchantment : Enchantment.values()) {
                    completions.add(enchantment.getKey().getKey());
                }
                return completions;
            }
        }
        return java.util.Collections.emptyList();
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        for (Map.Entry<Enchantment, Integer> entry : enchantmentCaps.entrySet()) {
            if (event.getEnchantsToAdd().containsKey(entry.getKey())) {
                int maxLevel = entry.getValue();
                int currentLevel = event.getEnchantsToAdd().get(entry.getKey());
                if (currentLevel > maxLevel) {
                    event.getEnchantsToAdd().put(entry.getKey(), maxLevel);
                }
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getResult() != null && event.getResult().getItemMeta() != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantmentCaps.entrySet()) {
                if (event.getResult().getItemMeta().hasEnchant(entry.getKey())) {
                    int maxLevel = entry.getValue();
                    int currentLevel = event.getResult().getItemMeta().getEnchantLevel(entry.getKey());
                    if (currentLevel > maxLevel) {
                        event.getResult().removeEnchantment(entry.getKey());
                        event.getResult().addEnchantment(entry.getKey(), maxLevel);
                    }
                }
            }
        }
    }

    private void loadCaps() {
        capsFile = new File(getDataFolder(), "caps.yml");
        if (!capsFile.exists()) {
            saveResource("caps.yml", false);
        }
        capsConfig = YamlConfiguration.loadConfiguration(capsFile);

        for (String key : capsConfig.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key));
            int level = capsConfig.getInt(key);
            if (enchantment != null) {
                enchantmentCaps.put(enchantment, level);
            }
        }
    }

    private void saveCaps() {
        if (capsConfig == null || capsFile == null) {
            return;
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantmentCaps.entrySet()) {
            capsConfig.set(entry.getKey().getKey().getKey(), entry.getValue());
        }

        try {
            capsConfig.save(capsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save caps to " + capsFile);
            e.printStackTrace();
        }
    }
}