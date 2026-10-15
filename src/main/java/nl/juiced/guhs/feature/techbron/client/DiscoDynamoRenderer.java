package nl.juiced.guhs.feature.techbron.client;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.DiscoDynamoBlockEntity;
import nl.juiced.guhs.feature.techbron.LichtShow;

/**
 * The Disco-dynamo's light show and its turning disc. The floor itself (frame, dark glass) and the DJ desk are ordinary
 * block models (the kern and its eight floor parts); this draws, while a disc is on the turntable, the 6 x 6 lamps in the
 * colours of that disc's {@link LichtShow} (the model {@code disco_dynamo_lampen}: four lamps of one floor tile, each with
 * its own tint index, drawn once per tile at full brightness; the kern's tile only has the two lamps behind the desk) and
 * the disc lying on the desk, going round.
 */
public class DiscoDynamoRenderer implements BlockEntityRenderer<DiscoDynamoBlockEntity, DiscoDynamoRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> LAMPEN = new StandaloneModelKey<>(() -> "guhs:block/disco_dynamo_lampen");
    public static final StandaloneModelKey<BlockStateModelPart> LAMPEN_KERN = new StandaloneModelKey<>(() -> "guhs:block/disco_dynamo_lampen_kern");
    /** Where the disc lies, in model pixels of the kern (see tools/features/tech_bronnen.py: the platter on the desk). */
    private static final float PLAAT_X = 8f / 16f, PLAAT_Y = 12.3f / 16f, PLAAT_Z = 3f / 16f, PLAAT_SCHAAL = 0.34f;
    private static final int TEGELS = LichtShow.RASTER / 2;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean aan;
        /** Per floor tile (row 0 = the back, column 0 = left seen from the front): the colours of its four lamps. */
        final int[][] tinten = new int[TEGELS * TEGELS][4];
        final ItemStackRenderState plaat = new ItemStackRenderState();
        float draai;
    }

    private final ItemModelResolver items;

    public DiscoDynamoRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    /** Mod bus (client): the lamp models, which are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(LAMPEN, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/disco_dynamo_lampen")));
        event.register(LAMPEN_KERN, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/disco_dynamo_lampen_kern")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DiscoDynamoBlockEntity disco, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(disco, state, partialTick, camera, breakProgress);
        state.facing = disco.getBlockState().getValue(BronBlock.FACING);
        state.plaat.clear();
        state.aan = !disco.plaat().isEmpty();
        if (!state.aan || disco.getLevel() == null) {
            return;
        }
        float tijd = (disco.getLevel().getGameTime() % 24000L) + partialTick;
        LichtShow show = disco.show();
        for (int tegel = 0; tegel < TEGELS * TEGELS; tegel++) {
            int rij = tegel / TEGELS * 2, kolom = tegel % TEGELS * 2;
            // (the lamp order of the model: back left, back right, front left, front right)
            state.tinten[tegel][0] = show.kleur(kolom, rij, tijd);
            state.tinten[tegel][1] = show.kleur(kolom + 1, rij, tijd);
            state.tinten[tegel][2] = show.kleur(kolom, rij + 1, tijd);
            state.tinten[tegel][3] = show.kleur(kolom + 1, rij + 1, tijd);
        }
        state.draai = tijd * 6f;
        items.updateForTopItem(state.plaat, disco.plaat(), ItemDisplayContext.FIXED, disco.getLevel(), null, (int) disco.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.aan) {
            return;
        }
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        // like a block model (models face north by default)
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        for (int tegel = 0; tegel < TEGELS * TEGELS; tegel++) {
            int rij = tegel / TEGELS, kolom = tegel % TEGELS;
            boolean kern = rij == TEGELS - 1 && kolom == TEGELS / 2;
            pose.pushPose();
            // seen from the front (looking south) left is east: column 0 is one block east of the kern, row 0 two blocks south
            pose.translate(TEGELS / 2 - kolom, 0, TEGELS - 1 - rij);
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(kern ? LAMPEN_KERN : LAMPEN)),
                    state.tinten[tegel], LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (!state.plaat.isEmpty()) {
            pose.pushPose();
            pose.translate(PLAAT_X, PLAAT_Y, PLAAT_Z);
            pose.mulPose(Axis.YP.rotationDegrees(-state.draai));
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.scale(PLAAT_SCHAAL, PLAAT_SCHAAL, PLAAT_SCHAAL);
            state.plaat.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;   // the floor is three blocks wide and deep
    }

    @Override
    public AABB getRenderBoundingBox(DiscoDynamoBlockEntity disco) {
        return new AABB(disco.getBlockPos()).inflate(3, 1, 3);
    }
}
