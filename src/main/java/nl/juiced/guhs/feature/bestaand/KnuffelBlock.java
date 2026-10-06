package nl.juiced.guhs.feature.bestaand;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A plush deco block of the Knuffelmaker-guh (Knuffelguh, Knuffel-Mika, Knuffel-Rookguh): a sitting plush that looks at
 * whoever puts it down. Squeeze it (right-click) and it squeaks, each in its own pitch ({@code toon}), with a little heart.
 */
public class KnuffelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KnuffelBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(propertiesCodec(),
            com.mojang.serialization.Codec.FLOAT.fieldOf("toon").forGetter(b -> b.toon)).apply(i, KnuffelBlock::new));
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);
    private final float toon;

    public KnuffelBlock(Properties properties, float toon) {
        super(properties);
        this.toon = toon;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** A squeeze: a squeak and a heart. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.BLOCKS, 0.8f, toon + level.getRandom().nextFloat() * 0.1f);
            server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}
