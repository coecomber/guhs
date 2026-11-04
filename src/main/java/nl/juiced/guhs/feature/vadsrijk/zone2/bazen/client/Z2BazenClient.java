package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Hulpje;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boorkop;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boormeester;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf.Galmwolf;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelbeeld;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelzucht;

/**
 * Zone-2 bazen, the client: GeckoLib renderers, the Galmwolf's blizzard (white fog in fase 3), and Vrouwe Spiegelzucht's
 * mirror images: no nameplate and NO vanilla shadow on her or her images (the only shadows are the server's, so the rule
 * "the one without a shadow is her" is fair).
 */
public final class Z2BazenClient {
    private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_vr_z2_kop_kleur", Integer.class);
    private static final DataTicket<Boolean> HEET = DataTicket.create("guhs_vr_z2_kop_heet", Boolean.class);
    private static float mist, mistO;

    private Z2BazenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Z2BazenClient::renderers);
        NeoForge.EVENT_BUS.addListener(Z2BazenClient::onTick);
        NeoForge.EVENT_BUS.addListener(Z2BazenClient::onFog);
        NeoForge.EVENT_BUS.addListener(Z2BazenClient::onFogKleur);
        NeoForge.EVENT_BUS.addListener(Z2BazenClient::naamplaatje);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/z2bazen/" + naam + ".png");
    }

    private static <T extends net.minecraft.world.entity.LivingEntity & com.geckolib.animatable.GeoEntity> DefaultedEntityGeoModel<T> model(String id,
            String textuur) {
        return new DefaultedEntityGeoModel<T>(Guhs.id(id)) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return tex(textuur);
            }
        };
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Z2BazenRegistry.GALMWOLF.get(), ctx -> {
            var r = new GeoEntityRenderer<Galmwolf, LivingEntityRenderState>(ctx, Z2BazenClient.<Galmwolf>model("vr_z2_baas_galmwolf", "galmwolf"));
            r.withScale(2.0f);
            r.withRenderLayer(new AutoGlowingGeoLayer<Galmwolf, Void, LivingEntityRenderState>(r));
            return r;
        });
        e.registerEntityRenderer(Z2BazenRegistry.SPIEGELZUCHT.get(), SchaduwlozeRenderer<Spiegelzucht>::new);
        e.registerEntityRenderer(Z2BazenRegistry.SPIEGELBEELD.get(), SchaduwlozeRenderer<Spiegelbeeld>::new);
        e.registerEntityRenderer(Z2BazenRegistry.BOORMEESTER.get(), ctx -> {
            var r = new GeoEntityRenderer<Boormeester, LivingEntityRenderState>(ctx, Z2BazenClient.<Boormeester>model("vr_z2_baas_boormeester", "boormeester"));
            r.withScale(1.6f);
            return r;
        });
        e.registerEntityRenderer(Z2BazenRegistry.BOORKOP.get(), ctx -> {
            var r = new GeoEntityRenderer<Boorkop, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<Boorkop>(Guhs.id("vr_z2_baas_boorkop")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    int k = state.getOrDefaultGeckolibData(KLEUR, 0);
                    boolean h = Boolean.TRUE.equals(state.getOrDefaultGeckolibData(HEET, false));
                    return tex("boorkop_" + Boorkop.KLEUREN[Math.max(0, Math.min(2, k))] + (h ? "_heet" : ""));
                }
            }) {
                @Override
                public void addRenderData(Boorkop mob, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(KLEUR, mob.kleur());
                    state.addGeckolibData(HEET, mob.oververhit());
                }
            };
            r.withScale(1.4f);
            return r;
        });
        e.registerEntityRenderer(Z2BazenRegistry.WELP.get(), ctx -> {
            var r = new GeoEntityRenderer<Z2Hulpje, LivingEntityRenderState>(ctx, Z2BazenClient.<Z2Hulpje>model("vr_z2_baas_welp", "welp"));
            r.withRenderLayer(new AutoGlowingGeoLayer<Z2Hulpje, Void, LivingEntityRenderState>(r));
            return r;
        });
        e.registerEntityRenderer(Z2BazenRegistry.MIJNWERKER.get(), MijnwerkerRenderer::new);
    }

    /** Vrouwe Spiegelzucht and her images: the same look, no vanilla shadow. */
    static class SchaduwlozeRenderer<T extends net.minecraft.world.entity.LivingEntity & com.geckolib.animatable.GeoEntity>
            extends GeoEntityRenderer<T, LivingEntityRenderState> {
        SchaduwlozeRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, Z2BazenClient.<T>model("vr_z2_baas_spiegelzucht", "spiegelzucht"));
            this.shadowRadius = 0f;
            withScale(1.25f);
            withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(this));
        }

        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            return 0f;
        }
    }

    /** A Mika-mijnwerker: the Mika model with a miner's look. */
    static class MijnwerkerRenderer extends GeoEntityRenderer<Z2Hulpje, LivingEntityRenderState> {
        MijnwerkerRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<Z2Hulpje>(Guhs.id("mika")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("mijnwerker");
                }
            }.withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.4f;
            withScale(1.1f);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private static void naamplaatje(RenderNameTagEvent.CanRender e) {
        if (e.getEntity() instanceof Spiegelbeeld || (e.getEntity() instanceof Spiegelzucht s && s.gesplitst())) {
            e.setCanRender(TriState.FALSE);
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mistO = mist;
        boolean storm = mc.level != null && mc.player != null
                && !mc.level.getEntitiesOfClass(Galmwolf.class, mc.player.getBoundingBox().inflate(36), Galmwolf::blizzard).isEmpty();
        mist = Mth.approach(mist, storm ? 1f : 0f, 0.03f);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        float m = Mth.lerp((float) event.getPartialTick(), mistO, mist);
        if (m <= 0.001f || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float far = event.getFarPlaneDistance();
        event.setFarPlaneDistance(Mth.lerp(m, far, Math.min(far, 14f)));
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), 0f));
    }

    private static void onFogKleur(ViewportEvent.ComputeFogColor event) {
        float m = mist;
        if (m <= 0.001f) {
            return;
        }
        event.setRed(Mth.lerp(m, event.getRed(), 0.90f));
        event.setGreen(Mth.lerp(m, event.getGreen(), 0.94f));
        event.setBlue(Mth.lerp(m, event.getBlue(), 0.99f));
    }
}
