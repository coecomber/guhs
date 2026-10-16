package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 fix round 1, stage 2 (fix2-h4, playtest 1: "ietsjes meer hints, een duidelijkere aansluiting, en hoe je verder
 * kunt na doodgaan"): a {@link StapHint} for every step of chapter 4 ({@value E2Verhaal#VUUR}) and the finale
 * ({@value E2Verhaal#FINALE}) after the arrival: where the next part starts (a marker of the capital), a text with the
 * direction and a landmark, a column of light there, a ~3 s look scene where the place is not in view, and the spot
 * "Verder met het verhaal" puts you ({@code terug}).
 * <p>
 * The capital is big (the theatre, the ruin and the White Lotus camp are 100 to 200 blocks apart), and a look scene only
 * plays within {@link StapHint#KIJK_BEREIK} blocks. So the far steps ({@link #NADER}) show their hint (with the look scene)
 * once more when the player first comes within {@link #NADER_AFSTAND} blocks on the way ({@link #nader}).
 * Texts: {@code gui.guhs.einde2_h4_vuur.hint.*}, {@code scene.guhs.einde2_h4_vuur_kijk_*} (einde2_h4_vuur_tekst.py).
 */
final class H4Hints {
    static final String T = E2H4Vuur.G + "hint.";
    /** The id prefix of this slice's look scenes (not one of the 18 story scenes of {@link Scenes#LENGTES}). */
    static final String KIJK = Scenes.ID + "kijk_";
    /** The look scenes (common code: they must be known on both sides). */
    static Cutscene kijkSchool, kijkDans, kijkEendjes, kijkStrand, kijkKamp, kijkDrakendans, kijkToneel, kijkTent, kijkKroning, kijkSuki;
    /** The far steps: the hint plays again (with its look scene) when the player first comes near on the way. */
    static final Set<String> NADER = Set.of(E2Verhaal.VUUR + "/" + E2H4Vuur.V_SCHOOL, E2Verhaal.VUUR + "/" + E2H4Vuur.V_ZWARTE_ZON,
            E2Verhaal.VUUR + "/" + E2H4Vuur.V_ROTSBLOK, E2Verhaal.VUUR + "/" + E2H4Vuur.V_DRAKENDANS, E2Verhaal.VUUR + "/" + E2H4Vuur.V_TONEEL,
            E2Verhaal.VUUR + "/" + E2H4Vuur.V_TENT);
    /** Blocks: "near on the way" (inside the look scene's reach, not yet at the place). */
    static final double NADER_AFSTAND = 80, NADER_MIN = 18;
    /** (tests) the markers each step's hint points at and puts you back at: "lijn/stap" -> {target, terug}. */
    static final Map<String, String[]> MARKERS = new LinkedHashMap<>();
    /** The template spot on the invasion beach the hint points at (by the barricades and the sign; no marker there). */
    static final BlockPos STRAND = new BlockPos(141, 45, 209);
    /** Per player: the far steps whose hint already played near ("lijn/stap"); not saved, so a later visit gets it again. */
    private static final Map<UUID, Set<String>> NADER_GEZIEN = new ConcurrentHashMap<>();

    private H4Hints() {
    }

    /** From E2H4Vuur.register (common code), after the two lines are registered. */
    static void registreer() {
        kijkSchool = StapHint.kijkScene(KIJK + "school", true);
        kijkDans = StapHint.kijkScene(KIJK + "dans", true);
        kijkEendjes = StapHint.kijkScene(KIJK + "eendjes", true);
        kijkStrand = StapHint.kijkScene(KIJK + "strand", true);
        kijkKamp = StapHint.kijkScene(KIJK + "kamp", true);
        kijkDrakendans = StapHint.kijkScene(KIJK + "drakendans", true);
        kijkToneel = StapHint.kijkScene(KIJK + "toneel", true);
        kijkTent = StapHint.kijkScene(KIJK + "tent", true);
        kijkKroning = StapHint.kijkScene(KIJK + "kroning", true);
        kijkSuki = StapHint.kijkScene(KIJK + "suki", true);
        String v = E2Verhaal.VUUR, f = E2Verhaal.FINALE;
        // --- chapter 4 (step 0, the arrival, needs none: Appa flies you there; "Verder" uses the landing spot) ---------------
        hint(v, E2H4Vuur.V_SCHOOL, "rustpunt_3", null).tekst(T + "school").kijk(kijkSchool).bijStart().baken().zet(v, E2H4Vuur.V_SCHOOL);
        hint(v, E2H4Vuur.V_DANS, "dansgrot_ingang", null).tekst(T + "dans").kijk(kijkDans).bijStart().baken().zet(v, E2H4Vuur.V_DANS);
        hint(v, E2H4Vuur.V_EENDJES, "brood", null).tekst(T + "eendjes").kijk(kijkEendjes).bijStart().baken().zet(v, E2H4Vuur.V_EENDJES);
        MARKERS.put(v + "/" + E2H4Vuur.V_ZWARTE_ZON, new String[] {"invasie_strand", "invasie_strand"});
        StapHint.naar(sjabloon(STRAND)).tekst(T + "strand").kijk(kijkStrand).bijStart().baken().zet(v, E2H4Vuur.V_ZWARTE_ZON);
        // (the cell is 7 blocks from where the Black Sun leaves you: the scene starts by itself; the hint is for "Verder")
        hint(v, E2H4Vuur.V_GEVANGENIS, "miroh_cel", "rustpunt_4").tekst(T + "cel").zonderKijk().zet(v, E2H4Vuur.V_GEVANGENIS);
        hint(v, E2H4Vuur.V_KAMP, "appa_kamp", null).tekst(T + "kamp").kijk(kijkKamp).bijStart().baken().zet(v, E2H4Vuur.V_KAMP);
        // (in the camp the talk starts by itself)
        hint(v, E2H4Vuur.V_GOEDMAKEN, "team_zuka", "appa_kamp").tekst(T + "goedmaken").zonderKijk().zet(v, E2H4Vuur.V_GOEDMAKEN);
        // Het Kokend Rotsblok: the gondola first, the cell of Suki-guh once you are in (the step's own texts 8 / 8_binnen)
        MARKERS.put(v + "/" + E2H4Vuur.V_ROTSBLOK, new String[] {"gondel_boven", "rustpunt_5", "cel_suki", "rustpunt_6"});
        StapHint.naar(rotsblok("cel_suki", "gondel_boven")).bijStart().baken().terug(rotsblok("rustpunt_6", "rustpunt_5"))
                .zet(v, E2H4Vuur.V_ROTSBLOK);
        hint(v, E2H4Vuur.V_DRAKENDANS, "trap_boven", "rustpunt_7").tekst(T + "drakendans").kijk(kijkDrakendans).bijStart().baken()
                .zet(v, E2H4Vuur.V_DRAKENDANS);
        hint(v, E2H4Vuur.V_TONEEL, "publiek_speler", "rustpunt_9").tekst(T + "toneel").kijk(kijkToneel).bijStart().baken()
                .zet(v, E2H4Vuur.V_TONEEL);
        hint(v, E2H4Vuur.V_TENT, "verzoening_zuka", "rustpunt_10").tekst(T + "tent").kijk(kijkTent).bijStart().baken().zet(v, E2H4Vuur.V_TENT);
        // --- the finale --------------------------------------------------------------------------------------------------
        // (step 0 starts when chapter 4 ends: shown from E2H4Vuur.vuurKlaar; back from the Geestenwereld: Miroh again)
        hint(f, E2H4Vuur.F_TOP, "miroh_tent", "rustpunt_10").tekst(T + "miroh").zonderKijk().baken().zet(f, E2H4Vuur.F_TOP);
        hint(f, E2H4Vuur.F_SCHILDPAD, "rustpunt_10", null).tekst(T + "schildpad").zonderKijk().zet(f, E2H4Vuur.F_SCHILDPAD);
        hint(f, E2H4Vuur.F_VLOOT, "leeuwschildpad_kade", null).tekst(T + "vloot").zonderKijk().baken().zet(f, E2H4Vuur.F_VLOOT);
        hint(f, E2H4Vuur.F_AGNI_KAI, "agni_kai_midden", null).tekst(T + "agni_kai").zonderKijk().baken().zet(f, E2H4Vuur.F_AGNI_KAI);
        hint(f, E2H4Vuur.F_DUEL, "trap_arena", "rustpunt_8").tekst(T + "duel").zonderKijk().baken().zet(f, E2H4Vuur.F_DUEL);
        hint(f, E2H4Vuur.F_KRONING, "kroning_zuka", null).tekst(T + "kroning").kijk(kijkKroning).bijStart().baken().zet(f, E2H4Vuur.F_KRONING);
        hint(f, E2H4Vuur.F_AFTITELING, "agni_kai_midden", null).tekst(T + "aftiteling").zonderKijk().zet(f, E2H4Vuur.F_AFTITELING);
    }

    /** A hint at a marker of the capital (and where "Verder" puts you: another marker, or the target itself). */
    private static StapHint.Hint hint(String lijn, int stap, String doel, @Nullable String terug) {
        MARKERS.put(lijn + "/" + stap, new String[] {doel, terug == null ? doel : terug});
        StapHint.Hint h = StapHint.naar(Plek.STRUCTUUR, doel);
        return terug == null ? h : h.terug(Plek.STRUCTUUR, terug);
    }

    /** A template spot of the capital (a leegte_bouw is never turned: origin + spot). */
    static StapHint.Doel sjabloon(BlockPos spot) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            BlockPos o = Plekken.bekend(level, Plek.STRUCTUUR) ? Plekken.oorsprong(level, Plek.STRUCTUUR) : null;
            return o == null ? null : o.offset(spot);
        });
    }

    /** Het Kokend Rotsblok: one marker once you are inside (the flag {@code binnen}), the other before. */
    private static StapHint.Doel rotsblok(String binnen, String buiten) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> Plekken.bekend(level, Plek.STRUCTUUR)
                ? Plekken.wereld(level, Plek.STRUCTUUR, E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_BINNEN) ? binnen : buiten) : null);
    }

    /** From E2H4Vuur.regie: on the way to a far step, its hint once more (with the look scene) when it comes in reach. */
    static void nader(ServerPlayer p, Verhaallijn l) {
        if (l.klaar(p)) {
            return;
        }
        String sleutel = l.id() + "/" + l.stap(p);
        if (!NADER.contains(sleutel)) {
            return;
        }
        StapHint.Hint h = StapHint.hint(l.id(), l.stap(p));
        BlockPos b = h == null || p.level().dimension() != h.doel().dim() ? null : h.doel().blok(p);
        if (b == null) {
            return;
        }
        double d = p.position().distanceTo(Vec3.atCenterOf(b));
        Set<String> gezien = NADER_GEZIEN.computeIfAbsent(p.getUUID(), u -> ConcurrentHashMap.newKeySet());
        if (d < NADER_MIN) {
            gezien.add(sleutel);                                   // (already there: nothing to point at)
        } else if (d <= NADER_AFSTAND && gezien.add(sleutel)) {
            StapHint.toon(p, l.id());
        }
    }

    /** A step started within reach of its target: its own start hint already plays the look scene, no second one on the way. */
    static void gestart(ServerPlayer p, String lijn, int stap) {
        StapHint.Hint h = StapHint.hint(lijn, stap);
        BlockPos b = h == null || p.level().dimension() != h.doel().dim() ? null : h.doel().blok(p);
        Set<String> gezien = NADER_GEZIEN.computeIfAbsent(p.getUUID(), u -> ConcurrentHashMap.newKeySet());
        gezien.remove(lijn + "/" + stap);
        if (b != null && p.position().distanceTo(Vec3.atCenterOf(b)) <= NADER_AFSTAND) {
            gezien.add(lijn + "/" + stap);
        }
    }

    static void vergeet(UUID speler) {
        NADER_GEZIEN.remove(speler);
    }
}
