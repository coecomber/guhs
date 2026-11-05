package nl.juiced.guhs.feature.vadsrijk.vriendjes.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.vriendjes.DraakVriendjes;
import nl.juiced.guhs.feature.vadsrijk.vriendjes.RozeDraak;
import nl.juiced.guhs.feature.vadsrijk.vriendjes.VriendjeEntity;

/**
 * N-EXTRA, the client: the pink resting/visiting Vahoegnir (the o-draak model, the boss's fully pink texture + its glowmask, the
 * boss animation file, x1.6) and the critters (any model/texture/animation, synced).
 */
public final class VriendjesClient {
    private static final DataTicket<String> V_MODEL = DataTicket.create("guhs_vr_vriendje_model", String.class);
    private static final DataTicket<String> V_TEX = DataTicket.create("guhs_vr_vriendje_tex", String.class);
    private static final DataTicket<Float> V_SCHAAL = DataTicket.create("guhs_vr_vriendje_schaal", Float.class);

    private VriendjesClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(DraakVriendjes.ROZE_DRAAK.get(), DraakRenderer::new);
            e.registerEntityRenderer(DraakVriendjes.VRIENDJE.get(), VriendjeRenderer::new);
        });
    }

    static class DraakRenderer extends GeoEntityRenderer<RozeDraak, EntityRenderState> {
        DraakRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<RozeDraak>(Guhs.id("vadsrijk/wezens/draak")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/vadsrijk/z3bazen/vahoegnir_5.png");
                }

                @Override
                public Identifier getAnimationResource(RozeDraak d) {
                    return Guhs.id("entity/vr_z3_baas_vahoegnir");
                }
            });
            withScale(RozeDraak.SCHAAL);
            withRenderLayer(new AutoGlowingGeoLayer<RozeDraak, Void, EntityRenderState>(this));
            this.shadowRadius = 2.5f;
        }

        @Override
        public void addRenderData(RozeDraak d, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, d.yRotO, d.getYRot()));
        }

        @Override
        protected AABB getBoundingBoxForCulling(RozeDraak d) {
            return d.cullDoos();
        }
    }

    static class VriendjeRenderer extends GeoEntityRenderer<VriendjeEntity, EntityRenderState> {
        VriendjeRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<VriendjeEntity>(Guhs.id("pluisvinkje")) {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return Guhs.id("entity/" + state.getOrDefaultGeckolibData(V_MODEL, "pluisvinkje"));
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id(state.getOrDefaultGeckolibData(V_TEX, "textures/entity/pluisvinkje.png"));
                }

                @Override
                public Identifier getAnimationResource(VriendjeEntity f) {
                    return Guhs.id("entity/" + f.animBestand());
                }
            });
            this.shadowRadius = 0.15f;
        }

        @Override
        public void addRenderData(VriendjeEntity f, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(V_MODEL, f.model());
            state.addGeckolibData(V_TEX, f.textuur());
            state.addGeckolibData(V_SCHAAL, f.schaal());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, f.yRotO, f.getYRot()));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<EntityRenderState> info, float w, float h) {
            float k = info.getOrDefaultGeckolibData(V_SCHAAL, 1f);
            super.scaleModelForRender(info, w * k, h * k);
        }

        @Override
        protected AABB getBoundingBoxForCulling(VriendjeEntity f) {
            return f.cullDoos();
        }
    }
}
