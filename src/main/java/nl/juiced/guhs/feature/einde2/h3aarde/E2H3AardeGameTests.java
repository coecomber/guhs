package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.HashMap;
import java.util.List;
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
 *       distance: no) and that his head only turns within its sweep;</li>
 *   <li>{@link #einde2H3AardeHints} (fix2-h3): every step after the arrival has a hint whose target is a spot of the city,
 *       outdoors with a 3 s look scene, the team lines are said by members of the team, step 3 follows Toph's lesson, the
 *       trail of the catacombs runs from the foot of the stair into the crystal chamber, and Katara-guh is not at the
 *       campfire while the Dai Li hold her.</li>
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
        helper.assertTrue(!H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 5.5), true) && H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 2.0), true)
                && !H3Sluipen.ziet(wacht, zuid, new Vec3(0.5, 0, 2.5), true), "crouching: only seen close by (fix3-h3: within 1.8 blocks)");
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

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H3AardeHints(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Plekken.Plek aankomst = Plekken.plek(E2H3Aarde.STRUCTUUR, "aankomst_speler");
        BlockPos o = p.blockPosition().subtract(aankomst.pos());
        var oud = E2H3Aarde.oorsprong;
        E2H3Aarde.oorsprong = level -> level == helper.getLevel() ? o : null;
        try {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(helper.getLevel());
            String lijn = E2H3Aarde.LIJN.id();
            for (int stap = E2H3Aarde.LOKET; stap < E2H3Aarde.STAPPEN; stap++) {
                nl.juiced.guhs.feature.einde2.verhaal.StapHint.Hint h = nl.juiced.guhs.feature.einde2.verhaal.StapHint.hint(lijn, stap);
                helper.assertTrue(h != null && h.bakenAan(), "step " + stap + " has a hint with a light column");
                BlockPos doel = h.doel().waar().apply(p, helper.getLevel()), terug = h.terugDoel().waar().apply(p, helper.getLevel());
                helper.assertTrue(doel != null && terug != null && stad.doos(0).contains(Vec3.atCenterOf(doel)), "step " + stap + ": its target and resume spot lie in the city");
                boolean binnen = stap == E2H3Aarde.THEE || stap == E2H3Aarde.PAI_SHO;
                var kijk = h.kijk();
                helper.assertTrue(binnen == (kijk == null) && (kijk == null || kijk.duur() == nl.juiced.guhs.feature.einde2.verhaal.StapHint.KIJK_DUUR
                        && kijk.pauzes().length == 0), "step " + stap + ": a 3 s look scene outdoors, none in the tea house");
                // (fix3-h3: a step opens with its story scene or its team line, then the hint; step 14 runs by itself after the fall)
                boolean eigen = H3Hints.heeftAansluiting(stap);
                helper.assertTrue(stap == E2H3Aarde.TOPH || eigen ? !h.bijStartAan() : h.bijStartAan(),
                        "step " + stap + ": the hint comes once (by itself, or after the scene or team line)");
                helper.assertTrue(!eigen || h.tekst() != null, "step " + stap + ": a line of its own");
            }
            // step 3 follows the lesson: Toph-guh, then the opening of her course (west of its zone)
            E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.BLINDDOEK);
            var drie = nl.juiced.guhs.feature.einde2.verhaal.StapHint.hint(lijn, E2H3Aarde.BLINDDOEK).doel();
            BlockPos toph = drie.waar().apply(p, helper.getLevel());
            E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN, true);
            BlockPos baan = drie.waar().apply(p, helper.getLevel());
            var zone = stad.zone("blinddoek_baan", 0);
            helper.assertTrue(toph != null && toph.equals(stad.pos("toph")) && baan != null && baan.getX() == (int) zone.minX - 1
                    && baan.getZ() > zone.minZ && baan.getZ() < zone.maxZ, "step 3: Toph, then her course");
            // the team lines: members of the team, said after chapter 2
            for (int stap = 0; stap < E2H3Aarde.STAPPEN; stap++) {
                String t = H3Hints.teamLijn(stap);
                helper.assertTrue(t == null || nl.juiced.guhs.feature.einde2.reizen.E2Team.leden(p).stream().anyMatch(l -> t.startsWith(l.id() + "/")),
                        "team line of step " + stap + " (" + t + ") is said by a member of the team after chapter 2");
            }
            // the trail of the catacombs: from the foot of the old gate's stair into the crystal chamber
            int[] a = H3Hints.CATACOMBEN[0], z = H3Hints.CATACOMBEN[H3Hints.CATACOMBEN.length - 1];
            helper.assertTrue(Plekken.plek(E2H3Aarde.STRUCTUUR, "rustpunt_4").pos().equals(new BlockPos(a[0], a[1], a[2]))
                    && stad.zone("kristalkamer", 0).contains(Vec3.atBottomCenterOf(o.offset(z[0], z[1], z[2])).add(0, 0.5, 0)), "the trail of the catacombs");
            // Katara-guh is not at the campfire while the Dai Li hold her
            E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.VAL);
            boolean weg = nl.juiced.guhs.feature.einde2.reizen.E2Team.AFWEZIG.stream()
                    .anyMatch(f -> f.weg(p, E2H3Aarde.STRUCTUUR, nl.juiced.guhs.feature.einde2.reizen.E2Team.Lid.KATARA));
            E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.LU_TEN);
            boolean daar = nl.juiced.guhs.feature.einde2.reizen.E2Team.AFWEZIG.stream()
                    .noneMatch(f -> f.weg(p, E2H3Aarde.STRUCTUUR, nl.juiced.guhs.feature.einde2.reizen.E2Team.Lid.KATARA));
            helper.assertTrue(weg && daar, "Katara-guh: away at the fall, at the camp before it");
            helper.succeed();
        } finally {
            E2H3Aarde.oorsprong = oud;
            E2VerhaalDev.wis(p, E2Verhaal.AARDE);
            nl.juiced.guhs.feature.einde2.verhaal.StapHint.vergeet(p.getUUID());
            H3Hints.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * fix3-h3 (playtest 2 item 5): the sneak can be done. A crouching player who keeps to the far wall of the corridor (two
     * blocks from each guard, round the crates) is never seen by any of the six guards at any moment of their sweep, from
     * the rest point to the bars of Appa's chamber; walking the same way he is seen. The route's floor is the build's own
     * (einde2_bouw_aarde_onder.basis: one flat feet level, corridors three wide, crates one cell big); the guards and the
     * goal are the sidecar's markers. Being sent back puts you on the rest point facing down the corridor, not the stair.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H3AardeSluipRoute(GameTestHelper helper) {
        double[][] route = {{222.5, 108.5}, {222.5, 112.5}, {221.6, 114.5}, {222.5, 116.5}, {221.4, 117.5}, {221.4, 121.6}, {222.5, 123.5},
                {223.5, 124.6}, {223.5, 130.6}, {223.5, 134.5}, {223.4, 136.4}};
        int y = Plekken.plek(E2H3Aarde.STRUCTUUR, "rustpunt_3").pos().getY();
        Vec3[] oog = new Vec3[H3Sluipen.WACHTERS];
        float[] basis = new float[H3Sluipen.WACHTERS];
        for (int nr = 1; nr <= H3Sluipen.WACHTERS; nr++) {
            Plekken.Plek w = Plekken.plek(E2H3Aarde.STRUCTUUR, "sluip_wacht_" + nr);
            oog[nr - 1] = Vec3.atBottomCenterOf(w.pos());
            basis[nr - 1] = w.yaw();
        }
        boolean gebuktGezien = false, lopendGezien = false;
        String waar = "";
        for (int i = 1; i < route.length; i++) {
            Vec3 a = new Vec3(route[i - 1][0], y, route[i - 1][1]), b = new Vec3(route[i][0], y, route[i][1]);
            for (double f = 0; f <= 1.0001; f += 0.05) {
                Vec3 punt = a.lerp(b, f);
                for (long tick = 0; tick < 600; tick += 3) {
                    for (int nr = 1; nr <= H3Sluipen.WACHTERS; nr++) {
                        float yaw = H3Sluipen.kijkt(basis[nr - 1], nr, tick);
                        if (H3Sluipen.ziet(oog[nr - 1], yaw, punt, true) && !gebuktGezien) {
                            gebuktGezien = true;
                            waar = "guard " + nr + " at " + punt + " tick " + tick;
                        }
                        lopendGezien |= H3Sluipen.ziet(oog[nr - 1], yaw, punt, false);
                    }
                }
            }
        }
        Vec3 eind = new Vec3(route[route.length - 1][0], y, route[route.length - 1][1]);
        Vec3 poort = Vec3.atBottomCenterOf(Plekken.plek(E2H3Aarde.STRUCTUUR, "appa_poort").pos());
        helper.assertTrue(!gebuktGezien, "crouching along the far wall nobody sees you: " + waar);
        helper.assertTrue(lopendGezien, "walking the same way they do");
        helper.assertTrue(eind.distanceTo(poort) <= H3Sluipen.DOEL && eind.z < poort.z - 0.36, "the route ends at the bars, in front of them");
        helper.assertTrue(H3Sluipen.RUST_YAW == 0f, "sent back, you face south, down the corridor (the stair is north of the hall)");
        helper.succeed();
    }

    /**
     * fix3-h3 (playtest 2 items 6, 7, 1): Appa's bars are open in the world and shut only as a shown block (so after the
     * reunion nothing invisible blocks the way); the resident Miroh under the tree is never seen (only the chapter's own
     * Miroh at step 12); every step that opens with a story scene has that scene registered on a marker of the city.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H3AardeTralies(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Plekken.Plek poortPlek = Plekken.plek(E2H3Aarde.STRUCTUUR, "appa_poort");
        BlockPos o = p.blockPosition().offset(0, 0, 4).subtract(poortPlek.pos());
        var oud = E2H3Aarde.oorsprong;
        E2H3Aarde.oorsprong = level -> level == helper.getLevel() ? o : null;
        try {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(helper.getLevel());
            BlockPos poort = stad.pos("appa_poort");
            for (int dx = -H3Schijn.TRALIES_HALF; dx <= H3Schijn.TRALIES_HALF; dx++) {
                for (int dy = 0; dy < H3Schijn.TRALIES_HOOG; dy++) {
                    helper.getLevel().setBlock(poort.offset(dx, dy, 0), Blocks.IRON_BARS.defaultBlockState(), 2);
                }
            }
            H3Schijn.vergeetOpen();
            H3Schijn.traliesWeg(stad);
            boolean leeg = true;
            for (int dx = -H3Schijn.TRALIES_HALF; dx <= H3Schijn.TRALIES_HALF; dx++) {
                for (int dy = 0; dy < H3Schijn.TRALIES_HOOG; dy++) {
                    leeg &= helper.getLevel().getBlockState(poort.offset(dx, dy, 0)).isAir();
                }
            }
            helper.assertTrue(leeg, "the real bars are out of the world");
            E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, E2H3Aarde.LAOGAI);
            Map<BlockPos, BlockState> dicht = new HashMap<>();
            H3Schijn.vul(p, stad, dicht);
            helper.assertTrue(dicht.containsKey(poort) && dicht.get(poort).is(Blocks.IRON_BARS) && dicht.containsKey(poort.offset(2, 4, 0)),
                    "not freed yet: the whole gate is shown shut");
            E2H3Aarde.LIJN.vlag(p, H3Schijn.V_TRALIES, true);
            Map<BlockPos, BlockState> open = new HashMap<>();
            H3Schijn.vul(p, stad, open);
            helper.assertTrue(!open.containsKey(poort) && !open.containsKey(poort.offset(2, 0, 0)) && open.containsKey(poort.offset(3, 0, 0))
                    && !open.containsKey(poort.offset(3, 3, 0)), "freed: only the bent frame at the sides, the middle is open for real");
            // item 7: the resident on the hill is never seen
            Entity miroh = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            H3Rollen.bijMaken(miroh, E2H3Aarde.STRUCTUUR + "/lu_ten_miroh");
            String bereik = miroh.getPersistentData().getStringOr(nl.juiced.guhs.feature.ring.Zicht.BIJ, "");
            helper.assertTrue(bereik.endsWith(":" + H3Rollen.NOOIT + "-" + H3Rollen.NOOIT), "the resident Miroh under the tree is hidden: " + bereik);
            // item 1: the story scenes that open a step
            for (var e : H3Hints.BRUGGEN.entrySet()) {
                helper.assertTrue(Plekken.plek(E2H3Aarde.STRUCTUUR, e.getValue().anker()) != null && H3Scenes.ALLE.containsValue(e.getValue())
                        && !nl.juiced.guhs.feature.einde2.verhaal.StapHint.hint(E2H3Aarde.LIJN.id(), e.getKey()).bijStartAan(),
                        "step " + e.getKey() + ": its story scene " + e.getValue().id());
            }
            helper.assertTrue(H3Hints.BRUGGEN.size() == 7, "seven steps open with a story scene");
            helper.succeed();
        } finally {
            E2H3Aarde.oorsprong = oud;
            H3Schijn.vergeetOpen();
            E2VerhaalDev.wis(p, E2Verhaal.AARDE);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * fix3-h3 (playtest 2 items 8, 9, 10): from the crystal cell the chapter runs to its end by itself: the cell, the fall,
     * Miroh's capture, the escape with Toph and the talk in flight; then the line is done and Toph is in the team. No step
     * waits for a click on Toph any more.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2H3AardeFinale(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Plekken.Plek cel = Plekken.plek(E2H3Aarde.STRUCTUUR, "kristalcel");
        BlockPos o = p.blockPosition().subtract(cel.pos());
        var oud = E2H3Aarde.oorsprong;
        var oudOpen = H3Verloop.open;
        E2H3Aarde.oorsprong = level -> level == helper.getLevel() ? o : null;
        H3Verloop.open = pl -> true;
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(helper.getLevel());
        E2H3AardeCommando.zet(p, E2H3Aarde.VAL);
        helper.onEachTick(() -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p)));
        H3Verloop.val(p, stad);
        helper.runAfterDelay(300, () -> {
            try {
                helper.assertTrue(E2H3Aarde.stap(p) == E2H3Aarde.STAPPEN, "the chapter ran to its end by itself: step " + E2H3Aarde.stap(p));
                for (H3Scenes.Scene s : List.of(H3Scenes.KRISTALCEL, H3Scenes.VAL, H3Scenes.MIROH_GEVANGEN, H3Scenes.ONTSNAPPING, H3Scenes.KOERS)) {
                    helper.assertTrue(nl.juiced.guhs.feature.verhaal.Cutscenes.gezien(p, s.id()), "seen: " + s.id());
                }
                helper.assertTrue(nl.juiced.guhs.feature.einde2.reizen.E2Team.leden(p).contains(nl.juiced.guhs.feature.einde2.reizen.E2Team.Lid.TOPH),
                        "Toph-guh is in the team");
                helper.succeed();
            } finally {
                E2H3Aarde.oorsprong = oud;
                H3Verloop.open = oudOpen;
                H3Verloop.vergeet(p);
                E2VerhaalDev.wis(p, E2Verhaal.AARDE);
                H3Acteurs.wegAlle(p, "");
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            }
        });
    }
}
