package nl.juiced.guhs.feature.ringh6;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * bbq2 (ring-h6): a coal the Frituurberg throws at whoever climbs it (entity {@code guhs:ringh6_valkool}; drawn like a thrown
 * item: the gloeiend kooltje, big and bright). It falls, bounces once and puffs out in sparks. It NEVER hurts anybody: a
 * player it hits gets a shove ({@link Duwtje}) and a funny line, once per coal. It never sets anything on fire, never
 * breaks or places a block, and is never saved. {@link Kolen} drops them.
 */
public class ValkoolEntity extends Entity implements ItemSupplier {
    /** Blocks per tick per tick, and the fastest it falls. */
    public static final double ZWAARTEKRACHT = 0.045, MAX_VAL = 1.1;
    /** How hard it shoves, and how near it has to come (blocks around its own box). */
    public static final double DUW = 0.85, RAAK = 0.6;
    public static final int LEEFT = 160;

    private final Set<UUID> geduwd = new HashSet<>();
    private int stuiters;

    public ValkoolEntity(EntityType<? extends ValkoolEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(SpiesburchtFeature.GLOEIEND_KOOLTJE_ITEM.get());
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        v = new Vec3(v.x * 0.98, Math.max(-MAX_VAL, v.y - ZWAARTEKRACHT), v.z * 0.98);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 0, 0.02, 0);
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.03, 0);
            }
            if (verticalCollision && v.y < 0) {
                setDeltaMovement(v.x, 0.3, v.z);              // (the server decides when it is over)
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(RAAK))) {
            raak(p);
        }
        if (isRemoved()) {
            return;
        }
        if (verticalCollision && v.y < 0) {
            if (stuiters++ >= 1) {
                sputter(level);
                return;
            }
            // one bounce, a little way on in the direction it was going
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BASALT_HIT, SoundSource.HOSTILE, 0.7f, 0.7f);
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.1, getZ(), 3, 0.2, 0.05, 0.2, 0);
            setDeltaMovement(v.x * 1.4 + (random.nextDouble() - 0.5) * 0.16, 0.32, v.z * 1.4 + (random.nextDouble() - 0.5) * 0.16);
        }
        if (tickCount > LEEFT || !level.getFluidState(blockPosition()).isEmpty()) {
            sputter(level);
        }
    }

    /** It hits this player: a shove away from it, nothing else. Once per coal and per player. */
    void raak(ServerPlayer p) {
        if (p.isSpectator() || p.isCreative() || p.isPassenger() || !Duwtje.mag(p) || !geduwd.add(p.getUUID())) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 weg = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        if (weg.lengthSqr() < 0.01) {
            weg = Vec3.directionFromRotation(0, random.nextFloat() * 360f);
        }
        Duwtje.duw(p, weg, DUW);
        p.clearFire();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BASALT_BREAK, SoundSource.HOSTILE, 0.9f, 0.6f);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.kool.raak." + random.nextInt(3)).withStyle(ChatFormatting.GOLD));
        Kolen.geraakt(p);
        sputter(level);
    }

    private void sputter(ServerLevel level) {
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 5, 0.25, 0.1, 0.25, 0);
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 10, 0.25, 0.2, 0.25, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.4f, 1.4f);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
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
