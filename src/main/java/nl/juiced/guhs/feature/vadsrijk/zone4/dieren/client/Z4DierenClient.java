package nl.juiced.guhs.feature.vadsrijk.zone4.dieren.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.zone4.dieren.GuhVlieger;
import nl.juiced.guhs.feature.vadsrijk.zone4.dieren.OuweDobber;
import nl.juiced.guhs.feature.vadsrijk.zone4.dieren.Z4DierenRegistry;

/**
 * T-Z4DIEREN, the client: Ouwe Dobber (GeckoLib, his model is made at 1/4 size and drawn x4) and the guh kites (a few quads
 * per kite, no model: tinted diamond + fixed face layer, a ribbon tail and the string with a little sag down to a post; the wind
 * bob/sway/roll is computed here from the game time, so the server sends nothing). Level of detail for the kites: the tail only
 * within 64 blocks, the string in 10 pieces near and 2 far.
 */
public final class Z4DierenClient {
    static final Identifier DOBBER_TEX = Guhs.id("textures/vadsrijk/zone4/dieren/ouwe_dobber.png");
    static final Identifier VLIEGER_KLEUR = Guhs.id("textures/vadsrijk/zone4/dieren/vlieger_kleur.png");
    static final Identifier VLIEGER_VAST = Guhs.id("textures/vadsrijk/zone4/dieren/vlieger_vast.png");
    static final Identifier TOUW = Identifier.withDefaultNamespace("textures/block/white_wool.png");

