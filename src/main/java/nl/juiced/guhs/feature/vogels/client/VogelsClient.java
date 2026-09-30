package nl.juiced.guhs.feature.vogels.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vogels.GuhUiltjeEntity;
import nl.juiced.guhs.feature.vogels.Vogeltje;
import nl.juiced.guhs.feature.vogels.VogelsFeature;

/**
 * The birds' renderers (3.0 vogels): their own GeckoLib models (tools/features/vogels_modellen.py) with a turning head, eyes
 * that blink now and then and stay shut while the owl sleeps (the &lt;name&gt;_dicht.png texture), the kaasmeesje turned upside
 * down while it hangs under a leaf, and the owl's eyes glowing in the dark while it's awake; plus the drifting feather
 * particle.
 */
public final class VogelsClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(VogelsClient::renderers);
        modBus.addListener(VogelsClient::particles);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VogelsFeature.PLUISVINKJE.get(), c -> new VogelRenderer<>(c, "pluisvinkje", 0.2f));
        event.registerEntityRenderer(VogelsFeature.KAASMEESJE.get(), c -> new VogelRenderer<>(c, "kaasmeesje", 0.2f));
        event.registerEntityRenderer(VogelsFeature.GUH_UILTJE.get(), c -> new VogelRenderer<>(c, "guh_uiltje", 0.25f));
        event.registerEntityRenderer(VogelsFeature.ZEEMEEUWTJE.get(), c -> new VogelRenderer<>(c, "zeemeeuwtje", 0.25f));
    }

    /** Eyes shut: sleeping (the owl by day) or a blink of 3 ticks every few seconds. */
    public static boolean ogenDicht(Vogeltje v) {
        return v.houding() == Vogeltje.SLAAPT || (v.tickCount + v.getId() * 37) % 83 < 3;
    }

    /** Render state tickets (1.1.0): eyes shut, hanging upside down, the owl's glowing eyes. */
    static final DataTicket<Boolean> DICHT = DataTicket.create("guhs_vogel_dicht", Boolean.class);
    static final DataTicket<Boolean> HANGT = DataTicket.create("guhs_vogel_hangt", Boolean.class);
    static final DataTicket<Boolean> GLOEIT = DataTicket.create("guhs_vogel_gloeit", Boolean.class);

    public static class VogelRenderer<T extends Vogeltje> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public VogelRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam)) {
                private final Identifier open = Guhs.id("textures/entity/" + naam + ".png");
                private final Identifier dicht = Guhs.id("textures/entity/" + naam + "_dicht.png");

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(DICHT)) ? dicht : open;
                }
            });
            this.shadowRadius = schaduw;
            if (naam.equals("guh_uiltje")) {
                withRenderLayer(new GeoRenderLayer<T, Void, LivingEntityRenderState>(this) {
                    private final Identifier glow = Guhs.id("textures/entity/guh_uiltje_glowmask.png");

                    @Override
                    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
                        if (Boolean.TRUE.equals(info.getGeckolibData(GLOEIT)) && info.willRender()) {
                            getRenderer().submitRenderTasks(info, collector.order(1), nl.juiced.guhs.client.GuhRenderTypes.eyes(glow));
                        }
                    }
                });
            }
        }

        @Override
        public void addRenderData(T vogel, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            boolean dicht = ogenDicht(vogel);
            state.addGeckolibData(DICHT, dicht);
            state.addGeckolibData(HANGT, !vogel.vliegt() && vogel.houding() == Vogeltje.HANGT);
            state.addGeckolibData(GLOEIT, vogel instanceof GuhUiltjeEntity uil && uil.nacht() && !dicht);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }

        @Override
        protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack pose, float nativeScale) {
            super.applyRotations(info, pose, nativeScale);
            if (Boolean.TRUE.equals(info.getGeckolibData(HANGT))) {
                // upside down under its leaf: turned over around its middle
                float h = info.renderState().boundingBoxHeight;
                pose.translate(0, h / 2, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(0, -h / 2, 0);
            }
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VogelsFeature.VEERTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Veertje(level, x, y, z, dx, dy, dz, sprites.get(random)));
    }

    /** A little pink feather: pops out, then sways down slowly. */
    static class Veertje extends SingleQuadParticle {
        private final float spin;

        Veertje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 50 + random.nextInt(40);
            quadSize = 0.07f + random.nextFloat() * 0.05f;
            gravity = 0.015f;
            xd = dx + (random.nextDouble() - 0.5) * 0.04;
            yd = dy + 0.02;
            zd = dz + (random.nextDouble() - 0.5) * 0.04;
            friction = 0.9f;
            spin = (random.nextFloat() - 0.5f) * 0.2f;
            roll = random.nextFloat() * 6.28f;
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += spin;
            xd += Math.sin(age * 0.18) * 0.003;
            alpha = Math.min(1f, (lifetime - age) / 12f);
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private VogelsClient() {
    }
}
