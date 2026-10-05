package nl.juiced.guhs.feature.fossielmijn;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * bbq2 (fossiel-mijn): the blocks of the Fossiel-opgraving and the Zoutkristalmijn. None of the quest blocks ever changes
 * in the world for good, so every player finds both buildings whole and has a turn of their own:
 * <ul>
 *   <li>{@link Bottenzand} and {@link Skeletrek} are drawn per player (client.TekenRenderer): the sand looks brushed empty
 *       only to whoever brushed it, the stand shows each player their own skeleton ({@link Opgraving});</li>
 *   <li>{@link Zoutader} never breaks: hacking into it gives the player salt from their own stock ({@link Zoutmijn});</li>
 *   <li>{@link Puin} breaks to the cart rail under it and falls back after a minute ({@link Herstel}).</li>
 * </ul>
 * Resources: tools/features/fossiel_mijn.py.
 */
public final class FossielmijnBlocks {
    private FossielmijnBlocks() {
    }

    /** The block entity of the per-player drawn blocks: it holds nothing, it is only there so the client can draw them. */
    public static class TekenBlockEntity extends BlockEntity {
        public TekenBlockEntity(BlockPos pos, BlockState state) {
            super(FossielmijnFeature.TEKENING.get(), pos, state);
        }
    }

    /** Shared by the two per-player drawn blocks: invisible in the world's own drawing, a block entity for the real one. */
    public abstract static class Getekend extends Block implements EntityBlock {
        protected Getekend(Properties properties) {
            super(properties);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1f;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TekenBlockEntity(pos, state);
        }
    }

    // =================================================================================================================
    // the dig
    // =================================================================================================================

    /**
     * Ash with something in it. Brush it (any brush, hold the button): {@link Opgraving#strijk}. LEEG is never set in the
     * world: the client draws it "brushed empty" for the player who brushed this spot.
     */
    public static class Bottenzand extends Getekend {
        public static final MapCodec<Bottenzand> CODEC = simpleCodec(Bottenzand::new);
        public static final BooleanProperty LEEG = BooleanProperty.create("leeg");

        public Bottenzand(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LEEG, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LEEG);
        }
    }

    /**
     * The stand of the little Tyrannoguhrus Njex. Click it with a fossil bone: {@link Opgraving#zetOpRek}. The five part
     * properties are never set in the world: the client draws the parts this player has put on it.
     */
    public static class Skeletrek extends Getekend {
        public static final MapCodec<Skeletrek> CODEC = simpleCodec(Skeletrek::new);
        public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
        /** One property per bone, in the order of {@link FossielmijnFeature#BOTTEN}. */
        public static final BooleanProperty[] DELEN = {BooleanProperty.create("schedel"), BooleanProperty.create("ruggengraat"),
                BooleanProperty.create("ribben"), BooleanProperty.create("pootjes"), BooleanProperty.create("staart")};
        private static final VoxelShape PLINT = Block.box(1, 0, 1, 15, 2, 15), RAAK = Block.box(1, 0, 1, 15, 16, 15);

        public Skeletrek(Properties properties) {
            super(properties);
            BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
            for (BooleanProperty deel : DELEN) {
                state = state.setValue(deel, false);
            }
            registerDefaultState(state);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
            builder.add(DELEN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        /** The state with the parts of this mask (bit i = bone i) on it: what the client draws for a player. */
        public static BlockState metDelen(BlockState state, int mask) {
            for (int i = 0; i < DELEN.length; i++) {
                state = state.setValue(DELEN[i], (mask & (1 << i)) != 0);
            }
            return state;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return RAAK;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return PLINT;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return useWithoutItem(state, level, pos, player, hit);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Opgraving.zetOpRek(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The reward: the Tyrannoguhrus Njex in small, on a stone plinth. A click: "Njex!". Placing it is a little FTB quest. */
    public static class Beeldje extends HorizontalDirectionalBlock {
        public static final MapCodec<Beeldje> CODEC = simpleCodec(Beeldje::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(3, 0, 3, 13, 3, 13), Block.box(5, 3, 2, 11, 13, 14));

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
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer player) {
                GuhAdvancements.grant(player, "fossiel_mijn_beeldje");
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                long nu = server.getGameTime();
                if (nu - player.getPersistentData().getLongOr("guhs_fossielmijn_njex", 0L) > 30) {
                    player.getPersistentData().putLong("guhs_fossielmijn_njex", nu);
                    server.playSound(null, pos, SoundEvents.SKELETON_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.8f);
                    player.sendOverlayMessage(Component.translatable("block.guhs.fossielmijn_fossielbeeldje.klik").withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =================================================================================================================
    // the mine
    // =================================================================================================================

    /**
     * The crystal vein. It never breaks (except for a creative player): every time a player hacks it loose they get salt
     * from their own stock ({@link Zoutmijn#hak}), on both sides the block simply stays.
     */
    public static class Zoutader extends Block {
        public static final MapCodec<Zoutader> CODEC = simpleCodec(Zoutader::new);

        public Zoutader(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest,
                                           FluidState fluid) {
            if (player.getAbilities().instabuild) {
                return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
            }
            if (player instanceof ServerPlayer sp) {
                Zoutmijn.hak(sp, pos, state, toolStack);
            }
            return false;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(12) == 0) {
                Direction kant = Direction.getRandom(random);
                if (level.getBlockState(pos.relative(kant)).isAir()) {
                    level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + kant.getStepX() * 0.6 + (random.nextDouble() - 0.5) * 0.6,
                            pos.getY() + 0.5 + kant.getStepY() * 0.6 + (random.nextDouble() - 0.5) * 0.6,
                            pos.getZ() + 0.5 + kant.getStepZ() * 0.6 + (random.nextDouble() - 0.5) * 0.6, 0, 0.005, 0);
                }
            }
        }
    }

    /**
     * Fallen rock on the cart track. Hacked away it leaves the rail it lay on (AXIS = the way the track runs), and after a
     * minute it has fallen back, for the next player; the one who cleared it has it counted ({@link Zoutmijn#puinWeg}).
     */
    public static class Puin extends Block {
        public static final MapCodec<Puin> CODEC = simpleCodec(Puin::new);
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        /** How long the track stays clear. */
        public static final int TERUG = 1200;
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 10, 16);

        public Puin(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        /** The rail that lies under this rubble. */
        public static BlockState spoor(BlockState puin) {
            return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, puin.getValue(AXIS) == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
        }

        @Override
        public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest,
                                           FluidState fluid) {
            if (player.getAbilities().instabuild) {
                return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
            }
            boolean weg = level.setBlock(pos, spoor(state), level.isClientSide() ? 11 : 3);
            if (weg && level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                Herstel.na(server, pos, state, TERUG);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12,
                        0.3, 0.2, 0.3, 0.05);
                Zoutmijn.puinWeg(sp);
            }
            return weg;
        }
    }

    /** A tuft of salt crystals: a soft light that stands on anything solid. */
    public static class Kristalletjes extends Block {
        public static final MapCodec<Kristalletjes> CODEC = simpleCodec(Kristalletjes::new);
        private static final VoxelShape VORM = Block.box(3, 0, 3, 13, 12, 13);

        public Kristalletjes(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }
    }
}
