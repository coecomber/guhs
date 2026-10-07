package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;

/**
 * The schildpad-eendguh (vuur: it lives on the palace pond of the Vuurnatie): a yellow duck in a green turtle shell. It
 * <b>swims</b> (it bobs at the surface and paddles, and it goes looking for water when it is on dry land), it
 * <b>eats bread</b> and follows whoever holds some ({@link #isVoer}; every bite a happy kwak), and when it gets a fright
 * (a tap, a Mika close by) it pulls head and feet into its <b>shell</b> for a moment ({@link #isInSchild}).
 */
public class SchildpadEendguhEntity extends Elementdier {
    /** How long it stays in its shell after a fright (ticks). */
    public static final int SCHILD_TICKS = 60;

    private static final EntityDataAccessor<Boolean> DATA_SCHILD = SynchedEntityData.defineId(SchildpadEendguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private static final RawAnimation SCHELP = RawAnimation.begin().thenLoop("schelp");

    private int schildTijd;
    /** How many pieces of bread it has had (it keeps count; a later phase may ask). */
    private int broodjes;

    public SchildpadEendguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCHILD, false);
    }

    @Override
    public Einde2Ids.Element element() {
        return Einde2Ids.Element.VUUR;
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.BREAD);
    }

    @Override
    protected SoundEvent geluid() {
        return E2ZweversFeature.EENDGUH.get();
    }

    /** How many pieces of bread it was fed. */
    public int broodjes() {
        return broodjes;
    }

    @Override
    protected void gegeten(ServerPlayer player, ItemStack snack) {
        broodjes++;
        if (isInSchild()) {
            uitSchild();                                          // (bread: it peeks out at once)
        }
    }

    // --- its shell -------------------------------------------------------------------------------------------------------------------

    public boolean isInSchild() {
        return entityData.get(DATA_SCHILD);
    }

    @Override
    public boolean isBezig() {
        return isInSchild();
    }

    /** Head and feet in, for {@link #SCHILD_TICKS} more ticks. */
    public void inSchild() {
        if (!isInSchild()) {
            entityData.set(DATA_SCHILD, true);
            getNavigation().stop();
            playSound(E2ZweversFeature.EENDGUH_SCHELP.get(), 0.6f, 1.2f);
        }
        schildTijd = SCHILD_TICKS;
    }

    private void uitSchild() {
        entityData.set(DATA_SCHILD, false);
        schildTijd = 0;
        playSound(geluid(), 0.7f, 1.3f);
    }

    @Override
    protected void schrik() {
        inSchild();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % 10 == 0 && mikaDichtbij()) {
            inSchild();
        }
        if (schildTijd > 0 && --schildTijd == 0 && isInSchild()) {
            if (mikaDichtbij()) {
                schildTijd = 20;                                  // (still scary out there)
            } else {
                uitSchild();
            }
        }
    }

    private boolean mikaDichtbij() {
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(4, 2, 4))) {
            if (BasisKlus.isMika(e)) {
                return true;
            }
        }
        return false;
    }

    // --- swimming --------------------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (isInWater() && getFluidHeight(FluidTags.WATER) > 0.25) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, Math.min(0.08, v.y + 0.05), v.z);   // (it floats: a duck bobs on the water)
        }
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(1, new Goal() {                           // in its shell it doesn't walk, look or jump
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
            }

            @Override
            public boolean canUse() {
                return isInSchild();
            }

            @Override
            public void start() {
                getNavigation().stop();
            }
        });
        goalSelector.addGoal(3, new NaarWaterGoal());
    }

    @Override
    protected void zwerfDoelen() {
        goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    /** On dry land for a while: it waddles to water close by, when there is some (within its Honk). */
    private class NaarWaterGoal extends Goal {
        @Nullable
        private BlockPos water;
        private int droog, ticks;

        NaarWaterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isInWater()) {
                droog = 0;
                return false;
            }
            if (++droog < 100 || random.nextInt(20) != 0) {
                return false;
            }
            droog = 0;
            water = zoekWater();
            return water != null;
        }

        @Override
        public boolean canContinueToUse() {
            return water != null && !isInWater() && ticks < 300 && !getNavigation().isDone();
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public void stop() {
            water = null;
        }
    }

    /** Open water within 8 blocks (a water block with air above it), inside its Honk. Or null. */
    @Nullable
    public BlockPos zoekWater() {
        BlockPos hier = blockPosition();
        for (int i = 0; i < 24; i++) {
            BlockPos p = hier.offset(random.nextInt(17) - 8, random.nextInt(5) - 3, random.nextInt(17) - 8);
            if (level().getFluidState(p).is(FluidTags.WATER) && level().getBlockState(p.above()).isAir() && isWithinHome(p)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && isInWater() && getDeltaMovement().horizontalDistanceSqr() > 0.001 && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 0, 0.02, 0);
        }
    }

    // --- save ------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Broodjes", broodjes);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        broodjes = tag.getIntOr("Broodjes", 0);
    }

    // --- animation -------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Elementdier> state) {
        if (isInSchild()) {
            return SCHELP;
        }
        return isInWater() ? ZWEM : super.beweging(state);
    }
}
