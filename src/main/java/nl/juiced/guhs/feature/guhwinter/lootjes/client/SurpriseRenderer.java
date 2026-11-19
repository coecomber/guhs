package nl.juiced.guhs.feature.guhwinter.lootjes.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhwinter.lootjes.Inhoud;
import nl.juiced.guhs.feature.guhwinter.lootjes.SurpriseBlokEntity;
import nl.juiced.guhs.feature.guhwinter.lootjes.SurpriseData;

/**
 * Draws a placed Surprise from its kern: the cached {@link SurpriseMesh} (built once; rebuilt only for new data, or when the
 * light on it changed, checked every two seconds), plus the vanilla renderers of its signs, heads, banners, beds and
 * bells. Culled by the frustum (the box of its 7x7x7 room) and by distance ({@link #getViewDistance}). The parts have no
 * renderer at all.
 */
public class SurpriseRenderer implements BlockEntityRenderer<SurpriseBlokEntity, SurpriseRenderer.State> {
    private static final int LICHT_CHECK = 40;

    public static class State extends BlockEntityRenderState {
        @Nullable
        SurpriseMesh mesh;
        final List<BlockEntityRenderState> extra = new ArrayList<>();
        final List<float[]> extraPlek = new ArrayList<>();
    }

    public SurpriseRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The gift's mesh, built when missing or out of date. */
    @Nullable
    static SurpriseMesh mesh(SurpriseBlokEntity be) {
        SurpriseData d = be.gift();
        BlockPos o = be.oorsprong();
        if (d == null || o == null || !(be.getLevel() instanceof ClientLevel level)) {
            return null;
        }
        SurpriseMesh m = be.mesh instanceof SurpriseMesh sm && sm.versie == be.versie ? sm : null;
        long nu = level.getGameTime();
        if (m != null && nu - m.gecontroleerd >= LICHT_CHECK) {
            m.gecontroleerd = nu;
            if (SurpriseMesh.lichtSom(level, o, m.inhoud) != m.lichtSom) {
                m = SurpriseMesh.bouw(level, o, m.inhoud, be.getBlockPos(), be.versie);   // the light changed
            }
        }
        if (m == null) {
            m = SurpriseMesh.bouw(level, o, d.lees().gedraaid(be.draai()), be.getBlockPos(), be.versie);
        }
        be.mesh = m;
        return m;
    }

    @Override
    public void extractRenderState(SurpriseBlokEntity be, State state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breaking);
        state.extra.clear();
        state.extraPlek.clear();
        state.mesh = be.isKern() ? mesh(be) : null;
        if (state.mesh == null || state.mesh.extra.isEmpty()) {
            return;
        }
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (SurpriseMesh.Extra e : state.mesh.extra) {
            BlockEntityRenderState s = dispatcher.tryExtractRenderState(e.be(), partialTick, null);
            if (s != null) {
                state.extra.add(s);
                state.extraPlek.add(new float[]{e.dx(), e.dy(), e.dz()});
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        SurpriseMesh m = state.mesh;
        if (m == null) {
            return;
        }
        for (int laag = 0; laag < 3; laag++) {
            if (m.heeft(laag)) {
                int l = laag;
                collector.submitCustomGeometry(pose, SurpriseMesh.type(laag), (p, vc) -> m.teken(p, vc, l, -1, -1));
            }
        }
        if (!state.extra.isEmpty()) {
            BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
            for (int i = 0; i < state.extra.size(); i++) {
                float[] d = state.extraPlek.get(i);
                pose.pushPose();
                pose.translate(d[0], d[1], d[2]);
                dispatcher.submit(state.extra.get(i), pose, collector, camera);
                pose.popPose();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(SurpriseBlokEntity be) {
        BlockPos o = be.oorsprong();
        return o == null ? new AABB(be.getBlockPos()) : new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + Inhoud.N, o.getY() + Inhoud.N, o.getZ() + Inhoud.N);
    }
}
