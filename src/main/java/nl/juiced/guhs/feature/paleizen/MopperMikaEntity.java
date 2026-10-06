package nl.juiced.guhs.feature.paleizen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * A neighbour of the Mika-woonblokken (bbq2): a Mika that just lives there. It stands in its flat (or on the gallery, at
 * the barbecue, on the roof), looks at whoever comes by and grumbles when you right-click it. It never moves, never attacks
 * and can't be hurt.
 * <p>
 * Its {@link #getNr number} says who it is: 0 Brom-Mika, 1 Zeur-Mika and 2 Snurk-Mika are the grumpy three that get a bowl
 * of Mika-oma's worstsoep in her questline ({@link OmaQuest#klik}); 3, 4 and 5 only have a line or two to say. The number
 * also picks its things (a flat cap, curlers, a nightcap...: bones mopper_&lt;nr&gt; of its model, see
 * tools/features/paleizen_modellen.py and client/PaleizenClient).
 */
public class MopperMikaEntity extends PathfinderMob implements GeoEntity {
    /** How many different neighbours there are. */
    public static final int AANTAL = 6;
    /** The first numbers are the grumpy ones of the questline. */
    public static final int MOPPERAARS = 3;

    private static final EntityDataAccessor<Integer> DATA_NR = SynchedEntityData.defineId(MopperMikaEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MopperMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
        setNr(0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NR, 0);
    }

    public int getNr() {
        return entityData.get(DATA_NR);
    }

    public void setNr(int nr) {
        int n = Math.floorMod(nr, AANTAL);
        entityData.set(DATA_NR, n);
        setCustomName(Component.translatable("entity.guhs.paleizen_mopper_mika." + n));
    }

    /** One of the three that mopper until they get soup. */
    public boolean isMopperaar() {
        return getNr() < MOPPERAARS;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            OmaQuest.klik(sp, this, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        // a little thundercloud over a mopperaar, only for the players it still moppers at (their own questline)
        if (level() instanceof ServerLevel level && isMopperaar() && (tickCount + getId()) % 40 == 0) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(10))) {
                if (OmaQuest.moppertTegen(p, getNr())) {
                    level.sendParticles(p, ParticleTypes.ANGRY_VILLAGER, false, false, getX(), getY() + getBbHeight() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }

    /** A happy little hop (it got its soup). */
    public void blij() {
        triggerAnim("actie", "blij");
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Nr", getNr());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setNr(tag.getIntOr("Nr", 0));
    }

    // --- a neighbour: can't be hurt, pushed or led away, never despawns ------------------------------------------------------

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

    @Override
    protected SoundEvent getAmbientSound() {
        return isMopperaar() ? PaleizenFeature.MOPPER.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>("actie", 0, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("animation.guh.happy")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
