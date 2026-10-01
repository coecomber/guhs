package nl.juiced.guhs.feature.weerder.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.huisje.client.HuisjeKoepel;
import nl.juiced.guhs.feature.weerder.WeerderBlockEntity;

/**
 * "Laat de area zien" of a Wilde-guhweerder (1.2.0): the same translucent blue dome as the Guhhuisje's klus-area
 * ({@link HuisjeKoepel#koepel}), over the weerder with its radius: where no wild guhs spawn. On/off per weerder in its
 * screen, remembered while the game runs (client only).
 */
public final class WeerderKoepel {
    /** "dimension|weerder pos" of the weerders whose dome is on. */
    private static final Set<String> AAN = ConcurrentHashMap.newKeySet();

    private WeerderKoepel() {
    }

    private static String sleutel(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        return (mc.level == null ? "?" : mc.level.dimension().identifier().toString()) + "|" + pos.asLong();
    }

    public static boolean aan(BlockPos pos) {
        return AAN.contains(sleutel(pos));
    }

    public static void zet(BlockPos pos, boolean aan) {
        if (aan) {
            AAN.add(sleutel(pos));
        } else {
            AAN.remove(sleutel(pos));
        }
    }

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (AAN.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        String dim = mc.level.dimension().identifier() + "|";
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean iets = false;
        for (String k : AAN) {
            if (!k.startsWith(dim)) {
                continue;
            }
            BlockPos pos = BlockPos.of(Long.parseLong(k.substring(dim.length())));
            if (!(mc.level.getBlockEntity(pos) instanceof WeerderBlockEntity be)) {
                continue;
            }
            Vec3 m = Vec3.atBottomCenterOf(pos);
            int r = be.straal();
            if (m.distanceToSqr(cam) > (96 + r) * (96 + r)) {
                continue;
            }
            pose.pushPose();
            pose.translate(m.x - cam.x, m.y - cam.y + 0.02, m.z - cam.z);
            HuisjeKoepel.koepel(pose.last().pose(), buffers.getBuffer(RenderTypes.debugQuads()), r,
                    mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            pose.popPose();
            iets = true;
        }
        if (iets) {
            buffers.endBatch(RenderTypes.debugQuads());
        }
    }
}
