package nl.juiced.guhs.feature.landdiertjes;

import java.util.EnumSet;

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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;

/**
 * Sjokkel (no guh!): a real Sjokkel in Minecraft style: a round red shell full of cheese holes (like gatenkaas), a yellow
 * little head with bead eyes and yellow feet that poke out of the holes. Very, very slow; very, very lief.
 * <ul>
 *   <li>It pulls into its shell ({@link #isInSchelp()}) when a player comes close without sneaking (a wild one), when it is
 *       hurt, or when a Mika comes by; in its shell nothing hurts it ("tink!"). After a quiet while it peeks out again.</li>
 *   <li>It lives around the rocks and coast of the kloon-eiland (kept there by the {@link ShucklePlekjeBlock shuckle-plekjes})
 *       and, rarely, in the Gatenkaasgrotten.</li>
 *   <li>Tame it with sweet berries (or glow berries / a kaasknabbel), 1 in {@link #TAME_CHANCE}. It follows you, slowly.</li>
 *   <li>Bessensapje: a tamed Sjokkel keeps the berries you feed it (up to {@link #BESSEN_MAX}); now and then, with
 *       {@link #BESSEN_PER_SAPJE} berries, it pulls into its shell and... a {@code landdiertjes_bessensapje} pops out. Its
 *       big button "Bessensapje!" does it right now.</li>
 *   <li>In a Guhhuisje its chore is {@link PolijstenKlus stenen polijsten}.</li>
 * </ul>
 */
