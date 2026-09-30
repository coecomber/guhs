package nl.juiced.guhs.feature.hemel;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;

/**
 * The hemel slice's game events: the client learns at login whether the Knuffelhart beats for it, nobody breaks a
 * Knuffelhart (not even outside a chapel, except in creative mode), the "Een echt hemelguhtje" advancement, a comforting
 * line when a tamed guh goes to the wolkjes, and the glans of a guh that just came back.
 */
public final class HemelEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            HemelPayloads.status(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            HemelPayloads.status(p);
        }
    }

    /** The Knuffelhart is made of hugs: it can't be broken (creative players may). */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(HemelFeature.KNUFFELHART.get()) && !event.getPlayer().getAbilities().instabuild) {
            event.setCanceled(true);
            event.getPlayer().sendOverlayMessage(Component.translatable("gui.guhs.hemel.hart_heel").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** "Een echt hemelguhtje": both hemel pieces unlocked (checked now and then). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 100 == 37 && HemelQuest.klopt(p)
                && KledingUnlocks.heeft(p, GuhClothes.HEMEL_AUREOOLTJE) && KledingUnlocks.heeft(p, GuhClothes.HEMEL_WOLKENVLEUGELTJES)) {
            GidsFeature.grant(p, "verhalen/hemel_engeltje");
        }
    }

    /** (Wolkjes.opDood) a tamed guh went to the wolkjes: a soft line about the Knuffelhart for its owner. */
    static void naarDeWolkjes(ServerLevel level, GuhEntity guh, @Nullable ServerPlayer eigenaar) {
        if (eigenaar == null) {
            return;
        }
        String key = HemelQuest.klopt(eigenaar) ? "gui.guhs.hemel.dood.hint_klopt" : "gui.guhs.hemel.dood.hint";
        eigenaar.sendSystemMessage(Component.translatable(key, guh.getName()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    /** (GuhHooks.tick, server) a guh that came back sparkles for a minute: little stars, now and then a heart. */
    static void glans(GuhEntity guh) {
        if (!GuhHooks.heeft(guh, VerhaalVlaggen.GLANS) || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        long tot = guh.getPersistentData().getLongOr(Hemel.GLANS_TOT, 0L);
        if (level.getGameTime() > tot) {
            GuhHooks.zet(guh, VerhaalVlaggen.GLANS, false);
            guh.getPersistentData().remove(Hemel.GLANS_TOT);
            return;
        }
        int t = guh.tickCount + guh.getId();
        if (t % 4 == 0) {
            level.sendParticles(HemelFeature.STERRETJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.7, guh.getZ(), 2,
                    guh.getBbWidth() * 0.5, guh.getBbHeight() * 0.35, guh.getBbWidth() * 0.5, 0.01);
        }
        if (t % 30 == 0) {
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
    }

    private HemelEvents() {
    }
}
