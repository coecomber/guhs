package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-DROOM: de Grauwe Droom. The hook (no crash, false outside Het Vadsrijk), the cloud floor (only into air, inverts and back),
 * the backwards lullaby (right order = getroost: stunned + 50 % more damage; wrong note = start over; too late = nightmare), the
 * grey tendrils (tougher per tendril, cut), the humming (the 30 % floor holds until the meter is full) and the pink end, and the
 * chest. Run: {@code runGameTestServer -Pgt=Z5DroomGameTests}.
 */
public class Z5DroomGameTests {
    private static final String BATCH = "vadsrijk_z5droom";

    private static void forceer(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
    }

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar, boolean creatief) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(creatief ? GameType.CREATIVE : GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    /** A cloud + the boss over it at the test spot (high up, in air). */
    private static GrauweDroom opzet(GameTestHelper helper, BlockPos c) {
        ServerLevel sl = helper.getLevel();
        forceer(sl, c, Z5Droom.STRAAL + 4);
        Z5Droom.bouw(sl, c, true);
        GrauweDroom b = Z5Droom.spawn(sl, c);
        helper.assertTrue(b != null, "the Grauwe Droom spawns");
        return b;
    }

    private static BlockPos plek(GameTestHelper helper, int dx) {
        return helper.absolutePos(new BlockPos(4, 2, 4)).offset(dx, 40, 0);
    }

    /** The story hook is registered and harmless outside Het Vadsrijk; the cloud is built only into air and inverts and back. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void haakEnWolk(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = speler(helper, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), true);
        Z5DroomRegistry.aanmelden();
        helper.assertTrue(!Z5DroomRegistry.HAAK.start(p), "outside Het Vadsrijk the dream does not start (the story's fallback scene plays)");
        BlockPos c = plek(helper, 0);
        forceer(sl, c, Z5Droom.STRAAL + 4);
        sl.setBlock(c.offset(3, -1, 0), Blocks.STONE.defaultBlockState(), 2);
        int n = Z5Droom.bouw(sl, c, true);
        helper.assertTrue(n > 900, "a cloud floor of ~" + n + " blocks");
        helper.assertTrue(sl.getBlockState(c.offset(3, -1, 0)).is(Blocks.STONE), "only into air: the stone stays");
        helper.assertTrue(sl.getBlockState(c.below()).is(Blocks.WHITE_WOOL) || sl.getBlockState(c.below()).is(Blocks.LIGHT_GRAY_WOOL), "cloud under the middle");
        int om = Z5Droom.keerOm(sl, c, true);
        helper.assertTrue(om > 800 && (sl.getBlockState(c.below()).is(Blocks.GRAY_WOOL) || sl.getBlockState(c.below()).is(Blocks.BLACK_WOOL)), "the dream turns night-grey");
        helper.assertTrue(sl.getBlockState(c.offset(3, -1, 0)).is(Blocks.STONE), "inverting never touches foreign blocks");
        int terug = Z5Droom.keerOm(sl, c, false);
        helper.assertTrue(terug == om && (sl.getBlockState(c.below()).is(Blocks.WHITE_WOOL) || sl.getBlockState(c.below()).is(Blocks.LIGHT_GRAY_WOOL)),
                "and back to white (" + terug + "/" + om + ")");
        helper.succeed();
    }

    /** The backwards lullaby: watch, then click in the right order = getroost (stunned, +50 %); a wrong note starts over. */
    @GuhTest(template = "empty", batch = BATCH + "_wiegelied", timeoutTicks = 300)
    public static void slaapliedjeTroost(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = plek(helper, 0);
        GrauweDroom b = opzet(helper, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 6), true);
        b.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(b.noot(0) != null && b.noot(GrauweDroom.NOTEN - 1) != null, "four note stones");
            b.wiegelied(sl);
            int[] reeks = b.reeks();
            helper.assertTrue(reeks != null && reeks.length == GrauweDroom.NOTEN, "a sequence of 4 notes");
            helper.runAfterDelay(reeks.length * 16 + 12, () -> {
                helper.assertTrue(b.wachtOpNoten(), "after the show: waiting for the players");
                int fout = reeks[1] == 0 ? 1 : 0;   // (anything but the second note)
                b.doelGebruikt(b.noot(reeks[0]), p);
                b.doelGebruikt(b.noot(fout), p);
                helper.assertTrue(b.wachtOpNoten() && b.wiegeliedGoed == 0, "a wrong note starts over, no harm");
                for (int i : reeks) {
                    b.doelGebruikt(b.noot(i), p);
                }
                helper.assertTrue(b.wiegeliedGoed == 1 && b.getroost() && Statussen.heeft(b, Status.VERDOOFD), "right order: getroost + stunned");
                double v = b.vads();
                b.krijgSchade(1000, p);
                helper.assertTrue(Math.abs((v - b.vads()) - 1500) < 1, "+50 % damage while getroost (" + (v - b.vads()) + ")");
                b.discard();
                helper.succeed();
            });
        });
    }

    /** Too late = a nightmare: 30 % on everyone, the dream heals a bit. */
    @GuhTest(template = "empty", batch = BATCH + "_nachtmerrie", timeoutTicks = 500)
    public static void slaapliedjeTeLaat(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = plek(helper, 0);
        GrauweDroom b = opzet(helper, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 6), false);
        b.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            Statussen.geef(b, Status.VERDOOFD, 600, 1, null);   // (no other moves in the way)
            b.krijgSchade(b.maxVads() * 0.2, p);
            double bv = b.vads();
            b.wiegelied(sl);
            int[] reeks = b.reeks();
            helper.runAfterDelay(reeks.length * 16 + 10 + 20 * (6 + 2 * reeks.length) + 5, () -> {
                helper.assertTrue(b.wiegeliedMis == 1 && !b.wachtOpNoten(), "the window closed: a nightmare");
                helper.assertTrue(Spelers.vads(p) < Spelers.maxVads(p) * 0.75, "30 % on the player");
                helper.assertTrue(b.vads() > bv, "the dream healed a little");
                b.discard();
                helper.succeed();
            });
        });
    }

    /** Grey tendrils: 15 % tougher each, they grip, and cutting one removes it. */
    @GuhTest(template = "empty", batch = BATCH + "_ranken", timeoutTicks = 200)
    public static void grauweRanken(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = plek(helper, 0);
        GrauweDroom b = opzet(helper, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 6), true);
        b.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            Statussen.geef(b, Status.VERDOOFD, 400, 1, null);
            b.ranken(sl, 3);
            helper.assertTrue(b.rankenLevend() == 3, "three tendrils");
            double v = b.vads();
            b.krijgSchade(1000, p);
            helper.assertTrue(Math.abs((v - b.vads()) - 550) < 1, "three tendrils: 55 % damage (" + (v - b.vads()) + ")");
            List<Z3Doel> l = sl.getEntitiesOfClass(Z3Doel.class, b.getBoundingBox().inflate(30), d -> d.getType() == Z5DroomRegistry.GRAUWE_RANK.get());
            Z3Doel r = l.get(0);
            r.krijgSchade(r.maxVads() + 1, p);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(b.rankenGeknipt == 1 && b.rankenLevend() == 2, "one cut, two left");
                b.discard();
                helper.succeed();
            });
        });
    }

    /** Fase 3: the dream holds at 30 % until the hum meter fills (stand in the yellow circle); then +25 % and the pink end. */
    @GuhTest(template = "empty", batch = BATCH + "_neurie", timeoutTicks = 700)
    public static void samenNeurienEnRoze(GameTestHelper helper) {
        BlockPos c = plek(helper, 0);
        GrauweDroom b = opzet(helper, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 1), true);
        b.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            b.krijgSchade(b.maxVads() * 0.70, p);
            helper.assertTrue(b.fase() == 3, "fase 3 (" + b.fase() + ")");
            b.krijgSchade(b.maxVads(), p);
            helper.assertTrue(!b.geplofd() && b.vads() >= b.maxVads() * 0.29, "held at 30 % until the humming");
            helper.runAfterDelay(GrauweDroom.NEURIE_NODIG + 40, () -> {
                p.snapTo(Vec3.atBottomCenterOf(c).add(0, 0, 1));
                helper.assertTrue(b.neurieKlaar() && b.kleur() == GrauweDroom.LILA, "the meter filled while standing in the circle (" + b.neurie() + ")");
                b.krijgSchade(b.maxVads() * 2, p);
                helper.assertTrue(b.geplofd() && b.kleur() == GrauweDroom.ROZE_DROOM, "cheered up: pink");
                helper.assertTrue(Z5Droom.alOpgevrolijkt(p), "the helper counts (replay flag)");
                helper.succeed();
            });
        });
    }

    /** The chest: first time a named Legendary + the trophy, all bound but the trophy. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void droomKist(GameTestHelper helper) {
        ServerPlayer p = speler(helper, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), true);
        LootTabel.Resultaat k = DroomLoot.kist(p, 60, RandomSource.create(5));
        boolean benoemd = k.items().stream().anyMatch(s -> {
            Uitrusting u = Rollen.van(s);
            return u != null && u.type().startsWith("z5droom_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH && u.gebonden();
        });
        boolean trofee = k.items().stream().anyMatch(s -> s.is(DroomLoot.TROFEE.get()));
        helper.assertTrue(benoemd && trofee, "first chest: a named Legendary + the trophy");
        helper.succeed();
    }
}
