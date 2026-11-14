package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Zwamtempel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Fix round 1 (bazen): the bosses really fight. A player standing still at range must become the target, the boss must come
 * at them and hurt them (without a one-shot); hits from outside the fight do nothing; the temple door gate.
 * Run: {@code runGameTestServer -Pgt=VadsrijkBazenGevecht}.
 */
public class VadsrijkBazenGevechtTests {
    private static final String BATCH = "vadsrijk_bazen_gevecht";

    /** A 41x41 stone floor around the test origin, air above. */
    private static void vloer(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                sl.setBlock(o.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 9; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static ServerPlayer speler(GameTestHelper helper, int dx) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        p.snapTo(o.getX() + 0.5 + dx, o.getY() + 1, o.getZ() + 0.5);
        return p;
    }

    private static <T extends VadsrijkBaas> T zet(GameTestHelper helper, T b, int level, Element e) {
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        b.snapTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5, 0, 0);
        b.stelIn(level, e, false, Set.of());
        helper.getLevel().addFreshEntity(b);
        return b;
    }

    /** The player stands still {@code dx} blocks away for 18 s: the boss must pick them, come at them and hurt them. */
    private static void vecht(GameTestHelper helper, VadsrijkBaas b, ServerPlayer p, String naam) {
        b.testFilter = x -> x == p;
        Vec3 start = b.position(), plek = p.position();
        double[] verst = {0}, dichtst = {Double.MAX_VALUE}, grootsteKlap = {0}, vorige = {Spelers.vads(p)};
        boolean[] doel = {false};
        double vads0 = Spelers.vads(p);
        int[] geraakt = {0};
        for (int t = 1; t <= 360; t++) {
            helper.runAfterDelay(t, () -> {
                if (!nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.beschermd(p)) {
                    p.snapTo(plek.x, plek.y, plek.z);   // (the mock stands still at range; a faint sends it away, then it's back)
                }
                doel[0] |= b.doel() == p;
                verst[0] = Math.max(verst[0], b.position().distanceTo(start));
                dichtst[0] = Math.min(dichtst[0], b.position().distanceTo(plek));
                double v = Spelers.vads(p);
                if (v < vorige[0]) {
                    grootsteKlap[0] = Math.max(grootsteKlap[0], vorige[0] - v);
                    geraakt[0]++;
                }
                vorige[0] = v;
            });
        }
        helper.runAfterDelay(370, () -> {
            System.out.println("[gevecht] " + naam + ": moved " + (int) verst[0] + ", closest " + (int) dichtst[0] + ", hits " + geraakt[0] + ", biggest hit "
                    + (int) grootsteKlap[0] + " of " + (int) vads0 + " vads");
            helper.assertTrue(doel[0], naam + ": the player at range became the target");
            helper.assertTrue(verst[0] > 4, naam + ": he moved (" + verst[0] + ")");
            helper.assertTrue(dichtst[0] < 7, naam + ": he came at the player at range (closest " + dichtst[0] + ")");
            helper.assertTrue(geraakt[0] > 0, naam + ": he hurt the player standing still");
            helper.assertTrue(grootsteKlap[0] < vads0, naam + ": but never a one-shot (" + grootsteKlap[0] + " of " + vads0 + ")");
            b.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void klodderreusVecht(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer p = speler(helper, 14);
        Spelers.zetLevel(p, 3);
        VadsrijkBaas b = zet(helper, KernRegistry.KLODDERREUS.get().create(helper.getLevel(), EntitySpawnReason.COMMAND), 3, Element.WATER);
        vecht(helper, b, p, "klodderreus");
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void koningVecht(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer p = speler(helper, 16);
        Spelers.zetLevel(p, 8);
        VadsrijkBaas b = zet(helper, BazenRegistry.KONING_KLODDER.get().create(helper.getLevel(), EntitySpawnReason.COMMAND), 8, Element.WATER);
        vecht(helper, b, p, "koning");
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void opperzwamVecht(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer p = speler(helper, 14);
        Spelers.zetLevel(p, 18);
        VadsrijkBaas b = zet(helper, BazenRegistry.OPPERZWAM.get().create(helper.getLevel(), EntitySpawnReason.COMMAND), 19, Element.NATUUR);
        vecht(helper, b, p, "opperzwam");
    }

    /** A creative player still makes him act (he jumps at them), and a hit from outside the fight does nothing. */
    @GuhTest(template = "empty", batch = BATCH + "_creatief", timeoutTicks = 200)   // (own batch: no other test's mock players nearby)
    public static void creatiefEnBuitenArena(GameTestHelper helper) {
        vloer(helper);
        ServerPlayer c = GuhMockPlayer.of(helper);
        c.setGameMode(GameType.CREATIVE);
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        c.snapTo(o.getX() + 12.5, o.getY() + 1, o.getZ() + 0.5);
        VadsrijkBaas b = zet(helper, KernRegistry.KLODDERREUS.get().create(helper.getLevel(), EntitySpawnReason.COMMAND), 3, Element.WATER);
        ServerPlayer ver = speler(helper, 0);
        b.testFilter = x -> x == c || x == ver;
        ver.snapTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 40.5);   // (40 blocks: outside the 24 of a boss without arena)
        Vec3 start = b.position();
        boolean[] doelC = {false};
        for (int t = 1; t < 150; t++) {
            helper.runAfterDelay(t, () -> {
                doelC[0] |= b.doel() == c;
                if (b.tickCount % 20 == 0) {
                    System.out.println("[gevecht] creatief: doel=" + (b.doel() == null ? "-" : b.doel() == c ? "c" : "ander") + " c=" + c.position() + " creatief="
                            + c.isCreative() + " vechters=" + b.vechters().size() + " boss=" + b.position());
                }
            });
        }
        helper.runAfterDelay(5, () -> {
            double v = b.vads();
            Schade.doe(new Schade.Klap(ver, b, 50, Element.GEEN, Schade.Soort.AFSTAND, false, "test"));
            helper.assertTrue(b.vads() == v, "a hit from 40 blocks away does nothing (" + v + " -> " + b.vads() + ")");
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(doelC[0], "the creative player became his target (nobody else in the fight)");
            helper.assertTrue(b.position().distanceTo(start) > 4, "and he jumped at them (" + b.position().distanceTo(start) + ")");
            b.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void tempeldeurPoort(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Spelers.zetLevel(p, 10);
        helper.assertTrue("vadsrijk.bazen.tempel.te_laag".equals(Zwamtempel.waaromDicht(p)), "level 10: too low");
        helper.assertTrue(!Zwamtempel.MAG_NAAR_BINNEN.test(p), "level 10 may not open the temple");
        Spelers.zetLevel(p, 16);
        helper.assertTrue("vadsrijk.bazen.tempel.verhaal".equals(Zwamtempel.waaromDicht(p)), "level 16 without the temple quest: the story first ("
                + Zwamtempel.waaromDicht(p) + ")");
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(!Zwamtempel.MAG_NAAR_BINNEN.test(p), "creative is no exception");
        p.setGameMode(GameType.SURVIVAL);
        Spelers.zetLevel(p, Zwamtempel.TEMPEL_ALTIJD_LEVEL);
        helper.assertTrue(Zwamtempel.waaromDicht(p) == null && Zwamtempel.MAG_NAAR_BINNEN.test(p), "level 18: a click always opens it (g-ux rule)");
        Spelers.zetLevel(p, 17);
        helper.assertTrue("vadsrijk.bazen.tempel.verhaal".equals(Zwamtempel.waaromDicht(p)), "level 17 without the quest: not yet");
        helper.assertTrue(Zwamtempel.bijDeur(BazenPlekken.TEMPELDEUR_MIN) && Zwamtempel.bijDeur(BazenPlekken.TEMPELDEUR_VOOR)
                && !Zwamtempel.bijDeur(BazenPlekken.TEMPELDEUR_VOOR.south(12)), "the door area");
        helper.succeed();
    }

    /**
     * 1.10.2 (live bug 1.10.1): the big temple door did not open on hq11's "click the big door". The ceremony opened wereld's
     * moving door, which slides shut by itself after 30 s, while the temple kept its own 5-minute "open" window (and kept it for
     * ever while anyone stood within 12 blocks): a click in that window only said "already open". Now a click (or walking up)
     * opens the shut door again, the quest step counts, and a shut door never keeps the window open.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tempeldeurGaatWeerOpen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Zwamtempel.deurVergeet(sl);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Spelers.zetLevel(p, Zwamtempel.TEMPEL_ALTIJD_LEVEL);          // hq11 is offered from level 18 (its own lv)
        nl.juiced.guhs.feature.vadsrijk.npc.Quest hq11 = nl.juiced.guhs.feature.vadsrijk.npc.Quests.van(Zwamtempel.QUEST_OPPERZWAM);
        helper.assertTrue(hq11 != null && hq11.lv() <= Zwamtempel.TEMPEL_ALTIJD_LEVEL, "hq11 never needs more than the always-open level");
        nl.juiced.guhs.feature.vadsrijk.npc.Questlog.neemAan(p, hq11);
        BlockPos voor = BazenPlekken.TEMPELDEUR_VOOR;
        p.snapTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 40.5);   // (far from the door: no scene for the mock player)
        long nu = sl.getGameTime();
        // 1. someone opened it earlier (the ceremony), then wereld's door slid shut by itself after 30 s
        Zwamtempel.ceremonie(sl, nu);
        helper.assertTrue(Zwamtempel.deurStaatOpen(), "the ceremony opens the door");
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren.sluit(sl, "zwamtempel");
        helper.assertTrue(!Zwamtempel.deurStaatOpen() && Zwamtempel.deurOpenTot() > 0, "the bug state: shut, but the window still open");
        // 2. the player clicks the big door within the window: it must open again and the quest step must count
        helper.assertTrue(Zwamtempel.klikDeurIn(sl, p, BazenPlekken.TEMPELDEUR_MIN.offset(6, 2, 0)), "the click is handled");
        helper.assertTrue(Zwamtempel.deurStaatOpen(), "a click on the shut door opens it again");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.npc.Questlog.doelKlaar(p, hq11, 0), "hq11 step 1 (click the big door) counts");
        // 3. shut again; the player walks up (within 6 blocks): the door opens again without a click
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren.sluit(sl, "zwamtempel");
        p.snapTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 3.5);
        Zwamtempel.deur(sl, nu + 20 * 60);
        helper.assertTrue(Zwamtempel.deurStaatOpen(), "walking up to the shut door opens it again");
        // 4. the window is over, the door shut by itself, someone waits 10 blocks away: the window must end (no stuck state)
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren.sluit(sl, "zwamtempel");
        p.snapTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 10.5);
        Zwamtempel.deur(sl, Zwamtempel.deurOpenTot() + 1);
        helper.assertTrue(Zwamtempel.deurOpenTot() < 0, "a shut door never keeps the open window alive");
        // 5. and then a click is the full ceremony again
        p.snapTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 40.5);
        Zwamtempel.klikDeurIn(sl, p, BazenPlekken.TEMPELDEUR_MIN.offset(6, 2, 0));
        helper.assertTrue(Zwamtempel.deurStaatOpen() && Zwamtempel.deurOpenTot() > 0, "a later click opens it with the ceremony");
        Zwamtempel.deurVergeet(sl);
        nl.juiced.guhs.feature.vadsrijk.npc.Questlog.wisAlles(p);
        helper.succeed();
    }
}
