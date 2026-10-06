package nl.juiced.guhs.feature.guhriow3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.client.DoosModel;
import nl.juiced.guhs.feature.guhrio.client.GuhrioClient;
import nl.juiced.guhs.feature.guhriow3.GroteNetherMikaEntity;
import nl.juiced.guhs.feature.guhriow3.GuhrioW3Feature;
import nl.juiced.guhs.feature.guhriow3.KooltjeEntity;
import nl.juiced.guhs.feature.guhriow3.TaartEntity;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Client side of bbq2 (guhrio-w3): how the Grote Nether-Mika, his coals and the cake of the end scene are drawn. Each is a
 * box model of the engine's kind ({@link DoosModel}; the boxes, the textures and a preview picture come from
 * tools/features/guhrio_w3_modellen.py), drawn part by part with a little movement worked out here.
 */
public final class GuhrioW3Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(GuhrioW3Feature.GROTE_NETHER_MIKA.get(), Mika::new);
            event.registerEntityRenderer(GuhrioW3Feature.KOOLTJE.get(), Kooltje::new);
            event.registerEntityRenderer(GuhrioW3Feature.TAART.get(), Taart::new);
        });
    }

    private GuhrioW3Client() {
    }

    /** What these renderers need. */
    public static class Staat extends EntityRenderState {
        float yaw, t, loop, rol, animT;
        int stand, kijk, getal;
        String anim = "";
        Direction langs = Direction.EAST;
        boolean inLevel;
    }

    private abstract static class Basis<E extends net.minecraft.world.entity.Entity> extends EntityRenderer<E, Staat> {
        protected final DoosModel model;

        Basis(EntityRendererProvider.Context context, String model, float schaduw) {
            super(context);
            this.model = DoosModel.van(model);
            this.shadowRadius = schaduw;
        }

        @Override
        public Staat createRenderState() {
            return new Staat();
        }

        @Override
        public void submit(Staat state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (!state.isInvisible) {
                pose.pushPose();
                teken(state, pose, collector);
                pose.popPose();
            }
            super.submit(state, pose, collector, camera);
        }

        protected abstract void teken(Staat state, PoseStack pose, SubmitNodeCollector collector);

        /** One part, moved (pixels). */
        protected void deel(String naam, Staat state, PoseStack pose, SubmitNodeCollector collector, float dx, float dy, float dz) {
            pose.pushPose();
            pose.translate(dx, dy, dz);
            model.dien(naam, pose, collector, state.lightCoords);
            pose.popPose();
        }
    }

    /**
     * The yaw to draw somebody with who faces {@code yaw}: in a level turned half way to the side the camera stands on (you
     * see his face and the shell on his back); outside a level (a scene watched again from the Guhdex) simply the way he looks.
     */
    static float naarCamera(float yaw) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
            return yaw;
        }
        Vec3 c = baan.naarCamera(0);
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        double x = fx + c.x * 0.9, z = fz + c.z * 0.9;
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    /** The Grote Nether-Mika (see {@link GroteNetherMikaEntity} for his stands). */
    public static class Mika extends Basis<GroteNetherMikaEntity> {
        /** The model is drawn this much bigger than its pixels. */
        public static final float SCHAAL = 1.6f;

        public Mika(EntityRendererProvider.Context context) {
            super(context, "grote_nether_mika", 1.1f);
        }

        @Override
        public void extractRenderState(GroteNetherMikaEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.stand = e.stand();
            state.kijk = e.kijk();
            state.t = e.standTicks + partialTick;
            state.loop = Mth.lerp(partialTick, e.loopO, e.loop);
            state.rol = Mth.lerp(partialTick, e.rolO, e.rol);
            state.yaw = Mth.rotLerp(partialTick, e.yRotO, e.getYRot());
            state.langs = e.langs();
            state.anim = Cutscenes.animatie(e);
            state.animT = Cutscenes.animatieTicks(e) + partialTick;
            state.inLevel = GuhrioClient.baan() != null;
        }

        @Override
        protected boolean affectedByCulling(GroteNetherMikaEntity e) {
            return false;
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            int stand = state.stand;
            float px = SCHAAL / 16f;
            if (GroteNetherMikaEntity.schild(stand) && !(stand == GroteNetherMikaEntity.TREKT_IN && state.t < 7)) {
                schild(state, pose, collector, px);
                return;
            }
            pose.mulPose(Axis.YP.rotationDegrees(-naarCamera(state.yaw)));
            boolean zit = stand >= GroteNetherMikaEntity.PLONS;
            boolean blij = zit && "blij".equals(state.anim), op = zit && "op".equals(state.anim);
            float t = state.t;
            // how tall he is drawn (a crouch, a landing, pulling in)
            float hoog = 1f, lijfY = 0f, armY = 0f, armZ = 0f, armX = 0f, voetY = 0f, kopKnik = 0f, kopY = 0f, tril = 0f;
            float stap = 0f;
            switch (stand) {
                case GroteNetherMikaEntity.INTRO -> {
                    kopKnik = -18f * (float) Math.min(1.0, Math.max(0.0, (t - 8) / 8.0));
                    armY = 4f;
                    armX = 2f;
                    tril = t > 10 && t < 50 ? (float) Math.sin(t * 2.4f) * 0.5f : 0f;
                }
                case GroteNetherMikaEntity.LOOPT -> {
                    stap = (float) Math.sin(state.loop);
                    lijfY = Math.abs(stap) * 0.7f;
                }
                case GroteNetherMikaEntity.GOOIT -> {
                    float k = (float) Math.sin(Math.min(1.0, t / GroteNetherMikaEntity.GOOI_TICKS) * Math.PI);
                    armY = 7f * k;
                    armZ = 3f * k;
                }
                case GroteNetherMikaEntity.HURKT -> hoog = 1f - 0.2f * (float) Math.min(1.0, t / 6.0);
                case GroteNetherMikaEntity.SPRINGT -> {
                    hoog = 1.05f;
                    armY = 5f;
                    voetY = 2f;
                }
                case GroteNetherMikaEntity.LANDT -> hoog = 1f - 0.22f * (float) Math.max(0.0, 1.0 - t / 8.0);
                case GroteNetherMikaEntity.SCHRIKT -> {
                    armY = 6f;
                    armX = 2f;
                    kopKnik = -10f;
                    tril = (float) Math.sin(t * 2.2f) * 0.4f;
                }
                case GroteNetherMikaEntity.WACHT -> {
                    lijfY = (float) Math.sin(t * 0.25f) * 0.5f;
                    armY = 2f + (float) Math.sin(t * 0.5f) * 2f;
                }
                case GroteNetherMikaEntity.TREKT_IN -> hoog = 1f - 0.5f * (float) Math.min(1.0, t / 7.0);
                default -> {
                }
            }
            if (zit) {
                // he sits: arms crossed, chin on his chest; looking up when the princess talks; a hop when he gets his cake
                lijfY = -4f + (blij ? Math.abs((float) Math.sin(state.animT * 0.45f)) * 3f : (float) Math.sin(t * 0.08f) * 0.3f);
                kopKnik = blij ? -12f : op ? -8f : 12f;
                kopY = -1f;
                if (blij) {
                    armY = 6f;
                    armX = 2f;
                } else {
                    armX = -4f;
                    armZ = 6f;
                    armY = -2f;
                }
            }
            pose.scale(px, px * hoog, px);
            pose.translate(tril, 0, 0);
            if (zit) {
                // legs stretched out in front of him
                deel("voet_l", state, pose, collector, 0, 0, 7);
                deel("voet_r", state, pose, collector, 0, 0, 7);
            } else {
                deel("voet_l", state, pose, collector, 0, voetY, stap * 2.2f);
                deel("voet_r", state, pose, collector, 0, voetY, -stap * 2.2f);
            }
            deel("staart", state, pose, collector, 0, lijfY, 0);
            deel("lijf", state, pose, collector, 0, lijfY, 0);
            deel("schild", state, pose, collector, 0, lijfY, 0);
            deel("arm_l", state, pose, collector, armX, lijfY + armY, armZ);
            deel("arm_r", state, pose, collector, -armX, lijfY + armY, armZ);
            // the head nods round the neck
            pose.pushPose();
            pose.translate(0, 16 + lijfY + kopY, 0);
            pose.mulPose(Axis.XP.rotationDegrees(kopKnik));
            pose.translate(0, -16, 0);
            model.dien("hoofd", pose, collector, state.lightCoords);
            model.dien(blij ? "gezicht_blij" : zit ? "gezicht_mok" : "gezicht_boos", pose, collector, state.lightCoords);
            pose.popPose();
        }

        /** Tucked in: the spiked shell, rolling like a wheel along the lane; braked, his head peeks out of it. */
        private void schild(Staat state, PoseStack pose, SubmitNodeCollector collector, float px) {
            Direction d = state.langs;
            // (the model's own front, +z, points further along the lane: the wheel turns round the model's x)
            float langsYaw = (float) Math.toDegrees(Math.atan2(-d.getStepX(), d.getStepZ()));
            float midden = GroteNetherMikaEntity.SCHILD_HOOG / 2f;
            pose.translate(0, midden, 0);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-langsYaw));
            pose.mulPose(Axis.XP.rotationDegrees(state.rol));
            pose.scale(px, px, px);
            model.dien("bol", pose, collector, state.lightCoords);
            pose.popPose();
            if (state.stand == GroteNetherMikaEntity.REMT || state.stand == GroteNetherMikaEntity.WANKELT) {
                // his head out of the shell: a look at you (dizzy when he teeters at the edge)
                float uit = (float) Math.min(1.0, state.t / 6.0);
                pose.mulPose(Axis.YP.rotationDegrees(-naarCamera(state.yaw)));
                if (state.stand == GroteNetherMikaEntity.WANKELT) {
                    pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(state.t * 0.6f) * 14f));
                }
                pose.scale(px, px, px);
                pose.translate(0, -16 + 2 * uit, 3 * uit);
                model.dien("hoofd", pose, collector, state.lightCoords);
                model.dien(state.stand == GroteNetherMikaEntity.WANKELT ? "gezicht_mok" : "gezicht_boos", pose, collector, state.lightCoords);
            }
        }
    }

    /** A slow glowing coal, tumbling. */
    public static class Kooltje extends Basis<KooltjeEntity> {
        public Kooltje(EntityRendererProvider.Context context) {
            super(context, "guhriow3_kooltje", 0.2f);
        }

        @Override
        public void extractRenderState(KooltjeEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.rol = Mth.lerp(partialTick, e.rolO, e.rol);
            state.t = e.tickCount + partialTick;
            // (it glows: never darker than the sauce it came from)
            state.lightCoords = 0xF000F0;
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.translate(0, 0.3, 0);
            pose.mulPose(Axis.YP.rotationDegrees(state.rol));
            pose.mulPose(Axis.XP.rotationDegrees(state.rol * 0.6f));
            float puls = 1f + (float) Math.sin(state.t * 0.5f) * 0.06f;
            pose.scale(puls / 16f, puls / 16f, puls / 16f);
            model.dien("kool", pose, collector, state.lightCoords);
        }
    }

    /** The cake of the end scene: a plate and four quarters (one gone once the Grote Nether-Mika has had his piece). */
    public static class Taart extends Basis<TaartEntity> {
        public Taart(EntityRendererProvider.Context context) {
            super(context, "guhriow3_taart", 0.3f);
        }

        @Override
        public void extractRenderState(TaartEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.yaw = Mth.rotLerp(partialTick, e.yRotO, e.getYRot());
            state.getal = Math.min(3, e.stukjes() + ("stukje".equals(Cutscenes.animatie(e)) ? 1 : 0));
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.mulPose(Axis.YP.rotationDegrees(-naarCamera(state.yaw)));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            model.dien("bord", pose, collector, state.lightCoords);
            for (int i = state.getal; i < 4; i++) {
                model.dien("punt_" + i, pose, collector, state.lightCoords);
            }
        }
    }
}
