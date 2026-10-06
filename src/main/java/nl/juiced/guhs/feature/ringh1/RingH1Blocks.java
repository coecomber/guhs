package nl.juiced.guhs.feature.ringh1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * bbq2 (ring-h1): the quest props of Guhdalf's camp. None of them has an item, a loot table or a block entity, none can be
 * broken in survival or pushed: they stand in the Knabbelgouw (protected anyway) and in the camps next to old barbecue pits
 * (where nothing else protects them). A click never changes the block: it only moves the clicking player's own story
 * ({@link Feest}), so every player can use every prop, for ever. Models and textures: tools/features/ring_h1_tex.py.
 */
public final class RingH1Blocks {
    /** A prop that answers any click of the main hand (whatever is in it). */
    abstract static class Prop extends Block {
        private final VoxelShape vorm;

        Prop(Properties properties, VoxelShape vorm) {
            super(properties);
            this.vorm = vorm;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer p) {
                klik(p, pos, state);
            }
            return InteractionResult.SUCCESS;
        }

        abstract void klik(ServerPlayer p, BlockPos pos, BlockState state);
    }

    /** Guhdalf's fireworks crate: a rocket per click. */
    public static class Vuurwerkkist extends Prop {
        public Vuurwerkkist(Properties properties) {
            super(properties, Block.box(1, 0, 1, 15, 16, 15));
        }

        @Override
        void klik(ServerPlayer p, BlockPos pos, BlockState state) {
            Feest.vuurwerk(p, pos);
        }
    }

    /** The party table with the cake that is never finished. */
    public static class Feesttafel extends Prop {
        public Feesttafel(Properties properties) {
            super(properties, Block.box(0, 0, 0, 16, 14, 16));
        }

        @Override
        void klik(ServerPlayer p, BlockPos pos, BlockState state) {
            Feest.tafel(p, pos);
        }
    }

    /** An open crate of provisions: soort 0 worst, 1 kaas, 2 knabbels. */
    public static class Proviand extends Prop {
        public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 2);

        public Proviand(Properties properties) {
            super(properties, Block.box(1, 0, 1, 15, 12, 15));
            registerDefaultState(stateDefinition.any().setValue(SOORT, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SOORT);
        }

        @Override
        void klik(ServerPlayer p, BlockPos pos, BlockState state) {
            Feest.proviand(p, pos, state.getValue(SOORT));
        }
    }

    /**
     * The chimney pot on a heuvelholletje: a wisp of smoke, always, and nothing that burns (a lit camp fire would hurt
     * whoever climbs the hill). Only looks: the smoke is made on the client.
     */
    public static class Schoorsteentje extends Block {
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 12, 12);

        public Schoorsteentje(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(5) == 0) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 0.9,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.05 + random.nextDouble() * 0.02, 0.0);
            }
        }
    }

    private RingH1Blocks() {
    }
}
