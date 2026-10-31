package nl.juiced.guhs.feature.kermisritten.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.kermisritten.KermisritBlockEntity;
import nl.juiced.guhs.feature.kermisritten.KermisritSoort;

/**
 * Draws what moves on a kermis ride, from the same numbers as the seats ({@link KermisritSoort}). Every part is one baked
 * block model (tools/features/kermisritten.py) built in units of 1/{@code schaal} block around its pivot, so a huge
 * Booster arm is a single cheap draw. The lights of the rides stay on at night.
 */
public class KermisritRenderer implements BlockEntityRenderer<KermisritBlockEntity, KermisritRenderer.State> {
    /** The ride lights: the block light they never drop under. */
    private static final int LAMPJES = 11;
    /** Part name to scale (the same table as MODELLEN in kermisritten.py). */
    private static final Map<String, Float> DELEN = new HashMap<>();
    private static final Map<String, StandaloneModelKey<BlockStateModelPart>> KEYS = new HashMap<>();
    static final String[] RAD_KLEUREN = {"aqua", "orchidee", "boter", "pistache", "perzik", "korenbloem", "framboos", "lavendel"};
    static final String[] ZWEEF_KLEUREN = {"aqua", "orchidee", "boter"};
    static final String[] TWIST_KLEUREN = {"boter", "perzik", "orchidee"};

    static {
        deel("loket", 2f);
        deel("booster_arm", 16f);
        deel("booster_gondel_0", 3f);
        deel("booster_gondel_1", 3f);
        deel("rad_wiel", 10f);
        for (String k : RAD_KLEUREN) {
            deel("rad_cabine_" + k, 3f);
        }
        for (String k : ZWEEF_KLEUREN) {
            deel("zweef_stoel_" + k, 5f);
        }
        deel("twister_ster", 8f);
        for (String k : TWIST_KLEUREN) {
            deel("twister_kruis_" + k, 4f);
        }
    }

    private static void deel(String naam, float schaal) {
        DELEN.put(naam, schaal);
        KEYS.put(naam, new StandaloneModelKey<>(() -> "guhs:block/kermisrit/" + naam));
    }

    public static class State extends BlockEntityRenderState {
        KermisritSoort soort = KermisritSoort.ZWEEFMOLEN;
        float yaw;
        double t;
    }

    public KermisritRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        for (Map.Entry<String, StandaloneModelKey<BlockStateModelPart>> e : KEYS.entrySet()) {
            event.register(e.getValue(), SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/kermisrit/" + e.getKey())));
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KermisritBlockEntity rit, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rit, state, partialTick, camera, breakProgress);
        state.soort = rit.soort();
        state.yaw = rit.facing().toYRot();
        state.t = rit.tijd(partialTick);
        if (rit.getLevel() != null) {
            // the controller itself sits in the dark under the ride: light from above it
            int licht = LevelRenderer.getLightCoords(rit.getLevel(), rit.getBlockPos().above(state.soort.hoogte + 1));
            state.lightCoords = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(licht), LAMPJES), LightCoordsUtil.sky(licht));
        }
    }

    private void teken(String naam, State state, PoseStack pose, SubmitNodeCollector collector) {
        float schaal = DELEN.get(naam);
        pose.pushPose();
        pose.scale(schaal, schaal, schaal);
        pose.translate(-0.5, -0.5, -0.5);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS.get(naam))),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        KermisritSoort s = state.soort;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));

        pose.pushPose();
        pose.translate(s.loket.x, s.loket.y + 0.5, s.loket.z);
        teken("loket", state, pose, collector);
        pose.popPose();

        switch (s) {
            case BOOSTER -> booster(s, state, pose, collector);
            case ZWEEFMOLEN -> zweefmolen(s, state, pose, collector);
            case REUZENRAD -> reuzenrad(s, state, pose, collector);
            case TWISTER -> twister(s, state, pose, collector);
        }
        pose.popPose();
    }

    private void booster(KermisritSoort s, State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0, KermisritSoort.BOOSTER_AS_Y, KermisritSoort.BOOSTER_AS_Z);
        pose.mulPose(Axis.ZP.rotation((float) s.armHoek(state.t)));
        teken("booster_arm", state, pose, collector);
        pose.popPose();
        for (int g = 0; g < 2; g++) {
            Vec3 m = s.gondel(g, state.t);
            pose.pushPose();
            pose.translate(m.x, m.y, m.z);
            teken("booster_gondel_" + g, state, pose, collector);
            pose.popPose();
        }
    }

    private void zweefmolen(KermisritSoort s, State state, PoseStack pose, SubmitNodeCollector collector) {
        float uit = (float) s.zweefUit(state.t);
        for (int i = 0; i < KermisritSoort.ZWEEF_STOELEN; i++) {
            pose.pushPose();
            pose.translate(0, KermisritSoort.ZWEEF_TOP, 0);
            pose.mulPose(Axis.YP.rotation((float) -s.zweefHoek(i, state.t)));
            pose.translate(KermisritSoort.ZWEEF_R, 0, 0);
            pose.mulPose(Axis.ZP.rotation(uit));
            teken("zweef_stoel_" + ZWEEF_KLEUREN[i % ZWEEF_KLEUREN.length], state, pose, collector);
            pose.popPose();
        }
    }

    private void reuzenrad(KermisritSoort s, State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0, KermisritSoort.RAD_AS_Y, 0);
        pose.mulPose(Axis.ZP.rotation((float) s.radHoek(state.t)));
        teken("rad_wiel", state, pose, collector);
        pose.popPose();
        for (int g = 0; g < KermisritSoort.RAD_CABINES; g++) {
            Vec3 m = s.cabine(g, state.t);
            pose.pushPose();
            pose.translate(m.x, m.y, m.z);
            teken("rad_cabine_" + RAD_KLEUREN[g], state, pose, collector);
            pose.popPose();
        }
    }

    private void twister(KermisritSoort s, State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0, 0.5, 0);
        pose.mulPose(Axis.YP.rotation((float) -s.twisterHoek(state.t)));
        teken("twister_ster", state, pose, collector);
        pose.popPose();
        for (int a = 0; a < KermisritSoort.TWIST_ARMEN; a++) {
            Vec3 n = s.twisterNaaf(a, state.t);
            pose.pushPose();
            pose.translate(n.x, 0.5, n.z);
            pose.mulPose(Axis.YP.rotation((float) -s.twisterKruis(a, state.t)));
            teken("twister_kruis_" + TWIST_KLEUREN[a], state, pose, collector);
            pose.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;      // far bigger than its block, and that block is buried
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(KermisritBlockEntity rit) {
        KermisritSoort s = rit.soort();
        return new AABB(rit.getBlockPos()).inflate(s.bereik, 0, s.bereik).expandTowards(0, s.hoogte + 24, 0);
    }
}
