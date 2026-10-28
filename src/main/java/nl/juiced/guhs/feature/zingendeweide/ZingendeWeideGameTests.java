package nl.juiced.guhs.feature.zingendeweide;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of De Zingende Weide: its share of the Guhmension (rare, in real patches, and how high the region noise goes:
 * how TERM in tools/features/zingendeweide.py was tuned), the Blubs of each part, bubbelsap (it flows, it makes you bubbly
 * and never drowns you), and a build of each shape drawn into the test box without trouble.
 */
public class ZingendeWeideGameTests {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
    private static final int STAP = 32, N = 400;

    @GuhTest(template = "empty", batch = "zingendeweide_aandeel", timeoutTicks = 2400)
    public static void weideAandeel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        MultiNoiseBiomeSource multi = (MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, source).getOrThrow();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        long totaal = 0, in = 0, bleek = 0, wolk = 0;
        List<Integer> plekken = new ArrayList<>();
        float[] waarden = new float[3 * N * N];
        int n = 0;
        for (long seed : new long[]{1L, 20281201L, -778899L}) {
            RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            Climate.Sampler sampler = random.sampler();
            var ruis = random.getOrCreateNoise(net.minecraft.resources.ResourceKey.create(Registries.NOISE, Guhs.id("zingende_weide")));
            boolean[][] is = new boolean[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    int x = -6400 + i * STAP, z = -6400 + j * STAP;
                    var biome = multi.getNoiseBiome(sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(100), QuartPos.fromBlock(z)));
                    is[i][j] = biome.is(ZingendeWeideFeature.ZINGENDE_WEIDE);
                    bleek += biome.is(nl.juiced.guhs.feature.bleekwoud.BleekwoudFeature.BLEEKWOUD) ? 1 : 0;
                    wolk += biome.is(nl.juiced.guhs.feature.bio.Bio.WOLKENWEIDE) ? 1 : 0;
                    totaal++;
                    in += is[i][j] ? 1 : 0;
                    waarden[n++] = (float) ruis.getValue(x, 0, z);
                }
            }
            java.util.ArrayDeque<int[]> rij = new java.util.ArrayDeque<>();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (!is[i][j]) {
                        continue;
                    }
                    int grootte = 0;
                    is[i][j] = false;
                    rij.add(new int[]{i, j});
                    while (!rij.isEmpty()) {
                        int[] c = rij.poll();
                        grootte++;
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], b = c[1] + d[1];
                            if (a >= 0 && b >= 0 && a < N && b < N && is[a][b]) {
                                is[a][b] = false;
                                rij.add(new int[]{a, b});
                            }
                        }
                    }
                    plekken.add(grootte);
                }
            }
        }
        java.util.Arrays.sort(waarden);
        StringBuilder q = new StringBuilder();
        for (double deel : new double[]{0.01, 0.02, 0.03, 0.05}) {
            q.append(String.format(java.util.Locale.ROOT, " top %.0f%% > %.3f;", 100 * deel, waarden[(int) (waarden.length * (1 - deel))]));
        }
        plekken.sort(null);
        double ha = STAP * STAP / 10000.0, groot = 0;
        for (int g : plekken) {
            groot += g * ha >= 4 ? g : 0;
        }
        double aandeel = in / (double) totaal;
        LOGGER.info("Zingende Weide share of the Guhmension surface ({} samples): {}% in {} patches (median {} ha, biggest {} ha, {}% in patches of 4 ha "
                        + "or more); the Bleekwoud has {}%, the Wolkenweide {}%; region noise:{}", totaal, String.format("%.2f", 100 * aandeel), plekken.size(),
                plekken.isEmpty() ? 0 : plekken.get(plekken.size() / 2) * ha, plekken.isEmpty() ? 0 : plekken.get(plekken.size() - 1) * ha,
                in == 0 ? 0 : Math.round(100 * groot / in), String.format("%.2f", 100.0 * bleek / totaal), String.format("%.2f", 100.0 * wolk / totaal), q);
        helper.assertTrue(aandeel >= 0.007 && aandeel <= 0.025, "De Zingende Weide is about as rare as the Wolkenweide (~1.3%): " + String.format("%.2f%%", 100 * aandeel));
        helper.assertTrue(in > 0 && groot / in >= 0.6, "most of it lies in real patches (4 ha or more): " + Math.round(100 * groot / in) + "%");
        helper.assertTrue(bleek > 0, "the Bleekwoud is still there");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = "zingendeweide")
    public static void weideBlubSoorten(GameTestHelper helper) {
        RandomSource r = RandomSource.create(42);
        int blauw = 0, zwam = 0, anders = 0;
        for (int i = 0; i < 2000; i++) {
            BlubTrekking.Soort s = BlubTrekking.trek(r, WeideDeel.WOLKJESBOS.blub).soort();
            if (s == BlubTrekking.Soort.BLAUW) {
                blauw++;
            } else if (s == BlubTrekking.Soort.ZWAM) {
                zwam++;
            } else {
                anders++;
            }
        }
        helper.assertTrue(anders == 0 && blauw > 850 && blauw < 1150 && zwam > 850, "in the Wolkjesbos: blue or zwam, about half each: " + blauw + "/" + zwam);
        helper.assertTrue(WeideDeel.ZANGWEIDE.blub == null && WeideDeel.SCHILDJESZAND.blub == BlubTrekking.Soort.SCHILD
                && WeideDeel.ZINGKLOOFJES.blub == BlubTrekking.Soort.RAVIJN && WeideDeel.WIEGWEIDE.blub == BlubTrekking.Soort.WEIDE, "each part its kind");
        // the parts cover every value of the noise without gaps
        double vorige = -10.0;
        for (WeideDeel d : WeideDeel.values()) {
            helper.assertTrue(d.van == vorige, "the parts follow each other: " + d);
            vorige = d.tot;
        }
        helper.assertTrue(WeideDeel.blubSoort(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))) == null, "outside the meadow: the usual draw");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = "zingendeweide", timeoutTicks = 200)
    public static void weideBubbelsap(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);
                helper.setBlock(new BlockPos(x, 2, z), x == 0 || z == 0 || x == 4 || z == 4 ? Blocks.GLASS.defaultBlockState()
                        : ZingendeWeideFeature.BUBBELSAP_BLOK.get().defaultBlockState());
            }
        }
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        pig.setAirSupply(0);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(ZingendeWeideFeature.BUBBELIG), "a pig in bubbelsap is bubbly");
            helper.assertTrue(pig.isAlive() && pig.getHealth() >= pig.getMaxHealth() - 0.01f, "and never hurt (no drowning)");
        });
    }

    @GuhTest(template = "empty", batch = "zingendeweide", timeoutTicks = 100)
    public static void weideVormen(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 2, 1));
        WeideTeken.Zet zet = WeideCommando.zet(level);
        RandomSource r = RandomSource.create(7);
        // a pillar with a cap and ears, a crystal cluster, a pool and a little tree: no trouble, and their blocks stand
        WeideTeken.pilaar(zet, r, o.getX() + 2, o.getY(), o.getZ() + 2, 8, 1.5, 0, 0, 1, true, false);
        helper.assertTrue(level.getBlockState(o.offset(2, 4, 2)).is(ZingendeWeideFeature.ZALMKLEI.get())
                || level.getBlockState(o.offset(2, 4, 2)).is(ZingendeWeideFeature.ZALMKLEI_STREEP_A.get())
                || level.getBlockState(o.offset(2, 4, 2)).is(ZingendeWeideFeature.ZALMKLEI_STREEP_B.get()), "the pillar is striped clay");
        WeideTeken.boompje(zet, r, o.getX() + 8, o.getY(), o.getZ() + 2);
        helper.assertTrue(level.getBlockState(o.offset(8, 1, 2)).is(ZingendeWeideFeature.STAM.get()), "the little tree has a trunk");
        helper.succeed();
    }
}
