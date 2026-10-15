package nl.juiced.guhs.feature.park.plek;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.bio.dieren.KoiEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.4.2 (Het Knabbelpark, de japanse tuin): de Knabbelkoi, the koi of the Klaterdal ({@link KoiEntity}: the same plump
 * guh-faced fish, the same model and moves) in kaasknabbel colours: cheese yellow with orange "holes", a toasted cap.
 * It only lives in the koi pond of a park's japanse tuin (Bezetting keeps four there; never wild, never in a bucket).
 * <ul>
 *   <li>Koivoer works as on every koi (they come up and beg); a kaasknabbel too, given by hand: it snaps it up and
 *       jumps out of the water for joy (plons!).</li>
 *   <li>It has no kleintjes (the pond stays as it is) and a water bucket can't scoop it up.</li>
 * </ul>
 * Texture: tools/features/park_plek_a_modellen.py (textures/entity/knabbelkoi.png, on koi.geo.json).
 */
public class Knabbelkoi extends KoiEntity {
    public Knabbelkoi(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    /** Never a kleintje (a kaasknabbel-koi pond is full as it is). */
    @Override
    public boolean uitgerust() {
        return false;
    }

    /** It eats (koivoer sprinkled or given, or a kaasknabbel by hand): hearts, a snap, and a jump for joy. */
    @Nullable
    @Override
    public KoiEntity voer(@Nullable ServerPlayer speler) {
        super.voer(null);   // (the koi's own: hearts, bubbles, the snap; no Klaterdal koi advancement)
        if (level() instanceof ServerLevel level) {
            if (isInWater()) {
                setDeltaMovement(getDeltaMovement().add(0, 0.62, 0));
                hurtMarked = true;
                level.playSound(null, this, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.6f, 1.3f);
                level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.1, 0.3, 0.1);
            }
            if (speler != null) {
                GuhAdvancements.grant(speler, "parkplek_knabbelkoi");
                speler.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.koi.plons").withStyle(ChatFormatting.GOLD));
            }
        }
        return null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.WATER_BUCKET)) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.koi.emmer").withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.KAAS_KNABBELS.get())) {
            if (player instanceof ServerPlayer sp) {
                stack.consume(1, player);
                voer(sp);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }
}
