package dev.shimmer.command;

import dev.shimmer.ShimmerPlugin;
import dev.shimmer.trail.Trail;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /trail &lt;name&gt; picks a trail, off removes it, toggle pauses and resumes it, visibility turns seeing all trails off and
 * on, list shows what the player can use. For admins: set, clear and reload.
 */
public final class TrailCommand implements TabExecutor {

    private static final String ADMIN = "shimmer.admin";

    private final ShimmerPlugin plugin;

    public TrailCommand(ShimmerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            plugin.messages().send(sender, "usage");
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "off", "remove", "clear" -> player(sender, player -> {
                plugin.remove(player);
                plugin.messages().send(player, "removed");
            });
            case "toggle" -> player(sender, this::toggle);
            case "visibility", "hide", "show" -> player(sender, this::visibility);
            case "list" -> player(sender, this::list);
            case "reload" -> reload(sender);
            case "set" -> set(sender, args);
            default -> player(sender, player -> equip(player, first));
        }
        return true;
    }

    private void player(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
        } else if (!player.hasPermission("shimmer.use")) {
            plugin.messages().send(sender, "no-permission");
        } else {
            action.accept(player);
        }
    }

    private void equip(Player player, String id) {
        Trail trail = plugin.trails().get(id);
        if (trail == null) {
            plugin.messages().send(player, "unknown-trail");
        } else if (!plugin.canUse(player, trail)) {
            plugin.messages().send(player, "no-permission");
        } else {
            plugin.equip(player, trail);
            plugin.messages().send(player, "equipped", Map.of("%trail%", trail.display()));
        }
    }

    /** Pauses or brings back the player's own trail, and remembers which trail it was. */
    private void toggle(Player player) {
        if (plugin.prefs().trail(player) == null) {
            plugin.messages().send(player, "no-trail");
            return;
        }
        boolean pause = !plugin.prefs().paused(player);
        plugin.prefs().setPaused(player, pause);
        plugin.load(player);
        plugin.messages().send(player, pause ? "paused" : "resumed");
    }

    /** Turns seeing trails, other players' and the own one, off and on. It also saves that player's connection. */
    private void visibility(Player player) {
        boolean hide = !plugin.prefs().hidden(player);
        plugin.prefs().setHidden(player, hide);
        plugin.emitter().setBlind(player, hide);
        plugin.messages().send(player, hide ? "not-seeing" : "seeing");
    }

    private void list(Player player) {
        List<Trail> usable = plugin.trails().all().stream().filter(trail -> plugin.canUse(player, trail)).toList();
        if (usable.isEmpty()) {
            plugin.messages().send(player, "list-empty");
            return;
        }
        plugin.messages().send(player, "list-header");
        for (Trail trail : usable) {
            plugin.messages().send(player, "list-entry", Map.of("%trail%", trail.display() + " (" + trail.id() + ")"));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        int count = plugin.reloadAll();
        plugin.messages().send(sender, "reloaded", Map.of("%amount%", String.valueOf(count)));
    }

    /** /trail set <player> <name>, or /trail set <player> off */
    private void set(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        Player target = args.length < 2 ? null : Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-not-found");
            return;
        }
        if (args.length >= 3 && args[2].equalsIgnoreCase("off")) {
            plugin.remove(target);
            plugin.messages().send(sender, "admin-cleared", Map.of("%player%", target.getName()));
            return;
        }
        Trail trail = args.length < 3 ? null : plugin.trails().get(args[2]);
        if (trail == null) {
            plugin.messages().send(sender, "unknown-trail");
            return;
        }
        plugin.equip(target, trail);
        plugin.messages().send(sender, "admin-set", Map.of("%player%", target.getName(), "%trail%", trail.display()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            options.addAll(List.of("off", "toggle", "visibility", "list"));
            if (admin) options.addAll(List.of("set", "reload"));
            if (sender instanceof Player player) {
                plugin.trails().all().stream().filter(trail -> plugin.canUse(player, trail)).forEach(trail -> options.add(trail.id()));
            }
        } else if (args[0].equalsIgnoreCase("set") && admin) {
            if (args.length == 2) Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
            if (args.length == 3) {
                options.add("off");
                plugin.trails().all().forEach(trail -> options.add(trail.id()));
            }
        }

        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
