package dev.pixie.trail;

import org.bukkit.Particle;

import java.util.List;

/**
 * One trail of trails.yml: a few layers of particles that are sent every {@code interval} ticks while the player
 * moves. Everything about a trail is worked out when the file is read, so sending it later needs no thinking.
 */
public record Trail(String id, String display, String permission, int interval, boolean movingOnly, List<Layer> layers) {

    /** How the particles of a layer are placed around the player. */
    public enum Shape {
        /** A cloud around the feet. */
        SCATTER,
        /** A few points on a small circle that turns. */
        RING,
        /** Two points that climb the body in a spiral. */
        HELIX,
        /** One point that circles the waist. */
        ORBIT
    }

    /**
     * One kind of particle of a trail.
     *
     * @param variants what the particle carries, one after the other on every puff (a colour, a block...). A particle
     *                 that carries nothing has a single null. Never empty.
     */
    public record Layer(Particle particle, Shape shape, int count, double spread, double height, double x, double y,
                        double z, double speed, double radius, int points, List<Object> variants) {

        /** What the particle carries on puff number {@code puff}. */
        public Object data(int puff) {
            return variants.get(Math.floorMod(puff, variants.size()));
        }
    }
}
