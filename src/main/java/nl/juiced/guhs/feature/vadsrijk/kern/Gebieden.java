package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * The area plan of zone 1 (CONTRACT_VADSRIJK §2). Boxes are inclusive x/z ranges; the first matching area wins, so the
 * catch-all "ring" is last. Slices may {@link #registreer} extra (smaller) areas before the big ones with {@link #voegVooraan}.
 * <p>
 * z3-systemen: zone 3 is layered (crown / branches / trunk / ground / roots on the same x/z), so an area may carry an optional
 * {@link Laag}: a y-range and/or a cylinder (or ring) around a centre, on top of its box. Old 8-argument areas have no layer and
 * behave exactly as before. Use {@link #op(double, double, double)} wherever a y is known; the 2D {@link #op(double, double)}
 * cannot know the layer, so it skips areas with a y-range (in zone 3 it returns the zone's catch-all ring).
 */
public final class Gebieden {
    /**
     * The optional layer of an area. {@code minY..maxY} inclusive block y; {@code rMax > 0} = a cylinder of radius rMax around
     * (cx, cz), {@code rMin > 0} hollows it into a ring. A layer without a radius is only a y-range.
     */
    public record Laag(int minY, int maxY, int cx, int cz, int rMin, int rMax) {
        public static final int GEEN_MIN = -2048, GEEN_MAX = 4096;

        public static Laag y(int minY, int maxY) {
            return new Laag(minY, maxY, 0, 0, 0, 0);
        }

        public boolean heeftY() {
            return minY > GEEN_MIN || maxY < GEEN_MAX;
        }

        public boolean bevatY(double y) {
            return y >= minY && y < maxY + 1;
        }

        public boolean bevatRond(double x, double z) {
            if (rMax <= 0) {
                return true;
            }
            double dx = x - (cx + 0.5), dz = z - (cz + 0.5), d2 = dx * dx + dz * dz;
            return d2 <= (double) rMax * rMax && (rMin <= 0 || d2 >= (double) rMin * rMin);
        }
    }

    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLevel, int maxLevel, boolean stad, @Nullable Laag laag) {
        /** The flat (zone 1/2) area: a box, every height. */
        public Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLevel, int maxLevel, boolean stad) {
            this(id, minX, minZ, maxX, maxZ, minLevel, maxLevel, stad, null);
        }

        /** A cylinder (rMin > 0: a ring) around (cx, cz) between minY and maxY; its box is the bounding square. */
        public static Gebied cilinder(String id, int cx, int cz, int rMin, int rMax, int minY, int maxY, int minLevel, int maxLevel, boolean stad) {
            return new Gebied(id, cx - rMax, cz - rMax, cx + rMax, cz + rMax, minLevel, maxLevel, stad, new Laag(minY, maxY, cx, cz, rMin, rMax));
        }

        /** This area limited to a y-range (keeps a cylinder if it has one). */
        public Gebied metY(int minY, int maxY) {
            Laag l = laag == null ? Laag.y(minY, maxY) : new Laag(minY, maxY, laag.cx(), laag.cz(), laag.rMin(), laag.rMax());
            return new Gebied(id, minX, minZ, maxX, maxZ, minLevel, maxLevel, stad, l);
        }

        /** This area cut to a cylinder/ring around (cx, cz) (the box stays as a clip: a ring "cut off at the zone box"). */
        public Gebied metRing(int cx, int cz, int rMin, int rMax) {
            Laag l = new Laag(laag == null ? Laag.GEEN_MIN : laag.minY(), laag == null ? Laag.GEEN_MAX : laag.maxY(), cx, cz, rMin, rMax);
            return new Gebied(id, minX, minZ, maxX, maxZ, minLevel, maxLevel, stad, l);
        }

        /** True when this area only covers part of the height (a zone-3 layer). */
        public boolean gelaagd() {
            return laag != null && laag.heeftY();
        }

        /** The box (and cylinder, if any) at x/z, ignoring height. */
        public boolean bevat(double x, double z) {
            return x >= minX && x <= maxX + 0.999 && z >= minZ && z <= maxZ + 0.999 && (laag == null || laag.bevatRond(x, z));
        }

        /** The full test: box, cylinder and y-range. */
        public boolean bevat(double x, double y, double z) {
            return bevat(x, z) && (laag == null || laag.bevatY(y));
        }

        public boolean bevat(BlockPos pos) {
            return bevat(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }

        public BlockPos midden(int y) {
            if (laag != null && laag.rMax() > 0) {
                int r = (laag.rMin() + laag.rMax()) / 2, cx = laag.cx(), cz = laag.cz();
                return laag.rMin() > 0 ? new BlockPos(cx + r, y, cz) : new BlockPos(cx, y, cz);
            }
            return new BlockPos((minX + maxX) / 2, y, (minZ + maxZ) / 2);
        }

        public String naamKey() {
            return "vadsrijk.kern.gebied." + id;
        }
    }

    /** Where a player arrives the first time (Knuffelkamp dock). */
    public static final BlockPos AANKOMST = new BlockPos(0, 65, 1130);

    private static final List<Gebied> ALLE = new ArrayList<>(List.of(
            new Gebied("zwamzoet", -160, -160, 160, 160, 1, 20, true),
            new Gebied("haven", -120, 400, 120, 600, 1, 20, true),
            new Gebied("klodderweide", 160, -300, 600, 400, 1, 6, false),
            new Gebied("sporenwoud", -600, -160, -160, 400, 6, 13, false),
            new Gebied("druipgrotten", -600, -600, -160, -160, 12, 18, false),
            new Gebied("zwamtempel", 160, -600, 600, -300, 18, 20, false),
            new Gebied("knuffelkamp", -128, 900, 128, 1150, 1, 3, true),
            new Gebied("proeftuin", 3000, 3000, 3192, 3192, 1, 20, false),
            new Gebied("ring", -600, -600, 600, 600, 1, 20, false)));

    private Gebieden() {
    }

    public static List<Gebied> alle() {
        return Collections.unmodifiableList(ALLE);
    }

    /** The area at x/z without a height: layered areas (with a y-range) are skipped. Prefer {@link #op(double, double, double)}. */
    @Nullable
    public static synchronized Gebied op(double x, double z) {
        for (Gebied g : ALLE) {
            if (!g.gelaagd() && g.bevat(x, z)) {
                return g;
            }
        }
        return null;
    }

    /** The area at x/y/z (zone-3 layers included); the first match wins. */
    @Nullable
    public static synchronized Gebied op(double x, double y, double z) {
        for (Gebied g : ALLE) {
            if (g.bevat(x, y, z)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public static Gebied op(BlockPos pos) {
        return op(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Nullable
    public static synchronized Gebied van(String id) {
        for (Gebied g : ALLE) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** A smaller area (a dungeon, an arena) that must win over the big boxes. */
    public static synchronized void voegVooraan(Gebied g) {
        ALLE.removeIf(x -> x.id().equals(g.id()));
        ALLE.add(0, g);
    }

    /** The proeftuin (kern test area): its centre at ground level + 1. */
    public static BlockPos proeftuin() {
        return new BlockPos(3096, Vadsrijk.GROND + 1, 3096);
    }
}
