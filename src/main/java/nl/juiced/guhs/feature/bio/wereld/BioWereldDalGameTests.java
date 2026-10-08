package nl.juiced.guhs.feature.bio.wereld;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * biomes3 wereld-dal: the game tests of the finished Klaterdal (the shape rules of {@link DalTerrein}); the shared
 * tests of the model are in {@link BioWereldGameTests}.
 */
public class BioWereldDalGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_wereld_dal", EMPTY = "empty";
    private static final long[] SEEDS = {20261007L, 1L};

    static BioModel model(GameTestHelper helper, long seed) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        return BioModel.van(RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed));
    }

    /** The middles of a few dals (the highest "how far in" near a lake column), from a coarse scan. */
    static List<int[]> dalen(BioModel m, int max) {
        List<int[]> uit = new ArrayList<>();
        for (int x = -12000; x <= 12000 && uit.size() < max; x += 96) {
            for (int z = -12000; z <= 12000 && uit.size() < max; z += 96) {
                if (m.soort(x, z) == Kaart.MEER && m.meng(x, z) >= 1f) {
                    int px = x, pz = z;
                    for (int stap = 0; stap < 200; stap++) {
                        double best = m.eDal(px, pz);
                        int bx = px, bz = pz;
                        for (int[] d : new int[][]{{8, 0}, {-8, 0}, {0, 8}, {0, -8}}) {
                            double v = m.eDal(px + d[0], pz + d[1]);
                            if (v > best) {
                                best = v;
                                bx = px + d[0];
                                bz = pz + d[1];
                            }
                        }
                        if (bx == px && bz == pz) {
                            break;
                        }
                        px = bx;
                        pz = bz;
                    }
                    boolean nieuw = true;
                    for (int[] p : uit) {
                        nieuw &= Math.abs(p[0] - px) + Math.abs(p[1] - pz) > 500;
                    }
                    if (nieuw) {
                        uit.add(new int[]{px, pz});
                    }
                }
            }
        }
        return uit;
    }

    /**
     * Not a test of the game: with the environment variable GUHS_BIO_DAL_DUMP (a directory) it writes the raw fields of
     * the model around a few dals (how far in, the river noise, the detail noise) so the shape can be drawn and tuned
     * offline (guhs_workbio/reports/screens/wereld-dal/_bron). Without the variable it does nothing.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void bioWereldDalVelden(GameTestHelper helper) {
        String map = System.getenv("GUHS_BIO_DAL_VELDEN");
        if (map == null || map.isEmpty()) {
            helper.succeed();
            return;
        }
        final int n = 768;
        try {
            Path dir = Path.of(map);
            Files.createDirectories(dir);
            for (long seed : SEEDS) {
                BioModel m = model(helper, seed);
                for (int[] dal : plekken(m, seed)) {
                    int x0 = dal[0] - n / 2, z0 = dal[1] - n / 2;
                    Path uit = dir.resolve("veld_" + seed + "_" + dal[0] + "_" + dal[1] + ".bin");
                    try (DataOutputStream o = new DataOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(uit), 1 << 20))) {
                        o.writeInt(x0);
                        o.writeInt(z0);
                        o.writeInt(n);
                        o.writeInt(10);
                        for (int f = 0; f < 10; f++) {
                            for (int j = 0; j < n; j++) {
                                for (int i = 0; i < n; i++) {
                                    double x = x0 + i, z = z0 + j;
                                    double v = switch (f) {
                                        case 0 -> m.eDal(x, z);
                                        case 1 -> m.ruis(BioModel.R_DAL, x, z) - BioModel.DAL_VANAF;
                                        case 2 -> m.masker(x, z);
                                        case 3 -> m.ruis(BioModel.R_RIVIER, x, z);
                                        case 4 -> m.ruis(BioModel.R_DETAIL, x, z);
                                        case 5 -> m.ruis(BioModel.R_DETAIL, x * 0.15 + 300, z * 0.15 - 700);
                                        case 6 -> m.ruis(BioModel.R_DETAIL, x * 0.5 + 1000, z * 0.5 + 1000);
                                        case 7 -> m.ruis(BioModel.R_DETAIL, x * 0.3 + 5000, z * 0.3 + 100);
                                        case 8 -> m.ruis(BioModel.R_DETAIL, x * 0.07 + 77, z * 0.07 - 33);
                                        default -> m.ruis(BioModel.R_RIVIER, x + 7000, z - 3000);
                                    };
                                    o.writeFloat((float) v);
                                }
                            }
                        }
                    }
                    LOGGER.info("biomes3 dal: fields of the dal at {} {} (seed {}) written to {}", dal[0], dal[1], seed, uit.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /**
     * Not a test of the game either: with GUHS_BIO_DAL_DUMP it writes what the MODEL says (height, water, terrace, flags)
     * around the same dals, for the maps in guhs_workbio/reports/screens/wereld-dal.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void bioWereldDalKaarten(GameTestHelper helper) {
        String map = System.getenv("GUHS_BIO_DAL_DUMP");
        if (map == null || map.isEmpty()) {
            helper.succeed();
            return;
        }
        final int chunks = 48, n = chunks * 16;
        try {
            Path dir = Path.of(map);
            Files.createDirectories(dir);
            for (long seed : SEEDS) {
                BioModel m = model(helper, seed);
                for (int[] dal : plekken(m, seed)) {
                    int c0x = (dal[0] >> 4) - chunks / 2, c0z = (dal[1] >> 4) - chunks / 2;
                    Path uit = dir.resolve("kaart_" + seed + "_" + dal[0] + "_" + dal[1] + ".bin");
                    long t0 = System.nanoTime();
                    try (DataOutputStream o = new DataOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(uit), 1 << 20))) {
                        o.writeInt(c0x << 4);
                        o.writeInt(c0z << 4);
                        o.writeInt(n);
                        o.writeInt(9);
                        int[][] veld = new int[9][n * n];
                        DalTerrein.Vorm v = new DalTerrein.Vorm();
                        for (int cz = 0; cz < chunks; cz++) {
                            for (int cx = 0; cx < chunks; cx++) {
                                Kaart k = m.kaart(c0x + cx, c0z + cz);
                                for (int q = 0; q < 256; q++) {
                                    int idx = (cx << 4 | q & 15) + (cz << 4 | q >> 4) * n;
                                    veld[0][idx] = k.leeg ? 0 : k.soort[q] | (k.meng[q] >= 1f ? 16 : 0);
                                    veld[1][idx] = k.leeg ? -1 : k.hoogte[q];
                                    veld[2][idx] = k.leeg || k.water[q] == Kaart.GEEN ? -1 : k.water[q];
                                    veld[3][idx] = k.leeg ? -1 : k.terras[q];
                                    veld[4][idx] = k.leeg ? 0 : k.vlag[q];
                                    if (!k.leeg && k.terras[q] >= 0 && k.meng[q] >= 1f) {
                                        int x = (c0x + cx << 4) + (q & 15), z = (c0z + cz << 4) + (q >> 4);
                                        DalTerrein.vorm(m, x, z, m.eDal(x, z), DalTerrein.steilte(m, x, z), v);
                                        if (v.kern) {
                                            veld[5][idx] = v.val;
                                            veld[6][idx] = (v.echt ? 1 : 0) | (v.hoog ? 2 : 0) | ((v.t1 - v.t0) / v.g < DalTerrein.RICHEL ? 4 : 0)
                                                    | ((v.t2 - v.t1) / v.g < DalTerrein.RICHEL ? 8 : 0);
                                            veld[7][idx] = (int) Math.round(v.bron * 100);
                                            veld[8][idx] = (int) Math.round(Math.max(-300, Math.min(300, v.sn)) * 10);
                                        }
                                    }
                                }
                            }
                        }
                        for (int[] f : veld) {
                            for (int w : f) {
                                o.writeShort(w);
                            }
                        }
                    }
                    LOGGER.info("biomes3 dal: model map of the dal at {} {} (seed {}) written to {} ({} ms for {} chunk maps)", dal[0], dal[1], seed,
                            uit.toAbsolutePath(), (System.nanoTime() - t0) / 1000000, chunks * chunks);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /** The dals the pictures are made of. */
    static List<int[]> plekken(BioModel m, long seed) {
        List<int[]> lijst = new ArrayList<>(dalen(m, 2));
        if (seed == SEEDS[0]) {
            // the dal nearest to spawn on the scratch servers' seed (the one every report names)
            lijst.add(new int[]{300, 480});
        }
        return lijst;
    }
}
