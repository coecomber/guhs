package nl.juiced.guhs.feature.ring;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-kern): Smikagol, the thin Mika who lost his vadsje (entity {@code guhs:smikagol}; resources:
 * tools/features/ring_modellen.py). He is sweet in his own sneaky way and never hurts anybody.
 * <ul>
 *   <li><b>The guide</b> ({@link #GIDS}; chapters 5 and 6, through {@link Smikagol}): he belongs to ONE player and only that
 *       player's game knows about him. He follows on all fours, or scurries ahead along a route the chapter gives him and
 *       waits when his player falls behind ("Deze kant, vadsje!").</li>
 *   <li><b>The buddy</b> ({@link #MAATJE}; once per player after the story, {@link Smikagol#maakMaatje}): he follows his
 *       player or stays where he is told (crouch + click), and near water he fishes: a fish every one to two minutes into
 *       his own little stash of at most {@link #VIS_MAX}; a click hands them over ("lekkere vissss").</li>
 * </ul>
 */
public class SmikagolEntity extends PathfinderMob implements GeoEntity {
    public static final int GIDS = 0, MAATJE = 1;
    /** What he is doing (synced, for the animation): nothing special, fishing, creeping (leading the way), grabbing. */
    public static final int NIETS = 0, VIST = 1, KRUIPT = 2, GRIJPT = 3;
    public static final int VIS_MAX = 6;
    /** A fish takes between VIS_TIJD and 2 x VIS_TIJD ticks. */
    public static final int VIS_TIJD = 1200;
    /** The guide waits for his player when they are this far behind. */
    public static final double WACHT_AFSTAND = 9;

    private static final EntityDataAccessor<Integer> DATA_MODUS = SynchedEntityData.defineId(SmikagolEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DOET = SynchedEntityData.defineId(SmikagolEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BLIJFT = SynchedEntityData.defineId(SmikagolEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.smikagol.idle");
    private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("animation.smikagol.loop");
    private static final RawAnimation KRUIP = RawAnimation.begin().thenLoop("animation.smikagol.kruip");
    private static final RawAnimation VIS = RawAnimation.begin().thenLoop("animation.smikagol.vis");
    private static final RawAnimation GRIJP = RawAnimation.begin().thenPlay("animation.smikagol.grijp");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID eigenaar;
    /** A buddy belongs to the "generation" of its owner: calling him with the Vissenbotje makes a new one and retires the old. */
    private int generatie;
    private int vissen, visTijd = VIS_TIJD, waterKijk, grijpTot;
    private boolean bijWater;
    /** The route the guide leads his player along (world positions), where he is on it, and what happens at the end. */
    private final List<Vec3> route = new ArrayList<>();
    private int routeStap;
    @Nullable
    private Consumer<ServerPlayer> naRoute;

    public SmikagolEntity(EntityType<? extends SmikagolEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODUS, GIDS);
        builder.define(DATA_DOET, NIETS);
        builder.define(DATA_BLIJFT, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Leid());
        this.goalSelector.addGoal(2, new Volg());
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // --- who he is ---------------------------------------------------------------------------------------------------------

    public int modus() {
        return this.entityData.get(DATA_MODUS);
    }

    public boolean isMaatje() {
        return modus() == MAATJE;
    }

    public int doet() {
        return this.entityData.get(DATA_DOET);
    }

    private void zetDoet(int doet) {
        if (doet() != doet) {
            this.entityData.set(DATA_DOET, doet);
        }
    }

    /** A buddy that was told to stay. */
    public boolean blijft() {
        return this.entityData.get(DATA_BLIJFT);
    }

    public void zetBlijft(boolean blijft) {
        this.entityData.set(DATA_BLIJFT, blijft);
        if (blijft) {
            getNavigation().stop();
        }
    }

    public void zetEigenaar(UUID speler, int modus, int generatie) {
        this.eigenaar = speler;
        this.generatie = generatie;
        this.entityData.set(DATA_MODUS, modus);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public int generatie() {
        return generatie;
    }

    public int vissen() {
        return vissen;
    }

    /** (tests, ops) fills his stash. */
    public void zetVissen(int n) {
        this.vissen = Math.max(0, Math.min(VIS_MAX, n));
    }

    @Nullable
    public ServerPlayer baas() {
        return eigenaar != null && level().getPlayerByUUID(eigenaar) instanceof ServerPlayer p && p.isAlive() ? p : null;
    }

    /** The guide scurries ahead along this route and waits for his player; at the end {@code daarna} runs. */
    public void leid(List<Vec3> punten, @Nullable Consumer<ServerPlayer> daarna) {
        route.clear();
        route.addAll(punten);
        routeStap = 0;
        naRoute = daarna;
    }

    public boolean leidt() {
        return !route.isEmpty();
    }

    /** Plays the grab (chapter 6: he snatches the ring). */
    public void grijp() {
        grijpTot = tickCount + 30;
        zetDoet(GRIJPT);
        playSound(ModSounds.MIKA_AMBIENT.get(), 1.2f, 1.8f);
    }

    // --- his day -----------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if ((tickCount + getId()) % 20 == 0) {
            Smikagol.houdBij(this);
            if (isRemoved()) {
                return;
            }
        }
        if (grijpTot > 0) {
            if (tickCount >= grijpTot) {
                grijpTot = 0;
                zetDoet(NIETS);
            }
            return;
        }
        if (!isMaatje()) {
            zetDoet(leidt() && getDeltaMovement().horizontalDistanceSqr() > 1e-4 ? KRUIPT : NIETS);
            return;
        }
        // the buddy: near water, with nothing else to do, he fishes
        if (--waterKijk <= 0) {
            waterKijk = 100;
            bijWater = waterDichtbij(level);
        }
        ServerPlayer p = baas();
        boolean rustig = getNavigation().isDone() && (blijft() || p == null || distanceToSqr(p) < 64);
        if (bijWater && rustig) {
            zetDoet(VIST);
            if (vissen < VIS_MAX && --visTijd <= 0) {
                vissen++;
                visTijd = VIS_TIJD + random.nextInt(VIS_TIJD);
                level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.4, getZ(), 12, 0.4, 0.1, 0.4, 0.1);
                playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.6f, 1.2f);
            }
        } else {
            zetDoet(NIETS);
        }
    }

    private boolean waterDichtbij(ServerLevel level) {
        BlockPos hier = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(hier.offset(-4, -2, -4), hier.offset(4, 1, 4))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide() || !(player instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        playSound(ModSounds.MIKA_AMBIENT.get(), 0.9f, getVoicePitch());
        if (eigenaar == null || !eigenaar.equals(p.getUUID())) {
            GuhQuests.say(p, this, "quest.guhs.ring.smikagol.niet_van_jou");
            return InteractionResult.SUCCESS;
        }
        if (!isMaatje()) {
            for (Smikagol.Klik k : Smikagol.BIJ_KLIK) {
                if (k.klik(this, p)) {
                    return InteractionResult.SUCCESS;
                }
            }
            GuhQuests.say(p, this, "quest.guhs.ring.smikagol.gids." + random.nextInt(4));
            return InteractionResult.SUCCESS;
        }
        if (p.isSecondaryUseActive()) {
            zetBlijft(!blijft());
            GuhQuests.say(p, this, blijft() ? "quest.guhs.ring.smikagol.blijft" : "quest.guhs.ring.smikagol.volgt");
            return InteractionResult.SUCCESS;
        }
        if (vissen > 0) {
            for (int i = 0; i < vissen; i++) {
                Minigames.give(p, new ItemStack(random.nextInt(4) == 0 ? Items.SALMON : Items.COD));
            }
            GuhQuests.say(p, this, "quest.guhs.ring.smikagol.vis", vissen);
            vissen = 0;
            Ring.behaald(p, "ring_smikagol_vis");
            return InteractionResult.SUCCESS;
        }
        GuhQuests.say(p, this, bijWater ? "quest.guhs.ring.smikagol.bijt_nog_niet" : "quest.guhs.ring.smikagol.maatje." + random.nextInt(4));
        return InteractionResult.SUCCESS;
    }

    /** The guide: ahead along the route, never further than {@link #WACHT_AFSTAND} from his player. */
    private final class Leid extends Goal {
        private int wacht, roep;

        Leid() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isMaatje() && leidt() && baas() != null && grijpTot == 0;
        }

        @Override
        public void tick() {
            ServerPlayer p = baas();
            if (p == null || route.isEmpty()) {
                return;
            }
            Vec3 naar = route.get(Math.min(routeStap, route.size() - 1));
            if (distanceToSqr(p) > WACHT_AFSTAND * WACHT_AFSTAND) {
                // too far ahead: wait, look back, call
                getNavigation().stop();
                getLookControl().setLookAt(p, 30f, 30f);
                if (--roep <= 0) {
                    roep = 160;
                    GuhQuests.say(p, SmikagolEntity.this, "quest.guhs.ring.smikagol.kom." + random.nextInt(3));
                }
                return;
            }
            if (naar.distanceToSqr(position()) < 2.5) {
                if (routeStap + 1 >= route.size()) {
                    if (distanceToSqr(p) < 16) {
                        Consumer<ServerPlayer> daarna = naRoute;
                        route.clear();
                        naRoute = null;
                        if (daarna != null) {
                            daarna.accept(p);
                        }
                    }
                    return;
                }
                routeStap++;
                wacht = 0;
                return;
            }
            if (--wacht <= 0) {
                wacht = 10;
                if (!getNavigation().moveTo(naar.x, naar.y, naar.z, 1.05)) {
                    getMoveControl().setWantedPosition(naar.x, naar.y, naar.z, 1.05);
                }
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** Follows his player (the guide without a route, the buddy that isn't told to stay). */
    private final class Volg extends Goal {
        private int wacht;

        Volg() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer p = baas();
            return p != null && !leidt() && !blijft() && grijpTot == 0 && p.level() == level() && distanceToSqr(p) > 25;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer p = baas();
            return p != null && !leidt() && !blijft() && p.level() == level() && distanceToSqr(p) > 9;
        }

        @Override
        public void tick() {
            ServerPlayer p = baas();
            if (p == null) {
                return;
            }
            getLookControl().setLookAt(p, 20f, 20f);
            if (--wacht <= 0) {
                wacht = 10;
                double d = distanceToSqr(p);
                if (!getNavigation().moveTo(p, d > 100 ? 1.3 : 1.05) && d > 100 || d > 30 * 30) {
                    Vec3 naast = Sam.naast(p);
                    teleportTo(naast.x, naast.y, naast.z);
                    getNavigation().stop();
                }
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // --- he can't be hurt and never goes away by himself --------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isMaatje() && blijft();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? ModSounds.MIKA_AMBIENT.get() : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.6f + random.nextFloat() * 0.2f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Modus", modus());
        tag.putInt("Generatie", generatie);
        tag.putInt("Vissen", vissen);
        tag.putBoolean("Blijft", blijft());
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_MODUS, tag.getIntOr("Modus", GIDS));
        generatie = tag.getIntOr("Generatie", 0);
        vissen = tag.getIntOr("Vissen", 0);
        this.entityData.set(DATA_BLIJFT, tag.getBooleanOr("Blijft", false));
        eigenaar = tag.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SmikagolEntity>("main", 4, state -> {
            int doet = doet();
            if (doet == GRIJPT) {
                return state.setAndContinue(GRIJP);
            }
            if (state.isMoving()) {
                return state.setAndContinue(doet == KRUIPT ? KRUIP : LOOP);
            }
            return state.setAndContinue(doet == VIST ? VIS : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
