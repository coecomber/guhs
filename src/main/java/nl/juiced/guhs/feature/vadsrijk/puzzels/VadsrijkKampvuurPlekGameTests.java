package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcVloer;
import nl.juiced.guhs.feature.vadsrijk.npc.TegelVloer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 npc-vloer ("the campfire in zone 2 around 604 100 -986 is inaccessible or doesn't even exist"): (1) every campfire of
 * zones 1-3 exists on the map tiles (a campfire block within 1 block of its listed spot) with a standable, lit cell next to it;
 * (2) a puzzle stamp never covers a campfire (z2_ijsschuif was stamped over kampvuur_knuffelmeer_1).
 */
public class VadsrijkKampvuurPlekGameTests {
    private static final String B = "vadsrijk_npc_vloer";

    @GuhTest(template = "empty", batch = B, timeoutTicks = 1200)
    public static void elkKampvuurBestaatEnIsBereikbaar(GameTestHelper helper) {
        Map<String, List<BlockPos>> lijsten = new LinkedHashMap<>();
        lijsten.put("z1", nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.KAMPVUREN);
        lijsten.put("z2", nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.KAMPVUREN);
        lijsten.put("z3", nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.KAMPVUREN);
        TegelVloer w = new TegelVloer(helper.getLevel());
        List<String> fout = new ArrayList<>();
        int n = 0;
        for (var e : lijsten.entrySet()) {
            for (BlockPos p : e.getValue()) {
                n++;
                BlockPos vuur = null;
                for (BlockPos x : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                    if (w.staat(x).getBlock() instanceof CampfireBlock) {
                        vuur = x.immutable();
                        break;
                    }
                }
                if (vuur == null) {
                    fout.add(e.getKey() + " " + p.toShortString() + ": no campfire block (" + w.naam(p) + ")");
                    continue;
                }
                boolean staan = false;
                for (BlockPos x : BlockPos.betweenClosed(vuur.offset(-2, -2, -2), vuur.offset(2, 2, 2))) {
                    if (!x.equals(vuur) && NpcVloer.staan(w, x) && !w.afgesloten(x, 20000)) {
                        staan = true;
                        break;
                    }
                }
                if (!staan) {
                    fout.add(e.getKey() + " " + vuur.toShortString() + ": nowhere to stand next to it");
                }
            }
        }
        org.slf4j.LoggerFactory.getLogger("guhs/npcvloer").info("campfires zones 1-3 checked on the tiles: {}, problems {}: {}", n, fout.size(), fout);
        helper.assertTrue(n > 30, "the campfire lists were read: " + n);
        helper.assertTrue(fout.isEmpty(), fout.size() + " campfires missing / unreachable: " + String.join(" | ", fout));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 400)
    public static void puzzelNooitOverEenKampvuur(GameTestHelper helper) {
        ServerLevel l = helper.getLevel();
        PuzzelDef d = Puzzels.def("z2_ijsschuif");
        helper.assertTrue(d != null, "the slide puzzle of the Knuffelmeer exists");
        BlockPos h = helper.absolutePos(BlockPos.ZERO);
        BlockPos anker = new BlockPos(h.getX(), 240, h.getZ());
        int r = PuzzelStempel.ZOEK + 12;
        try {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    l.setBlock(anker.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
            l.setBlock(anker, Blocks.CAMPFIRE.defaultBlockState(), 2);
            BlockPos o = PuzzelStempel.zoek(l, d, anker);
            int m = PuzzelStempel.BESCHERM_RAND;
            boolean raakt = anker.getX() >= o.getX() - m && anker.getX() < o.getX() + d.breed() + m && anker.getZ() >= o.getZ() - m
                    && anker.getZ() < o.getZ() + d.diep() + m;
            helper.assertTrue(!raakt, "the stamp keeps " + m + " blocks from the campfire: origin " + o.toShortString() + ", campfire " + anker.toShortString());
        } finally {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    l.setBlock(anker.offset(x, -1, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            l.setBlock(anker, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }
}
