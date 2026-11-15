package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.10.4 "Ambachten: van 9 naar 4": the migration of old saved data, the tier gates, Smeden = jewellery (~10 % above the zone's
 * drops), jewellery rare as loot, the shops' small heal potion only, the removed Maatwerk gear and the gathering-group API.
 * Run: {@code runGameTestServer -Pgt=VadsrijkAmbachtVier}.
 */
public class VadsrijkAmbachtVierGameTests {
    private static final String BATCH = "vadsrijk_ambacht_vier";
    private static final Logger LOG = LogUtils.getLogger();

    /** Old xp of a level on the 1.10.3 curve (scale 1). */
    private static long oudXp(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) {
            t += Math.max(1, Math.round(40 + 25 * Math.pow(l, 1.6)));
        }
        return t;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void migratie(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        CompoundTag t = Spelers.data(p).extra(AmbachtData.SLICE);
        for (String k : List.copyOf(t.keySet())) {
            t.remove(k);
        }
        // a 1.10.3 player: plukken 6, mijnen 5, houthakken 3, smeden 2, naaien 4, brouwen 3, vissen 1 (no "versie")
        CompoundTag xp = new CompoundTag();
        xp.putLong("plukken", oudXp(6));
        xp.putLong("mijnen", oudXp(5) + 10);
        xp.putLong("houthakken", oudXp(3));
        xp.putLong("smeden", oudXp(2));
        xp.putLong("naaien", oudXp(4));
        xp.putLong("brouwen", oudXp(3));
        t.put("xp", xp);
        CompoundTag verhaal = new CompoundTag();
        verhaal.putInt("mijnen", 1);
        verhaal.putInt("plukken", 2);
        verhaal.putInt("naaien", 1);
        t.put("verhaal", verhaal);
        ListTag meester = new ListTag();
        meester.add(StringTag.valueOf("mijnen@60"));
        meester.add(StringTag.valueOf("plukken@60"));
        t.put("meester", meester);
        CompoundTag bestel = new CompoundTag();
        bestel.putString("dag", "gisteren");
        bestel.putInt("weekN", 7);
        t.put("bestel", bestel);
        t.putLong("uitgerust", 1234);
        // read through the API: migrated once
        long verzamelXp = AmbachtData.xp(p, Ambacht.VERZAMELEN);
        helper.assertTrue(verzamelXp == oudXp(6), "Verzamelen = the highest of mijnen/houthakken/plukken/imkeren (plukken 6): " + verzamelXp);
        helper.assertTrue(AmbachtData.xp(p, Ambacht.SMEDEN) == oudXp(4), "Smeden = max(smeden, naaien)");
        helper.assertTrue(AmbachtData.xp(p, Ambacht.KOKEN) == oudXp(3), "Koken = max(koken, brouwen)");
        helper.assertTrue(AmbachtData.xp(p, Ambacht.VISSEN) == 0, "Vissen untouched");
        int lv = AmbachtData.level(p, Ambacht.VERZAMELEN);
        helper.assertTrue(lv > 6 && lv <= 12, "the same xp is a higher level on the 3x faster curve: " + lv);
        helper.assertTrue(!t.getCompoundOrEmpty("xp").contains("plukken") && !t.getCompoundOrEmpty("xp").contains("naaien"), "old keys gone");
        helper.assertTrue(AmbachtData.verhaal(p, Ambacht.VERZAMELEN) == 2 && AmbachtData.verhaal(p, Ambacht.SMEDEN) == 1, "story: the furthest chain");
        helper.assertTrue(AmbachtData.heeft(p, "meester", "verzamelen@60") && AmbachtData.lijst(p, "meester").size() == 1, "mastery under the new id, once");
        helper.assertTrue(!AmbachtData.tag(p).getCompoundOrEmpty("bestel").contains("dag") && AmbachtData.tag(p).getCompoundOrEmpty("bestel").getIntOr("weekN", 0) == 7,
                "today's orders drawn again, week kept");
        helper.assertTrue(AmbachtData.uitgerust(p) == 1234 && AmbachtData.tag(p).getIntOr("versie", 0) == AmbachtData.VERSIE, "rest kept, version set");
        // idempotent: a second read changes nothing
        AmbachtData.zetXp(p, Ambacht.VERZAMELEN, verzamelXp + 5);
        helper.assertTrue(AmbachtData.xp(p, Ambacht.VERZAMELEN) == verzamelXp + 5, "only once");
        // titles: the chosen one moves to its new ambacht; the announced list learns the new ids
        CompoundTag saved = GuhQuests.saved(p);
        saved.putString(Titels.KEUZE, "vr_amb_naaien_leerling");
        saved.putString(Titels.BEKEND, "vr_amb_plukken_leerling,held_van_nomguh");
        AmbachtData.migreerTitel(p);
        helper.assertTrue(saved.getStringOr(Titels.KEUZE, "").equals("vr_amb_smeden_leerling"), "chosen title migrated");
        helper.assertTrue(saved.getStringOr(Titels.BEKEND, "").contains("vr_amb_verzamelen_leerling"), "announced list knows the new id");
        helper.assertTrue(AmbachtData.titelNaarNieuw("vr_amb_brouwen_meester").equals("vr_amb_koken_meester") && AmbachtData.titelNaarNieuw("held_van_nomguh")
                .equals("held_van_nomguh") && AmbachtData.titelNaarNieuw("vr_amb_grootmeester").equals("vr_amb_grootmeester"), "title ids");
        helper.assertTrue(Titels.van("vr_amb_smeden_leerling") != null && Titels.van("vr_amb_naaien_leerling") == null, "only the 4 ambachten have titles");
        // the rankings: a saved 1.10.3 row
        CompoundTag rang = new CompoundTag();
        ListTag rijen = new ListTag();
        CompoundTag rij = new CompoundTag();
        java.util.UUID u = java.util.UUID.randomUUID();
        rij.store("uuid", UUIDUtil.CODEC, u);
        rij.putString("naam", "Oudje");
        CompoundTag rx = new CompoundTag(), rs = new CompoundTag();
        rx.putLong("mijnen", 500);
        rx.putLong("plukken", 900);
        rx.putLong("brouwen", 300);
        rx.putLong("totaal", 1700);
        rs.putLong("plukken", 42L);
        rij.put("xp", rx);
        rij.put("sinds", rs);
        rijen.add(rij);
        rang.put("rijen", rijen);
        Map<String, Long> nieuw = Ranglijst.laad(rang).xpVan(u);
        helper.assertTrue(nieuw != null && nieuw.get("verzamelen") == 900 && nieuw.get("koken") == 300 && nieuw.get(Ranglijst.TOTAAL) == 1200
                && !nieuw.containsKey("mijnen"), "ranking row migrated: " + nieuw);
        helper.assertTrue(Ranglijst.laad(rang).lijst("verzamelen").get(0).sinds() == 42L, "since: of the chosen old list");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void tierPoorten(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        Verzamelen.vergeet(p.getUUID());
        Spot t2 = VadsrijkAmbachtGameTests.testSpot(helper, NodeSoort.ERTS, 2, "t_erts_t2");
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, 11);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, t2, 0).equals("vadsrijk.ambacht.oogst.te_laag"), "T2 ore at 11: too low");
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, 12);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, t2, 0).isEmpty(), "T2 ore at 12");
        int[] poort = {0, 1, 12, 22, 32, 40};
        for (int t = 1; t <= 5; t++) {
            helper.assertTrue(AmbachtXp.tierLevel(t) == poort[t], "T" + t + " from " + poort[t]);
        }
        // the four ambachten and their kinds
        helper.assertTrue(NodeSoort.ERTS.ambacht == Ambacht.VERZAMELEN && NodeSoort.KORF.ambacht == Ambacht.VERZAMELEN && NodeSoort.DUIK.ambacht == Ambacht.VISSEN,
                "spot kinds -> ambacht");
        helper.assertTrue(Ambacht.van("mijnen") == Ambacht.VERZAMELEN && Ambacht.van("naaien") == Ambacht.SMEDEN && Ambacht.van("brouwen") == Ambacht.KOKEN
                && Ambacht.van("imkeren") == Ambacht.VERZAMELEN, "old ids resolve");
        for (AmbachtPlekken.Groep g : AmbachtPlekken.GROEPEN) {
            helper.assertTrue(g.ambacht() == Ambacht.VERZAMELEN || g.ambacht() == Ambacht.VISSEN, g.id() + ": a gathering ambacht");
        }
        // recipes: every recipe within the cap, tiers line up with the new levels
        for (Recepten.Recept r : Recepten.ALLE.values()) {
            helper.assertTrue(r.ambacht() == Ambacht.SMEDEN || r.ambacht() == Ambacht.KOKEN, r.id() + ": a making ambacht");
            helper.assertTrue(r.level() >= AmbachtXp.tierLevel(r.tier()) - 3 && r.level() <= AmbachtXp.tierTop(r.tier()), r.id() + " lv " + r.level() + " in T" + r.tier());
        }
        // story steps and titles follow the new scale
        helper.assertTrue(AmbachtNpcs.VERHAAL_LEVELS[4] == 32 && HoogvadsGilde.LEVELS[0] == 40 && AmbachtBoek.LEERLING == 15 && AmbachtBoek.GEZEL == 32, "levels");
        helper.assertTrue(AmbachtPlekken.WERKPLAATSEN.stream().filter(w -> w.zone() == 1 && w.poi().equals("plein") && w.ambacht() != null).count() == 4, "4 workshop guhs in Zwamzoet");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void sieradenBovenBuit(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        StringBuilder log = new StringBuilder("Smeden jewellery vs the best zone drop (worth ratio, vadsigheid 100 / 0):");
        for (int t = 1; t <= AmbachtXp.MAX_TIER; t++) {
            int lv = Maatwerk.GEAR_LEVEL[t];
            for (String s : Recepten.SIERADEN) {
                Maatwerk.Soort so = Maatwerk.SOORTEN.get(s);
                List<Stat> drie = List.of(Stat.VADS, Stat.KRITIEK, Stat.KRITIEKSCHADE);
                double refL = Maatwerk.buitReferentie(t, so.slot(), lv, Zeldzaamheid.LEGENDARISCH), refE = Maatwerk.buitReferentie(t, so.slot(), lv, Zeldzaamheid.EPISCH);
                helper.assertTrue(refL > 0 && refE > 0, "T" + t + " " + s + ": the zone has jewellery to compare with");
                Uitrusting mw = Rollen.van(Maatwerk.maak(so, t, drie, 100, "test", Maatwerk.Eigenschap.BAZEN, p.getRandom()));
                Uitrusting mw0 = Rollen.van(Maatwerk.maak(so, t, drie, 0, "test", Maatwerk.Eigenschap.BAZEN, p.getRandom()));
                Uitrusting mat = Rollen.van(Maatwerk.maak(so, t, drie.subList(0, 2), 100, "test", null, p.getRandom()));
                double r = Maatwerk.kracht(mw) / refL, r0 = Maatwerk.kracht(mw0) / refL, rm = Maatwerk.kracht(mat) / refE;
                log.append(String.format(java.util.Locale.ROOT, " T%d %s %.3f/%.3f (Maatwerk %.3f);", t, s, r, r0, rm));
                helper.assertTrue(r > 1.06 && r < 1.14, "T" + t + " " + s + ": best Meesterwerk ~10 % above the zone's Legendary drop: " + r);
                helper.assertTrue(r0 > 1.01 && r0 < r, "T" + t + " " + s + ": 0 % vadsigheid still above it, but less: " + r0);
                helper.assertTrue(rm > 1.06 && rm < 1.14, "T" + t + " " + s + ": Maatwerk ~10 % above the zone's Episch drop: " + rm);
                helper.assertTrue(mw.level() == lv && mw.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH && mat.zeldzaamheid() == Zeldzaamheid.EPISCH, "rarity + level");
            }
        }
        LOG.info("[ambacht-vier] {}", log);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void sieradenZeldzaamAlsBuit(GameTestHelper helper) {
        StringBuilder log = new StringBuilder("jewellery share of the zone loot pools (before -> now):");
        helper.assertTrue(DropBalans.SIERAAD_DEEL == 5, "1/5");
        int tabellen = 0;
        for (Map.Entry<String, LootTabel> e : LootTabellen.alle().entrySet()) {
            for (LootTabel.Regel r : e.getValue().regels()) {
                boolean sieraad = DropBalans.sieraadDrop(r.basisType());
                helper.assertTrue(DropBalans.gewicht(r) == (sieraad ? r.gewicht() : r.gewicht() * DropBalans.SIERAAD_DEEL), e.getKey() + "/" + r.basisType());
                if (ZonePools.benoemd(r.basisType())) {
                    helper.assertTrue(!sieraad, "named boss items keep their weight: " + r.basisType());
                }
            }
            tabellen++;
        }
        helper.assertTrue(tabellen > 20, "tables checked: " + tabellen);
        for (int z = 1; z <= ZonePools.ZONES; z++) {
            List<BasisType> pool = ZonePools.voorZone(z, null);
            int juweel = 0, rest = 0;
            for (BasisType t : pool) {
                if (DropBalans.sieraadDrop(t.id())) {
                    juweel++;
                    helper.assertTrue(ZonePools.gewicht(t) == 1, "jewellery weight 1");
                } else {
                    rest++;
                    helper.assertTrue(ZonePools.gewicht(t) == DropBalans.SIERAAD_DEEL, "other weight 5");
                }
            }
            double voor = juweel / (double) Math.max(1, juweel + rest), na = juweel / (double) Math.max(1, juweel + rest * DropBalans.SIERAAD_DEEL);
            log.append(String.format(java.util.Locale.ROOT, " zone %d %.1f%% -> %.1f%%;", z, voor * 100, na * 100));
            // a real draw: ~1/5 as often
            if (juweel > 0 && rest > 0) {
                net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(1234L + z);
                int n = 20000, j = 0;
                for (int i = 0; i < n; i++) {
                    BasisType t = ZonePools.stuk(z, null, r);
                    if (t != null && DropBalans.sieraadDrop(t.id())) {
                        j++;
                    }
                }
                helper.assertTrue(Math.abs(j / (double) n - na) < 0.02, "zone " + z + ": drawn share " + j / (double) n + " vs " + na);
            }
        }
        // a named boss jewel is never reduced (the boss's own first-kill reward)
        helper.assertTrue(!DropBalans.sieraadDrop("z3baas_eierwarmer") && DropBalans.sieraadDrop("honingring") && !DropBalans.sieraadDrop("schorspantser"),
                "named vs random jewellery");
        LOG.info("[ambacht-vier] {}", log);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void winkelAlleenKleinDrankje(GameTestHelper helper) {
        Item klein = EcoSpul.KLEIN_VADSDRANKJE.get();
        for (String id : List.of(Winkels.DRANKJES, Winkels.ALGEMEEN, Winkels.KAMPWINKEL, nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2Winkel.KANDIJKRAAM,
                nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3SystemenRegistry.HONINGKRAAM)) {
            Winkels.Winkel w = Winkels.winkel(id);
            helper.assertTrue(w != null, "shop " + id);
            boolean heeftKlein = false;
            for (Winkels.Waar x : w.waren()) {
                ItemStack s = x.toon().get();
                boolean drank = s.has(net.minecraft.core.component.DataComponents.CONSUMABLE)
                        && s.get(net.minecraft.core.component.DataComponents.CONSUMABLE).animation() == net.minecraft.world.item.ItemUseAnimation.DRINK;
                heeftKlein |= s.is(klein);
                helper.assertTrue(!drank || s.is(klein) || x.id().equals("warme_chocomelk"), id + " sells only the small heal potion, not " + x.id());
            }
            helper.assertTrue(heeftKlein, id + " sells the Klein Vadsdrankje");
        }
        // the better potions are Koken's now (or a player's order)
        for (String r : List.of("vadsdrankje", "grote_vadsdrank", "energiedrankje", "vahoegdrankje", "snelpootjesdrankje")) {
            helper.assertTrue(Recepten.van(r) != null && Recepten.van(r).ambacht() == Ambacht.KOKEN && Opdrachten.vraagbaar(Recepten.van(r)), r + ": Koken + orders");
        }
        // a new player gets a starter stack, once
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.getInventory().clearContent();
        Spelers.data(p).extra("klassen").remove("start_drankjes");
        nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.startDrankjes(p);
        nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.startDrankjes(p);
        helper.assertTrue(p.getInventory().countItem(klein) == nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie.START_DRANKJES, "5 small potions, once");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void maatwerkGearGeschrapt(GameTestHelper helper) {
        for (Recepten.Recept r : Recepten.ALLE.values()) {
            helper.assertTrue(!r.soort().equals("gear"), r.id() + ": no Maatwerk weapons/armour any more");
            helper.assertTrue(!Recepten.GESCHRAPT.contains(r.doel()), r.id() + ": not a removed kind");
        }
        for (String s : Recepten.GESCHRAPT) {
            helper.assertTrue(Maatwerk.ITEM.containsKey(s) && Maatwerk.ITEM.get(s).get() != null, s + ": item still registered (old worlds load)");
            helper.assertTrue(!Maatwerk.sieraad(Maatwerk.SOORTEN.get(s)), s + ": not jewellery");
        }
        for (int t = 1; t <= AmbachtXp.MAX_TIER; t++) {
            for (String s : Recepten.SIERADEN) {
                Recepten.Recept m = Recepten.van(s + "_t" + t), mw = Recepten.van("meester_" + s + "_t" + t);
                helper.assertTrue(m != null && m.soort().equals("sieraad") && m.ambacht() == Ambacht.SMEDEN && m.keuzes() == 2, s + " T" + t + " Maatwerk");
                helper.assertTrue(mw != null && mw.soort().equals("meester") && mw.keuzes() == 3, s + " T" + t + " Meesterwerk");
                helper.assertTrue(mw.in().stream().anyMatch(i -> i.opties().contains(Materialen.KERNEN.get(mw.tier() - 1))), s + " T" + t + ": a boss core");
            }
        }
        helper.assertTrue(Recepten.van("ring_t1").level() == 1, "jewellery from Smeden 1");
        helper.assertTrue(Recepten.VERHAAL.keySet().equals(AmbachtVerhaal.RECEPT_STAP.keySet()) && Recepten.van("post_klodderknuffel").ambacht() == Ambacht.SMEDEN, "story items: only the craft steps' (1.10.4); naaien's post items");
        helper.assertTrue(Recepten.van("slijp_geslepen").ambacht() == Ambacht.KOKEN, "crystal cutting is Koken's");
        // the story-item kind works end to end (a temporary recipe)
        Recepten.Recept v = Recepten.verhaalRecept("t_tempelsleutel", Ambacht.SMEDEN, 1, 1, () -> new ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK),
                Recepten.ingredient(2, "pluisijzer"));
        try {
            ServerPlayer p = GuhMockPlayer.of(helper);
            AmbachtData.reset(p);
            p.getInventory().clearContent();
            MateriaalTas.geef(p, Materialen.stack("pluisijzer", 0, 2));
            helper.assertTrue(Werkplaats.maak(p, v.id(), List.of(), null, null, 1, 1).startsWith("vadsrijk.ambacht.werk."), "made");
            helper.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.TRIPWIRE_HOOK) == 1, "the story item is in the bag");
        } finally {
            Recepten.ALLE.remove(v.id());
            Recepten.VERHAAL.remove(v.id());
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void groepApi(GameTestHelper helper) {
        int voor = AmbachtPlekken.GROEPEN.size();
        AmbachtPlekken.Groep g = AmbachtPlekken.groep("t_eiland_erts", 1, "kampvuur_kamp", NodeSoort.ERTS, 1, 5, 12, 20, 30);
        try {
            helper.assertTrue(AmbachtPlekken.groep("t_eiland_erts") == g && AmbachtPlekken.GROEPEN.size() == voor + 1, "registered");
            helper.assertTrue(g.ambacht() == Ambacht.VERZAMELEN && g.zoek() == AmbachtPlekken.Zoek.STEEN && g.leegMin() == 20 && g.leegMax() == 30 && g.anker() != null,
                    "kind, terrain search, respawn, anchor");
            boolean dubbel = false;
            try {
                AmbachtPlekken.groep("t_eiland_erts", 1, "kampvuur_kamp", NodeSoort.HOUT, 1, 5, 12, 20, 30);
            } catch (IllegalArgumentException e) {
                dubbel = true;
            }
            helper.assertTrue(dubbel, "a duplicate id throws");
            // the respawn of the group is used
            ServerLevel sl = helper.getLevel();
            ServerPlayer p = GuhMockPlayer.of(helper);
            AmbachtData.reset(p);
            Verzamelen.vergeet(p.getUUID());
            Spot s = new Spot("t_eiland_erts_0", g, List.of(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)),
                    helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4))), List.of());
            Grondstoffen.get(sl.getServer()).wis(s.id());
            long nu = sl.getGameTime();
            helper.assertTrue(Verzamelen.doeOogst(p, sl, s, 0).isEmpty(), "harvest");
            long klaar = Grondstoffen.get(sl.getServer()).node(s).klaar() - nu;   // (1.10.4: the shared node's timer)
            helper.assertTrue(klaar >= 20L * 20 && klaar <= 20L * 30, "empty 20-30 s: " + klaar + " ticks");
        } finally {
            AmbachtPlekken.GROEPEN.remove(g);
        }
        helper.succeed();
    }
}
