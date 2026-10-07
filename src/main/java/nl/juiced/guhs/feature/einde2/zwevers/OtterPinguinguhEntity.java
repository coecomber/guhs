package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;

/**
 * The otter-pinguïnguh (water): a slate-blue penguin body with a white otter face and four flippers. It waddles, and on
 * ice and snow it drops onto its belly and <b>slides</b> ({@link #isGlijdend}: faster than it walks). It loves fish.
 * <p>
 * <b>A sled when told so.</b> With {@link #laatRijden laatRijden(true)} (NBT {@code Slee: 1b}, what a building writes for
 * the ones at the top of a sled run) a player can sit on it (empty hand). It then lies on its belly and slides where the
 * rider looks: it keeps its speed on ice and snow, gains speed downhill and loses it on bare ground; forward pushes off,
 * back brakes. The rider's own client steers ({@link #tickRidden}, {@link #sleeReis}), like a horse.
 * <p>
 * hook fase B: the sled run of the Noordelijke Waterstam (markers slee_start, slee_baan, slee_finish) times a ride
 * through {@link #OPGESTAPT} and {@link #AFGESTAPT}, and may read {@link #vaart()}.
 */
public class OtterPinguinguhEntity extends Elementdier {
    /** The fastest it slides with a rider (blocks per tick). */
    public static final double MAX_VAART = 0.7;
    /** Called on the server when a player gets on a sled otter / gets off it again. */
    public static final List<BiConsumer<OtterPinguinguhEntity, ServerPlayer>> OPGESTAPT = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<OtterPinguinguhEntity, ServerPlayer>> AFGESTAPT = new CopyOnWriteArrayList<>();

    private static final EntityDataAccessor<Boolean> DATA_SLEE = SynchedEntityData.defineId(OtterPinguinguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GLIJDT = SynchedEntityData.defineId(OtterPinguinguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier GLIJ_VAART = Guhs.id("einde2_zwevers_glij");
    private static final RawAnimation GLIJ = RawAnimation.begin().thenLoop("glij");

    /** (The side that moves it while ridden) its speed along where it points. */
    private double vaart;
    private int glijRust;
    private double vorigeX, vorigeZ;

    public OtterPinguinguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLEE, false);
        builder.define(DATA_GLIJDT, false);
    }

