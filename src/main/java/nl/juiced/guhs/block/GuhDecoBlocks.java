package nl.juiced.guhs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModItems;

/** The smaller 2.0.0 blocks: food, deco and the kaasknabbel plant. */
public final class GuhDecoBlocks {

    /** The guh cake: 7 bites, like a normal cake (and it looks like a guh). */
    public static class GuhTaart extends CakeBlock {
        public GuhTaart(Properties properties) {
            super(properties);
        }
    }

    /** The guh cupboard ("kast"): a barrel with doors that always stands upright. */
    public static class Kast extends BarrelBlock {
        public Kast(Properties properties) {
            super(properties);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    /** A string of little flags ("vlaggetjes"): put them next to each other for a garland. Hangs anywhere. */
    public static class Vlaggetjes extends Block {
        public static final MapCodec<Vlaggetjes> CODEC = simpleCodec(Vlaggetjes::new);
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X = Block.box(0, 6, 7, 16, 16, 9), Z = Block.box(7, 6, 0, 9, 16, 16);

        public Vlaggetjes(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }

    /** Guh flowers: grow on grass and dirt, but also on pink wool and kaasknabbels (the Guhmension ground). */
    public static class GuhBloem extends FlowerBlock {
        public GuhBloem(Holder<MobEffect> effect, float seconds, Properties properties) {
            super(effect, seconds, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
        }
    }

    /** Pink grass of the Guhmension: now and then gives kaasknabbel seeds. */
    public static class RozeGras extends TallGrassBlock {
        public RozeGras(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return false; // (vanilla would grow it into normal tall grass)
        }
    }

    /** The kaasknabbel plant: a crop (8 stages) that grows kaas knabbels. */
    public static class Kaasknabbelplant extends CropBlock {
        public Kaasknabbelplant(Properties properties) {
            super(properties);
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return ModItems.KAASKNABBELZAADJES.get();
        }
    }

    private GuhDecoBlocks() {
    }
}
