package nl.juiced.guhs.feature.balto;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
/**
 * The blocks of Nomguh and the Sneeuwguhtoendra (resources: tools/features/balto.py + balto_tex.py):
 * <ul>
 *   <li>{@link Beeldje}: the Baltoguh-beeldje, a bronze statue of the wolf-guh on a little snowy pedestal (the reward of
 *       the questline; a click gives the famous line and a tiny howl);</li>
 *   <li>{@link Routepaal}: the red-and-white route marker pole of the trek, with a glowing guh-ear lamp on top (you see it in
 *       the storm);</li>
 *   <li>{@link Medicijnkist}: the little medicine chest from the berghut (a deco block too);</li>
 *   <li>the sneeuwguhspar: its log, the log with a sleepy guh face ({@link SparGezicht}), its needles with a snow cap
 *       ({@link SparNaalden}) and the sapling ({@link #SPAR_GROEIER}).</li>
 * </ul>
 */
public final class BaltoBlocks {
    /** A snow cap on the top needles of a sneeuwguhspar. */
    public static final BooleanProperty SNEEUW = BooleanProperty.create("sneeuw");

    /** The sapling grows the configured feature guhs:balto_sneeuwguhspar (BaltoWorldgen.Sneeuwguhspar). */
    public static final TreeGrower SPAR_GROEIER = new TreeGrower("balto_sneeuwguhspar", java.util.Optional.empty(),
            java.util.Optional.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    nl.juiced.guhs.Guhs.id("balto_sneeuwguhspar"))), java.util.Optional.empty());

    private BaltoBlocks() {
    }

    /** A block item with a grey lore line (lang {@code <block>.lore}). */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    // =================================================================================================================
    // the Baltoguh-beeldje
    // =================================================================================================================

    /** The bronze Baltoguh on his pedestal (facing the one who put it down); a click: "Maar heel misschien..." and a little howl. */
    public static class Beeldje extends HorizontalDirectionalBlock {
        public static final MapCodec<Beeldje> CODEC = simpleCodec(Beeldje::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(3, 5, 3, 13, 22, 13));

        public Beeldje(Properties properties) {
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
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                long nu = server.getGameTime();
                if (nu - player.getPersistentData().getLongOr("guhs_balto_beeldje", 0L) > 40) {
                    player.getPersistentData().putLong("guhs_balto_beeldje", nu);
                    server.playSound(null, pos, BaltoFeature.HUIL.get(), SoundSource.BLOCKS, 0.35f, 1.35f);
                    server.sendParticles(BaltoFeature.WOLFGLANS.get(), pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
                    player.sendOverlayMessage(Component.translatable("block.guhs.baltoguh_beeldje.klik").withStyle(ChatFormatting.AQUA));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =================================================================================================================
    // the route marker
    // =================================================================================================================

    /** A thin red-and-white pole with a glowing guh-ear lamp: the marked trek route through the storm. */
    public static class Routepaal extends Block {
        private static final VoxelShape VORM = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);

        public Routepaal(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(10) == 0) {
                level.addParticle(BaltoFeature.WOLFGLANS.get(), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }
    }

    // =================================================================================================================
    // the medicine chest
    // =================================================================================================================

    /** The little wooden medicine chest with a pink heart (the kist from the berghut). */
    public static class Medicijnkist extends HorizontalDirectionalBlock {
        public static final MapCodec<Medicijnkist> CODEC = simpleCodec(Medicijnkist::new);
        private static final VoxelShape NS = Block.box(2, 0, 4, 14, 9, 12), OW = Block.box(4, 0, 2, 12, 9, 14);

        public Medicijnkist(Properties properties) {
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
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : OW;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    // =================================================================================================================
    // the sneeuwguhspar
    // =================================================================================================================

    /** The trunk block with a sleepy guh face (one per tree, at eye height). */
    public static class SparGezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<SparGezicht> CODEC = simpleCodec(SparGezicht::new);

        public SparGezicht(Properties properties) {
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
    }

    /** Spruce-like needles; the top ones carry a snow cap ({@link #SNEEUW}) like the knotwilg twigs. */
    public static class SparNaalden extends LeavesBlock {
        public static final com.mojang.serialization.MapCodec<SparNaalden> CODEC = simpleCodec(SparNaalden::new);

        public SparNaalden(Properties properties) {
            super(0.0f, properties);       // (1.1.0: like 1.0.0's plain LeavesBlock: no falling leaf particles)
            registerDefaultState(defaultBlockState().setValue(SNEEUW, false));
        }

        @Override
        public com.mojang.serialization.MapCodec<? extends LeavesBlock> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(net.minecraft.world.level.Level level, BlockPos pos, RandomSource random) {
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SNEEUW);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            BlockState out = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
            if (direction == Direction.UP && out.is(this)) {
                if (neighbor.is(BlockTags.SNOW)) {
                    out = out.setValue(SNEEUW, true);
                } else if (neighbor.isFaceSturdy(level, neighborPos, Direction.DOWN)) {
                    out = out.setValue(SNEEUW, false);
                }
            }
            return out;
        }
    }

    /** The sapling (grows a small sneeuwguhspar). */
    public static class SparZaailing extends SaplingBlock {
        public SparZaailing(Properties properties) {
            super(SPAR_GROEIER, properties);
        }
    }
}
