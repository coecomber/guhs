package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.BouwRuimte;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * einde2, for the game tests: the headless test server has no Guheinde, so biome and placement logic is tested against
 * the dimension's own generator, built here from {@code data/guhs/dimension/guheinde.json} (the pattern of
 * world/PlaatsingGameTests): which biome lies where, whether a structure really starts in a chunk, where the guaranteed
 * copy of a big place lands for a seed. Nothing is generated in the test world itself.
 */
public final class Einde2Test {
    /** A Guheinde of one seed: the test level (templates, registries), the dimension's generator and its structure state. */
    public record Wereld(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState random, ChunkGeneratorStructureState state, long seed) {
        /** The build height of the Guheinde (its dimension type: higher than its noise, the high band lies above the terrain). */
        public LevelHeightAccessor hoogte() {
            var type = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getValue(Guhs.id("guheinde"));
            return LevelHeightAccessor.create(type.minY(), type.height());
        }
    }

    /** The seeds {@link #controleerLeegteBouw} tries. */
    public static final long[] SEEDS = {21400001L, 21400002L, 21400003L};

    private static final Map<String, Wereld> WERELDEN = new ConcurrentHashMap<>();

    private Einde2Test() {
    }

    /** The Guheinde generator of this seed, built from the dimension JSON (made once per seed). */
    public static Wereld wereld(GameTestHelper helper, long seed) {
        return wereld(helper, seed, null);
    }

    /**
     * The same with one biome everywhere instead of the dimension's own biome source: for a structure that is bound to a
     * biome, so its placement can be tested without hunting for that biome.
     */
    public static Wereld wereld(GameTestHelper helper, long seed, @Nullable ResourceKey<Biome> overal) {
        ServerLevel level = helper.getLevel();
        return WERELDEN.compute(seed + "/" + (overal == null ? "" : overal.identifier()), (k, oud) -> oud != null && oud.level == level ? oud : maak(level, seed, overal));
    }

    private static Wereld maak(ServerLevel level, long seed, @Nullable ResourceKey<Biome> overal) {
        var access = level.registryAccess();
        BiomeSource biomes;
        if (overal != null) {
            biomes = new FixedBiomeSource(access.lookupOrThrow(Registries.BIOME).getOrThrow(overal));
        } else {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
            try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/guheinde.json")).orElseThrow().openAsReader()) {
                biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator")
                        .getAsJsonObject("biome_source")).getOrThrow();
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guheinde")));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(random, seed, biomes, access.lookupOrThrow(Registries.STRUCTURE_SET));
        state.ensureStructuresGenerated();
        Wereld w = new Wereld(level, generator, random, state, seed);
        BouwRuimte.remember(random, state);
        GegarandeerdPlacement.onthoud(level, state, seed, generator, w.hoogte());
        return w;
    }

