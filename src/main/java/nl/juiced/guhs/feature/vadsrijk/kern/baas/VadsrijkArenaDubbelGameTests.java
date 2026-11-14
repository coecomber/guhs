package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * 1.10.3 (live: "double spawns of bosses"; the server log showed "[dubbel] arena:bazen:opperzwam respawned, old copy at the centre
 * removed" right at a kill). After the arena's chunk unloads and loads again, the boss is a NEW object with the SAME uuid: the arena
 * found it by its remembered uuid but never gave it its arena again (only the other two search paths did). Beaten, it then did not
 * report the kill, the arena saw "no boss, no respawn time" and spawned a fresh one at once, next to the cheered-up one.
 */
public class VadsrijkArenaDubbelGameTests {
    private static final String B = "vadsrijk_arena_dubbel";
    private static final String ARENA = "test:arena_dubbel";

    @GuhTest(template = "empty", batch = B)
    public static void geenTweedeBaasNaHerladen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(2, 1, 2));
        Arena a = new Arena(ARENA, c, 6, c, 1200, c.above(4), "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(a, VadsrijkArenaDubbelGameTests::maak);
        Arenas.testStaat(ARENA, -1);   // (no saved respawn time from an earlier run)
        long[] nu = {sl.getGameTime() + 1000};
        Runnable tik = () -> {
            Arenas.tickEen(sl, ARENA, nu[0]);
            nu[0] += 5;
        };
        try {
            tik.run();
            List<Klodderreus> l = bazen(sl, c, false);
            helper.assertTrue(l.size() == 1, "the arena spawned its boss, found " + l.size());
            Klodderreus eerste = l.get(0);

            // the chunk unloads and loads again: the same boss (same uuid) comes back as a new object, without its arena
            CompoundTag tag = Nbt.saveWithoutId(eerste);
            eerste.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            Klodderreus terug = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.LOAD);
            Nbt.load(terug, tag);
            terug.setNoAi(true);
            helper.assertTrue(terug.getUUID().equals(eerste.getUUID()) && terug.arena() == null, "a reloaded copy: same uuid, no arena yet");
            sl.addFreshEntity(terug);
            tik.run();
            tik.run();
            helper.assertTrue(bazen(sl, c, false).size() == 1, "after the reload still one boss");
            helper.assertTrue(terug.arena() == a, "the arena gives the reloaded boss its arena again");

            // beaten: the arena knows it (respawn timer), no fresh boss next to the cheered-up one
            terug.verslagen(null);
            for (int i = 0; i < 5; i++) {
                tik.run();
            }
            helper.assertTrue(bazen(sl, c, false).isEmpty(), "no second boss right after the kill, found " + bazen(sl, c, false).size());
            helper.assertTrue(Arenas.respawnOp(ARENA) > nu[0], "the respawn timer runs: " + Arenas.respawnOp(ARENA) + " vs " + nu[0]);

            // a beaten copy that never reported (any other path): the arena waits for it and starts the timer itself
            Arenas.testStaat(ARENA, -1);
            for (Klodderreus k : bazen(sl, c, true)) {
                k.discard();
            }
            Klodderreus stil = maak(sl);
            stil.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            sl.addFreshEntity(stil);
            stil.verslagen(null);   // (no arena: no report)
            stil.arenaId = ARENA;
            tik.run();
            tik.run();
            helper.assertTrue(bazen(sl, c, false).isEmpty(), "a cheered-up copy of the arena is still there: no fresh boss yet");
            helper.assertTrue(Arenas.respawnOp(ARENA) > nu[0], "and its respawn timer started: " + Arenas.respawnOp(ARENA));
        } finally {
            for (Klodderreus k : bazen(sl, c, true)) {
                k.discard();
            }
            Arenas.afmelden(ARENA);
        }
        helper.succeed();
    }

    private static Klodderreus maak(ServerLevel sl) {
        Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.SPAWNER);
        if (k != null) {
            k.stelIn(3, Element.WATER, false, Set.of());
            k.setNoAi(true);
        }
        return k;
    }

    /** The bosses of the test arena near its centre ({@code ookGeplofd}: also the cheered-up ones). */
    private static List<Klodderreus> bazen(ServerLevel sl, BlockPos c, boolean ookGeplofd) {
        return sl.getEntitiesOfClass(Klodderreus.class, new AABB(c).inflate(16),
                k -> !k.isRemoved() && (ookGeplofd || !k.geplofd()) && (k.arenaId.isEmpty() || ARENA.equals(k.arenaId)));
    }
}
