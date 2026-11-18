package nl.juiced.guhs.feature.guhwinter.hoentjes;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.world.WildeDieren;

/**
 * 1.10.9 Guhwinter: what the two hoentjes share ({@link WaterhoenEntity}, {@link AlpinehoenEntity}). Lief, shy wild birds of
 * the Guhmension with a guh face (big glossy guh eyes, a blush, two little round guh ears):
 * <ul>
 *   <li>passive and shy: they panic when hurt and walk away from a player who comes close without sneaking;</li>
 *   <li>never bred, never tamed; they only drop a feather (loot table);</li>
 *   <li>a wild one is a come-and-go animal ({@link WildeDieren#KOM_EN_GA}, set by {@link HoentjesSpawner}): never saved,
 *       gone again when every player is far away;</li>
 *   <li>a <b>scene</b> one ({@link #startScene}) walks or hops to its goal for a cutscene, can't be hurt, ignores players,
 *       and vanishes in a little puff when it gets there (or when it takes far too long). Never saved either.</li>
 * </ul>
 */
public abstract class Hoentje extends Animal implements GeoEntity {
    /** The entity tag of a scene hoentje (kerstwandeling): no NOWAY chat line for it (the story guhs say it). */
    public static final String SCENE = "guhwinter_hoentje_scene";
    /** A shy hoentje walks away from a player who comes this close (not when the player sneaks). */
    public static final float SCHUW = 5f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** (Scene) where it goes, and how long it may take before it just vanishes. */
    @Nullable
    private Vec3 sceneDoel;
    private int sceneMax;
    private double sceneSnelheid = 1.0;

    protected Hoentje(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** "waterhoen" or "alpinehoen" (the soort of {@link GuhwinterHoentjes#spawnScene}). */
    public abstract String soort();

    protected abstract SoundEvent roep();

    protected abstract SoundEvent piep();

    // --- goals ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SceneGoal());
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, SCHUW, 1.0, 1.35,
                p -> !p.isShiftKeyDown() && !p.isSpectator() && !(p instanceof Player pl && pl.isCreative())));
        addStrollGoal();
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** The idle wandering (priority 5). */
    protected abstract void addStrollGoal();

    // --- the scene ------------------------------------------------------------------------------------------------------------

    /** Makes this a scene hoentje that goes to {@code doel} (speed modifier {@code snelheid}) and then vanishes. */
    public void startScene(Vec3 doel, double snelheid) {
        sceneDoel = doel;
        sceneSnelheid = snelheid;
        sceneMax = 20 * 10 + (int) (position().distanceTo(doel) * 20 / 0.12);   // (generous: ~0.12 blocks a tick is slow walking)
        addTag(SCENE);
        setInvulnerable(true);
    }

    public boolean isScene() {
        return sceneDoel != null || entityTags().contains(SCENE);
    }

    @Nullable
    public Vec3 sceneDoel() {
        return sceneDoel;
    }

    /** A little puff of snow-white clouds, and gone. */
    public void poef() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        }
        discard();
    }

    /** Walks/hops to the scene goal; stuck: a little jump, then a nudge; there (or far too late): poef. */
    class SceneGoal extends Goal {
        private Vec3 laatst = Vec3.ZERO;
        private int tijd, vast;

        SceneGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return sceneDoel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return sceneDoel != null && isAlive();
        }

        @Override
        public void start() {
            tijd = 0;
            vast = 0;
            laatst = position();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 doel = sceneDoel;
            if (doel == null) {
                return;
            }
            tijd++;
            double dx = doel.x - getX(), dz = doel.z - getZ();
            if (dx * dx + dz * dz < 0.7 * 0.7 || tijd > sceneMax) {
                poef();
                return;
            }
            getLookControl().setLookAt(doel.x, doel.y + 0.3, doel.z, 10f, getMaxHeadXRot());
            if (tijd % 10 == 1 && !getNavigation().moveTo(doel.x, doel.y, doel.z, sceneSnelheid)) {
                getMoveControl().setWantedPosition(doel.x, doel.y, doel.z, sceneSnelheid);   // (no path: straight ahead)
            } else if (getNavigation().isDone()) {
                getMoveControl().setWantedPosition(doel.x, doel.y, doel.z, sceneSnelheid);
            }
            if (tijd % 40 == 0) {
                if (position().distanceToSqr(laatst) < 0.3 * 0.3) {
                    vast++;
                    if (vast >= 3) {                               // really stuck: a little nudge towards the goal
                        Vec3 stap = new Vec3(dx, 0, dz).normalize();
                        BlockPos p = BlockPos.containing(getX() + stap.x, getY() + 1, getZ() + stap.z);
                        snapTo(p.getX() + 0.5, grond(level(), p), p.getZ() + 0.5, getYRot(), getXRot());
                        vast = 0;
                    } else {
                        getJumpControl().jump();
                    }
                } else {
                    vast = 0;
                }
                laatst = position();
            }
        }
    }

    /** The y to stand on at this column, near p (searching from p.y + 2 down to p.y - 4; else p.y). */
    static double grond(Level level, BlockPos p) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos q = p.above(dy);
            if (level.getBlockState(q).getCollisionShape(level, q).isEmpty() && !level.getBlockState(q.below()).getCollisionShape(level, q.below()).isEmpty()) {
                return q.getY();
            }
            if (level.getBlockState(q).getCollisionShape(level, q).isEmpty() && level.getFluidState(q.below()).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(q).getFluidState().isEmpty()) {
                return q.getY() - 0.2;                                // (the water surface: the waterhoen swims)
            }
        }
        return p.getY();
    }

    // --- wild, saving, despawning ----------------------------------------------------------------------------------------------

    /** A come-and-go one (and a scene one) is never written to disk. */
    @Override
    public boolean shouldBeSaved() {
        return super.shouldBeSaved() && !isScene() && !WildeDieren.isKomEnGa(this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isScene() && WildeDieren.isKomEnGa(this);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;                                          // (it flutters down)
    }

    // --- sounds --------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return roep();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("actie", "roep");
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return piep();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return piep();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
