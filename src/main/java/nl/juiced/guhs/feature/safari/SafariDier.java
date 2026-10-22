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
    /** Round 3: the id of a "bekende bewoner" (bewoners.json), "" for an ordinary animal. */
    private static final EntityDataAccessor<String> DATA_BEWONER = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.STRING);
    /** Round 4 (L4): its colour ("kleuren" in soorten.json: the chameleon, the poison dart frog), "" for the base texture. */
    private static final EntityDataAccessor<String> DATA_KLEUR = SynchedEntityData.defineId(SafariDier.class, EntityDataSerializers.STRING);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("slaap");
    /** Round 3: activity animations that also play while it moves (the belly slide, carrying a branch, running). */
    static final java.util.Set<String> BEWEGEND = java.util.Set.of("buikglij", "draag", "ren", "bouw_sjouw");   // (F10a: a helper carrying while it walks)
    /** How far outside its home an animal may stray before it is put back at once. */
    static final int TELEPORT_MARGE = 8, LOOP_MARGE = 2;
    /** (S3) True while this animal is calm and does not hide from players (the feeding time of its zone). */
    public static java.util.function.Predicate<SafariDier> KALM = d -> false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos thuis = BlockPos.ZERO;
    private int straal = 8;
    private int zone = -1;
    private int plek = -1;
    private int generatie;
    private int actieTot;
    @Nullable
    private ParkData.Soort info;
    /** (BezigheidGoal: not before regel() set its navigation up.) */
    boolean geregeld;
    /** Where a hanging animal (the sloth) hangs under a branch; null: it walks. Saved. */
    @Nullable
    private BlockPos hangPlek;
    /** How many checks in a row a land animal was in deep water (2+ blocks). */
    private int diepTel;
    /** Ticks until a species with an idle_mix picks its next standing animation. */
    private int mixPauze = 40;
    /** Round 3: a nap by day (ticks left; it sleeps meanwhile, eyes shut). */
    int dutTot;
    /** Round 3: while it visits a spot outside its home (a favourite in view of a viewpoint, the slide, the tunnel, the dam),
     *  its home reaches that far (bewaak); null: its own home only. */
    @Nullable
    BlockPos bezoek;
    int bezoekStraal;
    /** Round 3: how long a guh may still ride along (the capybara). */
    private int rijdtTot;
    /** Round 4: the custom name currently is the debug label. */
    private boolean debugNaam, zichtbaarVoorDebug;
    @Nullable
    private Component naamVoorDebug;
    /** Round 4 (L4): where it stays put (a little creature in its terrariumbord tank): no AI, no physics; null: it moves. Saved. */
    @Nullable
    private Vec3 vast;
    /** Round 4 (L4): ticks until the chameleon changes colour again. */
    private int kleurPauze = 600;
    /** Round 4 (L4): how far above its home's centre the floor may lie before it counts as indoors (a roof over it). */
    static final int BINNEN = 4;
    /** (Tests) act even without a player nearby (and stay awake at night; favourites outside the park). */
    public static boolean ALTIJD_ACTIEF = false;
    @Nullable
    BezigheidGoal bezigheid;

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
        if (info != null && !info.kleuren().isEmpty() && kleur().isEmpty()) {
            entityData.set(DATA_KLEUR, info.kleuren().get(random.nextInt(info.kleuren().size())));
        }
    }

    /** Round 4 (L4): stays put at {@code p} (its terrariumbord tank): no AI, no gravity, nothing pushes it. */
    public void zetVast(Vec3 p) {
        vast = p;
        snapTo(p.x, p.y, p.z, getYRot(), 0f);
        regel();
    }

    @Nullable
    public Vec3 vast() {
        return vast;
    }

    /** Round 4 (L4): its colour texture suffix ("" = the base texture). */
    public String kleur() {
        return entityData.get(DATA_KLEUR);
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
            // land animals stay out of the water; a wading one (the elephant) may step into it at the edge, never into
            // deep water (kiesDoel never picks a deep target, and bewaak pulls it out)
            this.navigation = new GroundPathNavigation(this, level());
            setPathfindingMalus(PathType.WATER, info.wadend() ? 8f : -1f);
        }
        setNoGravity(hangPlek != null);
        // round 4 (L4): the anemone is a still decoration; a creature in its tank stays put
        setNoAi(info.decor() || vast != null);
        if (vast != null) {
            setNoGravity(true);
            noPhysics = true;
        }
    }

    /** (Step 2, S5: a verblijfsbord animal at home) moves its home; the animal walks there by itself. */
    public void zetThuis(BlockPos thuis, int straal) {
        this.thuis = thuis.immutable();
        this.straal = Math.max(2, straal);
    }

    /** Does x,z lie in this animal's home? The park: a circle around the centre (a home animal may have another shape). */
    protected boolean binnenThuis(int x, int z) {
        return Math.hypot(x - thuis.getX(), z - thuis.getZ()) <= straal;
    }

    /** Plays one of its animations for a while (petting, friends meeting...). */
    public void doeActie(String animatie, int ticks) {
        entityData.set(DATA_ACTIE, animatie);
        actieTot = Math.max(1, ticks);
    }

    /** Hangs this animal still under a branch at {@code plek} (its feet; the "hang" animation hangs from its top). */
    public void hang(BlockPos plek) {
        hangPlek = plek.immutable();
        setNoGravity(true);
    }

    public boolean hangt() {
        return hangPlek != null;
    }

    /** (Step 2, lobby) plays a named animation of its species now, for {@code ticks} ticks (e.g. "blij", "eet", "spuug"). */
    public void speelActie(String actie, int ticks) {
        entityData.set(DATA_ACTIE, actie);
        actieTot = Math.max(1, ticks);
    }

    /** (Step 2, lobby) moves its home for a while (the alpaca walking along with a player); zone -1: no zone check. */
    public void zetThuis(BlockPos thuis, int straal, int zone) {
        this.thuis = thuis.immutable();
        this.straal = Math.max(2, straal);
        this.zone = zone;
    }

    public int zoneIndex() {
        return zone;
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
                return info != null && info.heeft("verstopper") && hangPlek == null && !slaapt() && !KALM.test(SafariDier.this)
                        && !(Safari.echt(level()) && nl.juiced.guhs.feature.safari.gedrag.Kijkplekken.get().inZicht(getX(), getY(), getZ()))
                        && super.canUse();
            }
        });
        bezigheid = new BezigheidGoal(this);
        goalSelector.addGoal(5, bezigheid);
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
        builder.define(DATA_BEWONER, "");
        builder.define(DATA_KLEUR, "");
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

    /** Round 3: the "bekende bewoner" id ("" for an ordinary animal). */
    public String bewoner() {
        return entityData.get(DATA_BEWONER);
    }

    /** Round 3: makes this animal a named resident (a visible name tag). */
    public void zetBewoner(String id, String naam) {
        entityData.set(DATA_BEWONER, id);
        setCustomName(Component.literal(naam));
        setCustomNameVisible(true);
    }

    /** Round 3: the spot it is visiting now (null: at home). */
    @Nullable
    public BlockPos bezoek() {
        return bezoek;
    }

    /** Round 3: what it is busy with now (null: nothing, e.g. no player near). */
    @Nullable
    public BezigheidGoal.Soort bezigheid() {
        return bezigheid == null ? null : bezigheid.nu();
    }

    @Nullable
    ParkData.Soort info() {
        return info;
    }

    void zetActie(String a, int ticks) {
        entityData.set(DATA_ACTIE, a);
        actieTot = Math.max(1, ticks);
    }

    void stopActie() {
        entityData.set(DATA_ACTIE, "");
        actieTot = 0;
    }

    /** Round 3 (GuhReacties): lets a guh climb on for a little ride (the capybara). */
    public boolean neemMee(Entity guh) {
        if (!canAddPassenger(guh) || slaapt()) {
            return false;
        }
        if (guh.startRiding(this, true, true)) {
            rijdtTot = 160 + random.nextInt(120);
            return true;
        }
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return "capibara".equals(soort()) && passenger instanceof nl.juiced.guhs.entity.GuhEntity && getPassengers().isEmpty();
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
        float f = (isBaby() ? (s == null ? 0.55f : s.babySchaal()) : 1f) * (s == null ? 1f : s.schaal());
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
            if ("narwal".equals(soort()) && (tickCount + getId()) % 5 == 0) {
                // round 4: the narwhal, the rarest park animal, glitters softly (also under water) so you know it is special
                double r = getBbWidth() * 0.8;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.GLOW, getX() + (random.nextDouble() - 0.5) * r * 2,
                        getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * r * 2, 0, 0.01, 0);
                if (random.nextInt(3) == 0) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * r * 2,
                            getY() + getBbHeight() * (0.5 + random.nextDouble() * 0.6), getZ() + (random.nextDouble() - 0.5) * r * 2,
                            0, 0.005, 0);
                }
            }
            if (slaapt() && (tickCount + getId()) % 30 == 10) {
                // round 3: zzz over its head, just like a sleeping guh
                level().addParticle(nl.juiced.guhs.feature.emotes.EmotesFeature.GUH_ZZZ.get(), getX(), getY() + getBbHeight() + 0.1, getZ(),
                        0.01 + random.nextDouble() * 0.01, 0.03, 0);
            }
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
        if (vast != null) {
            if (distanceToSqr(vast) > 0.0025) {
                setPos(vast);
            }
            setDeltaMovement(Vec3.ZERO);
        } else if (hangPlek != null) {
            hangStil();
        } else if ((tickCount + getId()) % 10 == 0) {
            bewaak(level);
        }
        if (info != null && info.heeft("kleurwissel") && !slaapt() && --kleurPauze <= 0) {
            // round 4 (L4): the chameleon slowly changes colour (now and then another one of its colours)
            kleurPauze = 900 + random.nextInt(1500);
            List<String> k = info.kleuren();
            if (k.size() > 1) {
                String nu = kleur(), nieuw = nu;
                while (nieuw.equals(nu)) {
                    nieuw = k.get(random.nextInt(k.size()));
                }
                entityData.set(DATA_KLEUR, nieuw);
            }
        }
        if ((tickCount + getId()) % 10 == 0) {
            slaap(level);
        }
        if (actieTot > 0 && --actieTot == 0) {
            entityData.set(DATA_ACTIE, "");
        }
        if (hangPlek == null && info != null && !info.idleMix().isEmpty() && actie().isEmpty() && !slaapt()
                && getNavigation().isDone() && --mixPauze <= 0) {
            // most of its standing time in one of these poses (the meerkat upright on guard, looking around)
            mixPauze = 20 + random.nextInt(40);
            float r = random.nextFloat(), som = 0f;
            for (java.util.Map.Entry<String, Float> e : info.idleMix().entrySet()) {
                som += e.getValue();
                if (r < som) {
                    entityData.set(DATA_ACTIE, e.getKey());
                    actieTot = 120 + random.nextInt(200);
                    break;
                }
            }
        }
        if (dutTot > 0) {
            dutTot--;
        }
        if (isVehicle() && --rijdtTot <= 0) {
            ejectPassengers();
        }
        if ((tickCount + getId()) % 40 == 0 && !slaapt() && level.hasNearbyAlivePlayer(getX(), getY(), getZ(), 24)) {
            nl.juiced.guhs.feature.safari.gedrag.GuhReacties.kijk(level, this);
        }
        if ((tickCount + getId()) % 60 == 0 && !slaapt() && hangPlek == null) {
            nieuwsgierig(level);
        }
        debug();
    }

    /** Round 4: the debug label and the measurement (SafariDebug). */
    private void debug() {
        if (nl.juiced.guhs.feature.safari.gedrag.SafariDebug.meten) {
            double dx = getX() - xo, dz = getZ() - zo;
            nl.juiced.guhs.feature.safari.gedrag.SafariDebug.tik(soort(), getId(), Math.sqrt(dx * dx + dz * dz), slaapt(),
                    bezigheid() != null && !BezigheidGoal.rustig(bezigheid()));
        }
        boolean aan = nl.juiced.guhs.feature.safari.gedrag.SafariDebug.labels;
        if (aan && (tickCount + getId()) % 10 == 0) {
            BezigheidGoal.Soort b = bezigheid();
            String t = (b == null ? "-" : b.name().toLowerCase(java.util.Locale.ROOT))
                    + (bezigheid != null && !bezigheid.actie().isEmpty() ? " " + bezigheid.actie() : "")
                    + (actie().isEmpty() ? "" : " [" + actie() + "]") + (slaapt() ? " zzz" : "")
                    + (getNavigation().isDone() ? "" : " >");
            if (!debugNaam) {
                naamVoorDebug = getCustomName();
                zichtbaarVoorDebug = isCustomNameVisible();
            }
            setCustomName(Component.literal(t));
            setCustomNameVisible(true);
            debugNaam = true;
        } else if (!aan && debugNaam) {
            debugNaam = false;
            setCustomName(naamVoorDebug);
            setCustomNameVisible(zichtbaarVoorDebug);
        }
    }

    /** Round 4: reactions to a player close by. A curious animal (not a hider) now and then stops, turns to look at you and
     *  plays a happy or a look-around animation; a hider darts away (its AvoidEntityGoal). Once in a while per animal. */
    private void nieuwsgierig(ServerLevel level) {
        if (info == null || info.heeft("verstopper") || !actie().isEmpty() || random.nextInt(4) != 0) {
            return;
        }
        Player p = level.getNearestPlayer(this, 6);
        if (p == null || p.isSpectator() || (bezigheid() != null && bezigheid() == BezigheidGoal.Soort.GEDRAG)) {
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(p, 30f, 30f);
        String a = info.animaties().contains("rondkijk") && random.nextBoolean() ? "rondkijk" : info.animaties().contains("blij") ? "blij" : "";
        if (!a.isEmpty()) {
            zetActie(a, 50);
        }
        if (random.nextInt(3) == 0) {
            roep();
        }
    }

    /** Round 4: its own call, now (shows, curiosity). */
    void roep() {
        playAmbientSound();
    }

    /** A hanging animal stays exactly where it hangs (no gravity, no drifting) and plays "hang". */
    private void hangStil() {
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (distanceToSqr(hangPlek.getX() + 0.5, hangPlek.getY(), hangPlek.getZ() + 0.5) > 0.25) {
            teleportTo(hangPlek.getX() + 0.5, hangPlek.getY(), hangPlek.getZ() + 0.5);
        }
        if (!"hang".equals(actie())) {
            entityData.set(DATA_ACTIE, "hang");
        }
        actieTot = 0;
    }

    /** How many water blocks stand on top of each other from {@code p} down (0: dry), at most 3. */
    static int waterDiepte(Level level, BlockPos p) {
        int n = 0;
        while (n < 3 && !level.getFluidState(p.below(n)).isEmpty()) {
            n++;
        }
        return n;
    }

    /** Keeps the animal home: walk back when a little outside, be put back when far outside, fallen, or out of its zone. */
    protected void bewaak(ServerLevel level) {
        if (info != null && !info.zee() && !info.water()) {
            // a land animal in deep water (2+): out of it, to its home; still in it after ~5 s: put back home
            int diep = isInWater() ? Math.max(waterDiepte(level, blockPosition()), waterDiepte(level, blockPosition().above()) + 1) : 0;
            if (diep >= 2) {
                if (++diepTel > 10) {
                    diepTel = 0;
                    terugNaarHuis(level);
                    return;
                }
                if (getNavigation().isDone()) {
                    BlockPos g = grond(level, thuis.getX(), thuis.getZ());
                    getNavigation().moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 1.2);
                }
            } else {
                diepTel = 0;
            }
        }
        double dx = getX() - (thuis.getX() + 0.5), dz = getZ() - (thuis.getZ() + 0.5);
        double afstand = Math.sqrt(dx * dx + dz * dz);
        int straal = this.straal;
        if (bezoek != null) {
            // visiting a spot: its home reaches up to there (the way there and back included)
            straal = Math.max(straal, (int) Math.ceil(Math.hypot(bezoek.getX() - thuis.getX(), bezoek.getZ() - thuis.getZ())) + bezoekStraal);
        }
        boolean uitZone = zone >= 0 && Safari.echt(level) && ParkData.get().zone(getBlockX(), getBlockZ()) != zone;
        boolean gevallen = getY() < thuis.getY() - 24 || getY() < level.getMinY() + 2;
        if (afstand > straal + TELEPORT_MARGE || uitZone || gevallen) {
            terugNaarHuis(level);
        } else if (afstand > straal + LOOP_MARGE && getNavigation().isDone()) {
            getNavigation().moveTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, 1.0);
        }
    }

    /** Straight back to a free spot at home (its centre). */
    protected void terugNaarHuis(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        BlockPos p = grond(level, thuis.getX(), thuis.getZ());
        teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        fallDistance = 0;
    }

    /** The spot to stand at in this column: water animals in the water, others on the ground (not on leaves). */
    public BlockPos grond(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY()) {
            return thuis;
        }
        if (!thuis.equals(BlockPos.ZERO) && y > thuis.getY() + BINNEN) {
            // round 4 (L4): a roof over its home (the Kriebelhuis): the floor or the water near its home's height
            BlockPos b = binnenGrond(level, x, z, thuis.getY(), info != null && info.zee(), info != null && info.heeft("bodem"));
            return b != null ? b : thuis;
        }
        if (info != null && info.zee()) {
            if (level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                return new BlockPos(x, y, z);
            }
            // test round 5: a swimmer in the middle of the water (it hugged the surface), a bottom dweller on the bottom
            return waterPlek(level, x, y - 1, z, level.getMinY() + 1, info.heeft("bodem"));
        }
        return new BlockPos(x, y, z);
    }

    /** Round 4 (L4): indoors, the spot in this column near height {@code rond} (from 3 above down to 8 below): a land animal
     *  on the first floor with room for it, a swimmer in the middle of the water (test round 5; it was 1 below the surface,
     *  so the Diepzeezaal's fish swam against the ceiling) and a bottom dweller on the bottom; null: none here. */
    @Nullable
    static BlockPos binnenGrond(net.minecraft.world.level.LevelReader level, int x, int z, int rond, boolean zee, boolean bodem) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = rond + 3; y >= rond - 8; y--) {
            p.set(x, y, z);
            if (zee) {
                if (!level.getFluidState(p).isEmpty()) {
                    return waterPlek(level, x, y, z, rond - 12, bodem);
                }
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    return null;                                // a floor without water: not for a swimmer
                }
                continue;
            }
            BlockPos voet = p.immutable();
            if (level.getBlockState(voet).getCollisionShape(level, voet).isEmpty() && level.getFluidState(voet).isEmpty()
                    && !level.getBlockState(voet.below()).getCollisionShape(level, voet.below()).isEmpty()) {
                return voet;
            }
        }
        return null;
    }

    /**
     * Test round 5: where a swimmer goes in the water column whose top water block is {@code boven}: a bottom dweller on
     * the bottom, a swimmer in the middle (never in the top block, nor the top two of water 4 or more deep).
     */
    static BlockPos waterPlek(net.minecraft.world.level.LevelReader level, int x, int boven, int z, int ondergrens, boolean bodem) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int onder = boven;
        while (onder > ondergrens && !level.getFluidState(p.set(x, onder - 1, z)).isEmpty()) {
            onder--;
        }
        if (bodem) {
            return new BlockPos(x, onder, z);
        }
        int diep = boven - onder + 1;
        int y = (boven + onder) / 2;
        if (diep >= 2) {
            y = Math.min(y, boven - (diep >= 4 ? 2 : 1));
        }
        return new BlockPos(x, Math.max(onder, y), z);
    }

    /** Round 4 (L4): is it awake at night? Nocturnal species; a Nachtzaal species only at home: in the park's Nachtzaal day
     *  and night are turned round, so there it is awake by day (when the visitors come). */
    public static boolean nachtdier(@Nullable ParkData.Soort info, boolean inPark) {
        return info != null && (info.heeft("nacht") || info.heeft("nachtzaal") && !inPark);
    }

    private void slaap(ServerLevel level) {
        boolean nacht = nachtdier(info, Safari.echt(level));
        boolean donker = level.isDarkOutside();
        // most animals sleep in the dark (nocturnal ones by day), but not all of them at once
        // round 4: nocturnal species sleep by day only in part (4 in 10), so in the park you still see badgers, hedgehogs
        // and salamanders; the others sleep at night as before (7 in 10)
        int deel = nacht && !donker ? 4 : 7;
        boolean moet = !ALTIJD_ACTIEF && (nacht != donker) && Math.floorMod(getUUID().hashCode(), 10) < deel && bezoek == null || dutTot > 0;
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

    /** S6: every species its own cute call (soorten.json "geluid"; babies higher, as vanilla does for babies). */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return nl.juiced.guhs.feature.safari.spullen.SafariGeluid.voorSoort(soort());
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
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

    /**
     * 1.6.2: a wild one (entity tag {@link #WILD}, the Beverbeek's beavers): comes and goes like the mod's other wild
     * animals (WildeDieren: not saved, despawns far from every player). Every other safari animal stays for good.
     */
    public static final String WILD = "guhs_wild_safaridier";

    public boolean isWild() {
        return entityTags().contains(WILD);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return isWild();
    }

    @Override
    public boolean isPersistenceRequired() {
        return !isWild();
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
        if (!bewoner().isEmpty()) {
            tag.putString("Bewoner", bewoner());
        }
        if (!kleur().isEmpty()) {
            tag.putString("Kleur", kleur());
        }
        if (vast != null) {
            tag.putDouble("VastX", vast.x);
            tag.putDouble("VastY", vast.y);
            tag.putDouble("VastZ", vast.z);
        }
        if (hangPlek != null) {
            tag.putInt("HangX", hangPlek.getX());
            tag.putInt("HangY", hangPlek.getY());
            tag.putInt("HangZ", hangPlek.getZ());
        }
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
        entityData.set(DATA_BEWONER, tag.getStringOr("Bewoner", ""));
        entityData.set(DATA_KLEUR, tag.getStringOr("Kleur", ""));
        double vy = tag.getDoubleOr("VastY", Double.NaN);
        vast = Double.isNaN(vy) ? null : new Vec3(tag.getDoubleOr("VastX", 0), vy, tag.getDoubleOr("VastZ", 0));
        int hy = tag.getIntOr("HangY", Integer.MIN_VALUE);
        hangPlek = hy == Integer.MIN_VALUE ? null : new BlockPos(tag.getIntOr("HangX", 0), hy, tag.getIntOr("HangZ", 0));
        geregeld = false;
    }

    // --- animation ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SafariDier>("beweeg", 4, state -> {
            String a = actie();
            if ("hang".equals(a)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("hang"));
            }
            if (slaapt() && info != null && info.animaties().contains("slaap")) {
                return state.setAndContinue(SLAAP);
            }
            if (!a.isEmpty() && (!state.isMoving() || BEWEGEND.contains(a))) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(a));
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- strolling at home: see BezigheidGoal (round 3: activities instead of only strolling) -------------------------------

    /** A random spot in its home AND its zone to walk (or swim) to (null: none found this time). */
    @Nullable
    BlockPos kiesDoel(ServerLevel level) {
        boolean boom = info.heeft("boom");
        int r = boom ? Math.min(4, straal) : straal;
        for (int poging = 0; poging < 8; poging++) {
            double hoek = random.nextDouble() * Mth.TWO_PI, d = Math.sqrt(random.nextDouble()) * r;
            int x = (boom ? getBlockX() : thuis.getX()) + (int) Math.round(Math.cos(hoek) * d);
            int z = (boom ? getBlockZ() : thuis.getZ()) + (int) Math.round(Math.sin(hoek) * d);
            if (!binnenThuis(x, z)) {
                continue;
            }
            BlockPos p = magStaan(level, x, z);
            if (p != null) {
                return boom ? new BlockPos(x, getBlockY(), z) : p;
            }
        }
        return null;
    }

    /** Where it may stand in this column (its zone, water rules of its species), null: not here. */
    @Nullable
    BlockPos magStaan(ServerLevel level, int x, int z) {
        if (zone >= 0 && Safari.echt(level) && ParkData.get().zone(x, z) != zone) {
            return null;
        }
        if (info.heeft("boom")) {
            return new BlockPos(x, getBlockY(), z);
        }
        BlockPos p = grond(level, x, z);
        FluidState f = level.getFluidState(p.below());
        boolean nat = !f.isEmpty() || !level.getFluidState(p).isEmpty();
        if (info.zee() && !nat) {
            return null;
        }
        if (!info.zee() && !info.water() && nat) {
            // a wading animal may stand in 1-deep water at the edge; nothing else goes into the water
            if (!info.wadend() || waterDiepte(level, p.below()) + waterDiepte(level, p) > 1) {
                return null;
            }
        }
        if (!info.zee() && !ruimte(level, p)) {
            return null;                    // R5: a big animal (giraffe, olifant at 1.5x) only where its whole body fits
        }
        return p;
    }

    /** Is this a big animal (its path and spots need room: under trees and roofs a giraffe gets stuck)? */
    public boolean groot() {
        return getBbWidth() > 1.2f || getBbHeight() > 2.6f;
    }

    /** R5: does its body fit standing at p (no blocks in its box)? Small animals: always (the old behaviour). */
    public boolean ruimte(net.minecraft.world.level.LevelReader level, BlockPos p) {
        if (!groot()) {
            return true;
        }
        net.minecraft.world.phys.AABB box = getDimensions(getPose()).makeBoundingBox(p.getX() + 0.5, p.getY(), p.getZ() + 0.5).deflate(0.05);
        return level instanceof net.minecraft.world.level.Level l ? l.noBlockCollision(this, box) : true;
    }

    int straalThuis() {
        return straal;
    }

    int zoneIdx() {
        return zone;
    }
}
