package nl.juiced.guhs.feature.einde2.biomes;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice biomes: the knabbelkoor, the chorus plant of the Guheinde: tall purple stems ({@link Plant}) with a flower
 * on every tip ({@link Bloem}) that softly sings, each flower its own note. Own classes: the vanilla chorus classes only
 * know end stone and their own two blocks. They grow the way the vanilla ones do, on {@link #GROND} (the ground of the
 * outer islands and the knabbelkoor's own building blocks), and a whole plant at once comes from {@link #groei}.
 */
public final class Knabbelkoor {
    /** What a knabbelkoor stands on. */
    public static final TagKey<Block> GROND = TagKey.create(Registries.BLOCK, Guhs.id("einde2/biomes_koorgrond"));
    /** The notes the flowers sing (semitones from the sound's own pitch): a pentatonic scale, so any handful sounds like a chord. */
    private static final int[] NOTEN = {-12, -10, -7, -5, -3, 0, 2, 5, 7};

    private static Block plant() {
        return E2BiomesFeature.KNABBELKOORPLANT.get();
    }

    private static Block bloem() {
        return E2BiomesFeature.KNABBELKOORBLOEM.get();
    }

    private static boolean deel(BlockState state) {
        return state.is(plant()) || state.is(bloem());
    }

    /** The stem with its arms towards whatever it joins here. */
    public static BlockState stam(BlockGetter level, BlockPos pos) {
        BlockState onder = level.getBlockState(pos.below());
        return plant().defaultBlockState()
                .setValue(PipeBlock.DOWN, deel(onder) || onder.is(GROND))
                .setValue(PipeBlock.UP, deel(level.getBlockState(pos.above())))
                .setValue(PipeBlock.NORTH, deel(level.getBlockState(pos.north())))
                .setValue(PipeBlock.EAST, deel(level.getBlockState(pos.east())))
                .setValue(PipeBlock.SOUTH, deel(level.getBlockState(pos.south())))
                .setValue(PipeBlock.WEST, deel(level.getBlockState(pos.west())));
    }

    private static boolean burenLeeg(LevelReader level, BlockPos pos, @Nullable Direction behalve) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d != behalve && !level.isEmptyBlock(pos.relative(d))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Grows a whole knabbelkoor from {@code voet} upwards (the block there becomes its first stem; the caller checked the
     * ground). Taller than a vanilla chorus plant: the Knabbelkoorbos is a forest of singing poles.
     */
    public static void groei(LevelAccessor level, BlockPos voet, RandomSource random, int spreiding) {
        level.setBlock(voet, stam(level, voet), 2);
        tak(level, voet, random, voet, spreiding, 0);
    }

    private static void tak(LevelAccessor level, BlockPos van, RandomSource random, BlockPos voet, int spreiding, int diepte) {
        int wil = diepte == 0 ? 3 + random.nextInt(5) : 1 + random.nextInt(4);
        int hoogte = 0;
        while (hoogte < wil && level.isEmptyBlock(van.above(hoogte + 1)) && burenLeeg(level, van.above(hoogte + 1), null)) {
            hoogte++;
        }
        if (hoogte == 0) {
            return;      // (no room at all: it stays a bare stem)
        }
        for (int i = 1; i <= hoogte; i++) {
            BlockPos p = van.above(i);
            level.setBlock(p, stam(level, p), 2);
            level.setBlock(p.below(), stam(level, p.below()), 2);
        }
        boolean vertakt = false;
        if (diepte < 4 && hoogte == wil) {
            int takken = random.nextInt(4) + (diepte == 0 ? 1 : 0);
            for (int i = 0; i < takken; i++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos p = van.above(hoogte).relative(d);
                if (Math.abs(p.getX() - voet.getX()) < spreiding && Math.abs(p.getZ() - voet.getZ()) < spreiding && level.isEmptyBlock(p)
                        && level.isEmptyBlock(p.below()) && burenLeeg(level, p, d.getOpposite())) {
                    vertakt = true;
                    level.setBlock(p, stam(level, p), 2);
                    level.setBlock(p.relative(d.getOpposite()), stam(level, p.relative(d.getOpposite())), 2);
                    tak(level, p, random, voet, spreiding, diepte + 1);
                }
            }
        }
        if (!vertakt) {
            BlockPos top = van.above(hoogte);
            level.setBlock(top, bloem().defaultBlockState().setValue(Bloem.AGE, Bloem.DOOD), 2);
            level.setBlock(top.below(), stam(level, top.below()), 2);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** The stem (vanilla ChorusPlantBlock with our own ground and flower). */
    public static class Plant extends PipeBlock {
        public static final MapCodec<Plant> CODEC = simpleCodec(Plant::new);

        public Plant(BlockBehaviour.Properties properties) {
            super(10.0f, properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                    .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
        }

        @Override
        protected MapCodec<Plant> codec() {
            return CODEC;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return stam(context.getLevel(), context.getClickedPos());
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction naar, BlockPos buurPos,
                BlockState buur, RandomSource random) {
            if (!state.canSurvive(level, pos)) {
                ticks.scheduleTick(pos, this, 1);
                return super.updateShape(state, level, ticks, pos, naar, buurPos, buur, random);
            }
            return state.setValue(PROPERTY_BY_DIRECTION.get(naar), deel(buur) || naar == Direction.DOWN && buur.is(GROND));
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.canSurvive(level, pos)) {
                level.destroyBlock(pos, true);
            }
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState onder = level.getBlockState(pos.below());
            boolean klem = !level.getBlockState(pos.above()).isAir() && !onder.isAir();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos naast = pos.relative(d);
                if (level.getBlockState(naast).is(this)) {
                    if (klem) {
                        return false;
                    }
                    BlockState eronder = level.getBlockState(naast.below());
                    if (eronder.is(this) || eronder.is(GROND)) {
                        return true;
                    }
                }
            }
            return onder.is(this) || onder.is(GROND);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        }

        /** 1.7.0: a structure placed turned or mirrored turns this block along (Block.rotate/mirror keep the state as it is). */
        @Override
        protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
            return nl.juiced.guhs.block.Draaien.kanten(state, PROPERTY_BY_DIRECTION, rotation);
        }

        @Override
        protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
            return nl.juiced.guhs.block.Draaien.kanten(state, PROPERTY_BY_DIRECTION, mirror);
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The flower on a tip (vanilla ChorusFlowerBlock with our own ground and stem): ages 0-4 grow on random ticks, 5 is done
     * growing. Every flower sings now and then, young or old; bone meal on a young one grows the whole plant at once.
     */
    public static class Bloem extends Block implements BonemealableBlock {
        public static final MapCodec<Bloem> CODEC = simpleCodec(Bloem::new);
        public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
        public static final int DOOD = 5;
        private static final VoxelShape STEUN = Block.column(14.0, 0.0, 15.0);

        public Bloem(BlockBehaviour.Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
        }

        @Override
        protected MapCodec<Bloem> codec() {
            return CODEC;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.canSurvive(level, pos)) {
                level.destroyBlock(pos, true);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return state.getValue(AGE) < DOOD;
        }

        @Override
        public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
            return STEUN;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            groeiStap(state, level, pos, random);
        }

        /** One step of growing (a random tick): up, or a branch sideways, or the flower is done. Returns whether anything changed. */
        public boolean groeiStap(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            BlockPos boven = pos.above();
            int leeftijd = state.getValue(AGE);
            if (leeftijd >= DOOD || !level.isEmptyBlock(boven) || boven.getY() > level.getMaxY()) {
                return false;
            }
            boolean omhoog = false;
            boolean opGrond = false;
            BlockState onder = level.getBlockState(pos.below());
            if (onder.is(GROND)) {
                omhoog = true;
            } else if (onder.is(plant())) {
                int hoogte = 1;
                for (int i = 0; i < 6; i++) {
                    BlockState s = level.getBlockState(pos.below(hoogte + 1));
                    if (!s.is(plant())) {
                        opGrond = s.is(GROND);
                        break;
                    }
                    hoogte++;
                }
                if (hoogte < 3 || hoogte <= random.nextInt(opGrond ? 7 : 5)) {
                    omhoog = true;
                }
            } else if (onder.isAir()) {
                omhoog = true;
            }
            if (omhoog && burenLeeg(level, boven, null) && level.isEmptyBlock(pos.above(2))) {
                level.setBlock(pos, stam(level, pos), 2);
                zet(level, boven, leeftijd);
            } else if (leeftijd < 4) {
                int pogingen = random.nextInt(4) + (opGrond ? 1 : 0);
                boolean vertakt = false;
                for (int i = 0; i < pogingen; i++) {
                    Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    BlockPos p = pos.relative(d);
                    if (level.isEmptyBlock(p) && level.isEmptyBlock(p.below()) && burenLeeg(level, p, d.getOpposite())) {
                        zet(level, p, leeftijd + 1);
                        vertakt = true;
                    }
                }
                if (vertakt) {
                    level.setBlock(pos, stam(level, pos), 2);
                } else {
                    klaar(level, pos);
                }
            } else {
                klaar(level, pos);
            }
            return true;
        }

        private void zet(Level level, BlockPos pos, int leeftijd) {
            level.setBlock(pos, this.defaultBlockState().setValue(AGE, leeftijd), 2);
            level.levelEvent(1033, pos, 0);
        }

        private void klaar(Level level, BlockPos pos) {
            level.setBlock(pos, this.defaultBlockState().setValue(AGE, DOOD), 2);
            level.levelEvent(1034, pos, 0);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction naar, BlockPos buurPos,
                BlockState buur, RandomSource random) {
            if (naar != Direction.UP && !state.canSurvive(level, pos)) {
                ticks.scheduleTick(pos, this, 1);
            }
            return super.updateShape(state, level, ticks, pos, naar, buurPos, buur, random);
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState onder = level.getBlockState(pos.below());
            if (onder.is(plant()) || onder.is(GROND)) {
                return true;
            }
            if (!onder.isAir()) {
                return false;
            }
            boolean een = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState naast = level.getBlockState(pos.relative(d));
                if (naast.is(plant())) {
                    if (een) {
                        return false;
                    }
                    een = true;
                } else if (!naast.isAir()) {
                    return false;
                }
            }
            return een;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
            BlockPos pos = hit.getBlockPos();
            if (level instanceof ServerLevel server && projectile.mayInteract(server, pos) && projectile.mayBreak(server)) {
                level.destroyBlock(pos, true, projectile);
            }
        }

        // --- the singing (client side) -----------------------------------------------------------------------------------
        /** The note of the flower at this spot, as a sound pitch. */
        public static float toon(BlockPos pos) {
            int n = NOTEN[Math.floorMod(Mth.murmurHash3Mixer(pos.getX() * 31 + pos.getY() * 17 + pos.getZ() * 7), NOTEN.length)];
            return (float) Math.pow(2.0, n / 12.0);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(18) != 0) {
                return;
            }
            float toon = toon(pos);
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, E2BiomesFeature.KNABBELKOOR_ZING.get(), SoundSource.BLOCKS,
                    0.35f, toon, false);
            // (the note particle takes its colour from its x speed)
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, (Math.log(toon) / Math.log(2.0) + 1.0) / 2.0, 0.0, 0.0);
        }

        // --- bone meal: a young flower shoots up into a whole plant ------------------------------------------------------
        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return state.getValue(AGE) < DOOD && level.isEmptyBlock(pos.above());
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return random.nextFloat() < 0.4f;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(E2BiomesFeature.GEKWEEKT)
                    .ifPresent(f -> f.value().place(level, level.getChunkSource().getGenerator(), random, pos));
        }
    }

    private Knabbelkoor() {
    }
}
