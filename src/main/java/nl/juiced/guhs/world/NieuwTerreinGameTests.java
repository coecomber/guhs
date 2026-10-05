package nl.juiced.guhs.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * bbq2: guaranteed buildings in a world that exists already ({@link GegarandeerdPlacement} "alleen_nieuw" / "rond",
 * {@link NieuwTerrein}, {@link GegarandeerdData}). The test server has none of our dimensions, so the search runs in a
 * Guhmension generator built from the dimension JSON (like PlaatsingGameTests), with a copy of a real guaranteed set that
 * is "alleen_nieuw", and the test says which chunks exist.
 */
public class NieuwTerreinGameTests {
    private static final String EMPTY = "empty";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");

    /** The placement JSON: the two new fields load, and a set without them is what it always was. */
    @GuhTest(template = EMPTY, batch = "nieuwterrein")
    public static void nieuwTerreinCodec(GameTestHelper helper) {
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        JsonObject oud = JsonParser.parseString("{\"type\":\"guhs:gegarandeerd\",\"salt\":11,\"min_afstand\":700,\"max_afstand\":1500,\"sector\":3,\"sectoren\":17}")
                .getAsJsonObject();
        GegarandeerdPlacement a = (GegarandeerdPlacement) StructurePlacement.CODEC.parse(ops, oud).getOrThrow();
        helper.assertTrue(!a.alleenNieuw() && a.rond().isEmpty() && a.minAfstand() == 700 && a.maxAfstand() == 1500 && a.sector() == 3
                && a.sectoren() == 17 && a.zout() == 11, "an old set: no new fields");
        JsonObject nieuw = JsonParser.parseString("{\"type\":\"guhs:gegarandeerd\",\"salt\":21309308,\"min_afstand\":250,\"max_afstand\":500,"
                + "\"sector\":0,\"sectoren\":1,\"alleen_nieuw\":true,\"rond\":\"guhs:guhvendel_gegarandeerd\"}").getAsJsonObject();
        GegarandeerdPlacement b = (GegarandeerdPlacement) StructurePlacement.CODEC.parse(ops, nieuw).getOrThrow();
        helper.assertTrue(b.alleenNieuw() && b.rond().equals(Optional.of(Guhs.id("guhvendel_gegarandeerd"))) && b.sectoren() == 1, "alleen_nieuw and rond");
        JsonObject terug = StructurePlacement.CODEC.encodeStart(ops, b).getOrThrow().getAsJsonObject();
        helper.assertTrue(terug.get("alleen_nieuw").getAsBoolean() && terug.get("rond").getAsString().equals("guhs:guhvendel_gegarandeerd")
                && terug.get("min_afstand").getAsInt() == 250, "and they are written back: " + terug);
        JsonObject oudTerug = StructurePlacement.CODEC.encodeStart(ops, a).getOrThrow().getAsJsonObject();
        helper.assertTrue(!oudTerug.has("rond") && oudTerug.get("sector").getAsInt() == 3, "an old set is written as it was: " + oudTerug);
        helper.succeed();
    }

