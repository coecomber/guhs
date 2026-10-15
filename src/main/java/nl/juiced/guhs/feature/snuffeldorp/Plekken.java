package nl.juiced.guhs.feature.snuffeldorp;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Eiland;
import org.slf4j.Logger;

/**
 * The named spots of Snuffeldorp that the story needs and that are not a resident or a scent source: where a scene is
 * anchored, where something starts when a dog walks by, and the line behind the roadblock. They are DATA, written
 * together with the island by tools/features/snuffel_dorp_bouw.py: {@code data/guhs/snuffeldorp/dorp.json}
 * <pre>
 * {"plekken": {"strand": [x, y, z], "strandpoort": [..], "emmer": [..], "plein": [..], "weipoort": [..], "wei": [..],
 *              "boom": [..], "haven": [..], "versperring": [..], "dokter": [..]},      blocks, relative to the island's corner
 *  "grens_z": 47,                  north of this line (relative z smaller than this) the island is closed for a Snuffelpup
 *  "gesprekken": {"redder.welkom": 3, ...}}      how many pages every conversation has (quest.guhs.snuffeldorp.&lt;id&gt;.&lt;i&gt;)
 * </pre>
 * The game tests mark their own little island and give it its own spots ({@link #test}).
 */
public record Plekken(Map<String, BlockPos> plekken, int grensZ) {
    private static final Logger LOG = LogUtils.getLogger();
    static final String PAD = "/data/guhs/snuffeldorp/dorp.json";
    public static final String STRAND = "strand", STRANDPOORT = "strandpoort", EMMER = "emmer", PLEIN = "plein", WEIPOORT = "weipoort", WEI = "wei",
            BOOM = "boom", HAVEN = "haven", VERSPERRING = "versperring", DOKTER = "dokter";
    /** No line: nothing of the island is closed. */
    public static final int GEEN_GRENS = Integer.MIN_VALUE;

    private static Plekken echt;
    private static final Map<String, Integer> GESPREKKEN = new HashMap<>();
    private static final Map<Long, Plekken> TEST = new ConcurrentHashMap<>();

    /** The spots of the island of this jar. */
    public static synchronized Plekken echt() {
        if (echt == null) {
            Map<String, BlockPos> plekken = new HashMap<>();
            int grens = GEEN_GRENS;
            try (InputStream in = Plekken.class.getResourceAsStream(PAD)) {
                if (in == null) {
                    LOG.error("Snuffeldorp: {} is missing (run tools/make_resources.py)", PAD);
                } else {
                    try (Reader lezer = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        JsonObject o = JsonParser.parseReader(lezer).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("plekken").entrySet()) {
                            JsonArray a = e.getValue().getAsJsonArray();
                            plekken.put(e.getKey(), new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
                        }
                        grens = o.get("grens_z").getAsInt();
                        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("gesprekken").entrySet()) {
                            GESPREKKEN.put(e.getKey(), e.getValue().getAsInt());
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Snuffeldorp: could not read {}", PAD, e);
            }
            echt = new Plekken(Map.copyOf(plekken), grens);
        }
        return echt;
    }

    /**
     * The spots of this island: the real island's, or a test island's own (see {@link #test}). Null: a test island that
     * is not Snuffeldorp (the kern's game tests mark bare islands: nothing of the village's story happens there).
     */
    @Nullable
    public static Plekken van(Eiland.Plaats plaats) {
        return plaats.test() ? TEST.get(plaats.oorsprong().asLong()) : echt();
    }

    /** (Game tests) the spots of a test island; forget them with {@link #testWeg}. */
    public static void test(Eiland.Plaats plaats, Plekken plekken) {
        TEST.put(plaats.oorsprong().asLong(), plekken);
    }

    public static void testWeg(Eiland.Plaats plaats) {
        TEST.remove(plaats.oorsprong().asLong());
    }

    /** How many pages this conversation has (0: it does not exist). */
    public static int paginas(String gesprek) {
        echt();
        return GESPREKKEN.getOrDefault(gesprek, 0);
    }

    /** Every conversation of the data (for the tests). */
    public static Map<String, Integer> gesprekken() {
        echt();
        return Map.copyOf(GESPREKKEN);
    }

    public boolean heeft(String naam) {
        return plekken.containsKey(naam);
    }

    /** The block of this spot in the world (null: the data does not name it). */
    @Nullable
    public BlockPos wereld(Eiland.Plaats plaats, String naam) {
        BlockPos rel = plekken.get(naam);
        return rel == null ? null : plaats.wereld(rel);
    }

    /** The middle of this spot's floor in the world. */
    @Nullable
    public Vec3 midden(Eiland.Plaats plaats, String naam) {
        BlockPos p = wereld(plaats, naam);
        return p == null ? null : Vec3.atBottomCenterOf(p);
    }

    /** Is this spot (world) behind the roadblock? */
    public boolean dicht(Eiland.Plaats plaats, Vec3 pos) {
        return grensZ != GEEN_GRENS && pos.z - plaats.oorsprong().getZ() < grensZ;
    }
}
