package nl.juiced.guhs.feature.bakkerij;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.registry.ModSounds;

/** Little extras of the bakery: your own tamed guh likes a pastry too (GuhHooks.klik). */
public final class BakkerijEvents {
    /**
     * A pastry for your tamed guh: it munches it happily (the smakken emote, hearts, a bit of health back). Guhs can
     * never be too vads - but they can be not VAHOEG enough, so every little bit helps.
     */
    static InteractionResult guhEet(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BakjeItem bakje) || BakjeItem.isSpel(stack) || !guh.isTame() || !guh.isOwnedBy(player)
                || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (guh.level() instanceof ServerLevel world) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            guh.heal(6f);
            guh.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.OWNER);
            world.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 5, 0.4, 0.2, 0.4, 0);
            world.sendParticles(BakkerijFeature.KNABBELWOLKJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.7, guh.getZ(), 4, 0.3, 0.2, 0.3, 0.01);
            world.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.2f);
            player.sendOverlayMessage(Component.translatable("gui.guhs.bakkerij.guh_eet", guh.getDisplayName(), bakje.recept.naam())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    private BakkerijEvents() {
    }
}
