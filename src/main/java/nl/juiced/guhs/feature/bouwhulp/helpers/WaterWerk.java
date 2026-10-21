package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * The water templates (design 2.8): the beaver's dam with its pool, lodge, bridge, jetty and boardwalk; the otter's stepping
 * stones and mud slide; the flamingo's shore; the penguin's ice floor. They all start from a {@link Oversteek} (the
 * narrowest crossing of water near a spot) or from the shore.
 */
final class WaterWerk {
    static final BlockState WATER = Blocks.WATER.defaultBlockState();

    /**
     * A crossing of water: the line across it runs along {@code as} from {@code van} to {@code tot} (the water's extent on
     * that axis) at stream coordinate {@code s}; {@code oppervlak} the water's surface, {@code bodem} the lowest bed under the
     * line.
     */
    record Oversteek(Direction.Axis as, int van, int tot, int s, int oppervlak, int bodem) {
        int breedte() {
            return tot - van + 1;
        }

        int midden() {
            return (van + tot) / 2;
        }

        /** World x,z of line coordinate a and stream coordinate s. */
        BlockPos kolom(int a, int sc, int y) {
            return as == Direction.Axis.X ? new BlockPos(a, y, sc) : new BlockPos(sc, y, a);
        }
    }

    // =====================================================================================================================
    // finding water
    // =====================================================================================================================

    private static int uitstrek(Klus k, int x, int z, int dx, int dz, int opp) {
        int n = 0;
        while (n < 40 && k.water(x + dx * (n + 1), z + dz * (n + 1)) == opp) {
            n++;
        }
        return n;
    }

    /** The crossing through water column x,z (null: no water there, or wider than {@code max}, or no stream-like extent). */
    @Nullable
    static Oversteek oversteekBij(Klus k, int x, int z, int max) {
        int opp = k.water(x, z);
        if (opp == Integer.MIN_VALUE) {
            return null;
        }
        int xm = uitstrek(k, x, z, -1, 0, opp), xp = uitstrek(k, x, z, 1, 0, opp);
        int zm = uitstrek(k, x, z, 0, -1, opp), zp = uitstrek(k, x, z, 0, 1, opp);
        int bx = xm + xp + 1, bz = zm + zp + 1;
        boolean overX = bx <= bz;
        int breed = Math.min(bx, bz);
        if (breed > max) {
            return null;
        }
        Oversteek o = overX ? new Oversteek(Direction.Axis.X, x - xm, x + xp, z, opp, 0) : new Oversteek(Direction.Axis.Z, z - zm, z + zp, x, opp, 0);
        int bodem = Integer.MAX_VALUE;
        for (int a = o.van(); a <= o.tot(); a++) {
            BlockPos c = o.kolom(a, o.s(), 0);
            bodem = Math.min(bodem, k.grond(c.getX(), c.getZ()));
        }
        return new Oversteek(o.as(), o.van(), o.tot(), o.s(), opp, bodem);
    }

    /** The narrowest crossing near the board (a stream: the other way it runs on at least a few blocks). */
    static Oversteek smalste(Klus k, int max) {
        Oversteek beste = null;
        int score = Integer.MAX_VALUE;
        for (BlockPos c : k.rondBord(k.zoekStraal())) {
            if (!k.level().isLoaded(c) || !k.isWater(c.getX(), c.getZ())) {
                continue;
            }
            Oversteek o = oversteekBij(k, c.getX(), c.getZ(), max);
            if (o == null) {
                continue;
            }
            int opp = o.oppervlak();
            int lengte = o.as() == Direction.Axis.X ? uitstrek(k, c.getX(), c.getZ(), 0, -1, opp) + uitstrek(k, c.getX(), c.getZ(), 0, 1, opp) + 1
                    : uitstrek(k, c.getX(), c.getZ(), -1, 0, opp) + uitstrek(k, c.getX(), c.getZ(), 1, 0, opp) + 1;
            if (lengte < o.breedte() + 2) {
                continue;                       // a pond's edge, not a stream
            }
            int d = (int) Math.sqrt(c.distSqr(k.bord().atY(0)));
            int sc = o.breedte() * 200 + d;
            if (sc < score) {
                score = sc;
                beste = new Oversteek(o.as(), o.van(), o.tot(), o.s(), o.oppervlak(), o.bodem());
            }
        }
        if (beste == null) {
            throw Klus.nee("geen_water");
        }
        return beste;
    }

