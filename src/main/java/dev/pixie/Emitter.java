package dev.pixie;

import dev.pixie.trail.Trail;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sends the trails. One task for the whole server goes through the players who wear a trail, so nothing is created
 * per player except one small object, and a puff that is not due costs a single comparison.
 *
 * <p>The connection of the players is what particles cost, so this is careful: a trail is only sent while its wearer
 * moves (unless it says otherwise), a scatter layer is one packet however many particles it has, the number of packets per
 * tick has a limit shared fairly, and everything slows down when the server does.</p>
 */
public final class Emitter {

    /** A player who wears a trail. */
    private static final class Wearer {
        final Player player;
        Trail trail;
        long next;
        int puffs;
        int idle;
        double x;
        double y;
        double z;

        Wearer(Player player, Trail trail) {
            this.player = player;
            this.trail = trail;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }

    /** Puffs still sent after the wearer stopped, so a trail does not end in the middle of a step. */
    private static final int GRACE_TICKS = 6;
    private static final double MOVED = 0.0025;

    private final PixiePlugin plugin;
    private final Map<UUID, Wearer> wearers = new HashMap<>();
    private final List<Wearer> order = new ArrayList<>();
    /** Players who turned off seeing trails. */
    private final Set<UUID> blind = new HashSet<>();
    private final List<Player> receivers = new ArrayList<>();
    private final List<Wearer> dropped = new ArrayList<>();
    private long tick;
    private int turn;

    Emitter(PixiePlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::run, 1L, 1L);
    }

    // ---- who wears what ----

    public void wear(Player player, Trail trail) {
        Wearer existing = wearers.get(player.getUniqueId());
        if (existing != null) {
            existing.trail = trail;
            existing.next = 0;
            return;
        }
        Wearer wearer = new Wearer(player, trail);
        wearers.put(player.getUniqueId(), wearer);
        order.add(wearer);
    }

    public void remove(Player player) {
        Wearer wearer = wearers.remove(player.getUniqueId());
        if (wearer != null) order.remove(wearer);
    }

    public @Nullable Trail wearing(Player player) {
        Wearer wearer = wearers.get(player.getUniqueId());
        return wearer == null ? null : wearer.trail;
    }

    public void setBlind(Player player, boolean value) {
        if (value) blind.add(player.getUniqueId());
        else blind.remove(player.getUniqueId());
    }

    public boolean sends() {
        return !order.isEmpty();
    }

    // ---- the loop ----

    private void run() {
        tick++;
        int count = order.size();
        if (count == 0) return;

        Settings settings = plugin.settings();
        int slow = 1;
        if (settings.lagGuard()) {
            double busy = Bukkit.getAverageTickTime();
            if (busy > settings.pauseAbove()) return;
            if (busy > settings.slowAbove()) slow = 2;
        }

        int budget = settings.budget();
        turn = (turn + 1) % count;
        for (int i = 0; i < count && budget > 0; i++) {
            Wearer wearer = order.get((turn + i) % count);
            if (tick < wearer.next) continue;

            int interval = wearer.trail.interval() * slow;
            wearer.next = tick + interval;
            if (!due(wearer, interval, settings)) continue;

            try {
                // A permission lost while the player is online ends the trail without waiting for a relog
                if (!plugin.canUse(wearer.player, wearer.trail)) {
                    dropped.add(wearer);
                    continue;
                }
                budget -= wearer.trail.packets();
                puff(wearer);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("The trail " + wearer.trail.id() + " of " + wearer.player.getName()
                        + " could not be sent and was taken off: " + e);
                dropped.add(wearer);
            }
        }
        if (!dropped.isEmpty()) {
            for (Wearer wearer : dropped) {
                remove(wearer.player);
            }
            dropped.clear();
        }
    }

    /** Whether the wearer should show the trail now. */
    private boolean due(Wearer wearer, int interval, Settings settings) {
        Player player = wearer.player;
        if (!player.isOnline() || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (settings.isDisabled(player.getWorld())) return false;
        if (vanished(player)) return false;
        if (player.isInvisible() && !settings.showWhenInvisible()) return false;

        if (wearer.trail.movingOnly()) {
            double dx = player.getX() - wearer.x;
            double dy = player.getY() - wearer.y;
            double dz = player.getZ() - wearer.z;
            if (dx * dx + dy * dy + dz * dz > MOVED) {
                wearer.x = player.getX();
                wearer.y = player.getY();
                wearer.z = player.getZ();
                wearer.idle = 0;
            } else {
                wearer.idle += interval;
                if (wearer.idle > GRACE_TICKS) return false;
            }
        }
        return true;
    }

    /** Some plugins leave the metadata behind with the value false when a player shows again. */
    private static boolean vanished(Player player) {
        if (!player.hasMetadata("vanished")) return false;
        for (var value : player.getMetadata("vanished")) {
            if (value.asBoolean()) return true;
        }
        return false;
    }

    private void puff(Wearer wearer) {
        Player player = wearer.player;
        World world = player.getWorld();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        int puff = wearer.puffs++;

        // Only the wearer and the players who can see them, so a hidden or vanished wearer shows nothing
        receivers.clear();
        if (!blind.contains(player.getUniqueId())) receivers.add(player);
        for (Player viewer : player.getTrackedBy()) {
            if (!blind.contains(viewer.getUniqueId())) receivers.add(viewer);
        }
        if (receivers.isEmpty()) return;

        for (Trail.Layer layer : wearer.trail.layers()) {
            Object data = layer.data(puff);
            switch (layer.shape()) {
                case SCATTER -> send(world, player, layer, x + layer.x(), y + layer.y(), z + layer.z(),
                        layer.count(), layer.spread(), layer.height(), layer.spread(), data);
                case RING -> {
                    double turn = puff * 0.35;
                    for (int i = 0; i < layer.points(); i++) {
                        double angle = turn + i * (2 * Math.PI / layer.points());
                        send(world, player, layer, x + layer.x() + Math.cos(angle) * layer.radius(), y + layer.y(),
                                z + layer.z() + Math.sin(angle) * layer.radius(), 1, 0, 0, 0, data);
                    }
                }
                case HELIX -> {
                    double climb = (puff % 18) / 18.0 * layer.height();
                    double angle = puff * 0.55;
                    for (int i = 0; i < 2; i++) {
                        double a = angle + i * Math.PI;
                        send(world, player, layer, x + layer.x() + Math.cos(a) * layer.radius(), y + layer.y() + climb,
                                z + layer.z() + Math.sin(a) * layer.radius(), 1, 0, 0, 0, data);
                    }
                }
                case ORBIT -> {
                    double angle = puff * 0.5;
                    send(world, player, layer, x + layer.x() + Math.cos(angle) * layer.radius(), y + layer.y(),
                            z + layer.z() + Math.sin(angle) * layer.radius(), 1, 0, 0, 0, data);
                }
            }
        }
    }

    /** One particle call, sent to the receivers only. */
    private void send(World world, Player owner, Trail.Layer layer, double x, double y, double z, int count,
                      double ox, double oy, double oz, @Nullable Object data) {
        Particle particle = layer.particle();
        double speed = layer.speed();
        if (particle == Particle.NOTE) {
            // A note is coloured by its offset, with a count of zero
            double note = data instanceof Integer value ? value / 24.0 : ThreadLocalRandom.current().nextDouble();
            world.spawnParticle(particle, receivers, owner, x, y, z, 0, note, 0, 0, 1.0, null);
            return;
        }
        world.spawnParticle(particle, receivers, owner, x, y, z, count, ox, oy, oz, speed, data);
    }
}
