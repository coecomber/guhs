package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the ghosts (design 2.1: "zwak doorzichtig, iedereen ziet het"): the real block models, see-through, full bright,
 * softly pulsing; a little stronger for the owner and helpers. A ghost block over a block that is still in the way is drawn
 * a hair bigger (no flicker). Also the progress bar that floats over a working Bouwkeet.
 */
public final class SpookTekenaar {
    private static final float EIGEN = 0.5f, ANDER = 0.26f;
    private static final int LICHT = 0xF000F0;
    /** The quads of a block state per face (index 6: the quads that are never culled). */
    private static final Map<BlockState, List<BakedQuad>[]> QUADS = new HashMap<>();

    static void vergeet() {
        QUADS.clear();
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[] quads(BlockState s) {
        return QUADS.computeIfAbsent(s, st -> {
            List<BakedQuad>[] uit = new List[7];
            for (int i = 0; i < 7; i++) {
                uit[i] = new ArrayList<>();
            }
            List<BlockStateModelPart> delen = new ArrayList<>();
            Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(st).collectParts(RandomSource.create(42L), delen);
            for (BlockStateModelPart deel : delen) {
                for (Direction d : Direction.values()) {
                    uit[d.ordinal()].addAll(deel.getQuads(d));
                }
                uit[6].addAll(deel.getQuads(null));
            }
            return uit;
        });
    }

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float t = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float puls = 0.85f + 0.15f * (float) Math.sin(t * 0.12f);
        List<ClientSpoken.Teken> lijst = ClientSpoken.teken();
        if (!lijst.isEmpty()) {
            RenderType type = RenderTypes.translucentMovingBlock();
            VertexConsumer vc = buffers.getBuffer(type);
            Vector3f v = new Vector3f();
            for (ClientSpoken.Teken k : lijst) {
                BlockPos p = k.pos();
                pose.pushPose();
                pose.translate(p.getX() - cam.x, p.getY() - cam.y, p.getZ() - cam.z);
                if (k.overlapt()) {
                    pose.translate(0.5, 0.5, 0.5);
                    pose.scale(1.006f, 1.006f, 1.006f);
                    pose.translate(-0.5, -0.5, -0.5);
                }
                Matrix4f m = pose.last().pose();
                int alfa = (int) (255 * (k.eigen() ? EIGEN : ANDER) * puls);
                if (k.buiten()) {
                    alfa = Math.min(255, alfa + 40);   // (test round 5: out of the keet's reach: red and a bit stronger)
                }
                List<BakedQuad>[] q = quads(k.staat());
                for (int i = 0; i < 7; i++) {
                    if (i < 6 && (k.vlakken() & 1 << i) == 0) {
                        continue;
                    }
                    for (BakedQuad quad : q[i]) {
                        int kleur = 0xFFFFFF;
                        if (quad.materialInfo().isTinted()) {
                            BlockTintSource bron = mc.getBlockColors().getTintSource(k.staat(), quad.materialInfo().tintIndex());
                            if (bron != null) {
                                kleur = bron.colorInWorld(k.staat(), mc.level, p) & 0xFFFFFF;
                            }
                        }
                        if (k.buiten()) {
                            kleur = rood(kleur);
                        }
                        int argb = alfa << 24 | kleur;
                        Direction d = quad.direction();
                        for (int n = 0; n < 4; n++) {
                            m.transformPosition(quad.position(n), v);
                            long uv = quad.packedUV(n);
                            vc.addVertex(v.x, v.y, v.z, argb, UVPair.unpackU(uv), UVPair.unpackV(uv), OverlayTexture.NO_OVERLAY, LICHT,
                                    d.getStepX(), d.getStepY(), d.getStepZ());
                        }
                    }
                }
                pose.popPose();
            }
            buffers.endBatch(type);
        }
        balken(pose, buffers, cam);
    }

    /** Test round 5: a block out of reach: tinted red (its own colours still faintly there). */
    private static int rood(int kleur) {
        int r = kleur >> 16 & 0xFF, g = kleur >> 8 & 0xFF, b = kleur & 0xFF;
        return Math.min(255, 140 + r / 2) << 16 | g * 3 / 10 << 8 | b * 3 / 10;
    }

    /** A little bar over every working keet: pink = done, cream = to go. */
    private static void balken(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 cam) {
        boolean iets = false;
        Minecraft mc = Minecraft.getInstance();
        for (ClientSpoken.Spook s : ClientSpoken.alle()) {
            BlockPos keet = s.bouwer();
            if (keet == null || s.status() != 1 || !keet.closerToCenterThan(cam, 48)) {
                continue;
            }
            float deel = s.gedaan() + s.rest() <= 0 ? 0 : s.gedaan() / (float) (s.gedaan() + s.rest());
            VertexConsumer vc = buffers.getBuffer(RenderTypes.debugQuads());
            pose.pushPose();
            pose.translate(keet.getX() + 0.5 - cam.x, keet.getY() + 3.6 - cam.y, keet.getZ() + 0.5 - cam.z);
            pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
            Matrix4f m = pose.last().pose();
            float w = 1.2f, h = 0.12f;
            rechthoek(vc, m, -w - 0.04f, -h - 0.04f, w + 0.04f, h + 0.04f, -0.01f, 0xC0402030);
            rechthoek(vc, m, -w, -h, w, h, 0, 0xE0FFF0DC);
            rechthoek(vc, m, -w, -h, -w + 2 * w * deel, h, 0.01f, 0xF0F07CB4);
            pose.popPose();
            iets = true;
        }
        if (iets) {
            buffers.endBatch(RenderTypes.debugQuads());
        }
    }

    /** Both windings (seen from either side). */
    private static void rechthoek(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb) {
        vc.addVertex(m, x0, y0, z).setColor(argb);
        vc.addVertex(m, x1, y0, z).setColor(argb);
        vc.addVertex(m, x1, y1, z).setColor(argb);
        vc.addVertex(m, x0, y1, z).setColor(argb);
        vc.addVertex(m, x0, y1, z).setColor(argb);
        vc.addVertex(m, x1, y1, z).setColor(argb);
        vc.addVertex(m, x1, y0, z).setColor(argb);
        vc.addVertex(m, x0, y0, z).setColor(argb);
    }

    private SpookTekenaar() {
    }
}
