package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import nl.juiced.guhs.feature.vadsrijk.biomen.VadsrijkBiomen;
import nl.juiced.guhs.feature.vadsrijk.indeling.Barrieres;
import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-Z6WERELD (zone 6 "Het Zijdeland", batch vadsrijk_z6wereld): every spot the other zone-6 slices asked for is on the map; the
 * areas give the right biome, level band and town flag; the barrier between zone 5 and 6 (De Roze Kliffen + de Zijdewind) and the
 * closed east; the world layout knows zone 6.
 */
public class Z6WereldGameTests {
    private static final String B = "vadsrijk_z6wereld";

    /** The ids u-z6verhaal (Z6VerhaalPlekken), u-z6wezens (Z6WezensPlekken) and u-z6raid (Z6RaidPlekken) look up. */
    static final String[] GEVRAAGD = {"haven_z6_z5_vertrek", "rozenkloof_aankomst", "rozenkloof_plein", "rozenkloof_schatkamer",
            "rozenkloof_siq_top", "rozenkloof_luchthaven", "rozenkloof_havenmeester", "rozenkloof_stal", "rozenkloof_tapijtwinkel",
            "rozenkloof_bank", "rozenkloof_post", "rozenkloof_karavaan", "rozenkloof_fontein", "rozenkloof_zeemond", "rozenkloof_tuin_1",
            "saffraanpoort_plein", "saffraanpoort_paleis", "saffraanpoort_spiegelzaal", "spiegelzaal_troon", "saffraanpoort_bazaar",
            "saffraanpoort_poort_west", "saffraanpoort_karavanserai", "saffraanpoort_sterrenwacht", "saffraanpoort_paradijstuin",
            "saffraanpoort_saffraanbrug", "saffraanpoort_bank", "karavanserai_keuken", "sterrenwacht_brouwerij", "paleis_iwan", "kanaalwijk",
            "noria_1", "noria_2", "noria_3", "palmbaken", "duinbaken", "zoutbaken", "ruinebaken", "oasebaken", "verzonken_sterrenwacht",
            "zoutgrot", "zoutgrot_schat", "wormelien", "moederoase", "toren_voet", "toren_top", "zoutvlakte_ziedershuisje",
            "zoutvlakte_ziedershuisje_huis", "zoutvlakte_kamp", "zoutvlakte_mikakamp", "duinen_tent", "duinen_rustplek", "duinen_kam",
            "ruinedal_kamp", "ruinedal_tafelmesa", "oase_tenten", "oase_tent_1", "palmenkloven_monding", "palmenkloven_waterval_onder",
            "palmenkloven_palmenbos", "palmenkloven_moerbei", "oostroute_luchtspiegeling", "zoutgrot_kristallen", "duinen_bobbeltje",
            "geheim_knuffel_1", "geheim_knuffel_8", "geheim_schat_1", "geheim_schat_3", "kampvuur_rozenkloof", "kampvuur_saffraanpoort",
            "kampvuur_duinen", "kampvuur_palmenkloven", "kampvuur_zoutvlakte", "kampvuur_ruinedal", "kampvuur_moederoase", "z6_burger_6",
            "z6_wacht_6", "waterval_3", "rozenkloof_tuin_3", "dak_2",
            // u-z6wezens
            "z6w_mobs_duinen_1", "z6w_mobs_moederoase_3", "z6w_zout_nacht", "z6w_leider_oorschelp", "z6w_leider_brie",
            "z6w_veilig_rozenkloof", "z6w_veilig_toren", "z6w_wormelien", "z6w_wormelien_deur", "z6w_wormelien_bord", "z6w_zoutkolos",
            "z6w_zoutkolos_deur", "z6w_zoutkolos_bord", "z6w_sterrenwacht_ingang", "z6w_zoutgrot_elite",
            // u-z6raid
            "raid_toren_poort", "raid_toren_buiten", "raid_toren_binnen"};

