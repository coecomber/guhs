package nl.juiced.guhs.feature.vadsrijk.indeling;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-WERELD: the world layout W-3 (zone offsets, zone lookup, generated POIs at their new place, barrier free areas, sea floor).
 * Run: {@code runGameTestServer -Pgt=Indeling}.
 */
public class IndelingGameTests {
    private static final String BATCH = "vadsrijk_indeling";

    @GuhTest(template = "empty", batch = BATCH)
    public static void zonesOpHunPlek(GameTestHelper helper) {
        // the offsets of the approved layout
        helper.assertTrue(VadsrijkZones.KAMP_DX == -384 && VadsrijkZones.KAMP_DZ == 0, "camp offset");
        helper.assertTrue(VadsrijkZones.Z2_DX == 704 && VadsrijkZones.Z2_DZ == 1344, "zone-2 offset");
        helper.assertTrue(VadsrijkZones.Z3_DX == 1404 && VadsrijkZones.Z3_DZ == -576, "zone-3 trunk");
        // the arrival and the camp area moved with the camp
        helper.assertTrue(Gebieden.AANKOMST.equals(new BlockPos(-384, 65, 1130)), "arrival " + Gebieden.AANKOMST);
        helper.assertTrue(VadsrijkZones.zoneVan(Gebieden.AANKOMST.getX(), Gebieden.AANKOMST.getZ()) == VadsrijkZones.Zone.KAMP, "arrival in camp");
        var kamp = Gebieden.van("knuffelkamp");
        helper.assertTrue(kamp != null && kamp.bevat(Gebieden.AANKOMST.getX() + 0.5, Gebieden.AANKOMST.getZ() - 20.5), "camp area holds the camp");
        // generated POIs are in their zone's tile box
        BlockPos plein = WereldPlekken.plek("plein");
        helper.assertTrue(VadsrijkZones.zoneVan(plein.getX(), plein.getZ()) == VadsrijkZones.Zone.Z1, "Zwamzoet in zone 1");
        BlockPos kvuur = WereldPlekken.plek("kampvuur_kamp");
        helper.assertTrue(VadsrijkZones.inKamp(kvuur.getX(), kvuur.getZ()), "camp campfire in the camp box " + kvuur);
        BlockPos kk = Z2WereldPlekken.plek("plein");
        helper.assertTrue(VadsrijkZones.inZone2(kk.getX(), kk.getZ()), "Kandijkom in zone 2 " + kk);
        helper.assertTrue(Math.abs(kk.getX() - 704) < 40 && Math.abs(kk.getZ() + 1068) < 40, "Kandijkom near (704, -1068): " + kk);
        BlockPos dok3 = Z3WereldPlekken.DOK_LANDING;
        helper.assertTrue(VadsrijkZones.inZone3(dok3.getX(), dok3.getZ()), "Honingtak dock in zone 3");
        helper.assertTrue(!VadsrijkZones.inZone3(kk.getX(), kk.getZ()) && !Z3WereldPlekken.inZone3(990, -1000), "zone 3 box no longer covers zone 2");
        // every zone-2 POI (and its areas) lies in the zone-2 box
        for (var e : Z2WereldPlekken.ALLE.entrySet()) {
            BlockPos b = e.getValue();
            helper.assertTrue(VadsrijkZones.inZone2(b.getX(), b.getZ()) || b.getY() <= 0, "zone-2 POI outside the box: " + e.getKey() + " " + b);
        }
        // the shared harbour ids exist
        for (String id : new String[] {"haven_z1_z2_vertrek", "haven_z2_z1_vertrek", "haven_z2_z3_vertrek", "haven_z3_z2_vertrek", "haven_z3_z4_vertrek"}) {
            BlockPos b = IndelingRegistry.plek(id);
            helper.assertTrue(b != null, id + " missing");
        }
        // zone 5 over zone 4
        helper.assertTrue(VadsrijkZones.zoneVan(2500, 200, -520) == VadsrijkZones.Zone.Z5 && VadsrijkZones.zoneVan(2500, 64, -520) == VadsrijkZones.Zone.Z4,
                "zone 5 above zone 4");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void barrieres(GameTestHelper helper) {
        // free: the middle of every zone
        helper.assertTrue(Barrieres.vrij(0, 64, 0), "Zwamzoet free");
        helper.assertTrue(Barrieres.vrij(-384, 87, 1030), "camp free");
        helper.assertTrue(Barrieres.vrij(704, 91, -1060), "Kandijkom free");
        helper.assertTrue(Barrieres.vrij(1404, 166, -576), "Honingtak free");
        helper.assertTrue(Barrieres.vrij(1655, 160, -827), "the werf free");
        helper.assertTrue(Barrieres.vrij(2100, 63, -480), "zone 4 free");
        helper.assertTrue(Barrieres.vrij(2740, 260, -520), "Hoogvads free");
        helper.assertTrue(Barrieres.vrij(520, 63, 720), "the turtle island free");
        BlockPos nk = IndelingRegistry.plek("haven_z1_z2_vertrek");
        helper.assertTrue(Barrieres.vrij(nk.getX(), nk.getY(), nk.getZ()), "Noordklifhaven platform free " + nk);
        // not free: the barriers between the zones
        helper.assertTrue(!Barrieres.vrij(500, 63, -720), "Splinterwand gap");
        helper.assertTrue(!Barrieres.vrij(1066, 63, -700), "Wolkenkloof channel");
        helper.assertTrue(!Barrieres.vrij(1735, 63, -480), "Wolkenval");
        helper.assertTrue(!Barrieres.vrij(-380, 63, 760), "Zoete-Zeestroom strait");
        helper.assertTrue(!Barrieres.vrij(2700, 64, -520), "under Hoogvads (zone 5 starts at y 150)");
        helper.assertTrue(Barrieres.vrij(3096, 64, 3096), "the proeftuin is never pushed");
        helper.assertTrue("wand".equals(Barrieres.soort(500, 63, -720, true)) && "kloof".equals(Barrieres.soort(1066, 63, -700, true))
                && "wind".equals(Barrieres.soort(500, 160, -720, false)), "barrier kinds");
        helper.assertTrue(Barrieres.dichtste(500, 63, -680).zone().equals("z1"), "pushed back to zone 1 from the south side of the wall");
        helper.succeed();
    }

    /**
     * 1.10.2: the turtle island (zone-1 secret geheim_schildpad 524,76,722, south-east of zone 1) is reachable by swimming or by
     * boat: a flood over the barrier's free cells (sea level, 2-block grid) from Zwamzoet reaches the secret, and the old gap
     * (the strip of "current" between the coast and the island free area) is free now. The real map edge further out still pushes.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void schildpadEilandBereikbaar(GameTestHelper helper) {
        BlockPos geheim = WereldPlekken.plek("geheim_schildpad");
        helper.assertTrue(geheim != null && Barrieres.vrij(geheim.getX(), 63, geheim.getZ()), "the secret itself is free");
        // the old gap between the coast (z1) and the island area (z1_schildpad)
        for (int[] g : new int[][] {{520, 610}, {480, 620}, {450, 630}, {500, 640}, {540, 625}}) {
            helper.assertTrue(Barrieres.vrij(g[0], 63, g[1]), "the water between the coast and the turtle island is free: " + g[0] + "," + g[1]);
        }
        // flood on a 2-block grid inside the box x -100..700, z -100..900 from Zwamzoet's square
        int x0 = -100, z0 = -100, w = 400, h = 500, st = 2;
        boolean[] gezien = new boolean[w * h];
        java.util.ArrayDeque<int[]> rij = new java.util.ArrayDeque<>();
        rij.add(new int[] {(0 - x0) / st, (0 - z0) / st});
        boolean bereikt = false;
        int gx = (geheim.getX() - x0) / st, gz = (geheim.getZ() - z0) / st;
        while (!rij.isEmpty()) {
            int[] c = rij.poll();
            if (c[0] < 0 || c[1] < 0 || c[0] >= w || c[1] >= h || gezien[c[1] * w + c[0]]) {
                continue;
            }
            gezien[c[1] * w + c[0]] = true;
            if (!Barrieres.vrij(x0 + c[0] * st, 63, z0 + c[1] * st)) {
                continue;
            }
            if (Math.abs(c[0] - gx) <= 1 && Math.abs(c[1] - gz) <= 1) {
                bereikt = true;
                break;
            }
            rij.add(new int[] {c[0] + 1, c[1]});
            rij.add(new int[] {c[0] - 1, c[1]});
            rij.add(new int[] {c[0], c[1] + 1});
            rij.add(new int[] {c[0], c[1] - 1});
        }
        helper.assertTrue(bereikt, "from Zwamzoet over free water to the turtle island secret");
        // the real map edge beyond the island and the strait to the camp keep pushing
        helper.assertTrue(!Barrieres.vrij(680, 63, 720) && !Barrieres.vrij(520, 63, 880), "the open sea past the turtle island still pushes");
        helper.assertTrue(!Barrieres.vrij(-380, 63, 760), "Zoete-Zeestroom strait (camp / zone 1) unchanged");
        helper.assertTrue(ZonePoortZone.zone1(geheim), "the turtle island counts as zone 1 for the zone gate");
        helper.succeed();
    }

    /** (the zone gate's own lookup, kept here so the test reads as a sentence) */
    private static final class ZonePoortZone {
        static boolean zone1(BlockPos b) {
            return nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.zoneOp(b.getX(), 63, b.getZ()) == 1
                    && nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.zoneOp(520, 63, 615) == 1;
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void zeeVulling(GameTestHelper helper) {
        helper.assertTrue(ZeeVulling.inZee(1200, -200) && !ZeeVulling.inZee(3100, 3100), "sea area");
        int diep = ZeeVulling.vloer(-900, 0);
        helper.assertTrue(diep <= 51 && diep >= 47, "open sea deep: " + diep);
        // U-OVERGANG: the Java sea must equal tools/vadsrijk/zeenaad.py exactly (the seam pass pulls the tiles onto it);
        // reference values printed by zeenaad.py
        int[][] ref = {{-900, 0, 51, 0}, {700, -1500, 50, 0}, {1100, -1100, 50, 0}, {1000, -900, 49, 1}, {1780, -500, 48, 0},
                {-1200, -1800, 51, 0}, {2900, -100, 49, 0}};
        for (int[] r : ref) {
            helper.assertTrue(ZeeVulling.vloer(r[0], r[1]) == r[2] && ZeeVulling.materiaal(r[0], r[1]) == r[3],
                    "zeenaad floor at " + r[0] + "," + r[1] + ": " + ZeeVulling.vloer(r[0], r[1]) + "/" + ZeeVulling.materiaal(r[0], r[1]));
        }
        helper.assertTrue(ZeeVulling.wolkDikte(1001, -737) == 3 && ZeeVulling.wolkSoort(1001, -737) == 2 && ZeeVulling.wolkDikte(-900, 0) == 0,
                "zone-3 cloud banks continue past the box: " + ZeeVulling.wolkDikte(1001, -737));
        helper.assertTrue(ZeeVulling.rots(905, -1540) == 66 && ZeeVulling.rots(168, -1279) == 51, "rock stacks: " + ZeeVulling.rots(905, -1540));
        helper.assertTrue(ZeeVulling.zeeOnderTegel(new net.minecraft.world.level.ChunkPos(3000 >> 4, -520 >> 4))
                && !ZeeVulling.zeeOnderTegel(new net.minecraft.world.level.ChunkPos(2100 >> 4, -520 >> 4)), "sea under zone-5-only tiles");
        helper.succeed();
    }
}
