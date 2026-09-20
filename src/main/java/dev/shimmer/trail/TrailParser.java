package dev.shimmer.trail;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Reads trails.yml. A trail or layer with a problem is skipped and the reason is logged. */
public final class TrailParser {

    /** Makes the data a particle carries from a block or item name, or null if there is no such block or item. */
    public interface Names {
        @Nullable Object block(String name);

        @Nullable Object item(String name);
    }

    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final int MAX_COUNT = 30;
    private static final int MAX_POINTS = 8;

    private TrailParser() {
    }

    public static List<Trail> parse(@Nullable ConfigurationSection root, Logger log, Names names) {
        List<Trail> trails = new ArrayList<>();
        ConfigurationSection section = root == null ? null : root.getConfigurationSection("trails");
        if (section == null) {
            log.warning("trails.yml has no 'trails' section.");
            return trails;
        }

        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (id.isEmpty() || entry == null) {
                log.warning("trails.yml: '" + key + "' is not a trail.");
                continue;
            }
            Trail trail = parseTrail(id, entry, log, names);
            if (trail != null) trails.add(trail);
        }
        return trails;
    }

    private static @Nullable Trail parseTrail(String id, ConfigurationSection entry, Logger log, Names names) {
        List<Trail.Layer> layers = new ArrayList<>();
        int number = 0;
        for (var raw : entry.getMapList("layers")) {
            number++;
            org.bukkit.configuration.file.YamlConfiguration wrapper = new org.bukkit.configuration.file.YamlConfiguration();
            raw.forEach((k, v) -> wrapper.set(String.valueOf(k), v));
            Trail.Layer layer = parseLayer(id, number, wrapper, log, names);
            if (layer != null) layers.add(layer);
        }
        if (layers.isEmpty()) {
            log.warning("trails.yml: " + id + " has no usable layers, skipping it.");
            return null;
        }

        int interval = Math.max(1, entry.getInt("interval", 3));
        String permission = entry.getString("permission", "shimmer.trail.{id}").replace("{id}", id);
        return new Trail(id, entry.getString("display", prettyName(id)), permission, interval,
                entry.getBoolean("moving", true), List.copyOf(layers));
    }

    private static @Nullable Trail.Layer parseLayer(String trail, int number, ConfigurationSection layer, Logger log, Names names) {
        String where = "trails.yml: " + trail + " layer " + number + " ";
        Particle particle = particle(layer.getString("particle"));
        if (particle == null) {
            log.warning(where + "has the particle '" + layer.getString("particle") + "', which does not exist. Skipping it.");
            return null;
        }

        Trail.Shape shape = Trail.Shape.SCATTER;
        String shapeName = layer.getString("shape", "SCATTER").toUpperCase(Locale.ROOT);
        try {
            shape = Trail.Shape.valueOf(shapeName);
        } catch (IllegalArgumentException ex) {
            log.warning(where + "has the shape '" + shapeName + "' (use SCATTER, RING, HELIX or ORBIT), using SCATTER.");
        }

        List<Object> variants = variants(particle, layer, where, log, names);
        if (variants == null) return null;

        List<? extends Number> offset = layer.getDoubleList("offset");
        double x = offset.size() > 0 ? offset.get(0).doubleValue() : 0;
        double y = offset.size() > 1 ? offset.get(1).doubleValue() : 0.05;
        double z = offset.size() > 2 ? offset.get(2).doubleValue() : 0;

        int count = Math.max(0, Math.min(MAX_COUNT, layer.getInt("count", 1)));
        int points = Math.max(1, Math.min(MAX_POINTS, layer.getInt("points", 4)));
        return new Trail.Layer(particle, shape, count, Math.max(0, layer.getDouble("spread", 0.25)),
                Math.max(0, layer.getDouble("height", 0.15)), x, y, z, Math.max(0, layer.getDouble("speed", 0)),
                Math.max(0, layer.getDouble("radius", 0.4)), points, Collections.unmodifiableList(variants));
    }

    /** What each puff of the layer carries, or null when the layer cannot be used. */
    private static @Nullable List<Object> variants(Particle particle, ConfigurationSection layer, String where, Logger log, Names names) {
        Class<?> type = particle.getDataType();
        List<Object> result = new ArrayList<>();

        if (particle == Particle.NOTE) {
            for (int note : layer.getIntegerList("notes")) result.add(Math.max(0, Math.min(24, note)));
            if (result.isEmpty()) result.add(null);
            return result;
        }
        if (type == Void.class) {
            result.add(null);
            return result;
        }

        if (type == Particle.DustOptions.class) {
            float size = (float) Math.max(0.1, Math.min(4, layer.getDouble("size", 1.0)));
            for (Color color : colors(layer.getStringList("colors"), where, log)) result.add(new Particle.DustOptions(color, size));
        } else if (type == Particle.DustTransition.class) {
            float size = (float) Math.max(0.1, Math.min(4, layer.getDouble("size", 1.0)));
            List<Color> from = colors(layer.getStringList("colors"), where, log);
            List<Color> to = colors(layer.getStringList("to"), where, log);
            if (to.isEmpty()) to = from;
            for (int i = 0; i < from.size(); i++) result.add(new Particle.DustTransition(from.get(i), to.get(i % to.size()), size));
        } else if (type == Color.class) {
            result.addAll(colors(layer.getStringList("colors"), where, log));
        } else if (type == org.bukkit.block.data.BlockData.class) {
            for (String name : layer.getStringList("block")) {
                Object data = names.block(name.toUpperCase(Locale.ROOT));
                if (data != null) {
                    result.add(data);
                } else {
                    log.warning(where + "has the block '" + name + "', which does not exist. Skipping it.");
                }
            }
        } else if (type == ItemStack.class) {
            for (String name : layer.getStringList("item")) {
                Object data = names.item(name.toUpperCase(Locale.ROOT));
                if (data != null) {
                    result.add(data);
                } else {
                    log.warning(where + "has the item '" + name + "', which does not exist. Skipping it.");
                }
            }
        } else {
            log.warning(where + "uses " + particle + ", which needs data Shimmer does not support. Skipping it.");
            return null;
        }

        if (result.isEmpty()) {
            log.warning(where + "has nothing for " + particle + " to carry (colors, block or item). Skipping it.");
            return null;
        }
        return result;
    }

    /** {@code #rrggbb}, {@code rrggbb} or a list of three numbers written as r,g,b. */
    private static List<Color> colors(List<String> texts, String where, Logger log) {
        List<Color> colors = new ArrayList<>();
        for (String text : texts) {
            Color color = color(text);
            if (color == null) {
                log.warning(where + "has the colour '" + text + "', which is not #rrggbb. Skipping it.");
            } else {
                colors.add(color);
            }
        }
        return colors;
    }

    static @Nullable Color color(String text) {
        String trimmed = text.trim();
        if (HEX.matcher(trimmed).matches()) {
            return Color.fromRGB(Integer.parseInt(trimmed.replace("#", ""), 16));
        }
        String[] parts = trimmed.split("[,:]");
        if (parts.length == 3) {
            try {
                int[] rgb = Arrays.stream(parts).mapToInt(part -> Integer.parseInt(part.trim())).toArray();
                for (int value : rgb) if (value < 0 || value > 255) return null;
                return Color.fromRGB(rgb[0], rgb[1], rgb[2]);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    static @Nullable Particle particle(@Nullable String name) {
        if (name == null) return null;
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String prettyName(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("[_-]+")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** Names the game knows. Only used on a server, where the registries exist. */
    public static Names realNames() {
        return new Names() {
            @Override
            public @Nullable Object block(String name) {
                Material material = Material.matchMaterial(name);
                return material != null && material.isBlock() ? material.createBlockData() : null;
            }

            @Override
            public @Nullable Object item(String name) {
                Material material = Material.matchMaterial(name);
                return material != null && material.isItem() ? new ItemStack(material) : null;
            }
        };
    }
}
