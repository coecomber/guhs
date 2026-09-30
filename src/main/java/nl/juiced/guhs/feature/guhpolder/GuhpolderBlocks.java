package nl.juiced.guhs.feature.guhpolder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of the Guhpolder (see {@link GuhpolderFeature}). */
public final class GuhpolderBlocks {
    /** A snow cap on the knotwilg's twigs (set by the worldgen on the top twigs; snow on top keeps it, a solid block takes it off). */
    public static final BooleanProperty SNEEUW = BooleanProperty.create("sneeuw");

    private GuhpolderBlocks() {
    }

    /** Where the polder plants grow: rijpgras, dirt-like blocks and snow. */
    static boolean poldergrond(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(GuhpolderFeature.RIJPGRAS.get()) || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
    }

    /** A soft glitter now and then (frost in the sun). */
    static void glinster(Level level, BlockPos pos, RandomSource random, int oneIn, double y) {
        if (random.nextInt(oneIn) == 0) {
            level.addParticle(GuhpolderFeature.GLINSTER.get(), pos.getX() + random.nextDouble(), pos.getY() + y, pos.getZ() + random.nextDouble(),
                    0, 0.004, 0);
        }
    }

    // =================================================================================================================

    /**
     * Rijpgras: frosted guh grass, white-blue with a glitter of rime. With snow on top it shows its snowy sides (like a
     * grass block). Bone meal: rijpsprietjes and now and then a guh-ijsbloempje around it.
     */
    public static class Rijpgras extends SnowyBlock implements BonemealableBlock {
        public static final MapCodec<Rijpgras> CODEC = simpleCodec(Rijpgras::new);

        public Rijpgras(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends SnowyBlock> codec() {
            return CODEC;
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return level.getBlockState(pos.above()).isAir();
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            for (int i = 0; i < 24; i++) {
                BlockPos p = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
                if (level.getBlockState(p).is(this) && level.getBlockState(p.above()).isAir()) {
                    Block plant = random.nextInt(10) == 0 ? GuhpolderFeature.GUH_IJSBLOEMPJE.get() : GuhpolderFeature.RIJPSPRIETJES.get();
                    level.setBlock(p.above(), plant.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (!state.getValue(SNOWY) && level.getBlockState(pos.above()).isAir()) {
                glinster(level, pos, random, 90, 1.02);
            }
        }
    }

    /** Rijpsprietjes: a tuft of frosted grass sprigs with little ice needles. */
    public static class Rijpsprietjes extends BushBlock {
        public static final MapCodec<Rijpsprietjes> CODEC = simpleCodec(Rijpsprietjes::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 11, 14);

        public Rijpsprietjes(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return poldergrond(state) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** The guh-ijsbloempje: a see-through ice-blue flower with a tiny guh face; it glows a little and glitters. */
    public static class IJsbloempje extends FlowerBlock {
        public IJsbloempje(Holder<MobEffect> effect, float seconds, Properties properties) {
            super(effect, seconds, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return poldergrond(state) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            glinster(level, pos, random, 25, 0.55);
        }
    }

    /**
     * Polderijs: the ice of the canal and the sloten. Slippery like packed ice (great for skating and for a Pinguh's
     * belly), and it NEVER melts, not even next to a campfire in summer.
     */
    public static class Polderijs extends Block {
        public Polderijs(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return false;
        }
    }

    /**
     * Knotwilg twigs: thin reddish willow twigs with a bit of frost. The top twigs of a tree from the worldgen carry a
     * snow cap ({@link #SNEEUW}); snow landing on top gives them one too, a solid block on top takes it off.
     */
    public static class KnotwilgBladeren extends LeavesBlock {
        public KnotwilgBladeren(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(SNEEUW, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SNEEUW);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState out = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
            if (direction == Direction.UP && out.is(this)) {
                if (neighbor.is(BlockTags.SNOW)) {
                    out = out.setValue(SNEEUW, true);
                } else if (neighbor.isFaceSturdy(level, neighborPos, Direction.DOWN)) {
                    out = out.setValue(SNEEUW, false);
                }
            }
            return out;
        }
    }

    /**
     * The ijspegelguh-kristal: a glowing ice crystal shaped like a guh ear. It grows on any side of a block (like an
     * amethyst cluster), glows and glitters.
     */
    public static class IJspegelKristal extends AmethystClusterBlock {
        public IJspegelKristal(Properties properties) {
            super(9, 3, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(6) == 0) {
                Direction d = state.getValue(BlockStateProperties.FACING);
                level.addParticle(GuhpolderFeature.GLINSTER.get(), pos.getX() + 0.5 + d.getStepX() * 0.3 + (random.nextDouble() - 0.5) * 0.5,
                        pos.getY() + 0.5 + d.getStepY() * 0.3 + (random.nextDouble() - 0.5) * 0.5,
                        pos.getZ() + 0.5 + d.getStepZ() * 0.3 + (random.nextDouble() - 0.5) * 0.5, 0, 0.01, 0);
            }
        }
    }
}
