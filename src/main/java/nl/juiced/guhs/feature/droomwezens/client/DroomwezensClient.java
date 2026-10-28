package nl.juiced.guhs.feature.droomwezens.client;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.feature.droomwezens.DroomwezensFeature;
import nl.juiced.guhs.feature.droomwezens.SterrenvisjeEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * The look of the dream creatures (bouwplan C): each dream NPC has its own model (assets/guhs/geckolib/models/entity/
 * guh_npc_&lt;kind&gt;.geo.json), the Wekkerguh and the kussengevecht-guh their own animation file ("rinkel", "gooi"), the
 * Zandguh a glowmask (his star and the glitter in his sack); the Zandguh's sack sways a little. The sterrenvisje: one model,
 * a texture and a glowmask per colour. The thrown pillow: the pink guhkussen item.
 */
public final class DroomwezensClient {
    static final DataTicket<String> VIS_KLEUR = DataTicket.create("guhs_droom_sterrenvisje_kleur", String.class);

    private DroomwezensClient() {
    }

    public static void init(IEventBus modBus) {
        for (Kind k : DroomNpc.KINDS) {
            SittingGuhRenderers.NPC_MODELEN.put(k, Guhs.id("entity/guh_npc_" + k.id()));
        }
        SittingGuhRenderers.NPC_ANIMATIES.put(Kind.WEKKERGUH, Guhs.id("entity/guh_npc_wekkerguh"));
        SittingGuhRenderers.NPC_ANIMATIES.put(Kind.KUSSENGEVECHT_GUH, Guhs.id("entity/guh_npc_kussengevecht_guh"));
        SittingGuhRenderers.NPC_GLOED.add(Kind.ZANDGUH);
        SittingGuhRenderers.NPC_ANIMATORS.put(Kind.ZANDGUH, (npc, tick) -> {
            float zwaai = Mth.sin((float) (tick + npc.getId() * 23) * 0.08f) * 0.12f;
            return bones -> bones.ifPresent("zand_zak", b -> b.setRotZ(zwaai));
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(DroomwezensFeature.STERRENVISJE.get(), VisRenderer::new);
            event.registerEntityRenderer(DroomwezensFeature.KUSSEN_PROJECTIEL.get(), ThrownItemRenderer::new);
        });
    }

    /** The sterrenvisje: a texture per colour, and its glowmask full bright on top. */
    public static class VisRenderer extends GeoEntityRenderer<SterrenvisjeEntity, LivingEntityRenderState> {
        public VisRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SterrenvisjeEntity>(Guhs.id("sterrenvisje")) {
                @Override
                public void addAdditionalStateData(SterrenvisjeEntity vis, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(VIS_KLEUR, vis.kleur().id());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String kleur = state.getGeckolibData(VIS_KLEUR);
                    return Guhs.id("textures/entity/sterrenvisje_" + (kleur == null ? "geel" : kleur) + ".png");
                }
            });
            this.shadowRadius = 0.15f;
            withRenderLayer(new AutoGlowingGeoLayer<SterrenvisjeEntity, Void, LivingEntityRenderState>(this) {
                @Nullable
                @Override
                protected RenderType getRenderType(LivingEntityRenderState state) {
                    String kleur = state.getGeckolibData(VIS_KLEUR);
                    return GuhRenderTypes.eyes(Guhs.id("textures/entity/sterrenvisje_" + (kleur == null ? "geel" : kleur) + "_glowmask.png"));
                }
            });
        }
    }
}
