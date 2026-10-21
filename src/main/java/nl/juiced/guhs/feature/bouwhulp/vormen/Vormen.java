package nl.juiced.guhs.feature.bouwhulp.vormen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * The shapes of the vormstaf (design 2.5), every one hollow or filled with a wall thickness: bol, koepel, cilinder/toren,
 * boog, gewelfde tunnel, brug, piramide, kegel, ring, the natuurvormen (rots, heuvel, boomkruin, eiland; Zoutkeet),
 * wenteltrap, helix, 3D-hart, guh-silhouet, bloktekst (Zoutkeet), ei, halve cilinder and kasteelmuur.
 * <p>
 * Most shapes are a test "is this spot inside" over the box b × h × d. Filled: every spot inside. Hollow: the spots within
 * {@code dikte} of the outside, the rest is dug out (AIR), so a hollow shape in a hill is really hollow. A spot that faces
 * the outside up or down (a floor, a ceiling, the top of a hill) gets the rand material, the others the wand material.
 */
public final class Vormen {
    public enum Vorm {
        BOL, KOEPEL, CILINDER, BOOG, TUNNEL, BRUG, PIRAMIDE, KEGEL, RING, ROTS(true), HEUVEL(true), BOOMKRUIN(true), EILAND(true),
        WENTELTRAP, HELIX, HART, GUH, TEKST, EI, HALVE_CILINDER, KASTEELMUUR;

        public final boolean natuur;

        Vorm() {
            this(false);
        }

        Vorm(boolean natuur) {
            this.natuur = natuur;
        }

        public static Vorm van(int i) {
            return values()[Math.floorMod(i, values().length)];
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Can this level make it (natuurvormen and bloktekst from the Zoutkeet)? */
        public boolean mag(BouwNiveau n) {
            return this == TEKST ? n.bloktekst() : !natuur || n.natuurvormen();
        }
    }

    private interface Binnen {
        boolean in(int x, int y, int z);
    }

    /** The ontwerp of a shape (sizes clamped to what the level allows). Empty when the level can't make it. */
    public static Ontwerp maak(VormInstelling v0, BouwNiveau niveau) {
        if (!v0.vorm().mag(niveau)) {
            return Ontwerp.van(Map.of());
        }
        return Ontwerp.van(blokken(v0.begrensd(niveau.maxVorm)));
    }

