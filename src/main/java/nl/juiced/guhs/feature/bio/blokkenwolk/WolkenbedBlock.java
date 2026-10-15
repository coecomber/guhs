package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The wolkenbed: a real bed made of cloud. Two blocks long like vanilla's (foot and head, FACING points from the foot to the
 * head), you sleep in it at night, it skips the night and it becomes your spawn point, all through the same rules as a
 * vanilla bed (the dimension's bed rule, monsters nearby, "too far away").
 * <p>
 * It is NOT a {@link BedBlock}: no block entity and no bed renderer, a plain model. NeoForge's bed hooks ({@link #isBed},
 * {@link #getBedDirection}, {@link #getRespawnPosition}) make it one for sleeping and respawning; it has vanilla's three bed
 * properties, so everything that reads a bed's state (the block tag minecraft:beds) can read this one.
 * <p>
 * Guhs are sweet: where a vanilla bed would explode (the Barbecuether, the Guhmaag) this one only says you can't sleep here.
 * Landing on it never hurts, and it does not bounce.
 */
public class WolkenbedBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WolkenbedBlock> CODEC = simpleCodec(WolkenbedBlock::new);
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 9, 16);

    public WolkenbedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Where the other half of this half is. */
    private static Direction naarAndereHelft(BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
    }

    // --- two blocks that belong together (as vanilla's bed) ------------------------------------------------------------

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos hoofd = context.getClickedPos().relative(facing);
        Level level = context.getLevel();
        return level.getBlockState(hoofd).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(hoofd)
                ? defaultBlockState().setValue(FACING, facing) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, BedPart.HEAD), 3);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction == naarAndereHelft(state)) {
            return neighbour.is(this) && neighbour.getValue(PART) != state.getValue(PART)
                    ? state.setValue(OCCUPIED, neighbour.getValue(OCCUPIED)) : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    /** Broken in creative from the foot end: the head goes without dropping a second bed (the head is the half that drops). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(PART) == BedPart.FOOT) {
            BlockPos hoofd = pos.relative(state.getValue(FACING));
            BlockState daar = level.getBlockState(hoofd);
            if (daar.is(this) && daar.getValue(PART) == BedPart.HEAD) {
                level.setBlock(hoofd, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, hoofd, Block.getId(daar));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // --- sleeping ------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.getValue(PART) != BedPart.HEAD) {
            pos = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) {
                return InteractionResult.CONSUME;
            }
        }
        BedRule regel = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        if (regel.explodes()) {
            // a vanilla bed blows up here; a cloud just stays a cloud
            player.sendOverlayMessage(Component.translatable("block.guhs.wolkenbed.hier_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (state.getValue(OCCUPIED)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.bed.occupied"));
        } else {
            player.startSleepInBed(pos).ifLeft(probleem -> {
                if (probleem.message() != null) {
                    player.sendOverlayMessage(probleem.message());
                }
            });
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) {
        return true;
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    /** Respawning at the bed: beside it, as with a vanilla bed (and only where the dimension lets a bed be a spawn point). */
    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader levelReader, BlockPos pos,
                                                                     float orientation) {
        if (levelReader instanceof Level level && !level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).canSetSpawn(level)) {
            return Optional.empty();
        }
        return BedBlock.findStandUpPosition(type, levelReader, pos, state.getValue(FACING), orientation)
                .map(plek -> ServerPlayer.RespawnPosAngle.of(plek, pos, 0.0f));
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        WolkenBlokken.zachtLanden(level, pos, entity, fallDistance);
    }
}
