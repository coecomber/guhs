package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.eilanden.WolkenstroomBlock;

/**
 * biomes3 wereld, the Wolkenweide: the blocks that are not solid ground (called by {@link BioVulling} for every chunk).
 * <p>
 * {@link #plan} is a pure function of the model: the cloud banks (rounded with slabs and stairs), the thin cloud sea,
 * the cloud stepping stones and cushions, the wolkenlift / wolkenstroom columns (3 x 3, a {@code guhs:wolkenlift} pad
 * and {@code guhs:wolkenstroom} above it, exactly as in the zwevende_eilanden structure; up-columns end two blocks above
 * the floor they puff you onto), and all water: island ponds, the thin falls (placed complete: a flowing block beside
 * the spout and falling water down to the catch pool, which is what the game itself would make of it, so nothing has to
 * start flowing and nothing can flood), the pond and stream on the meadow. Clouds keep out of the air the routes need
 * ({@link WolkTerrein.Stapel#vrij}). The game test walks the jump rule over exactly these blocks.
 * <p>
 * {@link #vul} places the plan and then dresses the land: softly glowing crystal tips in the drip points, pale vines
 * under the islands, flowers and small trees on top (a few big ones on the larger islands), and a calm meadow with
 * flowers in loose patches.
 */
public final class WolkVulling {
    /** Receives a planned block; alleenLucht: only where nothing is yet. */
    public interface Zet {
        void zet(int x, int y, int z, BlockState s, boolean alleenLucht);
    }

    static final AtomicLong TIJD = new AtomicLong(), KEER = new AtomicLong(), BLOKKEN = new AtomicLong();
    private static final Direction[] ZIJDEN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    static BlockState wolk(boolean roze) {
        return (roze ? Bio.blok("wolkenblok_roze", Blocks.PINK_WOOL) : Bio.blok("wolkenblok_wit", Blocks.WHITE_WOOL)).defaultBlockState();
    }

    private static BlockState plaat(boolean roze, boolean boven) {
        BlockState s = Bio.blok(roze ? "wolkenblok_roze_plaat" : "wolkenblok_wit_plaat", Blocks.SMOOTH_QUARTZ_SLAB).defaultBlockState();
        return s.hasProperty(SlabBlock.TYPE) ? s.setValue(SlabBlock.TYPE, boven ? SlabType.TOP : SlabType.BOTTOM) : s;
    }

    private static BlockState trap(boolean roze, Direction naar) {
        BlockState s = Bio.blok(roze ? "wolkenblok_roze_trap" : "wolkenblok_wit_trap", Blocks.SMOOTH_QUARTZ_STAIRS).defaultBlockState();
        return s.hasProperty(StairBlock.FACING) ? s.setValue(StairBlock.FACING, naar) : s;
    }

    /** The blocks of one call, looked up once. */
    private static final class Pal {
        final BlockState wit = wolk(false), roze = wolk(true), witOnder = plaat(false, false), witBoven = plaat(false, true),
                rozeOnder = plaat(true, false), rozeBoven = plaat(true, true);
        final BlockState[] witTrap = new BlockState[4], rozeTrap = new BlockState[4];
        final BlockState water = Blocks.WATER.defaultBlockState(), stroomt = water.setValue(LiquidBlock.LEVEL, 1), valt = water.setValue(LiquidBlock.LEVEL, 8);
        final BlockState bed = Bio.blok("parelmoer", Blocks.SAND).defaultBlockState();

        Pal() {
            for (int i = 0; i < 4; i++) {
                witTrap[i] = trap(false, ZIJDEN[i]);
                rozeTrap[i] = trap(true, ZIJDEN[i]);
            }
        }
    }

