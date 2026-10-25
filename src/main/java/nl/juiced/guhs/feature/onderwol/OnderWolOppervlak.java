package nl.juiced.guhs.feature.onderwol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * 1.7.0 "Onder de Wol", ronde 27-30: a cheerful finish for the surface of the common Guhmension (the data:
 * tools/features/onderwol_oppervlak.py, which also writes the vanilla-typed pieces: the flower meadows, the fallen logs,
 * the little trees and bushes, the Bloemenvallei biome, the fluff in the air). The Java features here:
 * <ul>
 *   <li>{@link PastelWol} the quilt: big soft patches where the top 1-3 pink wool turns lila, white or peach;</li>
 *   <li>{@link Viltkiezel} the shores: soft gravel along the waterline, and (ronde 29) stretches of pink sand with little
 *       shells and of plain sand in between (all three kinds of beach side by side);</li>
 *   <li>{@link Wolkei} little round boulders of viltsteen or wool with a pink mossy carpet;</li>
 *   <li>{@link Vijvertje} a small pond with a soft gravel bed, lily pads and reeds;</li>
 *   <li>{@link Waterval} a spring in a cliff face that falls into a little pool (water, or kaas saus);</li>
 *   <li>{@link Paddenstoelenkring} a fairy ring of glinsterpaddenstoelen, now and then with a guh in the middle;</li>
 *   <li>ronde 29-30: {@link Eilandje} little islands with a tree in lakes and shallow seas, {@link Heg} short hedges,
 *       {@link Kaaslappendeken} the cheese quilt of the kaas biomes and {@link Kaasplasje} its saus puddles,
 *       {@link Hartjesmeertje} heart-shaped lakes, {@link Regenboogheuvels} pastel-striped hillsides and
 *       {@link BloemenvalleiGrond} the grassy floor of the Bloemenvallei.</li>
 * </ul>
 * New blocks (ronde 29-30): roze zand (falls like sand), a schelpje (a little shell on the beach) and the hartjeslelie (a
 * guh lily pad that lets little hearts float up). All deterministic from the world seed (and the feature's own
 * decoration random). New chunks only.
 */
