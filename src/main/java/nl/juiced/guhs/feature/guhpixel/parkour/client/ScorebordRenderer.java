package nl.juiced.guhs.feature.guhpixel.parkour.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourStats;
import nl.juiced.guhs.feature.guhpixel.parkour.ScorebordBlock;
import nl.juiced.guhs.feature.guhpixel.parkour.ScorebordBlockEntity;
import nl.juiced.guhs.taal.Tekst;

/**
 * The text on a Scorebord: "GUH-PARKOUR" and, per guh (best first, at most five), its place, name, best lap time and
 * laps. The board itself is the block model (26 x 17 pixels of dark board, its front at z 7 when it faces north); the text
 * lies just in front of it, full bright so it reads at night.
 */
public class ScorebordRenderer implements BlockEntityRenderer<ScorebordBlockEntity, ScorebordRenderer.State> {
    /** One font pixel in blocks, the half width of the text area in font pixels, the line height, the lines. */
    private static final float SCHAAL = 1f / 80f;
    private static final int HALF = 62, REGEL = 11, REGELS = 7, RIJEN = 5;
    /** The middle of the board (blocks, y) and how far its front lies from the block's middle. */
    private static final float MIDDEN_Y = 14.5f / 16f, VOOR = 1f / 16f + 0.006f;
    private static final int GOUD = 0xFFFFD27A, TEKST = 0xFFFFE6EE, DOF = 0xFFD8A8C0;

    /** One line: a left part and a right part (either may be null: then the other is centred). */
    record Regel(@Nullable FormattedCharSequence links, @Nullable FormattedCharSequence rechts, int kleur) {
    }

    public static class State extends BlockEntityRenderState {
        float yaw;
        final List<Regel> regels = new ArrayList<>();
    }

    private final Font font;

    public ScorebordRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ScorebordBlockEntity bord, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bord, state, partialTick, camera, breakProgress);
        Direction facing = bord.getBlockState().hasProperty(ScorebordBlock.FACING) ? bord.getBlockState().getValue(ScorebordBlock.FACING) : Direction.NORTH;
        state.yaw = -facing.toYRot();
        state.regels.clear();
        state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.titel").getVisualOrderText(), null, GOUD));
        ListTag rijen = bord.rijen();
        if (bord.paal() == null) {
            state.regels.add(new Regel(null, null, 0));
            state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.los_1").getVisualOrderText(), null, DOF));
            state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.los_2").getVisualOrderText(), null, DOF));
        } else if (rijen.isEmpty()) {
            state.regels.add(new Regel(null, null, 0));
            state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.leeg_1").getVisualOrderText(), null, DOF));
            state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.leeg_2").getVisualOrderText(), null, DOF));
        } else {
            for (int i = 0; i < Math.min(RIJEN, rijen.size()); i++) {
                CompoundTag r = rijen.getCompoundOrEmpty(i);
                FormattedCharSequence rechts = Component.translatable("gui.guhs.guhparkour.bord.score", ParkourStats.tijd(r.getIntOr("Beste", 0)),
                        r.getIntOr("Rondjes", 0)).getVisualOrderText();
                int ruimte = 2 * HALF - font.width(rechts) - 4;
                String naam = (i + 1) + ". " + Tekst.get(r, "Naam").getString();
                if (font.width(naam) > ruimte) {
                    naam = font.plainSubstrByWidth(naam, Math.max(8, ruimte - font.width(".."))) + "..";
                }
                state.regels.add(new Regel(Component.literal(naam).getVisualOrderText(), rechts, i == 0 ? GOUD : TEKST));
            }
            if (rijen.size() > RIJEN) {
                state.regels.add(new Regel(Component.translatable("gui.guhs.guhparkour.bord.meer", rijen.size() - RIJEN).getVisualOrderText(), null, DOF));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, MIDDEN_Y, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(0f, 0f, VOOR);
        pose.scale(SCHAAL, -SCHAAL, SCHAAL);
        int y = -(REGELS * REGEL) / 2 + 1;
        for (Regel r : state.regels) {
            if (r.links() != null && r.rechts() != null) {
                tekst(pose, collector, r.links(), -HALF, y, r.kleur());
                tekst(pose, collector, r.rechts(), HALF - font.width(r.rechts()), y, r.kleur());
            } else if (r.links() != null) {
                tekst(pose, collector, r.links(), -font.width(r.links()) / 2f, y, r.kleur());
            }
            y += REGEL;
        }
        pose.popPose();
    }

    private static void tekst(PoseStack pose, SubmitNodeCollector collector, FormattedCharSequence tekst, float x, float y, int kleur) {
        collector.submitText(pose, x, y, tekst, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, kleur, 0, 0);
    }

    @Override
    public AABB getRenderBoundingBox(ScorebordBlockEntity bord) {
        return new AABB(bord.getBlockPos()).inflate(1.0, 0.6, 1.0);
    }
}
