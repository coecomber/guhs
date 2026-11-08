package nl.juiced.guhs.feature.vadsrijk.kern.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-SPEL: a cutscene camera far from the player gets its chunks: during the scene the player's chunk view follows the camera (so
 * the server sends those chunks), afterwards the normal view around the player is back; a camera near the player changes nothing.
 * Run: {@code runGameTestServer -Pgt=SceneLaden}.
 */
public class SceneLadenGameTests {
    @GuhTest(template = "empty", batch = "vadsrijk_sceneladen")
    public static void verreCameraKrijgtChunks(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 o = Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO));
        // near: inside the normal view
        RijkScene dichtbij = RijkScene.maak("test_dichtbij", 40).camera(0, o.add(3, 4, 3), o).camera(40, o.add(-3, 4, 3), o).bouw();
        RijkScenes.speel(p, dichtbij);
        helper.assertFalse(SceneLaden.actief(p), "a camera next to the player changes nothing");
        RijkScenes.vergeet(p);
        // far: 1200 blocks east
        Vec3 ver = o.add(1200, 40, 0);
        RijkScene s = RijkScene.maak("test_ver", 100).camera(0, ver, ver.add(20, -20, 0)).camera(100, ver.add(48, 0, 0), ver.add(80, -20, 0)).bouw();
        RijkScenes.speel(p, s);
        helper.assertTrue(SceneLaden.actief(p), "a far camera moves the chunk view");
        ChunkPos cam = ChunkPos.containing(BlockPos.containing(ver));
        helper.assertTrue(p.getChunkTrackingView() instanceof ChunkTrackingView.Positioned v && SceneLaden.afstand(v.center(), cam) <= SceneLaden.HERCENTREER,
                "the view is around the camera: " + p.getChunkTrackingView() + " vs " + cam);
        RijkScenes.vergeet(p);
        helper.assertFalse(SceneLaden.actief(p), "after the scene: no longer moved");
        helper.assertTrue(p.getChunkTrackingView() instanceof ChunkTrackingView.Positioned v && v.center().equals(p.chunkPosition()),
                "the normal view around the player is back: " + p.getChunkTrackingView());
        // the camera path, sampled the way the client glides (smoothstep)
        helper.assertTrue(SceneLaden.camera(s, 0).distanceTo(ver) < 1e-6 && SceneLaden.camera(s, 100).distanceTo(ver.add(48, 0, 0)) < 1e-6
                && Math.abs(SceneLaden.camera(s, 50).x - (ver.x + 24)) < 1e-6, "camera(t) follows the keyframes");
        helper.succeed();
    }
}
