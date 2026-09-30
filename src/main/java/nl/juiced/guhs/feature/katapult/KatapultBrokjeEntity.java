package nl.juiced.guhs.feature.katapult;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A brokje: a block knocked out of a Mika fort (or one that lost its hold), tumbling through the air. It falls with
 * gravity, bounces off what it lands on, and when it lands hard on another block of the fort it can knock that one out
 * too ({@link KatapultGame#crash}). After a little while it's gone in a puff: it never becomes a block again (the fort is
 * rebuilt from its template). A Mika that falls runs off giggling, a crate of kaasknabbels bursts open: free knabbels!
 */
public class KatapultBrokjeEntity extends Entity {
    private static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.defineId(KatapultBrokjeEntity.class, EntityDataSerializers.BLOCK_STATE);
    public static final double GRAVITY = 0.04, DRAG = 0.98;
    /** Landing this hard (speed) on a fort block can knock it out; energy = speed² × weight × CRASH. */
    public static final double CRASH_SPEED = 0.42, CRASH = 4.0;
    public static final int LIFE = 50;

    @Nullable
    private UUID npc;
    private boolean crashed;
    private int life = LIFE;

    public KatapultBrokjeEntity(EntityType<? extends KatapultBrokjeEntity> type, Level level) {
        super(type, level);
    }

    public static KatapultBrokjeEntity create(ServerLevel level, BlockPos pos, BlockState state, Vec3 velocity, @Nullable UUID npc) {
        KatapultBrokjeEntity e = new KatapultBrokjeEntity(KatapultFeature.BROKJE.get(), level);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        e.entityData.set(STATE, state);
        e.setDeltaMovement(velocity);
        e.npc = npc;
        e.life = LIFE + level.random.nextInt(20);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, Blocks.OAK_PLANKS.defaultBlockState());
    }

    public BlockState getBlockState() {
        return entityData.get(STATE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Vec3 v = getDeltaMovement().add(0, -GRAVITY, 0).scale(DRAG);
        double speed = v.length();
        move(MoverType.SELF, v);
        if (isRemoved()) {
            return;
        }
        Vec3 after = getDeltaMovement();
        if (verticalCollisionBelow || horizontalCollision) {
            if (!crashed && speed >= CRASH_SPEED && npc != null) {
                BlockPos hit = verticalCollisionBelow ? BlockPos.containing(getX(), getY() - 0.2, getZ())
                        : BlockPos.containing(getX() + Math.signum(v.x) * 0.6, getY() + 0.3, getZ() + Math.signum(v.z) * 0.6);
                crashed = KatapultGame.crash(this, hit, v, speed * speed * KatapultFort.strength(getBlockState()) * CRASH);
            }
            double vx = horizontalCollision ? -v.x * 0.3 : v.x * 0.7, vz = horizontalCollision ? -v.z * 0.3 : v.z * 0.7;
            double vy = verticalCollisionBelow ? (v.y < -0.2 ? -v.y * 0.25 : 0) : after.y;
            setDeltaMovement(vx, vy, vz);
        } else {
            setDeltaMovement(v);
        }
        if (--life <= 0 || getY() < level().getMinBuildHeight()) {
            poof();
        }
    }

    /** Gone in a puff (a Mika giggles and runs off, a crate of knabbels bursts open). */
    public void poof() {
        if (level() instanceof ServerLevel server) {
            BlockState state = getBlockState();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY() + 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            if (KatapultFort.isMika(state)) {
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                server.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), 3, 0.4, 0.2, 0.4, 1);
                server.playSound(null, blockPosition(), ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.7f, 1.7f);
            } else if (KatapultFort.isKist(state)) {
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.KAAS_KNABBELS.get())), getX(), getY() + 0.6, getZ(),
                        14, 0.3, 0.3, 0.3, 0.15);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 6, 0.4, 0.3, 0.4, 0);
                server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
            }
        }
        discard();
    }

    @Nullable
    public UUID getNpc() {
        return npc;
    }

    // --- not a normal entity -------------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
