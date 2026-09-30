package nl.juiced.guhs.feature.vogels.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vogels.GuhUiltjeEntity;
import nl.juiced.guhs.feature.vogels.Vogeltje;
import nl.juiced.guhs.feature.vogels.VogelsFeature;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

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

    public static class VogelRenderer<T extends Vogeltje> extends GeoEntityRenderer<T> {
        public VogelRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam), true) {
                private final ResourceLocation open = Guhs.id("textures/entity/" + naam + ".png");
                private final ResourceLocation dicht = Guhs.id("textures/entity/" + naam + "_dicht.png");

                @Override
                public ResourceLocation getTextureResource(T vogel) {
                    return ogenDicht(vogel) ? dicht : open;
                }
            });
            this.shadowRadius = schaduw;
            if (naam.equals("guh_uiltje")) {
                addRenderLayer(new GeoRenderLayer<>(this) {
                    private final ResourceLocation glow = Guhs.id("textures/entity/guh_uiltje_glowmask.png");

                    @Override
                    public void render(PoseStack poseStack, T vogel, BakedGeoModel model, RenderType renderType, MultiBufferSource buffers,
                                       VertexConsumer buffer, float partialTick, int light, int overlay) {
                        if (vogel instanceof GuhUiltjeEntity uil && uil.nacht() && !ogenDicht(vogel)) {
                            RenderType type = RenderType.eyes(glow);
                            getRenderer().reRender(model, poseStack, buffers, vogel, type, buffers.getBuffer(type), partialTick,
                                    LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
                        }
                    }
                });
            }
        }

        @Override
        protected void applyRotations(T vogel, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
            super.applyRotations(vogel, pose, ageInTicks, rotationYaw, partialTick, nativeScale);
            if (!vogel.vliegt() && vogel.houding() == Vogeltje.HANGT) {
                // upside down under its leaf: turned over around its middle
                pose.translate(0, vogel.getBbHeight() / 2, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(0, -vogel.getBbHeight() / 2, 0);
            }
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VogelsFeature.VEERTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Veertje(level, x, y, z, dx, dy, dz, sprites));
    }

    /** A little pink feather: pops out, then sways down slowly. */
    static class Veertje extends TextureSheetParticle {
        private final float spin;

        Veertje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
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
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private VogelsClient() {
    }
}
