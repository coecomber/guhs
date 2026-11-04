package nl.juiced.guhs.feature.vadsrijk.bazen.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.HulpSlijmpje;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.ElementKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.KoningKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion;
import nl.juiced.guhs.feature.vadsrijk.bazen.mika.SpionBallon;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Opperzwam;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;

/** Het Vadsrijk, slice "bazen": renderers (GeckoLib) and the Opperzwam's darkness fog. */
public final class BazenClient {
    private static final DataTicket<Integer> ELEMENT = DataTicket.create("guhs_vr_baas_element", Integer.class);
    private static final DataTicket<Boolean> DONKER = DataTicket.create("guhs_vr_baas_donker", Boolean.class);
    private static final DataTicket<Boolean> ONTHULD = DataTicket.create("guhs_vr_baas_onthuld", Boolean.class);
    private static float mist, mistO;

    private BazenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(BazenClient::renderers);
        NeoForge.EVENT_BUS.addListener(BazenClient::onTick);
        NeoForge.EVENT_BUS.addListener(BazenClient::onFog);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/bazen/" + naam + ".png");
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(BazenRegistry.KONING_KLODDER.get(), ctx -> {
            var r = new GeoEntityRenderer<KoningKlodder, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<KoningKlodder>(Guhs.id("vr_baas_koning_klodder")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("koning_klodder");
                }
            });
            r.withScale(3.0f);
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.KK_KLODDER.get(), ctx -> {
            var r = new GeoEntityRenderer<ElementKlodder, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<ElementKlodder>(Guhs.id("vr_baas_kk_klodder")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    int el = state.getOrDefaultGeckolibData(ELEMENT, Element.ROZE.ordinal());
                    return tex(el == Element.WATER.ordinal() ? "kk_klodder_blauw" : el == Element.VUUR.ordinal() ? "kk_klodder_geel" : "kk_klodder_roze");
                }
            }) {
                @Override
                public void addRenderData(ElementKlodder mob, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(ELEMENT, mob.element().ordinal());
                }
            };
            r.withScale(1.3f);
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.OPPERZWAM.get(), ctx -> {
            var r = new GeoEntityRenderer<Opperzwam, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<Opperzwam>(Guhs.id("vr_baas_opperzwam")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(Boolean.TRUE.equals(state.getOrDefaultGeckolibData(DONKER, false)) ? "opperzwam_donker" : "opperzwam");
                }
            }) {
                @Override
                public void addRenderData(Opperzwam mob, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(DONKER, mob.donker());
                }
            };
            r.withScale(1.8f);
            r.withRenderLayer(new AutoGlowingGeoLayer<Opperzwam, Void, LivingEntityRenderState>(r));
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.MIKA_SPION.get(), ctx -> {
            var r = new GeoEntityRenderer<MikaSpion, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<MikaSpion>(Guhs.id("mika")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(Boolean.TRUE.equals(state.getOrDefaultGeckolibData(ONTHULD, false)) ? "mika_spion" : "mika_spion_vermomd");
                }
            }.withAltAnimations(Guhs.id("guh"))) {
                @Override
                public void addRenderData(MikaSpion mob, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(ONTHULD, mob.onthuld());
                }

                @Override
                public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                    DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
                }
            };
            r.withScale(1.3f);
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.LOK_MIKA.get(), ctx -> {   // (fix round 1: a decoy looks exactly like the unmasked spy)
            var r = new GeoEntityRenderer<nl.juiced.guhs.feature.vadsrijk.bazen.mika.LokMika, LivingEntityRenderState>(ctx,
                    new DefaultedEntityGeoModel<nl.juiced.guhs.feature.vadsrijk.bazen.mika.LokMika>(Guhs.id("mika")) {
                        @Override
                        public Identifier getTextureResource(GeoRenderState state) {
                            return tex("mika_spion");
                        }
                    }.withAltAnimations(Guhs.id("guh"))) {
                @Override
                public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                    DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
                }
            };
            r.withScale(1.3f);
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.SPION_BALLON.get(), ctx -> {
            var r = new GeoEntityRenderer<SpionBallon, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<SpionBallon>(Guhs.id("vr_baas_spion_ballon")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("spion_ballon");
                }
            });
            r.withScale(1.6f);
            return r;
        });
        e.registerEntityRenderer(BazenRegistry.HULPSLIJMPJE.get(), SlijmpjeRenderer::new);
    }

    /** The mini-slijmpje: the vanilla slime model with the pink guh slime texture. */
    static class SlijmpjeRenderer extends MobRenderer<HulpSlijmpje, SlimeRenderState, SlimeModel> {
        SlijmpjeRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new SlimeModel(ctx.bakeLayer(ModelLayers.SLIME)), 0.2f);
        }

        @Override
        public SlimeRenderState createRenderState() {
            return new SlimeRenderState();
        }

        @Override
        public void extractRenderState(HulpSlijmpje e, SlimeRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
            s.size = 1;
            s.squish = (float) Mth.clamp(-e.getDeltaMovement().y * 1.5, -0.6, 0.6);
        }

        @Override
        protected void scale(SlimeRenderState s, PoseStack pose) {
            float ss = s.squish / 1.5f;
            float w = 1.0f / (ss + 1.0f);
            pose.scale(w * 0.6f, 1.0f / w * 0.6f, w * 0.6f);
        }

        @Override
        public Identifier getTextureLocation(SlimeRenderState s) {
            return Guhs.id("textures/entity/guh_slime.png");
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mistO = mist;
        boolean donker = false;
        if (mc.level != null && mc.player != null) {
            var helm = BazenLoot.LICHTZWAMHELM.get();
            boolean beschermd = mc.player.getItemBySlot(EquipmentSlot.HEAD).is(helm);
            if (!beschermd) {
                donker = !mc.level.getEntitiesOfClass(Opperzwam.class, mc.player.getBoundingBox().inflate(40), Opperzwam::donker).isEmpty();
            }
        }
        mist = Mth.approach(mist, donker ? 1f : 0f, 0.04f);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        float m = Mth.lerp((float) event.getPartialTick(), mistO, mist);
        if (m <= 0.001f || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float newFar = Mth.lerp(m, far, Math.min(far, 9f));
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), 0f));
    }
}
