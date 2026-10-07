package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * The blocks of Bikinibodem (einde2, slice bouw_bikinibodem): the soap bubble the dome is made of, the bubbelstroom that
 * is its lift, the flower clouds of its sky, and the decorative things of its houses (see {@link E2BouwBikinibodemFeature}).
 * Models and textures: tools/features/einde2_bouw_bikinibodem.py.
 */
public final class BikinibodemBlokken {
    private BikinibodemBlokken() {
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** The soap bubble of the dome: glass with a sheen (neighbours of its own kind draw as one film). */
    public static class Zeepbel extends TransparentBlock {
        public Zeepbel(BlockBehaviour.Properties properties) {
            super(properties);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Which way a bubbelstroom carries. */
    public enum Richting implements StringRepresentable {
        OMLAAG, OMHOOG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The bubbelstroom: a column block without collision that carries whoever stands in it, gently down or up (think
     * bubble column, without the water). Only the block at an entity's feet carries, so a wide column never counts double.
     * Going up, the top two blocks puff you off to the side where the column is open and there is a floor to land on. It
     * runs on the client (your own movement, so nobody rubber-bands) and on the server (mobs, items), and whoever leaves it
     * lands softly for a few seconds more. Fluids flow around it.
     */
    public static class Bubbelstroom extends Block implements LiquidBlockContainer {
        public static final EnumProperty<Richting> RICHTING = EnumProperty.create("richting", Richting.class);
        /** Blocks per tick: going up, going down, the hop at the top (up and sideways), and the most you drift sideways. */
        public static final double STIJG = 0.6, ZAK = 0.4, TOP_OP = 0.36, TOP_DUW = 0.32, OPZIJ = 0.12;
        /** Entity data: the game time it was last carried (no fall damage for {@link #ZACHT_TICKS} after it). */
        public static final String ZACHT = "guhs_einde2_bouw_bikinibodem_gedragen";
        public static final int ZACHT_TICKS = 100;
        private static final String GEMELD = "guhs_einde2_bouw_bikinibodem_gemeld";

        public Bubbelstroom(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(RICHTING, Richting.OMHOOG));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RICHTING);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }

        // --- fluids flow around it ------------------------------------------------------------------------------------------
        @Override
        protected boolean canBeReplaced(BlockState state, Fluid fluid) {
            return false;
        }

        @Override
        public boolean canPlaceLiquid(@Nullable LivingEntity player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
            return false;
        }

        @Override
        public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
            return false;
        }

        // --- the ride -------------------------------------------------------------------------------------------------------
        private static boolean isStroom(BlockGetter level, BlockPos pos) {
            return level.getBlockState(pos).getBlock() instanceof Bubbelstroom;
        }

        /** The highest block of this column (no stream above it). */
        public static boolean isTop(BlockGetter level, BlockPos pos) {
            return !isStroom(level, pos.above());
        }

        private static boolean vrij(BlockGetter level, BlockPos pos) {
            return !isStroom(level, pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        }

        /**
         * The side on which the top of a column is open: past the stream (a column may be three wide) the cell at this
         * height and the one below it are free. Null: shut in all around.
         */
        @Nullable
        public static Direction uitgang(BlockGetter level, BlockPos top) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = top;
                for (int i = 0; i < 3; i++) {
                    q = q.relative(d);
                    if (!isStroom(level, q)) {
                        if (vrij(level, q) && vrij(level, q.below())) {
                            return d;
                        }
                        break;
                    }
                }
            }
            return null;
        }

        /** What the stream does to an entity with its feet in this block. */
        public static void draag(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
            Vec3 v = entity.getDeltaMovement();
            entity.resetFallDistance();
            if (state.getValue(RICHTING) == Richting.OMLAAG) {
                entity.setDeltaMovement(klem(v.x), -ZAK, klem(v.z));
                return;
            }
            boolean top = isTop(level, pos);
            if (top || isTop(level, pos.above())) {
                Direction uit = uitgang(level, top ? pos : pos.above());
                if (uit != null) {
                    entity.setDeltaMovement(uit.getStepX() * TOP_DUW, TOP_OP, uit.getStepZ() * TOP_DUW);
                    return;
                }
            }
            entity.setDeltaMovement(klem(v.x), Math.min(STIJG, Math.max(v.y, 0) + 0.15), klem(v.z));
        }

        private static double klem(double opzij) {
            return Mth.clamp(opzij, -OPZIJ, OPZIJ);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            if (!(entity instanceof LivingEntity || entity instanceof ItemEntity) || entity.isPassenger()
                    || entity instanceof Player player && (player.getAbilities().flying || player.isSpectator())) {
                return;
            }
            if (Mth.floor(entity.getY() + 1.0E-4) != pos.getY()) {
                return; // only the block at its feet
            }
            draag(state, level, pos, entity);
            if (!level.isClientSide()) {
                long nu = level.getGameTime();
                if (entity instanceof ServerPlayer player && nu - entity.getPersistentData().getLongOr(GEMELD, -1000L) > 100) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_bouw_bikinibodem." + state.getValue(RICHTING).getSerializedName()));
                }
                entity.getPersistentData().putLong(GEMELD, nu);
                entity.getPersistentData().putLong(ZACHT, nu);
            }
        }

