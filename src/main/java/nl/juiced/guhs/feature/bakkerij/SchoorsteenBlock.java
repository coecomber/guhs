package nl.juiced.guhs.feature.bakkerij;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The bakery's chimney pot (bakkerij_schoorsteen): a round brick pot with a little guh face that puffs knabbelwolkjes -
 * small clouds shaped like kaasknabbels - into the sky, all day long. Also a deco block for your own bakery.
 */
public class SchoorsteenBlock extends Block {
    public static final MapCodec<SchoorsteenBlock> CODEC = simpleCodec(SchoorsteenBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public SchoorsteenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
        if (random.nextInt(3) != 0) {
            level.addParticle(BakkerijFeature.KNABBELWOLKJE.get(), x + (random.nextDouble() - 0.5) * 0.2, y, z + (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.01, 0.045 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 0, 0.05, 0);
        }
    }
}
