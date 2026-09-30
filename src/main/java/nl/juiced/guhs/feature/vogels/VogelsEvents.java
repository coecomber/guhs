package nl.juiced.guhs.feature.vogels;

import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The birds' advancements that follow from the Guhdex (tab guhs:diertjes/): one per bird whose page you filled in, the
 * challenge "Vogelkijker" for all four, and "Stil maar..." for sneaking right up to a bird that sits; and now and then a new
 * little flock around a player ({@link VogelSpawns#aanvullen}).
 */
public final class VogelsEvents {
    /** Guhdex page → its advancement (diertjes/vogels_&lt;id&gt;). */
    public static final Map<GuhVariant, String> PAGINAS = Map.of(
            GuhVariant.PLUISVINKJE, "diertjes/vogels_pluisvinkje",
            GuhVariant.KAASMEESJE, "diertjes/vogels_kaasmeesje",
            GuhVariant.GUH_UILTJE, "diertjes/vogels_guh_uiltje",
            GuhVariant.ZEEMEEUWTJE, "diertjes/vogels_zeemeeuwtje");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 40 != 0) {
            return;
        }
        controleer(player);
        if ((player.tickCount + player.getId()) % VogelSpawns.AANVUL_TIJD == 0 && !player.isSpectator()
                && player.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            VogelSpawns.aanvullen(player, player.getRandom());
        }
    }

    /** Grants what the player has earned (also used by the tests). */
    public static void controleer(ServerPlayer player) {
        var seen = GuhWorldData.get(player.server).player(player.getUUID()).seen;
        int n = 0;
        for (var e : PAGINAS.entrySet()) {
            if (seen.contains(e.getKey())) {
                GidsFeature.grant(player, e.getValue());
                n++;
            }
        }
        if (n == PAGINAS.size()) {
            GidsFeature.grant(player, "diertjes/vogels_alle");
        }
        if (player.isShiftKeyDown()) {
            for (Vogeltje v : player.level().getEntitiesOfClass(Vogeltje.class, player.getBoundingBox().inflate(1.5))) {
                if (!v.vliegt()) {
                    GidsFeature.grant(player, "diertjes/vogels_sluipen");
                    break;
                }
            }
        }
    }

    private VogelsEvents() {
    }
}
