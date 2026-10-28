package nl.juiced.guhs.feature.huisje.client;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeBlockEntity;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;

/**
 * Draws a Guhhuisje: the guh-head model of its size (models/block/guhhuisje_&lt;maat&gt;_model.json, 32 model pixels
 * wide, made by tools/features/huisje.py) scaled up to the whole footprint (2, 3 or 4 blocks), turned so the snoet (the
 * door) faces the way the huisje was placed.
 */
public class HuisjeRenderer implements BlockEntityRenderer<HuisjeBlockEntity, HuisjeRenderer.State> {
    private static final Map<HuisjeMaat, StandaloneModelKey<BlockStateModelPart>> MODELLEN = new EnumMap<>(HuisjeMaat.class);

    static {
        for (HuisjeMaat maat : HuisjeMaat.values()) {
            MODELLEN.put(maat, new StandaloneModelKey<>(() -> "guhs:block/guhhuisje_" + maat.id() + "_model"));
        }
    }

    /** 1.8.0: the Guhvilla is five blocks tall: its roof (the big ears, the crown, the chimney tops) is a second model,
     *  drawn 32 model pixels higher (a block model can't reach past 32). */
    private static final StandaloneModelKey<BlockStateModelPart> VILLA_DAK = new StandaloneModelKey<>(() -> "guhs:block/guhhuisje_villa_dak_model");

    public static StandaloneModelKey<BlockStateModelPart> model(HuisjeMaat maat) {
        return MODELLEN.get(maat);
    }

    /** Mod bus (client): the three guh-head models (1.1.0: NeoForge standalone models). */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        for (HuisjeMaat maat : HuisjeMaat.values()) {
            event.register(model(maat), SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guhhuisje_" + maat.id() + "_model")));
        }
        event.register(VILLA_DAK, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guhhuisje_villa_dak_model")));
    }

    public static class State extends BlockEntityRenderState {
        @Nullable
        HuisjeMaat maat;
        Direction facing = Direction.NORTH;
        Vec3 offset = Vec3.ZERO;
    }

    public HuisjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HuisjeBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.maat = null;
        if (!(be.getBlockState().getBlock() instanceof HuisjeBlock blok)) {
            return;
        }
        state.maat = blok.maat();
        state.facing = be.getBlockState().getValue(HuisjeBlock.FACING);
        Vec3 m = Huisje.midden(be.getBlockPos(), state.facing, state.maat);
        state.offset = new Vec3(m.x - be.getBlockPos().getX(), 0, m.z - be.getBlockPos().getZ());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        HuisjeMaat maat = state.maat;
        if (maat == null) {
            return;
        }
        pose.pushPose();
        pose.translate(state.offset.x, 0, state.offset.z);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.scale(maat.schaal(), maat.schaal(), maat.schaal());
        pose.translate(-0.5, 0, -0.5);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(model(maat))),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (maat == HuisjeMaat.VILLA) {
            pose.translate(0, 2, 0);   // (32 model pixels up: the roof)
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(VILLA_DAK)),
                    BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HuisjeBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(4, 0, 4).expandTowards(0, 7, 0);
    }
}
