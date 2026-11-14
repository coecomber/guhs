package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.1, quest bosses for several players (audit of zones 1-6: one shared arena boss with a respawn of 5-20 minutes blocked
 * everyone who came just after someone else's kill):
 * <ul>
 * <li>everyone inside the arena at the defeat gets the quest credit (not only who hit it), but no xp/loot;</li>
 * <li>a player who was NOT at that kill and walks into the arena makes the boss come back within
 * {@link Arenas#NIEUWKOMER_SECONDEN}; those who were there (farming) still wait the full time.</li>
 * </ul>
 */
public class VadsrijkArenaSamenGameTests {
    private static final String B = "vadsrijk_arena_samen";
    private static final String ARENA = "test:arena_samen";

    @GuhTest(template = "empty", batch = B)
    public static void kijkersTellenEnNieuwkomerSnel(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(2, 1, 2));
        Arena a = new Arena(ARENA, c, 6, c, 1200, c.above(4), "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(a, x -> null);
        try {
            Quest q = Quests.van("hq05_klodderreus");
            ServerPlayer slaat = GuhMockPlayer.of(helper), kijkt = GuhMockPlayer.of(helper), later = GuhMockPlayer.of(helper);
            for (ServerPlayer p : new ServerPlayer[] {slaat, kijkt, later}) {
                Questlog.wisAlles(p);
                Questlog.neemAan(p, q);
            }
            slaat.snapTo(c.getX() + 2.5, c.getY(), c.getZ() + 0.5);
            kijkt.snapTo(c.getX() - 2.5, c.getY(), c.getZ() + 0.5);
            later.snapTo(c.getX() + 30.5, c.getY(), c.getZ() + 0.5);   // (outside the arena)
            Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.SPAWNER);
            k.stelIn(3, Element.WATER, false, Set.of());
            k.setNoAi(true);
            k.zetArena(a);
            k.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            sl.addFreshEntity(k);
            helper.assertTrue(Arenas.baas(sl, ARENA) == k, "the arena knows its boss");
            k.verslagen(slaat);
            int baas = -1;
            for (int i = 0; i < q.doelen().size(); i++) {
                if (q.doelen().get(i).type() == Quest.Doel.Type.BAAS) {
                    baas = i;
                }
            }
            helper.assertTrue(Questlog.doelKlaar(slaat, q, baas), "the hitter got the boss step");
            helper.assertTrue(Questlog.doelKlaar(kijkt, q, baas), "the onlooker inside the arena got it too");
            helper.assertTrue(!Questlog.doelKlaar(later, q, baas), "someone outside did not");
            helper.assertTrue(!Arenas.nieuwkomerBinnen(sl, ARENA), "only players of the kill inside: the full respawn time stays");
            later.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 3.5);
            helper.assertTrue(Arenas.nieuwkomerBinnen(sl, ARENA), "a newcomer inside: the boss comes back soon");
            k.discard();
        } finally {
            Arenas.afmelden(ARENA);
        }
        helper.succeed();
    }
}
