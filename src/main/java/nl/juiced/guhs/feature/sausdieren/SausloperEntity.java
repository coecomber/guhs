package nl.juiced.guhs.feature.sausdieren;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * De Sausloper (the strider of the Guhbarbecuether): a round guh on two very long legs, with a tuft of fries on its head.
 * <ul>
 *   <li>It walks over kaasfrituursaus (and lava) as if it were ground, half sunk in it, and never burns. On anything cold it
 *       shivers and is slow ({@link #isKoud}); the sauce, glowing coal and the heated floor of its stall keep it warm
 *       ({@link SausdierenFeature#WARM}).</li>
 *   <li>It follows pindasaus aan een stok and anything of the tag {@code guhs:sausdieren/sausloper_voer} ({@link LokGoal}).</li>
 *   <li>Wild ones can be tamed with that food (1 in {@link #TEM_KANS}) by a player who finished the stable's questline
 *       ({@link SausdierenFeature#magTemmen}); a tame one takes a saddle, is ridden by right-click and goes where you look
 *       while you hold the stick (use the stick for a sprint). Its owner sneaks and right-clicks to make it stay.</li>
 *   <li>The stable's own Sauslopers ({@link #isBewoner}, a {@link Bezetting} resident) are nobody's: every player feeds them
 *       for the questline, they stay around their home. A {@link #leen loaner} carries one player on the test lap and is gone
 *       again afterwards ({@link Proefrit}).</li>
 *   <li>Sweet: it never hurts anyone and can't be hurt (you pet a Sausloper), and whoever rides one is kept from burning.</li>
 * </ul>
 * Model, animations and textures: tools/features/sausdieren_modellen.py.
 */
public class SausloperEntity extends TamableAnimal implements GeoEntity, ItemSteerable {
    public static final int TEM_KANS = 3;
    /** A resident further than this from its home is put back. */
    public static final int THUIS_TE_VER = 26;
    private static final Identifier KOUD_ID = Guhs.id("sausdieren_koud");
    private static final AttributeModifier KOUD_MODIFIER = new AttributeModifier(KOUD_ID, -0.34, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(SausloperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_KOUD = SynchedEntityData.defineId(SausloperEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation BIBBER = RawAnimation.begin().thenLoop("bibber");
    private static final RawAnimation WALK_KOUD = RawAnimation.begin().thenLoop("walk_koud");
    private static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
    private LokGoal lokGoal;
    /** A loaner: the player it carries on the test lap (null: not a loaner). Loaners are never saved. */
    @Nullable
    private UUID leen;
    private int leenAlleen;
    private long aaiRust;

    public SausloperEntity(EntityType<? extends SausloperEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.xpReward = 0;
        this.setPathfindingMalus(PathType.WATER, -1.0f);
        this.setPathfindingMalus(PathType.LAVA, 0.0f);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0f);
        this.setPathfindingMalus(PathType.FIRE, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** Wild ones come up out of the sauce sea (the spot is in the sauce, see {@link SausdierenFeature#IN_SAUS}): open air above it, not too many together. */
    public static boolean magSpawnen(EntityType<SausloperEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        BlockPos.MutableBlockPos p = pos.mutable();
        int n = 0;
        do {
            p.move(Direction.UP);
        } while (SausdierenFeature.isSaus(level.getFluidState(p)) && ++n < 8);
        if (!level.getBlockState(p).isAir()) {
            return false;
        }
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        return random.nextInt(3) == 0 && level.getEntitiesOfClass(SausloperEntity.class, new AABB(pos).inflate(48, 16, 48)).size() < 3;
    }

    /** A Sausloper of the stable (made by {@link Bezetting} at a copy that misses one). */
    public static SausloperEntity bewoner(ServerLevel level, Vec3 plek) {
        SausloperEntity loper = SausdierenFeature.SAUSLOPER.get().create(level, EntitySpawnReason.STRUCTURE);
        if (loper != null) {
            loper.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0);
            loper.setPersistenceRequired();
        }
        return loper;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOOST_TIME, 0);
        builder.define(DATA_KOUD, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BOOST_TIME.equals(accessor) && level().isClientSide()) {
            steering.onSynced();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        lokGoal = new LokGoal();
        goalSelector.addGoal(2, lokGoal);
        goalSelector.addGoal(4, new NaarSausGoal(this, 1.0));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 60));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // --- who it is ---------------------------------------------------------------------------------------------------------------

    /** One of the stable's own Sauslopers: nobody's, fed by everybody. */
    public boolean isBewoner() {
        return Bezetting.isBezetting(this);
    }

    /** The player this loaner carries on the test lap (null: not a loaner). */
    @Nullable
    public UUID leen() {
        return leen;
    }

    public void zetLeen(@Nullable UUID speler) {
        this.leen = speler;
        this.leenAlleen = 0;
    }

    /** The player it is following right now (the stick or food in hand), or null. */
    @Nullable
    public Player lokker() {
        return lokGoal == null ? null : lokGoal.doel;
    }

    /** (tests) It stays where it is put: no goals. */
    public void stilVoorTest() {
        goalSelector.removeAllGoals(g -> true);
        lokGoal = null;
    }

    public boolean isKoud() {
        return entityData.get(DATA_KOUD);
    }

    public void zetKoud(boolean koud) {
        if (koud == isKoud()) {
            return;
        }
        entityData.set(DATA_KOUD, koud);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (koud) {
                speed.addOrUpdateTransientModifier(KOUD_MODIFIER);
            } else {
                speed.removeModifier(KOUD_ID);
            }
        }
    }

    public static boolean isVoer(ItemStack stack) {
        return stack.is(SausdierenFeature.SAUSLOPER_VOER);
    }

    public static boolean isLokker(ItemStack stack) {
        return isVoer(stack) || stack.is(SausdierenFeature.PINDASAUS_STOK.get());
    }

    // --- the sauce -----------------------------------------------------------------------------------------------------------------

    /** Standing in (on) kaasfrituursaus or lava. */
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

    /** Like the strider: on the sauce it stands, under it it comes up. */
    private void drijf() {
        if (!inSaus()) {
            return;
        }
        CollisionContext context = CollisionContext.of(this);
        if (context.isAbove(getLiquidCollisionShape(), blockPosition(), true) && !SausdierenFeature.isSaus(level().getFluidState(blockPosition().above()))) {
            setOnGround(true);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (inSaus()) {
            resetFallDistance();
        } else {
            super.checkFallDamage(y, onGround, state, pos);
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SausNavigatie(this, level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (SausdierenFeature.isSaus(level.getFluidState(pos))) {
            return 10.0f;
        }
        return inSaus() ? Float.NEGATIVE_INFINITY : 0.0f;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !isNoAi()) {
            BlockState in = level().getBlockState(blockPosition());
            BlockState op = getBlockStateOn();
            zetKoud(!(in.is(SausdierenFeature.WARM) || op.is(SausdierenFeature.WARM) || inSaus()));
        }
        super.tick();
        drijf();
        if (level() instanceof ServerLevel level) {
            serverTick(level);
        } else if (isKoud() && random.nextInt(24) == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.4), getY() + 1.2 + random.nextDouble() * 0.5, getRandomZ(0.4), 0, 0.01, 0);
        }
    }

    private void serverTick(ServerLevel level) {
        // whoever rides a Sausloper is kept from burning (also just after getting off: a slip into the sauce costs nothing)
        if (tickCount % 20 == 0) {
            for (Entity e : getPassengers()) {
                if (e instanceof LivingEntity rider) {
                    rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, true, false, true));
                }
            }
            if (isKoud() && !isSilent() && random.nextInt(8) == 0) {
                playSound(SausdierenFeature.SAUSLOPER_BIBBER.get(), 0.6f, 1.0f + random.nextFloat() * 0.2f);
            }
        }
        if (leen != null) {
            leenTick(level);
        } else if (isBewoner() && tickCount % 40 == 7) {
            thuisTick(level);
        }
    }

    /** A resident keeps a home (where it was first seen) and is put back when it strayed far (lured away, pushed). */
    private void thuisTick(ServerLevel level) {
        if (!hasHome()) {
            setHomeTo(blockPosition(), inSaus() ? 9 : 3);
            return;
        }
        BlockPos thuis = getHomePosition();
        if (!isVehicle() && lokker() == null && blockPosition().distManhattan(thuis) > THUIS_TE_VER) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            snapTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, getYRot(), 0);
            getNavigation().stop();
        }
    }

    /** A loaner waits for its player and is gone when that player is: never while somebody sits on it. */
    private void leenTick(ServerLevel level) {
        if (isVehicle()) {
            leenAlleen = 0;
            return;
        }
        Player speler = level.getPlayerByUUID(leen);
        boolean weg = speler == null || !speler.isAlive() || speler.distanceToSqr(this) > 48 * 48;
        if (weg || ++leenAlleen > (Proefrit.bezig(leen) ? 20 * 90 : 20 * 12)) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 14, 0.3, 0.5, 0.3, 0.02);
            Proefrit.loperWeg(leen);
            discard();
        }
    }

    // --- petting, feeding, taming, the saddle, getting on ---------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            boolean iets = isVoer(stack) || isSaddled() || isTame() && isEquippableInSlot(stack, EquipmentSlot.SADDLE) || stack.isEmpty();
            return iets ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (isVoer(stack)) {
            voer(sp, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (isTame() && !isSaddled() && isEquippableInSlot(stack, EquipmentSlot.SADDLE)) {
            return stack.interactLivingEntity(player, this, hand);
        }
        if (isTame() && isOwnedBy(player) && player.isSecondaryUseActive() && leen == null) {
            setOrderedToSit(!isOrderedToSit());
            getNavigation().stop();
            sp.sendOverlayMessage(Component.translatable(isOrderedToSit() ? "gui.guhs.sausdieren.blijft" : "gui.guhs.sausdieren.loopt", getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (isSaddled() && (isTame() || leen != null) && !isVehicle() && !player.isSecondaryUseActive() && !stack.is(Items.LEAD)) {
            if (leen != null && !leen.equals(player.getUUID())) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.leen_ander").withStyle(ChatFormatting.GRAY));
                return InteractionResult.SUCCESS_SERVER;
            }
            setOrderedToSit(false);
            player.startRiding(this);
            GuhAdvancements.grant(sp, "sausdieren_gereden");
            if (!player.isHolding(SausdierenFeature.PINDASAUS_STOK.get())) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.stok_nodig").withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty()) {
            aai(sp);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    /** A pet on its head: hearts, a happy little hop, and what to do with it now. */
    private void aai(ServerPlayer player) {
        if (level().getGameTime() < aaiRust) {
            return;
        }
        aaiRust = level().getGameTime() + 30;
        blij(3);
        String hint = isBewoner() ? "gui.guhs.sausdieren.aai_bewoner" : leen != null ? "gui.guhs.sausdieren.aai_leen"
                : isTame() ? (isSaddled() ? "gui.guhs.sausdieren.aai_tam" : "gui.guhs.sausdieren.aai_zadel")
                : SausdierenFeature.magTemmen(player) ? "gui.guhs.sausdieren.aai_wild" : "gui.guhs.sausdieren.aai_onbekend";
        player.sendOverlayMessage(Component.translatable(hint, getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void blij(int hartjes) {
        triggerAnim("actie", "blij");
        playSound(SausdierenFeature.SAUSLOPER_BLIJ.get(), 0.9f, 1.0f + random.nextFloat() * 0.2f);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), hartjes, 0.35, 0.2, 0.35, 0.02);
        }
    }

    /** Fed by this player: a resident counts it for the questline, a wild one may become yours, a tame one is just happy. */
    private void voer(ServerPlayer player, ItemStack stack) {
        triggerAnim("actie", "eet");
        playSound(SausdierenFeature.SAUSLOPER_SMAK.get(), 0.9f, 1.0f + random.nextFloat() * 0.2f);
        if (isBewoner() || leen != null) {
            stack.consume(1, player);
            blij(2);
            VerzorgerRol.gevoerd(player, this);
            return;
        }
        if (isTame()) {
            stack.consume(1, player);
            heal(4);
            blij(2);
            return;
        }
        if (!SausdierenFeature.magTemmen(player)) {
            // (nothing is taken: it sniffs, but it doesn't know you yet)
            player.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.eerst_stal", getDisplayName()).withStyle(ChatFormatting.GOLD));
            return;
        }
        stack.consume(1, player);
        if (random.nextInt(TEM_KANS) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
            tame(player);
            setPersistenceRequired();
            getNavigation().stop();
            level().broadcastEntityEvent(this, (byte) 7);
            blij(6);
            player.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.getemd", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            GidsFeature.grant(player, "barbecuether/sausdieren_eigen_loper");
        } else {
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;   // (no breeding: feeding is handled in mobInteract)
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        // (a saddle only goes on somebody's own Sausloper or on a loaner: never on a wild one or on a resident of the stable)
        return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : isAlive() && (isTame() || leen != null);
    }

    @Override
    public boolean canBeLeashed() {
        return isTame() && leen == null && !isBewoner();
    }

    // --- riding --------------------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player player && player.isHolding(SausdierenFeature.PINDASAUS_STOK.get())
                ? player : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        setRot(controller.getYRot(), controller.getXRot() * 0.5f);
        yRotO = yBodyRot = yHeadRot = getYRot();
        steering.tickBoost();
        super.tickRidden(controller, riddenInput);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * (isKoud() ? 0.5 : 0.75) * steering.boostFactor());
    }

    @Override
    public boolean boost() {
        return steering.boost(getRandom());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !isVehicle();
    }

    @Override
    protected boolean shouldPassengersInheritMalus() {
        return true;
    }

    /** Getting off: next to it on something that is not sauce when there is such a spot (like the strider). */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Set<BlockPos> plekken = new LinkedHashSet<>();
        double top = getBoundingBox().maxY, bottom = getBoundingBox().minY - 0.5;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (float hoek : new float[]{0f, -22.5f, 22.5f, -45f, 45f, -90f, 90f, 180f}) {
            Vec3 d = getCollisionHorizontalEscapeVector(getBbWidth(), passenger.getBbWidth(), passenger.getYRot() + hoek);
            pos.set(getX() + d.x, top, getZ() + d.z);
            for (double y = top; y > bottom; y--) {
                plekken.add(pos.immutable());
                pos.move(Direction.DOWN);
            }
        }
        for (BlockPos p : plekken) {
            if (SausdierenFeature.isSaus(level().getFluidState(p))) {
                continue;
            }
            double vloer = level().getBlockFloorHeight(p);
            if (DismountHelper.isBlockFloorValid(vloer)) {
                Vec3 plek = Vec3.upFromBottomCenterOf(p, vloer);
                for (Pose pose : passenger.getDismountPoses()) {
                    if (DismountHelper.canDismountTo(level(), passenger, passenger.getLocalBoundsForPose(pose).move(plek))) {
                        passenger.setPose(pose);
                        return plek;
                    }
                }
            }
        }
        return new Vec3(getX(), getBoundingBox().maxY, getZ());
    }

    // --- sweet: never hurt ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.niet_slaan").withStyle(ChatFormatting.GRAY));
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // (wild ones come and go through WildeDieren, see SausdierenEvents)
    }

    @Override
    public boolean shouldBeSaved() {
        return leen == null && super.shouldBeSaved();
    }

    // --- sounds ----------------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SausdierenFeature.SAUSLOPER_GELUID.get();
    }

    @Override
    public float getVoicePitch() {
        return isKoud() ? 0.8f : 1.0f + (random.nextFloat() - random.nextFloat()) * 0.15f;
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.6f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (inSaus()) {
            playSound(SausdierenFeature.SAUSLOPER_STAP.get(), 0.5f, 1.0f);
        } else {
            super.playStepSound(pos, state);
        }
    }

    // --- save ------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.storeNullable("Leen", UUIDUtil.CODEC, leen);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        leen = tag.read("Leen", UUIDUtil.CODEC).orElse(null);
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SausloperEntity>("beweeg", 4, state -> {
            if (isInSittingPose()) {
                return state.setAndContinue(ZIT);
            }
            if (state.isMoving()) {
                return state.setAndContinue(isKoud() ? WALK_KOUD : WALK);
            }
            return state.setAndContinue(isKoud() ? BIBBER : IDLE);
        }));
        controllers.add(new AnimationController<SausloperEntity>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- AI --------------------------------------------------------------------------------------------------------------------------

    /** Follows the nearest player who holds the stick or its food (and remembers who: the questline's "lure" step). */
    class LokGoal extends Goal {
        @Nullable
        Player doel;
        private int rust;

        LokGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean lokt(@Nullable Player p) {
            return p != null && p.isAlive() && !p.isSpectator() && (isLokker(p.getMainHandItem()) || isLokker(p.getOffhandItem()));
        }

        @Override
        public boolean canUse() {
            if (rust > 0) {
                rust--;
                return false;
            }
            if (isOrderedToSit() || isVehicle() || teVerVanHuis()) {
                return false;
            }
            Player beste = null;
            double d = 10 * 10;
            for (Player p : level().players()) {
                double a = p.distanceToSqr(SausloperEntity.this);
                if (a < d && lokt(p)) {
                    d = a;
                    beste = p;
                }
            }
            doel = beste;
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && lokt(doel) && doel.distanceToSqr(SausloperEntity.this) < 14 * 14 && !isOrderedToSit() && !isVehicle() && !teVerVanHuis();
        }

        /** A resident of the stable does not let itself be lured far from home. */
        private boolean teVerVanHuis() {
            return hasHome() && isBewoner() && blockPosition().distManhattan(getHomePosition()) > THUIS_TE_VER - 4;
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
            if (distanceToSqr(doel) > 2.6 * 2.6) {
                if (tickCount % 5 == 0 || getNavigation().isDone()) {
                    getNavigation().moveTo(doel, 1.3);
                }
            } else {
                getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            doel = null;
            rust = 30;
            getNavigation().stop();
        }
    }

    /** Cold and not in the sauce: it walks to the nearest sauce (the strider's "go to lava"). */
    static class NaarSausGoal extends MoveToBlockGoal {
        private final SausloperEntity loper;

        NaarSausGoal(SausloperEntity loper, double speed) {
            super(loper, speed, 8, 2);
            this.loper = loper;
        }

        @Override
        public BlockPos getMoveToTarget() {
            return this.blockPos;
        }

        @Override
        public boolean canContinueToUse() {
            return !loper.inSaus() && isValidTarget(loper.level(), this.blockPos);
        }

        @Override
        public boolean canUse() {
            return loper.isKoud() && !loper.inSaus() && !loper.isOrderedToSit() && super.canUse();
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 20 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return SausdierenFeature.isSaus(level.getFluidState(pos)) && level.getFluidState(pos).isSource()
                    && level.getBlockState(pos.above()).isPathfindable(PathComputationType.LAND);
        }
    }

    /** Ground navigation that takes the sauce for ground. */
    static class SausNavigatie extends GroundPathNavigation {
        SausNavigatie(SausloperEntity mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }

        @Override
        protected boolean hasValidPathType(PathType pathType) {
            return pathType == PathType.LAVA || pathType == PathType.FIRE || pathType == PathType.FIRE_IN_NEIGHBOR || super.hasValidPathType(pathType);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            return SausdierenFeature.isSaus(this.level.getFluidState(pos)) || super.isStableDestination(pos);
        }
    }
}
