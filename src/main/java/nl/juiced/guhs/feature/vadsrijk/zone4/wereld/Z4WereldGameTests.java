package nl.juiced.guhs.feature.vadsrijk.zone4.wereld;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.dimension.LevelStem;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.biomen.VadsrijkBiomeSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-Z4WERELD: zone 4 "De Lampionkust": the generated plan holds every id the other zone-4 slices rely on, the areas/biomes
 * answer per island (also through kern Gebieden and the dimension's biome source), the Kringstroom pushes only at the edge
 * and the Grauwe Mistbank only sits in the south-east. Run: {@code runGameTestServer -Pgt=Z4Wereld}.
 */
public class Z4WereldGameTests {
    private static final String BATCH = "vadsrijk_z4wereld";

    @GuhTest(template = "empty", batch = BATCH)
    public static void plekkenEnRanden(GameTestHelper helper) {
        for (String id : new String[]{"respawn", "haven_3_4_landing", "haven_3_4_bel", "haven_3_4_kapitein", "haven_4_5_landing",
                "haven_4_5_bel", "opstijgpier", "arena_zeeslurf", "arena_vliegervanger", "wrak_ingang", "boot_dok_lampionhaven",
                "uitkijk_lampionduin", "uitkijk_vuurtoren", "gebouw.herberg.npc", "gebouw.vliegerwinkel.npc", "gebouw.bank.npc",
                "gebouw.havenmeester.npc", "gebouw.opvangcentrum.npc", "mysterie_poort", "schildpad"}) {
            helper.assertTrue(Z4WereldPlekken.plek(id) != null, "plek " + id + " missing");
        }
        helper.assertTrue(Z4WereldPlekken.KAMPVUREN.size() >= 9, "campfires " + Z4WereldPlekken.KAMPVUREN.size());
        helper.assertTrue(Z4WereldPlekken.DOKKEN.size() >= 10, "docks " + Z4WereldPlekken.DOKKEN.size());
        helper.assertTrue(Z4WereldPlekken.SCHILDPAD_ROUTE.length >= 12, "turtle route");
        for (BlockPos b : Z4WereldPlekken.ALLE.values()) {
            helper.assertTrue(Z4WereldPlekken.inZone4(b.getX(), b.getZ()), "plek outside the zone-4 grid: " + b);
        }
        BlockPos r = Z4WereldPlekken.plek("respawn");
        helper.assertTrue(Z4WereldPlekken.stroming(r.getX(), r.getZ()) == 0, "current in Lampionhaven");
        helper.assertTrue(Z4WereldPlekken.stroming(1995, -760) > 0.5, "no current at the south edge");
        helper.assertTrue(Z4WereldPlekken.mist(r.getX(), r.getZ()) == 0, "mist in Lampionhaven");
        BlockPos m = Z4WereldPlekken.plek("mysterie_poort");
        helper.assertTrue(Z4WereldPlekken.mist(m.getX(), m.getZ()) > 0.9, "no mist at the grey gate");
        // the areas: own lookup + kern Gebieden (flat boxes in front)
        Z4WereldRegistry.gebieden();
        Gebieden.Gebied g = Gebieden.op(r.getX(), r.getY(), r.getZ());
        helper.assertTrue(g != null && g.id().equals("z4_lampionhaven") && g.stad(), "kern area at the respawn: " + (g == null ? "-" : g.id()));
        BlockPos k = Z4WereldPlekken.plek("eiland_koraalbaai");
        Gebieden.Gebied gk = Gebieden.op(k.getX(), k.getY(), k.getZ());
        helper.assertTrue(gk != null && gk.id().equals("z4_koraalbaai") && gk.minLevel() >= 50, "kern area koraalbaai: " + (gk == null ? "-" : gk.id()));
        Gebieden.Gebied gz = Gebieden.op(2000, 63, -720);
        helper.assertTrue(gz != null && gz.id().equals("z4_zee"), "kern area at sea: " + (gz == null ? "-" : gz.id()));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void biomen(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var res = server.getResourceManager().getResource(Guhs.id("dimension/vadsrijk.json")).orElseThrow();
        JsonElement json;
        try (Reader rd = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(rd);
        }
        LevelStem stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), json).getOrThrow();
        VadsrijkBiomeSource vb = (VadsrijkBiomeSource) stem.generator().getBiomeSource();
        verwacht(helper, vb, Z4WereldPlekken.plek("respawn"), "lampionhaven");
        verwacht(helper, vb, Z4WereldPlekken.plek("haven_3_4_landing"), "lampionhaven");
        verwacht(helper, vb, Z4WereldPlekken.plek("eiland_koraalbaai"), "koraalbaai");
        verwacht(helper, vb, Z4WereldPlekken.plek("eiland_vliegereiland"), "vliegereiland");
        verwacht(helper, vb, Z4WereldPlekken.plek("mysterie_poort"), "mysterie");
        verwacht(helper, vb, Z4WereldPlekken.plek("knuffelwas"), "knuffeltij");
        verwacht(helper, vb, Z4WereldPlekken.plek("wrak_ingang"), "wrak");
        verwacht(helper, vb, new BlockPos(2000, 63, -720), "lampionkust");
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, VadsrijkBiomeSource vb, BlockPos p, String id) {
        String naam = vb.biomeOp(p.getX(), p.getZ()).getRegisteredName();
        helper.assertTrue(naam.equals("guhs:vadsrijk_" + id), "at " + p.toShortString() + ": " + naam + " instead of guhs:vadsrijk_" + id);
    }
}
