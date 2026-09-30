package nl.juiced.guhs.feature.boerderij;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * De guhkoe: a big, vadsige cow with cheese-coloured spots (with little holes, like gatenkaas), a pink guh face, round guh
 * ears and two stubby horns. When she is content she gives kaasmelk: right-click her with an empty glass bottle (once a
 * day). Moeh!
 */
public class GuhkoeEntity extends BoerderijDier {
    public GuhkoeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    public String soort() {
        return "guhkoe";
    }

    @Override
    protected boolean productOpAfroep() {
        return true;
    }

    @Override
    protected void geefProduct(@Nullable ServerPlayer player) {
        // (the kaasmelk is fetched with a bottle: see mobInteract)
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.GLASS_BOTTLE) || isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return melk((ServerPlayer) player, hand) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /** Milking with a bottle in this hand: true when she gave kaasmelk. */
    public boolean melk(ServerPlayer player, InteractionHand hand) {
        nieuweDag();                                        // (a new day first: not yesterday's care, not yesterday's milking)
        if (!isBlij() || productGegeven()) {
            player.sendOverlayMessage(Component.translatable(productGegeven() ? "gui.guhs.boerderij.koe.al_gemolken" : "gui.guhs.boerderij.koe.niet_blij")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            playSound(BoerderijFeature.KOE_MOEH.get(), 0.7f, 0.8f);
            return false;
        }
        setProductGegeven(true);
        ItemStack stack = player.getItemInHand(hand);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(BoerderijFeature.KAASMELK.get())));
        level().playSound(null, this, SoundEvents.COW_MILK, SoundSource.PLAYERS, 1f, 1.1f);
        ((ServerLevel) level()).sendParticles(BoerderijFeature.MELKDRUPPEL.get(), getX(), getY() + 0.5, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        triggerAnim("actie", "blij");
        BoerderijVoortgang.product(player, BoerderijVoortgang.Product.KAASMELK, 1);
        return true;
    }

    /**
     * 2.10 (klusjes): a huisje guh milks her with an empty bottle from the huisje's chest (no player). Returns the
     * kaasmelk, or EMPTY when she isn't content or already gave her milk today.
     */
    public ItemStack melkZonderSpeler() {
        nieuweDag();
        if (isBaby() || !isBlij() || productGegeven()) {
            return ItemStack.EMPTY;
        }
        setProductGegeven(true);
        level().playSound(null, this, SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1f, 1.1f);
        ((ServerLevel) level()).sendParticles(BoerderijFeature.MELKDRUPPEL.get(), getX(), getY() + 0.5, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        triggerAnim("actie", "blij");
        return new ItemStack(BoerderijFeature.KAASMELK.get());
    }

    @Override
    protected void speelGeluid() {
        playSound(BoerderijFeature.KOE_MOEH.get(), 0.8f, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return BoerderijFeature.KOE_MOEH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.5f : 1.15f) + (random.nextFloat() - 0.5f) * 0.15f;
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions borstelDeeltje() {
        return BoerderijFeature.MELKDRUPPEL.get();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return BoerderijFeature.GUHKOE.get().create(level, EntitySpawnReason.TRIGGERED);
    }
}
