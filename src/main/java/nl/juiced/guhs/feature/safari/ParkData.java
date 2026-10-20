package nl.juiced.guhs.feature.safari;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

/**
 * Everything the park generator (slice B) and the species generator (slice C) write, read once from the jar (class
 * resources, so the biome source can use it at chunk generation too):
 * <ul>
 *   <li>{@code data/guhs/safari/park.json}: versie, ingang, ingang_yaw (fallback: versie 1 and a spot on the test plateau);</li>
 *   <li>{@code data/guhs/safari/zones.json}: one char per 16 x 16 column ({@code raster[z * breedte + x]}), a base-36 index
 *       into {@code zones}, '.' outside the park (fallback: a voronoi of the zone seeds of plattegrond C);</li>
 *   <li>{@code data/guhs/safari/plekken.json}: the animal homes (fallback: a few test animals near the entrance);</li>
 *   <li>{@code data/guhs/safari/soorten.json}: per species its zone, hitbox, behaviours and animations (fallback: the 51
 *       species of the phase-0 models with defaults).</li>
 * </ul>
 * Every lookup is cheap (arrays), the AI asks {@link #zone} every few ticks.
 */
public final class ParkData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MAP = "/data/guhs/safari/";
    /** The zone names, in the order of the biomes ({@code guhs:safari_<zone>}). */
    public static final List<String> ZONE_NAMEN = List.of("entree", "savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie");
    public static final String DECOR = "decor";

    public record Soort(String naam, String zone, float breedte, float hoogte, float babySchaal, boolean man, Set<String> gedrag,
                        List<String> animaties, double snelheid, boolean glow) {
        public boolean heeft(String g) {
            return gedrag.contains(g);
        }

        /** Lives only in water (sea cows, narwhals, octopuses): swims, never walks. */
        public boolean zee() {
            return heeft("zee") || naam.equals("zeekoe") || naam.equals("narwal") || naam.equals("octopus");
        }

        /** Likes water and land (hippos, crocodiles, otters...). */
        public boolean water() {
            return heeft("water") && !zee();
        }

        /** The extra animations it can play now and then (not idle/walk). */
        public List<String> extra() {
            return animaties.stream().filter(a -> !a.equals("idle") && !a.equals("walk")).toList();
        }
    }

    public record Plek(int id, String soort, String zone, BlockPos centrum, int straal, int volwassenen, int jongen, boolean water, boolean boom) {
    }

    private static ParkData data;

    public final int versie;
    public final BlockPos ingang;
    public final float ingangYaw;
    public final boolean testPlateau;
    private final int cel, breedte;
    private final byte[] raster;          // zone index per cell, -1 outside
    private final byte[] dichtst;         // nearest park zone per cell (for the decor biomes)
    public final List<String> zones;
    public final List<Plek> plekken;
    public final Map<String, Soort> soorten;

    private ParkData(int versie, BlockPos ingang, float ingangYaw, boolean testPlateau, int cel, int breedte, byte[] raster,
                     List<String> zones, List<Plek> plekken, Map<String, Soort> soorten) {
        this.versie = versie;
        this.ingang = ingang;
        this.ingangYaw = ingangYaw;
        this.testPlateau = testPlateau;
        this.cel = cel;
        this.breedte = breedte;
        this.raster = raster;
        this.zones = zones;
        this.plekken = plekken;
        this.soorten = soorten;
        this.dichtst = dichtste(raster, breedte);
    }

    public static synchronized ParkData get() {
        if (data == null) {
            data = laad();
        }
        return data;
    }

    /** Forget the cached data (tests, a dev reload). */
    public static synchronized void vergeet() {
        data = null;
    }

    // --- lookups -------------------------------------------------------------------------------------------------------------

    /** The zone index at this block column, -1 outside the park (decor or beyond the stamped area). */
    public int zone(int x, int z) {
        if (!Safari.binnen(x, z)) {
            return -1;
        }
        int cx = x / cel, cz = z / cel;
        if (cx >= breedte || cz >= breedte) {
            return -1;
        }
        return raster[cz * breedte + cx];
    }

    /** The zone of the biome here: the park zone, or for the decor the nearest park zone (so colours do not jump). */
    public int biomeZone(int x, int z) {
        int cx = Math.floorDiv(x, cel), cz = Math.floorDiv(z, cel);
        cx = Math.max(0, Math.min(breedte - 1, cx));
        cz = Math.max(0, Math.min(breedte - 1, cz));
        return dichtst[cz * breedte + cx];
    }

    public int zoneIndex(String naam) {
        return zones.indexOf(naam);
    }

    public String zoneNaam(int index) {
        return index < 0 || index >= zones.size() ? DECOR : zones.get(index);
    }

    @Nullable
    public Soort soort(String naam) {
        return soorten.get(naam);
    }

    // --- loading -------------------------------------------------------------------------------------------------------------

    @Nullable
    private static JsonElement lees(String bestand) {
        try (InputStream in = ParkData.class.getResourceAsStream(MAP + bestand)) {
            if (in == null) {
                return null;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r);
            }
        } catch (Exception e) {
            LOGGER.error("Safari: could not read {}{}", MAP, bestand, e);
            return null;
        }
    }

    private static ParkData laad() {
        JsonElement park = lees("park.json");
        int versie = 1;
        BlockPos ingang = null;
        float yaw = 0f;
        boolean plateau = true;
        if (park != null && park.isJsonObject()) {
            JsonObject o = park.getAsJsonObject();
            versie = Math.max(1, o.has("versie") ? o.get("versie").getAsInt() : 1);
            ingang = pos(o.get("ingang"));
            yaw = o.has("ingang_yaw") ? o.get("ingang_yaw").getAsFloat() : 0f;
            plateau = false;
        }
        if (ingang == null) {
            ingang = new BlockPos(208, 64, 572);
        }

        JsonElement zj = lees("zones.json");
        List<String> zones = new ArrayList<>(ZONE_NAMEN);
        int cel = 16, breedte = Safari.MAAT / 16;
        byte[] raster;
        if (zj != null && zj.isJsonObject()) {
            JsonObject o = zj.getAsJsonObject();
            cel = o.has("cel") ? o.get("cel").getAsInt() : 16;
            breedte = o.has("breedte") ? o.get("breedte").getAsInt() : Safari.MAAT / cel;
            zones = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("zones")) {
                zones.add(e.getAsString());
            }
            String r = o.get("raster").getAsString();
            raster = new byte[breedte * breedte];
            for (int i = 0; i < raster.length; i++) {
                char c = i < r.length() ? r.charAt(i) : '.';
                raster[i] = (byte) (c == '.' ? -1 : Character.digit(c, 36));
                if (raster[i] >= zones.size()) {
                    raster[i] = -1;
                }
            }
        } else {
            raster = fallbackRaster(cel, breedte, zones);
        }

        Map<String, Soort> soorten = new LinkedHashMap<>();
        JsonElement sj = lees("soorten.json");
        if (sj != null && sj.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : sj.getAsJsonObject().entrySet()) {
                soorten.put(e.getKey(), soort(e.getKey(), e.getValue().getAsJsonObject()));
            }
        } else {
            for (String[] s : FALLBACK_SOORTEN) {
                soorten.put(s[0], new Soort(s[0], s[1], 0.9f, 1.2f, 0.55f, false, Set.of(), List.of("idle", "walk", "blij", "eet"), 0.2, false));
            }
        }

        List<Plek> plekken = new ArrayList<>();
        JsonElement pj = lees("plekken.json");
        if (pj != null && pj.isJsonArray()) {
            int id = 0;
            for (JsonElement e : pj.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                BlockPos c = pos(o.get("centrum"));
                if (c == null) {
                    continue;
                }
                plekken.add(new Plek(id++, o.get("soort").getAsString(), str(o, "zone", ""), c, o.has("straal") ? o.get("straal").getAsInt() : 12,
                        o.has("volwassenen") ? o.get("volwassenen").getAsInt() : 2, o.has("jongen") ? o.get("jongen").getAsInt() : 1,
                        o.has("water") && o.get("water").getAsBoolean(), o.has("boom") && o.get("boom").getAsBoolean()));
            }
        } else {
            String[] test = {"leeuw", "olifant", "giraffe", "zebra", "pinguin", "rode_panda"};
            for (int i = 0; i < test.length; i++) {
                double hoek = i * Math.PI * 2 / test.length;
                BlockPos c = ingang.offset((int) (Math.cos(hoek) * 22), 0, (int) (Math.sin(hoek) * 22) - 30);
                Soort s = soorten.get(test[i]);
                plekken.add(new Plek(i, test[i], s == null ? "savanne" : s.zone(), c, 8, 2, 1, false, false));
            }
        }
        ParkData d = new ParkData(versie, ingang, yaw, plateau, cel, breedte, raster, zones, plekken, soorten);
        LOGGER.info("Safari: park versie {} ({}), {} zones, {} species, {} homes", versie, plateau ? "test plateau" : "templates",
                zones.size(), soorten.size(), plekken.size());
        return d;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    private static Soort soort(String naam, JsonObject o) {
        java.util.HashSet<String> gedrag = new java.util.HashSet<>();
        if (o.has("gedrag")) {
            for (JsonElement g : o.getAsJsonArray("gedrag")) {
                gedrag.add(g.getAsString());
            }
        }
        List<String> anim = new ArrayList<>();
        if (o.has("animaties")) {
            for (JsonElement a : o.getAsJsonArray("animaties")) {
                anim.add(a.getAsString());
            }
        }
        if (anim.isEmpty()) {
            anim.addAll(List.of("idle", "walk"));
        }
        return new Soort(naam, str(o, "zone", "savanne"),
                o.has("breedte") ? o.get("breedte").getAsFloat() : 0.9f, o.has("hoogte") ? o.get("hoogte").getAsFloat() : 1.2f,
                o.has("baby_schaal") ? o.get("baby_schaal").getAsFloat() : 0.55f, o.has("man") && o.get("man").getAsBoolean(),
                Set.copyOf(gedrag), List.copyOf(anim), o.has("snelheid") ? o.get("snelheid").getAsDouble() : 0.2,
                o.has("glow") && o.get("glow").getAsBoolean());
    }

    @Nullable
    private static BlockPos pos(@Nullable JsonElement e) {
        if (e == null || !e.isJsonArray()) {
            return null;
        }
        JsonArray a = e.getAsJsonArray();
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    /** Plattegrond C (the chosen layout): a voronoi of its zone seeds inside a rounded park square. Only without zones.json. */
    private static byte[] fallbackRaster(int cel, int breedte, List<String> zones) {
        Object[][] seeds = {
                {"entree", 80, 452}, {"savanne", 210, 440}, {"savanne", 330, 455}, {"savanne", 455, 425}, {"delta", 445, 300},
                {"regenwoud", 330, 300}, {"regenwoud", 445, 175}, {"bamboe", 445, 55}, {"oertijd", 310, 70}, {"ijskust", 175, 45},
                {"ijskust", 55, 75}, {"europa", 85, 215}, {"europa", 190, 205}, {"australie", 70, 335}, {"australie", 190, 330}};
        byte[] r = new byte[breedte * breedte];
        for (int cz = 0; cz < breedte; cz++) {
            for (int cx = 0; cx < breedte; cx++) {
                int x = cx * cel + cel / 2 - Safari.PARK_MIN, z = cz * cel + cel / 2 - Safari.PARK_MIN;
                if (x < 0 || z < 0 || x >= 512 || z >= 512) {
                    r[cz * breedte + cx] = -1;
                    continue;
                }
                double best = Double.MAX_VALUE;
                int zone = -1;
                for (Object[] s : seeds) {
                    double dx = x - (int) s[1], dz = z - (int) s[2];
                    double d = (dx * dx + dz * dz) * (s[0].equals("entree") ? 1.6 : 1.0);
                    if (d < best) {
                        best = d;
                        zone = zones.indexOf((String) s[0]);
                    }
                }
                r[cz * breedte + cx] = (byte) zone;
            }
        }
        return r;
    }

    /** For every cell the nearest park zone (breadth-first from the park cells; all decor when there is no park). */
    private static byte[] dichtste(byte[] raster, int breedte) {
        byte[] uit = Arrays.copyOf(raster, raster.length);
        ArrayDeque<Integer> rij = new ArrayDeque<>();
        for (int i = 0; i < uit.length; i++) {
            if (uit[i] >= 0) {
                rij.add(i);
            }
        }
        while (!rij.isEmpty()) {
            int i = rij.poll(), x = i % breedte, z = i / breedte;
            int[][] buren = {{x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
            for (int[] b : buren) {
                if (b[0] < 0 || b[1] < 0 || b[0] >= breedte || b[1] >= breedte) {
                    continue;
                }
                int j = b[1] * breedte + b[0];
                if (uit[j] < 0) {
                    uit[j] = uit[i];
                    rij.add(j);
                }
            }
        }
        return uit;
    }

    /** The 51 species of the phase-0 studio (name, zone), used only when soorten.json is missing. */
    static final String[][] FALLBACK_SOORTEN = {
            {"leeuw", "savanne"}, {"luipaard", "savanne"}, {"olifant", "savanne"}, {"neushoorn", "savanne"}, {"kafferbuffel", "savanne"},
            {"dikdik", "savanne"}, {"giraffe", "savanne"}, {"zebra", "savanne"}, {"stokstaartje", "savanne"}, {"jachtluipaard", "savanne"},
            {"struisvogel", "savanne"}, {"gouden_dikdik", "savanne"},
            {"zeekoe", "delta"}, {"nijlpaard", "delta"}, {"flamingo", "delta"}, {"capibara", "delta"}, {"krokodil", "delta"},
            {"rode_panda", "bamboe"}, {"reuzenpanda", "bamboe"}, {"gibbon", "bamboe"},
            {"luiaard", "regenwoud"}, {"okapi", "regenwoud"}, {"orang_oetan", "regenwoud"}, {"toekan", "regenwoud"}, {"gekko", "regenwoud"},
            {"glimworm", "regenwoud"},
            {"pinguin", "ijskust"}, {"ijsbeer", "ijskust"}, {"zeehond", "ijskust"}, {"narwal", "ijskust"}, {"octopus", "ijskust"},
            {"sabeltandtijger", "oertijd"}, {"mammoet", "oertijd"}, {"wolharige_neushoorn", "oertijd"},
            {"bever", "europa"}, {"vuursalamander", "europa"}, {"egel", "europa"}, {"otter", "europa"}, {"das", "europa"},
            {"edelhert", "europa"}, {"ooievaar", "europa"}, {"specht", "europa"},
            {"koala", "australie"}, {"kangoeroe", "australie"}, {"wombat", "australie"}, {"quokka", "australie"}, {"prieelvogel", "australie"},
            {"geitje", "entree"}, {"alpaca", "entree"}, {"cavia", "entree"}, {"ezeltje", "entree"}};
}