    @GuhTest(template = "empty", batch = B)
    public static void gevraagdePlekken(GameTestHelper helper) {
        for (String id : GEVRAAGD) {
            helper.assertTrue(Z6WereldPlekken.plek(id) != null, "zone-6 spot " + id + " is on the map");
        }
        for (String id : GEVRAAGD) {
            var p = Z6WereldPlekken.plek(id);
            helper.assertTrue(Z6WereldPlekken.inZone6(p.getX(), p.getZ()), id + " lies in zone 6");
        }
        helper.assertTrue(!Z6WereldPlekken.KAMPVUREN.isEmpty() && !Z6WereldPlekken.RIJDIEREN.isEmpty(), "campfires and mounts");
        boolean bult = false, tapijt = false, pluim = false;
        for (Z6WereldPlekken.Rijdier r : Z6WereldPlekken.RIJDIEREN) {
            bult |= r.soort().equals("bultguh");
            tapijt |= r.soort().equals("tapijt");
            pluim |= r.soort().equals("pluimguh");
        }
        helper.assertTrue(bult && tapijt && pluim, "all three mounts (bultguh, flying carpet, Pluimguh) stand somewhere");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void gebiedenEnBiomen(GameTestHelper helper) {
        Language t = Language.getInstance();
        var plein = Z6WereldPlekken.plek("rozenkloof_plein");
        helper.assertTrue("zijdeland_rozenkloof".equals(Z6Wereld.biome(plein.getX(), plein.getZ())), "Rozenkloof biome");
        helper.assertTrue(VadsrijkBiomen.id(plein.getX(), plein.getY(), plein.getZ()).equals("zijdeland_rozenkloof"), "VadsrijkBiomen asks zone 6");
        var paleis = Z6WereldPlekken.plek("saffraanpoort_plein");
        helper.assertTrue("zijdeland_saffraanpoort".equals(Z6Wereld.biome(paleis.getX(), paleis.getZ())), "Saffraanpoort biome");
        var zout = Z6WereldPlekken.plek("zoutbaken");
        helper.assertTrue("zijdeland_zoutvlakte".equals(Z6Wereld.biome(zout.getX(), zout.getZ())), "the Zoutbaken is on the salt flat");
        var toren = Z6WereldPlekken.plek("toren_voet");
        helper.assertTrue(Z6Wereld.biome(toren.getX(), toren.getZ()) != null, "the tower area");
        helper.assertTrue(Z6Wereld.biome(2000, -500) == null, "zone 4 is not zone 6");
        for (Z6WereldPlekken.Gebied g : Z6WereldPlekken.GEBIEDEN) {
            helper.assertTrue(VadsrijkBiomen.IDS.contains(g.biome()), "biome id " + g.biome());
            helper.assertTrue(t.has("biome.guhs.vadsrijk_" + g.biome()) && t.has(g.naamKey()), "names of " + g.id());
            helper.assertTrue(g.minLevel() >= 60 && g.maxLevel() <= 70, "levels 60-70: " + g.id());
        }
        Z6Wereld.gebieden();
        Gebieden.Gebied stad = Gebieden.op(plein.getX() + 0.5, plein.getY(), plein.getZ() + 0.5);
        helper.assertTrue(stad != null && stad.id().equals("z6_rozenkloof") && stad.stad() && stad.minLevel() == 60, "Rozenkloof is a lv-60 town");
        Gebieden.Gebied duin = Gebieden.op(3660.5, 80, -640.5);
        helper.assertTrue(duin != null && duin.id().equals("z6_roze_duinen") && !duin.stad(), "the dunes are open land: " + (duin == null ? "-" : duin.id()));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void barriereEnIndeling(GameTestHelper helper) {
        helper.assertTrue(VadsrijkZones.zoneVan(3762, -846) == VadsrijkZones.Zone.Z6, "Saffraanpoort is zone 6");
        helper.assertTrue(Barrieres.vrij(3762, 71, -846) && Barrieres.vrij(3400, 70, -714), "the cities are free land");
        helper.assertTrue(!Barrieres.vrij(3240, 63, -700) && Barrieres.soort(3240, 63, -700, true).equals("zijdewind"), "the strait is the Zijdewind");
        helper.assertTrue(!Barrieres.vrij(4260, 63, -740) && Barrieres.soort(4260, 63, -740, false).equals("oostgrens"), "the closed east");
        helper.assertTrue(Language.getInstance().has("vadsrijk.indeling.barriere.zijdewind"), "barrier text");
        helper.succeed();
    }
}
