package nl.juiced.guhs.feature.guhriobeloning;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The flagpole to build with (a reward of Super Guhrio, sold by Pad-guh): stack the blocks and they make one pole by
 * themselves: the lowest one gets a stone foot, the highest one the golden ball and the pink guh flag ({@link #DEEL}).
 * The flag hangs to the side the builder looked from ({@link #FACING}, the whole pole follows the block it is put on).
 * Click the pole anywhere and the flag at the top celebrates: stars and a little fanfare. Nothing else: the flagpole
 * that ends a level is the engine's own piece.
 */
public class VlaggenmastBlock extends Block {
    /** Which part of the pole this block is. */
    public enum Deel implements StringRepresentable {
        VOET, PAAL, TOP, LOS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** voet = the lowest block, top = the highest (ball + flag), paal = between; los = one block alone (foot and flag). */
    public static final EnumProperty<Deel> DEEL = EnumProperty.create("deel", Deel.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape PAAL = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
    private static final VoxelShape VOET = Shapes.or(Block.box(3, 0, 3, 13, 4, 13), PAAL);

    public VlaggenmastBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEEL, Deel.LOS).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEEL, FACING);
    }

    private static Deel deel(boolean onder, boolean boven) {
        return onder && boven ? Deel.PAAL : boven ? Deel.VOET : onder ? Deel.TOP : Deel.LOS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState onder = level.getBlockState(pos.below()), boven = level.getBlockState(pos.above());
        // a block added to a pole looks the way the pole looks; a new pole looks at its builder
        Direction kijkt = onder.is(this) ? onder.getValue(FACING) : boven.is(this) ? boven.getValue(FACING) : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, kijkt).setValue(DEEL, deel(onder.is(this), boven.is(this)));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour.getAxis() != Direction.Axis.Y) {
            return state;
        }
        return state.setValue(DEEL, deel(level.getBlockState(pos.below()).is(this), level.getBlockState(pos.above()).is(this)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Deel deel = state.getValue(DEEL);
        return deel == Deel.VOET || deel == Deel.LOS ? VOET : PAAL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** The top block of the pole this block belongs to. */
    public static BlockPos top(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < 64 && level.getBlockState(p.above()).getBlock() instanceof VlaggenmastBlock; i++) {
            p.move(Direction.UP);
        }
        return p.immutable();
    }

    /** A click anywhere on the pole: the flag celebrates. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            BlockPos top = top(level, pos);
            server.sendParticles(ParticleTypes.FIREWORK, top.getX() + 0.5, top.getY() + 0.9, top.getZ() + 0.5, 24, 0.25, 0.25, 0.25, 0.08);
            server.sendParticles(ParticleTypes.WAX_ON, top.getX() + 0.5, top.getY() + 0.6, top.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0.0);
            level.playSound(null, top, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
            level.playSound(null, top, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.5f);
            level.playSound(null, top, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 0.5f, 1.3f);
            if (player instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.mast.vahoeg").withStyle(ChatFormatting.GREEN));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
