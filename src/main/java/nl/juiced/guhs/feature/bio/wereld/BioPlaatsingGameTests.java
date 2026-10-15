package nl.juiced.guhs.feature.bio.wereld;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * biomes3 fix-plaatsing: where the three biomes lie and how big they are (the game test server has no Guhmensie: the
 * model is built from the dimension's noise settings, as in {@link BioWereldGameTests}).
 */
public class BioPlaatsingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_plaatsing", EMPTY = "empty";
    static final long[] SEEDS = {20261007L, 1L, 77L};

    static BioModel model(GameTestHelper helper, long seed) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        return BioModel.van(RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed));
    }

    /**
     * The sizes of every lake and every Wolkenweide in 32 x 32 km of three seeds, as they are and (the same code on the
     * two noises the regions used to be) as they were. With the environment variable GUHS_BIO_PLAATSING_KAART (a
     * directory) it also writes the maps (tools/features/bio_wereld_plaatsing_kaart.py draws them).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 12000)
    public static void bioPlaatsingMaten(GameTestHelper helper) {
        String map = System.getenv("GUHS_BIO_PLAATSING_KAART");
        int kleinst = Integer.MAX_VALUE, grootst = 0, weideKleinst = Integer.MAX_VALUE, zonderStapel = 0, weiden = 0, meren = 0, tussen = 0, kruimels = 0, kruimelsKlein = 0;
        double dal = 0, meer = 0, weide = 0;
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            if (System.getenv("GUHS_BIO_PLAATSING_RUIMTE") != null) {
                // (how much room the older regions leave: the share of points with the mask above 0 / 0.08 / 0.145, and around
                // the last kind the radius of the largest circle on which the mask stays above 0.145 and above 0.05)
                int n = 0, vrij = 0, half = 0, vol = 0;
                int[] straal = new int[18], straalDal = new int[30];
                for (int x = -16000; x < 16000; x += 80) {
                    for (int z = -16000; z < 16000; z += 80) {
                        double k = m.masker(x, z);
                        n++;
                        vrij += k > 0 ? 1 : 0;
                        half += k > 0.08 ? 1 : 0;
                        if (k < 0.145) {
                            continue;
                        }
                        vol++;
                        int r = 20, rd = 20;
                        for (int welke = 0; welke < 2; welke++) {
                            double grens = welke == 0 ? 0.145 : 0.05;
                            int rr = 20;
                            zoek:
                            for (; rr < (welke == 0 ? 340 : 580); rr += 20) {
                                for (int i = 0; i < 16; i++) {
                                    if (m.masker(x + Math.cos(i * Math.PI / 8) * rr, z + Math.sin(i * Math.PI / 8) * rr) < grens) {
                                        break zoek;
                                    }
                                }
                            }
                            if (welke == 0) {
                                r = rr;
                            } else {
                                rd = rr;
                            }
                        }
                        straal[Math.min(17, r / 20)]++;
                        straalDal[Math.min(29, rd / 20)]++;
                    }
                }
                LOGGER.info("biomes3 plaatsing ROOM, seed {}: of {} points the mask is above 0 at {}, above 0.08 at {}, above 0.145 at {}; free lake radius (x20) {}; free dal radius (x20) {}",
                        seed, n, vrij, half, vol, java.util.Arrays.toString(straal), java.util.Arrays.toString(straalDal));
            }
            for (boolean oud : new boolean[]{true, false}) {
                java.nio.file.Path kaart = null;
                if (map != null && !map.isEmpty()) {
                    try {
                        java.nio.file.Files.createDirectories(java.nio.file.Path.of(map));
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException(e);
                    }
                    kaart = java.nio.file.Path.of(map).resolve("plaatsing_" + (oud ? "voor" : "na") + "_" + seed + ".bin");
                }
                PlaatsingMeting.Uitkomst u = PlaatsingMeting.meet(m, 0, 0, 16000, 8, oud, kaart);
                LOGGER.info("biomes3 plaatsing {}, seed {}: {}", oud ? "BEFORE (two noise tops)" : "AFTER (regions with a size)", seed, PlaatsingMeting.verslag(u, 16000, 8));
                if (oud) {
                    continue;
                }
                dal += u.deelDal / SEEDS.length;
                meer += u.deelMeer / SEEDS.length;
                weide += u.deelWeide / SEEDS.length;
                for (PlaatsingMeting.Stuk s : u.meren) {
                    if (!s.afgesneden()) {
                        meren++;
                        kleinst = Math.min(kleinst, s.smal());
                        grootst = Math.max(grootst, s.breed());
                        tussen += s.breed() >= 120 && s.breed() <= 350 ? 1 : 0;
                    }
                }
                for (PlaatsingMeting.Stuk s : u.dalen) {
                    kruimels += !s.afgesneden() && s.oppervlak() < 3000 ? 1 : 0;
                    kruimelsKlein += !s.afgesneden() && s.oppervlak() < 600 ? 1 : 0;
                }
                for (PlaatsingMeting.Stuk s : u.weiden) {
                    if (!s.afgesneden() && s.oppervlak() < 3000) {
                        kruimels++;
                        kruimelsKlein += s.oppervlak() < 600 ? 1 : 0;
                    } else if (!s.afgesneden()) {
                        weiden++;
                        weideKleinst = Math.min(weideKleinst, s.smal());
                        zonderStapel += s.extra() < 3 ? 1 : 0;
                    }
                }
            }
        }
        // (a crumb: a few columns of valley rim or meadow rim that an older region cuts loose from its region; not a lake, never a stack)
        LOGGER.info("biomes3 plaatsing: {} lakes and {} Wolkenweides in all three seeds; crumbs (loose pieces under 3000 blocks squared): {}, of which under 600: {}", meren, weiden, kruimels, kruimelsKlein);
        helper.assertTrue(kruimels <= 0.06 * (meren + weiden), "few crumbs: " + kruimels);
        helper.assertTrue(meren >= 300 && kleinst >= 80 && grootst <= 470, "every lake is a lake: " + meren + " lakes, the narrowest " + kleinst + ", the longest " + grootst);
        helper.assertTrue(tussen >= 0.75 * meren, "most lakes are 120-350 across: " + tussen + " of " + meren);
        helper.assertTrue(weiden >= 60 && weideKleinst >= 180 && zonderStapel == 0, "every Wolkenweide holds a cluster of stacks: " + weiden + " meadows, the narrowest "
                + weideKleinst + ", with room for fewer than 3 stacks: " + zonderStapel);
        helper.assertTrue(dal >= 0.025 && dal <= 0.06 && meer >= 0.008 && meer <= 0.03 && weide >= 0.007 && weide <= 0.016,
                String.format(java.util.Locale.ROOT, "the shares: Klaterdal %.2f%%, Bloesemmeertje %.2f%%, Wolkenweide %.2f%%", 100 * dal, 100 * meer, 100 * weide));
        helper.succeed();
    }

    /** How many of each building a region gets (the model's count, this server's structure sets): one per region is one per region. */
    @GuhTest(template = EMPTY, batch = "bio_telling_plaatsing", timeoutTicks = 12000)
    public static void bioPlaatsingTelling(GameTestHelper helper) {
        for (long seed : new long[]{SEEDS[0], SEEDS[1]}) {
            LOGGER.info("biomes3 plaatsing, seed {}: {}", seed, PlaatsingMeting.telling(model(helper, seed), 0, 0, 16000, 30, 20));
        }
        helper.succeed();
    }

    /**
     * What the buildings in the air cost the natural sky: the stacks, islands, rocks and cloud banks of the same 24
     * Wolkenweides with and without the air the buildings keep free. (Before biomes3 fix-plaatsing the six structure
     * sets cost a Wolkenweide about half of all of it.)
     */
    @GuhTest(template = EMPTY, batch = "bio_telling_lucht", timeoutTicks = 12000)
    public static void bioPlaatsingLucht(GameTestHelper helper) {
        int[] met = new int[8], zonder = new int[8];
        try {
            for (long seed : new long[]{SEEDS[0], SEEDS[1]}) {
                Luchtruim.zonder(true);
                int[] a = PlaatsingMeting.lucht(model(helper, seed), 0, 0, 12000, 12);
                Luchtruim.zonder(false);
                int[] b = PlaatsingMeting.lucht(model(helper, seed), 0, 0, 12000, 12);
                for (int i = 0; i < 8; i++) {
                    zonder[i] += a[i];
                    met[i] = i == 6 ? Math.max(met[i], b[i]) : met[i] + b[i];
                }
            }
        } finally {
            Luchtruim.zonder(false);
        }
        LOGGER.info("biomes3 plaatsing, the sky of {} Wolkenweides with {} buildings in the air (without any building -> with them): stacks {} -> {}, islands in stacks {} -> {}, "
                        + "loose islands {} -> {}, rocks {} -> {}, cloud banks {} -> {}; highest island {} above its meadow.{}", met[0], met[7], zonder[1], met[1], zonder[2], met[2],
                zonder[3], met[3], zonder[4], met[4], zonder[5], met[5], met[6], Luchtruim.verslag());
        helper.assertTrue(met[0] >= 12 && met[7] >= 3 * met[0], "Wolkenweides with their buildings: " + met[0] + " / " + met[7]);
        helper.assertTrue(met[1] >= 0.8 * zonder[1] && met[2] >= 0.8 * zonder[2] && met[5] >= 0.8 * zonder[5], "the buildings leave at least four fifths of the natural sky: stacks "
                + zonder[1] + " -> " + met[1] + ", islands " + zonder[2] + " -> " + met[2] + ", cloud banks " + zonder[5] + " -> " + met[5]);
        helper.succeed();
    }
}
