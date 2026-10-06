package nl.juiced.guhs.feature.techmachine.client;

import java.util.EnumMap;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;

/**
 * The little animation of every machine of this slice: the part that moves is a block model of its own
 * (models/block/&lt;machine&gt;_&lt;part&gt;.json, drawn facing north like the machine's model itself; tools/features/
 * tech_machines_modellen.py writes them and uses the same numbers as the table below), turned or shifted by how long the
 * machine has been working ({@link TechBlockEntity#beweging}: it stands still when the machine does).
 * <ul>
 *   <li>Oogster: the reel of blades turns in front of its snoet;</li>
 *   <li>Knabbelaar: the two teeth bite down and the jaw comes up, five ticks a bite (it really gnaws);</li>
 *   <li>Neerzetter: the arm pushes out of the hatch and back;</li>
 *   <li>Vadsmolen: the sails go round;</li>
 *   <li>Knutselmachine: the two hammers tap in turn;</li>
 *   <li>Plantagebak: the watering can tips over the bed.</li>
 * </ul>
 * The machine itself (with its face) is the normal block model.
 */
public class MachineRenderer implements BlockEntityRenderer<TechBlockEntity, MachineRenderer.State> {
    private static final float PX = 1f / 16f;

    /** How a part moves: t = ticks of movement. */
    private interface Beweging {
        void pas(PoseStack pose, float t);
    }

    private record Deel(String naam, StandaloneModelKey<BlockStateModelPart> model, Beweging beweging) {
    }

    private static Deel deel(String naam, Beweging beweging) {
        return new Deel(naam, new StandaloneModelKey<>(() -> "guhs:block/" + naam), beweging);
    }

    /** Turn around a line along the x axis through (y, z) in pixels. */
    private static void omX(PoseStack pose, float y, float z, float graden) {
        pose.translate(0, y * PX, z * PX);
        pose.mulPose(Axis.XP.rotationDegrees(graden));
        pose.translate(0, -y * PX, -z * PX);
    }

    /** Turn around a line along the z axis through (x, y) in pixels. */
    private static void omZ(PoseStack pose, float x, float y, float graden) {
        pose.translate(x * PX, y * PX, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(graden));
        pose.translate(-x * PX, -y * PX, 0);
    }

    private static float golf(float t, float periode) {
        return (float) Math.sin(t * Math.PI * 2 / periode);
    }

    // (the same numbers as tools/features/tech_machines_modellen.py)
    private static final Map<MachineSoort, List<Deel>> DELEN = new EnumMap<>(MachineSoort.class);

    static {
        DELEN.put(MachineSoort.OOGSTER, List.of(
                deel("oogster_haspel", (pose, t) -> omX(pose, 5f, -4.5f, -t * 18f))));
        DELEN.put(MachineSoort.KNABBELAAR, List.of(
                deel("knabbelaar_tanden", (pose, t) -> pose.translate(0, -Math.abs(golf(t, 10f)) * 1.5f * PX, 0)),
                deel("knabbelaar_kaak", (pose, t) -> pose.translate(0, Math.abs(golf(t, 10f)) * 0.5f * PX, 0))));
        DELEN.put(MachineSoort.NEERZETTER, List.of(
                deel("neerzetter_arm", (pose, t) -> {
                    float uit = golf(t, 40f);
                    pose.translate(0, 0, -uit * uit * 7f * PX);
                })));
        DELEN.put(MachineSoort.VADSMOLEN, List.of(
                deel("vadsmolen_wieken", (pose, t) -> omZ(pose, 8f, 22f, t * 9f))));
        DELEN.put(MachineSoort.KNUTSELMACHINE, List.of(
                deel("knutselmachine_hamer_links", (pose, t) -> omX(pose, 17f, 14f, Math.max(0f, golf(t, 10f)) * 50f)),
                deel("knutselmachine_hamer_rechts", (pose, t) -> omX(pose, 17f, 14f, Math.max(0f, -golf(t, 10f)) * 50f))));
        DELEN.put(MachineSoort.PLANTAGEBAK, List.of(
                deel("plantagebak_gieter", (pose, t) -> omX(pose, 17f, 2.7f, (0.5f - 0.5f * (float) Math.cos(t * Math.PI * 2 / 40f)) * 38f))));
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        MachineSoort soort = MachineSoort.OOGSTER;
        float beweging;
    }

    public MachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the moving parts, which are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        for (List<Deel> delen : DELEN.values()) {
            for (Deel deel : delen) {
                event.register(deel.model(), SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/" + deel.naam())));
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TechBlockEntity machine, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTick, camera, breakProgress);
        state.facing = machine.voor();
        state.soort = machine.soort();
        state.beweging = machine.beweging(partialTick);
        if (machine.getLevel() != null) {
            // the part sticks out of the block: it is lit like the air it is in (in front of the snoet, or on top)
            BlockPos pos = machine.getBlockPos();
            BlockPos buiten = switch (state.soort) {
                case KNUTSELMACHINE, PLANTAGEBAK -> pos.above();
                case VADSMOLEN -> pos.above().relative(state.facing);
                default -> pos.relative(state.facing);
            };
            state.lightCoords = Math.max(state.lightCoords, LevelRenderer.getLightCoords(machine.getLevel(), buiten));
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        List<Deel> delen = DELEN.get(state.soort);
        if (delen == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        for (Deel deel : delen) {
            pose.pushPose();
            deel.beweging().pas(pose, state.beweging);
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(deel.model())),
                    BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TechBlockEntity machine) {
        return new AABB(machine.getBlockPos()).inflate(1.0, 1.5, 1.0);
    }
}