    @Override
    public Einde2Ids.Element element() {
        return Einde2Ids.Element.WATER;
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH);
    }

    @Override
    protected SoundEvent geluid() {
        return E2ZweversFeature.OTTER.get();
    }

    @Override
    protected void zwerfDoelen() {
        goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    // --- sliding by itself -----------------------------------------------------------------------------------------------------------

    /** On its belly right now (sliding over ice or snow, or carrying a rider)? */
    public boolean isGlijdend() {
        return entityData.get(DATA_GLIJDT) || isVehicle() && isSlee();
    }

    /** Is what it stands on (or in: a layer of snow) ice or snow? */
    public boolean opGlad() {
        BlockState in = level().getBlockState(blockPosition());
        BlockPos onder = getBlockPosBelowThatAffectsMyMovement();
        return glad(in) || glad(level().getBlockState(onder));
    }

    /** Ice, snow, and anything as slippery as ice. */
    public static boolean glad(BlockState state) {
        return state.is(BlockTags.ICE) || state.is(BlockTags.SNOW) || state.getBlock().getFriction() > 0.9f;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        boolean glijdt = !isVehicle() && onGround() && opGlad() && getNavigation().isInProgress();
        if (glijdt) {
            glijRust = 12;
        } else if (glijRust > 0) {
            glijRust--;
        }
        zetGlijdt(glijRust > 0 && !isVehicle());
        if (tickCount % 12 == 0) {
            double dx = getX() - vorigeX, dz = getZ() - vorigeZ;
            if (isGlijdend() && dx * dx + dz * dz > 0.6 * 0.6) {
                playSound(E2ZweversFeature.OTTER_GLIJ.get(), 0.5f, 0.9f + random.nextFloat() * 0.3f);
                level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 4, 0.25, 0.05, 0.25, 0.01);
            }
            vorigeX = getX();
            vorigeZ = getZ();
        }
    }

    private void zetGlijdt(boolean glijdt) {
        if (entityData.get(DATA_GLIJDT) == glijdt) {
            return;
        }
        entityData.set(DATA_GLIJDT, glijdt);
        AttributeInstance snelheid = getAttribute(Attributes.MOVEMENT_SPEED);
        if (snelheid != null) {
            snelheid.removeModifier(GLIJ_VAART);
            if (glijdt) {
                snelheid.addOrUpdateTransientModifier(new AttributeModifier(GLIJ_VAART, 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    // --- the sled --------------------------------------------------------------------------------------------------------------------

    public boolean isSlee() {
        return entityData.get(DATA_SLEE);
    }

    /** Makes it a sled a player can sit on (or an ordinary otter-pinguïnguh again: whoever sits on it gets off). */
    public void laatRijden(boolean slee) {
        entityData.set(DATA_SLEE, slee);
        if (!slee) {
            ejectPassengers();
        }
    }

    /** Its sliding speed with a rider, in blocks per tick (on the side that moves it; elsewhere how fast it last moved). */
    public double vaart() {
        return isLocalInstanceAuthoritative() ? vaart : getKnownMovement().horizontalDistance();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isSlee() && !isVehicle() && !player.isSecondaryUseActive() && hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide()) {
                getNavigation().stop();
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isSlee() && getFirstPassenger() instanceof Player rider ? rider : super.getControllingPassenger();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        vaart = 0;
        if (passenger instanceof ServerPlayer p) {
            OPGESTAPT.forEach(h -> h.accept(this, p));
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        vaart = 0;
        if (passenger instanceof ServerPlayer p) {
            AFGESTAPT.forEach(h -> h.accept(this, p));
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, 0.5 * scale, 0);
    }

    /** It turns towards where its rider looks (a sled doesn't turn on the spot), and its speed follows the ground. */
    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setYRot(Mth.approachDegrees(getYRot(), rider.getYRot(), 4.5f));
        setXRot(0);
        yRotO = yBodyRot = yHeadRot = getYRot();
        vaart = volgendeVaart(vaart, opGlad(), rider.zza);
    }

    /**
     * The sled's speed a tick later on level ground: on ice and snow it nearly keeps it, on bare ground it drags;
     * forward pushes off, back brakes.
     */
    public static double volgendeVaart(double vaart, boolean glad, float duw) {
        double v = vaart * (glad ? 0.992 : 0.9);
        if (duw > 0) {
            v += 0.012;
        } else if (duw < 0) {
            v -= 0.03;
        }
        return Mth.clamp(v, 0.0, MAX_VAART);
    }

    @Override
    public void travel(Vec3 input) {
        if (isSlee() && getControllingPassenger() instanceof Player) {
            sleeReis();
        } else {
            super.travel(input);
        }
    }

    /** One tick of sliding: straight ahead at its speed; a drop makes it faster on ice and snow, a step up or a bump slower. */
    private void sleeReis() {
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        double y0 = getY();
        boolean glad = opGlad();
        setDeltaMovement(f.x * vaart, getDeltaMovement().y, f.z * vaart);
        move(MoverType.SELF, getDeltaMovement());
        double dy = getY() - y0;
        if (dy < 0 && glad) {
            vaart = Math.min(MAX_VAART, vaart - dy * 0.1);
        } else if (dy > 0) {
            vaart = Math.max(0, vaart - dy * 0.12);
        }
        if (horizontalCollision) {
            vaart *= 0.5;
        }
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x, isNoGravity() ? 0 : (v.y - getGravity()) * 0.98, v.z);
        calculateEntityAnimation(false);
    }

    // --- save ------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Slee", isSlee());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_SLEE, tag.getBooleanOr("Slee", false));
    }

    // --- animation -------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Elementdier> state) {
        return isGlijdend() ? GLIJ : super.beweging(state);
    }
}
