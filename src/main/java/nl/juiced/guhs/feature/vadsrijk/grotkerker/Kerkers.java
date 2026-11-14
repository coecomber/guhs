package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;

/**
 * U-GROTKERKER: the caves that really have a run in this build. Zones 1-5 always (their caves are U-GROTTEN's, in the tiles);
 * zone 6 (De Zingende Zoutgrot, built by u-z6wereld's own generator) only when {@code zone6.wereld.Z6WereldPlekken} is on the
 * class path: then its two POIs {@code zoutgrot} (the hall stand spot) and {@code zoutgrot_elite} (the elite chamber) are read
 * from there and the whole entry is moved onto them, so a later move of the cave in u-z6wereld follows by itself.
 */
public final class Kerkers {
    /** Where KerkerData expects the zone-6 POIs (u-z6wereld at the time of writing). */
    static final BlockPos Z6_HAL = new BlockPos(3874, 50, -534), Z6_ELITE = new BlockPos(3917, 50, -542);
    private static List<Kerker> actief;
    /** Gametests: a test cave that stands in for the real one with the same id (van/op see it; the server tick skips the id). */
    static final Map<String, Kerker> TEST = new java.util.concurrent.ConcurrentHashMap<>();

    private Kerkers() {
    }

    public static synchronized List<Kerker> alle() {
        if (actief == null) {
            List<Kerker> l = new ArrayList<>();
            for (Kerker k : KerkerData.ALLE) {
                if (k.zone() < 6) {
                    l.add(k);
                } else {
                    Kerker z6 = zone6(k);
                    if (z6 != null) {
                        l.add(z6);
                    }
                }
            }
            actief = List.copyOf(l);
        }
        return actief;
    }

    @Nullable
    public static Kerker van(String id) {
        Kerker t = TEST.get(id);
        if (t != null) {
            return t;
        }
        for (Kerker k : alle()) {
            if (k.id().equals(id)) {
                return k;
            }
        }
        return null;
    }

    /** The cave run at this position (null: none). */
    @Nullable
    public static Kerker op(double x, double y, double z) {
        for (Kerker k : alle()) {
            if (k.bevat(x, y, z)) {
                return k;
            }
        }
        return null;
    }

