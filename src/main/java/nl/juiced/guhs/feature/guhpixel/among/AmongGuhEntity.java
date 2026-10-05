package nl.juiced.guhs.feature.guhpixel.among;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.among.model.Kleur;

/**
 * A participant of Among Guhs as a guh in a coloured space suit: a guh NPC of the crew (or an NPC Mika: you cannot tell),
 * or the sleeping body of a real player who was pushed asleep. A stand-in: never a real guh (not tameable, no goals, never
 * saved); the session ({@link AmongSessie}) puts it where the rules ({@code among.model}) say it is. Right-click on a
 * sleeper = report it.
 */
public class AmongGuhEntity extends GuhEntity {
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(AmongGuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SLAAPT = SynchedEntityData.defineId(AmongGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WERKT = SynchedEntityData.defineId(AmongGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation STAAN = RawAnimation.begin().thenLoop("animation.guh.idle");

    /** The participant this guh is (index in the round). */
    public int deelnemer = -1;

    public AmongGuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_SLAAPT, false);
        builder.define(DATA_WERKT, false);
    }

    @Override
    protected void registerGoals() {
    }

    public Kleur kleur() {
        return Kleur.op(entityData.get(DATA_KLEUR));
    }

    public void setKleur(Kleur kleur) {
        entityData.set(DATA_KLEUR, kleur.ordinal());
    }

    public boolean slaapt() {
        return entityData.get(DATA_SLAAPT);
    }

    public void setSlaapt(boolean slaapt) {
        if (slaapt() != slaapt) {
            entityData.set(DATA_SLAAPT, slaapt);
        }
    }

    public boolean werkt() {
        return entityData.get(DATA_WERKT);
    }

    public void setWerkt(boolean werkt) {
        if (werkt() != werkt) {
            entityData.set(DATA_WERKT, werkt);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> {
            if (slaapt()) {
                return state.setAndContinue(Emote.SLAPEN.animation);
            }
            return state.setAndContinue(state.isMoving() ? LOOP : STAAN);
        }));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            Sessie s = Sessies.van(sp);
            if (s instanceof AmongSessie among) {
                among.klikGuh(sp, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The name only shows when you look straight at the guh (no name tags through the ship's walls). */
    @Override
    public boolean shouldShowName() {
        return false;
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
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return slaapt() ? null : super.getAmbientSound();
    }
}
