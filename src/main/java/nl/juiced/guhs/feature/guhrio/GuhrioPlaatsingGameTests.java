package nl.juiced.guhs.feature.guhrio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.BouwRuimte;
import nl.juiced.guhs.world.GegarandeerdData;
import nl.juiced.guhs.world.GegarandeerdPlacement;
import nl.juiced.guhs.world.NieuwTerrein;

/**
 * bbq2 (the user's decision B2): the Kasteel van de Grote Nether-Mika is "one big structure": every world has its ONE
 * guaranteed copy near 0,0, and besides it only a rare extra copy far away (the random-spread set, numbers in
 * tools/features/guhrio_kasteel.py). The test server has no Guhbarbecuether, so the real generator of that dimension is
 * built here (as world/PlaatsingGameTests does for the Guhmensie and ringh2 for Guhvendel) and asked on six seeds.
 */
public class GuhrioPlaatsingGameTests {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
    /** The seed of every dev server check of this update (CONTRACT 13.12 - 13.17) first, then five others. */
    private static final long[] SEEDS = {20261099L, 628114333L, 628122252L, 20261001L, 7L, 123456789L};
    /**
     * Where the guaranteed castle of each seed stood BEFORE the random set was thinned out (spacing 44 / separation 18; this
     * test's own run on that tree; the first one is also where it stood on the dev servers of CONTRACT 13.14 - 13.17).
     */
    private static final ChunkPos[] VAST = {new ChunkPos(-20, -39), new ChunkPos(-29, 2), new ChunkPos(-27, -10), new ChunkPos(-13, 60),
            new ChunkPos(-32, -31), new ChunkPos(29, -32)};
    /** Around 0,0 this far (blocks) every start of the random set is looked up. */
    private static final int STRAAL = 3200;
    /** No extra copy this near (blocks) to 0,0 or to the guaranteed copy: "far away". */
    private static final int VER = 1000;

