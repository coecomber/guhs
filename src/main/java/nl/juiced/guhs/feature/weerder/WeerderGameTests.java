package nl.juiced.guhs.feature.weerder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.onderwater.Zeemeerguh;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Wilde-guhweerder (1.2.0, batch "guhweerder"): a wild guh's natural / chunk-generation spawn is refused
 * inside its area and allowed outside it (also above it); tamed guhs, spawn eggs and breeding are unaffected; the
 * Guhmension top-up check and the wild Zeemeerguh spawn respect it; the radius is saved and loaded (block entity and the
 * level index) and only the steps are allowed; only its owner changes it. Every test removes its weerder again, so the
 * level's index is empty for the other tests. Each test has a batch of its own (guhweerder_*: batches run one after the other), so
 * one test's big area never covers another test's "outside" spots.
 */
public class WeerderGameTests {
    private static final String BATCH = "guhweerder";
    private static final BlockPos BORD = new BlockPos(1, 1, 1);

    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** A weerder at BORD for this owner with this radius (as if placed by them). */
    static WeerderBlockEntity zet(GameTestHelper helper, ServerPlayer eigenaar, int straal) {
        helper.setBlock(BORD, WeerderFeature.WEERDER.get().defaultBlockState().setValue(WildeGuhweerderBlock.FACING, Direction.SOUTH));
        WeerderBlockEntity be = (WeerderBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BORD));
        helper.assertTrue(be != null, "weerder has a block entity");
        be.zetEigenaar(eigenaar);
        helper.assertTrue(be.zetStraal(straal), "radius " + straal + " is a step");
        return be;
    }

    /** Removes the weerder again; its area must be gone from the index. */
    static void weg(GameTestHelper helper, ServerPlayer... players) {
        helper.setBlock(BORD, Blocks.AIR.defaultBlockState());
        helper.assertTrue(WeerderIndex.straalOp(helper.getLevel(), helper.absolutePos(BORD)) == -1, "index forgets a removed weerder");
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** What the spawn position check (NeoForge's MobSpawnEvent.PositionCheck, used by the natural spawner) says for a guh here. */
    static MobSpawnEvent.PositionCheck.Result check(ServerLevel level, BlockPos abs, EntitySpawnReason reason, boolean tam, ServerPlayer owner) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        guh.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        if (tam) {
            guh.tame(owner);
        }
        MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(guh, level, reason, null);
        NeoForge.EVENT_BUS.post(event);
        guh.discard();
        return event.getResult();
    }

    // =====================================================================================================================

    @GuhTest(template = "empty", batch = BATCH + "_spawn")
    public static void wildeGuhNietBinnenWelBuiten(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        zet(helper, p, 8);
        ServerLevel level = helper.getLevel();
        BlockPos bord = helper.absolutePos(BORD);
        var FAIL = MobSpawnEvent.PositionCheck.Result.FAIL;
        // inside the area (in the middle, at the edge, a bit lower and higher): natural and chunk-generation spawns are refused
        for (BlockPos in : new BlockPos[] {bord.offset(3, 0, 2), bord.offset(8, 0, 0), bord.offset(-5, -4, 5), bord.offset(0, 8, -7)}) {
            helper.assertTrue(check(level, in, EntitySpawnReason.NATURAL, false, p) == FAIL, "natural wild guh refused at " + in.subtract(bord));
            helper.assertTrue(check(level, in, EntitySpawnReason.CHUNK_GENERATION, false, p) == FAIL, "chunk-gen wild guh refused at " + in.subtract(bord));
            helper.assertFalse(WeerderFeature.wildeGuhMag(level, in), "Guhmension top-up refused at " + in.subtract(bord));
        }
        // outside: beside it, in a corner past the circle, far above, far below
        for (BlockPos uit : new BlockPos[] {bord.offset(9, 0, 0), bord.offset(6, 0, 6), bord.offset(0, 9, 0), bord.offset(2, -9, 0), bord.offset(20, 0, 0)}) {
            helper.assertTrue(check(level, uit, EntitySpawnReason.NATURAL, false, p) != FAIL, "natural wild guh allowed at " + uit.subtract(bord));
            helper.assertTrue(WeerderFeature.wildeGuhMag(level, uit), "Guhmension top-up allowed at " + uit.subtract(bord));
        }
        // tamed, spawn egg, breeding, commands: always welcome
        BlockPos in = bord.offset(2, 0, 2);
        helper.assertTrue(check(level, in, EntitySpawnReason.NATURAL, true, p) != FAIL, "a tamed guh is not wild");
        for (EntitySpawnReason r : new EntitySpawnReason[] {EntitySpawnReason.SPAWN_ITEM_USE, EntitySpawnReason.BREEDING, EntitySpawnReason.COMMAND,
                EntitySpawnReason.TRIGGERED, EntitySpawnReason.STRUCTURE}) {
            helper.assertTrue(check(level, in, r, false, p) != FAIL, r + " guh still welcome");
        }
        weg(helper, p);
        helper.assertTrue(WeerderFeature.wildeGuhMag(level, in), "no weerder, no area");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_zee")
    public static void zeemeerguhNietInDeArea(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        zet(helper, p, 16);
        ServerLevel level = helper.getLevel();
        BlockPos bord = helper.absolutePos(BORD);
        helper.assertTrue(Zeemeerguh.spawnAt(level, bord.offset(4, 1, 4), level.getRandom()) == null, "no wild Zeemeerguh in the area");
        GuhEntity buiten = Zeemeerguh.spawnAt(level, bord.offset(20, 1, 0), level.getRandom());
        helper.assertTrue(buiten != null, "a wild Zeemeerguh outside the area");
        buiten.discard();
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_opslag")
    public static void straalOpgeslagenEnGeladen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        WeerderBlockEntity be = zet(helper, p, 32);
        ServerLevel level = helper.getLevel();
        BlockPos bord = helper.absolutePos(BORD);
        helper.assertFalse(be.zetStraal(20), "20 is not a step");
        helper.assertTrue(be.straal() == 32, "radius stays 32");
        helper.assertTrue(WeerderIndex.straalOp(level, bord) == 32, "index has radius 32");
        helper.assertFalse(WeerderFeature.wildeGuhMag(level, bord.offset(30, 0, 0)), "32: 30 blocks away is inside");
        // block entity: save and load into a fresh one
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        WeerderBlockEntity kopie = new WeerderBlockEntity(bord, be.getBlockState());
        kopie.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        helper.assertTrue(kopie.straal() == 32, "loaded radius 32, was " + kopie.straal());
        helper.assertTrue(p.getUUID().equals(kopie.eigenaar()), "loaded owner");
        // the level index: save and load
        WeerderIndex index = WeerderIndex.get(level);
        CompoundTag data = WeerderIndex.TYPE.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, index).getOrThrow() instanceof CompoundTag c ? c : null;
        helper.assertTrue(data != null, "index saves as a compound");
        WeerderIndex geladen = WeerderIndex.TYPE.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, data).getOrThrow();
        helper.assertTrue(geladen.binnen(bord.getX() + 31, bord.getY(), bord.getZ()), "loaded index: 31 away is inside");
        helper.assertFalse(geladen.binnen(bord.getX() + 33, bord.getY(), bord.getZ()), "loaded index: 33 away is outside");
        // smaller again
        helper.assertTrue(be.zetStraal(8), "back to 8");
        helper.assertTrue(WeerderFeature.wildeGuhMag(level, bord.offset(30, 0, 0)), "8: 30 blocks away is outside");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_eigenaar")
    public static void alleenDeEigenaar(GameTestHelper helper) {
        ServerPlayer eigenaar = speler(helper), ander = speler(helper);
        WeerderBlockEntity be = zet(helper, eigenaar, 16);
        BlockPos bord = helper.absolutePos(BORD);
        ander.snapTo(bord.getX() + 1.5, bord.getY(), bord.getZ() + 1.5);
        eigenaar.snapTo(bord.getX() - 0.5, bord.getY(), bord.getZ() + 1.5);
        helper.assertTrue(be.magBewerken(eigenaar), "the owner may");
        helper.assertFalse(be.magBewerken(ander), "someone else may not");
        helper.assertFalse(WeerderPayloads.zet(ander, bord, 48), "someone else can't change the radius");
        helper.assertTrue(be.straal() == 16, "radius unchanged");
        helper.assertFalse(WeerderPayloads.data(ander, be).getBooleanOr("MagBewerken", true), "read-only screen for someone else");
        helper.assertTrue(be.getBlockState().getDestroyProgress(ander, helper.getLevel(), bord) == 0f, "someone else can't break it");
        helper.assertTrue(WeerderPayloads.zet(eigenaar, bord, 48), "the owner changes the radius");
        helper.assertTrue(be.straal() == 48 && WeerderIndex.straalOp(helper.getLevel(), bord) == 48, "radius 48 everywhere");
        helper.assertTrue(WeerderPayloads.data(eigenaar, be).getBooleanOr("MagBewerken", false), "the owner's screen can edit");
        weg(helper, eigenaar, ander);
        helper.succeed();
    }
}
