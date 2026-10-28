package nl.juiced.guhs.feature.sterrenveld;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A stuiterkussen: a giant soft pillow you bounce high up from (the spiral of the Sterrenkussenveld). Much bouncier than the
 * circuit's stuiterpaddenstoel, and it works under the dream's slow falling too: stepping on it always throws you up
 * {@link #STUITER} (about five blocks), whatever your fall speed. Sneak to just stand on it. Nobody gets hurt falling on it.
 */
public class Stuiterkussen extends Block {
    public static final MapCodec<Stuiterkussen> CODEC = simpleCodec(Stuiterkussen::new);
    /** The upward speed a bounce gives (a little over 3 blocks of rise between two pillows of the spiral). */
    public static final double STUITER = 0.95;

    public Stuiterkussen(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0f, level.damageSources().fall());   // (no damage at all)
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 v = entity.getDeltaMovement();
        if (v.y < 0) {
            entity.setDeltaMovement(v.x, entity instanceof LivingEntity ? STUITER : -v.y * 0.6, v.z);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        stuiter(level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    /** Boing: up it goes (the side that moves the entity bounces it; the server adds the sound and a puff). */
    public static void stuiter(Level level, BlockPos pos, Entity entity) {
        if (entity.isSteppingCarefully() || !entity.onGround() || !(entity instanceof LivingEntity)) {
            return;
        }
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 1.15, STUITER, v.z * 1.15);
        entity.needsSync = true;
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.7f + server.getRandom().nextFloat() * 0.2f);
            server.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 0.5f, 1.5f);
            server.sendParticles(ParticleTypes.CLOUD, entity.getX(), pos.getY() + 1.1, entity.getZ(), 4, 0.3, 0.05, 0.3, 0.01);
        }
    }
}
