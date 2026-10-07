package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;

/**
 * The das-molguh (aarde): a low brown digger with a badger's striped head and big claws. It is <b>blind</b> (its eyes are
 * shut for good): it never looks at you, but it feels you walk: steps close by make it lift its snout and sniff your way
 * ({@link TrilGoal}; sneak, and it notices nothing). And it <b>digs</b>: on soft ground ({@link #zacht}) it goes nose first
 * into the earth and tunnels around with only a strip of back above the ground and dirt flying ({@link #isOnder}),
 * then pops up again a little further on. It changes no block doing so. It loves kaasknabbels (and a beetroot).
 */
public class DasMolguhEntity extends Elementdier {
    private static final EntityDataAccessor<Boolean> DATA_ONDER = SynchedEntityData.defineId(DasMolguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation GRAAF = RawAnimation.begin().thenPlay("graaf_in").thenLoop("onder");
    private static final EntityDimensions MAAT_ONDER = EntityDimensions.scalable(0.95f, 0.3f).withEyeHeight(0.2f);

    private int graafTijd, volgendeGraaf = 200;
    private long volgendeSnuf;

    public DasMolguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ONDER, false);
    }

    @Override
    public Einde2Ids.Element element() {
        return Einde2Ids.Element.AARDE;
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(Items.BEETROOT);
    }

    @Override
    protected SoundEvent geluid() {
        return E2ZweversFeature.DAS.get();
    }

    // --- digging ---------------------------------------------------------------------------------------------------------------------

    /** Dug in (tunnelling just under the surface)? */
    public boolean isOnder() {
        return entityData.get(DATA_ONDER);
    }

    @Override
    public boolean isBezig() {
        return isOnder();
    }

    /** Ground it can dig in: the ground of the outer islands, and whatever a shovel digs (earth, sand, gravel, snow). */
    public static boolean zacht(BlockState state) {
        return state.is(Einde2Ids.EILANDGROND) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    /** Does it stand on ground it can dig in? */
    public boolean opZacht() {
        return onGround() && zacht(level().getBlockState(getBlockPosBelowThatAffectsMyMovement()));
    }

    /** Digs in for this many ticks (when the ground is soft). */
    public boolean graafIn(int ticks) {
        if (isOnder() || !opZacht()) {
            return false;
        }
        graafTijd = ticks;
        zetOnder(true);
        playSound(E2ZweversFeature.DAS_GRAAF.get(), 0.8f, 0.9f + random.nextFloat() * 0.2f);
        return true;
    }

    /** Pops up again, with a sniff. */
    public void komBoven() {
        if (!isOnder()) {
            return;
        }
        zetOnder(false);
        graafTijd = 0;
        volgendeGraaf = 300 + random.nextInt(500);
        getNavigation().stop();
        triggerAnim("actie", "snuffel");
        playSound(geluid(), 0.8f, 1.2f);
    }

    private void zetOnder(boolean onder) {
        entityData.set(DATA_ONDER, onder);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ONDER.equals(accessor)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return isOnder() ? MAAT_ONDER : super.getDefaultDimensions(pose);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!isOnder()) {
            volgendeGraaf--;
            return;
        }
        if (--graafTijd <= 0 || !opZacht()) {
            komBoven();                                           // (time is up, or it tunnelled onto hard ground)
            return;
        }
        if (tickCount % 3 == 0) {
            BlockPos onder = getBlockPosBelowThatAffectsMyMovement();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(onder)), getX(), getY() + 0.15, getZ(),
                    4, 0.3, 0.05, 0.3, 0.05);
        }
        if (tickCount % 25 == 0) {
            playSound(E2ZweversFeature.DAS_GRAAF.get(), 0.5f, 0.8f + random.nextFloat() * 0.3f);
        }
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(2, new GraafGoal());
        goalSelector.addGoal(3, new TrilGoal());
    }

    @Override
    protected void zwerfDoelen() {
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        // (no LookAtPlayerGoal: it is blind)
    }

    /** Now and then, on soft ground: it digs in and tunnels from spot to spot until its time is up. */
    private class GraafGoal extends Goal {
        GraafGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isOnder() || volgendeGraaf <= 0 && !isPassenger() && !isLeashed() && opZacht();
        }

        @Override
        public boolean canContinueToUse() {
            return isOnder();
        }

        @Override
        public void start() {
            if (!isOnder() && !graafIn(100 + random.nextInt(140))) {
                volgendeGraaf = 100;
            }
        }

        @Override
        public void tick() {
            if (isOnder() && getNavigation().isDone() && graafTijd > 20 && random.nextInt(10) == 0) {
                Vec3 verder = LandRandomPos.getPos(DasMolguhEntity.this, 6, 1);
                if (verder != null && zacht(level().getBlockState(BlockPos.containing(verder).below()))) {
                    getNavigation().moveTo(verder.x, verder.y, verder.z, 0.8);
                }
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** Blind, but it feels steps: a player walking close by (not sneaking) makes it turn its snout that way and sniff. */
    private class TrilGoal extends Goal {
        @Nullable
        private Player wie;
        private int ticks;

        TrilGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isOnder() || level().getGameTime() < volgendeSnuf) {
                return false;
            }
            wie = voeltStappen();
            return wie != null;
        }

        @Override
        public boolean canContinueToUse() {
            return wie != null && wie.isAlive() && ticks < 40 && !isOnder();
        }

        @Override
        public void start() {
            ticks = 0;
            volgendeSnuf = level().getGameTime() + 120 + random.nextInt(120);
            triggerAnim("actie", "snuffel");
            playSound(geluid(), 0.6f, 1.3f);
        }

        @Override
        public void tick() {
            ticks++;
            if (wie != null) {
                getLookControl().setLookAt(wie.getX(), wie.getY(), wie.getZ());   // (its feet: that is where the steps are)
            }
        }

        @Override
        public void stop() {
            wie = null;
        }
    }

    /** The nearest player within 8 blocks whose steps it can feel: on the ground, moving, not sneaking. */
    @Nullable
    public Player voeltStappen() {
        Player best = null;
        for (Player p : level().players()) {
            double d = p.distanceToSqr(this);
            if (d < 64 && stapt(p) && (best == null || d < best.distanceToSqr(this))) {
                best = p;
            }
        }
        return best;
    }

    /** Does this player make steps it can feel? */
    public static boolean stapt(Player p) {
        return !p.isSpectator() && p.onGround() && !p.isCrouching() && p.getKnownMovement().horizontalDistanceSqr() > 0.003;
    }

    @Override
    protected void schrik() {
        if (!isOnder() && !graafIn(60)) {
            triggerAnim("actie", "snuffel");
        }
    }

    @Override
    public boolean isPushable() {
        return !isOnder() && super.isPushable();
    }

    // --- save ------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GraafTijd", isOnder() ? graafTijd : 0);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        graafTijd = tag.getIntOr("GraafTijd", 0);
        zetOnder(graafTijd > 0);
    }

    // --- animation -------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Elementdier> state) {
        return isOnder() ? GRAAF : super.beweging(state);
    }

    @Override
    protected void extraActies(AnimationController<Elementdier> actie) {
        actie.triggerableAnim("snuffel", RawAnimation.begin().thenPlay("snuffel"));
    }
}
