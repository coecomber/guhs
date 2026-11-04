package nl.juiced.guhs.feature.vadsrijk.momenten.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.vadsrijk.momenten.KaravaanSlak;
import nl.juiced.guhs.feature.vadsrijk.momenten.MomentenPayloads;
import nl.juiced.guhs.feature.vadsrijk.momenten.MomentenRegistry;

/** Het Vadsrijk "momenten", client: the recolour wave ({@link GolfClient}) and the caravan's renderers. */
public final class MomentenClient {
    private MomentenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(MomentenRegistry.KARAVAAN.get(), SlakRenderer::new);
            e.registerEntityRenderer(MomentenRegistry.HANDELAAR.get(), GuhRenderer::new);
        });
        MomentenPayloads.komtOntvanger = g -> net.minecraft.client.Minecraft.getInstance().execute(() -> GolfClient.wacht(g.ticks()));
        MomentenPayloads.startOntvanger = g -> net.minecraft.client.Minecraft.getInstance().execute(() -> GolfClient.start(g.baan()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> GolfClient.tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> GolfClient.stop(false));
    }

    /** The snail cart: GeckoLib model + texture from tools/features/vadsrijk_momenten.py. */
    static class SlakRenderer extends GeoEntityRenderer<KaravaanSlak, EntityRenderState> {
        private static final Identifier TEX = Guhs.id("textures/vadsrijk/momenten/karavaan.png");

        SlakRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KaravaanSlak>(Guhs.id("vadsrijk/momenten/karavaan")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return TEX;
                }
            });
            this.shadowRadius = 1.1f;
        }

        @Override
        protected AABB getBoundingBoxForCulling(KaravaanSlak s) {
            return s.getBoundingBoxForCulling();
        }

        @Override
        public void addRenderData(KaravaanSlak s, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, s.yRotO, s.getYRot()));
        }
    }
}
