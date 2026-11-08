package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * U-Z6RAID: the raid's spots. The tower OUTSIDE (and its door) is u-z6wereld's; the INSIDE is our own template
 * ({@code tools/vadsrijk/zone6/raid/binnentoren.py}, placed by {@link Z6RaidBouw}) in a pocket under the tower. Everything inside
 * is relative to the anchor {@link #anker()} (the feet spot in the middle of floor 1); the generator uses the same numbers.
 * <p><b>POI ids wanted from u-z6wereld</b> (asked by reflection on {@code zone6.wereld.Z6WereldPlekken.plek(String)}, like zone 4):
 * <ul>
 *   <li>{@code raid_toren_poort}: the spot in the tower door (iwan) where you walk in (a 3x4x3 box around it teleports you to the
 *       Wachtzaal). Placeholder: the iwan of the render, west side of the tower foot (4123, 71, -688);</li>
 *   <li>{@code raid_toren_buiten}: where you come out (in front of the door). Placeholder (4118, 71, -688);</li>
 *   <li>{@code raid_toren_binnen} (optional): the anchor of the pocket. Placeholder (4140, 16, -688): right under the tower; the
 *       template reaches y 2..68 and x -25..+34 / z -25..+25 around it. <b>u-z6wereld: please keep caves/water out of that box.</b></li>
 * </ul>
 */
public final class Z6RaidPlekken {
    /** Feet height difference between two floors (binnentoren.VERDIEPING). */
    public static final int VERDIEPING = 12;
    /** The arena radius on every floor (the walkable octagon is ~21.5). */
    public static final int STRAAL = 21;
    /** Template min corner relative to the anchor (binnentoren.MIN). */
    public static final BlockPos TEMPLATE_MIN = new BlockPos(-25, -14, -25);
    /** Floor 1 hourglass plates (x, z). */
    public static final int[][] PLATEN = {{-12, 0}, {12, 0}};
    public static final double PLAAT_R = 2.6;
    /** Floor 0: the start circle (x, z, r) and the exit (x, z). */
    public static final int[] START = {-6, 0};
    public static final double START_R = 3.2;
    public static final int[] UITGANG = {-20, 0};

    private static boolean gezocht;
    @Nullable
    private static Method wereldPlek;

    private Z6RaidPlekken() {
    }

    @Nullable
    public static BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken");
                wereldPlek = c.getMethod("plek", String.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                wereldPlek = null;
            }
        }
        if (wereldPlek == null) {
            return null;
        }
        try {
            Object o = wereldPlek.invoke(null, id);
            if (o instanceof BlockPos b) {
                return b;
            }
            if (o instanceof int[] a && a.length >= 3) {
                return new BlockPos(a[0], a[1], a[2]);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // fall back to the placeholder
        }
        return null;
    }

    private static BlockPos p(String id, int x, int y, int z) {
        BlockPos w = wereld(id);
        return w != null ? w : new BlockPos(x, y, z);
    }

    /** Dev / gametests: a fixed anchor overrides everything (null = the POI / placeholder). */
    @Nullable
    public static BlockPos testAnker;

    public static BlockPos anker() {
        return testAnker != null ? testAnker : p("raid_toren_binnen", 4140, 16, -688);
    }

    public static BlockPos poort() {
        return p("raid_toren_poort", 4123, 71, -688);
    }

    public static BlockPos buiten() {
        return p("raid_toren_buiten", 4118, 71, -688);
    }

    /** Feet height offset of floor k (0 = Wachtzaal, 1-4 the bosses). */
    public static int dy(int k) {
        return VERDIEPING * (k - 1);
    }

    /** The middle of floor k (feet). */
    public static BlockPos midden(int k) {
        return anker().offset(0, dy(k), 0);
    }

    /** The checkpoint of floor k: its landing east of the hall (you wake up here). */
    public static BlockPos landing(int k) {
        return anker().offset(25, dy(k), 1);
    }

    /** The sterrenvlies gate of floor k (closes flight A of its stair up): 6 blocks. */
    public static BlockPos[] poortBlokken(int k) {
        BlockPos a = anker();
        BlockPos[] l = new BlockPos[6];
        int i = 0;
        for (int x = 28; x <= 29; x++) {
            for (int y = 1; y <= 3; y++) {
                l[i++] = a.offset(x, dy(k) + y, 2);
            }
        }
        return l;
    }

    public static Vec3 plaat(int i) {
        return Vec3.atBottomCenterOf(anker().offset(PLATEN[i][0], dy(1), PLATEN[i][1]));
    }

    public static Vec3 start() {
        return Vec3.atBottomCenterOf(anker().offset(START[0], dy(0), START[1]));
    }

    public static BlockPos uitgang() {
        return anker().offset(UITGANG[0], dy(0), UITGANG[1]);
    }

    /** A spot on a ring round the middle of floor k (feet), angle in radians. */
    public static Vec3 ring(int k, double r, double hoek) {
        BlockPos m = midden(k);
        return new Vec3(m.getX() + 0.5 + Math.cos(hoek) * r, m.getY(), m.getZ() + 0.5 + Math.sin(hoek) * r);
    }

    /** Is this position inside the pocket (any floor, the landings and the stair annex)? */
    public static boolean inToren(double x, double y, double z) {
        BlockPos a = anker();
        double dx = x - a.getX(), dz = z - a.getZ(), dy = y - a.getY();
        return dx >= -26 && dx <= 35 && dz >= -26 && dz <= 26 && dy >= -15 && dy <= 53;
    }

    /** Which floor this position is on (0-4), by height; -1 outside the pocket. */
    public static int verdieping(double x, double y, double z) {
        if (!inToren(x, y, z)) {
            return -1;
        }
        double dy = y - anker().getY();
        return Math.max(0, Math.min(4, (int) Math.floor((dy + VERDIEPING + 2) / VERDIEPING)));
    }

    /** In the arena (the hall, not the landing) of floor k? */
    public static boolean inArena(int k, double x, double y, double z) {
        BlockPos m = midden(k);
        double dx = x - (m.getX() + 0.5), dz = z - (m.getZ() + 0.5), dy = y - m.getY();
        return dx * dx + dz * dz <= STRAAL * STRAAL && dy > -2 && dy < (k == 4 ? 16 : 10);
    }
}
