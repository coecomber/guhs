package nl.juiced.guhs.feature.snuffel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.storage.GuhSavedData;
import org.slf4j.Logger;

/**
 * Het Snuffeleiland itself: ONE fixed island in the dimension {@code guhs:snuffeleiland} (a flat sea without anything
 * else: tools/features/snuffel.py). What the island is made of is DATA, {@code data/guhs/snuffel/eiland.json} (read from
 * the mod's own resources, written by the python module that builds the island):
 * <pre>
 * {"versie": 1,                                  a higher number stamps the island again on a live server
 *  "oorsprong": [x, y, z],                       the min corner of the island's box in the dimension
 *  "maat": [sx, sy, sz],                         the box (put back to sea and sky before a new stamp)
 *  "stukken": [{"template": "guhs:snuffel/eiland", "plek": [x, y, z]}],     templates, corners relative to oorsprong
 *  "strand": {"plek": [x, y, z], "yaw": 0},      where you wash ashore the first time (feet; relative, like every plek)
 *  "haven":  {"plek": [x, y, z], "yaw": 0},      where the captain's boat lands later
 *  "boom": [x, y, z], "boom_draai": 0,           the tree's foot (a block), and how the growth scene is turned (0..3)
 *  "grens": 10,                                  how far outside the box a dog may swim before the current brings it back
 *  "bewoners": [{"sleutel": "dokter", "bewoner": "dokter", "plek": [..], "yaw": 90, "houding": "zit"},
 *               {"sleutel": "buur1", "ras": "corgi", "kleur": "sable", "pup": false, "plek": [..], "yaw": 0}],
 *  "geuren": [{"id": "botje", "soort": "eten", "rang": 1, "icoon": "minecraft:bone"}],
 *  "geurbronnen": [{"id": "botje_strand", "geur": "botje", "plek": [x, y, z], "graven": true, "bereik": 24}]}
 * </pre>
 * The kern ships a small flat test island; the island slice replaces the file and the templates. Residents and the tree
 * are not part of a template: {@link #bewoon} keeps exactly one of each on its spot (also after a new stamp).
 * <p>
 * The game test server has no datapack dimensions: a test marks its own island ({@link #test}) with its own
 * {@link Opzet}; everything here and in the rest of the package works on a {@link Plaats} (a level + a corner + an opzet).
 * On the real island the whole dimension counts as "on the island"; on a test island its box plus the grens.
 */
