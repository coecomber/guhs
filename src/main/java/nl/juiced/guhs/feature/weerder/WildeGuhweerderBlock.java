package nl.juiced.guhs.feature.weerder;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Wilde-guhweerder (1.2.0): a little pink sign on a post with two guh ears and a sleeping guh face ("hier woont al een
 * vadsje"). No wild guhs spawn in the area around it ({@link WeerderFeature}). Right-click: its screen (the radius, the blue
 * dome); for someone else's weerder the screen is read-only. Only its owner (or an op) breaks it.
 */
public class WildeGuhweerderBlock extends BaseEntityBlock {
    public static final MapCodec<WildeGuhweerderBlock> CODEC = simpleCodec(WildeGuhweerderBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape NOORD_ZUID = Shapes.or(Block.box(5, 0, 5, 11, 1, 11), Block.box(7, 1, 7, 9, 6, 9),
            Block.box(1, 6, 7, 15, 14, 9), Block.box(2.5, 14, 7.5, 13.5, 16, 8.5));
    private static final VoxelShape OOST_WEST = Shapes.or(Block.box(5, 0, 5, 11, 1, 11), Block.box(7, 1, 7, 9, 6, 9),
            Block.box(7, 6, 1, 9, 14, 15), Block.box(7.5, 14, 2.5, 8.5, 16, 13.5));

    public WildeGuhweerderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NOORD_ZUID : OOST_WEST;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeerderBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof WeerderBlockEntity be) {
            if (placer instanceof ServerPlayer player) {
                be.zetEigenaar(player);
                player.sendSystemMessage(Component.translatable("gui.guhs.weerder.geplaatst", be.straal()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            WeerderIndex.zet(sl, pos, be.straal());
        }
    }

    /** Only the owner (or an op) can break it: for anyone else it doesn't even crack (BreakBlockEvent is cancelled too). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WeerderBlockEntity be && !be.magBewerken(player) ? 0f
                : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof WeerderBlockEntity be) {
            if (be.eigenaar() == null) {
                be.zetEigenaar(sp);   // (an unowned weerder, e.g. from a command, is claimed by the first one to use it)
            }
            WeerderPayloads.open(sp, be);
        }
        return InteractionResult.SUCCESS;
    }

    /** Now and then a tiny "zzz" from the sleeping guh on the sign. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) == 0) {
            level.addParticle(nl.juiced.guhs.feature.emotes.EmotesFeature.GUH_ZZZ.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
                    pos.getY() + 1.05, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
        }
    }
}
