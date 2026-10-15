package nl.juiced.guhs.feature.guhriobeloning;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The green pipe to build with (a reward of Super Guhrio, sold by Pad-guh): the pipe that really works. Stack the blocks
 * for a taller pipe: the highest one is the mouth ({@link #MOND}), the ones under it the body. Stand on a mouth and sneak
 * (or click the pipe while you stand on it) and you slide down into it and come out of the nearest other mouth of the
 * SAME COLOUR within {@link Pijpreis#BEREIK} blocks ({@link Pijpreis}). Two green pipes are a pair out of the box; a dye
 * on a pipe paints the whole pipe ({@link #KLEUR}), so more pairs can stand close together.
 * <p>
 * It is an ordinary full block (you stand on it, things can stand on it); the pipes of a level are the engine's own
 * pieces and have nothing to do with this one.
 */
public class PijpBlock extends Block {
    public static final EnumProperty<DyeColor> KLEUR = EnumProperty.create("kleur", DyeColor.class);
    /** The top block of a pipe: where you go in and come out. */
    public static final BooleanProperty MOND = BooleanProperty.create("mond");

    public PijpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KLEUR, DyeColor.GREEN).setValue(MOND, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KLEUR, MOND);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState onder = level.getBlockState(pos.below()), boven = level.getBlockState(pos.above());
        // a block added to a pipe takes the pipe's colour
        DyeColor kleur = onder.is(this) ? onder.getValue(KLEUR) : boven.is(this) ? boven.getValue(KLEUR) : DyeColor.GREEN;
        return defaultBlockState().setValue(KLEUR, kleur).setValue(MOND, !boven.is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return directionToNeighbour == Direction.UP ? state.setValue(MOND, !neighbourState.is(this)) : state;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** The mouth of the pipe this block belongs to (the highest pipe block of its column). */
    public static BlockPos mond(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < 64 && level.getBlockState(p.above()).getBlock() instanceof PijpBlock; i++) {
            p.move(Direction.UP);
        }
        return p.immutable();
    }

    /** A dye paints the whole pipe (every pipe block of the column). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        DyeColor kleur = stack.get(DataComponents.DYE);
        if (kleur == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (kleur == state.getValue(KLEUR)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (Bescherming.beschermd(level, pos) && !Bescherming.mag(player, pos)) {
                // (a pipe of a protected building, like the pair on the castle's forecourt, keeps its colour)
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.wereld.beschermd").withStyle(ChatFormatting.YELLOW));
                }
                return InteractionResult.SUCCESS;
            }
            BlockPos.MutableBlockPos p = mond(level, pos).mutable();
            for (int i = 0; i < 128 && level.getBlockState(p).is(this); i++) {
                level.setBlock(p, level.getBlockState(p).setValue(KLEUR, kleur), Block.UPDATE_ALL);
                p.move(Direction.DOWN);
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            stack.consume(1, player);
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.pijp.geverfd").withStyle(ChatFormatting.GREEN));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A click with an empty hand while you stand on the pipe: in you go. Anywhere else: how it works. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            BlockPos mond = mond(level, pos);
            if (mond.equals(Pijpreis.onder(p))) {
                Pijpreis.probeer(p, mond, true);
            } else {
                p.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.pijp.hint").withStyle(ChatFormatting.GREEN));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
