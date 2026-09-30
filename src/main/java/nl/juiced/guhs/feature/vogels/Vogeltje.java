package nl.juiced.guhs.feature.vogels;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A little bird of the Guhmensie (3.0 vogels). Common behaviour of the four birds:
 * <ul>
 *   <li><b>Sitting</b> on the ground, a branch, a roof or a {@link VoerhuisjeBlock}: hopping about, pecking, looking at you.</li>
 *   <li><b>Flying</b> (no gravity, steered like the kaasmot) around its home spot for a while, then it picks a landing spot
 *   (the top of the ground or of a tree: the MOTION_BLOCKING heightmap; a bird feeder with food; its own special spot).</li>
 *   <li><b>Startled</b>: a player who comes within {@link #schrikAfstand()} without sneaking (and without the bird's food in
 *   hand, and who didn't feed it just now) or who hits it makes it fly up. Sneak to get really close.</li>
 *   <li><b>Fed</b> with its favourite food ({@link #lekker(ItemStack)}): hearts, a happy song, it trusts you for a while.</li>
 * </ul>
 * Birds are always friendly: no target, no attack, no fall damage. They don't despawn (a spawn rule keeps their number
 * down, {@link VogelSpawns}). Subclasses: {@link PluisvinkjeEntity}, {@link KaasmeesjeEntity}, {@link GuhUiltjeEntity},
 * {@link ZeemeeuwtjeEntity}.
 */
public abstract class Vogeltje extends PathfinderMob implements GeoEntity {
    /** Flying? */
    static final EntityDataAccessor<Boolean> VLIEGT = SynchedEntityData.defineId(Vogeltje.class, EntityDataSerializers.BOOLEAN);
    /** The pose while sitting: {@link #STAAT}, {@link #HANGT} (upside down, the kaasmeesje), {@link #SLAAPT} (the owl by day). */
    static final EntityDataAccessor<Integer> HOUDING = SynchedEntityData.defineId(Vogeltje.class, EntityDataSerializers.INT);
    public static final int STAAT = 0, HANGT = 1, SLAAPT = 2;
    /** How far from its home spot a bird flies around. */
    public static final int THUIS_STRAAL = 14;

    protected final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    protected Vec3 doel;
    @Nullable
    protected BlockPos landplek;
    @Nullable
    protected BlockPos thuis;
    protected int vliegTijd;
    protected int zitTijd = 100;
    protected int hupTijd = 40;
    protected int vast;
    /** Ticks this bird still trusts the player who fed it (it doesn't fly away from anyone then). */
    protected int vertrouwen;
    /** Hanging / special landing: the spot is not the ground top (the kaasmeesje hangs, a bird on a feeder). */
    protected boolean bijzonderePlek;
    private double laatsteAfstand = Double.MAX_VALUE;

    protected Vogeltje(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(double health) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VLIEGT, false);
        builder.define(HOUDING, STAAT);
    }

    // --- per bird --------------------------------------------------------------------------------------------------------
    /** Flying speed (blocks per tick, roughly). */
    protected abstract double vliegSnelheid();

    /** Its favourite food (feeding it: hearts, trust, {@link #gevoerd}). */
    public abstract boolean lekker(ItemStack stack);

    /** Its call (ambient and when it's happy). */
    protected abstract SoundEvent roep();

    /** How close a (not sneaking) player may come before it flies up. */
    public double schrikAfstand() {
        return 4.0;
    }

    /** The name of this bird's animation file ("pluisvinkje", ...). */
    public abstract String naam();

    /** Called after the bird was fed by a player (the subclasses add their own reward: a feather, a song, "Mijn!"). */
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
    }

    /** A landing spot of its own (a leaf to hang under, a berry bush, a player to circle...), or null for the usual ones. */
    @Nullable
    protected BlockPos eigenLandplek() {
        return null;
    }

    /** Called every server tick while it sits (after the common logic). */
    protected void zitStap() {
    }

    /** Called every server tick while it flies; may change {@link #doel}. Return true when it handled the steering itself. */
    protected boolean vliegStap() {
        return false;
    }

    /** May it take off now by itself (the owl stays put by day)? */
    protected boolean wilVliegen() {
        return true;
    }

    // --- state ----------------------------------------------------------------------------------------------------------
    public boolean vliegt() {
        return entityData.get(VLIEGT);
    }

    public int houding() {
        return entityData.get(HOUDING);
    }

    public void zetHouding(int h) {
        entityData.set(HOUDING, h);
    }

    @Nullable
    public BlockPos thuis() {
        return thuis;
    }

    public void zetThuis(BlockPos pos) {
        thuis = pos.immutable();
    }

    @Nullable
    public Vec3 doel() {
        return doel;
    }

    @Nullable
    public BlockPos landplek() {
        return landplek;
    }

    public int vertrouwen() {
        return vertrouwen;
    }

    /** Takes off: flies around for a while (away from `weg` if given), then looks for a place to land. */
    public void startVliegen(@Nullable Vec3 weg, int tijd) {
        if (!vliegt()) {
            level().playSound(null, getX(), getY(), getZ(), VogelsFeature.FLADDER.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + random.nextFloat() * 0.3f);
        }
        entityData.set(VLIEGT, true);
        zetHouding(STAAT);
        setNoGravity(true);
        vliegTijd = tijd;
        landplek = null;
        bijzonderePlek = false;
        vast = 0;
        Vec3 up = new Vec3(0, 0.25, 0);
        if (weg != null) {
            Vec3 away = position().subtract(weg).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) {
                up = up.add(away.normalize().scale(0.25));
            }
            doel = position().add(away.lengthSqr() > 1e-4 ? away.normalize().scale(6) : Vec3.ZERO).add(0, 4, 0);
        } else {
            doel = kiesVliegDoel();
        }
        setDeltaMovement(getDeltaMovement().add(up));
    }

    /** Lands right here (tests, and when it reaches its landing spot). */
    public void land() {
        entityData.set(VLIEGT, false);
        setNoGravity(houding() == HANGT);
        doel = null;
        vliegTijd = 0;
        zitTijd = 200 + random.nextInt(600);
        setDeltaMovement(Vec3.ZERO);
    }

    // --- ticking ---------------------------------------------------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && vliegt() && random.nextInt(6) == 0 && this instanceof PluisvinkjeEntity) {
            level().addParticle(VogelsFeature.VEERTJE.get(), getX(), getY() + 0.2, getZ(), 0, -0.01, 0);
        }
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        if (thuis == null) {
            thuis = blockPosition();
        }
        if (vertrouwen > 0) {
            vertrouwen--;
        }
        if (vliegt()) {
            vliegen();
        } else {
            zitten();
        }
    }

    private void vliegen() {
        setNoGravity(true);
        if (!vliegStap()) {
            if (landplek != null) {
                Vec3 to = landDoel(landplek);
                double d = to.distanceTo(position());
                if (d < 0.45 || (d < 1.2 && vast > 40)) {
                    snapTo(to.x, to.y, to.z, getYRot(), getXRot());
                    land();
                    geland(landplek);
                    return;
                }
                vast = d < laatsteAfstand - 0.02 ? 0 : vast + 1;
                laatsteAfstand = d;
                if (vast > 80) {                // can't get there: another spot
                    landplek = null;
                    bijzonderePlek = false;
                    vast = 0;
                    laatsteAfstand = Double.MAX_VALUE;
                    doel = kiesVliegDoel();
                    vliegTijd = 20 + random.nextInt(40);
                    return;
                }
                stuur(to, Math.min(vliegSnelheid(), 0.12 + d * 0.08));
                return;
            }
            if (--vliegTijd <= 0) {
                landplek = kiesLandplek();
                laatsteAfstand = Double.MAX_VALUE;
                if (landplek == null) {
                    vliegTijd = 40;
                }
                return;
            }
            if (doel == null || doel.distanceToSqr(position()) < 2.0 || random.nextInt(80) == 0 || horizontalCollision) {
                doel = kiesVliegDoel();
                if (horizontalCollision && doel != null) {
                    doel = doel.add(0, 3, 0);
                }
            }
        }
        if (doel != null) {
            stuur(doel, vliegSnelheid());
        }
    }

    /** Where it flies to while it flies around: somewhere above its home, 3 to 9 blocks over the ground. */
    @Nullable
    protected Vec3 kiesVliegDoel() {
        BlockPos base = thuis != null ? thuis : blockPosition();
        int x = base.getX() + random.nextInt(THUIS_STRAAL * 2 + 1) - THUIS_STRAAL;
        int z = base.getZ() + random.nextInt(THUIS_STRAAL * 2 + 1) - THUIS_STRAAL;
        int top = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int y = Math.max(top + 3 + random.nextInt(7), level().getSeaLevel() + 2);
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    /** Where the entity stands when it lands on this spot. */
    protected Vec3 landDoel(BlockPos plek) {
        if (houding() == HANGT) {
            return new Vec3(plek.getX() + 0.5, plek.getY() + 1 - getBbHeight(), plek.getZ() + 0.5);
        }
        BlockState s = level().getBlockState(plek.below());
        double top = 0;
        if (s.getBlock() instanceof VoerhuisjeBlock) {
            return new Vec3(plek.getX() + 0.5, plek.getY() - 1 + VoerhuisjeBlock.TAFEL_HOOGTE, plek.getZ() + 0.5);
        }
        return new Vec3(plek.getX() + 0.5, plek.getY() + top, plek.getZ() + 0.5);
    }

    /** Right after landing on its spot. */
    protected void geland(BlockPos plek) {
    }

    /** A place to land: its own spot, sometimes a bird feeder with food, else the top of the ground / a tree nearby. */
    @Nullable
    protected BlockPos kiesLandplek() {
        BlockPos eigen = eigenLandplek();
        if (eigen != null) {
            bijzonderePlek = true;
            return eigen;
        }
        zetHouding(STAAT);
        if (level() instanceof ServerLevel server && random.nextInt(10) < 6) {
            BlockPos voer = VoerhuisjeBlock.dichtbij(server, blockPosition(), 16);
            if (voer != null) {
                bijzonderePlek = true;
                return voer.above();
            }
        }
        bijzonderePlek = false;
        BlockPos base = thuis != null && thuis.distSqr(blockPosition()) > 24 * 24 ? thuis : blockPosition();
        for (int i = 0; i < 12; i++) {
            int x = base.getX() + random.nextInt(21) - 10;
            int z = base.getZ() + random.nextInt(21) - 10;
            BlockPos top = new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (magLanden(top)) {
                return top;
            }
        }
        return null;
    }

    /** Can it sit on the block below this (air) spot? Solid tops and leaves; no water (the gull says otherwise). */
    public boolean magLanden(BlockPos plek) {
        if (!level().isLoaded(plek) || plek.getY() <= level().getMinY() + 1) {
            return false;
        }
        BlockState onder = level().getBlockState(plek.below());
        BlockState hier = level().getBlockState(plek);
        if (!hier.getFluidState().isEmpty() || !hier.getCollisionShape(level(), plek).isEmpty()) {
            return false;
        }
        return onder.getFluidState().isEmpty() && (onder.is(BlockTags.LEAVES) || onder.isFaceSturdy(level(), plek.below(), net.minecraft.core.Direction.UP));
    }

    /** Steers towards `to` (the kaasmot's flight, smoother). */
    protected void stuur(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        double len = d.length();
        if (len < 1e-3) {
            setDeltaMovement(getDeltaMovement().scale(0.5));
            return;
        }
        Vec3 want = d.scale(Math.min(speed, len * 0.5) / len);
        Vec3 v = getDeltaMovement();
        Vec3 nv = v.add(want.subtract(v).scale(0.15));
        setDeltaMovement(nv);
        if (nv.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(nv.z, nv.x) * 180.0 / Math.PI) - 90.0f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private void zitten() {
        if (houding() == HANGT) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        } else {
            setNoGravity(false);
        }
        if (isInWater() && !magZwemmen()) {
            startVliegen(null, 60);
            return;
        }
        // startled?
        Player bang = schrikVan();
        if (bang != null) {
            schrik(bang.position());
            return;
        }
        if (--zitTijd <= 0 && wilVliegen()) {
            startVliegen(null, 120 + random.nextInt(200));
            return;
        }
        if (houding() == STAAT && onGround()) {
            Player kijk = level().getNearestPlayer(this, 8);
            if (kijk != null && random.nextInt(3) != 0) {
                getLookControl().setLookAt(kijk, 30, 30);
            }
            if (--hupTijd <= 0) {
                hupTijd = 30 + random.nextInt(80);
                int r = random.nextInt(4);
                if (r == 0 || opVoerhuisje() != null) {
                    triggerAnim("actie", "peck");
                    pik();
                } else if (r == 1 && !bijzonderePlek) {
                    float yaw = random.nextFloat() * 360f;
                    Vec3 dir = Vec3.directionFromRotation(0, yaw);
                    BlockPos next = BlockPos.containing(position().add(dir.scale(1.2)));
                    if (level().getBlockState(next).getCollisionShape(level(), next).isEmpty() && !level().getBlockState(next.below()).isAir()
                            && level().getFluidState(next.below()).isEmpty()) {
                        setYRot(yaw);
                        yBodyRot = yaw;
                        setDeltaMovement(dir.x * 0.18, 0.3, dir.z * 0.18);
                    }
                }
            }
        }
        zitStap();
    }

    /** The bird feeder it sits on, or null. */
    @Nullable
    public BlockPos opVoerhuisje() {
        for (BlockPos p : new BlockPos[]{blockPosition(), blockPosition().below()}) {
            if (level().getBlockState(p).getBlock() instanceof VoerhuisjeBlock) {
                return p;
            }
        }
        return null;
    }

    /** A peck: at a bird feeder it eats (the seeds go down now and then, {@link #smult}). */
    protected void pik() {
        BlockPos voer = opVoerhuisje();
        if (voer != null && level() instanceof ServerLevel server && VoerhuisjeBlock.pik(server, voer, this)) {
            smult(server, voer);
        }
    }

    /** Ate a bit at a bird feeder (the pluisvinkje may lose a feather). */
    protected void smult(ServerLevel level, BlockPos voerhuisje) {
    }

    /** May it sit in water (the gull bobs on the sea)? */
    protected boolean magZwemmen() {
        return false;
    }

    /** The player that startles it now, or null. */
    @Nullable
    public Player schrikVan() {
        if (vertrouwen > 0) {
            return null;
        }
        double r = schrikAfstand();
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), p -> !p.isSpectator() && p.isAlive())) {
            if (p.isShiftKeyDown()) {
                continue;             // sneaking: it stays (that's how you get close)
            }
            if (lekker(p.getMainHandItem()) || lekker(p.getOffhandItem())) {
                continue;             // it's curious about the food
            }
            if (p.distanceTo(this) <= r) {
                return p;
            }
        }
        return null;
    }

    /** Flies up, away from `van` (and warns its flock). */
    public void schrik(Vec3 van) {
        startVliegen(van, 80 + random.nextInt(120));
    }

    // --- players -------------------------------------------------------------------------------------------------------
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (lekker(stack)) {
            if (level() instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                vertrouwen = 1200;
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                level().playSound(null, getX(), getY(), getZ(), roep(), SoundSource.NEUTRAL, 0.8f, 1.1f + random.nextFloat() * 0.2f);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.4f, 1.8f);
                getLookControl().setLookAt(player, 30, 30);
                if (vliegt() && landplek == null) {
                    vliegTijd = 1;
                }
                GidsFeature.grant(sp, "diertjes/vogels_voeren");
                gevoerd(sp, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !level().isClientSide() && isAlive()) {
            vertrouwen = 0;
            Entity by = source.getEntity();
            schrik(by != null ? by.position() : position());
        }
        return hurt;
    }

    // --- always friendly ------------------------------------------------------------------------------------------------
    @Override
    public void setTarget(@Nullable LivingEntity target) {
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, Entity target) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isPushable() {
        return !vliegt() && houding() != HANGT;
    }

    // --- sounds --------------------------------------------------------------------------------------------------------
    @Override
    protected SoundEvent getAmbientSound() {
        return houding() == SLAAPT ? null : roep();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // --- saving ------------------------------------------------------------------------------------------------------
    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (thuis != null) {
            tag.store("VogelThuis", BlockPos.CODEC, thuis);
        }
        tag.putBoolean("VogelVliegt", vliegt());
        tag.putInt("VogelHouding", houding());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        (tag).read("VogelThuis", BlockPos.CODEC).ifPresent(p -> thuis = p);
        entityData.set(VLIEGT, tag.getBooleanOr("VogelVliegt", false));
        entityData.set(HOUDING, tag.getIntOr("VogelHouding", 0));
        setNoGravity(vliegt() || houding() == HANGT);
    }

    // --- helpers for the subclasses -------------------------------------------------------------------------------------
    /** The players within `r` (for advancements of things you see happen). */
    protected List<ServerPlayer> kijkers(double r) {
        return level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(r), p -> !p.isSpectator());
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------
    protected RawAnimation anim(String name, boolean loop) {
        String full = "animation." + naam() + "." + name;
        return loop ? RawAnimation.begin().thenLoop(full) : RawAnimation.begin().thenPlay(full);
    }

    /** The looping animation for the current state (the subclasses add their own poses). */
    protected RawAnimation beweging(com.geckolib.animation.state.AnimationTest<Vogeltje> state) {
        if (vliegt()) {
            return anim("fly", true);
        }
        if (!onGround() && !isInWater()) {
            return anim("hop", true);
        }
        return state.isMoving() ? anim("hop", true) : anim("idle", true);
    }

    /** Extra one-shot animations of a bird (besides "peck"): name -> animation. */
    protected String[] extraActies() {
        return new String[0];
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Vogeltje> beweging = new AnimationController<>("beweging", 4, state -> {
            state.setAnimation(beweging(state));
            return PlayState.CONTINUE;
        });
        AnimationController<Vogeltje> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        actie.triggerableAnim("peck", anim("peck", false));
        for (String a : extraActies()) {
            actie.triggerableAnim(a, anim(a, false));
        }
        controllers.add(beweging, actie);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
