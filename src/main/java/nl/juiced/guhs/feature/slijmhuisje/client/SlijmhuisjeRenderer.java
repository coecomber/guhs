package nl.juiced.guhs.feature.slijmhuisje.client;

import java.util.List;

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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.slijmhuisje.SlijmhuisjeBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * What moves in a Slijmhuisje (the house, the dome, the cushion and the bowl are block models): the guh slime on its
 * lavender cushion, built like the entity (model {@code slijmhuisje_slijm_binnen}: the inner cube with eyes and mouth,
 * cutout; {@code slijmhuisje_slijm_buiten}: the jelly around it, translucent; the real guh slime texture), and the pink
 * slimeballs in the mint bowl in front of the door (model {@code slijmhuisje_balletje}, one per 4, at most 4).
 * <p>
 * Awake (vadskracht) the slime bounces gently: a squash when it lands, a stretch in the air. Asleep it sits a bit flattened
 * and breathes slowly. Its size is the real size (1, 2, 4) scaled to fit under the dome ({@link #schaal}); a big one only
 * squashes, it has no room to hop. Positions match tools/features/slijmhuisje.py ("the look").
 */
public class SlijmhuisjeRenderer implements BlockEntityRenderer<SlijmhuisjeBlockEntity, SlijmhuisjeRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> SLIJM_BINNEN = new StandaloneModelKey<>(() -> "guhs:block/slijmhuisje_slijm_binnen");
    public static final StandaloneModelKey<BlockStateModelPart> SLIJM_BUITEN = new StandaloneModelKey<>(() -> "guhs:block/slijmhuisje_slijm_buiten");
    public static final StandaloneModelKey<BlockStateModelPart> BALLETJE = new StandaloneModelKey<>(() -> "guhs:block/slijmhuisje_balletje");
    /** Model space (facing north, the kern at 0..1): the top of the cushion (its middle is one block behind the kern). */
    private static final float KUSSEN_Y = 28f / 16f, KUSSEN_Z = 1.5f;
    /** How high (blocks) the slime may reach above the cushion under the dome. */
    private static final float RUIMTE = 1.1f;
    /** The slimeballs in the bowl: x, y, z in pixels of the kern (the bowl is at x 3..13, z -15..-5, its floor at y 1). */
    private static final int[][] BALLETJES = {{8, 1, -10}, {6, 1, -12}, {8, 1, -13}, {6, 2, -9}};

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int grootte, schaal;
        boolean wakker;
        float fase;
    }

    public SlijmhuisjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the two models that are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(SLIJM_BINNEN, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/slijmhuisje_slijm_binnen")));
        event.register(SLIJM_BUITEN, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/slijmhuisje_slijm_buiten")));
        event.register(BALLETJE, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/slijmhuisje_balletje")));
    }

    /** How big the slime is drawn (blocks): the real size, scaled to fit under the dome. */
    static float schaal(int grootte) {
        return grootte >= 4 ? 1.0f : grootte >= 2 ? 0.8f : 0.5f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SlijmhuisjeBlockEntity huisje, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(huisje, state, partialTick, camera, breakProgress);
        state.facing = huisje.voor();
        state.grootte = huisje.grootte();
        state.schaal = huisje.schaal();
        state.wakker = huisje.snoet() != Snoet.SLAAPT;
        long tijd = huisje.getLevel() == null ? 0 : huisje.getLevel().getGameTime() + huisje.getBlockPos().asLong() % 40;
        state.fase = ((tijd % 24) + partialTick) / 24f;   // one bounce per 24 ticks
        if (huisje.getLevel() != null) {
            state.lightCoords = Math.max(state.lightCoords, LevelRenderer.getLightCoords(huisje.getLevel(), huisje.kussen()));
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        if (state.grootte > 0) {
            float s = schaal(state.grootte), hoogte = 0f, plat, breed;
            if (state.wakker) {
                float sprong = (float) Math.sin(state.fase * Math.PI);    // 0 on the cushion .. 1 at the top
                plat = 0.84f + 0.24f * sprong;                             // squashed when it lands, stretched in the air
                breed = 1.08f - 0.1f * sprong;
                hoogte = sprong * Math.max(0f, Math.min(0.25f * s, RUIMTE - s * 1.08f));
            } else {
                float adem = (float) Math.sin(state.fase * Math.PI * 2) * 0.03f;
                plat = 0.8f + adem;
                breed = 1.1f - adem;
            }
            float k = s / 0.5f;                                            // (the model is 8 px = half a block wide)
            pose.pushPose();
            pose.translate(0.5, KUSSEN_Y + hoogte, KUSSEN_Z);
            pose.scale(k * breed, k * plat, k * breed);
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(SLIJM_BINNEN)),
                    BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            collector.submitBlockModel(pose, Sheets.translucentBlockItemSheet(), List.of(models.getStandaloneModel(SLIJM_BUITEN)),
                    BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        int n = Math.min(BALLETJES.length, (state.schaal + 3) / 4);
        for (int i = 0; i < n; i++) {
            pose.pushPose();
            pose.translate(BALLETJES[i][0] / 16f, BALLETJES[i][1] / 16f, BALLETJES[i][2] / 16f);
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(BALLETJE)),
                    BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SlijmhuisjeBlockEntity huisje) {
        return new AABB(huisje.getBlockPos()).inflate(2.0, 3.0, 2.0);
    }
}