    /** Which chunks exist: read from the header of a region file, from the chunks in memory, per axis around a chunk. */
    @GuhTest(template = EMPTY, batch = "nieuwterrein")
    public static void nieuwTerreinRegiobestand(GameTestHelper helper) {
        try {
            Path map = Files.createTempDirectory("guhs-nieuwterrein");
            // r.0.0: the chunks (3, 5) and (31, 31); r.-1.0: chunk (-1, 0) = local (31, 0); r.1.1: a file that is too short
            ByteBuffer kop = ByteBuffer.allocate(8192);
            kop.putInt((3 + 5 * 32) * 4, (2 << 8) | 1);
            kop.putInt((31 + 31 * 32) * 4, (3 << 8) | 1);
            Files.write(map.resolve("r.0.0.mca"), kop.array());
            ByteBuffer links = ByteBuffer.allocate(8192);
            links.putInt((31 + 0 * 32) * 4, (2 << 8) | 1);
            Files.write(map.resolve("r.-1.0.mca"), links.array());
            Files.write(map.resolve("r.1.1.mca"), new byte[100]);
            NieuwTerrein t = NieuwTerrein.van(map, Set.of(ChunkPos.pack(40, 40)));
            helper.assertTrue(t.bestaat(3, 5) && t.bestaat(31, 31) && !t.bestaat(4, 5) && !t.bestaat(5, 3), "the chunks of r.0.0");
            helper.assertTrue(t.bestaat(-1, 0) && !t.bestaat(-1, 1) && !t.bestaat(-32, 0), "a region left of 0 (negative chunks)");
            helper.assertTrue(!t.bestaat(40, 40 + 1) && t.bestaat(40, 40), "a chunk in memory");
            helper.assertTrue(!t.bestaat(33, 33) && !t.bestaat(100, -100), "an empty file, no file: nothing there");
            // around chunk (10, 10) (middle block 168, 168): chunk (3, 5) is 5 chunks away in z
            helper.assertTrue(t.nieuw(new ChunkPos(10, 10), 60) && !t.nieuw(new ChunkPos(10, 10), 120), "new terrain within 60 blocks, not within 120");
            helper.assertTrue(!t.nieuw(new ChunkPos(3, 5), 0) && t.nieuw(new ChunkPos(4, 6), 7) && !t.nieuw(new ChunkPos(4, 6), 9), "the chunk itself and its neighbours");
            NieuwTerrein alles = NieuwTerrein.van(k -> true), niets = NieuwTerrein.van(k -> false);
            helper.assertTrue(!alles.nieuw(new ChunkPos(5, 5), 0) && niets.nieuw(new ChunkPos(5, 5), 500), "everything / nothing exists");
            for (String f : List.of("r.0.0.mca", "r.-1.0.mca", "r.1.1.mca")) {
                Files.deleteIfExists(map.resolve(f));
            }
            Files.deleteIfExists(map);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // the real level: what the test world has loaded here exists, far away nothing does
        NieuwTerrein echt = NieuwTerrein.van(helper.getLevel());
        ChunkPos hier = ChunkPos.containing(helper.absolutePos(net.minecraft.core.BlockPos.ZERO));
        helper.assertTrue(echt.bestaat(hier.x(), hier.z()) && !echt.bestaat(hier.x() + 20000, hier.z() - 20000), "the level's own chunks");
        helper.succeed();
    }

    /**
     * The search itself, in a real Guhmension generator (seed 20261001) with an "alleen_nieuw" copy of the guaranteed Guhdisco
     * set: in a new world it finds exactly the spot of the old set; when that spot exists already it takes another one in
     * new terrain; the saved spot stays the answer whatever happens to the terrain; and when nothing is new anywhere, nothing
     * is placed (and nothing saved).
     */
    @GuhTest(template = EMPTY, batch = "nieuwterrein", timeoutTicks = 12000)
    public static void nieuwTerreinZoeken(GameTestHelper helper) {
        long seed = 20261001L;
        var level = helper.getLevel();
        var access = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource biomes;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator")
                    .getAsJsonObject("biome_source")).getOrThrow();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guhmension")));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        LevelHeightAccessor height = LevelHeightAccessor.create(settings.value().noiseSettings().minY(), settings.value().noiseSettings().height());
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        Identifier oudId = Guhs.id("guh_disco_gegarandeerd");
        StructureSet oudSet = sets.getValue(oudId);
        GegarandeerdPlacement oud = (GegarandeerdPlacement) oudSet.placement();

        // the old set in a new world: its spot
        RandomState randomA = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        ChunkGeneratorStructureState stateA = ChunkGeneratorStructureState.createForNormal(randomA, seed, biomes, sets);
        stateA.ensureStructuresGenerated();
        BouwRuimte.remember(randomA, stateA);
        GegarandeerdPlacement.onthoud(level, stateA, seed, generator, height);
        ChunkPos s1 = oud.plek(stateA, seed).orElse(null);
        helper.assertTrue(s1 != null, "the old Guhdisco set has its spot");

        // the same set, "alleen_nieuw", in a state of its own (every other set as it is)
        GegarandeerdPlacement nieuw = new GegarandeerdPlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1f, oud.zout(), Optional.empty(),
                oud.minAfstand(), oud.maxAfstand(), new GegarandeerdPlacement.Sector(oud.sector(), oud.sectoren()),
                new GegarandeerdPlacement.Nieuw(true, Optional.empty()));
        List<Holder<StructureSet>> alle = new ArrayList<>();
        sets.listElements().filter(h -> !h.key().identifier().equals(oudId)).forEach(alle::add);
        alle.add(Holder.direct(new StructureSet(oudSet.structures(), nieuw)));
        RandomState randomB = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        ChunkGeneratorStructureState stateB = ChunkGeneratorStructureState.createForFlat(randomB, seed, biomes, alle.stream());
        stateB.ensureStructuresGenerated();
        BouwRuimte.remember(randomB, stateB);

