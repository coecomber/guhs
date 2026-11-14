package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.TikWacht;

/**
 * 1.10.2 (the live crash of 2026-11-14 15:27 in {@code Sfeer.tick}): the tick guards. {@link TikWacht#alle} drops nulls and lets you
 * discard while walking; {@link TikWacht#veilig} logs a throwing hook and keeps going; {@code Sfeer.tick} survives its cleanup tick
 * with tagged critters next to a player-less spot. Run: {@code runGameTestServer -Pgt=VadsrijkTikWacht}.
 */
public class VadsrijkTikWachtGameTests {
    private static final String BATCH = "vadsrijk_tikwacht";

    /** A null in the entity walk (what 26.1's lookup yielded) is skipped, not dereferenced. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void alleSlaatNullOver(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Entity a = EntityType.ARMOR_STAND.create(sl, EntitySpawnReason.COMMAND);
        Entity b = EntityType.ARMOR_STAND.create(sl, EntitySpawnReason.COMMAND);
        List<Entity> metNull = new ArrayList<>(Arrays.asList(a, null, b, null));
        List<Entity> l = TikWacht.alle(metNull);
        helper.assertTrue(l.size() == 2 && l.get(0) == a && l.get(1) == b, "the nulls are dropped: " + l.size());
        helper.assertTrue(TikWacht.alle(null).isEmpty(), "no source = an empty list");
        helper.succeed();
    }

    /** Discarding every tagged entity while walking the copy of getAllEntities() never throws (the live lookup is untouched). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void weggooienTijdensLopen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        List<Entity> gemaakt = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Entity e = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
            e.addTag(Sfeer.TAG + "_test");
            gemaakt.add(e);
        }
        int weg = 0;
        for (Entity e : TikWacht.alle(sl.getAllEntities())) {
            if (e.entityTags().contains(Sfeer.TAG + "_test")) {
                e.discard();
                weg++;
            }
        }
        helper.assertTrue(weg == 12, "all 12 discarded while walking: " + weg);
        helper.assertTrue(gemaakt.stream().allMatch(Entity::isRemoved), "they are really gone");
        helper.succeed();
    }

    /** Sfeer's cleanup tick (t % 200 == 100) over a level with tagged critters far from any player: no exception, they go. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sfeerOpruimTik(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        List<Entity> gemaakt = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Entity e = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
            e.addTag(Sfeer.TAG);
            gemaakt.add(e);
        }
        boolean geenPlayerDichtbij = gemaakt.stream().allMatch(e -> sl.getNearestPlayer(e, 64) == null);
        helper.runAfterDelay(1, () -> {
            boolean ok = TikWacht.veilig("test:sfeer", () -> {
                // (run the cleanup branch until the game time is on it; the fill-up branch only runs for real players)
                long t = sl.getGameTime();
                if (t % 200 != 100) {
                    Sfeer.ruimOp(sl);
                } else {
                    Sfeer.tick(sl);
                }
            });
            helper.assertTrue(ok, "Sfeer's cleanup never throws");
            if (geenPlayerDichtbij) {
                helper.assertTrue(gemaakt.stream().allMatch(Entity::isRemoved), "critters without a player within 64 blocks are removed");
            }
            helper.succeed();
        });
    }

    /** A throwing hook is logged and swallowed (the server keeps ticking), and counted. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void veiligSlikt(GameTestHelper helper) {
        boolean eerste = TikWacht.veilig("test:gooit", () -> {
            Entity e = null;
            e.entityTags();   // (the very NPE of the live crash)
        });
        boolean tweede = TikWacht.veilig("test:gooit", () -> {
            throw new IllegalStateException("test");
        });
        helper.assertTrue(!eerste && !tweede, "both throws were caught");
        helper.assertTrue(TikWacht.fouten("test:gooit") >= 1, "the second one is counted for the rate-limited log");
        helper.assertTrue(TikWacht.veilig("test:goed", () -> { }), "a quiet hook runs normally");
        helper.succeed();
    }
}
