package nl.juiced.guhs.feature.circuit;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * The blocks of the Guh-Circuit: the glowing rainbow road (7 colours), the rainbow boost ring (you run right through it:
 * VAHOEG!), the bouncy stuiterpaddenstoel, and the invisible markers of the tracks (where the Mika-pikkers wait, where
 * the Mika's push the kaasknabbels down the Kaasberg, where the Vadslooping starts). The plain road blocks (kaasweg,
 * slippery kaassaus, glittering bergijs) are simple blocks with their own friction (CircuitFeature).
 */
public final class CircuitBlocks {
    /** Which colour of the rainbow a piece of rainbow road is (red, orange, yellow, green, blue, indigo, violet). */
    public static final IntegerProperty KLEUR = IntegerProperty.create("kleur", 0, 6);
    /** From which level on a marker counts (0 makkelijk, 1 medium, 2 lastig): read by RaceTrack as "vanaf". */
    public static final IntegerProperty VANAF = IntegerProperty.create("vanaf", 0, 2);

    /** The rainbow road of the Regenboogbaan: seven glowing colours side by side. */
    public static class Regenboogweg extends Block {
        public static final MapCodec<Regenboogweg> CODEC = simpleCodec(Regenboogweg::new);

        public Regenboogweg(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KLEUR, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KLEUR);
        }
    }

    /**
     * The rainbow boost ring's shimmering skin: no collision, you run right through it and your race guh gets a VAHOEG boost
     * (the tag guhs:race_boost; RaceGuhEntity). Its axis is the way the track runs through it.
     */
    public static class Boostring extends Block {
        public static final MapCodec<Boostring> CODEC = simpleCodec(Boostring::new);
        public static final EnumProperty<Direction.Axis> AXIS = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X = Block.box(6, 0, 0, 10, 16, 16), Z = Block.box(0, 0, 6, 16, 16, 10);

        public Boostring(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
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
            return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                float hue = (level.getGameTime() % 70) / 70f + random.nextFloat() * 0.2f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.6f, 1f);
                level.addParticle(new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1f),
                        pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
            }
        }
    }

    /**
     * The stuiterpaddenstoel: a big bouncy guh mushroom cap. Whatever steps on it bounces up (a race guh too: boing!);
     * whatever falls on it bounces back (like slime, a bit more). Nothing gets hurt falling on it.
     */
    public static class Stuiterpaddenstoel extends Block {
        public static final MapCodec<Stuiterpaddenstoel> CODEC = simpleCodec(Stuiterpaddenstoel::new);
        /** How hard a step on it throws you up. */
        public static final double STUITER = 0.62;

        public Stuiterpaddenstoel(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            entity.causeFallDamage(fallDistance, 0f, level.damageSources().fall());   // (no damage at all)
        }

        @Override
        public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
            if (entity.isSuppressingBounce()) {
                super.updateEntityAfterFallOn(level, entity);
                return;
            }
            Vec3 v = entity.getDeltaMovement();
            if (v.y < 0) {
                double factor = entity instanceof LivingEntity ? 1.0 : 0.8;
                entity.setDeltaMovement(v.x, Math.max(-v.y * factor, STUITER * 0.6), v.z);
            }
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            bounce(level, pos, entity);
            super.stepOn(level, pos, state, entity);
        }

        /** Boing: up it goes (the side that moves the entity does the bouncing; the server adds the sound and puffs). */
        public static void bounce(Level level, BlockPos pos, Entity entity) {
            if (entity.isSteppingCarefully() || !entity.onGround()) {
                return;
            }
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x * 1.1, STUITER, v.z * 1.1);
            entity.hasImpulse = true;
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 0.9f, 1.3f + level.getRandom().nextFloat() * 0.3f);
                ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1,
                        pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
            }
        }
    }

    /**
     * An invisible marker of a track (tag guhs:race_marker: RaceTrack keeps where they are): facing the way it counts, and
     * from which level on (VANAF). Walk through it, see nothing.
     */
    public static class Marker extends HorizontalDirectionalBlock {
        public static final MapCodec<Marker> CODEC = simpleCodec(Marker::new);

        public Marker(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VANAF, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, VANAF);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    private CircuitBlocks() {
    }
}
