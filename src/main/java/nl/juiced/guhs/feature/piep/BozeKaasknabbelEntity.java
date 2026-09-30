package nl.juiced.guhs.feature.piep;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * A boze kaasknabbel: a cheese-puff plush (a flattened crescent standing on one end) with cross little brows. It is NOT a
 * guh (it is a snack), so it may be cross: it hops at players and bumps them (weak). They come out of the kaasknabbel-nest
 * ({@link KaasknabbelNest}). Beaten, its brows go up, it flops over and it says "zieli..." (it was just zieli all along);
 * it drops normal kaasknabbels (loot table guhs:entities/boze_kaasknabbel). Breaks no blocks, and never despawns in peaceful
 * (the nest must stay doable).
 */
public class BozeKaasknabbelEntity extends PathfinderMob implements Enemy, GeoEntity {
    /** Persistent data: the nest (its key) this knabbel belongs to. */
    public static final String NEST = "guhs_piep_nest";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ZIELI = RawAnimation.begin().thenPlayAndHold("zieli");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BozeKaasknabbelEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            protected void checkAndPerformAttack(net.minecraft.world.entity.LivingEntity target) {
                if (canPerformAttack(target) && isTimeToAttack()) {
                    triggerAnim("actie", "aanval");
                }
                super.checkAndPerformAttack(target);
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The nest this knabbel came from ("" for one from an egg). */
    public String nest() {
        return getPersistentData().getStringOr(NEST, "");
    }

    public void setNest(String key) {
        getPersistentData().putString(NEST, key);
        setPersistenceRequired();
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return nest().isEmpty() && super.removeWhenFarAway(distance);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            zieli(level, this, Component.translatable("gui.guhs.piep.zieli"), 1.0);
            if (source.getEntity() instanceof ServerPlayer player) {
                PiepVoortgang.tel(player, PiepVoortgang.KNABBELS, 1);
                PiepVoortgang.pagina(player, this instanceof BozeOppernabbelEntity ? "boze_oppernabbel" : "boze_kaasknabbel");
            }
        }
    }

    /** A little floating "zieli..." above a beaten knabbel (a text display that goes away by itself). */
    static void zieli(ServerLevel level, Entity at, Component text, double scale) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", Component.Serializer.toJson(text.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD), level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putInt("background", 0);
        tag.putByte("shadow", (byte) 1);
        CompoundTag transformation = new CompoundTag();
        net.minecraft.nbt.ListTag schaal = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 3; i++) {
            schaal.add(net.minecraft.nbt.FloatTag.valueOf((float) scale));
        }
        transformation.put("scale", schaal);
        transformation.put("translation", floats(0, 0, 0));
        transformation.put("left_rotation", floats(0, 0, 0, 1));
        transformation.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", transformation);
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf(PiepEvents.ZIELI_TAG));
        tag.put("Tags", tags);
        Vec3 pos = at.position().add(0, at.getBbHeight() + 0.35, 0);
        Entity display = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, level, e -> {
            e.moveTo(pos.x, pos.y, pos.z, 0, 0);
            return e;
        });
        if (display != null) {
            level.addFreshEntity(display);
            PiepEvents.weg(display, 50);
        }
        level.playSound(null, at.blockPosition(), PiepFeature.KNABBEL_ZIELI.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1f,
                at instanceof BozeOppernabbelEntity ? 0.7f : 1.2f);
    }

    private static net.minecraft.nbt.ListTag floats(float... v) {
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (float f : v) {
            l.add(net.minecraft.nbt.FloatTag.valueOf(f));
        }
        return l;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return PiepFeature.KNABBEL_BOOS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "beweeg", 3, state -> state.setAndContinue(
                isDeadOrDying() ? ZIELI : state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "actie", 1, state -> PlayState.STOP)
                .triggerableAnim("aanval", RawAnimation.begin().thenPlay("aanval"))
                .triggerableAnim("stamp", RawAnimation.begin().thenPlay("stamp")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
