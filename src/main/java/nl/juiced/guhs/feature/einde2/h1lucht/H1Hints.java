package nl.juiced.guhs.feature.einde2.h1lucht;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 fix round 1 (fix-kern, playtest 1): what chapter 1 adds to the story's hints and continuity, kept out of the
 * minigame files (they are another agent's this round).
 * <ul>
 *   <li>the kaasbol's spot for "Verder met het verhaal" ({@link E2Hervat#KAASBOL});</li>
 *   <li>the example {@link StapHint}s of three steps: the luchtscooter start ({@value E2H1Lucht#SCOOTER}), the balcony of
 *       the glide rings ({@value E2H1Lucht#ZWEVEN}) and the closed workshop door ({@value E2H1Lucht#DEUR}): a look scene
 *       with one caption when the step starts, a chat line with the direction, a column of light at the spot;</li>
 *   <li>the airbending lesson (playtest 1 item 11, "ik heb niet echt leren luchtsturen"): right after the luchtbal match
 *       Aang-guh says that the wind the player gave the ball WAS airbending, and asks them to try it without a ball (the
 *       power wheel: Luchtsprong, then Luchtbal); the first use of each gets his answer. Text only, no step: the chapter's
 *       steps, scenes and its flag {@code geleerd} at the end stay exactly as they were.</li>
 * </ul>
 * Texts: tools/features/einde2_verhaal.py ({@code gui.guhs.einde2_verhaal.h1.*}, {@code scene.guhs.einde2_verhaal_kijk_h1_*}).
 */
public final class H1Hints {
    static final String T = "gui.guhs.einde2_verhaal.h1.";
    /** The look scenes of the three example hints (each with its own caption). */
    static Cutscene kijkScooter, kijkZweven, kijkDeur;

    private H1Hints() {
    }

    /** From E2H1LuchtFeature.register (common code: the look scenes must be known on both sides). */
    static void registreer() {
        E2Hervat.KAASBOL = Kaasbol::plek;
        kijkScooter = StapHint.kijkScene("einde2_verhaal_kijk_h1_scooter", true);
        kijkZweven = StapHint.kijkScene("einde2_verhaal_kijk_h1_zweven", true);
        kijkDeur = StapHint.kijkScene("einde2_verhaal_kijk_h1_deur", true);
        String tempel = E2H1Lucht.STRUCTUUR;
        // (the scooter hint waits for Aang-guh's three lesson lines after the luchtbal match)
        StapHint.naar(tempel, "scooter_start").tekst(T + "scooter").kijk(kijkScooter).bijStart(LES_KLAAR + 20).baken()
                .zet(E2Verhaal.LUCHT, E2H1Lucht.SCOOTER);
        StapHint.naar(tempel, "zweef_start").tekst(T + "zweven").kijk(kijkZweven).bijStart().baken()
                .zet(E2Verhaal.LUCHT, E2H1Lucht.ZWEVEN);
        StapHint.naar(tempel, "werkplaatsdeur").tekst(T + "deur").kijk(kijkDeur).bijStart().baken()
                .zet(E2Verhaal.LUCHT, E2H1Lucht.DEUR);
        // the airbending lesson
        E2H1Lucht.LIJN.opStap((p, oud, nieuw) -> {
            if (oud == E2H1Lucht.LUCHTBAL && nieuw == E2H1Lucht.SCOOTER) {
                les(p);
            }
        });
        Krachten.NA_GEBRUIK.add(H1Hints::geoefend);
    }

    /** When the last lesson line is said (ticks after the match). */
    static final int LES_KLAAR = 200;

    /** Aang-guh's three lines after the luchtbal match. */
    static void les(ServerPlayer p) {
        if (!E2H1Lucht.LIJN.eenmalig(p, "les_lucht")) {
            return;
        }
        StapHint.later(p, 60, s -> aang(s, Component.translatable(T + "les.1")));
        StapHint.later(p, 130, s -> aang(s, Component.translatable(T + "les.2", Component.keybind("key.guhs.einde2_kracht"))));
        StapHint.later(p, LES_KLAAR, s -> s.sendSystemMessage(Component.literal("* ").append(Component.translatable(T + "les.3"))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    /** The first Luchtsprong and the first Luchtbal after the lesson: Aang-guh's answer (once each). */
    static void geoefend(ServerPlayer p, Krachten.Kracht k) {
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap < E2H1Lucht.SCOOTER || stap >= E2H1Lucht.STAPPEN) {
            return;
        }
        if (k.id().equals(H1Krachten.LUCHTSPRONG) && E2H1Lucht.LIJN.eenmalig(p, "les_sprong")) {
            StapHint.later(p, 25, s -> aang(s, Component.translatable(T + "les.sprong")));
        } else if (k.id().equals(H1Krachten.LUCHTBAL) && E2H1Lucht.LIJN.eenmalig(p, "les_bal")) {
            StapHint.later(p, 15, s -> aang(s, Component.translatable(T + "les.bal")));
        }
    }

    /** "<Aang-guh> text" in the chat (he is not always near: his voice on the wind). */
    static void aang(ServerPlayer p, Component tekst) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc." + GuhNpcEntity.Kind.AANG_GUH.id()))
                .append("> ").withStyle(ChatFormatting.LIGHT_PURPLE).append(tekst.copy().withStyle(ChatFormatting.WHITE)));
    }
}
