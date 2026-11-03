package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.erfenis.sherlock.Voortgang.Status;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Getuige;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Kaart;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/** The Sherlock story: the cases' data and texts, the linear order and its moments, saving, the board, the courtroom, clue spots. */
public class SherlockGameTests {
    private static final String BATCH = "sherlock";

    private static boolean has(String key) {
        return Language.getInstance().has(key);
    }

    /** Every case is well-formed (one red herring, enough matching cards, known suspects/witnesses/items) and has all its texts. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sherlockZakenKloppen(GameTestHelper helper) {
        List<String> mis = new ArrayList<>();
        helper.assertTrue(Zaken.ALLE.size() == 11, "eleven chapters");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < Zaken.ALLE.size(); i++) {
            Zaak z = Zaken.ALLE.get(i);
            helper.assertTrue(z.nr() == i + 1 && ids.add(z.id()), z.id() + ": numbered in order, unique");
            for (String k : List.of("titel", "kort", "wacht")) {
                need(mis, "sherlock.zaak." + z.id() + "." + k);
            }
            if (z.finale()) {
                continue;
            }
            for (String g : List.of("intro", "oplossing")) {
                List<String> sprekers = Regels.sprekers("zaak." + z.id() + "." + g);
                helper.assertTrue(!sprekers.isEmpty(), z.id() + ": dialogue " + g);
                for (int n = 0; n < sprekers.size(); n++) {
                    need(mis, "sherlock.zaak." + z.id() + "." + g + "." + n);
                    String wie = sprekers.get(n).replace("!", "");
                    if (!wie.isEmpty() && Sherlock.kindVan(wie) == null) {
                        need(mis, "sherlock.wie." + wie);
                    }
                }
            }
            Set<String> kaarten = new HashSet<>();
            for (Kaart k : z.kaarten()) {
                helper.assertTrue(kaarten.add(k.id()), z.id() + ": unique card " + k.id());
                need(mis, "sherlock.zaak." + z.id() + ".kaart." + k.id());
                need(mis, "sherlock.zaak." + z.id() + ".kaart." + k.id() + ".tekst");
                helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(k.icoon())) != Items.AIR, z.id() + ": icon " + k.icoon());
                if (k.getuige() != null) {
                    helper.assertTrue(z.getuige(k.getuige()) != null, z.id() + ": the witness of " + k.id() + " stands in the village");
                } else {
                    helper.assertTrue(k.x() >= 0 && k.x() < Plot.MAAT && k.z() >= 0 && k.z() < Plot.MAAT, z.id() + ": " + k.id() + " lies on the plot");
                }
            }
            for (Getuige g : z.getuigen()) {
                for (String vraag : z.verzamelZaak() ? List.of("hallo", "gezien", "waar") : List.of("hallo", "gezien", "waar", "toon")) {
                    need(mis, "sherlock.zaak." + z.id() + ".getuige." + g.id() + "." + vraag);
                }
                need(mis, "sherlock.wie." + g.id());
                helper.assertTrue(z.kaarten().stream().anyMatch(k -> g.id().equals(k.getuige())), z.id() + ": witness " + g.id() + " gives a card");
            }
            if (z.verzamelZaak()) {
                helper.assertTrue(z.verdachten().isEmpty() && z.kaarten().stream().allMatch(k -> k.getuige() != null), "chapter 10 only collects testimonies");
                continue;
            }
            long haringen = z.kaarten().stream().filter(k -> !k.goed()).count();
            long goed = z.kaarten().stream().filter(Kaart::goed).count();
            helper.assertTrue(haringen == 1 && goed >= 3, z.id() + ": one red herring and at least three matching cards");
            helper.assertTrue(z.verdachten().contains(z.dader()), z.id() + ": the culprit is a suspect");
            helper.assertTrue(z.glasKaarten().size() >= 3, z.id() + ": clues to find with the glass");
            for (Kaart k : z.kaarten()) {
                if (!k.goed()) {
                    helper.assertTrue(z.verdachten().contains(k.wijstNaar()) && !k.wijstNaar().equals(z.dader()), z.id() + ": the herring points at an innocent suspect");
                }
            }
            for (String v : z.verdachten()) {
                need(mis, "sherlock.wie." + v);
                helper.assertTrue(Zaken.WIE.containsKey(v), z.id() + ": a polaroid for " + v);
                if (!v.equals(z.dader())) {
                    need(mis, "sherlock.zaak." + z.id() + ".fout." + v);
                }
            }
        }
        for (String icoon : Zaken.WIE.values()) {
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(icoon)) != Items.AIR, "polaroid icon " + icoon);
        }
        for (String g : List.of("welkom", "proloog")) {
            for (int n = 0; n < Regels.sprekers(g).size(); n++) {
                need(mis, "sherlock." + g + "." + n);
            }
            helper.assertTrue(!Regels.sprekers(g).isEmpty(), "dialogue " + g);
        }
        for (Zaak z : Zaken.ALLE) {
            for (String b : z.beloningen()) {
                if (Zaken.DECO.contains(b)) {
                    need(mis, "sherlock.deco." + b);
                } else if (!b.equals("detective_pet")) {
                    need(mis, "sherlock.beloning." + b);
                }
            }
        }
        for (String b : Zaken.BEWIJS) {
            need(mis, "sherlock.bewijs." + b);
            need(mis, "sherlock.rechtbank.fout." + b);
        }
        for (int r = 1; r <= 3; r++) {
            need(mis, "sherlock.rechtbank.bewering." + r);
            need(mis, "sherlock.rechtbank.hint." + r);
        }
        for (int n = 0; n < Guhrrit.ZINNEN; n++) {
            need(mis, "sherlock.guhrrit." + n);
        }
        for (String t : Zaken.TITELS.values()) {
            helper.assertTrue(Titels.van(t) != null, "title " + t + " registered");
        }
        for (Cutscene s : List.of(SherlockScenes.BEGIN, SherlockScenes.BEZWAAR.get(0), SherlockScenes.BEZWAAR.get(1), SherlockScenes.BEZWAAR.get(2))) {
            helper.assertTrue(Cutscene.van(s.id()) == s, "scene " + s.id() + " registered");
            need(mis, "scene.guhs." + s.id() + ".titel");
        }
        helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:detective_pet")) != Items.AIR, "the ruitjespet reward exists");
        helper.assertTrue(mis.isEmpty(), "missing texts: " + mis);
        helper.succeed();
    }

    private static void need(List<String> mis, String key) {
        if (!has(key)) {
            mis.add(key);
        }
    }

    /** Linear: a case is ready only when the one before is solved AND its moment came; chapter 10 follows 9 at once; 11 waits for Hoeve 9. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sherlockVolgorde(GameTestHelper helper) {
        Voortgang v = new Voortgang();
        helper.assertTrue(v.status(Zaken.EIEREN) == Status.WACHT, "nothing before the Detectivebureau");
        v.gebouwd.add("bakkerij");
        v.hoeve = 5;
        helper.assertTrue(v.status(Zaken.KRUIMELS) == Status.WACHT, "chapter 2 waits for chapter 1 even when the bakery stands");
        v.gebouwd.add("detectivebureau");
        helper.assertTrue(v.status(Zaken.EIEREN) == Status.KLAAR && v.huidige() == Zaken.EIEREN, "the bureau: chapter 1 is ready");
        v.opgelost.add("eieren");
        helper.assertTrue(v.status(Zaken.KRUIMELS) == Status.KLAAR, "then chapter 2 (the bakery was already there)");
        v.opgelost.add("kruimels");
        helper.assertTrue(v.status(Zaken.SPOOK) == Status.KLAAR, "Hoeve 4 reached earlier: chapter 3 waits in the letterbox");
        v.opgelost.add("spook");
        v.gebouwd.add("weverij");
        v.bestellingen = 9;
        helper.assertTrue(v.status(Zaken.WOL) == Status.WACHT, "chapter 4 needs 10 orders too");
        v.bestellingen = 10;
        helper.assertTrue(v.status(Zaken.WOL) == Status.KLAAR, "... and then it is ready");
        for (String id : List.of("wol", "kelder", "koeienbel", "paaltjes", "licht")) {
            v.opgelost.add(id);
        }
        v.hoeve = 7;
        helper.assertTrue(v.status(Zaken.BLIKJE) == Status.WACHT, "chapter 9 waits for Hoeve 8");
        v.hoeve = 8;
        v.opgelost.add("blikje");
        helper.assertTrue(v.status(Zaken.VOORBEREIDING) == Status.KLAAR, "chapter 10 follows chapter 9 at once");
        v.opgelost.add("voorbereiding");
        helper.assertTrue(v.status(Zaken.RECHTSZAAK) == Status.WACHT, "the courtroom waits for Hoeve 9");
        v.hoeve = 9;
        helper.assertTrue(v.status(Zaken.RECHTSZAAK) == Status.KLAAR, "Hoeve 9: to the courtroom");
        v.actief = "rechtszaak";
        helper.assertTrue(v.status(Zaken.RECHTSZAAK) == Status.BEZIG, "running");
        v.opgelost.add("rechtszaak");
        helper.assertTrue(v.huidige() == null, "the whole story done");
        helper.succeed();
    }

    /** Progress survives saving: moments, solved cases with their boards, the running case. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sherlockOpslaan(GameTestHelper helper) {
        Voortgang v = new Voortgang();
        v.gebouwd.add("detectivebureau");
        v.hoeve = 4;
        v.bestellingen = 12;
        v.sleuteltje = true;
        v.bureau = true;
        v.opgelost.add("eieren");
        v.eersteKeer.add("eieren");
        v.borden.put("eieren", java.util.Map.of("rood_veertje", "hendrik"));
        v.beloningen.add("hartjesnest");
        v.actief = "kruimels";
        v.gevonden.add("kruimelspoor");
        v.draadjes.put("kruimelspoor", "babyguhs");
        v.rood = "meelzak";
        v.foutGeraden = true;
        v.ronde = 2;
        Voortgang w = Voortgang.load(v.save());
        helper.assertTrue(w.gebouwd.contains("detectivebureau") && w.hoeve == 4 && w.bestellingen == 12 && w.sleuteltje && w.bureau, "moments");
        helper.assertTrue(w.opgelost.contains("eieren") && w.eersteKeer.contains("eieren") && "hendrik".equals(w.borden.get("eieren").get("rood_veertje"))
                && w.beloningen.contains("hartjesnest"), "solved cases and rewards");
        helper.assertTrue("kruimels".equals(w.actief) && w.gevonden.contains("kruimelspoor") && "babyguhs".equals(w.draadjes.get("kruimelspoor"))
                && "meelzak".equals(w.rood) && w.foutGeraden && w.ronde == 2, "the running case");
        helper.succeed();
    }

    /** A whole case with a player: the bureau's moment hands out the glass, wrong guesses only give hints, three right cards solve it. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void sherlockZaakSpelen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        UUID id = p.getUUID();
        try {
            Sherlock.opMoment(p, "vondst:roestige_weerhaan");
            Voortgang v = SherlockData.van(helper.getLevel().getServer(), id);
            helper.assertTrue(v.sleuteltje, "the prologue: the little key");
            Sherlock.opMoment(p, "gebouw_gebouwd:detectivebureau");
            helper.assertTrue(v.bureau && Vergrootglas.heeft(p), "Sherlock moved in and handed over the vergrootglas");
            helper.assertTrue(Sherlock.zaakKlaar(helper.getLevel().getServer(), id), "a case in the letterbox");
            Sherlock.start(p, "kruimels");
            helper.assertTrue(v.actief == null, "chapter 2 can't start before chapter 1");
            Sherlock.start(p, "eieren");
            helper.assertTrue("eieren".equals(v.actief) && v.status(Zaken.EIEREN) == Status.BEZIG, "chapter 1 runs");
            for (Kaart k : Zaken.EIEREN.glasKaarten()) {
                Sherlock.vondst(p, Zaken.EIEREN, v, k);
            }
            Sherlock.opGetuigeAntwoord(p, null, 1);   // (no witness entity: nothing happens)
            v.gevonden.add("bonnie_zuchten");
            helper.assertTrue(Sherlock.alleKaarten(Zaken.EIEREN, v), "all cards found");
            Sherlock.bordActie(p, "pin", "rood_veertje", "nobody");
            helper.assertTrue(v.draadjes.isEmpty(), "a thread only goes to a suspect of the case");
            // three cards on the wrong suspect: a sweet answer, nothing lost
            for (String k : List.of("rood_veertje", "pootafdrukjes", "hartjesschaal")) {
                Sherlock.bordActie(p, "pin", k, "pim");
            }
            Sherlock.bordActie(p, "wijs", "pim", "");
            helper.assertTrue(!v.isOpgelost("eieren") && v.foutGeraden && v.gevonden.size() == 5, "wrong: not solved, all cards kept");
            // the culprit, but with the red herring among the threads: Vadson tints it red
            Sherlock.bordActie(p, "pin", "rood_veertje", "hendrik");
            Sherlock.bordActie(p, "pin", "pootafdrukjes", "hendrik");
            Sherlock.bordActie(p, "pin", "stagiairspasje", "hendrik");
            Sherlock.bordActie(p, "wijs", "hendrik", "");
            helper.assertTrue(!v.isOpgelost("eieren") && "stagiairspasje".equals(v.rood), "the herring is tinted red");
            Sherlock.bordActie(p, "los", "stagiairspasje", "");
            helper.assertTrue(v.rood == null, "taking its thread away clears the red");
            Sherlock.bordActie(p, "wijs", "hendrik", "");
            helper.assertTrue(!v.isOpgelost("eieren"), "two threads are not enough");
            Sherlock.bordActie(p, "pin", "hartjesschaal", "hendrik");
            Sherlock.bordActie(p, "wijs", "hendrik", "");
            helper.assertTrue(v.isOpgelost("eieren") && v.actief == null && !v.eersteKeer.contains("eieren"), "solved (no sticker after a wrong guess)");
            helper.assertTrue(v.beloningen.contains("hartjesnest") && v.borden.get("eieren").size() == 3, "the reward and the board in the Speurboekje");
            helper.assertTrue(Titels.van("sherlock_speurneus").behaald().test(p), "the title is earned");
            helper.assertTrue(v.status(Zaken.KRUIMELS) == Status.WACHT, "chapter 2 waits for the bakery");
            Sherlock.opMoment(p, "gebouw_gebouwd:bakkerij");
            helper.assertTrue(v.status(Zaken.KRUIMELS) == Status.KLAAR, "... and then it is ready");
            // chapter 2 solved in one go: the sticker
            Sherlock.start(p, "kruimels");
            Zaken.KRUIMELS.kaarten().forEach(k -> v.gevonden.add(k.id()));
            for (String k : List.of("kruimelspoor", "handjes_in_meel", "meelspoken")) {
                Sherlock.bordActie(p, "pin", k, "babyguhs");
            }
            Sherlock.bordActie(p, "wijs", "babyguhs", "");
            helper.assertTrue(v.isOpgelost("kruimels") && v.eersteKeer.contains("kruimels") && v.beloningen.contains("recept_theetaartje"),
                    "in one go: the sticker and the recipe flag");
            helper.assertTrue(Sherlock.beloningen(helper.getLevel().getServer(), id).contains("theekransje_tafeltje"), "the rewards for the other slices");
            CompoundCheck.bord(helper, Sherlock.staat(helper.getLevel().getServer(), id, "bord", Zaken.KRUIMELS));
        } finally {
            SherlockData.get(helper.getLevel().getServer()).vergeet(id);
            Cast.vergeet(helper.getLevel().getServer(), id);
        }
        helper.succeed();
    }

    /** The screen's state of a solved case shows its board (all cards, the threads, the culprit). */
    private static final class CompoundCheck {
        static void bord(GameTestHelper helper, net.minecraft.nbt.CompoundTag staat) {
            net.minecraft.nbt.CompoundTag b = staat.getCompoundOrEmpty("Bord");
            helper.assertTrue(b.getBooleanOr("Opgelost", false) && "babyguhs".equals(b.getStringOr("Dader", "")), "the board says solved and who");
            helper.assertTrue(b.getListOrEmpty("Kaarten").size() == Zaken.KRUIMELS.kaarten().size() && b.getCompoundOrEmpty("Draadjes").size() == 3,
                    "all cards and the three threads");
            helper.assertTrue(staat.getListOrEmpty("Zaken").size() == 11, "the Speurboekje lists every chapter");
        }
    }

