package nl.juiced.guhs.feature.bibliotheek;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The secret bookcase: looks like a bookshelf (with one pink book sticking out a little). Right-click it and it swings
 * open, together with the secret bookcases touching it (a door of two high); after a few seconds it swings shut again,
 * but never on someone standing in it.
 */
public class GeheimeKastBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GeheimeKastBlock> CODEC = simpleCodec(GeheimeKastBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** How long it stays open (ticks). */
    public static final int OPEN_TICKS = 60;
    private static final VoxelShape NORTH = Block.box(0, 0, 0, 2, 16, 16), EAST = Block.box(0, 0, 0, 16, 16, 2),
            SOUTH = Block.box(14, 0, 0, 16, 16, 16), WEST = Block.box(0, 0, 14, 16, 16, 16);

    public GeheimeKastBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return Shapes.block();
        }
        return switch (state.getValue(FACING)) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            setOpen((ServerLevel) level, pos, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Opens (or closes) this secret bookcase and the ones touching it. */
    public static void setOpen(ServerLevel level, BlockPos pos, boolean open) {
        Set<BlockPos> door = connected(level, pos);
        for (BlockPos p : door) {
            BlockState s = level.getBlockState(p);
            if (open || nobodyIn(level, p)) {
                level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_ALL);
            }
            if (open) {
                level.scheduleTick(p, s.getBlock(), OPEN_TICKS);
            }
        }
        level.playSound(null, pos, open ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED,
                SoundSource.BLOCKS, 1f, open ? 0.7f : 0.9f);
        level.playSound(null, pos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.8f, 0.6f);
    }

    /** The secret bookcases touching this one (at most 8). */
    static Set<BlockPos> connected(Level level, BlockPos pos) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(pos);
        seen.add(pos);
        while (!todo.isEmpty() && seen.size() < 8) {
            BlockPos p = todo.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).getBlock() instanceof GeheimeKastBlock) {
                    seen.add(n);
                    todo.add(n);
                }
            }
        }
        return seen;
    }

    private static boolean nobodyIn(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty();
    }

    /** Time's up: swing shut (unless someone is standing in the doorway: then a bit later). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) {
            return;
        }
        if (nobodyIn(level, pos)) {
            level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.6f);
        } else {
            level.scheduleTick(pos, this, 20);
        }
    }
}
