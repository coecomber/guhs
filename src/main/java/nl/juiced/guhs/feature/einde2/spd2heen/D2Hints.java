package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: a {@link StapHint} for each of this slice's steps (playtest 1: WHAT and WHERE, a look
 * scene where the target is not obvious, a soft column of light, and a resume spot for "Verder met het verhaal" / the
 * kaasbol). Texts: {@code gui.guhs.einde2_sp_d2_heen.hint.*}, look scenes {@code scene.guhs.einde2_sp_d2_heen_kijk_*}.
 * <ul>
 *   <li>0 the manager: De Krokante Knabbel (the middle of its hall), shown a moment after part 1 is done (the line's step 0
 *       never "starts", so {@link SpD2Heen} asks for it) and on every "Verder";</li>
 *   <li>1 the ice cream: your stool in the Guhfy Guhber;</li>
 *   <li>2 the crown: the king's throne on the square (after the horn);</li>
 *   <li>3 the drive: the Knabbelburgermobiel while you have not got in, De Ruige Kroeg's parking once you drive ({@code 3_b});
 *       "Verder" puts you by the car in front of De Krokante Knabbel;</li>
 *   <li>4 the key: the sleeping Mika's table; "Verder" puts you on the bench outside De Ruige Kroeg.</li>
 * </ul>
 */
final class D2Hints {
    static final String T = SpD2Heen.G + "hint.";
    static final String KIJK = D2Scenes.ID + "kijk_";
    static Cutscene kijkKnabbel, kijkIjssalon, kijkPlein, kijkMobiel, kijkKroeg;
    /** (tests) step -> {structure, target marker, resume marker}. */
    static final Map<Integer, String[]> MARKERS = new LinkedHashMap<>();

    private D2Hints() {
    }

    /** From SpD2Heen.register (common code: the look scenes must be known on both sides). */
    static void registreer() {
        kijkKnabbel = StapHint.kijkScene(KIJK + "knabbel", true);
        kijkIjssalon = StapHint.kijkScene(KIJK + "ijssalon", true);
        kijkPlein = StapHint.kijkScene(KIJK + "plein", true);
        kijkMobiel = StapHint.kijkScene(KIJK + "mobiel", true);
        kijkKroeg = StapHint.kijkScene(KIJK + "kroeg", true);
        String k = SpVerhaal.KROON, bb = D2Plek.BB, sp = D2Plek.SP;
        hint(SpVerhaal.K_MANAGER, bb, "d2h_zaal", "mobiel_start").tekst(T + "knabbel").kijk(kijkKnabbel).baken().zet(k, SpVerhaal.K_MANAGER);
        hint(SpVerhaal.K_IJSSALON, bb, "ijs_speler", null).tekst(T + "ijssalon").kijk(kijkIjssalon).bijStart().baken().zet(k, SpVerhaal.K_IJSSALON);
        hint(SpVerhaal.K_KROON, bb, "neptuguh_troon", null).tekst(T + "plein").kijk(kijkPlein).bijStart().baken().zet(k, SpVerhaal.K_KROON);
        // the drive: the car until you drive, then the parking of De Ruige Kroeg
        MARKERS.put(SpVerhaal.K_RIT, new String[] {bb, "mobiel_start", "mobiel_start"});
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> Rit.rijdt(p) ? wereld(level, sp, "kroeg_parkeer") : wereld(level, bb, "mobiel_start")))
                .tekst(T + "rit").kijk(kijkMobiel).bijStart().baken().terug(bb, "mobiel_start").zet(k, SpVerhaal.K_RIT);
        hint(SpVerhaal.K_SLEUTEL, sp, "sleutel", "rustpunt_1").tekst(T + "kroeg").kijk(kijkKroeg).bijStart().baken().zet(k, SpVerhaal.K_SLEUTEL);
    }

    private static StapHint.Hint hint(int stap, String structuur, String doel, String terug) {
        MARKERS.put(stap, new String[] {structuur, doel, terug == null ? doel : terug});
        StapHint.Hint h = StapHint.naar(structuur, doel);
        return terug == null ? h : h.terug(structuur, terug);
    }

    private static BlockPos wereld(net.minecraft.server.level.ServerLevel level, String structuur, String plek) {
        return Plekken.bekend(level, structuur) ? Plekken.wereld(level, structuur, plek) : null;
    }
}
