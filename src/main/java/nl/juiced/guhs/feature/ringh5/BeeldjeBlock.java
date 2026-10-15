package nl.juiced.guhs.feature.ringh5;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h5): the statuette of the Oog van Sausron (the fixed id {@code oog_van_sausron_beeldje}; an end reward of the
 * Knabbelring, handed out by ring-kern): the little tower with the Mika head and its one burning eye. The eye blinks all
 * by itself (an animated texture: tools/features/ring_h5.py) and gives a little light. Pat it (right-click): it purrs, a
 * few embers, and above the hotbar one of its thoughts ("hij heeft gewoon trek").
 */
public class BeeldjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BeeldjeBlock> CODEC = simpleCodec(BeeldjeBlock::new);
    /** How many different thoughts it has (gui.guhs.ringh5.beeldje.&lt;n&gt;). */
    public static final int GEDACHTEN = 5;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public BeeldjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            level.playSound(null, pos, ModSounds.MIKA_AMBIENT.get(), SoundSource.BLOCKS, 0.6f, 0.5f);
            server.sendParticles(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 4, 0.15, 0.1, 0.15, 0.005);
            player.sendOverlayMessage(Component.translatable("gui.guhs.ringh5.beeldje." + level.getRandom().nextInt(GEDACHTEN)).withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0.01, 0);
        }
    }
}
