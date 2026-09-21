package dev.pixie.trail;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All the trails, in the order of the file. */
public final class TrailLibrary {

    private final Map<String, Trail> byId = new LinkedHashMap<>();

    public TrailLibrary(List<Trail> trails) {
        for (Trail trail : trails) {
            byId.putIfAbsent(trail.id(), trail);
        }
    }

    public @Nullable Trail get(@Nullable String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Trail> all() {
        return byId.values();
    }

    public int size() {
        return byId.size();
    }
}
