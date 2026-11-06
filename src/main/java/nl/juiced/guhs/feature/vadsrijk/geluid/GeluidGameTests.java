package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-GELUID: where which music/ambience plays, the boss -> theme mapping, every sound registered with its OGG in the jar, and the
 * server stingers (first visit of an area once; a level-up once per tick). The sound itself is a listen check for the user.
 * Run: {@code runGameTestServer -Pgt=Geluid}.
 */
public class GeluidGameTests {
    private static final String BATCH = "vadsrijk_geluid";

    @GuhTest(template = "empty", batch = BATCH)
    public static void streken(GameTestHelper helper) {
        // every wilderness area of zones 1-3 has a theme; check through the real area boxes (their centres)
        String[][] verwacht = {{"klodderweide", "klodderweide"}, {"sporenwoud", "sporenwoud"}, {"druipgrotten", "druipgrotten"},
                {"zwamtempel", "zwamtempel"}, {"z2_knuffelmeer", "knuffelmeer"}, {"z2_galmhellingen", "galmhellingen"},
                {"z2_splinterkloof", "splinterkloof"}, {"z2_spiegelburcht", "spiegelburcht"}, {"z3_wortelduister", "wortelduister"},
                {"z3_kruinzee", "kruinzee"}, {"z3_zeppelinwerf", "zeppelinwerf"}};
        for (String[] v : verwacht) {
            GeluidPlan.Streek s = GeluidPlan.streek(v[0], 0, 64, 0);
            helper.assertTrue(s != null && s.themas().contains(v[1]), v[0] + " -> " + v[1] + ", got " + s);
            helper.assertTrue(GeluidPlan.GEBIED_THEMAS.contains(v[1]), v[1] + " not in GEBIED_THEMAS");
        }
        // every area id that exists in kern Gebieden gets a streek or is a town (no gaps after a map regen)
        for (Gebieden.Gebied g : Gebieden.alle()) {
            String id = g.id();
            if (id.equals("proeftuin") || g.stad()) {
                continue;
            }
            helper.assertTrue(GeluidPlan.streek(id, 0, 64, 0) != null, "area without music/ambience: " + id);
        }
        // the towns: no area theme (their town tunes play), Honingtak gets the honey buzz
        for (String stad : List.of("zwamzoet", "haven", "knuffelkamp", "z2_kandijkom", "z2_kandijkom_dok")) {
            helper.assertTrue(GeluidPlan.themas(GeluidPlan.streek(stad, 0, 64, 0)).isEmpty(), stad + ": no area theme in a town");
        }
        GeluidPlan.Streek honing = GeluidPlan.streek("z3_honingtak", 0, 64, 0);
        helper.assertTrue(honing != null && honing.themas().isEmpty() && honing.bed().equals("honing"), "Honingtak: honey bed, no theme");
        // zones 4 and 5: by id prefix and by the W-3 boxes
        helper.assertTrue(GeluidPlan.streek("z4_strandfeest", 0, 64, 0).themas().contains("lampionkust_dag"), "z4 id");
        helper.assertTrue(GeluidPlan.streek(null, 2000, 64, -500).bed().equals("kust"), "z4 box");
        helper.assertTrue(GeluidPlan.streek(null, 2800, 200, -500).themas().contains("hoogvads"), "z5 box above y 150");
        helper.assertTrue(GeluidPlan.streek(null, 9000, 64, 9000) == null, "nowhere");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bazen(GameTestHelper helper) {
        String[][] verwacht = {{"vr_klodderreus", "klodderreus"}, {"vr_baas_koning_klodder", "koning_klodder"}, {"vr_baas_opperzwam", "opperzwam"},
                {"vr_baas_mika_spion", "mika_spion"}, {"vr_z2_baas_galmwolf", "galmwolf"}, {"vr_z2_baas_spiegelzucht", "spiegelzucht"},
                {"vr_z2_baas_spiegelbeeld", "spiegelzucht"}, {"vr_z2_baas_boormeester", "boormeester"}, {"vr_z3_baas_bleu", "baron_bleu"},
                {"vr_z3_baas_vahoegnir", "vahoegnir"}, {"vr_z3_baas_piepvel", "piepvel"}, {"vr_z3_baas_taleggio", "taleggio"},
                {"vr_z3_baas_brokkel", "brokkel"}, {"vr_z3_baas_roquefort", "roquefort"}, {"vr_z3_baas_nestwachter", "nestwachter"},
                {"vr_z3_baas_wortelwurger", "wortelwurger"}, {"vr_z3_baas_kruinroofvogel", "kruinroofvogel"},
                {"vr_z4_baas_grauwe_zeeslurf", "zeeslurf"}, {"vr_z4_vliegervanger", "vliegervanger"}, {"vr_iets_nieuws", "algemeen"}};
        for (String[] v : verwacht) {
            helper.assertTrue(GeluidPlan.baasThema(v[0]).equals(v[1]), v[0] + " -> " + v[1] + ", got " + GeluidPlan.baasThema(v[0]));
        }
        // the zone 1-3 boss entity types really exist under those ids
        for (int i = 0; i < 17; i++) {
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(verwacht[i][0])), "no entity type " + verwacht[i][0]);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geluidenEnBestanden(GameTestHelper helper) {
        for (String id : GeluidRegistry.ALLE.keySet()) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id(id)), "not registered: " + id);
        }
        for (String t : GeluidPlan.GEBIED_THEMAS) {
            ogg(helper, "muziek/gebied/" + t);
        }
        for (String t : GeluidPlan.BAAS_THEMAS) {
            ogg(helper, "muziek/baas/" + t);
        }
        for (String t : GeluidPlan.STINGERS) {
            ogg(helper, "muziek/stinger/" + t);
        }
        for (String b : GeluidPlan.BEDDEN) {
            ogg(helper, "sfeer/bed_" + b);
        }
        helper.succeed();
    }

    private static void ogg(GameTestHelper helper, String pad) {
        String r = "/assets/guhs/sounds/vadsrijk/" + pad + ".ogg";
        helper.assertTrue(GeluidGameTests.class.getResource(r) != null, "missing " + r);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void stingers(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        GeluidRegistry.GESTUURD.remove(p.getUUID());
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "sporenwoud"));
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "sporenwoud"));
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "ring"));
        List<String> l = GeluidRegistry.GESTUURD.getOrDefault(p.getUUID(), List.of());
        helper.assertTrue(l.stream().filter("ontdekking"::equals).count() == 1, "discovery: once for the first visit, none for the ring: " + l);
        Haken.LEVEL_OP.forEach(h -> h.accept(p, "generiek:5"));
        Haken.LEVEL_OP.forEach(h -> h.accept(p, "ridder:5"));
        l = GeluidRegistry.GESTUURD.getOrDefault(p.getUUID(), List.of());
        helper.assertTrue(l.stream().filter("level_op"::equals).count() == 1, "level up: one stinger for two level-ups in a tick: " + l);
        helper.succeed();
    }
    // ---------------------------------------------------------------------------------------------------------------- U-AUDIO2

    /** Every boss has its own theme, a last-phase loop and a fanfare (registered + OGG in the jar); the new bosses map right. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void fasesEnFanfares(GameTestHelper helper) {
        for (String t : GeluidPlan.BAAS_THEMAS) {
            for (String id : new String[] {GeluidPlan.baasId(t), GeluidPlan.faseId(t), GeluidPlan.fanfareId(t)}) {
                helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id(id)), "not registered: " + id);
            }
            ogg(helper, "muziek/baas/" + t);
            ogg(helper, "muziek/baas/" + t + "_fase");
            ogg(helper, "muziek/fanfare/" + t);
        }
        String[][] verwacht = {{"vr_z3_imker_mika", "mika_imker"}, {"vr_z5_baas_grauwe_droom", "grauwe_droom"},
                {"vr_z5_baas_donderbuik", "donderbuik"}, {"vr_z5_baas_gorgonzola", "gorgonzola"}, {"vr_z5_baas_zilverstaart", "zilverstaart"},
                {"vr_z4_baas_zeeslurf", "zeeslurf"}, {"vr_z4_baas_vliegervanger", "vliegervanger"}};
        for (String[] v : verwacht) {
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(v[0])), "no entity type " + v[0]);
            helper.assertTrue(GeluidPlan.baasThema(v[0]).equals(v[1]), v[0] + " -> " + v[1] + ", got " + GeluidPlan.baasThema(v[0]));
        }
        helper.assertTrue("zeeslurf".equals(GeluidPlan.themaVanId("guhs:vr.geluid.baas.zeeslurf")), "themaVanId");
        helper.assertTrue(GeluidPlan.themaVanId("guhs:vr.kern.baas") == null, "themaVanId: foreign id");
        helper.succeed();
    }

    /** De Grauwe Droom names its OWN theme; a phase change reaches the players near the boss (with the phase count). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void faseHaak(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        GeluidRegistry.GESTUURD.remove(p.getUUID());
        var droom = nl.juiced.guhs.feature.vadsrijk.zone5.droom.Z5DroomRegistry.GRAUWE_DROOM.get().create(helper.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(droom != null, "no Grauwe Droom");
        droom.setPos(p.getX(), p.getY(), p.getZ());
        helper.assertTrue("grauwe_droom".equals(GeluidRegistry.themaVan(droom)), "Grauwe Droom theme: " + GeluidRegistry.themaVan(droom));
        GeluidRegistry.fase(droom, 3, 3);
        GeluidRegistry.fanfare(p, GeluidRegistry.themaVan(droom));
        List<String> l = GeluidRegistry.GESTUURD.getOrDefault(p.getUUID(), List.of());
        helper.assertTrue(l.contains("fase:3/3") && l.contains("fanfare:grauwe_droom"), "phase + fanfare sent: " + l);
        helper.succeed();
    }

    /** The world point sources: a beehive, an anvil, a bell, a lit campfire and the foot of a SMALL fall; a big fall is left alone. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void wereldBronnen(GameTestHelper helper) {
        var lvl = helper.getLevel();
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.BEEHIVE);
        helper.setBlock(2, 1, 1, net.minecraft.world.level.block.Blocks.ANVIL);
        helper.setBlock(3, 1, 1, net.minecraft.world.level.block.Blocks.BELL);
        helper.setBlock(4, 1, 1, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        var valt = net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock();
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(1, y, 3, valt);
        }
        for (int y = 1; y <= 6; y++) {
            helper.setBlock(3, y, 3, valt);
        }
        String[][] verwacht = {{"1,1,1", "bijen"}, {"2,1,1", "smidse"}, {"3,1,1", "windgong"}, {"4,1,1", "kampvuur"}, {"1,1,3", "fontein"},
                {"3,1,3", null}, {"1,2,3", null}, {"5,1,5", null}};
        for (String[] v : verwacht) {
            String[] c = v[0].split(",");
            var pos = helper.absolutePos(new net.minecraft.core.BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])));
            String got = WereldBron.soortOp(lvl, pos);
            helper.assertTrue(java.util.Objects.equals(got, v[1]), v[0] + " -> " + v[1] + ", got " + got);
        }
        helper.assertTrue(!WereldBron.vastePunten().isEmpty(), "the gondola/lift points");
        // NPC voices: narrator silent, Mika its own, otherwise the asked kind (or now and then a piep/blub)
        helper.assertTrue(StemPlan.kies("groet", "verteller", "z1", 0.9) == null, "narrator silent");
        helper.assertTrue("mika".equals(StemPlan.kies("groet", "mika", "z1", 0.9)), "mika");
        helper.assertTrue("vraag".equals(StemPlan.kies("vraag", "guh", "z1", 0.9)), "vraag");
        helper.assertTrue("blub".equals(StemPlan.kies("groet", "guh", "z4", 0.1)), "z4 blub");
        helper.assertTrue(StemPlan.toon("Bram") == StemPlan.toon("Bram"), "stable pitch");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------- U-Z6MUZIEK

    /** Zone 6: the area ids -> themes + beds, the towns, the tower, the box, day/night, the boss ids, the raid alias, the files. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zijdeland(GameTestHelper helper) {
        String[][] verwacht = {{"z6_roze_duinen", "roze_duinen", "duinen"}, {"z6_palmenkloven", "palmenkloven", "oase"},
                {"z6_zoutvlakte", "zoutvlakte", "duinen"}, {"z6_ruinedal", "ruinedal", "duinen"}, {"z6_moederoase", "moederoase", "oase"}};
        for (String[] v : verwacht) {
            GeluidPlan.Streek s = GeluidPlan.streek(v[0], 0, 64, 0);
            helper.assertTrue(s != null && s.themas().contains(v[1]) && s.bed().equals(v[2]), v[0] + " -> " + v[1] + "/" + v[2] + ", got " + s);
        }
        helper.assertTrue(GeluidPlan.streek("z6_zoutvlakte", 0, 64, 0).themas().contains("zoutvlakte_nacht"), "salt flat night theme");
        GeluidPlan.Streek toren = GeluidPlan.streek("z6_toren", 0, 64, 0);
        helper.assertTrue(toren != null && toren.themas().isEmpty() && toren.bed().equals("toren"), "tower: drone only");
        for (String stad : List.of("z6_rozenkloof", "z6_saffraanpoort")) {
            helper.assertTrue(GeluidPlan.streek(stad, 0, 64, 0) == null, stad + ": a town (town tunes)");
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.muziekVan(
                    nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.stadVan(stad)).size() == 2, stad + ": two tunes");
        }
        helper.assertTrue(GeluidPlan.streek("z6_kerker_iets", 0, 64, 0).bed().equals("duinen"), "other z6 id -> zone streek");
        helper.assertTrue(GeluidPlan.streek(null, 3700, 70, -700) != null, "zone-6 box");
        // day / night
        List<String> zv = GeluidPlan.streek("z6_zoutvlakte", 0, 64, 0).themas();
        helper.assertTrue("zoutvlakte_nacht".equals(GeluidPlan.dagNacht(zv, true)) && "zoutvlakte".equals(GeluidPlan.dagNacht(zv, false))
                && GeluidPlan.dagNacht(List.of("hoogvads", "hoogvads_markt"), true) == null, "dagNacht");
        helper.assertTrue(GeluidPlan.nachtBed("duinen").equals("sterren") && GeluidPlan.nachtBed("oase").equals("sterren")
                && GeluidPlan.nachtBed("weide").equals("krekels") && GeluidPlan.vogelBed("duinen") && GeluidPlan.binnenBed("toren"), "night beds");
        // bosses by entity path (u-z6wezens / u-z6raid ids) and the raid's per-phase music ids
        String[][] bazen = {{"vr_z6_baas_wormelien", "wormelien"}, {"vr_z6_baas_zoutkolos", "zoutkolos"},
                {"vr_z6_baas_astrolabiumwachter", "sterrenwacht"}, {"vr_z6_baas_glimmertje", "glimmertje"},
                {"vr_z6_baas_sterrenguh", "verzonken_sterrenguh"}, {"vr_z6raid_baas_zandwachter", "zandwachter"},
                {"vr_z6raid_baas_spiegelvizier", "spiegelvizier"}, {"vr_z6raid_baas_ster_knuffel", "sterrenbeelden"},
                {"vr_z6raid_baas_ster_pluis", "sterrenbeelden"}, {"vr_z6raid_baas_ringeling", "ringeling"}};
        for (String[] v : bazen) {
            helper.assertTrue(GeluidPlan.baasThema(v[0]).equals(v[1]), v[0] + " -> " + v[1] + ", got " + GeluidPlan.baasThema(v[0]));
        }
        GeluidPlan.Alias a1 = GeluidPlan.alias("guhs:vr.z6raid.muziek.ringeling_2"), a2 = GeluidPlan.alias("guhs:vr.z6raid.muziek.ringeling_3"),
                a3 = GeluidPlan.alias("guhs:vr.z6raid.muziek.zandwachter_2"), a4 = GeluidPlan.alias("guhs:vr.z6raid.muziek.sterrenbeelden");
        helper.assertTrue(a1 != null && a1.thema().equals("ringeling") && !a1.laatsteFase() && a2 != null && a2.laatsteFase()
                && a3 != null && a3.laatsteFase() && a4 != null && !a4.laatsteFase(), "raid alias phases");
        helper.assertTrue(GeluidPlan.alias("guhs:vr.z6raid.muziek.opgevrolijkt") == null && GeluidPlan.alias("guhs:vr.geluid.baas.vahoegnir") == null,
                "no alias for other ids");
        helper.assertTrue("spiegelvizier".equals(GeluidPlan.themaVanId("guhs:vr.z6raid.muziek.spiegelvizier_2")), "themaVanId via alias");
        // the files: town tunes, the zone's one-shots, the raid's extra ids
        for (String n : List.of("rozenkloof_1", "rozenkloof_2", "saffraanpoort_1", "saffraanpoort_2")) {
            ogg(helper, "muziek/stad/" + n);
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id("vr.stad.muziek." + n)), "not registered: " + n);
        }
        for (String n : List.of("belletje_1", "sterretje_1", "zoutzang_1")) {
            ogg(helper, "sfeer/" + n);
        }
        // the first visit of De Moederoase plays its own stinger instead of the generic discovery
        ServerPlayer p = GuhMockPlayer.of(helper);
        GeluidRegistry.GESTUURD.remove(p.getUUID());
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "z6_moederoase"));
        List<String> l = GeluidRegistry.GESTUURD.getOrDefault(p.getUUID(), List.of());
        helper.assertTrue(l.contains("moederoase") && !l.contains("ontdekking"), "oasis stinger: " + l);
        helper.succeed();
    }
}
