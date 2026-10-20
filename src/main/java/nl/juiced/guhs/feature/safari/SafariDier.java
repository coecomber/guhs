package nl.juiced.guhs.feature.safari;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An animal of Safaripark Vadse Bergen: ONE entity type for all species; its {@link #soort} and {@link #variant} (man / vrouw /
 * baby) are synced and the client draws {@code safari/<soort>[_man|_baby]} (SafariClient). What a species is (hitbox,
 * behaviours, animations) comes from {@link ParkData#soort}.
 * <p>
 * Passive and invulnerable (a sneaking creative player removes it), never despawns, no leash, no boat or minecart. It lives
 * in a <b>home</b> (a centre and a radius, from the park's plekken.json) and in its <b>zone</b> of the park: it strolls only
 * to spots inside both, and every half second it checks where it is. A bit outside the home: it walks back. Far outside, out
 * of its zone, or fallen: it is put straight back on its home (escape-proof, design 4.5c; nothing about fences matters).
 * <p>
 * Behaviours ({@code gedrag} in soorten.json): {@code zee} swims and never walks (sea cows, narwhals, octopuses);
 * {@code water} walks and swims; {@code verstopper} runs from players that do not sneak; {@code boom} hardly moves (it stays
 * on its branch); {@code nacht} is awake at night and sleeps by day (the others the other way round). Now and then it plays
 * one of its extra animations ({@code animaties} minus idle/walk), synced as {@link #actie}.
 * <p>
 * An animal of an older {@link SafariOpslag#generatie generation} (the populations were respawned) removes itself.
 */
public class SafariDier extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_SOORT = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_VARIANT = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIE = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SLAAPT = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("slaap");
    /** How far outside its home an animal may stray before it is put back at once. */
    static final int TELEPORT_MARGE = 8, LOOP_MARGE = 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos thuis = BlockPos.ZERO;
    private int straal = 8;
    private int zone = -1;
    private int plek = -1;
    private int generatie;
    private int actieTot;
    private int volgendeActie = 200;
    @Nullable
    private ParkData.Soort info;
    private boolean geregeld;

    public SafariDier(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // --- set-up ---------------------------------------------------------------------------------------------------------------

    /** Makes this animal a {@code soort} in this variant, living at this home (zone -1: no zone check, e.g. outside the park). */
    public void zet(String soort, String variant, BlockPos thuis, int straal, int zone, int plek, int generatie) {
        entityData.set(DATA_SOORT, soort);
        entityData.set(DATA_VARIANT, variant);
        this.thuis = thuis.immutable();
        this.straal = Math.max(2, straal);
        this.zone = zone;
        this.plek = plek;
        this.generatie = generatie;
        regel();
    }

    /** Navigation, controls and speed for the species (again after loading). */
    private void regel() {
        info = ParkData.get().soort(soort());
        geregeld = true;
        refreshDimensions();
        if (level().isClientSide() || info == null) {
            return;
        }
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(info.snelheid());
        if (info.zee()) {
            this.navigation = new WaterBoundPathNavigation(this, level());
            this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02f, 0.1f, true);
            this.lookControl = new SmoothSwimmingLookControl(this, 10);
            setPathfindingMalus(PathType.WATER, 0f);
        } else if (info.water()) {
            this.navigation = new AmphibiousPathNavigation(this, level());
            this.moveControl = new MoveControl(this);
            this.lookControl = new LookControl(this);
            setPathfindingMalus(PathType.WATER, 0f);
        } else {
            this.navigation = new GroundPathNavigation(this, level());
            setPathfindingMalus(PathType.WATER, -1f);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return info != null && !info.zee() && !info.water() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7f, 1.0, 1.25,
                e -> e instanceof Player p && !p.isCrouching() && !p.isSpectator()) {
            @Override
            public boolean canUse() {
                return info != null && info.heeft("verstopper") && !slaapt() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new ThuisStroll());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f) {
            @Override
            public boolean canUse() {
                return !slaapt() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !slaapt() && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SOORT, "");
        builder.define(DATA_VARIANT, "vrouw");
        builder.define(DATA_ACTIE, "");
        builder.define(DATA_SLAAPT, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SOORT.equals(key) || DATA_VARIANT.equals(key)) {
            info = ParkData.get().soort(soort());
            refreshDimensions();
        }
    }

    public String soort() {
        return entityData.get(DATA_SOORT);
    }

    public String variant() {
        return entityData.get(DATA_VARIANT);
    }

    public String actie() {
        return entityData.get(DATA_ACTIE);
    }

    public boolean slaapt() {
        return entityData.get(DATA_SLAAPT);
    }

    public BlockPos thuis() {
        return thuis;
    }

    public int straal() {
        return straal;
    }

    public int plek() {
        return plek;
    }

    @Override
    public boolean isBaby() {
        return "baby".equals(variant());
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        ParkData.Soort s = info != null ? info : ParkData.get().soort(soort());
        float w = s == null ? 0.9f : s.breedte(), h = s == null ? 1.2f : s.hoogte();
        float f = isBaby() ? (s == null ? 0.55f : s.babySchaal()) : 1f;
        return EntityDimensions.scalable(w * f, h * f);
    }

    @Override
    protected Component getTypeName() {
        String s = soort();
        return s.isEmpty() ? super.getTypeName() : Component.translatable("entity.guhs.safari." + s);
    }

    // --- living ---------------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!geregeld) {
            regel();
        }
        if (generatie < SafariOpslag.get(level.getServer()).generatie() && plek >= 0) {
            discard();
            return;
        }
        if (isPassenger()) {
            stopRiding();
        }
        if ((tickCount + getId()) % 10 == 0) {
            bewaak(level);
            slaap(level);
        }
        if (actieTot > 0 && --actieTot == 0) {
            entityData.set(DATA_ACTIE, "");
        }
        if (--volgendeActie <= 0) {
            volgendeActie = 200 + random.nextInt(600);
            List<String> extra = info == null ? List.of() : info.extra();
            if (!extra.isEmpty() && !slaapt() && getNavigation().isDone()) {
                entityData.set(DATA_ACTIE, extra.get(random.nextInt(extra.size())));
                actieTot = 60;
            }
        }
    }

    /** Keeps the animal home: walk back when a little outside, be put back when far outside, fallen, or out of its zone. */
    void bewaak(ServerLevel level) {
        double dx = getX() - (thuis.getX() + 0.5), dz = getZ() - (thuis.getZ() + 0.5);
        double afstand = Math.sqrt(dx * dx + dz * dz);
        boolean uitZone = zone >= 0 && Safari.echt(level) && ParkData.get().zone(getBlockX(), getBlockZ()) != zone;
        boolean gevallen = getY() < thuis.getY() - 24 || getY() < level.getMinY() + 2;
        if (afstand > straal + TELEPORT_MARGE || uitZone || gevallen) {
            terugNaarHuis(level);
        } else if (afstand > straal + LOOP_MARGE && getNavigation().isDone()) {
            getNavigation().moveTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, 1.0);
        }
    }

    /** Straight back to a free spot at home (its centre). */
    void terugNaarHuis(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        BlockPos p = grond(level, thuis.getX(), thuis.getZ());
        teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        fallDistance = 0;
    }

    /** The spot to stand at in this column: water animals in the water, others on the ground (not on leaves). */
    BlockPos grond(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY()) {
            return thuis;
        }
        if (info != null && info.zee()) {
            BlockPos w = new BlockPos(x, y - 2, z);
            return level.getFluidState(w).isEmpty() ? new BlockPos(x, y, z) : w;
        }
        return new BlockPos(x, y, z);
    }

    private void slaap(ServerLevel level) {
        boolean nacht = info != null && info.heeft("nacht");
        boolean donker = level.isDarkOutside();
        // most animals sleep in the dark (nocturnal ones by day), but not all of them at once
        boolean moet = (nacht != donker) && Math.floorMod(getUUID().hashCode(), 10) < 7;
        if (moet != slaapt()) {
            entityData.set(DATA_SLAAPT, moet);
            if (moet) {
                getNavigation().stop();
            }
        }
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        if (info != null && info.zee()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            return;
        }
        super.travelInWater(input, baseGravity, isFalling, oldY);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return info == null || !info.zee();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.isCreativePlayer() && source.getEntity() instanceof Player p && p.isShiftKeyDown()) {
            discard();
            return true;
        }
        return false;
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
    public boolean startRiding(Entity entity, boolean force, boolean sendEventAndTriggers) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    // --- saving ---------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Soort", soort());
        tag.putString("Variant", variant());
        tag.putInt("ThuisX", thuis.getX());
        tag.putInt("ThuisY", thuis.getY());
        tag.putInt("ThuisZ", thuis.getZ());
        tag.putInt("Straal", straal);
        tag.putInt("Zone", zone);
        tag.putInt("Plek", plek);
        tag.putInt("Generatie", generatie);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_SOORT, tag.getStringOr("Soort", ""));
        entityData.set(DATA_VARIANT, tag.getStringOr("Variant", "vrouw"));
        thuis = new BlockPos(tag.getIntOr("ThuisX", getBlockX()), tag.getIntOr("ThuisY", getBlockY()), tag.getIntOr("ThuisZ", getBlockZ()));
        straal = tag.getIntOr("Straal", 8);
        zone = tag.getIntOr("Zone", -1);
        plek = tag.getIntOr("Plek", -1);
        generatie = tag.getIntOr("Generatie", 0);
        geregeld = false;
    }

    // --- animation ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SafariDier>("beweeg", 4, state -> {
            String a = actie();
            if (slaapt() && info != null && info.animaties().contains("slaap")) {
                return state.setAndContinue(SLAAP);
            }
            if (!a.isEmpty() && !state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(a));
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- strolling at home ----------------------------------------------------------------------------------------------------

    /** Now and then a walk (or swim) to a random spot that lies in the home AND in the zone; never while asleep. */
    final class ThuisStroll extends Goal {
        private int pauze;

        ThuisStroll() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (slaapt() || info == null || !(level() instanceof ServerLevel level)) {
                return false;
            }
            if (pauze > 0) {
                pauze--;
                return false;
            }
            int kans = info.heeft("boom") ? 600 : info.zee() ? 60 : 120;
            if (random.nextInt(kans) != 0) {
                return false;
            }
            BlockPos doel = kiesDoel(level);
            if (doel == null) {
                return false;
            }
            getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !slaapt() && !getNavigation().isDone();
        }

        @Override
        public void stop() {
            pauze = 20 + random.nextInt(60);
        }

        @Nullable
        private BlockPos kiesDoel(ServerLevel level) {
            boolean boom = info.heeft("boom");
            int r = boom ? Math.min(4, straal) : straal;
            for (int poging = 0; poging < 8; poging++) {
                double hoek = random.nextDouble() * Mth.TWO_PI, d = Math.sqrt(random.nextDouble()) * r;
                int x = (boom ? getBlockX() : thuis.getX()) + (int) Math.round(Math.cos(hoek) * d);
                int z = (boom ? getBlockZ() : thuis.getZ()) + (int) Math.round(Math.sin(hoek) * d);
                if (Math.hypot(x - thuis.getX(), z - thuis.getZ()) > straal) {
                    continue;
                }
                if (zone >= 0 && Safari.echt(level) && ParkData.get().zone(x, z) != zone) {
                    continue;
                }
                if (boom) {
                    return new BlockPos(x, getBlockY(), z);
                }
                BlockPos p = grond(level, x, z);
                FluidState f = level.getFluidState(p.below());
                boolean nat = !f.isEmpty() || !level.getFluidState(p).isEmpty();
                if (info.zee() && !nat || !info.zee() && !info.water() && nat) {
                    continue;
                }
                return p;
            }
            return null;
        }
    }
}
