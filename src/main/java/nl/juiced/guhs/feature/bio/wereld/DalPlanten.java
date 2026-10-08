package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * biomes3 wereld, the Klaterdal: what grows there, placed with the terrain (by {@link DalVulling}) because every choice
 * needs the terrain model: how far the water is, whether the ground is plain terrace, where the rock is.
 * <p>
 * Calm and uncluttered is the rule (the first sketch had too much tall grass): no grass at all; soft patches of pale moss
 * ({@link DalBlokken#MOS}) on the pink ground, a few flowers in small clumps of one kind, reeds on stretches of bank,
 * petals and a lily pad on still water. Trees keep {@link #BOOM_AF} blocks from every water so no crown hides the river:
 * guhbloesem trees thinly spread, a rare red or orange esdoorn as an accent ({@link #esdoorn}: a rounded crown of one
 * colour with a soft cloud of the other), crooked little trees on boulders and rock ledges ({@link #bonsai}), and
 * here and there a grove of guh-bamboe. Nothing is planted in or on a building ({@link BouwRuimte#inBuilding}).
 * Everything is a pure function of the world seed and the position.
 */
public final class DalPlanten {
    // <dal-planten>
    /** Trees stand at least this far from water. */
    public static final int BOOM_AF = 7;
    /** One try for a tree per cell of this size; the chance of a guhbloesem, of an esdoorn. */
    public static final int BOOM_CEL = 12;
    public static final double BLOESEM_KANS = 0.30, ESDOORN_KANS = 0.04;
    /** One try for a bamboo grove per cell. */
    public static final int BAMBOE_CEL = 36;
    public static final double BAMBOE_KANS = 0.25;
    /** The moss noise above this is a moss patch (about a fifth of the ground). */
    public static final double MOS_VANAF = 0.24;
    /** Flowers: one try for a clump per cell. */
    public static final int BLOEM_CEL = 14;
    public static final double BLOEM_KANS = 0.3;
    /** How many boulders carry a crooked little tree. */
    public static final double BONSAI_KANS = 0.45;
    // </dal-planten>

    private static final String[] BLOEMEN = {"roze_guhbloem", "knabbelroos", "roze_hibiscus", "guhoortjes"};
    private static final ResourceKey<ConfiguredFeature<?, ?>> GUHBLOESEM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("guhbloesem"));

    private static boolean grond(BlockState s) {
        return s.is(Blocks.PINK_WOOL) || s.is(DalBlokken.MOS.get());
    }

    /** Plain dry terrace ground of the Klaterdal biome at full strength? */
    private static boolean vlak(BioModel m, int x, int z) {
        return m.terras(x, z) >= 0 && m.droog(x, z) && m.vlag(x, z) == 0 && m.soort(x, z) == Kaart.DAL;
    }

    /** Is there water (or anything that is not plain ground at this height) within r blocks? */
    private static boolean vrij(BioModel m, int x, int z, int r, int h, boolean ookRots) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) {
                    continue;
                }
                if (m.water(x + dx, z + dz) != Kaart.GEEN || (m.vlag(x + dx, z + dz) & Kaart.LIP) != 0) {
                    return false;
                }
                if (ookRots && Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && (m.hoogte(x + dx, z + dz) != h || m.vlag(x + dx, z + dz) != 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean gebouwen = !level.getChunk(k.cx, k.cz).getAllReferences().isEmpty();
        BlockState mos = DalBlokken.MOS.get().defaultBlockState(), riet = DalBlokken.RIET.get().defaultBlockState();
        BlockState blaadjes = Bio.blok("drijvende_bloesemblaadjes", Blocks.PINK_PETALS).defaultBlockState();
        BlockState lelie = Bio.blok("guh_waterlelie", Blocks.LILY_PAD).defaultBlockState();
        var poelen = DalTerrein.poelen(m, k.cx, k.cz);
        // --- per column: moss, reeds, petals ---------------------------------------------------------------------------------
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] < 0 || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o], w = k.water[o];
            long hash = m.hash(x, z, 6101);
            if (w != Kaart.GEEN) {
                // petals and a lily pad on still water: not at a fall, and not where a fall lands
                if ((k.vlag[o] & Kaart.VAL) != 0) {
                    continue;
                }
                boolean stil = true;
                for (int dx = -2; dx <= 2 && stil; dx++) {
                    for (int dz = -2; dz <= 2 && stil; dz++) {
                        int bw = m.water(x + dx, z + dz);
                        stil = bw == Kaart.GEEN || bw == w;
                    }
                }
                if (!stil) {
                    continue;
                }
                boolean poel = false;
                for (DalTerrein.Poel pl : poelen) {
                    poel |= pl.rand(x - pl.x(), z - pl.z()) > 0;
                }
                double kans = poel ? 0.26 : m.ruis(BioModel.R_DETAIL, x * 0.8 + 500, z * 0.22 - 500) > 0.38 ? 0.3 : k.terras[o] == 0 ? 0.03 : 0.012;
                double worp = BioModel.kans(hash, 0);
                if (worp < kans && level.isEmptyBlock(p.set(x, w + 1, z)) && !(gebouwen && BouwRuimte.inBuilding(level, p))) {
                    level.setBlock(p, DalVulling.met(blaadjes, "dichtheid", String.valueOf(1 + (int) (BioModel.kans(hash, 1) * (poel ? 3 : 2)))), 2);
                    gezet++;
                } else if (poel && worp > 0.955 && level.isEmptyBlock(p.set(x, w + 1, z)) && !(gebouwen && BouwRuimte.inBuilding(level, p))) {
                    level.setBlock(p, lelie, 2);
                    gezet++;
                }
                continue;
            }
            if (k.vlag[o] != 0 || !grond(level.getBlockState(p.set(x, h, z))) || gebouwen && BouwRuimte.inBuilding(level, p)) {
                continue;
            }
            boolean oever = false, rots = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int bx = x + d.getStepX(), bz = z + d.getStepZ();
                oever |= m.water(bx, bz) == h - 1;
                rots |= m.water(bx, bz) == Kaart.GEEN && (m.vlag(bx, bz) & DalTerrein.VORM) != 0 && m.hoogte(bx, bz) > h;
            }
            boolean opMos = m.ruis(BioModel.R_DETAIL, x * 0.35 + 900, z * 0.35 + 300) > MOS_VANAF || rots && BioModel.kans(hash, 2) < 0.7
                    || oever && BioModel.kans(hash, 2) < 0.35;
            if (opMos) {
                level.setBlock(p, mos, 2);
                gezet++;
            }
            if (oever && m.ruis(BioModel.R_DETAIL, x * 0.25 + 123, z * 0.25 + 77) > 0.12 && BioModel.kans(hash, 3) < 0.5
                    && level.isEmptyBlock(p.set(x, h + 1, z))) {
                level.setBlock(p, riet, 2);
                gezet++;
            }
        }
        // --- flowers: a small clump of one kind per cell, now and then -----------------------------------------------------------
        for (int cx = Math.floorDiv(x0 - 4, BLOEM_CEL); cx <= Math.floorDiv(x0 + 19, BLOEM_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - 4, BLOEM_CEL); cz <= Math.floorDiv(z0 + 19, BLOEM_CEL); cz++) {
                long hash = m.hash(cx, cz, 6201);
                if (BioModel.kans(hash, 0) >= BLOEM_KANS) {
                    continue;
                }
                int mx = cx * BLOEM_CEL + (int) (BioModel.kans(hash, 1) * BLOEM_CEL), mz = cz * BLOEM_CEL + (int) (BioModel.kans(hash, 2) * BLOEM_CEL);
                BlockState bloem = Bio.blok(BLOEMEN[(int) (BioModel.kans(hash, 3) * BLOEMEN.length)], Blocks.PINK_TULIP).defaultBlockState();
                int aantal = 2 + (int) (BioModel.kans(hash, 4) * 4);
                for (int i = 0; i < aantal; i++) {
                    int x = mx + (int) Math.round((BioModel.kans(hash, 10 + i) - 0.5) * 6), z = mz + (int) Math.round((BioModel.kans(hash, 30 + i) - 0.5) * 6);
                    if ((x >> 4) != k.cx || (z >> 4) != k.cz || !vlak(m, x, z)) {
                        continue;
                    }
                    int h = m.hoogte(x, z);
                    if (grond(level.getBlockState(p.set(x, h, z))) && level.isEmptyBlock(p.set(x, h + 1, z)) && !(gebouwen && BouwRuimte.inBuilding(level, p))) {
                        level.setBlock(p, bloem, 2);
                        gezet++;
                    }
                }
            }
        }
        // --- trees: thinly spread, never near the water ---------------------------------------------------------------------------
        for (int cx = Math.floorDiv(x0, BOOM_CEL); cx <= Math.floorDiv(x0 + 15, BOOM_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, BOOM_CEL); cz <= Math.floorDiv(z0 + 15, BOOM_CEL); cz++) {
                long hash = m.hash(cx, cz, 6301);
                int x = cx * BOOM_CEL + 1 + (int) (BioModel.kans(hash, 1) * (BOOM_CEL - 2)), z = cz * BOOM_CEL + 1 + (int) (BioModel.kans(hash, 2) * (BOOM_CEL - 2));
                double worp = BioModel.kans(hash, 0);
                if ((x >> 4) != k.cx || (z >> 4) != k.cz || worp >= BLOESEM_KANS + ESDOORN_KANS || !vlak(m, x, z)) {
                    continue;
                }
                int h = m.hoogte(x, z);
                if (!vrij(m, x, z, BOOM_AF, h, true) || !grond(level.getBlockState(p.set(x, h, z))) || !level.isEmptyBlock(p.set(x, h + 1, z))
                        || inGebouw(level, x, h + 1, z, 3)) {
                    continue;
                }
                RandomSource rand = RandomSource.create(hash);
                if (worp < ESDOORN_KANS) {
                    gezet += esdoorn(level, rand, x, h + 1, z);
                } else {
                    var boom = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(GUHBLOESEM);
                    if (boom.isPresent() && boom.get().value().place(level, level.getLevel().getChunkSource().getGenerator(), rand, new BlockPos(x, h + 1, z))) {
                        gezet += 40;
                    }
                }
            }
        }
        // --- crooked little trees on boulders -------------------------------------------------------------------------------------
        for (DalTerrein.Kei kei : DalTerrein.keien(m, k.cx, k.cz)) {
            if ((kei.x() >> 4) != k.cx || (kei.z() >> 4) != k.cz || kei.hoog() < 2 || kei.straal() < 1.8) {
                continue;
            }
            long hash = m.hash(kei.x(), kei.z(), 6401);
            int h = m.hoogte(kei.x(), kei.z());
            if (BioModel.kans(hash, 0) < BONSAI_KANS && (m.vlag(kei.x(), kei.z()) & DalTerrein.VORM) != 0 && m.water(kei.x(), kei.z()) == Kaart.GEEN
                    && h > DalTerrein.HOOGTE[kei.terras()] && !inGebouw(level, kei.x(), h + 1, kei.z(), 2)) {
                gezet += bonsai(level, RandomSource.create(hash), kei.x(), h + 1, kei.z());
            }
        }
        // --- a grove of guh-bamboe ------------------------------------------------------------------------------------------------
        for (int cx = Math.floorDiv(x0, BAMBOE_CEL); cx <= Math.floorDiv(x0 + 15, BAMBOE_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, BAMBOE_CEL); cz <= Math.floorDiv(z0 + 15, BAMBOE_CEL); cz++) {
                long hash = m.hash(cx, cz, 6501);
                int x = cx * BAMBOE_CEL + 6 + (int) (BioModel.kans(hash, 1) * (BAMBOE_CEL - 12)), z = cz * BAMBOE_CEL + 6 + (int) (BioModel.kans(hash, 2) * (BAMBOE_CEL - 12));
                if ((x >> 4) != k.cx || (z >> 4) != k.cz || BioModel.kans(hash, 0) >= BAMBOE_KANS || !vlak(m, x, z)
                        || !vrij(m, x, z, 5, m.hoogte(x, z), false) || inGebouw(level, x, m.hoogte(x, z) + 1, z, 5)) {
                    continue;
                }
                gezet += bamboe(level, m, hash, x, z, 3.2 + 2.3 * BioModel.kans(hash, 3));
            }
        }
        return gezet;
    }

    private static boolean inGebouw(WorldGenLevel level, int x, int y, int z, int r) {
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                BlockPos pos = new BlockPos(x + dx, y, z + dz);
                if (BouwRuimte.inBuilding(level, pos) || BouwRuimte.inBuilding(level, pos.above(4))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockState blad(BlockState s) {
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    private static boolean zet(WorldGenLevel level, BlockPos.MutableBlockPos p, BlockState s) {
        if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
            level.setBlock(p, s, 2);
            return true;
        }
        return false;
    }

    /**
     * An esdoorn accent tree with its foot at (x, y, z): a short trunk and a rounded, slightly flattened crown. The crown
     * is one colour; the other colour only comes as a soft cloud in its sunny top (never block by block: that looked like
     * a hard mosaic in the sketch).
     */
    static int esdoorn(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        BlockState stam = Bio.blok("esdoorn_stam", Blocks.CHERRY_LOG).defaultBlockState();
        boolean rood = rand.nextFloat() < 0.6f;
        BlockState een = blad(Bio.blok(rood ? "esdoorn_bladeren_rood" : "esdoorn_bladeren_oranje", Blocks.CHERRY_LEAVES).defaultBlockState());
        BlockState ander = blad(Bio.blok(rood ? "esdoorn_bladeren_oranje" : "esdoorn_bladeren_rood", Blocks.CHERRY_LEAVES).defaultBlockState());
        int hoog = 4 + rand.nextInt(2), gezet = 0;
        double straal = 2.9 + rand.nextFloat() * 0.7, wolkX = (rand.nextFloat() - 0.5) * 3, wolkZ = (rand.nextFloat() - 0.5) * 3, fase = rand.nextFloat() * 6.283f;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = (int) Math.ceil(straal);
        int cy = y + hoog;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    double hoek = Math.atan2(dz, dx);
                    double rand2 = straal * (1 + 0.10 * Math.sin(3 * hoek + fase) + 0.06 * Math.sin(5 * hoek - fase));
                    double q = (dx * dx + dz * dz) / (rand2 * rand2) + (dy > 0 ? dy * dy / 6.2 : dy * dy / 2.4);
                    if (q > 1.0 || q > 0.82 && rand.nextFloat() < 0.35f) {
                        continue;
                    }
                    // the cloud of the other colour: a soft ball high in the crown, a little to one side
                    double w = (dx - wolkX) * (dx - wolkX) + (dz - wolkZ) * (dz - wolkZ) + (dy - 2.2) * (dy - 2.2) * 1.6;
                    if (zet(level, p.set(x + dx, cy + dy, z + dz), w < 3.4 ? ander : een)) {
                        gezet++;
                    }
                }
            }
        }
        for (int dy = 0; dy < hoog + 1; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 2);
            gezet++;
        }
        return gezet;
    }

    /**
     * A crooked little tree on a rock, its foot at (x, y, z): two blocks of trunk, a jog to the side, and two or three flat
     * pads of leaves at different heights (a cloud-pruned garden tree). Most are pale green, some blossom pink, a few red.
     */
    static int bonsai(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        BlockState stam = Bio.blok("esdoorn_stam", Blocks.CHERRY_LOG).defaultBlockState();
        float soort = rand.nextFloat();
        BlockState loof = blad(soort < 0.62f ? DalBlokken.BONSAIBLAD.get().defaultBlockState()
                : soort < 0.87f ? Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES).defaultBlockState()
                : Bio.blok("esdoorn_bladeren_rood", Blocks.CHERRY_LEAVES).defaultBlockState());
        Direction zij = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (!level.isEmptyBlock(p.set(x, y, z)) || !level.isEmptyBlock(p.set(x, y + 3, z))) {
            return 0;
        }
        int gezet = 0, hoog = 1 + rand.nextInt(2);
        for (int dy = 0; dy < hoog; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 2);
        }
        int tx = x + zij.getStepX(), tz = z + zij.getStepZ(), ty = y + hoog;
        BlockState dwars = stam.hasProperty(RotatedPillarBlock.AXIS) ? stam.setValue(RotatedPillarBlock.AXIS, zij.getAxis()) : stam;
        level.setBlock(p.set(x, ty, z), dwars, 2);
        level.setBlock(p.set(tx, ty, tz), dwars, 2);
        level.setBlock(p.set(tx, ty + 1, tz), stam, 2);
        gezet += hoog + 3;
        // the top pad over the end of the jog, a smaller one on the other side lower down, sometimes a third at the tip
        gezet += kussen(level, rand, tx, ty + 2, tz, loof, true);
        gezet += kussen(level, rand, x - zij.getStepX(), ty + 1, z - zij.getStepZ(), loof, false);
        if (rand.nextFloat() < 0.5f) {
            gezet += kussen(level, rand, tx + zij.getStepX() * 2, ty, tz + zij.getStepZ() * 2, loof, false);
        }
        return gezet;
    }

    private static int kussen(WorldGenLevel level, RandomSource rand, int x, int y, int z, BlockState loof, boolean groot) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int gezet = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean hoek = dx != 0 && dz != 0;
                if (hoek && (!groot || rand.nextFloat() < 0.45f)) {
                    continue;
                }
                if (zet(level, p.set(x + dx, y, z + dz), loof)) {
                    gezet++;
                }
            }
        }
        if (groot && zet(level, p.set(x, y + 1, z), loof)) {
            gezet++;
        }
        return gezet;
    }

    /** A grove of guh-bamboe around (x, z): stalks on about half of the plain ground within the radius, on moss. */
    private static int bamboe(WorldGenLevel level, BioModel m, long hash, int x, int z, double straal) {
        BlockState stengel = Bio.blok("guh_bamboe", Blocks.BAMBOO).defaultBlockState();
        stengel = DalVulling.met(stengel, "stage", "0");
        BlockState mos = DalBlokken.MOS.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = (int) Math.ceil(straal), gezet = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double q = (dx * dx + dz * dz) / (straal * straal);
                int px = x + dx, pz = z + dz;
                if (q > 1 || !vlak(m, px, pz)) {
                    continue;
                }
                int h = m.hoogte(px, pz);
                if (!grond(level.getBlockState(p.set(px, h, pz))) || BouwRuimte.inBuilding(level, p)) {
                    continue;
                }
                level.setBlock(p, mos, 2);
                long kh = m.hash(px, pz, 6502);
                if (BioModel.kans(kh, 0) >= 0.5 - 0.2 * q || !level.isEmptyBlock(p.set(px, h + 1, pz))) {
                    continue;
                }
                int hoog = Math.max(3, (int) Math.round((5 + BioModel.kans(kh, 1) * 4.5) * (1 - 0.3 * q)));
                for (int i = 0; i < hoog; i++) {
                    if (!level.isEmptyBlock(p.set(px, h + 1 + i, pz))) {
                        hoog = i;
                        break;
                    }
                }
                for (int i = 0; i < hoog; i++) {
                    String loof = i == hoog - 1 ? (hoog == 1 ? "small" : "large") : i == hoog - 2 ? "small" : "none";
                    level.setBlock(p.set(px, h + 1 + i, pz), DalVulling.met(stengel, "leaves", loof), 2);
                    gezet++;
                }
            }
        }
        return gezet;
    }

    private DalPlanten() {
    }
}
