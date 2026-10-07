package nl.juiced.guhs.feature.einde2.zwevers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * The Kruimelkwal: a little jellyfish of glowing cheese crumbs with a guh face, drifting in swarms through the
 * Kruimelwolken ({@link Kruimelkwallen} brings them). It weighs nothing and you walk right through it, but:
 * <ul>
 *   <li><b>fall onto one and it bounces you up</b> ({@link #stuiter}; anything alive bounces, sneaking players sink
 *       through). After a bounce the next landing never hurts ({@link #GESTUITERD}, E2ZweversEvents#onFall);</li>
 *   <li><b>a glass bottle catches it</b>: you get a {@code kruimelkwal_pot} (a lamp); breaking the pot lets it go again;</li>
 *   <li>it stays near where it came ({@link #thuis}; a kwal placed by a building keeps to its {@code Honk}).</li>
 * </ul>
 * Lief: it can't be hurt (a tap makes it bob away) and it pushes nobody.
 */
public class KruimelkwalEntity extends PathfinderMob implements GeoEntity {
    /** How fast a bounce sends you up (about five and a half blocks high). */
    public static final double STUITER = 0.95;
    /** Persistent-data key on whoever bounced: the game time of the bounce (the next landing costs nothing). */
    public static final String GESTUITERD = "guhs_einde2_zwevers_gestuiterd";
    /** How long after a bounce a landing is still free (ticks). */
    public static final int ZACHT_LANDEN = 20 * 20;
    /** How far it drifts from where it came, when no building told it otherwise. */
    public static final int ZWERF = 5;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** The middle of its drifting (set on its first tick). */
    @Nullable
    private Vec3 thuis;
    private int straal = ZWERF;
    /** Kwallen of one swarm share this number (the top-up counts swarms). */
    private int zwerm;
    @Nullable
    private Vec3 doel;
    private int doelTijd, stuiterRust;

    public KruimelkwalEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    @Override
    protected void registerGoals() {
    }

    public int zwerm() {
        return zwerm;
    }

    public void setZwerm(int zwerm) {
        this.zwerm = zwerm;
    }

    /** Where it drifts around, and how far. */
    public void setThuis(Vec3 thuis, int straal) {
        this.thuis = thuis;
        this.straal = Math.max(1, straal);
    }

    @Nullable
    public Vec3 thuis() {
        return thuis;
    }

    // --- drifting ------------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (thuis == null) {
            thuis = position();
        }
        if (doel == null || --doelTijd <= 0 || doel.distanceToSqr(position()) < 0.36) {
            doel = nieuwDoel(level);
            doelTijd = 80 + random.nextInt(120);
        }
        // a jellyfish swims in pulses: a push, then it glides
        double puls = 0.55 + 0.45 * Math.sin((tickCount + getId() * 7) * 0.13);
        Vec3 d = doel.subtract(position());
        Vec3 wil = d.lengthSqr() < 1e-4 ? Vec3.ZERO : d.normalize().scale(0.045 * puls);
        setDeltaMovement(getDeltaMovement().lerp(wil, 0.12));
        if (d.horizontalDistanceSqr() > 0.01) {
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 4f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    /** A free spot near home; it likes to hang a block or two above the ground, where you can jump onto it. */
    private Vec3 nieuwDoel(ServerLevel level) {
        Vec3 midden = thuis == null ? position() : thuis;
        for (int i = 0; i < 8; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * straal;
            Vec3 p = midden.add(Math.cos(a) * r, (random.nextDouble() - 0.5) * Math.min(3.0, straal), Math.sin(a) * r);
            BlockPos bp = BlockPos.containing(p);
            if (level.isLoaded(bp) && level.getBlockState(bp).isAir() && level.getBlockState(bp.above()).isAir()
                    && level.getBlockState(bp.below()).isAir()) {
                return p;
            }
        }
        return position();
    }

    // --- bouncing ------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(14) == 0) {
                level().addParticle(ParticleTypes.WAX_ON, getRandomX(0.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.6), 0, 0.01, 0);
            }
            return;
        }
        if (stuiterRust > 0) {
            stuiterRust--;
            return;
        }
        AABB bel = getBoundingBox().inflate(0.12, 0.0, 0.12).expandTowards(0, 0.35, 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, bel, this::stuitertOp)) {
            stuiter(e);
        }
    }

    /** Does this one bounce on it right now? Something alive that comes down onto its bell (not another kwal, not sneaking). */
    public boolean stuitertOp(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof KruimelkwalEntity || e instanceof LeegtewalvisguhEntity || e.isPassenger() || e.isSpectator()
                || e.isShiftKeyDown() || e.isFallFlying() || e instanceof Player p && p.getAbilities().flying) {
            return false;
        }
        if (e.getY() < getY() + getBbHeight() * 0.35) {
            return false;                                         // (it came from the side or from below: it just drifts through)
        }
        double vy = e instanceof Player ? e.getKnownMovement().y : e.getDeltaMovement().y;
        boolean valt = !e.onGround() && (vy < -0.12 || e.fallDistance > 0.4);
        return valt && level().getGameTime() - e.getPersistentData().getLongOr(GESTUITERD, -100L) > 6;
    }

    /** Boing: up it goes (its own speed sideways is kept), the kwal squashes and dips. */
    public void stuiter(LivingEntity e) {
        Vec3 v = e instanceof Player ? e.getKnownMovement() : e.getDeltaMovement();
        e.setDeltaMovement(v.x, STUITER, v.z);
        e.hurtMarked = true;                                       // (a player's own client moves it: this sends the speed)
        e.resetFallDistance();
        e.getPersistentData().putLong(GESTUITERD, level().getGameTime());
        stuiterRust = 4;
        setDeltaMovement(getDeltaMovement().add(0, -0.22, 0));
        triggerAnim("actie", "stuiter");
        playSound(E2ZweversFeature.KWAL_STUITER.get(), 0.8f, 0.9f + random.nextFloat() * 0.4f);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight(), getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        }
        if (e instanceof ServerPlayer p) {
            GuhAdvancements.grant(p, "einde2_zwevers_gestuiterd");
        }
    }

    // --- catching, petting -----------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean fles = stack.is(Items.GLASS_BOTTLE);
        if (!fles && !(stack.isEmpty() && hand == InteractionHand.MAIN_HAND)) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (fles) {
            vang(sp, stack);
        } else {
            triggerAnim("actie", "blij");
            playSound(E2ZweversFeature.KWAL.get(), 0.7f, 1.2f + random.nextFloat() * 0.3f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
        return InteractionResult.SUCCESS;
    }

    /** Scooped up in a glass bottle: the player gets the kruimelkwal_pot, the kwal is in it. */
    public void vang(ServerPlayer player, ItemStack fles) {
        fles.consume(1, player);
        playSound(E2ZweversFeature.KWAL_POT.get(), 0.9f, 1.0f);
        ((ServerLevel) level()).sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.5, getZ(), 10, 0.25, 0.25, 0.25, 0.02);
        Minigames.give(player, new ItemStack(E2ZweversFeature.KRUIMELKWAL_POT_ITEM.get()));
        discard();
    }

    // --- lief ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() != null) {                         // a tap: it bobs away, nothing more
            Vec3 weg = position().subtract(source.getEntity().position()).multiply(1, 0, 1);
            setDeltaMovement(getDeltaMovement().add(weg.lengthSqr() < 1e-4 ? Vec3.ZERO : weg.normalize().scale(0.25)).add(0, 0.12, 0));
            playSound(E2ZweversFeature.KWAL.get(), 0.6f, 1.5f);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;                                             // (a wild one of the top-up comes and goes: WildeDieren)
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return E2ZweversFeature.KWAL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35f;
    }

    // --- save ------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Honk", straal);
        tag.putInt("Zwerm", zwerm);
        if (thuis != null) {
            tag.putDouble("ThuisX", thuis.x);
            tag.putDouble("ThuisY", thuis.y);
            tag.putDouble("ThuisZ", thuis.z);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        straal = Math.max(1, tag.getIntOr("Honk", ZWERF));        // (a building's kwal: Honk, and home is where it stands)
        zwerm = tag.getIntOr("Zwerm", 0);
        double y = tag.getDoubleOr("ThuisY", Double.NaN);
        thuis = Double.isNaN(y) ? null : new Vec3(tag.getDoubleOr("ThuisX", 0), y, tag.getDoubleOr("ThuisZ", 0));
    }

    // --- GeckoLib ----------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KruimelkwalEntity>("beweeg", 5, state -> state.setAndContinue(state.isMoving() ? ZWEM : IDLE)));
        controllers.add(new AnimationController<KruimelkwalEntity>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("stuiter", RawAnimation.begin().thenPlay("stuiter"))
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
