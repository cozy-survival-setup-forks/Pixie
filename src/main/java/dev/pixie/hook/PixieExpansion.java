package dev.pixie.hook;

import dev.pixie.PixiePlugin;
import dev.pixie.trail.Trail;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * %pixie_id%, %pixie_display%, %pixie_paused%, %pixie_hidden%, and for menus %pixie_equipped_&lt;id&gt;% and
 * %pixie_owned_&lt;id&gt;%.
 */
public final class PixieExpansion extends PlaceholderExpansion {

    private final PixiePlugin plugin;

    public PixieExpansion(PixiePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pixie";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        String request = params.toLowerCase(Locale.ROOT);
        Trail picked = plugin.trails().get(plugin.prefs().trail(player));

        if (request.equals("id")) return picked == null ? "none" : picked.id();
        if (request.equals("display")) return picked == null ? "None" : picked.display();
        if (request.equals("paused")) return String.valueOf(plugin.prefs().paused(player));
        if (request.equals("hidden")) return String.valueOf(plugin.prefs().hidden(player));
        if (request.startsWith("equipped_")) {
            return String.valueOf(picked != null && picked.id().equals(request.substring(9)));
        }
        if (request.startsWith("owned_")) {
            Trail trail = plugin.trails().get(request.substring(6));
            return String.valueOf(trail != null && plugin.canUse(player, trail));
        }
        return null;
    }
}
