package nl.juiced.guhs.feature.einde2.castschurk.client;

import java.util.List;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.castschurk.CastSchurk;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.castschurk.VosguhEntity;
import nl.juiced.guhs.feature.einde2.castschurk.Wezens;

/**
 * einde2, slice cast_schurk, client side: the models of its Mika kinds go into the stock NPC renderer
 * ({@link SittingGuhRenderers#NPC_MODELEN}, one model per kind, one animation file per character), the Vuurnatie-Mika's show
 * one of their three mix sets (picked from where they stand), and one GeckoLib renderer draws every creature
 * ({@link WezenRenderer}: its texture per look, the vosguh's scroll only while it carries one, a culling box that fits the
 * model: the dragon's flying ring, the lion turtle's face). Models: tools/features/einde2_cast_schurk_modellen.py.
 */
public final class E2CastSchurkClient {
    static final DataTicket<Integer> SOORT = DataTicket.create("guhs_e2_cast_schurk_soort", Integer.class);
    static final DataTicket<Boolean> DRAAGT = DataTicket.create("guhs_e2_cast_schurk_draagt", Boolean.class);

    public static void init(IEventBus modBus) {
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
            SittingGuhRenderers.NPC_ANIMATIES.put(kind, Guhs.id("entity/guh_npc_" + CastSchurk.basis(kind).id()));
            int sets = CastSchurk.mix(kind);
            if (sets > 0) {
                SittingGuhRenderers.NPC_ANIMATORS.put(kind, (npc, tick) -> {
                    int eigen = Math.floorMod(npc.getBlockX() * 31 + npc.getBlockZ() * 17, sets);
                    return bones -> {
                        for (int i = 0; i < sets; i++) {
                            if (i == eigen) {
                                continue;
                            }
                            List<String> namen = CastSchurk.mixBotten(kind, i);
                            for (String naam : namen) {
                                bones.ifPresent(naam, b -> b.skipRender(true).skipChildrenRender(true));
                            }
                        }
                    };
                });
            }
        }
        modBus.addListener(E2CastSchurkClient::renderers);
    }

    @SuppressWarnings("unchecked")
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (Wezens w : Wezens.values()) {
            event.registerEntityRenderer((EntityType<CastWezen>) E2CastSchurkFeature.type(w), context -> new WezenRenderer(context, w));
        }
    }

    /** One creature: its own model and animations, its texture per look. */
    public static class WezenRenderer extends GeoEntityRenderer<CastWezen, LivingEntityRenderState> {
        private final Wezens wezen;

        public WezenRenderer(EntityRendererProvider.Context context, Wezens wezen) {
            super(context, new DefaultedEntityGeoModel<CastWezen>(Guhs.id(wezen.id())) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/entity/" + wezen.textuur(state.getOrDefaultGeckolibData(SOORT, 0)) + ".png");
                }
            });
            this.wezen = wezen;
            this.shadowRadius = wezen == Wezens.LEEUWSCHILDPAD_GUH ? 0f : Math.min(1.6f, wezen.breed * 0.5f);
        }

        @Override
        public void addRenderData(CastWezen dier, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(SOORT, dier.soort());
            state.addGeckolibData(DRAAGT, dier instanceof VosguhEntity vos && vos.draagt());
        }

        /** The dragon flies a ring of radius 9 around its spot, the lion turtle's parts lie all over a head of 24 blocks. */
        @Override
        protected AABB getBoundingBoxForCulling(CastWezen dier) {
            AABB box = dier.getBoundingBox();
            return switch (wezen) {
                case DRAAKGUH -> box.inflate(11.0, 0, 11.0).expandTowards(0, 6.0, 0);
                case LEEUWSCHILDPAD_GUH -> box.inflate(16.0, 12.0, 16.0);
                case LUCHTBIZON_GUH, WAN_SHI_GUH -> box.inflate(1.5);
                default -> box.inflate(0.5);
            };
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (wezen != Wezens.LEEUWSCHILDPAD_GUH && wezen != Wezens.MAANVIS && wezen != Wezens.OCEAANVIS) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
            if (wezen == Wezens.VOSGUH && !Boolean.TRUE.equals(info.getGeckolibData(DRAAGT))) {
                bones.ifPresent("vosguh_rol", b -> b.skipRender(true).skipChildrenRender(true));
            }
        }
    }

    private E2CastSchurkClient() {
    }
}