    /** The biome of a column (the Guheinde's biomes never depend on y). */
    public static Holder<Biome> biome(Wereld w, int x, int z) {
        return w.generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), w.random.sampler());
    }

    /** The start of this structure in this chunk, the way the chunk generator makes it (null: it doesn't start there). */
    @Nullable
    public static StructureStart start(Wereld w, String structuur, ChunkPos chunk) {
        Holder<Structure> holder = w.level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(Guhs.id(structuur)).orElse(null);
        if (holder == null) {
            return null;
        }
        Structure structure = holder.value();
        StructureStart start = structure.generate(holder, w.level.dimension(), w.level.registryAccess(), w.generator, w.generator.getBiomeSource(), w.random,
                w.level.getStructureManager(), w.seed, chunk, 0, w.hoogte(), structure.biomes()::contains);
        return start.isValid() ? start : null;
    }

    /** The chunk of the guaranteed copy of this structure for this seed (null: it has none, or no spot was found). */
    @Nullable
    public static ChunkPos gegarandeerd(Wereld w, String structuur) {
        return Plekken.startChunk(w.level.registryAccess(), w.state, w.seed, structuur);
    }

    /**
     * The checks every guhs:leegte_bouw must pass, for three seeds: the guaranteed copy exists, lies in its ring and really
     * starts there, its box lies in the high band and its tiles within 8 chunks of the start chunk, every tile template
     * exists, every marker of the sidecar lies inside the box and the biome at the anchor is one of the structure's.
     */
    public static void controleerLeegteBouw(GameTestHelper helper, String structuur) {
        for (long seed : SEEDS) {
            List<String> fouten = controleer(wereld(helper, seed), structuur);
            helper.assertTrue(fouten.isEmpty(), structuur + " (seed " + seed + "): " + fouten);
        }
    }

    /** The same checks in one world; returns what is wrong (empty: all fine). */
    public static List<String> controleer(Wereld w, String structuur) {
        List<String> fouten = new java.util.ArrayList<>();
        LeegteBouwStructure bouw = Plekken.leegteBouw(w.level.registryAccess(), structuur);
        if (bouw == null) {
            return List.of("no guhs:leegte_bouw structure");
        }
        StructureSet set = w.level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(structuur + "_gegarandeerd"));
        if (set == null || !(set.placement() instanceof GegarandeerdPlacement ring)) {
            return List.of("no guaranteed set");
        }
        ChunkPos chunk = gegarandeerd(w, structuur);
        if (chunk == null) {
            return List.of("no spot for the guaranteed copy");
        }
        double d = Math.hypot(chunk.getMinBlockX(), chunk.getMinBlockZ());
        if (d < ring.minAfstand() || d > ring.maxAfstand()) {
            fouten.add("not in its ring: " + (int) d + " blocks from 0,0");
        }
        StructureStart start = start(w, structuur, chunk);
        if (start == null) {
            return List.of("doesn't start at " + chunk);
        }
        BoundingBox doos = bouw.doos(chunk);
        if (doos.minY() < Einde2Ids.HOOG_MIN || doos.maxY() > Einde2Ids.HOOG_MAX) {
            fouten.add("outside the high band: y " + doos.minY() + ".." + doos.maxY());
        }
        int tiles = 0;
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof LeegteBouwStructure.Stuk)) {
                continue;
            }
            tiles++;
            BoundingBox b = piece.getBoundingBox();
            if ((b.minX() >> 4) < chunk.x() - 8 || (b.maxX() >> 4) > chunk.x() + 8 || (b.minZ() >> 4) < chunk.z() - 8 || (b.maxZ() >> 4) > chunk.z() + 8) {
                fouten.add("a tile further than 8 chunks from the start chunk: " + b);
            }
            if (b.minX() < doos.minX() || b.maxX() > doos.maxX() || b.minY() < doos.minY() || b.maxY() > doos.maxY() || b.minZ() < doos.minZ()
                    || b.maxZ() > doos.maxZ()) {
                fouten.add("a tile outside the build's box: " + b);
            }
        }
        if (tiles == 0) {
            fouten.add("no tiles");
        }
        // (the sidecar names the tiles it saved through its size: every tile that should hold blocks is a piece when its template exists)
        Plekken.Blad blad = Plekken.blad(structuur);
        if (!blad.grootte().equals(bouw.size())) {
            fouten.add("the sidecar's size " + blad.grootte() + " is not the structure's " + bouw.size());
        }
        BlockPos o = bouw.oorsprong(chunk);
        for (Plekken.Plek p : blad.plekken().values()) {
            List<BlockPos> punten = new java.util.ArrayList<>(p.punten());
            punten.add(p.pos());
            if (p.tot() != null) {
                punten.add(p.tot());
            }
            for (BlockPos q : punten) {
                BlockPos at = o.offset(q);
                // (the uitzicht lies outside the build on purpose: up to 40 blocks off its rim)
                boolean ok = p.id().equals("uitzicht") ? doos.inflatedBy(40).isInside(at) : doos.isInside(at);
                if (!ok) {
                    fouten.add("plek " + p.id() + " outside the box: " + q);
                    break;
                }
            }
        }
        for (Plekken.Laag laag : blad.lagen().values()) {
            BlockPos a = o.offset(laag.pos()), b = a.offset(laag.grootte().getX() - 1, laag.grootte().getY() - 1, laag.grootte().getZ() - 1);
            if (!doos.isInside(a) || !doos.isInside(b)) {
                fouten.add("laag " + laag.naam() + " outside the box");
            }
            if (w.level.getStructureManager().get(laag.template()).isEmpty()) {
                fouten.add("laag " + laag.naam() + ": no template " + laag.template());
            }
        }
        if (!bouw.biomes().contains(biome(w, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()))) {
            fouten.add("the biome at the anchor is not allowed");
        }
        return fouten;
    }
}
