package nl.juiced.guhs.feature.einde2.h2water;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.Minigames;

/**
 * Sokka-guh's boemerang in flight (the reward of chapter 2): it flies out in a little curve, and ALWAYS comes back to whoever
 * threw it. On its way it picks up the items it touches (at most {@link #MAX_DINGEN} stacks) and brings them along; a Mika
 * it hits gets a little shove, nothing more (no damage to anyone, ever). A block turns it round. Whatever it carries is
 * never lost: when it cannot get back (its thrower logged out, another dimension) the things are handed over or dropped
 * where it is. It is never saved with the world (the item itself stays in the thrower's inventory: this is only its flight).
 */
public class BoemerangEntity extends ThrowableItemProjectile {
    /** How many ticks it flies out before it turns; how far at most; how many stacks it carries. */
    public static final int UIT = 14, MAX_DINGEN = 8;
    public static final double MAX_AFSTAND = 15;

    private boolean terug;
    private final List<ItemStack> dingen = new ArrayList<>();
    private boolean afgeleverd;

    public BoemerangEntity(EntityType<? extends BoemerangEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BoemerangEntity(Level level, LivingEntity werper) {
        super(E2H2WaterFeature.BOEMERANG_VLUCHT.get(), werper, level, new ItemStack(E2H2WaterFeature.BOEMERANG.get()));
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return E2H2WaterFeature.BOEMERANG.get();
    }

    /** Is it on its way back? */
    public boolean terug() {
        return terug;
    }

    /** What it carries now (copies). */
    public List<ItemStack> dingen() {
        return dingen.stream().map(ItemStack::copy).toList();
    }

    @Nullable
    private ServerPlayer werper() {
        return getOwner() instanceof ServerPlayer p && p.isAlive() && p.level() == level() ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        ServerPlayer werper = werper();
        if (werper == null) {
            if (tickCount > 2) {
                lever(null);
                discard();
            }
            return;
        }
        if (!terug && (tickCount >= UIT || distanceToSqr(werper) > MAX_AFSTAND * MAX_AFSTAND)) {
            draai();
        }
        if (terug) {
            Vec3 naar = werper.position().add(0, werper.getBbHeight() * 0.6, 0).subtract(position());
            double l = naar.length();
            if (l < 1.4 || tickCount > 200) {
                lever(werper);
                discard();
                return;
            }
            Vec3 v = getDeltaMovement().scale(0.7).add(naar.normalize().scale(0.45));
            double max = 1.2;
            setDeltaMovement(v.length() > max ? v.normalize().scale(max) : v);
        } else {
            // a little curve to the left while it goes out
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.add(new Vec3(-v.z, 0, v.x).scale(0.04)));
        }
        // it picks up what it touches
        if (dingen.size() < MAX_DINGEN) {
            for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.9))) {
                if (item.isAlive() && !item.hasPickUpDelay() && dingen.size() < MAX_DINGEN) {
                    dingen.add(item.getItem().copy());
                    item.discard();
                }
            }
        }
        if (tickCount % 4 == 0) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.25f, 1.6f + (tickCount % 8) * 0.05f);
        }
        if (tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void draai() {
        terug = true;
        setDeltaMovement(getDeltaMovement().scale(-0.3));
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide()) {
            return;
        }
        if (result instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            if (e == getOwner()) {
                return;
            }
            if (e instanceof MikaEntity mika) {
                Vec3 d = new Vec3(mika.getX() - getX(), 0, mika.getZ() - getZ());
                d = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
                mika.push(d.x * 0.6, 0.3, d.z * 0.6);
                mika.hurtMarked = true;
            }
        }
        if (!terug) {
            draai();
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return e != getOwner() && super.canHitEntity(e);
    }

    /** Hands over what it carries: to the thrower, else on the ground where it is. Once. */
    private void lever(@Nullable ServerPlayer werper) {
        if (afgeleverd) {
            return;
        }
        afgeleverd = true;
        for (ItemStack s : dingen) {
            if (werper != null) {
                Minigames.give(werper, s.copy());
            } else if (level() instanceof ServerLevel server) {
                server.addFreshEntity(new ItemEntity(server, getX(), getY(), getZ(), s.copy()));
            }
        }
        dingen.clear();
        if (werper != null) {
            level().playSound(null, werper.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && !afgeleverd && !dingen.isEmpty()) {
            ServerPlayer werper = werper();
            if (werper == null && getOwner() instanceof ServerPlayer p && p.isAlive()) {
                werper = p;   // (another dimension: still theirs)
            }
            lever(werper);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** (tests) the UUID of the thrower. */
    @Nullable
    UUID werperId() {
        Entity o = getOwner();
        return o == null ? null : o.getUUID();
    }
}