    /** The blocks of a shape (relative to its low corner), without the level's checks. */
    public static Map<BlockPos, BlockState> blokken(VormInstelling v) {
        int b = v.b(), h = v.h(), d = v.d(), t = v.dikte();
        Block wand = v.wand().eerste(standaardWand(v.vorm())), rand = v.rand().eerste(standaardRand(v.vorm(), wand));
        Mix mw = v.wand().isLeeg() ? standaardMix(v.vorm(), wand) : v.wand();
        Mix mr = v.rand().isLeeg() ? Mix.van(rand) : v.rand();
        double hx = b / 2.0, hz = d / 2.0;
        Map<BlockPos, BlockState> uit;
        switch (v.vorm()) {
            case BOL -> uit = vul(b, h, d, (x, y, z) -> sq(nx(x, b)) + sq(nx(y, h)) + sq(nx(z, d)) <= 1.0, v.hol(), t, mw, mr, false, false);
            case KOEPEL -> uit = vul(b, h, d, (x, y, z) -> sq(nx(x, b)) + sq((y + 0.5) / h) + sq(nx(z, d)) <= 1.0, v.hol(), t, mw, mr, false, false);
            case CILINDER -> uit = vul(b, h, d, (x, y, z) -> sq(nx(x, b)) + sq(nx(z, d)) <= 1.0, v.hol(), t, mw, mr, false, false);
            case EI -> uit = vul(b, h, d, (x, y, z) -> {
                double a = (y + 0.5) / h * 2 - 1, r = Math.sqrt(Math.max(0, 1 - a * a)) * (1 - 0.18 * a);
                return sq(nx(x, b)) + sq(nx(z, d)) <= r * r;
            }, v.hol(), t, mw, mr, false, false);
            case HALVE_CILINDER -> uit = vul(b, h, d, (x, y, z) -> sq((y + 0.5) / h) + sq(nx(z, d)) <= 1.0, v.hol(), t, mw, mr, true, false);
            case KEGEL -> uit = vul(b, h, d, (x, y, z) -> {
                double r = 1 - (double) y / h;
                return sq(nx(x, b)) + sq(nx(z, d)) <= r * r;
            }, v.hol(), t, mw, mr, false, false);
            case PIRAMIDE -> uit = vul(b, h, d, (x, y, z) -> {
                double f = 1 - (double) y / h;
                return Math.abs(x + 0.5 - hx) <= hx * f + 0.01 && Math.abs(z + 0.5 - hz) <= hz * f + 0.01;
            }, v.hol(), t, mw, mr, false, false);
            case RING -> {
                double binnen = Math.max(0, 1 - 2.0 * t / Math.min(b, d));
                uit = vul(b, h, d, (x, y, z) -> {
                    double r = sq(nx(x, b)) + sq(nx(z, d));
                    return r <= 1.0 && r >= binnen * binnen;
                }, false, t, mw, mr, false, false);
            }
            case BOOG -> {
                double rx = Math.max(0.5, hx - t), ry = Math.max(0.5, h - t);
                uit = vul(b, h, d, (x, y, z) -> sq((x + 0.5 - hx) / rx) + sq((y + 0.5) / ry) > 1.0, false, t, mw, mr, false, false);
            }
            case TUNNEL -> {
                double rug = Math.min(hx, h);
                uit = vul(b, h, d, (x, y, z) -> {
                    double boven = h - rug;
                    return y + 0.5 < boven || sq((x + 0.5 - hx) / hx) + sq((y + 0.5 - boven) / rug) <= 1.0;
                }, true, t, mw, mr, true, true);
            }
            case BRUG -> uit = brug(b, h, d, mw, mr);
            case ROTS -> uit = vul(b, h, d, (x, y, z) -> {
                double dy = nx(y, h);
                return sq(nx(x, b)) + sq(dy < 0 ? dy * 0.7 : dy) + sq(nx(z, d)) <= 1.0 + (ruis(x, y, z, 3.5) - 0.5) * 0.7;
            }, v.hol(), t, mw, mr, false, false);
            case BOOMKRUIN -> uit = vul(b, h, d, (x, y, z) -> sq(nx(x, b)) + sq(nx(y, h)) + sq(nx(z, d)) <= 0.95 + (ruis(x, y, z, 2.5) - 0.5) * 0.8,
                    false, t, mw, mr, false, false);
            case HEUVEL -> uit = vul(b, h, d, (x, y, z) -> {
                double r = sq(nx(x, b)) + sq(nx(z, d));
                double top = h * Math.max(0, 1 - r) * (0.8 + 0.4 * ruis(x, 0, z, 4));
                return r <= 1.0 && y + 0.5 <= top;
            }, false, t, mw, mr, false, false);
            case EILAND -> uit = vul(b, h, d, (x, y, z) -> {
                int plaat = Math.max(1, Math.min(3, h / 4));
                double r = sq(nx(x, b)) + sq(nx(z, d)), wiebel = (ruis(x, y, z, 3) - 0.5) * 0.35;
                if (y >= h - plaat) {
                    return r <= 1.0 + wiebel;
                }
                double f = Math.pow((y + 1.0) / (h - plaat), 0.8);
                return r <= f * f + wiebel;
            }, false, t, mw, mr, false, false);
            case WENTELTRAP -> uit = wenteltrap(b, h, d, v.hol(), mw, mr);
            case HELIX -> uit = helix(b, h, d, mw, mr);
            case HART -> uit = vul(b, h, d, (x, y, z) -> {
                double X = nx(x, b) * 1.25, Z = ((y + 0.5) / h * 2 - 1) * 1.25 + 0.1, Y = nx(z, d) * 0.9;
                double a = X * X + 2.25 * Y * Y + Z * Z - 1;
                return a * a * a - X * X * Z * Z * Z - 0.1125 * Y * Y * Z * Z * Z <= 0;
            }, v.hol(), t, mw, mr, false, false);
            case GUH -> uit = guh(b, h, d, v.hol(), t, mw, mr, v.rand().isLeeg());
            case TEKST -> {
                // as many letters as fit in the width (6 blocks per letter per 7 of height)
                String tekst = v.tekst().replace("<3", "♥");
                int past = Math.max(1, (b + 1) / (6 * Math.max(1, h / 7)));
                uit = BlokTekst.maak(tekst.substring(0, Math.min(tekst.length(), past)), h, d, mw);
            }
            case KASTEELMUUR -> uit = kasteelmuur(b, h, d, mw, mr);
            default -> uit = new LinkedHashMap<>();
        }
        Daken.trapvormen(uit);
        return uit;
    }

