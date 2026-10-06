package nl.juiced.guhs.feature.ringh6;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h6): where everything is on a Frituurberg. The mountain is built by tools/features/ring_h6_bouw.py, which also
 * writes the named spots and routes of its template to data/guhs/ringh6/berg.json (and those of the small test mountain to
 * berg_test.json); this class reads them and turns them into world positions for a copy ({@link Kopie}), whatever way the
 * copy is turned.
 * <p>
 * The spots (template coordinates; "feet" = the air cell you stand in): kamp, kamp_vuur, and per stage n = 1..3: richel_n
 * (feet), vuur_n (the Rustvuurtje), slot_n (the lock of the cage), kooi_n (feet, inside the cage), haak_n (the hook block),
 * start_n (feet, where the rope is thrown from), boven_n (feet, where the rope puts you); spleet (feet, the landing in
 * front of the cleft), rand (feet, the balcony over the pool: the anchor of the finale), frituur (the pool). The route
 * "sam": from the Derde Richel through the cleft to the balcony.
 */
public final class Berg {
    public static final String STRUCTUUR = "frituurberg";

    /** The spots and routes of one template. */
    public record Gegevens(Map<String, BlockPos> plekken, Map<String, List<Vec3>> routes, BlockPos grootte, BlockPos anker) {
        public BlockPos plek(String naam) {
            BlockPos pos = plekken.get(naam);
            if (pos == null) {
                throw new IllegalArgumentException("the Frituurberg has no spot '" + naam + "'");
            }
            return pos;
        }
    }

    private static Gegevens echt, test;

    /** The real mountain's spots (data/guhs/ringh6/berg.json). */
    public static synchronized Gegevens echt() {
        if (echt == null) {
            echt = lees("/data/guhs/ringh6/berg.json");
        }
        return echt;
    }

    /** The test mountain's spots (data/guhs/ringh6/berg_test.json; template ringh6_test_berg). */
    public static synchronized Gegevens test() {
        if (test == null) {
            test = lees("/data/guhs/ringh6/berg_test.json");
        }
        return test;
    }

