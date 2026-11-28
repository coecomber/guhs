package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5BazenLoot;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-DROOM: de Grauwe Droom. The hook (no crash, false outside Het Vadsrijk), the cloud floor (only into air, inverts and back),
 * the backwards lullaby (right order = getroost: stunned + 50 % more damage; wrong note = start over; too late = nightmare), the
 * grey tendrils (tougher per tendril, cut), the humming (the 30 % floor holds until the meter is full) and the pink end, the
 * chest, and the chests of the two-stage finale (falling chest, wake-up, brought after the player). Run: {@code runGameTestServer -Pgt=Z5DroomGameTests}.
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

    /**
     * The two-stage finale (spy -> dream) and the chests, with the chest ceremony of a real server (the chest falls first, here: it
     * lands when the test says so). The spy's chest lands in the Wolkwaldroom while the player is already in the dream; the dream's
     * chest lands seconds after the plof. Waking up waits for the dream chest to land AND be opened (1.10.26 woke the player while
     * it was still in the air), and what was left behind is brought after the player; other players' chests stay.
     */
    @GuhTest(template = "empty", batch = BATCH + "_kist", timeoutTicks = 200)
    public static void kistNaSpionEnDroom(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        // (30 above the other tests' clouds, which may overlap this plot; the "Wolkwaldroom" well under our own cloud)
        BlockPos c = plek(helper, 0).above(30), arena = c.offset(0, -12, 0);
        GrauweDroom b = opzet(helper, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 6), true);
        ServerPlayer ander = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 8), true);
        b.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            List<Runnable> lucht = new java.util.ArrayList<>();   // (chests still falling)
            BaasKist.Ceremonie echt = BaasKist.CEREMONIE;
            BaasKist.CEREMONIE = new BaasKist.Ceremonie() {
                @Override
                public void val(ServerLevel l, ServerPlayer wie, BlockPos plek, List<ItemStack> buit, Runnable neerzetten) {
                    lucht.add(neerzetten);
                }

                @Override
                public void open(ServerLevel l, ServerPlayer wie, BlockPos pos, List<ItemStack> buit) {
                }
            };
            try {
                // stage 1: the spy is beaten, his chest is on its way down in the Wolkwaldroom, the dream takes the player at once
                List<ItemStack> spion = new java.util.ArrayList<>(Z5BazenLoot.kist("zilverstaart", p, 60, RandomSource.create(3)).items());
                spion.add(new ItemStack(Items.DIAMOND, 3));
                BaasKist.plaats(sl, p, arena.offset(3, 0, 0), new LootTabel.Resultaat(spion, 0));
                // stage 2: the dream is cheered up
                b.krijgSchade(b.maxVads() * 0.70, p);
                b.neurieKlaar(sl);
                b.krijgSchade(b.maxVads() * 2, p);
            } finally {
                BaasKist.CEREMONIE = echt;
            }
            helper.assertTrue(b.geplofd() && Z5Droom.alOpgevrolijkt(p), "the dream is cheered up");
            helper.assertTrue(lucht.size() == 2, "two chests on their way down: the spy's and the dream's (" + lucht.size() + ")");
            BlockPos min = c.offset(-Z5Droom.STRAAL - 6, -4, -Z5Droom.STRAAL - 6), max = c.offset(Z5Droom.STRAAL + 6, 10, Z5Droom.STRAAL + 6);
            helper.assertTrue(BaasKist.vanSpeler(sl, p.getUUID(), min, max).isEmpty(), "no chest on the cloud yet");
            helper.assertTrue(!Z5Droom.kistOpen(sl, p) && !Z5Droom.kistOpen(sl, p), "a chest that has not landed yet is not an opened chest: no wake-up");
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!Z5Droom.kistOpen(sl, p), "still waiting for the chest");
                lucht.forEach(Runnable::run);   // (both land)
                BaasKist.plaats(sl, ander, arena.offset(-3, 0, 0), new LootTabel.Resultaat(List.of(new ItemStack(Items.GOLD_INGOT)), 0));
                List<BlockPos> opWolk = BaasKist.vanSpeler(sl, p.getUUID(), min, max);
                helper.assertTrue(opWolk.size() == 1, "the dream chest stands on the cloud (" + opWolk.size() + ")");
                BlockPos k = opWolk.get(0);
                helper.assertTrue(!Z5Droom.kistOpen(sl, p), "it stands: not opened yet");
                helper.assertTrue(!BaasKist.lever(sl, k, ander), "somebody else's chest is never handed over");
                sl.getBlockState(k).useWithoutItem(sl, p, new BlockHitResult(Vec3.atCenterOf(k), Direction.UP, k, false));
                helper.assertTrue(sl.getBlockState(k).isAir() && !p.getInventory().isEmpty(), "the owner opens it: the loot is theirs");
                helper.assertTrue(Z5Droom.kistOpen(sl, p), "landed and opened: now the player wakes up");
                helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0 && sl.getBlockState(arena.offset(3, 0, 0)).is(KernRegistry.BAASKIST.get()),
                        "the spy's chest still stands in the Wolkwaldroom");
                // waking up: what was left behind comes after the player (here also a second, unopened chest on the cloud)
                BaasKist.plaats(sl, p, c.offset(2, 0, 0), new LootTabel.Resultaat(List.of(new ItemStack(Items.EMERALD, 2)), 0));
                int n = Z5Droom.nabrengen(sl, p, c, arena);
                helper.assertTrue(n == 2, "two chests brought after the player (" + n + ")");
                helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 3 && p.getInventory().countItem(Items.EMERALD) == 2, "their loot is in the bags");
                helper.assertTrue(sl.getBlockState(arena.offset(3, 0, 0)).isAir() && sl.getBlockState(c.offset(2, 0, 0)).isAir(), "and the chests are gone");
                helper.assertTrue(sl.getBlockState(arena.offset(-3, 0, 0)).is(KernRegistry.BAASKIST.get()) && ander.getInventory().countItem(Items.GOLD_INGOT) == 0
                        && p.getInventory().countItem(Items.GOLD_INGOT) == 0, "the other player's chest stays where it is");
                helper.assertTrue(Z5Droom.nabrengen(sl, p, c, arena) == 0, "nothing twice");
                sl.setBlock(arena.offset(-3, 0, 0), Blocks.AIR.defaultBlockState(), 3);
                b.discard();
                helper.succeed();
            });
        });
    }
}
