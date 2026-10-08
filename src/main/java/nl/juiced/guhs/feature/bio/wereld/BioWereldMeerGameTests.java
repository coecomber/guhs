package nl.juiced.guhs.feature.bio.wereld;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * biomes3 wereld, the Bloesemmeertje: the game tests of the finished lake. Like {@link BioWereldGameTests} they ask the
 * terrain model of the real noise settings (the test server has no Guhmensie); what needs generated chunks is measured
 * on the dev server from the region files (tools/features/bio_wereld_meer_meet.py).
 * <p>
 * With the environment variable GUHS_BIO_MEER_KAART set, {@link #bioWereldMeerVorm} also writes every lake it measures as
 * a text map (bio_meer_&lt;seed&gt;_&lt;n&gt;.txt in the working directory; bio_wereld_meer_meet.py draws it).
 */
public class BioWereldMeerGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_wereld_meer", EMPTY = "empty";
    private static final long[] SEEDS = {20261007L, 1L};

    private static BioModel model(GameTestHelper helper, long seed) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        return BioModel.van(RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed));
    }

    private static boolean meerkolom(Kaart k, int o) {
        return k.terras[o] < 0 && k.meng[o] >= 1f && k.soort[o] == Kaart.MEER;
    }

    /** One measured lake. */
    private static final class Meer {
        final Map<Long, Kaart> chunks = new HashMap<>();
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        int water, oever, strand, eiland, steen, grootKolommen, oeverDiep, oeverWater, lek, teDiep;
        final int[] diep = new int[10];
    }

    private static long ck(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** The lake around a lake column: every chunk with lake columns that hangs together with it (at most 3000 chunks). */
    private static Meer meet(BioModel m, int x, int z) {
        Meer meer = new Meer();
        ArrayDeque<long[]> rij = new ArrayDeque<>();
        rij.add(new long[]{x >> 4, z >> 4});
        Set<Long> gezien = new HashSet<>();
        gezien.add(ck(x >> 4, z >> 4));
        while (!rij.isEmpty() && meer.chunks.size() < 3000) {
            long[] c = rij.poll();
            int cx = (int) c[0], cz = (int) c[1];
            Kaart k = m.kaart(cx, cz);
            if (k.leeg) {
                continue;
            }
            boolean iets = false;
            for (int o = 0; o < 256; o++) {
                if (!meerkolom(k, o)) {
                    continue;
                }
                iets = true;
                int px = (cx << 4) + (o & 15), pz = (cz << 4) + (o >> 4), v = k.vlag[o], h = k.hoogte[o];
                meer.x0 = Math.min(meer.x0, px);
                meer.x1 = Math.max(meer.x1, px);
                meer.z0 = Math.min(meer.z0, pz);
                meer.z1 = Math.max(meer.z1, pz);
                if (k.water[o] != Kaart.GEEN) {
                    int d = k.water[o] - h;
                    meer.water++;
                    meer.diep[Math.min(9, d)]++;
                    meer.teDiep += d > MeerTerrein.DIEP_MAX ? 1 : 0;
                    boolean aanLand = false;
                    for (int r = 0; r < 8; r += 2) {
                        int bx = px + MeerTerrein.RX[r], bz = pz + MeerTerrein.RZ[r];
                        if (m.water(bx, bz) == Kaart.GEEN) {
                            // (a stepping stone or a boulder may stand in deeper water)
                            aanLand |= (m.vlag(bx, bz) & MeerTerrein.STEEN) == 0;
                            meer.lek += m.meng(bx, bz) < 1f || m.hoogte(bx, bz) < MeerTerrein.WATER ? 1 : 0;
                        }
                    }
                    if (aanLand) {
                        meer.oeverWater++;
                        meer.oeverDiep += d > 2 ? 1 : 0;
                    }
                } else if ((v & MeerTerrein.STEEN) != 0) {
                    meer.steen++;
                } else if ((v & Kaart.EILAND) != 0) {
                    meer.eiland++;
                    meer.grootKolommen += (v & Kaart.GROOT) != 0 ? 1 : 0;
                } else {
                    meer.oever++;
                    meer.strand += (v & MeerTerrein.STRAND) != 0 ? 1 : 0;
                }
            }
            if (iets) {
                meer.chunks.put(ck(cx, cz), k);
                for (int r = 0; r < 8; r += 2) {
                    int bx = cx + MeerTerrein.RX[r], bz = cz + MeerTerrein.RZ[r];
                    if (gezien.add(ck(bx, bz))) {
                        rij.add(new long[]{bx, bz});
                    }
                }
            }
        }
        return meer;
    }

    /** Lake columns from a coarse scan of 16 x 16 km, the max nearest to 0 0, at least 700 blocks apart. */
    private static List<int[]> zoek(BioModel m, int max) {
        List<int[]> alle = new ArrayList<>();
        for (int x = -8064; x <= 8064; x += 96) {
            for (int z = -8064; z <= 8064; z += 96) {
                if (m.soort(x, z) == Kaart.MEER && m.meng(x, z) >= 1f && m.terras(x, z) < 0) {
                    alle.add(new int[]{x, z});
                }
            }
        }
        alle.sort(java.util.Comparator.comparingLong(p -> (long) p[0] * p[0] + (long) p[1] * p[1]));
        List<int[]> uit = new ArrayList<>();
        for (int[] kandidaat : alle) {
            boolean nieuw = true;
            for (int[] p : uit) {
                nieuw &= Math.abs(p[0] - kandidaat[0]) + Math.abs(p[1] - kandidaat[1]) > 700;
            }
            if (nieuw && uit.size() < max) {
                uit.add(kandidaat);
            }
        }
        return uit;
    }

    private static void schrijf(BioModel m, Meer meer, String naam) {
        int x0 = (meer.x0 >> 4 << 4) - 16, z0 = (meer.z0 >> 4 << 4) - 16, x1 = (meer.x1 >> 4 << 4) + 32, z1 = (meer.z1 >> 4 << 4) + 32;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(naam), StandardCharsets.UTF_8))) {
            w.println("kaart " + x0 + " " + z0 + " " + (x1 - x0) + " " + (z1 - z0));
            StringBuilder rij = new StringBuilder();
            for (int z = z0; z < z1; z++) {
                rij.setLength(0);
                for (int x = x0; x < x1; x++) {
                    Kaart k = m.kaart(x >> 4, z >> 4);
                    int o = Kaart.index(x, z);
                    char c;
                    if (k.leeg || k.meng[o] < 1f) {
                        c = ' ';
                    } else if (k.terras[o] >= 0) {
                        c = (k.vlag[o] & Kaart.RIVIER) != 0 && k.water[o] != Kaart.GEEN ? '~' : (char) ('a' + k.terras[o]);
                    } else if (k.water[o] != Kaart.GEEN) {
                        c = (char) ('0' + Math.min(9, k.water[o] - k.hoogte[o]));
                    } else if ((k.vlag[o] & MeerTerrein.STEEN) != 0) {
                        c = 'o';
                    } else if ((k.vlag[o] & Kaart.EILAND) != 0) {
                        c = (k.vlag[o] & MeerTerrein.STRAND) != 0 ? 's' : k.hoogte[o] > MeerTerrein.WATER + 2 ? 'H' : k.hoogte[o] == MeerTerrein.WATER + 2 ? 'E' : 'e';
                    } else {
                        c = (k.vlag[o] & MeerTerrein.STRAND) != 0 ? 'z' : '.';
                    }
                    rij.append(c);
                }
                w.println(rij);
            }
            for (MeerTerrein.Boom b : MeerTerrein.bomen(m, x0, z0, x1, z1, true)) {
                double[] kr = BloesemBoom.kroon(b);
                w.println(String.format(Locale.ROOT, "boom %d %d %d %d %d %d %.1f %.1f %.1f", b.x(), b.z(), b.maat(), b.leunX(), b.leunZ(), b.vast() ? 1 : 0, kr[0], kr[1], kr[2]));
                for (long s : MeerLeven.sliert(m, b)) {
                    w.println("blaadje " + (int) (s >> 32) + " " + (int) s);
                }
            }
            for (int cx = x0 >> 4; cx < x1 >> 4; cx++) {
                for (int cz = z0 >> 4; cz < z1 >> 4; cz++) {
                    for (BioPlekken.Soort s : new BioPlekken.Soort[]{BioPlekken.Soort.MEER_EILAND, BioPlekken.Soort.MEER_BOOM}) {
                        BioPlekken.zoek(m, s, cx, cz, 0).ifPresent(p -> w.println("plek " + s.getSerializedName() + " " + p.x() + " " + p.z()));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("biomes3 meer: could not write {}: {}", naam, e.toString());
        }
    }

    /**
     * The shape of the lake, measured on whole lakes of two seeds: open water dominates; the shore and the water around
     * islands are 1-2 deep and the middle 5-8, never deeper; a lake that is big enough has a large island or a few, never
     * an archipelago; every large island keeps flat ground free of trees, with room to build; the big tree does not cover
     * the island; the structure spots on islands exist; no water lies next to lower dry ground.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void bioWereldMeerVorm(GameTestHelper helper) {
        boolean kaart = System.getenv("GUHS_BIO_MEER_KAART") != null;
        int meren = 0, groteMeren = 0, metEiland = 0, grootTotaal = 0, reuzen = 0, plekEiland = 0, plekBoom = 0, plekOever = 0, kleinBedekt = 0, kleinVlak = 0;
        int lek = 0, teDiep = 0, kleinTotaal = 0, stenen = 0, bomen = 0, leunend = 0;
        long waterTotaal = 0, landTotaal = 0, oeverWater = 0, oeverDiep = 0, diep5 = 0;
        double minOpen = 1, maxGrootPer = 0;
        String fout = "";
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            int nr = 0;
            Set<Long> gemeten = new HashSet<>();
            for (int[] plek : zoek(m, 6)) {
                // (a big lake is found more than once)
                if (nr >= 4 || gemeten.contains(ck(plek[0] >> 4, plek[1] >> 4))) {
                    continue;
                }
                Meer meer = meet(m, plek[0], plek[1]);
                gemeten.addAll(meer.chunks.keySet());
                if (meer.water < 400) {
                    continue;
                }
                meren++;
                nr++;
                int groot = 0, klein = 0, reus = 0, pe = 0, pb = 0, po = 0;
                StringBuilder eilanden = new StringBuilder();
                Set<Long> gezien = new HashSet<>();
                for (MeerTerrein.Eiland ei : MeerTerrein.eilandenBij(m, meer.x0, meer.z0, meer.x1 + 1, meer.z1 + 1)) {
                    if (!gezien.add(MeerTerrein.pak(ei.x(), ei.z())) || !meer.chunks.containsKey(ck(ei.x() >> 4, ei.z() >> 4))
                            || (m.vlag(ei.x(), ei.z()) & Kaart.EILAND) == 0) {
                        continue;
                    }
                    if (!ei.groot()) {
                        klein++;
                        continue;
                    }
                    groot++;
                    reus += ei.reus() ? 1 : 0;
                    // the island's columns: its size, its flat ground, what the crowns cover
                    Set<Long> blad = new HashSet<>();
                    for (MeerTerrein.Boom b : ei.bomen()) {
                        if (m.droog(b.x(), b.z())) {
                            for (Map.Entry<Long, Integer> c : BloesemBoom.cellen(b, m.hoogte(b.x(), b.z()) + 1).entrySet()) {
                                blad.add(MeerTerrein.pak(BlockPos.getX(c.getKey()), BlockPos.getZ(c.getKey())));
                            }
                        }
                    }
                    int land = 0, vlak = 0, vrij = 0, heuvel = 0, zand = 0, bx0 = 999999, bx1 = -999999, bz0 = 999999, bz1 = -999999, vrijVierkant = 0;
                    int r = (int) ei.bereik();
                    for (int px = ei.x() - r; px <= ei.x() + r; px++) {
                        for (int pz = ei.z() - r; pz <= ei.z() + r; pz++) {
                            int v = m.vlag(px, pz);
                            if ((v & Kaart.GROOT) == 0 || m.water(px, pz) != Kaart.GEEN) {
                                continue;
                            }
                            land++;
                            bx0 = Math.min(bx0, px);
                            bx1 = Math.max(bx1, px);
                            bz0 = Math.min(bz0, pz);
                            bz1 = Math.max(bz1, pz);
                            int h = m.hoogte(px, pz);
                            zand += (v & MeerTerrein.STRAND) != 0 ? 1 : 0;
                            heuvel += h > MeerTerrein.WATER + 2 ? 1 : 0;
                            if (h == MeerTerrein.WATER + 2) {
                                vlak++;
                                vrij += blad.contains(MeerTerrein.pak(px, pz)) ? 0 : 1;
                            }
                        }
                    }
                    // the largest square of flat ground with no crown over it (a simple search over the island's box)
                    for (int px = bx0; px <= bx1; px++) {
                        for (int pz = bz0; pz <= bz1; pz++) {
                            int n = 0;
                            zoek:
                            for (; n < 16; n++) {
                                for (int a = 0; a <= n; a++) {
                                    if (!vlakVrij(m, blad, px + n, pz + a) || !vlakVrij(m, blad, px + a, pz + n)) {
                                        break zoek;
                                    }
                                }
                            }
                            vrijVierkant = Math.max(vrijVierkant, n);
                        }
                    }
                    kleinVlak += vlak < 110 ? 1 : 0;
                    kleinBedekt += vrij * 100 < vlak * 60 || vrijVierkant < 6 ? 1 : 0;
                    eilanden.append(String.format(Locale.ROOT, " [%d %d: %dx%d, land %d, flat %d (free of crown %d, free square %d), hill %d, sand %d%s%s%s]", ei.x(), ei.z(),
                            bx1 - bx0 + 1, bz1 - bz0 + 1, land, vlak, vrij, vrijVierkant, heuvel, zand, ei.reus() ? ", giant tree" : "",
                            ei.lobben().length > 2 ? ", spit" : "", ei.stenen().length > 0 ? ", stepping stones" : ""));
                }
                int nb = 0, nl = 0;
                for (MeerTerrein.Boom b : MeerTerrein.bomen(m, meer.x0 - 16, meer.z0 - 16, meer.x1 + 17, meer.z1 + 17, true)) {
                    nb++;
                    nl += b.leunX() != 0 || b.leunZ() != 0 ? 1 : 0;
                }
                for (long c : meer.chunks.keySet()) {
                    int cx = (int) (c >> 32), cz = (int) c;
                    pe += BioPlekken.zoek(m, BioPlekken.Soort.MEER_EILAND, cx, cz, 0).isPresent() ? 1 : 0;
                    pb += BioPlekken.zoek(m, BioPlekken.Soort.MEER_BOOM, cx, cz, 0).isPresent() ? 1 : 0;
                    po += BioPlekken.zoek(m, BioPlekken.Soort.MEER_OEVER, cx, cz, 0).isPresent() ? 1 : 0;
                }
                int binnen = meer.water + meer.eiland + meer.steen;
                double open = meer.water / (double) Math.max(1, binnen);
                LOGGER.info("biomes3 meer {}/{} around {} {}: box {} x {}, water {} columns ({}% of water + islands), shore land {} (sand {}), island land {} (large {}), "
                                + "stone columns {}; depth 1..8: {} {} {} {} {} {} {} {}; water against land deeper than 2: {} of {}; large islands {}, small {}; "
                                + "trees {} (leaning {}); chunks with a spot: meer_eiland {}, meer_boom {}, meer_oever {};{}", seed, nr, plek[0], plek[1],
                        meer.x1 - meer.x0 + 1, meer.z1 - meer.z0 + 1, meer.water, Math.round(open * 1000) / 10.0, meer.oever, meer.strand, meer.eiland, meer.grootKolommen,
                        meer.steen, meer.diep[1], meer.diep[2], meer.diep[3], meer.diep[4], meer.diep[5], meer.diep[6], meer.diep[7], meer.diep[8], meer.oeverDiep,
                        meer.oeverWater, groot, klein, nb, nl, pe, pb, po, eilanden);
                if (kaart) {
                    schrijf(m, meer, "bio_meer_" + seed + "_" + nr + ".txt");
                }
                if (meer.water >= 20000) {
                    groteMeren++;
                    metEiland += groot > 0 ? 1 : 0;
                    minOpen = Math.min(minOpen, open);
                    // large islands per 100 x 100 blocks of water
                    maxGrootPer = Math.max(maxGrootPer, groot * 10000.0 / meer.water);
                }
                grootTotaal += groot;
                kleinTotaal += klein;
                reuzen += reus;
                plekEiland += pe;
                plekBoom += pb;
                plekOever += po;
                lek += meer.lek;
                teDiep += meer.teDiep;
                stenen += meer.steen;
                bomen += nb;
                leunend += nl;
                waterTotaal += meer.water;
                landTotaal += meer.eiland;
                oeverWater += meer.oeverWater;
                oeverDiep += meer.oeverDiep;
                diep5 += meer.diep[5] + meer.diep[6] + meer.diep[7] + meer.diep[8];
                if (meer.diep[9] > 0 && fout.isEmpty()) {
                    fout = "deeper than 8 in the lake at " + plek[0] + " " + plek[1];
                }
            }
        }
        LOGGER.info("biomes3 meer, all {} lakes: water {} columns, island land {}, large islands {} (giant tree on {}), small {}, lakes of 20000+ water columns {} "
                        + "(with a large island {}), least open water {}%, most large islands per 100x100 of water {}, spot chunks eiland {} boom {} oever {}, "
                        + "stone columns {}, trees {} (leaning {}), shore water deeper than 2: {} of {}, 5+ deep {}%", meren, waterTotaal, landTotaal, grootTotaal, reuzen,
                kleinTotaal, groteMeren, metEiland, Math.round(minOpen * 1000) / 10.0, Math.round(maxGrootPer * 100) / 100.0, plekEiland, plekBoom, plekOever, stenen,
                bomen, leunend, oeverDiep, oeverWater, Math.round(diep5 * 1000.0 / Math.max(1, waterTotaal)) / 10.0);
        helper.assertTrue(meren >= 4, "lakes found: " + meren);
        helper.assertTrue(fout.isEmpty() && teDiep == 0, "never deeper than 8: " + fout);
        helper.assertTrue(lek == 0, "no lake water next to lower dry ground: " + lek);
        helper.assertTrue(minOpen >= 0.9, "open water dominates: at least " + minOpen);
        helper.assertTrue(oeverDiep * 50 <= oeverWater, "the water against land is 1-2 deep: " + oeverDiep + " of " + oeverWater + " deeper");
        helper.assertTrue(diep5 * 100 >= waterTotaal * 30, "the middle is 5-8 deep: " + diep5 + " of " + waterTotaal);
        helper.assertTrue(groteMeren >= 2 && metEiland * 4 >= groteMeren * 3, "a big lake has a large island: " + metEiland + " of " + groteMeren);
        helper.assertTrue(maxGrootPer <= 1.0, "no archipelago: at most one large island per 100 x 100 blocks of water, measured " + maxGrootPer);
        helper.assertTrue(kleinVlak == 0 && kleinBedekt == 0, "every large island keeps flat building ground free of trees: too little flat on " + kleinVlak
                + ", too much under a crown on " + kleinBedekt);
        helper.assertTrue(plekEiland >= grootTotaal && plekBoom >= grootTotaal * 3 / 4, "the island spots exist: meer_eiland in " + plekEiland + " chunks, meer_boom in "
                + plekBoom + ", on " + grootTotaal + " large islands");
        helper.assertTrue(bomen >= 40 && leunend >= 10 && stenen >= 10, "trees (some leaning over the water), boulders and stepping stones exist");
        helper.succeed();
    }

    private static boolean vlakVrij(BioModel m, Set<Long> blad, int x, int z) {
        return (m.vlag(x, z) & Kaart.GROOT) != 0 && m.hoogte(x, z) == MeerTerrein.WATER + 2 && m.water(x, z) == Kaart.GEEN && !blad.contains(MeerTerrein.pak(x, z));
    }

    /**
     * The tree in its four sizes, standing and leaning: every block hangs together with the trunk, hardly a leaf is
     * further than 6 from wood, nothing hangs lower than two above the foot, the hanging strands are few (no curtain),
     * and the giant's crown stays well under the size of a large island.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldMeerBoom(GameTestHelper helper) {
        StringBuilder log = new StringBuilder();
        for (int maat = 0; maat <= 3; maat++) {
            for (int variant = 0; variant < 12; variant++) {
                boolean vast = maat >= 2 && variant % 3 == 0;
                int lx = variant % 4 == 1 ? 0 : variant % 2 == 0 ? 1 : -1, lz = variant % 4 == 1 ? 0 : variant % 4 == 2 ? 1 : 0;
                if (maat == 3 && lx == 0 && lz == 0) {
                    lx = 1;
                }
                MeerTerrein.Boom b = new MeerTerrein.Boom(100, 200, maat, lx, lz, vast, BioModel.mix(maat * 100L + variant));
                Map<Long, Integer> cellen = BloesemBoom.cellen(b, 60);
                helper.assertTrue(cellen.getOrDefault(BlockPos.asLong(100, 60, 200), 0) == BloesemBoom.STAM_Y, "the foot is wood");
                // one piece
                Set<Long> bereikt = new HashSet<>();
                ArrayDeque<Long> rij = new ArrayDeque<>();
                rij.add(BlockPos.asLong(100, 60, 200));
                bereikt.add(rij.peek());
                while (!rij.isEmpty()) {
                    long p = rij.poll();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                long q = BlockPos.asLong(BlockPos.getX(p) + dx, BlockPos.getY(p) + dy, BlockPos.getZ(p) + dz);
                                if (cellen.containsKey(q) && bereikt.add(q)) {
                                    rij.add(q);
                                }
                            }
                        }
                    }
                }
                int blad = 0, ver = 0, laag = 999, hang = 0;
                Set<Long> kolommen = new HashSet<>();
                Map<Long, Integer> onder = new HashMap<>();
                for (Map.Entry<Long, Integer> c : cellen.entrySet()) {
                    if (c.getValue() >= BloesemBoom.BLAD) {
                        blad++;
                        ver += c.getValue() - BloesemBoom.BLAD >= 7 ? 1 : 0;
                        int y = BlockPos.getY(c.getKey());
                        laag = Math.min(laag, y);
                        long kol = MeerTerrein.pak(BlockPos.getX(c.getKey()), BlockPos.getZ(c.getKey()));
                        kolommen.add(kol);
                        onder.merge(kol, y, Math::min);
                    }
                }
                int vloer = 60 + BloesemBoom.hoogte(b) - 2 - (maat >= 2 ? 1 : 0);
                for (int y : onder.values()) {
                    hang += y < vloer ? 1 : 0;
                }
                if (variant == 0) {
                    log.append(String.format(Locale.ROOT, " size %d: %d blocks, %d leaves over %d columns, %d persistent, %d columns with a hanging strand;", maat,
                            cellen.size(), blad, kolommen.size(), ver, hang));
                }
                helper.assertTrue(bereikt.size() == cellen.size(), "the tree is one piece (size " + maat + ", variant " + variant + "): " + bereikt.size() + " of " + cellen.size());
                helper.assertTrue(ver * 10 <= blad, "leaves are near wood (size " + maat + "): " + ver + " of " + blad + " further than 6");
                helper.assertTrue(laag >= 62, "nothing hangs lower than two above the foot: " + laag);
                helper.assertTrue(hang * 5 <= kolommen.size(), "hanging strands are few (size " + maat + "): " + hang + " of " + kolommen.size() + " columns");
                helper.assertTrue(kolommen.size() <= (maat == 3 ? 190 : maat == 2 ? 110 : 60), "the crown of size " + maat + " covers " + kolommen.size() + " columns");
                double[] kroon = BloesemBoom.kroon(b);
                helper.assertTrue(cellen.keySet().stream().anyMatch(p -> Math.abs(BlockPos.getX(p) - kroon[0]) < 1.5 && Math.abs(BlockPos.getZ(p) - kroon[1]) < 1.5),
                        "kroon() points into the crown");
            }
        }
        LOGGER.info("biomes3 meer trees:{}", log);
        helper.succeed();
    }

    /** The bed: sand in the shallows, the darkest block in the deep, bluer with every block of depth, and mixed between (no contour lines). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldMeerBodem(GameTestHelper helper) {
        List<Block> volgorde = List.of(Blocks.SAND, MeerBodem.MEERZAND.get(), MeerBodem.SLIB_LICHT.get(), MeerBodem.SLIB.get(), MeerBodem.SLIB_DIEP.get());   // biomes3 fix-klein
        double vorige = -1;
        for (int d = 1; d <= MeerTerrein.DIEP_MAX; d++) {
            int[] n = new int[volgorde.size()];
            double som = 0;
            for (int i = 0; i < 4000; i++) {
                int idx = volgorde.indexOf(MeerVulling.bodem(d, i * 7 - 9000, i * 13 + 500).getBlock());
                helper.assertTrue(idx >= 0, "a known bed block");
                n[idx]++;
                som += idx;
            }
            double gemiddeld = som / 4000;
            int soorten = 0;
            for (int a : n) {
                soorten += a > 40 ? 1 : 0;
            }
            helper.assertTrue(gemiddeld > vorige, "the bed gets bluer with depth: " + gemiddeld + " at " + d);
            helper.assertTrue(soorten <= 3, "at most three kinds at one depth: " + soorten + " at " + d);
            if (d == 1) {
                helper.assertTrue(n[0] > 3000, "sand in the shallows");
            }
            if (d == 3 || d == 5) {
                helper.assertTrue(soorten >= 2, "mixed where two kinds meet (depth " + d + ")");
            }
            if (d == MeerTerrein.DIEP_MAX) {
                helper.assertTrue(n[4] == 4000, "the deep is the darkest block");
            }
            vorige = gemiddeld;
        }
        helper.succeed();
    }

    /** The day rhythm of the petals: more in a gust, at dusk and dawn, in rain, most in thunder; fewer in a quiet night. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldMeerRitme(GameTestHelper helper) {
        long stil = 1125, vlaag = 375;   // the trough and the crest of the breeze
        float dag = MeerRitme.sterkte(0, 0, false, 0, stil);
        helper.assertTrue(MeerRitme.sterkte(0, 0, false, 0, vlaag) > dag * 2, "more petals in a gust");
        helper.assertTrue(MeerRitme.sterkte(0, 0, true, 0, stil) < dag, "fewer in a quiet night");
        helper.assertTrue(MeerRitme.sterkte(0, 0, false, MeerRitme.BRIES, stil) > dag * 2, "more in the dusk and dawn breeze");
        float regen = MeerRitme.sterkte(1, 0, false, 0, stil);
        helper.assertTrue(regen > dag * 2.5f, "more in rain");
        helper.assertTrue(MeerRitme.sterkte(1, 1, false, 0, stil) > regen, "most in thunder");
        helper.assertTrue(MeerRitme.sterkte(1, 1, true, MeerRitme.BRIES, vlaag) < 6, "never a blizzard: at most a handful a tick");
        helper.succeed();
    }

    /** Bloesemriet: a tall plant that stands on sand and on the Guhmensie's wool, and has an item. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldMeerRiet(GameTestHelper helper) {
        BlockState onder = MeerLeven.RIET.get().defaultBlockState();
        int i = 0;
        for (Block grond : List.of(Blocks.SAND, Blocks.PINK_WOOL)) {
            BlockPos p = new BlockPos(1 + i++ * 2, 1, 1);
            helper.setBlock(p, grond);
            helper.setBlock(p.above(), onder);
            helper.setBlock(p.above(2), onder.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            helper.assertTrue(onder.canSurvive(helper.getLevel(), helper.absolutePos(p.above())), "bloesemriet stands on " + grond);
        }
        helper.assertTrue(!onder.canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(1, 5, 1))), "but not in the air");
        helper.assertTrue(MeerLeven.RIET_ITEM.get().getBlock() == MeerLeven.RIET.get(), "its item places it");
        helper.succeed();
    }
}
