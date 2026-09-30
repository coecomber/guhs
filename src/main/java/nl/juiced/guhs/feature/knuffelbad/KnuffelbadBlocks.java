package nl.juiced.guhs.feature.knuffelbad;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Knuffelbad's blocks. */
public final class KnuffelbadBlocks {
    public static final EnumProperty<Glijbaan> GLIJBAAN = EnumProperty.create("glijbaan", Glijbaan.class);
    public static final EnumProperty<Vulling> VULLING = EnumProperty.create("vulling", Vulling.class);
    public static final EnumProperty<Kleur> KLEUR = EnumProperty.create("kleur", Kleur.class);
    public static final IntegerProperty LAGEN = IntegerProperty.create("lagen", 1, 8);
    public static final BooleanProperty FEL = BooleanProperty.create("fel");

    /** What is in a wash tub. */
    public enum Vulling implements StringRepresentable {
        LEEG, WATER, SCHUIM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The colours of the slides' running surface (glijgoot): pink, white, blush, dark (a funnel's eyes), glowing stars. */
    public enum Kleur implements StringRepresentable {
        ROZE, WIT, BLOS, DONKER, GLIM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The start gate at the top of a slide: an arch with the slide's colours and a guh face. Right-click it: into the
     * zwembandje and down you go. Which slide (and so which path, relative to this block) is in its state.
     */
    public static class GlijbaanStart extends HorizontalDirectionalBlock {
        public static final MapCodec<GlijbaanStart> CODEC = simpleCodec(GlijbaanStart::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 5, 3, 16, 11), Block.box(13, 0, 5, 16, 16, 11), Block.box(0, 13, 5, 16, 16, 11));

        public GlijbaanStart(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GLIJBAAN, Glijbaan.ROZE_TRECHTER));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, GLIJBAAN);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE : rotated();
        }

        private static VoxelShape rotated() {
            return Shapes.or(Block.box(5, 0, 0, 11, 16, 3), Block.box(5, 0, 13, 11, 16, 16), Block.box(5, 13, 0, 11, 16, 16));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                GlijRit.start(sp, pos, state.getValue(GLIJBAAN), state.getValue(FACING));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(KnuffelbadFeature.ZEEPBELLETJE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 1.1,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0, 0.02, 0);
            }
        }
    }

    /**
     * A guh wash tub: a round wooden tub with a guh face on the front and a little shower. Right-click it with an empty
     * hand: the shower (rinse the foam off a guh in the tub). Shows water or foam while a guh is being washed.
     */
    public static class Wastobbe extends HorizontalDirectionalBlock {
        public static final MapCodec<Wastobbe> CODEC = simpleCodec(Wastobbe::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(0, 2, 0, 16, 9, 2), Block.box(0, 2, 14, 16, 9, 16),
                Block.box(0, 2, 2, 2, 9, 14), Block.box(14, 2, 2, 16, 9, 14));

        public Wastobbe(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VULLING, Vulling.LEEG));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, VULLING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Wasritueel.douche(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            if (stack.is(Items.WATER_BUCKET)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    Wasritueel.douche(sp, pos);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(VULLING) == Vulling.SCHUIM && random.nextInt(2) == 0) {
                level.addParticle(KnuffelbadFeature.ZEEPBELLETJE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.65,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.015, 0);
            } else if (state.getValue(VULLING) == Vulling.WATER && random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0, 0);
            }
        }
    }

    /**
     * The running surface of the slides: wet pink (white, blush, dark, star) tiles in layers of 1/8 block, so a slide's
     * surface can follow its curve. Slippery. The star version glows in the dark.
     */
    public static class Glijgoot extends Block {
        private static final VoxelShape[] SHAPES = new VoxelShape[9];

        static {
            for (int i = 0; i <= 8; i++) {
                SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
            }
        }

        public Glijgoot(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LAGEN, 8).setValue(KLEUR, Kleur.ROZE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LAGEN, KLEUR);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES[state.getValue(LAGEN)];
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return type == PathComputationType.LAND && state.getValue(LAGEN) < 5;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(KLEUR) == Kleur.GLIM && random.nextInt(24) == 0 && level.getBlockState(pos.above()).isAir()) {
                level.addParticle(KnuffelbadFeature.GLINSTERING.get(), pos.getX() + random.nextDouble(), pos.getY() + state.getValue(LAGEN) / 8.0 + 0.1,
                        pos.getZ() + random.nextDouble(), 0, 0.01, 0);
            }
        }
    }

    /** Glow-in-the-dark tiles: dark blue with glowing stars (the Glimtunnel). "fel": a bright light ring. */
    public static class Glimtegel extends Block {
        public Glimtegel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FEL, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FEL);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(state.getValue(FEL) ? 6 : 40) == 0) {
                Direction d = Direction.getRandom(random);
                BlockPos side = pos.relative(d);
                if (level.getBlockState(side).isAir()) {
                    level.addParticle(KnuffelbadFeature.GLINSTERING.get(), side.getX() + random.nextDouble(), side.getY() + random.nextDouble(),
                            side.getZ() + random.nextDouble(), 0, 0, 0);
                }
            }
        }
    }

    /**
     * Pink bath foam: soft (you sink into it and it slows you down), catches a fall, bubbles. The foam baths and the end of
     * the Roze Trechter.
     */
    public static class Schuim extends Block {
        public Schuim(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        /** Two foam blocks next to each other: no wall between them (like glass). */
        @Override
        protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
            return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return type == PathComputationType.WATER;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof LivingEntity || entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                entity.makeStuckInBlock(state, new Vec3(0.85, 0.55, 0.85));
                entity.resetFallDistance();
                if (level.isClientSide() && level.getRandom().nextInt(4) == 0 && entity.getDeltaMovement().horizontalDistanceSqr() > 1e-4) {
                    level.addParticle(KnuffelbadFeature.SCHUIMVLOKJE.get(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                            (level.getRandom().nextDouble() - 0.5) * 0.1, 0.05, (level.getRandom().nextDouble() - 0.5) * 0.1);
                }
            }
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            entity.resetFallDistance();
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(10) == 0 && level.getBlockState(pos.above()).isAir()) {
                level.addParticle(KnuffelbadFeature.ZEEPBELLETJE.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(),
                        0, 0.01, 0);
            }
        }
    }

    private KnuffelbadBlocks() {
    }
}
