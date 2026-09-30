package nl.juiced.guhs.feature.wereldleven;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;

/**
 * A guh plushie (knuffel_&lt;variant&gt;, 2.8 wereldleven): a little soft guh to put down (it looks at you). Squeeze it
 * (right-click) and it squeaks; tamed guhs come and cuddle it (Knuffels). The glitter one sparkles.
 */
public class KnuffelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KnuffelBlock> CODEC = simpleCodec(KnuffelBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public KnuffelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.BLOCKS, 0.7f, 1.6f + level.random.nextFloat() * 0.3f);
            server.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this == WereldlevenFeature.knuffel(WereldlevenFeature.GLITTER) && random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 0.6f), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.1 + random.nextDouble() * 0.8, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }
}
