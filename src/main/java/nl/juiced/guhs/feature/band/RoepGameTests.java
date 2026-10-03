package nl.juiced.guhs.feature.band;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of "Roep naar mij" (1.2.5, {@link Roepen}): from far away in the same level (loaded, and from a chunk that
 * unloaded: a ticket loads it and we wait for the entity), out of its Guhhuisje (no longer a resident), from the Nether
 * (the same guh: name and hearts kept), and refused for someone else's guh, a picked-up one and one in the wolkjes.
 * Every guh lands ~2 blocks from the player on safe ground, not sitting. (Template huisje_test_tuin: 24 x 24 grass at y 0.)
 */
public class RoepGameTests {
    private static final String TUIN = "huisje_test_tuin";
    private static final String BATCH = "roep";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        Band.bijwerken(guh);
        return guh;
    }

    /** A tamed guh in a forced chunk far away (in any level), standing on a little stone floor. */
    private static GuhEntity verGuh(ServerLevel level, ServerPlayer owner, BlockPos pos) {
        ChunkPos c = ChunkPos.containing(pos);
        level.setChunkForced(c.x(), c.z(), true);
        level.getChunk(c.x(), c.z());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlockAndUpdate(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        guh.tame(owner);
        guh.setPersistenceRequired();
        level.addFreshEntity(guh);
        Band.bijwerken(guh);
        GuhVolger.zet(guh, BandEvents.plekSoort(guh), BandEvents.plekDetail(guh));
        return guh;
    }

    /**
     * 1.2.6: the test server sprints its ticks, but a far chunk and its entities load on worker threads in real time: while
     * we wait for one, give those threads a few milliseconds per tick (else thousands of ticks pass before it's loaded).
     */
    private static void adem(boolean wachten) {
        if (wachten) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void losLaten(ServerLevel level, BlockPos pos) {
        ChunkPos c = ChunkPos.containing(pos);
        level.setChunkForced(c.x(), c.z(), false);
    }

    /** Next to the player (1 to ~3.5 blocks), on the ground, in the player's level, standing. */
    private static void bijSpeler(GameTestHelper helper, ServerPlayer p, Entity e, String wat) {
        helper.assertTrue(e != null && !e.isRemoved(), wat + ": the guh is there");
        helper.assertTrue(e.level() == p.level(), wat + ": in the player's level");
        double d = e.distanceTo(p);
        helper.assertTrue(d >= 0.9 && d <= 3.6, wat + ": about 2 blocks away (" + d + ")");
        helper.assertTrue(e.level().noCollision(e, e.getBoundingBox()), wat + ": not in a wall");
        BlockPos onder = BlockPos.containing(e.getX(), e.getY() - 0.1, e.getZ());
        helper.assertTrue(!e.level().getBlockState(onder).getCollisionShape(e.level(), onder).isEmpty(), wat + ": on the ground");
        helper.assertTrue(e instanceof GuhEntity g && !g.isOrderedToSit(), wat + ": not sitting");
        helper.assertTrue(GuhVolger.plek(p.level().getServer(), p.getUUID(), e.getUUID()).soort() == PlekSoort.BIJ_JOU, wat + ": waar is hij? bij jou");
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 2000)
    public static void roepVanVer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 1, 12));
        ServerLevel level = helper.getLevel();
        BlockPos ver = helper.absolutePos(new BlockPos(12, 1, 12)).offset(400, 0, 0);
        GuhEntity guh = verGuh(level, p, ver);
        guh.toggleSit();
        UUID id = guh.getUUID();
        helper.succeedWhen(() -> {
            adem(level.getEntity(id) == null);
            helper.assertTrue(level.getEntity(id) != null, "its forced chunk is loaded");   // (the entity section loads a tick later)
            helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OK, "called");
            Entity e = level.getEntity(id);
            bijSpeler(helper, p, e, "from 400 blocks away");
            losLaten(level, ver);
            e.discard();
            weg(helper, p);
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 4000)
    public static void roepUitOngeladenChunk(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 1, 12));
        ServerLevel level = helper.getLevel();
        BlockPos ver = helper.absolutePos(new BlockPos(12, 1, 12)).offset(0, 0, 1200);
        GuhEntity guh = verGuh(level, p, ver);
        guh.setCustomName(Component.literal("Verre Vadsig"));
        Band.bijwerken(guh);
        UUID id = guh.getUUID();
        // (the test server doesn't unload a chunk whose entities are loaded, so the guh goes in and its chunk is let go in
        // the same tick, before its entity section is loaded: just like a guh far away whose chunk isn't loaded)
        losLaten(level, ver);
        helper.assertTrue(level.getEntity(id) == null, "not loaded");
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.BEZIG, "not loaded: being fetched");
        helper.assertTrue(Roepen.bezig(id), "a fetch job");
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.BEZIG && Roepen.bezig(id), "calling again: still the same fetch");
        helper.succeedWhen(() -> {
            adem(level.getEntity(id) == null);
            Entity e = level.getEntity(id);
            helper.assertTrue(e != null && !Roepen.bezig(id), "loaded with a ticket and arrived");
            bijSpeler(helper, p, e, "from an unloaded chunk");
            helper.assertTrue(e.getCustomName() != null && e.getCustomName().getString().equals("Verre Vadsig"), "the same guh");
            e.discard();
            weg(helper, p);
        });
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void roepUitHuisje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        Huisje h = HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
        GuhEntity guh = guh(helper, p, new BlockPos(6, 1, 12));
        helper.assertTrue(Huisjes.trekIn(h, guh) && Huisjes.isBewoner(guh), "lives in the huisje");
        UUID id = guh.getUUID();
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OK, "called");
        Entity e = helper.getLevel().getEntity(id);
        bijSpeler(helper, p, e, "out of its huisje");
        helper.assertTrue(!Huisjes.isBewoner(e) && !h.bewoners().contains(id), "moved out: its place is free");
        helper.assertTrue(Huisjes.vanBewoner(p.level().getServer(), p.getUUID(), id) == null, "no huisje any more");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void roepWeigert(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ServerPlayer ander = speler(helper, new BlockPos(2, 1, 20));
        MinecraftServer s = p.level().getServer();
        // someone else's guh
        GuhEntity vreemd = guh(helper, ander, new BlockPos(5, 1, 5));
        BlockPos was = vreemd.blockPosition();
        helper.assertTrue(Roepen.roep(p, vreemd.getUUID()) == Roepen.Uitkomst.NIET_JOUW, "not your guh");
        helper.assertTrue(vreemd.blockPosition().equals(was) && !vreemd.isRemoved(), "it stays where it is");
        // a picked-up guh (in the pockets / a chest)
        GuhEntity opgepakt = guh(helper, p, new BlockPos(8, 1, 5));
        UUID id = opgepakt.getUUID();
        PickedUpGuhItem.pickUp(opgepakt);
        helper.assertTrue(GuhVolger.plek(s, p.getUUID(), id).soort() == PlekSoort.ITEM_SPELER, "picked up");
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OPGEPAKT, "a picked-up guh can't be called");
        helper.assertTrue(Roepen.nietRoepbaar(PlekSoort.ITEM_KIST, false) == Roepen.Uitkomst.OPGEPAKT
                && Roepen.nietRoepbaar(PlekSoort.ITEM_RUGZAK, false) == Roepen.Uitkomst.OPGEPAKT
                && Roepen.nietRoepbaar(PlekSoort.GUHWIEL, false) == Roepen.Uitkomst.GUHWIEL
                && Roepen.nietRoepbaar(PlekSoort.HUISJE, false) == null && Roepen.nietRoepbaar(PlekSoort.WERELD, true) == Roepen.Uitkomst.DOOD,
                "grey for items, the wheel and the wolkjes");
        // a guh in the wolkjes
        GuhEntity dood = guh(helper, p, new BlockPos(11, 1, 5));
        UUID doodId = dood.getUUID();
        BandData.Rec r = BandData.get(s).vind(p.getUUID(), doodId);
        dood.discard();
        r.dood = true;
        helper.assertTrue(Roepen.roep(p, doodId) == Roepen.Uitkomst.DOOD, "a guh in the wolkjes can't be called");
        r.dood = false;
        // and a guh nobody knows
        helper.assertTrue(Roepen.roep(p, UUID.randomUUID()) == Roepen.Uitkomst.NIET_JOUW, "an unknown id");
        weg(helper, p, ander);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 4000)
    public static void roepUitDeNether(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 1, 12));
        ServerLevel nether = p.level().getServer().getLevel(Level.NETHER);
        if (nether == null) {   // (no Nether on this test server: nothing to test across dimensions)
            weg(helper, p);
            helper.succeed();
            return;
        }
        BlockPos ver = new BlockPos(3000, 100, 3000);
        GuhEntity guh = verGuh(nether, p, ver);
        guh.setCustomName(Component.literal("Netherknabbel"));
        Band.bijwerken(guh);
        UUID id = guh.getUUID();
        Band.geefHartjes(guh, p, 5, Reden.KNUFFELEN);
        int hartjes = Band.hartjes(guh);
        helper.assertTrue(hartjes > 0, "it has hearts");
        helper.succeedWhen(() -> {
            // (the Nether is really generated on the test server: its chunks around the guh take a while)
            adem(nether.getEntity(id) == null);
            helper.assertTrue(nether.getEntity(id) != null, "its forced chunk in the Nether is loaded");
            helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OK, "called from the Nether");
            Entity e = helper.getLevel().getEntity(id);
            helper.assertTrue(nether.getEntity(id) == null, "gone from the Nether");
            bijSpeler(helper, p, e, "from the Nether");
            helper.assertTrue(e.getCustomName() != null && e.getCustomName().getString().equals("Netherknabbel"), "same name");
            helper.assertTrue(Band.hartjes((GuhEntity) e) == hartjes, "same hearts");
            helper.assertTrue(((GuhEntity) e).isTame() && p.getUUID().equals(((GuhEntity) e).getOwnerUUID()), "still yours");
            losLaten(nether, ver);
            e.discard();
            weg(helper, p);
        });
    }
}
