package nl.juiced.guhs.feature.kaasmoeras;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Borrelende kaassaus: a thick, bubbling puddle of cheese sauce. Step (or fall) on it and it bounces you up high
 * ({@link #BOUNCE}); it never hurts, not even after a long fall. Sneak to wade through it without bouncing.
 * It bubbles and blubs all the time and gives a warm glow.
 */
public class BorrelendeKaassausBlock extends Block {
    public static final MapCodec<BorrelendeKaassausBlock> CODEC = simpleCodec(BorrelendeKaassausBlock::new);
    /** Upward speed of a bounce (a normal jump is 0.42): about 4 blocks high. */
    public static final double BOUNCE = 0.95;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    static final DustParticleOptions CHEESE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.25f), 1.0f);

    public BorrelendeKaassausBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** No fall damage on bubbling cheese: it's like landing in a warm pudding. */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.resetFallDistance();
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            bounce(entity);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) {
            bounce(entity);
            if (!level.isClientSide()) {
                blub(level, pos, entity);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Throws the entity up (keeps a bit of its sideways speed). */
    public static void bounce(Entity entity) {
        Vec3 v = entity.getDeltaMovement();
        double up = entity instanceof LivingEntity ? BOUNCE : BOUNCE * 0.6;
        if (v.y < up) {
            entity.setDeltaMovement(v.x * 0.9, up, v.z * 0.9);
            entity.hasImpulse = true;
        }
        entity.resetFallDistance();
    }

    private static void blub(Level level, BlockPos pos, Entity entity) {
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 1.0f, 0.6f + level.getRandom().nextFloat() * 0.3f);
        level.playSound(null, pos, SoundEvents.SLIME_JUMP_SMALL, SoundSource.BLOCKS, 0.6f, 0.8f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(CHEESE_DUST, entity.getX(), pos.getY() + 0.8, entity.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
            server.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), pos.getY() + 0.8, entity.getZ(), 6, 0.3, 0.05, 0.3, 0.05);
        }
        if (entity instanceof ServerPlayer player) {
            KaasmoerasEvents.bounced(player);
        }
    }

    /** Bubbles rise and pop, now and then with a deep blub. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7, z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        double y = pos.getY() + 0.78;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.02, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(CHEESE_DUST, x, y + 0.05, z, 0, 0.04, 0);
        }
        if (random.nextInt(60) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x, y + 0.1, z, 0, 0.03, 0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5f, 0.5f + random.nextFloat() * 0.3f, false);
        }
        if (random.nextInt(160) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.MUD_STEP, SoundSource.BLOCKS, 0.7f, 0.6f, false);
        }
    }
}