    /** Chapter 10 is solved by talking to every witness; the courtroom wants the right evidence three times, wrong is just a hint. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void sherlockVoorbereidingEnRechtbank(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        UUID id = p.getUUID();
        try {
            Sherlock.forceer(p, Zaken.VOORBEREIDING);
            Voortgang v = SherlockData.van(helper.getLevel().getServer(), id);
            helper.assertTrue("voorbereiding".equals(v.actief) && v.isOpgelost("blikje") && !v.isOpgelost("voorbereiding"), "dev: chapter 10 runs, 1-9 solved");
            for (Kaart k : Zaken.VOORBEREIDING.kaarten()) {
                v.gevonden.add(k.id());
            }
            Sherlock.losOp(p, Zaken.VOORBEREIDING);
            helper.assertTrue(v.isOpgelost("voorbereiding") && v.beloningen.contains("getuigenbank"), "chapter 10 done: the Getuigenbank");
            helper.assertTrue(v.status(Zaken.RECHTSZAAK) == Status.WACHT || v.hoeve >= 9, "the courtroom waits for Hoeve 9");
            v.hoeve = 9;
            v.actief = "rechtszaak";
            v.ronde = 0;
            int fout = 100 + Zaken.BEWIJS.indexOf("rood_veertje");
            Rechtbank.opBewijs(p, null, fout);
            helper.assertTrue(v.ronde == 0, "a wrong piece of evidence: a hint, the same round");
            for (int r = 0; r < 3; r++) {
                Rechtbank.opBewijs(p, null, 100 + Zaken.BEWIJS.indexOf(Zaken.BEZWAAR.get(r)));
                helper.assertTrue(v.ronde == r + 1, "BEZWAAR " + (r + 1));
            }
            Rechtbank.klaar(p, false);
            helper.assertTrue(v.isOpgelost("rechtszaak") && v.huidige() == null && v.beloningen.containsAll(List.of("standbeeld_opa_knabbel", "knabbelparkinkje")),
                    "the case is won: the statue and the parking spot");
            helper.assertTrue(p.getInventory().countItem(BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:detective_pet"))) > 0, "Sherlock's cap");
        } finally {
            SherlockData.get(helper.getLevel().getServer()).vergeet(id);
            Cast.vergeet(helper.getLevel().getServer(), id);
        }
        helper.succeed();
    }

    /** A clue (or witness) whose tile is taken moves to the nearest free grass; a free tile stays where it is. The courtroom set stamps. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void sherlockPlekkenEnRechtbank(GameTestHelper helper) {
        ServerLevel erf = Erfenis.level(helper.getLevel().getServer());
        if (erf == null) {
            helper.succeed();
            return;
        }
        BlockPos o = Plot.oorsprong(2900, 2900);
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                erf.setBlockAndUpdate(o.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                erf.setBlockAndUpdate(o.offset(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        erf.setBlockAndUpdate(o.offset(2, 1, 2), Blocks.OAK_PLANKS.defaultBlockState());
        helper.assertTrue(Sporen.plek(erf, o, 1, 1).equals(o.offset(1, 1, 1)), "a free tile: right there, on the grass");
        BlockPos b = Sporen.plek(erf, o, 2, 2);
        helper.assertTrue(!b.equals(o.offset(2, 1, 2)) && b.getY() == Plot.Y + 1 && Math.abs(b.getX() - o.getX() - 2) <= 1 && Math.abs(b.getZ() - o.getZ() - 2) <= 1,
                "a taken tile: the nearest free grass");
        Rechtbank.bouw(erf);
        helper.assertTrue(Rechtbank.staat(erf), "the courtroom stands (and is stamped only once)");
        helper.assertTrue(erf.getBlockState(Rechtbank.anker().offset(0, 0, 3)).is(Blocks.RED_CARPET), "the red carpet in the aisle");
        helper.succeed();
    }
}
