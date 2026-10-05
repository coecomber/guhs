package nl.juiced.guhs.feature.guhpixel.grap2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;

/**
 * A stand-in guh of the two joke games of this slice (entity types guhs:guhmon_guh and guhs:bzg_guh): the copy of your
 * own guh and the guh of Gymleider Dutjes in the gym, the sleeping audience, the three candidates of Boer zoekt Guh.
 * It is nobody's guh: never tamed, fed, dressed, hurt, pushed, leashed or saved; it only sits, looks around, and sleeps
 * when its game says so. A click goes to the game of the player who clicked ({@link Grap2Slice#klik}).
 */
public class StandInGuh extends GuhEntity {
    private boolean slaapt;
    /** Which one of its game it is (the candidate's number, -1: just decoration). */
    private int rol = -1;

    public StandInGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f) {
            @Override
            public boolean canUse() {
                return !slaapt && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !slaapt && super.canContinueToUse();
            }
        });
    }

    /** Dresses it up like the looks snapshot of a real guh (BandData looks: Variant, Personality, Clothes, Haar). */
    public void looks(CompoundTag looks) {
        setVariant(GuhVariant.byId(looks.getStringOr("Variant", "")));
        GuhPersonality aard = GuhPersonality.byId(looks.getStringOr("Personality", ""));
        if (aard != null) {
            setPersonality(aard);
        }
        CompoundTag clothes = looks.getCompoundOrEmpty("Clothes");
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(clothes.getStringOr(slot.name(), ""));
            if (c != null) {
                wear(c);
            }
        }
        if (looks.contains("Haar")) {
            setHaarkleur(looks.getIntOr("Haar", 0));
        }
    }

    public int rol() {
        return rol;
    }

    public void rol(int rol) {
        this.rol = rol;
    }

    public boolean slaapt() {
        return slaapt;
    }

    /** Asleep (sitting, eyes shut, a zzz now and then) or awake again. */
    public void slaap(boolean aan) {
        slaapt = aan;
        setInSittingPose(aan);
        GuhHooks.zet(this, SleepInNestGoal.OOGJES_DICHT, aan);
        if (aan) {
            getNavigation().stop();
            setXRot(0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !slaapt) {
            return;
        }
        // (the Vadswoud's own tidy-up opens the eyes of a guh that does not sleep in a nest: shut them again)
        if (!GuhHooks.heeft(this, SleepInNestGoal.OOGJES_DICHT)) {
            GuhHooks.zet(this, SleepInNestGoal.OOGJES_DICHT, true);
        }
        if (!isInSittingPose()) {
            setInSittingPose(true);
        }
        if ((this.tickCount + getId() * 7) % 50 == 0 && this.level() instanceof ServerLevel level) {
            level.sendParticles(EmotesFeature.GUH_ZZZ.get(), getX(), getY() + 0.95, getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        }
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getType().getDescriptionId());
    }

    @Override
    public boolean shouldShowName() {
        return hasCustomName();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            Grap2Slice.klik(this, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public void onLaunchPressed(Player rider) {
    }

    // --- nobody's, never hurt, never saved -------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
