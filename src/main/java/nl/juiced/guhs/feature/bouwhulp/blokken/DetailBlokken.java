package nl.juiced.guhs.feature.bouwhulp.blokken;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.vogels.VogelsFeature;

/**
 * F11 walls, details, garden and street (design 2.7): the blocks that do a little something.
 * <ul>
 *   <li>{@link Behang}: wallpaper; the bottom row gets a skirting board by itself;</li>
 *   <li>{@link Knuffelblok}: plush, you bounce a little and it squeaks (sneak: no bounce);</li>
 *   <li>{@link Kozijn}: a guh window frame, right-click: ears / window box / curtains / plain;</li>
 *   <li>{@link Luik}: a shutter with a heart, right-click opens it like a door;</li>
 *   <li>{@link Schoorsteen}: a guh chimney that smokes, right-click: the fire out or on again;</li>
 *   <li>{@link Lantaarnpaal}: two blocks high, the lamp under a guh cap;</li>
 *   <li>{@link Detail}: shaped decorations that just face a side (gutter, drainpipe, dormer, doormat), {@link Vogelhuisje}
 *       (now and then a bird sings in it).</li>
 * </ul>
 */
public final class DetailBlokken {

    public static class Behang extends Block {
        public static final MapCodec<Behang> CODEC = simpleCodec(Behang::new);
        public static final BooleanProperty PLINT = BooleanProperty.create("plint");

        public Behang(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(PLINT, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PLINT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(PLINT, !(context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof Behang));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return direction == Direction.DOWN ? state.setValue(PLINT, !(neighbor.getBlock() instanceof Behang)) : state;
        }
    }

    public static class Knuffelblok extends Block {
        public static final MapCodec<Knuffelblok> CODEC = simpleCodec(Knuffelblok::new);

        public Knuffelblok(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            entity.causeFallDamage(fallDistance, 0.2f, level.damageSources().fall());
            if (fallDistance > 0.4 && !level.isClientSide()) {
                level.playSound(null, pos, KnuffelbadFeature.EENDJE_PIEP.get(), SoundSource.BLOCKS, 0.7f, 1.1f + level.getRandom().nextFloat() * 0.3f);
            }
        }

        @Override
        public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
            Vec3 v = entity.getDeltaMovement();
            if (entity.isSuppressingBounce() || v.y > -0.25) {
                super.updateEntityMovementAfterFallOn(level, entity);
            } else {
                entity.setDeltaMovement(v.x, -v.y * 0.5, v.z);
            }
        }
    }

    public enum Versiering implements StringRepresentable {
        KAAL, OORTJES, BLOEMBAK, GORDIJN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A guh window frame (facing = outside, where the window box hangs). */
    public static class Kozijn extends HorizontalDirectionalBlock {
        public static final MapCodec<Kozijn> CODEC = simpleCodec(Kozijn::new);
        public static final EnumProperty<Versiering> DECOR = EnumProperty.create("decor", Versiering.class);
        static final Map<Direction, VoxelShape> GEWOON = Vorm.draai(new double[]{0, 0, 6, 16, 16, 10}, new double[]{0, 0, 3.5, 16, 1.5, 6});
        static final Map<Direction, VoxelShape> MET_BAK = Vorm.draai(new double[]{0, 0, 6, 16, 16, 10}, new double[]{0, 0, 3.5, 16, 1.5, 6},
                new double[]{1, 1.5, 1, 15, 5, 5.5});

