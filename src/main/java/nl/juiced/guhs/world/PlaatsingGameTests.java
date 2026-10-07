package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.wereld.RegioPiekPlacement;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.1.2: the placement rebalance of the Guhmension (tools/features/plaatsing.py, {@link GegarandeerdPlacement}, {@link BouwRuimte}):
 * the sets load with their new numbers, and in a real Guhmension generator (the test server has no Guhs dimensions: it is built
 * here from the dimension JSON) every minigame has its guaranteed copy 700-1500 blocks from 0,0, every landmark 1500-2500, each
 * really starts there, and no story structure starts within 600 blocks of 0,0.
 */
public class PlaatsingGameTests {
    private static final String EMPTY = "empty";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");

    static final List<String> MINIGAMES = List.of("guh_circuit", "guhdoolhof", "guh_golfbaan", "guh_racebaan", "knabbelspelen", "mika_mep_hal",
            "vadsig_eetfestijn", "guh_beauty_theater", "guh_disco", "guh_kermis", "verstopguh_huis", "guhvis_vijver", "sjoelhuisje",
            "knabbelkatapult", "guh_sterrenwacht", "ballonfestival", "knuffelbad");
    static final List<String> LANDMARKS = List.of("guh_kasteel", "guh_village", "guhbibliotheek", "kaasmijn", "hemelkapelletje", "zwevende_eilanden");
    /** guhpixel: the Guh-internetcafe and the Reisbureau, in a ring outside the old ones (tools/features/guhpixel_lib.py gegarandeerd). */
    static final List<String> GUHPIXEL = List.of("internetcafe", "reisbureau");