    private static Gegevens lees(String pad) {
        try (InputStream in = Berg.class.getResourceAsStream(pad)) {
            if (in == null) {
                throw new IllegalStateException(pad + " is missing (tools/features/ring_h6.py writes it)");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, BlockPos> plekken = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("plekken").entrySet()) {
                plekken.put(e.getKey(), pos(e.getValue().getAsJsonArray()));
            }
            Map<String, List<Vec3>> routes = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("routes").entrySet()) {
                List<Vec3> punten = new ArrayList<>();
                for (JsonElement punt : e.getValue().getAsJsonArray()) {
                    JsonArray a = punt.getAsJsonArray();
                    punten.add(new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()));
                }
                routes.put(e.getKey(), List.copyOf(punten));
            }
            return new Gegevens(Map.copyOf(plekken), Map.copyOf(routes), pos(root.getAsJsonArray("grootte")), pos(root.getAsJsonArray("anker")));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("can't read " + pad, ex);
        }
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    /**
     * One mountain in the world: its level, the world position of the template's anchor and the way it is turned.
     * {@link #wereld} / {@link #midden} / {@link #route} give world positions of the named spots.
     */
    public record Kopie(ServerLevel level, Gegevens g, BlockPos anker, Rotation draai) {
        /** The world position of a block of the template. */
        public BlockPos wereld(BlockPos lokaal) {
            return anker.offset(StructureTemplate.transform(lokaal.subtract(g.anker()), Mirror.NONE, draai, BlockPos.ZERO));
        }

        public BlockPos wereld(String plek) {
            return wereld(g.plek(plek));
        }

        /** The world position of a point of the template (not a block: any spot in it). */
        public Vec3 wereld(Vec3 lokaal) {
            return Cutscene.wereld(anker, draai, lokaal.subtract(g.anker().getX(), g.anker().getY(), g.anker().getZ()));
        }

        /** The middle of the floor of a named feet cell. */
        public Vec3 midden(String plek) {
            return Vec3.atBottomCenterOf(wereld(plek));
        }

        public List<Vec3> route(String naam) {
            List<Vec3> uit = new ArrayList<>();
            for (Vec3 punt : g.routes().getOrDefault(naam, List.of())) {
                uit.add(wereld(punt));
            }
            return uit;
        }

        /** Is this spot on (or within {@code rand} blocks of) the mountain's template box? */
        public boolean binnen(Vec3 pos, double rand) {
            BlockPos a = wereld(BlockPos.ZERO), b = wereld(g.grootte().offset(-1, -1, -1));
            return pos.x >= Math.min(a.getX(), b.getX()) - rand && pos.x <= Math.max(a.getX(), b.getX()) + 1 + rand
                    && pos.z >= Math.min(a.getZ(), b.getZ()) - rand && pos.z <= Math.max(a.getZ(), b.getZ()) + 1 + rand
                    && pos.y >= Math.min(a.getY(), b.getY()) - rand && pos.y <= Math.max(a.getY(), b.getY()) + 1 + rand;
        }

        /** How high up the climb this height is: 0 at the camp, 1 on the Derde Richel. */
        public double hoogte(double y) {
            double onder = wereld("kamp").getY(), boven = wereld("richel_3").getY();
            return boven <= onder ? 0 : Math.max(0, Math.min(1, (y - onder) / (boven - onder)));
        }
    }

    private static final Map<StructureStart, Kopie> ECHTE = Collections.synchronizedMap(new WeakHashMap<>());
    /** (tests) mountains put down by hand: level -> the copy. */
    private static final Map<ServerLevel, List<Kopie>> TEST = Collections.synchronizedMap(new WeakHashMap<>());
    /** The mountain each player was on at their last look (asked every tick; looked up once a second). */
    private static final Map<UUID, Kopie> VAN = new ConcurrentHashMap<>();

    /** The mountain this spot is on (within 4 blocks of its box), or null. Loaded chunks only. */
    @Nullable
    public static Kopie bij(ServerLevel level, BlockPos pos) {
        Vec3 v = Vec3.atCenterOf(pos);
        for (Kopie k : TEST.getOrDefault(level, List.of())) {
            if (k.binnen(v, 4)) {
                return k;
            }
        }
        // (cheap first: the sluier of the mountain knows where the copies are; only near one the chunks are looked at)
        boolean dichtbij = false;
        for (Sluiers.Zone zone : Sluiers.zones(level, STRUCTUUR)) {
            dichtbij |= pos.getX() >= zone.x0() - 8 && pos.getX() <= zone.x1() + 8 && pos.getZ() >= zone.z0() - 8 && pos.getZ() <= zone.z1() + 8;
        }
        if (!dichtbij) {
            return null;
        }
        StructureStart start = Bezetting.start(level, STRUCTUUR, pos);
        if (start == null) {
            return null;
        }
        Kopie k = ECHTE.get(start);
        if (k == null) {
            Gegevens g = echt();
            BlockPos anker = Kopieen.wereld(start, null, g.anker());
            if (anker == null) {
                return null;
            }
            k = new Kopie(level, g, anker, Kopieen.draai(start, null));
            ECHTE.put(start, k);
        }
        return k.binnen(v, 4) ? k : null;
    }

    /** (once a second) looks up the mountain this player is on and remembers it for {@link #van}. */
    @Nullable
    static Kopie zoek(ServerPlayer p) {
        Kopie k = VAN.get(p.getUUID());
        if (k != null && k.level() == p.level() && k.binnen(p.position(), 4)) {
            return k;
        }
        k = bij(p.level(), p.blockPosition());
        if (k == null) {
            VAN.remove(p.getUUID());
        } else {
            VAN.put(p.getUUID(), k);
        }
        return k;
    }

    /** The mountain this player is on, as of the last {@link #zoek} (cheap: for every tick). */
    @Nullable
    public static Kopie van(ServerPlayer p) {
        Kopie k = VAN.get(p.getUUID());
        return k != null && k.level() == p.level() && k.binnen(p.position(), 4) ? k : null;
    }

    static void vergeet(UUID speler) {
        VAN.remove(speler);
    }

    /** (tests, and the dev command) a mountain put down by hand: the template's (0, 0, 0) is at {@code nul}, unturned. */
    public static Kopie zetTest(ServerLevel level, Gegevens g, BlockPos nul) {
        Kopie k = new Kopie(level, g, nul.offset(g.anker()), Rotation.NONE);
        TEST.computeIfAbsent(level, l -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(k);
        return k;
    }

    public static void wisTest(ServerLevel level) {
        TEST.remove(level);
        VAN.clear();
    }

    private Berg() {
    }
}
