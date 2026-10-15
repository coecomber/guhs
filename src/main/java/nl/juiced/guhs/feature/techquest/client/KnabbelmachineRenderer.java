package nl.juiced.guhs.feature.techquest.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.techquest.TechquestBlocks;
import nl.juiced.guhs.feature.techquest.TechquestBlocks.Deel;
import nl.juiced.guhs.feature.techquest.TechquestBlocks.Onderdeel;
import nl.juiced.guhs.feature.techquest.TechquestFeature;

/**
 * Draws De Grote Knabbelmachine on its kern block (the bowl), for the stage the LOCAL player has built: the scaffolding,
 * the foundation, the boiler, the shoulders, the head (asleep), and at last the machine awake, gnawing, with the knabbel of
 * today in the bowl. The block in the world is only the bowl; nothing else of the machine exists there, so any number of
 * players see their own machine on the same bordes.
 * <p>
 * The machine is seven blocks wide and nine high, and a block model only reaches from -16 to 32. So every part is cut
 * into cells of three blocks (tools/features/tech_quests_modellen.py): the block state {@link Deel#NR} of the helper block
 * is the model of one cell of one part, and this draws cell (i, j, k) three blocks further per step, turned the way the
 * kern looks (the models look north).
 */
public class KnabbelmachineRenderer implements BlockEntityRenderer<TechquestBlocks.Kern, KnabbelmachineRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private static final int CELLEN = Deel.CELLEN, DELEN = Onderdeel.values().length;
    /** How far the jaw drops while it gnaws (model pixels). */
    private static final float HAP = 5f;

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState[] cellen = new BlockModelRenderState[DELEN * CELLEN];
        float draai;
        float kaak;

        State() {
            for (int i = 0; i < cellen.length; i++) {
                cellen[i] = new BlockModelRenderState();
            }
        }
    }

    private final BlockModelResolver blokken;

    public KnabbelmachineRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TechquestBlocks.Kern be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        int fase = TechquestClient.fase();
        boolean knabbel = TechquestClient.knabbel();
        BlockState deel = TechquestFeature.KNABBELMACHINE_DEEL.get().defaultBlockState();
        for (Onderdeel o : Onderdeel.values()) {
            boolean zichtbaar = o.zichtbaar(fase) && (o != Onderdeel.KNABBEL || knabbel);
            for (int c = 0; c < CELLEN; c++) {
                int nr = o.ordinal() * CELLEN + c;
                state.cellen[nr].clear();
                if (zichtbaar) {
                    blokken.update(state.cellen[nr], deel.setValue(Deel.NR, nr), DISPLAY);
                }
            }
        }
        BlockState kern = be.getBlockState();
        // (the models look north; Direction.toYRot: south 0, west 90, north 180, east 270)
        state.draai = kern.hasProperty(HorizontalDirectionalBlock.FACING) ? 180f - kern.getValue(HorizontalDirectionalBlock.FACING).toYRot() : 0f;
        float tijd = be.getLevel() == null ? 0f : (be.getLevel().getGameTime() % 100000L) + partialTick;
        state.kaak = fase >= Onderdeel.OGEN_OPEN.van ? (0.5f + 0.5f * (float) Math.sin(tijd * 0.45f)) * HAP : 0f;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, 0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(state.draai));
        pose.translate(-0.5f, 0f, -0.5f);
        for (int nr = 0; nr < state.cellen.length; nr++) {
            BlockModelRenderState cel = state.cellen[nr];
            if (cel.isEmpty()) {
                continue;
            }
            int deel = nr / CELLEN, c = nr % CELLEN;
            int i = c / (Deel.NJ * Deel.NK), j = (c / Deel.NK) % Deel.NJ, k = c % Deel.NK;
            pose.pushPose();
            pose.translate((i - 1) * 3f, j * 3f - (deel == Onderdeel.KAAK.ordinal() ? state.kaak / 16f : 0f), k * 3f);
            cel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /** (the machine reaches three blocks to every side of the kern, six behind it and nine up, whichever way it is turned) */
    @Override
    public AABB getRenderBoundingBox(TechquestBlocks.Kern be) {
        return new AABB(be.getBlockPos()).inflate(8.0, 0.0, 8.0).expandTowards(0.0, 10.0, 0.0);
    }
}
