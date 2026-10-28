package nl.juiced.guhs.feature.zingendeweide;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Drawing the meadow's big shapes (pillars, arches, giant trees, crystals, pools...) into a {@link Zet}: the features place
 * straight into the world, the structures clip to the chunk they finish, the test commands place in front of you. All
 * shapes are pure functions of their arguments and the random they get, so a structure drawn chunk by chunk stays whole.
 */
public final class WeideTeken {
    private WeideTeken() {
    }

    /** Where the blocks go. */
    public interface Zet {
        void zet(int x, int y, int z, BlockState state);

        BlockState get(int x, int y, int z);

        /** Overwrite only air, plants and fluids (the rest of a shape that pokes into terrain stays the terrain's). */
        default void zacht(int x, int y, int z, BlockState state) {
            BlockState s = get(x, y, z);
            if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) {
                zet(x, y, z, state);
            }
        }
    }

    // --- the palette ----------------------------------------------------------------------------------------------------------
    static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    static BlockState gras() {
        return s(ZingendeWeideFeature.ZALMKLEI_GRAS.get());
    }

    static BlockState groen() {
        return s(ZingendeWeideFeature.ZEEGROEN_GRAS.get());
    }

    static BlockState klei() {
        return s(ZingendeWeideFeature.ZALMKLEI.get());
    }

    /** The striped cliffs: the same bands as the surface rule (tools/features/zingendeweide.py: _streepjes). */
    static BlockState streep(int y) {
        int n = Math.floorMod(Math.floorDiv(y, 2), 5);
        return switch (n) {
            case 1, 4 -> s(ZingendeWeideFeature.ZALMKLEI_STREEP_A.get());
            case 3 -> s(ZingendeWeideFeature.ZALMKLEI_STREEP_B.get());
            default -> klei();
        };
    }

    static BlockState sap() {
        return s(ZingendeWeideFeature.BUBBELSAP_BLOK.get());
    }

    static BlockState blad(RandomSource r, double roze) {
        Block b = r.nextDouble() < roze ? ZingendeWeideFeature.BLADEREN_ROZE.get() : ZingendeWeideFeature.BLADEREN.get();
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    static BlockState kristal(RandomSource r) {
        return switch (r.nextInt(3)) {
            case 0 -> s(ZingendeWeideFeature.KRISTAL_CYAAN.get());
            case 1 -> s(ZingendeWeideFeature.KRISTAL_LILA.get());
            default -> s(ZingendeWeideFeature.KRISTAL_ROZE.get());
        };
    }

    static BlockState kristalletjes(RandomSource r, Direction op) {
        Block b = switch (r.nextInt(3)) {
            case 0 -> ZingendeWeideFeature.KRISTALLETJES_CYAAN.get();
            case 1 -> ZingendeWeideFeature.KRISTALLETJES_LILA.get();
            default -> ZingendeWeideFeature.KRISTALLETJES_ROZE.get();
        };
        return b.defaultBlockState().setValue(BlockStateProperties.FACING, op);
    }

    /** Pink guh-ear wool: the outside, and the inside of an ear. */
    static BlockState oorBuiten() {
        return s(Blocks.PINK_TERRACOTTA);
    }

    static BlockState oorBinnen() {
        return s(Blocks.PINK_WOOL);
    }

    // --- shapes -------------------------------------------------------------------------------------------------------------------

    /**
     * A rock pillar of striped salmon clay, from the ground (y0) up h blocks; radius rad, thinning to the top; leaning (dx, dz
     * blocks over its height); with a cap (grass on top, hanging strands underneath) that spreads out to {@code kap} past
     * the column (0: none); guh ears on top; a little bubbelsap spring on its side (a waterfall).
     */
    public static void pilaar(Zet z, RandomSource r, int x0, int y0, int z0, int h, double rad, double dx, double dz, int kap, boolean oortjes, boolean bron) {
        double topRad = Math.max(1.2, rad * 0.65);
        int top = y0 + h;
        // into the ground a little, so it never floats on a slope
        for (int y = y0 - 4; y <= top; y++) {
            double t = Mth.clamp((y - y0) / (double) h, 0, 1);
            double r0 = Mth.lerp(t, rad, topRad) + (y < y0 + 2 ? 0.8 : 0) + Math.sin(y * 0.7 + x0) * 0.25;
            double cx = x0 + 0.5 + dx * t, cz = z0 + 0.5 + dz * t;
            int ri = (int) Math.ceil(r0);
            for (int a = -ri; a <= ri; a++) {
                for (int b = -ri; b <= ri; b++) {
                    double ddx = (Math.floor(cx) + a + 0.5) - cx, ddz = (Math.floor(cz) + b + 0.5) - cz;
                    if (ddx * ddx + ddz * ddz <= r0 * r0) {
                        z.zet((int) Math.floor(cx) + a, y, (int) Math.floor(cz) + b, y == top && kap == 0 ? groen() : streep(y));
                    }
                }
            }
        }
        double tx = x0 + 0.5 + dx, tz = z0 + 0.5 + dz;
        if (kap > 0) {
            // a mushroom-like cap: a flat disc a little wider than the top, its underside clay, its top grass, strands below
            double kr = topRad + kap;
            int ki = (int) Math.ceil(kr);
            for (int a = -ki; a <= ki; a++) {
                for (int b = -ki; b <= ki; b++) {
                    double d = Math.sqrt(a * a + b * b);
                    if (d > kr) {
                        continue;
                    }
                    int x = (int) Math.floor(tx) + a, zz = (int) Math.floor(tz) + b;
                    int dik = d < kr - 1.5 ? 2 : 1;
                    for (int k = 0; k < dik; k++) {
                        z.zet(x, top - k, zz, k == 0 ? groen() : streep(top - k));
                    }
                    if (d > topRad + 0.5 && r.nextInt(5) == 0) {
                        hangend(z, x, top - dik, zz, 1 + r.nextInt(4));
                    }
                    if (k(r, 6) && d < kr - 1) {
                        WeideFeatures.plantje(z, x, top + 1, zz, r);
                    }
                }
            }
        }
        if (oortjes) {
            oortjes(z, (int) Math.floor(tx), top + 1, (int) Math.floor(tz), Math.max(2, (int) Math.round(topRad + kap * 0.5)), r.nextBoolean());
        }
        if (bron) {
            // a notch near the top with a spring: the sap runs down the side
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int by = top - 3 - r.nextInt(Math.max(1, h / 3));
            double t = (by - y0) / (double) h;
            double rr = Mth.lerp(t, rad, topRad);
            int bx = (int) Math.floor(x0 + 0.5 + dx * t + d.getStepX() * (rr + 0.5)), bz = (int) Math.floor(z0 + 0.5 + dz * t + d.getStepZ() * (rr + 0.5));
            z.zet(bx, by, bz, sap());
            z.zet(bx, by - 1, bz, streep(by - 1));
            z.zet(bx + d.getClockWise().getStepX(), by, bz + d.getClockWise().getStepZ(), streep(by));
            z.zet(bx - d.getClockWise().getStepX(), by, bz - d.getClockWise().getStepZ(), streep(by));
        }
    }

    private static boolean k(RandomSource r, int n) {
        return r.nextInt(n) == 0;
    }

    /** Two round guh ears on a top at (x, y, z), spread out by {@code wijd}; turned along x or z. */
    public static void oortjes(Zet z, int x, int y, int zz, int wijd, boolean langsX) {
        for (int kant = -1; kant <= 1; kant += 2) {
            int ox = langsX ? x + kant * wijd : x, oz = langsX ? zz : zz + kant * wijd;
            for (int a = -2; a <= 2; a++) {
                for (int b = 0; b <= 4; b++) {
                    double dx = a, dy = b - 2;
                    double d = dx * dx + dy * dy;
                    if (d > 5.2) {
                        continue;
                    }
                    // (flat in the plane the ears are spread in, two blocks thick: the pink inside on both faces)
                    BlockState st = d <= 1.2 && b >= 1 ? oorBinnen() : oorBuiten();
                    int px = langsX ? ox + a : ox, pz = langsX ? oz : oz + a;
                    z.zet(px, y + b, pz, st);
                    z.zet(langsX ? px : px + 1, y + b, langsX ? pz + 1 : pz, st);
                }
            }
        }
    }

    /** Hanging strands under (x, y, z), {@code lengte} long (the last one a tip). */
    public static void hangend(Zet z, int x, int y, int zz, int lengte) {
        BlockState sliert = ZingendeWeideFeature.SLIERT.get().defaultBlockState();
        for (int i = 1; i <= lengte; i++) {
            if (!z.get(x, y - i, zz).isAir()) {
                if (i > 1) {
                    z.zet(x, y - i + 1, zz, sliert.setValue(HangingMossBlock.TIP, true));
                }
                return;
            }
            z.zet(x, y - i, zz, sliert.setValue(HangingMossBlock.TIP, i == lengte));
        }
    }

    /**
     * A natural arch whose outline is a guh's head: two legs, a rounded span, and two round ears on top. From (x, y0, z),
     * {@code breed} from leg to leg along x (langsX) or z, {@code hoog} high.
     */
    public static void boog(Zet z, RandomSource r, int x, int y0, int zz, int breed, int hoog, boolean langsX) {
        double half = breed / 2.0;
        int dik = 3;
        for (int a = (int) -Math.ceil(half) - 2; a <= Math.ceil(half) + 2; a++) {
            for (int y = y0 - 3; y <= y0 + hoog + 3; y++) {
                // outer ellipse minus inner ellipse (the hole), plus legs into the ground
                double ox = a / (half + 2.0), oy = (y - y0) / (hoog + 2.0);
                double ix = a / (half - 1.0), iy = (y - y0) / (hoog - 1.5);
                boolean buiten = ox * ox + oy * oy <= 1.0 && y >= y0 - 3;
                boolean gat = ix * ix + iy * iy < 1.0 && y >= y0;
                if (!buiten || gat) {
                    continue;
                }
                for (int b = -dik / 2; b <= dik / 2; b++) {
                    int px = langsX ? x + a : x + b, pz = langsX ? zz + b : zz + a;
                    z.zet(px, y, pz, y == top(y0, hoog, a, half) ? groen() : streep(y));
                }
            }
        }
        // grass on its back and a few strands under the span
        for (int a = (int) -Math.floor(half) + 1; a <= Math.floor(half) - 1; a++) {
            double iy = Math.sqrt(Math.max(0, 1 - (a / (half - 1.0)) * (a / (half - 1.0)))) * (hoog - 1.5);
            int under = y0 + (int) Math.ceil(iy);
            if (r.nextInt(3) == 0) {
                int px = langsX ? x + a : x, pz = langsX ? zz : zz + a;
                hangend(z, px, under, pz, 1 + r.nextInt(3));
            }
        }
        oortjes(z, x, y0 + hoog + 2, zz, Math.max(3, (int) (half * 0.55)), langsX);
    }

    private static int top(int y0, int hoog, int a, double half) {
        double ox = a / (half + 2.0);
        return y0 + (int) Math.floor(Math.sqrt(Math.max(0, 1 - ox * ox)) * (hoog + 2.0));
    }

    /**
     * A giant wolkjesboom: a thick grey trunk (2x2 or 3x3) with root feet, a great cloudy crown of white and pink leaves made
     * of overlapping blobs, long strands below, and now and then a sleepy guh face in the trunk.
     */
    public static void reuzenboom(Zet z, RandomSource r, int x, int y0, int zz, int hoog, boolean gezicht) {
        int dik = hoog >= 24 ? 3 : 2;
        BlockState stam = ZingendeWeideFeature.STAM.get().defaultBlockState();
        for (int y = y0 - 2; y < y0 + hoog; y++) {
            for (int a = 0; a < dik; a++) {
                for (int b = 0; b < dik; b++) {
                    z.zet(x + a, y, zz + b, stam);
                }
            }
        }
        // root feet
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int len = 1 + r.nextInt(3);
            for (int i = 1; i <= len; i++) {
                int ox = x + (d.getStepX() > 0 ? dik - 1 + i : d.getStepX() < 0 ? -i : r.nextInt(dik));
                int oz = zz + (d.getStepZ() > 0 ? dik - 1 + i : d.getStepZ() < 0 ? -i : r.nextInt(dik));
                for (int y = y0 - 2; y < y0 + (len - i + 1) - 1 + 1; y++) {
                    z.zet(ox, y, oz, ZingendeWeideFeature.HOUT.get().defaultBlockState());
                }
            }
        }
        if (gezicht) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int gy = y0 + 3 + r.nextInt(3);
            int gx = x + (d.getStepX() > 0 ? dik - 1 : d.getStepX() < 0 ? 0 : dik / 2), gz = zz + (d.getStepZ() > 0 ? dik - 1 : d.getStepZ() < 0 ? 0 : dik / 2);
            z.zet(gx, gy, gz, ZingendeWeideFeature.GEZICHT.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d));
        }
        // a few branches and the crown: blobs around the top
        double mx = x + dik / 2.0, mz = zz + dik / 2.0;
        int kruin = y0 + hoog;
        int blobs = 6 + r.nextInt(4);
        double straal = 4.5 + hoog * 0.18;
        kruinBlob(z, r, mx, kruin, mz, straal, 0.3);
        for (int i = 0; i < blobs; i++) {
            double hoek = r.nextDouble() * Math.PI * 2, ver = straal * (0.55 + r.nextDouble() * 0.45);
            double bx = mx + Math.cos(hoek) * ver, bz = mz + Math.sin(hoek) * ver, by = kruin - 1.5 + r.nextDouble() * 3;
            tak(z, mx, kruin - 3, mz, bx, by, bz);
            kruinBlob(z, r, bx, by, bz, straal * (0.45 + r.nextDouble() * 0.2), 0.3);
        }
        // strands under the crown
        int si = (int) Math.ceil(straal * 1.4);
        for (int a = -si; a <= si; a++) {
            for (int b = -si; b <= si; b++) {
                if (r.nextInt(6) != 0) {
                    continue;
                }
                int px = (int) Math.floor(mx) + a, pz = (int) Math.floor(mz) + b;
                for (int y = kruin + 4; y >= kruin - (int) straal - 2; y--) {
                    if (z.get(px, y, pz).getBlock() instanceof LeavesBlock && z.get(px, y - 1, pz).isAir()) {
                        hangend(z, px, y, pz, 2 + r.nextInt(6));
                        break;
                    }
                }
            }
        }
    }

    /** A soft blob of leaves (squashed a little: wider than high). */
    static void kruinBlob(Zet z, RandomSource r, double cx, double cy, double cz, double rad, double roze) {
        int ri = (int) Math.ceil(rad);
        for (int a = -ri; a <= ri; a++) {
            for (int b = -ri / 2 - 1; b <= ri / 2 + 2; b++) {
                for (int c = -ri; c <= ri; c++) {
                    double dx = a, dy = b * 1.6, dz = c;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + r.nextDouble() * 0.8;
                    if (d <= rad) {
                        z.zacht((int) Math.floor(cx) + a, (int) Math.floor(cy) + b, (int) Math.floor(cz) + c, blad(r, roze));
                    }
                }
            }
        }
    }

    static void tak(Zet z, double x0, double y0, double z0, double x1, double y1, double z1) {
        int n = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0))));
        BlockState hout = ZingendeWeideFeature.HOUT.get().defaultBlockState();
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : i / (double) n;
            z.zet((int) Math.floor(Mth.lerp(t, x0, x1)), (int) Math.floor(Mth.lerp(t, y0, y1)), (int) Math.floor(Mth.lerp(t, z0, z1)), hout);
        }
    }

    /** A small wolkjesboom (the sapling's): a thin trunk and a round crown with a few strands. */
    public static void boompje(Zet z, RandomSource r, int x, int y0, int zz) {
        int hoog = 4 + r.nextInt(4);
        BlockState stam = ZingendeWeideFeature.STAM.get().defaultBlockState();
        for (int y = y0; y < y0 + hoog; y++) {
            z.zet(x, y, zz, stam);
        }
        double rad = 2.6 + r.nextDouble() * 1.2;
        kruinBlob(z, r, x + 0.5, y0 + hoog, zz + 0.5, rad, 0.25);
        for (int i = 0; i < 4; i++) {
            int px = x + r.nextInt(5) - 2, pz = zz + r.nextInt(5) - 2;
            for (int y = y0 + hoog + 2; y >= y0 + 2; y--) {
                if (z.get(px, y, pz).getBlock() instanceof LeavesBlock && z.get(px, y - 1, pz).isAir()) {
                    hangend(z, px, y, pz, 1 + r.nextInt(3));
                    break;
                }
            }
        }
    }

    /** A cluster of pastel crystals: a few leaning columns and little crystals around them. */
    public static void kristallen(Zet z, RandomSource r, int x, int y0, int zz) {
        int n = 2 + r.nextInt(4);
        for (int i = 0; i < n; i++) {
            int px = x + r.nextInt(5) - 2, pz = zz + r.nextInt(5) - 2;
            int h = 1 + r.nextInt(i == 0 ? 5 : 3);
            int lx = r.nextInt(3) - 1, lz = r.nextInt(3) - 1;
            BlockState st = kristal(r);
            for (int y = 0; y < h; y++) {
                z.zet(px + (y >= h / 2 + 1 ? lx : 0), y0 + y, pz + (y >= h / 2 + 1 ? lz : 0), st);
            }
            z.zacht(px + (h >= 2 ? lx : 0), y0 + h, pz + (h >= 2 ? lz : 0), kristalletjes(r, Direction.UP));
        }
        for (int i = 0; i < 6; i++) {
            int px = x + r.nextInt(7) - 3, pz = zz + r.nextInt(7) - 3;
            if (z.get(px, y0, pz).isAir() && !z.get(px, y0 - 1, pz).isAir() && z.get(px, y0 - 1, pz).getFluidState().isEmpty()) {
                z.zet(px, y0, pz, kristalletjes(r, Direction.UP));
            }
        }
    }

    /** A bubbelsap pool: an oval basin {@code diep} deep, with a rim of polished seagreen stone and a few crystals at the edge. */
    public static void poel(Zet z, RandomSource r, int x, int y0, int zz, double rx, double rz, int diep) {
        int ix = (int) Math.ceil(rx) + 1, iz = (int) Math.ceil(rz) + 1;
        BlockState rand = s(ZingendeWeideFeature.GEPOLIJSTE_ZEEGROEN_STEEN.get());
        for (int a = -ix; a <= ix; a++) {
            for (int b = -iz; b <= iz; b++) {
                double d = (a / rx) * (a / rx) + (b / rz) * (b / rz);
                int px = x + a, pz = zz + b;
                if (d <= 1.0) {
                    int bodem = y0 - 1 - (int) Math.round(diep * (1 - d * 0.6));
                    for (int y = y0 + 3; y > bodem; y--) {
                        z.zet(px, y, pz, y < y0 ? sap() : Blocks.AIR.defaultBlockState());
                    }
                    z.zet(px, bodem, pz, (a + b) % 3 == 0 ? rand : klei());
                } else if (d <= 1.55) {
                    // the rim: level with the water, so it holds
                    for (int y = y0 - diep - 1; y < y0; y++) {
                        BlockState s = z.get(px, y, pz);
                        if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) {
                            z.zet(px, y, pz, klei());
                        }
                    }
                    z.zet(px, y0 - 1, pz, d <= 1.3 && r.nextInt(3) > 0 ? rand : gras());
                    for (int y = y0; y < y0 + 3; y++) {
                        if (!z.get(px, y, pz).isAir() && z.get(px, y, pz).canBeReplaced()) {
                            z.zet(px, y, pz, Blocks.AIR.defaultBlockState());
                        }
                    }
                    if (r.nextInt(14) == 0) {
                        z.zet(px, y0, pz, kristalletjes(r, Direction.UP));
                    }
                }
            }
        }
        // a little island in a bigger pool
        if (rx >= 4.5 && r.nextBoolean()) {
            int px = x + r.nextInt(3) - 1, pz = zz + r.nextInt(3) - 1;
            for (int y = y0 - diep - 1; y < y0; y++) {
                z.zet(px, y, pz, klei());
            }
            z.zet(px, y0 - 1, pz, groen());
            z.zet(px, y0, pz, s(ZingendeWeideFeature.ZINGBLOEMETJE_ROZE.get()));
        }
    }

    /** A Blub hole: a little grass mound with a round doorway, a lantern on a post beside it. */
    public static void holletje(Zet z, RandomSource r, int x, int y0, int zz, Direction deur) {
        int rad = 3;
        for (int a = -rad; a <= rad; a++) {
            for (int b = -rad; b <= rad; b++) {
                double d = Math.sqrt(a * a + b * b);
                if (d > rad + 0.3) {
                    continue;
                }
                int h = (int) Math.round((rad + 0.3 - d) * 0.9);
                for (int y = y0 - 1; y <= y0 + h - 1; y++) {
                    z.zet(x + a, y, zz + b, y == y0 + h - 1 ? groen() : klei());
                }
            }
        }
        // the hollow inside and the doorway
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                z.zet(x + a, y0, zz + b, Blocks.AIR.defaultBlockState());
            }
        }
        for (int i = 2; i <= rad; i++) {
            z.zet(x + deur.getStepX() * i, y0, zz + deur.getStepZ() * i, Blocks.AIR.defaultBlockState());
        }
        z.zet(x, y0 - 1, zz, s(Blocks.PINK_WOOL));
        int lx = x + deur.getStepX() * (rad + 1) + deur.getClockWise().getStepX() * 2, lz = zz + deur.getStepZ() * (rad + 1) + deur.getClockWise().getStepZ() * 2;
        z.zet(lx, y0, lz, s(ZingendeWeideFeature.HEK.get()));
        z.zet(lx, y0 + 1, lz, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, false));
        z.zacht(x + deur.getStepX() * (rad + 1) - deur.getClockWise().getStepX(), y0, zz + deur.getStepZ() * (rad + 1) - deur.getClockWise().getStepZ(),
                s(ZingendeWeideFeature.ZINGBLOEMETJE_GEEL.get()));
    }

    /** A shell rock of the Schildjeszand: a low dome of sand with a mint ring and a peach middle (the carapace Blubs' shells). */
    public static void schildrots(Zet z, RandomSource r, int x, int y0, int zz) {
        int rad = 2 + r.nextInt(3);
        for (int a = -rad; a <= rad; a++) {
            for (int b = -rad; b <= rad; b++) {
                double d = Math.sqrt(a * a + b * b);
                if (d > rad + 0.4) {
                    continue;
                }
                int h = (int) Math.round((rad + 0.4 - d) * 0.75);
                BlockState top = d < rad * 0.35 ? s(Blocks.PINK_TERRACOTTA) : d < rad * 0.75 ? s(ZingendeWeideFeature.GEPOLIJSTE_ZEEGROEN_STEEN.get())
                        : s(ZingendeWeideFeature.SCHILDJESZAND.get());
                for (int y = y0 - 1; y < y0 + h; y++) {
                    z.zet(x + a, y, zz + b, y == y0 + h - 1 ? top : s(ZingendeWeideFeature.ZEEGROEN_STEEN.get()));
                }
            }
        }
    }

    /**
     * A singing gorge: a winding trench from (x, y0, z) along {@code dir}, {@code lengte} long, 3-5 wide and 7-12 deep, with
     * striped walls and a little stream of bubbelsap at the bottom.
     */
    public static void kloof(Zet z, RandomSource r, int x, int y0, int zz, Direction dir, int lengte) {
        Direction dwars = dir.getClockWise();
        double golf = r.nextDouble() * 6;
        for (int i = -lengte / 2; i <= lengte / 2; i++) {
            double t = Math.abs(i) / (lengte / 2.0);
            int diep = (int) Math.round(Mth.lerp(t * t, 7 + r.nextInt(5), 1));
            int breed = 1 + (int) Math.round(Mth.lerp(t, 2.2, 0.6));
            int mid = (int) Math.round(Math.sin(i * 0.22 + golf) * 2.5);
            for (int w = -breed - 1; w <= breed + 1; w++) {
                int px = x + dir.getStepX() * i + dwars.getStepX() * (w + mid), pz = zz + dir.getStepZ() * i + dwars.getStepZ() * (w + mid);
                int bodem = y0 - 1 - diep;
                if (Math.abs(w) <= breed) {
                    for (int y = bodem + 1; y <= y0 + 6; y++) {
                        z.zet(px, y, pz, Blocks.AIR.defaultBlockState());
                    }
                    z.zet(px, bodem, pz, w == 0 && diep > 2 ? sap() : klei());
                    z.zet(px, bodem - 1, pz, klei());
                    if (w != 0 && r.nextInt(7) == 0) {
                        z.zet(px, bodem + 1, pz, kristalletjes(r, Direction.UP));
                    }
                } else {
                    // the walls: striped, from the bottom to the old ground
                    for (int y = bodem; y < y0; y++) {
                        BlockState s = z.get(px, y, pz);
                        if (!s.isAir()) {
                            z.zet(px, y, pz, streep(y));
                        }
                    }
                }
            }
        }
    }

    /** Is this the meadow's own ground (a feature only builds on it)? */
    public static boolean grond(BlockState s) {
        return s.is(ZingendeWeideFeature.ZALMKLEI_GRAS.get()) || s.is(ZingendeWeideFeature.ZEEGROEN_GRAS.get())
                || s.is(ZingendeWeideFeature.SCHILDJESZAND.get()) || s.is(ZingendeWeideFeature.ZALMKLEI.get());
    }

    static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
