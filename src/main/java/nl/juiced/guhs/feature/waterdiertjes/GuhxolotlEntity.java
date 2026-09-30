package nl.juiced.guhs.feature.waterdiertjes;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.PiepMenu;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * De guhxolotl (3.0, DESIGN_30 §6): a guh that is an axolotl. It behaves like a vanilla axolotl, but lief:
 * <ul>
 *   <li>It swims in the pools of the Guhzee and the Kaasmoeras and crawls slowly on land; out of the water it gets dry
 *       after {@link #DROOG_NA} ticks: then it is a bit sad and slow and heads back to the water (it is never hurt by it).
 *       Rain, water, or living in a guhhuisje keeps it moist.</li>
 *   <li>When it gets hurt it plays dead ("plat als een pannenkoekje", {@link #PLAT_TICKS}) and gets a little regeneration.</li>
 *   <li>Five colours ({@link Kleur}): roze, mint, choco, wit and, rarely ({@link #GOUD_KANS}), goud.</li>
 *   <li>A guhvisje (guhs:guh_vis, or a bucket of them) tames it (1 in {@link #TAME_CHANCE}; always with "Lief kijken");
 *       tamed guhxolotls with a guhvisje fall in love and get a little one (the colour of a parent, now and then golden).</li>
 *   <li>A water bucket scoops it (wild or your own) into a {@link GuhxolotlEmmertje}; sneak + empty hand (owner) or
 *       "Oppakken" in its menu picks it up too. It is a piep-maatje ({@link PiepMaatje}): menu, "waar is hij", a guhhuisje
 *       to live in. Its menu button "Blubbeltjes!" gives you water breathing for a while.</li>
 * </ul>
 */
public class GuhxolotlEntity extends TamableAnimal implements GeoEntity, PiepMaatje {
    public static final int TAME_CHANCE = 3;
    /** 1 in this many wild guhxolotls is golden (breeding: 1 in {@link #GOUD_KANS_KLEINTJE}). */
    public static final int GOUD_KANS = 100, GOUD_KANS_KLEINTJE = 40;
    /** Out of the water this long: dry (5 minutes, like a vanilla axolotl's air). */
    public static final int DROOG_NA = 20 * 60 * 5;
    /** How long it plays dead after being hurt. */
    public static final int PLAT_TICKS = 20 * 10;
    /** The "Blubbeltjes!" button: water breathing for this long, then it rests. */
    public static final int BLUB_TICKS = 20 * 120, BLUB_RUST = 20 * 60 * 3;

    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(GuhxolotlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_UIT = SynchedEntityData.defineId(GuhxolotlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAT = SynchedEntityData.defineId(GuhxolotlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DROOG = SynchedEntityData.defineId(GuhxolotlEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation PLAT = RawAnimation.begin().thenLoop("plat");
    private static final RawAnimation DROOG = RawAnimation.begin().thenLoop("droog");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** The five colours (saved by name, synced by ordinal: append only). */
    public enum Kleur implements StringRepresentable {
        ROZE, MINT, CHOCO, WIT, GOUD;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kleur van(int i) {
            Kleur[] all = values();
            return all[Math.floorMod(i, all.length)];
        }

        public static Kleur van(String naam) {
            for (Kleur k : values()) {
                if (k.getSerializedName().equals(naam)) {
                    return k;
                }
            }
            return ROZE;
        }

        /** A wild one: roze, mint, choco and wit about equally often, goud 1 in {@link #GOUD_KANS}. */
        public static Kleur rol(RandomSource random) {
            if (random.nextInt(GOUD_KANS) == 0) {
                return GOUD;
            }
            return values()[random.nextInt(4)];
        }

        public String langKey() {
            return "gui.guhs.waterdiertjes.kleur." + getSerializedName();
        }
    }

    /** Ticks out of the water (saved), the playing-dead timer, the button's rest. */
    private int droogTicks, platTicks;
    private long rustTot;

    public GuhxolotlEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1f, 0.5f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        this.setPathfindingMalus(PathType.WATER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /**
     * Spawns in shallow water: the pools and edges of the Guhzee and the Kaasmoeras (water here, land or the bottom close
     * by, near the sea level). Spawn eggs and commands: anywhere.
     */
    public static boolean checkSpawn(EntityType<GuhxolotlEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != EntitySpawnReason.NATURAL && spawnType != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        int sea = level instanceof Level l ? l.getSeaLevel() : 63;
        if (pos.getY() < sea - 8 || pos.getY() > sea + 12) {
            return false;
        }
        int diep = 0;
        BlockPos.MutableBlockPos p = pos.mutable();
        while (diep < 6 && level.getFluidState(p).is(FluidTags.WATER)) {
            p.move(0, -1, 0);
            diep++;
        }
        if (diep >= 6) {
            return false;                                      // (open, deep water: no pool)
        }
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-6, -1, -6), pos.offset(6, 1, 6))) {
            if (level.getFluidState(q).isEmpty() && level.getBlockState(q).isSolid() && level.getBlockState(q.above()).isAir()) {
                return level.getEntitiesOfClass(GuhxolotlEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(24)).size() < 6;
            }
        }
        return false;
    }

    // --- data ------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_UIT, 0);
        builder.define(DATA_PLAT, false);
        builder.define(DATA_DROOG, false);
    }

    public Kleur kleur() {
        return Kleur.van(entityData.get(DATA_KLEUR));
    }

    public void setKleur(Kleur kleur) {
        entityData.set(DATA_KLEUR, kleur.ordinal());
    }

    public boolean isPlat() {
        return entityData.get(DATA_PLAT);
    }

    public boolean isDroog() {
        return entityData.get(DATA_DROOG);
    }

    public int droogTicks() {
        return droogTicks;
    }

    /** (Tests) how long it has been out of the water. */
    public void setDroogTicks(int ticks) {
        droogTicks = ticks;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        if (spawnType != EntitySpawnReason.BREEDING && spawnType != EntitySpawnReason.BUCKET) {
            setKleur(Kleur.rol(level.getRandom()));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", kleur().getSerializedName());
        tag.putInt("PiepUit", uitVlaggen());
        tag.putInt("Droog", droogTicks);
        tag.putLong("BlubRust", rustTot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKleur(Kleur.van(tag.getStringOr("Kleur", "")));
        setUitVlaggen(tag.getIntOr("PiepUit", 0));
        droogTicks = tag.getIntOr("Droog", 0);
        rustTot = tag.getLongOr("BlubRust", 0L);
    }

    // --- moving: like an axolotl (smooth swimming, slow crawling) ---------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return DROOG_NA;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PlatGoal());
        goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return !isTame() && !isPlat() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new BlijfGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new NaarWaterGoal());
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
        goalSelector.addGoal(4, new VolgGoal());
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 20) {
            @Override
            public boolean canUse() {
                return aan(PiepInstelling.ZWEMMEN) && !isPlat() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6, 100) {
            @Override
            public boolean canUse() {
                return !isPlat() && !isInWater() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void travel(Vec3 input) {
        if (isPlat()) {
            super.travel(Vec3.ZERO);
            return;
        }
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input.scale(isDroog() ? 0.5 : 1.0));
        }
    }

    @Override
    public void baseTick() {
        int air = getAirSupply();
        super.baseTick();
        setAirSupply(air);                                         // (it never drowns or suffocates in water: it has gills)
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        boolean nat = isInWaterRainOrBubble() || Huisjes.isBewoner(this) || isPassenger();
        droogTicks = nat ? 0 : droogTicks + 1;
        boolean droog = droogTicks >= DROOG_NA;
        if (droog != isDroog()) {
            entityData.set(DATA_DROOG, droog);
            if (droog && isTame() && getOwner() instanceof ServerPlayer owner && distanceToSqr(owner) < 32 * 32) {
                owner.sendOverlayMessage(Component.translatable("gui.guhs.waterdiertjes.droog", getDisplayName())
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        if (droog && tickCount % 40 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 0.5, getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
        if (platTicks > 0 && --platTicks == 0) {
            entityData.set(DATA_PLAT, false);
            triggerAnim("actie", "blij");
        }
        if (kleur() == Kleur.GOUD && tickCount % 30 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 0.35, getZ(), 1, 0.25, 0.15, 0.25, 0);
        }
    }

    // --- playing dead ------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && isAlive() && !isPlat() && random.nextInt(3) > 0) {
            speelPlat();
        }
        return hurt;
    }

    /** Plays dead: on its back, paws up, for {@link #PLAT_TICKS}; meanwhile a little regeneration and nobody bothers it. */
    public void speelPlat() {
        platTicks = PLAT_TICKS;
        entityData.set(DATA_PLAT, true);
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, PLAT_TICKS, 0));
        getNavigation().stop();
    }

    class PlatGoal extends Goal {
        PlatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isPlat();
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }
    }

    /** Dry: head for the nearest water (within 12 blocks). */
    class NaarWaterGoal extends Goal {
        @Nullable
        private BlockPos doel;
        private int opnieuw;

        NaarWaterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isDroog() || isPlat() || isOrderedToSit() || --opnieuw > 0) {
                return false;
            }
            opnieuw = 40;
            doel = null;
            BlockPos here = blockPosition();
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(here.offset(-12, -3, -12), here.offset(12, 3, 12))) {
                if (level().getFluidState(p).is(FluidTags.WATER)) {
                    double d = p.distSqr(here);
                    if (d < best) {
                        best = d;
                        doel = p.immutable();
                    }
                }
            }
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && isDroog() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
        }
    }

    /** Follows its owner over land and through water (vanilla's FollowOwnerGoal can't swim); too far away, it hops over. */
    class VolgGoal extends Goal {
        private int herberekenen;

        VolgGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = getOwner();
            return isTame() && owner != null && !owner.isSpectator() && !isOrderedToSit() && !isPlat() && aan(PiepInstelling.VOLGEN)
                    && !Huisjes.isBewoner(GuhxolotlEntity.this) && distanceToSqr(owner) > 6 * 6;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = getOwner();
            return owner != null && !getNavigation().isDone() && !isOrderedToSit() && !isPlat() && aan(PiepInstelling.VOLGEN)
                    && distanceToSqr(owner) > 2.5 * 2.5;
        }

        @Override
        public void start() {
            herberekenen = 0;
        }

        @Override
        public void tick() {
            LivingEntity owner = getOwner();
            if (owner == null) {
                return;
            }
            getLookControl().setLookAt(owner, 10f, getMaxHeadXRot());
            if (--herberekenen > 0) {
                return;
            }
            herberekenen = adjustedTickDelay(10);
            if (distanceToSqr(owner) > 16 * 16) {
                BlockPos at = owner.blockPosition();
                for (int i = 0; i < 10; i++) {
                    BlockPos p = at.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                    if (level().getBlockState(p).getCollisionShape(level(), p).isEmpty()
                            && (!level().getBlockState(p.below()).getCollisionShape(level(), p.below()).isEmpty() || level().getFluidState(p).is(FluidTags.WATER))) {
                        moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, getYRot(), getXRot());
                        getNavigation().stop();
                        return;
                    }
                }
            } else {
                getNavigation().moveTo(owner, 1.3);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // --- food, taming, breeding ---------------------------------------------------------------------------------------------

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.GUH_VIS.get()) || stack.is(ModItems.GUH_VIS_BUCKET.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.WATER_BUCKET)) {
            return schep(player, hand, stack);
        }
        boolean voer = isFood(stack);
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!voer && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        ServerLevel level = (ServerLevel) level();
        if (voer) {
            eetVisje(player, hand, stack);
            triggerAnim("actie", "eet");
            playSound(WaterdiertjesFeature.BLUB.get(), 0.8f, 1.2f);
            if (!isTame()) {
                if ((player.hasEffect(PiepFeature.LIEF_KIJKEN) || random.nextInt(TAME_CHANCE) == 0)
                        && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    temmen(sp);
                } else {
                    level.broadcastEntityEvent(this, (byte) 6);
                }
            } else if (isOwnedBy(player) && getAge() == 0 && canFallInLove()) {
                setInLove(player);                              // two tamed ones with a guhvisje: a little one
            } else {
                heal(4);
                level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && player.isSecondaryUseActive()) {
            PiepDierItem.pakOp(this, sp);                       // sneak + empty hand (owner): into an emmertje
            return InteractionResult.SUCCESS;
        }
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0);
        triggerAnim("actie", "blij");
        playSound(WaterdiertjesFeature.BLUB.get(), 0.7f, 1.3f);
        if (isTame() && isOwnedBy(player)) {
            PiepMenu.open(sp, this);
        } else if (isTame() && player.isSecondaryUseActive()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.niet_jouw_maatje").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    /** A guhvisje eaten: a bucket of guhvisjes gives back a water bucket. */
    private void eetVisje(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(ModItems.GUH_VIS_BUCKET.get())) {
            if (!player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            }
        } else {
            stack.consume(1, player);
        }
    }

    /** A water bucket: scoop it up (a wild one, or your own tamed one) into a guhxolotl-emmertje. */
    private InteractionResult schep(Player player, InteractionHand hand, ItemStack bucket) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (isTame() && !isOwnedBy(player)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.niet_jouw_maatje").withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }
        if (isBezig()) {
            return InteractionResult.CONSUME;
        }
        ItemStack emmer = GuhxolotlEmmertje.vol(this, true);
        playSound(SoundEvents.BUCKET_FILL_AXOLOTL, 1f, 1f);
        nl.juiced.guhs.feature.band.GuhVolger.inZakken(emmer, player);
        if (player.hasInfiniteMaterials()) {
            if (!player.getInventory().add(emmer)) {
                player.drop(emmer, false);
            }
        } else if (bucket.getCount() == 1) {
            player.setItemInHand(hand, emmer);
        } else {
            bucket.shrink(1);
            if (!player.getInventory().add(emmer)) {
                player.drop(emmer, false);
            }
        }
        sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.opgepakt.guhxolotl", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        discard();
        return InteractionResult.SUCCESS;
    }

    public void temmen(ServerPlayer player) {
        tame(player);
        getNavigation().stop();
        level().broadcastEntityEvent(this, (byte) 7);
        triggerAnim("actie", "blij");
        player.sendOverlayMessage(Component.translatable("gui.guhs.waterdiertjes.getemd", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        WaterdiertjesEvents.getemd(player, this);
    }

    @Override
    public boolean canMate(Animal other) {
        return other instanceof GuhxolotlEntity x && x != this && isInLove() && x.isInLove() && !isPlat() && !x.isPlat();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        GuhxolotlEntity baby = WaterdiertjesFeature.GUHXOLOTL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baby == null) {
            return null;
        }
        Kleur k = random.nextInt(GOUD_KANS_KLEINTJE) == 0 ? Kleur.GOUD
                : (random.nextBoolean() || !(other instanceof GuhxolotlEntity x) ? kleur() : x.kleur());
        baby.setKleur(k);
        if (isTame() && getOwnerUUID() != null) {
            baby.setOwnerUUID(getOwnerUUID());
            baby.setTame(true, true);
        }
        return baby;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTame() && !hasCustomName() && !isPersistenceRequired();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return isTame() || super.requiresCustomPersistence();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return false;
    }

    /** Its name floats above it once it is tamed (like the other maatjes); wild ones stay anonymous. */
    @Override
    public boolean shouldShowName() {
        return (isTame() || hasCustomName()) && !isInvisible();
    }

    // --- sounds --------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isInWater() ? WaterdiertjesFeature.BLUB.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("actie", "blub");
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.4, getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.4f : 1.0f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    // --- the menu (PiepMaatje) ------------------------------------------------------------------------------------------------

    @Override
    public TamableAnimal dier() {
        return this;
    }

    @Override
    public String soort() {
        return "guhxolotl";
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return PiepInstelling.POEPSCHILLY;
    }

    @Override
    public Item oppakItem() {
        return WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get();
    }

    @Override
    public int uitVlaggen() {
        return entityData.get(DATA_UIT);
    }

    @Override
    public void setUitVlaggen(int vlaggen) {
        entityData.set(DATA_UIT, vlaggen);
        setPathfindingMalus(PathType.WATER, aan(PiepInstelling.ZWEMMEN) ? 0.0f : 8.0f);
    }

    @Override
    public boolean isBezig() {
        return isPlat();
    }

    @Override
    public SoundEvent oppakGeluid() {
        return SoundEvents.BUCKET_FILL_AXOLOTL;
    }

    @Override
    public int rustSeconden() {
        return (int) Math.max(0, (rustTot - level().getGameTime() + 19) / 20);
    }

    /** "Blubbeltjes!": it blows a cloud of bubbles around you: water breathing for {@link #BLUB_TICKS}, then it rests. */
    @Override
    public void speciaal(ServerPlayer player) {
        long now = level().getGameTime();
        if (now < rustTot) {
            long sec = (rustTot - now) / 20;
            player.sendOverlayMessage(Component.translatable("gui.guhs.waterdiertjes.blub_rust", sec / 60, String.format(Locale.ROOT, "%02d", sec % 60))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        rustTot = now + BLUB_RUST;
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, BLUB_TICKS, 0, false, true, true));
        triggerAnim("actie", "blub");
        playSound(WaterdiertjesFeature.BLUB.get(), 1f, 0.9f);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.5, 0.6, 0.5, 0.02);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.waterdiertjes.blubbeltjes", getDisplayName()).withStyle(ChatFormatting.AQUA));
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "waterdiertjes_blubbeltjes");
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 4, state -> {
            if (isPlat()) {
                return state.setAndContinue(PLAT);
            }
            if (isInWater()) {
                return state.setAndContinue(state.isMoving() ? SWIM : IDLE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(isDroog() ? DROOG : IDLE);
        }));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"))
                .triggerableAnim("blub", RawAnimation.begin().thenPlay("blub")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
