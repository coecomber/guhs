package nl.juiced.guhs.feature.klusjes.client;

import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.klusjes.KlusjesFeature;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.client.GuhRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Client side of klusjes (2.10): the sparkle and the "!" particles, and the little icon over a working guh's head (its
 * tool while it works, what it carries on the way to the chest: the display copy in its hand, flag KLUSJE).
 */
public final class KlusjesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(KlusjesFeature.STERRETJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Sterretje(level, x, y, z, sprites.get(random)));
            event.registerSpriteSet(KlusjesFeature.UITROEP.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Uitroep(level, x, y, z, sprites.get(random)));
        });
        // 1.1.0: a GuhRenderer hook (extract: values and the item's render state; submit: the pose and the item)
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!BandVlaggen.heeft(guh, BandVlaggen.KLUSJE)) {
                return;
            }
            ItemStack stack = guh.getMainHandItem();
            if (!StappenTaak.isToon(stack)) {
                return;
            }
            float t = guh.tickCount + partialTick;
            double up = guh.getBbHeight() / Math.max(0.2f, guh.getScale()) + 0.3 + Math.sin(t * 0.15) * 0.04;
            float turn = (float) Math.sin(t * 0.05) * 25f;
            ItemStackRenderState item = GuhRenderer.itemState(stack, ItemDisplayContext.GROUND, guh);
            frame.layerExtra((pose, collector, light) -> {
                pose.translate(0, up, 0);
                pose.mulPose(Axis.YP.rotationDegrees(turn));
                pose.scale(0.55f, 0.55f, 0.55f);
                item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            });
        });
    }

    /** A little yellow star: pops up, twinkles and fades. */
    static class Sterretje extends SingleQuadParticle {
        Sterretje(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 16 + random.nextInt(12);
            quadSize = 0.07f + random.nextFloat() * 0.05f;
            gravity = -0.01f;
            xd = (random.nextDouble() - 0.5) * 0.04;
            yd = 0.03 + random.nextDouble() * 0.03;
            zd = (random.nextDouble() - 0.5) * 0.04;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            xd *= 0.9;
            zd *= 0.9;
            alpha = (age / 3) % 2 == 0 ? 1f : 0.55f;
            if (age > lifetime - 6) {
                alpha *= (lifetime - age) / 6f;
            }
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A pink "!" that bounces up over a guh that peeps a warning, then fades. */
    static class Uitroep extends SingleQuadParticle {
        private final double basisY;

        Uitroep(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            basisY = y;
            lifetime = 30;
            quadSize = 0.25f;
            gravity = 0f;
            xd = yd = zd = 0;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            yd = 0;
            y = basisY + Math.abs(Math.sin(age * 0.35)) * 0.15 * Math.max(0, 1 - age / 20.0);
            yo = y;
            if (age > lifetime - 8) {
                alpha = (lifetime - age) / 8f;
            }
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private KlusjesClient() {
    }
}
