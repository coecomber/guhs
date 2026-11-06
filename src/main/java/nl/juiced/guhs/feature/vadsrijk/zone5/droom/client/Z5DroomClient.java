package nl.juiced.guhs.feature.vadsrijk.zone5.droom.client;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.droom.GrauweDroom;
import nl.juiced.guhs.feature.vadsrijk.zone5.droom.Z5DroomRegistry;

/**
 * T-DROOM, the client: GeckoLib renderers for the Grauwe Droom (grey / lilac / pink texture by its stage), the four lullaby
 * notes (one colour each, by the Z3Doel stand) and the grey tendrils; each with its always-on glowmask.
 */
public final class Z5DroomClient {
    static final DataTicket<Integer> STAND = DataTicket.create("guhs_vr_z5droom_stand", Integer.class);

    private Z5DroomClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            reg(e, Z5DroomRegistry.GRAUWE_DROOM.get(), "grauwe_droom", 1.0f);
            reg(e, Z5DroomRegistry.WIEGNOOT.get(), "wiegnoot", 1.0f);
            reg(e, Z5DroomRegistry.GRAUWE_RANK.get(), "grauwe_rank", 1.0f);
        });
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone5/droom/" + naam + ".png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void reg(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, String naam, float schaal) {
        e.registerEntityRenderer((EntityType) type, c -> new Renderer(c, naam, schaal));
    }

    static String textuur(String naam, int stand) {
        return switch (naam) {
            case "grauwe_droom" -> stand == GrauweDroom.LILA ? "grauwe_droom_lila" : stand == GrauweDroom.ROZE_DROOM ? "grauwe_droom_roze" : "grauwe_droom";
            case "wiegnoot" -> "wiegnoot_" + Math.max(0, Math.min(3, stand));
            default -> naam;
        };
    }

    public static class Renderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final float schaal;

        public Renderer(EntityRendererProvider.Context context, String naam, float schaal) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone5/droom/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(textuur(naam, state.getOrDefaultGeckolibData(STAND, 0)));
                }
            });
            this.schaal = schaal;
            this.shadowRadius = "grauwe_droom".equals(naam) ? 0 : 0.4f;
            withRenderLayer(new Gloed<>(this, tex(naam + "_gloed")));
        }

        @Override
        public void addRenderData(T w, @javax.annotation.Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(STAND, w instanceof GrauweDroom d ? d.kleur() : w instanceof Z3Doel d ? d.stand() : 0);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * schaal, heightScale * schaal);
        }
    }

    /** The always-on glowmask, additive on top. */
    static class Gloed<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
        private final Identifier tex;

        Gloed(GeoRenderer<T, Void, LivingEntityRenderState> r, Identifier tex) {
            super(r);
            this.tex = tex;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.willRender()) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(tex));
            }
        }
    }
}
