package nl.juiced.guhs.feature.vadsrijk.biomen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Fix round 2 "biomen": the Vadsrijk dimension json decodes to our generator + biome source (the gametest server has no
 * datapack dimensions, so the json is decoded here), and that source gives the right biome per area; every biome places the
 * map feature guhs:vr_kaart. Run: {@code runGameTestServer -Pgt=VadsrijkBiomen}.
 */
public class VadsrijkBiomenGameTests {
    private static final String BATCH = "vadsrijk_biomen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void biomePerGebied(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var res = server.getResourceManager().getResource(Guhs.id("dimension/vadsrijk.json")).orElseThrow();
        JsonElement json;
        try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(r);
        }
        LevelStem stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), json).getOrThrow();
        helper.assertTrue(stem.generator() instanceof VadsrijkGenerator, "generator is " + stem.generator().getClass().getSimpleName());
        BiomeSource bron = stem.generator().getBiomeSource();
        helper.assertTrue(bron instanceof VadsrijkBiomeSource, "biome source is " + bron.getClass().getSimpleName());
        VadsrijkBiomeSource vb = (VadsrijkBiomeSource) bron;

        // the centre of every zone-1 area (the Kloof cuts through none of the centres)
        for (String id : new String[]{"zwamzoet", "haven", "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel", "knuffelkamp"}) {
            var g = Gebieden.van(id);
            helper.assertTrue(g != null, "area " + id + " missing");
            var m = g.midden(64);
            verwacht(helper, vb, m.getX(), m.getZ(), id);
        }
        verwacht(helper, vb, WereldPlekken.AANKOMST.getX(), WereldPlekken.AANKOMST.getZ(), "knuffelkamp");
        verwacht(helper, vb, -499, -484, "druipgrotten");   // the Druipschacht
        verwacht(helper, vb, 240, -300, "kloof");           // on the ravine line
        verwacht(helper, vb, 0, 280, "tussenland");         // between Zwamzoet and De Haven (zone-1 ring)
        verwacht(helper, vb, 0, 750, "zee");                // between the ring and Knuffelkamp
        verwacht(helper, vb, 0, -1200, "zee");              // between zone 1 and zone 2
        // zone 2
        verwacht(helper, vb, Z2WereldPlekken.DOK_LANDING.getX(), Z2WereldPlekken.DOK_LANDING.getZ(), "kandijkom");
        verwacht(helper, vb, 0, -2400, "kandijkom");
        verwacht(helper, vb, 170, -2350, "galmhellingen");
        verwacht(helper, vb, -172, -2378, "knuffelmeer");
        verwacht(helper, vb, -100, -2545, "splinterkloof");
        verwacht(helper, vb, 150, -2693, "spiegelburcht");
        verwacht(helper, vb, 280, -2500, "splinterpieken");   // g-int: g-z2's ring (the cloud sea round the 500x575 island)
        // every biome is a possible biome, and places the map
        var kaart = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE, Guhs.id("vr_kaart")));
        for (Holder<Biome> b : vb.possibleBiomes()) {
            boolean heeft = false;
            for (var stap : b.value().getGenerationSettings().features()) {
                for (Holder<PlacedFeature> f : stap) {
                    heeft |= f.is(kaart.key());
                }
            }
            helper.assertTrue(heeft, "biome " + b.getRegisteredName() + " has no guhs:vr_kaart");
        }
        helper.assertTrue(vb.possibleBiomes().size() == VadsrijkBiomen.IDS.size() + 1, "possible biomes " + vb.possibleBiomes().size());
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, VadsrijkBiomeSource vb, int x, int z, String id) {
        String naam = vb.biomeOp(x, z).getRegisteredName();
        helper.assertTrue(naam.equals("guhs:vadsrijk_" + id), "at " + x + ", " + z + ": " + naam + " instead of guhs:vadsrijk_" + id);
        // (and through the quart-based getNoiseBiome)
        String q = vb.getNoiseBiome(x >> 2, 16, z >> 2, null).getRegisteredName();
        helper.assertTrue(q.equals(naam), "getNoiseBiome at " + x + ", " + z + ": " + q);
    }
}
