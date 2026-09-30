package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.onderwater.GuhbubbelStructure;
import nl.juiced.guhs.feature.verhaal.wereld.Regio;
import nl.juiced.guhs.feature.verhaal.wereld.RegioJigsawStructure;
import nl.juiced.guhs.feature.verhaal.wereld.RegioPiekPlacement;
import nl.juiced.guhs.feature.verhaal.wereld.RegioPlek;

/**
 * Game tests of the 3.0 worldgen (tools/features/verhaal_wereld.py + feature/verhaal/wereld). The GameTest server has no
 * Guhmension, so the dimension JSON is sampled with RandomState (like GuhpolderGameTests.guhpolderDeelEnRuimte): the two new
 * biomes take their share, and outside them (with their terms undone) no other biome changes; one region structure per
 * region (Nomguh per tundra, the capsule per island); no Guhwai'i island near a Guhbubbel peak (it still spawns in every sea);
 * guh villagers in both biomes; the region structures load (types, placements).
 */
public class VerhaalWereldGameTests {
    private static final String EMPTY = "empty";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
    private static final long[] SEEDS = {1L, 20300101L, -778899L};

    // the terms of verhaal_wereld.py (keep in sync: the undo must be exact)
    static final double[] TOENDRA_TERM = {0.51, 0.53}, SEA_OFF = {0.28, 0.30}, KNUFFEL_OFF = {0.40, 0.46}, POLDER_OFF = {0.50, 0.56};
    static final double TOENDRA_CONT = 3.0, GUHWAII_CONT = -3.0;
    static final double EILAND_ZEE = 0.42, EILAND_STRAND = 0.56;
    static final double[] KUST = {-0.13, -0.05, 0.34, 0.37}, TOENDRA_OFF = {0.46, 0.50};   // (1.0.0: + diepzee.ZEE_KRIMP 0.07)

    static double spline(double v, double a, double b, double va, double vb) {
        if (v <= a) {
            return va;
        }
        if (v >= b) {
            return vb;
        }
        double u = (v - a) / (b - a);
        return va + (vb - va) * u * u * (3 - 2 * u);
    }

    static double kust(double zee) {
        return zee < KUST[1] ? spline(zee, KUST[0], KUST[1], 0, 1) : spline(zee, KUST[2], KUST[3], 1, 0);
    }

    record Ruis(NormalNoise toendra, NormalNoise guhwaii, NormalNoise zee, NormalNoise knuffel, NormalNoise polder) {
        double toendraTerm(double x, double z) {
            return spline(toendra.getValue(x, 0, z), TOENDRA_TERM[0], TOENDRA_TERM[1], 0, 1)
                    * spline(zee.getValue(x, 0, z), SEA_OFF[0], SEA_OFF[1], 1, 0)
                    * spline(knuffel.getValue(x, 0, z), KNUFFEL_OFF[0], KNUFFEL_OFF[1], 1, 0)
                    * spline(polder.getValue(x, 0, z), POLDER_OFF[0], POLDER_OFF[1], 1, 0);
        }

        double guhwaiiTerm(double x, double z) {
            return spline(guhwaii.getValue(x, 0, z), EILAND_ZEE - 0.01, EILAND_ZEE, 0, 1) * plank(x, z);
        }

        /** Where a Guhwai'i region may be: on the deep seas' coasts, away from the Knuffeldal, the Guhpolder and the tundra. */
        double plank(double x, double z) {
            return kust(zee.getValue(x, 0, z)) * spline(knuffel.getValue(x, 0, z), KNUFFEL_OFF[0], KNUFFEL_OFF[1], 1, 0)
                    * spline(polder.getValue(x, 0, z), POLDER_OFF[0], POLDER_OFF[1], 1, 0)
                    * spline(toendra.getValue(x, 0, z), TOENDRA_OFF[0], TOENDRA_OFF[1], 1, 0);
        }
    }

    static Ruis ruis(RandomState random) {
        return new Ruis(random.getOrCreateNoise(VerhaalFeature.TOENDRA_NOISE), random.getOrCreateNoise(VerhaalFeature.GUHWAII_NOISE),
                random.getOrCreateNoise(noise("guhmension_zee")), random.getOrCreateNoise(noise("guhmension_knuffel")),
                random.getOrCreateNoise(noise("guhmension_polder")));
    }

