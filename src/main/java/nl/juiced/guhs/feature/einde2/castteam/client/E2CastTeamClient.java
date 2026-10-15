package nl.juiced.guhs.feature.einde2.castteam.client;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.feature.einde2.castteam.CastTeam;

/**
 * einde2, slice cast_team, client side: every kind of {@link CastTeam} draws its own model
 * ({@code geckolib/models/entity/guh_npc_<kind>.geo.json}) with the animation file of its character on its body
 * ({@link CastTeam#animatieBestand}: {@code guh_npc_<basis>.animation.json}, or {@code guh_npc_zuka_zit} for Zuka's guh
 * looks); the stock NPC renderer does the rest (texture {@code npc_<kind>.png}).
 */
public final class E2CastTeamClient {
    public static void init(IEventBus modBus) {
        for (CastTeam rij : CastTeam.values()) {
            SittingGuhRenderers.NPC_MODELEN.put(rij.kind, Guhs.id("entity/guh_npc_" + rij.kind.id()));
            SittingGuhRenderers.NPC_ANIMATIES.put(rij.kind, Guhs.id("entity/" + CastTeam.animatieBestand(rij.kind)));
        }
    }

    private E2CastTeamClient() {
    }
}
