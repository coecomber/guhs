package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.gametest.GuhTest;

/** 1.9 "Een opknapper!": every exported shape is valid and stamps cleanly (every level, palette and rotation). */
public class VormenGameTests {
    private static final String BATCH = "erfenis_vormen";
    private static final List<String> IDS = List.of("hoeve", "opslagschuur", "timmerschuur", "kippenhok", "schaapskooi", "konijnenren",
            "eendenvijver", "koeienstal", "bijenhof", "mammoetwei", "akker", "boomgaard", "molen", "bakkerij", "weverij", "kaasmakerij",
            "kaarsenmakerij", "bosje", "houtzagerij", "groeve", "steenbakkerij", "zandkuil", "glasblazerij", "bouwblokkenwerkplaats",
            "guhhutje", "marktplein", "speeltuin", "guhpension", "detectivebureau", "dorpscafe", "muziektent", "loket", "vadsmast");
    private static final Map<String, int[]> MATEN = Map.ofEntries(Map.entry("hoeve", new int[] {11, 11}), Map.entry("opslagschuur", new int[] {7, 7}),
            Map.entry("timmerschuur", new int[] {6, 6}), Map.entry("kippenhok", new int[] {8, 10}), Map.entry("schaapskooi", new int[] {10, 12}),
            Map.entry("konijnenren", new int[] {8, 8}), Map.entry("eendenvijver", new int[] {10, 10}), Map.entry("koeienstal", new int[] {12, 12}),
            Map.entry("bijenhof", new int[] {8, 8}), Map.entry("mammoetwei", new int[] {16, 16}), Map.entry("akker", new int[] {8, 8}),
            Map.entry("boomgaard", new int[] {10, 10}), Map.entry("molen", new int[] {7, 7}), Map.entry("bakkerij", new int[] {7, 7}),
            Map.entry("weverij", new int[] {7, 7}), Map.entry("kaasmakerij", new int[] {7, 7}), Map.entry("kaarsenmakerij", new int[] {6, 6}),
            Map.entry("bosje", new int[] {10, 10}), Map.entry("houtzagerij", new int[] {7, 7}), Map.entry("groeve", new int[] {10, 10}),
            Map.entry("steenbakkerij", new int[] {7, 7}), Map.entry("zandkuil", new int[] {8, 8}), Map.entry("glasblazerij", new int[] {7, 7}),
            Map.entry("bouwblokkenwerkplaats", new int[] {9, 9}), Map.entry("guhhutje", new int[] {5, 5}), Map.entry("marktplein", new int[] {9, 9}),
            Map.entry("speeltuin", new int[] {8, 8}), Map.entry("guhpension", new int[] {8, 8}), Map.entry("detectivebureau", new int[] {6, 6}),
            Map.entry("dorpscafe", new int[] {8, 8}), Map.entry("muziektent", new int[] {9, 9}), Map.entry("loket", new int[] {5, 5}),
            Map.entry("vadsmast", new int[] {4, 4}));

