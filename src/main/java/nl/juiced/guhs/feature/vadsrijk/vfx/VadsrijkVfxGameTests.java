package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Anker;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Stap;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-VFX: the shapes hit exactly what they show, every boss of zones 1-3 has a bouwdoos profile (with fair telegraphs), a telegraph
 * goes out as ONE decal packet, a bouwdoos attack hits who stands in it and not who stands out, an interruptible cast stops when
 * the boss takes enough damage (and stuns it). Run: {@code runGameTestServer -Pgt=VadsrijkVfx}.
 */
public class VadsrijkVfxGameTests {
    private static final String BATCH = "vadsrijk_vfx";
    /** The bosses of zones 1-3 that must have at least one new mechanic. */
    static final List<String> BAZEN = List.of("klodderreus", "koning_klodder", "opperzwam", "galmwolf", "spiegelzucht", "boormeester", "kruinroofvogel",
            "taleggio", "brokkel", "roquefort", "piepvel", "wortelwurger", "nestwachter", "bleu", "vahoegnir");

    @GuhTest(template = "empty", batch = BATCH)
    public static void vormen(GameTestHelper helper) {
        Vec3 o = Vec3.ZERO;
        helper.assertTrue(Vorm.cirkel(3).raakt(o, 0, new Vec3(2.9, 0, 0)) && !Vorm.cirkel(3).raakt(o, 0, new Vec3(3.1, 0, 0)), "circle edge");
        helper.assertTrue(!Vorm.donut(4, 10).raakt(o, 0, new Vec3(2, 0, 0)) && Vorm.donut(4, 10).raakt(o, 0, new Vec3(0, 0, 6))
                && !Vorm.donut(4, 10).raakt(o, 0, new Vec3(11, 0, 0)), "donut: safe inside, hit in the ring, nothing outside");
        helper.assertTrue(!Vorm.veilig(3).raakt(o, 0, new Vec3(1, 0, 1)) && Vorm.veilig(3).raakt(o, 0, new Vec3(8, 0, 0)), "safe spot: hit outside only");
        Vorm k = Vorm.kegel(10, 90);
        helper.assertTrue(k.raakt(o, 0, new Vec3(5, 0, 0)) && k.raakt(o, 0, new Vec3(5, 0, 4.9)) && !k.raakt(o, 0, new Vec3(5, 0, 5.5))
                && !k.raakt(o, 0, new Vec3(-5, 0, 0)) && !k.raakt(o, 0, new Vec3(11, 0, 0)), "cone: 90 degrees along +x, 10 long");
        helper.assertTrue(k.raakt(o, Math.PI, new Vec3(-5, 0, 0)) && !k.raakt(o, Math.PI, new Vec3(5, 0, 0)), "cone turned around");
        Vorm l = Vorm.lijn(10, 2);
        helper.assertTrue(l.raakt(o, 0, new Vec3(9, 0, 0.9)) && !l.raakt(o, 0, new Vec3(9, 0, 1.1)) && !l.raakt(o, 0, new Vec3(-1, 0, 0))
                && !l.raakt(o, 0, new Vec3(11, 0, 0)), "strip: from the anchor forward, 2 wide");
        helper.assertTrue(l.raakt(o, Math.PI / 2, new Vec3(0, 0, 5)) && !l.raakt(o, Math.PI / 2, new Vec3(5, 0, 0)), "strip along +z");
        Vorm x = Vorm.kruis(8, 2);
        helper.assertTrue(x.raakt(o, 0, new Vec3(-7, 0, 0)) && x.raakt(o, 0, new Vec3(0, 0, 7)) && x.raakt(o, 0, new Vec3(0, 0, -7))
                && !x.raakt(o, 0, new Vec3(4, 0, 4)), "cross: both bars both ways, not between");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void profielen(GameTestHelper helper) {
        Map<String, List<Aanval>> alle = BaasProfielen.alle();
        for (String b : BAZEN) {
            helper.assertTrue(alle.containsKey(b) && !alle.get(b).isEmpty(), "boss " + b + " has bouwdoos attacks");
        }
        Set<String> ids = new java.util.HashSet<>();
        int onderbreek = 0, veilig = 0, stapel = 0;
        for (List<Aanval> l : alle.values()) {
            for (Aanval a : l) {
                helper.assertTrue(ids.add(a.id()), "attack ids are unique: " + a.id());
                helper.assertTrue(!a.stappen().isEmpty() && a.duur() > 0 && a.duur() < 200, a.id() + ": steps and a sane length");
                for (Stap s : a.stappen()) {
                    helper.assertTrue(s.telegraaf() >= 20, a.id() + ": every telegraph shows at least 1 s (solo fair)");
                    veilig += s.vorm().soort() == Vorm.Soort.VEILIG ? 1 : 0;
                    stapel += s.stapel ? 1 : 0;
                }
                onderbreek += a.onderbreekbaar() ? 1 : 0;
            }
        }
        helper.assertTrue(onderbreek >= 3 && veilig >= 3 && stapel >= 1, "the mechanics are used: interrupts " + onderbreek + ", safe spots " + veilig
                + ", stacks " + stapel);
        helper.succeed();
    }

    /** A 31x31 stone floor. */
    private static void vloer(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                sl.setBlock(o.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 6; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static ServerPlayer speler(GameTestHelper helper, double dx, double dz) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        p.snapTo(o.getX() + 0.5 + dx, o.getY() + 1, o.getZ() + 0.5 + dz);
        return p;
    }

    private static Klodderreus reus(GameTestHelper helper) {
        Klodderreus b = KernRegistry.KLODDERREUS.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        b.snapTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5, 0, 0);
        b.stelIn(3, Element.WATER, false, Set.of());
        helper.getLevel().addFreshEntity(b);
        return b;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void telegraafIsDecal(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 2, 0);
        int voor = VfxPayloads.verstuurd;
        Telegraaf.cirkel(helper.getLevel(), p.position(), 3, 20, Telegraaf.ROOD);
        helper.assertTrue(Telegraaf.DECALS && VfxPayloads.verstuurd > voor, "a telegraph goes out as a decal packet");
        helper.succeed();
    }

    /** A circle on the target hits it; a player 9 blocks away stays untouched. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void bouwdoosRaakt(GameTestHelper helper) {
        vloer(helper);
        Klodderreus b = reus(helper);
        ServerPlayer erin = speler(helper, 4, 0), eruit = speler(helper, -9, 0);
        b.testFilter = x -> x == erin || x == eruit;
        Vec3 plekIn = erin.position(), plekUit = eruit.position();
        double vadsIn = Spelers.vads(erin), vadsUit = Spelers.vads(eruit);
        Aanval a = Aanval.nieuw("test_cirkel").stap(Stap.op(0, 20, Vorm.cirkel(2.5), Anker.ELKE_SPELER).factor(1.0));
        Aanval smal = Aanval.nieuw("test_cirkel_weg").stap(Stap.op(0, 20, Vorm.cirkel(1.5), Anker.MIDDEN).factor(1.0));
        helper.runAfterDelay(5, () -> {
            BaasVfx.start(b, helper.getLevel(), a);
            helper.assertTrue(BaasVfx.loopt(b), "the attack runs");
        });
        for (int t = 6; t < 40; t++) {
            helper.runAfterDelay(t, () -> {
                erin.snapTo(plekIn.x, plekIn.y, plekIn.z);
                eruit.snapTo(plekUit.x, plekUit.y, plekUit.z);
            });
        }
        helper.runAfterDelay(40, () -> {
            Bouwdoos d = BaasVfx.laatste(b);
            helper.assertTrue(d != null && !BaasVfx.loopt(b), "it ended");
            helper.assertTrue(d.geraakt == 2, "spread: one circle under each fighter hit both (" + d.geraakt + ")");
            helper.assertTrue(Spelers.vads(erin) < vadsIn && Spelers.vads(eruit) < vadsUit, "both lost Vads");
            // now a circle in the middle (under the boss, r 1.5): nobody stands there
            BaasVfx.start(b, helper.getLevel(), smal);
        });
        helper.runAfterDelay(75, () -> {
            Bouwdoos d = BaasVfx.laatste(b);
            helper.assertTrue(d != null && d.aanval.id().equals("test_cirkel_weg") && d.geraakt == 0, "who stands out of it is not hit");
            b.discard();
            helper.succeed();
        });
    }

    /** An interruptible cast stops when the boss takes its share of damage, and the boss is stunned. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void onderbreken(GameTestHelper helper) {
        vloer(helper);
        Klodderreus b = reus(helper);
        ServerPlayer p = speler(helper, 3, 0);
        b.testFilter = x -> x == p;
        Aanval a = Aanval.nieuw("test_bad").onderbreek(0.05).stap(Stap.op(0, 60, Vorm.cirkel(3), Anker.BAAS).factor(1.0));
        boolean[] geheeld = {false};
        a.nietOnderbroken((x, sl) -> geheeld[0] = true);
        helper.runAfterDelay(5, () -> BaasVfx.start(b, helper.getLevel(), a));
        helper.runAfterDelay(20, () -> b.krijgSchade(b.maxVads() * 0.06, p));
        helper.runAfterDelay(30, () -> {
            Bouwdoos d = BaasVfx.laatste(b);
            helper.assertTrue(d != null && d.onderbroken && !BaasVfx.loopt(b), "interrupted by 6 % damage");
            helper.assertTrue(Statussen.heeft(b, Status.VERDOOFD), "the boss reels (stunned)");
            helper.assertTrue(!geheeld[0], "no heal when interrupted");
        });
        // not interrupted: the follow-up runs
        Aanval b2 = Aanval.nieuw("test_bad2").onderbreek(0.5).stap(Stap.op(0, 20, Vorm.cirkel(1), Anker.MIDDEN).factor(1.0));
        b2.nietOnderbroken((x, sl) -> geheeld[0] = true);
        helper.runAfterDelay(90, () -> BaasVfx.start(b, helper.getLevel(), b2));
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(geheeld[0], "not interrupted: the follow-up (heal) ran");
            b.discard();
            helper.succeed();
        });
    }
}
