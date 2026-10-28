package nl.juiced.guhs.feature.rozegraan;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.8.0: roze graan, the crop of the Zingende Weide. Four stages (age 0..3, like beetroot); a ripe plant glitters a little.
 * It is a plain {@link CropBlock}, so the Oogster, the guh farm chore and the Blub's Bellenbesproeier know it by themselves.
 */
public class RozeGraanBlock extends CropBlock {
    public static final MapCodec<RozeGraanBlock> CODEC = simpleCodec(RozeGraanBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.column(16.0, 0.0, 3.0), Block.column(16.0, 0.0, 7.0), Block.column(16.0, 0.0, 11.0),
            Block.column(16.0, 0.0, 14.0)};

    public RozeGraanBlock(Properties properties) {
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
        return RozegraanFeature.ZAADJES.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    /** A ripe plant glitters now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isMaxAge(state) && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.7 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }
}
