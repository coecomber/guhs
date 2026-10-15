package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 phase B, slice h3_aarde (CONTRACT_E2_B 8: very few tests, the core of the chapter):
 * <ul>
 *   <li>{@link #einde2H3AardeKeten}: two players in a city of the test's own (the sidecar's markers round the test spot):
 *       one arrives (the card and the scene: step 0 to 1) and the other does not move; the closed gate is the first one's
 *       alone; walking into the tea house plays its scene and moves only that player; the dev command's step state;</li>
 *   <li>{@link #einde2H3AardePaiSho}: Guh Pai Sho's rules and its guh (a lotus wins, it finishes its own and stops yours,
 *       a game of two guhs always ends);</li>
 *   <li>{@link #einde2H3AardeSluipen}: what a guard of the base sees (in front near: yes; behind, far, crouching at a
 *       distance: no) and that his head only turns within its sweep.</li>
 * </ul>
 */
public class E2H3AardeGameTests {
    private static final String BATCH = "einde2_h3_aarde";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 260)
    public static void einde2H3AardeKeten(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        Plekken.Plek aankomst = Plekken.plek(E2H3Aarde.STRUCTUUR, "aankomst_speler");
        helper.assertTrue(aankomst != null && Plekken.plek(E2H3Aarde.STRUCTUUR, "lu_ten_camera") != null && Plekken.plek(E2H3Aarde.STRUCTUUR, "h3_val_wijd_camera") != null,
                "the sidecar of ba_sing_guh has the chapter's markers");
        BlockPos o = p.blockPosition().subtract(aankomst.pos());
        var oud = E2H3Aarde.oorsprong;
        var oudOpen = H3Verloop.open;
        E2H3Aarde.oorsprong = level -> level == helper.getLevel() ? o : null;
        H3Verloop.open = pl -> true;
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(helper.getLevel());
        E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.AANKOMST);
        E2VerhaalDev.zetTot(q, E2Verhaal.AARDE, E2H3Aarde.LOKET + 1);
        // the gate: closed (bars) for the one without a stamped passport, open for the other
        BlockPos poort = o.offset(H3Schijn.POORT_X, H3Schijn.POORT_Y0 + 1, (H3Schijn.POORT_Z0 + H3Schijn.POORT_Z1) / 2);
        Map<BlockPos, BlockState> voorP = new HashMap<>(), voorQ = new HashMap<>();
        H3Schijn.vul(p, stad, voorP);
        H3Schijn.vul(q, stad, voorQ);
        helper.assertTrue(voorP.containsKey(poort) && voorP.get(poort).is(Blocks.IRON_BARS) && !voorQ.containsKey(poort), "the closed gate is p's alone");
        // (mock players do not tick by themselves: their tick events drive the lock of the card and the scenes)
        helper.onEachTick(() -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(q));
        });
        // p arrives: the narrator card, then the scene; step 1 (q stays where q was)
        H3Verloop.aankomst(p, stad);
        helper.runAfterDelay(30, () -> helper.assertTrue(E2H3Aarde.stap(p) == E2H3Aarde.LOKET && E2H3Aarde.stap(q) == E2H3Aarde.LOKET + 1,
                "arrived: p at the desk, q untouched: " + E2H3Aarde.stap(p) + " " + E2H3Aarde.stap(q)));
        helper.runAfterDelay(80, () -> {
            // (after the after-care of the last scene) the tea house: walking into it plays its scene, only for the one whose step it is
            E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.THEEHUIS);
            Plekken.Plek thee = Plekken.plek(E2H3Aarde.STRUCTUUR, "theehuis");
            Vec3 binnen = Vec3.atBottomCenterOf(o.offset(thee.pos()).offset(2, 0, 2));
            p.snapTo(binnen.x, binnen.y, binnen.z);
            H3Verloop.tik(p, stad);
        });
        helper.runAfterDelay(130, () -> {
            try {
                helper.assertTrue(E2H3Aarde.stap(p) == E2H3Aarde.THEE, "the tea house scene: p at the tea, step " + E2H3Aarde.stap(p));
                // the dev command's step state: earth learnt after the lesson, the bars open after the reunion
                E2H3AardeCommando.zet(q, E2H3Aarde.LU_TEN);
                helper.assertTrue(E2H3Aarde.stap(q) == E2H3Aarde.LU_TEN && E2Verhaal.vlag(q, E2Verhaal.AARDE, E2Verhaal.V_GELEERD)
                        && E2H3Aarde.LIJN.vlag(q, H3Schijn.V_TRALIES) && E2Verhaal.klaar(q, E2Verhaal.LUCHT) == (E2Verhaal.lijn(E2Verhaal.LUCHT) != null),
                        "ga lu_ten: the flags of the steps before it");
                helper.assertTrue(E2H3AardeCommando.stapVan("val") == E2H3Aarde.VAL && E2H3AardeCommando.START.length == E2H3Aarde.STAPPEN + 1
                        && E2H3Aarde.STAP_NAMEN.size() == E2H3Aarde.STAPPEN + 1, "every step has a name and a start");
                helper.succeed();
            } finally {
                E2H3Aarde.oorsprong = oud;
                H3Verloop.open = oudOpen;
                E2VerhaalDev.wis(p, E2Verhaal.AARDE);
                E2VerhaalDev.wis(q, E2Verhaal.AARDE);
                H3Acteurs.wegAlle(p, "");
                H3Acteurs.wegAlle(q, "");
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                helper.getLevel().removePlayerImmediately(q, Entity.RemovalReason.DISCARDED);
            }
        });
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H3AardePaiSho(GameTestHelper helper) {
        // a lotus: a tile with its four petals
        String b = PaiSho.NIEUW;
        int[] lotus = {PaiSho.cel(2, 2), PaiSho.cel(2, 1), PaiSho.cel(3, 2), PaiSho.cel(2, 3), PaiSho.cel(1, 2)};
        int[] ander = {PaiSho.cel(0, 0), PaiSho.cel(4, 0), PaiSho.cel(0, 4), PaiSho.cel(4, 4)};
        for (int i = 0; i < 4; i++) {
            b = PaiSho.zet(b, lotus[i], PaiSho.A);
            helper.assertTrue(b != null && PaiSho.winnaar(b) == PaiSho.LEEG, "no lotus yet after " + (i + 1));
            b = PaiSho.zet(b, ander[i], PaiSho.B);
        }
        helper.assertTrue(PaiSho.zet(b, lotus[0], PaiSho.A) == null && PaiSho.zet(b, lotus[4], PaiSho.B) == null, "a taken square, a move out of turn");
        // the guh finishes its own lotus, and (as the other) stops it
        helper.assertTrue(PaiSho.guhZet(b, PaiSho.A, new Random(1), 0) == lotus[4], "the guh finishes its own lotus");
        helper.assertTrue(PaiSho.guhZet(b.substring(0, lotus[4]) + "." + b.substring(lotus[4] + 1), PaiSho.B, new Random(1), 0) == lotus[4]
                || PaiSho.zet(b, lotus[4], PaiSho.A) != null, "the other guh stops it");
        String gewonnen = PaiSho.zet(b, lotus[4], PaiSho.A);
        helper.assertTrue(gewonnen != null && PaiSho.winnaar(gewonnen) == PaiSho.A && PaiSho.klaar(gewonnen) && PaiSho.winnendeLotus(gewonnen).length == 5
                && PaiSho.zet(gewonnen, PaiSho.cel(0, 1), PaiSho.B) == null, "a lotus wins and the game is over");
        // two guhs always finish a game (a lotus or a full board)
        Random r = new Random(42);
        for (int spel = 0; spel < 20; spel++) {
            String bord = PaiSho.NIEUW;
            int zetten = 0;
            while (!PaiSho.klaar(bord) && zetten < 30) {
                char wie = PaiSho.beurt(bord);
                bord = PaiSho.zet(bord, PaiSho.guhZet(bord, wie, r, 0.3), wie);
                helper.assertTrue(bord != null, "the guh only plays allowed moves");
                zetten++;
            }
            helper.assertTrue(PaiSho.klaar(bord) && zetten <= PaiSho.N * PaiSho.N, "game " + spel + " ends");
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H3AardeSluipen(GameTestHelper helper) {
        Vec3 wacht = new Vec3(0.5, 0, 0.5);
        float zuid = 0f;   // (yaw 0 = he looks south, +z)
        helper.assertTrue(H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 5.5), false), "in front, walking, 5 blocks: seen");
        helper.assertTrue(!H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, -4.5), false), "behind him: not seen");
        helper.assertTrue(!H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 5.5), true) && H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 2.5), true),
                "crouching: only seen close by");
        helper.assertTrue(!H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 9.5), false), "too far: not seen");
        helper.assertTrue(!H3Sluipen.ziet(wacht, zuid, new Vec3(6.5, 0, 2.5), false), "far to the side: not seen");
        boolean binnen = true;
        for (long t = 0; t < 400; t += 7) {
            float y = H3Sluipen.kijkt(90f, 3, t);
            binnen &= y >= 90f - H3Sluipen.ZWAAI - 0.01 && y <= 90f + H3Sluipen.ZWAAI + 0.01;
        }
        helper.assertTrue(binnen, "a guard's head stays within its sweep");
        helper.succeed();
    }
}