        /** Whoever was carried a moment ago lands softly (the stream lets go of you a little above the floor). */
        public static void onVal(LivingFallEvent event) {
            Entity entity = event.getEntity();
            if (entity.level().getGameTime() - entity.getPersistentData().getLongOr(ZACHT, -1000L) <= ZACHT_TICKS) {
                event.setDamageMultiplier(0);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            boolean omlaag = state.getValue(RICHTING) == Richting.OMLAAG;
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, omlaag ? -0.08 : 0.12, 0);
            }
            if (random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, omlaag ? -0.04 : 0.06, 0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A flower cloud: soft (nobody hurts themselves on it), in four pastel colours. */
    public static class Bloemwolk extends Block {
        public static final IntegerProperty KLEUR = IntegerProperty.create("kleur", 0, 3);

        public Bloemwolk(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KLEUR, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KLEUR);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            // (a different colour per spot, so a cloud built by hand is not one flat colour)
            BlockPos p = context.getClickedPos();
            return defaultBlockState().setValue(KLEUR, Math.floorMod(p.getX() / 3 + p.getY() + p.getZ() / 3, 4));
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            // (no fall damage)
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A decorative thing with a front: the clarinet on its stand, the safe, the till, the camera, the filing cabinet. */
    public static class Sierblok extends HorizontalDirectionalBlock {
        public static final MapCodec<Sierblok> CODEC = simpleCodec(p -> new Sierblok(Shapes.block(), p));
        private final VoxelShape vorm;

        public Sierblok(VoxelShape vorm, BlockBehaviour.Properties properties) {
            super(properties);
            this.vorm = vorm;
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
            return this.vorm;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** One of the many self-portraits of Octo Tentakel: a panel against the wall behind it, four different pictures. */
    public static class Zelfportret extends HorizontalDirectionalBlock {
        public static final MapCodec<Zelfportret> CODEC = simpleCodec(Zelfportret::new);
        public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 3);
        private static final VoxelShape NOORD = Block.box(0, 0, 14, 16, 16, 16), ZUID = Block.box(0, 0, 0, 16, 16, 2),
                OOST = Block.box(0, 0, 0, 2, 16, 16), WEST = Block.box(14, 0, 0, 16, 16, 16);

        public Zelfportret(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOORT, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SOORT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction kant = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, kant).setValue(SOORT, context.getLevel().getRandom().nextInt(4));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> ZUID;
                case EAST -> OOST;
                case WEST -> WEST;
                default -> NOORD;
            };
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }
}