public final class Eiland {
    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("snuffeleiland"));
    static final String PAD = "/data/guhs/snuffel/eiland.json";
    /** The flat sea of the dimension: the top water block, the top of the sand under it, the top of the stone under that. */
    public static final int ZEE = 62, ZAND = 43, STEEN = 39;
    /** The tag (persistent data) of everything {@link #bewoon} puts on an island: its key and the stamp it belongs to. */
    public static final String TAG_SLEUTEL = "guhs_snuffel_plek", TAG_VERSIE = "guhs_snuffel_versie";
    public static final String BOOM_SLEUTEL = "@boom";

    public record Stuk(Identifier template, BlockPos plek) {
    }

    public record Punt(Vec3 plek, float yaw) {
    }

    /** A resident on its spot: a named one ({@code bewoner}) or any dog (ras + kleur + pup). */
    public record BewonerPlek(String sleutel, String bewoner, String ras, String kleur, boolean pup, Vec3 plek, float yaw, String houding) {
    }

    public record BronPlek(String id, String geur, BlockPos plek, boolean graven, int bereik) {
    }

    public record GeurDef(String id, String soort, int rang, String icoon) {
    }

    /** The island as data (every position relative to the island's corner). */
    public record Opzet(int versie, BlockPos oorsprong, Vec3i maat, List<Stuk> stukken, Punt strand, Punt haven, BlockPos boom, int boomDraai, int grens,
                        List<BewonerPlek> bewoners, List<BronPlek> bronnen, List<GeurDef> geuren) {
    }

    /** An island that exists: the level it is in, the world position of its corner, and what it is made of. */
    public record Plaats(ServerLevel level, BlockPos oorsprong, Opzet opzet, boolean test) {
        public Vec3 wereld(Vec3 rel) {
            return rel.add(oorsprong.getX(), oorsprong.getY(), oorsprong.getZ());
        }

        public BlockPos wereld(BlockPos rel) {
            return rel.offset(oorsprong);
        }

        public Vec3 relatief(Vec3 wereld) {
            return wereld.subtract(oorsprong.getX(), oorsprong.getY(), oorsprong.getZ());
        }

        /** The island's box in the world. */
        public AABB doos() {
            return Stempel.doos(oorsprong, opzet.maat());
        }

        /** How far outside the box (sideways) this spot is; 0 inside. */
        public double buiten(Vec3 pos) {
            AABB d = doos();
            double dx = Math.max(Math.max(d.minX - pos.x, pos.x - d.maxX), 0), dz = Math.max(Math.max(d.minZ - pos.z, pos.z - d.maxZ), 0);
            return Math.max(dx, dz);
        }

        /** The tree's foot (the block it stands on is one lower). */
        public BlockPos boom() {
            return wereld(opzet.boom());
        }

        public Vec3 strand() {
            return wereld(opzet.strand().plek());
        }

        public Vec3 haven() {
            return wereld(opzet.haven().plek());
        }

        /** The same island (a record compares its lists too: this is cheaper and all that is needed). */
        public boolean zelfde(@Nullable Plaats ander) {
            return ander != null && ander.level == level && ander.oorsprong.equals(oorsprong);
        }
    }

    private static Opzet opzet;
    @Nullable
    private static Plaats echt;
    private static final List<Plaats> TEST = new CopyOnWriteArrayList<>();
    /** How often in a row a resident was not found on its spot (it is made at the second miss). */
    private static final Map<String, Integer> GEMIST = new HashMap<>();

    private Eiland() {
    }

    // =====================================================================================================================
    // the data
    // =====================================================================================================================

    /** The island of this jar. */
    public static synchronized Opzet opzet() {
        if (opzet == null) {
            Opzet o = null;
            try (InputStream in = Eiland.class.getResourceAsStream(PAD)) {
                if (in == null) {
                    LOG.error("Snuffeleiland: {} is missing (run tools/make_resources.py)", PAD);
                } else {
                    try (Reader lezer = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        o = lees(JsonParser.parseReader(lezer).getAsJsonObject());
                    }
                }
            } catch (Exception e) {
                LOG.error("Snuffeleiland: could not read {}", PAD, e);
            }
            opzet = o != null ? o : new Opzet(0, new BlockPos(-8, ZEE + 1, -8), new Vec3i(16, 8, 16), List.of(), new Punt(new Vec3(8, 0, 8), 0f),
                    new Punt(new Vec3(8, 0, 8), 0f), new BlockPos(8, 0, 4), 0, 8, List.of(), List.of(), List.of());
        }
        return opzet;
    }

    /** An island from its json (see the class comment). */
    public static Opzet lees(JsonObject o) {
        List<Stuk> stukken = new ArrayList<>();
        for (JsonElement e : lijst(o, "stukken")) {
            JsonObject x = e.getAsJsonObject();
            stukken.add(new Stuk(Identifier.parse(x.get("template").getAsString()), blok(x.getAsJsonArray("plek"))));
        }
        List<BewonerPlek> bewoners = new ArrayList<>();
        for (JsonElement e : lijst(o, "bewoners")) {
            JsonObject x = e.getAsJsonObject();
            bewoners.add(new BewonerPlek(x.get("sleutel").getAsString(), tekst(x, "bewoner", ""), tekst(x, "ras", ""), tekst(x, "kleur", ""),
                    x.has("pup") && x.get("pup").getAsBoolean(), punt(x.getAsJsonArray("plek")), x.has("yaw") ? x.get("yaw").getAsFloat() : 0f,
                    tekst(x, "houding", "")));
        }
        List<BronPlek> bronnen = new ArrayList<>();
        for (JsonElement e : lijst(o, "geurbronnen")) {
            JsonObject x = e.getAsJsonObject();
            bronnen.add(new BronPlek(x.get("id").getAsString(), x.get("geur").getAsString(), blok(x.getAsJsonArray("plek")),
                    !x.has("graven") || x.get("graven").getAsBoolean(), x.has("bereik") ? x.get("bereik").getAsInt() : Geurbronnen.BEREIK));
        }
        List<GeurDef> geuren = new ArrayList<>();
        for (JsonElement e : lijst(o, "geuren")) {
            JsonObject x = e.getAsJsonObject();
            geuren.add(new GeurDef(x.get("id").getAsString(), x.get("soort").getAsString(), x.has("rang") ? x.get("rang").getAsInt() : 1,
                    tekst(x, "icoon", "minecraft:bone")));
        }
        BlockPos m = blok(o.getAsJsonArray("maat"));
        return new Opzet(o.get("versie").getAsInt(), blok(o.getAsJsonArray("oorsprong")), new Vec3i(m.getX(), m.getY(), m.getZ()), List.copyOf(stukken),
                puntMetYaw(o.getAsJsonObject("strand")), puntMetYaw(o.getAsJsonObject(o.has("haven") ? "haven" : "strand")), blok(o.getAsJsonArray("boom")),
                o.has("boom_draai") ? o.get("boom_draai").getAsInt() : 0, o.has("grens") ? o.get("grens").getAsInt() : 10, List.copyOf(bewoners),
                List.copyOf(bronnen), List.copyOf(geuren));
    }

    private static JsonArray lijst(JsonObject o, String key) {
        return o.has(key) ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static String tekst(JsonObject o, String key, String anders) {
        return o.has(key) ? o.get(key).getAsString() : anders;
    }

    private static BlockPos blok(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static Vec3 punt(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    private static Punt puntMetYaw(JsonObject o) {
        return new Punt(punt(o.getAsJsonArray("plek")), o.has("yaw") ? o.get("yaw").getAsFloat() : 0f);
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /** The real island (null on a server without the dimension, like the game test server). */
    @Nullable
    public static Plaats echt(MinecraftServer server) {
        ServerLevel level = server.getLevel(DIM);
        if (level == null) {
            return null;
        }
        if (echt == null || echt.level() != level) {
            echt = new Plaats(level, opzet().oorsprong(), opzet(), false);
        }
        return echt;
    }

    /** The island players travel to: the real one, else (game tests) the island of a test. */
    @Nullable
    public static Plaats plaats(MinecraftServer server) {
        Plaats p = echt(server);
        return p != null ? p : TEST.isEmpty() ? null : TEST.get(0);
    }

    /** The island this spot is on (null: none). */
    @Nullable
    public static Plaats van(Level level, Vec3 pos) {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        if (level.dimension() == DIM) {
            return echt(sl.getServer());
        }
        for (Plaats t : TEST) {
            if (t.level() == level && t.buiten(pos) <= t.opzet().grens() && pos.y >= t.oorsprong().getY() - 8
                    && pos.y <= t.oorsprong().getY() + t.opzet().maat().getY() + 24) {
                return t;
            }
        }
        return null;
    }

    @Nullable
    public static Plaats van(@Nullable Entity e) {
        return e == null ? null : van(e.level(), e.position());
    }

    /** On the island (server side; a client asks {@link #inClient}). */
    public static boolean in(@Nullable Entity e) {
        return van(e) != null;
    }

    public static boolean in(Level level, BlockPos pos) {
        return van(level, Vec3.atCenterOf(pos)) != null;
    }

    /** Client: is this the island's dimension? */
    public static boolean inClient(Level level) {
        return level.dimension() == DIM;
    }

    /** (Game tests) marks an island in a test's own level; forget it with {@link #testWeg}. */
    public static Plaats test(ServerLevel level, BlockPos oorsprong, Opzet opzet) {
        Plaats p = new Plaats(level, oorsprong.immutable(), opzet, true);
        TEST.add(p);
        return p;
    }

    public static void testWeg(Plaats p) {
        TEST.removeIf(t -> t.zelfde(p));
    }

    // =====================================================================================================================
    // the stamp
    // =====================================================================================================================

    /** Which version of the island stands in a level. */
    public static final class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("snuffel_eiland", Stand::new, Stand::load, Stand::save);
        int versie;

        private static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            s.versie = tag.getIntOr("Versie", 0);
            return s;
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("Versie", versie);
            return t;
        }
    }

    /** The version that stands on this island (a test island always "stands": the test's own template is the island). */
    public static int gebouwd(Plaats p) {
        return p.test() ? p.opzet().versie() : p.level().getDataStorage().computeIfAbsent(Stand.TYPE).versie;
    }

    /**
     * Makes sure the island stands: the first time, and again when the jar holds a newer island, the box is put back to
     * sea and sky and the templates are stamped. Players who are on the island during a new stamp stand on the beach
     * afterwards. False when a template is missing (nothing was changed then).
     */
    public static boolean zorg(Plaats p) {
        if (p.test() || gebouwd(p) == p.opzet().versie()) {
            return true;
        }
        ServerLevel level = p.level();
        for (Stuk s : p.opzet().stukken()) {
            if (Stempel.maat(level, s.template()) == null) {
                LOG.error("Snuffeleiland: template {} is missing, the island is not built", s.template());
                return false;
            }
        }
        boolean opnieuw = gebouwd(p) > 0;
        LOG.info("Snuffeleiland: building the island (version {} -> {})", gebouwd(p), p.opzet().versie());
        zee(level, p.oorsprong(), p.opzet().maat());
        for (Stuk s : p.opzet().stukken()) {
            Stempel.plaats(level, s.template(), p.wereld(s.plek()));
        }
        Stand stand = level.getDataStorage().computeIfAbsent(Stand.TYPE);
        stand.versie = p.opzet().versie();
        stand.setDirty();
        if (opnieuw) {
            for (ServerPlayer speler : List.copyOf(level.players())) {
                Reis.zetOp(speler, p, p.opzet().strand());
            }
        }
        return true;
    }

    /** Puts a box back to what the dimension is made of: stone, sand, the sea up to {@link #ZEE}, air above. */
    static void zee(ServerLevel level, BlockPos min, Vec3i maat) {
        Stempel.laad(level, min, maat);
        BlockState lucht = Blocks.AIR.defaultBlockState(), water = Blocks.WATER.defaultBlockState(), zand = Blocks.SAND.defaultBlockState(),
                steen = Blocks.STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y0 = Math.max(level.getMinY() + 1, min.getY()), y1 = Math.min(level.getMaxY(), min.getY() + maat.getY() - 1);
        for (int x = min.getX(); x < min.getX() + maat.getX(); x++) {
            for (int z = min.getZ(); z < min.getZ() + maat.getZ(); z++) {
                for (int y = y1; y >= y0; y--) {
                    pos.set(x, y, z);
                    BlockState moet = y > ZEE ? lucht : y > ZAND ? water : y > STEEN ? zand : steen;
                    if (level.getBlockState(pos) != moet) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, moet, 2 | 16);
                    }
                }
            }
        }
    }

    // =====================================================================================================================
    // residents and the tree
    // =====================================================================================================================

    /** Every island with somebody on it gets its residents and its tree looked after (about once a second). */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        Plaats e = echt(server);
        if (e != null && !e.level().players().isEmpty()) {
            if (gebouwd(e) != e.opzet().versie()) {
                zorg(e);
            }
            bewoon(e);
        }
        for (Plaats t : TEST) {
            bewoon(t);
        }
    }

    /**
     * Exactly one of every resident on its spot, and one tree. A resident that is missing is only made the second time in a
     * row it is not found, and only where the chunk and its entities are loaded (entities of a chunk that is still loading
     * are invisible: a hasty check makes doubles). One that belongs to an older stamp, wandered off or is double goes away.
     */
    static void bewoon(Plaats p) {
        ServerLevel level = p.level();
        for (BewonerPlek b : p.opzet().bewoners()) {
            Vec3 plek = p.wereld(b.plek());
            if (!geladen(level, plek)) {
                continue;
            }
            List<BewonerEntity> er = level.getEntitiesOfClass(BewonerEntity.class, new AABB(plek, plek).inflate(48, 24, 48),
                    e -> b.sleutel().equals(e.getPersistentData().getStringOr(TAG_SLEUTEL, "")));
            BewonerEntity goed = null;
            for (BewonerEntity e : er) {
                boolean past = goed == null && e.getPersistentData().getIntOr(TAG_VERSIE, -1) == p.opzet().versie() && e.klopt(b)
                        && e.position().distanceToSqr(plek) < 24 * 24;
                if (past) {
                    goed = e;
                } else {
                    e.discard();
                }
            }
            String sleutel = sleutel(p, b.sleutel());
            if (goed != null) {
                GEMIST.remove(sleutel);
            } else if (GEMIST.merge(sleutel, 1, Integer::sum) >= 2) {
                GEMIST.remove(sleutel);
                BewonerEntity nieuw = Bewoners.maak(level, b, plek);
                if (nieuw != null) {
                    nieuw.getPersistentData().putString(TAG_SLEUTEL, b.sleutel());
                    nieuw.getPersistentData().putInt(TAG_VERSIE, p.opzet().versie());
                    level.addFreshEntity(nieuw);
                }
            }
        }
        // the tree
        BlockPos voet = p.boom();
        Vec3 plek = Vec3.atBottomCenterOf(voet);
        if (geladen(level, plek)) {
            List<BoompjeEntity> er = level.getEntitiesOfClass(BoompjeEntity.class, new AABB(plek, plek).inflate(48, 24, 48),
                    e -> BOOM_SLEUTEL.equals(e.getPersistentData().getStringOr(TAG_SLEUTEL, "")));
            BoompjeEntity goed = null;
            for (BoompjeEntity e : er) {
                if (goed == null && e.position().distanceToSqr(plek) < 0.01) {
                    goed = e;
                } else {
                    e.discard();
                }
            }
            String sleutel = sleutel(p, BOOM_SLEUTEL);
            if (goed != null) {
                GEMIST.remove(sleutel);
            } else if (GEMIST.merge(sleutel, 1, Integer::sum) >= 2) {
                GEMIST.remove(sleutel);
                BoompjeEntity boom = SnuffelFeature.SNUFFEL_BOOMPJE.get().create(level, EntitySpawnReason.TRIGGERED);
                if (boom != null) {
                    boom.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
                    boom.getPersistentData().putString(TAG_SLEUTEL, BOOM_SLEUTEL);
                    level.addFreshEntity(boom);
                }
            }
        }
    }

    private static String sleutel(Plaats p, String s) {
        return p.level().dimension().identifier() + "@" + p.oorsprong().asLong() + "@" + s;
    }

    private static boolean geladen(ServerLevel level, Vec3 plek) {
        int cx = (int) Math.floor(plek.x) >> 4, cz = (int) Math.floor(plek.z) >> 4;
        return level.hasChunk(cx, cz) && level.areEntitiesLoaded(ChunkPos.pack(cx, cz));
    }

    /** The tree of this island, when it is loaded. */
    @Nullable
    public static BoompjeEntity boomEntity(Plaats p) {
        Vec3 plek = Vec3.atBottomCenterOf(p.boom());
        List<BoompjeEntity> er = p.level().getEntitiesOfClass(BoompjeEntity.class, new AABB(plek, plek).inflate(1));
        return er.isEmpty() ? null : er.get(0);
    }

    /** A resident of this island by its key (null: not loaded or not there yet). */
    @Nullable
    public static BewonerEntity bewoner(Plaats p, String sleutel) {
        for (BewonerPlek b : p.opzet().bewoners()) {
            if (b.sleutel().equals(sleutel)) {
                Vec3 plek = p.wereld(b.plek());
                List<BewonerEntity> er = p.level().getEntitiesOfClass(BewonerEntity.class, new AABB(plek, plek).inflate(48, 24, 48),
                        e -> sleutel.equals(e.getPersistentData().getStringOr(TAG_SLEUTEL, "")));
                return er.isEmpty() ? null : er.get(0);
            }
        }
        return null;
    }

    static void opStop() {
        echt = null;
        TEST.clear();
        GEMIST.clear();
    }

    /** Everybody who is on this island now. */
    public static List<ServerPlayer> spelers(Plaats p) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (Player s : p.level().players()) {
            if (s instanceof ServerPlayer sp && p.zelfde(van(sp))) {
                uit.add(sp);
            }
        }
        return uit;
    }
}