    /** The crossing at (or within 4 of) a clicked spot. */
    static Oversteek oversteekRond(Klus k, BlockPos p, int max) {
        Oversteek beste = null;
        double d = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                Oversteek o = oversteekBij(k, p.getX() + dx, p.getZ() + dz, max);
                if (o != null && dx * dx + dz * dz < d) {
                    d = dx * dx + dz * dz;
                    beste = o;
                }
            }
        }
        if (beste == null) {
            throw Klus.nee(waterIn(k, p, 4) ? "te_breed" : "geen_water");
        }
        return beste;
    }

    static boolean waterIn(Klus k, BlockPos p, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (k.isWater(p.getX() + dx, p.getZ() + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The water column nearest to p within r (null: none). */
    @Nullable
    static BlockPos naasteWater(Klus k, BlockPos p, int r) {
        BlockPos beste = null;
        int d = Integer.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int y = k.water(p.getX() + dx, p.getZ() + dz);
                if (y != Integer.MIN_VALUE && dx * dx + dz * dz < d) {
                    d = dx * dx + dz * dz;
                    beste = new BlockPos(p.getX() + dx, y, p.getZ() + dz);
                }
            }
        }
        return beste;
    }

    /** Any water near the board (the start of the shore jobs' "Zoek plek"). */
    static BlockPos waterBijBord(Klus k) {
        for (BlockPos c : k.rondBord(k.zoekStraal())) {
            if (k.level().isLoaded(c) && k.isWater(c.getX(), c.getZ())) {
                return new BlockPos(c.getX(), k.water(c.getX(), c.getZ()), c.getZ());
            }
        }
        throw Klus.nee("geen_water");
    }

    /** A land column next to water near p, and the direction from it into the water (null: no shore). */
    record Oever(BlockPos land, Direction naarWater) {
    }

    @Nullable
    static Oever oever(Klus k, BlockPos p, int r) {
        Oever beste = null;
        int d = Integer.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = p.getX() + dx, z = p.getZ() + dz;
                if (k.isWater(x, z)) {
                    continue;
                }
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (k.isWater(x + dir.getStepX(), z + dir.getStepZ()) && dx * dx + dz * dz < d) {
                        d = dx * dx + dz * dz;
                        beste = new Oever(k.grondPos(x, z), dir);
                    }
                }
            }
        }
        return beste;
    }

    /** +1 when upstream is the + side of the stream axis, -1 when the - side (by the flow, the water level, then the bed). */
    static int bovenstrooms(Klus k, Oversteek o) {
        double som = 0;
        for (int a = o.van(); a <= o.tot(); a++) {
            for (int ds = -3; ds <= 3; ds++) {
                BlockPos c = o.kolom(a, o.s() + ds, o.oppervlak());
                FluidState f = k.level().getFluidState(c);
                if (!f.isEmpty()) {
                    Vec3 v = f.getFlow(k.level(), c);
                    som += o.as() == Direction.Axis.X ? v.z : v.x;
                }
            }
        }
        if (som > 0.05) {
            return -1;
        }
        if (som < -0.05) {
            return 1;
        }
        BlockPos plus = o.kolom(o.midden(), o.s() + 6, 0), min = o.kolom(o.midden(), o.s() - 6, 0);
        int wp = k.water(plus.getX(), plus.getZ()), wm = k.water(min.getX(), min.getZ());
        if (wp != wm && wp != Integer.MIN_VALUE && wm != Integer.MIN_VALUE) {
            return wp > wm ? 1 : -1;
        }
        return k.grond(plus.getX(), plus.getZ()) >= k.grond(min.getX(), min.getZ()) ? 1 : -1;
    }

    private static BlockState damBlok(Klus k, Direction.Axis as) {
        int r = k.r().nextInt(100);
        if (r < 40) {
            return Blocks.MUD.defaultBlockState();
        }
        if (r < 72) {
            return Blok.as(Blocks.OAK_LOG.defaultBlockState(), as);
        }
        if (r < 88) {
            return Blok.as(Blocks.STRIPPED_OAK_LOG.defaultBlockState(), as);
        }
        return Blocks.PACKED_MUD.defaultBlockState();
    }

    /** May water go into this spot (air, water, a plant)? */
    private static boolean nat(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced() && !s.is(Blocks.SNOW);
    }

    // =====================================================================================================================
    // the beaver
    // =====================================================================================================================

    /**
     * The dam (design 2.8, "Voorbeeld: de beverdam"): across the narrowest spot of the stream, from bank to bank until the
     * banks are high enough, with the water behind it (upstream, within a radius) rising 1-3 blocks into a beaver pool; low
     * land at the pool's edge gets a little levee so it stays in.
     */
    static Map<BlockPos, BlockState> dam(Klus k, BlockPos p) {
        Oversteek o = oversteekRond(k, p, 9);
        return dam(k, o);
    }

    static Map<BlockPos, BlockState> dam(Klus k, Oversteek o) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int rijs = 1 + k.grootte();
        int peil = o.oppervlak() + rijs;
        int boven = bovenstrooms(k, o);
        int dik = k.grootte() >= 2 ? 3 : 2;
        // the line: across the water, then on over the banks until they are as high as the new water
        int lo = o.van() - 1, hi = o.tot() + 1;
        for (int i = 0; i < 6; i++) {
            BlockPos c = o.kolom(lo, o.s(), 0);
            if (k.grond(c.getX(), c.getZ()) >= peil) {
                break;
            }
            lo--;
        }
        for (int i = 0; i < 6; i++) {
            BlockPos c = o.kolom(hi, o.s(), 0);
            if (k.grond(c.getX(), c.getZ()) >= peil) {
                break;
            }
            hi++;
        }
        Set<Long> damKolommen = new HashSet<>();
        for (int a = lo; a <= hi; a++) {
            for (int t = 0; t < dik; t++) {
                int sc = o.s() - boven * t;
                BlockPos kol = o.kolom(a, sc, 0);
                damKolommen.add(BlockPos.asLong(kol.getX(), 0, kol.getZ()));
                int bed = k.grond(kol.getX(), kol.getZ());
                int top = t == dik - 1 ? peil - 1 : peil;          // (the downstream face a step lower: it looks piled up)
                for (int y = bed + 1; y <= top; y++) {
                    m.put(new BlockPos(kol.getX(), y, kol.getZ()), damBlok(k, o.as()));
                }
                if (t == 0 && a > lo && a < hi) {
                    m.put(new BlockPos(kol.getX(), peil + 1, kol.getZ()), Blok.as((a & 1) == 0 ? Blocks.OAK_LOG.defaultBlockState()
                            : Blocks.STRIPPED_OAK_LOG.defaultBlockState(), o.as()));
                }
            }
        }
        // the pool upstream
        int straal = 5 + 3 * k.grootte();
        BlockPos midden = o.kolom(o.midden(), o.s(), 0);
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        for (int a = o.van(); a <= o.tot(); a++) {
            BlockPos c = o.kolom(a, o.s() + boven, 0);
            rij.add(c);
            gezien.add(BlockPos.asLong(c.getX(), 0, c.getZ()));
        }
        int n = 0;
        while (!rij.isEmpty() && n < 900) {
            BlockPos c = rij.poll();
            int bed = k.grond(c.getX(), c.getZ());
            if (bed >= peil) {
                continue;
            }
            n++;
            for (int y = bed + 1; y <= peil; y++) {
                BlockPos q = new BlockPos(c.getX(), y, c.getZ());
                if (!m.containsKey(q) && nat(k.staat(q))) {
                    m.put(q, WATER);
                }
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos nb = c.relative(d);
                long key = BlockPos.asLong(nb.getX(), 0, nb.getZ());
                if (gezien.contains(key) || damKolommen.contains(key)) {
                    continue;
                }
                gezien.add(key);
                int sc = o.as() == Direction.Axis.X ? nb.getZ() : nb.getX();
                boolean kant = (sc - o.s()) * boven >= 1;
                double afstand = Math.sqrt(nb.distSqr(midden.atY(0)));
                int nbed = k.grond(nb.getX(), nb.getZ());
                if (kant && afstand <= straal) {
                    rij.add(nb);
                } else if (nbed < peil && !k.isWater(nb.getX(), nb.getZ())) {
                    // low dry land at the edge: a little levee keeps the pool in
                    for (int y = nbed + 1; y <= peil; y++) {
                        BlockPos q = new BlockPos(nb.getX(), y, nb.getZ());
                        if (!m.containsKey(q)) {
                            m.put(q, y == peil ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                        }
                    }
                }
            }
        }
        return m;
    }

    /** The lodge: a mound of sticks and mud in the water, a dry room above the water, a dive hole in its floor. */
    static Map<BlockPos, BlockState> burcht(Klus k, BlockPos p) {
        BlockPos w = naasteWater(k, p, 5);
        if (w == null) {
            throw Klus.nee("geen_water");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 2 + k.grootte();
        int opp = w.getY();
        int bed = k.grond(w.getX(), w.getZ());
        double rh = r + 0.5, rv = r + 1.0;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                for (int y = bed + 1; y <= opp + r + 1; y++) {
                    double hy = y > opp ? (y - opp) / rv : 0;
                    double d = Math.sqrt((dx * dx + dz * dz) / (rh * rh) + hy * hy);
                    BlockPos q = new BlockPos(w.getX() + dx, y, w.getZ() + dz);
                    if (d > 1.0) {
                        continue;
                    }
                    boolean schil = d > 1.0 - 1.2 / Math.max(r, 1) || y <= opp - 1 && Math.abs(dx) + Math.abs(dz) > r - 1;
                    if (schil) {
                        int t = k.r().nextInt(10);
                        m.put(q, t < 4 ? Blok.as(Blocks.STRIPPED_OAK_LOG.defaultBlockState(), (dx + y) % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z)
                                : t < 7 ? Blocks.MUD.defaultBlockState() : t < 9 ? Blok.as(Blocks.OAK_LOG.defaultBlockState(), Direction.Axis.Y)
                                : Blocks.PACKED_MUD.defaultBlockState());
                    } else if (y > opp) {
                        m.put(q, Blocks.AIR.defaultBlockState());
                    } else if (y == opp) {
                        m.put(q, dx == 0 && dz == 0 ? WATER : Blocks.PACKED_MUD.defaultBlockState());
                    } else {
                        m.put(q, WATER);
                    }
                }
            }
        }
        // the front door: under water, on the side toward the board
        Direction deur = Direction.getApproximateNearest(k.bord().getX() - w.getX(), 0, k.bord().getZ() - w.getZ());
        for (int i = 1; i <= r + 1; i++) {
            BlockPos q = w.atY(opp - 1).relative(deur, i);
            if (m.containsKey(q)) {
                m.put(q, WATER);
            }
        }
        return m;
    }

    static BlockPos burchtZoek(Klus k) {
        // the widest water near the board (the pool behind a dam if there is one)
        BlockPos beste = null;
        int top = -1;
        for (BlockPos c : k.rondBord(Math.min(k.zoekStraal(), 40))) {
            if (!k.level().isLoaded(c) || !k.isWater(c.getX(), c.getZ())) {
                continue;
            }
            int n = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (k.isWater(c.getX() + dx, c.getZ() + dz)) {
                        n++;
                    }
                }
            }
            if (n > top) {
                top = n;
                beste = c;
            }
            if (n == 49) {
                break;
            }
        }
        if (beste == null) {
            throw Klus.nee("geen_water");
        }
        return new BlockPos(beste.getX(), k.water(beste.getX(), beste.getZ()), beste.getZ());
    }

    /** A wooden bridge across the narrowest spot (railings, posts in the water, onto the banks). */
    static Map<BlockPos, BlockState> brug(Klus k, BlockPos p) {
        Oversteek o = oversteekRond(k, p, 12);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockPos e1 = o.kolom(o.van() - 1, o.s(), 0), e2 = o.kolom(o.tot() + 1, o.s(), 0);
        int dek = Math.max(Math.max(k.grond(e1.getX(), e1.getZ()), k.grond(e2.getX(), e2.getZ())) + 1, o.oppervlak() + 2);
        int half = k.grootte() >= 1 ? 2 : 1;
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState(), hek = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int a = o.van() - 2; a <= o.tot() + 2; a++) {
            for (int t = -half - 1; t <= half + 1; t++) {
                BlockPos kol = o.kolom(a, o.s() + t, 0);
                boolean rand = Math.abs(t) == half + 1;
                m.put(kol.atY(dek), rand ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : plank);
                if (rand && a >= o.van() - 1 && a <= o.tot() + 1) {
                    m.put(kol.atY(dek + 1), hek);
                }
                if (!rand) {
                    for (int y = dek + 1; y <= dek + 2; y++) {
                        BlockPos q = kol.atY(y);
                        if (Klus.natuur(k.staat(q)) && !k.staat(q).isAir()) {
                            m.put(q, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                // posts in the water every third block, abutments on the banks
                int bed = k.grond(kol.getX(), kol.getZ());
                boolean water = a >= o.van() && a <= o.tot();
                if (rand && water && (a - o.van()) % 3 == 0 || !water) {
                    for (int y = bed + 1; y < dek; y++) {
                        m.put(kol.atY(y), water ? Blocks.SPRUCE_LOG.defaultBlockState() : plank);
                    }
                }
            }
        }
        Blok.verbind(m);
        return m;
    }

    /** A jetty from the shore into the water: deck, posts, railings and a lantern at its end. */
    static Map<BlockPos, BlockState> steiger(Klus k, BlockPos p) {
        Oever ov = oever(k, p, 6);
        if (ov == null) {
            throw Klus.nee("geen_water");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Direction d = ov.naarWater(), rechts = d.getClockWise();
        BlockPos start = ov.land().relative(d);
        int opp = k.water(start.getX(), start.getZ());
        if (opp == Integer.MIN_VALUE) {
            throw Klus.nee("geen_water");
        }
        int dek = opp + 1, lengte = 4 + 2 * k.grootte(), breed = k.grootte() >= 2 ? 3 : 2;
        int echt = 0;
        for (int i = -1; i < lengte; i++) {
            BlockPos rij = start.relative(d, i);
            if (i >= 0 && !k.isWater(rij.getX(), rij.getZ())) {
                break;
            }
            echt = i;
            for (int j = 0; j < breed; j++) {
                BlockPos kol = rij.relative(rechts, j);
                m.put(kol.atY(dek), Blocks.SPRUCE_PLANKS.defaultBlockState());
                if (i >= 0 && (j == 0 || j == breed - 1) && (i % 3 == 2 || i == lengte - 1)) {
                    for (int y = k.grond(kol.getX(), kol.getZ()) + 1; y < dek; y++) {
                        m.put(kol.atY(y), Blocks.SPRUCE_LOG.defaultBlockState());
                    }
                }
                if (i >= 0 && (j == 0 || j == breed - 1)) {
                    m.put(kol.atY(dek + 1), Blocks.SPRUCE_FENCE.defaultBlockState());
                }
            }
        }
        if (echt < 1) {
            throw Klus.nee("geen_water");
        }
        BlockPos eind = start.relative(d, echt);
        m.put(eind.atY(dek + 1), Blocks.SPRUCE_FENCE.defaultBlockState());
        m.put(eind.atY(dek + 2), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "false"));
        BlockPos eind2 = eind.relative(rechts, breed - 1);
        m.put(eind2.atY(dek + 2), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "false"));
        Blok.verbind(m);
        return m;
    }

    /** A boardwalk along the shore, over the water's edge. */
    static Map<BlockPos, BlockState> vlonder(Klus k, BlockPos p) {
        BlockPos w = naasteWater(k, p, 6);
        if (w == null) {
            throw Klus.nee("geen_water");
        }
        int max = 8 + 4 * k.grootte();
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        rij.add(w.atY(0));
        gezien.add(BlockPos.asLong(w.getX(), 0, w.getZ()));
        int n = 0;
        while (!rij.isEmpty() && n < max) {
            BlockPos c = rij.poll();
            int opp = k.water(c.getX(), c.getZ());
            if (opp == Integer.MIN_VALUE) {
                continue;
            }
            boolean langsOever = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (!k.isWater(c.getX() + d.getStepX(), c.getZ() + d.getStepZ())) {
                    langsOever = true;
                }
            }
            if (!langsOever && n > 0) {
                continue;
            }
            n++;
            m.put(c.atY(opp + 1), Blocks.SPRUCE_PLANKS.defaultBlockState());
            if (n % 3 == 1) {
                for (int y = k.grond(c.getX(), c.getZ()) + 1; y <= opp; y++) {
                    m.put(c.atY(y), Blocks.SPRUCE_LOG.defaultBlockState());
                }
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos nb = c.relative(d);
                if (gezien.add(BlockPos.asLong(nb.getX(), 0, nb.getZ()))) {
                    rij.add(nb);
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_water");
        }
        return m;
    }

    // =====================================================================================================================
    // the otter, the flamingo, the penguin
    // =====================================================================================================================

    /** Stepping stones across the narrowest spot: every other block a mossy stone at the water's surface. */
    static Map<BlockPos, BlockState> stapstenen(Klus k, BlockPos p) {
        Oversteek o = oversteekRond(k, p, 12);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState[] stenen = {Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                Blocks.ANDESITE.defaultBlockState()};
        for (int a = o.van(); a <= o.tot(); a++) {
            if ((a - o.van()) % 2 != 0 && a != o.tot()) {
                continue;
            }
            BlockPos kol = o.kolom(a, o.s(), 0);
            for (int y = k.grond(kol.getX(), kol.getZ()) + 1; y <= o.oppervlak(); y++) {
                m.put(kol.atY(y), y == o.oppervlak() ? stenen[k.r().nextInt(stenen.length)] : Blocks.COBBLESTONE.defaultBlockState());
            }
        }
        return m;
    }

    /** A mud slide from the bank down into the water, two wide. */
    static Map<BlockPos, BlockState> modderglijbaan(Klus k, BlockPos p) {
        Oever ov = oever(k, p, 6);
        if (ov == null) {
            throw Klus.nee("geen_water");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Direction d = ov.naarWater(), rechts = d.getClockWise();
        BlockPos rand = ov.land().relative(d);
        int opp = k.water(rand.getX(), rand.getZ());
        int lengte = 5 + 2 * k.grootte();
        BlockPos begin = ov.land().relative(d, -(lengte - 2));
        int top = Math.max(k.grond(begin.getX(), begin.getZ()) + 1, opp + 2);
        for (int i = 0; i < lengte + 2; i++) {
            BlockPos kol = begin.relative(d, i);
            int y = Math.max(opp - 1, top - i / 2);
            for (int j = 0; j < 2; j++) {
                BlockPos q = kol.relative(rechts, j);
                int bed = k.grond(q.getX(), q.getZ());
                for (int yy = bed + 1; yy <= y; yy++) {
                    m.put(q.atY(yy), yy == y ? Blocks.MUD.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState());
                }
                for (int yy = y + 1; yy <= y + 2; yy++) {
                    BlockPos lucht = q.atY(yy);
                    BlockState nu = k.staat(lucht);
                    if (!nu.isAir() && nu.getFluidState().isEmpty() && Klus.natuur(nu)) {
                        m.put(lucht, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            if (y <= opp - 1) {
                break;
            }
        }
        return m;
    }

    /** The flamingo's shore: pink flowers, petals and reed along the water, water lilies on it. */
    static Map<BlockPos, BlockState> oever(Klus k, BlockPos p) {
        if (!waterIn(k, p, 6)) {
            throw Klus.nee("geen_water");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 5 + 2 * k.grootte();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                if (k.isWater(x, z)) {
                    int opp = k.water(x, z);
                    BlockPos boven = new BlockPos(x, opp + 1, z);
                    boolean randje = false;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        randje |= !k.isWater(x + d.getStepX(), z + d.getStepZ());
                    }
                    if (randje && k.staat(boven).isAir() && k.r().nextInt(3) == 0) {
                        m.put(boven, Blocks.LILY_PAD.defaultBlockState());
                    }
                    continue;
                }
                if (!waterIn(k, new BlockPos(x, 0, z), 2) || k.r().nextInt(10) >= 5) {
                    continue;
                }
                int g = k.grond(x, z);
                BlockPos op = new BlockPos(x, g + 1, z);
                if (!k.staat(op).isAir() || !Klus.aarde(k.staat(op.below())) && !k.staat(op.below()).is(net.minecraft.tags.BlockTags.SAND)) {
                    continue;
                }
                boolean naast = false;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    naast |= k.water(x + d.getStepX(), z + d.getStepZ()) == g;
                }
                int t = k.r().nextInt(100);
                if (naast && t < 30) {
                    m.put(op, Blocks.SUGAR_CANE.defaultBlockState());
                    if (k.r().nextBoolean()) {
                        m.put(op.above(), Blocks.SUGAR_CANE.defaultBlockState());
                    }
                } else if (t < 55) {
                    m.put(op, Blocks.PINK_TULIP.defaultBlockState());
                } else if (t < 75 && k.staat(op.above()).isAir()) {
                    m.put(op, Blocks.PEONY.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
                    m.put(op.above(), Blocks.PEONY.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
                } else {
                    m.put(op, Blok.met(Blok.met(Blocks.PINK_PETALS.defaultBlockState(), "flower_amount", String.valueOf(1 + k.r().nextInt(4))),
                            "facing", Direction.Plane.HORIZONTAL.getRandomDirection(k.r()).getSerializedName()));
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("niets_te_doen");
        }
        return m;
    }

    /** The penguin's ice floor: the water's surface around the spot freezes (variant: ice or packed ice). */
    static Map<BlockPos, BlockState> ijsvloer(Klus k, BlockPos p) {
        if (!waterIn(k, p, 5)) {
            throw Klus.nee("geen_water");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 4 + 2 * k.grootte();
        BlockState ijs = k.variant() == 1 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.ICE.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                int opp = k.water(x, z);
                if (opp != Integer.MIN_VALUE) {
                    m.put(new BlockPos(x, opp, z), ijs);
                }
            }
        }
        return m;
    }

    static List<BlockPos> ergens(BlockPos p) {
        return List.of(p);
    }

    private WaterWerk() {
    }
}
