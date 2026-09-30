package nl.juiced.guhs.feature.landdiertjes;

import java.util.EnumSet;
import java.util.List;

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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The pluisegeltje: a little hedgehog with soft, fluffy pink-cream spikes (they never prick: "pluisstekeltjes"), a guh face
 * with big glossy eyes and a pointy snoetje. Lives in the Vadswoud.
 * <ul>
 *   <li>When it gets a fright it rolls up into a fluffy ball ({@link #isOpgerold()}): a wild one when a player walks up to it
 *       without sneaking (or runs by), any one when it is hurt or a Mika comes close. Rolled up nothing can hurt it; after a
 *       few quiet seconds it peeks out again. Sneak up to it to tame it.</li>
 *   <li>Tame it with sweet berries (or glow berries), 1 in {@link #TAME_CHANCE}. It walks right through berry bushes.</li>
 *   <li>Its big button "Rol eens!": it rolls up and rolls a little circle around you, then pops open with a happy snuffle.</li>
 * </ul>
 */
public class PluisegeltjeEntity extends Landdiertje {
    public static final int TAME_CHANCE = 3;
    /** 0 = open, 1 = rolled up (still), 2 = rolling (the big button). */
    private static final EntityDataAccessor<Integer> DATA_ROL = SynchedEntityData.defineId(PluisegeltjeEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation OPGEROLD = RawAnimation.begin().thenLoop("opgerold");
    private static final RawAnimation ROLT = RawAnimation.begin().thenLoop("rollen");
    /** How long it stays rolled up after the last fright, and the length of a rolling circle (ticks). */
    public static final int ROL_TICKS = 70, RONDJE_TICKS = 80;

    private int rolTijd;
    @Nullable
    private Vec3 rondjeMidden;
    private float rondjeHoek;

    public PluisegeltjeEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROL, 0);
    }

    @Override
    public String soort() {
        return "pluisegeltje";
    }

    @Override
    public Item oppakItem() {
        return LanddiertjesFeature.PLUISEGELTJE_ITEM.get();
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES);
    }

    @Override
    public int temKans() {
        return TAME_CHANCE;
    }

    @Override
    protected SoundEvent geluid() {
        return LanddiertjesFeature.EGELTJE_SNUF.get();
    }

    @Override
    protected double volgSnelheid() {
        return 1.3;
    }

    public boolean isOpgerold() {
        return entityData.get(DATA_ROL) != 0;
    }

    public boolean isRollend() {
        return entityData.get(DATA_ROL) == 2;
    }

    @Override
    public boolean isBezig() {
        return isOpgerold();
    }

    @Nullable
    @Override
    protected Component nietNu() {
        return isOpgerold() ? Component.translatable("gui.guhs.landdiertjes.egeltje_opgerold") : null;
    }

    /** Rolls up (a fright): for {@link #ROL_TICKS} more ticks. */
    public void rolOp() {
        if (entityData.get(DATA_ROL) == 0) {
            entityData.set(DATA_ROL, 1);
            playSound(LanddiertjesFeature.EGELTJE_ROL.get(), 0.6f, 1.2f);
            getNavigation().stop();
        }
        rolTijd = Math.max(rolTijd, ROL_TICKS);
    }

    /** "Rol eens!": a little rolling circle around this spot (the owner), then open again. */
    public void rolRondje(Vec3 midden) {
        entityData.set(DATA_ROL, 2);
        rondjeMidden = midden;
        rondjeHoek = (float) Math.atan2(getZ() - midden.z, getX() - midden.x);
        rolTijd = RONDJE_TICKS;
        getNavigation().stop();
        playSound(LanddiertjesFeature.EGELTJE_ROL.get(), 0.7f, 1.0f);
    }

    private void rolOpen() {
        entityData.set(DATA_ROL, 0);
        rondjeMidden = null;
        rolTijd = 0;
        playSound(geluid(), 0.7f, 1.3f);
        triggerAnim("actie", "blij");
    }

    // --- goals: rolled up = still ----------------------------------------------------------------------------------------------

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(0, new StilGoal());
    }

    /** While rolled up it doesn't walk, look or jump (priority 0: nothing else moves it). */
    private class StilGoal extends Goal {
        StilGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isOpgerold();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if ((tickCount + getId()) % 5 == 0 && entityData.get(DATA_ROL) != 2 && schrikt()) {
            rolOp();
        }
        if (isRollend() && rondjeMidden != null) {
            rondjeHoek += (float) (Math.PI * 2 / RONDJE_TICKS);
            double r = 1.8;
            Vec3 doel = rondjeMidden.add(Math.cos(rondjeHoek) * r, 0, Math.sin(rondjeHoek) * r);
            Vec3 d = doel.subtract(position());
            setDeltaMovement(Mth.clamp(d.x, -0.25, 0.25), getDeltaMovement().y, Mth.clamp(d.z, -0.25, 0.25));
            if (tickCount % 6 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.2, getZ(), 1, 0.1, 0.05, 0.1, 0);
            }
        }
        if (rolTijd > 0 && --rolTijd == 0 && isOpgerold()) {
            if (!isRollend() && schrikt()) {
                rolTijd = 20;                                     // (still scary out there: it waits a bit longer)
            } else {
                rolOpen();
            }
        }
    }

    /** Is something scary close by? A Mika; for a wild one also a player who walks up without sneaking. */
    private boolean schrikt() {
        List<Entity> dichtbij = level().getEntities(this, getBoundingBox().inflate(4, 2, 4));
        for (Entity e : dichtbij) {
            if (BasisKlus.isMika(e)) {
                return true;
            }
            if (!isTame() && e instanceof Player p && !p.isSpectator() && !p.isCreative() && !p.isCrouching()) {
                double d = p.distanceToSqr(this);
                double v = p.getDeltaMovement().horizontalDistanceSqr();
                if (d < 2.5 * 2.5 && v > 0.0004 || p.isSprinting() && d < 16) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (isOpgerold() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            playSound(LanddiertjesFeature.EGELTJE_ROL.get(), 0.5f, 1.6f);
            return false;                                         // a fluffy ball: nothing gets through
        }
        boolean r = super.hurtServer(level, source, amount);
        if (r && isAlive() && !level().isClientSide()) {
            rolOp();
        }
        return r;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speed) {
        if (!(state.getBlock() instanceof SweetBerryBushBlock)) {
            super.makeStuckInBlock(state, speed);                 // (berry bushes are its home: no slowing down)
        }
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel level, DamageSource source) {
        return source.is(net.minecraft.world.damagesource.DamageTypes.SWEET_BERRY_BUSH) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean isPushable() {
        return !isOpgerold() && super.isPushable();
    }

    // --- the big button ---------------------------------------------------------------------------------------------------------

    @Override
    public void speciaal(ServerPlayer player) {
        rolRondje(player.position());
        player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.egeltje_rondje", getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        GidsFeature.grant(player, "diertjes/landdiertjes_rondje");
    }

    // --- save ---------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Opgerold", isRollend() ? 0 : entityData.get(DATA_ROL));
        tag.putInt("RolTijd", rolTijd);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ROL, Mth.clamp(tag.getIntOr("Opgerold", 0), 0, 1));
        rolTijd = tag.getIntOr("RolTijd", 0);
        if (isOpgerold() && rolTijd <= 0) {
            rolTijd = 20;
        }
    }

    // --- animation ----------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Landdiertje> state) {
        int rol = entityData.get(DATA_ROL);
        if (rol == 2) {
            return ROLT;
        }
        if (rol == 1) {
            return OPGEROLD;
        }
        return super.beweging(state);
    }

    /** (Tests) where it sits. */
    BlockPos plek() {
        return blockPosition();
    }
}