    /**
     * The guaranteed copy is where it always was (the random set does not push it around), it lies in its ring and really
     * starts there; the random set is a wide triangular spread and gives, on six seeds, no copy near 0,0 or near the
     * guaranteed one and only a few within {@link #STRAAL} blocks (the old set, spacing 44 / separation 18: four or five
     * within 1200 blocks on every seed).
     */
    @GuhTest(template = "empty", batch = "guhrio_wereld", timeoutTicks = 24000)
    public static void guhrioEenKasteelEnEenZeldzameVerWeg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource biomes;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/barbecuether.json")).orElseThrow().openAsReader()) {
            biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator")
                    .getAsJsonObject("biome_source")).getOrThrow();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        var settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("barbecuether")));
        var generator = new NoiseBasedChunkGenerator(biomes, settings);
        var height = LevelHeightAccessor.create(settings.value().noiseSettings().minY(), settings.value().noiseSettings().height());
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        StructureSet vast = sets.getValue(Guhs.id(GuhrioKasteel.STRUCTUUR + "_gegarandeerd")), los = sets.getValue(Guhs.id(GuhrioKasteel.STRUCTUUR));
        helper.assertTrue(vast != null && vast.placement() instanceof GegarandeerdPlacement, "the guaranteed set of the castle");
        helper.assertTrue(los != null && los.placement() instanceof RandomSpreadStructurePlacement, "the random set of the castle");
        GegarandeerdPlacement ring = (GegarandeerdPlacement) vast.placement();
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) los.placement();
        var entry = vast.structures().get(0);
        Structure structure = entry.structure().value();
        List<String> fouten = new ArrayList<>();
        int binnen = 0;
        for (int nr = 0; nr < SEEDS.length; nr++) {
            long seed = SEEDS[nr];
            RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
            ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(random, seed, biomes, sets);
            state.ensureStructuresGenerated();
            BouwRuimte.remember(random, state);
            // (a world in which nothing exists yet: the guaranteed sets of this update are "alleen_nieuw")
            GegarandeerdPlacement.onthoudBestaand(level, state, seed, generator, height, new GegarandeerdData(), () -> NieuwTerrein.van(k -> false));
            // 1. the guaranteed copy
            Optional<ChunkPos> plek = ring.plek(state, seed);
            StringBuilder verslag = new StringBuilder("GuhrioPlaatsing seed " + seed + ": gegarandeerd " + plek.map(ChunkPos::toString).orElse("GEEN PLEK"));
            if (plek.isEmpty()) {
                fouten.add("seed " + seed + ": the guaranteed castle has no spot");
                LOGGER.info("{}", verslag);
                continue;
            }
            ChunkPos c = plek.get();
            int d = (int) Math.round(Math.hypot(c.getMiddleBlockX(), c.getMiddleBlockZ()));
            verslag.append(" (").append(d).append(" blokken)");
            if (d < ring.minAfstand() - 16 || d > ring.maxAfstand() + 16) {
                fouten.add("seed " + seed + ": the guaranteed castle lies " + d + " blocks from 0,0, outside its ring");
            }
            StructureStart start = structure.generate(entry.structure(), level.dimension(), access, generator, biomes, random, level.getStructureManager(),
                    seed, c, 0, height, structure.biomes()::contains);
            if (!start.isValid()) {
                fouten.add("seed " + seed + ": the guaranteed castle does not start at " + c);
            }
            if (!c.equals(VAST[nr])) {
                fouten.add("seed " + seed + ": the guaranteed castle moved from " + VAST[nr] + " to " + c);
            }
            if (nr == 0) {
                // (every guaranteed copy of the Guhbarbecuether on the reference seed: to compare one run with another by eye)
                StringBuilder alle = new StringBuilder("GuhrioPlaatsing seed " + seed + " alle gegarandeerde sets:");
                for (GegarandeerdPlacement.Kopie k : GegarandeerdPlacement.kopieen(state, seed)) {
                    alle.append(' ').append(k.set().replace("guhs:", "")).append('=').append(k.plek().map(ChunkPos::toString).orElse("GEEN PLEK")).append(';');
                }
                LOGGER.info("{}", alle);
            }
            // 2. the random set: every start the chunk generator would make within STRAAL blocks of 0,0
            int r = STRAAL / 16, s = spread.spacing(), n = 0;
            verslag.append("; los binnen ").append(STRAAL).append(':');
            for (int rx = Math.floorDiv(-r, s); rx <= Math.floorDiv(r, s); rx++) {
                for (int rz = Math.floorDiv(-r, s); rz <= Math.floorDiv(r, s); rz++) {
                    ChunkPos k = spread.getPotentialStructureChunk(seed, rx * s, rz * s);
                    int ver = (int) Math.round(Math.hypot(k.getMiddleBlockX(), k.getMiddleBlockZ()));
                    if (ver > STRAAL || !spread.isStructureChunk(state, k.x(), k.z())) {
                        continue;
                    }
                    StructureStart extra = structure.generate(los.structures().get(0).structure(), level.dimension(), access, generator, biomes, random,
                            level.getStructureManager(), seed, k, 0, height, structure.biomes()::contains);
                    if (!extra.isValid()) {
                        continue;
                    }
                    n++;
                    int vanVast = (int) Math.round(Math.hypot(k.getMiddleBlockX() - c.getMiddleBlockX(), k.getMiddleBlockZ() - c.getMiddleBlockZ()));
                    verslag.append(' ').append(k).append(" (").append(ver).append(" van 0,0, ").append(vanVast).append(" van het vaste)");
                    if (ver < VER) {
                        fouten.add("seed " + seed + ": an extra castle only " + ver + " blocks from 0,0, at " + k);
                    }
                    if (vanVast < VER) {
                        fouten.add("seed " + seed + ": an extra castle only " + vanVast + " blocks from the guaranteed one, at " + k);
                    }
                }
            }
            verslag.append(" = ").append(n);
            binnen += n;
            LOGGER.info("{}", verslag);
        }
        LOGGER.info("GuhrioPlaatsing: spacing {} separation {} {}, {} extra castles within {} blocks on {} seeds, problems: {}", spread.spacing(),
                spread.separation(), spread.spreadType(), binnen, STRAAL, SEEDS.length, fouten);
        helper.assertTrue(spread.spacing() >= 192 && spread.separation() >= 64 && spread.spreadType() == RandomSpreadType.TRIANGULAR,
                "the random set is rare and spread wide: spacing " + spread.spacing() + ", separation " + spread.separation() + ", " + spread.spreadType());
        helper.assertTrue(fouten.isEmpty(), "one castle near 0,0, extra ones far away: " + fouten);
        // (rare: on average at most one extra copy per world within 3200 blocks; the old numbers gave about thirty)
        helper.assertTrue(binnen <= SEEDS.length, "rare: " + binnen + " extra castles within " + STRAAL + " blocks on " + SEEDS.length + " seeds");
        helper.succeed();
    }
}
