package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.MeneerKrabguhEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.taal.NlTekst;

/**
 * einde2 "Sponsguh", slice sp_d2_heen (CONTRACT_E2_C 8.3): the core state machine of the stand-still game in De Ruige Kroeg
 * (moving while watched sends you back to your rest point facing the route, the step does not move; the checkpoints; the
 * key ends the game and arms the step's end), and the seams of the five steps (owned, variants, hints, the scenes' lengths
 * and THE ice-cream line of CONTRACT_E2_C 5.3 verbatim, every marker this slice names).
 */
public class E2SpD2HeenGameTests {
    private static final String BATCH = "einde2_sp_d2_heen";
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void einde2SpD2HeenKroeg(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        // De Ruige Kroeg's template laid round the test area: its route and table land a few blocks from the test's origin
        BlockPos o = helper.absolutePos(new BlockPos(0, 1, 0)).offset(-30, -16, -30);
        D2Plek.testOorsprong = (q, s) -> s.equals(D2Plek.SP) ? o : null;
        try {
            SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_SLEUTEL);
            List<Vec3> route = D2Plek.pad(p, D2Plek.SP, "kroeg_sluip");
            Vec3 sleutel = D2Plek.staan(p, D2Plek.SP, "sleutel");
            helper.assertTrue(route.size() >= 4 && sleutel != null, "the route and the key are markers");
            long nu = helper.getLevel().getServer().getTickCount() + 100000L;
            helper.assertTrue(Kroeg.start(p, nu) && Kroeg.actief(p) && Kroeg.fase(p) == Kroeg.Fase.RUST, "the game starts, resting");
            helper.assertTrue(p.position().distanceTo(route.get(0)) < 0.3, "at the swing gates");
            helper.assertTrue(SpStappenSleutel.van(p).equals("4_b"), "the variant 4_b while it runs");
            // walking while nobody looks: fine
            p.teleportTo(route.get(1).x, route.get(1).y, route.get(1).z);
            Kroeg.tick(p, ++nu);
            helper.assertTrue(Kroeg.stand(p)[0] == 0 && Kroeg.actief(p), "moving while nobody looks is fine");
            // moving while they look: back to the start, facing the route; the step stays
            Kroeg.zetFase(p, Kroeg.Fase.KIJKT, nu);
            p.teleportTo(route.get(1).x + 1.0, route.get(1).y, route.get(1).z);
            Kroeg.tick(p, ++nu);
            Vec3 naar = route.get(1);
            float yaw = D2Plek.yaw(route.get(0), naar);
            helper.assertTrue(Kroeg.stand(p)[0] == 1 && p.position().distanceTo(route.get(0)) < 0.3 && Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot() - yaw)) < 1,
                    "seen: back at the gates, facing in (yaw " + p.getYRot() + " want " + yaw + ")");
            helper.assertTrue(Kroeg.fase(p) == Kroeg.Fase.RUST && SpVerhaal.stap(p, SpVerhaal.KROON) == SpVerhaal.K_SLEUTEL, "a fresh rest, the step stays");
            // standing still while they look: nothing happens
            Kroeg.zetFase(p, Kroeg.Fase.KIJKT, nu);
            Kroeg.tick(p, ++nu);
            helper.assertTrue(Kroeg.stand(p)[0] == 1 && Kroeg.fase(p) == Kroeg.Fase.KIJKT, "standing still while watched: safe");
            Kroeg.zetFase(p, Kroeg.Fase.RUST, nu);
            // the first checkpoint: from now on you go back there
            Vec3 cp = route.get(Kroeg.RUSTPUNTEN[0]);
            p.teleportTo(cp.x, cp.y, cp.z);
            Kroeg.tick(p, ++nu);
            helper.assertTrue(Kroeg.stand(p)[1] == 1, "rest point 1 reached");
            Kroeg.zetFase(p, Kroeg.Fase.KIJKT, nu);
            p.teleportTo(cp.x + 0.6, cp.y, cp.z + 0.6);
            Kroeg.tick(p, ++nu);
            helper.assertTrue(Kroeg.stand(p)[0] == 2 && p.position().distanceTo(cp) < 0.3, "seen again: back at rest point 1, not at the gates");
            // the key: the game ends, the step's end is armed (the scene sleutel_terug moves the step on)
            p.teleportTo(sleutel.x, sleutel.y, sleutel.z);
            Kroeg.tick(p, ++nu);
            helper.assertTrue(!Kroeg.actief(p) && SpVerhaal.KROON_LIJN.vlag(p, SpD2Heen.SLEUTEL) && SpVerhaal.stap(p, SpVerhaal.KROON) == SpVerhaal.K_SLEUTEL,
                    "the key: the game is over, the key is in your hands, the step waits for its scene");
            // a game stops when the step is not this one any more
            SpVerhaal.KROON_LIJN.vlag(p, SpD2Heen.SLEUTEL, false);
            Kroeg.start(p, nu);
            SpVerhaal.KROON_LIJN.verder(p, SpVerhaal.K_SLEUTEL);
            helper.assertTrue(!Kroeg.actief(p) && !SpVerhaal.KROON_LIJN.vlag(p, SpD2Heen.SLEUTEL), "the step moved on: no game, no flag");
        } finally {
            D2Plek.testOorsprong = null;
            SpD2Heen.wisEigen(p, true);
            StapHint.vergeet(p.getUUID());
            SpVerhaalDev.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2SpD2HeenNaden(GameTestHelper helper) {
        String k = SpVerhaal.KROON;
        for (int s = SpVerhaal.K_MANAGER; s <= SpVerhaal.K_SLEUTEL; s++) {
            helper.assertTrue(SpStappen.isEcht(k, s), "step " + s + " is this slice's");
            helper.assertTrue(StapHint.hint(k, s) != null, "step " + s + " has a hint");
            String[] m = D2Hints.MARKERS.get(s);
            helper.assertTrue(m != null && Plekken.van(m[0]).containsKey(m[1]) && Plekken.van(m[0]).containsKey(m[2]), "step " + s + ": its hint's markers");
        }
        // step 5 (SNOR) is d2_weg's: merged into the same tree, it is real too (no stand-in), but this slice gives it no hint markers
        helper.assertTrue(SpStappen.isEcht(k, SpVerhaal.K_SNOR) && !D2Hints.MARKERS.containsKey(SpVerhaal.K_SNOR), "step 5 is d2_weg's, not this slice's");
        // every marker this slice names in its builds and scenes
        for (String id : List.of("d2h_knabbel", "d2h_zaal", "d2h_manager_camera", "d2h_ijs_camera", "d2h_plein_camera", "d2h_mobiel_camera",
                "ijs_speler", "ijs_sponsguh", "ijs_patguh", "ijsverkoper", "knipoog_ijs_coupe", "knipoog_ijs_balto", "knipoog_ijs_kistje",
                "neptuguh_troon", "bevroren_krabguh", "mobiel_start", "stadspoort", "octo_kassa", "klant_1", "klant_2", "ijssalon", "krabguh")) {
            helper.assertTrue(Plekken.van(D2Plek.BB).containsKey(id), "bikinibodem/" + id);
        }
        for (String id : List.of("d2h_kroeg_camera", "d2h_binnen_camera", "d2h_sponsguh_deur", "d2h_patguh_deur", "kroeg_parkeer", "kroeg_sluip",
                "sleutel", "rustpunt_1", "ruige_kroeg", "weg_naad", "barman", "uitsmijter", "slapende_mika")) {
            helper.assertTrue(Plekken.van(D2Plek.SP).containsKey(id), "schelpenstad/" + id);
        }
        for (String m : Kroeg.MIKAS.keySet()) {
            helper.assertTrue(Plekken.van(D2Plek.SP).containsKey(m), "schelpenstad/" + m);
        }
        // the scenes: registered, their lengths, a text for every line, a speaker name
        for (Map.Entry<String, int[]> e : D2Scenes.LENGTES.entrySet()) {
            Cutscene s = D2Scenes.van(e.getKey());
            helper.assertTrue(s != null && SpD2Heen.ANKERS.containsKey(e.getKey()), "scene " + e.getKey());
            double sec = s.duur() / 20.0;
            helper.assertTrue(sec >= e.getValue()[0] && sec <= e.getValue()[1], "scene " + e.getKey() + " lasts " + sec + " s, not " + e.getValue()[0] + "-" + e.getValue()[1]);
            helper.assertTrue(NlTekst.has(s.titelKey()), "scene " + e.getKey() + ": a title");
            for (Cutscene.Zeg z : s.zinnen()) {
                helper.assertTrue(!z.lied() && NlTekst.has(s.tekstKey(z.key())), "scene " + e.getKey() + ": the line " + z.key());
                helper.assertTrue(NlTekst.has(s.tekstKey("naam." + z.spreker())), "scene " + e.getKey() + ": the speaker " + z.spreker());
            }
        }
        // CONTRACT_E2_C 5.3: the user's line, verbatim, spoken by Sponsguh in the ice-cream scene
        Cutscene ijs = D2Scenes.IJSSALON;
        helper.assertTrue(ijs.zinnen().stream().anyMatch(z -> z.key().equals("poppetje") && z.spreker().equals("sponsguh"))
                && "zeg.. hoort hier geen poppetje bij?".equals(NlTekst.get(ijs.tekstKey("poppetje"))), "the ice-cream line is the user's own");
        helper.assertTrue(nl.juiced.guhs.feature.verhaal.Verteller.van(SpD2Heen.KAART) != null
                && NlTekst.has("gui.guhs.verhaal.kaart." + SpD2Heen.KAART + ".regel.3"), "the narrator card of part 2 with 4 lines");
        Set<String> klik = Set.of("sponsguh", "patguh", "meneer_krabguh", "octo_mika");
        SpD2Heen.KLIKKEN.forEach((cast, stappen) -> stappen.forEach(s -> helper.assertTrue(klik.contains(cast)
                && NlTekst.has(SpD2Heen.G + "klik." + cast + "." + s), "the click line " + cast + "." + s)));
        helper.succeed();
    }

    /**
     * (fix 1.5.0) The frozen Krabguh on the square: an old resident (a mark without the strict flag) gets today's rule when it
     * joins the level; seen only from the freeze (K_RIT) to the finale (K_GUHFY), also not by a creative operator; and a
     * click on him never reaches the story listeners of the real Meneer Krabguh (part 1 step 3, the job interview, matched
     * by cast), while the real one still does.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2SpD2HeenIjsKrabguh(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        MeneerKrabguhEntity ijs = E2SponsguhFeature.MENEER_KRABGUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        MeneerKrabguhEntity echt = E2SponsguhFeature.MENEER_KRABGUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        int[] gehoord = {0, 0};
        java.util.function.BiFunction<SponsguhWezen, ServerPlayer, Boolean> proef = (w, q) -> {
            if (q == p && w == ijs) {
                gehoord[0]++;
            } else if (q == p && w == echt) {
                gehoord[1]++;
                return true;
            }
            return false;
        };
        try {
            Vec3 a = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
            ijs.snapTo(a.x, a.y, a.z, 0f, 0f);
            echt.snapTo(a.x + 3, a.y, a.z, 0f, 0f);
            // an old resident: the one-range mark of before 1.5.0, not frozen yet, no strict flag
            ijs.getPersistentData().putString(nl.juiced.guhs.feature.einde2.Bewoners.TAG, "einde2_" + D2Plek.BB + "_bevroren_krabguh@0");
            Zicht.alleenBij(ijs, SpVerhaal.KROON, SpVerhaal.K_RIT, SpVerhaal.K_GUHFY);
            level.addFreshEntity(ijs);
            level.addFreshEntity(echt);
            helper.assertTrue(ijs.isBevroren() && ijs.entityTags().contains(Zicht.STRENG) && ijs.entityTags().contains(Zicht.MERK),
                    "joining the level: frozen, marked and strict");
            // part 1, step 3 (the job interview): not there
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, SpVerhaal.R_SOLLICITATIE);
            helper.assertTrue(!Zicht.magZien(p, ijs) && Zicht.magZien(p, echt), "part 1: no frozen Krabguh, the real one is there");
            // the click: not one story listener of the real Krabguh is asked for the ice; the real one still reaches them
            SponsguhWezen.BIJ_KLIK.add(0, proef);
            ijs.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND);
            echt.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(gehoord[0] == 0 && gehoord[1] == 1, "the ice is never the real Krabguh for a step: " + gehoord[0] + ", " + gehoord[1]);
            helper.assertTrue(SpVerhaal.stap(p, SpVerhaal.RECEPT) == SpVerhaal.R_SOLLICITATIE, "the job interview did not start from the ice");
            // part 2: before the freeze not, from K_RIT to the finale yes, after it not
            int[][] wanneer = {{SpVerhaal.K_MANAGER, 0}, {SpVerhaal.K_KROON, 0}, {SpVerhaal.K_RIT, 1}, {SpVerhaal.K_EMMERS, 1},
                    {SpVerhaal.K_GUHFY, 1}, {SpVerhaal.K_STAPPEN, 0}};
            for (int[] w : wanneer) {
                SpVerhaalDev.zetTot(p, SpVerhaal.KROON, w[0]);
                helper.assertTrue(Zicht.magZien(p, ijs) == (w[1] == 1), "part 2 step " + w[0] + ": frozen Krabguh seen " + (w[1] == 1));
            }
            // strict: a creative operator does not see him outside his steps either (spectators see everything)
            p.setGameMode(GameType.CREATIVE);
            if (nl.juiced.guhs.feature.verhaal.Sluiers.passeert(p)) {
                helper.assertTrue(!Zicht.magZien(p, ijs) && Zicht.magZien(p, echt), "creative operator after the finale: no frozen Krabguh");
            }
            p.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(Zicht.magZien(p, ijs), "a spectator sees him");
        } finally {
            SponsguhWezen.BIJ_KLIK.remove(proef);
            ijs.discard();
            echt.discard();
            SpVerhaalDev.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /** The objective sleutel of the player's current step of part 2 (the line asks SpStappen). */
    private static final class SpStappenSleutel {
        static String van(ServerPlayer p) {
            return SpVerhaal.KROON_LIJN.sleutel(p);
        }
    }
}
