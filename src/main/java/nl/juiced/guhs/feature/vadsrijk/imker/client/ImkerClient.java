package nl.juiced.guhs.feature.vadsrijk.imker.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.imker.ImkerRegistry;
import nl.juiced.guhs.feature.vadsrijk.imker.MikaImker;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * T-IMKER, the client: GeckoLib renderers for De Mika-imker (the mika model in his beekeeper's suit; his smoker hides while he
 * pours, the honey can shows), the Koninginnekorf (gold -> grey with the meter: stand 0..2) and the Rookpotjes.
 */
public final class ImkerClient {
    private static final DataTicket<Integer> STAND = DataTicket.create("guhs_vr_z3imker_stand", Integer.class);
    private static final DataTicket<Boolean> GIET = DataTicket.create("guhs_vr_z3imker_giet", Boolean.class);

    private ImkerClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ImkerClient::renderers);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/z3imker/" + naam + ".png");
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ImkerRegistry.MIKA.get(), ctx -> new MikaRenderer(ctx));
        e.registerEntityRenderer(ImkerRegistry.KORF.get(), ctx -> new DoelRenderer(ctx, "vr_z3_imker_korf", "korf", 1.4f, true));
        e.registerEntityRenderer(ImkerRegistry.ROOKPOT.get(), ctx -> new DoelRenderer(ctx, "vr_z3_imker_rookpot", "rookpot", 1.2f, false));
    }

    static final class MikaRenderer extends GeoEntityRenderer<MikaImker, LivingEntityRenderState> {
        MikaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<MikaImker>(Guhs.id("vr_z3_imker_mika")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("mika_imker");
                }
            });
            withScale(1.9f);
        }

        @Override
        public void addRenderData(MikaImker m, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(GIET, m.giet());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            boolean giet = Boolean.TRUE.equals(info.getOrDefaultGeckolibData(GIET, false));
            bones.ifPresent(giet ? "roker" : "honingblik", s -> s.skipRender(true).skipChildrenRender(true));
        }
    }

    static final class DoelRenderer extends GeoEntityRenderer<Z3Doel, LivingEntityRenderState> {
        DoelRenderer(EntityRendererProvider.Context ctx, String model, String textuur, float schaal, boolean grauwt) {
            super(ctx, new DefaultedEntityGeoModel<Z3Doel>(Guhs.id(model)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    if (!grauwt) {
                        return tex(textuur);
                    }
                    int s = state.getOrDefaultGeckolibData(STAND, 0);
                    return tex(s <= 0 ? textuur : textuur + "_grauw" + Math.min(2, s));
                }
            });
            withScale(schaal);
            this.shadowRadius = 0.6f;
        }

        @Override
        public void addRenderData(Z3Doel d, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(STAND, d.stand());
        }
    }
}