    /** Places the Wolkenweide of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        boolean weide = false;
        for (int o = 0; o < 256 && !weide; o++) {
            weide = k.soort[o] == Kaart.WEIDE;
        }
        if (!weide) {
            return 0;
        }
        long t0 = System.nanoTime();
        int x0 = k.cx << 4, z0 = k.cz << 4;
        int[] n = {0};
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        List<WolkTerrein.Stapel> stapels = WolkTerrein.stapels(m, x0, z0, x0 + 16, z0 + 16);
        plan(m, k, stapels, (x, y, z, s, lucht) -> {
            p.set(x, y, z);
            if (!lucht || level.isEmptyBlock(p)) {
                level.setBlock(p, s, 18);
                n[0]++;
            }
        });
        n[0] += versier(level, m, k, stapels);
        TIJD.addAndGet(System.nanoTime() - t0);
        KEER.incrementAndGet();
        BLOKKEN.addAndGet(n[0]);
        return n[0];
    }

    // ==================================================================================================================
    // the plan
    // ==================================================================================================================
    /** Everything of this chunk that follows from the model alone. */
    public static void plan(BioModel m, Kaart k, List<WolkTerrein.Stapel> stapels, Zet zet) {
        int x0 = k.cx << 4, z0 = k.cz << 4;
        Pal pal = new Pal();
        // the air that must stay free in this chunk
        List<WolkTerrein.Doos> dozen = new ArrayList<>();
        boolean[] doosKolom = new boolean[256], dicht = new boolean[256];
        for (WolkTerrein.Stapel s : stapels) {
            for (WolkTerrein.Doos d : s.vrij) {
                if (d.x1() < x0 || d.x0() > x0 + 15 || d.z1() < z0 || d.z0() > z0 + 15) {
                    continue;
                }
                dozen.add(d);
                for (int x = Math.max(x0, d.x0()); x <= Math.min(x0 + 15, d.x1()); x++) {
                    for (int z = Math.max(z0, d.z0()); z <= Math.min(z0 + 15, d.z1()); z++) {
                        doosKolom[Kaart.index(x, z)] = true;
                    }
                }
            }
            for (WolkTerrein.Val v : s.vallen) {
                if ((v.x() >> 4) == k.cx && (v.z() >> 4) == k.cz) {
                    dicht[Kaart.index(v.x(), v.z())] = true;
                }
            }
        }
        for (int o = 0; o < 256; o++) {
            dicht[o] |= k.soort[o] != Kaart.WEIDE;
        }
        Zet wolk = (x, y, z, s, lucht) -> {
            int o = Kaart.index(x, z);
            if (dicht[o] || y <= k.hoogte[o] || k.water[o] != Kaart.GEEN && y <= k.water[o] + 1 || k.inSpan(o, y)) {
                return;
            }
            if (doosKolom[o]) {
                for (WolkTerrein.Doos d : dozen) {
                    if (d.bevat(x, y, z)) {
                        return;
                    }
                }
            }
            zet.zet(x, y, z, s, true);
        };
        banken(m, k, pal, wolk);
        zee(m, k, pal, wolk);
        for (WolkTerrein.Stapel s : stapels) {
            for (WolkTerrein.Stap st : s.stappen) {
                if (st.wolk()) {
                    stap(k, st, pal, wolk);
                }
            }
            for (WolkTerrein.Kussen ku : s.kussens) {
                kussen(k, ku, pal, wolk);
            }
        }
        for (WolkTerrein.Stapel s : stapels) {
            for (WolkTerrein.Kolom ko : s.kolommen) {
                if (ko.x() + 1 < x0 || ko.x() - 1 > x0 + 15 || ko.z() + 1 < z0 || ko.z() - 1 > z0 + 15) {
                    continue;
                }
                if (!ko.los() || WolkTerrein.liftVrij(m, ko.x(), ko.z(), ko.voet(), ko.boven())) {
                    kolom(k, ko, zet);
                }
            }
            for (WolkTerrein.Eiland ei : s.eilanden) {
                int w = 2 * ei.r + 1;
                for (int x = Math.max(x0, ei.x - ei.r); x <= Math.min(x0 + 15, ei.x + ei.r); x++) {
                    for (int z = Math.max(z0, ei.z - ei.r); z <= Math.min(z0 + 15, ei.z + ei.r); z++) {
                        if (ei.nat[(x - ei.x + ei.r) + (z - ei.z + ei.r) * w] && k.meng[Kaart.index(x, z)] > 0) {
                            zet.zet(x, ei.top, z, pal.water, false);
                            zet.zet(x, ei.top - 1, z, pal.bed, false);
                        }
                    }
                }
            }
            for (WolkTerrein.Val v : s.vallen) {
                if ((v.x() >> 4) == k.cx && (v.z() >> 4) == k.cz) {
                    zet.zet(v.x(), v.boven(), v.z(), pal.stroomt, false);
                    for (int y = v.boven() - 1; y >= v.onder(); y--) {
                        zet.zet(v.x(), y, v.z(), pal.valt, false);
                    }
                }
            }
        }
        // the pond and the stream on the meadow
        for (int o = 0; o < 256; o++) {
            if (k.soort[o] == Kaart.WEIDE && k.water[o] != Kaart.GEEN) {
                int x = x0 + (o & 15), z = z0 + (o >> 4);
                zet.zet(x, k.hoogte[o], z, pal.bed, false);
                for (int y = k.hoogte[o] + 1; y <= k.water[o]; y++) {
                    zet.zet(x, y, z, pal.water, false);
                }
            }
        }
    }

