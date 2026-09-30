package nl.juiced.guhs.feature.boerderij;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;

/**
 * Het kippennestje: a little straw nest. Content knabbelkippetjes nearby lay their knabbelei in it (up to {@link #MAX});
 * right-click to take them out.
 */
public class KippennestjeBlock extends Block {
    public static final MapCodec<KippennestjeBlock> CODEC = simpleCodec(KippennestjeBlock::new);
    public static final int MAX = 3;
    public static final IntegerProperty EIEREN = IntegerProperty.create("eieren", 0, MAX);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);

    public KippennestjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EIEREN, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EIEREN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int n = state.getValue(EIEREN);
        if (n <= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            neem(level, pos, (ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    /** The player takes the eggs out: returns how many. */
    public static int neem(Level level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        int n = state.getBlock() instanceof KippennestjeBlock ? state.getValue(EIEREN) : 0;
        if (n > 0) {
            level.setBlock(pos, state.setValue(EIEREN, 0), Block.UPDATE_ALL);
            Minigames.give(player, new ItemStack(BoerderijFeature.KNABBELEI.get(), n));
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.3f);
            BoerderijVoortgang.product(player, BoerderijVoortgang.Product.KNABBELEI, n);
        }
        return n;
    }

    /** Lays an egg in the nest: false when it's full. */
    public static boolean leg(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof KippennestjeBlock) || state.getValue(EIEREN) >= MAX) {
            return false;
        }
        level.setBlock(pos, state.setValue(EIEREN, state.getValue(EIEREN) + 1), Block.UPDATE_ALL);
        return true;
    }

    /** The nearest nest with room (r blocks sideways, 3 up and down), or null. */
    @Nullable
    public static BlockPos vindPlekje(Level level, BlockPos from, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-r, -3, -r), from.offset(r, 3, r))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof KippennestjeBlock && s.getValue(EIEREN) < MAX) {
                double d = p.distSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(EIEREN) * 5;
    }
}
