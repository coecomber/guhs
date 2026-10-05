package nl.juiced.guhs.feature.guhpixel.bioscoop.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.bioscoop.Doek;
import nl.juiced.guhs.feature.guhpixel.bioscoop.ProjectorBlockEntity;

/**
 * Draws the running film on the Bioscoopdoek in front of the projector (the projector itself is a plain block model).
 * At extract time the film's scene of this moment is worked out into a list of flat quads (sprites of the film's own
 * sprite sheet, in the order of the layers) and lines of text; submit draws them a hair in front of the cloth, each layer
 * a little nearer than the one before, full bright. The time is the level's game time minus the block entity's start, so
 * everybody sees the same frame. Pictures use a cutout render type (no half-transparent pixels: no sorting trouble).
 */
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity, ProjectorRenderer.State> {
    /** Floats per quad: four corners (x, y), u0 v0 u1 v1, the colour. */
    private static final int PER = 13;
    /** The first layer lies this far in front of the cloth, every next one this much nearer (blocks). */
    private static final float Z0 = 0.012f, DZ = 0.004f;
    private static final int DONKER = 0xFF0B0A10, BALK = 0xFF18101E;
    private static final float ONDER_SCHAAL = 0.5f;

    record TekstOp(FormattedCharSequence tekst, float x, float y, float schaal, int kleur, boolean schaduw, float dx) {
    }

    public static class State extends BlockEntityRenderState {
        boolean aan;
        @Nullable
        Identifier atlas;
        float[] quads = new float[PER * 48];
        int aantal;
        final List<TekstOp> teksten = new ArrayList<>();
        double mx, my, mz;
        float draai, px;

        private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float u0, float v0, float u1, float v1, int argb) {
            if ((aantal + 1) * PER > quads.length) {
                quads = java.util.Arrays.copyOf(quads, quads.length * 2);
            }
            int i = aantal++ * PER;
            quads[i] = x0;
            quads[i + 1] = y0;
            quads[i + 2] = x1;
            quads[i + 3] = y1;
            quads[i + 4] = x2;
            quads[i + 5] = y2;
            quads[i + 6] = x3;
            quads[i + 7] = y3;
            quads[i + 8] = u0;
            quads[i + 9] = v0;
            quads[i + 10] = u1;
            quads[i + 11] = v1;
            quads[i + 12] = Float.intBitsToFloat(argb);
        }
    }

    public ProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProjectorBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.aan = false;
        state.aantal = 0;
        state.teksten.clear();
        Level level = be.getLevel();
        Doek doek = be.doek();
        if (level == null || doek == null || !be.speelt()) {
            return;
        }
        FilmData film = FilmData.laad(be.film());
        if (film == null) {
            return;
        }
        float t = Math.max(0f, level.getGameTime() - be.start() + partialTick);
        BlockPos p = be.getBlockPos();
        Vec3 m = doek.midden();
        state.mx = m.x - p.getX();
        state.my = m.y - p.getY();
        state.mz = m.z - p.getZ();
        state.draai = -doek.kant().toYRot();
        float schaal = Math.min(doek.breedte() / (float) Doek.MAX_B, doek.hoogte() / (float) Doek.MAX_H);
        state.px = Doek.MAX_B * schaal / FilmData.B;        // blocks per film pixel
        state.atlas = film.atlas;
        state.aan = true;
        float wu = (film.wit.u() + film.wit.w() / 2f) / film.atlasB, wv = (film.wit.v() + film.wit.h() / 2f) / film.atlasH;
        // the whole cloth goes dark (bars beside the film when the screen is not 7 : 4)
        float hb = doek.breedte() / state.px / 2f, hh = doek.hoogte() / state.px / 2f, cx = FilmData.B / 2f, cy = FilmData.H / 2f;
        state.quad(cx - hb, cy - hh, cx - hb, cy + hh, cx + hb, cy + hh, cx + hb, cy - hh, wu, wv, wu, wv, DONKER);
        Font font = Minecraft.getInstance().font;
        FilmData.Scene scene = film.scene(t);
        if (scene != null) {
            float lt = t - scene.van;
            float[] w = new float[6];
            int[] hulp = new int[1];
            for (FilmData.Laag l : scene.lagen) {
                if (lt < l.van || lt >= l.tot) {
                    continue;
                }
                l.op(lt, w, hulp);
                switch (l.soort) {
                    case FilmData.SPRITE -> sprite(state, film, l, lt, w);
                    case FilmData.VLAK -> recht(state, w[0], w[1], w[0] + w[4], w[1] + w[5], wu, wv, wu, wv, l.kleur, false);
                    default -> {
                        Component c = l.letterlijk ? Component.literal(l.tekst)
                                : l.teller ? Component.translatable(l.tekst, teller(l, lt)) : Component.translatable(l.tekst);
                        FormattedCharSequence seq = c.getVisualOrderText();
                        int breed = font.width(seq);
                        float dx = l.uitlijn == FilmData.MIDDEN ? -breed / 2f : l.uitlijn == FilmData.RECHTS ? -breed : 0f;
                        state.teksten.add(new TekstOp(seq, w[0], w[1], l.schaal * w[2], l.kleur, l.schaduw, dx));
                    }
                }
            }
        }
        FilmData.Ondertitel o = film.ondertitel(t);
        if (o != null) {
            List<FormattedCharSequence> regels = font.split(Component.translatable(o.sleutel()), (int) ((FilmData.B - 6) / ONDER_SCHAAL));
            int n = Math.min(3, regels.size());
            float hoog = n * 5 + 3, y0 = FilmData.H - hoog;
            recht(state, 0, y0, FilmData.B, FilmData.H, wu, wv, wu, wv, BALK, false);
            for (int i = 0; i < n; i++) {
                FormattedCharSequence seq = regels.get(i);
                state.teksten.add(new TekstOp(seq, FilmData.B / 2f, y0 + 2 + i * 5, ONDER_SCHAAL, 0xFFFFFFFF, false, -font.width(seq) / 2f));
            }
        }
    }

    private static int teller(FilmData.Laag l, float lt) {
        float f = Mth.clamp((lt - l.tellerT0) / Math.max(1f, l.tellerT1 - l.tellerT0), 0f, 1f);
        return Math.round(Mth.lerp(f, l.tellerVan, l.tellerNaar));
    }

    private static void sprite(State state, FilmData film, FilmData.Laag l, float lt, float[] w) {
        FilmData.Sprite sp = l.sprite;
        if (sp == null) {
            return;
        }
        int frame = l.frame >= 0 ? Math.min(l.frame, sp.frames() - 1) : (int) (lt / sp.per()) % sp.frames();
        float u0 = (sp.u() + frame * sp.w()) / (float) film.atlasB, u1 = (sp.u() + (frame + 1) * sp.w()) / (float) film.atlasB;
        float v0 = sp.v() / (float) film.atlasH, v1 = (sp.v() + sp.h()) / (float) film.atlasH;
        if (l.spiegel) {
            float h = u0;
            u0 = u1;
            u1 = h;
        }
        float bw = sp.w() * w[2], bh = sp.h() * w[2];
        float cx = w[0] + sp.w() / 2f, cy = w[1] + sp.h() / 2f;
        if (w[3] == 0f) {
            recht(state, cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, u0, v0, u1, v1, l.kleur, true);
            return;
        }
        // turned: its four corners round the middle (not clipped: keep turned sprites inside the picture)
        if (cx < -bw || cx > FilmData.B + bw || cy < -bh || cy > FilmData.H + bh) {
            return;
        }
        float rad = w[3] * Mth.DEG_TO_RAD, c = Mth.cos(rad), s = Mth.sin(rad), hx = bw / 2, hy = bh / 2;
        state.quad(cx + (-hx * c + hy * s), cy + (-hx * s - hy * c), cx + (-hx * c - hy * s), cy + (-hx * s + hy * c),
                cx + (hx * c - hy * s), cy + (hx * s + hy * c), cx + (hx * c + hy * s), cy + (hx * s - hy * c), u0, v0, u1, v1, l.kleur);
    }

    /** An upright rectangle in film pixels, cut off at the edge of the picture (clip: the UVs are cut along). */
    private static void recht(State state, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb, boolean clipUv) {
        if (x1 <= 0 || y1 <= 0 || x0 >= FilmData.B || y0 >= FilmData.H || x1 <= x0 || y1 <= y0) {
            return;
        }
        float cx0 = Math.max(0, x0), cy0 = Math.max(0, y0), cx1 = Math.min(FilmData.B, x1), cy1 = Math.min(FilmData.H, y1);
        if (clipUv) {
            float du = (u1 - u0) / (x1 - x0), dv = (v1 - v0) / (y1 - y0);
            float nu0 = u0 + (cx0 - x0) * du, nu1 = u0 + (cx1 - x0) * du, nv0 = v0 + (cy0 - y0) * dv, nv1 = v0 + (cy1 - y0) * dv;
            u0 = nu0;
            u1 = nu1;
            v0 = nv0;
            v1 = nv1;
        }
        state.quad(cx0, cy0, cx0, cy1, cx1, cy1, cx1, cy0, u0, v0, u1, v1, argb);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.aan || state.atlas == null) {
            return;
        }
        final float px = state.px;
        final int n = state.aantal;
        final float[] q = state.quads;
        pose.pushPose();
        pose.translate(state.mx, state.my, state.mz);
        pose.mulPose(Axis.YP.rotationDegrees(state.draai));
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.atlas), (last, vc) -> {
            for (int k = 0; k < n; k++) {
                int i = k * PER;
                float z = Z0 + k * DZ;
                int argb = Float.floatToRawIntBits(q[i + 12]);
                punt(vc, last, q[i], q[i + 1], z, q[i + 8], q[i + 9], argb, px);
                punt(vc, last, q[i + 2], q[i + 3], z, q[i + 8], q[i + 11], argb, px);
                punt(vc, last, q[i + 4], q[i + 5], z, q[i + 10], q[i + 11], argb, px);
                punt(vc, last, q[i + 6], q[i + 7], z, q[i + 10], q[i + 9], argb, px);
            }
        });
        float zt = Z0 + (n + 2) * DZ;
        for (TekstOp op : state.teksten) {
            pose.pushPose();
            pose.translate((op.x() - FilmData.B / 2f) * px, (FilmData.H / 2f - op.y()) * px, zt);
            float s = px * op.schaal();
            pose.scale(s, -s, s);
            collector.submitText(pose, op.dx(), 0f, op.tekst(), op.schaduw(), Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, op.kleur(), 0, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    /** One corner: film pixels (x to the right, y down) to the cloth (x to the right, y up, z towards the audience). */
    private static void punt(VertexConsumer vc, PoseStack.Pose p, float fx, float fy, float z, float u, float v, int argb, float px) {
        vc.addVertex(p, (fx - FilmData.B / 2f) * px, (FilmData.H / 2f - fy) * px, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(ProjectorBlockEntity be) {
        AABB doos = new AABB(be.getBlockPos());
        Doek doek = be.doek();
        if (doek != null) {
            doos = doos.minmax(new AABB(doek.hoek())).minmax(new AABB(doek.blok(doek.breedte() - 1, doek.hoogte() - 1)));
        }
        return doos.inflate(0.5);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
