package nl.juiced.guhs.feature.snuffel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * A dog of Het Snuffeleiland as an entity: the one parametric dog of tools/features/snuffel_modellen.py. What it looks
 * like is data on the entity: a named resident ({@code Bewoner}: its own model with its accessories) or any dog
 * ({@code Ras} + {@code Kleur} + {@code Pup}); {@code Speler} = "look like the dog of whoever looks at me" (the stand-in
 * of the viewer in a cutscene; with {@code Pup} the viewer's little brother or sister). It is as tall as its breed.
 * <p>
 * Animations (every dog has them all): idle, walk, snuffel, snuffel_loop (sniffing while walking), zit, kwispel, graaf,
 * blaf. Which one plays follows from the poses it holds ({@link #houding}: the bits of {@link Hondvorm}), from whether it
 * walks, and from the short gestures ({@link #blaf}, {@link #graaf}); a cutscene's {@code animatie(actor, t, name)} with
 * one of those names overrules all of it.
 * <p>
 * Two entity types use this class: {@link BewonerEntity} (a resident: it lives on the server) and {@link HondEntity} (a
 * look only: the dog a client draws in place of a player, a cutscene's actor, a picture in a screen).
 */
public abstract class SnuffelHond extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_BEWONER = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RAS = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_KLEUR = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_PUP = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SPELER = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HOUDING = SynchedEntityData.defineId(SnuffelHond.class, EntityDataSerializers.INT);

    public static final String[] ANIMATIES = {"idle", "walk", "snuffel", "snuffel_loop", "zit", "kwispel", "graaf", "blaf"};
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk"),
            SNUFFEL = RawAnimation.begin().thenLoop("snuffel"), SNUFFEL_LOOP = RawAnimation.begin().thenLoop("snuffel_loop"),
            ZIT = RawAnimation.begin().thenLoop("zit"), KWISPEL = RawAnimation.begin().thenLoop("kwispel"),
            GRAAF = RawAnimation.begin().thenLoop("graaf"), BLAF = RawAnimation.begin().thenLoop("blaf");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client, for a dog that is moved by hand (a player's stand-in): does it walk? Null: ask the animation state. */
    public Boolean loopt;
    /** Client: the entity's own tick count until which it barks / digs. */
    public int blafTot = -1, graafTot = -1;

    protected SnuffelHond(EntityType<? extends SnuffelHond> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEWONER, "");
        builder.define(DATA_RAS, "shiba");
        builder.define(DATA_KLEUR, "rood");
        builder.define(DATA_PUP, false);
        builder.define(DATA_SPELER, false);
        builder.define(DATA_HOUDING, 0);
    }

    // --- the look ------------------------------------------------------------------------------------------------------------

    /** The named resident this is ("" = any dog). */
    public String bewoner() {
        return entityData.get(DATA_BEWONER);
    }

    public String ras() {
        return entityData.get(DATA_RAS);
    }

    public String kleur() {
        return entityData.get(DATA_KLEUR);
    }

    public boolean pup() {
        return entityData.get(DATA_PUP);
    }

    /** Drawn as the dog (or, with {@link #pup}, the puppy) of whoever looks at it. */
    public boolean alsSpeler() {
        return entityData.get(DATA_SPELER);
    }

    /** A named resident (its breed, coat and puppy flag come from the generator's list). */
    public void zetBewoner(String id) {
        Honden.Bewoner b = Honden.bewoner(id);
        if (b == null) {
            return;
        }
        entityData.set(DATA_BEWONER, id);
        entityData.set(DATA_RAS, b.ras());
        entityData.set(DATA_KLEUR, b.kleur());
        entityData.set(DATA_PUP, b.pup());
        setCustomName(b.naam());
        refreshDimensions();
    }

    /** Any dog: a breed, one of its coats, grown or a puppy. */
    public void zetHond(String ras, String kleur, boolean pup) {
        Honden.Ras r = Honden.ras(ras);
        if (r == null) {
            return;
        }
        entityData.set(DATA_BEWONER, "");
        entityData.set(DATA_RAS, ras);
        entityData.set(DATA_KLEUR, r.kleuren().contains(kleur) ? kleur : r.kleuren().get(0));
        entityData.set(DATA_PUP, pup);
        refreshDimensions();
    }

    public void zetAlsSpeler(boolean aan) {
        entityData.set(DATA_SPELER, aan);
    }

    /** The poses it holds: bits {@link Hondvorm#SNUFFELT}, {@link Hondvorm#ZIT}, {@link Hondvorm#KWISPELT}. */
    public int houding() {
        return entityData.get(DATA_HOUDING);
    }

    public void zetHouding(int vlaggen) {
        entityData.set(DATA_HOUDING, vlaggen);
    }

    /** "zit" / "kwispel" / "snuffel" / "" as the island's data writes a pose. */
    public void zetHouding(String naam) {
        zetHouding(switch (naam) {
            case "zit" -> Hondvorm.ZIT;
            case "kwispel" -> Hondvorm.KWISPELT;
            case "snuffel" -> Hondvorm.SNUFFELT;
            default -> 0;
        });
    }

    /** (Client) a bark / a dig for this many ticks. */
    public void blaf(int ticks) {
        blafTot = tickCount + ticks;
    }

    public void graaf(int ticks) {
        graafTot = tickCount + ticks;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_RAS.equals(accessor) || DATA_PUP.equals(accessor)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        Honden.Ras r = Honden.ras(ras());
        boolean pup = pup();
        if (r == null) {
            return super.getDefaultDimensions(pose);
        }
        return EntityDimensions.scalable(pup ? 0.45f : Honden.BREEDTE, r.hoogte(pup)).withEyeHeight(r.oog(pup));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Bewoner", bewoner());
        out.putString("Ras", ras());
        out.putString("Kleur", kleur());
        out.putBoolean("Pup", pup());
        out.putBoolean("Speler", alsSpeler());
        out.putInt("Houding", houding());
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String bewoner = in.getStringOr("Bewoner", "");
        if (!bewoner.isEmpty() && Honden.bewoner(bewoner) != null) {
            zetBewoner(bewoner);
        } else {
            zetHond(in.getStringOr("Ras", "shiba"), in.getStringOr("Kleur", "rood"), in.getBooleanOr("Pup", false));
        }
        zetAlsSpeler(in.getBooleanOr("Speler", false));
        zetHouding(in.getIntOr("Houding", 0));
    }

    // --- a sweet dog: nothing hurts it, nobody pushes it around ------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- animations ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SnuffelHond>("houding", 3, state -> {
            String scene = level().isClientSide() ? Cutscenes.animatie(this) : "";
            if (!scene.isEmpty()) {
                for (String naam : ANIMATIES) {
                    if (naam.equals(scene)) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop(naam));
                    }
                }
            }
            boolean beweegt = loopt != null ? loopt : state.isMoving();
            int h = houding();
            RawAnimation a;
            if (graafTot >= tickCount) {
                a = GRAAF;
            } else if (blafTot >= tickCount) {
                a = BLAF;
            } else if ((h & Hondvorm.SNUFFELT) != 0) {
                a = beweegt ? SNUFFEL_LOOP : SNUFFEL;
            } else if (beweegt) {
                a = WALK;
            } else if ((h & Hondvorm.ZIT) != 0) {
                a = ZIT;
            } else if ((h & Hondvorm.KWISPELT) != 0) {
                a = KWISPEL;
            } else {
                a = IDLE;
            }
            return state.setAndContinue(a);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
