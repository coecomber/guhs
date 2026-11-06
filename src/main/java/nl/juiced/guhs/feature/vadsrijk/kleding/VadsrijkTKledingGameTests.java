package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-KLEDING: the catalogue, the account store (not in SpelerData), choices (back slot shared by wings + capes, only unlocked
 * things), sources (start, story flags, kill counter, unknown zones), the Vahoegnir moment (first wings + gliding), the glide rules
 * outside the Vadsrijk, the secrets, transmog looks and what other clients get to see.
 * Run: {@code runGameTestServer -Pgt=VadsrijkTKleding}.
 */
public class VadsrijkTKledingGameTests {
    private static final String BATCH = "vadsrijk_t_kleding";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        KledingData.wis(p);
        return p;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void catalogus(GameTestHelper helper) {
        Set<String> ids = new HashSet<>();
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            helper.assertTrue(ids.add(d.id()), "unique id " + d.id());
            helper.assertTrue(d.id().startsWith(d.soort().id + ":"), "id prefix = kind: " + d.id());
            helper.assertTrue((d.soort() == Cosmetica.Soort.VLEUGEL) == (d.kracht() != null), "only wings (all wings) have a trick: " + d.id());
            if (d.soort() == Cosmetica.Soort.NAAMKLEUR || d.soort() == Cosmetica.Soort.VERF) {
                helper.assertTrue(d.kleur() > 0, "colour set: " + d.id());
            }
        }
        // R3/R4: every wing a different trick except that two may share; the first wings are the plain glide
        helper.assertTrue(Cosmetica.VAHOEGVEERTJES.kracht() == Cosmetica.Kracht.ZWEVEN, "Vahoegveertjes = plain glide");
        Set<Cosmetica.Kracht> krachten = new HashSet<>();
        for (Cosmetica.Ding d : Cosmetica.van(Cosmetica.Soort.VLEUGEL)) {
            krachten.add(d.kracht());
        }
        helper.assertTrue(krachten.size() == Cosmetica.Kracht.values().length, "every movement trick is on some wing");
        helper.assertTrue(Cosmetica.Soort.VLEUGEL.keuze().equals("rug") && Cosmetica.Soort.KAAP.keuze().equals("rug"), "wings and capes share the back");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void accountOpslag(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(!Kledingkast.heeft(p, Cosmetica.KRISTAL.id()), "fresh: nothing");
        helper.assertTrue(Kledingkast.ontgrendel(p, Cosmetica.KRISTAL.id(), false), "unlock");
        helper.assertTrue(!Kledingkast.ontgrendel(p, Cosmetica.KRISTAL.id(), false), "unlock twice = false");
        helper.assertTrue(!Kledingkast.ontgrendel(p, "vleugel:bestaatniet", false), "unknown id = false");
        helper.assertTrue(Kledingkast.heeft(p, Cosmetica.KRISTAL.id()), "kept");
        helper.assertTrue(Kledingkast.keuze(p, "rug").equals(Cosmetica.KRISTAL.id()), "first wings go on at once");
        // account-wide: NOT in the per-character SpelerData
        helper.assertTrue(!Spelers.data(p).extra.containsKey("kleding") && !Spelers.data(p).extra.containsKey("t_kleding"), "not in SpelerData");
        helper.assertTrue(KledingData.kast(p).getCompoundOrEmpty("heeft").getBooleanOr(Cosmetica.KRISTAL.id(), false), "in the account store");
        KledingData.wis(p);
        helper.assertTrue(!Kledingkast.heeft(p, Cosmetica.KRISTAL.id()), "wiped");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kiezen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(!Kledingkast.kies(p, Cosmetica.Soort.KAAP, Cosmetica.KAAP_ROZE.id()), "locked cape cannot be picked");
        Kledingkast.ontgrendel(p, Cosmetica.KAAP_ROZE.id(), false);
        Kledingkast.ontgrendel(p, Cosmetica.HONINGBIJ.id(), false);   // (first wings: on at once)
        helper.assertTrue(Kledingkast.keuze(p, "rug").equals(Cosmetica.HONINGBIJ.id()), "wings on");
        helper.assertTrue(Kledingkast.kies(p, Cosmetica.Soort.KAAP, Cosmetica.KAAP_ROZE.id()), "pick the cape");
        helper.assertTrue(Kledingkast.keuze(p, "rug").equals(Cosmetica.KAAP_ROZE.id()), "the cape replaced the wings (one back slot)");
        helper.assertTrue(!Kledingkast.kies(p, Cosmetica.Soort.VLEUGEL, Cosmetica.KAAP_ROZE.id()), "a cape is not a wing");
        helper.assertTrue(!Kledingkast.kies(p, Cosmetica.Soort.VLEUGEL, ""), "taking off wings while wearing a cape: nothing");
        helper.assertTrue(Kledingkast.kies(p, Cosmetica.Soort.KAAP, ""), "take the cape off");
        helper.assertTrue(Kledingkast.keuze(p, "rug").isEmpty(), "back empty");
        // transmog
        helper.assertTrue(!Kledingkast.kiesLook(p, EquipmentSlot.HEAD, "guhs:vr_pluishelm"), "a look you never had: no");
        helper.assertTrue(Kledingkast.kiesLook(p, EquipmentSlot.HEAD, Kledingkast.VERBORGEN), "hiding the helmet is always allowed");
        helper.assertTrue(!Kledingkast.kiesLook(p, EquipmentSlot.MAINHAND, ""), "not an armour slot");
        helper.assertTrue(Kledingkast.uiterlijk(p).getStringOr("tm.head", "").equals(Kledingkast.VERBORGEN), "hidden helmet goes to the clients");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void looks(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ItemStack helm = new ItemStack(KernRegistry.PLUISHELM.get());
        String k = Kledingkast.lookSleutel(helm);
        helper.assertTrue(k != null && k.startsWith("head|"), "the kern helmet has a head look, got " + k);
        helper.assertTrue(Kledingkast.lookSleutel(new ItemStack(net.minecraft.world.item.Items.STICK)) == null, "a stick has no look");
        p.getInventory().add(helm);
        helper.assertTrue(Kledingkast.verzamel(p) >= 1, "a new look from the bag");
        helper.assertTrue(Kledingkast.verzamel(p) == 0, "not twice");
        String item = Kledingkast.looks(p, EquipmentSlot.HEAD).get(0);
        helper.assertTrue(Kledingkast.kiesLook(p, EquipmentSlot.HEAD, item), "pick the collected look");
        helper.assertTrue(Kledingkast.alleLooks(p) >= 0 && !Kledingkast.looks(p, EquipmentSlot.CHEST).isEmpty(), "dev: every Vadsrijk look");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bronnen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Bronnen.check(p);
        helper.assertTrue(Kledingkast.heeft(p, Cosmetica.PLOF_CONFETTI.id()) && Kledingkast.heeft(p, Cosmetica.VERF_ROZE.id()), "starters");
        helper.assertTrue(!Kledingkast.heeft(p, Cosmetica.LAMPION.id()) && !Kledingkast.heeft(p, Cosmetica.PLUIS.id()), "later + secrets never by check");
        helper.assertTrue(!Bronnen.gehaald(p, "boek:zone9:25", new java.util.HashMap<>()), "unknown zone: no");
        helper.assertTrue(!Kledingkast.heeft(p, Cosmetica.PLOF_GIECHEL.id()), "no kills yet");
        KledingData.kast(p).putInt("kills", 100);
        Bronnen.check(p);
        helper.assertTrue(Kledingkast.heeft(p, Cosmetica.PLOF_GIECHEL.id()) && !Kledingkast.heeft(p, Cosmetica.NAAM_PAARS.id()), "100 cheer-ups: giggle, not yet purple");
        Questlog.zetVlag(p, "zone1_klaar", true);
        Bronnen.check(p);
        helper.assertTrue(Kledingkast.heeft(p, Cosmetica.KAAP_KNUFFELORDE.id()) && Kledingkast.heeft(p, Cosmetica.NAAM_ROZE.id()), "zone-1 story rewards");
        Questlog.zetVlag(p, "zone1_klaar", false);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void vahoegnir(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(!Kledingkast.zweven(p), "no gliding yet");
        nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenApi.moment(p, "vahoegnir_opgevrolijkt");
        helper.assertTrue(Kledingkast.zweven(p), "Vahoegnir pink: gliding");
        helper.assertTrue(Kledingkast.keuze(p, "rug").equals(Cosmetica.VAHOEGVEERTJES.id()), "and the feathers are on");
        helper.assertTrue(Zweven.kracht(p) == Cosmetica.Kracht.ZWEVEN, "plain glide");
        // the rules: not in the Vadsrijk (the gametest level) = no glide, a client claim is ignored
        helper.assertTrue(!Zweven.mag(p) && !Zweven.kan(p), "no gliding outside the Vadsrijk");
        Zweven.ontvang(p, 1);
        helper.assertTrue(!Zweven.zweeft(p), "a glide claim outside the Vadsrijk is ignored");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geheimen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(Kledingkast.geheim(p, "zweefvlucht") && Kledingkast.heeft(p, Cosmetica.PLUIS.id()), "the long glide secret");
        helper.assertTrue(!Kledingkast.geheim(p, "zweefvlucht"), "only once");
        Kledingkast.open(p, "kleermaker");
        Kledingkast.open(p, "z2_breister");
        helper.assertTrue(!Kledingkast.heeft(p, Cosmetica.KAAP_REIS.id()), "two tailors: not yet");
        Kledingkast.open(p, "z3_kleermaker");
        helper.assertTrue(Kledingkast.heeft(p, Cosmetica.KAAP_REIS.id()), "all three tailors: the travel cape");
        // t-puzzels rewards (the hook gives "puzzel:<id>"); without t-puzzels in the tree the check pass finds nothing
        helper.assertTrue(!Bronnen.gehaald(p, "puzzel:pz_cape_mos", new java.util.HashMap<>()) || KledingRegistry.puzzels(), "no puzzles here");
        helper.assertTrue(Kledingkast.geefBron(p, "puzzel:pz_vleugels_poollicht") && Kledingkast.heeft(p, Cosmetica.POOLLICHT.id()), "puzzle wings");
        helper.assertTrue(Cosmetica.POOLLICHT.kracht() == Cosmetica.Kracht.LANGER_ZWEVEN && Cosmetica.KAAP_MOS.geheim(), "movement trick; hidden name");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void uiterlijk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        KledingData.deel(p, "keuze").putString("hoofd", Cosmetica.FEESTMUTS.id());   // (a forged choice: not unlocked)
        helper.assertTrue(!Kledingkast.uiterlijk(p).contains("hoofd"), "locked choices are never shown");
        Kledingkast.ontgrendel(p, Cosmetica.NAAM_GOUD.id(), false);
        Kledingkast.ontgrendel(p, Cosmetica.VERF_LILA.id(), false);
        Kledingkast.kies(p, Cosmetica.Soort.NAAMKLEUR, Cosmetica.NAAM_GOUD.id());
        Kledingkast.kies(p, Cosmetica.Soort.VERF, Cosmetica.VERF_LILA.id());
        CompoundTag u = Kledingkast.uiterlijk(p);
        helper.assertTrue(u.getIntOr("naamkleur", -1) == Cosmetica.NAAM_GOUD.kleur() && Kledingkast.naamkleur(p) == 0xFFD700, "name colour");
        helper.assertTrue(u.getIntOr("verf", -1) == Cosmetica.VERF_LILA.kleur(), "dye");
        CompoundTag scherm = Kledingkast.schermData(p);
        helper.assertTrue(scherm.getListOrEmpty("heeft").size() >= 2 && scherm.contains("looks") && scherm.contains("titels"), "screen data");
        helper.succeed();
    }
}
