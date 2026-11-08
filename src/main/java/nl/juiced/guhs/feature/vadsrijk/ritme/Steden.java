package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;

/**
 * U-LEVEN-RITME: the towns of Het Vadsrijk (zone 1-6) as this slice sees them: a box (or ellipse/cylinder) per town that works
 * on the server AND on the client (kern {@code Gebieden} only gets the zone 2-6 areas at server start, so a client on a dedicated
 * server would not know them), the zone (weather), and the anchors for the notice board and the night market.
 * <p>Anchors are map POIs looked up by name in the generated {@code *WereldPlekken.ALLE} maps (so they follow a regenerated
 * map); zone 6 ({@code zone6.wereld.Z6WereldPlekken}, branch 110-u-z6wereld) is read by reflection with the u-z6wereld numbers
 * as fallback, so it works before and after that merge. The area ids are the same as kern's ({@code z6_rozenkloof}, ...).
 */
public final class Steden {
    /** box: minX, minZ, maxX, maxZ (inclusive), y range; ellipse: centre + radii; cylinder: centre + radius. */
    public record Stad(String id, int zone, int minX, int minZ, int maxX, int maxZ, int minY, int maxY, Vorm vorm,
            @Nullable Supplier<BlockPos> bord, @Nullable Supplier<BlockPos> markt, List<String> gebieden) {
        public boolean bevat(double x, double y, double z) {
            if (y < minY || y > maxY + 0.999) {
                return false;
            }
            return switch (vorm) {
                case DOOS -> x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
                case OVAAL -> {
                    double cx = (minX + maxX + 1) / 2.0, cz = (minZ + maxZ + 1) / 2.0, rx = (maxX + 1 - minX) / 2.0, rz = (maxZ + 1 - minZ) / 2.0;
                    double dx = (x - cx) / rx, dz = (z - cz) / rz;
                    yield dx * dx + dz * dz <= 1.0;
                }
            };
        }

        /** Only x/z (the client's chunk scans). */
        public boolean bevatXZ(double x, double z) {
            return bevat(x, (minY + maxY) / 2.0, z);
        }

        public BlockPos midden() {
            return new BlockPos((minX + maxX) / 2, Math.max(minY, Math.min(maxY, 70)), (minZ + maxZ) / 2);
        }

        public String naamKey() {
            return Ritme.P + "stad." + id;
        }

        /** "Het Zwamzoetse Pluizenblaadje". */
        public String krantKey() {
            return Ritme.P + "krant.titel." + id;
        }
    }

    public enum Vorm {
        DOOS, OVAAL
    }

    private static final int ALLE_Y_MIN = -64, ALLE_Y_MAX = 400;

    private static Supplier<BlockPos> z1(String poi, BlockPos fb) {
        return () -> uit(nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.ALLE, poi, fb);
    }

    private static Supplier<BlockPos> z2(String poi, BlockPos fb) {
        return () -> uit(nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ALLE, poi, fb);
    }

    private static Supplier<BlockPos> z3(String poi, BlockPos fb) {
        return () -> uit(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ALLE, poi, fb);
    }

    private static Supplier<BlockPos> z4(String poi, BlockPos fb) {
        return () -> uit(nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE, poi, fb);
    }

    private static Supplier<BlockPos> z5(String poi, BlockPos fb) {
        return () -> uit(nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.ALLE, poi, fb);
    }

