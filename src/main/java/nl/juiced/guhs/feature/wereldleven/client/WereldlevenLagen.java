package nl.juiced.guhs.feature.wereldleven.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.feature.wereldleven.Dagritme;
import nl.juiced.guhs.feature.wereldleven.Kaasijsjes;

/**
 * The wereldleven layers on every guh: the ice-cream hat (a waffle cone upside down on its head, the scoop in the flavour's
 * colour, a cherry), blushing cheeks, and the little straw nestje under a guh napping on the spot.
 * The hat and the cheeks are extra bones in the guh model (outfit_wl_*, made by tools/features/wereldleven.py; hidden
 * unless drawn here), each drawn with its own texture like the clothes.
 * <p>
 * 1.1.0 (GeckoLib 5): one {@link GuhRenderer.Hook} (was a GuhRenderHooks layer): the hat/cheek passes are
 * {@link GuhRenderFrame#pass model passes} with only those bones, the nestje is a block model resolved at extract time and
 * submitted in the old layer pose ({@link GuhRenderFrame#layerExtra}).
 */
public final class WereldlevenLagen {
    public static final String HOORNTJE = "outfit_wl_hoorntje", BOLLETJE = "outfit_wl_bolletje", KERSJE = "outfit_wl_kersje",
            BLOSJES = "outfit_wl_blosjes";
    private static final Identifier HOED_TEX = Guhs.id("textures/entity/guh_clothes/wereldleven_ijshoedje.png");
    private static final Identifier BLOS_TEX = Guhs.id("textures/entity/guh_clothes/wereldleven_blosjes.png");
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    /** Registered once from {@link WereldlevenClient#init}. */
    static void hook(GuhEntity guh, float partialTick, GuhRenderFrame frame) {
        int flags = guh.getKnusVlaggen();
        if ((flags & Dagritme.DUTJE) != 0) {
            nestje(guh, frame);
        }
        if ((flags & GuhHooks.IJSHOEDJE) != 0) {
            frame.pass(HOED_TEX, 0xFFFFFFFF, alleen(List.of(HOORNTJE, KERSJE)));
            Kaasijsjes.Smaak smaak = Kaasijsjes.hoedjeSmaak(guh);
            int kleur = smaak == null ? 0xFFF4F8 : smaak.kleur;
            frame.pass(HOED_TEX, 0xFF000000 | kleur, alleen(List.of(BOLLETJE)));
        }
        if ((flags & GuhHooks.BLOSJES) != 0) {
            frame.pass(BLOS_TEX, 0xFFFFFFFF, alleen(List.of(BLOSJES)));
        }
    }

    /** Only the bones whose name starts with one of these (their own cubes; children decide for themselves). */
    private static java.util.function.Predicate<String> alleen(List<String> bones) {
        return name -> bones.stream().anyMatch(name::startsWith);
    }

    /** A guh nest under a guh that naps on the spot (the pose here is at its feet, scaled for its age but not its size). */
    private static void nestje(GuhEntity guh, GuhRenderFrame frame) {
        BlockModelRenderState nest = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(nest, VadswoudFeature.GUHNESTJE.get().defaultBlockState(), DISPLAY);
        float s = guh.getBbWidth() * 1.45f / Math.max(0.2f, guh.getAgeScale());
        frame.layerExtra((pose, collector, light) -> {
            pose.pushPose();
            pose.scale(s, s * 0.8f, s);
            pose.translate(-0.5, -0.02, -0.5);
            nest.submitMultiLayer(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        });
    }

    private WereldlevenLagen() {
    }
}
