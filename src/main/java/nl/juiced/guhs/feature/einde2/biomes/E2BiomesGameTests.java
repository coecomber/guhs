package nl.juiced.guhs.feature.einde2.biomes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import org.slf4j.Logger;

/**
 * Game tests of the einde2 slice biomes. The headless test server has no Guheinde, so the layout is tested by sampling the
 * dimension's own generator ({@link Einde2Test}): the terrain is exactly what it was before the biomes came, the main
 * island keeps its biome, all six biomes lie on the outer islands in the shares the design asks for, and the old
 * structures of the outer islands still find their biome. The rest runs in the room {@code einde2_biomes_test_kamer}
 * (tools/features/einde2_biomes.py: a thick floor of kaaskorst): the features, the knabbelkoor's growth, the fruit's
 * flash, the cloud's carrying rule, loot and recipes.
 */
public class E2BiomesGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMPTY = "empty";
    private static final String KAMER = "einde2_biomes_test_kamer";
    private static final String BATCH = "einde2_biomes";
    /** The five seeds the layout is sampled for. */
    private static final long[] SEEDS = {21400101L, 21400102L, 21400103L, 21400104L, 21400105L};
    /** The middle of the room's floor: the first free cell above the kaaskorst (helper coordinates). */
    private static final BlockPos MIDDEN = new BlockPos(16, 7, 16);
    private static final int KAMER_BREED = 33;

    // ------------------------------------------------------------------------------------------------------------------
    // the layout
    // ------------------------------------------------------------------------------------------------------------------
    /** The terrain did not move: the patched noise settings give the same height and the same columns as the old ones. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void einde2BiomesTerreinGelijk(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Holder<NoiseGeneratorSettings> oud = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("einde2_biomes_oud")));
        Holder<NoiseGeneratorSettings> nieuw = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guheinde")));
        helper.assertTrue(oud.value().noiseSettings().equals(nieuw.value().noiseSettings()), "the noise size is the same");
        helper.assertTrue(oud.value().defaultBlock() == nieuw.value().defaultBlock(), "the default block is the same");
        for (long seed : new long[]{SEEDS[0], SEEDS[1]}) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            NoiseBasedChunkGenerator oudGen = new NoiseBasedChunkGenerator(
                    new FixedBiomeSource(access.lookupOrThrow(Registries.BIOME).getOrThrow(Einde2Ids.GUHEINDE)), oud);
            RandomState oudRandom = RandomState.create(oud.value(), access.lookupOrThrow(Registries.NOISE), seed);
            Random r = new Random(21400150L);
            int land = 0;
            for (int i = 0; i < 200; i++) {
                // (the first forty on and around the main island, the rest all over the outer islands)
                int bereik = i < 40 ? 300 : 4200;
                int x = r.nextInt(2 * bereik) - bereik, z = r.nextInt(2 * bereik) - bereik;
                int a = oudGen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, w.hoogte(), oudRandom);
                int b = w.generator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, w.hoogte(), w.random());
                helper.assertTrue(a == b, "seed " + seed + ": the height at " + x + "," + z + " moved: " + a + " -> " + b);
                if (a > w.hoogte().getMinY()) {
                    land++;
                    if (land % 5 == 0) {
                        NoiseColumn ka = oudGen.getBaseColumn(x, z, w.hoogte(), oudRandom), kb = w.generator().getBaseColumn(x, z, w.hoogte(), w.random());
                        for (int y = w.hoogte().getMinY(); y < 128; y++) {
                            helper.assertTrue(ka.getBlock(y).isAir() == kb.getBlock(y).isAir(), "seed " + seed + ": the column at " + x + "," + z + " differs at y " + y);
                        }
                    }
                }
            }
            helper.assertTrue(land >= 40, "seed " + seed + ": enough of the 200 points have terrain to mean something: " + land);
        }
        helper.succeed();
    }

    /** What one sampling of the ring between 1000 and 4000 blocks found. */
    private record Streken(int[] aantal, int totaal, double typisch, int stukken) {
        double deel(int k) {
            return aantal[k] / (double) totaal;
        }

        double zeldzaam() {
            return deel(3) + deel(4) + deel(5) + deel(6);
        }
    }

    private interface Kiezer {
        int kies(int x, int z);
    }

    /**
     * Samples every 16th block of the ring between 1000 and 4000 blocks from 0,0. {@code typisch}: how far across (as a
     * circle of the same area) the rare patch is that a random rare block lies in (the area-weighted median over the
     * patches, a patch being one connected piece of one rare biome).
     */
    private static Streken meet(Kiezer kiezer) {
        final int stap = 16, n = 501, half = 250;
        byte[][] veld = new byte[n][n];
        int[] aantal = new int[7];
        int totaal = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = (i - half) * stap, z = (j - half) * stap;
                double d = Math.sqrt((double) x * x + (double) z * z);
                if (d < 1000 || d > 4000) {
                    veld[i][j] = -1;
                    continue;
                }
                veld[i][j] = (byte) kiezer.kies(x, z);
                aantal[veld[i][j]]++;
                totaal++;
            }
        }
        List<Integer> stukken = new ArrayList<>();
        boolean[][] gezien = new boolean[n][n];
        ArrayDeque<int[]> rij = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (veld[i][j] < 3 || gezien[i][j]) {
                    continue;
                }
                int soort = veld[i][j], cellen = 0;
                gezien[i][j] = true;
                rij.add(new int[]{i, j});
                while (!rij.isEmpty()) {
                    int[] c = rij.poll();
                    cellen++;
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int a = c[0] + d[0], b = c[1] + d[1];
                        if (a >= 0 && b >= 0 && a < n && b < n && !gezien[a][b] && veld[a][b] == soort) {
                            gezien[a][b] = true;
                            rij.add(new int[]{a, b});
                        }
                    }
                }
                stukken.add(cellen);
            }
        }
        stukken.sort(Comparator.naturalOrder());
        long alle = stukken.stream().mapToLong(Integer::longValue).sum(), som = 0;
        double typisch = 0;
        for (int cellen : stukken) {
            som += cellen;
            if (som * 2 >= alle) {
                typisch = 2 * Math.sqrt(cellen * (double) stap * stap / Math.PI);
                break;
            }
        }
        return new Streken(aantal, totaal, typisch, stukken.size());
    }

    private static int index(Holder<Biome> biome) {
        if (biome.is(Einde2Ids.GUHEINDE)) {
            return 0;
        }
        for (int k = 0; k < Einde2Ids.BUITEN.size(); k++) {
            if (biome.is(Einde2Ids.BUITEN.get(k))) {
                return k + 1;
            }
        }
        throw new IllegalStateException("a biome that is not the Guheinde's: " + biome);
    }

    /**
     * The layout for five seeds: the main island and the ring of nothing around it keep guhs:guheinde, beyond it lie all six
     * biomes, the four rare ones together take 8 to 18 % and each at least 1 %, a rare patch is small but big enough for its
     * side place (150 to 400 blocks across, taken over the five seeds), and a column is one biome from bottom to top.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 1200)
    public static void einde2BiomesStreken(GameTestHelper helper) {
        double typisch = 0;
        for (long seed : SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            helper.assertTrue(w.generator().getBiomeSource() instanceof GuheindeBiomeSource, "the dimension has the biome source guhs:guheinde_biomes");
            helper.assertTrue(w.generator().getBiomeSource().possibleBiomes().size() == 7, "seven possible biomes");
            for (int[] p : new int[][]{{0, 0}, {300, -200}, {-640, 640}, {900, 100}, {0, -950}}) {
                helper.assertTrue(Einde2Test.biome(w, p[0], p[1]).is(Einde2Ids.GUHEINDE), "seed " + seed + ": the middle stays guhs:guheinde at " + p[0] + "," + p[1]);
            }
            for (int[] p : new int[][]{{1000, 0}, {-700, 700}, {0, 2400}, {-3000, -3000}}) {
                helper.assertTrue(!Einde2Test.biome(w, p[0], p[1]).is(Einde2Ids.GUHEINDE), "seed " + seed + ": an outer biome at " + p[0] + "," + p[1]);
            }
            Streken s = meet((x, z) -> index(Einde2Test.biome(w, x, z)));
            LOGGER.info("einde2_biomes: seed {}: gewoon {} / {} %, zeldzaam {} / {} / {} / {} % (together {} %), {} rare patches, typically {} blocks across",
                    seed, pct(s.deel(1)), pct(s.deel(2)), pct(s.deel(3)), pct(s.deel(4)), pct(s.deel(5)), pct(s.deel(6)), pct(s.zeldzaam()), s.stukken(), (int) s.typisch());
            helper.assertTrue(s.aantal()[0] == 0, "seed " + seed + ": no guhs:guheinde beyond 1000 blocks");
            for (int k = 1; k <= 2; k++) {
                helper.assertTrue(s.deel(k) >= 0.25, "seed " + seed + ": common biome " + Einde2Ids.BUITEN.get(k - 1).identifier() + " takes " + pct(s.deel(k)) + " %");
            }
            for (int k = 3; k <= 6; k++) {
                helper.assertTrue(s.deel(k) >= 0.01, "seed " + seed + ": rare biome " + Einde2Ids.BUITEN.get(k - 1).identifier() + " takes " + pct(s.deel(k)) + " %");
            }
            helper.assertTrue(s.zeldzaam() >= 0.08 && s.zeldzaam() <= 0.18, "seed " + seed + ": the rare biomes take " + pct(s.zeldzaam()) + " % together");
            helper.assertTrue(s.typisch() >= 100 && s.typisch() <= 500, "seed " + seed + ": a rare patch is typically " + (int) s.typisch() + " blocks across");
            typisch += s.typisch() / SEEDS.length;
            // a column is one biome: whatever floats above an island has that island's biome
            Random r = new Random(seed);
            for (int i = 0; i < 60; i++) {
                int x = r.nextInt(8000) - 4000, z = r.nextInt(8000) - 4000;
                Holder<Biome> laag = w.generator().getBiomeSource().getNoiseBiome(x >> 2, 0, z >> 2, w.random().sampler());
                Holder<Biome> hoog = w.generator().getBiomeSource().getNoiseBiome(x >> 2, 240 >> 2, z >> 2, w.random().sampler());
                helper.assertTrue(laag.equals(hoog), "seed " + seed + ": the column at " + x + "," + z + " is one biome");
            }
        }
        helper.assertTrue(typisch >= 150 && typisch <= 400, "over the five seeds a rare patch is typically " + (int) typisch + " blocks across (150-400)");
        // the rule on its own
        helper.assertTrue(GuheindeBiomeSource.kies(100L * 100, 960, 0.9, 0.9, 0.55) == 0, "inside the middle: midden");
        helper.assertTrue(GuheindeBiomeSource.kies(2000L * 2000, 960, 0.2, 0.1, 0.55) == 1 && GuheindeBiomeSource.kies(2000L * 2000, 960, -0.2, -0.5, 0.55) == 2, "the common two by the sign of streek");
        helper.assertTrue(GuheindeBiomeSource.kies(2000L * 2000, 960, 0.2, 0.6, 0.55) == 3 && GuheindeBiomeSource.kies(2000L * 2000, 960, -0.2, 0.6, 0.55) == 4
                && GuheindeBiomeSource.kies(2000L * 2000, 960, 0.2, -0.6, 0.55) == 5 && GuheindeBiomeSource.kies(2000L * 2000, 960, -0.2, -0.6, 0.55) == 6,
                "the rare four by the two signs: kruimelwolken and kristalkaasklippen lie in gatenkaasheuvels country");
        helper.succeed();
    }

    private static String pct(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d * 100);
    }

    /**
     * The structures that stood on the outer islands before einde2 still find their biome there, with the sets they
     * always had, and every big place of einde2 that is bound to a biome finds its one guaranteed spot.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BiomesStructuren(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (String naam : new String[]{"mika_vesting", "guheinde_terugpoort"}) {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, Guhs.id("has_structure/" + naam));
            for (ResourceKey<Biome> biome : Einde2Ids.BUITEN) {
                helper.assertTrue(access.lookupOrThrow(Registries.BIOME).getOrThrow(biome).is(tag), naam + " still generates in " + biome.identifier());
            }
            helper.assertTrue(access.lookupOrThrow(Registries.BIOME).getOrThrow(Einde2Ids.GUHEINDE).is(tag), naam + " keeps guhs:guheinde (old chunks)");
        }
        StructureSet vesting = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("mika_vesting"));
        StructureSet poort = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("guheinde_terugpoort"));
        helper.assertTrue(vesting != null && vesting.placement() instanceof RandomSpreadStructurePlacement v && v.spacing() == 20 && v.separation() == 8,
                "the vesting keeps its grid (20 / 8)");
        helper.assertTrue(poort != null && poort.placement() instanceof RandomSpreadStructurePlacement p && p.spacing() == 24 && p.separation() == 10,
                "the terugpoort keeps its grid (24 / 10)");
        // the guaranteed copies of the places that ask for one biome (the stand-ins of the skeleton until their slices come)
        for (String structuur : new String[]{"omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leegtehaven"}) {
            Einde2Test.controleerLeegteBouw(helper, structuur);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the room: features
    // ------------------------------------------------------------------------------------------------------------------
    private static boolean plaats(GameTestHelper helper, Feature<NoneFeatureConfiguration> feature, BlockPos relatief, long seed) {
        ServerLevel level = helper.getLevel();
        return feature.place(new FeaturePlaceContext<>(Optional.empty(), level, level.getChunkSource().getGenerator(), RandomSource.create(seed),
                helper.absolutePos(relatief), NoneFeatureConfiguration.INSTANCE));
    }

    /** How many blocks of this kind the room holds between two heights (helper y). */
    private static int tel(GameTestHelper helper, Block block, int y0, int y1) {
        int n = 0;
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Holes: cheese is eaten away, and the underside of the island stays closed under every hole. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesKaasgat(GameTestHelper helper) {
        int voor = tel(helper, GuheindeFeature.KAASKORST.get(), 1, 6);
        boolean gemaakt = false;
        for (long seed = 1; seed <= 6; seed++) {
            gemaakt |= plaats(helper, E2BiomesFeature.KAASGAT.get(), new BlockPos(6 + (int) seed * 3, 7, 8 + (int) (seed % 3) * 7), seed);
        }
        helper.assertTrue(gemaakt, "a hole on a thick island");
        int na = tel(helper, GuheindeFeature.KAASKORST.get(), 1, 6);
        helper.assertTrue(voor - na >= 12, "cheese is gone: " + (voor - na) + " blocks");
        helper.assertTrue(tel(helper, GuheindeFeature.KAASKORST.get(), 1, 1) == KAMER_BREED * KAMER_BREED, "the bottom layer of the island is whole");
        // a thin crust gets none
        helper.setBlock(new BlockPos(16, 20, 16), GuheindeFeature.KAASKORST.get());
        helper.setBlock(new BlockPos(16, 19, 16), GuheindeFeature.KAASKORST.get());
        helper.assertTrue(!E2BiomesWorldgen.Kaasgat.gat(helper.getLevel(), helper.absolutePos(new BlockPos(16, 20, 16)).getX(), helper.absolutePos(new BlockPos(16, 20, 16)).getY(),
                helper.absolutePos(new BlockPos(16, 20, 16)).getZ(), 2.5f), "no hole where the island is thin");
        helper.succeed();
    }

    /** Ponds do not leak: every cell of fluid has ground under it and ground or fluid on its four sides. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesPoelen(GameTestHelper helper) {
        helper.assertTrue(plaats(helper, E2BiomesFeature.SAUSMEERTJE.get(), new BlockPos(8, 7, 8), 3), "a sauce pond on flat ground");
        helper.assertTrue(plaats(helper, E2BiomesFeature.VIJVER.get(), new BlockPos(23, 7, 9), 4), "a pond on flat ground");
        helper.assertTrue(plaats(helper, E2BiomesFeature.OASE.get(), new BlockPos(16, 7, 24), 5), "an oasis on flat ground");
        int saus = tel(helper, ModBlocks.KAAS_SAUS.get(), 1, 7), water = tel(helper, Blocks.WATER, 1, 7);
        helper.assertTrue(saus >= 6 && water >= 20, "sauce and water: " + saus + ", " + water);
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = 1; y <= 7; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (helper.getBlockState(p).getFluidState().isEmpty()) {
                        continue;
                    }
                    helper.assertTrue(y <= 6 && helper.getBlockState(p).getFluidState().isSource(), "still fluid in the ground at " + p);
                    for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                        BlockState naast = helper.getBlockState(p.relative(d));
                        helper.assertTrue(!naast.getFluidState().isEmpty() || naast.isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(p.relative(d))),
                                "the pool is closed at " + p + " " + d);
                    }
                }
            }
        }
        helper.assertTrue(tel(helper, E2BiomesFeature.SCHIMMELPADDENSTOEL.get(), 7, 8) >= 1, "glowing mushrooms on the pond's bank");
        helper.assertTrue(tel(helper, E2BiomesFeature.KORSTGRAS.get(), 7, 8) >= 10, "the oasis is green");
        // no pool on a slope or at an edge
        helper.assertTrue(!plaats(helper, E2BiomesFeature.SAUSMEERTJE.get(), new BlockPos(1, 7, 1), 6), "no pond at the edge of an island");
        helper.succeed();
    }

    /** What stands on the ground: boulders, dunes, giant mushrooms, crystal spires. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesOpDeGrond(GameTestHelper helper) {
        helper.assertTrue(plaats(helper, E2BiomesFeature.KRUIMELROTS_KEI.get(), new BlockPos(5, 7, 5), 1), "a boulder");
        helper.assertTrue(tel(helper, E2BiomesFeature.KRUIMELROTS.get(), 5, 12) >= 4, "of kruimelrots");
        helper.assertTrue(plaats(helper, E2BiomesFeature.DUIN.get(), new BlockPos(24, 7, 8), 2), "a dune");
        int lagen = tel(helper, E2BiomesFeature.KRUIMELZANDLAAG.get(), 7, 11), zand = tel(helper, E2BiomesFeature.KRUIMELZAND.get(), 7, 11);
        helper.assertTrue(lagen >= 8, "with slopes of layers: " + lagen + " (and " + zand + " blocks)");
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = 7; y <= 11; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = helper.getBlockState(p);
                    if (s.is(E2BiomesFeature.KRUIMELZANDLAAG.get()) || s.is(E2BiomesFeature.KRUIMELZAND.get())) {
                        helper.assertTrue(s.canSurvive(helper.getLevel(), helper.absolutePos(p)) && !helper.getBlockState(p.below()).isAir(), "the dune lies on something at " + p);
                    }
                }
            }
        }
        boolean reus = false;
        for (long seed = 1; seed <= 4 && !reus; seed++) {
            reus = plaats(helper, E2BiomesFeature.SCHIMMELREUS.get(), new BlockPos(8, 7, 24), seed);
        }
        helper.assertTrue(reus && tel(helper, E2BiomesFeature.SCHIMMELSTEEL.get(), 7, 16) >= 4 && tel(helper, E2BiomesFeature.SCHIMMELHOED.get(), 9, 18) >= 12,
                "a giant mushroom: a stem under a cap");
        helper.assertTrue(plaats(helper, E2BiomesFeature.KRISTALSPITS.get(), new BlockPos(23, 7, 23), 7), "crystal spires");
        int klip = tel(helper, E2BiomesFeature.KLIPKAAS.get(), 7, 39), kristal = tel(helper, E2BiomesFeature.KRISTALKAAS_KRISTAL.get(), 7, 39);
        helper.assertTrue(klip >= 40 && kristal >= 2, "columns of klipkaas with clusters: " + klip + ", " + kristal);
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = 7; y <= 39; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (helper.getBlockState(p).is(E2BiomesFeature.KRISTALKAAS_KRISTAL.get())) {
                        helper.assertTrue(helper.getBlockState(p).canSurvive(helper.getLevel(), helper.absolutePos(p)), "every cluster sits on its column: " + p);
                    }
                }
            }
        }
        // nothing starts in the void
        for (var feature : List.of(E2BiomesFeature.KAASGAT, E2BiomesFeature.KRUIMELROTS_KEI, E2BiomesFeature.DUIN, E2BiomesFeature.SCHIMMELREUS,
                E2BiomesFeature.KRISTALSPITS, E2BiomesFeature.KNABBELKOOR, E2BiomesFeature.SAUSMEERTJE, E2BiomesFeature.OASE)) {
            helper.assertTrue(!plaats(helper, feature.get(), new BlockPos(16, 28, 16), 9), "nothing in thin air: " + feature.getId());
        }
        helper.succeed();
    }

    /** What floats: crumb islets and clouds, in open air only. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesInDeLucht(GameTestHelper helper) {
        helper.assertTrue(plaats(helper, E2BiomesFeature.KRUIMELEILANDJE.get(), new BlockPos(16, 24, 16), 1), "a crumb islet in the air");
        int rots = tel(helper, E2BiomesFeature.KRUIMELROTS.get(), 12, 26), korst = tel(helper, GuheindeFeature.KAASKORST.get(), 12, 26);
        helper.assertTrue(rots >= 6 && korst >= 6, "kruimelrots under a cap of kaaskorst: " + rots + ", " + korst);
        helper.assertTrue(tel(helper, E2BiomesFeature.KRUIMELROTS.get(), 7, 9) == 0, "it floats");
        helper.assertTrue(!plaats(helper, E2BiomesFeature.KRUIMELEILANDJE.get(), new BlockPos(16, 9, 16), 2), "no islet where there is ground in the way");
        helper.assertTrue(plaats(helper, E2BiomesFeature.WOLK.get(), new BlockPos(16, 32, 16), 3), "a cloud");
        helper.assertTrue(tel(helper, E2BiomesFeature.KRUIMELWOLK.get(), 27, 37) >= 40, "of kruimelwolk");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the knabbelkoor
    // ------------------------------------------------------------------------------------------------------------------
    private static int[] telKoor(GameTestHelper helper) {
        int plant = 0, jong = 0, klaar = 0;
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = 7; y <= 39; y++) {
                    BlockState s = helper.getBlockState(new BlockPos(x, y, z));
                    if (s.is(E2BiomesFeature.KNABBELKOORPLANT.get())) {
                        plant++;
                    } else if (s.is(E2BiomesFeature.KNABBELKOORBLOEM.get())) {
                        if (s.getValue(Knabbelkoor.Bloem.AGE) < Knabbelkoor.Bloem.DOOD) {
                            jong++;
                        } else {
                            klaar++;
                        }
                    }
                }
            }
        }
        return new int[]{plant, jong, klaar};
    }

    /** A planted flower grows into a plant step by step and then stops; a whole plant stands and falls with its foot. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void einde2BiomesKnabbelkoorGroeit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Knabbelkoor.Bloem bloem = (Knabbelkoor.Bloem) E2BiomesFeature.KNABBELKOORBLOEM.get();
        helper.assertTrue(GuheindeFeature.KAASKORST.get().defaultBlockState().is(Knabbelkoor.GROND) && E2BiomesFeature.KOORKORST.get().defaultBlockState().is(Knabbelkoor.GROND)
                && !Blocks.STONE.defaultBlockState().is(Knabbelkoor.GROND), "the knabbelkoor's ground: the islands, not plain stone");
        helper.setBlock(MIDDEN, bloem.defaultBlockState());
        helper.assertTrue(bloem.defaultBlockState().canSurvive(level, helper.absolutePos(MIDDEN)), "a flower stands on the ground of the islands");
        RandomSource random = RandomSource.create(21400151L);
        for (int ronde = 0; ronde < 400 && telKoor(helper)[1] > 0; ronde++) {
            for (int x = 0; x < KAMER_BREED; x++) {
                for (int z = 0; z < KAMER_BREED; z++) {
                    for (int y = 7; y <= 39; y++) {
                        BlockPos p = helper.absolutePos(new BlockPos(x, y, z));
                        BlockState s = level.getBlockState(p);
                        if (s.is(bloem) && s.getValue(Knabbelkoor.Bloem.AGE) < Knabbelkoor.Bloem.DOOD) {
                            bloem.groeiStap(s, level, p, random);
                        }
                    }
                }
            }
        }
        int[] n = telKoor(helper);
        helper.assertTrue(n[0] >= 2 && n[1] == 0 && n[2] >= 1, "it grew and then it was done: " + n[0] + " stems, " + n[1] + " young and " + n[2] + " grown flowers");
        BlockState voet = helper.getBlockState(MIDDEN);
        helper.assertTrue(voet.is(E2BiomesFeature.KNABBELKOORPLANT.get()) && voet.getValue(PipeBlock.DOWN) && voet.getValue(PipeBlock.UP), "the foot is a stem that joins the ground and goes up");
        // a whole plant at once, a little further (the worldgen feature), and bone meal on a young flower
        helper.assertTrue(plaats(helper, E2BiomesFeature.KNABBELKOOR.get(), new BlockPos(6, 7, 6), 4), "the feature grows a whole plant");
        BlockPos jong = new BlockPos(26, 7, 26);
        helper.setBlock(jong, bloem.defaultBlockState());
        helper.assertTrue(bloem.isValidBonemealTarget(level, helper.absolutePos(jong), helper.getBlockState(jong)), "bone meal works on a young flower");
        bloem.performBonemeal(level, RandomSource.create(5), helper.absolutePos(jong), helper.getBlockState(jong));
        helper.assertTrue(helper.getBlockState(jong).is(E2BiomesFeature.KNABBELKOORPLANT.get()), "and it shoots up into a plant");
        int[] alle = telKoor(helper);
        helper.assertTrue(alle[0] >= n[0] + 8 && alle[1] == 0 && alle[2] >= n[2] + 2, "three plants now: " + alle[0] + " stems, " + alle[2] + " flowers");
        for (int x = 0; x < KAMER_BREED; x++) {
            for (int z = 0; z < KAMER_BREED; z++) {
                for (int y = 7; y <= 39; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = helper.getBlockState(p);
                    if (s.is(E2BiomesFeature.KNABBELKOORPLANT.get()) || s.is(bloem)) {
                        helper.assertTrue(s.canSurvive(level, helper.absolutePos(p)), "every part holds on to something: " + p);
                    }
                }
            }
        }
        // each flower has its own note, always the same one
        float toon = Knabbelkoor.Bloem.toon(helper.absolutePos(MIDDEN));
        helper.assertTrue(toon >= 0.5f && toon <= 2.0f && toon == Knabbelkoor.Bloem.toon(helper.absolutePos(MIDDEN)), "a flower's note is a playable pitch: " + toon);
        // chop the feet: everything comes down
        for (BlockPos p : new BlockPos[]{MIDDEN, new BlockPos(6, 7, 6), jong}) {
            level.destroyBlock(helper.absolutePos(p), false);
        }
        helper.succeedWhen(() -> {
            int[] rest = telKoor(helper);
            helper.assertTrue(rest[0] == 0 && rest[2] == 0, "the plants fall apart without their feet: " + rest[0] + " stems left");
        });
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the fruit, the clouds, loot and recipes
    // ------------------------------------------------------------------------------------------------------------------
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    /** Eating a knabbelkoorvrucht: one fruit less, and you stand a few blocks away, on your feet. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesVruchtFlitst(GameTestHelper helper) {
        ServerPlayer player = speler(helper);
        BlockPos start = helper.absolutePos(MIDDEN);
        ItemStack vrucht = new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), 3);
        helper.assertTrue(vrucht.has(net.minecraft.core.component.DataComponents.FOOD) && vrucht.has(net.minecraft.core.component.DataComponents.CONSUMABLE), "it is food");
        for (int i = 0; i < 3; i++) {
            player.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            player.fallDistance = 5;
            vrucht = vrucht.getItem().finishUsingItem(vrucht, helper.getLevel(), player);
            double dx = player.getX() - (start.getX() + 0.5), dy = player.getY() - start.getY(), dz = player.getZ() - (start.getZ() + 0.5);
            helper.assertTrue(dx != 0 || dz != 0 || dy != 0, "the fruit moved its eater");
            helper.assertTrue(Math.abs(dx) <= KnabbelkoorvruchtItem.BEREIK / 2 + 1 && Math.abs(dz) <= KnabbelkoorvruchtItem.BEREIK / 2 + 1, "a short hop: " + dx + ", " + dz);
            helper.assertTrue(!helper.getLevel().getBlockState(player.blockPosition().below()).isAir() && player.fallDistance == 0, "landed on something, softly");
        }
        helper.assertTrue(vrucht.isEmpty(), "three fruits eaten");
        // a creature that eats one flashes too
        var varken = helper.spawn(EntityType.PIG, MIDDEN);
        BlockPos van = varken.blockPosition();
        boolean weg = false;
        for (int i = 0; i < 4 && !weg; i++) {
            weg = KnabbelkoorvruchtItem.flits(helper.getLevel(), varken);
        }
        helper.assertTrue(weg && !varken.blockPosition().equals(van), "the flash works for any eater");
        varken.discard();
        weg(helper, player);
        helper.succeed();
    }

    /** The cloud's rule: nobody stands on a kruimelwolk but who Wolken names; whoever falls in sinks slowly and lands softly. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void einde2BiomesWolkDraagt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState wolk = E2BiomesFeature.KRUIMELWOLK.get().defaultBlockState();
        helper.assertTrue(wolk.is(Einde2Ids.WOLK) && E2BiomesFeature.DICHTE_KRUIMELWOLK.get().defaultBlockState().is(Einde2Ids.WOLK), "both clouds are in the cloud tag");
        Wolken.looptErop(e -> e.entityTags().contains("einde2_biomes_test_drager"));
        // a deck of cloud four blocks above the floor, two thick
        for (int x = 8; x <= 24; x++) {
            for (int z = 8; z <= 24; z++) {
                helper.setBlock(new BlockPos(x, 11, z), wolk);
                helper.setBlock(new BlockPos(x, 12, z), wolk);
            }
        }
        var gewoon = helper.spawn(EntityType.PIG, new BlockPos(12, 13, 16));
        var drager = helper.spawn(EntityType.PIG, new BlockPos(20, 13, 16));
        drager.addTag("einde2_biomes_test_drager");
        var valler = helper.spawn(EntityType.PIG, new BlockPos(16, 38, 12));
        helper.assertTrue(!Wolken.draagt(gewoon) && Wolken.draagt(drager), "Wolken.draagt names who registered");
        helper.assertTrue(!E2BiomesBlokken.Kruimelwolk.houdtTegen(gewoon, true, false), "a cloud holds nobody else up");
        helper.assertTrue(E2BiomesBlokken.Kruimelwolk.houdtTegen(drager, true, false) && !E2BiomesBlokken.Kruimelwolk.houdtTegen(drager, false, false)
                && !E2BiomesBlokken.Kruimelwolk.houdtTegen(drager, true, true), "whoever it carries stands on top, and drops through by going down");
        BlockPos top = helper.absolutePos(new BlockPos(20, 12, 16));
        helper.assertTrue(wolk.getCollisionShape(level, top, CollisionContext.of(drager)) == Shapes.block() && wolk.getCollisionShape(level, top, CollisionContext.of(gewoon)).isEmpty()
                && wolk.getCollisionShape(level, top, CollisionContext.empty()).isEmpty(), "the block's shape follows the rule");
        double deck = helper.absolutePos(new BlockPos(0, 13, 0)).getY(), vloer = helper.absolutePos(new BlockPos(0, 7, 0)).getY();
        helper.runAfterDelay(8, () -> helper.assertTrue(gewoon.getY() < deck - 0.2 && gewoon.getY() > deck - 2.5 && gewoon.getDeltaMovement().y >= E2BiomesBlokken.Kruimelwolk.ZAKKEN - 0.09,
                "it sinks slowly through the cloud: y " + (gewoon.getY() - deck) + ", speed " + gewoon.getDeltaMovement().y));
        helper.succeedWhen(() -> {
            helper.assertTrue(gewoon.onGround() && Math.abs(gewoon.getY() - vloer) < 0.1, "the pig sank through the cloud to the floor");
            helper.assertTrue(valler.onGround() && Math.abs(valler.getY() - vloer) < 0.1, "the falling pig fell through the cloud to the floor");
            helper.assertTrue(drager.getY() >= deck - 0.01 && drager.onGround(), "the carried pig stands on the cloud");
            // the falling one came from 25 blocks above the deck: without the cloud that is a dead pig; the cloud left it 4 blocks to fall
            helper.assertTrue(valler.isAlive() && valler.getHealth() >= valler.getMaxHealth() - 2.0f && gewoon.getHealth() >= gewoon.getMaxHealth() - 2.0f,
                    "the cloud broke the fall: " + valler.getHealth() + " of " + valler.getMaxHealth());
        });
    }

    /** Loot and recipes of the chain: the ore and the cluster give kristalkaas, the fruit pops, a popped fruit becomes blocks and light. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 100)
    public static void einde2BiomesKetting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack houweel = new ItemStack(Items.IRON_PICKAXE);
        for (var blok : List.of(E2BiomesFeature.KRISTALKAAS_ERTS, E2BiomesFeature.KRISTALKAAS_KRISTAL)) {
            var drops = Block.getDrops(blok.get().defaultBlockState(), level, pos, null, null, houweel);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(E2BiomesFeature.KRISTALKAAS.get()) && drops.get(0).getCount() <= 2, blok.getId() + " drops kristalkaas, modestly: " + drops);
        }
        boolean vrucht = false;
        for (int i = 0; i < 40 && !vrucht; i++) {
            vrucht = Block.getDrops(E2BiomesFeature.KNABBELKOORPLANT.get().defaultBlockState(), level, pos, null).stream().anyMatch(s -> s.is(E2BiomesFeature.KNABBELKOORVRUCHT.get()));
        }
        helper.assertTrue(vrucht, "a stem gives a fruit now and then");
        helper.assertTrue(E2BiomesFeature.KRISTALKAAS_ERTS.get().defaultBlockState().requiresCorrectToolForDrops(), "the ore wants a real pickaxe");
        var recepten = level.getServer().getRecipeManager();
        for (String naam : new String[]{"gepofte_knabbelkoorvrucht", "knabbelkoorblok", "knabbelkoorpilaar", "knabbelkoorblok_trap", "knabbelkoorblok_plaat", "lichtstaafje",
                "kristalkaas_van_brokjes", "kristalkaas_brokje", "kristalkaas_blok", "dichte_kruimelwolk", "kruimelzandlaag", "kruimelrots_stenen", "schimmelkaas_stenen",
                "klipkaas_stenen", "klipkaas_stenen_muur_van_klipkaas_steenzagen", "kruimelrots_stenen_trap_van_kruimelrots_stenen_steenzagen"}) {
            helper.assertTrue(recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(naam))).isPresent(), "recipe " + naam);
        }
        for (var blok : E2BiomesFeature.BLOCK_ITEMS) {
            helper.assertTrue(blok.get().asItem() != Items.AIR, blok.getId() + " has an item");
        }
        for (var blok : List.of(E2BiomesFeature.GEBARSTEN_KORST, E2BiomesFeature.KRUIMELZAND, E2BiomesFeature.SCHIMMELKAAS, E2BiomesFeature.KLIPKAAS,
                E2BiomesFeature.KOORKORST, E2BiomesFeature.KRUIMELROTS)) {
            helper.assertTrue(blok.get().defaultBlockState().is(Einde2Ids.EILANDGROND), blok.getId() + " is ground of the islands");
        }
        helper.succeed();
    }
}