    /** Is zone 6's map class here (u-z6wereld merged)? */
    public static boolean zone6Aanwezig() {
        return z6Plekken() != null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static Map<String, BlockPos> z6Plekken() {
        try {
            Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken");
            Object m = c.getField("ALLE").get(null);
            return m instanceof Map<?, ?> mm ? (Map<String, BlockPos>) mm : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** The zone-6 entry moved onto the real POIs, or null when zone 6's map is not in this build. */
    @Nullable
    static Kerker zone6(Kerker k) {
        Map<String, BlockPos> m = z6Plekken();
        if (m == null || m.get("zoutgrot") == null) {
            return null;
        }
        BlockPos hal = m.get("zoutgrot");
        int dx = hal.getX() - Z6_HAL.getX(), dy = hal.getY() - Z6_HAL.getY(), dz = hal.getZ() - Z6_HAL.getZ();
        if (dx == 0 && dy == 0 && dz == 0) {
            return k;   // 1.10.3: the generated entry (the dungeon wing, its gate + sluis) fits the map as it is
        }
        // u-z6wereld moved the cave: everything moves along (the per-column space no longer fits: box only)
        GESCHOVEN.add(k.id());
        List<Kamer> kamers = new ArrayList<>();
        for (Kamer a : k.kamers()) {
            kamers.add(schuif(a, dx, dy, dz));
        }
        return new Kerker(k.id(), k.zone(), k.minX() + dx, k.minY() + dy, k.minZ() + dz, k.maxX() + dx, k.maxY() + dy, k.maxZ() + dz, k.minLv(), k.maxLv(),
                k.eliteLv(), k.baasLv(), List.copyOf(kamers), schuif(k.baas(), dx, dy, dz), kolommen(k.poort(), dx, dy, dz), plus(k.bord(), dx, dy, dz),
                kolommen(k.achter(), dx, dy, dz), kolommen(k.sluis(), dx, dy, dz), plus(k.sluisBord(), dx, dy, dz),
                k.hendels().stream().map(h -> h.offset(dx, dy, dz)).toList(), plus(k.kist(), dx, dy, dz));
    }

    private static final java.util.Set<String> GESCHOVEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static Kamer schuif(Kamer a, int dx, int dy, int dz) {
        int[] p = a.plekken().clone();
        for (int i = 0; i + 2 < p.length; i += 3) {
            p[i] += dx;
            p[i + 1] += dy;
            p[i + 2] += dz;
        }
        return new Kamer(a.id(), a.x() + dx, a.y() + dy, a.z() + dz, a.r(), p);
    }

    private static List<int[]> kolommen(List<int[]> l, int dx, int dy, int dz) {
        List<int[]> uit = new ArrayList<>();
        for (int[] c : l) {
            // gate columns {x, y0, z, y1}; behind circles {x, y, z, r}: the 4th value moves with y only for columns
            uit.add(new int[] {c[0] + dx, c[1] + dy, c[2] + dz, c[3]});
        }
        return List.copyOf(uit);
    }

    @Nullable
    private static BlockPos plus(@Nullable BlockPos b, int dx, int dy, int dz) {
        return b == null ? null : b.offset(dx, dy, dz);
    }

    // ---- 1.10.3: the cave's real space per column ------------------------------------------------------------------------

    /**
     * 1.10.3 (live bug "ik word steeds naar de ingang gezet"): where players count as IN the cave: per column of the cave a y
     * range (the cave's air and water + 2, never at or above the natural ground), generated by tools/vadsrijk/grotten/herbouw.py
     * into data/guhs/vadsrijk/grotkerker/&lt;id&gt;.json. The plain box reached up to the surface above the cave (the Kristalgrot's
     * flank) and missed the lake bed: a non-member on the mountain was "in the dungeon" and put out; a member in a pocket was
     * "not in it" and the run closed.
     */
    public record Ruimte(int x0, int z0, int w, int d, short[] lo, short[] hi) {
        public boolean bevat(double x, double y, double z) {
            int i = (int) Math.floor(x) - x0, j = (int) Math.floor(z) - z0;
            if (i < 0 || j < 0 || i >= w || j >= d) {
                return false;
            }
            int by = (int) Math.floor(y + 1e-3);
            int k = j * w + i;
            return by >= lo[k] && by <= hi[k] + 1;
        }
    }

    private static final Map<String, java.util.Optional<Ruimte>> RUIMTE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, java.util.Optional<com.google.gson.JsonObject>> JSON = new java.util.concurrent.ConcurrentHashMap<>();

    /** The generated data of a cave (null: none, e.g. the Zwamgrot's rebuild part). */
    @Nullable
    public static com.google.gson.JsonObject json(String id) {
        return JSON.computeIfAbsent(id, i -> {
            try (var in = Kerkers.class.getResourceAsStream("/data/guhs/vadsrijk/grotkerker/" + i + ".json")) {
                if (in == null) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject());
            } catch (Exception e) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
    }

    static short[] shorts(String b64) {
        byte[] b = java.util.Base64.getDecoder().decode(b64);
        short[] s = new short[b.length / 2];
        java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.BIG_ENDIAN).asShortBuffer().get(s);
        return s;
    }

    /** The per-column space of this cave; null = the box alone (gametest stand-ins, a moved zone-6 cave, no data). */
    @Nullable
    public static Ruimte ruimte(Kerker k) {
        if (TEST.get(k.id()) == k || GESCHOVEN.contains(k.id())) {
            return null;
        }
        return RUIMTE.computeIfAbsent(k.id(), i -> {
            com.google.gson.JsonObject o = json(i);
            if (o == null || !o.has("ruimte")) {
                return java.util.Optional.empty();
            }
            com.google.gson.JsonObject r = o.getAsJsonObject("ruimte");
            return java.util.Optional.of(new Ruimte(r.get("x0").getAsInt(), r.get("z0").getAsInt(), r.get("w").getAsInt(), r.get("d").getAsInt(),
                    shorts(r.get("lo").getAsString()), shorts(r.get("hi").getAsString())));
        }).orElse(null);
    }
}
