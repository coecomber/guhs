package nl.juiced.guhs.feature.ringh3;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * bbq2 (ring-h3): the three blocks of the mine (models and textures: tools/features/ring_h3.py). None of them can be broken
 * (they belong to a protected building and are quest props), none has a recipe.
 */
public final class RingH3Blocks {
    private RingH3Blocks() {
    }

    /**
     * Brokkelsteen: the stone of the Brokkelpad over the chasm. It carries you for {@link #BARST_TICKS} ticks after a player
     * steps on it (it cracks and groans), then it is gone for {@link #WEG_TICKS} ticks and comes back by itself: so the path
     * is whole again for whoever comes next, and nobody waits for anybody. Falling costs nothing (MijnEvents puts you back at
     * your rest fire).
     */
    public static class Brokkelsteen extends Block {
        public static final MapCodec<Brokkelsteen> CODEC = simpleCodec(Brokkelsteen::new);
        /** 0 whole, 1 cracking under somebody, 2 gone. */
        public static final IntegerProperty STAAT = IntegerProperty.create("staat", 0, 2);
        public static final int BARST_TICKS = 14, WEG_TICKS = 90;

        public Brokkelsteen(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STAAT, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAAT);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (!level.isClientSide() && entity instanceof Player player && !player.isSpectator() && state.getValue(STAAT) == 0) {
                barst((ServerLevel) level, pos, state);
            }
            super.stepOn(level, pos, state, entity);
        }

        /** Starts to crack (what a player's foot does). */
        public static void barst(ServerLevel level, BlockPos pos, BlockState state) {
            level.setBlock(pos, state.setValue(STAAT, 1), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), BARST_TICKS);
            level.playSound(null, pos, RingH3Feature.BROKKEL.get(), SoundSource.BLOCKS, 0.7f, 0.8f + level.getRandom().nextFloat() * 0.3f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.05, 0.3, 0.0);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int staat = state.getValue(STAAT);
            if (staat == 1) {
                level.setBlock(pos, state.setValue(STAAT, 2), Block.UPDATE_ALL);
                level.scheduleTick(pos, this, WEG_TICKS);
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.9f, 0.6f);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.02);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
            } else if (staat == 2) {
                // (it only comes back when nobody stands in its place)
                if (!level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
                    level.scheduleTick(pos, this, 10);
                    return;
                }
                level.setBlock(pos, state.setValue(STAAT, 0), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5f, 0.7f);
            }
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(STAAT) == 2 ? Shapes.empty() : Shapes.block();
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(STAAT) == 2 ? Shapes.empty() : Shapes.block();
        }

        @Override
        protected VoxelShape getOcclusionShape(BlockState state) {
            return Shapes.empty();
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return state.getValue(STAAT) == 2 ? RenderShape.INVISIBLE : RenderShape.MODEL;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return state.getValue(STAAT) == 2;
        }
    }

    /**
     * A dwarf-guh lever of the Hal van de Hefbomen: a click pulls it (it springs back by itself) and tells {@link Raadsels}
     * which one ({@link #NR}) which player pulled: the order is that player's own.
     */
    public static class Hendel extends HorizontalDirectionalBlock {
        public static final MapCodec<Hendel> CODEC = simpleCodec(Hendel::new);
        public static final IntegerProperty NR = IntegerProperty.create("nr", 0, 3);
        public static final BooleanProperty OM = BooleanProperty.create("om");
        public static final int TERUG_TICKS = 16;

        public Hendel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.SOUTH).setValue(NR, 0).setValue(OM, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, NR, OM);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case NORTH -> Block.box(4, 2, 6, 12, 14, 16);
                case EAST -> Block.box(0, 2, 4, 10, 14, 12);
                case WEST -> Block.box(6, 2, 4, 16, 14, 12);
                default -> Block.box(4, 2, 0, 12, 14, 10);
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                if (!state.getValue(OM)) {
                    level.setBlock(pos, state.setValue(OM, true), Block.UPDATE_ALL);
                    level.scheduleTick(pos, this, TERUG_TICKS);
                }
                Raadsels.hendel(p, pos, state.getValue(NR));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(OM)) {
                level.setBlock(pos, state.setValue(OM, false), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, 0.5f);
            }
        }
    }

    /**
     * A rune stone: a block with one of eight carved signs ({@link #TEKEN}: kaas, worst, saus, knabbel, bot, vlam, the glowing
     * "njeg" of the gate's inscription, a drum). A click knocks on it; what that does depends on where it is
     * ({@link Raadsels#rune}): the inscription of the west gate, the six stones round Gimguh's door, or just a carving.
     */
    public static class Rune extends Block {
        public static final MapCodec<Rune> CODEC = simpleCodec(Rune::new);
        public static final IntegerProperty TEKEN = IntegerProperty.create("teken", 0, 7);

        public Rune(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(TEKEN, Plekken.RUNE_NJEG));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TEKEN);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                Raadsels.rune(p, pos, state.getValue(TEKEN));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(TEKEN) == Plekken.RUNE_NJEG && random.nextInt(5) == 0) {
                net.minecraft.core.Direction d = net.minecraft.core.Direction.getRandom(random);
                if (d.getAxis().isHorizontal() && level.getBlockState(pos.relative(d)).isAir()) {
                    level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + d.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.6,
                            pos.getY() + random.nextDouble(), pos.getZ() + 0.5 + d.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.6, 0, 0.004, 0);
                }
            }
        }
    }
}