    /** The cloud banks: per bank a soft top and underside over the chunk, then blocks, slabs and stairs. */
    private static void banken(BioModel m, Kaart k, Pal pal, Zet zet) {
        int x0 = k.cx << 4, z0 = k.cz << 4;
        int bereik = 30, cel = WolkTerrein.WOLK_CEL;
        float[] top = new float[18 * 18], bodem = new float[18 * 18];
        boolean[] roze = new boolean[18 * 18];
        for (int cx = Math.floorDiv(x0 - bereik, cel); cx <= Math.floorDiv(x0 + 15 + bereik, cel); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, cel); cz <= Math.floorDiv(z0 + 15 + bereik, cel); cz++) {
                WolkTerrein.Wolk[] alle = WolkTerrein.wolken(m, cx, cz);
                for (int bank = 0; bank < 2; bank++) {
                    boolean iets = false;
                    for (WolkTerrein.Wolk w : alle) {
                        if (w.bank() != bank || w.x() + w.rx() < x0 - 1 || w.x() - w.rx() > x0 + 16 || w.z() + w.rz() < z0 - 1 || w.z() - w.rz() > z0 + 16) {
                            continue;
                        }
                        if (!iets) {
                            java.util.Arrays.fill(top, Float.NaN);
                            iets = true;
                        }
                        for (int x = Math.max(x0 - 1, (int) Math.floor(w.x() - w.rx())); x <= Math.min(x0 + 16, (int) Math.ceil(w.x() + w.rx())); x++) {
                            for (int z = Math.max(z0 - 1, (int) Math.floor(w.z() - w.rz())); z <= Math.min(z0 + 16, (int) Math.ceil(w.z() + w.rz())); z++) {
                                double q = (x - w.x()) * (x - w.x()) / (w.rx() * w.rx()) + (z - w.z()) * (z - w.z()) / (w.rz() * w.rz());
                                if (q >= 1) {
                                    continue;
                                }
                                // (round on top, flatter underneath)
                                double hoog = w.ry() * Math.sqrt(1 - q);
                                int f = (x - x0 + 1) + (z - z0 + 1) * 18;
                                float t = (float) (w.y() + hoog), b = (float) (w.y() - 0.4 * hoog - 0.35);
                                if (top[f] != top[f]) {
                                    top[f] = t;
                                    bodem[f] = b;
                                    roze[f] = w.roze();
                                } else {
                                    if (t > top[f]) {
                                        top[f] = t;
                                        roze[f] = w.roze();
                                    }
                                    bodem[f] = Math.min(bodem[f], b);
                                }
                            }
                        }
                    }
                    if (iets) {
                        bank(k, top, bodem, roze, pal, zet);
                    }
                }
            }
        }
    }

    private static void bank(Kaart k, float[] top, float[] bodem, boolean[] roze, Pal pal, Zet zet) {
        int x0 = k.cx << 4, z0 = k.cz << 4;
        int[] buur = {-18, 1, 18, -1};
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                int f = (i + 1) + (j + 1) * 18;
                float t = top[f];
                if (t != t) {
                    continue;
                }
                float b = bodem[f];
                boolean r = roze[f];
                int x = x0 + i, z = z0 + j;
                int ya = (int) Math.ceil(b - 0.25), yb = (int) Math.floor(t + 0.25) - 1;
                if (yb < ya) {
                    if (t - b >= 0.4) {
                        zet.zet(x, (int) Math.floor((t + b) / 2), z, r ? pal.rozeOnder : pal.witOnder, true);
                    }
                    continue;
                }
                for (int y = ya; y <= yb; y++) {
                    zet.zet(x, y, z, r ? pal.roze : pal.wit, true);
                }
                if (t - (yb + 1) >= 0.25) {
                    // half a block more: a slab, or a stair where the bank is a block higher on exactly one side
                    int hoger = 0, kant = 0;
                    for (int d = 0; d < 4; d++) {
                        float nt = top[f + buur[d]];
                        if (nt == nt && nt + 0.25 >= yb + 2) {
                            hoger++;
                            kant = d;
                        }
                    }
                    zet.zet(x, yb + 1, z, hoger == 1 ? (r ? pal.rozeTrap : pal.witTrap)[kant] : r ? pal.rozeOnder : pal.witOnder, true);
                }
                if (ya - b >= 0.25) {
                    zet.zet(x, ya - 1, z, r ? pal.rozeBoven : pal.witBoven, true);
                }
            }
        }
    }

    /** The thin cloud sea: one flat layer that the islands rise through, wispy (slabs) at its edges. */
    private static void zee(BioModel m, Kaart k, Pal pal, Zet zet) {
        int x0 = k.cx << 4, z0 = k.cz << 4, y = WolkTerrein.WEIDE_Y + WolkTerrein.ZEE_HOOGTE;
        for (int o = 0; o < 256; o++) {
            if (k.soort[o] != Kaart.WEIDE || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4);
            double dik = WolkTerrein.zee(m, x, z);
            if (dik <= 0 || Luchtruim.bezet(x, z, 2)) {
                continue;
            }
            int vol = (int) dik;
            for (int i = 0; i < vol; i++) {
                zet.zet(x, y + i, z, pal.wit, true);
            }
            if (dik - vol >= 0.5) {
                zet.zet(x, y + vol, z, pal.witOnder, true);
            }
        }
    }

    /** A stepping stone of cloud: a plus of blocks, slabs at its corners, rounded underneath. */
    private static void stap(Kaart k, WolkTerrein.Stap st, Pal pal, Zet zet) {
        boolean r = (st.hoeken() & 128) != 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = st.x() + dx, z = st.z() + dz;
                if ((x >> 4) != k.cx || (z >> 4) != k.cz) {
                    continue;
                }
                boolean hoek = dx != 0 && dz != 0;
                zet.zet(x, st.top(), z, hoek ? (r ? pal.rozeOnder : pal.witOnder) : r ? pal.roze : pal.wit, true);
                if (!hoek) {
                    zet.zet(x, st.top() - 1, z, dx == 0 && dz == 0 ? (r ? pal.roze : pal.wit) : r ? pal.rozeBoven : pal.witBoven, true);
                }
            }
        }
    }

    /** A cushion of cloud at an island's rim: under a lift pad that sticks out, and where the stream down lands. */
    private static void kussen(Kaart k, WolkTerrein.Kussen ku, Pal pal, Zet zet) {
        int x0 = k.cx << 4, z0 = k.cz << 4, r = (int) Math.ceil(ku.straal());
        for (int x = Math.max(x0, ku.x() - r); x <= Math.min(x0 + 15, ku.x() + r); x++) {
            for (int z = Math.max(z0, ku.z() - r); z <= Math.min(z0 + 15, ku.z() + r); z++) {
                double d = Math.hypot(x - ku.x(), z - ku.z());
                if (d > ku.straal()) {
                    continue;
                }
                zet.zet(x, ku.y(), z, d > ku.straal() - 0.9 ? pal.witOnder : pal.wit, true);
                if (d <= ku.straal() - 1.8) {
                    zet.zet(x, ku.y() - 1, z, pal.witBoven, true);
                }
            }
        }
    }

    /** The part of a 3 x 3 lift column that lies in this chunk: the pads at its foot, the stream above them. */
    private static void kolom(Kaart k, WolkTerrein.Kolom ko, Zet zet) {
        BlockState pad = Bio.blok("wolkenlift", Blocks.WHITE_WOOL).defaultBlockState(), stroom = Bio.blok("wolkenstroom", Blocks.AIR).defaultBlockState();
        if (pad.hasProperty(HorizontalDirectionalBlock.FACING) && pad.hasProperty(WolkenstroomBlock.DOWN)) {
            pad = pad.setValue(HorizontalDirectionalBlock.FACING, ko.kijk()).setValue(WolkenstroomBlock.DOWN, ko.omlaag());
        }
        if (stroom.hasProperty(HorizontalDirectionalBlock.FACING) && stroom.hasProperty(WolkenstroomBlock.DOWN)) {
            stroom = stroom.setValue(HorizontalDirectionalBlock.FACING, ko.kijk()).setValue(WolkenstroomBlock.DOWN, ko.omlaag());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int px = ko.x() + dx, pz = ko.z() + dz;
                if ((px >> 4) != k.cx || (pz >> 4) != k.cz) {
                    continue;
                }
                zet.zet(px, ko.voet(), pz, pad, false);
                for (int y = ko.voet() + 1; y <= ko.boven(); y++) {
                    zet.zet(px, y, pz, stroom, false);
                }
            }
        }
    }

    // ==================================================================================================================
    // dressing the land
    // ==================================================================================================================
    private static long kolomHash(long zaad, int x, int z) {
        return BioModel.mix(zaad ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL);
    }

    private static int versier(WorldGenLevel level, BioModel m, Kaart k, List<WolkTerrein.Stapel> stapels) {
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        Block gras = WolkBlokken.GRAS.get();
        BlockState kristal = WolkBlokken.KRISTAL.get().defaultBlockState(), punt = WolkBlokken.KRISTALPUNT.get().defaultBlockState();
        BlockState rank = WolkBlokken.RANK.get().defaultBlockState().setValue(HangingMossBlock.TIP, false), rankPunt = rank.setValue(HangingMossBlock.TIP, true);
        BlockState[] bloemen = {Bio.blok("roze_guhbloem", Blocks.PINK_TULIP).defaultBlockState(), Bio.blok("guhoortjes", Blocks.PINK_TULIP).defaultBlockState(),
                Bio.blok("roze_guhbloem", Blocks.PINK_TULIP).defaultBlockState(), Bio.blok("knabbelroos", Blocks.POPPY).defaultBlockState()};
        BlockState pluis = Bio.blok("pluisgras", Blocks.SHORT_GRASS).defaultBlockState();
        for (WolkTerrein.Stapel s : stapels) {
            for (WolkTerrein.Eiland ei : s.eilanden) {
                if (ei.x + ei.r < x0 || ei.x - ei.r > x0 + 15 || ei.z + ei.r < z0 || ei.z - ei.r > z0 + 15) {
                    continue;
                }
                // the crystal tips of the drip points
                for (int t = 0; t < ei.punten.length; t += 2) {
                    int tx = ei.x + ei.punten[t], tz = ei.z + ei.punten[t + 1], diep = ei.onder(tx, tz);
                    for (int b = 0; b < 5; b++) {
                        int x = tx + (b == 1 ? 1 : b == 2 ? -1 : 0), z = tz + (b == 3 ? 1 : b == 4 ? -1 : 0);
                        if ((x >> 4) != k.cx || (z >> 4) != k.cz || !ei.is(x - ei.x, z - ei.z)) {
                            continue;
                        }
                        int o = Kaart.index(x, z), onder = ei.onder(x, z);
                        if (k.meng[o] <= 0 || onder < k.hoogte[o] + 4 || onder > diep + 3) {
                            continue;
                        }
                        for (int y = onder; y <= Math.min(onder + (b == 0 ? 1 : 0), ei.top - 2); y++) {
                            level.setBlock(p.set(x, y, z), kristal, 18);
                            gezet++;
                        }
                        if (b == 0 && level.isEmptyBlock(p.set(x, onder - 1, z))) {
                            level.setBlock(p, punt, 18);
                            gezet++;
                        }
                    }
                }
                int w = 2 * ei.r + 1;
                for (int x = Math.max(x0, ei.x - ei.r); x <= Math.min(x0 + 15, ei.x + ei.r); x++) {
                    for (int z = Math.max(z0, ei.z - ei.r); z <= Math.min(z0 + 15, ei.z + ei.r); z++) {
                        int i = (x - ei.x + ei.r) + (z - ei.z + ei.r) * w;
                        int o = Kaart.index(x, z);
                        if (ei.diep[i] < 0 || k.meng[o] <= 0) {
                            continue;
                        }
                        long h = kolomHash(ei.zaad, x, z);
                        int onder = ei.top - ei.diep[i], boven = ei.top + ei.bov[i];
                        // vines and little crystal points under the island
                        double kr = BioModel.kans(h, 1);
                        if (onder >= k.hoogte[o] + 6 && kr < (ei.rots ? 0.06 : 0.15)) {
                            if (kr < 0.025) {
                                if (level.isEmptyBlock(p.set(x, onder - 1, z))) {
                                    level.setBlock(p, punt, 18);
                                    gezet++;
                                }
                            } else {
                                int lang = 1 + (int) (BioModel.kans(h, 2) * (ei.rots ? 2 : ei.klasse == WolkTerrein.KLEIN ? 4 : 6));
                                for (int j = 1; j <= lang; j++) {
                                    if (onder - j <= k.hoogte[o] + 2 || !level.isEmptyBlock(p.set(x, onder - j, z))) {
                                        if (j > 1) {
                                            level.setBlock(p.set(x, onder - j + 1, z), rankPunt, 18);
                                        }
                                        break;
                                    }
                                    level.setBlock(p, j == lang ? rankPunt : rank, 18);
                                    gezet++;
                                }
                            }
                        }
                        // flowers and tufts on top
                        double kb = BioModel.kans(h, 3);
                        if (!ei.nat[i] && kb < (ei.rots ? 0.05 : 0.11) && level.getBlockState(p.set(x, boven, z)).is(gras) && level.isEmptyBlock(p.set(x, boven + 1, z))) {
                            level.setBlock(p, kb < 0.06 ? bloemen[(int) (BioModel.kans(h, 4) * 4)] : pluis, 18);
                            gezet++;
                        }
                    }
                }
                for (int[] boom : ei.bomen) {
                    int x = ei.x + boom[0], z = ei.z + boom[1];
                    if ((x >> 4) == k.cx && (z >> 4) == k.cz && k.meng[Kaart.index(x, z)] >= 1f) {
                        gezet += boom(level, x, ei.boven(x, z), z, boom[2], kolomHash(ei.zaad, x, z), gras);
                    }
                }
            }
        }
        // the meadow: calm, with flowers and fluff in loose patches and now and then a small tree
        for (int o = 0; o < 256; o++) {
            if (k.soort[o] != Kaart.WEIDE || k.meng[o] < 1f || k.water[o] != Kaart.GEEN) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4);
            long h = kolomHash(m.zaad + 8101, x, z);
            boolean veldje = BioModel.kans(m.hash(x >> 3, z >> 3, 8102), 0) < 0.2;
            double kb = BioModel.kans(h, 0);
            if (kb < (veldje ? 0.07 : 0.004) && level.getBlockState(p.set(x, k.hoogte[o], z)).is(gras) && level.isEmptyBlock(p.set(x, k.hoogte[o] + 1, z))) {
                level.setBlock(p, BioModel.kans(h, 1) < 0.6 ? bloemen[(int) (BioModel.kans(h, 2) * 4)] : pluis, 18);
                gezet++;
            }
        }
        long hc = m.hash(k.cx, k.cz, 8103);
        if (BioModel.kans(hc, 0) < 0.14) {
            int x = x0 + 2 + (int) (BioModel.kans(hc, 1) * 12), z = z0 + 2 + (int) (BioModel.kans(hc, 2) * 12), o = Kaart.index(x, z);
            if (k.soort[o] == Kaart.WEIDE && k.meng[o] >= 1f && k.water[o] == Kaart.GEEN && weideBoomVrij(stapels, x, k.hoogte[o], z)) {
                gezet += boom(level, x, k.hoogte[o], z, BioModel.kans(hc, 3) < 0.6 ? 0 : 1, hc, gras);
            }
        }
        return gezet;
    }

    private static boolean weideBoomVrij(List<WolkTerrein.Stapel> stapels, int x, int y, int z) {
        WolkTerrein.Doos boom = new WolkTerrein.Doos(x - 4, y + 1, z - 4, x + 4, y + 9, z + 4);
        for (WolkTerrein.Stapel s : stapels) {
            for (WolkTerrein.Doos d : s.vrij) {
                if (d.snijdt(boom)) {
                    return false;
                }
            }
            for (WolkTerrein.Kolom ko : s.kolommen) {
                if (Math.abs(ko.x() - x) <= 6 && Math.abs(ko.z() - z) <= 6) {
                    return false;
                }
            }
            for (WolkTerrein.Eiland e : s.eilanden) {
                if (WolkTerrein.raakt(e, boom)) {
                    return false;
                }
            }
            if (s.plas != null && x >= s.plas.x0 - 3 && x <= s.plas.x1 + 3 && z >= s.plas.z0 - 3 && z <= s.plas.z1 + 3) {
                return false;
            }
            for (WolkTerrein.Val v : s.vallen) {
                if (Math.abs(v.x() - x) <= 3 && Math.abs(v.z() - z) <= 3) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A tree with its foot on (x, y, z): 0 a small pluizenboom, 1 a small guhbloesem, 2 a big one (the mod's own tree feature). */
    private static int boom(WorldGenLevel level, int x, int y, int z, int soort, long h, Block gras) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (!level.getBlockState(p.set(x, y, z)).is(gras) || !level.isEmptyBlock(p.set(x, y + 1, z))) {
            return 0;
        }
        boolean bloesem = soort == 1 || soort == 2 && BioModel.kans(h, 10) < 0.5;
        if (soort == 2) {
            ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id(bloesem ? "guhbloesem" : "pluizenboom"));
            var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key);
            if (feature.isPresent() && feature.get().value().place(level, level.getLevel().getChunkSource().getGenerator(), RandomSource.create(h), new BlockPos(x, y + 1, z))) {
                return 60;
            }
            // (no room for the big one: a small one)
        }
        BlockState stam = (bloesem ? Bio.blok("guhbloesem_log", Blocks.CHERRY_LOG) : Bio.blok("pluizenboom_stam", Blocks.CHERRY_LOG)).defaultBlockState();
        BlockState blad = (bloesem ? Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES) : Bio.blok("pluizenboom_bladeren", Blocks.CHERRY_LEAVES)).defaultBlockState();
        int hoog = 3 + (int) (BioModel.kans(h, 11) * 2), gezet = 0;
        // a round little crown: two wide rings, a narrower one and a cap
        for (int dy = -1; dy <= 2; dy++) {
            double r2 = dy <= 0 ? 5.2 : dy == 1 ? 2.2 : 1.0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > r2 || dy == -1 && Math.abs(dx) == 2 && Math.abs(dz) == 2
                            || dx * dx + dz * dz > r2 - 1.5 && BioModel.kans(h, 20 + (dy + 1) * 25 + (dx + 2) * 5 + dz + 2) < 0.3) {
                        continue;
                    }
                    if (level.isEmptyBlock(p.set(x + dx, y + hoog + dy, z + dz))) {
                        BlockState b = blad;
                        if (b.hasProperty(LeavesBlock.DISTANCE)) {
                            b = b.setValue(LeavesBlock.DISTANCE, Math.max(1, Math.min(6, Math.abs(dx) + Math.abs(dz) + Math.max(0, dy))));
                        }
                        level.setBlock(p, b, 18);
                        gezet++;
                    }
                }
            }
        }
        for (int dy = 1; dy <= hoog; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 18);
        }
        return gezet + hoog;
    }

    private WolkVulling() {
    }
}
