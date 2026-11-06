package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Zoek;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-AMBACHT2: the professions grow with zones 4 and 5 (T4/T5, cap 60), coral diving, the Gilde story, the Knutselwinkel, the
 * Opdrachtenbord, the Meesterschort path and the zone-6 hooks. Run: {@code runGameTestServer -Pgt=VadsrijkAmbachtZones}.
 */
public class VadsrijkAmbachtZonesGameTests {
    private static final String BATCH = "vadsrijk_u_ambacht2";

    @GuhTest(template = "empty", batch = BATCH)
    public static void zone45Data(GameTestHelper helper) {
        // every gatherer has spots in zones 4 and 5, the fishing spots of zone 4 are in the book overview (the fix)
        for (int zone = 4; zone <= 5; zone++) {
            for (Ambacht a : Ambacht.values()) {
                if (a.verzamel) {
                    int z = zone;
                    helper.assertTrue(AmbachtPlekken.GROEPEN.stream().anyMatch(g -> g.zone() == z && g.ambacht() == a && g.tier() == z),
                            a.id + " has T" + zone + " spots in zone " + zone);
                }
            }
        }
        helper.assertTrue(AmbachtPlekken.maxZone() == 5, "book pages go to zone 5");
        helper.assertTrue(AmbachtPlekken.GROEPEN.stream().anyMatch(g -> g.zoek() == Zoek.KORAAL), "coral diving spots");
        helper.assertTrue(AmbachtPlekken.honingSoorten().size() == 7, "3 + 2 + 2 honey kinds: " + AmbachtPlekken.honingSoorten());
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        AmbachtData.voegToe(p, "plekken", "z4_vis_haven");
        CompoundTag d = new CompoundTag();
        AmbachtSchermen.plekken(p, d);
        ListTag l = d.getListOrEmpty("plekken");
        boolean z4vis = false, z5 = false;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            if (e.getIntOr("zone", 0) == 4 && e.getStringOr("ambacht", "").equals("vissen")) {
                z4vis = e.getIntOr("gevonden", 0) == 1 && e.getIntOr("totaal", 0) >= 5;
            }
            z5 |= e.getIntOr("zone", 0) == 5;
        }
        helper.assertTrue(z4vis && z5, "zone-4 fishing + zone 5 in the Plekken overview");
        helper.assertTrue(AmbachtBoek.bouw().stream().anyMatch(c -> c.id().equals("plekken_4")) && AmbachtBoek.bouw().stream()
                .anyMatch(c -> c.id().equals("plekken_5")), "Verkenningsboek pages for zones 4 + 5");
        // the fish collection: 15 fish (T1-T5 x 3), the coral finds are not fish
        long vissen = Materialen.ALLE.values().stream().filter(Materialen.Materiaal::vis).count();
        helper.assertTrue(vissen == 15 && !Materialen.ALLE.get("roze_koraal").vis(), "15 fish, coral is a dive find: " + vissen);
        // T4/T5 Maatwerk at the zone-4/5 set levels, Meesterwerk above the zone drops
        helper.assertTrue(Maatwerk.GEAR_LEVEL[4] == 50 && Maatwerk.GEAR_LEVEL[5] == 56, "gear levels");
        BasisType z5borst = BasisTypes.van("mw_t5_borstplaat"), z3ring = BasisTypes.van("mw_t3_ring"), z5ring = BasisTypes.van("mw_t5_ring");
        helper.assertTrue(z5borst != null && z5ring != null && z5ring.basis().get(Stat.KRITIEK) > z3ring.basis().get(Stat.KRITIEK), "T5 above T3");
        helper.assertTrue(Materialen.KERNEN.size() == 5 && AmbachtRegistry.kernVoor(52, 0, 64, 0).equals("lampionkern")
                && AmbachtRegistry.kernVoor(58, 0, 64, 0).equals("wolkkern") && AmbachtRegistry.kernVoor(40, 0, 64, 0).equals("wortelkern"), "boss cores per zone");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void koraalDuiken(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        // a pool: sand floor at y 1, water y 2..5, air above: dive spots on the floor (surface 4 blocks up)
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.SAND.defaultBlockState());
                for (int y = 2; y <= 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
                }
                helper.setBlock(new BlockPos(x, 6, z), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos anker = helper.absolutePos(new BlockPos(6, 4, 6));
        Groep g = new Groep("t_duik", 4, "x", Ambacht.VISSEN, 4, 1, 5, Zoek.KORAAL, "");
        List<Spot> l = PlekWereld.scan(sl, anker, g);
        helper.assertTrue(!l.isEmpty(), "dive spots in the pool");
        for (BlockPos q : l.get(0).posities()) {
            helper.assertTrue(sl.getBlockState(q.below()).is(Blocks.SAND) && sl.getBlockState(q).is(Blocks.WATER), "on the sea floor: " + q);
        }
        // a dive gives coral/sponges, never fish; it needs Vissen 50
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        Verzamelen.vergeet(p.getUUID());
        helper.assertTrue(Verzamelen.doeOogst(p, sl, l.get(0), 1).equals("vadsrijk.ambacht.oogst.te_laag"), "T4 needs Vissen 50");
        AmbachtData.zetLevel(p, Ambacht.VISSEN, 50);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, l.get(0), 1).isEmpty(), "dive");
        int duik = MateriaalTas.tel(p, s -> Materialen.van(s) != null && Materialen.van(s).duik());
        int vis = MateriaalTas.tel(p, s -> Materialen.van(s) != null && Materialen.van(s).vis());
        helper.assertTrue(duik >= 1 && vis == 0, "coral finds only: " + duik + " / fish " + vis);
        helper.assertTrue(AmbachtData.lijst(p, "vissen").isEmpty(), "not in the fish collection");
        // too deep: no spot
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                for (int y = 2; y <= 12; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
                }
            }
        }
        helper.assertTrue(PlekWereld.oppervlak(sl, helper.absolutePos(new BlockPos(5, 2, 5))) == 0, "deeper than " + PlekWereld.DUIK_DIEP + ": no surface");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void imkerenZone45(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        Groep g = new Groep("t_korf4", 4, "x", Ambacht.IMKEREN, 4, 1, 5, Zoek.BLOEM, "lampionhoning");
        Spot k = new Spot("t_korf4_0", g, List.of(helper.absolutePos(new BlockPos(2, 2, 2))), List.of());
        helper.assertTrue(Imkeren.korfLevel(3) == 1 && Imkeren.korfLevel(4) == 20 && Imkeren.korfLevel(5) == 35, "hive gates");
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 1).equals("vadsrijk.ambacht.oogst.te_laag"), "zone-4 meadow needs Imkeren 20");
        AmbachtData.zetLevel(p, Ambacht.IMKEREN, 20);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 1).isEmpty() && Imkeren.eigen(p, k), "placed at 20");
        Imkeren.vul(p, k);
        Verzamelen.doeOogst(p, sl, k, 2);
        helper.assertTrue(MateriaalTas.tel(p, s -> Materialen.van(s) != null && Materialen.van(s).id().equals("lampionhoning")) == Imkeren.MAX_RATEN,
                "lampionhoning harvested");
        helper.assertTrue(Imkeren.max(55) == 5, "a fifth hive at 55");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void maakT4T5(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        AmbachtData.zetLevel(p, Ambacht.SMEDEN, 56);
        AmbachtData.zetLevel(p, Ambacht.KOKEN, 55);
        for (String m : List.of("hemelzilver", "wolkenhout", "glinstergruis", "wolkbes", "wolkenhoning")) {
            MateriaalTas.geef(p, Materialen.stack(m, 1, 20));
        }
        String r = Werkplaats.maak(p, "zwaard_t5", List.of(Stat.KRACHT, Stat.KRITIEK), null, null, 2, 1);
        helper.assertTrue(r.startsWith("vadsrijk.ambacht.werk."), "made: " + r);
        ItemStack zwaard = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (Maatwerk.info(p.getInventory().getItem(i)) != null) {
                zwaard = p.getInventory().getItem(i);
            }
        }
        Uitrusting u = Rollen.van(zwaard);
        helper.assertTrue(u != null && u.type().equals("mw_t5_zwaard") && u.level() == 56, "a T5 Maatwerk sword at lv 56");
        helper.assertTrue(Werkplaats.maak(p, "wolkbessentaart", List.of(), null, null, 1, 1).startsWith("vadsrijk.ambacht.werk."), "T5 dish");
        // T4/T5 only at the Gilde (T4 also Lampionhaven)
        Recepten.Recept t5 = Recepten.van("zwaard_t5"), t4 = Recepten.van("zwaard_t4"), t3 = Recepten.van("zwaard_t3");
        helper.assertTrue(Werkplaats.magHier("amb_smid_z1", t3) && !Werkplaats.magHier("amb_smid_z1", t4) && Werkplaats.magHier("z4_werkplaats", t4)
                && !Werkplaats.magHier("z4_werkplaats", t5) && Werkplaats.magHier("z5_leermeester_smeden", t5) && Werkplaats.magHier(null, t5),
                "workshop gates");
        helper.assertTrue(Recepten.van("post_vlieger") != null && !Recepten.toon(Recepten.van("post_vlieger")).isEmpty(), "kite building (Naaien)");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void knutselwinkel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        helper.assertTrue(Knutselwinkel.koop(p, "kernsplinter").equals("vadsrijk.ambacht.winkel.te_weinig"), "no penningen, no sale");
        Knutselwinkel.geef(p, 20);
        helper.assertTrue(Knutselwinkel.koop(p, "kernsplinter").equals("vadsrijk.ambacht.winkel.gekocht") && Knutselwinkel.penningen(p) == 14,
                "bought for 6: " + Knutselwinkel.penningen(p));
        helper.assertTrue(p.getInventory().countItem(Materialen.KERNSPLINTER.get()) == 1, "the splinter came");
        AmbachtData.zetUitgerust(p, 0);
        helper.assertTrue(Knutselwinkel.koop(p, "thee").equals("vadsrijk.ambacht.winkel.gekocht") && AmbachtData.uitgerust(p) == Knutselwinkel.THEE, "rest tea");
        helper.assertTrue(Knutselwinkel.koop(p, "wolkkern").equals("vadsrijk.ambacht.winkel.te_weinig") && Knutselwinkel.penningen(p) == 2, "too expensive");
        // a Bestelbord order pays penningen
        int voor = Knutselwinkel.penningen(p);
        Bestelbord.openKist(p);
        helper.assertTrue(Knutselwinkel.penningen(p) == voor + Knutselwinkel.WEEKKIST, "the weekly chest pays penningen");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void opdrachtenbord(GameTestHelper helper) {
        ServerPlayer vrager = GuhMockPlayer.of(helper), maker = GuhMockPlayer.of(helper);
        AmbachtData.reset(vrager);
        AmbachtData.reset(maker);
        Spelers.data(vrager).munten = 1000;
        Spelers.data(maker).munten = 0;
        // escrow: the reward leaves the asker at once
        helper.assertTrue(Opdrachten.plaats(vrager, "klodderpudding", 4, 10).equals("vadsrijk.ambacht.opdracht.beloning"), "minimum 25 per item");
        helper.assertTrue(Opdrachten.plaats(vrager, "post_vaandel", 1, 500).equals("vadsrijk.ambacht.opdracht.onbekend"), "post recipes can't be asked");
        helper.assertTrue(Opdrachten.plaats(vrager, "klodderpudding", 4, 400).equals("vadsrijk.ambacht.opdracht.geplaatst") && Spelers.data(vrager).munten == 600,
                "posted, 400 in escrow");
        Opdrachten o = Opdrachten.get(helper.getLevel().getServer());
        long id = -1;
        for (Opdrachten.Opdracht x : o.lijst) {
            if (x.vrager.equals(vrager.getUUID())) {
                id = x.id;
            }
        }
        helper.assertTrue(Opdrachten.lever(vrager, id).equals("vadsrijk.ambacht.opdracht.eigen"), "not your own");
        helper.assertTrue(Opdrachten.lever(maker, id).equals("vadsrijk.ambacht.opdracht.niet_genoeg"), "needs the dishes");
        maker.getInventory().add(new ItemStack(Eten.ITEM.get("klodderpudding").get(), 6));
        helper.assertTrue(Opdrachten.lever(maker, id).equals("vadsrijk.ambacht.opdracht.geleverd"), "delivered");
        helper.assertTrue(Spelers.data(maker).munten == 400 && Knutselwinkel.penningen(maker) == Knutselwinkel.OPDRACHT, "the crafter is paid once");
        helper.assertTrue(maker.getInventory().countItem(Eten.ITEM.get("klodderpudding").get()) == 2, "exactly 4 left the crafter");
        helper.assertTrue(Opdrachten.lever(maker, id).equals("vadsrijk.ambacht.opdracht.weg"), "never twice");
        helper.assertTrue(Opdrachten.haalOp(vrager).equals("vadsrijk.ambacht.opdracht.opgehaald")
                && vrager.getInventory().countItem(Eten.ITEM.get("klodderpudding").get()) == 4, "the asker gets the 4");
        helper.assertTrue(Opdrachten.haalOp(vrager).equals("vadsrijk.ambacht.opdracht.niks_op_te_halen"), "and only once");
        // withdraw: the escrow comes back exactly
        Opdrachten.plaats(vrager, "vadsdrankje", 2, 100);
        long id2 = -1;
        for (Opdrachten.Opdracht x : o.lijst) {
            if (x.vrager.equals(vrager.getUUID())) {
                id2 = x.id;
            }
        }
        helper.assertTrue(Spelers.data(vrager).munten == 500 && Opdrachten.trekIn(vrager, id2).equals("vadsrijk.ambacht.opdracht.ingetrokken")
                && Spelers.data(vrager).munten == 600, "refund");
        helper.assertTrue(Opdrachten.trekIn(vrager, id2).equals("vadsrijk.ambacht.opdracht.weg"), "refund only once");
        // Maatwerk orders: only that kind and tier, never Meesterwerk
        Recepten.Recept r = Recepten.van("helm_t1");
        ItemStack helm = Maatwerk.maak(Maatwerk.SOORTEN.get("helm"), 1, List.of(Stat.VADS, Stat.KRACHT), 50, "x", null, helper.getLevel().getRandom());
        ItemStack ring = Maatwerk.maak(Maatwerk.SOORTEN.get("ring"), 1, List.of(Stat.VADS, Stat.KRACHT, Stat.KRITIEK), 50, "x", Maatwerk.Eigenschap.BAZEN,
                helper.getLevel().getRandom());
        helper.assertTrue(Opdrachten.past(r).test(helm) && !Opdrachten.past(Recepten.van("helm_t2")).test(helm) && !Opdrachten.past(r).test(ring), "gear match");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void meesterEnGilde(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        AmbachtData.zetLevel(p, Ambacht.KOKEN, AmbachtXp.CAP - 1);
        AmbachtData.geefXp(p, Ambacht.KOKEN, AmbachtXp.xpVoor(AmbachtXp.CAP - 1), false);
        helper.assertTrue(AmbachtData.level(p, Ambacht.KOKEN) == AmbachtXp.CAP, "at the cap");
        helper.assertTrue(AmbachtData.heeft(p, "meester", "koken@" + AmbachtXp.CAP) && Knutselwinkel.penningen(p) == Meesterschap.PENNINGEN, "the Meester moment");
        Titels.Titel t = Titels.van(Meesterschap.titelId(Ambacht.KOKEN));
        helper.assertTrue(t != null && t.behaald().test(p) && !Titels.van("vr_amb_grootmeester").behaald().test(p), "Meester-kok, not yet Grootmeester");
        Meesterschap.check(p);
        helper.assertTrue(Knutselwinkel.penningen(p) == Meesterschap.PENNINGEN, "once per cap");
        // the Gilde story: steps 6 and 7 at 55 and 60 (65/70 = zone 6 volgt)
        helper.assertTrue(HoogvadsGilde.stappen() == 2 && HoogvadsGilde.totaal() == 7, "2 Gilde steps under cap 60");
        AmbachtData.zetVerhaal(p, Ambacht.KOKEN, 2);
        helper.assertTrue(HoogvadsGilde.verhaal(p, "z5_leermeester_koken", List.of(Ambacht.KOKEN)) == null, "first the hub teacher's five");
        AmbachtData.zetVerhaal(p, Ambacht.KOKEN, 5);
        helper.assertTrue(HoogvadsGilde.verhaal(p, "z5_leermeester_koken", List.of(Ambacht.KOKEN)) == Ambacht.KOKEN && AmbachtData.verhaal(p, Ambacht.KOKEN) == 6,
                "Gilde step 1");
        HoogvadsGilde.verhaal(p, "z5_leermeester_koken", List.of(Ambacht.KOKEN));
        helper.assertTrue(AmbachtData.verhaal(p, Ambacht.KOKEN) == 7 && HoogvadsGilde.verhaal(p, "z5_leermeester_koken", List.of(Ambacht.KOKEN)) == null,
                "Gilde step 2, then done");
        // the gatherers at the Gildemeester: the first one with a step ready
        AmbachtData.zetVerhaal(p, Ambacht.VISSEN, 5);
        AmbachtData.zetLevel(p, Ambacht.VISSEN, 55);
        helper.assertTrue(HoogvadsGilde.verhaal(p, "z5_gildemeester", HoogvadsGilde.VERZAMELAARS) == Ambacht.VISSEN, "the Gildemeester tells Vissen");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void zone6Haken(GameTestHelper helper) {
        Set<String> ids = new HashSet<>(Materialen.ALLE.keySet());
        for (Zone6Haken.Materiaal m : Zone6Haken.MATERIALEN) {
            helper.assertTrue(ids.add(m.id()), "zone-6 material id is new: " + m.id());
        }
        helper.assertTrue(!Materialen.ALLE.containsKey(Zone6Haken.HONING) && !Materialen.KERNEN.contains(Zone6Haken.KERN), "honey + core are new");
        for (Zone6Haken.PlekSlot s : Zone6Haken.PLEKKEN) {
            helper.assertTrue(AmbachtPlekken.groep(s.id()) == null, "spot slot not active yet: " + s.id());
        }
        helper.assertTrue(Zone6Haken.CAP == 70 && AmbachtXp.CAP < Zone6Haken.CAP && AmbachtXp.TIER_MIN[6] == 60 && AmbachtXp.VERZAMEL.length > 6, "T6 band ready");
        helper.assertTrue(HoogvadsGilde.LEVELS.length == 4 && HoogvadsGilde.LEVELS[3] == 70, "Gilde steps 65/70 wait for zone 6");
        helper.succeed();
    }
}
