package nl.juiced.guhs.feature.bio.wereld;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
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
     * The finished shape, in a few dals of two seeds: terraces are broad (or absent), water never steps down ONE block (a
     * one-block step would turn the lower river into new sources), the lip of every fall is one deep, falls come in the
     * heights of the three kinds of cascade, tall waterfalls (10 or more within 6 columns) exist, and so do koi pools,
     * stepping stones, boulders and natural steps; a flight of steps climbs one block at a time.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldDalVorm(GameTestHelper helper) {
        int dalen = 0, eentjes = 0, diepeLip = 0, hoog = 0, stenen = 0, treden = 0, tredenLos = 0, vorm = 0, poelen = 0, breed = 0, vlak = 0, water = 0;
        int[] val = new int[24];
        String eerste = "";
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            for (int[] dal : plekken(m, seed)) {
                dalen++;
                int c0x = dal[0] >> 4, c0z = dal[1] >> 4;
                for (int cx = c0x - 16; cx <= c0x + 16; cx++) {
                    for (int cz = c0z - 16; cz <= c0z + 16; cz++) {
                        Kaart k = m.kaart(cx, cz);
                        if (k.leeg) {
                            continue;
                        }
                        final int kx = cx, kz = cz;
                        poelen += (int) DalTerrein.poelen(m, cx, cz).stream().filter(p -> (Math.floorDiv((int) Math.floor(p.x()), 16)) == kx
                                && (Math.floorDiv((int) Math.floor(p.z()), 16)) == kz).count();
                        for (int o = 0; o < 256; o++) {
                            if (k.terras[o] < 0 || k.meng[o] < 1f) {
                                continue;
                            }
                            int x = (cx << 4) + (o & 15), z = (cz << 4) + (o >> 4), h = k.hoogte[o], w = k.water[o], vl = k.vlag[o];
                            if (w != Kaart.GEEN) {
                                water++;
                                int diepst = 0;
                                for (Direction d : Direction.Plane.HORIZONTAL) {
                                    int bw = m.water(x + d.getStepX(), z + d.getStepZ());
                                    if (bw != Kaart.GEEN && bw < w) {
                                        diepst = Math.max(diepst, w - bw);
                                        if (w - bw == 1) {
                                            eentjes++;
                                            eerste = eerste.isEmpty() ? "a step of one at " + x + " " + z : eerste;
                                        }
                                        hoog += BioPlekken.val(m, x, z, d) >= 10 ? 1 : 0;
                                    }
                                }
                                if (diepst > 0) {
                                    val[Math.min(23, diepst)]++;
                                    if (w - h != 1) {
                                        diepeLip++;
                                        eerste = eerste.isEmpty() ? "the lip of a fall deeper than one at " + x + " " + z : eerste;
                                    }
                                }
                                continue;
                            }
                            if ((vl & Kaart.LIP) != 0) {
                                stenen += h == DalTerrein.HOOGTE[k.terras[o]] - 1 ? 1 : 0;
                            } else if ((vl & DalTerrein.TREDE) != 0) {
                                treden++;
                                boolean aansluiting = false;
                                for (Direction d : Direction.Plane.HORIZONTAL) {
                                    int bx = x + d.getStepX(), bz = z + d.getStepZ();
                                    aansluiting |= m.water(bx, bz) == Kaart.GEEN && Math.abs(m.hoogte(bx, bz) - h) == 1;
                                }
                                tredenLos += aansluiting ? 0 : 1;
                            } else if ((vl & DalTerrein.VORM) != 0) {
                                vorm++;
                            } else if (k.terras[o] == 1 || k.terras[o] == 2) {
                                // a plain column of a middle terrace: how broad is the terrace through it (the shorter of the two runs)?
                                vlak++;
                                int run = 99;
                                for (int as = 0; as < 2; as++) {
                                    int n = 1;
                                    for (int richting = -1; richting <= 1; richting += 2) {
                                        for (int a = 1; a <= 6; a++) {
                                            int bx = x + (as == 0 ? richting * a : 0), bz = z + (as == 1 ? richting * a : 0);
                                            if (m.terras(bx, bz) != k.terras[o] || m.meng(bx, bz) < 1f) {
                                                break;
                                            }
                                            n++;
                                        }
                                    }
                                    run = Math.min(run, n);
                                }
                                breed += run >= 9 ? 1 : 0;
                            }
                        }
                    }
                }
            }
        }
        StringBuilder hist = new StringBuilder();
        for (int i = 1; i < val.length; i++) {
            if (val[i] > 0) {
                hist.append(' ').append(i).append(':').append(val[i]);
            }
        }
        LOGGER.info("biomes3 Klaterdal shape: {} dals; {} river and pool columns, falls by drop [{}], tall falls (10+) {}, steps of one {}, deep lips {}; "
                        + "koi pools {}, stepping stones {}, sculpted rock columns {}, natural steps {} (loose {}); middle-terrace columns in a terrace "
                        + "9+ wide both ways: {} of {} ({}%) {}", dalen, water, hist.toString().trim(), hoog, eentjes, diepeLip, poelen, stenen, vorm, treden,
                tredenLos, breed, vlak, vlak == 0 ? 0 : 100 * breed / vlak, eerste);
        helper.assertTrue(dalen >= 3, "dals found: " + dalen);
        helper.assertTrue(eentjes == 0, "water never steps down one block: " + eentjes + " " + eerste);
        helper.assertTrue(diepeLip == 0, "the lip of a fall is one deep: " + diepeLip + " " + eerste);
        helper.assertTrue(val[2] > 50 && val[3] > 10 && val[5] > 30 && val[4] > 5, "the three kinds of cascade occur (drops of 2, 3, 4 and 5): " + hist);
        int groot = 0, alle = 0;
        for (int i = 1; i < val.length; i++) {
            alle += val[i];
            groot += i > 5 ? val[i] : 0;
        }
        // (where two kinds of cascade meet, a few columns drop two steps at once)
        helper.assertTrue(groot * 50 <= alle && val[12] + val[13] + val[14] == 0, "falls come in steps of at most 5: " + hist);
        helper.assertTrue(hoog >= 6, "tall waterfalls (10 or more within 6 columns): " + hoog);
        helper.assertTrue(poelen >= 6 && stenen >= 6 && vorm > 500, "pools " + poelen + ", stepping stones " + stenen + ", rock " + vorm);
        helper.assertTrue(treden > 100 && tredenLos * 20 <= treden, "natural steps climb one block at a time: " + treden + ", loose " + tredenLos);
        helper.assertTrue(vlak > 5000 && breed * 100 >= vlak * 70, "middle terraces are broad: " + breed + " of " + vlak);
        helper.succeed();
    }

    /**
     * The waterfall rule of slice blokken-wolk (foam, mist and sound from a drop of {@code Waterval.GROOT} on), asked the way
     * the client asks it, for every kind of step the valley has: the steps of a gentle cascade (2 and 3) and the first step
     * of the other (2) get nothing, the fall at its foot (5) and every step of the tall waterfall (5, 5, 4) do; a drop of
     * one never occurs (see bioWereldDalVorm).
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldDalWatervalRegel(GameTestHelper helper) {
        for (double[][] soort : new double[][][]{DalTerrein.ZACHT, DalTerrein.STEVIG, DalTerrein.HOOG, DalTerrein.ZACHT_RAND, DalTerrein.STEVIG_RAND,
                DalTerrein.HOOG_RAND}) { // biomes3 fix-dal: the rim has its own three
            double vorige = 0;
            for (int i = 0; i < soort.length; i++) {
                int stap = (int) (soort[i][1] - vorige);
                vorige = soort[i][1];
                helper.assertTrue(stap >= 2, "a step is never one block: " + stap);
                // the water of the upper bed (top block at y 20) falls to the lower bed (top block at 20 - stap): the block
                // beside the lip at y 20 flows, below it the water falls down to just above the lower water
                final int boven = 20, onder = 20 - stap;
                var voet = nl.juiced.guhs.feature.bio.blokkenwolk.Waterval.meet(y -> y > onder && y < boven, y -> y <= onder, boven - 1);
                boolean groot = voet != null && voet.groot();
                boolean moet = stap >= nl.juiced.guhs.feature.bio.blokkenwolk.Waterval.GROOT;
                helper.assertTrue(groot == moet, "a drop of " + stap + (moet ? " gets" : " gets no") + " foam: " + voet);
                if (soort == DalTerrein.ZACHT || soort == DalTerrein.ZACHT_RAND) {
                    helper.assertTrue(!groot, "a gentle cascade has no foam (drop " + stap + ")");
                }
                if (soort == DalTerrein.HOOG || soort == DalTerrein.HOOG_RAND) {
                    helper.assertTrue(groot, "every step of the tall waterfall has foam (drop " + stap + ")");
                }
            }
        }
        var stevig = DalTerrein.STEVIG;
        helper.assertTrue(stevig[stevig.length - 1][1] - stevig[stevig.length - 2][1] >= 4, "the cascade with a fall has a real fall at its foot");
        helper.succeed();
    }

    /** The spots the structure slice asks for stay plentiful in every dal: counted per dal (33 x 33 chunks around its lake). */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldDalPlekken(GameTestHelper helper) {
        BioPlekken.Soort[] soorten = {BioPlekken.Soort.TERRAS, BioPlekken.Soort.OEVER, BioPlekken.Soort.OVER_RIVIER, BioPlekken.Soort.WATERVAL,
                BioPlekken.Soort.ROTS};
        int[] totaal = new int[soorten.length];
        int dalen = 0, zonderRots = 0;
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            for (int[] dal : plekken(m, seed)) {
                dalen++;
                int[] n = new int[soorten.length];
                for (int cx = (dal[0] >> 4) - 16; cx <= (dal[0] >> 4) + 16; cx++) {
                    for (int cz = (dal[1] >> 4) - 16; cz <= (dal[1] >> 4) + 16; cz++) {
                        if (m.kaart(cx, cz).leeg) {
                            continue;
                        }
                        for (int i = 0; i < soorten.length; i++) {
                            if (BioPlekken.zoek(m, soorten[i], cx, cz, 30).isPresent()) {
                                n[i]++;
                                totaal[i]++;
                            }
                        }
                    }
                }
                zonderRots += n[4] == 0 ? 1 : 0;
                LOGGER.info("biomes3 Klaterdal spots, dal at {} {} (seed {}): chunks with terras {}, oever {}, over_rivier {}, waterval {}, rots {}", dal[0],
                        dal[1], seed, n[0], n[1], n[2], n[3], n[4]);
                helper.assertTrue(n[0] >= 20 && n[1] >= 10 && n[2] >= 4 && n[3] >= 2, "every dal has terras, oever, over_rivier and waterval spots: "
                        + java.util.Arrays.toString(n) + " at " + dal[0] + " " + dal[1]);
            }
        }
        LOGGER.info("biomes3 Klaterdal spots in {} dals together: terras {}, oever {}, over_rivier {}, waterval {}, rots {} ({} dals without a rots spot)",
                dalen, totaal[0], totaal[1], totaal[2], totaal[3], totaal[4], zonderRots);
        helper.assertTrue(totaal[4] >= 5, "rots spots (the top of a tall waterfall): " + totaal[4]);
        helper.succeed();
    }

    /** The weather rhythm: calm most of the time, a gust that swells and dies, more petals in a gust and in the rain. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldDalWeer(GameTestHelper helper) {
        helper.assertTrue(DalWeer.vlaag(DalWeer.VLAAG_DUUR + 5) == 0 && DalWeer.blaadjes(DalWeer.VLAAG_DUUR + 5, false) == 0, "calm between the gusts");
        helper.assertTrue(DalWeer.vlaag(DalWeer.VLAAG_DUUR / 2) > 0.99, "the middle of a gust");
        helper.assertTrue(DalWeer.vlaag(DalWeer.VLAAG_OM * 7L + DalWeer.VLAAG_DUUR / 2) > 0.99, "a gust every " + DalWeer.VLAAG_OM + " ticks");
        helper.assertTrue(DalWeer.blaadjes(DalWeer.VLAAG_DUUR / 2, false) == DalWeer.VLAAG_BLAADJES, "petals at the height of a gust");
        helper.assertTrue(DalWeer.blaadjes(DalWeer.VLAAG_DUUR + 5, true) == DalWeer.REGEN_BLAADJES, "petals in the rain");
        helper.assertTrue(DalWeer.blaadjes(DalWeer.VLAAG_DUUR / 2, true) == DalWeer.VLAAG_BLAADJES + DalWeer.REGEN_BLAADJES, "both together");
        int stil = 0;
        for (long t = 0; t < DalWeer.VLAAG_OM; t++) {
            stil += DalWeer.blaadjes(t, false) == 0 ? 1 : 0;
        }
        helper.assertTrue(stil > DalWeer.VLAAG_OM * 0.8, "most of the time nothing extra falls: " + stil + " of " + DalWeer.VLAAG_OM + " ticks");
        helper.succeed();
    }

    /** The valley's own blocks: each has an item, drops itself, and stands in its tags; the three sounds are registered. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldDalBlokken(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (var blok : DalBlokken.blokken()) {
            String id = blok.getId().getPath();
            helper.assertTrue(blok.get().asItem() != net.minecraft.world.item.Items.AIR, id + " has an item");
            var tabel = blok.get().getLootTable();
            helper.assertTrue(tabel.isPresent() && server.reloadableRegistries().getLootTable(tabel.get()) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    id + " has a loot table");
        }
        helper.assertTrue(DalBlokken.MOS.get().defaultBlockState().is(net.minecraft.tags.BlockTags.DIRT), "plants stand on the moss (tag dirt)");
        helper.assertTrue(DalBlokken.BONSAIBLAD.get().defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES), "bonsailoof is leaves");
        for (String geluid : new String[]{"klaterdal.muziek", "klaterdal.sfeer", "klaterdal.windgong"}) {
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id(geluid)), "sound " + geluid);
        }
        helper.succeed();
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
                                        case 1 -> m.dalEigen(x, z); // biomes3 fix-plaatsing: was the dal noise above its threshold
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
            // biomes3 fix-plaatsing: was the fixed place 300 480; the regions are shapes now (BioRegio) and lie elsewhere: asked of the model
            BioRegio dichtst = null;
            for (BioRegio g : BioRegio.bij(m, -4000, -4000, 4000, 4000)) {
                if (!g.weide && (dichtst == null || Math.hypot(g.x, g.z) < Math.hypot(dichtst.x, dichtst.z))) {
                    dichtst = g;
                }
            }
            if (dichtst != null) {
                double[] p = dichtst.inDal(0, 0.5);
                lijst.add(new int[]{(int) p[0], (int) p[1]});
            }
        }
        return lijst;
    }
}
