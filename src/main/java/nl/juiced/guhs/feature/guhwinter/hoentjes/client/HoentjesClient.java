package nl.juiced.guhs.feature.guhwinter.hoentjes.client;

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
import nl.juiced.guhs.feature.guhwinter.hoentjes.GuhwinterHoentjes;
import nl.juiced.guhs.feature.guhwinter.hoentjes.Hoentje;

/**
 * The hoentjes' renderers (1.10.9 Guhwinter): their own GeckoLib models (tools/features/guhwinter_hoentjes_modellen.py) with
 * a turning head and guh eyes that blink now and then (the &lt;name&gt;_dicht.png texture, like the birds of 3.0).
 */
public final class HoentjesClient {
    static final DataTicket<Boolean> DICHT = DataTicket.create("guhs_hoentje_dicht", Boolean.class);

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(GuhwinterHoentjes.WATERHOEN_TYPE.get(), c -> new HoentjeRenderer<>(c, "guhwinter_waterhoen", 0.25f));
            event.registerEntityRenderer(GuhwinterHoentjes.ALPINEHOEN_TYPE.get(), c -> new HoentjeRenderer<>(c, "guhwinter_alpinehoen", 0.28f));
        });
    }

    /** A blink of 3 ticks every few seconds (each hoentje its own rhythm). */
    public static boolean ogenDicht(Hoentje h) {
        return (h.tickCount + h.getId() * 37) % 97 < 3;
    }

    public static class HoentjeRenderer<T extends Hoentje> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public HoentjeRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam)) {
                private final Identifier open = Guhs.id("textures/entity/" + naam + ".png");
                private final Identifier dicht = Guhs.id("textures/entity/" + naam + "_dicht.png");

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(DICHT)) ? dicht : open;
                }
            });
            this.shadowRadius = schaduw;
        }

        @Override
        public void addRenderData(T hoentje, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(DICHT, ogenDicht(hoentje));
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private HoentjesClient() {
    }
}
