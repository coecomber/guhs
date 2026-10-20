package nl.juiced.guhs.feature.safari.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;

/**
 * Draws a {@link SafariDier} with the files of its species and variant (SAFARI_CONTRACT.md): for variant v of species s it
 * takes {@code safari/s_v} when that exists (geo, texture and animations each on their own), else {@code safari/s}; the
 * female is the base. A baby without its own model is the base at the species' baby scale. A species with a
 * {@code s[_v]_glowmask.png} (GeckoLib's name) gets a glowing layer (the glimworm's tail). Which files exist is looked up once per name (cleared
 * on a resource reload), never per frame. A species without any model shows the pluisegeltje, so a gap is visible.
 */
public final class SafariClient {
    static final DataTicket<String> MODEL = DataTicket.create("guhs_safari_dier_model", String.class);
    static final DataTicket<String> TEXTUUR = DataTicket.create("guhs_safari_dier_textuur", String.class);
    static final DataTicket<Float> SCHAAL = DataTicket.create("guhs_safari_dier_schaal", Float.class);
    static final DataTicket<Boolean> GLOED = DataTicket.create("guhs_safari_dier_gloed", Boolean.class);
    private static final Map<String, Boolean> BESTAAT = new ConcurrentHashMap<>();

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(SafariFeature.DIER.get(), DierRenderer::new));
        modBus.addListener((AddClientReloadListenersEvent event) -> event.addListener(Guhs.id("safari_dieren"),
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> BESTAAT.clear()));
    }

    static boolean bestaat(String pad) {
        return BESTAAT.computeIfAbsent(pad, p -> Minecraft.getInstance().getResourceManager().getResource(Guhs.id(p)).isPresent());
    }

    static boolean geo(String naam) {
        return bestaat("geckolib/models/entity/safari/" + naam + ".geo.json");
    }

    static boolean textuur(String naam) {
        return bestaat("textures/entity/safari/" + naam + ".png");
    }

    static boolean animatie(String naam) {
        return bestaat("geckolib/animations/entity/safari/" + naam + ".animation.json");
    }

    /** s_v when it has its own file of this kind (the female "vrouw" is the base), else s. */
    static String kies(String soort, String variant, java.util.function.Predicate<String> heeft) {
        return heeft.test(soort + "_" + variant) ? soort + "_" + variant : soort;
    }

    public static class DierRenderer extends GeoEntityRenderer<SafariDier, LivingEntityRenderState> {
        public DierRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SafariDier>(Guhs.id("safari/_leeg")) {
                @Override
                public void addAdditionalStateData(SafariDier dier, @Nullable Object related, GeoRenderState state) {
                    String soort = dier.soort(), variant = dier.variant();
                    String g = kies(soort, variant, SafariClient::geo);
                    state.addGeckolibData(MODEL, geo(g) ? g : "");
                    String t = kies(soort, variant, SafariClient::textuur);
                    state.addGeckolibData(TEXTUUR, textuur(t) ? t : "");
                    ParkData.Soort info = ParkData.get().soort(soort);
                    boolean kleineBaby = variant.equals("baby") && !g.endsWith("_baby");
                    state.addGeckolibData(SCHAAL, kleineBaby ? (info == null ? 0.55f : info.babySchaal()) : 1f);
                    state.addGeckolibData(GLOED, textuur(t + "_glowmask"));      // GeckoLib's emissive name (RenderUtil.getEmissiveResource)
                }

                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    String g = state.getOrDefaultGeckolibData(MODEL, "");
                    return g.isEmpty() ? buildFormattedModelPath(Guhs.id("pluisegeltje")) : buildFormattedModelPath(Guhs.id("safari/" + g));
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String t = state.getOrDefaultGeckolibData(TEXTUUR, "");
                    return t.isEmpty() ? buildFormattedTexturePath(Guhs.id("pluisegeltje")) : buildFormattedTexturePath(Guhs.id("safari/" + t));
                }

                @Override
                public Identifier getAnimationResource(SafariDier dier) {
                    String a = kies(dier.soort(), dier.variant(), SafariClient::animatie);
                    return buildFormattedAnimationPath(Guhs.id("safari/" + (animatie(a) ? a : "_leeg")));
                }
            });
            this.shadowRadius = 0.5f;
            withRenderLayer(new AutoGlowingGeoLayer<SafariDier, Void, LivingEntityRenderState>(this) {
                @Nullable
                @Override
                protected RenderType getRenderType(LivingEntityRenderState state) {
                    return Boolean.TRUE.equals(state.getOrDefaultGeckolibData(GLOED, false)) ? super.getRenderType(state) : null;
                }
            });
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = info.getOrDefaultGeckolibData(SCHAAL, 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private SafariClient() {
    }
}
