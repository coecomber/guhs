package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.biomen.VadsrijkBiomen;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-Z5WERELD (zone 5 "Hoogvads", batch vadsrijk_z5wereld): the Hoogvads blocks exist with names and the banner is a thin cloth;
 * every spot the story and combat slices asked for is on the map; districts, kern areas and biomes by HEIGHT (zone 4 lies below the
 * whale in the same x/z); the Wolkvangnet rule.
 */
public class Z5WereldGameTests {
    private static final String B = "vadsrijk_z5wereld";

    /** The ids t-z5verhaal (Z5VerhaalPlekken) and t-z5wezens (Z5WezensPlekken) look up in Z5WereldPlekken. */
    static final String[] GEVRAAGD = {"luchthaven_aankomst", "luchthaven_station", "zachtmoedlaan", "markt_fontein", "soepkeuken_ketel",
            "raadszaal_lessenaar", "troonzaal_troon", "academie_oefenplein", "universiteit_binnenhof", "kathedraal_poort", "kathedraal_altaar",
            "staartpark_vijver", "ooggaarde_tuin", "balkon_oog", "spuitgat_midden", "wolkwaldroom_arena", "raadszaal_zijkamer",
            "luchthaven_havenmeester", "academie_kapitein", "academie_kazerne", "universiteit_bibliotheek", "soepkeuken_tafel", "bank_balie",
            "universiteit_archief", "academie_toren", "paleis_kanselarij", "soepkeuken_hoek", "soepkeuken_bank", "paleis_hof", "markt_bakker",
            "markt_kleermaker", "markt_smid", "markt_juwelier", "luchthaven_pier_west", "gilde_hal", "gilde_keuken", "gilde_brouwerij",
            "gilde_smidse", "gilde_atelier", "gilde_bijenstal", "academie_kastjes", "luchthaven_logboek", "luchthaven_pier_oost",
            "paleis_kanselarij_bureau", "universiteit_sterrenwacht", "spuitgat_noord", "spuitgat_zuid", "kathedraal_apsis",
            "kathedraal_roos_noord", "kathedraal_roos_zuid", "kathedraal_geveltoren", "staartvin_top", "universiteit_sterrenwacht_top",
            "kampvuur_luchthaven", "kampvuur_markt", "kampvuur_staartpark", "kampvuur_ooggaarde", "soepkeuken_rij_4", "burger_6", "wolkwacht_6",
            "academie_parcours_4", "staartpark_boom_4", "geheim_knuffel_8", "geheim_schat_3", "wolkbes_8", "wolbol_3",
            // t-z5wezens
            "z5w_proefarena", "z5w_buik_luik", "z5w_buik_uit", "z5w_schuilnest_luik", "z5w_schuilnest_uit", "z5w_zuidvin", "z5w_noordvin",
            "z5w_lift_zuidvin_boven", "z5w_lift_zuidvin_onder", "z5w_lift_noordvin_boven", "z5w_lift_noordvin_onder", "z5w_lift_staart_boven",
            "z5w_lift_staart_onder", "z5w_staartvlakte_noord", "z5w_donderbuik_arena", "z5w_donderbuik_deur", "z5w_wolkwaldroom_deur",
            "z5w_leider_strobaal", "z5w_leider_mistmoeder", "z5w_buikgangen_min", "z5w_buikgangen_max",
            // t-luchtschip (line D, zone 5 side)
            "haven_z5_z4_vertrek", "haven_5_4_landing", "haven_5_4_bel", "haven_5_4_vertrekbord", "haven_5_4_kapitein"};