public final class OnderWolOppervlak {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    // --- the blocks (ronde 29-30) --------------------------------------------------------------------------------------
    /** Pink sand of the roze strandjes: falls like sand, melts into glass like sand. */
    public static final DeferredBlock<ColoredFallingBlock> ROZE_ZAND = BLOCKS.registerBlock("roze_zand",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFFF4BCD0), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_PINK));
    /** A little pink shell lying on the beach (decoration; picks up in one tap). */
    public static final DeferredBlock<Schelpje> SCHELPJE = BLOCKS.registerBlock("schelpje", Schelpje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.CORAL_BLOCK)
                    .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    /** A guh lily pad of the hartjesmeertjes: now and then a little heart floats up from it. */
    public static final DeferredBlock<Hartjeslelie> HARTJESLELIE = BLOCKS.registerBlock("hartjeslelie", Hartjeslelie::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).mapColor(MapColor.COLOR_PINK));

    /** The blocks with a plain block item, in creative-tab order (the hartjeslelie has its own item: placed on water). */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(ROZE_ZAND, SCHELPJE);
    public static final DeferredItem<PlaceOnWaterBlockItem> HARTJESLELIE_ITEM = ITEMS.registerItem("hartjeslelie",
            p -> new PlaceOnWaterBlockItem(HARTJESLELIE.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    static {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    // --- the features --------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, PastelWol> PASTELWOL = FEATURES.register("onderwol_pastelwol", PastelWol::new);
    public static final DeferredHolder<Feature<?>, Viltkiezel> VILTKIEZEL = FEATURES.register("onderwol_viltkiezel", Viltkiezel::new);
    public static final DeferredHolder<Feature<?>, Wolkei> WOLKEI = FEATURES.register("onderwol_wolkei", Wolkei::new);
    public static final DeferredHolder<Feature<?>, Vijvertje> VIJVERTJE = FEATURES.register("onderwol_vijvertje", Vijvertje::new);
    public static final DeferredHolder<Feature<?>, Waterval> WATERVAL = FEATURES.register("onderwol_waterval", Waterval::new);
    public static final DeferredHolder<Feature<?>, Paddenstoelenkring> KRING = FEATURES.register("onderwol_paddenstoelenkring", Paddenstoelenkring::new);
    public static final DeferredHolder<Feature<?>, Eilandje> EILANDJE = FEATURES.register("onderwol_eilandje", Eilandje::new);
    public static final DeferredHolder<Feature<?>, Heg> HEG = FEATURES.register("onderwol_heg", Heg::new);
    public static final DeferredHolder<Feature<?>, Kaaslappendeken> KAASLAPPEN = FEATURES.register("onderwol_kaaslappendeken", Kaaslappendeken::new);
    public static final DeferredHolder<Feature<?>, Kaasplasje> KAASPLASJE = FEATURES.register("onderwol_kaasplasje", Kaasplasje::new);
    public static final DeferredHolder<Feature<?>, Hartjesmeertje> HARTJESMEERTJE = FEATURES.register("onderwol_hartjesmeertje", Hartjesmeertje::new);
    public static final DeferredHolder<Feature<?>, Regenboogheuvels> REGENBOOG = FEATURES.register("onderwol_regenboogheuvels", Regenboogheuvels::new);
    public static final DeferredHolder<Feature<?>, BloemenvalleiGrond> VALLEI_GROND = FEATURES.register("onderwol_bloemenvallei_grond", BloemenvalleiGrond::new);

    /** The natural ground of the surface these features may change or build on (wool, kaasknabbels, grass...). */
    public static final TagKey<Block> GROND = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/oppervlakgrond"));
    /** The biomes where the quilt and the soft gravel are worked out column by column (so biome borders stay clean). */
    public static final TagKey<Biome> PASTELWOL_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/pastelwol"));
    public static final TagKey<Biome> KIEZEL_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/viltkiezel"));
    /** Ronde 29: the biomes whose shores also get stretches of pink sand and plain sand (the others: only viltkiezel). */
    public static final TagKey<Biome> STRAND_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/roze_strand"));
    /** Ronde 30: where the rainbow hills may be, and where the cheese quilt lies. */
    public static final TagKey<Biome> REGENBOOG_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/regenboogheuvels"));
    public static final TagKey<Biome> KAAS_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/kaaslappendeken"));
    /** Ronde 30: the small rare flower biome. */
    public static final ResourceKey<Biome> BLOEMENVALLEI = ResourceKey.create(Registries.BIOME, Guhs.id("bloemenvallei"));

    /** The share of the world the quilt patches cover is set by these thresholds of a simplex noise (range about -1..1). */
    private static final double VLEK_SCHAAL = 38.0, VLEK_HOOG = 0.50, VLEK_LAAG = -0.56, KLEUR_SCHAAL = 75.0;
    /** The beaches: above ROZE pink sand, below ZAND plain sand, in between the soft gravel (scale in blocks). */
    private static final double STRAND_SCHAAL = 44.0, STRAND_ROZE = 0.18, STRAND_ZAND = -0.42;
    /** The rainbow hills: a rare region of a big slow noise. */
    private static final double REGENBOOG_SCHAAL = 320.0, REGENBOOG_DREMPEL = 0.60;

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        // the Superkompas tab "Biomes" (section Guhmension): the Bloemenvallei
        nl.juiced.guhs.feature.bio.BiomeLijst.biome("guhmension", "bloemenvallei");
        nl.juiced.guhs.feature.bio.kompas.BiomeIconen.zet("bloemenvallei", "guhs:knabbelroos");
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the seed's noises (pure functions of the seed, so neighbouring chunks agree)
    // ------------------------------------------------------------------------------------------------------------------
    private record Ruis(long zaad, SimplexNoise vlek, SimplexNoise kleur, SimplexNoise kiezel, SimplexNoise strand, SimplexNoise regen,
                        SimplexNoise golf, SimplexNoise kaas1, SimplexNoise kaas2, BlockState[] banden) {
    }

    private static volatile Ruis ruis;

    private static Ruis ruis(long zaad) {
        Ruis r = ruis;
        if (r == null || r.zaad() != zaad) {
            r = new Ruis(zaad, new SimplexNoise(new LegacyRandomSource(zaad ^ 0x9A57_E1L)), new SimplexNoise(new LegacyRandomSource(zaad ^ 0x0C01_0E25L)),
                    new SimplexNoise(new LegacyRandomSource(zaad ^ 0x1C1E_2E1L)), new SimplexNoise(new LegacyRandomSource(zaad ^ 0x57A4_D0L)),
                    new SimplexNoise(new LegacyRandomSource(zaad ^ 0x2E6E_B00L)), new SimplexNoise(new LegacyRandomSource(zaad ^ 0x601F5L)),
                    new SimplexNoise(new LegacyRandomSource(zaad ^ 0x4AA51L)), new SimplexNoise(new LegacyRandomSource(zaad ^ 0x4AA52L)),
                    banden(zaad));
            ruis = r;
        }
        return r;
    }

    /**
     * The stripes of the rainbow hills, one entry per height (wrapping every 64): runs of 1-3 blocks of the soft palette,
     * never the same colour twice in a row; the base tones (pink wool, peach) come more often than the light blue.
     */
    private static BlockState[] banden(long zaad) {
        BlockState[] palet = {Blocks.PINK_WOOL.defaultBlockState(), Blocks.WHITE_TERRACOTTA.defaultBlockState(), Blocks.PINK_TERRACOTTA.defaultBlockState(),
                Blocks.WHITE_WOOL.defaultBlockState(), Blocks.MAGENTA_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_BLUE_WOOL.defaultBlockState()};
        int[] gewicht = {3, 3, 2, 2, 1, 1};
        RandomSource random = new LegacyRandomSource(zaad ^ 0x8A2D_E7L);
        BlockState[] banden = new BlockState[64];
        int vorige = -1;
        for (int y = 0; y < banden.length; ) {
            int k;
            do {
                int t = random.nextInt(12);
                k = 0;
                while (t >= gewicht[k]) {
                    t -= gewicht[k];
                    k++;
                }
            } while (k == vorige);
            vorige = k;
            int dik = 1 + random.nextInt(3);
            for (int i = 0; i < dik && y < banden.length; i++) {
                banden[y++] = palet[k];
            }
        }
        return banden;
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

    /** The top of the ground (plants and leaves don't count). */
    static int grond(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
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

    /** Leaves that never fall (they are not connected to a tree trunk the game knows about). */
    static BlockState blad(String id, Block standIn) {
        BlockState s = blok(id, standIn);
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    /** Places a plant only where it may grow and only into air. */
    static boolean plant(WorldGenLevel level, BlockPos pos, BlockState plant) {
        if (!level.getBlockState(pos).isAir() || !plant.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, plant, Block.UPDATE_CLIENTS);
        return true;
    }

    /** The guh flowers that grow on any sturdy ground. */
    static BlockState[] guhBloemen() {
        return new BlockState[]{blok("roze_guhbloem", Blocks.PINK_TULIP), blok("knabbelroos", Blocks.ALLIUM), blok("guhoortjes", Blocks.AZURE_BLUET),
                blok("kaasbloem", Blocks.DANDELION)};
    }

    /**
     * A little tree (on islands): a trunk of 3-4 and a round crown of persistent leaves (two wide layers, a small top),
     * only into air or plants.
     */
    static void boompje(WorldGenLevel level, BlockPos voet, RandomSource random, BlockState stam, BlockState blad) {
        int hoog = 3 + random.nextInt(2);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < hoog; i++) {
            BlockState s = level.getBlockState(p.set(voet.getX(), voet.getY() + i, voet.getZ()));
            if (!s.isAir() && !s.canBeReplaced()) {
                return;
            }
            level.setBlock(p, stam, Block.UPDATE_CLIENTS);
        }
        for (int dy = -2; dy <= 1; dy++) {
            int r = dy <= -1 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean hoek = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (hoek && (r == 1 || random.nextFloat() < 0.6f) || dy < 1 && dx == 0 && dz == 0) {
                        continue;
                    }
                    p.set(voet.getX() + dx, voet.getY() + hoog + dy, voet.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty()) {
                        level.setBlock(p, blad, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
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
     * The shores (once per chunk, after the lakes and pools): the ground (1-2 deep) of a column within 1-3 blocks of
     * surface water (or saus) at the waterline changes, and the bed of the shallow rim (one block of water deep, next to
     * the shore) too, in strips. Which beach a stretch of shore gets wanders with a big noise (ronde 29: "ze moeten er
     * beide zijn"): mostly soft viltkiezel, here and there a stretch of pink sand (wider, with little shells) or of plain
     * sand. Pink and plain sand only in the {@link #STRAND_BIOMES}. Never over a hole (it falls).
     */
    public static class Viltkiezel extends Feature<NoneFeatureConfiguration> {
        private static final int RAND = 3;

        public Viltkiezel() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            BlockState kiezel = OnderWolFeature.VILTKIEZEL.get().defaultBlockState(), roze = ROZE_ZAND.get().defaultBlockState();
            BlockState zand = Blocks.SAND.defaultBlockState(), schelp = SCHELPJE.get().defaultBlockState();
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15, n = 16 + 2 * RAND;
            // the tops of the chunk and a border of 3 (the water may be just across the chunk edge)
            int[][] hoogte = new int[n][n];
            boolean[][] nat = new boolean[n][n];
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean water = false;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = x0 + i - RAND, z = z0 + j - RAND;
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
                    int i = dx + RAND, j = dz + RAND, x = x0 + dx, z = z0 + dz, y = hoogte[i][j];
                    p.set(x, y, z);
                    var biome = level.getBiome(p);
                    if (!biome.is(KIEZEL_BIOMES)) {
                        continue;
                    }
                    // which beach: 0 viltkiezel, 1 pink sand, 2 plain sand
                    int soort = 0;
                    if (biome.is(STRAND_BIOMES)) {
                        double s = r.strand().getValue(x / STRAND_SCHAAL, z / STRAND_SCHAAL) + (hash(zaad, x, z, 13) - 0.5) * 0.06;
                        soort = s > STRAND_ROZE ? 1 : s < STRAND_ZAND ? 2 : 0;
                    }
                    BlockState bodem = soort == 1 ? roze : soort == 2 ? zand : kiezel;
                    if (nat[i][j]) {
                        // the shallow rim: one block of water on ground, next to the shore, in strips
                        p.setY(y - 1);
                        if (!level.getBlockState(p).is(GROND) || hash(zaad, x, z, 7) > (soort == 0 ? 0.7 : 0.9)) {
                            continue;
                        }
                        boolean oever = false;
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            int a = i + d.getStepX(), b = j + d.getStepZ();
                            oever |= !nat[a][b] && hoogte[a][b] >= y;
                        }
                        if (oever && draagt(level, p.below())) {
                            level.setBlock(p, bodem, Block.UPDATE_CLIENTS);
                            iets = true;
                        }
                        continue;
                    }
                    if (!level.getBlockState(p).is(GROND)) {
                        continue;
                    }
                    double k = r.kiezel().getValue(x / 18.0, z / 18.0);
                    int bereik = soort == 0 ? (k > 0.1 ? 2 : 1) : (k > -0.2 ? 3 : 2);   // (a sandy beach is wider)
                    boolean langsWater = false;
                    for (int a = -bereik; a <= bereik && !langsWater; a++) {
                        for (int b = -bereik; b <= bereik && !langsWater; b++) {
                            if (a * a + b * b > bereik * bereik + 1 || !nat[i + a][j + b]) {
                                continue;
                            }
                            int w = hoogte[i + a][j + b];
                            langsWater = y == w || y == w + 1 || y == w + 2 && (soort != 0 || a * a + b * b <= 1);
                        }
                    }
                    if (!langsWater) {
                        continue;
                    }
                    boolean dubbel = soort != 0 || hash(zaad, x, z, 3) < 0.5;
                    int diepte = dubbel && level.getBlockState(p.set(x, y - 1, z)).is(GROND) ? 2 : 1;
                    if (!draagt(level, p.set(x, y - diepte, z))) {
                        continue;
                    }
                    for (int q = 0; q < diepte; q++) {
                        level.setBlock(p.set(x, y - q, z), bodem, Block.UPDATE_CLIENTS);
                    }
                    iets = true;
                    // a little shell here and there (mostly on the pink sand)
                    if (soort != 0 && hash(zaad, x, z, 14) < (soort == 1 ? 0.06 : 0.025)) {
                        plant(level, p.set(x, y + 1, z), schelp);
                    }
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

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * An eilandje (ronde 29): a little round island (radius 2-3) in a lake or a shallow sea, with a pink sand rim, a soft
     * hill of wool and grass in the middle, a little blossom or fluff tree, a few flowers and a shell. Only where the
     * water is shallow (1-4 deep over solid ground) and the island has at least two blocks of water around it on every
     * side, so it only comes in the bigger lakes and just off the coast.
     */
    public static class Eilandje extends Feature<NoneFeatureConfiguration> {
        public Eilandje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        /** The ground under the water of this column (or Integer.MIN_VALUE when not 1-4 deep over solid ground). */
        private static int bodem(WorldGenLevel level, int x, int z, int y0, BlockPos.MutableBlockPos p) {
            if (top(level, x, z) != y0) {
                return Integer.MIN_VALUE;
            }
            for (int d = 0; d < 5; d++) {
                BlockState s = level.getBlockState(p.set(x, y0 - d, z));
                if (!bron(s) || !s.getFluidState().is(FluidTags.WATER)) {
                    return d >= 1 && draagt(level, p) ? y0 - d : Integer.MIN_VALUE;
                }
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            int cx = context.origin().getX(), cz = context.origin().getZ(), y0 = top(level, cx, cz);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            if (bodem(level, cx, cz, y0, p) == Integer.MIN_VALUE || bodem(level, cx, cz, y0, p) < y0 - 3) {
                return false;
            }
            double rx = 2 + random.nextInt(2) + random.nextDouble() * 0.6, rz = 2 + random.nextInt(2) + random.nextDouble() * 0.6;
            double rand = 1.0 + 2.5 / Math.min(rx, rz);
            int R = (int) Math.ceil(Math.max(rx, rz) * rand) + 1;
            int[][] bed = new int[2 * R + 1][2 * R + 1];
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz));
                    if (d > rand) {
                        continue;
                    }
                    bed[dx + R][dz + R] = bodem(level, cx + dx, cz + dz, y0, p);
                    if (bed[dx + R][dz + R] == Integer.MIN_VALUE) {
                        return false;   // (too deep, ground, or the shore is too near)
                    }
                }
            }
            if (BouwRuimte.touchesBuilding(level, cx - R, y0 - 5, cz - R, cx + R, y0 + 8, cz + R)) {
                return false;
            }
            long zaad = level.getSeed();
            BlockState wol = Blocks.PINK_WOOL.defaultBlockState(), zand = ROZE_ZAND.get().defaultBlockState();
            BlockState gras = Blocks.GRASS_BLOCK.defaultBlockState(), schelp = SCHELPJE.get().defaultBlockState();
            BlockState[] bloemen = guhBloemen();
            List<BlockPos> toppen = new ArrayList<>();
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz)) + (hash(zaad, cx + dx, cz + dz, 21) - 0.5) * 0.2;
                    if (d > 1.0) {
                        continue;
                    }
                    int x = cx + dx, z = cz + dz, h = y0 + (d < 0.45 ? 2 : 1);
                    for (int y = bed[dx + R][dz + R] + 1; y < h; y++) {
                        level.setBlock(p.set(x, y, z), wol, Block.UPDATE_CLIENTS);
                    }
                    level.setBlock(p.set(x, h, z), d > 0.68 ? zand : d < 0.45 ? gras : wol, Block.UPDATE_CLIENTS);
                    toppen.add(new BlockPos(x, h + 1, z));
                }
            }
            if (toppen.isEmpty()) {
                return false;
            }
            // the tree in the middle (on the hill), then flowers on the wool and grass and a shell on the sand
            boolean bloesem = random.nextBoolean();
            BlockState stam = bloesem ? blok("guhbloesem_log", Blocks.CHERRY_LOG) : blok("pluizenboom_stam", Blocks.BIRCH_LOG);
            BlockState blad = bloesem ? blad("guhbloesem_leaves", Blocks.CHERRY_LEAVES) : blad("pluizenboom_bladeren", Blocks.CHERRY_LEAVES);
            BlockPos voet = new BlockPos(cx, top(level, cx, cz) + 1, cz);
            boompje(level, voet, random, stam, blad);
            int schelpen = 0;
            for (BlockPos q : toppen) {
                BlockState onder = level.getBlockState(q.below());
                if (onder.is(zand.getBlock())) {
                    if (schelpen < 2 && random.nextFloat() < 0.15f && plant(level, q, schelp)) {
                        schelpen++;
                    }
                } else if (random.nextFloat() < 0.3f) {
                    plant(level, q, bloemen[random.nextInt(bloemen.length)]);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A heggetje (ronde 29): a short row (3-7) of soft leaves along the ground, now and then with a second layer in the
     * middle and a flower at its ends. Mostly pluizenboom leaves with a few blossom leaves (like little flowers in it).
     */
    public static class Heg extends Feature<NoneFeatureConfiguration> {
        public Heg() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            Direction richting = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int lengte = 3 + random.nextInt(5);
            List<BlockPos> plekken = new ArrayList<>();
            int vorige = origin.getY();
            for (int i = 0; i < lengte; i++) {
                int x = origin.getX() + richting.getStepX() * i, z = origin.getZ() + richting.getStepZ() * i, h = grond(level, x, z);
                BlockPos q = new BlockPos(x, h + 1, z);
                BlockState s = level.getBlockState(q);
                if (Math.abs(q.getY() - vorige) > 1 || !(s.isAir() || s.canBeReplaced()) || !s.getFluidState().isEmpty()
                        || !level.getBlockState(q.below()).is(GROND) || !draagt(level, q.below())) {
                    break;
                }
                plekken.add(q);
                vorige = q.getY();
            }
            if (plekken.size() < 3) {
                return false;
            }
            BlockPos a = plekken.getFirst(), b = plekken.getLast();
            if (BouwRuimte.touchesBuilding(level, Math.min(a.getX(), b.getX()) - 1, a.getY() - 2, Math.min(a.getZ(), b.getZ()) - 1,
                    Math.max(a.getX(), b.getX()) + 1, a.getY() + 3, Math.max(a.getZ(), b.getZ()) + 1)) {
                return false;
            }
            BlockState pluis = blad("pluizenboom_bladeren", Blocks.CHERRY_LEAVES), bloesem = blad("guhbloesem_leaves", Blocks.CHERRY_LEAVES);
            for (int i = 0; i < plekken.size(); i++) {
                BlockPos q = plekken.get(i);
                level.setBlock(q, random.nextFloat() < 0.28f ? bloesem : pluis, Block.UPDATE_CLIENTS);
                if (i > 0 && i < plekken.size() - 1 && random.nextFloat() < 0.22f && level.getBlockState(q.above()).isAir()) {
                    level.setBlock(q.above(), random.nextFloat() < 0.4f ? bloesem : pluis, Block.UPDATE_CLIENTS);
                }
            }
            BlockState[] bloemen = guhBloemen();
            for (BlockPos eind : List.of(a.relative(richting.getOpposite()), b.relative(richting))) {
                if (random.nextFloat() < 0.6f) {
                    BlockPos q = new BlockPos(eind.getX(), grond(level, eind.getX(), eind.getZ()) + 1, eind.getZ());
                    plant(level, q, bloemen[random.nextInt(bloemen.length)]);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The kaaslappendeken (ronde 29, once per chunk): the kaasknabbel ground of the kaas biomes stays the base, with
     * stitched patches of gatenkaas and of kaasmos (with a kaasmos carpet and a kaasbloem here and there) on top.
     */
    public static class Kaaslappendeken extends Feature<NoneFeatureConfiguration> {
        public Kaaslappendeken() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            BlockState gatenkaas = blok("gatenkaas", Blocks.YELLOW_TERRACOTTA), kaasmos = blok("kaasmos", Blocks.MOSS_BLOCK);
            BlockState tapijt = blok("kaasmos_tapijt", Blocks.MOSS_CARPET), bloem = blok("kaasbloem", Blocks.DANDELION);
            Block knabbels = Bio.blok("block_of_kaasknabbels", Blocks.YELLOW_WOOL);
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    double rafel = (hash(zaad, x, z, 31) - 0.5) * 0.08;
                    double a = r.kaas1().getValue(x / 17.0, z / 17.0) + rafel, b = r.kaas2().getValue(x / 13.0 + 50.0, z / 13.0 - 20.0) + rafel;
                    BlockState lap = a > 0.46 ? gatenkaas : b > 0.50 ? kaasmos : null;
                    if (lap == null) {
                        continue;
                    }
                    int y = top(level, x, z);
                    p.set(x, y, z);
                    BlockState oud = level.getBlockState(p);
                    if (!(oud.is(knabbels) || oud.is(Blocks.PINK_WOOL)) || !level.getBiome(p).is(KAAS_BIOMES)) {
                        continue;
                    }
                    level.setBlock(p, lap, Block.UPDATE_CLIENTS);
                    if (lap == gatenkaas && a > 0.56 && level.getBlockState(p.set(x, y - 1, z)).is(knabbels)) {
                        level.setBlock(p, lap, Block.UPDATE_CLIENTS);   // (deeper in the middle of a patch)
                    }
                    iets = true;
                    if (lap == kaasmos) {
                        double kans = hash(zaad, x, z, 32);
                        p.set(x, y + 1, z);
                        if (kans < 0.16) {
                            plant(level, p, tapijt);
                        } else if (kans < 0.24) {
                            plant(level, p, bloem);
                        }
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A kaasplasje (ronde 29): a little puddle (3-8 cells) of kaas saus in a dip one block deep, closed in by solid
     * ground at its level, with a kaasmos rim here and there.
     */
    public static class Kaasplasje extends Feature<NoneFeatureConfiguration> {
        public Kaasplasje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            int cx = context.origin().getX(), cz = context.origin().getZ(), y0 = top(level, cx, cz);
            BlockPos midden = new BlockPos(cx, y0, cz);
            List<BlockPos> plas = new ArrayList<>(List.of(midden));
            Set<BlockPos> in = new HashSet<>(plas);
            int doel = 3 + random.nextInt(6);
            for (int poging = 0; poging < 30 && plas.size() < doel; poging++) {
                BlockPos n = plas.get(random.nextInt(plas.size())).relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
                if (Math.abs(n.getX() - cx) <= 2 && Math.abs(n.getZ() - cz) <= 2 && in.add(n)) {
                    plas.add(n);
                }
            }
            for (BlockPos q : plas) {
                if (top(level, q.getX(), q.getZ()) != y0 || !level.getBlockState(q).is(GROND) || !draagt(level, q.below())) {
                    return false;
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = q.relative(d);
                    if (!in.contains(n) && (top(level, n.getX(), n.getZ()) < y0 || !draagt(level, n))) {
                        return false;
                    }
                }
            }
            if (BouwRuimte.touchesBuilding(level, cx - 3, y0 - 2, cz - 3, cx + 3, y0 + 3, cz + 3)) {
                return false;
            }
            BlockState saus = blok("kaas_saus", Blocks.WATER), kaasmos = blok("kaasmos", Blocks.MOSS_BLOCK);
            Block knabbels = Bio.blok("block_of_kaasknabbels", Blocks.YELLOW_WOOL);
            for (BlockPos q : plas) {
                level.setBlock(q, saus, Block.UPDATE_CLIENTS);
            }
            for (BlockPos q : plas) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = q.relative(d);
                    if (!in.contains(n) && random.nextFloat() < 0.3f && level.getBlockState(n).is(knabbels)
                            && top(level, n.getX(), n.getZ()) == y0) {
                        level.setBlock(n, kaasmos, Block.UPDATE_CLIENTS);
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A hartjesmeertje (ronde 30): a small lake shaped like a heart seen from above (11-15 blocks), 1-2 deep with a pink
     * sand bed, a crisp rim of flowers all around it, and lily pads of which a few are hartjeslelies (little hearts float
     * up from them). Like the vijvertje: only on flat, dry, natural ground away from buildings; it never spills.
     */
    public static class Hartjesmeertje extends Feature<NoneFeatureConfiguration> {
        public Hartjesmeertje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        /** The classic heart curve (lobes at +v, the point at v = -1). */
        static boolean inHart(double u, double v) {
            double a = u * u + v * v - 1;
            return a * a * a - u * u * v * v * v <= 0;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            int cx = context.origin().getX(), cz = context.origin().getZ(), y0 = top(level, cx, cz);
            double s = 5.0 + random.nextDouble() * 1.5;   // (2.28 s wide: 11-15 blocks)
            int draai = random.nextInt(4);
            int R = (int) Math.ceil(1.3 * s) + 1, n = 2 * R + 1;
            // 0 nothing, 1 water, 2 deep water, 3 rim
            int[][] soort = new int[n][n];
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    int a = switch (draai) {
                        case 0 -> dx;
                        case 1 -> dz;
                        case 2 -> -dx;
                        default -> -dz;
                    };
                    int b = switch (draai) {
                        case 0 -> dz;
                        case 1 -> -dx;
                        case 2 -> -dz;
                        default -> dx;
                    };
                    double u = a / s, v = -b / s + 0.12;
                    soort[dx + R][dz + R] = inHart(u / 0.55, v / 0.55) ? 2 : inHart(u, v) ? 1 : 0;
                }
            }
            for (int i = 1; i < n - 1; i++) {
                for (int j = 1; j < n - 1; j++) {
                    if (soort[i][j] != 0) {
                        continue;
                    }
                    for (int a = -1; a <= 1 && soort[i][j] == 0; a++) {
                        for (int b = -1; b <= 1; b++) {
                            if (soort[i + a][j + b] == 1 || soort[i + a][j + b] == 2) {
                                soort[i][j] = 3;
                                break;
                            }
                        }
                    }
                }
            }
            if (BouwRuimte.touchesBuilding(level, cx - R, y0 - 3, cz - R, cx + R, y0 + 4, cz + R)) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (soort[i][j] == 0) {
                        continue;
                    }
                    int x = cx + i - R, z = cz + j - R, h = top(level, x, z);
                    if (!level.getBlockState(p.set(x, h, z)).is(GROND) || h < y0 || h > y0 + 2) {
                        return false;
                    }
                    if (soort[i][j] != 3) {
                        int diep = soort[i][j] == 2 ? 2 : 1;
                        if (!draagt(level, p.set(x, y0 - diep, z))) {
                            return false;
                        }
                    }
                }
            }
            BlockState water = Blocks.WATER.defaultBlockState(), zand = ROZE_ZAND.get().defaultBlockState();
            BlockState lelie = blok("guh_waterlelie", Blocks.LILY_PAD), hartje = HARTJESLELIE.get().defaultBlockState();
            BlockState[] rand = {blok("roze_guhbloem", Blocks.PINK_TULIP), blok("knabbelroos", Blocks.ALLIUM), blok("roze_guhbloem", Blocks.PINK_TULIP),
                    Blocks.PINK_TULIP.defaultBlockState(), blok("guhoortjes", Blocks.AZURE_BLUET)};
            int hartjes = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = cx + i - R, z = cz + j - R;
                    if (soort[i][j] == 1 || soort[i][j] == 2) {
                        int diep = soort[i][j] == 2 ? 2 : 1;
                        for (int y = y0 + 3; y > y0; y--) {
                            level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                        for (int k = 0; k < diep; k++) {
                            level.setBlock(p.set(x, y0 - k, z), water, Block.UPDATE_CLIENTS);
                        }
                        level.setBlock(p.set(x, y0 - diep, z), zand, Block.UPDATE_CLIENTS);
                        float f = random.nextFloat();
                        if (f < 0.07f && hartjes < 6) {
                            level.setBlock(p.set(x, y0 + 1, z), hartje, Block.UPDATE_CLIENTS);
                            hartjes++;
                        } else if (f < 0.13f) {
                            level.setBlock(p.set(x, y0 + 1, z), lelie, Block.UPDATE_CLIENTS);
                        }
                    } else if (soort[i][j] == 3) {
                        int h = top(level, x, z);
                        p.set(x, h + 1, z);
                        BlockState b = rand[(int) (hash(level.getSeed(), x, z, 41) * rand.length)];
                        if (random.nextFloat() < 0.92f) {
                            if (!level.getBlockState(p).isAir()) {
                                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                            if (!plant(level, p, b)) {
                                plant(level, p, rand[0]);   // (a vanilla tulip needs grass; the guh flower grows anywhere)
                            }
                        }
                    }
                }
            }
            if (hartjes == 0) {
                level.setBlock(new BlockPos(cx, y0 + 1, cz), hartje, Block.UPDATE_CLIENTS);
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The regenboogheuvels (ronde 30, once per chunk): inside a rare region of a big slow noise, the exposed hillsides
     * (every block of a column that has open air beside it, where the column is at least 2 higher than a neighbour) show
     * soft pastel stripes by height, like gentle badlands: pink, peach, magenta and white terracotta with pink, white
     * and light blue wool. The stripes wave a little (another noise) and the region's edge is dithered. Flat ground and
     * the hill tops stay as they are.
     */
    public static class Regenboogheuvels extends Feature<NoneFeatureConfiguration> {
        public Regenboogheuvels() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            // quick check: is any corner (or the middle) near the region at all?
            boolean dichtbij = false;
            for (int[] c : new int[][]{{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}}) {
                dichtbij |= r.regen().getValue((x0 + c[0]) / REGENBOOG_SCHAAL, (z0 + c[1]) / REGENBOOG_SCHAAL) > REGENBOOG_DREMPEL - 0.05;
            }
            if (!dichtbij) {
                return false;
            }
            int[][] hoogte = new int[18][18];
            for (int i = 0; i < 18; i++) {
                for (int j = 0; j < 18; j++) {
                    hoogte[i][j] = grond(level, x0 + i - 1, z0 + j - 1);
                }
            }
            BlockState[] banden = r.banden();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz, i = dx + 1, j = dz + 1, h = hoogte[i][j];
                    double v = r.regen().getValue(x / REGENBOOG_SCHAAL, z / REGENBOOG_SCHAAL) + (hash(zaad, x, z, 51) - 0.5) * 0.04;
                    if (v < REGENBOOG_DREMPEL) {
                        continue;
                    }
                    int laag = Math.min(Math.min(hoogte[i - 1][j], hoogte[i + 1][j]), Math.min(hoogte[i][j - 1], hoogte[i][j + 1]));
                    if (h - laag < 2 || !level.getBiome(p.set(x, h, z)).is(REGENBOOG_BIOMES)) {
                        continue;
                    }
                    int golf = (int) Math.round(r.golf().getValue(x / 52.0, z / 52.0) * 3.0);
                    for (int y = h; y > laag && y > h - 14; y--) {
                        boolean open = hoogte[i - 1][j] < y || hoogte[i + 1][j] < y || hoogte[i][j - 1] < y || hoogte[i][j + 1] < y;
                        if (!open) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (!s.is(GROND) || s.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                            continue;
                        }
                        level.setBlock(p, banden[Math.floorMod(y + golf, banden.length)], Block.UPDATE_CLIENTS);
                        iets = true;
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The floor of the Bloemenvallei (ronde 30, once per chunk): the wool on top turns into grass (pink, from the
     * biome's colour) so all the flowers, guh and vanilla, can grow there; a few soft patches of wool stay.
     */
    public static class BloemenvalleiGrond extends Feature<NoneFeatureConfiguration> {
        public BloemenvalleiGrond() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            Ruis r = ruis(zaad);
            BlockState gras = Blocks.GRASS_BLOCK.defaultBlockState();
            Block knabbels = Bio.blok("block_of_kaasknabbels", Blocks.YELLOW_WOOL);
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz, y = top(level, x, z);
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!(s.is(Blocks.PINK_WOOL) || s.is(Blocks.MAGENTA_WOOL) || s.is(Blocks.WHITE_WOOL) || s.is(Blocks.WHITE_TERRACOTTA)
                            || s.is(Blocks.PINK_CONCRETE_POWDER) || s.is(knabbels)) || !level.getBiome(p).is(BLOEMENVALLEI)) {
                        continue;
                    }
                    if (r.vlek().getValue(x / 21.0 + 77.0, z / 21.0 - 13.0) + (hash(zaad, x, z, 61) - 0.5) * 0.1 < -0.62) {
                        continue;   // (a soft wool patch stays)
                    }
                    level.setBlock(p, gras, Block.UPDATE_CLIENTS);
                    iets = true;
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the blocks
    // ------------------------------------------------------------------------------------------------------------------
    /** A schelpje: a little flat shell on sturdy ground (sand, wool...). */
    public static class Schelpje extends VegetationBlock {
        public static final MapCodec<Schelpje> CODEC = simpleCodec(Schelpje::new);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 2, 13);

        public Schelpje(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends VegetationBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.isFaceSturdy(level, pos, Direction.UP);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE.move(state.getOffset(pos));
        }
    }

    /** A hartjeslelie: a guh lily pad from which now and then a little pink heart floats up (client only). */
    public static class Hartjeslelie extends LilyPadBlock {
        public static final MapCodec<Hartjeslelie> CODEC = simpleCodec(Hartjeslelie::new);

        public Hartjeslelie(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MapCodec<LilyPadBlock> codec() {
            return (MapCodec<LilyPadBlock>) (MapCodec<?>) CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(14) == 0) {
                level.addParticle(nl.juiced.guhs.feature.theehuis.TheehuisFeature.GEZELLIG_HARTJE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                        pos.getY() + 0.25, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
            }
        }
    }

    private OnderWolOppervlak() {
    }
}
