package nl.juiced.guhs.feature.kaaskas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.8.0: het pluisbolletje, a crop like cotton: four stages (age 0..3), the ripe one is a white-pink puff that gives pluis.
 * A plain {@link CropBlock}: the Oogster, the guh farm chore, the Kassier and the Kaaskas know it by themselves.
 */
public class PluisbolletjeBlock extends CropBlock {
    public static final MapCodec<PluisbolletjeBlock> CODEC = simpleCodec(PluisbolletjeBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.column(16.0, 0.0, 4.0), Block.column(16.0, 0.0, 8.0), Block.column(16.0, 0.0, 11.0),
            Block.column(16.0, 0.0, 14.0)};

    public PluisbolletjeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return KaaskasFeature.PLUIS_ZAAD.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }
}
