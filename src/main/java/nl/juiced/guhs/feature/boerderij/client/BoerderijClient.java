package nl.juiced.guhs.feature.boerderij.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the Guhboerderij: the GeckoLib renderers of the three animals (babies smaller; a shorn guhschaapje shows
 * its bare, pink body until its wool is back the next day), the particles (wolplukje, melkdruppel) and Boerin Hooibaal's
 * own model (a straw hat, a straw in her mouth that wiggles, an apron; tools/features/boerderij_dieren.py).
 */
public final class BoerderijClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BoerderijClient::renderers);
        modBus.addListener(BoerderijClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BOERINNEGUH, Guhs.id("geo/entity/guh_npc_boerinneguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BOERINNEGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.08f;
            bot.apply("boerin_strootje").ifPresent(b -> b.setRotY((float) Math.sin(t) * 0.25f));
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BoerderijFeature.GUHSCHAAPJE.get(), context -> new DierRenderer<>(context, "guhschaapje", 0.45f));
        event.registerEntityRenderer(BoerderijFeature.KNABBELKIPPETJE.get(), context -> new DierRenderer<>(context, "knabbelkippetje", 0.3f));
        event.registerEntityRenderer(BoerderijFeature.GUHKOE.get(), context -> new DierRenderer<>(context, "guhkoe", 0.55f));
    }

    /** A farm animal: its own model, a turning head, babies at 60 %, and the wool that goes and comes back (bones wol / kaal). */
    public static class DierRenderer<T extends BoerderijDier> extends GeoEntityRenderer<T> {
        public DierRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam), true) {
                @Override
                public void setCustomAnimations(T dier, long instanceId, AnimationState<T> state) {
                    super.setCustomAnimations(dier, instanceId, state);
                    GeoBone wol = getAnimationProcessor().getBone("wol");
                    GeoBone kaal = getAnimationProcessor().getBone("kaal");
                    if (wol != null && kaal != null) {
                        wol.setHidden(dier.productGegeven());
                        kaal.setHidden(!dier.productGegeven());
                    }
                }
            });
            this.shadowRadius = schaduw;
        }

        @Override
        public void render(T dier, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float scale = dier.isBaby() ? 0.6f : 1f;
            this.scaleWidth = scale;
            this.scaleHeight = scale;
            super.render(dier, yaw, partialTick, pose, buffers, light);
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BoerderijFeature.WOLPLUKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Wolplukje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(BoerderijFeature.MELKDRUPPEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Melkdruppel(level, x, y, z, sprites));
    }

    /** A soft tuft of pluiswol: pops out, then drifts down slowly, swaying. */
    static class Wolplukje extends TextureSheetParticle {
        Wolplukje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 40 + random.nextInt(40);
            quadSize = 0.08f + random.nextFloat() * 0.06f;
            gravity = 0.02f;
            xd = dx + (random.nextDouble() - 0.5) * 0.08;
            yd = 0.05 + random.nextDouble() * 0.05;
            zd = dz + (random.nextDouble() - 0.5) * 0.08;
            friction = 0.92f;
        }

        @Override
        public void tick() {
            super.tick();
            xd += Math.sin(age * 0.2) * 0.002;
            alpha = Math.min(1f, (lifetime - age) / 10f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A drop of kaasmelk: falls and is gone. */
    static class Melkdruppel extends TextureSheetParticle {
        Melkdruppel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 16 + random.nextInt(10);
            quadSize = 0.06f + random.nextFloat() * 0.03f;
            gravity = 0.7f;
            xd = (random.nextDouble() - 0.5) * 0.05;
            yd = 0.08;
            zd = (random.nextDouble() - 0.5) * 0.05;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    private BoerderijClient() {
    }
}