    static ResourceKey<NormalNoise.NoiseParameters> noise(String id) {
        return ResourceKey.create(Registries.NOISE, Guhs.id(id));
    }

    static JsonObject json(GameTestHelper helper, String path) {
        var server = helper.getLevel().getServer();
        try (var reader = server.getResourceManager().getResource(Guhs.id(path)).orElseThrow().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The share of the two biomes (the tundra ~3-5 % of the Guhmension, the islands in part of the seas) and nothing else moves:
     * with both terms undone on the sampler's target point, the biome source without them gives the very same biome.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 6000)
    public static void verhaalWereldDeelEnRuimte(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        JsonObject source = json(helper, "dimension/guhmension.json").getAsJsonObject("generator").getAsJsonObject("biome_source");
        JsonObject without = source.deepCopy();
        JsonArray kept = new JsonArray();
        for (JsonElement e : without.getAsJsonArray("biomes")) {
            String b = e.getAsJsonObject().get("biome").getAsString();
            if (!b.equals("guhs:sneeuwguhtoendra") && !b.equals("guhs:guhwaii")) {
                kept.add(e);
            }
        }
        without.add("biomes", kept);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource with = BiomeSource.CODEC.parse(ops, source).getOrThrow();
        MultiNoiseBiomeSource before = (MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, without).getOrThrow();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        Map<String, Integer> now = new HashMap<>(), then = new HashMap<>();
        final int step = 32, half = 6400, n = 2 * half / step;
        int samples = 0, anders = 0, zeeen = 0, toendras = 0, groteToendras = 0, breedsteToendra = 0;
        int guhwaiis = 0, groteGuhwaiis = 0, metEiland = 0, eilandOppervlak = 0, breedteGuhwaii = 0;
        StringBuilder report = new StringBuilder();
        for (long seed : SEEDS) {
            RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            Climate.Sampler sampler = random.sampler();
            Ruis r = ruis(random);
            String[][] bioom = new String[n][n];
            boolean[][] zee = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = -half + i * step, z = -half + j * step;
                    int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(100), qz = QuartPos.fromBlock(z);
                    String b = with.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath();
                    double tt = r.toendraTerm(x, z), tg = r.guhwaiiTerm(x, z);
                    Climate.TargetPoint tp = sampler.sample(qx, qy, qz);
                    Climate.TargetPoint undone = new Climate.TargetPoint(tp.temperature(), tp.humidity(),
                            tp.continentalness() - Climate.quantizeCoord((float) (tt * TOENDRA_CONT + tg * GUHWAII_CONT)), tp.erosion(),
                            tp.depth(), tp.weirdness());   // (both shifts undone)
                    String was = before.getNoiseBiome(undone).unwrapKey().orElseThrow().identifier().getPath();
                    now.merge(b, 1, Integer::sum);
                    then.merge(was, 1, Integer::sum);
                    if (!b.equals("sneeuwguhtoendra") && !b.equals("guhwaii") && !b.equals(was)) {
                        anders++;
                    }
                    bioom[i][j] = b;
                    zee[i][j] = b.equals("diepe_guhzee") || b.equals("guhwaii");
                    samples++;
                }
            }
            // regions: the tundras (how wide), the deep seas, the Guhwai'i regions (how wide, with an island in the middle?)
            int[][] label = new int[n][n];
            int labels = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    String soort = bioom[i][j];
                    if (label[i][j] != 0 || !(soort.equals("sneeuwguhtoendra") || soort.equals("diepe_guhzee") || soort.equals("guhwaii"))) {
                        continue;
                    }
                    labels++;
                    int area = 0, eiland = 0, minI = i, maxI = i, minJ = j, maxJ = j;
                    ArrayDeque<int[]> todo = new ArrayDeque<>();
                    todo.add(new int[]{i, j});
                    label[i][j] = labels;
                    while (!todo.isEmpty()) {
                        int[] c = todo.poll();
                        area++;
                        minI = Math.min(minI, c[0]);
                        maxI = Math.max(maxI, c[0]);
                        minJ = Math.min(minJ, c[1]);
                        maxJ = Math.max(maxJ, c[1]);
                        if (soort.equals("guhwaii") && r.guhwaii().getValue(-half + c[0] * step, 0, -half + c[1] * step) >= EILAND_STRAND) {
                            eiland++;
                        }
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], bb = c[1] + d[1];
                            if (a >= 0 && bb >= 0 && a < n && bb < n && label[a][bb] == 0 && bioom[a][bb].equals(soort)) {
                                label[a][bb] = labels;
                                todo.add(new int[]{a, bb});
                            }
                        }
                    }
                    boolean groot = area * step * step >= 60000;
                    int breed = Math.min(maxI - minI + 1, maxJ - minJ + 1) * step;
                    switch (soort) {
                        case "diepe_guhzee" -> zeeen += groot ? 1 : 0;
                        case "guhwaii" -> {
                            guhwaiis++;
                            if (area * step * step >= 30000) {
                                groteGuhwaiis++;
                                metEiland += eiland > 0 ? 1 : 0;
                                eilandOppervlak += eiland;
                                breedteGuhwaii += breed;
                            }
                        }
                        default -> {
                            toendras++;
                            if (groot) {
                                groteToendras++;
                                breedsteToendra = Math.max(breedsteToendra, breed);
                            }
                        }
                    }
                }
            }
            int e = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    e += bioom[i][j].equals("guhwaii") ? 1 : 0;
                }
            }
            report.append(String.format("seed %d: %d guhwaii samples; ", seed, e));
        }
        for (String b : new java.util.TreeSet<>(now.keySet())) {
            report.append(String.format("%s %.2f%% -> %.2f%%; ", b, 100.0 * then.getOrDefault(b, 0) / samples, 100.0 * now.get(b) / samples));
        }
        double toendra = now.getOrDefault("sneeuwguhtoendra", 0) / (double) samples, guhwaii = now.getOrDefault("guhwaii", 0) / (double) samples;
        report.append(String.format("toendra %.2f%% (%d regions, %d big, widest %d), guhwaii %.2f%% (%d regions, %d big, avg width %d, "
                        + "%d with an island, avg island %.0f m2), big deep seas %d, changed elsewhere %d",
                100 * toendra, toendras, groteToendras, breedsteToendra, 100 * guhwaii, guhwaiis, groteGuhwaiis,
                groteGuhwaiis == 0 ? 0 : breedteGuhwaii / groteGuhwaiis, metEiland, metEiland == 0 ? 0.0 : eilandOppervlak * step * step / (double) metEiland,
                zeeen, anders));
        LOGGER.info("Verhaal wereld: {}", report);
        helper.assertTrue(anders <= samples / 2000, "outside the new biomes every biome stays the same: " + report);
        helper.assertTrue(toendra >= 0.025 && toendra <= 0.06, "the Sneeuwguhtoendra's share: " + report);
        helper.assertTrue(groteToendras >= 4 && breedsteToendra >= 256, "big tundras: " + report);
        helper.assertTrue(guhwaii >= 0.002 && groteGuhwaiis * 6 >= zeeen && groteGuhwaiis <= zeeen && metEiland * 10 >= groteGuhwaiis * 8,
                "Guhwai'i regions by part of the seas, nearly all with an island: " + report);
        for (var e : then.entrySet()) {
            double was = e.getValue() / (double) samples, is = now.getOrDefault(e.getKey(), 0) / (double) samples;
            if (was >= 0.01 && !e.getKey().equals("diepe_guhzee")) {
                helper.assertTrue(is >= 0.8 * was, e.getKey() + " keeps most of its share: " + report);
            }
        }
        helper.succeed();
    }

    /**
     * One region structure per region: the spots of Nomguh (one per tundra peak) and of the island capsule (one per island) lie
     * inside their biome, never two in one region; and no Guhwai'i island reaches a Guhbubbel peak or its rings (so the bubble
     * still spawns in every sea).
     */
    @GuhTest(template = EMPTY, timeoutTicks = 6000)
    public static void verhaalWereldEenPerRegio(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        BiomeSource with = BiomeSource.CODEC.parse(ops, json(helper, "dimension/guhmension.json").getAsJsonObject("generator")
                .getAsJsonObject("biome_source")).getOrThrow();
        // the placements load and the structures are of our type
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        var structures = access.lookupOrThrow(Registries.STRUCTURE);
        Map<String, RegioPlek> plekken = new HashMap<>();
        for (String name : List.of("nomguh", "guhwaii_capsule", "guhwaii_ohana", "guhwaii_surfstrand", "kloon_eiland")) {
            var set = sets.getValue(Guhs.id(name));
            helper.assertTrue(set != null && set.placement() instanceof RegioPiekPlacement, name + ": a regio_piek placement");
            helper.assertTrue(structures.getValue(Guhs.id(name)) instanceof RegioJigsawStructure, name + ": a regio_jigsaw structure");
            plekken.put(name, ((RegioPiekPlacement) set.placement()).plek());
        }
        StringBuilder report = new StringBuilder();
        int nomguhs = 0, capsules = 0, fouten = 0, bubbels = 0, dubbel = 0;
        final int half = 4096, step = 32, n = 2 * half / step;
        for (long seed : SEEDS) {
            Regio.vergeet();
            RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            Climate.Sampler sampler = random.sampler();
            Ruis r = ruis(random);
            // the regions (connected samples) of each biome
            int[][] regio = new int[n][n];
            String[][] bioom = new String[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    bioom[i][j] = with.getNoiseBiome(QuartPos.fromBlock(-half + i * step), QuartPos.fromBlock(100), QuartPos.fromBlock(-half + j * step),
                            sampler).unwrapKey().orElseThrow().identifier().getPath();
                }
            }
            int labels = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (regio[i][j] != 0 || !(bioom[i][j].equals("sneeuwguhtoendra") || bioom[i][j].equals("guhwaii"))) {
                        continue;
                    }
                    labels++;
                    String soort = bioom[i][j];
                    ArrayDeque<int[]> todo = new ArrayDeque<>();
                    todo.add(new int[]{i, j});
                    regio[i][j] = labels;
                    while (!todo.isEmpty()) {
                        int[] c = todo.poll();
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], bb = c[1] + d[1];
                            if (a >= 0 && bb >= 0 && a < n && bb < n && regio[a][bb] == 0 && bioom[a][bb].equals(soort)) {
                                regio[a][bb] = labels;
                                todo.add(new int[]{a, bb});
                            }
                        }
                    }
                }
            }
            for (String name : List.of("nomguh", "guhwaii_capsule")) {
                RegioPlek plek = plekken.get(name);
                String soort = name.equals("nomguh") ? "sneeuwguhtoendra" : "guhwaii";
                Map<Integer, Integer> perRegio = new HashMap<>();
                int cel = plek.cell() * 16;
                for (int cx = Math.floorDiv(-half, cel) + 1; cx < Math.floorDiv(half, cel) - 1; cx++) {
                    for (int cz = Math.floorDiv(-half, cel) + 1; cz < Math.floorDiv(half, cel) - 1; cz++) {
                        Optional<RegioPlek.Plek> p = plek.plek(seed, cx, cz);
                        if (p.isEmpty()) {
                            continue;
                        }
                        int i = Math.floorDiv(p.get().x() + half, step), j = Math.floorDiv(p.get().z() + half, step);
                        if (i < 0 || j < 0 || i >= n || j >= n) {
                            continue;
                        }
                        // the spot lies in its biome (or right next to it: a sample is 32 blocks)
                        boolean in = false;
                        for (int a = -2; a <= 2 && !in; a++) {
                            for (int b = -2; b <= 2 && !in; b++) {
                                int ii = i + a, jj = j + b;
                                in = ii >= 0 && jj >= 0 && ii < n && jj < n && bioom[ii][jj].equals(soort);
                            }
                        }
                        if (!in) {
                            fouten++;
                            report.append(String.format("%s at %d %d is not in %s; ", name, p.get().x(), p.get().z(), soort));
                            continue;
                        }
                        int reg = regio[i][j] != 0 ? regio[i][j] : -1;
                        if (reg > 0 && perRegio.merge(reg, 1, Integer::sum) > 1) {
                            dubbel++;   // (a region with two lobes joined by a bent neck may get one per lobe, like the Knuffeldal)
                            report.append(String.format("two %s in one region (at %d %d); ", name, p.get().x(), p.get().z()));
                        }
                        if (name.equals("nomguh")) {
                            nomguhs++;
                        } else {
                            capsules++;
                        }
                    }
                }
            }
            // the Guhbubbel peaks: no island on the peak or its rings (r 46 / 62)
            NormalNoise zee = r.zee();
            for (int bx = -half / 64; bx < half / 64; bx++) {
                for (int bz = -half / 64; bz < half / 64; bz++) {
                    GuhbubbelStructure.Peak p = GuhbubbelStructure.peak(seed, zee, 4, bx, bz);
                    if (p.value() < RegioPlek.BUBBEL_MIN || !GuhbubbelStructure.highestOfItsSea(seed, zee, 4, 8, RegioPlek.BUBBEL_ZEE, bx, bz)) {
                        continue;
                    }
                    bubbels++;
                    double max = r.guhwaiiTerm(p.x(), p.z());
                    for (int ring : new int[]{46, 62}) {
                        for (int k = 0; k < 16; k++) {
                            double a = Math.PI * 2 * k / 16;
                            max = Math.max(max, r.guhwaiiTerm(p.x() + Math.cos(a) * ring, p.z() + Math.sin(a) * ring));
                        }
                    }
                    if (max > 0.0) {
                        fouten++;
                        report.append(String.format("an island near the Guhbubbel at %d %d (%.2f); ", p.x(), p.z(), max));
                    }
                }
            }
        }
        // the kloon-eilanden (one per big deep sea)
        int kloons = 0;
        RegioPlek kloon = plekken.get("kloon_eiland");
        for (long seed : SEEDS) {
            Regio.vergeet();
            int cel = kloon.cell() * 16;
            for (int cx = Math.floorDiv(-half, cel) + 1; cx < Math.floorDiv(half, cel) - 1; cx++) {
                for (int cz = Math.floorDiv(-half, cel) + 1; cz < Math.floorDiv(half, cel) - 1; cz++) {
                    kloons += kloon.plek(seed, cx, cz).isPresent() ? 1 : 0;
                }
            }
        }
        report.append(String.format("%d Nomguhs, %d capsules, %d kloon-eilanden, %d Guhbubbels", nomguhs, capsules, kloons, bubbels));
        LOGGER.info("Verhaal regio's: {}", report);
        helper.assertTrue(fouten == 0, "in its biome, never near a Guhbubbel: " + report);
        helper.assertTrue(dubbel * 10 <= nomguhs + capsules, "(almost) one per region: " + report);
        helper.assertTrue(nomguhs >= 3 && capsules >= 3 && kloons >= 3 && bubbels >= 3, "there are some: " + report);
        // 1.0.0: the story structures only in the bigger regions, about half as many as in 3.0 (then, same seeds and area:
        // 127 Nomguhs, 71 capsules; 1.0.0: ~55, ~36 and ~27 kloon-eilanden, whose candidate deep-sea peaks also halved)
        helper.assertTrue(nomguhs <= 80 && capsules <= 42 && kloons <= 36, "the story structures stay rare: " + report);
        helper.succeed();
    }

    /** Guh villagers in both new biomes; the weather tag has the Guhpolder (and not the tundra). */
    @GuhTest(template = EMPTY)
    public static void verhaalWereldDorpelingenEnWeer(GameTestHelper helper) {
        JsonObject types;
        try (var in = VerhaalWereldGameTests.class.getResourceAsStream("/data/neoforge/data_maps/worldgen/biome/villager_types.json")) {
            types = JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("values");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        for (String b : List.of("guhs:sneeuwguhtoendra", "guhs:guhwaii")) {
            helper.assertTrue(types.has(b) && types.getAsJsonObject(b).get("villager_type").getAsString().equals("guhs:guh"), "guh villagers in " + b);
        }
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        helper.assertTrue(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Guhs.id("guhpolder"))).is(nl.juiced.guhs.feature.guhpolder.GuhpolderWeer.GEEN_WEER)
                && !biomes.getOrThrow(VerhaalFeature.SNEEUWGUHTOENDRA).is(nl.juiced.guhs.feature.guhpolder.GuhpolderWeer.GEEN_WEER)
                && biomes.getOrThrow(VerhaalFeature.SNEEUWGUHTOENDRA).value().hasPrecipitation(), "the weather: polder calm, tundra snowy");
        helper.succeed();
    }
}
