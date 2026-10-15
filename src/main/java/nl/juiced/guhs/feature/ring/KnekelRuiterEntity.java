package nl.juiced.guhs.feature.ring;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-kern): one of the Nine, a Knekel-Mika rider: a hooded Mika skeleton on a smoke-black steed (one model, entity
 * {@code guhs:knekel_ruiter}; resources: tools/features/ring_modellen.py). It only ever SHOVES: whoever it catches is put
 * back on their last rest point ({@link Ring#terugNaarRustpunt}), nothing is lost and nobody is hurt.
 * <ul>
 *   <li><b>The hunt</b> ({@link Negen#jaag}): nine of them ride at one player who wears the ring, and only that player's
 *       game knows about them ({@link Zicht#alleenVoor}). Ring off: they lose the scent, sniff around and are gone.</li>
 *   <li><b>A patrol</b> ({@link Negen#patrouille}): rides a fixed round (chapter 5); it sees a player in front of it within
 *       {@link #ZICHT} blocks (not a rock under the Elfenmanteltje) and smells a worn ring from {@link #RUIKT} blocks; then it
 *       gives chase for a while and goes back to its round.</li>
 *   <li>The Lichtflesje blinds it ({@link #verblind}): it stands still with its paws over its eye sockets.</li>
 * </ul>
 */
public class KnekelRuiterEntity extends PathfinderMob implements GeoEntity {
    /** A patrol sees this far in front of it, smells a worn ring this far all around, and chases this long. */
    public static final double ZICHT = 9, RUIKT = 16;
    public static final int ACHTERVOLG_TICKS = 200;
    /** A hunter that lost the scent sniffs around this long and then leaves. */
    public static final int KWIJT_TICKS = 100;
    /** Within this distance a rider has you. */
    public static final double PAK = 1.9;
    /** The speed factor of a chase: a little faster than a walking player, slower than a sprint. */
    public static final double JAAG_SNELHEID = 1.13, RONDE_SNELHEID = 0.7;
    public static final int RUST = 0, JAAGT = 1, BLIND = 2;

    private static final EntityDataAccessor<Integer> DATA_STAAT = SynchedEntityData.defineId(KnekelRuiterEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.knekel_ruiter.idle");
    private static final RawAnimation DRAF = RawAnimation.begin().thenLoop("animation.knekel_ruiter.draf");
    private static final RawAnimation VERBLIND = RawAnimation.begin().thenLoop("animation.knekel_ruiter.verblind");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** Who it is after (a hunt: for good; a patrol: until {@link #achtervolgTot}). */
    @Nullable
    private UUID prooi;
    /** A hunter (made by {@link Negen#jaag}): it exists for its prey only and leaves when the hunt is over. */
    private boolean jager;
    private long achtervolgTot, blindTot, kwijtSinds = -1;
    /** The round of a patrol (world positions). */
    private final List<BlockPos> route = new ArrayList<>();
    private int routeStap;

    public KnekelRuiterEntity(EntityType<? extends KnekelRuiterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAAT, RUST);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Jaag());
        this.goalSelector.addGoal(2, new Ronde());
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // --- what it is doing -------------------------------------------------------------------------------------------------

    /** {@link #RUST}, {@link #JAAGT} or {@link #BLIND} (synced: the client picks the animation). */
    public int staat() {
        return this.entityData.get(DATA_STAAT);
    }

    public boolean isBlind() {
        return blindTot > level().getGameTime();
    }

    /** The Lichtflesje: blind for this many ticks. */
    public void verblind(int ticks) {
        blindTot = Math.max(blindTot, level().getGameTime() + ticks);
        getNavigation().stop();
        this.entityData.set(DATA_STAAT, BLIND);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Makes it a hunter of this player (the Nine of a hunt). */
    public void zetJager(UUID speler) {
        this.prooi = speler;
        this.jager = true;
    }

    public boolean isJager() {
        return jager;
    }

    @Nullable
    public UUID prooi() {
        return prooi;
    }

    /** The round a patrol rides (world positions, at least two), over and over. */
    public void zetRoute(List<BlockPos> ronde) {
        route.clear();
        route.addAll(ronde);
        routeStap = 0;
    }

    public List<BlockPos> route() {
        return List.copyOf(route);
    }

    @Nullable
    private ServerPlayer prooiSpeler() {
        return prooi != null && level().getPlayerByUUID(prooi) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() ? p : null;
    }

    /** Does this rider know where that player is right now? A hunter follows the worn ring; a patrol also what it sees. */
    public boolean ziet(ServerPlayer p) {
        if (isBlind() || !p.isAlive() || p.isSpectator() || p.isCreative()) {
            return false;
        }
        if (Ring.om(p)) {
            return jager || distanceToSqr(p) <= RUIKT * RUIKT;
        }
        if (jager || Gaven.isRots(p) || distanceToSqr(p) > ZICHT * ZICHT || !hasLineOfSight(p)) {
            return false;
        }
        Vec3 naar = p.position().subtract(position()).multiply(1, 0, 1);
        Vec3 kijk = Vec3.directionFromRotation(0, getYHeadRot());
        return naar.lengthSqr() < 4 || naar.normalize().dot(kijk) > 0.35;   // (about 70 degrees to either side)
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            if (staat() == JAAGT && tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + 0.2, getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        long nu = level.getGameTime();
        boolean blind = isBlind();
        ServerPlayer p = prooiSpeler();
        if (jager) {
            // a hunter without prey, or one that lost the scent for too long, is gone in a puff of smoke
            boolean ziet = p != null && p.level() == level && ziet(p);
            if (ziet || blind) {
                kwijtSinds = -1;
            } else if (kwijtSinds < 0) {
                kwijtSinds = nu;
            }
            if (p == null || p.level() != level || kwijtSinds >= 0 && nu - kwijtSinds > KWIJT_TICKS || distanceToSqr(p) > 96 * 96) {
                verdwijn();
                return;
            }
        } else if ((tickCount + getId()) % 10 == 0 && !blind) {
            // a patrol looks around; a chase ends by itself
            if (prooi != null && (nu > achtervolgTot || p == null)) {
                prooi = null;
            }
            if (prooi == null) {
                for (ServerPlayer kandidaat : level.players()) {
                    if (kandidaat.distanceToSqr(this) <= RUIKT * RUIKT && ziet(kandidaat) && Duwtje.mag(kandidaat) && Zicht.magZien(kandidaat, this)) {
                        prooi = kandidaat.getUUID();
                        achtervolgTot = nu + ACHTERVOLG_TICKS;
                        playSound(SoundEvents.SKELETON_HORSE_AMBIENT, 1.2f, 0.6f);
                        kandidaat.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quest.guhs.ring.negen.betrapt")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                        break;
                    }
                }
            }
            p = prooiSpeler();
        }
        if (!blind && p != null && p.level() == level && distanceToSqr(p) <= PAK * PAK && ziet(p) && Duwtje.mag(p)) {
            Vec3 hier = position();
            if (!jager) {
                prooi = null;
            }
            playSound(ModSounds.MIKA_AMBIENT.get(), 1.2f, 0.5f);
            Ring.terugNaarRustpunt(p, Ring.NEGEN, hier);   // (a hunt ends there: its riders vanish)
            return;
        }
        int staat = blind ? BLIND : prooi != null && p != null ? JAAGT : RUST;
        if (staat() != staat) {
            this.entityData.set(DATA_STAAT, staat);
        }
    }

    /** Gone in a puff of smoke (a hunter whose hunt is over). */
    public void verdwijn() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 14, 0.5, 0.8, 0.5, 0.02);
        }
        discard();
    }

    /** Rides at its prey while it knows where that is; a hunter that lost the scent rides on to where it last smelled it. */
    private final class Jaag extends Goal {
        private int wacht;

        Jaag() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer p = prooiSpeler();
            return p != null && !isBlind() && p.level() == level() && ziet(p);
        }

        @Override
        public void start() {
            wacht = 0;
        }

        @Override
        public void tick() {
            ServerPlayer p = prooiSpeler();
            if (p == null) {
                return;
            }
            getLookControl().setLookAt(p, 30f, 30f);
            if (--wacht <= 0) {
                wacht = 8;
                if (!getNavigation().moveTo(p, JAAG_SNELHEID)) {
                    getMoveControl().setWantedPosition(p.getX(), p.getY(), p.getZ(), JAAG_SNELHEID);   // (no path: straight at them)
                }
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** A patrol's round: from point to point, at a walk. */
    private final class Ronde extends Goal {
        private int wacht;

        Ronde() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !jager && prooi == null && route.size() >= 2 && !isBlind();
        }

        @Override
        public void tick() {
            BlockPos naar = route.get(routeStap % route.size());
            if (naar.distToCenterSqr(position()) < 4.0) {
                routeStap = (routeStap + 1) % route.size();
                wacht = 0;
                return;
            }
            if (--wacht <= 0) {
                wacht = 20;
                getNavigation().moveTo(naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5, RONDE_SNELHEID);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // --- it only shoves, and nothing shoves it -----------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
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
        return super.isImmobile() || isBlind();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return staat() == JAAGT ? SoundEvents.SKELETON_HORSE_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f + random.nextFloat() * 0.1f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.HORSE_STEP, 0.35f, 0.6f);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Jager", jager);
        if (prooi != null) {
            tag.store("Prooi", UUIDUtil.CODEC, prooi);
        }
        if (!route.isEmpty()) {
            tag.store("Route", BlockPos.CODEC.listOf(), List.copyOf(route));
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        jager = tag.getBooleanOr("Jager", false);
        prooi = tag.read("Prooi", UUIDUtil.CODEC).orElse(null);
        route.clear();
        route.addAll(tag.read("Route", BlockPos.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KnekelRuiterEntity>("main", 4,
                state -> state.setAndContinue(staat() == BLIND ? VERBLIND : state.isMoving() ? DRAF : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
