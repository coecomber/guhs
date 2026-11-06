package nl.juiced.guhs.feature.vadsrijk.indeling;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * T-WERELD: THE world layout of Het Vadsrijk (wereldindeling W-3, guhs1100-avontuur/wereldindeling/WERELDINDELING.md). One
 * offset per zone, so every slice can keep placing things in the zone's own LOCAL coordinates:
 * <pre>
 *   world = local + (DX, DZ)          (y is never shifted)
 * </pre>
 * <ul>
 * <li><b>Knuffelkamp</b>: local = the old camp coordinates (island around (0, 1000), arrival (0, 65, 1130)); {@link #KAMP_DX}.</li>
 * <li><b>Zone 1 Zwamzoet</b>: did not move; local = world.</li>
 * <li><b>Zone 2 De Splinterpieken</b>: local = the old zone-2 coordinates (Kandijkom (0, -2400), tools/vadsrijk/zone2/plan.py);
 * {@link #Z2_DX}/{@link #Z2_DZ}.</li>
 * <li><b>Zone 3 De Schemerwortel</b>: local = relative to the trunk axis (tools/vadsrijk/zone3/plan3.py "rel");
 * {@link #Z3_DX}/{@link #Z3_DZ} = the trunk (1404, -576).</li>
 * <li><b>Zone 4 De Lampionkust / zone 5 Hoogvads</b>: new zones, local = world (offset 0); only their boxes live here.</li>
 * </ul>
 * The same numbers are in tools/vadsrijk/indeling.py (the map generators). Change BOTH (and regenerate the map) to move a zone.
 */
public final class VadsrijkZones {
    private VadsrijkZones() {
    }

    // ------------------------------------------------------------------------------------------------ offsets (world = local + d)
    public static final int KAMP_DX = -384, KAMP_DZ = 0;
    public static final int Z1_DX = 0, Z1_DZ = 0;
    public static final int Z2_DX = 704, Z2_DZ = 1344;
    public static final int Z3_DX = 1404, Z3_DZ = -576;
    public static final int Z4_DX = 0, Z4_DZ = 0;
    public static final int Z5_DX = 0, Z5_DZ = 0;
    public static final int Z6_DX = 0, Z6_DZ = 0;   // U-Z6WERELD

    /** The zones of the island sea (zone 1-5) and the mainland (zone 6, U-Z6WERELD). */
    public enum Zone {
        KAMP("kamp", KAMP_DX, KAMP_DZ, -512, 832, -193, 1215),
        Z1("z1", Z1_DX, Z1_DZ, -704, -704, 767, 639),
        Z2("z2", Z2_DX, Z2_DZ, 384, -1472, 1023, -769),
        Z3("z3", Z3_DX, Z3_DZ, 1024, -1024, 1727, -129),
        Z4("z4", Z4_DX, Z4_DZ, 1728, -768, 2559, -257),
        Z5("z5", Z5_DX, Z5_DZ, 2432, -768, 3199, -257),
        /** U-Z6WERELD: Het Zijdeland, the first part of the mainland (zone box x 3280..4180, z -1060..-460 + a coast margin). */
        Z6("z6", Z6_DX, Z6_DZ, 3264, -1152, 4287, -385);

        public final String id;
        public final int dx, dz;
        /** The tile box (inclusive world blocks): the zone's own map tiles. Never visible in game. */
        public final int minX, minZ, maxX, maxZ;

        Zone(String id, int dx, int dz, int minX, int minZ, int maxX, int maxZ) {
            this.id = id;
            this.dx = dx;
            this.dz = dz;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        public boolean inVak(double x, double z) {
            return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
        }

        /** local -> world. */
        public BlockPos wereld(int x, int y, int z) {
            return new BlockPos(x + dx, y, z + dz);
        }

        public Vec3 wereld(double x, double y, double z) {
            return new Vec3(x + dx, y, z + dz);
        }

        /** world -> local. */
        public BlockPos lokaal(BlockPos p) {
            return p.offset(-dx, 0, -dz);
        }
    }

    /** Zone 5 floats above the east end of zone 4: in the shared tile columns, at/above this y it is zone 5. */
    public static final int Z5_VANAF_Y = 150;

    // ------------------------------------------------------------------------------------------------ land boxes (content, W-3 §2)
    /** Land (content) boxes after the shift: minX, minZ, maxX, maxZ. The coast runs out naturally inside these. */
    public static final int[] LAND_KAMP = {-488, 886, -258, 1106};
    public static final int[] LAND_Z1 = {-640, -650, 640, 650};
    public static final int[] LAND_Z2 = {434, -1436, 974, -806};
    public static final int[] LAND_Z3 = {1166, -897, 1725, -338};
    public static final int[] LAND_Z4 = {1740, -716, 2540, -266};
    public static final int[] LAND_Z5 = {2440, -660, 3150, -380};
    public static final int[] LAND_Z6 = {3280, -1060, 4180, -460};   // U-Z6WERELD

    // ------------------------------------------------------------------------------------------------ helpers
    /** The zone whose tile box holds x/z (zone 4 wins the zone-4/5 overlap: use {@link #zoneVan(double, double, double)}). */
    @Nullable
    public static Zone zoneVan(double x, double z) {
        for (Zone zo : Zone.values()) {
            if (zo != Zone.Z5 && zo.inVak(x, z)) {
                return zo;
            }
        }
        return Zone.Z5.inVak(x, z) ? Zone.Z5 : null;
    }

    /** The zone at x/y/z: in the zone-4/5 overlap, zone 5 from {@link #Z5_VANAF_Y} up. */
    @Nullable
    public static Zone zoneVan(double x, double y, double z) {
        if (y >= Z5_VANAF_Y && Zone.Z5.inVak(x, z)) {
            return Zone.Z5;
        }
        return zoneVan(x, z);
    }

    /** 1 inside the zone's tile box, fading linearly to 0 at {@code fade} blocks outside it (skies, ambience). */
    public static float nabij(Zone zo, double x, double z, double fade) {
        double dx = Math.max(Math.max(zo.minX - x, x - (zo.maxX + 1)), 0), dz = Math.max(Math.max(zo.minZ - z, z - (zo.maxZ + 1)), 0);
        return (float) Math.max(0, Math.min(1, 1 - Math.hypot(dx, dz) / fade));
    }

    public static boolean in(Zone zo, double x, double z) {
        return zo.inVak(x, z);
    }

    public static boolean inKamp(double x, double z) {
        return Zone.KAMP.inVak(x, z);
    }

    public static boolean inZone1(double x, double z) {
        return Zone.Z1.inVak(x, z);
    }

    public static boolean inZone2(double x, double z) {
        return Zone.Z2.inVak(x, z);
    }

    public static boolean inZone3(double x, double z) {
        return Zone.Z3.inVak(x, z);
    }

    public static boolean inZone4(double x, double z) {
        return Zone.Z4.inVak(x, z);
    }

    public static boolean inZone6(double x, double z) {   // U-Z6WERELD
        return Zone.Z6.inVak(x, z);
    }

    /** Old camp coordinates -> world. */
    public static BlockPos kamp(int x, int y, int z) {
        return new BlockPos(x + KAMP_DX, y, z + KAMP_DZ);
    }

    public static Vec3 kampV(double x, double y, double z) {
        return new Vec3(x + KAMP_DX, y, z + KAMP_DZ);
    }

    /**
     * Line 0 (Knuffelkamp <-> De Haven, camera paths over the Zoete Zee): old coordinates bent west with the camp. Full camp shift
     * at z >= 860 (the island), none at z <= 500 (De Haven), linear between. Same formula as bouwsels.kamp_meng (the map).
     */
    public static Vec3 kampMeng(double x, double y, double z) {
        double m = Math.max(0, Math.min(1, (z - 500) / 360.0));
        return new Vec3(x + KAMP_DX * m, y, z + KAMP_DZ * m);
    }

    /** Old (local) zone-2 coordinates -> world. */
    public static BlockPos z2(int x, int y, int z) {
        return new BlockPos(x + Z2_DX, y, z + Z2_DZ);
    }

    public static Vec3 z2V(double x, double y, double z) {
        return new Vec3(x + Z2_DX, y, z + Z2_DZ);
    }

    /** Zone-3 trunk-relative coordinates -> world. */
    public static BlockPos z3(int x, int y, int z) {
        return new BlockPos(x + Z3_DX, y, z + Z3_DZ);
    }
}
