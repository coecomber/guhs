package nl.juiced.guhs.feature.einde2.spzij;

import java.util.function.BiFunction;
import java.util.function.IntPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 4.4, the playtest lessons): a {@link StapHint} for every step of the three
 * side lines: the chat line says WHAT and WHERE (a direction and a landmark), a ~3 s look scene where the spot is not in
 * view, a soft column of light while the step is on (steps 1 and later: step 0 is a character you meet in town, and three
 * columns at once in the middle of part 1 would only clutter), and the resume spot for the kaasbol / "Verder" when a side
 * line is the one the player follows. Texts {@code gui.guhs.einde2_sp_zij.hint.*}, look scenes
 * {@code scene.guhs.einde2_sp_zij_kijk_*} (tools/features/einde2_sp_zij.py HINTS / KIJK).
 */
final class SpZijHints {
    static final String T = SpZij.P + "hint.";
    static final String K = SpZijScenes.VOOR + "kijk_";

    private SpZijHints() {
    }

    /** A marker of Bikinibodem. */
    static StapHint.Doel bij(String marker) {
        return bij((p, level) -> SpZij.plek(level, marker));
    }

    static StapHint.Doel bij(BiFunction<ServerPlayer, ServerLevel, BlockPos> waar) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, waar);
    }

    /** The nearest of the numbered markers {@code <voor>1..n} that still counts (null: none). */
    @Nullable
    static BlockPos dichtste(ServerPlayer p, ServerLevel level, String voor, int n, IntPredicate telt) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 1; i <= n; i++) {
            BlockPos b = telt.test(i) ? SpZij.plek(level, voor + i) : null;
            if (b == null) {
                continue;
            }
            double d = p.level() == level ? Vec3.atCenterOf(b).distanceToSqr(p.position()) : i;
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }

    /** From E2SpZijFeature.register (common code: the look scenes must be known on both sides), after the lines. */
    static void registreer() {
        Cutscene koepel = kijk("koepel"), mat = kijk("mat"), onderdeel = kijk("onderdeel"), raket = kijk("raket");
        Cutscene trap = kijk("trap"), stroom = kijk("stroom"), lantaarn = kijk("lantaarn");
        Cutscene rusthuis = kijk("rusthuis"), parkeer = kijk("parkeer"), knabbel = kijk("knabbel");

        // Sandy-guh's tree dome: in through the airlock, the mat, the parts round town, the rocket
        String s = SpZijLijnen.SANDY;
        StapHint.naar(bij("sandy")).tekst(T + "sandy.0").kijk(koepel).terug(bij("koepel_sluis")).zet(s, SpZijLijnen.S_BEZOEK);
        StapHint.naar(bij((p, level) -> {
            Vec3 m = Boomkoepel.zoneMidden(level, "karate_mat");
            return m == null ? null : BlockPos.containing(m);
        })).tekst(T + "sandy.1").kijk(mat).bijStart(40).baken().terug(bij("koepel_sluis")).zet(s, SpZijLijnen.S_KARATE);
        StapHint.naar(bij((p, level) -> dichtste(p, level, "raket_onderdeel_", Boomkoepel.ONDERDELEN.size(),
                i -> !Boomkoepel.ln().vlag(p, "onderdeel_" + i)))).tekst(T + "sandy.2").kijk(onderdeel).bijStart(60).baken()
                .zet(s, SpZijLijnen.S_ONDERDELEN);
        StapHint.naar(bij("raket")).tekst(T + "sandy.3").kijk(raket).bijStart(40).baken().terug(bij("koepel_sluis")).zet(s, SpZijLijnen.S_RAKET);

        // De Vliegende Guhlander: the mooring, the deck, the lanterns, the mooring again
        String g = SpZijLijnen.GUHLANDER;
        StapHint.naar(bij("guhlander_trap")).tekst(T + "guhlander.0").kijk(trap).zet(g, SpZijLijnen.G_WACHT);
        StapHint.naar(bij("guhlander_trap")).tekst(T + "guhlander.1").kijk(stroom).bijStart(40).baken().zet(g, SpZijLijnen.G_DEK);
        StapHint.naar(bij((p, level) -> dichtste(p, level, "zij_lantaarn_", Guhlander.LANTAARNS, i -> !Guhlander.ln().vlag(p, "lantaarn_" + i))))
                .tekst(T + "guhlander.2").kijk(lantaarn).bijStart(60).baken().zet(g, SpZijLijnen.G_LANTAARNS);
        StapHint.naar(bij("guhlander_trap")).tekst(T + "guhlander.3").kijk(trap).bijStart(40).baken().zet(g, SpZijLijnen.G_TERUG);

        // Zeemeerman and Mosselguh: the rest home, the car park, the rest home, the front door of De Krokante Knabbel and back
        String z = SpZijLijnen.ZEEMEERMAN;
        StapHint.naar(bij("zeemeerman")).tekst(T + "zeemeerman.0").kijk(rusthuis).zet(z, SpZijLijnen.Z_BEZOEK);
        StapHint.naar(bij("riem")).tekst(T + "zeemeerman.1").kijk(parkeer).bijStart(40).baken().zet(z, SpZijLijnen.Z_RIEM);
        StapHint.naar(bij("zeemeerman")).tekst(T + "zeemeerman.2").zonderKijk().bijStart(40).baken().zet(z, SpZijLijnen.Z_RIEM_TERUG);
        StapHint.naar(bij((p, level) -> SpZij.plek(level, Helden.ln().vlag(p, SpZijLijnen.LUNCH) ? "zeemeerman" : "mobiel_start")))
                .tekst(T + "zeemeerman.3").kijk(knabbel).bijStart(60).baken().zet(z, SpZijLijnen.Z_LUNCH);
    }

    private static Cutscene kijk(String naam) {
        return StapHint.kijkScene(K + naam, true);
    }
}
