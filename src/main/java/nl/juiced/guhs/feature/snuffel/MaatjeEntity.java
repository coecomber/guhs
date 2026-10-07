package nl.juiced.guhs.feature.snuffel;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The companion (entity {@code guhs:snuffel_maatje}): the naughty little forest sprite that only ITS OWN player can see.
 * That is a rule of the server, not of the picture: the entity is only ever sent to its owner
 * ({@link #broadcastToPlayer}), so no other client knows it exists. Three variants (a Zweefzaadje, b Mos-eikeltje,
 * c Zonnepluisje), each with a happy and a naughty face.
 * <p>
 * It floats along next to its dog. While the dog sniffs and smells something, it floats ahead in the direction of the
 * scent and points (animation "wijs"); on the spot it dances. It is never saved: {@link Maatjes} makes it again whenever
 * its player is a dog on the island.
 */
public class MaatjeEntity extends Entity implements GeoEntity {
    public static final int RUST = 0, BLIJ = 1, STOUT = 2, WIJST = 3;
    private static final EntityDataAccessor<String> DATA_SOORT = SynchedEntityData.defineId(MaatjeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ONDEUGEND = SynchedEntityData.defineId(MaatjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ACTIE = SynchedEntityData.defineId(MaatjeEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), DANS = RawAnimation.begin().thenLoop("blij"),
            STOUTERD = RawAnimation.begin().thenLoop("ondeugend"), WIJS = RawAnimation.begin().thenLoop("wijs");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID eigenaar;
    private int ondeugendTot = -1, actieTot = -1;

    public MaatjeEntity(EntityType<? extends MaatjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SOORT, "b");
        builder.define(DATA_ONDEUGEND, false);
        builder.define(DATA_ACTIE, RUST);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public void zetEigenaar(UUID eigenaar) {
        this.eigenaar = eigenaar;
    }

    /** "a", "b" or "c". */
    public String soort() {
        return entityData.get(DATA_SOORT);
    }

    public void zetSoort(String soort) {
        entityData.set(DATA_SOORT, Honden.MAATJES.contains(soort) ? soort : "b");
    }

    /** The naughty face (false: the happy one). */
    public boolean ondeugend() {
        return entityData.get(DATA_ONDEUGEND);
    }

    public int actie() {
        return entityData.get(DATA_ACTIE);
    }

    /** The naughty face for this many ticks (0: the happy face now). */
    public void zetOndeugend(int ticks) {
        entityData.set(DATA_ONDEUGEND, ticks > 0);
        ondeugendTot = ticks > 0 ? tickCount + ticks : -1;
        if (ticks > 0) {
            doe(STOUT, Math.min(ticks, 32));
        }
    }

    /** It does this for a moment: {@link #BLIJ} (a happy spin) or {@link #STOUT} (a naughty wiggle). */
    public void doe(int actie, int ticks) {
        entityData.set(DATA_ACTIE, actie);
        actieTot = tickCount + ticks;
    }

    /** The whole point of the companion: only its own player's client is ever told about it. */
    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return eigenaar != null && eigenaar.equals(player.getUUID());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if ("c".equals(soort()) && tickCount % 6 == 0) {
                // the Zonnepluisje leaves little sparks of fluff (only its own player has this entity, so only they see them)
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.35, getZ() + (random.nextDouble() - 0.5) * 0.3,
                        0, -0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Player speler = eigenaar == null ? null : level.getPlayerByUUID(eigenaar);
        if (!(speler instanceof ServerPlayer p) || !p.isAlive() || !Hondvorm.actief(p) || !Maatjes.heeft(p)) {
            discard();
            return;
        }
        if (ondeugendTot >= 0 && tickCount > ondeugendTot) {
            ondeugendTot = -1;
            entityData.set(DATA_ONDEUGEND, false);
        }
        Vec3 geur = Snuffelen.doel(p);
        boolean wijst = geur != null && Hondvorm.snuffelt(p);
        Vec3 doel;
        float kijk;
        if (wijst) {
            // ahead of the dog, towards the scent (never past it), nodding at it
            Vec3 naar = new Vec3(geur.x - p.getX(), 0, geur.z - p.getZ());
            double ver = naar.length();
            Vec3 richting = ver < 0.01 ? Vec3.ZERO : naar.scale(1 / ver);
            double stap = Math.min(2.2, Math.max(0.0, ver - 0.2));
            doel = new Vec3(p.getX() + richting.x * stap, p.getY() + 0.9, p.getZ() + richting.z * stap);
            kijk = ver < 0.01 ? p.getYRot() : (float) (Mth.atan2(richting.z, richting.x) * Mth.RAD_TO_DEG) - 90f;
        } else {
            // at the dog's left shoulder, a little ahead, looking where the dog looks
            float rad = p.getYRot() * Mth.DEG_TO_RAD;
            double vx = -Mth.sin(rad), vz = Mth.cos(rad);
            doel = new Vec3(p.getX() + vx * 0.5 + vz * 0.75, p.getY() + 0.95, p.getZ() + vz * 0.5 - vx * 0.75);
            kijk = p.getYRot();
        }
        Vec3 nu = position();
        Vec3 nieuw = nu.distanceToSqr(doel) > 12 * 12 ? doel : nu.lerp(doel, 0.22);
        setPos(nieuw.x, nieuw.y, nieuw.z);
        setYRot(Mth.rotLerp(0.3f, getYRot(), kijk));
        if (actieTot >= 0 && tickCount > actieTot) {
            actieTot = -1;
        }
        if (actieTot < 0) {
            int moet = wijst ? (Snuffelen.opPlek(p) ? BLIJ : WIJST) : RUST;
            if (actie() != moet) {
                entityData.set(DATA_ACTIE, moet);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MaatjeEntity>("actie", 3, state -> state.setAndContinue(switch (actie()) {
            case BLIJ -> DANS;
            case STOUT -> STOUTERD;
            case WIJST -> WIJS;
            default -> IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
