package nl.juiced.guhs.feature.huisje.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.Huisjes;
import org.joml.Matrix4f;

/**
 * "Laat klus-area zien" (2.10): a translucent blue dome over the home base of a Guhhuisje (radius
 * {@link Huisjes#BEREIK}), with a brighter ring on the ground: where the residents wander, do their chores and play. On/off
 * per huisje in its screen, remembered while the game runs (client only).
 */
public final class HuisjeKoepel {
    /** "dimension|controller pos" of the huisjes whose dome is on. */
    private static final Set<String> AAN = ConcurrentHashMap.newKeySet();
    private static final int RINGEN = 12, SEGMENTEN = 48;

    private HuisjeKoepel() {
    }

    static String sleutel(BlockPos pos) {
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

    static void teken(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || AAN.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        String dim = mc.level.dimension().identifier() + "|";
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean iets = false;
        for (String k : AAN) {
            if (!k.startsWith(dim)) {
                continue;
            }
            BlockPos pos = BlockPos.of(Long.parseLong(k.substring(dim.length())));
            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof HuisjeBlock blok)) {
                continue;
            }
            Vec3 m = Huisje.midden(pos, state.getValue(HuisjeBlock.FACING), blok.maat());
            if (m.distanceToSqr(cam) > 96 * 96) {
                continue;
            }
            pose.pushPose();
            pose.translate(m.x - cam.x, m.y - cam.y + 0.02, m.z - cam.z);
            koepel(pose.last().pose(), buffers.getBuffer(RenderType.debugQuads()), Huisjes.BEREIK, mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false));
            pose.popPose();
            iets = true;
        }
        if (iets) {
            buffers.endBatch(RenderType.debugQuads());
        }
    }

    /** A hemisphere of quads (two sides), the lower rings a bit stronger, with a slow shimmer; plus a ring on the ground. */
    private static void koepel(Matrix4f m, VertexConsumer vc, float r, float tijd) {
        for (int i = 0; i < RINGEN; i++) {
            double a0 = Math.PI / 2 * i / RINGEN, a1 = Math.PI / 2 * (i + 1) / RINGEN;
            float y0 = (float) (Math.sin(a0) * r), y1 = (float) (Math.sin(a1) * r);
            float r0 = (float) (Math.cos(a0) * r), r1 = (float) (Math.cos(a1) * r);
            for (int j = 0; j < SEGMENTEN; j++) {
                double b0 = Math.PI * 2 * j / SEGMENTEN, b1 = Math.PI * 2 * (j + 1) / SEGMENTEN;
                float glans = 0.5f + 0.5f * (float) Math.sin(tijd * 0.05 + j * 0.4 + i * 0.7);
                int alfa = (int) (26 + 22 * (1f - i / (float) RINGEN) + 10 * glans);
                int kleur = (alfa << 24) | 0x50A0FF;
                float x00 = (float) Math.cos(b0) * r0, z00 = (float) Math.sin(b0) * r0;
                float x01 = (float) Math.cos(b1) * r0, z01 = (float) Math.sin(b1) * r0;
                float x10 = (float) Math.cos(b0) * r1, z10 = (float) Math.sin(b0) * r1;
                float x11 = (float) Math.cos(b1) * r1, z11 = (float) Math.sin(b1) * r1;
                vc.addVertex(m, x00, y0, z00).setColor(kleur);
                vc.addVertex(m, x01, y0, z01).setColor(kleur);
                vc.addVertex(m, x11, y1, z11).setColor(kleur);
                vc.addVertex(m, x10, y1, z10).setColor(kleur);
            }
        }
        // the ring on the ground: where the home base ends
        for (int j = 0; j < SEGMENTEN * 2; j++) {
            double b0 = Math.PI * 2 * j / (SEGMENTEN * 2), b1 = Math.PI * 2 * (j + 1) / (SEGMENTEN * 2);
            float ri = r - 0.35f, ro = r;
            int kleur = 0xA078C8FF;
            vc.addVertex(m, (float) Math.cos(b0) * ri, 0, (float) Math.sin(b0) * ri).setColor(kleur);
            vc.addVertex(m, (float) Math.cos(b0) * ro, 0, (float) Math.sin(b0) * ro).setColor(kleur);
            vc.addVertex(m, (float) Math.cos(b1) * ro, 0, (float) Math.sin(b1) * ro).setColor(kleur);
            vc.addVertex(m, (float) Math.cos(b1) * ri, 0, (float) Math.sin(b1) * ri).setColor(kleur);
        }
    }
}
