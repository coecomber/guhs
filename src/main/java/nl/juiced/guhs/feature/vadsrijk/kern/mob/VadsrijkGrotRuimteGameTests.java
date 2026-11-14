package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.1 (bug: in the Druipgrotten lots of mobs spawned just outside the cave, right above or below it): a small cave in a
 * rock block: a tunnel, a sealed air pocket in the rock above it (the map's hollowed rock) and the open hill top. The old
 * column search of a cave spawner finds the pocket first; with {@link GrotRuimte} every mob stands in the tunnel, and a mob
 * left in the pocket counts as "verdwaald" (removed by the spawner tick).
 */
public class VadsrijkGrotRuimteGameTests {
    private static final String B = "vadsrijk_grotruimte";

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200, skyAccess = true)
    public static void grotMobsAlleenInDeGrot(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        // rock 15 x 14 x 15
        for (int x = 0; x <= 14; x++) {
            for (int y = 0; y <= 13; y++) {
                for (int z = 0; z <= 14; z++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        // the tunnel: floor at y 1 (stand y 2), 3 high, 3 wide (closed ends: a sealed test cave)
        for (int x = 1; x <= 13; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 6; z <= 8; z++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // a sealed pocket in the rock above it (stand y 7)
        for (int x = 3; x <= 11; x++) {
            for (int y = 7; y <= 8; y++) {
                for (int z = 5; z <= 9; z++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos c = o.offset(7, 3, 7);
        Spawners.Spawner s = new Spawners.Spawner("test:grotruimte#proef", c, 4, KernRegistry.PROEFKLODDER, 3, 20, 1, 2, Element.GEEN, 0)
                .grot(o.getY() + 1, o.getY() + 9);
        GrotRuimte.vergeet();
        // the old rule: the first standing spot of the column is the pocket above the tunnel
        int oud = Spawners.vloer(sl, c.getX(), c.getZ(), s.minY(), s.maxY());
        helper.assertTrue(oud == o.getY() + 7, "the test cave reproduces the bug (old column search finds the pocket): " + (oud - o.getY()));
        GrotRuimte.Ruimte r = GrotRuimte.van(sl, s);
        helper.assertTrue(r != null && r.bevat(c) && r.bevat(o.offset(1, 2, 7)), "the tunnel is the spawner's space");
        helper.assertTrue(!r.bevat(o.offset(7, 7, 7)) && !r.bevat(o.offset(7, 14, 7)), "the pocket and the hill top are not");
        List<VadsrijkMob> mobs = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            VadsrijkMob m = Spawners.spawn(sl, s);
            if (m != null) {
                mobs.add(m);
            }
        }
        helper.assertTrue(mobs.size() >= 10, "the cave spawner still spawns: " + mobs.size());
        for (VadsrijkMob m : mobs) {
            BlockPos b = m.blockPosition();
            int dy = b.getY() - o.getY(), dz = b.getZ() - o.getZ();
            helper.assertTrue(dy == 2 && dz >= 6 && dz <= 8, "every mob stands on the tunnel floor, not in the rock: dy " + dy + " dz " + dz);
            helper.assertTrue(!GrotRuimte.verdwaald(s, r, b), "a tunnel mob is not 'verdwaald'");
            m.discard();
        }
        helper.assertTrue(GrotRuimte.verdwaald(s, r, o.offset(7, 7, 7)), "a mob in the pocket is 'verdwaald' (the spawner removes it)");
        // a cave spawner marked "onder dak": never a spot under the open sky
        GrotRuimte.ONDERDAK.add("test:grotruimte_dak");
        Spawners.Spawner dak = new Spawners.Spawner("test:grotruimte_dak#proef", c, 4, KernRegistry.PROEFKLODDER, 3, 20, 1, 2, Element.GEEN, 0)
                .grot(o.getY() + 1, o.getY() + 9);
        BlockPos gat = o.offset(7, 2, 7);
        for (int y = 5; y <= 13; y++) {
            sl.setBlock(o.offset(7, y, 7), Blocks.AIR.defaultBlockState(), 2);   // a shaft to the sky above one tunnel cell
        }
        GrotRuimte.vergeet();
        GrotRuimte.Ruimte rd = GrotRuimte.van(sl, dak);
        helper.assertTrue(rd != null, "the roofed spawner has a space");
        helper.assertTrue(GrotRuimte.magStaan(sl, dak, rd, o.offset(2, 2, 7)), "elsewhere in the tunnel yes");
        helper.assertTrue(GrotRuimte.openHemel(sl, gat) && !GrotRuimte.magStaan(sl, dak, rd, gat), "under the shaft (open sky) no");
        GrotRuimte.ONDERDAK.remove("test:grotruimte_dak");
        GrotRuimte.vergeet();
        helper.succeed();
    }

    /** The druipgrotten spawners follow the generator's tunnels: their centres are in the tunnel/knot air range (no rock). */
    @GuhTest(template = "empty", batch = B)
    public static void druipgrottenSpawnersInDeGang(GameTestHelper helper) {
        int n = 0;
        for (Spawners.Spawner s : Spawners.alle()) {
            if (!s.id().startsWith("wezens:druipgrotten_gang_") && !s.id().startsWith("wezens:druipgrotten_knoop_")) {
                continue;
            }
            n++;
            helper.assertTrue(s.grot() && s.maxY() - s.minY() <= 5, s.id() + ": a narrow height window around the floor (" + s.minY() + ".." + s.maxY() + ")");
            helper.assertTrue(GrotRuimte.onderdak(s.id()), s.id() + ": marked as a real (roofed) cave");
            helper.assertTrue(s.centrum().getY() >= s.minY() && s.centrum().getY() <= s.maxY(), s.id() + ": its centre is inside its window");
        }
        helper.assertTrue(n >= 20, "the druipgrotten tunnel and knot spawners exist: " + n);
        // the knot tunnels end at the knot floor + 2 (bouwsels.py), not the knot centre
        for (var g : nl.juiced.guhs.feature.vadsrijk.wezens.WezensPlekken.gangen()) {
            int[] eind = g.punt(1.0);
            boolean opKnoop = false;
            for (int[] k : nl.juiced.guhs.feature.vadsrijk.wezens.WezensPlekken.KNOPEN) {
                if (Math.abs(eind[0] - k[0]) <= 1 && Math.abs(eind[2] - k[2]) <= 1) {
                    opKnoop = eind[1] == nl.juiced.guhs.feature.vadsrijk.wezens.WezensPlekken.knoopVoet(k) + 2;
                }
            }
            helper.assertTrue(opKnoop, "a tunnel ends at a knot's floor + 2: " + eind[0] + "," + eind[1] + "," + eind[2]);
        }
        helper.succeed();
    }

    @SuppressWarnings("unused")
    private static final Set<String> NIETS = Set.of();
}
