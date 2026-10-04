package dev.pixie;

import dev.pixie.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** The texts in lang.yml. A text missing from an older file falls back to the default one. */
public final class Messages {

    private final PixiePlugin plugin;
    private FileConfiguration file = new YamlConfiguration();

    Messages(PixiePlugin plugin) {
        this.plugin = plugin;
    }

    /** Reads lang.yml. A file that cannot be read leaves the texts as they were and returns false. */
    boolean load() {
        File target = new File(plugin.getDataFolder(), "lang.yml");
        if (!target.exists()) plugin.saveResource("lang.yml", false);
        YamlConfiguration fresh = new YamlConfiguration();
        try {
            fresh.load(target);
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("lang.yml could not be read: " + e.getMessage());
            return false;
        }

        var defaults = plugin.getResource("lang.yml");
        if (defaults != null) {
            fresh.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            fresh.options().copyDefaults(true);
        }
        file = fresh;
        return true;
    }

    private String text(String key) {
        String value = file.getString(key);
        return value == null ? "" : value;
    }

    public String raw(String key) {
        return text(key);
    }

    /** Sends a message with the prefix. An empty message is skipped, so any of them can be turned off. */
    public void send(CommandSender to, String key, Map<String, String> values) {
        if (file.isList(key)) {
            for (String line : file.getStringList(key)) {
                to.sendMessage(Text.component(Text.fill(line, values)));
            }
            return;
        }
        String text = text(key);
        if (text.isEmpty()) return;
        to.sendMessage(Text.component(Text.fill(text("prefix") + text, values)));
    }

    public void send(CommandSender to, String key) {
        send(to, key, Map.of());
    }

    public void broadcast(String key, Map<String, String> values) {
        String text = text(key);
        if (text.isEmpty()) return;
        var message = Text.component(Text.fill(text("prefix") + text, values));
        Bukkit.getServer().sendMessage(message);
    }
}
