package nl.juiced.guhs.feature.guhriow1;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStuk;

/**
 * The blocks of Super Guhrio's world 1 that are more than paint: two invisible pieces of a level and the little plants of
 * the lawn.
 * <ul>
 *     <li>{@link TipBlok}: a tip. Whoever walks through it in a level they have never finished reads one line above the
 *     panel ("Spring van onderen tegen het vraagtekenblok"), once per run.</li>
 *     <li>{@link GeheimBlok}: the mark of a level's secret room. Whoever gets there has found the secret of that level,
 *     for good.</li>
 *     <li>{@link Plantje}: a flower or a tuft of grass on the lawn, a painted cross you walk right through.</li>
 * </ul>
 * Both pieces are like the spots of the engine's creatures: no shape, no model; only somebody holding the block sees and
 * clicks it. What they do is in {@link Binnentuin}.
 */
public final class GuhrioW1Blocks {
    private GuhrioW1Blocks() {
    }

    /** A piece nobody sees: no model, no shape (except for a builder who holds it). */
    private abstract static class Onzichtbaar extends Block implements GuhrioStuk {
        protected Onzichtbaar(Properties properties) {
            super(properties);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            // (only somebody holding the block sees and clicks it, like a structure void)
            return context.isHoldingItem(asItem()) ? Shapes.block() : Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }
    }

    /** A tip of a level: {@link #TIP} says which line (gui.guhs.guhriow1.tip.&lt;n&gt;). */
    public static class TipBlok extends Onzichtbaar {
        public static final MapCodec<TipBlok> CODEC = simpleCodec(TipBlok::new);
        public static final IntegerProperty TIP = IntegerProperty.create("tip", 0, 15);

        public TipBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(TIP, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TIP);
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            Binnentuin.tip(player, sessie, state.getValue(TIP));
        }
    }

    /** The mark of a level's secret room. */
    public static class GeheimBlok extends Onzichtbaar {
        public static final MapCodec<GeheimBlok> CODEC = simpleCodec(GeheimBlok::new);

        public GeheimBlok(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            Binnentuin.geheim(player, sessie);
        }
    }

    /** A painted plant of the lawn ({@link #SOORT}: red flower, yellow flower, daisy, tuft of grass): you walk through it. */
    public static class Plantje extends Block {
        public static final MapCodec<Plantje> CODEC = simpleCodec(Plantje::new);
        public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 3);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

        public Plantje(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(SOORT, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SOORT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        /** Click it with an empty hand (creative): the next kind of plant, so builders need no commands. */
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!player.isCreative()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(SOORT), 3);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
