package nl.juiced.guhs.feature.boerderij.client;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the Guhboerderij: the GeckoLib renderers of the three animals (babies smaller; a shorn guhschaapje shows
 * its bare, pink body until its wool is back the next day), the particles (wolplukje, melkdruppel) and Boerin Hooibaal's
 * own model (a straw hat, a straw in her mouth that wiggles, an apron; tools/features/boerderij_dieren.py).
 */
public final class BoerderijClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BoerderijClient::renderers);
        modBus.addListener(BoerderijClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BOERINNEGUH, Guhs.id("entity/guh_npc_boerinneguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BOERINNEGUH, (npc, tick) -> {
            float t = (float) tick * 0.08f;
            return bones -> bones.ifPresent("boerin_strootje", b -> b.setRotY((float) Math.sin(t) * 0.25f));
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BoerderijFeature.GUHSCHAAPJE.get(), context -> new DierRenderer<>(context, "guhschaapje", 0.45f));
        event.registerEntityRenderer(BoerderijFeature.KNABBELKIPPETJE.get(), context -> new DierRenderer<>(context, "knabbelkippetje", 0.3f));
        event.registerEntityRenderer(BoerderijFeature.GUHKOE.get(), context -> new DierRenderer<>(context, "guhkoe", 0.55f));
    }

    /** A farm animal: its own model, a turning head, babies at 60 %, and the wool that goes and comes back (bones wol / kaal). */
    public static class DierRenderer<T extends BoerderijDier> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        /** 1.1.0 (GeckoLib 5): "shorn" is copied into the render state at extract time. */
        static final DataTicket<Boolean> KAAL = DataTicket.create("guhs_boerderij_kaal", Boolean.class);

        public DierRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam)));
            this.shadowRadius = schaduw;
        }

        @Override
        public void addRenderData(T dier, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(KAAL, dier.productGegeven());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            boolean kaal = info.getOrDefaultGeckolibData(KAAL, false);
            if (bones.get("wol").isPresent() && bones.get("kaal").isPresent()) {
                bones.ifPresent("wol", b -> b.skipRender(kaal).skipChildrenRender(kaal));
                bones.ifPresent("kaal", b -> b.skipRender(!kaal).skipChildrenRender(!kaal));
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.renderState().isBaby ? 0.6f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }

        /** 1.21.1 halved the shadow of baby mobs in the entity render dispatcher. */
        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            return state.isBaby ? this.shadowRadius * 0.5f : this.shadowRadius;
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BoerderijFeature.WOLPLUKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Wolplukje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(BoerderijFeature.MELKDRUPPEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Melkdruppel(level, x, y, z, sprites));
    }

    /** A soft tuft of pluiswol: pops out, then drifts down slowly, swaying. */
    static class Wolplukje extends SingleQuadParticle {
        Wolplukje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A drop of kaasmelk: falls and is gone. */
    static class Melkdruppel extends SingleQuadParticle {
        Melkdruppel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            lifetime = 16 + random.nextInt(10);
            quadSize = 0.06f + random.nextFloat() * 0.03f;
            gravity = 0.7f;
            xd = (random.nextDouble() - 0.5) * 0.05;
            yd = 0.08;
            zd = (random.nextDouble() - 0.5) * 0.05;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.OPAQUE;
        }
    }

    private BoerderijClient() {
    }
}
