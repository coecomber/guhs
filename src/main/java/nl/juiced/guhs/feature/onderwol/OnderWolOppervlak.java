package nl.juiced.guhs.feature.onderwol;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * 1.7.0 "Onder de Wol", ronde 27/28: a cheerful finish for the surface of the common Guhmension (the data:
 * tools/features/onderwol_oppervlak.py, which also writes the vanilla-typed pieces: the flower meadows, the fallen logs,
 * the fluff in the air). The Java features here:
 * <ul>
 *   <li>{@link PastelWol} the quilt: big soft patches where the top 1-3 pink wool turns lila, white or peach;</li>
 *   <li>{@link Viltkiezel} soft gravel along the waterline of lakes, seas and saus;</li>
 *   <li>{@link Wolkei} little round boulders of viltsteen or wool with a pink mossy carpet;</li>
 *   <li>{@link Vijvertje} a small pond with a soft gravel bed, lily pads and reeds;</li>
 *   <li>{@link Waterval} a spring in a cliff face that falls into a little pool (water, or kaas saus);</li>
 *   <li>{@link Paddenstoelenkring} a fairy ring of glinsterpaddenstoelen, now and then with a guh in the middle.</li>
 * </ul>
 * All deterministic from the world seed (and the feature's own decoration random). New chunks only.
 */
public final class OnderWolOppervlak {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final DeferredHolder<Feature<?>, PastelWol> PASTELWOL = FEATURES.register("onderwol_pastelwol", PastelWol::new);
    public static final DeferredHolder<Feature<?>, Viltkiezel> VILTKIEZEL = FEATURES.register("onderwol_viltkiezel", Viltkiezel::new);
    public static final DeferredHolder<Feature<?>, Wolkei> WOLKEI = FEATURES.register("onderwol_wolkei", Wolkei::new);
    public static final DeferredHolder<Feature<?>, Vijvertje> VIJVERTJE = FEATURES.register("onderwol_vijvertje", Vijvertje::new);
    public static final DeferredHolder<Feature<?>, Waterval> WATERVAL = FEATURES.register("onderwol_waterval", Waterval::new);
    public static final DeferredHolder<Feature<?>, Paddenstoelenkring> KRING = FEATURES.register("onderwol_paddenstoelenkring", Paddenstoelenkring::new);

    /** The natural ground of the surface these features may change or build on (wool, kaasknabbels, grass...). */
    public static final TagKey<Block> GROND = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/oppervlakgrond"));
    /** The biomes where the quilt and the soft gravel are worked out column by column (so biome borders stay clean). */
    public static final TagKey<Biome> PASTELWOL_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/pastelwol"));
    public static final TagKey<Biome> KIEZEL_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/viltkiezel"));

    /** The share of the world the quilt patches cover is set by these thresholds of a simplex noise (range about -1..1). */
    private static final double VLEK_SCHAAL = 38.0, VLEK_HOOG = 0.50, VLEK_LAAG = -0.56, KLEUR_SCHAAL = 75.0;

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the seed's noises (pure functions of the seed, so neighbouring chunks agree)
    // ------------------------------------------------------------------------------------------------------------------
    private record Ruis(long zaad, SimplexNoise vlek, SimplexNoise kleur, SimplexNoise kiezel) {
    }

    private static volatile Ruis ruis;

    private static Ruis ruis(long zaad) {
        Ruis r = ruis;
        if (r == null || r.zaad() != zaad) {
            r = new Ruis(zaad, new SimplexNoise(new LegacyRandomSource(zaad ^ 0x9A57_E1L)), new SimplexNoise(new LegacyRandomSource(zaad ^ 0x0C01_0E25L)),
                    new SimplexNoise(new LegacyRandomSource(zaad ^ 0x1C1E_2E1L)));
            ruis = r;
        }
        return r;
    }

    /** A fixed pseudo-random number in [0, 1) for this column (no state: the same in every run). */
    static double hash(long zaad, int x, int z, int salt) {
        long h = zaad ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    static boolean bron(BlockState state) {
        FluidState f = state.getFluidState();
        return !f.isEmpty() && f.isSource();
    }

    static boolean draagt(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.isAir() && s.getFluidState().isEmpty() && s.isFaceSturdy(level, pos, Direction.UP);
    }

    static BlockState blok(String id, Block standIn) {
        return Bio.blok(id, standIn).defaultBlockState();
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The quilt (once per chunk, before the structures): where the patch noise is far from zero the top 1-3 pink wool of
     * a column becomes magenta (lila), white or peach (the knuffelblok), deeper in the middle of a patch, with a stitched,
     * slightly dithered edge. Only pink wool is touched.
     */
    public static class PastelWol extends Feature<NoneFeatureConfiguration> {
        public PastelWol() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            BlockState[] palet = {Blocks.MAGENTA_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(),
                    Blocks.WHITE_TERRACOTTA.defaultBlockState()};   // (peach; not knuffelblok_perzik: that is a bought building block)
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    double v = r.vlek().getValue(x / VLEK_SCHAAL, z / VLEK_SCHAAL) + (hash(zaad, x, z, 1) - 0.5) * 0.08;
                    double over;
                    int set;
                    if (v > VLEK_HOOG) {
                        over = v - VLEK_HOOG;
                        set = 0;
                    } else if (v < VLEK_LAAG) {
                        over = VLEK_LAAG - v;
                        set = 1;
                    } else {
                        continue;
                    }
                    int y = top(level, x, z);
                    p.set(x, y, z);
                    if (!level.getBlockState(p).is(Blocks.PINK_WOOL) || !level.getBiome(p).is(PASTELWOL_BIOMES)) {
                        continue;
                    }
                    double c = r.kleur().getValue(x / KLEUR_SCHAAL + 311.0, z / KLEUR_SCHAAL - 97.0);
                    int idx = (Math.min(2, Math.max(0, (int) Math.floor((c + 1.0) * 1.5))) + set) % 3;
                    int diepte = 1 + Math.min(2, (int) (over / 0.08));
                    for (int i = 0; i < diepte; i++) {
                        p.setY(y - i);
                        if (!level.getBlockState(p).is(Blocks.PINK_WOOL)) {
                            break;
                        }
                        level.setBlock(p, palet[idx], Block.UPDATE_CLIENTS);
                        iets = true;
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Soft gravel along the water (once per chunk, after the lakes and pools): the ground (1-2 deep) of a column within
     * 1-2 blocks of surface water (or saus) at the waterline becomes viltkiezel, and the bed of the shallow rim (one
     * block of water deep, next to the shore) too, in strips. The width wanders with a noise. Never over a hole (it falls).
     */
    public static class Viltkiezel extends Feature<NoneFeatureConfiguration> {
        public Viltkiezel() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            BlockState kiezel = OnderWolFeature.VILTKIEZEL.get().defaultBlockState();
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            // the tops of the chunk and a border of 2 (the water may be just across the chunk edge)
            int[][] hoogte = new int[20][20];
            boolean[][] nat = new boolean[20][20];
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean water = false;
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 20; j++) {
                    int x = x0 + i - 2, z = z0 + j - 2;
                    hoogte[i][j] = top(level, x, z);
                    nat[i][j] = bron(level.getBlockState(p.set(x, hoogte[i][j], z)));
                    water |= nat[i][j];
                }
            }
            if (!water) {
                return false;
            }
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int i = dx + 2, j = dz + 2, x = x0 + dx, z = z0 + dz, y = hoogte[i][j];
                    p.set(x, y, z);
                    if (!level.getBiome(p).is(KIEZEL_BIOMES)) {
                        continue;
                    }
                    if (nat[i][j]) {
                        // the shallow rim: one block of water on ground, next to the shore, in strips
                        p.setY(y - 1);
                        if (!level.getBlockState(p).is(GROND) || hash(zaad, x, z, 7) > 0.7) {
                            continue;
                        }
                        boolean oever = false;
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            int a = i + d.getStepX(), b = j + d.getStepZ();
                            oever |= !nat[a][b] && hoogte[a][b] >= y;
                        }
                        if (oever && draagt(level, p.below())) {
                            level.setBlock(p, kiezel, Block.UPDATE_CLIENTS);
                            iets = true;
                        }
                        continue;
                    }
                    if (!level.getBlockState(p).is(GROND)) {
                        continue;
                    }
                    int bereik = r.kiezel().getValue(x / 18.0, z / 18.0) > 0.1 ? 2 : 1;
                    boolean langsWater = false;
                    for (int a = -bereik; a <= bereik && !langsWater; a++) {
                        for (int b = -bereik; b <= bereik && !langsWater; b++) {
                            if (a * a + b * b > bereik * bereik + 1 || !nat[i + a][j + b]) {
                                continue;
                            }
                            int w = hoogte[i + a][j + b];
                            langsWater = y == w || y == w + 1 || y == w + 2 && a * a + b * b <= 1;
                        }
                    }
                    if (!langsWater) {
                        continue;
                    }
                    int diepte = hash(zaad, x, z, 3) < 0.5 && level.getBlockState(p.set(x, y - 1, z)).is(GROND) ? 2 : 1;
                    if (!draagt(level, p.set(x, y - diepte, z))) {
                        continue;
                    }
                    for (int k = 0; k < diepte; k++) {
                        level.setBlock(p.set(x, y - k, z), kiezel, Block.UPDATE_CLIENTS);
                    }
                    iets = true;
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A wolkei: a little round boulder (2-4 blocks) of viltsteen, pink wool or white wool with a few specks of another,
     * half sunk into the ground, with a pink mossy carpet (pink carpet, now and then glowing lichtmos) on top.
     */
    public static class Wolkei extends Feature<NoneFeatureConfiguration> {
        public Wolkei() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            if (!level.getBlockState(origin.below()).is(GROND) || !level.getBlockState(origin).isAir()
                    || BouwRuimte.touchesBuilding(level, origin.getX() - 3, origin.getY() - 3, origin.getZ() - 3, origin.getX() + 3, origin.getY() + 4, origin.getZ() + 3)) {
                return false;
            }
            BlockState vilt = OnderWolFeature.VILTSTEEN.get().defaultBlockState();
            BlockState[] soorten = {vilt, Blocks.PINK_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState()};
            int s = random.nextInt(5);
            BlockState hoofd = soorten[s < 3 ? 0 : s - 2];   // (mostly viltsteen)
            BlockState spikkel = hoofd == vilt ? (random.nextBoolean() ? soorten[1] : soorten[2]) : vilt;
            List<BlockPos> gezet = new ArrayList<>();
            BlockPos c = origin.below();
            int blobs = 2 + random.nextInt(2);
            for (int k = 0; k < blobs; k++) {
                int xr = random.nextInt(2), yr = random.nextInt(2), zr = random.nextInt(2);
                float tr = (xr + yr + zr) * 0.333f + 0.6f;
                for (BlockPos q : BlockPos.betweenClosed(c.offset(-xr, -yr, -zr), c.offset(xr, yr, zr))) {
                    if (q.distSqr(c) > tr * tr) {
                        continue;
                    }
                    BlockState oud = level.getBlockState(q);
                    if (!oud.getFluidState().isEmpty() || !(oud.isAir() || oud.canBeReplaced() || oud.is(GROND))) {
                        continue;
                    }
                    level.setBlock(q, random.nextFloat() < 0.18f ? spikkel : hoofd, Block.UPDATE_CLIENTS);
                    gezet.add(q.immutable());
                }
                c = c.offset(-1 + random.nextInt(3), random.nextInt(2) == 0 ? 0 : 1, -1 + random.nextInt(3));
                if (c.getY() > origin.getY() + 1) {
                    c = c.below();
                }
            }
            BlockState tapijt = Blocks.PINK_CARPET.defaultBlockState(), lichtmos = OnderWolFeature.LICHTMOS.get().defaultBlockState();
            for (BlockPos q : gezet) {
                BlockPos boven = q.above();
                if (level.getBlockState(boven).isAir() && random.nextFloat() < 0.5f) {
                    level.setBlock(boven, random.nextFloat() < 0.25f ? lichtmos : tapijt, Block.UPDATE_CLIENTS);
                }
            }
            return !gezet.isEmpty();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A vijvertje: a small oval pond (3-5 blocks radius) dug into flat ground, 1-2 deep, with a bed of viltkiezel, guh
     * water lilies on the water and reeds (the Bloesemmeertje's riet) and pink grass on the rim. Only on flat, dry,
     * natural ground away from buildings; it never spills.
     */
    public static class Vijvertje extends Feature<NoneFeatureConfiguration> {
        public Vijvertje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            int cx = origin.getX(), cz = origin.getZ(), y0 = top(level, cx, cz);
            double rx = 3 + random.nextInt(3) + random.nextDouble() * 0.5, rz = 3 + random.nextInt(3) + random.nextDouble() * 0.5;
            int R = (int) Math.ceil(Math.max(rx, rz)) + 1;
            if (BouwRuimte.touchesBuilding(level, cx - R, y0 - 3, cz - R, cx + R, y0 + 4, cz + R)) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            // check: water area (d <= 1) on ground at y0..y0+2, the rim (d <= ~1.4) dry ground at y0 or higher
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz));
                    if (d > 1.0 + 1.6 / Math.min(rx, rz)) {
                        continue;
                    }
                    int x = cx + dx, z = cz + dz, h = top(level, x, z);
                    BlockState s = level.getBlockState(p.set(x, h, z));
                    if (!s.is(GROND) || h < y0 || h > y0 + 2) {
                        return false;
                    }
                    if (d <= 1.0) {
                        int diep = d < 0.55 ? 2 : 1;
                        if (!draagt(level, p.set(x, y0 - diep, z)) || !level.getBlockState(p.set(x, y0 - diep, z)).getFluidState().isEmpty()) {
                            return false;
                        }
                    }
                }
            }
            BlockState water = Blocks.WATER.defaultBlockState(), kiezel = OnderWolFeature.VILTKIEZEL.get().defaultBlockState();
            BlockState lelie = blok("guh_waterlelie", Blocks.LILY_PAD), riet = blok("bloesemmeertje_riet", Blocks.TALL_GRASS);
            BlockState gras = blok("roze_gras", Blocks.SHORT_GRASS);
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz));
                    int x = cx + dx, z = cz + dz;
                    if (d <= 1.0) {
                        int diep = d < 0.55 ? 2 : 1;
                        for (int y = y0 + 3; y > y0; y--) {
                            level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                        for (int k = 0; k < diep; k++) {
                            level.setBlock(p.set(x, y0 - k, z), water, Block.UPDATE_CLIENTS);
                        }
                        level.setBlock(p.set(x, y0 - diep, z), kiezel, Block.UPDATE_CLIENTS);
                        if (random.nextFloat() < 0.14f) {
                            level.setBlock(p.set(x, y0 + 1, z), lelie, Block.UPDATE_CLIENTS);
                        }
                    } else if (d <= 1.0 + 1.6 / Math.min(rx, rz)) {
                        int h = top(level, x, z);
                        if (h == y0 && hash(level.getSeed(), x, z, 11) < 0.5) {
                            level.setBlock(p.set(x, h, z), kiezel, Block.UPDATE_CLIENTS);   // (a soft gravel lip here and there)
                        }
                        p.set(x, h + 1, z);
                        if (!level.getBlockState(p).isAir() || !draagt(level, p.below())) {
                            continue;
                        }
                        float f = random.nextFloat();
                        if (f < 0.28f && level.getBlockState(p.above()).isAir() && riet.getBlock() instanceof DoublePlantBlock) {
                            DoublePlantBlock.placeAt(level, riet, p.immutable(), Block.UPDATE_CLIENTS);
                        } else if (f < 0.55f) {
                            level.setBlock(p, gras, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A wolwatervalletje: on a hillside with a drop of at least 4 right next to it, a spring comes out of the cliff face
     * two blocks under the edge, falls down and lands in a small pool dug into the ground at the foot. The fluid is the
     * config's state (water, or kaas saus).
     */
    public static class Waterval extends Feature<BlockStateConfiguration> {
        public Waterval() {
            super(BlockStateConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            BlockState vloeistof = context.config().state;
            int x = origin.getX(), z = origin.getZ(), h = top(level, x, z);
            List<Direction> richtingen = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            net.minecraft.util.Util.shuffle(richtingen, random);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (Direction d : richtingen) {
                int fx = x + d.getStepX(), fz = z + d.getStepZ(), voet = top(level, fx, fz);
                if (voet > h - 4 || bron(level.getBlockState(p.set(fx, voet, fz)))) {
                    continue;
                }
                BlockPos bronPos = new BlockPos(x, h - 2, z);
                // the spring sits in the rock: ground above, below and on the three other sides, air in front
                if (!level.getBlockState(bronPos).is(GROND) || !level.getBlockState(bronPos.above()).is(GROND)
                        || !level.getBlockState(bronPos.below()).is(GROND) || !level.getBlockState(bronPos.relative(d)).isAir()) {
                    continue;
                }
                boolean dicht = true;
                for (Direction z2 : Direction.Plane.HORIZONTAL) {
                    if (z2 != d && !level.getBlockState(bronPos.relative(z2)).is(GROND)) {
                        dicht = false;
                    }
                }
                if (!dicht || BouwRuimte.touchesBuilding(level, x - 4, voet - 2, z - 4, x + 4, h + 1, z + 4)) {
                    continue;
                }
                // the pool at the foot: a little plus around the column one block further out, which includes the landing
                // column; every cell on ground at the foot's level, closed in by solid ground at that level
                BlockPos m = new BlockPos(fx + d.getStepX(), voet, fz + d.getStepZ());
                List<BlockPos> poel = new ArrayList<>(List.of(m, m.relative(d.getOpposite()), m.relative(d), m.relative(d.getClockWise()),
                        m.relative(d.getCounterClockWise())));
                boolean ok = true;
                for (BlockPos q : poel) {
                    ok &= top(level, q.getX(), q.getZ()) == voet && level.getBlockState(q).is(GROND) && draagt(level, q.below());
                }
                for (int k = 0; ok && k < poel.size(); k++) {
                    for (Direction z2 : Direction.Plane.HORIZONTAL) {
                        BlockPos n = poel.get(k).relative(z2);
                        ok &= poel.contains(n) || draagt(level, n);
                    }
                }
                if (!ok) {
                    continue;
                }
                BlockState stil = vloeistof.getFluidState().createLegacyBlock();
                for (BlockPos q : poel) {
                    level.setBlock(q, stil, Block.UPDATE_CLIENTS);
                }
                level.setBlock(bronPos, stil, Block.UPDATE_CLIENTS);
                level.scheduleTick(bronPos, stil.getFluidState().getType(), 0);
                return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A paddenstoelenkring: a fairy ring (radius 3-5) of glowing glinsterpaddenstoelen on flat open ground. In 30% of
     * the rings a wild guh stands in the middle (kept: a guh born at worldgen would otherwise vanish before anyone comes).
     */
    public static class Paddenstoelenkring extends Feature<NoneFeatureConfiguration> {
        public Paddenstoelenkring() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos midden = context.origin();
            if (!draagt(level, midden.below()) || !level.getBlockState(midden).canBeReplaced() || !level.getBlockState(midden).getFluidState().isEmpty()) {
                return false;
            }
            int r = 3 + random.nextInt(3);
            BlockState paddenstoel = OnderWolFeature.GLINSTERPADDENSTOEL.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int gezet = 0;
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(d - r) >= 0.5 || random.nextFloat() < 0.15f) {
                        continue;
                    }
                    // the ground near the middle's height (a gentle slope is fine)
                    for (int dy = 2; dy >= -2; dy--) {
                        p.set(midden.getX() + dx, midden.getY() + dy, midden.getZ() + dz);
                        BlockState s = level.getBlockState(p);
                        if ((s.isAir() || s.canBeReplaced() && !s.is(BlockTags.FLOWERS)) && s.getFluidState().isEmpty() && draagt(level, p.below())) {
                            level.setBlock(p, paddenstoel, Block.UPDATE_CLIENTS);
                            gezet++;
                            break;
                        }
                    }
                }
            }
            if (gezet > 0 && random.nextFloat() < 0.30f && level.getBlockState(midden.above()).isAir()) {
                if (!level.getBlockState(midden).isAir()) {
                    level.setBlock(midden, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                GuhEntity guh = ModEntities.GUH.get().create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                if (guh != null) {
                    guh.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5, random.nextFloat() * 360f, 0);
                    guh.finalizeSpawn(level, level.getCurrentDifficultyAt(midden), EntitySpawnReason.CHUNK_GENERATION, null);
                    if (!guh.hasSecretNote()) {
                        guh.setVariant(GuhEntity.wildeVariant(random, true));   // (a normal wild roll, but no ghost: it stays, also by day)
                    }
                    guh.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(guh);
                }
            }
            return gezet > 0;
        }
    }

    private OnderWolOppervlak() {
    }
}
