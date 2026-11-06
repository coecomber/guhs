package nl.juiced.guhs.feature.vadsrijk.zone4.bazen.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Vliegervanger;
import nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Z4BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Zeeslurf;

/**
 * Zone-4 bazen, the client: the Zeeslurf (grey and sulking, pink and happy once cheered up: texture {@code zeeslurf_blij}; its
 * lantern-frills glow), Kootje (his war kite is only drawn while he hangs under it), the grey polyps and the deck winches
 * (the wound rope shows per click: bones {@code touw_1..3}).
 */
public final class Z4BazenClient {
    private static final DataTicket<Boolean> VLAG = DataTicket.create("guhs_vr_z4_vlag", Boolean.class);
    private static final DataTicket<Integer> GETAL = DataTicket.create("guhs_vr_z4_getal", Integer.class);

    private Z4BazenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Z4BazenClient::renderers);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/z4bazen/" + naam + ".png");
    }

    /** A plain renderer: model + one texture + scale (+ the glowmask layer, texture_glowmask). */
    static class Gewoon<T extends LivingEntity & com.geckolib.animatable.GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        Gewoon(EntityRendererProvider.Context ctx, com.geckolib.model.GeoModel<T> m, float schaal, boolean gloed) {
            super(ctx, m);
            withScale(schaal);
            if (gloed) {
                withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(this));
            }
        }
    }

    private static <T extends LivingEntity & com.geckolib.animatable.GeoEntity> DefaultedEntityGeoModel<T> model(String id, String textuur) {
        return new DefaultedEntityGeoModel<T>(Guhs.id(id)) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return tex(textuur);
            }
        };
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Z4BazenRegistry.ZEESLURF.get(), ctx -> new Gewoon<Zeeslurf>(ctx, new DefaultedEntityGeoModel<Zeeslurf>(Guhs.id("vr_z4_baas_zeeslurf")) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return Boolean.TRUE.equals(state.getGeckolibData(VLAG)) ? tex("zeeslurf_blij") : tex("zeeslurf");
            }
        }, 1.0f, true) {
            @Override
            public void addRenderData(Zeeslurf b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(VLAG, b.blij());
            }
        });
        e.registerEntityRenderer(Z4BazenRegistry.VLIEGERVANGER.get(), ctx -> new Gewoon<Vliegervanger>(ctx, model("vr_z4_baas_vliegervanger", "vliegervanger"), 1.15f,
                false) {
            @Override
            public void addRenderData(Vliegervanger b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(VLAG, b.vliegt());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                if (!Boolean.TRUE.equals(info.getOrDefaultGeckolibData(VLAG, false))) {
                    bones.ifPresent("vlieger", s -> s.skipRender(true).skipChildrenRender(true));
                }
            }
        });
        e.registerEntityRenderer(Z4BazenRegistry.POLIEP.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z4_baas_poliep", "poliep"), 1.0f, false));
        e.registerEntityRenderer(Z4BazenRegistry.LIER.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z4_baas_lier", "lier"), 1.0f, false) {
            @Override
            public void addRenderData(Z3Doel d, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, d.stand());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                int n = info.getOrDefaultGeckolibData(GETAL, 0);
                for (int i = n; i < 3; i++) {
                    bones.ifPresent("touw_" + (i + 1), s -> s.skipRender(true));
                }
            }
        });
    }
}
