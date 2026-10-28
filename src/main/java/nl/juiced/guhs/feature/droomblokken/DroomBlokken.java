package nl.juiced.guhs.feature.droomblokken;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.world.GuhTime;

/**
 * 1.8.0 (Guhdromenland): the dream blocks ({@link DroomblokkenFeature}). Zeepbelglas, droomzandblok, slaapliedjesgras,
 * maanbloemetje and the droomzee under the dream islands.
 */
public final class DroomBlokken {
    private DroomBlokken() {
    }

    /** Zeepbelglas: see-through glass with a soap sheen whose colours slowly drift (animated texture). */
    public static class Zeepbelglas extends TransparentBlock {
        public Zeepbelglas(Properties properties) {
            super(properties);
        }
    }

    /** Droomzandblok: glittering silver sand that does not fall; little sparkles drift UP out of it. Stores droomzand. */
    public static class Droomzandblok extends Block {
        public static final MapCodec<Droomzandblok> CODEC = simpleCodec(Droomzandblok::new);

        public Droomzandblok(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0 && !level.getBlockState(pos.above()).isSolidRender()) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(),
                        (random.nextDouble() - 0.5) * 0.01, 0.015 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    /**
     * Slaapliedjesgras: lavender grass with little notes on it. It hums softly when someone walks over it: now and then a
     * soft note from a lullaby scale (pentatonic, so any order sounds sweet), with a note particle.
     */
    public static class Slaapliedjesgras extends Block {
        public static final MapCodec<Slaapliedjesgras> CODEC = simpleCodec(Slaapliedjesgras::new);
        /** The pitches of a soft major pentatonic (note block pitch = 2^((n-12)/12)). */
        static final float[] TONEN = {0.749f, 0.841f, 0.944f, 1.122f, 1.260f, 1.498f};

        public Slaapliedjesgras(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 && level.getRandom().nextInt(14) == 0 && !entity.isSteppingCarefully()) {
                neurie(level, pos, level.getRandom());
            }
            super.stepOn(level, pos, state, entity);
        }

        /** One soft hum (both sides: the server plays the sound for everyone, the client draws the note). */
        static void neurie(Level level, BlockPos pos, RandomSource random) {
            float toon = TONEN[random.nextInt(TONEN.length)];
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.22f, toon);
            } else {
                level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, (toon - 0.7) / 0.85, 0, 0);
            }
        }
    }

    /**
     * Maanbloemetje: a little crescent-moon flower that only opens at night (and in the eternal dusk of Guhdromenland). Open,
     * it glows softly and lets a sparkle go now and then; by day it is a closed bud.
     */
    public static class Maanbloemetje extends VegetationBlock {
        public static final MapCodec<Maanbloemetje> CODEC = simpleCodec(Maanbloemetje::new);
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

        public Maanbloemetje(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(OPEN, false));
        }

        @Override
        protected MapCodec<? extends VegetationBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(OPEN);
        }

        /** Is it night here, so the moon flowers are open? Always in a world without day and night (Guhdromenland's eternal
         *  dusk); elsewhere from dusk to dawn on the day clock. */
        public static boolean nacht(Level level) {
            if (level.dimensionType().hasFixedTime()) {
                return true;
            }
            long t = GuhTime.timeOfDay(level);
            return t >= 12500 && t < 23500;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(OPEN, nacht(context.getLevel()));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.DIRT) || state.is(DroomblokkenFeature.SLAAPLIEDJESGRAS.get()) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean open = nacht(level);
            if (state.getValue(OPEN) != open) {
                level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(OPEN) && random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.7,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
            }
        }
    }

    /**
     * Droomzee: the soft lavender sea far below the dream islands. Not a fluid and not swimmable: you sink slowly through its
     * glossy layer as through a cloud. What happens when someone falls in is up to the dream dimension
     * ({@link #bijVallen}: Guhdromenland sends a Blub bubble that carries you back); without a handler it only cushions the fall.
     * Unbreakable and drops nothing (it is part of the dream).
     */
    public static class Droomzee extends Block {
        public static final MapCodec<Droomzee> CODEC = simpleCodec(Droomzee::new);
        @Nullable
        private static volatile BiConsumer<Entity, BlockPos> handler;

        public Droomzee(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        /** The dream dimension says what falling into the droomzee does (called on the server for every entity inside). */
        public static void bijVallen(@Nullable BiConsumer<Entity, BlockPos> nieuw) {
            handler = nieuw;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
            return neighbour.is(this) || super.skipRendering(state, neighbour, side);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x * 0.6, Math.max(v.y, -0.06), v.z * 0.6);
            entity.resetFallDistance();
            if (!level.isClientSide()) {
                BiConsumer<Entity, BlockPos> h = handler;
                if (h != null) {
                    h.accept(entity, pos);
                }
            } else if (entity instanceof LivingEntity && level.getRandom().nextInt(4) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + 0.3, entity.getZ(), 0, 0.02, 0);
            }
        }
    }
}
