package nl.juiced.guhs.feature.guheinde;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.registry.ModItems;
import org.jspecify.annotations.Nullable;

/**
 * A knabbelkristal: a floating crystal stuffed with the kaasknabbels Opper-Mika stole. On top of the kaaspilaren they
 * heal him (a beam); smash one and the knabbels rain back down (and his Enderguh gets a little more vahoeg). The four on
 * the knabbelsokkels call Opper-Mika back. Uses the end crystal's model (client: KnabbelkristalRenderer).
 * <p>
 * 1.1.0: 26.1's {@code EndCrystal#hurtServer} is final (it always explodes), so this is its own entity with the end
 * crystal's parts: the spin time, the beam target and "show bottom" (same save keys as the end crystal).
 */
public class KnabbelkristalEntity extends Entity {
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_BEAM_TARGET = SynchedEntityData.defineId(KnabbelkristalEntity.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_BOTTOM = SynchedEntityData.defineId(KnabbelkristalEntity.class,
            EntityDataSerializers.BOOLEAN);
    /** The spin (ticks), like the end crystal's. */
    public int time;

    public KnabbelkristalEntity(EntityType<? extends KnabbelkristalEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_BEAM_TARGET, Optional.empty());
        entityData.define(DATA_SHOW_BOTTOM, true);
    }

    @Override
    public void tick() {
        this.time++;
        this.applyEffectsFromBlocks();
        this.handlePortal();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.storeNullable("beam_target", BlockPos.CODEC, this.getBeamTarget());
        output.putBoolean("ShowBottom", this.showsBottom());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setBeamTarget(input.read("beam_target", BlockPos.CODEC).orElse(null));
        this.setShowBottom(input.getBooleanOr("ShowBottom", true));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public void setBeamTarget(@Nullable BlockPos target) {
        this.getEntityData().set(DATA_BEAM_TARGET, Optional.ofNullable(target));
    }

    public @Nullable BlockPos getBeamTarget() {
        return this.getEntityData().get(DATA_BEAM_TARGET).orElse(null);
    }

    public void setShowBottom(boolean showBottom) {
        this.getEntityData().set(DATA_SHOW_BOTTOM, showBottom);
    }

    public boolean showsBottom() {
        return this.getEntityData().get(DATA_SHOW_BOTTOM);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return super.shouldRenderAtSqrDistance(distance) || this.getBeamTarget() != null;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return !this.isInvulnerableToBase(source) && !(source.getEntity() instanceof OpperMikaEntity)
                && !(source.getEntity() instanceof HongerigeEnderguhEntity);
    }

    public KnabbelkristalEntity(Level level, double x, double y, double z) {
        this(GuheindeFeature.KNABBELKRISTAL_ENTITY.get(), level);
        setPos(x, y, z);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (isInvulnerableToBase(source) || source.getEntity() instanceof OpperMikaEntity || source.getEntity() instanceof HongerigeEnderguhEntity) {
            return false;
        }
        if (!isRemoved() && !level().isClientSide()) {
            smash(source);
        }
        return true;
    }

    @Override
    public void kill(ServerLevel level) {
        if (!isRemoved()) {
            smash(damageSources().generic());
        }
    }

    /** Pop! The stolen knabbels fly out, a harmless puff, and the fight hears about it. */
    public void smash(DamageSource source) {
        remove(Entity.RemovalReason.KILLED);
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5f, 1.3f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 2f, 0.8f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(new DustParticleOptions(0xFFD14C /* 1, 0.82, 0.3 */, 2f), getX(), getY() + 1, getZ(), 60, 1.2, 1.2, 1.2, 0.2);
        for (int i = 0; i < 6; i++) {
            ItemEntity knabbel = new ItemEntity(level, getX(), getY() + 1, getZ(), new ItemStack(ModItems.KAAS_KNABBELS.get()));
            knabbel.setDeltaMovement(random.nextGaussian() * 0.25, 0.4 + random.nextDouble() * 0.3, random.nextGaussian() * 0.25);
            level.addFreshEntity(knabbel);
        }
        GuheindeGevecht fight = GuheindeGevecht.of(level);
        if (fight != null) {
            fight.onCrystalSmashed(this, source);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(GuheindeFeature.KNABBELKRISTAL.get());
    }
}
