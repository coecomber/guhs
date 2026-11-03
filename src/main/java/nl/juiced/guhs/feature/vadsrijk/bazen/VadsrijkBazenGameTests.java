package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.ElementKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.KoningKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.SplitRegel;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.LichtzwamBlok;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Opperzwam;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk bazen: only the risky logic. Koning Klodder's split/heal rule (pure and on the entity), the Opperzwam's phase
 * transitions (dark -> 4 lights -> ceiling breaks open -> everything restored), and contribution -> personal chest with bound loot.
 * Run: {@code runGameTestServer -Pgt=VadsrijkBazen}.
 */
public class VadsrijkBazenGameTests {
    private static final String BATCH = "vadsrijk_bazen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void splitRegel(GameTestHelper helper) {
        SplitRegel r = new SplitRegel(100);
        helper.assertTrue(r.uitslag(100 + SplitRegel.MAX - 1) == SplitRegel.Uitslag.BEZIG, "nothing beaten yet: still going");
        helper.assertTrue(r.uitslag(100 + SplitRegel.MAX + 1) == SplitRegel.Uitslag.MISLUKT && r.overlevers() == 3, "nobody beaten in time: all 3 melt back");
        r = new SplitRegel(0);
        r.verslagen(0, 50);
        r.verslagen(1, 50 + SplitRegel.VENSTER - 10);
        helper.assertTrue(r.uitslag(50 + SplitRegel.VENSTER) == SplitRegel.Uitslag.BEZIG, "inside the window");
        r.verslagen(2, 50 + SplitRegel.VENSTER + 5);
        helper.assertTrue(!r.weg(2) && r.uitslag(50 + SplitRegel.VENSTER + 5) == SplitRegel.Uitslag.MISLUKT && r.overlevers() == 1,
                "too late for the third: it survives, the split failed");
        r = new SplitRegel(0);
        r.verslagen(2, 10);
        r.verslagen(2, 11);
        r.verslagen(0, 100);
        r.verslagen(1, 10 + SplitRegel.VENSTER);
        helper.assertTrue(r.uitslag(10 + SplitRegel.VENSTER) == SplitRegel.Uitslag.GELUKT, "all three inside the window (exactly at its end)");
        helper.succeed();
    }

    private static KoningKlodder koning(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        KoningKlodder k = BazenRegistry.KONING_KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
        BlockPos p = helper.absolutePos(new BlockPos(1, 1, 1));
        k.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        k.stelIn(8, Element.WATER, false, Set.of());
        sl.addFreshEntity(k);
        return k;
    }

    private static List<ElementKlodder> klodders(GameTestHelper helper, KoningKlodder k) {
        return helper.getLevel().getEntitiesOfClass(ElementKlodder.class, k.getBoundingBox().inflate(16), x -> !x.isRemoved() && !x.geplofd());
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void koningSplitstEnHeelt(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        KoningKlodder k = koning(helper);
        double max = k.maxVads();
        k.krijgSchade(max * 0.41, p);
        helper.assertTrue(k.fase() == 2 && k.gesplitst(), "at 59 % he splits (fase 2)");
        List<ElementKlodder> l = klodders(helper, k);
        helper.assertTrue(l.size() == 3, "three Kroonklodders, got " + l.size());
        double voor = k.vads();
        k.krijgSchade(1000, p);
        helper.assertTrue(k.vads() == voor, "while split the king can't be hurt");
        // one cheered up, then the window runs out
        ElementKlodder eerste = l.get(0);
        eerste.krijgSchade(eerste.vads() + 1, p);
        long nu = sl.getGameTime();
        k.splitControle(nu + SplitRegel.VENSTER + 2);
        helper.assertTrue(!k.gesplitst(), "the window closed: he re-forms");
        helper.assertTrue(k.vads() > voor + max * 0.15, "2 survivors melt back: he healed 16 % (" + voor + " -> " + k.vads() + ")");
        helper.assertTrue(klodders(helper, k).isEmpty(), "the survivors are gone (melted back)");
        helper.assertTrue(k.fase() == 2, "still fase 2");
        // split again and do it right this time
        k.splits(sl);
        l = klodders(helper, k);
        helper.assertTrue(l.size() == 3, "split again");
        for (ElementKlodder e : l) {
            e.krijgSchade(e.vads() + 1, p);
        }
        k.splitControle(sl.getGameTime());
        helper.assertTrue(!k.gesplitst() && k.fase() == 3, "all three in time: whole again, fase 3");
        helper.assertTrue(Math.abs(k.vads() - max * 0.35) < max * 0.01, "he's at 35 % (" + k.vads() / max + ")");
        helper.assertTrue(Statussen.heeft(k, Status.VERDOOFD), "and dizzy for a moment");
        k.discard();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void opperzwamFasen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Opperzwam o = BazenRegistry.OPPERZWAM.get().create(sl, EntitySpawnReason.COMMAND);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        o.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        o.stelIn(19, Element.NATUUR, false, Set.of());
        sl.addFreshEntity(o);
        BlockPos plafond = at.above(14);
        helper.runAfterDelay(2, () -> {
            sl.setBlock(plafond, Blocks.STONE.defaultBlockState(), 3);
            double max = o.maxVads();
            o.krijgSchade(max * 0.36, p);
            helper.assertTrue(o.fase() == 2 && o.donker(), "fase 2: dark");
            List<BlockPos> licht = o.lichtPlekken();
            helper.assertTrue(licht.stream().allMatch(b -> sl.getBlockState(b).is(BazenRegistry.LICHTZWAM.get()) && !LichtzwamBlok.aan(sl, b)),
                    "4 unlit Lichtzwammen placed");
            double v = o.vads();
            o.krijgSchade(400, p);
            helper.assertTrue(Math.abs((v - o.vads()) - 100) < 1, "in the dark he takes 25 % damage");
            for (int i = 0; i < 4; i++) {
                LichtzwamBlok.zet(sl, licht.get(i), true);
                o.aangestoken(sl, licht.get(i));
                helper.assertTrue(o.donker() == (i < 3), "dark until the 4th light (" + i + ")");
            }
            helper.assertTrue(Statussen.heeft(o, Status.VERDOOFD), "blinded by the light");
            Statussen.wis(o, Status.VERDOOFD);
            o.krijgSchade(o.vads() - max * 0.3, p);
            helper.assertTrue(o.fase() == 3, "fase 3");
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(sl.getBlockState(plafond).isAir() && Herstel.heeft(sl, Opperzwam.H_PLAFOND), "the ceiling broke open (remembered)");
            helper.assertTrue(o.getScale() > 1.9, "he became gigantic (" + o.getScale() + ")");
            Opperzwam.herstel(sl);
            helper.assertTrue(sl.getBlockState(plafond).is(Blocks.STONE), "the ceiling is back");
            helper.assertTrue(o.lichtPlekken().stream().allMatch(b -> sl.getBlockState(b).isAir()), "the Lichtzwammen are gone again (it was air)");
            o.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bijdrageGeeftKist(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer veel = GuhMockPlayer.of(helper), weinig = GuhMockPlayer.of(helper);
        KoningKlodder k = koning(helper);
        k.bijdragen().schade(veel.getUUID(), 970);
        k.bijdragen().schade(weinig.getUUID(), 30);
        k.verslagen(veel);
        helper.assertTrue(k.kisten.size() == 1, "one chest (only the player above 5 %), got " + k.kisten.size());
        helper.assertTrue(sl.getBlockState(k.kisten.get(0)).is(KernRegistry.BAASKIST.get()), "a boss chest stands there");
        // what such a chest holds: everything bound, the first time a Legendary + a trophy
        var r = BazenLoot.kist("opperzwam", weinig, 19, RandomSource.create(7));
        boolean legendarisch = false, trofee = false;
        for (ItemStack s : r.items()) {
            Uitrusting u = Rollen.van(s);
            if (u != null) {
                helper.assertTrue(u.gebonden(), "boss loot is gebonden: " + s);
                legendarisch |= u.type().startsWith("baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            } else {
                trofee |= s.is(BazenLoot.TROFEE_OPPERZWAM.get());
            }
        }
        helper.assertTrue(legendarisch && trofee, "first kill: a named Legendary and the trophy");
        var r2 = BazenLoot.kist("mika_spion", weinig, 15, RandomSource.create(8));
        helper.assertTrue(r2.items().stream().anyMatch(s -> s.is(BazenLoot.NEPPE_SNOR.get())), "the Mika-spion's first kill gives his Guhddelijk snor");
        helper.succeed();
    }
}