    private static StructureSet set(GameTestHelper helper, String name) {
        StructureSet set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(name));
        helper.assertTrue(set != null, "structure set " + name);
        return set;
    }

    private static int spacing(GameTestHelper helper, String name) {
        return ((RandomSpreadStructurePlacement) set(helper, name).placement()).spacing();
    }

    /** The sets and the story tag load with the 1.1.2 numbers. */
    @GuhTest(template = EMPTY, batch = "plaatsing")
    public static void plaatsingSets(GameTestHelper helper) {
        for (String name : MINIGAMES) {
            helper.assertTrue(set(helper, name + "_gegarandeerd").placement() instanceof GegarandeerdPlacement g && g.minAfstand() == 700
                    && g.maxAfstand() == 1500, name + ": a guaranteed copy 700-1500 blocks from spawn");
            helper.assertTrue(spacing(helper, name) <= 32, name + ": its normal set at most 32 chunks apart");
        }
        for (String name : LANDMARKS) {
            helper.assertTrue(set(helper, name + "_gegarandeerd").placement() instanceof GegarandeerdPlacement g && g.minAfstand() == 1500
                    && g.maxAfstand() == 2500, name + ": a guaranteed copy 1500-2500 blocks from spawn");
        }
        for (String name : GUHPIXEL) {
            helper.assertTrue(set(helper, name + "_gegarandeerd").placement() instanceof GegarandeerdPlacement g && g.minAfstand() == 4300
                    && g.maxAfstand() == 5600, name + ": a guaranteed copy 4300-5600 blocks from spawn (new chunks on a pregenerated server)");
        }
        Map<String, Integer> halved = Map.of("guh_caves", 10, "gatenkaas_mijnschacht", 14, "challenging_guh_caves", 24, "mini_picnic", 22, "quartz_statue", 24);
        halved.forEach((name, s) -> helper.assertTrue(spacing(helper, name) == s, name + ": spacing " + s + ", was " + spacing(helper, name)));
        helper.assertTrue(set(helper, "nomguh").placement() instanceof RegioPiekPlacement p && p.plek().cell() == 24, "nomguh: cells of 24 chunks");
        var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String name : List.of("nomguh", "sleehut", "evil_mika_home", "mika_kamp", "vadsig_heiligdom", "guhwaii_capsule", "guhwaii_ohana",
                "guhwaii_surfstrand", "kloon_eiland", "hemelkapelletje", "elfguhjestocht", "knuffeldal_stadje", "onderwater", "kaasknabbel_nest",
                "kampeerplekje", "guh_kasteel")) {
            helper.assertTrue(structures.get(Guhs.id(name)).orElseThrow().is(BouwRuimte.VERHAAL), name + ": in the tag guhs:verhaal");
        }
        helper.assertFalse(structures.get(Guhs.id("guh_circuit")).orElseThrow().is(BouwRuimte.VERHAAL), "a minigame is no story place");
        helper.succeed();
    }

    /**
     * In a real Guhmension (seed 20261001): every guaranteed copy is found, lies in its ring and really starts there (in
     * the same way the chunk generator starts it), and no story structure starts within 600 blocks of 0,0.
     */
    @GuhTest(template = EMPTY, batch = "plaatsing", timeoutTicks = 12000)
    public static void plaatsingGegarandeerd(GameTestHelper helper) {
        long seed = 20261001L;
        var level = helper.getLevel();
        var access = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource biomes;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator")
                    .getAsJsonObject("biome_source")).getOrThrow();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guhmension")));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(random, seed, biomes, access.lookupOrThrow(Registries.STRUCTURE_SET));
        state.ensureStructuresGenerated();
        LevelHeightAccessor height = LevelHeightAccessor.create(settings.value().noiseSettings().minY(), settings.value().noiseSettings().height());
        BouwRuimte.remember(random, state);
        GegarandeerdPlacement.onthoud(level, state, seed, generator, height);
        StringBuilder report = new StringBuilder();
        int found = 0;
        List<String> fouten = new ArrayList<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (!(set.value().placement() instanceof GegarandeerdPlacement g)) {
                continue;
            }
            String name = set.unwrapKey().orElseThrow().identifier().getPath();
            Optional<ChunkPos> plek = g.plek(state, seed);
            if (plek.isEmpty()) {
                fouten.add(name + ": no spot");
                continue;
            }
            ChunkPos c = plek.get();
            int d = (int) Math.round(Math.hypot(c.getMinBlockX(), c.getMinBlockZ()));
            report.append(String.format("%s %d; ", name, d));
            if (d < g.minAfstand() || d > g.maxAfstand()) {
                fouten.add(name + ": " + d + " blocks");
            }
            StructureSet.StructureSelectionEntry entry = set.value().structures().get(0);
            Structure structure = entry.structure().value();
            StructureStart start = structure.generate(entry.structure(), level.dimension(), access, generator, biomes, random, level.getStructureManager(),
                    seed, c, 0, height, structure.biomes()::contains);
            if (!start.isValid()) {
                fouten.add(name + ": doesn't start at " + c);
            }
            found++;
        }
        // the story structures: every start the chunk generator would make within 900 blocks
        int stories = 0;
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (!(set.value().placement() instanceof RandomSpreadStructurePlacement spread)) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                if (!entry.structure().is(BouwRuimte.VERHAAL)) {
                    continue;
                }
                int r = 900 / 16, s = spread.spacing();
                for (int rx = Math.floorDiv(-r, s); rx <= Math.floorDiv(r, s); rx++) {
                    for (int rz = Math.floorDiv(-r, s); rz <= Math.floorDiv(r, s); rz++) {
                        ChunkPos c = spread.getPotentialStructureChunk(seed, rx * s, rz * s);
                        if (Math.abs(c.x()) > r || Math.abs(c.z()) > r || !spread.isStructureChunk(state, c.x(), c.z())) {
                            continue;
                        }
                        Structure structure = entry.structure().value();
                        StructureStart start = structure.generate(entry.structure(), level.dimension(), access, generator, biomes, random,
                                level.getStructureManager(), seed, c, 0, height, structure.biomes()::contains);
                        if (!start.isValid()) {
                            continue;
                        }
                        stories++;
                        BoundingBox box = start.getBoundingBox();
                        long dx = Math.max(0, Math.max(box.minX(), -box.maxX())), dz = Math.max(0, Math.max(box.minZ(), -box.maxZ()));
                        if (dx * dx + dz * dz < 600L * 600L) {
                            fouten.add(entry.structure().unwrapKey().orElseThrow().identifier().getPath() + " at " + box.getCenter().toShortString());
                        }
                    }
                }
            }
        }
        LOGGER.info("Plaatsing: {} guaranteed ({}), {} story starts within 900 blocks, problems: {}", found, report, stories, fouten);
        // (guhpixel: + the Guh-internetcafe and the Reisbureau; bbq2 ring-h1: + the Knabbelgouw, the one guaranteed set of the Guhmensie
        // that is no minigame, landmark or guhpixel building)
        helper.assertTrue(found == MINIGAMES.size() + LANDMARKS.size() + GUHPIXEL.size() + 1, "every guaranteed set is in the Guhmension: " + found);
        helper.assertTrue(fouten.isEmpty(), "guaranteed copies in their ring, no story near spawn: " + fouten);
        helper.succeed();
    }
}
