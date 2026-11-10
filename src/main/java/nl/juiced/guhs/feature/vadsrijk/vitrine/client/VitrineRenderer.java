package nl.juiced.guhs.feature.vadsrijk.vitrine.client;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;
import nl.juiced.guhs.feature.vadsrijk.vitrine.Vitrine;
import nl.juiced.guhs.feature.vadsrijk.vitrine.VitrineBlock;
import nl.juiced.guhs.feature.vadsrijk.vitrine.VitrineBlockEntity;

/**
 * Y-VITRINE: draws what is inside and above a vitrine (the case itself is the block model).
 * <ul>
 *   <li>the statue: a client-only copy of the owner's mount (or pet) in the snapshot, small, slowly turning on its pedestal; made
 *       once per vitrine and rebuilt only when the snapshot's statue changes (as BlubKnuffelRenderer does); no mount and no pet =
 *       the Knuffelorde emblem (an item model, {@code guhs:vr_vitrine_embleem});</li>
 *   <li>the floating panel: the owner's name, class + spec + level, the six zone badges, raid clears, the date of the first
 *       completed zone. Turned to the camera, only drawn within {@link #TEKST_AFSTAND} blocks. Lines are rebuilt only when the
 *       snapshot changes.</li>
 * </ul>
 */
public class VitrineRenderer implements BlockEntityRenderer<VitrineBlockEntity, VitrineRenderer.State> {
    private static final double TEKST_AFSTAND = 14;
    /** The room inside the glass (blocks). */
    private static final float BINNEN_HOOG = 0.56f, BINNEN_BREED = 0.62f, VOET = 4.5f / 16f;
    private static final float TEKST_SCHAAL = 0.018f, TEKST_Y = 1.32f;
    private static final int REGEL = 10;

    public static class State extends BlockEntityRenderState {
        float yaw, draai;
        @Nullable
        EntityRenderState beeld;
        float schaal = 1f;
        final ItemStackRenderState embleem = new ItemStackRenderState();
        boolean metEmbleem, metTekst;
        List<FormattedCharSequence> regels = List.of();
    }

    /** One client copy + the text lines per vitrine (rebuilt when its snapshot or gate changes). */
    private static final Map<VitrineBlockEntity, Kopie> KOPIEEN = new WeakHashMap<>();

    private static final class Kopie {
        String sleutel = "";
        @Nullable
        LivingEntity beeld;
        @Nullable
        Level level;
        @Nullable
        CompoundTag foto;
        int open = -1;
        List<FormattedCharSequence> regels = List.of();
    }

    @Nullable
    private static ItemStack embleemStack;

    private final EntityRenderDispatcher entities;
    private final Font font;

    public VitrineRenderer(BlockEntityRendererProvider.Context context) {
        this.entities = context.entityRenderer();
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VitrineBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.yaw = be.getBlockState().hasProperty(VitrineBlock.FACING) ? be.getBlockState().getValue(VitrineBlock.FACING).toYRot() : 0f;
        state.beeld = null;
        state.metEmbleem = false;
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        state.draai = ((level.getGameTime() % 7200) + partialTick) * 0.75f;
        Kopie k = KOPIEEN.computeIfAbsent(be, x -> new Kopie());
        CompoundTag f = be.foto();
        if (k.foto != f || k.open != be.open()) {   // (the block entity gets a NEW tag on every sync)
            k.foto = f;
            k.open = be.open();
            k.regels = Vitrine.bordRegels(f, be.open()).stream().map(Component::getVisualOrderText).toList();
        }
        // the statue
        String sleutel = f.getStringOr("beeld", "") + "|" + f.getStringOr("soort", "") + "|" + f.getStringOr("kleur", "") + "|" + f.getStringOr("uiterlijk", "");
        if (!sleutel.equals(k.sleutel) || k.level != level) {
            k.sleutel = sleutel;
            k.level = level;
            k.beeld = maakBeeld(level, f);
        }
        if (k.beeld != null) {
            LivingEntity e = k.beeld;
            e.setYRot(0);
            e.yRotO = 0;
            e.yBodyRot = e.yBodyRotO = e.yHeadRot = e.yHeadRotO = 0;
            e.setXRot(0);
            state.beeld = entities.extractEntity(e, 0f);
            state.beeld.lightCoords = state.lightCoords;
            state.beeld.shadowPieces.clear();
            state.beeld.nameTag = null;
            float h = Math.max(0.2f, e.getBbHeight()), w = Math.max(0.2f, e.getBbWidth());
            state.schaal = Math.min(BINNEN_HOOG / h, BINNEN_BREED / w);
        } else if (!f.isEmpty()) {
            if (embleemStack == null) {
                embleemStack = new ItemStack(Items.PAPER);
                embleemStack.set(DataComponents.ITEM_MODEL, Guhs.id("vr_vitrine_embleem"));
            }
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.embleem, embleemStack, ItemDisplayContext.FIXED, level, null, 0);
            state.metEmbleem = true;
        }
        Vec3 midden = Vec3.atCenterOf(be.getBlockPos());
        state.metTekst = camera.distanceToSqr(midden) < TEKST_AFSTAND * TEKST_AFSTAND;
        state.regels = k.regels;
    }

    @Nullable
    private static LivingEntity maakBeeld(Level level, CompoundTag f) {
        String beeld = f.getStringOr("beeld", "");
        String soort = f.getStringOr("soort", "");
        try {
            if (beeld.equals("r")) {
                RijdierSoort s = RijdierSoort.van(soort);
                if (s == null || s.water()) {
                    return null;
                }
                RijdierEntity r = EcoSpul.type(s).create(level, EntitySpawnReason.LOAD);
                if (r != null) {
                    r.voorbeeld = true;
                    if (s.kleuren()) {
                        r.zetKleur(RijdierSoort.PluimKleur.van(f.getStringOr("kleur", "roze")));
                    }
                    r.zetUiterlijk(f.getStringOr("uiterlijk", ""));
                }
                return r;
            }
            if (beeld.equals("h") && !soort.isEmpty()) {
                HuisdierEntity h = HuisdierenRegistry.HUISDIER.get().create(level, EntitySpawnReason.LOAD);
                if (h != null) {
                    h.voorbeeld = true;
                    h.zetSoort(soort);
                }
                return h;
            }
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("Vadsrijk-vitrine: no statue for {} {}: {}", beeld, soort, e.toString());
        }
        return null;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        // the statue (or the emblem), slowly turning on the little pedestal inside the glass
        if (state.beeld != null) {
            pose.pushPose();
            pose.translate(0.5, VOET, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw + state.draai));
            pose.scale(state.schaal, state.schaal, state.schaal);
            entities.submit(state.beeld, camera, 0, 0, 0, pose, collector);
            pose.popPose();
        } else if (state.metEmbleem) {
            pose.pushPose();
            pose.translate(0.5, VOET + 0.3, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(state.draai));
            pose.scale(0.55f, 0.55f, 0.55f);
            state.embleem.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        // the floating panel
        if (!state.metTekst || state.regels.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, TEKST_Y + (state.regels.size() - 1) * REGEL * TEKST_SCHAAL, 0.5);
        pose.mulPose(camera.orientation);
        pose.scale(TEKST_SCHAAL, -TEKST_SCHAAL, TEKST_SCHAAL);
        int y = 0;
        for (FormattedCharSequence r : state.regels) {
            float w = font.width(r);
            collector.submitText(pose, -w / 2f, y, r, false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xFFFFFFFF, 0x55140A1E, 0);
            y += REGEL;
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(VitrineBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.6, 0, 0.6).expandTowards(0, 1.6, 0);
    }
}
