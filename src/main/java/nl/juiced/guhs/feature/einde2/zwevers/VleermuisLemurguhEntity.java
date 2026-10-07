package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;

/**
 * The vleermuis-lemurguh (lucht): a cream lemur with huge ears, bat wings and a ringed tail. It hops about on the ground,
 * flies <b>short hops</b> from spot to spot ({@link #VLIEGT}), and where there is a ceiling within reach it flies up and
 * <b>hangs upside down</b> from it by its feet ({@link #HANGT}), wings wrapped around itself, until its nap is over, the
 * ceiling goes, or somebody walks right up to it (sneak, and it sleeps on). It loves fruit (apples, berries, a
 * knabbelkoorvrucht).
 */
public class VleermuisLemurguhEntity extends Elementdier {
    /** Its states: on the ground, on a short flight, hanging upside down. */
    public static final int GROND = 0, VLIEGT = 1, HANGT = 2;
    /** How far a hop goes, and how high above itself it looks for a ceiling. */
    public static final int HOP = 8, PLAFOND = 6;

    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(VleermuisLemurguhEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("vlieg");
    private static final RawAnimation HANG = RawAnimation.begin().thenLoop("hang");

    @Nullable
    private Vec3 doel;
    /** (Flying) the block it will hang under, or null: it lands on the ground. */
    @Nullable
    private BlockPos hangBlok;
    private int vluchtTijd, hangTijd, volgendeHop = 100;

    public VleermuisLemurguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAND, GROND);
    }

    @Override
    public Einde2Ids.Element element() {
        return Einde2Ids.Element.LUCHT;
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.APPLE) || stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(E2BiomesFeature.KNABBELKOORVRUCHT.get());
    }

    @Override
    protected SoundEvent geluid() {
        return E2ZweversFeature.LEMUR.get();
    }

    public int stand() {
        return entityData.get(DATA_STAND);
    }

    @Override
    public boolean isBezig() {
        return stand() != GROND;
    }

    // --- goals: while it flies or hangs nothing else moves it ------------------------------------------------------------------------

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(1, new Goal() {
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
            }

            @Override
            public boolean canUse() {
                return stand() != GROND;
            }

            @Override
            public void start() {
                getNavigation().stop();
            }
        });
    }

    @Override
    protected void zwerfDoelen() {
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    // --- hopping, flying, hanging ---------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        switch (stand()) {
            case VLIEGT -> vliegTik(level);
            case HANGT -> hangTik(level);
            default -> {
                if (--volgendeHop <= 0 && onGround() && !isPassenger() && !isLeashed()) {
                    volgendeHop = 120 + random.nextInt(240);
                    kiesHop(level);
                }
            }
        }
    }

    /** A new hop: to a ceiling it can hang under when there is one, else to a spot on the ground a few blocks on. */
    private void kiesHop(ServerLevel level) {
        BlockPos plafond = random.nextInt(3) != 0 ? zoekPlafond(level) : null;
        if (plafond != null) {
            vliegNaar(new Vec3(plafond.getX() + 0.5, plafond.getY() - (double) getBbHeight(), plafond.getZ() + 0.5), plafond);
            return;
        }
        Vec3 grond = LandRandomPos.getPos(this, HOP, 3);
        if (grond != null && grond.distanceToSqr(position()) > 4) {
            vliegNaar(grond.add(0, 0.1, 0), null);
        }
    }

    /** A block to hang under: solid underneath, two blocks of air below it, within reach, inside its Honk. */
    @Nullable
    private BlockPos zoekPlafond(ServerLevel level) {
        BlockPos hier = blockPosition();
        for (int i = 0; i < 12; i++) {
            BlockPos kolom = hier.offset(random.nextInt(HOP * 2 + 1) - HOP, 0, random.nextInt(HOP * 2 + 1) - HOP);
            for (int dy = 2; dy <= PLAFOND; dy++) {
                BlockPos p = kolom.above(dy);
                if (kanHangen(level, p)) {
                    if (isWithinHome(p)) {
                        return p;
                    }
                    break;
                }
                if (!level.getBlockState(p).isAir()) {
                    break;
                }
            }
        }
        return null;
    }

    /** Can it hang under this block? */
    public static boolean kanHangen(Level level, BlockPos blok) {
        return level.getBlockState(blok).isFaceSturdy(level, blok, Direction.DOWN) && level.getBlockState(blok.below()).isAir()
                && level.getBlockState(blok.below(2)).isAir();
    }

    /** Takes off towards this spot (to hang under hangBlok, or to land when that is null). */
    public void vliegNaar(Vec3 doel, @Nullable BlockPos hangBlok) {
        this.doel = doel;
        this.hangBlok = hangBlok;
        vluchtTijd = 100;
        zetStand(VLIEGT);
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().add(0, 0.25, 0));
        playSound(E2ZweversFeature.LEMUR_VLEUGEL.get(), 0.5f, 1.2f + random.nextFloat() * 0.3f);
    }

    private void vliegTik(ServerLevel level) {
        if (doel == null || --vluchtTijd <= 0) {
            land();
            return;
        }
        Vec3 d = doel.subtract(position());
        if (d.lengthSqr() < 0.3 * 0.3) {
            if (hangBlok != null && kanHangen(level, hangBlok)) {
                hangAan(hangBlok);
            } else {
                land();
            }
            return;
        }
        // a short hop has a little arc: it climbs first, then glides down onto its spot
        double boog = hangBlok == null && d.horizontalDistanceSqr() > 4 ? 0.12 : 0;
        Vec3 wil = d.normalize().scale(Math.min(0.3, d.length() * 0.5)).add(0, boog, 0);
        setDeltaMovement(getDeltaMovement().lerp(wil, 0.35));
        if (d.horizontalDistanceSqr() > 0.01) {
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 20f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        if (tickCount % 8 == 0) {
            playSound(E2ZweversFeature.LEMUR_VLEUGEL.get(), 0.3f, 1.3f + random.nextFloat() * 0.3f);
        }
    }

    /** Hangs upside down under this block, right now (it is put there). */
    public void hangAan(BlockPos blok) {
        hangBlok = blok;
        doel = null;
        hangTijd = 300 + random.nextInt(500);
        zetStand(HANGT);
        setPos(blok.getX() + 0.5, blok.getY() - (double) getBbHeight(), blok.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
    }

    private void hangTik(ServerLevel level) {
        setDeltaMovement(Vec3.ZERO);
        if (hangBlok == null || !level.getBlockState(hangBlok).isFaceSturdy(level, hangBlok, Direction.DOWN)) {
            land();                                               // (its ceiling is gone: it drops, softly)
            return;
        }
        boolean wakker = --hangTijd <= 0;
        if (!wakker && tickCount % 10 == 0) {
            Player dichtbij = level.getNearestPlayer(this, 2.5);
            wakker = dichtbij != null && !dichtbij.isCrouching() && !dichtbij.isSpectator();
        }
        if (wakker) {
            Vec3 grond = LandRandomPos.getPos(this, HOP, 4);
            if (grond != null) {
                vliegNaar(grond.add(0, 0.1, 0), null);
            } else {
                land();
            }
        }
    }

    /** Back to the ground (it falls the last bit; a fall never hurts it). */
    public void land() {
        doel = null;
        hangBlok = null;
        zetStand(GROND);
        volgendeHop = 120 + random.nextInt(240);
    }

    private void zetStand(int stand) {
        entityData.set(DATA_STAND, stand);
        setNoGravity(stand != GROND);
    }

    @Override
    protected void schrik() {
        if (stand() == HANGT) {
            hangTijd = 0;
        } else if (stand() == GROND) {
            volgendeHop = 0;
        }
    }

    @Override
    public boolean isPushable() {
        return stand() != HANGT && super.isPushable();
    }

    // --- save -----------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (stand() == HANGT && hangBlok != null) {
            tag.putLong("HangBlok", hangBlok.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        hangBlok = tag.getLong("HangBlok").map(BlockPos::of).orElse(null);
        if (hangBlok != null) {
            hangTijd = 200;
            zetStand(HANGT);
        } else {
            zetStand(GROND);
        }
    }

    // --- animation ------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Elementdier> state) {
        return switch (stand()) {
            case VLIEGT -> VLIEG;
            case HANGT -> HANG;
            default -> super.beweging(state);
        };
    }
}