    /** Zone 6 lives on another branch until the merge: reflection, the u-z6wereld coordinates as fallback. */
    private static Supplier<BlockPos> z6(String poi, BlockPos fb) {
        return () -> {
            try {
                Object m = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken").getField("ALLE").get(null);
                if (m instanceof Map<?, ?> map && map.get(poi) instanceof BlockPos b) {
                    return b;
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                // (not merged yet)
            }
            return fb;
        };
    }

    private static BlockPos uit(Map<String, BlockPos> m, String poi, BlockPos fb) {
        BlockPos b = m.get(poi);
        return b == null ? fb : b;
    }

    /** The towns, in a fixed order (zone order). Boxes copied from kern/zone area plans (see the gametest that compares them). */
    public static final List<Stad> ALLE = List.of(
            new Stad("knuffelkamp", 0, -128 + VadsrijkZones.KAMP_DX, 900, 128 + VadsrijkZones.KAMP_DX, 1150, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    null, null, List.of("knuffelkamp")),
            new Stad("zwamzoet", 1, -160, -160, 160, 160, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    z1("questbord", new BlockPos(-7, 64, 31)), z1("plein", new BlockPos(0, 64, 59)), List.of("zwamzoet")),
            new Stad("haven", 1, -120, 400, 120, 600, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    z1("haven_plein", new BlockPos(90, 65, 470)), null, List.of("haven")),
            new Stad("kandijkom", 2, 620, -1140, 788, -856, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    z2("questbord", new BlockPos(716, 91, -1075)), z2("plein", new BlockPos(704, 91, -1068)), List.of("z2_kandijkom", "z2_kandijkom_dok")),
            new Stad("honingtak", 3, 1404 - 160, -576 - 160, 1404 + 160, -576 + 160, 145, 200, Vorm.OVAAL,
                    z3("questbord", new BlockPos(1398, 167, -518)), null, List.of("z3_honingtak")),
            new Stad("lampionhaven", 4, 1846 - 78, -470 - 72, 1846 + 78, -470 + 72, ALLE_Y_MIN, 149, Vorm.OVAAL,
                    z4("questbord", new BlockPos(1842, 67, -466)), z4("plein", new BlockPos(1848, 67, -467)), List.of("z4_lampionhaven")),
            new Stad("hoogvads", 5, 2526, -625, 2907, -415, VadsrijkZones.Z5_VANAF_Y, ALLE_Y_MAX, Vorm.DOOS,
                    z5("markt_fontein", new BlockPos(2748, 219, -529)), null,
                    List.of("z5_paleisheuvel", "z5_universiteit", "z5_academie", "z5_marktlaan", "z5_kathedraal", "z5_luchthaven", "z5_staartpark",
                            "z5_ooggaarde", "z5_hoogvads")),
            new Stad("rozenkloof", 6, 3236, -800, 3500, -652, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    z6("rozenkloof_plein", new BlockPos(3392, 69, -714)), null, List.of("z6_rozenkloof")),
            new Stad("saffraanpoort", 6, 3634, -950, 3890, -742, ALLE_Y_MIN, ALLE_Y_MAX, Vorm.DOOS,
                    z6("saffraanpoort_plein", new BlockPos(3756, 71, -863)), z6("saffraanpoort_bazaar", new BlockPos(3759, 71, -810)),
                    List.of("z6_saffraanpoort")));

    private Steden() {
    }

    @Nullable
    public static Stad van(@Nullable String id) {
        for (Stad s : ALLE) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** The town at this spot, or null (wilderness). */
    @Nullable
    public static Stad op(double x, double y, double z) {
        for (Stad s : ALLE) {
            if (s.bevat(x, y, z)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static Stad op(BlockPos p) {
        return op(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    /** The town of a kern area id (z6_rozenkloof -> rozenkloof), or null. */
    @Nullable
    public static Stad vanGebied(@Nullable String gebied) {
        if (gebied == null) {
            return null;
        }
        for (Stad s : ALLE) {
            if (s.gebieden().contains(gebied)) {
                return s;
            }
        }
        return null;
    }

    // ---- zones (weather) ---------------------------------------------------------------------------------------------------------
    /** U-Z6WERELD's zone-6 tile box (VadsrijkZones.Zone.Z6 after the merge). */
    public static final int Z6_MIN_X = 3264, Z6_MIN_Z = -1152, Z6_MAX_X = 4287, Z6_MAX_Z = -385;

    /** The zone number at x/y/z: 0 = Knuffelkamp, 1..6, -1 = none (open sea between). */
    public static int zone(double x, double y, double z) {
        if (x >= Z6_MIN_X && x < Z6_MAX_X + 1 && z >= Z6_MIN_Z && z < Z6_MAX_Z + 1) {
            return 6;
        }
        VadsrijkZones.Zone zo = VadsrijkZones.zoneVan(x, y, z);
        if (zo == null) {
            return -1;
        }
        return switch (zo) {
            case KAMP -> 0;
            case Z1 -> 1;
            case Z2 -> 2;
            case Z3 -> 3;
            case Z4 -> 4;
            case Z5 -> 5;
            default -> 6;   // (U-Z6WERELD adds Z6 to the enum)
        };
    }
}
