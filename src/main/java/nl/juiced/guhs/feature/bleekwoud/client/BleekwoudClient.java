package nl.juiced.guhs.feature.bleekwoud.client;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bleekwoud.BleekwoudFeature;
import nl.juiced.guhs.feature.bleekwoud.KraakMikaEntity;
import nl.juiced.guhs.feature.bleekwoud.KraakguhEntity;

/**
 * Client side of the Bleekwoud: the two wooden creatures (their eyes only glow while they are awake: a glow mask layer),
 * the sign's texture, and the pale haze that hangs between the trees.
 */
public final class BleekwoudClient {
    /** The haze (blocks): clear up to HAZE_NEAR, fully pale at HAZE_FAR. */
    public static final float HAZE_FAR = 88f, HAZE_NEAR = 20f;
    private static final DataTicket<Boolean> OGEN = DataTicket.create("guhs_kraak_ogen", Boolean.class);
    private static float haze;
    private static float hazeO;

    public static void init(IEventBus modBus) {
        modBus.addListener(BleekwoudClient::renderers);
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> Sheets.addWoodType(BleekwoudFeature.BLEEKHOUT_WOOD)));
        NeoForge.EVENT_BUS.addListener(BleekwoudClient::onTick);
        NeoForge.EVENT_BUS.addListener(BleekwoudClient::onFog);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BleekwoudFeature.KRAAKGUH.get(), ctx -> BleekwoudClient.<KraakguhEntity>renderer(ctx, "kraakguh", KraakguhEntity::wakker));
        event.registerEntityRenderer(BleekwoudFeature.KRAAK_MIKA.get(), ctx -> BleekwoudClient.<KraakMikaEntity>renderer(ctx, "kraak_mika", KraakMikaEntity::wakker));
    }

    /** geckolib/models/entity/&lt;id&gt;.geo.json + textures/entity/&lt;id&gt;.png (+ _glowmask.png: the eyes), the kraakguh animations. */
    private static <T extends Mob & GeoAnimatable> GeoEntityRenderer<T, LivingEntityRenderState> renderer(EntityRendererProvider.Context ctx, String id,
                                                                                                         Predicate<T> wakker) {
        var r = new GeoEntityRenderer<T, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<T>(Guhs.id(id)).withAltAnimations(Guhs.id("kraakguh"))) {
            {
                this.shadowRadius = 0.45f;
            }

            @Override
            public void addRenderData(T mob, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(OGEN, wakker.test(mob));
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        };
        r.withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(r) {
            @Nullable
            @Override
            protected RenderType getRenderType(LivingEntityRenderState state) {
                return Boolean.TRUE.equals(state.getOrDefaultGeckolibData(OGEN, false)) ? super.getRenderType(state) : null;
            }
        });
        return r;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        hazeO = haze;
        boolean inWood = mc.level != null && mc.gameRenderer.getMainCamera().isInitialized()
                && mc.level.getBiome(mc.gameRenderer.getMainCamera().blockPosition()).is(BleekwoudFeature.BLEEKWOUD);
        haze = Mth.approach(haze, inWood ? 1f : 0f, 0.012f);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        float m = Mth.lerp((float) event.getPartialTick(), hazeO, haze);
        if (m <= 0.001f || !(event.getEnvironment() instanceof net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment)
                || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float newFar = Mth.lerp(m, far, Math.min(far, HAZE_FAR));
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), Math.min(HAZE_NEAR, newFar * 0.4f)));
    }

    private BleekwoudClient() {
    }
}
