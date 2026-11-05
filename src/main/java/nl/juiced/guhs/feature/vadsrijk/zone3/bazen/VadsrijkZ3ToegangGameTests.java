package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Brokkel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Taleggio;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * (M-BAZEN) The playtest fixes: arenas never share space and a boss only greets / fights a player who is not mid-fight with another
 * boss and whose dungeon predecessor is beaten (the Kaaszeppelinwerf intro theft); the Hart gate hint + the order opened for a dev
 * spawn; Vahoegnir's dev tp, his return to the nest and the reset. Run: {@code runGameTestServer -Pgt=VadsrijkZ3Toegang}.
 */
public class VadsrijkZ3ToegangGameTests {
    private static final String BATCH = "vadsrijk_z3_toegang";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar, boolean creatief) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(creatief ? GameType.CREATIVE : GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
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

    // ---- arenas: disjoint, whatever the map does ------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_indeling", timeoutTicks = 40)
    public static void arenasDelenGeenRuimte(GameTestHelper helper) {
        List<Arena> l = new ArrayList<>(Z3BazenRegistry.ARENAS.values());
        helper.assertTrue(l.size() == 9, "9 zone-3 arenas (" + l.size() + ")");
        for (int i = 0; i < l.size(); i++) {
            for (int j = i + 1; j < l.size(); j++) {
                helper.assertTrue(!l.get(i).overlapt(l.get(j)), l.get(i).id() + " and " + l.get(j).id() + " overlap");
            }
            Arena a = l.get(i);
            helper.assertTrue(a.equals(Arenas.van(a.id())), "kern has the laid-out arena " + a.id());
            // the boss's own floor centre is in its own arena and in no other zone-3 arena
            BlockPos c = a.centrum();
            for (Arena o : l) {
                helper.assertTrue(o.binnen(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) == (o == a), a.id() + "'s floor lies in " + o.id());
            }
        }
        // the werf decks: Roquefort's wreck stays inside his arena
        Arena roq = Z3BazenRegistry.ARENAS.get(Z3BazenRegistry.ARENA_ROQUEFORT);
        BlockPos w = Z3BazenPlekken.ROQUEFORT_WRAK;
        helper.assertTrue(roq.binnen(w.getX() + 0.5, w.getY() + 0.1, w.getZ() + 0.5), "the wreck is in Roquefort's arena");
        // the layout rule itself (as after a compact revamp): stacked = split by height, one floor = shrunk with a gap
        BlockPos o = BlockPos.ZERO;
        Arena a = new Arena("t:a", o, 15, o, 1, o, "x").met(15, 6, 14), b = new Arena("t:b", o.offset(20, 15, 0), 15, o, 1, o, "x").met(15, 6, 14),
                c = new Arena("t:c", o.offset(0, 0, 24), 15, o, 1, o, "x").met(15, 6, 14);
        List<Arena> r = Z3BazenPlekken.indeel(List.of(a, b, c));
        for (int i = 0; i < r.size(); i++) {
            for (int j = i + 1; j < r.size(); j++) {
                helper.assertTrue(!r.get(i).overlapt(r.get(j)), "laid out: " + r.get(i) + " / " + r.get(j));
            }
        }
        helper.assertTrue(r.get(1).straal() == 15 && r.get(1).onder() <= 8, "the stacked arena keeps its radius, splits by height (" + r.get(1) + ")");
        helper.assertTrue(r.get(2).straal() < 15 && r.get(2).straal() >= Z3BazenPlekken.MIN_STRAAL, "the one-floor neighbour shrinks (" + r.get(2) + ")");
        helper.succeed();
    }

    // ---- one fight at a time + the dungeon order (the werf intro theft) ------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_intro", timeoutTicks = 100)
    public static void introAlleenVoorJouwGevecht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 30);
        Taleggio t = zet(sl, Z3BazenRegistry.TALEGGIO.get(), c, 38);
        Brokkel b = zet(sl, Z3BazenRegistry.BROKKEL.get(), c.offset(20, 0, 0), 39);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(10, 0, 0), false);   // in both loose fight areas (r 24)
        Predicate<ServerPlayer> alleenP = q -> q == p;
        t.testFilter = alleenP;
        b.testFilter = alleenP;
        Statussen.geef(t, Status.VERDOOFD, 400, 1, null);
        Statussen.geef(b, Status.VERDOOFD, 400, 1, null);
        Z3Toegang.vergeet(p);
        helper.assertTrue(t.magIntro(p) && b.magIntro(p), "nobody fights yet: both may greet");
        Z3Toegang.start(p, t);   // what Taleggio's intro does
        helper.assertTrue(Z3Toegang.actief(p) == t, "the player is in Taleggio's fight");
        helper.assertTrue(!b.magIntro(p), "Brokkel may NOT play his intro mid-fight with Taleggio");
        helper.assertTrue(!b.zDoetMee(p) && !b.strijders().contains(p), "Brokkel does not target the player");
        helper.assertTrue(t.zDoetMee(p) && t.magIntro(p), "Taleggio still fights the player");
        double v = b.vads();
        b.krijgSchade(300, p);
        helper.assertTrue(b.vads() == v, "the player can't hit Brokkel meanwhile");
        // Taleggio beaten -> free again; then the dungeon order: Brokkel (arena id) waits for Taleggio
        t.discard();
        helper.assertTrue(Z3Toegang.actief(p) == null, "Taleggio gone: no active fight");
        b.arenaId = Z3BazenRegistry.ARENA_BROKKEL;
        Arena ta = Arenas.van(Z3BazenRegistry.ARENA_TALEGGIO);
        if (ta == null || !sl.isLoaded(ta.centrum())) {   // (the real werf is not in this test level)
            helper.assertTrue(!b.magIntro(p) && !b.zDoetMee(p), "Brokkel stays asleep until Taleggio is beaten (survival)");
        }
        Z3Toegang.verslagen(p, Z3BazenRegistry.ARENA_TALEGGIO, sl.getGameTime());
        helper.assertTrue(b.magIntro(p) && b.zDoetMee(p), "Taleggio beaten in this run: Brokkel wakes");
        Z3Toegang.vergeet(p);
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(b.magIntro(p), "creative skips the order");
        p.setGameMode(GameType.SURVIVAL);
        Z3Toegang.geefVrij(p, Z3BazenRegistry.ARENA_ROQUEFORT);
        helper.assertTrue(Z3Toegang.vrijgegeven(p, Z3BazenRegistry.ARENA_ROQUEFORT) && b.magIntro(p), "the dev tp/spawn opens the whole order");
        b.discard();
        Z3Toegang.vergeet(p);
        helper.succeed();
    }

    // ---- the Hart: the gate hint, the order opened for a dev spawn ------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_hart", timeoutTicks = 40)
    public static void hartPoortHintEnSpawnVolgorde(GameTestHelper helper) {
        BlockPos g = Z3BazenPlekken.HART_INGANG;
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(g).add(2, 0, 1), false);
        var oud = Z3BazenApi.HART_VERHAAL;
        try {
            Z3Kerkers.vergeetHartHint(p);
            Z3BazenApi.HART_VERHAAL = q -> false;
            helper.assertTrue(Z3Kerkers.hartPoort(p), "at the gate without the story: the hint shows");
            helper.assertTrue(!Z3Kerkers.hartPoort(p), "not again right away (5 min)");
            Z3Kerkers.vergeetHartHint(p);
            Z3BazenApi.HART_VERHAAL = q -> true;
            helper.assertTrue(Z3Kerkers.hartPoort(p), "with the story: the route hint still shows");
            Z3Kerkers.vergeetHartHint(p);
            p.snapTo(g.getX() + 30.5, g.getY(), g.getZ() + 0.5);
            helper.assertTrue(!Z3Kerkers.hartPoort(p), "away from the gate: nothing");
        } finally {
            Z3BazenApi.HART_VERHAAL = oud;
        }
        // the Hart bosses are registered (not gated) and a dev spawn of the last one opens the order before it
        for (String id : Z3Toegang.VOLGORDE.get("hart")) {
            helper.assertTrue(Arenas.van(id) != null && Z3BazenRegistry.ARENAS.containsKey(id), "arena " + id + " registered");
        }
        Z3Toegang.vergeet(p);
        Arena wurger = Arenas.van(Z3BazenRegistry.ARENA_WURGER);
        if (!helper.getLevel().isLoaded(wurger.centrum())) {
            helper.assertTrue(!Z3Toegang.vrijgegeven(p, Z3BazenRegistry.ARENA_WACHTER), "fresh player: the Nestwachter waits for the Wortelwurger");
        }
        Z3Toegang.geefVrij(p, Z3BazenRegistry.ARENA_BLEU);
        helper.assertTrue(Z3Toegang.vrijgegeven(p, Z3BazenRegistry.ARENA_WACHTER) && Z3Toegang.vrijgegeven(p, Z3BazenRegistry.ARENA_BLEU),
                "spawn bleu opens Nestwachter + Bleu");
        helper.assertTrue(Z3BazenRegistry.BAZEN_DEV.containsAll(List.of("wortelwurger", "nestwachter", "bleu", "vahoegnir")), "spawn knows the Hart bosses");
        Z3Toegang.vergeet(p);
        helper.succeed();
    }

    // ---- Vahoegnir: dev tp when he is there, back to the nest when lost, reset --------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_vahoegnir", timeoutTicks = 400)
    public static void vahoegnirDevTpTerugEnReset(GameTestHelper helper) {
        ServerLevel thuis = helper.getLevel();
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))), true);
        ServerLevel vr = Vadsrijk.level(thuis.getServer());
        if (vr == null) {   // (no Vadsrijk dimension on this test server) an orphan in this level is still found and cleaned up
            BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
            vloer(thuis, c, 6);
            Vahoegnir v = zet(thuis, Z3BazenRegistry.VAHOEGNIR.get(), c, 50);
            helper.assertTrue(Z3BazenApi.alleVahoegnirs(thuis).contains(v), "the orphan is found");
            helper.assertTrue(Z3BazenApi.resetVahoegnir(thuis) >= 1 && v.isRemoved(), "reset removes it");
            helper.succeed();
            return;
        }
        BlockPos nest = Z3BazenPlekken.NEST_CENTRUM;
        for (int cx = (nest.getX() >> 4) - 2; cx <= (nest.getX() >> 4) + 2; cx++) {
            for (int cz = (nest.getZ() >> 4) - 2; cz <= (nest.getZ() >> 4) + 2; cz++) {
                vr.setChunkForced(cx, cz, true);
            }
        }
        Z3BazenApi.resetVahoegnir(vr);
        String s1 = Z3BazenApi.devVahoegnir(p, true);
        helper.assertTrue(s1.contains("herinnering") || s1.startsWith("Vahoegnir landt"), "he is called: " + s1);
        helper.assertTrue(p.level() == vr && p.blockPosition().distSqr(Z3BazenPlekken.NEST_DEUR) < 4, "the player stands at the nest edge");
        helper.runAfterDelay(40, () -> {
            List<Vahoegnir> l = Z3BazenApi.alleVahoegnirs(vr);
            helper.assertTrue(l.size() == 1, "one Vahoegnir landed (" + l.size() + ")");
            Vahoegnir v = l.get(0);
            double dx = v.getX() - nest.getX(), dz = v.getZ() - nest.getZ();
            helper.assertTrue(dx * dx + dz * dz < Math.pow(Z3BazenPlekken.NEST_STRAAL, 2) && Math.abs(v.getY() - nest.getY()) < 20,
                    "he is in the nest arena (" + v.blockPosition() + ")");
            p.snapTo(p.getX() + 300, p.getY(), p.getZ());   // somewhere else
            String s2 = Z3BazenApi.devVahoegnir(p, false);
            helper.assertTrue(s2.startsWith("Hij is er al") && s2.contains("Vahoegnir @"), "a second call says where he is: " + s2);
            helper.assertTrue(p.blockPosition().distSqr(Z3BazenPlekken.NEST_DEUR) < 4, "... and takes the player to him");
            helper.assertTrue(Z3BazenApi.alleVahoegnirs(vr).size() == 1, "still only one");
            v.snapTo(v.getX(), nest.getY() - 36, v.getZ());   // fallen out of the tree
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(v.teruggezet >= 1 && Math.abs(v.getY() - nest.getY()) < 20, "he is back at the nest (" + v.blockPosition() + ")");
                helper.assertTrue(Z3BazenApi.resetVahoegnir(vr) == 1 && Z3BazenApi.alleVahoegnirs(vr).isEmpty(), "reset cleans him up");
                helper.assertTrue(Arenas.baas(vr, Z3BazenRegistry.ARENA_NEST) == null, "the nest is empty");
                p.teleportTo(thuis, p.getX(), 0, p.getZ(), Set.<Relative>of(), 0, 0, true);
                for (int cx = (nest.getX() >> 4) - 2; cx <= (nest.getX() >> 4) + 2; cx++) {
                    for (int cz = (nest.getZ() >> 4) - 2; cz <= (nest.getZ() >> 4) + 2; cz++) {
                        vr.setChunkForced(cx, cz, false);
                    }
                }
                helper.succeed();
            });
        });
    }
}
