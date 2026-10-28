package nl.juiced.guhs.feature.dexdromen;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.droomwezens.DroomwezensFeature;
import nl.juiced.guhs.quest.GuhDex;

/**
 * 1.8.0: the Guhdex side of Guhdromenland. The sterrenvisje is an ordinary creature page; the Blub-guh's page is filled by
 * a Blub you may see ({@link BlubSnoes#zichtbaar}: a wild one only once you helped Snoes, so the page spoils nothing); the
 * dream characters fill their pages like every guh character (GuhDex.onPlayerTick, their Kind names). Plus the
 * {@link BlubMeterkaart}.
 */
public final class DexDromenFeature {
    private DexDromenFeature() {
    }

    public static void register(IEventBus modBus) {
        GuhDex.creaturePage(GuhVariant.STERRENVISJE, DroomwezensFeature.STERRENVISJE, 6.0);   // (a fish in a pond: from the side is fine)
        NeoForge.EVENT_BUS.addListener(DexDromenFeature::tick);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BlubMeterkaart.register(registrar);
    }

    static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 3
                || nl.juiced.guhs.world.GuhWorldData.get(player.level().getServer()).player(player.getUUID()).seen.contains(GuhVariant.BLUBGUH)) {
            return;
        }
        for (BlubGuhEntity blub : player.level().getEntitiesOfClass(BlubGuhEntity.class,
                player.getBoundingBox().inflate(GuhDex.CREATURE_RANGE))) {
            if (BlubSnoes.zichtbaar(blub, player)) {
                GuhDex.zie(player, GuhVariant.BLUBGUH);
                return;
            }
        }
    }
}