        public Kozijn(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DECOR, Versiering.OORTJES));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, DECOR);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return (state.getValue(DECOR) == Versiering.BLOEMBAK ? MET_BAK : GEWOON).get(state.getValue(FACING));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                Versiering[] v = Versiering.values();
                level.setBlock(pos, state.setValue(DECOR, v[(state.getValue(DECOR).ordinal() + 1) % v.length]), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public enum Kant implements StringRepresentable {
        LINKS, RECHTS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A shutter (facing = outward from the wall it hangs on; kant = its hinge, rechts = the facing's clockwise side). */
    public static class Luik extends HorizontalDirectionalBlock {
        public static final MapCodec<Luik> CODEC = simpleCodec(Luik::new);
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
        public static final EnumProperty<Kant> KANT = EnumProperty.create("kant", Kant.class);
        static final Map<Direction, VoxelShape> DICHT = Vorm.draai(new double[]{0, 0, 13, 16, 16, 16});
        static final Map<Direction, VoxelShape> OPEN_RECHTS = Vorm.draai(new double[]{13, 0, 0, 16, 16, 16});
        static final Map<Direction, VoxelShape> OPEN_LINKS = Vorm.draai(new double[]{0, 0, 0, 3, 16, 16});

        public Luik(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(KANT, Kant.LINKS));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, OPEN, KANT);
        }

        /** 1.7.0: in a mirrored structure the hinge goes to the other side too (like a vanilla door). */
        @Override
        protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
            BlockState s = super.mirror(state, mirror);
            return mirror == net.minecraft.world.level.block.Mirror.NONE ? s : s.setValue(KANT, s.getValue(KANT) == Kant.LINKS ? Kant.RECHTS : Kant.LINKS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction f = Vorm.tegenMuur(context);
            Vec3 k = Vorm.klik(context);
            Direction cw = f.getClockWise();
            double rechts = (k.x - 0.5) * cw.getStepX() + (k.z - 0.5) * cw.getStepZ();
            return defaultBlockState().setValue(FACING, f).setValue(KANT, rechts > 0 ? Kant.RECHTS : Kant.LINKS);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            Direction f = state.getValue(FACING);
            if (!state.getValue(OPEN)) {
                return DICHT.get(f);
            }
            return (state.getValue(KANT) == Kant.RECHTS ? OPEN_RECHTS : OPEN_LINKS).get(f);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                boolean open = !state.getValue(OPEN);
                level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
                level.playSound(null, pos, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8f, 1.2f);
                level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Schoorsteen extends Block {
        public static final MapCodec<Schoorsteen> CODEC = simpleCodec(Schoorsteen::new);
        public static final BooleanProperty ROOKT = BooleanProperty.create("rookt");
        private static final VoxelShape VORM = Shapes.or(Block.box(2, 0, 2, 14, 13, 14), Block.box(1, 13, 1, 15, 16, 15));

        public Schoorsteen(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(ROOKT, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ROOKT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                boolean aan = !state.getValue(ROOKT);
                level.setBlock(pos, state.setValue(ROOKT, aan), Block.UPDATE_ALL);
                level.playSound(null, pos, aan ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(ROOKT) && random.nextInt(3) == 0) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                        pos.getY() + 1.0, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
            }
        }
    }

    /** Two blocks high (like a door): the post below, the lamp with its guh cap above. */
    public static class Lantaarnpaal extends Block {
        public static final MapCodec<Lantaarnpaal> CODEC = simpleCodec(Lantaarnpaal::new);
        public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
        private static final VoxelShape ONDER = Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 16, 9.5), Block.box(4.5, 0, 4.5, 11.5, 3.5, 11.5));
        private static final VoxelShape BOVEN = Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 6, 9.5), Block.box(3, 6, 3, 13, 16, 13));

        public Lantaarnpaal(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HALF);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? ONDER : BOVEN;
        }

        @Override
        @Nullable
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockPos p = context.getClickedPos();
            Level level = context.getLevel();
            return p.getY() < level.getMaxY() && level.getBlockState(p.above()).canBeReplaced(context) ? defaultBlockState() : null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }

        /** Place a whole lamppost (lower half at pos), as the item does. */
        public void zet(Level level, BlockPos pos) {
            level.setBlock(pos, defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
                return neighbor.is(this) && neighbor.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
            }
            return state;
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                BlockPos onder = pos.below();
                BlockState o = level.getBlockState(onder);
                if (o.is(this) && o.getValue(HALF) == DoubleBlockHalf.LOWER) {   // creative: no drop from the post below
                    level.setBlock(onder, o.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, onder, Block.getId(o));
                }
            }
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    /** A decoration with a shape that faces a side ({@code tegenMuur}: hangs against the clicked wall; else faces the player). */
    public static class Detail extends HorizontalDirectionalBlock {
        private final boolean tegenMuur;
        private final double[][] boxes;
        private final Map<Direction, VoxelShape> vorm;

        public Detail(Properties properties, boolean tegenMuur, double[]... boxes) {
            super(properties);
            this.tegenMuur = tegenMuur;
            this.boxes = boxes;
            this.vorm = Vorm.draai(boxes);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(p -> new Detail(p, tegenMuur, boxes));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, tegenMuur ? Vorm.tegenMuur(context) : context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm.get(state.getValue(FACING));
        }
    }

    public static class Vogelhuisje extends Detail {
        public Vogelhuisje(Properties properties) {
            super(properties, false, new double[]{7, 0, 7, 9, 6, 9}, new double[]{4, 6, 4, 12, 13, 12}, new double[]{3, 13, 3, 13, 16, 13});
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(12) == 0) {
                level.playLocalSound(pos, random.nextBoolean() ? VogelsFeature.MEES.get() : VogelsFeature.TJIEP.get(), SoundSource.BLOCKS,
                        0.35f, 1.1f + random.nextFloat() * 0.3f, false);
            }
        }
    }

    private DetailBlokken() {
    }
}
