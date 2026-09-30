package nl.juiced.guhs.feature.guheinde;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A thrown Oog van Vadsig: flies up and towards the Knabbelkelder (the eye of ender's flight), then drops back as an item
 * (4 in 5) or breaks with a "njeg!" (1 in 5).
 */
public class OogVanVadsigEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(OogVanVadsigEntity.class, EntityDataSerializers.ITEM_STACK);
    private double tx, ty, tz;
    private int life;
    private boolean survives;
    @Nullable
    private UUID owner;

    public OogVanVadsigEntity(EntityType<? extends OogVanVadsigEntity> type, Level level) {
        super(type, level);
    }

    public OogVanVadsigEntity(Level level, double x, double y, double z) {
        this(GuheindeFeature.OOG_ENTITY.get(), level);
        setPos(x, y, z);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM, stack.isEmpty() ? defaultItem() : stack.copyWithCount(1));
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(DATA_ITEM);
    }

    private static ItemStack defaultItem() {
        return new ItemStack(GuheindeFeature.OOG_VAN_VADSIG.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    public void signalTo(BlockPos pos) {
        double dx = pos.getX() - getX(), dz = pos.getZ() - getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 12) {
            tx = getX() + dx / d * 12;
            tz = getZ() + dz / d * 12;
            ty = getY() + 8;
        } else {
            tx = pos.getX();
            ty = pos.getY();
            tz = pos.getZ();
        }
        life = 0;
        survives = random.nextInt(5) > 0;
    }

    /** For the game tests: does this eye come back as an item? */
    public boolean survives() {
        return survives;
    }

    public Vec3 target() {
        return new Vec3(tx, ty, tz);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        setDeltaMovement(x, y, z);
        if (xRotO == 0 && yRotO == 0) {
            setYRot((float) (Mth.atan2(x, z) * 180 / Math.PI));
            setXRot((float) (Mth.atan2(y, Math.sqrt(x * x + z * z)) * 180 / Math.PI));
            yRotO = getYRot();
            xRotO = getXRot();
        }
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        double nx = getX() + v.x, ny = getY() + v.y, nz = getZ() + v.z;
        double h = v.horizontalDistance();
        setXRot(lerpRotation(xRotO, (float) (Mth.atan2(v.y, h) * 180 / Math.PI)));
        setYRot(lerpRotation(yRotO, (float) (Mth.atan2(v.x, v.z) * 180 / Math.PI)));
        if (!level().isClientSide) {
            double dx = tx - nx, dz = tz - nz;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float angle = (float) Mth.atan2(dz, dx);
            double speed = Mth.lerp(0.0025, h, dist);
            double vy = v.y;
            if (dist < 1) {
                speed *= 0.8;
                vy *= 0.8;
            }
            int up = getY() < ty ? 1 : -1;
            v = new Vec3(Math.cos(angle) * speed, vy + (up - vy) * 0.015, Math.sin(angle) * speed);
            setDeltaMovement(v);
        }
        // a trail of pink and cheese-yellow sparkles
        level().addParticle(new DustParticleOptions(random.nextBoolean() ? new Vector3f(1f, 0.55f, 0.75f) : new Vector3f(1f, 0.82f, 0.3f), 1f),
                nx - v.x * 0.25 + random.nextDouble() * 0.6 - 0.3, ny - v.y * 0.25 - 0.3, nz - v.z * 0.25 + random.nextDouble() * 0.6 - 0.3, 0, 0, 0);
        if (!level().isClientSide) {
            setPos(nx, ny, nz);
            if (++life > 80) {
                finish();
            }
        } else {
            setPosRaw(nx, ny, nz);
        }
    }

    private void finish() {
        discard();
        if (survives) {
            playSound(SoundEvents.ENDER_EYE_DEATH, 1f, 1.2f);
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), getItem()));
            return;
        }
        playSound(SoundEvents.GLASS_BREAK, 1f, 1.4f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.02);
            server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.3f), 1.5f), getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            if (owner != null && server.getPlayerByUUID(owner) instanceof ServerPlayer player && player.distanceToSqr(this) < 64 * 64) {
                player.displayClientMessage(Component.translatable("gui.guhs.guheinde.oog_kapot").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }

    /** (Projectile.lerpRotation: turns smoothly, without spinning the long way round) */
    private static float lerpRotation(float from, float to) {
        while (to - from < -180f) {
            from -= 360f;
        }
        while (to - from >= 180f) {
            from += 360f;
        }
        return Mth.lerp(0.2f, from, to);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().save(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setItem(tag.contains("Item") ? ItemStack.parse(registryAccess(), tag.getCompound("Item")).orElse(defaultItem()) : defaultItem());
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1f;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
