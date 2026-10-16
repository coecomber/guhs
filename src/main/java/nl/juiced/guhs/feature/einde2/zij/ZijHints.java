package nl.juiced.guhs.feature.einde2.zij;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 fix round 1, stage 2 (fix2-geestzij, playtest 1: "ietsjes meer hints"): the step hints of the eight side lines
 * ({@link StapHint}). Every step whose spot is not obvious points at it: a chat line with the direction and a landmark when
 * the step starts, a soft column of light while it is on, and where the spot is out of view a 3 s look scene first.
 * <ul>
 *   <li>A side place may stand more than once: a hint points into the copy the player is in or near ({@link ZijPlek#bij}),
 *       else into the guaranteed copy ({@link Plekken#wereld}); "Verder met het verhaal" (the kaasbol, when a side line is
 *       the followed one) brings the player to that spot of the guaranteed copy.</li>
 *   <li>The Mika-vesting is a jigsaw building without one known copy: its steps point at the cook or the captain of a
 *       vesting the player is near (within {@value #VESTING_BEREIK} blocks), else at nothing (the step's own text then).</li>
 *   <li>Step 0 of each line starts with a talk at its resident: there is no step start before it (and the player stands
 *       there already), so step 0 shows no hint by itself; its hint still serves the compass, "Verder" and the light.</li>
 * </ul>
 * Texts: {@code gui.guhs.einde2_zij.hint.*} and {@code scene.guhs.einde2_zij_kijk_*} (tools/features/einde2_zij_tekst.py
 * HINTS / KIJK).
 */
public final class ZijHints {
    static final String T = "gui.guhs.einde2_zij.hint.";
    static final String K = "einde2_zij_kijk_";
    static final int VESTING_BEREIK = 96;

    private ZijHints() {
    }

    /** A marker of a side place: in the copy the player is in or near, else in the guaranteed copy. */
    static StapHint.Doel bij(String structuur, String plek) {
        return bij(structuur, (p, waar) -> plek);
    }

    /**
     * The same with the marker chosen per player ({@code waar} gives the world block of a marker in the copy that counts;
     * the chooser returns the marker id, null: nothing to point at now).
     */
    static StapHint.Doel bij(String structuur, BiFunction<ServerPlayer, Function<String, BlockPos>, String> plek) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            ZijPlek.Kopie k = p.level() == level ? ZijPlek.bij(p, structuur, 96) : null;
            Function<String, BlockPos> waar = k != null ? k::pos
                    : id -> Plekken.bekend(level, structuur) ? Plekken.wereld(level, structuur, id) : null;
            String m = plek.apply(p, waar);
            return m == null ? null : waar.apply(m);
        });
    }

    /** The nearest resident of this kind with a spot of the Mika-vesting near the player (the jigsaw building). */
    static StapHint.Doel vesting(GuhNpcEntity.Kind kind, String plek) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            if (p.level() != level) {
                return null;
            }
            Entity best = null;
            double bestD = Double.MAX_VALUE;
            String naam = Einde2Ids.MIKA_VESTING + "/" + plek;
            for (GuhNpcEntity n : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(p.blockPosition()).inflate(VESTING_BEREIK),
                    n -> n.isAlive() && n.getKind() == kind && naam.equals(Zij.plek(n)))) {
                double d = n.distanceToSqr(p);
                if (d < bestD) {
                    bestD = d;
                    best = n;
                }
            }
            return best == null ? null : best.blockPosition();
        });
    }

    /** From E2ZijFeature.register (common code: the look scenes must be known on both sides), after the lines. */
    static void registreer() {
        Cutscene omash1 = kijk("omash_proef1"), omash2 = kijk("omash_proef2"), omash3 = kijk("omash_proef3"), glijbaan = kijk("omash_glijbaan");
        Cutscene windfluit = kijk("lucht_windfluit"), kudde = kijk("lucht_kudde");
        Cutscene deuren = kijk("vuur_deuren"), heiligdom = kijk("vuur_heiligdom");
        Cutscene dans = kijk("kyoshi_dans"), schmink = kijk("kyoshi_schmink"), fan = kijk("kyoshi_fan");
        Cutscene vos = kijk("bieb_vos"), planetarium = kijk("bieb_planetarium");
        Cutscene top = kijk("schildpad_top"), poot = kijk("schildpad_poot"), snuit = kijk("schildpad_kop");
        Cutscene havenkok = kijk("haven_kok"), werf = kijk("haven_werf");

        // Omashguh: the three trials under the throne hall, then the chutes before the palace
        String o = ZijLijnen.OMASHGUH, os = Einde2Ids.OMASHGUH;
        StapHint.naar(bij(os, "bumi")).tekst(T + "omash.0").zonderKijk().baken().zet(o, 0);
        StapHint.naar(bij(os, "proef_1_start")).tekst(T + "omash.1").kijk(omash1).bijStart(50).baken().zet(o, 1);
        StapHint.naar(bij(os, "proef_2_start")).tekst(T + "omash.2").kijk(omash2).bijStart(50).baken().zet(o, 2);
        StapHint.naar(bij(os, "proef_3_start")).tekst(T + "omash.3").kijk(omash3).bijStart(50).baken().zet(o, 3);
        StapHint.naar(bij(os, "glijbaan_start")).tekst(T + "omash.4").kijk(glijbaan).bijStart(50).baken().zet(o, 4);

        // the Western Air Temple: the nearest silent corridor, the nearest stray of the herd
        String l = ZijLijnen.LUCHTTEMPEL, ls = Einde2Ids.WESTELIJKE_LUCHTTEMPEL;
        StapHint.naar(bij(ls, "luchtnon")).tekst(T + "lucht.0").zonderKijk().baken().zet(l, 0);
        StapHint.naar(bij(ls, (p, w) -> dichtste(p, w, "windfluit_", i -> !Luchttempel.ln().vlag(p, "fluit_" + i))))
                .tekst(T + "lucht.1").kijk(windfluit).bijStart(50).baken().zet(l, 1);
        StapHint.naar(bij(ls, (p, w) -> dichtste(p, w, "kudde_", i -> !Luchttempel.ln().vlag(p, "kudde_" + i))))
                .tekst(T + "lucht.2").kijk(kudde).bijStart(50).baken().terug(bij(ls, "aankomst_speler")).zet(l, 2);
        StapHint.naar(bij(ls, "luchtnon")).tekst(T + "lucht.3").zonderKijk().bijStart(50).baken().zet(l, 3);

        // the Fire Temple of Roku-guh: the next unlit door at the top of the tower, the sanctum, Roku-guh
        String v = ZijLijnen.VUURTEMPEL, vs = Einde2Ids.VUURTEMPEL_ROKU;
        StapHint.naar(bij(vs, "shyu")).tekst(T + "vuur.0").zonderKijk().baken().zet(v, 0);
        StapHint.naar(bij(vs, (p, w) -> dichtste(p, w, "vuurdeur_", i -> !Vuurtempel.ln().vlag(p, "deur_" + i))))
                .tekst(T + "vuur.1").kijk(deuren).bijStart(50).baken().zet(v, 1);
        StapHint.naar(bij(vs, "vuurdeur_3")).tekst(T + "vuur.2").kijk(heiligdom).bijStart(50).baken().zet(v, 2);
        StapHint.naar(bij(vs, "roku")).tekst(T + "vuur.3").zonderKijk().bijStart(60).zet(v, 3);

        // Kyoshi Village: the golden tile in the dojo, the make-up table, the fan at the gate
        String y = ZijLijnen.KYOSHI, ys = Einde2Ids.KYOSHI_DORP;
        StapHint.naar(bij(ys, "krijgster_leider")).tekst(T + "kyoshi.0").zonderKijk().baken().zet(y, 0);
        StapHint.naar(bij(ys, "dans_speler")).tekst(T + "kyoshi.1").kijk(dans).bijStart(40).baken().zet(y, 1);
        StapHint.naar(bij(ys, "schminktafel")).tekst(T + "kyoshi.2").kijk(schmink).bijStart(50).baken().zet(y, 2);
        StapHint.naar(bij(ys, "fan_guh")).tekst(T + "kyoshi.3").kijk(fan).bijStart(50).baken().zet(y, 3);

        // the Library of Wan Shi Guh: the owl at the bottom, a fox guh, the planetarium
        String b = ZijLijnen.BIBLIOTHEEK, bs = Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH;
        StapHint.naar(bij(bs, "wan_shi_guh")).tekst(T + "bieb.0").zonderKijk().baken().terug(bij(bs, "ingang_top")).zet(b, 0);
        StapHint.naar(bij(bs, "wan_shi_guh")).tekst(T + "bieb.1").zonderKijk().baken().zet(b, 1);
        StapHint.naar(bij(bs, "vosguh_3")).tekst(T + "bieb.2").kijk(vos).bijStart(50).baken().zet(b, 2);
        StapHint.naar(bij(bs, "planetarium_wiel")).tekst(T + "bieb.3").kijk(planetarium).bijStart(50).baken().zet(b, 3);

        // the Lion Turtle: over the shell, the raised paw, his snout
        String s = ZijLijnen.LEEUWSCHILDPAD, ss = Einde2Ids.LEEUWSCHILDPAD_EILAND;
        StapHint.naar(bij(ss, "schild_top")).tekst(T + "schildpad.0").kijk(top).baken().zet(s, 0);
        StapHint.naar(bij(ss, "zegen_plek")).tekst(T + "schildpad.1").kijk(poot).bijStart(50).baken().zet(s, 1);
        StapHint.naar(bij(ss, "kop")).tekst(T + "schildpad.2").kijk(snuit).bijStart(60).baken().terug(bij(ss, "zegen_plek")).zet(s, 2);

        // the sorry Mika cook: a vesting near you (jigsaw), then his own kitchen in the harbour
        String m = ZijLijnen.MIKA_KOK, hs = Einde2Ids.LEEGTEHAVEN;
        StapHint.naar(vesting(GuhNpcEntity.Kind.MIKA_KOK, "kok")).tekst(T + "kok.0").zonderKijk().baken().zet(m, 0, 3);
        StapHint.naar(vesting(GuhNpcEntity.Kind.VESTINGKAPITEIN_MIKA, "kapitein")).tekst(T + "kok.2").zonderKijk().bijStart(50).baken().zet(m, 2);
        StapHint.naar(bij(hs, "mika_kok")).tekst(T + "kok.4").kijk(havenkok).bijStart(60).baken().zet(m, 4);

        // the vetbootje: the shipwright of the harbour
        StapHint.naar(bij(hs, "scheepsbouwer")).tekst(T + "bootje.1").kijk(werf).bijStart(60).baken().zet(ZijLijnen.VETBOOTJE, 1, 2);
    }

    private static Cutscene kijk(String naam) {
        return StapHint.kijkScene(K + naam, true);
    }

    /** The nearest of the numbered markers {@code <prefix>1..5} of this copy that still counts (null: none). */
    @Nullable
    static String dichtste(ServerPlayer p, Function<String, BlockPos> waar, String prefix, java.util.function.IntPredicate telt) {
        String best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 1; i <= 5; i++) {
            if (!telt.test(i)) {
                continue;
            }
            BlockPos b = waar.apply(prefix + i);
            if (b == null) {
                continue;
            }
            double d = Vec3.atCenterOf(b).distanceToSqr(p.position());
            if (d < bestD) {
                bestD = d;
                best = prefix + i;
            }
        }
        return best;
    }
}
