package nl.juiced.guhs.feature.mewtwo;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The kloontank of Professor Knabbelkloon: this block is the middle of its floor; the 3 x 3 x 3 around it (minus this one)
 * are invisible {@link MewtwoBlokken.Tankwand} parts. Its renderer (client.KloontankRenderer) draws the round glass tank, the
 * pink knabbelsap and, as long as YOUR questline hasn't repaired it, the cracks, a low leaking level and four little lamps
 * (red: the part is missing, green: built in). A click (on any part, with or without parts in your hands) builds in the
 * parts you carry ({@link MewtwoVerhaal#klikTank}).
 */
public class KloontankBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<KloontankBlock> CODEC = simpleCodec(KloontankBlock::new);

    public KloontankBlock(Properties p) {
        super(p);
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
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KloontankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            MewtwoVerhaal.klikTank(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Bubbles in a repaired tank, drips from a cracked one (as you see it). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (MewtwoStand.tankHeel()) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(MewtwoFeature.BUBBEL.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0, pos.getY() + 0.3 + random.nextDouble() * 0.5,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0, 0, 0.05 + random.nextDouble() * 0.03, 0);
            }
            if (random.nextInt(60) == 0) {
                level.playLocalSound(pos, MewtwoFeature.TANK_BORREL.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 0.9f + random.nextFloat() * 0.3f, false);
            }
        } else if (random.nextInt(4) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.96f, 0.5f, 0.78f), 0.8f),
                    pos.getX() + 0.5 + Math.cos(a) * 1.4, pos.getY() + 0.2 + random.nextDouble() * 1.6, pos.getZ() + 0.5 + Math.sin(a) * 1.4, 0, -0.05, 0);
        }
    }
}
