package dev.shimmer;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** The values of config.yml. */
public final class Settings {

    private final boolean usePermissions;
    private final boolean showWhenInvisible;
    private final int budget;
    private final boolean lagGuard;
    private final double slowAbove;
    private final double pauseAbove;
    private final Set<String> disabledWorlds = new HashSet<>();

    Settings(FileConfiguration config) {
        usePermissions = config.getBoolean("use-permissions", true);
        showWhenInvisible = config.getBoolean("show-when-invisible", false);
        budget = Math.max(1, config.getInt("performance.budget-per-tick", 60));
        lagGuard = config.getBoolean("performance.lag-guard", true);
        slowAbove = config.getDouble("performance.slow-above-ms", 40);
        pauseAbove = config.getDouble("performance.pause-above-ms", 47);
        config.getStringList("disabled-worlds").forEach(world -> disabledWorlds.add(world.toLowerCase(Locale.ROOT)));
    }

    public boolean usePermissions() {
        return usePermissions;
    }

    public boolean showWhenInvisible() {
        return showWhenInvisible;
    }

    public int budget() {
        return budget;
    }

    public boolean lagGuard() {
        return lagGuard;
    }

    public double slowAbove() {
        return slowAbove;
    }

    public double pauseAbove() {
        return pauseAbove;
    }

    public boolean isDisabled(World world) {
        return disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }
}
