package nl.juiced.guhs.feature.einde2.h3aarde;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * The two winks of chapter 3 (DESIGN 5c): once per player, for every player (also who never played the old stories), and
 * never in the middle of an emotional moment.
 * <ul>
 *   <li>wink 1, Sjokkel creeping on along the wall between two rings: right after your monorail ride, on the platform of
 *       station Middenring (where its camera stands);</li>
 *   <li>wink 3, Mewtwo-guh in his tank under Meer Laogai: on your way OUT of the base, after the reunion with Appa (not
 *       while Appa is lost).</li>
 * </ul>
 */
public final class H3Knipogen {
    private H3Knipogen() {
    }

    static void tik(ServerPlayer p, E2H3Aarde.Stad stad, int stap) {
        if (stap > E2H3Aarde.LAOGAI && stad.bij(p, "knipoog_mewtwo_dai_li", 5) && !Cutscenes.gezien(p, H3Scenes.KNIPOOG_MEWTWO.id())
                && H3Verloop.vrij(p)) {
            H3Scenes.speel(p, stad, H3Scenes.KNIPOOG_MEWTWO, null);
        }
    }

    /** Wink 1 on the platform of station Middenring (once). */
    static void sjokkel(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (!Cutscenes.gezien(p, H3Scenes.KNIPOOG_SJOKKEL.id())) {
            H3Scenes.speel(p, stad, H3Scenes.KNIPOOG_SJOKKEL, null);
        }
    }
}
