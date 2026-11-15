package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.4 "T6 in het Zijdeland": the zone-6 gathering groups and T6 materials ({@link Zone6Haken}), the T6 recipes made only at the
 * zone-6 teachers in Saffraanpoort, the zone-6 craft steps on them, the islet's klaverhoning and the hut masters facing the square.
 * Run: {@code runGameTestServer -Pgt=VadsrijkAmbachtT6}.
 */
public class VadsrijkAmbachtT6GameTests {
    private static final String BATCH = "vadsrijk_ambacht_t6";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Questlog.wisAlles(p);
        AmbachtData.reset(p);
        p.getInventory().clearContent();
        return p;
    }

    /** A spot of a real group, at a position in the test area (the harvest itself never needs the block). */
    private static Spot spot(GameTestHelper helper, Groep g, int n) {
        Spot s = new Spot(g.id() + "_t" + n, g, List.of(helper.absolutePos(new BlockPos(1 + n, 2, 2))), List.of());
        Grondstoffen.get(helper.getLevel().getServer()).wis(s.id());
        return s;
    }

    /** T6 is on: every zone-6 group is registered at a real zone-6 POI, named, and every kind has T6 materials. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void groepenT6(GameTestHelper helper) {
        helper.assertTrue(AmbachtXp.MAX_TIER == 6 && AmbachtXp.tierVoor(48) == 6 && AmbachtXp.tierVoor(47) == 5, "T6 from 48");
        helper.assertTrue(AmbachtPlekken.maxZone() == 6, "the book pages go to zone 6");
        int erts = 0, hout = 0, kruid = 0, vis = 0, korf = 0;
        for (Zone6Haken.Plek z : Zone6Haken.PLEKKEN) {
            Groep g = AmbachtPlekken.groep(z.id());
            helper.assertTrue(g != null && g.zone() == 6 && g.tier() == 6 && g.soort() == z.soort() && g.poi().equals(z.poi()), z.id() + " registered");
            BlockPos a = g.anker();
            helper.assertTrue(a != null && a.equals(Z6WereldPlekken.plek(z.poi())) && Z6WereldPlekken.inZone6(a.getX(), a.getZ()), z.id() + ": POI in zone 6");
            helper.assertTrue(g.leegMin() == AmbachtPlekken.LEEG_MIN && g.leegMax() == AmbachtPlekken.LEEG_MAX, z.id() + ": the normal respawn");
            helper.assertTrue(!Materialen.van(g.soort(), 6).isEmpty(), z.id() + ": T6 materials of its kind");
            switch (g.soort()) {
                case ERTS -> erts++;
                case HOUT -> hout++;
                case KRUID -> kruid++;
                case VIS -> vis++;
                default -> {
                }
            }
        }
        Groep k = AmbachtPlekken.groep(Zone6Haken.KORF_GROEP);
        if (k != null && k.zone() == 6 && k.honing().equals(Zone6Haken.HONING) && k.anker() != null) {
            korf++;
        }
        helper.assertTrue(erts >= 3 && hout >= 3 && kruid >= 3 && vis >= 3 && korf == 1, "ore/wood/herbs/fish/hives: " + erts + hout + kruid + vis + korf);
        for (String m : Zone6Haken.MATERIALEN) {
            Materialen.Materiaal x = Materialen.ALLE.get(m);
            helper.assertTrue(x != null && x.tier() == 6 && Materialen.ITEM.get(m) != null, m + ": a T6 material");
        }
        helper.assertTrue(Materialen.ALLE.get(Zone6Haken.HONING).tier() == 6 && Materialen.KERNEN.get(5).equals(Zone6Haken.KERN)
                && Materialen.KERN.containsKey(Zone6Haken.KERN), "honey + core");
        helper.assertTrue(AmbachtRegistry.kernVoor(65, 3760, 71, -850).equals(Zone6Haken.KERN), "zone-6 bosses drop the Sterrenkern");
        helper.assertTrue(AmbachtPlekken.honingSoorten().contains(Zone6Haken.HONING), "the honey kind is listed");
        helper.assertTrue(HoogvadsGilde.stappen() == 2, "the Gilde steps 54/60 still wait (no texts yet)");
        // the T6 look of the real block
        helper.assertTrue(Grondstoffen.staat(NodeSoort.ERTS, 6, true).getValue(GrondstofBlok.TIER) == 6, "tier 6 block state");
        helper.succeed();
    }

    /** A Verzamelen 48 player gathers T6 ore, wood, herbs and fish at the zone-6 groups; at 47 not. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void materialenTeHalen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = speler(helper);
        int n = 0;
        for (String id : List.of("z6_mijn_zoutgrot", "z6_hout_palmenbos", "z6_pluk_moerbei", "z6_vis_kanaal")) {
            Groep g = AmbachtPlekken.groep(id);
            Ambacht a = g.ambacht();
            Spot s = spot(helper, g, n++);
            AmbachtData.zetLevel(p, a, 47);
            helper.assertTrue(Verzamelen.doeOogst(p, sl, s, 1).equals("vadsrijk.ambacht.oogst.te_laag"), id + ": needs 48");
            AmbachtData.zetLevel(p, a, 48);
            Verzamelen.LAATSTE.remove(p.getUUID());
            int voor = MateriaalTas.tel(p, x -> Materialen.van(x) != null && Materialen.van(x).tier() == 6 && Materialen.van(x).soort() == g.soort());
            String m = Verzamelen.doeOogst(p, sl, s, 2);
            int na = MateriaalTas.tel(p, x -> Materialen.van(x) != null && Materialen.van(x).tier() == 6 && Materialen.van(x).soort() == g.soort());
            helper.assertTrue(m.isEmpty() && na > voor, id + ": a T6 material (" + m + ", " + voor + " -> " + na + ")");
        }
        // the bee meadow of the Paradijstuin gives saffraanhoning
        Groep kg = AmbachtPlekken.groep(Zone6Haken.KORF_GROEP);
        Spot k = spot(helper, kg, n);
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, Imkeren.korfLevel(6));
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 1).isEmpty() && Imkeren.eigen(p, k), "hive placed");
        Imkeren.vul(p, k);
        Verzamelen.doeOogst(p, sl, k, 2);
        helper.assertTrue(MateriaalTas.tel(p, x -> Materialen.van(x) != null && Materialen.van(x).id().equals(Zone6Haken.HONING)) == Imkeren.MAX_RATEN,
                "saffraanhoning harvested");
        helper.succeed();
    }

    /** T6 recipes: jewellery, Meesterwerk, Koken; made only at the zone-6 teachers (they can do T4/T5 too). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void receptenT6(GameTestHelper helper) {
        for (String s : Recepten.SIERADEN) {
            Recepten.Recept r = Recepten.van(s + "_t6"), mw = Recepten.van("meester_" + s + "_t6");
            helper.assertTrue(r != null && r.tier() == 6 && r.level() >= 48 && r.in().get(0).opties().contains("sterrengoud"), s + " T6");
            helper.assertTrue(mw != null && mw.level() == Recepten.MEESTER_LEVEL[6] && mw.in().stream().anyMatch(i -> i.opties().contains("sterrenkern")),
                    s + " T6 Meesterwerk");
        }
        long koken = Recepten.van(Ambacht.KOKEN).stream().filter(r -> r.tier() == 6).count();
        helper.assertTrue(koken >= 4, "a T6 Koken row: " + koken);
        Recepten.Recept ring = Recepten.van("ring_t6"), t5 = Recepten.van("ring_t5"), t3 = Recepten.van("ring_t3");
        helper.assertTrue(Werkplaats.magHier("z6_leermeester_smeden", ring) && Werkplaats.magHier("z6_leermeester_koken", Recepten.van("saffraanrijst"))
                && Werkplaats.magHier(null, ring), "T6 at the zone-6 teachers (and the dev command)");
        helper.assertTrue(!Werkplaats.magHier("z5_leermeester_smeden", ring) && !Werkplaats.magHier("z5_gildemeester", ring)
                && !Werkplaats.magHier("amb_smid_z1", ring) && !Werkplaats.magHier("z4_werkplaats", ring), "T6 nowhere else");
        helper.assertTrue(Werkplaats.magHier("z6_leermeester_smeden", t5) && Werkplaats.magHier("z5_leermeester_smeden", t5)
                && Werkplaats.magHier("amb_smid_eiland", t3), "T5 at the Gilde and in Saffraanpoort, T3 everywhere");
        helper.assertTrue(Werkplaats.nietHier(6).equals("vadsrijk.ambacht.werk.zijdeland"), "the 'only in Saffraanpoort' line");
        // make one of each kind at T6
        ServerPlayer p = speler(helper);
        AmbachtData.zetLevel(p, Ambacht.SMEDEN, 54);
        AmbachtData.zetLevel(p, Ambacht.KOKEN, 56);
        MateriaalTas.geef(p, Materialen.stack("sterrengoud", 1, 10));
        MateriaalTas.geef(p, Materialen.stack("dadelpalmhout", 1, 4));
        MateriaalTas.geef(p, Materialen.stack("saffraan", 1, 6));
        MateriaalTas.geef(p, Materialen.stack("oasekarper", 1, 2));
        String m = Werkplaats.maak(p, "ring_t6", List.of(nl.juiced.guhs.feature.vadsrijk.kern.Stat.KRITIEK, nl.juiced.guhs.feature.vadsrijk.kern.Stat.VADS),
                null, null, 2, 1);
        helper.assertTrue(m.equals("vadsrijk.ambacht.werk.klaar") || m.equals("vadsrijk.ambacht.werk.vadsig"), "T6 ring made: " + m);
        m = Werkplaats.maak(p, "saffraanrijst", List.of(), null, null, 2, 1);
        helper.assertTrue(m.equals("vadsrijk.ambacht.werk.klaar") || m.equals("vadsrijk.ambacht.werk.vadsig"), "saffraanrijst made: " + m);
        helper.succeed();
    }

    /** The zone-6 craft steps ask for T6 materials gathered in zone 6, and the cord is made at the bazaar smidse of Saffraanpoort. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zone6Stappen(GameTestHelper helper) {
        Quest a = Quests.van("z6_hq07a_zijde"), b = Quests.van("z6_hq07b_kompaskoord");
        helper.assertTrue(a != null && b != null, "the zone-6 steps exist");
        for (Quest.Doel d : a.doelen()) {
            Materialen.Materiaal m = Materialen.van(new ItemStack(Questlog.item(d.doel())));
            helper.assertTrue(m != null && m.tier() == 6, d.doel() + ": a T6 material");
            boolean inZone6 = AmbachtPlekken.GROEPEN.stream().anyMatch(g -> g.zone() == 6 && g.soort() == m.soort() && g.tier() == 6);
            helper.assertTrue(inZone6, m.id() + ": gathered in zone 6");
            helper.assertTrue(d.plek().startsWith("poi:amb_z6_"), d.doel() + ": the tracker points into zone 6: " + d.plek());
        }
        Recepten.Recept koord = Recepten.van("kompaskoord");
        helper.assertTrue(koord.tier() == 6 && koord.level() == 48 && koord.ambacht() == Ambacht.SMEDEN, "the cord: Smeden 48, T6");
        helper.assertTrue(koord.in().stream().allMatch(i -> i.opties().stream().allMatch(o -> Materialen.ALLE.get(o).tier() == 6)), "from T6 materials");
        helper.assertTrue(Werkplaats.magHier("z6_leermeester_smeden", koord) && !Werkplaats.magHier("z5_leermeester_smeden", koord),
                "made at the bazaar smidse of Saffraanpoort, not in Hoogvads any more");
        helper.assertTrue(b.doelen().get(0).plek().equals("npc:z6_leermeester_smeden"), "the tracker points at the zone-6 teacher");
        // completable: gather (kept), make, hand in
        ServerPlayer p = speler(helper);
        try {
            for (String na : a.na()) {
                Questlog.neemAan(p, Quests.van(na));
                net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
                t.putInt("s", Questlog.KLAAR);
                ((net.minecraft.nbt.CompoundTag) Questlog.data(p).get("q")).put(na, t);
            }
            Questlog.neemAan(p, a);
            MateriaalTas.geef(p, Materialen.stack("zijdedraad", 0, 3));
            MateriaalTas.geef(p, Materialen.stack("sterrengoud", 0, 2));
            helper.assertTrue(Questlog.allesKlaar(p, a), "gathered");
            Questlog.voltooi(p, a);
            QuestMotor.doeActie(p, a.actieKlaar(), null);
            helper.assertTrue(Questlog.actief(p, b.id()), "the making step starts");
            AmbachtData.zetLevel(p, Ambacht.SMEDEN, 48);
            String m = Werkplaats.maak(p, "kompaskoord", List.of(), null, null, 0, 1);
            helper.assertTrue(m.equals("vadsrijk.ambacht.werk.klaar") || m.equals("vadsrijk.ambacht.werk.vadsig"), "the cord made: " + m);
            helper.assertTrue(Questlog.allesKlaar(p, b), "the step asks for nothing more");
        } finally {
            Questlog.wisAlles(p);
            AmbachtData.reset(p);
        }
        helper.succeed();
    }

    /** The islet's Bijenhofje gives klaverhoning (T1), and T1 Koken takes it as an alternative ingredient. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eilandHoning(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = speler(helper);
        Groep g = AmbachtPlekken.groep("eiland_bijenhofje");
        helper.assertTrue(g != null && g.soort() == NodeSoort.KORF && g.honing().equals(EilandRegistratie.HONING), "the Bijenhofje gives klaverhoning");
        Materialen.Materiaal h = Materialen.ALLE.get(EilandRegistratie.HONING);
        helper.assertTrue(h != null && h.tier() == 1 && h.soort() == NodeSoort.KORF, "a T1 honey");
        Spot k = spot(helper, g, 0);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 1).isEmpty() && Imkeren.eigen(p, k), "a level-1 player places a hive on the islet");
        Imkeren.vul(p, k);
        Verzamelen.doeOogst(p, sl, k, 1);
        helper.assertTrue(MateriaalTas.tel(p, x -> Materialen.van(x) == h) == Imkeren.MAX_RATEN, "klaverhoning harvested");
        // usable in T1 Koken
        long recepten = Recepten.van(Ambacht.KOKEN).stream().filter(r -> r.tier() == 1 && r.in().stream().anyMatch(i -> i.opties().contains(h.id()))).count();
        helper.assertTrue(recepten >= 2, "klaverhoning in T1 Koken recipes: " + recepten);
        MateriaalTas.geef(p, Materialen.stack("klodderklaver", 0, 2));
        String m = Werkplaats.maak(p, "klodderpudding", List.of(), null, null, 1, 1);
        helper.assertTrue(m.equals("vadsrijk.ambacht.werk.klaar") || m.equals("vadsrijk.ambacht.werk.vadsig"), "pudding with honey instead of fish: " + m);
        helper.assertTrue(MateriaalTas.tel(p, x -> Materialen.van(x) == h) == Imkeren.MAX_RATEN - 1, "one honey used");
        helper.succeed();
    }

    /** The four hut masters look at the centre of the square (ambacht_plein), whatever their place. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void meestersKijkenNaarPlein(GameTestHelper helper) {
        BlockPos plein = WereldPlekken.plek(EilandRegistratie.PLEIN);
        helper.assertTrue(plein != null, "ambacht_plein exists");
        for (Map.Entry<String, String> e : EilandRegistratie.MEESTERS.entrySet()) {
            AmbachtPlekken.Werkplaats w = AmbachtPlekken.werkplaats(e.getKey());
            helper.assertTrue(w != null && plein.equals(AmbachtPlekken.kijkNaar(w)), e.getKey() + " looks at the square");
            BlockPos van = w.anker();
            float yaw = AmbachtNpcs.yaw(van, plein);
            double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));   // (Minecraft's facing for a yaw)
            double dx = plein.getX() - van.getX(), dz = plein.getZ() - van.getZ(), l = Math.sqrt(dx * dx + dz * dz);
            helper.assertTrue(l > 0 && (fx * dx + fz * dz) / l > 0.99, e.getKey() + ": yaw " + yaw + " points at the plein");
        }
        // hub masters without a KIJK keep looking at their hub's centre
        AmbachtPlekken.Werkplaats z1 = AmbachtPlekken.werkplaats("amb_smid_z1");
        helper.assertTrue(z1 != null && z1.anker().equals(AmbachtPlekken.kijkNaar(z1)), "Zwamzoet's smith looks at the square centre");
        helper.assertTrue(AmbachtNpcs.yaw(new BlockPos(0, 0, 0), new BlockPos(0, 0, 5)) == 0f && AmbachtNpcs.yaw(new BlockPos(0, 0, 0), new BlockPos(-5, 0, 0)) == 90f,
                "south = 0, west = 90");
        helper.succeed();
    }

    /** Zone 6 is not spoiled: T6 orders and "where to gather" only for players who have been in zone 6. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zone6NietVerklapt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ZonePoort.zetHoogste(p, 5);
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, 50);
        helper.assertTrue(Bestelbord.mogelijk(p).stream().noneMatch(b -> Bestelbord.tier(b) >= 6), "no T6 orders before zone 6");
        helper.assertTrue(!AmbachtNpcs.zoneBekend(p, 6) && AmbachtNpcs.zoneBekend(p, 5), "zone-6 spots not pointed out");
        ZonePoort.zetHoogste(p, 6);
        helper.assertTrue(Bestelbord.mogelijk(p).stream().anyMatch(b -> Bestelbord.tier(b) == 6), "T6 orders once you were there");
        helper.assertTrue(AmbachtNpcs.zoneBekend(p, 6), "and the spots");
        ZonePoort.zetHoogste(p, 1);
        helper.succeed();
    }
}
