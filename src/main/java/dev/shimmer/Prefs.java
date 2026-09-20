package dev.shimmer;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/** What each player chose, kept on the player itself so it survives restarts and needs no database. */
public final class Prefs {

    private final NamespacedKey trail;
    private final NamespacedKey paused;
    private final NamespacedKey hidden;

    Prefs(ShimmerPlugin plugin) {
        this.trail = new NamespacedKey(plugin, "trail");
        this.paused = new NamespacedKey(plugin, "paused");
        this.hidden = new NamespacedKey(plugin, "hidden");
    }

    public @Nullable String trail(Player player) {
        return player.getPersistentDataContainer().get(trail, PersistentDataType.STRING);
    }

    public void setTrail(Player player, @Nullable String id) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (id == null) data.remove(trail);
        else data.set(trail, PersistentDataType.STRING, id);
    }

    public boolean paused(Player player) {
        return player.getPersistentDataContainer().has(paused, PersistentDataType.BYTE);
    }

    public void setPaused(Player player, boolean value) {
        flag(player, paused, value);
    }

    /** True when the player turned off seeing trails. */
    public boolean hidden(Player player) {
        return player.getPersistentDataContainer().has(hidden, PersistentDataType.BYTE);
    }

    public void setHidden(Player player, boolean value) {
        flag(player, hidden, value);
    }

    private static void flag(Player player, NamespacedKey key, boolean value) {
        if (value) player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        else player.getPersistentDataContainer().remove(key);
    }
}
