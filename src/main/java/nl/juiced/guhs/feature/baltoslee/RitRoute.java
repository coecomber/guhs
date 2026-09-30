package nl.juiced.guhs.feature.baltoslee;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The route of one sled ride: the two legs as tracks ({@link SleeBaan}: 0 = heen, from the stable to the berghut, 1 = terug)
 * and on each leg its zones: the rest points with their vuurkorf, the ice bridges, the avalanche slopes (with the side the
 * snow comes from) and, on the way back, the dieptepunt.
 * <p>
 * The server makes it once from the Nomguh route ({@link #maak}: the route's points, with the height of the snow that is
 * really there) and puts it in the sled's synced data as a tag ({@link #tag}); every game reads that same tag
 * ({@link #lees}), so every game has exactly the same tracks and zones.
 */
public final class RitRoute {
    public enum Soort { RUST, IJSBRUG, LAWINE, DIEPTEPUNT }

    /** A stretch of a leg: from s0 to s1; kant = the side the avalanche comes from (-1 left, +1 right), else 0. */
    public record Zone(Soort soort, double s0, double s1, int kant) {
        public boolean in(double s) {
            return s >= s0 && s <= s1;
        }

        public double midden() {
            return (s0 + s1) / 2;
        }
    }

    /** How far around a rest point you may stop to rest (blocks along the track). */
    public static final double RUST_STRAAL = 4.5;

    private final SleeBaan[] benen;
    private final List<List<Zone>> zones;
    private final CompoundTag tag;
    /** Where the ride ends when it went well (the hospital) and where you get off otherwise (the stable). */
    public final Vec3 ziekenhuis, stal;
    /** The time limits for the way back (ticks), by level id. */
    private final CompoundTag tijd;

    private RitRoute(CompoundTag tag) {
        this.tag = tag;
        this.benen = new SleeBaan[]{new SleeBaan(floats(tag.getIntArray("Heen").orElse(new int[0]))), new SleeBaan(floats(tag.getIntArray("Terug").orElse(new int[0])))};
        this.zones = new ArrayList<>();
        for (String key : new String[]{"ZonesHeen", "ZonesTerug"}) {
            List<Zone> list = new ArrayList<>();
            for (Tag t : tag.getListOrEmpty(key)) {
                CompoundTag z = (CompoundTag) t;
                list.add(new Zone(Soort.values()[Math.min(Soort.values().length - 1, z.getIntOr("Soort", 0))], z.getDoubleOr("S0", 0.0), z.getDoubleOr("S1", 0.0), z.getIntOr("Kant", 0)));
            }
            zones.add(List.copyOf(list));
        }
        this.ziekenhuis = vec(tag.getIntArray("Ziekenhuis").orElse(new int[0]));
        this.stal = vec(tag.getIntArray("Stal").orElse(new int[0]));
        this.tijd = tag.getCompoundOrEmpty("Tijd");
    }

    public static RitRoute lees(CompoundTag tag) {
        return new RitRoute(tag);
    }

    public CompoundTag tag() {
        return tag;
    }

    public SleeBaan baan(int been) {
        return benen[been == 0 ? 0 : 1];
    }

    public List<Zone> zones(int been) {
        return zones.get(been == 0 ? 0 : 1);
    }

    /** The first zone of this kind on this leg that contains s (null: none). */
    @Nullable
    public Zone zone(int been, Soort soort, double s) {
        for (Zone z : zones(been)) {
            if (z.soort == soort && z.in(s)) {
                return z;
            }
        }
        return null;
    }

    public List<Zone> zones(int been, Soort soort) {
        return zones(been).stream().filter(z -> z.soort == soort).toList();
    }

    /** The time limit for the way back on this level (ticks). */
    public int tijd(String niveau) {
        return tijd.contains(niveau) ? tijd.getIntOr(niveau, 0) : tijd.getIntOr("makkelijk", 0) > 0 ? tijd.getIntOr("makkelijk", 0) : 2400;
    }

    // --- making it (server) ------------------------------------------------------------------------------------------------

    /** The ride's route from the Nomguh route (world coordinates), with the snow's real height under every point. */
    public static RitRoute maak(ServerLevel level, NomguhRoute r) {
        float[] heen = punten(level, r.heen(), r.breedte());
        float[] terug = punten(level, r.terug(), terugBreedte(r));
        SleeBaan h = new SleeBaan(heen), t = new SleeBaan(terug);
        List<CompoundTag> zh = new ArrayList<>(), zt = new ArrayList<>();
        for (int i : r.rust()) {
            double s = h.sVanPunt(i);
            zh.add(zone(Soort.RUST, s - RUST_STRAAL, s + RUST_STRAAL, 0));
            double st = t.dichtstbij(h.midden(s));
            zt.add(zone(Soort.RUST, st - RUST_STRAAL, st + RUST_STRAAL, 0));
        }
        for (int[] b : r.ijsbrug()) {
            double a = h.sVanPunt(b[0]), c = h.sVanPunt(b[1]);
            zh.add(zone(Soort.IJSBRUG, Math.min(a, c), Math.max(a, c), 0));
            double ta = t.dichtstbij(h.midden(a)), tc = t.dichtstbij(h.midden(c));
            zt.add(zone(Soort.IJSBRUG, Math.min(ta, tc), Math.max(ta, tc), 0));
        }
        for (NomguhRoute.Lawine l : r.lawine()) {
            double a = h.sVanPunt(l.van()), c = h.sVanPunt(l.tot());
            int kant = l.links() ? -1 : 1;
            zh.add(zone(Soort.LAWINE, Math.min(a, c), Math.max(a, c), kant));
            // the same slope on the way back: the snow still comes from the same side of the valley
            double mid = (a + c) / 2;
            Vec3 gevaar = h.rechts(mid).scale(kant);
            double ta = t.dichtstbij(h.midden(a)), tc = t.dichtstbij(h.midden(c));
            double tm = (ta + tc) / 2;
            zt.add(zone(Soort.LAWINE, Math.min(ta, tc), Math.max(ta, tc), t.rechts(tm).dot(gevaar) >= 0 ? 1 : -1));
        }
        if (r.dieptepunt() >= 0) {
            double s = h.sVanPunt(r.dieptepunt());
            double st = t.dichtstbij(h.midden(s));
            zt.add(zone(Soort.DIEPTEPUNT, st, st, 0));
        }
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Heen", bits(heen));
        tag.putIntArray("Terug", bits(terug));
        tag.put("ZonesHeen", lijst(zh));
        tag.put("ZonesTerug", lijst(zt));
        tag.putIntArray("Ziekenhuis", pos(r.ziekenhuis()));
        tag.putIntArray("Stal", pos(r.stal()));
        CompoundTag tijd = new CompoundTag();
        r.tijd().forEach(tijd::putInt);
        tag.put("Tijd", tijd);
        return new RitRoute(tag);
    }

    /** The way back's widths: its own if the file has a separate way back, else the way there's reversed. */
    private static List<Double> terugBreedte(NomguhRoute r) {
        if (r.terug().size() == r.heen().size()) {
            List<Double> b = new ArrayList<>(r.breedte());
            java.util.Collections.reverse(b);
            return b;
        }
        List<Double> b = new ArrayList<>();
        for (Vec3 p : r.terug()) {
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < r.heen().size(); i++) {
                double d = r.heen().get(i).distanceToSqr(p);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            b.add(r.breedte().get(best));
        }
        return b;
    }

    /** The route's points (block coordinates: the middle of each block) at the height of the snow that is really there. */
    private static float[] punten(ServerLevel level, List<Vec3> punten, List<Double> breedte) {
        float[] out = new float[punten.size() * 4];
        for (int i = 0; i < punten.size(); i++) {
            Vec3 p = punten.get(i);
            double x = Math.floor(p.x) + 0.5, z = Math.floor(p.z) + 0.5;
            out[i * 4] = (float) x;
            out[i * 4 + 1] = (float) grond(level, x, p.y, z);
            out[i * 4 + 2] = (float) z;
            out[i * 4 + 3] = (float) (i < breedte.size() ? breedte.get(i) : 2.0);
        }
        return out;
    }

    /**
     * The top of the snow the sled can stand on near (x, y, z): of all the spots within 4 blocks up or down where there is
     * ground with room above it, the one nearest to y..y+1 (the route's y may be the ground block itself or the air above it).
     */
    public static double grond(ServerLevel level, double x, double y, double z) {
        BlockPos base = BlockPos.containing(x, y, z);
        double best = y + 1, bestD = Double.MAX_VALUE;
        for (int dy = -4; dy <= 4; dy++) {
            BlockPos pos = base.above(dy);
            BlockState here = level.getBlockState(pos), under = level.getBlockState(pos.below());
            VoxelShape hs = here.getCollisionShape(level, pos), us = under.getCollisionShape(level, pos.below());
            if (us.isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                continue;
            }
            if (!hs.isEmpty() && hs.max(Direction.Axis.Y) >= 0.99) {
                continue;
            }
            double top = hs.isEmpty() ? pos.getY() - 1 + us.max(Direction.Axis.Y) : pos.getY() + hs.max(Direction.Axis.Y);
            if (here.is(Blocks.SNOW)) {
                top = Math.max(top, pos.getY() + here.getValue(SnowLayerBlock.LAYERS) * 2 / 16.0 - 0.06);   // (the runners sink in a bit)
            }
            double d = top < y ? y - top : top > y + 1 ? top - y - 1 : 0;
            if (d < bestD) {
                bestD = d;
                best = top;
            }
        }
        return best;
    }

    // --- tags ------------------------------------------------------------------------------------------------------------

    private static CompoundTag zone(Soort soort, double s0, double s1, int kant) {
        CompoundTag z = new CompoundTag();
        z.putInt("Soort", soort.ordinal());
        z.putDouble("S0", Math.max(0, s0));
        z.putDouble("S1", Math.max(0, s1));
        z.putInt("Kant", kant);
        return z;
    }

    private static ListTag lijst(List<CompoundTag> zones) {
        ListTag l = new ListTag();
        l.addAll(zones);
        return l;
    }

    private static int[] bits(float[] f) {
        int[] out = new int[f.length];
        for (int i = 0; i < f.length; i++) {
            out[i] = Float.floatToIntBits(f[i]);
        }
        return out;
    }

    private static float[] floats(int[] bits) {
        float[] out = new float[bits.length];
        for (int i = 0; i < bits.length; i++) {
            out[i] = Float.intBitsToFloat(bits[i]);
        }
        return out;
    }

    private static int[] pos(BlockPos p) {
        return new int[]{p.getX(), p.getY(), p.getZ()};
    }

    private static Vec3 vec(int[] a) {
        return a.length < 3 ? Vec3.ZERO : new Vec3(a[0] + 0.5, a[1], a[2] + 0.5);
    }
}
