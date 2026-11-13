package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.9.8: the block checker of the Erfenis shapes. Every building, every level, every palette (T1..T5) is stamped for real and
 * each cell is compared with what the shape asks for:
 * <ul>
 *   <li>every palette entry is a block state the game knows (no unknown id or property: that would place air);</li>
 *   <li>every block placed is the block the shape asks for (nothing silently became air or something else);</li>
 *   <li>the ground layer is ground: no thin layer there (the T3 path, a carpet, left sunken holes in the Marktplein);</li>
 *   <li>every block can stay where it is (a flower on planks, a door half without its other half... would pop off at the
 *       first block update: "random missing blocks");</li>
 *   <li>no enclosure traps you: from every walkable cell at ground level you get out of the footprint, through gates and doors
 *       (they open in the Erfenis since 1.9.8) or over a block you can jump on (the Akker's fence trapped you).</li>
 * </ul>
 */
public class VormCheckGameTests {
    private static final String BATCH = "erfenis_vormcheck";

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 2400)
    public static void erfenisVormenPlaatsenAlles(GameTestHelper helper) {
        ServerLevel lvl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 60, 2));
        List<String[]> klussen = new ArrayList<>();
        for (String id : GebouwVormen.gebouwen()) {
            for (int l = 1; l <= GebouwVormen.levels(id); l++) {
                klussen.add(new String[] {id, Integer.toString(l)});
            }
        }
        Map<String, String> fouten = new TreeMap<>(), zacht = new TreeMap<>();
        int[] stap = {0};
        boolean[] gelogd = {false, false};
        helper.onEachTick(() -> {
            if (stap[0] >= klussen.size()) {
                return;
            }
            String[] k = klussen.get(stap[0]++);
            String id = k[0];
            int level = Integer.parseInt(k[1]);
            VormBestand v = GebouwVormen.vorm(id, level);
            for (int p = 1; p <= 5; p++) {
                GebouwVormen.stempel(lvl, id, level, p, o, 0);
                GebouwVormen.klaarNu();
                controleer(lvl, v, p, o, id + " L" + level + " T" + p, fouten, zacht);
            }
            GebouwVormen.wis(lvl, o, v.w, v.d);
            GebouwVormen.klaarNu();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stap[0] >= klussen.size(), "checking " + stap[0] + "/" + klussen.size());
            if (!gelogd[1]) {
                // the soft findings (fragile decoration, closed-in huts): a report, not a failure (the Erfenis never sends block
                // updates through its buildings; the Akker's own way out is checked hard in erfenisAkkerUitgang)
                gelogd[1] = true;
                StringBuilder z = new StringBuilder(zacht.size() + " soft findings:");
                zacht.forEach((k, v) -> z.append("\n  ").append(k).append(": ").append(v));
                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of("erfenis-vormcheck-zacht.txt"), z.toString());
                } catch (java.io.IOException e) {
                    org.slf4j.LoggerFactory.getLogger("guhs").warn("Erfenis-vormcheck: {}", z);
                }
                org.slf4j.LoggerFactory.getLogger("guhs").info("Erfenis-vormcheck: {} hard problems, {} soft findings", fouten.size(), zacht.size());
            }
            Map<String, String> onbekend = new TreeMap<>(VormBestand.ONBEKEND);
            helper.assertTrue(onbekend.isEmpty(), "unknown block states: " + onbekend);
            if (!fouten.isEmpty()) {
                StringBuilder sb = new StringBuilder(fouten.size() + " problems:");
                fouten.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
                if (!gelogd[0]) {
                    gelogd[0] = true;
                    try {
                        java.nio.file.Files.writeString(java.nio.file.Path.of("erfenis-vormcheck.txt"), sb.toString());
                    } catch (java.io.IOException e) {
                        org.slf4j.LoggerFactory.getLogger("guhs").error("Erfenis-vormcheck: {}", sb);
                    }
                }
                helper.fail(sb.substring(0, Math.min(sb.length(), 3000)));
            }
        });
    }

    /** Checks one stamped shape (rotation 0) against its palette; problems go into fouten (key: where). */
    static void controleer(ServerLevel lvl, VormBestand v, int palet, BlockPos o, String naam, Map<String, String> fouten,
                           Map<String, String> zacht) {
        GebouwVormen.Klus klus = GebouwVormen.Klus.vorm(lvl, v, palet, o, 0, true);
        for (int x = 0; x < v.w; x++) {
            for (int z = 0; z < v.d; z++) {
                for (int y = Math.min(-1, v.ymin); y <= v.ymax; y++) {
                    BlockPos p = o.offset(x, y, z);
                    BlockState wil = klus.blok(x, y, z);
                    BlockState is = lvl.getBlockState(p);
                    String waar = naam + " @" + x + "," + y + "," + z;
                    if (wil == null) {
                        continue;
                    }
                    if (!wil.isAir() && is.isAir()) {
                        fouten.put(waar, "placed as air instead of " + wil);
                    } else if (is.getBlock() != wil.getBlock()) {
                        fouten.put(waar, "placed " + is + " instead of " + wil);
                    } else if (y == 0 && !grond(lvl, p, is)) {
                        fouten.put(waar, "no ground in the ground layer: " + is);
                    } else if (!is.isAir() && !is.canSurvive(lvl, p)) {
                        zacht.put(waar, "cannot stay there (pops off at a block update): " + is);
                    }
                }
            }
        }
        String op = opgesloten(lvl, o, v.w, v.d);
        if (op != null) {
            zacht.put(naam + " enclosure", op);
        }
    }

    /** A ground block: water, or something you stand on at least half a block high. */
    private static boolean grond(ServerLevel lvl, BlockPos p, BlockState s) {
        if (!s.getFluidState().isEmpty()) {
            return true;
        }
        VoxelShape vorm = s.getCollisionShape(lvl, p);
        return !vorm.isEmpty() && vorm.max(net.minecraft.core.Direction.Axis.Y) >= 0.5;
    }

    private static double top(ServerLevel lvl, BlockPos p) {
        VoxelShape vorm = lvl.getBlockState(p).getCollisionShape(lvl, p);
        return vorm.isEmpty() ? 0 : vorm.max(net.minecraft.core.Direction.Axis.Y);
    }

    /**
     * The walking test at ground level (null = fine, else the first trapped cell). A cell is walkable when you can stand in
     * it at y 1 (low stuff at y 1, nothing at y 2) or it is a gate/door; a block you can jump on (top at most 1, free above
     * it) is walkable too, and from there you get over a fence next to it. Outside the footprint everything is free.
     */
    static String opgesloten(ServerLevel lvl, BlockPos o, int w, int d) {
        int[][] soort = new int[w][d];   // 0 blocked, 1 walk, 2 jump-on block, 3 fence-like (crossable from a jump-on block)
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                BlockPos p1 = o.offset(x, 1, z), p2 = o.offset(x, 2, z), p3 = o.offset(x, 3, z);
                double t1 = top(lvl, p1), t2 = top(lvl, p2), t3 = top(lvl, p3);
                if (Erfenis.magOpen(lvl.getBlockState(p1))) {
                    soort[x][z] = 1;
                } else if (t1 <= 0.6 && t2 == 0) {
                    soort[x][z] = 1;
                } else if (t1 <= 1.0 && t2 == 0 && t3 == 0) {
                    soort[x][z] = 2;
                } else if (t1 > 1.0 && t1 <= 1.5 && t2 <= 0.5 && t3 == 0) {
                    soort[x][z] = 3;
                }
            }
        }
        int[][] buren = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int sx = 0; sx < w; sx++) {
            for (int sz = 0; sz < d; sz++) {
                if (soort[sx][sz] != 1) {
                    continue;
                }
                // from this cell, walk until you stand on the border of the footprint (then one more step is outside)
                boolean[][] gezien = new boolean[w][d];
                Deque<int[]> rij = new ArrayDeque<>();
                rij.add(new int[] {sx, sz});
                gezien[sx][sz] = true;
                boolean uit = false;
                while (!rij.isEmpty() && !uit) {
                    int[] c = rij.poll();
                    if (c[0] == 0 || c[1] == 0 || c[0] == w - 1 || c[1] == d - 1) {
                        uit = true;
                        break;
                    }
                    int van = soort[c[0]][c[1]];
                    for (int[] b : buren) {
                        int nx = c[0] + b[0], nz = c[1] + b[1];
                        int naar = soort[nx][nz];
                        // a fence only from (or along) a block you jumped on
                        if (gezien[nx][nz] || naar == 0 || naar == 3 && van != 2 && van != 3) {
                            continue;
                        }
                        gezien[nx][nz] = true;
                        rij.add(new int[] {nx, nz});
                    }
                }
                if (!uit) {
                    return "walkable cell " + sx + "," + sz + " can't get out (" + lvl.getBlockState(o.offset(sx, 1, sz)) + ")";
                }
            }
        }
        return null;
    }

    /** The Akker of every level keeps a way out: its gate opens in the Erfenis, and from every cell inside you reach it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void erfenisAkkerUitgang(GameTestHelper helper) {
        ServerLevel lvl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 30, 2));
        for (int l = 1; l <= GebouwVormen.levels("akker"); l++) {
            for (int p = 1; p <= 5; p++) {
                GebouwVormen.stempel(lvl, "akker", l, p, o, 0);
                GebouwVormen.klaarNu();
                String op = opgesloten(lvl, o, 8, 8);
                helper.assertTrue(op == null, "akker L" + l + " T" + p + ": " + op);
            }
        }
        // the gate of the fenced Akker opens by hand in the Erfenis
        VormBestand v = GebouwVormen.vorm("akker", 4);
        for (int p = 1; p <= 5; p++) {
            boolean poort = false;
            for (BlockState s : v.states(p)) {
                if (s.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                    poort = true;
                    helper.assertTrue(Erfenis.magOpen(s), "T" + p + ": the gate opens: " + s);
                }
            }
            helper.assertTrue(poort, "T" + p + ": the fenced Akker has a gate");
        }
        helper.assertTrue(Erfenis.magOpen(Blocks.SPRUCE_DOOR.defaultBlockState()) && !Erfenis.magOpen(Blocks.IRON_DOOR.defaultBlockState())
                && !Erfenis.magOpen(Blocks.OAK_TRAPDOOR.defaultBlockState()) && !Erfenis.magOpen(Blocks.CHEST.defaultBlockState()),
                "doors open, iron doors, trapdoors and chests don't");
        GebouwVormen.wis(lvl, o, 8, 8);
        GebouwVormen.klaarNu();
        helper.succeed();
    }

    /** The T3 path (a carpet) in a ground layer: it lies on a ground block now, one higher, and no hole stays behind. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void erfenisT3PadGeenGat(GameTestHelper helper) {
        ServerLevel lvl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 30, 2));
        VormBestand v = GebouwVormen.vorm("marktplein", 10);
        BlockState[] t3 = v.states(3);
        int px = -1, pz = -1;
        for (int x = 0; x < v.w && px < 0; x++) {
            for (int z = 0; z < v.d; z++) {
                if (GebouwVormen.dun(t3[v.at(x, 0, z)]) && v.at(x, 1, z) == 0) {
                    px = x;
                    pz = z;
                    break;
                }
            }
        }
        helper.assertTrue(px >= 0, "the Marktplein T3 has its pebble path in the ground layer");
        GebouwVormen.stempel(lvl, "marktplein", 10, 3, o, 0);
        GebouwVormen.klaarNu();
        helper.assertTrue(lvl.getBlockState(o.offset(px, 0, pz)) == GebouwVormen.ONDER_DUN, "ground under the path");
        helper.assertTrue(GebouwVormen.dun(lvl.getBlockState(o.offset(px, 1, pz))), "the path on top of it");
        GebouwVormen.wis(lvl, o, v.w, v.d);
        GebouwVormen.klaarNu();
        helper.succeed();
    }

    /**
     * The Erfenis bakery's baker (a decoration, tag guhs_erfenis_deco) and a Sherlock witness of the same kind never put up
     * Bakker Korstje's top-3 board; the real Korstje (no tag) still does.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void erfenisBakkerZonderScorebord(GameTestHelper helper) {
        ServerLevel lvl = helper.getLevel();
        nl.juiced.guhs.entity.GuhNpcEntity[] npcs = new nl.juiced.guhs.entity.GuhNpcEntity[3];
        String[] tags = {DecoWezens.TAG, nl.juiced.guhs.feature.erfenis.sherlock.Cast.TAG, null};
        for (int i = 0; i < 3; i++) {
            var npc = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(lvl, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            npc.setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.BAKKERGUH);
            if (tags[i] != null) {
                npc.addTag(tags[i]);
            }
            npc.setNoAi(true);
            BlockPos p = helper.absolutePos(new BlockPos(2 + i * 8, 2, 2));
            npc.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
            lvl.addFreshEntity(npc);
            npcs[i] = npc;
        }
        helper.assertTrue(npcs[0].alleenDecor() && npcs[1].alleenDecor() && !npcs[2].alleenDecor(), "decor or not");
        helper.runAfterDelay(150, () -> {
            for (int i = 0; i < 3; i++) {
                var n = npcs[i];
                int borden = lvl.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                        n.getBoundingBox().inflate(3), e -> e.entityTags().contains(nl.juiced.guhs.quest.Scorebord.TAG)).size();
                helper.assertTrue(i == 2 ? borden == 1 : borden == 0, (tags[i] == null ? "the real Korstje" : tags[i]) + ": " + borden + " boards");
            }
            for (var e : lvl.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npcs[2].getBoundingBox().inflate(5))) {
                e.discard();
            }
            for (var n : npcs) {
                n.discard();
            }
            helper.succeed();
        });
    }
}
