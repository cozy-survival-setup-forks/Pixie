package dev.shimmer;

import dev.shimmer.command.TrailCommand;
import dev.shimmer.hook.ShimmerExpansion;
import dev.shimmer.trail.Trail;
import dev.shimmer.trail.TrailLibrary;
import dev.shimmer.trail.TrailParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Shimmer: particle trails. The trails are in trails.yml, and a player's choice is kept on the player, so there is no
 * database.
 */
public final class ShimmerPlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private Prefs prefs;
    private TrailLibrary trails;
    private Emitter emitter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(getConfig());
        messages = new Messages(this);
        messages.load();
        prefs = new Prefs(this);
        trails = loadTrails();
        getLogger().info("Loaded " + trails.size() + " trails.");

        emitter = new Emitter(this);
        emitter.start();
        getServer().getPluginManager().registerEvents(new ShimmerListener(this), this);

        var command = getCommand("trail");
        if (command != null) {
            TrailCommand handler = new TrailCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ShimmerExpansion(this).register();
        }

        for (Player player : Bukkit.getOnlinePlayers()) load(player);
    }

    private TrailLibrary loadTrails() {
        File file = new File(getDataFolder(), "trails.yml");
        if (!file.exists()) saveResource("trails.yml", false);
        return new TrailLibrary(TrailParser.parse(YamlConfiguration.loadConfiguration(file), getLogger(), TrailParser.realNames()));
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
        return player.hasPermission(trail.permission()) || player.hasPermission("shimmer.trail.*");
    }

    /** Gives the player a trail and remembers it. */
    public void equip(Player player, Trail trail) {
        prefs.setTrail(player, trail.id());
        prefs.setPaused(player, false);
        emitter.wear(player, trail);
    }

    /** Takes the trail off the player and forgets it. */
    public void remove(Player player) {
        prefs.setTrail(player, null);
        prefs.setPaused(player, false);
        emitter.remove(player);
    }

    /** Reads every file again. Returns how many trails there are. */
    public int reloadAll() {
        reloadConfig();
        settings = new Settings(getConfig());
        messages.load();
        trails = loadTrails();
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
