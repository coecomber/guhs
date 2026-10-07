package nl.juiced.guhs.feature.guhpixel.lobby;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Toegang;

/**
 * The lobby's part of the Guhdex tab "Guhpixel &amp; uitjes" (first section): how the ranks work and where the player
 * stands, the Netwerkkabeltje, the lobby parkour's best time and the golden knabbels found.
 */
final class LobbyGids implements GidsSectie {
    @Override
    public String id() {
        return "lobby";
    }

    @Override
    public int volgorde() {
        return 10;
    }

    @Override
    public boolean zichtbaar(ServerPlayer p) {
        return true;
    }

    @Override
    public void vul(ServerPlayer p, Bouwer b) {
        b.kop(Component.translatable("gui.guhs.lobby.gids.kop"));
        b.regel(Component.translatable("gui.guhs.lobby.gids.uitleg"));
        Rang rang = Muntjes.rang(p);
        b.stat(Component.translatable("gui.guhs.lobby.gids.rang"), rang.naam());
        Rang volgende = rang.volgende();
        if (volgende != null) {
            b.voortgang(Component.translatable("gui.guhs.lobby.gids.naar_rang", volgende.naam()), Math.min(Muntjes.totaal(p), volgende.vanaf()), volgende.vanaf());
        }
        // (a wrapped line, not a stat: five ranks do not fit in half a row)
        b.regel(Component.empty().append(Component.translatable("gui.guhs.lobby.gids.rangen")).append(": ").append(Component.translatable("gui.guhs.lobby.gids.rangen.lijst",
                Rang.GUH.naam(), Rang.VADS.naam(), Rang.VADS.vanaf(), Rang.VADS_PLUS.naam(), Rang.VADS_PLUS.vanaf(), Rang.MVG.naam(), Rang.MVG.vanaf(),
                Rang.MVG_PLUS_PLUS.naam(), Rang.MVG_PLUS_PLUS.vanaf())));
        b.stat(Component.translatable("gui.guhs.lobby.gids.kabeltje"),
                Component.translatable(Toegang.kabeltjeGehad(p) ? "gui.guhs.lobby.gids.kabeltje.ja" : "gui.guhs.lobby.gids.kabeltje.nee"));
        int best = LobbyParkour.best(p);
        b.stat(Component.translatable("gui.guhs.lobby.gids.parkour"),
                best > 0 ? Component.translatable("gui.guhs.lobby.gids.parkour.tijd", LobbyParkour.tijdTekst(best), LobbyParkour.keren(p))
                        : Component.translatable("gui.guhs.lobby.gids.parkour.geen"));
        b.voortgang(Component.translatable("gui.guhs.lobby.gids.knabbels"), Knabbels.gevonden(p), Knabbels.AANTAL);
        if (!Knabbels.alleGevonden(p)) {
            b.regel(Component.translatable("gui.guhs.lobby.gids.knabbels.tip." + (Knabbels.gevonden(p) % 3)));
        }
    }
}
