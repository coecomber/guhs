package nl.juiced.guhs.feature.wereldleven.client;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.feature.wereldleven.Dagritme;
import nl.juiced.guhs.feature.wereldleven.Kaasijsjes;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;

/**
 * The wereldleven layers on every guh (via GuhRenderHooks): the ice-cream hat (a waffle cone upside down on its head, the
 * scoop in the flavour's colour, a cherry), blushing cheeks, and the little straw nestje under a guh napping on the spot.
 * The hat and the cheeks are extra bones in the guh model (outfit_wl_*, made by tools/features/wereldleven.py; hidden
 * unless drawn here), each drawn with its own texture like the clothes.
 */
public final class WereldlevenLagen {
    public static final String HOORNTJE = "outfit_wl_hoorntje", BOLLETJE = "outfit_wl_bolletje", KERSJE = "outfit_wl_kersje",
            BLOSJES = "outfit_wl_blosjes";
    private static final Identifier HOED_TEX = Guhs.id("textures/entity/guh_clothes/wereldleven_ijshoedje.png");
    private static final Identifier BLOS_TEX = Guhs.id("textures/entity/guh_clothes/wereldleven_blosjes.png");

    static void render(GeoRenderer<GuhEntity> renderer, PoseStack pose, GuhEntity guh, BakedGeoModel model, MultiBufferSource buffers,
                       float partialTick, int light, int overlay) {
        int flags = guh.getKnusVlaggen();
        if ((flags & Dagritme.DUTJE) != 0) {
            nestje(pose, guh, buffers, light);
        }
        boolean hoed = (flags & GuhHooks.IJSHOEDJE) != 0, blos = (flags & GuhHooks.BLOSJES) != 0;
        if (!hoed && !blos) {
            return;
        }
        Map<GeoBone, boolean[]> was = new IdentityHashMap<>();
        for (GeoBone bone : model.topLevelBones()) {
            onthoud(bone, was);
        }
        try {
            if (hoed) {
                pass(renderer, pose, guh, model, buffers, partialTick, light, HOED_TEX, List.of(HOORNTJE, KERSJE), 0xFFFFFFFF);
                Kaasijsjes.Smaak smaak = Kaasijsjes.hoedjeSmaak(guh);
                int kleur = smaak == null ? 0xFFF4F8 : smaak.kleur;
                pass(renderer, pose, guh, model, buffers, partialTick, light, HOED_TEX, List.of(BOLLETJE), 0xFF000000 | kleur);
            }
            if (blos) {
                pass(renderer, pose, guh, model, buffers, partialTick, light, BLOS_TEX, List.of(BLOSJES), 0xFFFFFFFF);
            }
        } finally {
            for (var e : was.entrySet()) {
                e.getKey().setHidden(e.getValue()[0]);
                e.getKey().setChildrenHidden(e.getValue()[1]);
            }
        }
    }

    private static void pass(GeoRenderer<GuhEntity> renderer, PoseStack pose, GuhEntity guh, BakedGeoModel model, MultiBufferSource buffers,
                             float partialTick, int light, Identifier texture, List<String> bones, int colour) {
        for (GeoBone bone : model.topLevelBones()) {
            alleen(bone, bones);
        }
        RenderType type = RenderType.entityCutoutNoCull(texture);
        renderer.reRender(model, pose, buffers, guh, type, buffers.getBuffer(type), partialTick, light, OverlayTexture.NO_OVERLAY, colour);
    }

    private static void alleen(GeoBone bone, List<String> bones) {
        String name = bone.getName();
        bone.setHidden(bones.stream().noneMatch(name::startsWith));
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            alleen(child, bones);
        }
    }

    private static void onthoud(GeoBone bone, Map<GeoBone, boolean[]> was) {
        was.put(bone, new boolean[] {bone.isHidden(), bone.isHidingChildren()});
        for (GeoBone child : bone.getChildBones()) {
            onthoud(child, was);
        }
    }

    /** A guh nest under a guh that naps on the spot (the pose here is at its feet, scaled for its age but not its size). */
    private static void nestje(PoseStack pose, GuhEntity guh, MultiBufferSource buffers, int light) {
        BlockState nest = VadswoudFeature.GUHNESTJE.get().defaultBlockState();
        float s = guh.getBbWidth() * 1.45f / Math.max(0.2f, guh.getAgeScale());
        pose.pushPose();
        pose.scale(s, s * 0.8f, s);
        pose.translate(-0.5, -0.02, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(nest, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private WereldlevenLagen() {
    }
}
