package dev.shimmer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Gives a player their trail when they join, and takes it off when they leave. */
public final class ShimmerListener implements Listener {

    private final ShimmerPlugin plugin;

    ShimmerListener(ShimmerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // A moment later, when the player has finished loading in and their permissions are there
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.load(player);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.emitter().remove(event.getPlayer());
        plugin.emitter().setBlind(event.getPlayer(), false);
    }
}
