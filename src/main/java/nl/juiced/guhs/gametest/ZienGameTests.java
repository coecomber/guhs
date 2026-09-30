package nl.juiced.guhs.gametest;

import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhWorldData;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * 2.10: the Guhdex "sees" a guh (or a guh character) within {@link GuhDex#SEE_RANGE} = 3 blocks, no longer only right
 * next to it: 2.5 blocks away fills in its page, 5 blocks away doesn't.
 */
public class ZienGameTests {
    private static final String BATCH = "zien";

    @GuhTest(template = "empty", batch = BATCH)
    public static void zienGuhsEnNpcsOpDrieBlokjes(GameTestHelper helper) {
        helper.assertTrue(GuhDex.SEE_RANGE == 3, "the Guhdex sees 3 blocks far: " + GuhDex.SEE_RANGE);
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
            player.snapTo(at.x, at.y, at.z);
            GuhEntity dichtbij = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            dichtbij.setVariant(GuhVariant.MINT);
            dichtbij.snapTo(at.x + 2.5, at.y, at.z, 0, 0);
            dichtbij.setNoAi(true);
            helper.getLevel().addFreshEntity(dichtbij);
            GuhEntity ver = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            ver.setVariant(GuhVariant.CHOCO);
            ver.snapTo(at.x, at.y, at.z + 5, 0, 0);
            ver.setNoAi(true);
            helper.getLevel().addFreshEntity(ver);
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            npc.setKind(GuhNpcEntity.Kind.GOLFGUH);
            npc.snapTo(at.x - 2.5, at.y, at.z, 0, 0);
            npc.setNoAi(true);
            helper.getLevel().addFreshEntity(npc);

            GuhWorldData data = GuhWorldData.get(player.level().getServer());
            GuhDex.onPlayerTick(player, data);
            var seen = data.player(player.getUUID()).seen;
            helper.assertTrue(seen.contains(GuhVariant.MINT), "a mint guh 2.5 blocks away is in the Guhdex now");
            helper.assertTrue(seen.contains(GuhVariant.GOLFGUH), "and so is the Golfguh 2.5 blocks away");
            helper.assertTrue(!seen.contains(GuhVariant.CHOCO), "a choco guh 5 blocks away is too far");
            for (Entity e : new Entity[]{dichtbij, ver, npc}) {
                e.discard();
            }
        } finally {
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
