package nl.juiced.guhs.feature.guhrio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GrillspiesEntity;
import nl.juiced.guhs.feature.guhrio.GuhmbaEntity;
import nl.juiced.guhs.feature.guhrio.HapbloemEntity;
import nl.juiced.guhs.feature.guhrio.KnabbelEntity;
import nl.juiced.guhs.feature.guhrio.PlatformEntity;
import nl.juiced.guhs.feature.guhrio.PlofMikaEntity;
import nl.juiced.guhs.feature.guhrio.SchildMikaEntity;
import nl.juiced.guhs.feature.guhrio.ValblokEntity;

/**
 * How the creatures and moving things of Super Guhrio are drawn: each from its own box model ({@link DoosModel}; the
 * boxes, the textures and a preview come from tools/features/guhrio_modellen.py), part by part, with a little movement
 * worked out here. They are made to be seen from the side: while you are in a level the walkers turn their face three
 * quarters towards your camera ({@link #naarCamera}), like the creatures of the old games that always look at you.
 */
public final class WezenRenderers {
    private WezenRenderers() {
    }

    /** What every one of these renderers needs. */
    public static class Staat extends EntityRenderState {
        float yaw, a, b, c;
        int stand, getal;
        Direction langs = Direction.EAST;
    }

    /**
     * The yaw to draw a walker with: outside a level simply the way it walks; in a level turned most of the way to the
     * side the camera stands on, so you see its face.
     */
    static float naarCamera(float yaw) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
            return yaw;
        }
        Vec3 c = baan.naarCamera(BaanBesturing.stukNu());
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        double x = fx * 0.8 + c.x * 1.2, z = fz * 0.8 + c.z * 1.2;
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    /**
     * The yaw of a creature that always shows its face: in a level straight at the side the camera stands on, outside a
     * level (a summoned one, a spawn egg) simply the way the entity itself looks.
     */
    static float naarKijker(float yaw) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
            return yaw;
        }
        Vec3 c = baan.naarCamera(BaanBesturing.stukNu());
        return (float) Math.toDegrees(Math.atan2(-c.x, c.z));
    }

    /** The yaw that puts a model's +x along {@code langs} (and so its front, +z, towards the camera of a "rechts" lane). */
    static float langsYaw(Direction langs) {
        return (float) Math.toDegrees(Math.atan2(langs.getStepZ(), langs.getStepX()));
    }

    private abstract static class Basis<E extends Entity> extends EntityRenderer<E, Staat> {
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

        /** One part, moved (pixels) and scaled around the model's own origin. */
        protected void deel(String naam, Staat state, PoseStack pose, SubmitNodeCollector collector, float dx, float dy, float dz) {
            pose.pushPose();
            pose.translate(dx, dy, dz);
            model.dien(naam, pose, collector, state.lightCoords);
            pose.popPose();
        }
    }

    /** The Guhmba: a round grumpy mini-Mika on two big feet; flat and wide once you landed on it. */
    public static class Guhmba extends Basis<GuhmbaEntity> {
        public Guhmba(EntityRendererProvider.Context context) {
            super(context, "guhmba", 0.4f);
        }

        @Override
        public void extractRenderState(GuhmbaEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.yaw = naarCamera(Mth.rotLerp(partialTick, e.yRotO, e.getYRot()));
            state.a = Mth.lerp(partialTick, e.platheidO, e.platheid);
            state.b = Mth.lerp(partialTick, e.loopO, e.loop);
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            float plat = state.a;
            pose.scale(Mth.lerp(plat, 1f, 1.3f) / 16f, Mth.lerp(plat, 1f, 0.22f) / 16f, Mth.lerp(plat, 1f, 1.3f) / 16f);
            float stap = (float) Math.sin(state.b) * (1f - plat);
            float wieg = Math.abs(stap) * 0.6f;
            deel("voet_l", state, pose, collector, 0, 0, stap * 1.5f);
            deel("voet_r", state, pose, collector, 0, 0, -stap * 1.5f);
            deel("lijf", state, pose, collector, 0, wieg, 0);
            deel("oren", state, pose, collector, 0, wieg, 0);
        }
    }

    /** The Schild-Mika: a Mika with a green shell on its back; pulled in, only the shell is left (and spins when it slides). */
    public static class SchildMika extends Basis<SchildMikaEntity> {
        public SchildMika(EntityRendererProvider.Context context) {
            super(context, "schild_mika", 0.4f);
        }

        @Override
        public void extractRenderState(SchildMikaEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.yaw = naarCamera(Mth.rotLerp(partialTick, e.yRotO, e.getYRot()));
            state.a = Mth.lerp(partialTick, e.ingetrokkenO, e.ingetrokken);
            state.b = Mth.lerp(partialTick, e.loopO, e.loop);
            state.c = Mth.lerp(partialTick, e.tolO, e.tol);
            state.stand = e.stand();
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            float in = state.a;
            if (in > 0.6f) {
                // only the shell, lying flat in the middle (spinning while it slides)
                pose.mulPose(Axis.YP.rotationDegrees(state.c - state.yaw));
                pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
                deel("schild", state, pose, collector, 0, -3, 3.5f);
                return;
            }
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            float stap = (float) Math.sin(state.b) * (1f - in);
            float wieg = Math.abs(stap) * 0.5f - in * 3f;
            deel("voet_l", state, pose, collector, 0, 0, stap * 1.5f);
            deel("voet_r", state, pose, collector, 0, 0, -stap * 1.5f);
            deel("lijf", state, pose, collector, 0, wieg, -in * 4f);
            deel("oren", state, pose, collector, 0, wieg, -in * 4f);
            deel("schild", state, pose, collector, 0, 0, 0);
        }
    }

    /** The Plof-Mika: a square block of stone with a furious Mika face, ears and studs; it trembles before it drops. */
    public static class PlofMika extends Basis<PlofMikaEntity> {
        public PlofMika(EntityRendererProvider.Context context) {
            super(context, "plof_mika", 0.9f);
        }

        @Override
        public void extractRenderState(PlofMikaEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.yaw = naarKijker(Mth.rotLerp(partialTick, e.yRotO, e.getYRot()));
            state.stand = e.stand();
            state.a = e.tickCount + partialTick;
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            float tril = state.stand == PlofMikaEntity.TRILT ? (float) Math.sin(state.a * 2.6f) * 0.8f : 0f;
            // (hanging it scowls with its eyes half shut; awake = the other face)
            deel(state.stand == PlofMikaEntity.HANGT || state.stand == PlofMikaEntity.STIJGT ? "blok" : "blok_boos", state, pose, collector, tril, 0, 0);
            deel("oren", state, pose, collector, tril, 0, 0);
            deel("noppen", state, pose, collector, tril, 0, 0);
        }
    }

    /** The Hapbloem: a stem with two leaves and a big spotted head whose two halves chomp. */
    public static class Hapbloem extends Basis<HapbloemEntity> {
        public Hapbloem(EntityRendererProvider.Context context) {
            super(context, "hapbloem", 0f);
        }

        @Override
        public void extractRenderState(HapbloemEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.yaw = naarKijker(Mth.rotLerp(partialTick, e.yRotO, e.getYRot()));
            state.a = Mth.lerp(partialTick, e.hapO, e.hap);
            state.b = e.uit();
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            if (state.b <= 0.02f) {
                return;                                        // (all the way down in its pipe)
            }
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            deel("steel", state, pose, collector, 0, 0, 0);
            float open = (float) (Math.sin(state.a) * 0.5 + 0.5) * 24f + 4f;
            for (int kant = -1; kant <= 1; kant += 2) {
                pose.pushPose();
                pose.translate(0, 12, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(-kant * open));
                pose.translate(0, -12, 0);
                model.dien(kant < 0 ? "kaak_l" : "kaak_r", pose, collector, state.lightCoords);
                pose.popPose();
            }
        }
    }

    /** The grill spit: a hub cap and a skewer of iron with chunks of meat and pepper, turning like a clock hand. */
    public static class Grillspies extends Basis<GrillspiesEntity> {
        public Grillspies(EntityRendererProvider.Context context) {
            super(context, "guhrio_grillspies", 0f);
        }

        @Override
        public void extractRenderState(GrillspiesEntity e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.langs = e.langs();
            state.getal = e.lengte();
            state.a = e.hoek(partialTick);
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.translate(0, state.getal + 0.5, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-langsYaw(state.langs)));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            deel("naaf", state, pose, collector, 0, 0, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(state.a));
            for (int i = 0; i < state.getal; i++) {
                deel("stok", state, pose, collector, 8 + 16 * i, 0, 0);
                deel(i % 2 == 0 ? "brok_a" : "brok_b", state, pose, collector, 8 + 16 * i, 0, 0);
            }
            deel("punt", state, pose, collector, 8 + 16 * state.getal, 0, 0);
        }

        @Override
        protected boolean affectedByCulling(GrillspiesEntity e) {
            return false;
        }
    }

    /** A moving platform / a falling block: a deck of one to four blocks along the lane. */
    public static class Dek<E extends Entity> extends Basis<E> {
        public Dek(EntityRendererProvider.Context context, String model) {
            super(context, model, 0f);
        }

        @Override
        public void extractRenderState(E e, Staat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            if (e instanceof PlatformEntity p) {
                state.langs = p.langs();
                state.getal = p.breed();
                state.stand = 0;
            } else if (e instanceof ValblokEntity v) {
                state.langs = v.langs();
                state.getal = v.breed();
                state.stand = v.stand();
            }
            state.a = e.tickCount + partialTick;
        }

        @Override
        protected void teken(Staat state, PoseStack pose, SubmitNodeCollector collector) {
            pose.mulPose(Axis.YP.rotationDegrees(-langsYaw(state.langs)));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            float tril = state.stand == ValblokEntity.TRILT ? (float) Math.sin(state.a * 2.8f) * 0.7f : 0f;
            int n = Math.max(1, state.getal);
            for (int i = 0; i < n; i++) {
                deel("midden", state, pose, collector, (i - (n - 1) / 2f) * 16f, tril, 0);
            }
            deel("kap_l", state, pose, collector, -(n - 1) / 2f * 16f, tril, 0);
            deel("kap_r", state, pose, collector, (n - 1) / 2f * 16f, tril, 0);
        }
    }

    /** A thrown knabbel: the kaas knabbel item, tumbling. */
    public static class Knabbel extends EntityRenderer<KnabbelEntity, Knabbel.KnabbelStaat> {
        public static class KnabbelStaat extends EntityRenderState {
            final ItemStackRenderState item = new ItemStackRenderState();
            float rol;
        }

        private final ItemModelResolver items;

        public Knabbel(EntityRendererProvider.Context context) {
            super(context);
            this.items = context.getItemModelResolver();
        }

        @Override
        public KnabbelStaat createRenderState() {
            return new KnabbelStaat();
        }

        @Override
        public void extractRenderState(KnabbelEntity e, KnabbelStaat state, float partialTick) {
            super.extractRenderState(e, state, partialTick);
            state.rol = Mth.lerp(partialTick, e.rolO, e.rol);
            items.updateForNonLiving(state.item, new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get()), ItemDisplayContext.GROUND, e);
        }

        @Override
        public void submit(KnabbelStaat state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose();
            pose.translate(0, 0.2, 0);
            pose.mulPose(Axis.YP.rotationDegrees(state.rol));
            pose.mulPose(Axis.XP.rotationDegrees(state.rol * 0.7f));
            pose.scale(1.4f, 1.4f, 1.4f);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
            super.submit(state, pose, collector, camera);
        }
    }
}
