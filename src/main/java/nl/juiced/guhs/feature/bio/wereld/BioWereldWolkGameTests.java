package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * biomes3 wereld, the Wolkenweide polish: the jump rule, and the proof that the ways up work, on the model (the game
 * test server has no Guhmensie: the random state is built from the dimension's noise settings, as
 * {@link BioWereldGameTests} does). The same walk over generated region files is the offline tool
 * {@code guhs_workbio/reports/screens/wereld-wolk/_bron/wolkmeet.py}.
 */
public class BioWereldWolkGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_wereld_wolk", EMPTY = "empty";
    private static final long[] SEEDS = {20261007L, 1L};

    private static BioModel model(GameTestHelper helper, long seed) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        return BioModel.van(RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed));
    }

    private static List<int[]> weiden(BioModel m, int max) {
        List<int[]> uit = new ArrayList<>();
        for (int x = -12000; x <= 12000 && uit.size() < max; x += 96) {
            for (int z = -12000; z <= 12000 && uit.size() < max; z += 96) {
                if (m.soort(x, z) == Kaart.WEIDE && m.meng(x, z) >= 1f) {
                    boolean nieuw = true;
                    for (int[] p : uit) {
                        nieuw &= Math.abs(p[0] - x) + Math.abs(p[1] - z) > 400;
                    }
                    if (nieuw) {
                        uit.add(new int[]{x, z});
                    }
                }
            }
        }
        return uit;
    }

    /** The jump rule itself: what a player can and cannot do. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldWolkSprongregel(GameTestHelper helper) {
        // heights in half blocks; (dx, dz) between the two blocks
        helper.assertTrue(WolkRoute.kan(1, 0, 20, 22, false), "a step of one block up");
        helper.assertTrue(WolkRoute.kan(2, 0, 20, 22, false), "one up across a gap of one");
        helper.assertTrue(!WolkRoute.kan(3, 0, 20, 22, false), "one up across a gap of two is a trick jump");
        helper.assertTrue(!WolkRoute.kan(1, 0, 20, 23, false) && !WolkRoute.kan(1, 0, 20, 24, false), "more than one block up cannot be jumped");
        helper.succeed();
    }

    /** The search itself on a hand-made box: a stair with gaps, a lift, a stream, a drop onto cloud and one onto stone. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldWolkZoektocht(GameTestHelper helper) {
        helper.assertTrue(WolkRoute.gat(2, 0) == 1 && WolkRoute.gat(3, 0) == 2 && WolkRoute.gat(1, 1) == 0 && Math.abs(WolkRoute.gat(2, 2) - 1.414) < 0.01, "gaps");
        helper.assertTrue(!WolkRoute.kan(2, 2, 20, 22, false), "one up across a diagonal gap of 1.4 is too far");
        helper.assertTrue(WolkRoute.kan(3, 0, 20, 20, false) && WolkRoute.kan(3, 0, 20, 16, false) && !WolkRoute.kan(4, 0, 20, 20, false), "level and down: a gap of two");
        helper.assertTrue(WolkRoute.kan(1, 0, 20, 14, false) && !WolkRoute.kan(1, 0, 20, 13, false) && WolkRoute.kan(1, 0, 80, 14, true),
                "a drop of three onto stone, any drop onto cloud");
        WolkRoute.Blokken b = new WolkRoute.Blokken(0, 0, 0, 40, 40, 8);
        for (int x = 0; x < 40; x++) {
            for (int z = 0; z < 8; z++) {
                b.zet(x, 0, z, WolkRoute.VAST);
            }
        }
        // stepping stones: one up, a gap of one
        for (int s = 0; s < 5; s++) {
            b.zet(2 + s * 2, 1 + s, 3, WolkRoute.VAST);
        }
        // a platform at y 20 with a lift up beside it and a stream down
        for (int x = 20; x < 30; x++) {
            for (int z = 2; z < 6; z++) {
                b.zet(x, 20, z, WolkRoute.VAST);
            }
        }
        // a lone stone too high to jump to
        b.zet(14, 8, 3, WolkRoute.VAST);
        // cloud under the platform's far rim
        b.zet(31, 0, 3, WolkRoute.WOLK);
        List<WolkRoute.Lift> liften = List.of(new WolkRoute.Lift(18, 3, 0, 22, false));
        b.loop(new int[]{0, 2, 3}, liften);
        helper.assertTrue(b.bereikt(10, 12, 3), "the top stepping stone is reached");
        helper.assertTrue(!b.bereikt(14, 18, 3), "the lone high stone is not");
        helper.assertTrue(b.bereikt(21, 42, 3), "the lift carries to the platform");
        b.loop(new int[]{25, 42, 3}, List.of());
        helper.assertTrue(b.bereikt(31, 2, 3), "down from the platform onto cloud");
        WolkRoute.Blokken c = new WolkRoute.Blokken(0, 0, 0, 40, 40, 8);
        System.arraycopy(b.soort, 0, c.soort, 0, b.soort.length);
        c.zet(31, 0, 3, WolkRoute.VAST);
        c.loop(new int[]{25, 42, 3}, List.of());
        helper.assertTrue(!c.bereikt(19, 2, 3), "but not down onto stone from twenty up");
        c.loop(new int[]{25, 42, 3}, List.of(new WolkRoute.Lift(18, 3, 0, 21, true)));
        helper.assertTrue(c.bereikt(19, 2, 3), "the stream floats you down");
        helper.succeed();
    }

    private static byte soort(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (id.startsWith("wolkenblok")) {
            return s.hasProperty(SlabBlock.TYPE) && s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? WolkRoute.PLAAT : WolkRoute.WOLK;
        }
        return id.equals("wolkenlift") ? WolkRoute.PAD : id.equals("wolkenstroom") ? WolkRoute.STROOM : id.equals("water") ? WolkRoute.WATER : WolkRoute.VAST;
    }

    /** The blocks of the model around a stack, for the search. tel: [cloud blocks, slabs, stairs, water]. */
    private static WolkRoute.Blokken blokken(BioModel m, WolkTerrein.Stapel s, List<int[]> grond, int[] tel) {
        int reik = s.straal + 6, y0 = WolkTerrein.WEIDE_Y - 8;
        WolkRoute.Blokken b = new WolkRoute.Blokken(s.x - reik, y0, s.z - reik, 2 * reik + 1, s.hoogste().top + 12 - y0, 2 * reik + 1);
        for (int cx = (s.x - reik) >> 4; cx <= (s.x + reik) >> 4; cx++) {
            for (int cz = (s.z - reik) >> 4; cz <= (s.z + reik) >> 4; cz++) {
                Kaart k = m.kaart(cx, cz);
                if (k.leeg) {
                    continue;
                }
                for (int o = 0; o < 256; o++) {
                    if (k.meng[o] < 1f) {
                        continue;
                    }
                    int x = (cx << 4) + (o & 15), z = (cz << 4) + (o >> 4);
                    for (int y = y0; y <= k.hoogte[o]; y++) {
                        b.zet(x, y, z, WolkRoute.VAST);
                    }
                    if (k.soort[o] == Kaart.WEIDE) {
                        grond.add(new int[]{x, 2 * (k.hoogte[o] + 1), z});
                    }
                    int[] sp = k.spans[o];
                    if (sp != null) {
                        for (int i = 0; i < sp.length; i += 2) {
                            for (int y = sp[i]; y <= sp[i + 1]; y++) {
                                b.zet(x, y, z, WolkRoute.VAST);
                            }
                        }
                    }
                }
                WolkVulling.plan(m, k, WolkTerrein.stapels(m, cx << 4, cz << 4, (cx << 4) + 16, (cz << 4) + 16), (x, y, z, st, lucht) -> {
                    if (!lucht || b.op(x, y, z) == WolkRoute.LUCHT) {
                        byte srt = soort(st);
                        b.zet(x, y, z, srt);
                        if (tel != null) {
                            tel[0] += srt == WolkRoute.WOLK || srt == WolkRoute.PLAAT ? 1 : 0;
                            tel[1] += st.hasProperty(SlabBlock.TYPE) ? 1 : 0;
                            tel[2] += st.hasProperty(StairBlock.FACING) && st.hasProperty(StairBlock.HALF) ? 1 : 0;
                            tel[3] += srt == WolkRoute.WATER ? 1 : 0;
                        }
                    }
                });
            }
        }
        return b;
    }

    private static List<WolkRoute.Lift> liften(BioModel m, WolkTerrein.Stapel s) {
        List<WolkRoute.Lift> uit = new ArrayList<>();
        for (WolkTerrein.Stapel t : WolkTerrein.stapels(m, s.x - s.straal - 6, s.z - s.straal - 6, s.x + s.straal + 6, s.z + s.straal + 6)) {
            for (WolkTerrein.Kolom k : t.kolommen) {
                if (!k.los() || WolkTerrein.liftVrij(m, k.x(), k.z(), k.voet(), k.boven())) {
                    uit.add(new WolkRoute.Lift(k.x(), k.z(), k.voet(), k.boven(), k.omlaag()));
                }
            }
        }
        return uit;
    }

    private static int[] plat(List<int[]> l) {
        int[] r = new int[l.size() * 3];
        for (int i = 0; i < l.size(); i++) {
            System.arraycopy(l.get(i), 0, r, i * 3, 3);
        }
        return r;
    }

    private static List<int[]> vloer(WolkTerrein.Eiland e) {
        List<int[]> uit = new ArrayList<>();
        for (int dx = -e.r; dx <= e.r; dx++) {
            for (int dz = -e.r; dz <= e.r; dz++) {
                if (e.is(dx, dz)) {
                    uit.add(new int[]{e.x + dx, 2 * (e.boven(e.x + dx, e.z + dz) + 1), e.z + dz});
                }
            }
        }
        return uit;
    }

    private static boolean ergens(WolkRoute.Blokken b, List<int[]> plekken) {
        for (int[] p : plekken) {
            if (b.bereikt(p[0], p[1], p[2])) {
                return true;
            }
        }
        return false;
    }

    /**
     * THE proof: in a few Wolkenweides of two seeds, walk the jump rule over the model's blocks (ground, islands, stepping
     * stones, clouds, lift columns) from the meadow. Every island of every stack must be reached, the highest included,
     * and from every island there must be a way back to the meadow.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void bioWereldWolkRoutes(GameTestHelper helper) {
        int stapels = 0, eilanden = 0, bereikt = 0, topBereikt = 0, terug = 0, los = 0, losMetWeg = 0, losBereikt = 0, hoogste = 0, liften = 0, stromen = 0, stappen = 0,
                metWater = 0;
        int[] niveaus = new int[8], tel = new int[4];
        int cellen = 0, leeg = 0, straal = 0;
        String eerste = "";
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            for (int[] plek : weiden(m, 2)) {
                for (int cx = Math.floorDiv(plek[0] - 180, WolkTerrein.STAPEL_CEL); cx <= Math.floorDiv(plek[0] + 180, WolkTerrein.STAPEL_CEL); cx++) {
                    for (int cz = Math.floorDiv(plek[1] - 180, WolkTerrein.STAPEL_CEL); cz <= Math.floorDiv(plek[1] + 180, WolkTerrein.STAPEL_CEL); cz++) {
                        WolkTerrein.Stapel s = WolkTerrein.stapel(m, cx, cz);
                        if (m.eWeide(s.x, s.z) >= WolkTerrein.BINNEN) {
                            cellen++;
                            leeg += s.eilanden.isEmpty() ? 1 : 0;
                            straal += s.straal;
                        }
                    }
                }
                for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, plek[0] - 180, plek[1] - 180, plek[0] + 180, plek[1] + 180)) {
                    WolkTerrein.Eiland top = s.hoogste();
                    if (top.rots) {
                        continue;
                    }
                    List<int[]> grond = new ArrayList<>();
                    WolkRoute.Blokken b = blokken(m, s, grond, s.los ? null : tel);
                    List<WolkRoute.Lift> lf = liften(m, s);
                    b.loop(plat(grond), lf);
                    if (s.los) {
                        los++;
                        if (top.trap != null || top.lift != null) {
                            losMetWeg++;
                            losBereikt += ergens(b, vloer(top)) ? 1 : 0;
                        }
                        continue;
                    }
                    stapels++;
                    niveaus[Math.min(7, s.eilanden.size())]++;
                    stappen += s.stappen.size();
                    metWater += s.vallen.isEmpty() ? 0 : 1;
                    for (WolkTerrein.Kolom k : s.kolommen) {
                        liften += k.omlaag() ? 0 : 1;
                        stromen += k.omlaag() ? 1 : 0;
                    }
                    hoogste = Math.max(hoogste, top.top - WolkTerrein.grond(m, top.x, top.z));
                    for (WolkTerrein.Eiland e : s.eilanden) {
                        eilanden++;
                        boolean er = ergens(b, vloer(e));
                        bereikt += er ? 1 : 0;
                        if (!er && eerste.isEmpty()) {
                            eerste = "seed " + seed + ": island " + e.x + " " + e.top + " " + e.z + " (level " + e.niveau + ") of the stack at " + s.x + " " + s.z;
                        }
                    }
                    topBereikt += ergens(b, vloer(top)) ? 1 : 0;
                    boolean allen = true;
                    for (WolkTerrein.Eiland e : s.eilanden) {
                        b.loop(plat(vloer(e)), lf);
                        boolean beneden = ergens(b, grond);
                        allen &= beneden;
                        if (!beneden && eerste.isEmpty()) {
                            eerste = "seed " + seed + ": no way down from " + e.x + " " + e.top + " " + e.z + " (island " + s.eilanden.indexOf(e) + " of " + s.eilanden.size()
                                    + ", level " + e.niveau + ", stack at " + s.x + " " + s.z + "); from it reached:";
                            for (WolkTerrein.Eiland o : s.eilanden) {
                                eerste += " [" + s.eilanden.indexOf(o) + ": " + o.x + " " + o.top + " " + o.z + " r" + o.r + " v" + o.vorm + (ergens(b, vloer(o)) ? " yes]" : " no]");
                            }
                            for (WolkTerrein.Kolom k : s.kolommen) {
                                eerste += " {" + (k.omlaag() ? "down " : "up ") + k.x() + " " + k.voet() + ".." + k.boven() + " " + k.z() + "}";
                            }
                        }
                    }
                    terug += allen ? 1 : 0;
                }
            }
        }
        LOGGER.info("biomes3 Wolkenweide routes: {} stacks (by islands 1..7: {} {} {} {} {} {} {}), islands reached {} of {}, highest island reached in {} stacks, "
                        + "a way back down from every island in {}; highest top {} above the meadow; {} lifts, {} streams, {} stepping stones, {} stacks with a waterfall; loose islands {} "
                        + "({} with a way up, {} of those reached); in the stacks' boxes: cloud blocks {}, slabs {}, stairs {}, water {}; stack cells in the "
                        + "region {} ({} empty, mean radius {}). {}", stapels, niveaus[1],
                niveaus[2], niveaus[3], niveaus[4], niveaus[5], niveaus[6], niveaus[7], bereikt, eilanden, topBereikt, terug, hoogste, liften, stromen, stappen, metWater,
                los, losMetWeg, losBereikt, tel[0], tel[1], tel[2], tel[3], cellen, leeg, straal / Math.max(1, cellen - leeg), eerste);
        helper.assertTrue(stapels >= 8, "stacks found: " + stapels);
        helper.assertTrue(bereikt == eilanden, "every island of every stack is reached from the meadow by the jump rule: " + bereikt + " of " + eilanden + "; " + eerste);
        helper.assertTrue(terug == stapels, "from every island of every stack there is a way back down to the meadow: " + terug + " of " + stapels + "; " + eerste);
        helper.assertTrue(hoogste >= 60 && hoogste <= WolkTerrein.LAAG + WolkTerrein.HOOG, "the highest island is high: " + hoogste);
        helper.assertTrue(liften >= 4 && stromen >= 3 && stappen >= 40, "lifts " + liften + ", streams " + stromen + ", stepping stones " + stappen);
        helper.assertTrue(niveaus[3] + niveaus[4] + niveaus[5] + niveaus[6] + niveaus[7] >= stapels / 2, "most stacks have three islands or more");
        helper.assertTrue(losMetWeg == 0 || losBereikt * 10 >= losMetWeg * 8, "loose islands with a way up are reached: " + losBereikt + " of " + losMetWeg);
        helper.assertTrue(tel[0] > 500 && tel[1] > 50 && tel[2] > 5, "clouds with slabs and stairs: " + tel[0] + " / " + tel[1] + " / " + tel[2]);
        helper.assertTrue(metWater >= 1 && tel[3] > 10, "stacks with a waterfall: " + metWater);
        helper.succeed();
    }

    /**
     * Shapes and water of the model: every size class and shape, the arch of a double island, hills; ponds are walled in,
     * a fall starts beside a pond and ends on water, and the meadow's pond never lies next to lower ground.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldWolkVormenEnWater(GameTestHelper helper) {
        int[] klasse = new int[4];
        boolean[] vorm = new boolean[5];
        int boog = 0, heuvel = 0, vijvers = 0, lek = 0, vallen = 0, valFout = 0, plassen = 0, plasLek = 0, bomen = 0, punten = 0, laagste = 999, hoogste = 0;
        String eerste = "";
        for (long seed : SEEDS) {
            BioModel m = model(helper, seed);
            for (int[] plek : weiden(m, 3)) {
                for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, plek[0] - 220, plek[1] - 220, plek[0] + 220, plek[1] + 220)) {
                    for (WolkTerrein.Eiland e : s.eilanden) {
                        klasse[e.klasse]++;
                        vorm[e.vorm] = true;
                        bomen += e.bomen.size();
                        punten += e.punten.length / 2;
                        if (!e.rots) {
                            int h = e.top - WolkTerrein.grond(m, e.x, e.z);
                            laagste = Math.min(laagste, h);
                            hoogste = Math.max(hoogste, h);
                        }
                        boolean b = false, hv = false;
                        for (int dx = -e.r; dx <= e.r; dx++) {
                            for (int dz = -e.r; dz <= e.r; dz++) {
                                if (!e.is(dx, dz)) {
                                    continue;
                                }
                                int wx = e.x + dx, wz = e.z + dz;
                                b |= e.vorm == WolkTerrein.DUBBEL && e.top - e.onder(wx, wz) <= 1 && e.is(dx + 1, dz) && e.is(dx - 1, dz) && e.is(dx, dz + 1) && e.is(dx, dz - 1);
                                hv |= e.boven(wx, wz) > e.top;
                                if (e.vijver(wx, wz)) {
                                    vijvers++;
                                    for (Direction d : Direction.Plane.HORIZONTAL) {
                                        int nx = wx + d.getStepX(), nz = wz + d.getStepZ();
                                        if (!e.is(nx - e.x, nz - e.z) && !s.valt(nx, nz)) {
                                            lek++;
                                            eerste = eerste.isEmpty() ? "pond leak at " + wx + " " + e.top + " " + wz : eerste;
                                        }
                                    }
                                }
                            }
                        }
                        boog += b ? 1 : 0;
                        heuvel += hv ? 1 : 0;
                    }
                    for (WolkTerrein.Val v : s.vallen) {
                        vallen++;
                        // beside a pond column at its top, and it ends on water (a catch pool or the meadow's pond)
                        boolean bron = false, eind = false;
                        for (WolkTerrein.Eiland e : s.eilanden) {
                            for (Direction d : Direction.Plane.HORIZONTAL) {
                                bron |= e.top == v.boven() && e.vijver(v.x() + d.getStepX(), v.z() + d.getStepZ());
                            }
                            eind |= e.top == v.onder() - 1 && e.vijver(v.x(), v.z());
                        }
                        eind |= s.plas != null && s.plas.water == v.onder() - 1 && s.plas.diepte(v.x(), v.z()) > 0;
                        if (!bron || !eind || v.boven() - v.onder() < 3) {
                            valFout++;
                            eerste = eerste.isEmpty() ? "fall at " + v.x() + " " + v.z() + " from " + v.boven() + " to " + v.onder() + ": source " + bron + ", end " + eind : eerste;
                        }
                    }
                    if (s.plas != null) {
                        plassen++;
                        for (int i = 0; i < s.plas.kolommen.length; i += 3) {
                            int x = s.plas.kolommen[i], z = s.plas.kolommen[i + 1];
                            if (m.water(x, z) != s.plas.water || m.hoogte(x, z) >= s.plas.water) {
                                plasLek++;
                                eerste = eerste.isEmpty() ? "meadow pond column " + x + " " + z + " is not water in the map" : eerste;
                            }
                            for (Direction d : Direction.Plane.HORIZONTAL) {
                                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                                if (m.water(nx, nz) == Kaart.GEEN && (m.meng(nx, nz) < 1f || m.hoogte(nx, nz) < s.plas.water)) {
                                    plasLek++;
                                    eerste = eerste.isEmpty() ? "meadow pond leak at " + x + " " + z : eerste;
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("biomes3 Wolkenweide shapes: rocks {}, small {}, medium {}, large {}; arches {}, hills {}; island tops {}..{} above the meadow; tree spots {}, "
                        + "crystal tips {}; pond columns {} (leaks {}), falls {} (wrong {}), meadow ponds {} (leaks {}). {}", klasse[0], klasse[1], klasse[2], klasse[3],
                boog, heuvel, laagste, hoogste, bomen, punten, vijvers, lek, vallen, valFout, plassen, plasLek, eerste);
        helper.assertTrue(klasse[0] >= 10 && klasse[1] >= 20 && klasse[2] >= 8 && klasse[3] >= 1 && klasse[3] * 6 < klasse[1] + klasse[2],
                "size classes: rocks, small, medium now and then, large rarely");
        helper.assertTrue(vorm[0] && vorm[1] && vorm[2] && vorm[3] && vorm[4], "every shape occurs");
        helper.assertTrue(boog >= 1 && heuvel >= 1, "a double island with an arch, an island with a hill: " + boog + " / " + heuvel);
        helper.assertTrue(laagste <= 13 && hoogste >= 70, "from just above the meadow to very high: " + laagste + ".." + hoogste);
        helper.assertTrue(lek == 0 && valFout == 0 && plasLek == 0, "water stays where it belongs: " + eerste);
        helper.assertTrue(vallen >= 2 && vijvers >= 6 && plassen >= 1, "ponds, falls and a meadow pond exist: " + vijvers + " / " + vallen + " / " + plassen);
        helper.assertTrue(bomen >= 20 && punten >= 40, "trees and crystal tips: " + bomen + " / " + punten);
        helper.succeed();
    }
}
