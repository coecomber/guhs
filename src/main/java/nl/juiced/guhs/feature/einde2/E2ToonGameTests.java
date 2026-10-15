package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.Wezens;
import nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game test of {@code /guhs einde2 toon <groep>} (CONTRACT_E2 11.9): every group spawns without an error, everything of it
 * stands there NoAI-still and invulnerable (every kind and look, every crowd set, every creature in every look), and
 * {@code weg} leaves nothing. The chunks of the rows are kept loaded while the test runs.
 */
public class E2ToonGameTests {
    private static final String BATCH = "einde2_toon";

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void einde2Toon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        List<ChunkPos> vast = new ArrayList<>();
        for (int cx = (at.getX() - 80) >> 4; cx <= (at.getX() + 80) >> 4; cx++) {
            for (int cz = (at.getZ() - 8) >> 4; cz <= (at.getZ() + 240) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    vast.add(new ChunkPos(cx, cz));
                }
            }
        }
        // (merge_main) a forced chunk's entities load a tick or more later, and an entity added before that is not in
        // getAllEntities yet: wait until the whole area has its entities, else weg misses the far rows of "alles"
        net.minecraft.world.phys.AABB gebied = new net.minecraft.world.phys.AABB(at.getX() - 80, at.getY(), at.getZ() - 8, at.getX() + 80, at.getY() + 1, at.getZ() + 240);
        wachtTot(helper, () -> Bewoners.geladen(level, gebied), 300, () -> toon(helper, level, at, vast));
    }

    /** Runs {@code dan} as soon as {@code klaar} holds (checked every tick), or fails after {@code ticks}. */
    private static void wachtTot(GameTestHelper helper, java.util.function.BooleanSupplier klaar, int ticks, Runnable dan) {
        if (klaar.getAsBoolean()) {
            dan.run();
        } else if (ticks <= 0) {
            helper.fail("the forced chunks of the rows never loaded their entities");
        } else {
            helper.runAfterDelay(1, () -> wachtTot(helper, klaar, ticks - 1, dan));
        }
    }

    private static void toon(GameTestHelper helper, ServerLevel level, BlockPos at, List<ChunkPos> vast) {
        // the expected sizes, counted from the tables
        int team = CastTabellen.kinds(CastTabellen.Groep.TEAM).size();
        int volk = 0, schurk = 0, sponsguh = 1;
        for (GuhNpcEntity.Kind k : CastTabellen.kinds(CastTabellen.Groep.VOLK)) {
            volk += Math.max(1, CastTabellen.mix(k));
        }
        for (GuhNpcEntity.Kind k : CastTabellen.kinds(CastTabellen.Groep.SCHURK)) {
            schurk += Math.max(1, CastTabellen.mix(k));
        }
        int wezens = 0;
        for (Wezens w : Wezens.values()) {
            wezens += Math.max(1, w.soorten);
        }
        schurk += wezens;
        int creaturen = wezens;
        for (Cast c : Cast.values()) {
            sponsguh += Math.max(1, c.soorten);
        }
        Map<String, Integer> verwacht = new HashMap<>();
        verwacht.put("team", team);
        verwacht.put("volk", volk);
        verwacht.put("schurk", schurk);
        verwacht.put("sponsguh", sponsguh);
        verwacht.put("wezens", Einde2Toon.WEZENS.size() + Einde2Toon.BIOMEGUHS.size() + 1);
        verwacht.put("geest", nl.juiced.guhs.feature.einde2.geestwezens.GeestToon.aantal());   // (geestmodellen)
        for (String groep : Einde2Toon.GROEPEN) {
            int n = Einde2Toon.toon(level, at, Direction.SOUTH, List.of(groep));
            helper.assertTrue(n == verwacht.get(groep) && n == Einde2Toon.aantal(List.of(groep)), groep + ": everything stands: " + n + " of " + verwacht.get(groep));
            List<Entity> rij = rij(level);
            helper.assertTrue(rij.size() == n, groep + ": all of them are there: " + rij.size());
            for (Entity e : rij) {
                helper.assertTrue(e.isInvulnerable() && (!(e instanceof Mob m) || m.isNoAi()), groep + ": still and invulnerable: " + e);
            }
            if (groep.equals("volk")) {
                for (GuhNpcEntity.Kind kind : CastTabellen.kinds(CastTabellen.Groep.VOLK)) {
                    int mix = CastTabellen.mix(kind);
                    if (mix > 0) {
                        Set<Integer> sets = new HashSet<>();
                        for (Entity e : rij) {
                            if (e instanceof GuhNpcEntity npc && npc.getKind() == kind) {
                                sets.add(E2CastVolkFeature.mixSet(npc.blockPosition(), mix));
                            }
                        }
                        helper.assertTrue(sets.size() == mix, kind + ": every set is shown: " + sets);
                    }
                }
            }
            if (groep.equals("schurk")) {
                Set<String> looks = new HashSet<>();
                for (Entity e : rij) {
                    if (e instanceof CastWezen w) {
                        looks.add(w.wie() + w.soort());
                    }
                }
                helper.assertTrue(looks.size() == creaturen, "every creature in every look: " + looks.size());
            }
            int weg = Einde2Toon.weg(level);
            helper.assertTrue(weg == n, groep + ": weg takes them all: " + weg);
        }
        int alles = Einde2Toon.toon(level, at, Direction.SOUTH, Einde2Toon.GROEPEN);
        helper.assertTrue(alles == Einde2Toon.aantal(Einde2Toon.GROEPEN), "alles: " + alles);
        int wegAlles = Einde2Toon.weg(level);
        helper.assertTrue(wegAlles == alles, "weg after alles: " + wegAlles + " of " + alles);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(rij(level).isEmpty(), "nothing of the rows is left");
            for (ChunkPos c : vast) {
                level.setChunkForced(c.x(), c.z(), false);
            }
            helper.succeed();
        });
    }

    private static List<Entity> rij(ServerLevel level) {
        List<Entity> uit = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e.isAlive() && e.entityTags().contains(Einde2Toon.TAG)) {
                uit.add(e);
            }
        }
        return uit;
    }
}