public class ShuckleEntity extends Landdiertje {
    public static final int TAME_CHANCE = 4;
    public static final int BESSEN_MAX = 12, BESSEN_PER_SAPJE = 3;
    /** How long it stays in its shell after the last fright; how long the juice takes; the time between two juices by itself. */
    public static final int SCHELP_TICKS = 80, SAPJE_TICKS = 60, SAPJE_WACHT = 2400, RUST = 30;
    private static final EntityDataAccessor<Boolean> DATA_SCHELP = SynchedEntityData.defineId(ShuckleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IN_SCHELP = RawAnimation.begin().thenLoop("schelp");

    private int schelpTijd, sapjeTijd, bessen;
    private long volgendSapje, rustTot;
    /** A wild one from a shuckle-plekje stays around it (saved; forgotten once it is tamed). */
    @Nullable
    private BlockPos plekje;

    public ShuckleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.ARMOR, 10.0).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCHELP, false);
    }

    @Override
    public String soort() {
        return "shuckle";
    }

    @Override
    public Item oppakItem() {
        return LanddiertjesFeature.SHUCKLE_ITEM.get();
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public int temKans() {
        return TAME_CHANCE;
    }

    @Override
    protected SoundEvent geluid() {
        return LanddiertjesFeature.SHUCKLE_GELUID.get();
    }

    @Override
    protected double volgSnelheid() {
        return 1.6;
    }

    public boolean isInSchelp() {
        return entityData.get(DATA_SCHELP);
    }

    public int bessen() {
        return bessen;
    }

    @Override
    public boolean isBezig() {
        return isInSchelp();
    }

    @Nullable
    @Override
    protected Component nietNu() {
        return isInSchelp() ? Component.translatable("gui.guhs.landdiertjes.shuckle_in_schelp") : null;
    }

    /** Into its shell (for at least this many ticks). */
    public void inSchelp(int ticks) {
        if (!isInSchelp()) {
            entityData.set(DATA_SCHELP, true);
            playSound(LanddiertjesFeature.SHUCKLE_DICHT.get(), 0.6f, 1.2f);
            getNavigation().stop();
        }
        schelpTijd = Math.max(schelpTijd, ticks);
    }

    private void uitSchelp() {
        entityData.set(DATA_SCHELP, false);
        playSound(LanddiertjesFeature.SHUCKLE_OPEN.get(), 0.6f, 1.2f);
    }

    // --- berries -> juice ------------------------------------------------------------------------------------------------------------

    @Override
    protected void gegeten(@Nullable ServerPlayer player, ItemStack snack) {
        if (!snack.is(ModItems.KAAS_KNABBELS.get())) {
            bessen = Math.min(BESSEN_MAX, bessen + 1);
            if (player != null && bessen >= BESSEN_PER_SAPJE) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.shuckle_bessen", getDisplayName(), bessen)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Starts a juice now (it pulls into its shell); false when it doesn't have the berries. */
    public boolean maakSapje() {
        if (bessen < BESSEN_PER_SAPJE || sapjeTijd > 0) {
            return false;
        }
        bessen -= BESSEN_PER_SAPJE;
        sapjeTijd = SAPJE_TICKS;
        inSchelp(SAPJE_TICKS);
        return true;
    }

    private void sapjeKlaar() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ItemStack sap = new ItemStack(LanddiertjesFeature.BESSENSAPJE.get());
        ItemEntity item = new ItemEntity(sl, getX(), getY() + 0.5, getZ(), sap);
        Vec3 kijk = Vec3.directionFromRotation(0, getYRot());
        item.setDeltaMovement(kijk.x * 0.12, 0.2, kijk.z * 0.12);
        item.setPickUpDelay(10);
        sl.addFreshEntity(item);
        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 3, 0.2, 0.2, 0.2, 0);
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 12, 0.2, 0.1, 0.2, 0.05);
        playSound(LanddiertjesFeature.SHUCKLE_GELUID.get(), 0.8f, 1.5f);
        triggerAnim("actie", "blij");
    }

    // --- every tick ----------------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (plekje != null && !isTame() && !hasRestriction()) {
            restrictTo(plekje, 12);
        }
        if ((tickCount + getId()) % 5 == 0 && sapjeTijd <= 0 && schrikt()) {
            inSchelp(SCHELP_TICKS);
        }
        if (sapjeTijd > 0) {
            if (sapjeTijd % 10 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.55, getZ(), 2, 0.15, 0.05, 0.15, 0);
            }
            if (--sapjeTijd == 0) {
                schelpTijd = 1;
                sapjeKlaar();
            }
        }
        if (schelpTijd > 0 && --schelpTijd == 0 && isInSchelp()) {
            if (sapjeTijd <= 0 && schrikt()) {
                schelpTijd = 20;
            } else {
                uitSchelp();
            }
        }
        if (isTame() && sapjeTijd <= 0 && !isInSchelp() && bessen >= BESSEN_PER_SAPJE && level().getGameTime() >= volgendSapje) {
            volgendSapje = level().getGameTime() + SAPJE_WACHT + random.nextInt(SAPJE_WACHT / 2);
            maakSapje();
        }
    }

    /** Something scary close by? A Mika; for a wild one also a player who doesn't sneak; for a tame one a stranger who runs. */
    private boolean schrikt() {
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(4, 2, 4))) {
            if (BasisKlus.isMika(e)) {
                return true;
            }
            if (e instanceof Player p && !p.isSpectator() && !p.isCreative() && !p.isCrouching()) {
                double d = p.distanceToSqr(this);
                if (!isTame() && d < 3.5 * 3.5 || isTame() && !isOwnedBy(p) && p.isSprinting() && d < 2.5 * 2.5) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInSchelp() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            playSound(LanddiertjesFeature.SHUCKLE_TIK.get(), 0.6f, 1.4f);
            return false;
        }
        boolean r = super.hurt(source, amount);
        if (r && isAlive() && !level().isClientSide()) {
            inSchelp(SCHELP_TICKS);
        }
        return r;
    }

    @Override
    public boolean isPushable() {
        return !isInSchelp() && super.isPushable();
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(0, new StilGoal());
    }

    /** In its shell it doesn't walk, look or jump. */
    private class StilGoal extends Goal {
        StilGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isInSchelp();
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

    // --- the big button --------------------------------------------------------------------------------------------------------------------

    @Override
    public int rustSeconden() {
        return (int) Math.max(0, (rustTot - level().getGameTime() + 19) / 20);
    }

    @Override
    public void speciaal(ServerPlayer player) {
        if (rustSeconden() > 0) {
            return;
        }
        if (maakSapje()) {
            rustTot = level().getGameTime() + RUST * 20L;
            player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.shuckle_sapje", getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.shuckle_geen_bessen", getDisplayName(), BESSEN_PER_SAPJE)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    // --- save --------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Bessen", bessen);
        tag.putBoolean("InSchelp", isInSchelp() && sapjeTijd <= 0);
        tag.putInt("SchelpTijd", schelpTijd);
        if (plekje != null) {
            tag.putLong("Plekje", plekje.asLong());
        }
        if (sapjeTijd > 0) {
            tag.putInt("Bessen", bessen + BESSEN_PER_SAPJE);     // (saved half-way through a juice: the berries come back)
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bessen = Mth.clamp(tag.getIntOr("Bessen", 0), 0, BESSEN_MAX + BESSEN_PER_SAPJE);
        entityData.set(DATA_SCHELP, tag.getBooleanOr("InSchelp", false));
        schelpTijd = tag.getIntOr("SchelpTijd", 0);
        plekje = tag.contains("Plekje") ? BlockPos.of(tag.getLongOr("Plekje", 0L)) : null;
        if (isInSchelp() && schelpTijd <= 0) {
            schelpTijd = 20;
        }
    }

    // --- animation -------------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(AnimationTest<Landdiertje> state) {
        return isInSchelp() ? IN_SCHELP : super.beweging(state);
    }

    @Override
    protected void extraActies(com.geckolib.animation.AnimationController<Landdiertje> actie) {
        actie.triggerableAnim("poets", RawAnimation.begin().thenPlay("poets"));
    }

    /** (Tests) out of its shell right now. */
    void setUitSchelpVoorTest() {
        entityData.set(DATA_SCHELP, false);
        schelpTijd = 0;
    }

    /** (Tests) berries in its belly. */
    void zetBessen(int n) {
        bessen = n;
    }

    /** (The plekjes) the spot it belongs to: wild Sjokkels stay around it. */
    public void thuisBij(BlockPos plek) {
        plekje = plek.immutable();
        restrictTo(plekje, 12);
    }

    @Nullable
    public BlockPos plekje() {
        return plekje;
    }

    @Override
    public void temmen(ServerPlayer player) {
        super.temmen(player);
        plekje = null;
        clearRestriction();
    }
}
