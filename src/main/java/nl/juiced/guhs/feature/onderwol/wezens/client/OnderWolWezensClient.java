package nl.juiced.guhs.feature.onderwol.wezens.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.onderwol.wezens.GrotGlimwormEntity;
import nl.juiced.guhs.feature.onderwol.wezens.OnderWolWezens;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * 1.7.0 "Onder de Wol": the cave glimworm drawn with the safari glimworm's baby model, texture and glowing tail
 * (safari/glimworm_baby; its animations are the species', safari/glimworm), and the Grotguh's glowing crystal specks.
 */
public final class OnderWolWezensClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(OnderWolWezens.GROTGLIMWORM.get(), context -> {
            GeoEntityRenderer<GrotGlimwormEntity, LivingEntityRenderState> r = new GeoEntityRenderer<GrotGlimwormEntity, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<GrotGlimwormEntity>(Guhs.id("safari/glimworm_baby")) {
                        @Override
                        public Identifier getAnimationResource(GrotGlimwormEntity worm) {
                            return buildFormattedAnimationPath(Guhs.id("safari/glimworm"));
                        }
                    }) {
                {
                    this.shadowRadius = 0.0f;
                }
            };
            r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        }));
        // the Grotguh: its crystal specks glow in the dark (a full-bright pass, like the Asguh's cheeks)
        Identifier specks = Guhs.id("textures/entity/guh_grotguh_glowmask.png");
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (frame.variant == GuhVariant.GROTGUH) {
                frame.glow(specks);
            }
        });
    }

    private OnderWolWezensClient() {
    }
}