    /** All 33 contract buildings have shapes for all their levels, with the contract's footprints; every block state is known. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void alleVormenGeldig(GameTestHelper helper) {
        helper.assertTrue(GebouwVormen.gebouwen().containsAll(IDS), "all contract buildings have shapes: " + GebouwVormen.gebouwen());
        for (String id : IDS) {
            int[] m = GebouwVormen.maat(id);
            helper.assertTrue(m[0] == MATEN.get(id)[0] && m[1] == MATEN.get(id)[1], id + ": footprint " + m[0] + "x" + m[1]);
            int n = GebouwVormen.levels(id);
            helper.assertTrue(n == (id.equals("mammoetwei") ? 3 : 10), id + ": levels " + n);
            int vorige = 0;
            for (int l = 1; l <= n; l++) {
                VormBestand v = GebouwVormen.vorm(id, l);
                helper.assertTrue(v != null && v.w == m[0] && v.d == m[1], id + " L" + l + ": shape present with its footprint");
                int t = GebouwVormen.vormTier(id, l);
                helper.assertTrue(t >= 1 && t <= 5 && t >= vorige, id + " L" + l + ": shape tier " + t + " (never down)");
                vorige = t;
                for (int p = 1; p <= 5; p++) {
                    v.states(p);
                }
            }
        }
        for (String d : GebouwVormen.decoIds()) {
            VormBestand v = VormBestand.laad("deco/" + d);
            int[] m = GebouwVormen.decoMaat(d);
            helper.assertTrue(v != null && v.w == m[0] && v.d == m[1] && m[0] <= 3 && m[1] <= 3, "deco " + d + ": shape and size");
            v.states(1);
        }
        helper.assertTrue(GebouwVormen.decoIds().size() >= 55, "the deco catalogue (" + GebouwVormen.decoIds().size() + ")");
        for (int s = 0; s < 6; s++) {
            VormBestand v = VormBestand.laad("plukje/" + s);
            helper.assertTrue(v != null && v.w == 8 && v.d == 8, "plukje " + s);
            v.states(1);
        }
        helper.assertTrue(GebouwVormen.padSoorten().size() == 9, "9 path kinds: " + GebouwVormen.padSoorten());
        Map<String, String> fout = new TreeMap<>(VormBestand.ONBEKEND);
        helper.assertTrue(fout.isEmpty(), "unknown block states/creatures: " + fout);
        helper.succeed();
    }

    /**
     * Stamps every building at every level in every palette and rotation (one building level per tick, all 20 combinations)
     * on one spot high above the test: nothing outside its footprint changes (a ring of marker wool stays), its creatures
     * stand inside it, and a clear leaves grass and no creatures behind.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 2400)
    public static void stempelAlles(GameTestHelper helper) {
        ServerLevel lvl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 60, 2));
        List<int[]> klussen = new ArrayList<>();
        for (int i = 0; i < IDS.size(); i++) {
            for (int l = 1; l <= GebouwVormen.levels(IDS.get(i)); l++) {
                klussen.add(new int[] {i, l});
            }
        }
        int[] stap = {0};
        long[] tijd = {0};
        BlockState wol = Blocks.RED_WOOL.defaultBlockState();
        helper.onEachTick(() -> {
            if (stap[0] >= klussen.size()) {
                return;
            }
            int[] k = klussen.get(stap[0]++);
            String id = IDS.get(k[0]);
            int[] m = GebouwVormen.maat(id);
            long t0 = System.nanoTime();
            for (int rot = 0; rot < 4; rot++) {
                int bx = (rot & 1) == 0 ? m[0] : m[1], bz = (rot & 1) == 0 ? m[1] : m[0];
                ring(lvl, o, bx, bz, wol);
                for (int p = 1; p <= 5; p++) {
                    GebouwVormen.stempel(lvl, id, k[1], p, o, rot);
                    GebouwVormen.klaarNu();
                    String w = waar(lvl, o, bx, bz, wol);
                    if (w != null) {
                        helper.fail(id + " L" + k[1] + " palet " + p + " rot " + rot + ": " + w);
                    }
                    BlockState grond = lvl.getBlockState(o);
                    if (grond.isAir()) {
                        helper.fail(id + " L" + k[1] + " palet " + p + " rot " + rot + ": no ground at the corner");
                    }
                }
                GebouwVormen.wis(lvl, o, bx, bz);
                GebouwVormen.klaarNu();
                ring(lvl, o, bx, bz, Blocks.AIR.defaultBlockState());
                if (!lvl.getBlockState(o.offset(bx / 2, 1, bz / 2)).isAir() || !lvl.getBlockState(o.offset(bx - 1, 0, bz - 1)).is(Blocks.GRASS_BLOCK)
                        || !lvl.getEntities((Entity) null, new AABB(o).inflate(40), DecoWezens::isDeco).isEmpty()) {
                    helper.fail(id + " L" + k[1] + " rot " + rot + ": clearing left something behind");
                }
            }
            tijd[0] += System.nanoTime() - t0;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stap[0] >= klussen.size(), "stamping " + stap[0] + "/" + klussen.size());
            org.slf4j.LoggerFactory.getLogger("guhs").info("Erfenis-vormen: {} stamps in {} ms", klussen.size() * 20, tijd[0] / 1_000_000);
        });
    }

    /** A ring of blocks just around a footprint, y 0..3. */
    private static void ring(ServerLevel lvl, BlockPos o, int bx, int bz, BlockState s) {
        for (int x = -1; x <= bx; x++) {
            for (int z = -1; z <= bz; z++) {
                if (x >= 0 && x < bx && z >= 0 && z < bz) {
                    continue;
                }
                for (int y = 0; y <= 3; y++) {
                    lvl.setBlock(o.offset(x, y, z), s, 2 | 16);
                }
            }
        }
    }

    /** What went wrong around a stamp (null = fine): the ring changed, or a creature stands outside the footprint. */
    private static String waar(ServerLevel lvl, BlockPos o, int bx, int bz, BlockState wol) {
        for (int x = -1; x <= bx; x++) {
            for (int z = -1; z <= bz; z++) {
                if (x >= 0 && x < bx && z >= 0 && z < bz) {
                    continue;
                }
                for (int y = 0; y <= 3; y++) {
                    if (lvl.getBlockState(o.offset(x, y, z)) != wol) {
                        return "block outside the footprint changed at " + x + "," + y + "," + z;
                    }
                }
            }
        }
        AABB box = new AABB(o.getX(), o.getY() - 1, o.getZ(), o.getX() + bx, o.getY() + GebouwVormen.WIS_HOOGTE, o.getZ() + bz);
        for (Entity e : lvl.getEntities((Entity) null, box.inflate(30), DecoWezens::isDeco)) {
            if (!box.contains(e.position())) {
                return "creature " + e.getType() + " outside at " + e.position();
            }
        }
        return null;
    }
}