    // =====================================================================================================================
    // the general fill: inside -> filled, or a shell of `dikte` with the rest dug out
    // =====================================================================================================================

    /**
     * @param openX the box's x faces don't count as "outside" (a halve cilinder lying along x is open at its ends)
     * @param openZ the same for z (a tunnel along z)
     */
    private static Map<BlockPos, BlockState> vul(int b, int h, int d, Binnen binnen, boolean hol, int dikte, Mix wand, Mix rand, boolean openX,
                                                 boolean openZ) {
        boolean[] in = new boolean[b * h * d];
        for (int x = 0; x < b; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    in[(x * h + y) * d + z] = binnen.in(x, y, z);
                }
            }
        }
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (int x = 0; x < b; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    if (!in[(x * h + y) * d + z]) {
                        continue;
                    }
                    BlockPos p = new BlockPos(x, y, z);
                    int rand1 = rand(in, b, h, d, x, y, z, hol ? dikte : 1, openX, openZ);
                    if (rand1 == 0) {
                        uit.put(p, hol ? Blocks.AIR.defaultBlockState() : staat(wand.kies(p, Blocks.STONE)));
                    } else {
                        uit.put(p, staat((rand1 == 2 ? rand : wand).kies(p, Blocks.STONE)));
                    }
                }
            }
        }
        return uit;
    }

    /** 0: deep inside; 1: near the outside sideways; 2: near the outside up or down. */
    private static int rand(boolean[] in, int b, int h, int d, int x, int y, int z, int dikte, boolean openX, boolean openZ) {
        for (int k = 1; k <= dikte; k++) {
            if (buiten(in, b, h, d, x, y + k, z, false, false) || buiten(in, b, h, d, x, y - k, z, false, false)) {
                return 2;
            }
            if (buiten(in, b, h, d, x + k, y, z, openX, openZ) || buiten(in, b, h, d, x - k, y, z, openX, openZ)
                    || buiten(in, b, h, d, x, y, z + k, openX, openZ) || buiten(in, b, h, d, x, y, z - k, openX, openZ)) {
                return 1;
            }
        }
        return 0;
    }

    private static boolean buiten(boolean[] in, int b, int h, int d, int x, int y, int z, boolean openX, boolean openZ) {
        if (x < 0 || x >= b) {
            return !openX;
        }
        if (z < 0 || z >= d) {
            return !openZ;
        }
        if (y < 0 || y >= h) {
            return true;
        }
        return !in[(x * h + y) * d + z];
    }

    // =====================================================================================================================
    // the shapes that are not "inside or not"
    // =====================================================================================================================

    /** A bridge along x: an arch underneath, a deck (rand) and a railing along both sides. */
    private static Map<BlockPos, BlockState> brug(int b, int h, int d, Mix wand, Mix rand) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        int dek = Math.max(0, h - 2);
        double hx = b / 2.0, rx = Math.max(0.5, hx - 1.5), ry = Math.max(0.5, dek - 0.5);
        for (int x = 0; x < b; x++) {
            for (int z = 0; z < d; z++) {
                for (int y = 0; y < dek; y++) {
                    if (sq((x + 0.5 - hx) / rx) + sq((y + 0.5) / ry) > 1.0) {
                        BlockPos p = new BlockPos(x, y, z);
                        uit.put(p, staat(wand.kies(p, Blocks.STONE_BRICKS)));
                    }
                }
                BlockPos p = new BlockPos(x, dek, z);
                uit.put(p, staat(rand.kies(p, Blocks.OAK_PLANKS)));
                if ((z == 0 || z == d - 1) && h >= 2 && d >= 3) {
                    BlockPos q = new BlockPos(x, dek + 1, z);
                    Familie f = Familie.van(wand.eerste(Blocks.STONE_BRICKS));
                    uit.put(q, f.muur() != null ? f.muur().defaultBlockState() : staat(wand.kies(q, Blocks.STONE_BRICKS)));
                }
            }
        }
        return uit;
    }

    /** A spiral staircase: a middle post (rand), steps turning a twelfth each block up (wand's stairs), with a tower wall when hollow. */
    private static Map<BlockPos, BlockState> wenteltrap(int b, int h, int d, boolean hol, Mix wand, Mix rand) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        double cx = (b - 1) / 2.0, cz = (d - 1) / 2.0, r = Math.max(1.5, Math.min(b, d) / 2.0 - (hol ? 1 : 0));
        Block trap = Familie.van(wand.eerste(Blocks.OAK_PLANKS)).trap();
        for (int y = 0; y < h; y++) {
            BlockPos as = new BlockPos((int) Math.round(cx), y, (int) Math.round(cz));
            uit.put(as, staat(rand.kies(as, Blocks.OAK_LOG)));
            for (int sector = 0; sector < 2; sector++) {
                double a = (y + sector * 0.5) * Math.PI * 2 / 12;
                Direction op = richting(a + Math.PI / 2);
                for (double s = 1; s <= r; s += 0.5) {
                    BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(a) * s), y, (int) Math.round(cz + Math.sin(a) * s));
                    if (p.equals(as) || p.getX() < 0 || p.getZ() < 0 || p.getX() >= b || p.getZ() >= d) {
                        continue;
                    }
                    uit.put(p, trap != null ? trap.defaultBlockState().setValue(StairBlock.FACING, op) : staat(wand.kies(p, Blocks.OAK_PLANKS)));
                }
            }
            if (hol) {
                for (int x = 0; x < b; x++) {
                    for (int z = 0; z < d; z++) {
                        double rr = sq(nx(x, b)) + sq(nx(z, d));
                        if (rr <= 1.0 && rr > sq(1 - 1.6 / Math.min(b, d) * 2)) {
                            BlockPos p = new BlockPos(x, y, z);
                            uit.put(p, staat(wand.kies(p, Blocks.STONE_BRICKS)));
                        }
                    }
                }
            }
        }
        return uit;
    }

    /** Two strands turning around each other (rand rungs every third block), like a guh-DNA. */
    private static Map<BlockPos, BlockState> helix(int b, int h, int d, Mix wand, Mix rand) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        double cx = (b - 1) / 2.0, cz = (d - 1) / 2.0, rx = Math.max(0.5, (b - 1) / 2.0), rz = Math.max(0.5, (d - 1) / 2.0);
        for (int y = 0; y < h; y++) {
            double a = y * Math.PI * 2 / 16;
            for (int kant = 0; kant < 2; kant++) {
                double k = a + kant * Math.PI;
                BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(k) * rx), y, (int) Math.round(cz + Math.sin(k) * rz));
                uit.put(p, staat(wand.kies(p, Blocks.PINK_CONCRETE)));
            }
            if (y % 3 == 1) {
                for (double s = -0.8; s <= 0.8; s += 0.2) {
                    BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(a) * rx * s), y, (int) Math.round(cz + Math.sin(a) * rz * s));
                    uit.putIfAbsent(p, staat(rand.kies(p, Blocks.WHITE_CONCRETE)));
                }
            }
        }
        return uit;
    }

    /** A guh head: a round body with two ears on top and two eyes (rand, black when no rand was chosen) at the front (south). */
    private static Map<BlockPos, BlockState> guh(int b, int h, int d, boolean hol, int t, Mix wand, Mix rand, boolean zwarteOgen) {
        double hx = b / 2.0, hz = d / 2.0, ry = h * 0.4, cy = h * 0.4;
        double ex = Math.max(0.6, b * 0.13), ey = Math.max(0.8, h * 0.17), ez = Math.max(0.6, d * 0.18);
        Map<BlockPos, BlockState> uit = vul(b, h, d, (x, y, z) -> {
            double kop = sq((x + 0.5 - hx) / hx) + sq((y + 0.5 - cy) / ry) + sq((z + 0.5 - hz) / hz);
            double oor1 = sq((x + 0.5 - b * 0.27) / ex) + sq((y + 0.5 - h * 0.82) / ey) + sq((z + 0.5 - hz) / ez);
            double oor2 = sq((x + 0.5 - b * 0.73) / ex) + sq((y + 0.5 - h * 0.82) / ey) + sq((z + 0.5 - hz) / ez);
            return kop <= 1.0 || oor1 <= 1.0 || oor2 <= 1.0;
        }, hol, t, wand, wand, false, false);
        int oy = (int) Math.round(cy + ry * 0.25);
        for (int kant = 0; kant < 2; kant++) {
            int ox = (int) Math.round(kant == 0 ? b * 0.32 : b * 0.68 - 1);
            for (int z = d - 1; z >= 0; z--) {
                BlockPos p = new BlockPos(ox, oy, z);
                if (uit.containsKey(p) && !uit.get(p).isAir()) {
                    uit.put(p, zwarteOgen ? Blocks.BLACK_CONCRETE.defaultBlockState() : staat(rand.kies(p, Blocks.BLACK_CONCRETE)));
                    break;
                }
            }
        }
        return uit;
    }

    /** A castle wall along x, d thick, with crenellations (two blocks up, two down) on top. */
    private static Map<BlockPos, BlockState> kasteelmuur(int b, int h, int d, Mix wand, Mix rand) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        int body = Math.max(1, h - 1);
        for (int x = 0; x < b; x++) {
            for (int z = 0; z < d; z++) {
                for (int y = 0; y < body; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    uit.put(p, staat((y == body - 1 && d >= 3 && z > 0 && z < d - 1 ? rand : wand).kies(p, Blocks.STONE_BRICKS)));
                }
                if (h >= 2 && (x / 2) % 2 == 0 && (z == 0 || z == d - 1 || d < 3)) {
                    BlockPos p = new BlockPos(x, body, z);
                    uit.put(p, staat(wand.kies(p, Blocks.STONE_BRICKS)));
                }
            }
        }
        return uit;
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A block as a shape puts it: leaves stay (persistent). */
    public static BlockState staat(Block b) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(LeavesBlock.PERSISTENT)) {
            s = s.setValue(LeavesBlock.PERSISTENT, true);
        }
        return s;
    }

    private static Block standaardWand(Vorm v) {
        return switch (v) {
            case ROTS -> Blocks.STONE;
            case HEUVEL -> Blocks.DIRT;
            case BOOMKRUIN -> Blocks.OAK_LEAVES;
            case EILAND -> Blocks.STONE;
            case BRUG, KASTEELMUUR, BOOG, TUNNEL -> Blocks.STONE_BRICKS;
            case WENTELTRAP -> Blocks.OAK_PLANKS;
            case HELIX, HART -> Blocks.PINK_CONCRETE;
            case GUH -> Blocks.PINK_WOOL;
            default -> standaard();
        };
    }

    /** The guh pastel brick when F11 is there, else plain bricks. */
    private static Block standaard() {
        Block b = Familie.blok("guhs:pastelbaksteen_roze");
        return b != null ? b : Blocks.BRICKS;
    }

    private static Block standaardRand(Vorm v, Block wand) {
        return switch (v) {
            case HEUVEL, EILAND -> Blocks.GRASS_BLOCK;
            case BRUG -> Blocks.SPRUCE_PLANKS;
            case WENTELTRAP -> Blocks.STRIPPED_OAK_LOG;
            default -> wand;
        };
    }

    /** Natural shapes look better as a mix when nothing was chosen. */
    private static Mix standaardMix(Vorm v, Block wand) {
        return switch (v) {
            case ROTS -> Mix.van(Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
            case EILAND -> Mix.van(Blocks.STONE, Blocks.DIRT, Blocks.ANDESITE);
            case BOOMKRUIN -> Mix.van(Blocks.OAK_LEAVES, Blocks.AZALEA_LEAVES);
            default -> Mix.van(wand);
        };
    }

    private static Direction richting(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return Math.abs(c) > Math.abs(s) ? (c > 0 ? Direction.EAST : Direction.WEST) : (s > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** -1..1 across the box (the middle of the spot). */
    static double nx(int x, int n) {
        return (x + 0.5 - n / 2.0) / (n / 2.0);
    }

    static double sq(double a) {
        return a * a;
    }

    /** Smooth value noise 0..1 (the natural shapes' bumps), the same everywhere for the same spot. */
    public static double ruis(double x, double y, double z, double schaal) {
        double fx = x / schaal, fy = y / schaal, fz = z / schaal;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy), z0 = (int) Math.floor(fz);
        double tx = glad(fx - x0), ty = glad(fy - y0), tz = glad(fz - z0);
        double v = 0;
        for (int i = 0; i < 8; i++) {
            int dx = i & 1, dy = i >> 1 & 1, dz = i >> 2 & 1;
            double w = (dx == 1 ? tx : 1 - tx) * (dy == 1 ? ty : 1 - ty) * (dz == 1 ? tz : 1 - tz);
            v += w * (Math.floorMod(Mix.hash(x0 + dx, y0 + dy, z0 + dz), 1000) / 999.0);
        }
        return v;
    }

    private static double glad(double t) {
        return t * t * (3 - 2 * t);
    }

    private Vormen() {
    }
}