    @GuhTest(template = "empty", batch = B)
    public static void blokken(GameTestHelper helper) {
        Language t = Language.getInstance();
        helper.assertTrue(Z5Blokken.PER_ID.size() == 30, "30 Hoogvads blocks: " + Z5Blokken.PER_ID.size());
        for (String id : Z5Blokken.PER_ID.keySet()) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("guhs", id));
            helper.assertTrue(b != Blocks.AIR && b == Z5Blokken.blok(id), "block " + id + " is registered");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("guhs", id)), "item " + id);
            helper.assertTrue(t.has("block.guhs." + id), "name of " + id);
        }
        BlockPos p = new BlockPos(1, 2, 1);
        helper.setBlock(p, Z5Blokken.blok("hoogvads_vaandel_onder").defaultBlockState().setValue(Z5Blokken.Vaandel.FACING, Direction.EAST));
        BlockState s = helper.getBlockState(p);
        helper.assertTrue(s.getCollisionShape(helper.getLevel(), helper.absolutePos(p)).isEmpty(), "the banner has no collision");
        helper.assertTrue(s.getShape(helper.getLevel(), helper.absolutePos(p)).max(Direction.Axis.X) <= 0.2, "the banner is a thin cloth");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void plekkenVoorAndereSlices(GameTestHelper helper) {
        for (String id : GEVRAAGD) {
            BlockPos b = Z5WereldPlekken.plek(id);
            helper.assertTrue(b != null, "spot " + id + " is on the map (POI_Z5)");
            if (!id.startsWith("z5w_buikgangen")) {
                helper.assertTrue(b.getY() >= Z5WereldPlekken.MIN_Y && b.getY() < 320, id + " lies in zone 5 (y " + b.getY() + ")");
            }
        }
        helper.assertTrue(Z5WereldPlekken.HUIZEN.stream().anyMatch(h -> h.rol().equals("herberg")), "an inn");
        helper.assertTrue(Z5WereldPlekken.HUIZEN.stream().anyMatch(h -> h.rol().equals("bank")), "a bank");
        helper.assertTrue(Z5WereldPlekken.HUIZEN.stream().filter(h -> h.rol().equals("winkel")).count() >= 6, "shops");
        helper.assertTrue(Z5WereldPlekken.BEWONERS.size() > 300, "the townsfolk of the renders: " + Z5WereldPlekken.BEWONERS.size());
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void wijkenOpHoogte(GameTestHelper helper) {
        BlockPos altaar = Z5WereldPlekken.plek("kathedraal_altaar");
        BlockPos markt = Z5WereldPlekken.plek("markt_fontein");
        BlockPos troon = Z5WereldPlekken.plek("troonzaal_audientie");
        verwacht(helper, altaar, "z5_kathedraal", "hoogvads_kathedraal");
        verwacht(helper, markt, "z5_marktlaan", "hoogvads_markt");
        verwacht(helper, troon, "z5_paleisheuvel", "hoogvads_paleis");
        // the same x/z far below: zone 4's sea, not Hoogvads
        helper.assertTrue(Z5WereldPlekken.wijk(altaar.getX(), 70, altaar.getZ()) == null, "no zone-5 district down at the sea");
        String onder = VadsrijkBiomen.id(altaar.getX(), 70, altaar.getZ());
        helper.assertTrue(!onder.startsWith("hoogvads") && !onder.equals("moeder_wolkwal"), "below the whale another biome: " + onder);
        Z5WereldRegistry.gebieden();
        Gebieden.Gebied g = Gebieden.op(markt.getX() + 0.5, markt.getY(), markt.getZ() + 0.5);
        helper.assertTrue(g != null && g.id().equals("z5_marktlaan") && g.stad() && g.minLevel() == 56 && g.maxLevel() == 60,
                "kern area at the market: " + (g == null ? "-" : g.id()));
        Gebieden.Gebied laag = Gebieden.op(markt.getX() + 0.5, 70, markt.getZ() + 0.5);
        helper.assertTrue(laag == null || !laag.id().startsWith("z5_"), "kern area at the sea below is not zone 5");
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, BlockPos p, String wijk, String biome) {
        Z5WereldPlekken.Wijk w = Z5WereldPlekken.wijk(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        helper.assertTrue(w != null && w.id().equals(wijk), "district at " + p.toShortString() + ": " + (w == null ? "-" : w.id()) + " (want " + wijk + ")");
        helper.assertTrue(VadsrijkBiomen.id(p.getX(), p.getY(), p.getZ()).equals(biome), "biome at " + p.toShortString() + ": "
                + VadsrijkBiomen.id(p.getX(), p.getY(), p.getZ()));
    }

    @GuhTest(template = "empty", batch = B)
    public static void wolkvangnet(GameTestHelper helper) {
        BlockPos pier = Z5WereldPlekken.plek("haven_z5_z4_vertrek");
        long nu = 10_000;
        helper.assertTrue(Z5WereldRegistry.vangen(pier.getX(), 120, pier.getZ() + 20, -1.5, nu, nu - 40), "falling off the pier is caught");
        helper.assertTrue(!Z5WereldRegistry.vangen(pier.getX(), 120, pier.getZ() + 20, -1.5, nu, null), "never on the whale: no catch");
        helper.assertTrue(!Z5WereldRegistry.vangen(pier.getX(), 120, pier.getZ() + 20, -1.5, nu, nu - Z5WereldRegistry.VANG_TIJD - 1),
                "long ago on the whale (took the airship down): no catch");
        helper.assertTrue(!Z5WereldRegistry.vangen(pier.getX(), 200, pier.getZ(), -1.5, nu, nu - 40), "still above the catch height");
        helper.assertTrue(!Z5WereldRegistry.vangen(1800, 120, -470, -1.5, nu, nu - 40), "far from the whale (Lampionhaven): no catch");
        BlockPos terug = Z5WereldRegistry.dichtstbijzijnde(pier);
        helper.assertTrue(terug != null && terug.getY() > 200, "set back on the whale");
        helper.succeed();
    }
}
