package nl.juiced.guhs.feature.kamperen.client;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.client.GuhRenderHooks;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Client side of the kampeerplekjes: the pyjamas (a guh with the PYJAMA flag is drawn in the pyjama_pakje and the
 * slaapmutsje, over whatever isn't covered by its own clothes), the campfire sparks, and Opa Guh's own model
 * (moustache, round glasses, nightcap with a pompom, a walking stick).
 */
public final class KamperenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(KamperenClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.OPA_GUH, Guhs.id("geo/entity/guh_npc_opa_guh.geo.json"));
        // the pompom of his nightcap swings a little as he talks, and his moustache wiggles
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.OPA_GUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.07f;
            bot.apply("opa_mutspunt").ifPresent(b -> b.setRotZ(0.35f + (float) Math.sin(t) * 0.12f));
            bot.apply("opa_snor").ifPresent(b -> b.setRotZ((float) Math.sin(t * 2.3f) * 0.05f));
        });
        GuhRenderHooks.laag((renderer, pose, guh, model, buffers, partialTick, light, overlay) -> {
            if (!GuhHooks.heeft(guh, GuhHooks.PYJAMA)) {
                return;
            }
            Map<GeoBone, boolean[]> was = new IdentityHashMap<>();
            for (GeoBone bone : model.topLevelBones()) {
                onthoud(bone, was);
            }
            try {
                for (GuhClothes stuk : new GuhClothes[]{GuhClothes.PYJAMA_PAKJE, GuhClothes.SLAAPMUTSJE}) {
                    if (guh.getClothes(stuk.slot) != null) {
                        continue;                       // its own clothes stay on top
                    }
                    for (GeoBone bone : model.topLevelBones()) {
                        alleen(bone, stuk);
                    }
                    RenderType type = RenderType.entityCutoutNoCull(stuk.texture());
                    renderer.reRender(model, pose, buffers, guh, type, buffers.getBuffer(type), partialTick, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                }
            } finally {
                for (Map.Entry<GeoBone, boolean[]> e : was.entrySet()) {
                    e.getKey().setHidden(e.getValue()[0]);
                    e.getKey().setChildrenHidden(e.getValue()[1]);
                }
            }
        });
    }

    private static void onthoud(GeoBone bone, Map<GeoBone, boolean[]> was) {
        was.put(bone, new boolean[]{bone.isHidden(), bone.isHidingChildren()});
        for (GeoBone child : bone.getChildBones()) {
            onthoud(child, was);
        }
    }

    private static void alleen(GeoBone bone, GuhClothes stuk) {
        bone.setHidden(!stuk.shows(bone.getName()));
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            alleen(child, stuk);
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KamperenFeature.KAMPVUURVONKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Vonkje(level, x, y, z, dx, dy, dz, sprites));
    }

    /** A campfire spark: flies up zigzagging, glows, dims out. */
    static class Vonkje extends TextureSheetParticle {
        private final SpriteSet sprites;

        Vonkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
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
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private KamperenClient() {
    }
}
