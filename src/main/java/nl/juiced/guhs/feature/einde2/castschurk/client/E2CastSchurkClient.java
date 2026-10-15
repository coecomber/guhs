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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
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
 * model: the dragon's flying ring, the lion turtle's head; the lion turtle takes its light round its face, not from the nose
 * block it stands in). Models: tools/features/einde2_cast_schurk_modellen.py.
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

        /** The dragon flies a ring of radius 9 around its spot; the lion turtle's head with its mane and paw is some 44 blocks across. */
        @Override
        protected AABB getBoundingBoxForCulling(CastWezen dier) {
            AABB box = dier.getBoundingBox();
            return switch (wezen) {
                case DRAAKGUH -> box.inflate(11.0, 0, 11.0).expandTowards(0, 6.0, 0);
                case LEEUWSCHILDPAD_GUH -> box.inflate(26.0 * dier.getScale());
                case LUCHTBIZON_GUH, WAN_SHI_GUH -> box.inflate(1.5);
                default -> box.inflate(0.5);
            };
        }

        /**
         * The Leeuwschildpad-guh stands with its origin in the nose block of the island (and the palm under it): the light
         * there is 0, which would draw the whole head black. It takes the brightest light round its face instead, and never
         * less than {@link #SCHILDPAD_GLOED} block light (the block head had glowing eyes of sea lanterns that lit it).
         */
        @Override
        protected int getBlockLightLevel(CastWezen dier, BlockPos pos) {
            return wezen == Wezens.LEEUWSCHILDPAD_GUH ? rondKop(dier, pos, LightLayer.BLOCK) : super.getBlockLightLevel(dier, pos);
        }

        @Override
        protected int getSkyLightLevel(CastWezen dier, BlockPos pos) {
            return wezen == Wezens.LEEUWSCHILDPAD_GUH ? rondKop(dier, pos, LightLayer.SKY) : super.getSkyLightLevel(dier, pos);
        }

        private static int rondKop(CastWezen dier, BlockPos pos, LightLayer laag) {
            Direction voor = Direction.fromYRot(dier.getYRot());
            int stap = Math.max(1, Math.round(4 * dier.getScale()));
            int licht = dier.level().getBrightness(laag, pos);
            for (BlockPos p : new BlockPos[] {pos.above(stap), pos.above(2 * stap), pos.relative(voor, stap).above(stap), pos.relative(voor, 2).above(1)}) {
                licht = Math.max(licht, dier.level().getBrightness(laag, p));
            }
            return laag == LightLayer.BLOCK ? Math.max(licht, SCHILDPAD_GLOED) : licht;
        }

        /** The least block light the Leeuwschildpad-guh's head is drawn with. */
        static final int SCHILDPAD_GLOED = 7;

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
