package nl.juiced.guhs.feature.sterrenwacht.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.sterrenwacht.Sterrenbeeld;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtPayloads;

/**
 * Client side of the Guh-Sterrenwacht: Professor Sterretje's own model (a tall starry hat and a little spyglass on a
 * cord, tools/features/sterrenwacht.py), the wensster sparkle and the telescope screen.
 */
public final class SterrenwachtClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(SterrenwachtClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.STERRENKIJKERGUH, Guhs.id("geo/entity/guh_npc_sterrenkijkerguh.geo.json"));
        // the star on the tip of his hat wobbles, and he peers through his little spyglass now and then
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.STERRENKIJKERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.05f;
            bot.apply("sterretje_hoedpunt").ifPresent(b -> b.setRotZ((float) Math.sin(t * 1.3f) * 0.12f));
            float kijk = (float) Math.max(0, Math.sin(t * 0.35f));
            bot.apply("sterretje_kijker").ifPresent(b -> b.setRotX(-kijk * 0.9f));
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SterrenwachtFeature.WENSSTER_DEELTJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Sterretje(level, x, y, z, dx, dy, dz, sprites));
    }

    /** guhs:sterrenwacht_open: look through the telescope. */
    public static void open(SterrenwachtPayloads.Open payload) {
        Sterrenbeeld beeld = Sterrenbeeld.byIndex(payload.beeld());
        if (beeld != null) {
            Minecraft.getInstance().setScreen(new TelescoopScherm(beeld, payload.seed(), payload.nieuw(), payload.vannacht()));
        }
    }

    /** A little star: twinkles (grows and shrinks), drifts up slowly, glows in the dark. */
    static class Sterretje extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float basis;

        Sterretje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 24 + random.nextInt(24);
            basis = 0.07f + random.nextFloat() * 0.06f;
            quadSize = basis;
            gravity = -0.002f;
            xd = dx + (random.nextDouble() - 0.5) * 0.01;
            yd = dy;
            zd = dz + (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float f = age / (float) lifetime;
            quadSize = basis * (0.6f + 0.4f * (float) Math.abs(Math.sin(age * 0.5))) * (1f - f * 0.5f);
            alpha = Math.min(1f, (1f - f) * 2.5f);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private SterrenwachtClient() {
    }
}
