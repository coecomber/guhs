package nl.juiced.guhs.feature.droomwezens;

import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Het glas met een restje (1.8.0, bouwplan C): the Rommelguh's trademark, 1-3 glasses ({@link #GLAZEN}) with a little drink left
 * in them ({@link #DRANKJE}: sap, cola, limo, melk or thee; a placed glass gets a random one, like real life).
 * <ul>
 *   <li>Right-click with another glass: one more glass (up to 3, like candles).</li>
 *   <li>Right-click with an empty hand: a sip (a slurp, a joke). Of course a restje stays: it never runs out.</li>
 * </ul>
 */
public class GlasRestjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GlasRestjeBlock> CODEC = simpleCodec(GlasRestjeBlock::new);
    public static final int MAX = 3;
    public static final IntegerProperty GLAZEN = IntegerProperty.create("glazen", 1, MAX);
    public static final EnumProperty<Drankje> DRANKJE = EnumProperty.create("drankje", Drankje.class);
    private static final VoxelShape[] VORM = {Block.box(6, 0, 6, 10, 5, 10), Block.box(4, 0, 4, 12, 5, 12), Block.box(3, 0, 3, 13, 5, 13)};

    public enum Drankje implements StringRepresentable {
        SAP, COLA, LIMO, MELK, THEE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public GlasRestjeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GLAZEN, 1).setValue(DRANKJE, Drankje.SAP));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GLAZEN, DRANKJE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM[state.getValue(GLAZEN) - 1];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState there = context.getLevel().getBlockState(context.getClickedPos());
        if (there.is(this)) {
            return there.setValue(GLAZEN, Math.min(MAX, there.getValue(GLAZEN) + 1));
        }
        Drankje[] alle = Drankje.values();
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(DRANKJE, alle[context.getLevel().getRandom().nextInt(alle.length)]);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(GLAZEN) < MAX
                || super.canBeReplaced(state, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbourPos,
                                     BlockState neighbour, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        slokje((ServerLevel) level, pos, state, (ServerPlayer) player);
        return InteractionResult.CONSUME;
    }

    /** A sip: a slurp, a little splash and one of three jokes (the glass stays: there's always a restje). */
    public static void slokje(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        level.playSound(null, pos, DroomwezensFeature.SLOKJE.get(), SoundSource.PLAYERS, 0.7f, 1f);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, 4, 0.12, 0.05, 0.12, 0.02);
        int i = level.getRandom().nextInt(3);
        Component drank = Component.translatable("gui.guhs.glas_restje.drankje." + state.getValue(DRANKJE).getSerializedName());
        player.sendOverlayMessage(switch (i) {
            case 0 -> Component.translatable("gui.guhs.glas_restje.slokje");
            case 1 -> Component.translatable("gui.guhs.glas_restje.slokje2");
            default -> Component.translatable("gui.guhs.glas_restje.slokje3", drank);
        });
    }

    /** The item: places a glass (or adds one), with its two lore lines. */
    public static class GlasItem extends BlockItem {
        public GlasItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.glas_restje.lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("block.guhs.glas_restje.lore2").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
