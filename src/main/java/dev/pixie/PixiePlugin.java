package dev.pixie;

import dev.pixie.command.TrailCommand;
import dev.pixie.hook.PixieExpansion;
import dev.pixie.trail.Trail;
import dev.pixie.trail.TrailLibrary;
import dev.pixie.trail.TrailParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Pixie: particle trails. The trails are in trails.yml, and a player's choice is kept on the player, so there is no
 * database.
 */
public final class PixiePlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private Prefs prefs;
    private TrailLibrary trails;
    private Emitter emitter;
    private PixieExpansion expansion;

    @Override
    public void onEnable() {
        try {
            enableInner();
        } catch (RuntimeException e) {
            getLogger().severe("Pixie could not start: " + e.getMessage() + ". Check config.yml, lang.yml and trails.yml.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void enableInner() {
        saveDefaultConfig();
        settings = new Settings(readYaml("config.yml"));
        messages = new Messages(this);
        if (!messages.load()) {
            throw new IllegalStateException("lang.yml could not be read");
        }
        prefs = new Prefs(this);
        trails = loadTrails();
        getLogger().info("Loaded " + trails.size() + " trails.");

        emitter = new Emitter(this);
        emitter.start();
        getServer().getPluginManager().registerEvents(new PixieListener(this), this);

        var command = getCommand("trail");
        if (command != null) {
            TrailCommand handler = new TrailCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new PixieExpansion(this);
            expansion.register();
        }

        for (Player player : Bukkit.getOnlinePlayers()) load(player);
        Metrics.start(this);
        Banner.print(this, "Thanks for making the server sparkle.");
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
        }
    }

    /** Reads a yml file of the plugin folder. A file that does not parse is an error, not an empty file. */
    private YamlConfiguration readYaml(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IllegalStateException(name + " could not be read: " + e.getMessage(), e);
        }
        return yaml;
    }

    private TrailLibrary loadTrails() {
        return new TrailLibrary(TrailParser.parse(readYaml("trails.yml"), getLogger(), TrailParser.realNames()));
    }

    /** Starts what the player has picked, if it still exists and they may use it. Used on join and after a change. */
    public void load(Player player) {
        emitter.setBlind(player, prefs.hidden(player));

        Trail trail = trails.get(prefs.trail(player));
        if (trail == null || prefs.paused(player) || !canUse(player, trail)) {
            emitter.remove(player);
            return;
        }
        emitter.wear(player, trail);
    }

    public boolean canUse(Player player, Trail trail) {
        if (!settings.usePermissions()) return true;
        return player.hasPermission(trail.permission()) || player.hasPermission("pixie.trail.*")
                || trail.id().equals(prefs.granted(player));
    }

    /** Gives the player a trail and remembers it. */
    public void equip(Player player, Trail trail) {
        prefs.setGranted(player, null);
        prefs.setTrail(player, trail.id());
        prefs.setPaused(player, false);
        emitter.wear(player, trail);
    }

    /** Gives the player a trail they may wear without having the permission, as long as they keep it. */
    public void grant(Player player, Trail trail) {
        prefs.setGranted(player, trail.id());
        prefs.setTrail(player, trail.id());
        prefs.setPaused(player, false);
        emitter.wear(player, trail);
    }

    /** Takes the trail off the player and forgets it. */
    public void remove(Player player) {
        prefs.setGranted(player, null);
        prefs.setTrail(player, null);
        prefs.setPaused(player, false);
        emitter.remove(player);
    }

    /** Reads every file again. Returns how many trails there are, or -1 and changes nothing if a file cannot be read. */
    public int reloadAll() {
        Settings freshSettings;
        TrailLibrary freshTrails;
        try {
            freshSettings = new Settings(readYaml("config.yml"));
            freshTrails = loadTrails();
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage() + " - keeping the old settings and trails");
            return -1;
        }
        if (!messages.load()) {
            return -1;
        }
        settings = freshSettings;
        trails = freshTrails;
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
        return trails.size();
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Prefs prefs() {
        return prefs;
    }

    public TrailLibrary trails() {
        return trails;
    }

    public Emitter emitter() {
        return emitter;
    }
}
