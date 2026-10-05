package nl.juiced.guhs.feature.sausdieren;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Het Sausblubje (the magma cube of the Guhbarbecuether): a wobbly blob of sauce with a guh face that bounces around in
 * three sizes ({@link #KLEIN}, {@link #MIDDEL}, {@link #GROOT}). It is sweet: it never jumps at anyone and nothing hurts it.
 * <ul>
 *   <li>Hug it (right-click with an empty hand) or feed it a kaasknabbel: a big or middle one splits into two of the next
 *       size down with a happy blub and leaves a blob of {@link SausdierenFeature#BLUBROOM blubroom} ({@link #splits}).</li>
 *   <li>A small one grows back to a middle one after {@link #GROEI_VOER} knabbels, so a pen of them keeps giving blubroom as
 *       long as somebody feeds and hugs them (there is nothing to automate: no drops without a player).</li>
 *   <li>A small one fits in a glass bottle: {@link SausblubjePotjeItem het Sausblubje in een potje}.</li>
 *   <li>It bounces over kaasfrituursaus as over ground and never burns. Wild ones despawn like any wild mob; one a player
 *       fed, hugged apart or let out of a jar stays.</li>
 * </ul>
 * Model, animations and texture: tools/features/sausdieren_modellen.py.
 */
public class SausblubjeEntity extends Mob implements GeoEntity {
    public static final int KLEIN = 1, MIDDEL = 2, GROOT = 3;
    /** How many knabbels turn a small one into a middle one. */
    public static final int GROEI_VOER = 3;
    /** Ticks before a hug or a knabbel counts again (so one long click doesn't split a whole family). */
    public static final int RUST = 25;
    private static final float[] SCHAAL = {1.0f, 1.0f, 1.75f, 2.6f};
    private static final EntityDataAccessor<Integer> DATA_GROOTTE = SynchedEntityData.defineId(SausblubjeEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation LUCHT = RawAnimation.begin().thenLoop("lucht");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int gevoerd;
    private long rustTot;
    private boolean wasOpGrond = true;
    /** Sitting in front of somebody with a knabbel: it hops on the spot. */
    private boolean wachtOpKnabbel;

    public SausblubjeEntity(EntityType<? extends SausblubjeEntity> type, Level level) {
        super(type, level);
        this.moveControl = new HopControl(this);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 12.0);
    }

    /** Wild ones: on the floor of the Barbecuether, never many together (spawn eggs and commands always work). */
    public static boolean magSpawnen(EntityType<SausblubjeEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        return random.nextInt(3) == 0 && level.getBlockState(pos).isAir()
                && level.getEntitiesOfClass(SausblubjeEntity.class, new AABB(pos).inflate(32, 12, 32)).size() < 4;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.SPAWN_ITEM_USE
                || EntitySpawnReason.isSpawner(reason)) {
            int r = random.nextInt(20);
            setGrootte(r < 9 ? KLEIN : r < 16 ? MIDDEL : GROOT);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GROOTTE, KLEIN);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DrijfGoal());
        goalSelector.addGoal(2, new NaarKnabbelGoal());
        goalSelector.addGoal(3, new RichtingGoal());
        goalSelector.addGoal(5, new HopGoal());
    }

    // --- its size ------------------------------------------------------------------------------------------------------------------

    public int grootte() {
        return Mth.clamp(entityData.get(DATA_GROOTTE), KLEIN, GROOT);
    }

    public void setGrootte(int grootte) {
        entityData.set(DATA_GROOTTE, Mth.clamp(grootte, KLEIN, GROOT));
        reapplyPosition();
        refreshDimensions();
    }

    /** How much bigger than a small one it is drawn and collides. */
    public float schaal() {
        return SCHAAL[grootte()];
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_GROOTTE.equals(accessor)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(schaal());
    }

    // --- hugging, feeding, the jar ------------------------------------------------------------------------------------------------

    public static boolean isVoer(ItemStack stack) {
        return stack.is(SausdierenFeature.BLUBJE_VOER);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean potje = stack.is(Items.GLASS_BOTTLE), voer = isVoer(stack);
        if (!potje && !voer && !(hand == InteractionHand.MAIN_HAND && stack.isEmpty())) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (level().getGameTime() < rustTot) {
            return InteractionResult.CONSUME;
        }
        rustTot = level().getGameTime() + RUST;
        if (potje) {
            if (grootte() != KLEIN) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.te_groot_voor_potje", getDisplayName()).withStyle(ChatFormatting.GOLD));
                return InteractionResult.CONSUME;
            }
            inPotje(sp, hand, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (voer) {
            stack.consume(1, player);
            triggerAnim("actie", "eet");
        }
        setPersistenceRequired();   // (somebody cares about this one: it stays)
        if (grootte() > KLEIN) {
            splits(sp);
        } else if (voer) {
            gevoerd++;
            if (gevoerd >= GROEI_VOER) {
                gevoerd = 0;
                setGrootte(MIDDEL);
                blij(5);
                sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.gegroeid", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                blij(2);
                sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.nog_voer", GROEI_VOER - gevoerd).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            blij(3);
            sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.knuffel_klein", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            GuhAdvancements.grant(sp, "sausdieren_geknuffeld");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private void blij(int hartjes) {
        triggerAnim("actie", "blij");
        playSound(SausdierenFeature.BLUBJE_BLUB.get(), 0.8f, 1.5f - 0.2f * grootte());
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.15, getZ(), hartjes, 0.25 * schaal(), 0.15, 0.25 * schaal(), 0.02);
        }
    }

    /**
     * A happy blub: this one becomes two of the next size down, a little apart, and leaves one blubroom. The little ones stay
     * when this one would have. Returns them (empty: it is a small one, nothing happens).
     */
    public List<SausblubjeEntity> splits(@Nullable ServerPlayer door) {
        List<SausblubjeEntity> kleintjes = new ArrayList<>();
        if (grootte() <= KLEIN || !(level() instanceof ServerLevel level)) {
            return kleintjes;
        }
        float hoek = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < 2; i++) {
            SausblubjeEntity kind = SausdierenFeature.SAUSBLUBJE.get().create(level, EntitySpawnReason.TRIGGERED);
            if (kind == null) {
                continue;
            }
            double dx = Mth.cos(hoek + i * Mth.PI), dz = Mth.sin(hoek + i * Mth.PI);
            kind.setGrootte(grootte() - 1);
            kind.snapTo(getX() + dx * 0.3 * schaal(), getY() + 0.1, getZ() + dz * 0.3 * schaal(), random.nextFloat() * 360f, 0);
            kind.setDeltaMovement(dx * 0.22, 0.32, dz * 0.22);
            kind.rustTot = level.getGameTime() + RUST;
            if (isPersistenceRequired()) {
                kind.setPersistenceRequired();
            }
            if (hasCustomName()) {
                kind.setCustomName(getCustomName());
            }
            level.addFreshEntity(kind);
            kleintjes.add(kind);
        }
        spawnAtLocation(level, new ItemStack(SausdierenFeature.BLUBROOM.get()), getBbHeight() * 0.5f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, SausdierenFeature.BLUBROOM.get()), getX(), getY() + getBbHeight() * 0.5,
                getZ(), 14, 0.3 * schaal(), 0.25 * schaal(), 0.3 * schaal(), 0.06);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 4, 0.4, 0.2, 0.4, 0.02);
        playSound(SausdierenFeature.BLUBJE_SPLITS.get(), 1.0f, 1.3f - 0.15f * grootte());
        if (door != null) {
            door.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.gesplitst", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            GuhAdvancements.grant(door, "sausdieren_gesplitst");
            GidsFeature.grant(door, "barbecuether/sausdieren_blubroom");
        }
        discard();
        return kleintjes;
    }

    /** A small one hops into the bottle: the player holds a Sausblubje in een potje. */
    private void inPotje(ServerPlayer player, InteractionHand hand, ItemStack fles) {
        playSound(SausdierenFeature.POTJE.get(), 1.0f, 1.2f);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        }
        ItemStack potje = new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get());
        if (hasCustomName()) {
            potje.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(fles, player, potje));
        GuhAdvancements.grant(player, "sausdieren_potje");
        discard();
    }

    // --- sweet: never hurt, never hurts -----------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.niet_slaan_blubje").withStyle(ChatFormatting.GRAY));
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    // --- the sauce: it bounces over it ------------------------------------------------------------------------------------------------

    public boolean inSaus() {
        return getFluidTypeHeight(BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get()) > 0 || isInLava();
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return SausdierenFeature.isSaus(fluid);
    }

    @Override
    public VoxelShape getLiquidCollisionShape() {
        return Block.column(16.0, 0.0, 8.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (inSaus()) {
            CollisionContext context = CollisionContext.of(this);
            if (context.isAbove(getLiquidCollisionShape(), blockPosition(), true) && !SausdierenFeature.isSaus(level().getFluidState(blockPosition().above()))) {
                setOnGround(true);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.06, 0.0));
            }
        }
        if (onGround() && !wasOpGrond) {
            if (level().isClientSide()) {
                ItemParticleOption spetter = new ItemParticleOption(ParticleTypes.ITEM, SausdierenFeature.BLUBROOM.get());
                for (int i = 0; i < 4 * grootte(); i++) {
                    float a = random.nextFloat() * Mth.TWO_PI, r = getBbWidth() * (0.3f + random.nextFloat() * 0.3f);
                    level().addParticle(spetter, getX() + Mth.sin(a) * r, getY() + 0.05, getZ() + Mth.cos(a) * r, 0, 0.05, 0);
                }
            } else {
                triggerAnim("actie", "plof");
                playSound(SausdierenFeature.BLUBJE_PLOF.get(), 0.25f + 0.15f * grootte(), 1.6f - 0.25f * grootte());
            }
        }
        wasOpGrond = onGround();
    }

    @Override
    public void jumpFromGround() {
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x, 0.36f + 0.06f * grootte(), v.z);
        this.needsSync = true;
    }

    int hopWacht() {
        return 14 + random.nextInt(26);
    }

    // --- sounds ----------------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SausdierenFeature.BLUBJE_BLUB.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.5f - 0.2f * grootte() + (random.nextFloat() - random.nextFloat()) * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3f + 0.15f * grootte();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // --- save ------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Grootte", grootte());
        tag.putInt("Gevoerd", gevoerd);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setGrootte(tag.getIntOr("Grootte", KLEIN));
        gevoerd = Mth.clamp(tag.getIntOr("Gevoerd", 0), 0, GROEI_VOER - 1);
    }

    /** (tests) how many knabbels this small one has had. */
    public int gevoerd() {
        return gevoerd;
    }

    /** (tests) it may be hugged or fed again right now. */
    public void uitgerust() {
        rustTot = 0;
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SausblubjeEntity>("beweeg", 2, state -> state.setAndContinue(onGround() ? IDLE : LUCHT)));
        controllers.add(new AnimationController<SausblubjeEntity>("actie", 0, state -> PlayState.STOP)
                .triggerableAnim("plof", RawAnimation.begin().thenPlay("plof"))
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- AI: it hops (what the slime does, without ever going after anyone) -----------------------------------------------------------

    static class HopControl extends MoveControl {
        private final SausblubjeEntity blubje;
        private float yRot;
        private int wacht;
        private boolean haast;

        HopControl(SausblubjeEntity blubje) {
            super(blubje);
            this.blubje = blubje;
            this.yRot = blubje.getYRot();
        }

        void richting(float yRot, boolean haast) {
            this.yRot = yRot;
            this.haast = haast;
        }

        void wil(double snelheid) {
            this.speedModifier = snelheid;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            mob.setYRot(rotlerp(mob.getYRot(), yRot, 90.0f));
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            if (operation != MoveControl.Operation.MOVE_TO) {
                mob.setZza(0.0f);
                return;
            }
            operation = MoveControl.Operation.WAIT;
            float snelheid = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (!mob.onGround()) {
                mob.setSpeed(snelheid);
                return;
            }
            if (wacht-- <= 0) {
                wacht = haast ? blubje.hopWacht() / 3 : blubje.hopWacht();
                mob.setSpeed(snelheid);
                blubje.getJumpControl().jump();
            } else {
                blubje.xxa = 0.0f;
                blubje.zza = 0.0f;
                mob.setSpeed(0.0f);
            }
        }
    }

    private HopControl hop() {
        return (HopControl) moveControl;
    }

    /** In water (it doesn't like water): keep hopping up. */
    class DrijfGoal extends Goal {
        DrijfGoal() {
            setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
            getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return isInWater();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8f) {
                getJumpControl().jump();
            }
            hop().wil(1.2);
        }
    }

    /** Somebody nearby holds a knabbel: it turns to them and hops closer, quickly. */
    class NaarKnabbelGoal extends Goal {
        @Nullable
        private Player doel;

        NaarKnabbelGoal() {
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            doel = level().getNearestPlayer(SausblubjeEntity.this, 8.0);
            return doel != null && !doel.isSpectator() && (isVoer(doel.getMainHandItem()) || isVoer(doel.getOffhandItem()));
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && doel.isAlive() && distanceToSqr(doel) < 100 && (isVoer(doel.getMainHandItem()) || isVoer(doel.getOffhandItem()));
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
            lookAt(doel, 10.0f, 10.0f);
            // (close enough: it sits and waits for its knabbel)
            double ver = 1.6 + 0.5 * getBbWidth() + 0.5;
            hop().richting(getYRot(), distanceToSqr(doel) > (ver + 1.5) * (ver + 1.5));
            wachtOpKnabbel = distanceToSqr(doel) < ver * ver;
        }

        @Override
        public void stop() {
            doel = null;
            wachtOpKnabbel = false;
        }
    }

    /** A new direction now and then. */
    class RichtingGoal extends Goal {
        private float graden;
        private int volgende;

        RichtingGoal() {
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return onGround() || isInWater() || inSaus();
        }

        @Override
        public void tick() {
            if (--volgende <= 0) {
                volgende = adjustedTickDelay(40 + random.nextInt(60));
                graden = random.nextInt(360);
            }
            hop().richting(graden, false);
        }
    }

    /** It keeps hopping. */
    class HopGoal extends Goal {
        HopGoal() {
            setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !isPassenger();
        }

        @Override
        public void tick() {
            hop().wil(wachtOpKnabbel ? 0.0 : 1.0);
        }
    }
}
