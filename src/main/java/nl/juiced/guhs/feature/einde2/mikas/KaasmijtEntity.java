package nl.juiced.guhs.feature.einde2.mikas;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * De Kaasmijt (the endermite of the Guheinde): a tiny mite of cheese crumbs. It trips up to whoever is near and tickles
 * their toes, which gives them the hik ({@link HikEffect}); then it scurries off and comes back a little later. Like an
 * endermite it is gone after two minutes ({@link #LEVEN}; one with a name tag or from a building stays).
 * It never hurts and drops nothing. Ender-Mika's adore it and trail after it ({@link EnderMikaEntity}).
 */
public class KaasmijtEntity extends PathfinderMob implements GeoEntity {
    /** How long it lives (ticks): two minutes, like an endermite. */
    public static final int LEVEN = 2400;
    /** From how far it notices toes to tickle (blocks). */
    public static final double ZIET = 8.0;
    /** How near it must be to tickle (blocks, between the two middles). */
    public static final double KIETEL_AFSTAND = 1.3;
    /** How long the hik lasts after one tickle (ticks). */
    public static final int HIK_TIJD = 160;
    /** Ticks before it tickles again. */
    public static final int KIETEL_PAUZE = 200;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation KIETEL = RawAnimation.begin().thenPlay("kietel");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Honk honk = new Honk();
    private int leeftijd;
    private int pauze;

    public KaasmijtEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KietelGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public int leeftijd() {
        return leeftijd;
    }

    /** (for the tests: as old as this) */
    public void zetLeeftijd(int ticks) {
        this.leeftijd = ticks;
    }

    /** Ticks until it tickles again (0: it is looking for toes). */
    public int pauze() {
        return pauze;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            honk.tick(this);
            if (pauze > 0) {
                pauze--;
            }
            if (!isPersistenceRequired() && ++leeftijd >= LEVEN) {
                poef(server);
            }
        }
    }

    /** Its two minutes are over: a puff of crumbs and it is gone. */
    public void poef(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.15, getZ(), 6, 0.1, 0.05, 0.1, 0.01);
        level.sendParticles(E2MikasFeature.FLITS.get(), getX(), getY() + 0.15, getZ(), 4, 0.1, 0.05, 0.1, 0.01);
        discard();
    }

    /**
     * Tickles this player's toes: the hik for {@link #HIK_TIJD} ticks (a first hiccup at once), and a pause for the mite.
     * False while it is still pausing.
     */
    public boolean kietel(Player player) {
        if (pauze > 0 || !(level() instanceof ServerLevel server) || player.isSpectator()) {
            return false;
        }
        pauze = KIETEL_PAUZE;
        triggerAnim("action", "kietel");
        playSound(E2MikasFeature.MIJT_KIETEL.get(), 0.8f, 1.3f + random.nextFloat() * 0.3f);
        player.addEffect(new MobEffectInstance(E2MikasFeature.HIK, HIK_TIJD, 0, false, true, true));
        HikEffect.hik(server, player);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.gekieteld").withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    /** Trips up to the nearest player and tickles; pauses in between (then it just wanders). */
    private class KietelGoal extends Goal {
        @Nullable
        private Player doel;
        private int herpad;

        KietelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pauze > 0) {
                return false;
            }
            Player p = level().getNearestPlayer(KaasmijtEntity.this, ZIET);
            if (p == null || p.isSpectator() || !isWithinHome(p.blockPosition())) {
                return false;
            }
            doel = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return pauze <= 0 && doel != null && doel.isAlive() && !doel.isSpectator() && distanceToSqr(doel) < (ZIET + 4) * (ZIET + 4)
                    && isWithinHome(doel.blockPosition());
        }

        @Override
        public void start() {
            herpad = 0;
        }

        @Override
        public void stop() {
            doel = null;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (doel == null) {
                return;
            }
            getLookControl().setLookAt(doel, 30f, 30f);
            if (--herpad <= 0) {
                herpad = 8;
                getNavigation().moveTo(doel, 1.25);
            }
            if (distanceToSqr(doel) <= KIETEL_AFSTAND * KIETEL_AFSTAND) {
                kietel(doel);
            }
        }
    }

    // --- tiny and harmless ---------------------------------------------------------------------------------------------------

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return E2MikasFeature.MIJT_PIEP.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return E2MikasFeature.MIJT_PIEP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return E2MikasFeature.MIJT_PLOP.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.ENDERMITE_STEP, 0.1f, 1.6f);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Leeftijd", leeftijd);
        honk.bewaar(output);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        leeftijd = input.getIntOr("Leeftijd", 0);
        honk.lees(input);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 2, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP).triggerableAnim("kietel", KIETEL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
