package nl.juiced.guhs.feature.einde2.h1lucht;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 fix round 1 (fix-kern, playtest 1; reworked in merge_fix1 to fix-h1's new steps): what chapter 1 adds to the
 * story's hints and continuity, kept out of the minigame files.
 * <ul>
 *   <li>the kaasbol's spot for "Verder met het verhaal" ({@link E2Hervat#KAASBOL});</li>
 *   <li>a {@link StapHint} for every step inside the temple whose place is not obvious (steps {@value E2H1Lucht#HAL} to
 *       {@value E2H1Lucht#ZUKA}): a ~3 s look scene with one caption when the step starts, then one chat line with the
 *       direction. No markers of their own: fix-h1's steps already shine (the spirits, Guhatso, the start arch, the lit
 *       ring, Momo's trail, the saddle, the door, the pier). The targets follow fix-h1's mechanics:
 *       <ul>
 *         <li>{@value E2H1Lucht#ZWEVEN}: the wind carries the player up to the balcony by itself ({@code Training.naarBalkon},
 *             four seconds after the finish), so the hint waits for that and shows the FIRST RING; "Verder" puts the
 *             player on the balcony;</li>
 *         <li>{@value E2H1Lucht#MOMO}: the south door of the monnikentoren (Momo leads there; the hint waits until the wind
 *             set the player down on the square);</li>
 *         <li>{@value E2H1Lucht#STAL}: the stable's door (inside, the stable itself says what to click);</li>
 *         <li>{@value E2H1Lucht#ZUKA}: the pier (an escape that was broken off starts again there).</li>
 *       </ul></li>
 *   <li>the airbending lesson (playtest 1 item 11, "ik heb niet echt leren luchtsturen"): right after the luchtbal match
 *       Aang-guh says that the wind the player gave the ball WAS airbending, and asks them to try it without a ball (the
 *       power wheel: Luchtsprong, then Luchtbal); the first use of each gets his answer. Text only, no step: the chapter's
 *       steps, scenes and its flag {@code geleerd} at the end stay exactly as they were.</li>
 * </ul>
 * Texts: tools/features/einde2_h1_lucht_tekst.py (HINTS {@code gui.guhs.einde2_h1_lucht.hint.*}, LES {@code .les.*}, KIJK
 * the look scenes {@code scene.guhs.einde2_h1_lucht_kijk_*}).
 */
public final class H1Hints {
    static final String T = E2H1Lucht.P + "hint.", L = E2H1Lucht.P + "les.";
    /** The look scenes, one per hint (each with its own caption): {@code einde2_h1_lucht_kijk_<naam>}. */
    static final String[] KIJK = {"hal", "luchtbal", "scooter", "zweven", "momo", "stal", "deur", "zuka"};
    /** Ticks after the finish before the step-5 hint: the wind's lift to the balcony (80) and a breath on it. */
    static final int BALKON_WACHT = 110;
    /** Ticks after the last ring before the step-6 hint: the wind sets the player down on the square within 60. */
    static final int PLEIN_WACHT = 80;

    private H1Hints() {
    }

    private static Cutscene kijk(String naam) {
        return StapHint.kijkScene("einde2_h1_lucht_kijk_" + naam, true);
    }

    /** From E2H1LuchtFeature.register (common code: the look scenes must be known on both sides). */
    static void registreer() {
        E2Hervat.KAASBOL = Kaasbol::plek;
        String tempel = E2H1Lucht.STRUCTUUR;
        String lijn = E2Verhaal.LUCHT;
        // 2: after the arrival scene, the spirits in the Hal (their light columns are Tempel's)
        StapHint.naar(tempel, "roku_beeld").tekst(T + "hal").kijk(kijk("hal")).bijStart().zet(lijn, E2H1Lucht.HAL);
        // 3: after the scene in the Hal, monk Guhatso on the taartenterras
        StapHint.naar(tempel, "guhatso").tekst(T + "luchtbal").kijk(kijk("luchtbal")).bijStart().zet(lijn, E2H1Lucht.LUCHTBAL);
        // 4: the start arch (Training shines it); waits for Aang-guh's three lesson lines after the match
        StapHint.naar(tempel, "scooter_start").tekst(T + "scooter").kijk(kijk("scooter")).bijStart(LES_KLAAR + 20)
                .zet(lijn, E2H1Lucht.SCOOTER);
        // 5: the wind takes the player to the balcony by itself (Training.NAAR_BALKON, 80 ticks after the finish): show the
        //    first ring from there; "Verder" puts them on the balcony
        StapHint.naar(tempel, "zweefring_01").tekst(T + "zweven").kijk(kijk("zweven")).bijStart(BALKON_WACHT)
                .terug(tempel, "zweef_start").zet(lijn, E2H1Lucht.ZWEVEN);
        // 6: Momo leads to the monnikentoren; the hint waits until the wind set the player down on the square
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> zuidDeur(level, "monnikenverblijf")))
                .tekst(T + "momo").kijk(kijk("momo")).bijStart(PLEIN_WACHT).zet(lijn, E2H1Lucht.MOMO);
        // 7: after the scene momo, the stable's door (inside, Tempel names the next thing to click)
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> zuidDeur(level, "appa_stal")))
                .tekst(T + "stal").kijk(kijk("stal")).bijStart().zet(lijn, E2H1Lucht.STAL);
        // 8: the workshop door (its light column and the "deur.hier" line close by are Tempel's)
        StapHint.naar(tempel, "werkplaatsdeur").tekst(T + "deur").kijk(kijk("deur")).bijStart().zet(lijn, E2H1Lucht.DEUR);
        // 9: after the gong, the pier (also where a broken-off escape starts again)
        StapHint.naar(tempel, "steiger").tekst(T + "zuka").kijk(kijk("zuka")).bijStart().zet(lijn, E2H1Lucht.ZUKA);
        // the airbending lesson
        E2H1Lucht.LIJN.opStap((p, oud, nieuw) -> {
            if (oud == E2H1Lucht.LUCHTBAL && nieuw == E2H1Lucht.SCOOTER) {
                les(p);
            }
        });
        Krachten.NA_GEBRUIK.add(H1Hints::geoefend);
    }

    /** Just outside the middle of the south side of a zone marker of the temple, on its floor (that building's door). */
    @Nullable
    static BlockPos zuidDeur(ServerLevel level, String zone) {
        Plekken.Plek z = Plekken.plek(E2H1Lucht.STRUCTUUR, zone);
        BlockPos o = Plekken.bekend(level, E2H1Lucht.STRUCTUUR) ? Plekken.oorsprong(level, E2H1Lucht.STRUCTUUR) : null;
        if (z == null || z.tot() == null || o == null) {
            return null;
        }
        BlockPos a = z.pos(), b = z.tot();
        return o.offset((a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** When the last lesson line is said (ticks after the match). */
    static final int LES_KLAAR = 200;

    /** Aang-guh's three lines after the luchtbal match. */
    static void les(ServerPlayer p) {
        if (!E2H1Lucht.LIJN.eenmalig(p, "les_lucht")) {
            return;
        }
        StapHint.later(p, 60, s -> aang(s, Component.translatable(L + "1")));
        StapHint.later(p, 130, s -> aang(s, Component.translatable(L + "2", Component.keybind("key.guhs.einde2_kracht"))));
        StapHint.later(p, LES_KLAAR, s -> s.sendSystemMessage(Component.literal("* ").append(Component.translatable(L + "3"))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    /** The first Luchtsprong and the first Luchtbal after the lesson: Aang-guh's answer (once each). */
    static void geoefend(ServerPlayer p, Krachten.Kracht k) {
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap < E2H1Lucht.SCOOTER || stap >= E2H1Lucht.STAPPEN) {
            return;
        }
        if (k.id().equals(H1Krachten.LUCHTSPRONG) && E2H1Lucht.LIJN.eenmalig(p, "les_sprong")) {
            StapHint.later(p, 25, s -> aang(s, Component.translatable(L + "sprong")));
        } else if (k.id().equals(H1Krachten.LUCHTBAL) && E2H1Lucht.LIJN.eenmalig(p, "les_bal")) {
            StapHint.later(p, 15, s -> aang(s, Component.translatable(L + "bal")));
        }
    }

    /** "<Aang-guh> text" in the chat (he is not always near: his voice on the wind). */
    static void aang(ServerPlayer p, Component tekst) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc." + GuhNpcEntity.Kind.AANG_GUH.id()))
                .append("> ").withStyle(ChatFormatting.LIGHT_PURPLE).append(tekst.copy().withStyle(ChatFormatting.WHITE)));
    }
}
