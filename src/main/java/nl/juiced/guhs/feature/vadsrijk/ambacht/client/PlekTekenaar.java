package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.vadsrijk.ambacht.NodeSoort;
import nl.juiced.guhs.feature.vadsrijk.ambacht.client.ClientPlekken.Plek;

/**
 * Draws the hive stands (your own hive or a free stand; they are per player, so not blocks): the real block model (the bouwhulp
 * ghost technique); stands you are too low for are faint. 1.10.4: ore, wood, herbs, fishing and dive spots are real shared blocks
 * now ({@code GrondstofBlok}), so they are no longer drawn here.
 */
public final class PlekTekenaar {
    private static final int LICHT = 0xF000F0;
    private static final Map<BlockState, List<BakedQuad>[]> QUADS = new HashMap<>();

    private PlekTekenaar() {
    }

    /** The model and size of a spot. */
    record Model(BlockState staat, float maat, float dy) {
    }

    static Model model(Plek p) {
        return new Model((p.eigen() ? Blocks.BEEHIVE : Blocks.BEE_NEST).defaultBlockState(), 0.7f, 0);
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

    static void vergeet() {
        QUADS.clear();
    }

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        List<Plek> lijst = ClientPlekken.PLEKKEN;
        if (mc.level == null || lijst.isEmpty()) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float t = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        RenderType type = RenderTypes.translucentMovingBlock();
        VertexConsumer vc = buffers.getBuffer(type);
        Vector3f v = new Vector3f();
        for (Plek p : lijst) {
            if (p.echt() || p.soort() != NodeSoort.KORF) {
                continue;   // (1.10.4: a real block in the world)
            }
            BlockPos b = p.pos();
            if (b.distToCenterSqr(cam) > 64 * 64) {
                continue;
            }
            Model m = model(p);
            float puls = p.zeldzaam() ? 0.75f + 0.25f * (float) Math.sin(t * 0.25f) : 1f;
            int alfa = (int) (255 * (!p.lvOk() ? 0.4f : !p.eigen() ? 0.55f : 0.95f) * puls);
            pose.pushPose();
            pose.translate(b.getX() + 0.5 - cam.x, b.getY() + m.dy() - cam.y, b.getZ() + 0.5 - cam.z);
            pose.scale(m.maat(), m.maat(), m.maat());
            pose.translate(-0.5, 0, -0.5);
            Matrix4f mat = pose.last().pose();
            List<BakedQuad>[] q = quads(m.staat());
            for (int i = 0; i < 7; i++) {
                for (BakedQuad quad : q[i]) {
                    int kleur = 0xFFFFFF;
                    if (quad.materialInfo().isTinted()) {
                        BlockTintSource bron = mc.getBlockColors().getTintSource(m.staat(), quad.materialInfo().tintIndex());
                        if (bron != null) {
                            kleur = bron.colorInWorld(m.staat(), mc.level, b) & 0xFFFFFF;
                        }
                    }
                    if (p.zeldzaam()) {
                        kleur = goud(kleur);
                    }
                    int argb = alfa << 24 | kleur;
                    Direction d = quad.direction();
                    for (int n = 0; n < 4; n++) {
                        mat.transformPosition(quad.position(n), v);
                        long uv = quad.packedUV(n);
                        vc.addVertex(v.x, v.y, v.z, argb, UVPair.unpackU(uv), UVPair.unpackV(uv), OverlayTexture.NO_OVERLAY, LICHT, d.getStepX(), d.getStepY(),
                                d.getStepZ());
                    }
                }
            }
            pose.popPose();
        }
        buffers.endBatch(type);
    }

    /** A warm golden tint for a rare spot. */
    private static int goud(int kleur) {
        int r = kleur >> 16 & 0xFF, g = kleur >> 8 & 0xFF, b = kleur & 0xFF;
        return Math.min(255, r / 2 + 128) << 16 | Math.min(255, g / 2 + 100) << 8 | b / 3;
    }
}
