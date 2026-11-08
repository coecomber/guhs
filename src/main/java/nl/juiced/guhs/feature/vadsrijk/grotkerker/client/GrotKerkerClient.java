package nl.juiced.guhs.feature.vadsrijk.grotkerker.client;

import javax.annotation.Nullable;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBaas;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBazen;

/**
 * U-GROTKERKER, client: the cave bosses are the cave creature models they are based on, scaled up ({@link GrotBazen.Def#schaal}),
 * softly tinted in the zone's tone, fading after the plof.
 */
public final class GrotKerkerClient {
    private GrotKerkerClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (var en : GrotBazen.TYPES.entrySet()) {
                GrotBazen.Def d = GrotBazen.def(en.getKey());
                if (d != null) {
                    e.registerEntityRenderer(en.getValue().get(), c -> new BaasRenderer(c, d));
                }
            }
        });
    }

    public static class BaasRenderer extends GeoEntityRenderer<GrotBaas, LivingEntityRenderState> {
        private final GrotBazen.Def def;

        public BaasRenderer(EntityRendererProvider.Context context, GrotBazen.Def d) {
            super(context, new DefaultedEntityGeoModel<GrotBaas>(Guhs.id(d.model())) {
                private final Identifier tex = Guhs.id("textures/" + d.tex() + ".png");

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex;
                }
            });
            this.def = d;
            this.shadowRadius = 1.2f;
        }

        @Override
        public int getRenderColor(GrotBaas b, @Nullable Void related, float partialTick) {
            int k = def.tint();
            return b.geplofd() ? (k & 0x00FFFFFF) | (nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob.PLOF_ALPHA << 24) : k;
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * def.schaal(), heightScale * def.schaal());
        }
    }
}
