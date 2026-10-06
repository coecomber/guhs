package nl.juiced.guhs.feature.campingmarkt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * A stall holder of the Nether-Mika-ruilmarkt (bbq2): a Nether-Mika in an apron and a flat cap that stands behind its
 * counter. It never moves, never attacks, can't be hurt and stays on a peaceful server too (it is no monster). Right-click
 * it with a vahoege vads bar: a barter on the spot ({@link Ruilmarkt#kraam}); otherwise it praises its wares.
 * Its {@link #getKraam number} (0..2) says which stall is its own: its name and its lines.
 */
public class KraamMikaEntity extends PathfinderMob implements GeoEntity {
    /** How many stall holders a market has, and how many lines each of them knows. */
    public static final int AANTAL = 3, ZINNEN = 3;

    private static final EntityDataAccessor<Integer> DATA_KRAAM = SynchedEntityData.defineId(KraamMikaEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public KraamMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
        setKraam(0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KRAAM, 0);
    }

    public int getKraam() {
        return entityData.get(DATA_KRAAM);
    }

    public void setKraam(int kraam) {
        int n = Math.floorMod(kraam, AANTAL);
        entityData.set(DATA_KRAAM, n);
        setCustomName(Component.translatable("entity.guhs.campingmarkt_kraam_mika." + n));
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
            Ruilmarkt.kraam(sp, this, hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** A happy little hop (a deal!). */
    public void blij() {
        triggerAnim("actie", "blij");
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kraam", getKraam());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setKraam(tag.getIntOr("Kraam", 0));
    }

    // --- a stall holder: can't be hurt, pushed or led away, never despawns ----------------------------------------------------

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
