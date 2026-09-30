package nl.juiced.guhs.feature.mewtwo.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.mewtwo.MewEntity;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoPayloads;
import nl.juiced.guhs.feature.mewtwo.MewtwoStand;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * The kloon-eiland on the client: Mieuwguh's renderer, the kloontank's renderer (your own tank: {@link KloontankRenderer}),
 * Professor Knabbelkloon's model (a crooked little pair of glasses that he keeps pushing straight), the Guhtwo's look
 * ({@link MewtwoUiterlijk}), the particles (purple sparkles, "x2", pink bubbles), your questline state ({@link MewtwoStand}).
 */
public final class MewtwoClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(MewtwoFeature.MEW.get(), context -> {
                return new GeoEntityRenderer<MewEntity, LivingEntityRenderState>(context, new DefaultedEntityGeoModel<MewEntity>(Guhs.id("mew"))) {
                    @Override
                    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");   // was DefaultedEntityGeoModel(id, true)
                    }
                };
            });
            event.registerBlockEntityRenderer(MewtwoFeature.KLOONTANK_BE.get(), KloontankRenderer::new);
        });
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(MewtwoFeature.GLOED.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Deeltje(level, x, y, z, dx, dy, dz, sprites, sprites.get(random), 0));
            event.registerSpriteSet(MewtwoFeature.X2.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Deeltje(level, x, y, z, dx, dy, dz, sprites, sprites.get(random), 1));
            event.registerSpriteSet(MewtwoFeature.BUBBEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Deeltje(level, x, y, z, dx, dy, dz, sprites, sprites.get(random), 2));
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.KNABBELKLOON, Guhs.id("entity/guh_npc_knabbelkloon"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.KNABBELKLOON, (npc, tick) -> {
            // his glasses slide askew, and now and then he pushes them straight (for a moment)
            float t = (float) tick;
            float duw = (t % 200) < 12 ? (t % 200) / 12f : 1f;
            return bones -> bones.ifPresent("kloon_bril", b -> {
                b.setRotZ(0.10f * duw + Mth.sin(t * 0.03f) * 0.015f);
                b.setTranslateY(-0.3f * duw);
            });
        });
        VariantUiterlijk.zet(GuhVariant.MEWTWO, new MewtwoUiterlijk());
        MewtwoPayloads.standOntvanger = s -> MewtwoStand.zet(s.stap(), s.notities(), s.onderdelen(), s.ingebouwd());
        MewtwoPayloads.x2Ontvanger = x -> MewtwoUiterlijk.hap(x.guh());
    }

    /** The kloon-eiland's particles: 0 a purple sparkle (twinkles, drifts), 1 the "x2" (floats up, pops in), 2 a pink bubble (rises). */
    static class Deeltje extends SingleQuadParticle {
        private final int soort;
        private final SpriteSet sprites;

        Deeltje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, TextureAtlasSprite sprite, int soort) {
            super(level, x, y, z, sprite);
            this.soort = soort;
            this.sprites = sprites;
            hasPhysics = false;
            xd = dx;
            yd = dy;
            zd = dz;
            switch (soort) {
                case 1 -> {
                    lifetime = 30;
                    quadSize = 0.05f;
                    gravity = 0f;
                    yd = 0.03;
                }
                case 2 -> {
                    lifetime = 20 + random.nextInt(20);
                    quadSize = 0.05f + random.nextFloat() * 0.05f;
                    gravity = -0.02f;
                }
                default -> {
                    lifetime = 14 + random.nextInt(16);
                    quadSize = 0.06f + random.nextFloat() * 0.06f;
                    gravity = -0.005f;
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (soort == 1) {
                quadSize = Math.min(0.32f, quadSize + 0.05f);
                yd *= 0.94;
            } else if (soort == 0) {
                setSprite(sprites.get(age % 8 < 4 ? 0 : 1, 2));
                xd *= 0.9;
                zd *= 0.9;
            } else {
                xd += (random.nextDouble() - 0.5) * 0.004;
            }
            alpha = Math.min(1f, (lifetime - age) / 8f);
        }

        @Override
        public int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private MewtwoClient() {
    }
}
