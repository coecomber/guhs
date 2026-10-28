package nl.juiced.guhs.feature.dromen;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The Droomplein and the five islands of Guhdromenland, each on a fixed spot (ontwerp r00, BOUWPLAN_180.md part A):
 * the plein at (0, {@link #Y}, 0), the islands {@link #AFSTAND} blocks away on the compass (north = 0 degrees, clockwise):
 * N Sterrenkussenveld, 72 Maanmolentje, 144 Wolkenbadje, 216 Herinneringseiland, 288 Slaapliedjesheuvels.
 * <p>
 * <b>For the island agents</b> (each delivers only its own files, nothing in here changes):
 * <ul>
 *   <li>a template {@code data/guhs/structure/dromen/eiland_<id>.nbt} (at most {@link #MAX_MAAT} big), and</li>
 *   <li>{@code data/guhs/dromen/eiland/<id>.json}:
 * <pre>
 * {"versie": 1,                                  higher = stamped again on a live world (the old box is cleared first)
 *  "template": "guhs:dromen/eiland_liedjes",     optional; this is the default
 *  "midden": [x, y, z],                          the template block that lands on the island's centre spot
 *  "aankomst": {"plek": [x, y, z], "yaw": 0},    where you arrive (feet), RELATIVE TO THE CENTRE like every plek below
 *  "steiger": [x, y, z],                         islands: step on it to float to the plein
 *  "steigers": {"liedjes": [x, y, z], ...},      plein only: one per island, step on it to float there
 *  "wekkerguh": {"plek": [x, y, z], "yaw": 180}} plein only: where the Wekkerguh stands}
 * </pre></li>
 * </ul>
 * Without a json (or while its template is missing) the island is a small placeholder ({@link #placeholder}), version 0.
 * Ids: {@link #PLEIN}, {@link #LIEDJES}, {@link #STERREN}, {@link #HERINNERING}, {@link #BADJE}, {@link #MOLENTJE}.
 */
public final class DroomEilanden {
    private static final Logger LOG = LogUtils.getLogger();

    public static final String PLEIN = "plein", LIEDJES = "liedjes", STERREN = "sterren", HERINNERING = "herinnering", BADJE = "badje",
            MOLENTJE = "molentje";
    /** 1.8.0 uitbreiding: the Vliegdroom, high above the plein (you only get there on a Rookguh: feature/vliegdroom). */
    public static final String VLIEG = "vlieg";
    /** Every island on the ring, the plein first (the steigers, the brievenbusjes, the creatures falling back). */
    public static final List<String> IDS = List.of(PLEIN, STERREN, MOLENTJE, BADJE, HERINNERING, LIEDJES);
    /** Every island that is built: the ring and the islands that hang somewhere else ({@link #VAST}). */
    public static final List<String> ALLE = List.of(PLEIN, STERREN, MOLENTJE, BADJE, HERINNERING, LIEDJES, VLIEG);
    /** Islands with a fixed centre spot of their own instead of a place on the ring. */
    public static final Map<String, BlockPos> VAST = Map.of(VLIEG, new BlockPos(0, 200, 0));
    /** Above this height you are at the Vliegdroom (it hangs right above the plein). */
    public static final int VLIEG_VANAF = 150;
    /** Compass angle of each island from the plein (degrees, north = 0, clockwise). */
    public static final Map<String, Integer> HOEK = Map.of(STERREN, 0, MOLENTJE, 72, BADJE, 144, HERINNERING, 216, LIEDJES, 288);
    /** The height of every island's centre spot, and how far the islands lie from the plein. */
    public static final int Y = 96, AFSTAND = 120;
    /** The biggest template an island may have (its box is cleared before a new stamp). */
    public static final Vec3i MAX_MAAT = new Vec3i(112, 80, 112);
    /** The sea floor (the dimension's one flat layer, y 0) and how low you may sink before a Blub bubble takes you back. */
    public static final int ZEE_Y = 0, BUBBEL_Y = 40;

    /** A spot on an island: relative to its centre, with a direction to look in. */
    public record Punt(Vec3 plek, float yaw) {
    }

    /** What an island is made of (from its json, or the placeholder). */
    public record Opzet(String id, int versie, @Nullable Identifier template, BlockPos midden, Punt aankomst, @Nullable Vec3 steiger,
                        Map<String, Vec3> steigers, @Nullable Punt wekkerguh) {
    }

    private static final Map<String, Opzet> CACHE = new HashMap<>();

    private DroomEilanden() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(DroomEilanden::tick);
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /** The world block of an island's centre spot. */
    public static BlockPos midden(String id) {
        BlockPos vast = VAST.get(id);
        if (vast != null) {
            return vast;
        }
        Integer hoek = HOEK.get(id);
        if (hoek == null) {
            return new BlockPos(0, Y, 0);
        }
        double a = Math.toRadians(hoek);
        return new BlockPos(Mth.floor(Math.sin(a) * AFSTAND), Y, Mth.floor(-Math.cos(a) * AFSTAND));
    }

    /** A spot relative to an island's centre, in the world. */
    public static Vec3 wereld(String id, Vec3 rel) {
        BlockPos m = midden(id);
        return new Vec3(m.getX() + rel.x, m.getY() + rel.y, m.getZ() + rel.z);
    }

    /** The island closest to this spot (horizontally). */
    public static String dichtstbij(Vec3 pos) {
        if (pos.y >= VLIEG_VANAF) {
            return VLIEG;
        }
        String beste = PLEIN;
        double d = Double.MAX_VALUE;
        for (String id : IDS) {
            BlockPos m = midden(id);
            double dd = Mth.square(pos.x - m.getX() - 0.5) + Mth.square(pos.z - m.getZ() - 0.5);
            if (dd < d) {
                d = dd;
                beste = id;
            }
        }
        return beste;
    }

    /** The yaw that looks from a spot on this island towards another island's centre. */
    static float yawNaar(String van, String naar) {
        BlockPos a = midden(van), b = midden(naar);
        return (float) (Mth.atan2(b.getZ() - a.getZ(), b.getX() - a.getX()) * (180 / Math.PI)) - 90f;
    }

    // =====================================================================================================================
    // what (json or placeholder)
    // =====================================================================================================================

    /** This island's opzet: its json from the mod's resources, else the placeholder. */
    public static synchronized Opzet opzet(String id) {
        return CACHE.computeIfAbsent(id, DroomEilanden::lees);
    }

    /** Forget the read jsons (the test command "eilanden opnieuw" reads them again). */
    static synchronized void vergeet() {
        CACHE.clear();
    }

    private static Opzet lees(String id) {
        String pad = "/data/guhs/dromen/eiland/" + id + ".json";
        try (InputStream in = DroomEilanden.class.getResourceAsStream(pad)) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                return lees(id, o);
            }
        } catch (Exception e) {
            LOG.error("Guhdromenland: {} is broken, using the placeholder: {}", pad, e.toString());
        }
        return placeholder(id);
    }

    /** An island from its json (see the class comment). */
    public static Opzet lees(String id, JsonObject o) {
        Identifier template = Identifier.parse(o.has("template") ? o.get("template").getAsString() : Guhs.MODID + ":dromen/eiland_" + id);
        Map<String, Vec3> steigers = new LinkedHashMap<>();
        if (o.has("steigers")) {
            for (var e : o.getAsJsonObject("steigers").entrySet()) {
                steigers.put(e.getKey(), punt(e.getValue().getAsJsonArray()));
            }
        }
        return new Opzet(id, o.has("versie") ? o.get("versie").getAsInt() : 1, template, blok(o.getAsJsonArray("midden")),
                puntMetYaw(o.getAsJsonObject("aankomst")), o.has("steiger") ? punt(o.getAsJsonArray("steiger")) : null, steigers,
                o.has("wekkerguh") ? puntMetYaw(o.getAsJsonObject("wekkerguh")) : null);
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

    /** The placeholder island (version 0): a round little cloud island, its arrival in the middle, its steiger(s) on the rim. */
    public static Opzet placeholder(String id) {
        boolean plein = id.equals(PLEIN);
        int r = plein ? STRAAL_PLEIN : STRAAL_EILAND;
        Map<String, Vec3> steigers = new LinkedHashMap<>();
        Vec3 steiger = null;
        if (plein) {
            for (String e : IDS) {
                if (!e.equals(PLEIN)) {
                    steigers.put(e, rand(PLEIN, e, r - 1));
                }
            }
        } else {
            steiger = rand(id, PLEIN, r - 1);
        }
        Punt aankomst = new Punt(new Vec3(0.5, 1, 0.5), plein ? 180f : yawNaar(id, PLEIN));
        Punt wekker = plein ? new Punt(new Vec3(4.5, 1, 4.5), 135f) : null;
        return new Opzet(id, 0, null, BlockPos.ZERO, aankomst, steiger, steigers, wekker);
    }

    static final int STRAAL_PLEIN = 16, STRAAL_EILAND = 11;

    /** A spot on an island's rim (rel. to its centre, feet height) in the direction of another island. */
    private static Vec3 rand(String van, String naar, int r) {
        BlockPos a = midden(van), b = midden(naar);
        double dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ(), l = Math.sqrt(dx * dx + dz * dz);
        return new Vec3(Math.floor(dx / l * r) + 0.5, 1, Math.floor(dz / l * r) + 0.5);
    }

    // =====================================================================================================================
    // stamping
    // =====================================================================================================================

    /** Which version of every island stands in this world. */
    public static final class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("dromen_eilanden", Stand::new, Stand::load, Stand::save);
        final Map<String, Integer> versies = new HashMap<>();

        private static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            for (String k : tag.keySet()) {
                s.versies.put(k, tag.getIntOr(k, -1));
            }
            return s;
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            versies.forEach(t::putInt);
            return t;
        }
    }

    private static Stand stand(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Stand.TYPE);
    }

    /** The version of this island that stands in the world (-1: nothing yet). */
    public static int gebouwd(ServerLevel level, String id) {
        return stand(level).versies.getOrDefault(id, -1);
    }

    /** Makes sure every island stands in its newest version (placeholders where no template is shipped yet). */
    public static void zorg(ServerLevel level) {
        for (String id : ALLE) {
            zorg(level, id);
        }
    }

    /** Builds this island when nothing (or an older version) stands; true when it stands now. */
    public static boolean zorg(ServerLevel level, String id) {
        Opzet o = opzet(id);
        boolean echt = o.template() != null && Stempel.maat(level, o.template()) != null;
        int versie = echt ? o.versie() : 0;
        if (gebouwd(level, id) == versie) {
            return true;
        }
        if (o.template() != null && !echt) {
            LOG.warn("Guhdromenland: template {} of island {} is missing, a placeholder stands there", o.template(), id);
        }
        bouw(level, id, echt ? o : placeholder(id));
        Stand s = stand(level);
        s.versies.put(id, versie);
        s.setDirty();
        return true;
    }

    /** Clears the island's box and stamps it (the template, or the placeholder). */
    static void bouw(ServerLevel level, String id, Opzet o) {
        BlockPos m = midden(id);
        BlockPos min = m.offset(-MAX_MAAT.getX() / 2, -MAX_MAAT.getY() / 2, -MAX_MAAT.getZ() / 2);
        Stempel.leeg(level, min, MAX_MAAT);
        Stempel.ruim(level, Stempel.doos(min, MAX_MAAT));
        LOG.info("Guhdromenland: building island {} (version {})", id, o.versie());
        if (o.template() != null && o.versie() > 0) {
            Stempel.plaats(level, o.template(), m.subtract(o.midden()));
        } else {
            bouwPlaceholder(level, id, o);
        }
    }

    /** Forget every stamped version: the next {@link #zorg} builds every island again. */
    static void opnieuw(ServerLevel level) {
        vergeet();
        Stand s = stand(level);
        s.versies.clear();
        s.setDirty();
        zorg(level);
    }

    private static BlockState blok(String id, Block anders) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(anders).defaultBlockState();
    }

    /** A round cloud island: a fluffy top, a dripping underside, lanterns, and a steiger with a big bubble per steiger spot. */
    static void bouwPlaceholder(ServerLevel level, String id, Opzet o) {
        boolean plein = id.equals(PLEIN);
        int r = plein ? STRAAL_PLEIN : STRAAL_EILAND;
        BlockPos m = midden(id);
        BlockState top = blok("guhs:pluiswolblok", Blocks.PINK_WOOL), onder = blok("guhs:wolkenblok_roze", Blocks.PINK_WOOL),
                wit = blok("guhs:wolkenblok_wit", Blocks.WHITE_WOOL), rand = blok(plein ? "guhs:wolkenblok_wit" : KLEUR.getOrDefault(id, "minecraft:white_wool"), Blocks.WHITE_WOOL),
                bel = blok("guhs:zeepbel", Blocks.LIGHT_BLUE_STAINED_GLASS), licht = blok("guhs:sterrenlantaarn", Blocks.LANTERN);
        Stempel.laad(level, m.offset(-r - 2, 0, -r - 2), new Vec3i(2 * r + 5, 1, 2 * r + 5));
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.3) {
                    continue;
                }
                int diepte = 1 + (int) Math.round((1 - d / (r + 0.5)) * (plein ? 9 : 7));
                for (int dy = -diepte; dy <= 0; dy++) {
                    BlockState s = dy == 0 ? (d > r - 1.2 ? rand : top) : dy == -diepte ? wit : onder;
                    level.setBlock(m.offset(dx, dy, dz), s, 2 | 16);
                }
            }
        }
        for (int i = 0; i < 6; i++) {               // lanterns around the middle
            double a = Math.PI * 2 * i / 6;
            level.setBlock(m.offset((int) Math.round(Math.cos(a) * (r - 4)), 1, (int) Math.round(Math.sin(a) * (r - 4))), licht, 2 | 16);
        }
        java.util.function.Consumer<Vec3> steiger = rel -> {
            BlockPos s = BlockPos.containing(m.getX() + rel.x, m.getY() + rel.y - 1, m.getZ() + rel.z);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                level.setBlock(s.relative(d), wit, 2 | 16);
            }
            level.setBlock(s, wit, 2 | 16);
            level.setBlock(s.above(4), bel, 2 | 16);
        };
        if (o.steiger() != null) {
            steiger.accept(o.steiger());
        }
        o.steigers().values().forEach(steiger);
    }

    /** The placeholder rim colour of each island (as in r00: lavendel, boter, hemelsblauw, mokka, muisgrijs). */
    private static final Map<String, String> KLEUR = Map.of(LIEDJES, "minecraft:purple_wool", STERREN, "minecraft:yellow_wool",
            BADJE, "minecraft:light_blue_wool", HERINNERING, "minecraft:brown_wool", MOLENTJE, "minecraft:light_gray_wool");

    /** Once a second while somebody dreams: every island stands (a newer jar re-stamps them). */
    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel level = server.getLevel(DromenFeature.DIM);
        if (level != null && !level.players().isEmpty()) {
            zorg(level);
        }
    }
}
