package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * 1.10.3 (live: "the respawn timers in front of the boss arenas are nowhere to be seen"): the arena board. Kern ticks the arenas
 * every 5 SERVER ticks (tick count, 0 at every start), the board only updated when the GAME time (saved, keeps counting) was a
 * multiple of 20: after a restart where the two were not in step (4 in 5 starts) the board was never made. Here the game time is
 * deliberately out of step (nu % 5 == 3), and the board must also survive a chunk unload, a stale reference (a display that
 * never got into the level), a lost reference (no second board) and show "is wakker" with a boss.
 */
public class VadsrijkArenaBordGameTests {
    private static final String B = "vadsrijk_arena_bord";
    private static final String ARENA = "test:arena_bord";

    @GuhTest(template = "empty", batch = B)
    public static void bordStaatOokUitDePas(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos bord = c.offset(3, 0, 0);
        Arena a = new Arena(ARENA, c, 6, c, 1200, bord, "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(a, x -> null);
        Klodderreus k = null;
        try {
            long[] nu = {(sl.getGameTime() / 20) * 20 + 1003};   // (game time 3 ticks out of step with the 5-tick rhythm)
            Runnable seconde = () -> {
                for (int i = 0; i < 4; i++) {
                    Arenas.tickEen(sl, ARENA, nu[0]);
                    nu[0] += 5;
                }
            };
            seconde.run();
            List<Display.TextDisplay> l = borden(sl, bord);
            helper.assertTrue(l.size() == 1, "one board over the arena's board spot (out-of-step game time), found " + l.size());
            Display.TextDisplay eerste = l.get(0);
            helper.assertTrue(tekst(eerste).contains("vadsrijk.kern.arena.terug"), "the board counts down: " + tekst(eerste));
            helper.assertTrue(Math.abs(eerste.getY() - (bord.getY() + 1.6)) < 0.01, "the board hangs 1.6 over the spot");

            // the board's chunk unloads (the display is saved away, refused on reload): the board comes back
            eerste.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            seconde.run();
            l = borden(sl, bord);
            helper.assertTrue(l.size() == 1 && l.get(0) != eerste, "after an unload: a new board, found " + l.size());

            // a stale reference: a display that never made it into the level is not "the board"
            Display.TextDisplay nooit = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
            Arenas.zetBord(ARENA, nooit);
            seconde.run();
            l = borden(sl, bord);
            Display.TextDisplay nu1 = Arenas.bordVan(ARENA);
            helper.assertTrue(l.size() == 1, "a stale reference: still exactly one board in the level, found " + l.size());
            helper.assertTrue(nu1 != nooit && nu1 != null && sl.getEntity(nu1.getUUID()) == nu1, "the arena holds the board in the level");

            // a lost reference: the board is still there, no second one is made
            Arenas.zetBord(ARENA, null);
            seconde.run();
            l = borden(sl, bord);
            helper.assertTrue(l.size() == 1 && l.get(0) == nu1, "a lost reference: the board is found again, found " + l.size());

            // a boss in the arena: "is wakker"
            k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.SPAWNER);
            k.stelIn(3, Element.WATER, false, Set.of());
            k.setNoAi(true);
            k.zetArena(a);
            k.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            sl.addFreshEntity(k);
            seconde.run();
            l = borden(sl, bord);
            helper.assertTrue(l.size() == 1 && tekst(l.get(0)).contains("vadsrijk.kern.arena.wakker"), "with a boss the board says it is awake");
        } finally {
            if (k != null) {
                k.discard();
            }
            for (Display.TextDisplay d : borden(sl, bord)) {
                d.discard();
            }
            Arenas.afmelden(ARENA);
        }
        helper.succeed();
    }

    private static List<Display.TextDisplay> borden(ServerLevel sl, BlockPos bord) {
        return sl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(bord).inflate(4),
                d -> !d.isRemoved() && d.entityTags().contains(Getallen.TAG));
    }

    private static String tekst(Display.TextDisplay d) {
        var t = Nbt.saveWithoutId(d).get("text");
        return t == null ? "" : t.toString();
    }
}
