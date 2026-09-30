package nl.juiced.guhs.feature.theehuis.client;

import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knus.client.GuhRenderHooks;
import nl.juiced.guhs.feature.theehuis.TheeBlocks;
import nl.juiced.guhs.feature.theehuis.TheehuisFeature;
import nl.juiced.guhs.feature.theehuis.TheehuisPayloads;
import nl.juiced.guhs.feature.theehuis.Theekransje;

/**
 * Client side of the Knabbelthee-huisje: Mevrouw Theelepel's own model (a flowery hat and a big teaspoon), her screen,
 * the steam and heart particles, and the wish bubbles above the guests of a theekransje (a slowly turning cup of tea or
 * a cake above the guh's head, drawn by a GuhRenderHooks layer).
 */
public final class TheehuisClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(TheehuisClient::particles);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> TheehuisPayloads.clientVergeet());
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.THEEGUH, Guhs.id("geo/entity/guh_npc_theeguh.geo.json"));
        // she stirs her tea: the spoon rocks a little
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.THEEGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.12f;
            bot.apply("theeguh_lepel").ifPresent(b -> b.setRotZ((float) Math.sin(t) * 0.25f));   // (not animated: absolute)
        });
        GuhRenderHooks.laag((renderer, pose, guh, model, buffers, partialTick, light, overlay) -> {
            Integer wens = TheehuisPayloads.CLIENT_WENSEN.get(guh.getId());
            if (wens == null || wens == Theekransje.Wens.GEEN.ordinal()) {
                return;
            }
            ItemStack icon = wens == Theekransje.Wens.THEE.ordinal() ? new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.KNABBELTHEE)) : new ItemStack(Items.CAKE);
            float t = guh.tickCount + partialTick;
            pose.translate(0, 1.55 + Math.sin(t * 0.12) * 0.05, 0);
            pose.mulPose(Axis.YP.rotationDegrees(t * 3f));
            pose.scale(0.9f, 0.9f, 0.9f);
            Minecraft.getInstance().getItemRenderer().renderStatic(icon, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers,
                    guh.level(), guh.getId());
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TheehuisFeature.THEESTOOM.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Stoom(level, x, y, z, sprites));
        event.registerSpriteSet(TheehuisFeature.GEZELLIG_HARTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Hartje(level, x, y, z, sprites));
    }

    /** guhs:theehuis_open: Mevrouw Theelepel's screen. */
    public static void open(TheehuisPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new TheehuisScreen(payload));
    }

    /** A wisp of steam: curls up, grows and fades. */
    static class Stoom extends TextureSheetParticle {
        Stoom(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(20);
            quadSize = 0.08f + random.nextFloat() * 0.05f;
            gravity = -0.01f;
            xd = (random.nextDouble() - 0.5) * 0.01;
            yd = 0.02;
            zd = (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
            alpha = 0.7f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = 0.7f * (1f - t);
            quadSize *= 1.015f;
            xd += Math.sin(age * 0.3) * 0.002;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A little pink heart that floats up and wobbles. */
    static class Hartje extends TextureSheetParticle {
        Hartje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(15);
            quadSize = 0.1f + random.nextFloat() * 0.05f;
            gravity = -0.01f;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = 0.03;
            zd = (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = Math.min(1f, (1f - t) * 3f);
            xd += Math.sin(age * 0.4) * 0.002;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }
    }

    private TheehuisClient() {
    }
}
