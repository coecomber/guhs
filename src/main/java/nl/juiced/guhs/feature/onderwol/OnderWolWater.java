package nl.juiced.guhs.feature.onderwol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * 1.7.0 "Onder de Wol", ronde 29: water in the Guhmension, rarer than in the overworld and cute (the data:
 * tools/features/onderwol_water.py, which also writes the vanilla-typed cave springs). The pieces:
 * <ul>
 *   <li>{@link Beekje} the brooks: a little spring (a {@link Bronnetje}) from which a shallow brook winds downhill through
 *       the terrain, with now and then a step of one or two blocks, until it joins other water or ends in a little pond
 *       (sometimes with a second bronnetje). Two kinds: the kronkelbeekje (low ground, 1-3 wide, lazy bends, lily pads)
 *       and the bronbeekje (from a spring on a hill, narrow, more steps). Viltkiezel on the bed and the banks, reeds,
 *       roze gras and glinsterriet along it;</li>
 *   <li>{@link Grotmeertje} still ponds sunk into the low floors of the caves (y 14-60), with a glowing rim of lichtmos
 *       and glinsterpaddenstoelen;</li>
 *   <li>(the cave waterfalls are vanilla springs, written by the python.)</li>
 * </ul>
 * Both features first plan every block and check that the water stays where it is put (no open side, a solid bed, no
 * spill into caves or air, no buildings near), and only then place anything: a plan that does not fit is dropped whole.
 * Everything is deterministic from the world seed and the decoration random. New chunks only.
 */
