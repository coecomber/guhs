package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-4 bazen: the Zeeslurf (fights, sinks into its grotto and is immune until the 3 polyps break, then +25 %, cheered up pink at
 * the end), Kootje (fights, hangs under his kite: immune until the 3 winches are wound in, then stunned and +50 %; the plushie
 * rain phase), the loot/pets/arenas. Run: {@code runGameTestServer -Pgt=VadsrijkZ4Bazen}. Each test its own batch.
 */
public class VadsrijkZ4BazenGameTests {
    private static final String BATCH = "vadsrijk_z4_bazen";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void laad(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
    }

    private static <T extends Z4Baas> T zet(ServerLevel sl, EntityType<T> type, Vec3 c, int lv) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.snapTo(c.x, c.y, c.z, 0, 0);
        b.stelIn(lv, null, false, Set.of());
        sl.addFreshEntity(b);
        return b;
    }

    /** De Grauwe Zeeslurf in a small pool: it fights, fase 2 = in its grotto + immune until the 3 polyps break, cheered up pink. */
    @GuhTest(template = "empty", batch = BATCH + "_zeeslurf", timeoutTicks = 300)
    public static void zeeslurfGrotjeEnOpvrolijken(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        laad(sl, c, 30);
        int diep = 12;
        for (int x = -14; x <= 14; x++) {
            for (int z = -14; z <= 14; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.SAND.defaultBlockState(), 2);
                for (int y = 0; y < diep; y++) {
                    sl.setBlock(c.offset(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        Zeeslurf z = zet(sl, Z4BazenRegistry.ZEESLURF.get(), Vec3.atBottomCenterOf(c).add(0, 2, 0), 54);
        z.zetThuis(Vec3.atBottomCenterOf(c));
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-6, diep, 0));
        z.testFilter = q -> q == p;
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(z.strijders().contains(p) && z.aanvallenGedaan >= 1, "it fights (" + z.laatsteAanval + ")");
            helper.assertTrue(z.isInWater() || z.getY() <= z.zee(), "it stays in the water");
            Statussen.geef(z, Status.VERDOOFD, 400, 1, null);
            z.krijgSchade(z.vads() - z.maxVads() * 0.65, p);
            helper.assertTrue(z.fase() == 2 && z.inGrotje(), "fase 2: into its grotto");
            double v = z.vads();
            z.krijgSchade(1000, p);
            helper.assertTrue(z.vads() == v, "immune in its grotto");
            List<Z3Doel> poliepen = sl.getEntitiesOfClass(Z3Doel.class, new AABB(c).inflate(20), d -> d.getType() == Z4BazenRegistry.POLIEP.get());
            helper.assertTrue(poliepen.size() == 3, "3 polyps on the lagoon floor, got " + poliepen.size());
            for (Z3Doel d : poliepen) {
                d.krijgSchade(d.vads() + 1, p);
            }
            helper.assertTrue(z.polypenKapot == 3 && !z.inGrotje(), "all 3 polyps broken: it comes up");
            v = z.vads();
            z.krijgSchade(100, p);
            helper.assertTrue(v - z.vads() > 110, "gasping: +25 % damage (" + (v - z.vads()) + ")");
            z.krijgSchade(z.vads() + 10, p);
            helper.assertTrue(z.geplofd() && z.blij(), "cheered up: pink and happy");
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!z.isRemoved(), "it stays a while, singing");
                z.discard();
                helper.succeed();
            });
        });
    }

    /** Kootje: fights, hangs under his kite (immune) until the 3 winches are wound in (3 clicks each), then stunned + 50 %. */
    @GuhTest(template = "empty", batch = BATCH + "_vliegervanger", timeoutTicks = 300)
    public static void vliegervangerLieren(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        laad(sl, c, 20);
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            }
        }
        Vliegervanger g = zet(sl, Z4BazenRegistry.VLIEGERVANGER.get(), Vec3.atBottomCenterOf(c), 56);
        g.zetThuis(Vec3.atBottomCenterOf(c));
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-8, 0, 0));
        g.testFilter = q -> q == p;
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(g.strijders().contains(p) && g.aanvallenGedaan >= 1, "he fights (" + g.laatsteAanval + ")");
            g.krijgSchade(g.vads() - g.maxVads() * 0.59, p);
            helper.assertTrue(g.fase() == 2 && g.vliegt(), "fase 2: he hangs under his war kite");
            double v = g.vads();
            g.krijgSchade(1000, p);
            helper.assertTrue(g.vads() == v, "immune while he hangs up there");
            List<Z3Doel> lieren = sl.getEntitiesOfClass(Z3Doel.class, new AABB(c).inflate(20), d -> d.getType() == Z4BazenRegistry.LIER.get());
            helper.assertTrue(lieren.size() == 3, "3 winches on the deck, got " + lieren.size());
            for (Z3Doel d : lieren) {
                for (int i = 0; i < 3; i++) {
                    g.doelGebruikt(d, p);
                }
            }
            helper.assertTrue(g.lierenBinnen == 3 && !g.vliegt(), "all 3 wound in: the kite crashes");
            v = g.vads();
            g.krijgSchade(100, p);
            helper.assertTrue(v - g.vads() > 140, "crashed: +50 % damage (" + (v - g.vads()) + ")");
            g.krijgSchade(g.vads() - g.maxVads() * 0.24, p);
            helper.assertTrue(g.fase() == 3, "fase 3: the plushie rain");
            g.krijgSchade(g.vads() + 10, p);
            helper.assertTrue(g.geplofd(), "beaten: he flees in smoke");
            helper.succeed();
        });
    }

    /** Arenas, pets, named items, trophies and a first-kill chest. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootPetsArenas(GameTestHelper helper) {
        helper.assertTrue(Arenas.van(Z4BazenRegistry.ARENA_ZEESLURF) != null && Arenas.van(Z4BazenRegistry.ARENA_WRAK) != null, "both arenas registered");
        helper.assertTrue(Huisdieren.alle().stream().anyMatch(h -> h.id().equals("zeeslurfje")) && Huisdieren.alle().stream().anyMatch(h -> h.id().equals("vliegertje")),
                "both pets registered");
        for (Z4BazenLoot.Benoemd b : Z4BazenLoot.BENOEMD) {
            helper.assertTrue(BasisTypes.van(b.type()) != null, "base type " + b.type());
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        for (String baas : Z4BazenLoot.BAZEN) {
            LootTabel.Resultaat r = Z4BazenLoot.kist(baas, p, 55, p.getRandom());
            helper.assertTrue(r.items().size() >= 3, baas + ": first chest = sure piece + a named Legendary + the trophy, got " + r.items().size());
            helper.assertTrue(r.items().stream().anyMatch(s -> ItemStack.isSameItem(s, Z4BazenLoot.trofee(baas))), baas + ": trophy on the first kill");
        }
        helper.succeed();
    }
}
