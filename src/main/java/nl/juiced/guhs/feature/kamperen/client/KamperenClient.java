package nl.juiced.guhs.feature.kamperen.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Client side of the kampeerplekjes: the pyjamas (a guh with the PYJAMA flag is drawn in the pyjama_pakje and the
 * slaapmutsje, over whatever isn't covered by its own clothes), the campfire sparks, and Opa Guh's own model
 * (moustache, round glasses, nightcap with a pompom, a walking stick).
 */
public final class KamperenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(KamperenClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.OPA_GUH, Guhs.id("entity/guh_npc_opa_guh"));
        // the pompom of his nightcap swings a little as he talks, and his moustache wiggles
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.OPA_GUH, (npc, tick) -> {
            float t = (float) tick * 0.07f;
            return bones -> {
                bones.ifPresent("opa_mutspunt", b -> b.setRotZ(0.35f + (float) Math.sin(t) * 0.12f));
                bones.ifPresent("opa_snor", b -> b.setRotZ((float) Math.sin(t * 2.3f) * 0.05f));
            };
        });
        // 1.1.0: a GuhRenderer hook instead of a GuhRenderHooks layer: the model again with only the pyjama's bones
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!GuhHooks.heeft(guh, GuhHooks.PYJAMA)) {
                return;
            }
            for (GuhClothes stuk : new GuhClothes[]{GuhClothes.PYJAMA_PAKJE, GuhClothes.SLAAPMUTSJE}) {
                if (guh.getClothes(stuk.slot) != null) {
                    continue;                       // its own clothes stay on top
                }
                frame.pass(stuk.texture(), 0xFFFFFFFF, stuk::shows);
            }
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KamperenFeature.KAMPVUURVONKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Vonkje(level, x, y, z, dx, dy, dz, sprites, random));
    }

    /** A campfire spark: flies up zigzagging, glows, dims out. */
    static class Vonkje extends SingleQuadParticle {
        private final SpriteSet sprites;

        Vonkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, RandomSource random) {
            super(level, x, y, z, dx, dy, dz, sprites.get(random));
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 20 + random.nextInt(25);
            quadSize = 0.04f + random.nextFloat() * 0.03f;
            gravity = -0.03f;
            xd = (random.nextDouble() - 0.5) * 0.03;
            yd = 0.04 + random.nextDouble() * 0.04;
            zd = (random.nextDouble() - 0.5) * 0.03;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            xd += Math.sin(age * 0.6) * 0.004;
            alpha = Math.min(1f, (1f - age / (float) lifetime) * 2f);
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

    private KamperenClient() {
    }
}
