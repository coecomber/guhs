package nl.juiced.guhs.feature.guhriow2;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStuk;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;

/**
 * The pieces world 2 adds to Super Guhrio (the engine finds them in a lane like its own: {@link GuhrioStuk}). None of them
 * ever changes in the world; what they are for a player lives in that player's session or saved data.
 * <ul>
 *     <li>{@link WarpPijp}: the mouth of a warp pipe. Duck on it (S) and you are in the level its
 *     {@link GuhrioBlocks.PijpBlok#KANAAL} stands for ({@link GuhrioW2#WARP}), which is open for you from then on.</li>
 *     <li>{@link EiSlot}: the lock of the egg gate. It switches its channel on for whoever has Guhshi's egg (the gate
 *     itself is made of the engine's red switched blocks of that channel: solid until the channel is on).</li>
 *     <li>{@link Broedplek}: where a player who carries the egg lays it in the nest: Guhshi hatches (once per player).</li>
 *     <li>{@link Nest}: the warm nest itself, a plain block that marks where the hatching is staged.</li>
 * </ul>
 */
public final class GuhrioW2Blocks {
    private GuhrioW2Blocks() {
    }

    /**
     * A warp pipe's mouth: a green pipe's mouth in every way the player's game cares about (you stand on it, S ducks),
     * but it has no partner in its level: it leads to another level of the castle.
     */
    public static class WarpPijp extends GuhrioBlocks.PijpBlok {
        public static final MapCodec<WarpPijp> CODEC = simpleCodec(WarpPijp::new);

        public WarpPijp(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void duik(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            // (you must really stand on it)
            Vec3 sta = buiten(player.level(), pos, state);
            if (Math.abs(player.getY() - sta.y) > 0.6 || Math.abs(player.getX() - sta.x) > 0.8 || Math.abs(player.getZ() - sta.z) > 0.8) {
                return;
            }
            GuhrioW2.warp(player, sessie, pos, state.getValue(KANAAL));
        }
    }

    /**
     * The lock of the egg gate: a plain block with an egg-shaped keyhole. Its {@link GuhrioStukken#KANAAL} is the switch
     * channel of the gate; {@link GuhrioW2#tick} switches it on for a player the moment they have Guhshi's egg, and a
     * player who comes in with the egg finds the gate open from the start.
     */
    public static class EiSlot extends Block implements GuhrioStuk {
        public static final MapCodec<EiSlot> CODEC = simpleCodec(EiSlot::new);

        public EiSlot(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(GuhrioStukken.KANAAL, 7));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GuhrioStukken.KANAAL);
        }

        @Override
        public int begin(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (nl.juiced.guhs.feature.guhrio.GuhrioKasteel.heeftEi(player)) {
                sessie.kanalen |= 1 << state.getValue(GuhrioStukken.KANAAL);      // (told to the player's game with the level itself)
            }
            return 0;
        }
    }

    /**
     * Where the egg is laid in the nest: invisible, you walk through it (stack a few in the passage to the nest, so nobody
     * hops over it). A player who carries the egg and never hatched it sees Guhshi crawl out ({@link GuhrioW2#broed}).
     * Until then the engine's Guhshi spots of the level are empty for that player: there is no Guhshi yet.
     */
    public static class Broedplek extends Block implements GuhrioStuk {
        public static final MapCodec<Broedplek> CODEC = simpleCodec(Broedplek::new);

        public Broedplek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
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

        @Override
        public int begin(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (!GuhrioW2.uitgebroed(player)) {
                for (GuhrioSpel.Stuk stuk : sessie.actief.stukken) {
                    if (stuk.state().getBlock() instanceof GuhrioStukken.GuhshiPlek) {
                        sessie.staat.put(stuk.pos(), 1);                       // (1: nobody waits there)
                    }
                }
            }
            return 0;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            GuhrioW2.broed(player, sessie, pos);
        }
    }

    /** The nest: a little stove with a bed of straw on it. A plain block; as a piece it only says where the hatching is. */
    public static class Nest extends Block implements GuhrioStuk {
        public static final MapCodec<Nest> CODEC = simpleCodec(Nest::new);

        public Nest(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }
}
