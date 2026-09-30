package nl.juiced.guhs.feature.evenementen;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.registry.ModItems;
import org.joml.Vector3f;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A kaasknabbel of the kaasregen (drawn like a thrown item): it falls straight down to the ground it was aimed at, lies
 * there for a while and is caught by walking into it (or eaten by a wild guh), see {@link Kaasregen}. It's never saved:
 * a kaasregen doesn't survive a restart, and neither do its knabbels.
 */
public class VallendeKnabbelEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> DATA_GOLDEN = SynchedEntityData.defineId(VallendeKnabbelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_LAND_Y = SynchedEntityData.defineId(VallendeKnabbelEntity.class, EntityDataSerializers.FLOAT);
    /** Blocks per tick. */
    public static final double FALL_SPEED = 0.3;
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xFFD940 /* 1, 0.85, 0.25 */, 0.8f);

    /** Server: ticks it has been lying on the ground. */
    int landedTicks;

    public VallendeKnabbelEntity(EntityType<? extends VallendeKnabbelEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GOLDEN, false);
        builder.define(DATA_LAND_Y, 0f);
    }

    public void setUp(boolean golden, double landY) {
        this.entityData.set(DATA_GOLDEN, golden);
        this.entityData.set(DATA_LAND_Y, (float) landY);
    }

    public boolean isGolden() {
        return this.entityData.get(DATA_GOLDEN);
    }

    public double landY() {
        return this.entityData.get(DATA_LAND_Y);
    }

    public boolean landed() {
        return this.getY() <= landY() + 1.0e-4;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(isGolden() ? EvenementenFeature.GOUDEN_KAASKNABBEL.get() : ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0 && !Evenementen.owns(this)) {
            this.discard(); // a leftover of an event that's over
            return;
        }
        if (this.level().isClientSide() && !landed()) {
            fall(); // (on the server its kaasregen moves it: see serverStep)
        }
        if (this.level().isClientSide() && isGolden() && this.tickCount % 3 == 0) {
            this.level().addParticle(GOLD, this.getX() + (this.random.nextDouble() - 0.5) * 0.4, this.getY() + 0.2,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.02, 0);
            }
        }
    }

    private void fall() {
        this.setPos(this.getX(), Math.max(landY(), this.getY() - FALL_SPEED), this.getZ());
    }

    /** Server: moved by its {@link Kaasregen} every server tick, also where its chunk doesn't tick entities. */
    void serverStep() {
        if (landed()) {
            landedTicks++;
        } else {
            fall();
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