        // 1. a world where nothing exists yet: exactly the old spot, and it is saved
        GegarandeerdData opslag = new GegarandeerdData();
        GegarandeerdPlacement.onthoudBestaand(level, stateB, seed, generator, height, opslag, () -> NieuwTerrein.van(k -> false));
        ChunkPos nieuwNiets = nieuw.plek(stateB, seed).orElse(null);
        helper.assertTrue(s1.equals(nieuwNiets), "a brand-new world: the same spot as before (" + s1 + " / " + nieuwNiets + ")");
        helper.assertTrue(opslag.alles().size() == 1 && opslag.alles().values().iterator().next().chunk().equals(s1), "and it is saved: " + opslag.alles());

        // 2. the old spot and 20 chunks around it exist already: another spot, in new terrain
        java.util.function.LongPredicate rondS1 = k -> Math.abs(ChunkPos.getX(k) - s1.x()) <= 20 && Math.abs(ChunkPos.getZ(k) - s1.z()) <= 20;
        GegarandeerdData opslag2 = new GegarandeerdData();
        GegarandeerdPlacement.onthoudBestaand(level, stateB, seed, generator, height, opslag2, () -> NieuwTerrein.van(rondS1));
        ChunkPos s2 = nieuw.plek(stateB, seed).orElse(null);
        helper.assertTrue(s2 != null && !s2.equals(s1), "the old spot exists: another one (" + s2 + ")");
        int d = (int) Math.round(Math.hypot(s2.getMinBlockX(), s2.getMinBlockZ()));
        helper.assertTrue(d >= oud.minAfstand() && d <= oud.maxAfstand() * 2, "in the ring (at most twice as wide): " + d);
        helper.assertTrue(NieuwTerrein.van(rondS1).nieuw(s2, GegarandeerdPlacement.NIEUW_RAND + 16), "in new terrain, a rim around it");
        helper.assertTrue(Math.abs(s2.x() - s1.x()) > 20 || Math.abs(s2.z() - s1.z()) > 20, "outside what exists");
        helper.assertTrue(nieuw.vlakFactor(stateB, seed, s2) >= 1 && nieuw.vlakFactor(stateB, seed, s1) == 0, "the start belongs to the new spot only");

        // 3. the saved spot is the answer for ever: also when its own chunks exist by now (the building stands there)
        GegarandeerdPlacement.onthoudBestaand(level, stateB, seed, generator, height, opslag2, () -> NieuwTerrein.van(k -> true));
        helper.assertTrue(s2.equals(nieuw.plek(stateB, seed).orElse(null)), "the saved spot stays");

        // 4. nothing is new anywhere: no spot, nothing saved (it is searched again another time)
        GegarandeerdData opslag4 = new GegarandeerdData();
        GegarandeerdPlacement.onthoudBestaand(level, stateB, seed, generator, height, opslag4, () -> NieuwTerrein.van(k -> true));
        helper.assertTrue(nieuw.plek(stateB, seed).isEmpty() && opslag4.alles().isEmpty(), "no new terrain: not placed, not saved");
        LOGGER.info("NieuwTerrein: old spot {}, in an existing world {} ({} blocks from 0,0)", s1, s2, d);
        helper.succeed();
    }
}
