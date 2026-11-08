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
        BlockPos hal = m.get("zoutgrot"), elite = m.getOrDefault("zoutgrot_elite", Z6_ELITE.offset(hal.subtract(Z6_HAL)));
        int dx = hal.getX() - Z6_HAL.getX(), dy = hal.getY() - Z6_HAL.getY(), dz = hal.getZ() - Z6_HAL.getZ();
        List<Kamer> kamers = new ArrayList<>();
        for (Kamer a : k.kamers()) {
            kamers.add(new Kamer(a.id(), a.x() + dx, a.y() + dy, a.z() + dz, a.r()));
        }
        Kamer b = new Kamer(k.baas().id(), elite.getX(), elite.getY(), elite.getZ(), k.baas().r());
        List<int[]> achter = List.of(new int[] {elite.getX(), elite.getY(), elite.getZ(), k.baas().r() + 3});
        int minX = Math.min(k.minX() + dx, elite.getX() - 12), maxX = Math.max(k.maxX() + dx, elite.getX() + 12);
        int minZ = Math.min(k.minZ() + dz, elite.getZ() - 12), maxZ = Math.max(k.maxZ() + dz, elite.getZ() + 12);
        return new Kerker(k.id(), k.zone(), minX, k.minY() + dy, minZ, maxX, k.maxY() + dy, maxZ, k.minLv(), k.maxLv(), k.eliteLv(), k.baasLv(),
                List.copyOf(kamers), b, List.of(), null, achter);
    }
}
