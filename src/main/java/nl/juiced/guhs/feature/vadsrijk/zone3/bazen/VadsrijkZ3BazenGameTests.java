package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.piepvel.Piepvel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.roofvogel.Kruinroofvogel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Brokkel;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-3 bazen: only the risky rules. Targets at range + the rotation (Brokkel, the Kruinroofvogel), the deck cannon, the
 * Kruinroofvogel's grab (freed by damage) and the chase (riding allowed in his arena only then), Graaf Piepvel's cages (the shield
 * falls only with the 4 locks, the cape loses its panels, the dark + light rule, the big cage, the arm takes him), Vahoegnir's
 * splinter circle (immune until a splinter goes back; then the pink stage), and the two puzzles (solution yes, start no). Run:
 * {@code runGameTestServer -Pgt=VadsrijkZ3Bazen}. Each test its own batch (the bosses fight every player near them).
 */
public class VadsrijkZ3BazenGameTests {
    private static final String BATCH = "vadsrijk_z3_bazen";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar, boolean creatief) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(creatief ? GameType.CREATIVE : GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    private static <T extends Z3Baas> T zet(ServerLevel sl, net.minecraft.world.entity.EntityType<T> type, BlockPos c, int lv) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        b.stelIn(lv, null, false, Set.of());
        sl.addFreshEntity(b);
        return b;
    }

    // ---- Brokkel: targets at range, rotation, the deck cannon ------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_brokkel", timeoutTicks = 200)
    public static void brokkelBereikEnKanon(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 20);
        Brokkel b = zet(sl, Z3BazenRegistry.BROKKEL.get(), c, 39);
        ServerPlayer ver = speler(helper, Vec3.atBottomCenterOf(c).add(-16, 0, 0), false);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(b.strijders().contains(ver) && b.zDoel() != null, "the player 16 blocks away is a target");
            helper.assertTrue(b.aanvallenGedaan >= 1, "he started his rotation (" + b.laatsteAanval + ")");
            helper.assertTrue(b.kanonnen().size() == 2, "two deck cannons stand in the hall");
            Statussen.geef(b, Status.VERDOOFD, 400, 1, null);
            helper.assertTrue(b.platen() == 4, "4 plates");
            // armour: 4 plates = 40 % damage
            double v = b.vads();
            b.krijgSchade(1000, ver);
            helper.assertTrue(Math.abs((v - b.vads()) - 400) < 1, "4 plates: 40 % gets through (" + (v - b.vads()) + ")");
            // fire a cannon at him
            Z3Doel k = b.kanonnen().get(0);
            ver.snapTo(k.getX(), k.getY(), k.getZ());
            ver.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, b.position().add(0, 1, 0));
            b.doelGebruikt(k, ver);
            helper.assertTrue(b.voltreffers == 1 && b.platen() == 3, "the cannon knocked a plate off (" + b.platen() + ")");
            b.doelGebruikt(k, ver);
            helper.assertTrue(b.platen() == 3, "the cannon must reload first");
            // no damage from outside the fight
            ServerPlayer buiten = speler(helper, Vec3.atBottomCenterOf(c).add(60, 0, 0), false);
            v = b.vads();
            b.krijgSchade(500, buiten);
            helper.assertTrue(b.vads() == v, "a hit from outside the fight does nothing");
            b.discard();
            helper.succeed();
        });
    }

    // ---- De Kruinroofvogel: the grab and the chase -------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_roofvogel", timeoutTicks = 200)
    public static void roofvogelGreepEnJacht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 20);
        Kruinroofvogel k = zet(sl, Z3BazenRegistry.KRUINROOFVOGEL.get(), c, 38);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-14, 0, 0), true);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(k.strijders().contains(p) && k.aanvallenGedaan >= 1, "he fights the player at range (" + k.laatsteAanval + ")");
            Statussen.geef(k, Status.VERDOOFD, 400, 1, null);
            // the grab: held, then freed by teammates' damage (4 % of his Vads)
            k.krijgSchade(k.vads() - k.maxVads() * 0.69, p);
            helper.assertTrue(k.fase() == 2, "fase 2 at 70 %");
            Z3Vast.pak(k, p, "klauw", () -> k.position().add(0, -2.6, 0), 8, 120, 0.0, null);
            helper.assertTrue(Z3Vast.vast(p), "the player hangs in his talons");
            Z3Vast.help(p, 8);
            Z3Vast.tick(sl.getServer());
            helper.assertTrue(!Z3Vast.vast(p), "clicks / help free the player");
            // the chase: no riding in his arena, except while he flees
            Vec3 arena = Vec3.atBottomCenterOf(Z3BazenPlekken.ROOF_CENTRUM);
            helper.assertTrue(!Kruinroofvogel.magRijden(arena), "no riding in his arena normally");
            k.vlucht(sl);
            helper.assertTrue(k.jacht() && Kruinroofvogel.jachtActief && Kruinroofvogel.magRijden(arena), "he flees: riding allowed in his arena");
            helper.assertTrue(Rijdieren.RIJDEN_IN_ARENA.stream().anyMatch(f -> f.test(arena)), "the Rijdieren exception knows it");
            double v = k.vads();
            k.krijgSchade(500, p);
            helper.assertTrue(k.vads() == v, "he can't be hit while he flies off");
            k.discard();
            helper.assertTrue(!Kruinroofvogel.jachtActief, "a reset ends the chase");
            helper.succeed();
        });
    }

    // ---- Graaf Piepvel: the cages, the dark, the big cage, the arm --------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_piepvel", timeoutTicks = 400)
    public static void piepvelKooienEnDonker(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 18);
        Piepvel g = zet(sl, Z3BazenRegistry.PIEPVEL.get(), c, 42);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-6, 0, 3), true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(g.kooien().size() == 4 && g.groteKooi() != null, "4 cages + the big cage hang in the hall");
            helper.assertTrue(g.palenAan(sl) == 6, "6 lantern posts burn (" + g.palenAan(sl) + ")");
            Statussen.geef(g, Status.VERDOOFD, 600, 1, null);
            g.krijgSchade(g.vads() - g.maxVads() * 0.69, p);
            helper.assertTrue(g.fase() == 2 && g.schild(), "fase 2: the vads shield");
            double v = g.vads();
            g.krijgSchade(2000, p);
            helper.assertTrue(g.vads() == v, "the shield holds while the mice are caged");
            for (int i = 0; i < 3; i++) {
                Z3Doel k = g.kooien().get(i);
                k.krijgSchade(k.maxVads() + 1, p);
            }
            helper.assertTrue(g.bevrijd == 3 && g.schild(), "3 cages open: the shield still holds");
            Z3Doel laatste = g.kooien().get(3);
            laatste.krijgSchade(laatste.maxVads() + 1, p);
            helper.assertTrue(g.bevrijd == 4 && !g.schild(), "all 4 cages open: the shield is gone");
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(g.cape() == 0, "the mice nibbled every cape panel away (" + Integer.toBinaryString(g.cape()) + ")");
            g.krijgSchade(g.vads() - g.maxVads() * 0.39, p);
            helper.assertTrue(g.fase() == 3 && g.palenAan(sl) == 0, "fase 3: dark, every lantern out");
            double v = g.vads();
            g.krijgSchade(1000, p);
            helper.assertTrue(Math.abs((v - g.vads()) - 500) < 1, "in the dark, away from the light: -50 % (" + (v - g.vads()) + ")");
            // light the post next to him: full damage
            net.minecraft.core.BlockPos paal = g.palen().get(0);
            helper.assertTrue(g.paalAan(sl, paal, p), "a post can be lit again");
            g.snapTo(paal.getX() + 1.5, paal.getY(), paal.getZ() + 0.5, 0, 0);
            v = g.vads();
            g.krijgSchade(1000, p);
            helper.assertTrue(Math.abs((v - g.vads()) - 1000) < 1, "near a lit post: full damage (" + (v - g.vads()) + ")");
            g.krijgSchade(g.vads(), p);
            helper.assertTrue(!g.geplofd() && g.vads() >= g.maxVads() * 0.099, "he can't fall below 10 % before the big cage");
        });
        helper.runAfterDelay(76, () -> {
            helper.assertTrue(g.groteKooi() != null && g.groteKooi().raakbaar(), "at 10 % the big cage can be opened");
            g.doelGebruikt(g.groteKooi(), p);
        });
        helper.runAfterDelay(125, () -> helper.assertTrue(g.geplofd() && g.cape() == 0, "the mice storm his cape: defeated, in his shirt"));
        helper.runAfterDelay(125 + Z3Scenes.ARM_TICKS + 15, () -> {
            helper.assertTrue(g.isRemoved(), "the arm of the Keizer took him away");
            helper.succeed();
        });
    }

    // ---- Vahoegnir: the splinter circle -----------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_vahoegnir", timeoutTicks = 260)
    public static void vahoegnirSplinters(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 24);
        Vahoegnir v = zet(sl, Z3BazenRegistry.VAHOEGNIR.get(), c, 50);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-10, 0, 0), true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(v.strijders().contains(p) && v.ei() != null, "he fights, the egg lies in the nest");
            helper.assertTrue(v.splinters() == 0, "grey: 0 splinters");
            v.krijgSchade(v.vads() - v.maxVads() * 0.74, p);
            helper.assertTrue(v.fase() == 2 && v.wachtOpSplinter(), "75 %: he lands and waits for a splinter");
            double h = v.vads();
            v.krijgSchade(3000, p);
            helper.assertTrue(v.vads() == h, "he can't be hurt until a splinter goes back");
            Vec3 cirkel = v.splinterPlek();
            p.snapTo(cirkel.x, cirkel.y, cirkel.z);
        });
        helper.runAfterDelay(20 + Vahoegnir.SPLINTER_TICKS + 15, () -> {
            helper.assertTrue(v.splinters() == 1 && !v.wachtOpSplinter(), "3 s in the pink circle: splinter 1 is back (" + v.splinters() + ")");
            double h = v.vads();
            v.krijgSchade(500, p);
            helper.assertTrue(v.vads() < h, "he can be hurt again");
            v.discard();
            helper.succeed();
        });
    }

    // ---- the puzzles ------------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_puzzels", timeoutTicks = 40)
    public static void puzzelsLosbaar(GameTestHelper helper) {
        int[] loods = Z3Kerkers.LOODS_START.clone();
        helper.assertTrue(!(loods[0] == 0 && loods[1] == 0 && loods[2] == 0), "the Bewapeningsloods doesn't start solved");
        for (int i : new int[] {0, 2, 2}) {
            loods = Z3Kerkers.druk(loods, i, 4);
        }
        helper.assertTrue(loods[0] == 0 && loods[1] == 0 && loods[2] == 0, "its solution (levers 1, 0, 2 times) aims all cannons at the gate");
        int[] ringen = Z3Kerkers.RINGEN_START.clone();
        helper.assertTrue(!(ringen[0] == 0 && ringen[1] == 0 && ringen[2] == 0), "the Ringenzaal doesn't start solved");
        for (int i : new int[] {0, 1, 1, 2, 2, 2, 2}) {
            ringen = Z3Kerkers.druk(ringen, i, 8);
        }
        helper.assertTrue(ringen[0] == 0 && ringen[1] == 0 && ringen[2] == 0, "its solution (knobs 1, 2, 4 times) lines the pink vein up");
        // Vahoeg mode: a boss of a dungeon becomes lv 52 with x1.5 HP
        ServerLevel sl = helper.getLevel();
        Brokkel b = Z3BazenRegistry.BROKKEL.get().create(sl, EntitySpawnReason.COMMAND);
        b.stelIn(39, Element.VUUR, false, Set.of());
        double gewoon = b.maxVads();
        b.zetVahoeg(true);
        helper.assertTrue(b.vahoeg() && b.mobLevel() == 52 && b.maxVads() > gewoon * 1.5, "Vahoeg form: lv 52, more Vads (" + gewoon + " -> " + b.maxVads() + ")");
        helper.succeed();
    }
}
