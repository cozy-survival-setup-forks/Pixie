package dev.pixie.trail;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrailParserTest {

    private static final TrailParser.Names ANYTHING = new TrailParser.Names() {
        @Override
        public Object block(String name) {
            return "block:" + name;
        }

        @Override
        public Object item(String name) {
            return "item:" + name;
        }
    };

    private static List<Trail> parse(String text, List<String> warnings) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        Logger log = new Logger("test", null) {
            @Override
            public void warning(String msg) {
                warnings.add(msg);
            }
        };
        return TrailParser.parse(yaml, log, ANYTHING);
    }

    @Test
    void aTrailIsReadWithItsLayers() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<Trail> trails = parse("""
                trails:
                  Pink_Mix:
                    display: "Pink Mix"
                    interval: 2
                    moving: false
                    layers:
                      - particle: dust
                        colors: ["#FFC0CB", "255:255:0"]
                        size: 1.5
                        count: 4
                        spread: 0.3
                        height: 0.2
                        offset: [0.1, 0.5, 0.2]
                """, warnings);

        assertTrue(warnings.isEmpty(), warnings.toString());
        Trail trail = trails.get(0);
        assertEquals("pink_mix", trail.id());
        assertEquals("Pink Mix", trail.display());
        assertEquals("pixie.trail.pink_mix", trail.permission());
        assertEquals(2, trail.interval());
        assertFalse(trail.movingOnly());

        Trail.Layer layer = trail.layers().get(0);
        assertEquals(Particle.DUST, layer.particle());
        assertEquals(4, layer.count());
        assertEquals(0.5, layer.y());
        assertEquals(2, layer.variants().size());
        Particle.DustOptions first = assertInstanceOf(Particle.DustOptions.class, layer.data(0));
        assertEquals(Color.fromRGB(255, 192, 203), first.getColor());
        assertEquals(1.5f, first.getSize());
    }

    @Test
    void puffsUseTheColoursOneAfterTheOther() throws Exception {
        Trail.Layer layer = parse("trails:\n  t:\n    layers:\n      - {particle: DUST, colors: ['#ff0000', '#00ff00', '#0000ff']}\n", new ArrayList<>()).get(0).layers().get(0);

        assertEquals(Color.RED, ((Particle.DustOptions) layer.data(0)).getColor());
        assertEquals(Color.LIME, ((Particle.DustOptions) layer.data(1)).getColor());
        assertEquals(Color.BLUE, ((Particle.DustOptions) layer.data(2)).getColor());
        assertEquals(Color.RED, ((Particle.DustOptions) layer.data(3)).getColor());
    }

    @Test
    void aTransitionFadesFromOneColourToTheNext() throws Exception {
        Trail.Layer layer = parse("trails:\n  t:\n    layers:\n      - {particle: DUST_COLOR_TRANSITION, colors: ['#ff0000'], to: ['#0000ff']}\n", new ArrayList<>()).get(0).layers().get(0);

        Particle.DustTransition data = assertInstanceOf(Particle.DustTransition.class, layer.data(0));
        assertEquals(Color.RED, data.getColor());
        assertEquals(Color.BLUE, data.getToColor());
    }

    @Test
    void particlesWithoutDataNeedNothing() throws Exception {
        Trail.Layer layer = parse("trails:\n  t:\n    layers:\n      - {particle: END_ROD, count: 2}\n", new ArrayList<>()).get(0).layers().get(0);

        assertNull(layer.data(0));
        assertNull(layer.data(7));
    }

    @Test
    void blocksAndItemsAreLookedUp() throws Exception {
        List<Trail> trails = parse("trails:\n  t:\n    layers:\n      - {particle: BLOCK, block: [stone, dirt]}\n      - {particle: ITEM, item: [diamond]}\n", new ArrayList<>());

        assertEquals("block:STONE", trails.get(0).layers().get(0).data(0));
        assertEquals("block:DIRT", trails.get(0).layers().get(0).data(1));
        assertEquals("item:DIAMOND", trails.get(0).layers().get(1).data(0));
    }

    @Test
    void badLayersAreSkippedAndTheTrailStaysIfOneIsLeft() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<Trail> trails = parse("""
                trails:
                  t:
                    layers:
                      - {particle: NOT_A_PARTICLE}
                      - {particle: DUST}
                      - {particle: DUST, colors: ['banana']}
                      - {particle: VIBRATION}
                      - {particle: END_ROD}
                  empty:
                    layers: []
                """, warnings);

        assertEquals(1, trails.size());
        assertEquals(1, trails.get(0).layers().size());
        assertEquals(Particle.END_ROD, trails.get(0).layers().get(0).particle());
        assertTrue(warnings.size() >= 5, warnings.toString());
    }

    @Test
    void limitsKeepATrailFromBeingHeavy() throws Exception {
        Trail.Layer layer = parse("trails:\n  t:\n    interval: 0\n    layers:\n      - {particle: END_ROD, count: 5000, points: 99}\n", new ArrayList<>()).get(0).layers().get(0);
        Trail trail = parse("trails:\n  t:\n    interval: 0\n    layers:\n      - {particle: END_ROD}\n", new ArrayList<>()).get(0);

        assertEquals(30, layer.count());
        assertEquals(8, layer.points());
        assertEquals(1, trail.interval());
    }

    @Test
    void anUnknownShapeFallsBackToScatter() throws Exception {
        List<String> warnings = new ArrayList<>();
        Trail.Layer layer = parse("trails:\n  t:\n    layers:\n      - {particle: END_ROD, shape: zigzag}\n", warnings).get(0).layers().get(0);

        assertEquals(Trail.Shape.SCATTER, layer.shape());
        assertEquals(1, warnings.size());
    }

    @Test
    void notesCarryTheirNumber() throws Exception {
        Trail.Layer layer = parse("trails:\n  t:\n    layers:\n      - {particle: NOTE, notes: [3, 99]}\n", new ArrayList<>()).get(0).layers().get(0);

        assertEquals(3, layer.data(0));
        assertEquals(24, layer.data(1));
    }

    // ---- the trails that come with the plugin ----

    private static List<Trail> shipped(List<String> warnings) throws Exception {
        var stream = TrailParserTest.class.getResourceAsStream("/trails.yml");
        assertNotNull(stream);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        Logger log = new Logger("shipped", null) {
            @Override
            public void warning(String msg) {
                warnings.add(msg);
            }
        };
        return TrailParser.parse(yaml, log, ANYTHING);
    }

    @Test
    void allTheShippedTrailsLoadWithoutAProblem() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<Trail> trails = shipped(warnings);

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertTrue(trails.size() >= 30, "only " + trails.size() + " trails");
        for (Trail trail : trails) {
            assertFalse(trail.layers().isEmpty(), trail.id());
            assertFalse(trail.display().isEmpty(), trail.id());
            assertTrue(trail.layers().size() <= 3, trail.id() + " sends too many packets");
        }
    }

    @Test
    void theYellowPinkTrailHasItsThreeColours() throws Exception {
        Trail trail = shipped(new ArrayList<>()).stream().filter(t -> t.id().equals("yellow_pink")).findFirst().orElseThrow();

        assertEquals(3, trail.layers().get(0).variants().size());
        assertEquals(Color.fromRGB(255, 192, 203), ((Particle.DustOptions) trail.layers().get(0).data(0)).getColor());
        assertEquals(Color.fromRGB(255, 242, 0), ((Particle.DustOptions) trail.layers().get(0).data(1)).getColor());
    }

    @Test
    void theIdsAreUnique() throws Exception {
        List<Trail> trails = shipped(new ArrayList<>());

        assertEquals(trails.size(), trails.stream().map(Trail::id).distinct().count());
    }
}
