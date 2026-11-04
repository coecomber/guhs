package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;

/** The zone-3 bazen blocks (all unbreakable by hand; placed and restored through Herstel). */
public final class Z3Blokken {
    private Z3Blokken() {
    }

    /**
     * A lantern post (Pelszaal, Baron Bleu's hall): right-click lights it. In a fight the boss decides what lit means
     * ({@link Z3Baas#paalAan}); outside a fight it just glows.
     */
    public static class Lantaarnpaal extends Block {
        public static final MapCodec<Lantaarnpaal> CODEC = simpleCodec(Lantaarnpaal::new);
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 16, 12);

        public Lantaarnpaal(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer p && !state.getValue(LIT)) {
                if (Z3BazenApi.paal(sl, pos, p)) {
                    Tips.toon(p, "vadsrijk.z3bazen.tip.lantaarnpaal");
                }
            }
            return InteractionResult.SUCCESS;
        }

        public static boolean aan(BlockGetter level, BlockPos pos) {
            BlockState s = level.getBlockState(pos);
            return s.getBlock() instanceof Lantaarnpaal && s.getValue(LIT);
        }
    }

    /** The Vahoeg altar at a dungeon door: a Vahoegzegel starts the Vahoeg mode. */
    public static class Altaar extends Block {
        public static final MapCodec<Altaar> CODEC = simpleCodec(Altaar::new);
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 12, 15);

        public Altaar(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return gebruik(level, pos, player);
        }

        @Override
        protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            return gebruik(level, pos, player);
        }

        private static InteractionResult gebruik(Level level, BlockPos pos, Player player) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
                String kerker = pos.distSqr(Z3BazenPlekken.WERF_ALTAAR) < pos.distSqr(Z3BazenPlekken.HART_ALTAAR) ? "werf" : "hart";
                VahoegModus.altaar(sl, p, kerker, Vec3.atBottomCenterOf(pos));
                Tips.toon(p, "vadsrijk.z3bazen.tip.altaar");
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A lever with 4 stands (the Bewapeningsloods puzzle). */
    public static class Hendel extends Block {
        public static final MapCodec<Hendel> CODEC = simpleCodec(Hendel::new);
        public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 3);
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 12, 14);

        public Hendel(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(STAND, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(STAND);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
                sl.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1f, 0.8f);
                Z3Kerkers.hendel(sl, pos, p);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A year-ring knob with 8 stands (the Ringenzaal puzzle). */
    public static class Ringknop extends Block {
        public static final MapCodec<Ringknop> CODEC = simpleCodec(Ringknop::new);
        public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 7);

        public Ringknop(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(STAND, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(STAND);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
                sl.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1f, 0.7f);
                Z3Kerkers.ringknop(sl, pos, p);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A big nest branch in Vahoegnir's nest (cover from the crystal howl): BARST 0 whole, 1 cracked. */
    public static class Nesttak extends Block {
        public static final MapCodec<Nesttak> CODEC = simpleCodec(Nesttak::new);
        public static final IntegerProperty BARST = IntegerProperty.create("barst", 0, 1);

        public Nesttak(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(BARST, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(BARST);
        }
    }
}
