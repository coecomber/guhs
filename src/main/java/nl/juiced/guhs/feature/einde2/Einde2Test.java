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
    /** The ten seeds the search for a terrain-free box in the deep band is tried for ({@link #zoekLeeg}). */
    public static final long[] LEEG_SEEDS = {21400001L, 21400002L, 21400003L, 21400004L, 21400005L, 21400006L, 21400007L, 21400008L, 21400009L, 21400010L};

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
        LeegteBouwStructure.onthoud(random, state);
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

    /**
     * The chunk of the guaranteed copy of this structure for this seed (null: it has none, or no spot was found); for a build
     * that stands next to another one: the one chunk next to that neighbour's guaranteed copy.
     */
    @Nullable
    public static ChunkPos gegarandeerd(Wereld w, String structuur) {
        return Plekken.startChunk(w.level.registryAccess(), w.state, w.seed, structuur);
    }

    /**
     * The checks every guhs:leegte_bouw must pass, for three seeds: the guaranteed copy exists, lies in its ring and really
     * starts there, its box lies in its band (the high band; the deep band for a build whose y lies below it) and its tiles
     * within 8 chunks of the start chunk, every tile template exists, every marker of the sidecar lies inside the box and the
     * biome at the anchor is one of the structure's. A build with a terrain-free box ({@code leeg}): no more sampled columns
     * hold terrain than the search round that found the spot allows ({@link #leegRonde} says which round). A build that
     * stands next to another one ({@code bij}): it starts exactly at its offset and nowhere else, and the two boxes touch
     * without overlapping.
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
        GegarandeerdPlacement ring = set != null && set.placement() instanceof GegarandeerdPlacement g ? g : null;
        if (ring == null && bouw.bij().isEmpty()) {
            return List.of("no guaranteed set");
        }
        if (ring != null && bouw.bij().isPresent()) {
            fouten.add("a build that stands next to another one has no guaranteed set of its own");
        }
        ChunkPos chunk = gegarandeerd(w, structuur);
        if (chunk == null) {
            return List.of("no spot for the guaranteed copy");
        }
        double d = Math.hypot(chunk.getMinBlockX(), chunk.getMinBlockZ());
        if (bouw.bij().isEmpty() && (d < ring.minAfstand() || d > ring.maxAfstand())) {
            fouten.add("not in its ring: " + (int) d + " blocks from 0,0");
        }
        StructureStart start = start(w, structuur, chunk);
        if (start == null) {
            return List.of("doesn't start at " + chunk);
        }
        BoundingBox doos = bouw.doos(chunk);
        boolean diep = bouw.y() < Einde2Ids.HOOG_MIN;
        if (diep ? doos.minY() < Einde2Ids.LAAG_MIN || doos.maxY() > Einde2Ids.LAAG_MAX : doos.minY() < Einde2Ids.HOOG_MIN || doos.maxY() > Einde2Ids.HOOG_MAX) {
            fouten.add("outside the " + (diep ? "deep" : "high") + " band: y " + doos.minY() + ".." + doos.maxY());
        }
        if (diep != bouw.ruim()) {
            fouten.add(diep ? "a build of the deep band must empty its box (ruim)" : "only a build of the deep band empties its box (ruim)");
        }
        if (bouw.leeg().isPresent()) {
            int mag = bouw.leegMag(leegRonde(w, structuur));
            int staat = bouw.terrein(w.generator, w.random, w.hoogte(), bouw.oorsprong(chunk), Integer.MAX_VALUE);
            if (staat > mag) {
                fouten.add("terrain in " + staat + " of " + bouw.leegKolommen() + " columns of its leeg box (at most " + mag + ")");
            }
        }
        if (bouw.bij().isPresent()) {
            LeegteBouwStructure.Bij bij = bouw.bij().get();
            String naam = bij.structuur().getPath();
            LeegteBouwStructure buur = Plekken.leegteBouw(w.level.registryAccess(), naam);
            ChunkPos bij2 = buur == null ? null : gegarandeerd(w, naam);
            if (bij2 == null) {
                return List.of("its neighbour " + naam + " has no spot");
            }
            if (!bouw.oorsprong(chunk).equals(buur.oorsprong(bij2).offset(bij.dx(), bouw.y() - buur.y(), bij.dz()))) {
                fouten.add("not at its offset from " + naam + ": " + bouw.oorsprong(chunk) + " vs " + buur.oorsprong(bij2));
            }
            BoundingBox ander = buur.doos(bij2);
            if (doos.intersects(ander)) {
                fouten.add("its box cuts the box of " + naam);
            } else if (bouw.size().getX() > 16 && buur.size().getX() > 16 && !doos.inflatedBy(1, 0, 1).intersects(ander)) {
                fouten.add("its box doesn't touch the box of " + naam);   // (the skeleton's stand-in platforms are too small to touch)
            }
            for (ChunkPos elders : List.of(new ChunkPos(chunk.x() + 1, chunk.z()), new ChunkPos(chunk.x(), chunk.z() - 1), bij2)) {
                if (!elders.equals(chunk) && start(w, structuur, elders) != null) {
                    fouten.add("it also starts at " + elders);
                }
            }
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

    /**
     * Which round of the guaranteed search found this structure's spot, as its flatness factor: 1 = a spot whose {@code leeg}
     * box is wholly free of terrain, 2 = terrain in at most 5 % of the sampled columns, 3 = at most 15 % (0: no spot).
     */
    public static int leegRonde(Wereld w, String structuur) {
        StructureSet set = w.level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(structuur + "_gegarandeerd"));
        ChunkPos chunk = gegarandeerd(w, structuur);
        return chunk != null && set != null && set.placement() instanceof GegarandeerdPlacement ring ? ring.vlakFactor(w.state, w.seed, chunk) : 0;
    }

    /** What {@link #zoekLeeg} found: the flatness factor of the round (0: no spot), the spots tried, the columns with terrain at the spot. */
    public record Leegte(int factor, int pogingen, int terrein, int kolommen) {
    }

    /** The rounds of the guaranteed search as a terrain-free box meets them: {spots tried, flatness factor} (GegarandeerdPlacement.RONDES). */
    private static final int[][] LEEG_RONDES = {{1024, 1}, {384, 2}, {384, 3}};

    /**
     * Tries the search for a terrain-free box in this world the way the guaranteed search does, without a structure: spots
     * between {@code lo} and {@code hi} blocks from 0,0 (spread evenly over that ring, in a fixed order per seed), first
     * asking for no terrain at all in a box of {@code lx} by {@code lz} blocks, then letting it stand in 5 % and in 15 % of
     * the sampled columns ({@link LeegteBouwStructure#terrein}). So a slice (or whoever changes a ring or a box in
     * tools/features/einde2_kit.py) sees which round finds a spot, for any seed.
     */
    public static Leegte zoekLeeg(Wereld w, int lo, int hi, int lx, int lz) {
        java.util.SplittableRandom rng = new java.util.SplittableRandom(w.seed * 31 + 21400060L);
        LevelHeightAccessor hoogte = w.hoogte();
        int kolommen = LeegteBouwStructure.kolommen(lx, lz), pogingen = 0;
        for (int[] ronde : LEEG_RONDES) {
            int mag = LeegteBouwStructure.leegMag(kolommen, ronde[1]);
            for (int k = 0; k < ronde[0]; k++) {
                double a = rng.nextDouble() * Math.PI * 2;
                double r = Math.sqrt((double) lo * lo + rng.nextDouble() * ((double) hi * hi - (double) lo * lo));
                BlockPos o = new BlockPos(Math.floorDiv((int) Math.round(Math.cos(a) * r), 16) * 16, 0, Math.floorDiv((int) Math.round(Math.sin(a) * r), 16) * 16);
                pogingen++;
                int staat = LeegteBouwStructure.terrein(w.generator, w.random, hoogte, o, lx, lz, mag);
                if (staat <= mag) {
                    return new Leegte(ronde[1], pogingen, staat, kolommen);
                }
            }
        }
        return new Leegte(0, pogingen, 0, kolommen);
    }
}
