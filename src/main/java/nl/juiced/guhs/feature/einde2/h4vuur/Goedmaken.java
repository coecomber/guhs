package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * einde2 phase B, slice h4_vuur, step 7: Zuka makes it up with the team (DESIGN 5, "Zuka's boog"), member by member. In the
 * camp the Guhvatar helps him find the right words: Toph first (her feet), then Katara (who trusts him least). Each talk has
 * three answers: the honest one works, the other two get a funny answer and you simply try again. Sokka's own making-up is
 * the break-in at Het Kokend Rotsblok (step 8), Aang's the fire lesson and the dragons (step 9).
 */
final class Goedmaken {
    static final String TOPH = "einde2_h4_vuur_toph", KATARA = "einde2_h4_vuur_katara";
    private static final String Q = E2H4Vuur.Q + "goedmaken.", G = E2H4Vuur.G;

    private Goedmaken() {
    }

    /** In the camp at step 7: the next talk that still has to be had. */
    static void praat(ServerPlayer p) {
        if (!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH)) {
            Praat.scene(p, TOPH, List.of(
                    new Praat.Regel(null, G + "naam.toph", Q + "toph.1"),
                    new Praat.Regel(null, G + "naam.zuka", Q + "toph.2"),
                    new Praat.Regel(null, G + "naam.toph", Q + "toph.3")),
                    new Praat.Optie(1, G + "optie.toph.eerlijk"), new Praat.Optie(2, G + "optie.toph.warm"), new Praat.Optie(3, G + "optie.toph.eer"));
        } else if (!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_KATARA)) {
            Praat.scene(p, KATARA, List.of(
                    new Praat.Regel(null, G + "naam.katara", Q + "katara.1"),
                    new Praat.Regel(null, G + "naam.zuka", Q + "katara.2"),
                    new Praat.Regel(null, G + "naam.katara", Q + "katara.3")),
                    new Praat.Optie(1, G + "optie.katara.doen"), new Praat.Optie(2, G + "optie.katara.knuffel"), new Praat.Optie(3, G + "optie.katara.staart"));
        }
    }

    static void antwoordToph(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (E2H4Vuur.VUUR.stap(p) != E2H4Vuur.V_GOEDMAKEN) {
            return;
        }
        if (optie < 1) {
            Later.plan(p, 200, q -> {
            });                                   // (closed without an answer: the camp waits a while before asking again)
            return;
        }
        Praat.sluit(p);
        Later.plan(p, 60, q -> {
        });
        if (optie == 1) {
            E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH, true);
            E2H4Vuur.zeg(p, "goedmaken.toph.goed");
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        } else {
            E2H4Vuur.zeg(p, "goedmaken.toph.fout" + optie);
        }
    }

    static void antwoordKatara(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (E2H4Vuur.VUUR.stap(p) != E2H4Vuur.V_GOEDMAKEN) {
            return;
        }
        if (optie < 1) {
            Later.plan(p, 200, q -> {
            });                                   // (closed without an answer: the camp waits a while before asking again)
            return;
        }
        Praat.sluit(p);
        Later.plan(p, 60, q -> {
        });
        if (optie == 1) {
            E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_KATARA, true);
            E2H4Vuur.zeg(p, "goedmaken.katara.goed");
            if (E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH) && E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_GOEDMAKEN)) {
                E2H4Vuur.zeg(p, "goedmaken.sokka");
            }
        } else {
            E2H4Vuur.zeg(p, "goedmaken.katara.fout" + optie);
        }
    }
}
