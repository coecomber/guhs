package nl.juiced.guhs.feature.einde2.geestwezens.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestKinds;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezens;
import nl.juiced.guhs.feature.einde2.geestwezens.HeiBaiGuhEntity;

/**
 * einde2, slice geestmodellen, client side: the two kinds go into the stock NPC renderer ({@link SittingGuhRenderers#NPC_MODELEN},
 * their own animation files; the Theegeest see-through and glowing through {@code NPC_DOORZICHTIG} / {@code NPC_GLOED}), and one
 * GeckoLib renderer draws every creature ({@link GeestRenderer}): its texture per look, translucent where the spirit is
 * see-through, its glow mask full bright on top ({@code <texture>_glowmask.png}, added with {@link GuhRenderTypes#eyes}), Hei
 * Bai-guh's other form hidden, and a culling box that fits the model (Koh's body and Fang's tail reach far behind the hitbox).
 * Models: tools/features/einde2_geestmodellen_wezens.py and _kinds.py.
 */
public final class E2GeestWezensClient {
    static final DataTicket<Integer> SOORT = DataTicket.create("guhs_e2_geestmodellen_soort", Integer.class);
    static final DataTicket<Boolean> WEG = DataTicket.create("guhs_e2_geestmodellen_weg", Boolean.class);

    public static void init(IEventBus modBus) {
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds()) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
            SittingGuhRenderers.NPC_ANIMATIES.put(kind, Guhs.id("entity/guh_npc_" + GeestKinds.basis(kind).id()));
            if (GeestKinds.doorzichtig(kind)) {
                SittingGuhRenderers.NPC_DOORZICHTIG.add(kind);
            }
            if (GeestKinds.gloed(kind)) {
                SittingGuhRenderers.NPC_GLOED.add(kind);
            }
            int sets = GeestKinds.mix(kind);
            if (sets > 0) {                                         // (a crowd kind: one mix set, picked from where it stands)
                SittingGuhRenderers.NPC_ANIMATORS.put(kind, (npc, tick) -> {
                    int eigen = Math.floorMod(npc.getBlockX() * 31 + npc.getBlockZ() * 17, sets);
                    return bones -> {
                        for (int i = 0; i < sets; i++) {
                            if (i != eigen) {
                                for (String naam : GeestKinds.mixBotten(kind, i)) {
                                    bones.ifPresent(naam, b -> b.skipRender(true).skipChildrenRender(true));
                                }
                            }
                        }
                    };
                });
            }
        }
        modBus.addListener(E2GeestWezensClient::renderers);
    }

    @SuppressWarnings("unchecked")
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (GeestWezens w : GeestWezens.values()) {
            event.registerEntityRenderer((EntityType<GeestWezen>) E2GeestWezensFeature.type(w), context -> new GeestRenderer(context, w));
        }
    }

    static Identifier textuur(GeestWezens w, int soort) {
        return Guhs.id("textures/entity/" + w.textuur(soort) + ".png");
    }

    /** One creature: its own model and animations, its texture per look, see-through and glowing where the table says so. */
    public static class GeestRenderer extends GeoEntityRenderer<GeestWezen, LivingEntityRenderState> {
        private final GeestWezens wezen;

        public GeestRenderer(EntityRendererProvider.Context context, GeestWezens wezen) {
            super(context, new DefaultedEntityGeoModel<GeestWezen>(Guhs.id(wezen.id())) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return textuur(wezen, state.getOrDefaultGeckolibData(SOORT, 0));
                }
            });
            this.wezen = wezen;
            this.shadowRadius = switch (wezen) {
                case KOH -> 1.6f;
                case OUDE_REIGERGUH -> 1.4f;
                case KENNISVOSJE, MISTREIGER -> 0.3f;
                case WAN_GUH -> 0.4f;
                case HEI_BAI_GUH -> 0.5f;
                case SCHADUWGEEST -> 0.4f;
                case LANTAARNGEEST, LIBELKONIJNGUH -> 0.2f;
                default -> 0f;
            };
            if (wezen.gloed) {
                withRenderLayer(new AutoGlowingGeoLayer<GeestWezen, Void, LivingEntityRenderState>(this) {
                    @Nullable
                    @Override
                    protected RenderType getRenderType(LivingEntityRenderState state) {
                        return GuhRenderTypes.eyes(Guhs.id("textures/entity/" + wezen.textuur(state.getOrDefaultGeckolibData(SOORT, 0)) + "_glowmask.png"));
                    }
                });
            }
        }

        @Override
        public void addRenderData(GeestWezen geest, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(SOORT, geest.soort());
            state.addGeckolibData(WEG, geest.onzichtbaar());
        }

        /** A hidden spirit (on purpose, or Wan-guh / a giant hiding by itself) is not drawn at all. */
        @Override
        public boolean shouldRender(GeestWezen geest, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
            return !geest.onzichtbaar() && super.shouldRender(geest, frustum, x, y, z);
        }

        /** A see-through spirit is drawn translucent (its texture's alpha counts). */
        @Override
        public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            if (wezen.doorzichtig && !state.isInvisible) {
                return RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        /** Koh's body and Fang's tail reach ten blocks behind the hitbox, the koi's fins and the giant Hei Bai's arms wide. */
        @Override
        protected AABB getBoundingBoxForCulling(GeestWezen geest) {
            AABB box = geest.getBoundingBox();
            return switch (wezen) {
                case KOH -> box.inflate(10.0, 2.0, 10.0);
                case FANG -> box.inflate(9.0, 2.0, 9.0);
                case GEEST_KOI -> box.inflate(4.5, 1.5, 4.5);
                case HEI_BAI_GUH -> box.inflate(2.0, 0.5, 2.0);
                case OUDE_REIGERGUH -> box.inflate(4.0, 5.0, 4.0);
                case GEESTREUS -> box.inflate(40.0, 30.0, 40.0);
                case SCHADUWGEEST -> box.inflate(1.0, 0.2, 1.0);
                default -> box.inflate(0.5);
            };
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (wezen != GeestWezens.GEEST_KOI && wezen != GeestWezens.GEESTREUS && wezen != GeestWezens.OUDE_REIGERGUH) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
            if (wezen == GeestWezens.GEESTREUS) {                    // (one form per look)
                int vorm = info.getGeckolibData(SOORT) instanceof Integer s ? s : 0;
                for (int i = 0; i < 4; i++) {
                    if (i != vorm) {
                        bones.ifPresent("vorm_" + i, b -> b.skipRender(true).skipChildrenRender(true));
                    }
                }
            }
            if (wezen == GeestWezens.HEI_BAI_GUH) {
                boolean boos = info.getGeckolibData(SOORT) instanceof Integer s && s == HeiBaiGuhEntity.BOOS;
                bones.ifPresent(boos ? "klein" : "groot", b -> b.skipRender(true).skipChildrenRender(true));
            }
        }
    }

    private E2GeestWezensClient() {
    }
}