    private Z4DierenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(Z4DierenRegistry.OUWE_DOBBER.get(), DobberRenderer::new);
            e.registerEntityRenderer(Z4DierenRegistry.VLIEGER.get(), VliegerRenderer::new);
        });
    }

    /** Ouwe Dobber: his GeckoLib model x4, turned along his heading. */
    public static class DobberRenderer extends GeoEntityRenderer<OuweDobber, EntityRenderState> {
        public DobberRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<OuweDobber>(Guhs.id("vadsrijk/zone4/dieren/ouwe_dobber")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return DOBBER_TEX;
                }
            });
            this.shadowRadius = 0f;
        }

        @Override
        protected AABB getBoundingBoxForCulling(OuweDobber d) {
            return d.getBoundingBoxForCulling();
        }

        @Override
        public void addRenderData(OuweDobber d, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, d.yRotO, d.getYRot()));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<EntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * 4f, heightScale * 4f);
        }
    }

    /** A guh kite on its string. */
    public static class VliegerRenderer extends EntityRenderer<GuhVlieger, VliegerRenderer.State> {
        public static class State extends EntityRenderState {
            int kleur, kleur2;
            float grootte;
            /** The anchor relative to the kite. */
            Vec3 anker = Vec3.ZERO;
            float tijd, fase;
        }

        public VliegerRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.shadowRadius = 0f;
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        protected AABB getBoundingBoxForCulling(GuhVlieger v) {
            return v.getBoundingBoxForCulling();
        }

        @Override
        public void extractRenderState(GuhVlieger v, State state, float partial) {
            super.extractRenderState(v, state, partial);
            state.kleur = v.kleur();
            state.kleur2 = v.kleur2();
            state.grootte = v.grootte();
            state.anker = v.anker().subtract(v.getPosition(partial));
            state.tijd = (float) (v.level().getGameTime() % 240000L) + partial;
            state.fase = (float) ((v.getX() * 0.37 + v.getZ() * 0.61) % 100);
        }

        @Override
        public void submit(State st, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            float g = st.grootte;
            float t = st.tijd + st.fase * 20;
            boolean dichtbij = st.distanceToCameraSq < 64 * 64;
            // facing the flyer (horizontal direction to the anchor), leaning back, rolling a little in the gusts
            Vec3 n = new Vec3(st.anker.x, 0, st.anker.z);
            n = n.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : n.normalize();
            Vec3 r = new Vec3(n.z, 0, -n.x);
            double kantel = Math.toRadians(28);
            Vec3 op = new Vec3(0, Math.cos(kantel), 0).add(n.scale(-Math.sin(kantel)));
            double rol = Math.toRadians(Math.sin(t * 0.043) * 12 + Math.sin(t * 0.11) * 3);
            Vec3 r2 = r.scale(Math.cos(rol)).add(op.scale(Math.sin(rol)));
            Vec3 op2 = op.scale(Math.cos(rol)).subtract(r.scale(Math.sin(rol)));
            double schaal = g / 3.0;
            Vec3 wieg = r.scale(Math.sin(t * 0.031) * 0.9 * schaal).add(0, Math.sin(t * 0.05) * 0.5 * schaal, 0)
                    .add(n.scale(-Math.sin(t * 0.027) * 0.4 * schaal));
            // the quad: 1 x 1.25 of the size; the diamond's centre (at 0.6 of the texture height) on the kite's spot
            Vec3 midden = wieg.add(op2.scale(0.125 * g));
            Vec3 hr = r2.scale(g / 2), ho = op2.scale(0.625 * g);
            Vec3 lb = midden.subtract(hr).add(ho), rb = midden.add(hr).add(ho), ro = midden.add(hr).subtract(ho), lo = midden.subtract(hr).subtract(ho);
            Vec3 normaal = n.scale(1);
            int licht = st.lightCoords;
            int kleur = 0xFF000000 | st.kleur;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(VLIEGER_KLEUR), (last, vc) -> vlak(vc, last, lb, rb, ro, lo, normaal, kleur, licht));
            collector.submitCustomGeometry(pose, RenderTypes.entityCutoutZOffset(VLIEGER_VAST), (last, vc) -> vlak(vc, last, lb, rb, ro, lo, normaal, -1, licht));
            // bottom tip of the diamond: string and tail start there
            Vec3 punt = wieg.subtract(op2.scale(0.5 * g));
            // the string: sagging a little, to the anchor; a little post at the anchor
            Vec3 anker = st.anker;
            int stukken = dichtbij ? 10 : 2;
            double zak = Math.min(3.0, anker.subtract(punt).length() * 0.04);
            Vec3 vorige = punt;
            for (int i = 1; i <= stukken; i++) {
                double s = i / (double) stukken;
                Vec3 q = punt.add(anker.subtract(punt).scale(s)).add(0, -zak * 4 * s * (1 - s), 0);
                buis(pose, collector, vorige, q, 0.02, 0xFFF4EEDC, licht);
                vorige = q;
            }
            buis(pose, collector, anker.add(0, -1.0, 0), anker.add(0, 0.12, 0), 0.06, 0xFF9A7454, licht);
            // the ribbon tail with bows, waving downwind (away from the flyer)
            if (dichtbij) {
                int kleur2 = 0xFF000000 | st.kleur2;
                Vec3 p0 = punt;
                for (int i = 1; i <= 6; i++) {
                    double golf = Math.sin(t * 0.2 - i * 0.8) * 0.18 * i * schaal;
                    Vec3 p1 = punt.add(n.scale(-0.35 * i * schaal)).add(0, -0.45 * i * schaal, 0).add(r.scale(golf));
                    buis(pose, collector, p0, p1, 0.015, 0xFFF4EEDC, licht);
                    strik(pose, collector, p1, r, op, 0.22 * schaal, kleur2, licht);
                    p0 = p1;
                }
            }
            super.submit(st, pose, collector, camera);
        }

        private static void vlak(VertexConsumer vc, PoseStack.Pose pose, Vec3 lb, Vec3 rb, Vec3 ro, Vec3 lo, Vec3 n, int argb, int licht) {
            punt(vc, pose, lb, 0, 0, n, argb, licht);
            punt(vc, pose, lo, 0, 1, n, argb, licht);
            punt(vc, pose, ro, 1, 1, n, argb, licht);
            punt(vc, pose, rb, 1, 0, n, argb, licht);
        }

        /** A little bow: two small squares in a cross. */
        private static void strik(PoseStack pose, SubmitNodeCollector collector, Vec3 c, Vec3 r, Vec3 op, double h, int argb, int licht) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TOUW), (last, vc) -> {
                for (int k = -1; k <= 1; k += 2) {
                    Vec3 a = r.scale(k * h * 1.6), b = op.scale(h * 0.8);
                    Vec3 n = r.cross(op);
                    punt(vc, last, c.add(b), 0, 0, n, argb, licht);
                    punt(vc, last, c.subtract(b), 0, 0.1f, n, argb, licht);
                    punt(vc, last, c.add(a).subtract(b), 0.1f, 0.1f, n, argb, licht);
                    punt(vc, last, c.add(a).add(b), 0.1f, 0, n, argb, licht);
                }
            });
        }

        /** A thin square tube from a to b. */
        private static void buis(PoseStack pose, SubmitNodeCollector collector, Vec3 a, Vec3 b, double rad, int argb, int licht) {
            Vec3 dir = b.subtract(a);
            if (dir.lengthSqr() < 1e-6) {
                return;
            }
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TOUW), (last, vc) -> {
                Vec3 nn = dir.normalize();
                Vec3 side = Math.abs(nn.y) > 0.9 ? new Vec3(1, 0, 0) : nn.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 up = side.cross(nn).normalize();
                Vec3[] hoek = {side.scale(rad).add(up.scale(rad)), side.scale(-rad).add(up.scale(rad)), side.scale(-rad).add(up.scale(-rad)),
                        side.scale(rad).add(up.scale(-rad))};
                for (int i = 0; i < 4; i++) {
                    Vec3 h0 = hoek[i], h1 = hoek[(i + 1) % 4];
                    Vec3 normaal = h0.add(h1).normalize();
                    punt(vc, last, a.add(h0), 0, 0, normaal, argb, licht);
                    punt(vc, last, b.add(h0), 0, 0.1f, normaal, argb, licht);
                    punt(vc, last, b.add(h1), 0.1f, 0.1f, normaal, argb, licht);
                    punt(vc, last, a.add(h1), 0.1f, 0, normaal, argb, licht);
                }
            });
        }

        private static void punt(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int argb, int licht) {
            vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(licht).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
        }
    }
}