public final class OnderWolWater {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    // --- blocks ----------------------------------------------------------------------------------------------------------
    /** A bubbling spring stone: at the head of every brook (waterlogged), bubbles and pink glitter come up from it. */
    public static final DeferredBlock<Bronnetje> BRONNETJE = BLOCKS.registerBlock("bronnetje", Bronnetje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f, 4.0f).sound(SoundType.WOOL)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 5));
    /** A slender pink reed along the brooks that glitters softly. */
    public static final DeferredBlock<Glinsterriet> GLINSTERRIET = BLOCKS.registerBlock("glinsterriet", Glinsterriet::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().replaceable().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY).lightLevel(s -> 3));

    /** The blocks of this class with an item, in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(BRONNETJE, GLINSTERRIET);
    static {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            ITEMS.registerItem(block.getId().getPath(), p -> new LoreBlockItem(block.get(), p), p -> p.useBlockDescriptionPrefix());
        }
    }

    // --- features --------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, Beekje> KRONKELBEEKJE = FEATURES.register("onderwol_kronkelbeekje", () -> new Beekje(false));
    public static final DeferredHolder<Feature<?>, Beekje> BRONBEEKJE = FEATURES.register("onderwol_bronbeekje", () -> new Beekje(true));
    public static final DeferredHolder<Feature<?>, Grotmeertje> GROTMEERTJE = FEATURES.register("onderwol_grotmeertje", Grotmeertje::new);

    /** The biomes a brook may run through (every cell of its path is checked, so it never wanders into the sea or the lakes). */
    public static final TagKey<Biome> BEEK_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("onderwol/beekjes"));
    /** The stone of the cave layers (onderwol.py): a cave pond only lies on it. */
    public static final TagKey<Block> LAAGSTEEN = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/laagsteen"));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
    }

    // <onderwol-water> (free to tune)
    /** Kronkelbeekje: path length (cells), the shortest that counts, the bend strength. */
    static final int KRONKEL_MIN = 14, KRONKEL_LANG = 20, KRONKEL_EXTRA = 16;
    /** Bronbeekje: path length, the shortest, and how much lower than its spring it must end. */
    static final int BRON_MIN = 9, BRON_LANG = 14, BRON_EXTRA = 14, BRON_VAL = 2;
    /** Decoration chances per bank cell (riet, glinsterriet, roze gras) and per water cell (lily pad). */
    static final float RIET = 0.08f, GLINSTER = 0.08f, GRAS = 0.16f, LELIE_BEEK = 0.05f, LELIE_VIJVER = 0.14f;
    /** Cave ponds: the y range of their water. */
    static final int GROT_MIN_Y = 14, GROT_MAX_Y = 60;
    // </onderwol-water>

    // ==================================================================================================================
    // helpers
    // ==================================================================================================================
    static boolean leeg(BlockState s) {
        return s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    static boolean vast(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced() && s.isCollisionShapeFullBlock(level, p);
    }

    static boolean waterBron(BlockState s) {
        FluidState f = s.getFluidState();
        return f.isSource() && f.is(FluidTags.WATER);
    }

    static long kol(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    // ==================================================================================================================
    /**
     * A plan of water columns (x, z -> top, bottom, the ground to cut away above), checked as a whole before anything is
     * placed. {@link #controleer} works out the bank fills and raises a cell where two higher cells would spill over the
     * same lip (two sources around a flowing block would turn it into a new source that could run off).
     */
    static final class Plan {
        final WorldGenLevel level;
        final Map<Long, int[]> cellen = new LinkedHashMap<>();   // kol -> {x, z, top, bottom, h}
        final Map<Long, BlockPos> vullingen = new HashMap<>();   // BlockPos.asLong -> pos (a bank block to add)
        final Set<Long> lippen = new HashSet<>();                 // kol of the upper cells next to a lip (a little rapid)
        private final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        Plan(WorldGenLevel level) {
            this.level = level;
        }

        boolean heeft(int x, int z) {
            return cellen.containsKey(kol(x, z));
        }

        @Nullable
        int[] cel(int x, int z) {
            return cellen.get(kol(x, z));
        }

        void zet(int x, int z, int top, int bottom, int h) {
            cellen.putIfAbsent(kol(x, z), new int[]{x, z, top, bottom, h});
        }

        BlockState at(int x, int y, int z) {
            return level.getBlockState(p.set(x, y, z));
        }

        boolean vastAt(int x, int y, int z) {
            return vast(level, p.set(x, y, z));
        }

        /** True when the water of the plan stays in place; fills {@link #vullingen} and {@link #lippen}. */
        boolean controleer() {
            for (int ronde = 0; ronde < 10; ronde++) {
                vullingen.clear();
                lippen.clear();
                Map<Long, Integer> lipTelling = new HashMap<>();
                Map<Long, Boolean> lipEigen = new HashMap<>();
                for (int[] c : cellen.values()) {
                    int x = c[0], z = c[1], top = c[2], bottom = c[3];
                    if (!vastAt(x, bottom - 1, z)) {
                        return false;   // (no bed: a cave or air under it)
                    }
                    for (int y = bottom; y <= top; y++) {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            int nx = x + d.getStepX(), nz = z + d.getStepZ();
                            int[] n = cel(nx, nz);
                            if (n != null && n[3] <= y && y <= n[2]) {
                                continue;                       // our water
                            }
                            if (n != null && n[2] < y) {        // a lip: the neighbour's water is lower
                                if (y != top || y - n[2] > 2) {
                                    return false;
                                }
                                long k = BlockPos.asLong(nx, y, nz);
                                lipTelling.merge(k, 1, Integer::sum);
                                lipEigen.put(k, true);
                                lippen.add(kol(x, z));
                                continue;
                            }
                            if (n != null) {                    // under the neighbour's water: its bed or the ground below
                                if (!vastAt(nx, y, nz)) {
                                    return false;
                                }
                                continue;
                            }
                            BlockState s = at(nx, y, nz);
                            if (vastAt(nx, y, nz) || waterBron(s)) {
                                continue;                       // a wall, or other water at this level
                            }
                            if (y == top && leeg(s)) {
                                BlockState b = at(nx, y - 1, nz);
                                if (waterBron(b) || leeg(b) && b.getFluidState().isEmpty() && waterBron(at(nx, y - 2, nz))) {
                                    long k = BlockPos.asLong(nx, y, nz);   // a lip into water that is already there
                                    lipTelling.merge(k, 1, Integer::sum);
                                    lipEigen.putIfAbsent(k, false);
                                    lippen.add(kol(x, z));
                                    continue;
                                }
                                if (vastAt(nx, y - 1, nz)) {
                                    vullingen.put(BlockPos.asLong(nx, y, nz), new BlockPos(nx, y, nz));   // a soft bank block
                                    continue;
                                }
                            }
                            return false;
                        }
                    }
                    // the air over the water (and the cut away ground) must not border on other fluids
                    for (int y = top + 1; y <= top + 2; y++) {
                        if (!at(x, y, z).getFluidState().isEmpty() && y > c[4]) {
                            return false;
                        }
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            int nx = x + d.getStepX(), nz = z + d.getStepZ();
                            int[] n = cel(nx, nz);
                            if (n != null && n[3] <= y && y <= n[2]) {
                                continue;
                            }
                            if (!at(nx, y, nz).getFluidState().isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
                boolean verhoogd = false;
                for (Map.Entry<Long, Integer> e : lipTelling.entrySet()) {
                    if (e.getValue() < 2) {
                        continue;
                    }
                    if (!lipEigen.get(e.getKey())) {
                        return false;
                    }
                    BlockPos lip = BlockPos.of(e.getKey());
                    int[] n = cel(lip.getX(), lip.getZ());
                    n[2]++;     // raise the lower cell to the level of the cells above it
                    n[3]++;
                    verhoogd = true;
                }
                if (!verhoogd) {
                    return true;
                }
            }
            return false;
        }

        int[] kader() {
            int[] k = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (int[] c : cellen.values()) {
                k[0] = Math.min(k[0], c[0]);
                k[1] = Math.min(k[1], c[3] - 1);
                k[2] = Math.min(k[2], c[1]);
                k[3] = Math.max(k[3], c[0]);
                k[4] = Math.max(k[4], Math.max(c[2], c[4]) + 2);
                k[5] = Math.max(k[5], c[1]);
            }
            return k;
        }

        /** Places the plan: cut, fill the banks, the bed (viltkiezel where it lies safe, with this chance), then the water. */
        void plaats(RandomSource random, BlockState bank, float kiezelBed) {
            BlockState lucht = Blocks.AIR.defaultBlockState(), water = Blocks.WATER.defaultBlockState();
            BlockState kiezel = OnderWolFeature.VILTKIEZEL.get().defaultBlockState();
            for (int[] c : cellen.values()) {
                int x = c[0], z = c[1], top = c[2];
                for (int y = top + 1; y <= c[4]; y++) {
                    level.setBlock(p.set(x, y, z), lucht, Block.UPDATE_CLIENTS);
                }
                for (int y = Math.max(top, c[4]) + 1; y <= Math.max(top, c[4]) + 4; y++) {
                    BlockState s = at(x, y, z);
                    if (s.isAir() || !leeg(s)) {
                        break;
                    }
                    level.setBlock(p.set(x, y, z), lucht, Block.UPDATE_CLIENTS);   // (plants, the top of a tall one)
                }
            }
            for (BlockPos v : vullingen.values()) {
                BlockState boven = at(v.getX(), v.getY() + 1, v.getZ());
                if (!boven.isAir() && leeg(boven)) {
                    level.setBlock(p.set(v.getX(), v.getY() + 1, v.getZ()), lucht, Block.UPDATE_CLIENTS);
                }
                level.setBlock(v, bank, Block.UPDATE_CLIENTS);
            }
            for (int[] c : cellen.values()) {
                int x = c[0], z = c[1];
                if (random.nextFloat() < kiezelBed && vastAt(x, c[3] - 2, z)) {
                    level.setBlock(p.set(x, c[3] - 1, z), kiezel, Block.UPDATE_CLIENTS);
                }
                for (int y = c[3]; y <= c[2]; y++) {
                    level.setBlock(p.set(x, y, z), water, Block.UPDATE_CLIENTS);
                }
            }
            for (long k : lippen) {     // the little rapids start flowing as soon as the chunk is loaded
                int[] c = cellen.get(k);
                level.scheduleTick(new BlockPos(c[0], c[2], c[1]), Fluids.WATER, 0);
            }
        }

        /** The dry neighbours of the plan's cells: kol -> the highest water cell next to it. */
        Map<Long, int[]> oevers() {
            Map<Long, int[]> out = new LinkedHashMap<>();
            for (int[] c : cellen.values()) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = c[0] + d.getStepX(), nz = c[1] + d.getStepZ();
                    if (heeft(nx, nz)) {
                        continue;
                    }
                    int[] oud = out.get(kol(nx, nz));
                    if (oud == null || oud[2] < c[2]) {
                        out.put(kol(nx, nz), new int[]{nx, nz, c[2]});
                    }
                }
            }
            return out;
        }

        /** Whether water is going to flow over the top of this cell (a lily pad there would be washed away). */
        boolean onderLip(int x, int z) {
            int[] c = cel(x, z);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int[] n = cel(x + d.getStepX(), z + d.getStepZ());
                if (n != null && n[2] > c[2]) {
                    return true;
                }
            }
            return lippen.contains(kol(x, z));
        }
    }

    // ==================================================================================================================
    /**
     * A beekje. From the placed spot (the kronkelbeekje: only in a dip or flat low ground; the bronbeekje: first climbs to
     * the top of the hill it is on) a path of cells is traced downhill, one step at a time: level or a little up (cut into
     * the ground, at most 2), or one block down (a little rapid), or for the bronbeekje also two (a tiny waterfall). The
     * direction wanders in soft bends. The path ends where it meets other water (at its own level or just below) or, when
     * it is long enough or gets stuck, in a little pond. Everything stays inside the area a feature may write (the 3x3
     * chunks around), with a margin.
     */
    public static class Beekje extends Feature<NoneFeatureConfiguration> {
        private static final int N = 48, RAND = 7;
        private static final int[][] RICHTINGEN = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        private static final byte ONBEKEND = 0, GROND = 1, WATER = 2, ANDERS = 3;
        private final boolean bron;

        public Beekje(boolean bron) {
            super(NoneFeatureConfiguration.CODEC);
            this.bron = bron;
        }

        /** The columns of the area, worked out when first asked: ground (the top under plants) or water (its surface). */
        private static final class Kaart {
            final WorldGenLevel level;
            final int x0, z0;
            final byte[] soort = new byte[N * N];
            final int[] hoogte = new int[N * N];
            final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

            Kaart(WorldGenLevel level, int x0, int z0) {
                this.level = level;
                this.x0 = x0;
                this.z0 = z0;
            }

            boolean binnen(int x, int z) {
                return x >= x0 + RAND && x < x0 + N - RAND && z >= z0 + RAND && z < z0 + N - RAND;
            }

            private int i(int x, int z) {
                int i = (x - x0) * N + (z - z0);
                if (soort[i] == ONBEKEND) {
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    p.set(x, y, z);
                    for (int k = 0; k < 5 && leeg(level.getBlockState(p)); k++) {
                        p.move(Direction.DOWN);
                    }
                    BlockState s = level.getBlockState(p);
                    hoogte[i] = p.getY();
                    soort[i] = waterBron(s) ? WATER : s.is(OnderWolOppervlak.GROND) && s.getFluidState().isEmpty() ? GROND : ANDERS;
                }
                return i;
            }

            byte soort(int x, int z) {
                return soort[i(x, z)];
            }

            int h(int x, int z) {
                return hoogte[i(x, z)];
            }
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            Kaart k = new Kaart(level, (origin.getX() & ~15) - 16, (origin.getZ() & ~15) - 16);
            int x = origin.getX(), z = origin.getZ();
            if (k.soort(x, z) != GROND) {
                return false;
            }
            if (bron) {
                // climb to the top of the hill
                for (int stap = 0; stap < 16; stap++) {
                    int bx = x, bz = z, bh = k.h(x, z);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int nx = x + dx, nz = z + dz;
                            if (k.binnen(nx, nz) && k.soort(nx, nz) == GROND && k.h(nx, nz) > bh) {
                                bx = nx;
                                bz = nz;
                                bh = k.h(nx, nz);
                            }
                        }
                    }
                    if (bx == x && bz == z) {
                        break;
                    }
                    x = bx;
                    z = bz;
                }
            } else {
                // only in low ground: not higher than the ground around
                int som = 0, n = 0;
                for (int[] r : new int[][]{{8, 0}, {-8, 0}, {0, 8}, {0, -8}, {6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) {
                    if (k.binnen(x + r[0], z + r[1]) && k.soort(x + r[0], z + r[1]) != ANDERS) {
                        som += k.h(x + r[0], z + r[1]);
                        n++;
                    }
                }
                if (n < 5 || k.h(x, z) * n > som + n) {
                    return false;
                }
            }
            if (!k.binnen(x, z) || !level.getBiome(new BlockPos(x, k.h(x, z), z)).is(BEEK_BIOMES)) {
                return false;
            }

            // --- the path ---
            List<int[]> pad = new ArrayList<>();    // {x, z, level, dx, dz}
            int[] index = new int[N * N];
            Arrays.fill(index, -1);
            int w = k.h(x, z), doel = (bron ? BRON_LANG + random.nextInt(BRON_EXTRA) : KRONKEL_LANG + random.nextInt(KRONKEL_EXTRA));
            // the general direction: downhill
            double bx = 0, bz = 0;
            for (int[] r : RICHTINGEN) {
                for (int afstand = 3; afstand <= 6; afstand += 3) {
                    int nx = x + r[0] * afstand, nz = z + r[1] * afstand;
                    if (k.binnen(nx, nz) && k.soort(nx, nz) != ANDERS) {
                        double val = w - k.h(nx, nz);
                        bx += r[0] * val;
                        bz += r[1] * val;
                    }
                }
            }
            double hoek0 = bx * bx + bz * bz < 0.01 ? random.nextDouble() * Math.PI * 2 : Math.atan2(bz, bx);
            bx = Math.cos(hoek0);
            bz = Math.sin(hoek0);
            double fase = random.nextDouble() * Math.PI * 2, golf = bron ? 0.55 : 0.95, golfLengte = bron ? 2.6 : 3.3;
            pad.add(new int[]{x, z, w, 0, 0});
            index[(x - k.x0) * N + (z - k.z0)] = 0;
            boolean samen = false;    // joined other water
            int laagste = w;
            while (pad.size() < doel) {
                int[] cur = pad.get(pad.size() - 1);
                double hoek = Math.atan2(bz, bx) + golf * Math.sin(pad.size() / golfLengte + fase);
                double hx = Math.cos(hoek), hz = Math.sin(hoek);
                int[] beste = null;
                double besteKost = Double.MAX_VALUE;
                boolean besteSamen = false;
                for (int[] r : RICHTINGEN) {
                    int nx = cur[0] + r[0], nz = cur[1] + r[1];
                    if (!k.binnen(nx, nz) || index[(nx - k.x0) * N + (nz - k.z0)] >= 0 || teDichtBij(index, k, nx, nz, pad.size())) {
                        continue;
                    }
                    byte s = k.soort(nx, nz);
                    int dh = k.h(nx, nz) - cur[2];
                    double kost;
                    boolean meer = false;
                    if (s == WATER) {
                        if (dh > 0 || dh < (bron ? -2 : -1)) {
                            continue;
                        }
                        kost = -5;
                        meer = true;
                    } else if (s == GROND) {
                        if (dh > 2 || dh < (bron ? -2 : -1)) {
                            continue;
                        }
                        kost = dh > 0 ? dh * (bron ? 3.0 : 2.5) : dh == 0 ? (bron ? 0.6 : 0.0) : dh == -1 ? (bron ? -0.4 : 0.8) : 0.3;
                    } else {
                        continue;
                    }
                    if (!level.getBiome(new BlockPos(nx, k.h(nx, nz), nz)).is(BEEK_BIOMES)) {
                        continue;
                    }
                    kost += (1 - (r[0] * hx + r[1] * hz)) * (bron ? 1.2 : 1.6) + random.nextFloat() * 0.6;
                    if (kost < besteKost) {
                        besteKost = kost;
                        beste = r;
                        besteSamen = meer;
                    }
                }
                if (beste == null) {
                    break;
                }
                int nx = cur[0] + beste[0], nz = cur[1] + beste[1];
                if (besteSamen) {
                    samen = true;
                    break;
                }
                int nw = Math.min(cur[2], k.h(nx, nz));
                pad.add(new int[]{nx, nz, nw, beste[0], beste[1]});
                index[(nx - k.x0) * N + (nz - k.z0)] = pad.size() - 1;
                laagste = Math.min(laagste, nw);
                bx = bx * 0.8 + beste[0] * 0.2;
                bz = bz * 0.8 + beste[1] * 0.2;
            }
            if (pad.size() < (bron ? BRON_MIN : KRONKEL_MIN) || bron && k.h(x, z) - laagste < BRON_VAL && !samen) {
                return false;
            }

            // --- the water: the path, its width, and a pond at the end ---
            Plan plan = new Plan(level);
            double breedteFase = random.nextDouble() * Math.PI * 2;
            for (int i = 0; i < pad.size(); i++) {
                int[] c = pad.get(i);
                plan.zet(c[0], c[1], c[2], c[2], Math.max(c[2], k.h(c[0], c[1])));
            }
            for (int i = 0; i < pad.size(); i++) {
                int[] c = pad.get(i);
                int breedte;
                double s = Math.sin(i / 4.1 + breedteFase);
                if (bron) {
                    breedte = i < pad.size() / 2 ? 1 : s > 0.3 ? 2 : 1;
                } else {
                    breedte = i < 3 ? 1 : s > 0.55 ? 3 : s > -0.45 ? 2 : 1;
                }
                int[] r = i + 1 < pad.size() ? pad.get(i + 1) : c;
                int dx = i == 0 ? r[3] : c[3], dz = i == 0 ? r[4] : c[4];
                int px = -dz, pz = dx;      // perpendicular
                if (breedte >= 2) {
                    int kant = s >= 0 ? 1 : -1;
                    breedteCel(plan, k, c[0] + px * kant, c[1] + pz * kant, c[2]);
                }
                if (breedte == 3) {
                    int kant = s >= 0 ? -1 : 1;
                    breedteCel(plan, k, c[0] + px * kant, c[1] + pz * kant, c[2]);
                }
            }
            int[] eind = pad.get(pad.size() - 1);
            List<int[]> vijver = new ArrayList<>();
            if (!samen) {
                double rx = (bron ? 2.0 : 2.4) + random.nextDouble() * 1.2, rz = (bron ? 2.0 : 2.4) + random.nextDouble() * 1.2;
                int R = (int) Math.ceil(Math.max(rx, rz));
                // the pond lies a little ahead, in the direction the brook came
                int mx = eind[0] + eind[3] * (R - 1), mz = eind[1] + eind[4] * (R - 1);
                for (int dx = -R; dx <= R; dx++) {
                    for (int dz = -R; dz <= R; dz++) {
                        double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz));
                        int vx = mx + dx, vz = mz + dz;
                        if (d > 1.0 || !k.binnen(vx, vz) || plan.heeft(vx, vz) || k.soort(vx, vz) != GROND) {
                            continue;
                        }
                        int h = k.h(vx, vz);
                        if (h < eind[2] || h > eind[2] + 2) {
                            continue;
                        }
                        int bodem = d < 0.5 ? eind[2] - 1 : eind[2];
                        plan.zet(vx, vz, eind[2], bodem, h);
                        vijver.add(new int[]{vx, vz});
                    }
                }
                if (vijver.size() < 5) {
                    return false;
                }
            }
            if (!plan.controleer()) {
                return false;
            }
            int[] kader = plan.kader();
            if (BouwRuimte.touchesBuilding(level, kader[0] - 3, kader[1] - 2, kader[2] - 3, kader[3] + 3, kader[4] + 4, kader[5] + 3)) {
                return false;
            }
            plan.plaats(random, OnderWolFeature.VILTKIEZEL.get().defaultBlockState(), bron ? 0.6f : 0.75f);
            versier(level, random, plan, pad, vijver);
            return true;
        }

        /** Whether a cell lies next to an older part of the path (so the brook never touches itself). */
        private static boolean teDichtBij(int[] index, Kaart k, int x, int z, int lengte) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int ax = x + dx - k.x0, az = z + dz - k.z0;
                    if (ax < 0 || az < 0 || ax >= N || az >= N) {
                        continue;
                    }
                    int i = index[ax * N + az];
                    if (i >= 0 && i < lengte - 2) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static void breedteCel(Plan plan, Kaart k, int x, int z, int w) {
            if (!k.binnen(x, z) || plan.heeft(x, z) || k.soort(x, z) != GROND) {
                return;
            }
            int h = k.h(x, z);
            if (h >= w && h <= w + 2) {
                plan.zet(x, z, w, w, h);
            }
        }

        /** The bronnetjes, viltkiezel on the banks, reeds, glinsterriet, roze gras and lily pads. */
        private void versier(WorldGenLevel level, RandomSource random, Plan plan, List<int[]> pad, List<int[]> vijver) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            BlockState kiezel = OnderWolFeature.VILTKIEZEL.get().defaultBlockState();
            BlockState lelie = OnderWolOppervlak.blok("guh_waterlelie", Blocks.LILY_PAD), riet = OnderWolOppervlak.blok("bloesemmeertje_riet", Blocks.TALL_GRASS);
            BlockState gras = OnderWolOppervlak.blok("roze_gras", Blocks.SHORT_GRASS), glinster = GLINSTERRIET.get().defaultBlockState();
            BlockState bronnetje = BRONNETJE.get().defaultBlockState().setValue(Bronnetje.WATERLOGGED, true);
            long zaad = level.getSeed();
            // the spring at the head, and (half the time) a second one on the bottom of the pond
            int[] kop = plan.cel(pad.get(0)[0], pad.get(0)[1]);
            level.setBlock(p.set(kop[0], kop[3], kop[1]), bronnetje, Block.UPDATE_CLIENTS);
            if (!vijver.isEmpty() && random.nextBoolean()) {
                int[] m = null;
                for (int[] v : vijver) {
                    int[] c = plan.cel(v[0], v[1]);
                    if (c[3] < c[2] && (m == null || random.nextInt(3) == 0)) {
                        m = c;
                    }
                }
                if (m != null) {
                    level.setBlock(p.set(m[0], m[3], m[1]), bronnetje, Block.UPDATE_CLIENTS);
                }
            }
            // the banks
            for (int[] o : plan.oevers().values()) {
                int x = o[0], z = o[1], w = o[2];
                int hb = Integer.MIN_VALUE;
                for (int y = w + 3; y >= w - 1; y--) {
                    if (!leeg(level.getBlockState(p.set(x, y, z)))) {
                        hb = y;
                        break;
                    }
                }
                if (hb < w || hb > w + 1 || !vast(level, p.set(x, hb, z)) || !level.getBlockState(p.set(x, hb + 1, z)).isAir()) {
                    continue;
                }
                BlockState grond = level.getBlockState(p.set(x, hb, z));
                if (hb == w && grond.is(OnderWolOppervlak.GROND) && OnderWolOppervlak.hash(zaad, x, z, 29) < 0.5 && vast(level, p.set(x, hb - 1, z))) {
                    level.setBlock(p.set(x, hb, z), kiezel, Block.UPDATE_CLIENTS);
                }
                p.set(x, hb + 1, z);
                float f = random.nextFloat();
                if (f < RIET * (bron ? 0.7f : 1.0f)) {
                    if (level.getBlockState(p.above()).isAir() && riet.getBlock() instanceof DoublePlantBlock) {
                        DoublePlantBlock.placeAt(level, riet, p.immutable(), Block.UPDATE_CLIENTS);
                    }
                } else if (f < RIET + GLINSTER) {
                    level.setBlock(p, glinster, Block.UPDATE_CLIENTS);
                } else if (f < RIET + GLINSTER + GRAS) {
                    level.setBlock(p, gras, Block.UPDATE_CLIENTS);
                }
            }
            // lily pads on still water
            Set<Long> inVijver = new HashSet<>();
            for (int[] v : vijver) {
                inVijver.add(kol(v[0], v[1]));
            }
            for (int[] c : plan.cellen.values()) {
                float kans = inVijver.contains(kol(c[0], c[1])) ? LELIE_VIJVER : LELIE_BEEK * (bron ? 0.5f : 1.0f);
                if (c == kop || random.nextFloat() >= kans || plan.onderLip(c[0], c[1])) {
                    continue;
                }
                if (level.getBlockState(p.set(c[0], c[2] + 1, c[1])).isAir()) {
                    level.setBlock(p, lelie, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ==================================================================================================================
    /**
     * A grotmeertje: from a spot of cave air the floor is found, then the lowest floor nearby; there a little oval pond
     * (radius 2-4, 1-2 deep) is sunk into the stone. Where the floor around drops away a rim of glowing lichtmosblok holds
     * the water; around it lichtmos and glinsterpaddenstoelen. Never near saus, never on anything but the layer stone.
     */
    public static class Grotmeertje extends Feature<NoneFeatureConfiguration> {
        public Grotmeertje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            if (!level.getBlockState(origin).isAir()) {
                return false;
            }
            BlockPos.MutableBlockPos p = origin.mutable();
            for (int i = 0; i < 16 && level.getBlockState(p.below()).isAir(); i++) {
                p.move(Direction.DOWN);
            }
            if (!vloer(level, p.getX(), p.getY() - 1, p.getZ())) {
                return false;
            }
            int cx = p.getX(), cz = p.getZ(), L = p.getY() - 1;
            // walk to the low part of the floor
            for (int stap = 0; stap < 8; stap++) {
                int bx = cx, bz = cz, bl = L;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = cx + d.getStepX(), nz = cz + d.getStepZ();
                    if (!level.getBlockState(p.set(nx, L + 1, nz)).isAir()) {
                        continue;
                    }
                    int y = L + 1;
                    while (y > L - 3 && level.getBlockState(p.set(nx, y - 1, nz)).isAir()) {
                        y--;
                    }
                    if (y - 1 < bl && vloer(level, nx, y - 1, nz)) {
                        bx = nx;
                        bz = nz;
                        bl = y - 1;
                    }
                }
                if (bx == cx && bz == cz) {
                    break;
                }
                cx = bx;
                cz = bz;
                L = bl;
            }
            if (L < GROT_MIN_Y || L > GROT_MAX_Y || Math.abs(cx - origin.getX()) > 9 || Math.abs(cz - origin.getZ()) > 9) {
                return false;
            }
            // no saus (or any fluid but water) near
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int y = L - 2; y <= L + 3; y++) {
                        FluidState f = level.getBlockState(p.set(cx + dx, y, cz + dz)).getFluidState();
                        if (!f.isEmpty() && !f.is(FluidTags.WATER)) {
                            return false;
                        }
                    }
                }
            }
            double rx = 2.0 + random.nextDouble() * 2.2, rz = 2.0 + random.nextDouble() * 2.2;
            int R = (int) Math.ceil(Math.max(rx, rz));
            Plan plan = new Plan(level);
            long zaad = level.getSeed();
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    int x = cx + dx, z = cz + dz;
                    double d = Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz)) + OnderWolOppervlak.hash(zaad, x, z, 31) * 0.18;
                    if (d > 1.0 || !vloer(level, x, L, z) || !level.getBlockState(p.set(x, L + 1, z)).isAir()) {
                        continue;
                    }
                    int bodem = d < 0.5 && vast(level, p.set(x, L - 2, z)) ? L - 1 : L;
                    plan.zet(x, z, L, bodem, L);
                }
            }
            // only the part connected to the middle
            Set<Long> verbonden = new HashSet<>();
            ArrayDeque<long[]> rij = new ArrayDeque<>();
            if (plan.heeft(cx, cz)) {
                rij.add(new long[]{cx, cz});
                verbonden.add(kol(cx, cz));
            }
            while (!rij.isEmpty()) {
                long[] c = rij.poll();
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = (int) c[0] + d.getStepX(), nz = (int) c[1] + d.getStepZ();
                    if (plan.heeft(nx, nz) && verbonden.add(kol(nx, nz))) {
                        rij.add(new long[]{nx, nz});
                    }
                }
            }
            plan.cellen.keySet().retainAll(verbonden);
            if (plan.cellen.size() < 4 || !plan.controleer()) {
                return false;
            }
            BlockState lichtmosblok = OnderWolFeature.LICHTMOSBLOK.get().defaultBlockState();
            plan.plaats(random, lichtmosblok, 0.45f);
            // the glowing rim
            BlockState lichtmos = OnderWolFeature.LICHTMOS.get().defaultBlockState(), paddenstoel = OnderWolFeature.GLINSTERPADDENSTOEL.get().defaultBlockState();
            for (int[] o : plan.oevers().values()) {
                int x = o[0], z = o[1];
                p.set(x, L, z);
                if (!vast(level, p) || !level.getBlockState(p.set(x, L + 1, z)).isAir()) {
                    continue;
                }
                if (level.getBlockState(p.set(x, L, z)).is(LAAGSTEEN) && random.nextFloat() < 0.45f) {
                    level.setBlock(p, lichtmosblok, Block.UPDATE_CLIENTS);
                }
                float f = random.nextFloat();
                if (f < 0.45f) {
                    level.setBlock(p.set(x, L + 1, z), lichtmos, Block.UPDATE_CLIENTS);
                } else if (f < 0.58f) {
                    level.setBlock(p.set(x, L + 1, z), paddenstoel, Block.UPDATE_CLIENTS);
                }
            }
            if (random.nextFloat() < 0.3f) {
                int[] m = plan.cel(cx, cz);
                level.setBlock(p.set(m[0], m[3], m[1]), BRONNETJE.get().defaultBlockState().setValue(Bronnetje.WATERLOGGED, true), Block.UPDATE_CLIENTS);
            }
            return true;
        }

        private static boolean vloer(WorldGenLevel level, int x, int y, int z) {
            BlockPos p = new BlockPos(x, y, z);
            return vast(level, p) && level.getBlockState(p).is(LAAGSTEEN);
        }
    }

    // ==================================================================================================================
    // the blocks
    // ==================================================================================================================
    /**
     * The bronnetje: a little mound of felt pebbles around a spring mouth. Under water bubbles rise from it and at the
     * surface above it soap bubbles and pink glints come up (and now and then a soft bubbling sound); on land it only
     * glitters.
     */
    public static class Bronnetje extends Block implements SimpleWaterloggedBlock {
        public static final MapCodec<Bronnetje> CODEC = simpleCodec(Bronnetje::new);
        public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);

        public Bronnetje(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Bronnetje> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (state.getValue(WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
            if (!state.getValue(WATERLOGGED)) {
                if (random.nextInt(4) == 0) {
                    level.addParticle(KnuffelbadFeature.GLINSTERING.get(), x + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.4,
                            z + (random.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
                }
                return;
            }
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.BUBBLE, x + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.35, z + (random.nextDouble() - 0.5) * 0.4,
                        0, 0.12 + random.nextDouble() * 0.06, 0);
            }
            // the surface of the water above
            BlockPos.MutableBlockPos p = pos.mutable();
            int diep = 0;
            while (diep < 5 && level.getFluidState(p.move(Direction.UP)).is(FluidTags.WATER)) {
                diep++;
            }
            if (diep >= 5 || !level.getBlockState(p).isAir()) {
                return;
            }
            double y = p.getY() + 0.02;
            if (random.nextInt(3) == 0) {
                level.addParticle(KnuffelbadFeature.ZEEPBELLETJE.get(), x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5,
                        0, 0.015, 0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(KnuffelbadFeature.GLINSTERING.get(), x + (random.nextDouble() - 0.5) * 1.2, y + 0.1 + random.nextDouble() * 0.5,
                        z + (random.nextDouble() - 0.5) * 1.2, 0, 0.01, 0);
            }
            if (random.nextInt(140) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.25f, 1.3f + random.nextFloat() * 0.3f, false);
            }
        }
    }

    /** The glinsterriet: a slender pink reed on the banks; a pink glint floats off it now and then. */
    public static class Glinsterriet extends VegetationBlock {
        public static final MapCodec<Glinsterriet> CODEC = simpleCodec(Glinsterriet::new);
        private static final VoxelShape SHAPE = Block.column(10, 0, 14);

        public Glinsterriet(BlockBehaviour.Properties properties) {
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

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(7) == 0) {
                level.addParticle(KnuffelbadFeature.GLINSTERING.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.5 + random.nextDouble() * 0.5,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.008, 0);
            }
        }
    }

    /** A block item with its grey lore line (lang {@code block.guhs.<id>.lore}). */
    static class LoreBlockItem extends BlockItem {
        LoreBlockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private OnderWolWater() {
    }
}
