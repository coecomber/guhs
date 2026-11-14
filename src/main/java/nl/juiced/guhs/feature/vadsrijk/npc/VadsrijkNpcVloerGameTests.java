package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 npc-vloer ("in zone 2 I see NPCs in the floor and under the ground"):
 * <ol>
 *   <li>every NPC spot of zones 1-3 resolves ({@link NpcVloer#staanPlek}, the very code the keeper uses) to a standable cell
 *   (solid floor, feet + head free) that is not a sealed air pocket inside the hollowed terrain, on the MAP TILES themselves
 *   (the compact pack the jar ships, read block by block through {@link TegelVloer});</li>
 *   <li>a guh stuck in a wall goes up to the first free floor, a nep-speler spawned inside a slope stands on it, and the
 *   bedrock under the map counts as "under the map".</li>
 * </ol>
 */
public class VadsrijkNpcVloerGameTests {
    private static final String B = "vadsrijk_npc_vloer";
    private static final String PAKKET = "nl.juiced.guhs.feature.vadsrijk.";

    @GuhTest(template = "empty", batch = B, timeoutTicks = 2400)
    public static void elkeNpcPlekStaatOpDeKaart(GameTestHelper helper) {
        for (String c : List.of("zone2.npc.Z2Npcs", "zone3.npc.Z3Npcs")) {
            try {
                Class.forName(PAKKET + c, true, VadsrijkNpcVloerGameTests.class.getClassLoader());
            } catch (Throwable t) {
                // (the zone may be gone from this tree)
            }
        }
        TegelVloer w = new TegelVloer(helper.getLevel());
        List<String> fout = new ArrayList<>();
        int[] perZone = new int[4];
        for (NpcDef d : Npcs.alle()) {
            BlockPos p = d.plek();
            int zone = VadsrijkZones.inZone1(p.getX(), p.getZ()) ? 1 : VadsrijkZones.inZone2(p.getX(), p.getZ()) ? 2
                    : VadsrijkZones.inZone3(p.getX(), p.getZ()) ? 3 : 0;
            if (zone == 0) {
                continue;
            }
            if (!w.heeftTegel(p)) {
                fout.add("z" + zone + " " + d.id() + " " + p.toShortString() + ": no map tile here");
                continue;
            }
            perZone[zone]++;
            BlockPos s = NpcVloer.staanPlek(w, p);
            String waarom = !NpcVloer.staan(w, s) ? "no standable cell (feet " + w.naam(s) + ", head " + w.naam(s.above()) + ", below " + w.naam(s.below()) + ")"
                    : w.afgesloten(s, 20000) ? "stands in a sealed air pocket inside the terrain (under the ground)"
                    : Math.abs(s.getY() - p.getY()) > 12 ? "floor " + (s.getY() - p.getY()) + " blocks away" : null;
            if (waarom != null) {
                fout.add("z" + zone + " " + d.id() + " spot " + p.toShortString() + " -> " + s.toShortString() + ": " + waarom);
            }
        }
        org.slf4j.LoggerFactory.getLogger("guhs/npcvloer").info("npc spots checked on the tiles: zone1 {}, zone2 {}, zone3 {}; problems {}",
                perZone[1], perZone[2], perZone[3], fout.size());
        for (String f : fout) {
            org.slf4j.LoggerFactory.getLogger("guhs/npcvloer").warn("NPC-VLOER {}", f);
        }
        // self-check: the hollow inside of the Galmhellingen slope where the nep-speler TryHardTim stood live (feet in the 1-high
        // pocket at y 140 between the snow faces 139 and 141) counts as a sealed pocket
        BlockPos tim = new BlockPos(932, 140, -1177);
        helper.assertTrue(!w.heeftTegel(tim) || w.afgesloten(tim, 20000), "the slope's hollow inside at " + tim.toShortString() + " is a sealed pocket");
        helper.assertTrue(perZone[1] > 20 && perZone[2] > 40 && perZone[3] > 30, "the NPCs of zones 1-3 were checked: " + perZone[1] + "/" + perZone[2] + "/" + perZone[3]);
        helper.assertTrue(fout.isEmpty(), fout.size() + " NPC spots end in the floor / under the ground: " + String.join(" | ", fout));
        helper.succeed();
    }

    /** A guh in a wall goes up to the first free floor; the bedrock under the map is "under the map"; a blocked spot goes up. */
    @GuhTest(template = "empty", batch = B)
    public static void vastInDeMuurGaatOmhoog(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos voet = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int dy = -1; dy <= 2; dy++) {
            sl.setBlockAndUpdate(voet.above(dy), Blocks.STONE.defaultBlockState());   // a pillar: floor y-1, rock at y..y+2
        }
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        helper.assertTrue(g != null, "guh created");
        g.snapTo(voet.getX() + 0.5, voet.getY(), voet.getZ() + 0.5, 0, 0);
        sl.addFreshEntity(g);
        helper.assertTrue(NpcVloer.vast(g), "a guh with its feet in the rock is stuck");
        helper.assertTrue(NpcVloer.bevrijd(g), "it is moved");
        helper.assertTrue(g.getY() >= voet.getY() + 3 && !NpcVloer.vast(g) && NpcVloer.staan(NpcVloer.van(sl), g.blockPosition()),
                "out of the rock onto a free floor: y " + g.getY() + " (pillar top " + (voet.getY() + 2) + ")");
        helper.assertTrue(!NpcVloer.bevrijd(g), "a free guh stays where it is");
        // the spot rule: a blocked spot without a lit floor nearby goes UP, never stays in the rock
        NpcVloer.Wereld w = new NpcVloer.Wereld() {
            @Override
            public boolean botst(BlockPos p) {
                return !sl.getBlockState(p).getCollisionShape(sl, p).isEmpty();
            }

            @Override
            public boolean licht(BlockPos p) {
                return false;   // (night under a roof: the old rule found nothing and left the NPC inside its spot)
            }
        };
        BlockPos s = NpcVloer.staanPlek(w, voet);
        helper.assertTrue(s.getX() == voet.getX() && s.getZ() == voet.getZ() && s.getY() >= voet.getY() + 3 && NpcVloer.staan(w, s),
                "blocked spot -> first free floor above: " + s.toShortString());
        // the keeper's own path (Npcs.staanPlek on the live level) never returns a cell inside a block
        BlockPos s2 = Npcs.staanPlek(sl, voet);
        helper.assertTrue(!NpcVloer.geblokt(NpcVloer.van(sl), s2), "Npcs.staanPlek is free: " + s2.toShortString());
        // under the map
        g.snapTo(g.getX(), sl.getMinY() + 1, g.getZ());
        helper.assertTrue(NpcVloer.onderDeKaart(g), "on the bedrock under the map = under the ground");
        g.discard();
        helper.succeed();
    }

    /** A nep-speler spawned inside a slope stands on it, and follows onto free ground only. */
    @GuhTest(template = "empty", batch = B)
    public static void nepSpelerNietInDeHelling(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos voet = helper.absolutePos(new BlockPos(3, 2, 3));
        for (int dy = -1; dy <= 1; dy++) {
            sl.setBlockAndUpdate(voet.above(dy), Blocks.DIRT.defaultBlockState());
        }
        NepSpeler n = NepSpelers.spawn(sl, Vec3.atBottomCenterOf(voet), NepRol.HELER, 10, "xX_Vloertest_Xx", null);
        try {
            helper.assertTrue(!NpcVloer.vast(n) && !NpcVloer.geblokt(NpcVloer.van(sl), n.blockPosition()), "not spawned inside the slope: " + n.position());
            Vec3 speler = Vec3.atBottomCenterOf(voet.above(2));
            Vec3 v = NpcVloer.vrijBij(sl, speler, Vec3.atBottomCenterOf(voet));
            helper.assertTrue(v.equals(speler) || !NpcVloer.geblokt(NpcVloer.van(sl), BlockPos.containing(v)), "a follower never lands in the slope: " + v);
        } finally {
            NepSpelers.weg(n);
            n.discard();
        }
        helper.succeed();
    }
}
