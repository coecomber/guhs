package nl.juiced.guhs.feature.droomwezens;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.gametest.GuhTest;

/** 1.8.0 bouwplan C: the dream NPCs talk (story hook first, jokes in turn), the pillow fight, the sterrenvisje, the glas. */
public class DroomwezensGameTests {
    private static final String TUIN = "landdiertjes_test_tuin";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static GuhNpcEntity npc(GameTestHelper helper, BlockPos at, Kind kind) {
        BlockPos abs = helper.absolutePos(at);
        return DroomNpc.spawn(helper.getLevel(), Vec3.atBottomCenterOf(abs), 0, kind);
    }

    @GuhTest(template = TUIN, batch = "droomwezens")
    public static void droomNpcsPratenMetGrapjesEnVerhaal(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        var verhaal = DroomNpc.haalWeg();   // the story (snoesverhaal, molentje) talks over some kinds: check the jokes alone
        try {
        for (Kind k : DroomNpc.KINDS) {
            GuhNpcEntity n = npc(helper, new BlockPos(3, 1, 3), k);
            helper.assertTrue(Features.role(n.getKind()) != null, k.id() + " has a role");
            for (int i = 0; i < 5; i++) {
                Features.role(n.getKind()).talk(n, p);
            }
            helper.assertTrue(n.roleData.getIntOr(DroomNpc.VOLGENDE, 0) == 5, k.id() + ": the jokes come in turn");
            helper.assertTrue(DroomNpc.GRAPPEN.containsKey(k), k.id() + " has jokes");
            n.discard();
        }
        // the story's talk goes first; returning false lets the joke come anyway
        AtomicInteger gepraat = new AtomicInteger();
        DroomNpc.zet(Kind.ZANDGUH, (speler, npc) -> gepraat.incrementAndGet() % 2 == 1);
        GuhNpcEntity zand = npc(helper, new BlockPos(3, 1, 3), Kind.ZANDGUH);
        DroomNpc.praat(p, zand);
        helper.assertTrue(gepraat.get() == 1 && zand.roleData.getIntOr(DroomNpc.VOLGENDE, 0) == 0, "handled by the story: no joke");
        DroomNpc.praat(p, zand);
        helper.assertTrue(gepraat.get() == 2 && zand.roleData.getIntOr(DroomNpc.VOLGENDE, 0) == 1, "not handled: the joke");
        helper.assertTrue(DroomNpc.van("wekkerguh") == Kind.WEKKERGUH && DroomNpc.van("nope") == null, "names");
        helper.assertTrue(zand.duur("happy") > 0 && zand.duur("rinkel") < 0, "the Zandguh has no rinkel");
        GuhNpcEntity wekker = npc(helper, new BlockPos(1, 1, 1), Kind.WEKKERGUH);
        helper.assertTrue(wekker.duur("rinkel") == 20, "the Wekkerguh rings");
        } finally {
            DroomNpc.zetTerug(verhaal);
        }
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "droomwezens")
    public static void kussengevechtGooitEenKussen(GameTestHelper helper) {
        GuhNpcEntity a = npc(helper, new BlockPos(1, 1, 2), Kind.KUSSENGEVECHT_GUH);
        GuhNpcEntity b = npc(helper, new BlockPos(4, 1, 2), Kind.KUSSENGEVECHT_GUH);
        helper.assertTrue(DroomNpc.DroomNpcRol.gooi(helper.getLevel(), a), "a throws at b");
        helper.assertTrue("gooi".equals(a.speelt()), "the throw animation");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(KussenProjectiel.class, a.getBoundingBox().inflate(4)).isEmpty(), "a pillow flies");
        float hp = b.getHealth();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(b.isAlive() && b.getHealth() == hp, "a pillow never hurts");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(KussenProjectiel.class, a.getBoundingBox().inflate(12)).isEmpty(), "the pillow plopped");
            helper.succeed();
        });
    }

    @GuhTest(template = TUIN, batch = "droomwezens")
    public static void sterrenvisjeEnGlasRestje(GameTestHelper helper) {
        SterrenvisjeEntity v = DroomwezensFeature.STERRENVISJE.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        v.setKleur(SterrenvisjeEntity.Kleur.LILA);
        var emmer = v.getBucketItemStack();
        v.saveToBucketTag(emmer);
        SterrenvisjeEntity terug = DroomwezensFeature.STERRENVISJE.get().create(helper.getLevel(), EntitySpawnReason.BUCKET);
        terug.loadFromBucketTag(emmer.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
        helper.assertTrue(terug.kleur() == SterrenvisjeEntity.Kleur.LILA, "its colour stays in the bucket");
        helper.assertTrue(emmer.is(DroomwezensFeature.STERRENVISJE_EMMER.get()), "its own bucket");

        BlockPos glas = new BlockPos(2, 1, 2);
        helper.setBlock(glas, DroomwezensFeature.GLAS_RESTJE.get().defaultBlockState().setValue(GlasRestjeBlock.FACING, Direction.NORTH));
        BlockState s = helper.getBlockState(glas);
        helper.assertTrue(s.getValue(GlasRestjeBlock.GLAZEN) == 1, "one glass");
        helper.setBlock(glas, s.setValue(GlasRestjeBlock.GLAZEN, 3).setValue(GlasRestjeBlock.DRANKJE, GlasRestjeBlock.Drankje.COLA));
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GlasRestjeBlock.slokje(helper.getLevel(), helper.absolutePos(glas), helper.getBlockState(glas), p);
        helper.assertTrue(helper.getBlockState(glas).getValue(GlasRestjeBlock.GLAZEN) == 3, "a sip: the restje stays (never empty)");
        helper.succeed();
    }
}
