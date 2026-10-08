package nl.juiced.guhs.feature.snuffeldorp;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Honden;
import org.slf4j.Logger;

/**
 * (1.4.1) The island seen from above: one character per column, written together with the island by
 * tools/features/snuffel_dorp_bouw.py ({@code kaart}) from the very blocks it builds:
 * {@code data/guhs/snuffeldorp/kaart.json} = {@code {"breed": 168, "diep": 192, "rijen": ["~~~eeLLL...", ...]}} (a row per
 * z, a character per x, both relative to the island's corner). It is what tells the SEA from the island's own water, and
 * the closed ground from the start zone:
 * <ul>
 *   <li>{@code L} land: a column of the start zone a dog can WALK to from the beach. Only here and on {@code S} a dog is
 *   ever put back.</li>
 *   <li>{@code S} steiger: the same, but a deck over the water (the jetty, the captain's boat): standing on it is land,
 *   the water under it is sea.</li>
 *   <li>{@code x} dicht: the closed part AND its wall: the hills, the ridge's top, its row of rocks in the sea, the
 *   roadblock and the pocket behind it. {@link Wegversperring} puts a Snuffelpup that stands here back.</li>
 *   <li>{@code e} eiland: the rest of the island: the pond, the well, the moestuin's ditch, walls, roofs, trees, and the
 *   WET EDGE (the sand ledge and the paddling strip around the coast, where the sea is at most one block deep). Water
 *   here is never sea.</li>
 *   <li>{@code ~} zee: everything else, the harbour basin too (a dog that falls off the jetty cannot climb out there),
 *   and everything outside the map.</li>
 * </ul>
 * {@link Zee} uses it for the rule "a dog does not swim in the sea". An island without a map (the game tests' other
 * islands, or a jar whose file is broken) simply has no such rule.
 */
public record Landkaart(int breed, int diep, String[] rijen) {
    private static final Logger LOG = LogUtils.getLogger();
    static final String PAD = "/data/guhs/snuffeldorp/kaart.json", TEST_PAD = "/data/guhs/snuffeldorp/kaart_test.json";
    public static final char ZEE = '~', LAND = 'L', DEK = 'S', EILAND = 'e', DICHT = 'x';

    private static Landkaart echt;
    private static boolean gelezen;
    private static final Map<Long, Landkaart> TEST = new ConcurrentHashMap<>();

    /** The map of the island of this jar (null: the file is missing or broken; said once in the log). */
    @Nullable
    public static synchronized Landkaart echt() {
        if (!gelezen) {
            gelezen = true;
            echt = lees(PAD);
            if (echt == null) {
                LOG.error("Snuffeldorp: {} is missing or broken (run tools/make_resources.py): the sea is not guarded", PAD);
            }
        }
        return echt;
    }

    /** A map from the mod's own resources (null: not there, or not a map). */
    @Nullable
    public static Landkaart lees(String pad) {
        try (InputStream in = Landkaart.class.getResourceAsStream(pad)) {
            if (in == null) {
                return null;
            }
            try (Reader lezer = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(lezer).getAsJsonObject();
                int breed = o.get("breed").getAsInt(), diep = o.get("diep").getAsInt();
                JsonArray a = o.getAsJsonArray("rijen");
                String[] rijen = new String[a.size()];
                for (int i = 0; i < rijen.length; i++) {
                    rijen[i] = a.get(i).getAsString();
                    if (rijen[i].length() != breed) {
                        return null;
                    }
                }
                return rijen.length == diep && diep > 0 && breed > 0 ? new Landkaart(breed, diep, rijen) : null;
            }
        } catch (Exception e) {
            LOG.error("Snuffeldorp: could not read {}", pad, e);
            return null;
        }
    }

    /** The map of this island: the real island's, or the one a game test gave its own island ({@link #test}); else null. */
    @Nullable
    public static Landkaart van(Eiland.Plaats plaats) {
        return plaats.test() ? TEST.get(plaats.oorsprong().asLong()) : echt();
    }

    /** (Game tests) the map of a test island; forget it with {@link #testWeg}. */
    public static void test(Eiland.Plaats plaats, Landkaart kaart) {
        TEST.put(plaats.oorsprong().asLong(), kaart);
    }

    public static void testWeg(Eiland.Plaats plaats) {
        TEST.remove(plaats.oorsprong().asLong());
    }

    /** What this column is (relative to the island's corner); the sea outside the map. */
    public char vak(int x, int z) {
        return x < 0 || z < 0 || x >= breed || z >= diep ? ZEE : rijen[z].charAt(x);
    }

    /** What the column under this spot (world) is. */
    public char vak(Eiland.Plaats plaats, Vec3 pos) {
        return vak((int) Math.floor(pos.x - plaats.oorsprong().getX()), (int) Math.floor(pos.z - plaats.oorsprong().getZ()));
    }

    /** Is water at this spot the sea (and not the pond, the well, the wet edge of the beach)? */
    public boolean zee(Eiland.Plaats plaats, Vec3 pos) {
        char c = vak(plaats, pos);
        return c == ZEE || c == DEK;
    }

    /** Is the ground at this spot a place to put a dog back on (walkable land of the start zone, or a deck)? */
    public boolean veilig(Eiland.Plaats plaats, Vec3 pos) {
        char c = vak(plaats, pos);
        return c == LAND || c == DEK;
    }

    /**
     * Does a dog at this spot stand on closed ground? Asked for the dog's SOUTHERN edge (the roadblock closes the island
     * to the north): a dog that leans against the south side of the roadblock or of the ridge is not behind it, a dog
     * that leans against their north side is.
     */
    public boolean dicht(Eiland.Plaats plaats, Vec3 pos) {
        return vak(plaats, pos.add(0, 0, Honden.BREEDTE / 2.0)) == DICHT;
    }
}
