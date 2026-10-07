package nl.juiced.guhs.feature.einde2.mikas;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guheinde.GuheindeReis;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.GuhTime;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * De Schuilguh (the shulker of the Guheinde, but a guh and so always lief): a shy guh that lives in a round kaasdoos, stuck
 * to a wall, a ceiling or the floor (NBT {@code AttachFace}: the face of the block it holds on to, as the vanilla shulker).
 * <ul>
 *   <li>Far away its lid is shut; come nearer and it peeks; next to it the lid goes up and it looks at you.</li>
 *   <li><b>Een kusje</b> (an empty hand): you float for a moment ({@link #ZWEEF_TIJD} ticks of levitation) and land softly.</li>
 *   <li><b>Voeren</b> (a kaasknabbel): it pushes a doosschil out of its box, at most once per Minecraft day per Schuilguh.</li>
 *   <li>Hit it and nothing happens to it: the lid just snaps shut for a while ("toe!").</li>
 * </ul>
 * It never moves by itself, never despawns and can be stood on. When the block it holds on to goes, it takes hold of
 * another one next to it, or sinks down until it finds a floor.
 */
public class SchuilguhEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Direction> ATTACH_FACE = SynchedEntityData.defineId(SchuilguhEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Byte> STAND = SynchedEntityData.defineId(SchuilguhEntity.class, EntityDataSerializers.BYTE);
    /** The lid (synced): shut, ajar (it peeks) or thrown back. */
    public static final byte DICHT = 0, GLUUR = 1, OPEN = 2;

    /** Within this many blocks of a player the lid is open / it peeks. */
    public static final double OPEN_AFSTAND = 3.5, GLUUR_AFSTAND = 8.0;
    /** How long a kusje lets you float (ticks: 3 seconds of levitation I), and how long you fall slowly after it. */
    public static final int ZWEEF_TIJD = 60, ZACHT_TIJD = 200;
    /** Ticks between two kusjes of the same Schuilguh, and how long it stays shut after a fright. */
    public static final int KUS_PAUZE = 100, SCHRIK_TIJD = 100;

    private static final RawAnimation DICHT_ANIM = RawAnimation.begin().thenLoop("dicht");
    private static final RawAnimation GLUUR_ANIM = RawAnimation.begin().thenLoop("gluur");
    private static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenLoop("open");
    private static final RawAnimation KUSJE = RawAnimation.begin().thenPlay("kusje");
    private static final RawAnimation EET = RawAnimation.begin().thenPlay("eet");
    private static final RawAnimation SCHRIK = RawAnimation.begin().thenPlay("schrik");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int kusPauze;
    private int schrik;
    private long schilDag = Long.MIN_VALUE;

    public SchuilguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACH_FACE, Direction.DOWN);
        builder.define(STAND, DICHT);
    }

    /** (no goals: it sits in its box) */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    // =================================================================================================================
    // stuck to a block
    // =================================================================================================================

    /** The face of the block it holds on to (DOWN: it sits on a floor). */
    public Direction attachFace() {
        return this.entityData.get(ATTACH_FACE);
    }

    public void setAttachFace(Direction face) {
        this.entityData.set(ATTACH_FACE, face);
    }

    /** {@link #DICHT}, {@link #GLUUR} or {@link #OPEN}. */
    public byte stand() {
        return this.entityData.get(STAND);
    }

    /** Can it hold on to the block on this side of this spot? (A block that is not loaded: yes, wait.) */
    private boolean houdtVast(BlockPos plek, Direction face) {
        BlockPos steun = plek.relative(face);
        if (!level().isLoaded(steun)) {
            return true;
        }
        return level().getBlockState(steun).isFaceSturdy(level(), steun, face.getOpposite());
    }

    @Nullable
    private Direction zoekSteun(BlockPos plek) {
        for (Direction face : Direction.values()) {
            if (houdtVast(plek, face)) {
                return face;
            }
        }
        return null;
    }

    /** The block it held is gone: another one next to it, or one block down until there is a floor. */
    private void zoekNieuweSteun() {
        BlockPos plek = blockPosition();
        Direction face = zoekSteun(plek);
        if (face != null) {
            setAttachFace(face);
            return;
        }
        BlockPos onder = plek.below();
        if (onder.getY() > level().getMinY() && level().getBlockState(onder).getCollisionShape(level(), onder).isEmpty()) {
            setAttachFace(Direction.DOWN);
            setPos(onder.getX() + 0.5, onder.getY(), onder.getZ() + 0.5);
        }
    }

    /** (it sits exactly in a block, like a shulker) */
    @Override
    public void setPos(double x, double y, double z) {
        if (isPassenger()) {
            super.setPos(x, y, z);
        } else {
            super.setPos(Mth.floor(x) + 0.5, Mth.floor(y + 0.5), Mth.floor(z) + 0.5);
        }
    }

    @Override
    public Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    public void setDeltaMovement(Vec3 movement) {
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        if (reason != EntitySpawnReason.STRUCTURE) {         // (a building says which face; never look around during worldgen)
            Direction face = zoekSteun(blockPosition());
            if (face != null) {
                setAttachFace(face);
            }
            setYRot(Mth.floor((getYRot() + 45f) / 90f) * 90f);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        // on a wall or a ceiling the box decides how it sits; on a floor it keeps the way it was put down
        if (attachFace().getAxis() != Direction.Axis.Y || attachFace() == Direction.UP) {
            setYRot(0f);
        }
        this.yBodyRot = this.yBodyRotO = getYRot();
        this.yHeadRot = this.yHeadRotO = getYRot();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (kusPauze > 0) {
            kusPauze--;
        }
        if (schrik > 0) {
            schrik--;
        }
        if (tickCount % 10 == 0 && !isPassenger() && !houdtVast(blockPosition(), attachFace())) {
            zoekNieuweSteun();
        }
        if (tickCount % 5 == 0) {
            byte nu = DICHT;
            if (schrik <= 0) {
                Player p = level().getNearestPlayer(this, GLUUR_AFSTAND);
                if (p != null && !p.isSpectator()) {
                    nu = p.distanceToSqr(this) <= OPEN_AFSTAND * OPEN_AFSTAND ? OPEN : GLUUR;
                }
            }
            zetStand(nu);
        }
    }

    private void zetStand(byte nu) {
        byte was = stand();
        if (was == nu) {
            return;
        }
        this.entityData.set(STAND, nu);
        if (nu == DICHT) {
            playSound(E2MikasFeature.SCHUIL_DICHT.get(), 0.6f, 1.3f);
        } else if (was == DICHT) {
            playSound(E2MikasFeature.SCHUIL_OPEN.get(), 0.6f, 1.3f);
        }
    }

    // =================================================================================================================
    // a kusje, and a knabbel for a doosschil
    // =================================================================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean knabbel = GuheindeReis.isKnabbel(stack);
        if (!knabbel && !(stack.isEmpty() && hand == InteractionHand.MAIN_HAND)) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer sp) {
            if (knabbel) {
                voer(sp, stack);
            } else {
                kus(sp);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private boolean zitDicht(ServerPlayer player) {
        if (schrik <= 0) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.schuilguh_dicht").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /**
     * A kusje for this player: {@link #ZWEEF_TIJD} ticks of levitation I and a soft landing after it.
     *
     * @return true when the kusje was given (false: it sits shut after a fright, or it only just gave one)
     */
    public boolean kus(ServerPlayer player) {
        if (zitDicht(player) || kusPauze > 0 || !(level() instanceof ServerLevel server)) {
            return false;
        }
        kusPauze = KUS_PAUZE;
        zetStand(OPEN);
        triggerAnim("action", "kusje");
        playSound(E2MikasFeature.SCHUIL_KUSJE.get(), 0.9f, 1.0f + random.nextFloat() * 0.2f);
        Vec3 midden = getBoundingBox().getCenter();
        server.sendParticles(ParticleTypes.HEART, (midden.x + player.getX()) / 2, (midden.y + player.getEyeY()) / 2, (midden.z + player.getZ()) / 2,
                4, 0.25, 0.25, 0.25, 0.0);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ZWEEF_TIJD, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ZACHT_TIJD, 0, false, false, true));    // (a guh never lets you fall)
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.kusje").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(player, "einde2_mikas_kusje");
        GuhDex.zie(player, GuhVariant.SCHUILGUH);
        return true;
    }

    /** Did it already give its doosschil today? */
    public boolean schilGegeven() {
        return schilDag == GuhTime.day(level());
    }

    /**
     * Feeds it a kaasknabbel from this stack: it eats one and pushes a doosschil out, at most once per Minecraft day per
     * Schuilguh (a second knabbel on the same day stays in your hand).
     *
     * @return true when the player got a doosschil
     */
    public boolean voer(ServerPlayer player, ItemStack stack) {
        if (!GuheindeReis.isKnabbel(stack) || zitDicht(player) || level().isClientSide()) {
            return false;
        }
        if (schilGegeven()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.schil_morgen").withStyle(ChatFormatting.GRAY));
            return false;
        }
        schilDag = GuhTime.day(level());
        stack.consume(1, player);
        zetStand(OPEN);
        triggerAnim("action", "eet");
        playSound(ModSounds.GUH_EAT.get(), 0.8f, 1.2f);
        playSound(E2MikasFeature.SCHUIL_SCHIL.get(), 0.8f, 1.0f);
        Minigames.give(player, new ItemStack(E2MikasFeature.DOOSSCHIL.get()));
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.schil").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhDex.zie(player, GuhVariant.SCHUILGUH);
        return true;
    }

    // =================================================================================================================
    // a guh: nothing ever happens to it
    // =================================================================================================================

    /** A fright: the lid snaps shut and stays shut for {@link #SCHRIK_TIJD} ticks. */
    public void schrik() {
        if (level().isClientSide()) {
            return;
        }
        boolean was = schrik > 0;
        schrik = SCHRIK_TIJD;
        if (!was) {
            triggerAnim("action", "schrik");
        }
        zetStand(DICHT);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        schrik();
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void knockback(double power, double x, double z) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }

    @Override
    public int getMaxHeadYRot() {
        return 0;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return stand() == DICHT ? null : ModSounds.GUH_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + this.random.nextFloat() * 0.15f;     // (a small voice from inside a box)
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("AttachFace", Direction.LEGACY_ID_CODEC, attachFace());
        output.putLong("SchilDag", schilDag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setAttachFace(input.read("AttachFace", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN));
        schilDag = input.getLongOr("SchilDag", Long.MIN_VALUE);
    }

    /** (for the tests: a new day for this one) */
    public void vergeetSchil() {
        this.schilDag = Long.MIN_VALUE;
        this.kusPauze = 0;
        this.schrik = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> {
            byte stand = stand();
            return state.setAndContinue(stand == OPEN ? OPEN_ANIM : stand == GLUUR ? GLUUR_ANIM : DICHT_ANIM);
        }));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP)
                .triggerableAnim("kusje", KUSJE).triggerableAnim("eet", EET).triggerableAnim("schrik", SCHRIK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
