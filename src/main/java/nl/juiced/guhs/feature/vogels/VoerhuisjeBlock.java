package nl.juiced.guhs.feature.vogels;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.gids.GidsFeature;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The vogelvoerhuisje: a little pink bird table on a post. Put seeds on it (right-click with seeds: up to
 * {@link #MAX} scoops) and the birds within 16 blocks come and eat there; pluisvinkjes that eat there drop a pluisveertje now
 * and then. The birds eat the seeds slowly. Known feeders are kept per level (placing, a random tick after loading).
 */
public class VoerhuisjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<VoerhuisjeBlock> CODEC = simpleCodec(VoerhuisjeBlock::new);
    public static final int MAX = 4;
    public static final IntegerProperty VOER = IntegerProperty.create("voer", 0, MAX);
    /** The top of the table (a bird on it stands at this height in the block). */
    public static final double TAFEL_HOOGTE = 10.0 / 16.0;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(7, 0, 7, 9, 8, 9), Block.box(2, 8, 2, 14, 10, 14),
            Block.box(2, 10, 2, 14, 11, 3), Block.box(2, 10, 13, 14, 11, 14), Block.box(2, 10, 3, 3, 11, 13), Block.box(13, 10, 3, 14, 11, 13));
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BEKEND = new ConcurrentHashMap<>();

    public VoerhuisjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(VOER, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VOER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- filling it ----------------------------------------------------------------------------------------------------
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(VogelTags.ZAADJES)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (state.getValue(VOER) >= MAX) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.vogels.voerhuisje.vol"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            vul(server, pos, state);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player instanceof ServerPlayer sp) {
                GidsFeature.grant(sp, "diertjes/vogels_voerhuisje");
                sp.sendOverlayMessage(Component.translatable("gui.guhs.vogels.voerhuisje.gevuld", state.getValue(VOER) + 1, MAX));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** One scoop of seeds more (up to {@link #MAX}). */
    public static void vul(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(VOER, Math.min(MAX, state.getValue(VOER) + 1)), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1.3f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.WHEAT_SEEDS.asItem()), pos.getX() + 0.5, pos.getY() + 0.75,
                pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.02);
        onthoud(level, pos);
    }

    /** A bird pecks at the feeder: true when there was food (a scoop goes now and then). */
    public static boolean pik(ServerLevel level, BlockPos pos, Vogeltje vogel) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof VoerhuisjeBlock) || state.getValue(VOER) <= 0) {
            return false;
        }
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.WHEAT_SEEDS.asItem()), vogel.getX(), vogel.getY() + 0.2,
                vogel.getZ(), 2, 0.05, 0.05, 0.05, 0.02);
        if (vogel.getRandom().nextInt(6) == 0) {
            level.setBlock(pos, state.setValue(VOER, state.getValue(VOER) - 1), Block.UPDATE_ALL);
        }
        return true;
    }

    // --- known feeders ---------------------------------------------------------------------------------------------------
    static void onthoud(ServerLevel level, BlockPos pos) {
        BEKEND.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /** The nearest feeder with food within r blocks (checked: stale spots are dropped), or null. */
    @Nullable
    public static BlockPos dichtbij(ServerLevel level, BlockPos from, int r) {
        Set<BlockPos> set = BEKEND.get(level.dimension());
        if (set == null || set.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestD = (double) r * r;
        for (BlockPos p : List.copyOf(set)) {
            double d = p.distSqr(from);
            if (d > bestD || !level.isLoaded(p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof VoerhuisjeBlock)) {
                set.remove(p);
                continue;
            }
            if (s.getValue(VOER) > 0 && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                best = p;
                bestD = d;
            }
        }
        return best;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel server) {
            onthoud(server, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        onthoud(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            Set<BlockPos> set = BEKEND.get(server.dimension());
            if (set != null) {
                set.remove(pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** The item: a line of lore. */
    public static class Item extends BlockItem {
        public Item(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.vogels_voerhuisje.lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
