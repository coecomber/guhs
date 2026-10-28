package nl.juiced.guhs.feature.slaapzorg;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.wereldleven.Dagritme;
import nl.juiced.guhs.registry.ModItems;

/**
 * Breakfast in bed: in the morning, click a sleeping guh with knabbels (or another guh snack). It wakes up the cute way (a
 * big yawn and a stretch), munches its breakfast with hearts, and is "uitgerust" for the day (once a day per guh: its
 * chores go a bit faster). At night it is too early ("sst, nog even slapen"). Any guh, wild ones too: they love it.
 */
final class OntbijtOpBed {
    /** How long the morning's "uitgerust" lasts (most of a day). */
    static final int UITGERUST_TICKS = 20 * 60 * 15;

    private OntbijtOpBed() {
    }

    static boolean isOntbijt(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) || stack.is(BandFeature.SNACKS);
    }

    static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOntbijt(stack) || !Slapen.slaapt(guh) || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Dagdeel d = Slapen.dagdeel(guh);
        if (d != Dagdeel.OCHTEND && d != Dagdeel.DAG && d != Dagdeel.DUTJE) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.slaapzorg.ontbijt.te_vroeg", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }
        geef(guh, player instanceof ServerPlayer sp ? sp : null);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Wakes it with breakfast: yawn, then munch (Moeheid's timer), hearts, and "uitgerust" once a day. */
    static void geef(GuhEntity guh, ServerPlayer player) {
        Slapen.wordWakker(guh, false);
        guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
        guh.getPersistentData().putLong(Slapen.SMAKKEN_OM, guh.level().getGameTime() + Emote.GAPEN.onceTicks + 4);
        guh.heal(4f);
        long dag = Dagritme.ritmeDag(guh.level()) + 1;
        boolean eerste = guh.getPersistentData().getLongOr(Slapen.ONTBIJT_DAG, 0L) != dag;
        if (eerste) {
            guh.getPersistentData().putLong(Slapen.ONTBIJT_DAG, dag);
            guh.addEffect(new MobEffectInstance(KamperenFeature.UITGERUST, UITGERUST_TICKS, 0));
        }
        if (guh.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
            sl.playSound(null, guh.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6f, 1.3f);
        }
        if (player != null) {
            player.sendOverlayMessage(Component.translatable(eerste ? "gui.guhs.slaapzorg.ontbijt.lekker" : "gui.guhs.slaapzorg.ontbijt.nog_een",
                    guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
