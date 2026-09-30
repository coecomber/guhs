package nl.juiced.guhs.feature.vadswoud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * Client side of the Vadswoud: its particles (glowing fluff, falling mint leaves, the Zzz of sleeping guhs) and the
 * mist: in the Vadswoud the fog closes in (smoothly, over a few seconds), so the giant trees fade into mint haze.
 */
public final class VadswoudClient {
    /**
     * The mist (blocks): clear up to MIST_NEAR, fully white-mint at MIST_FAR. A light haze: you still see ~60-80 blocks
     * (also from high up in a reuzenguhboom or flying), only the far-off trees fade away.
     */
    public static final float MIST_FAR = 112f, MIST_NEAR = 36f;
    /** 0 = clear, 1 = the full Vadswoud mist (follows the camera's biome). */
    private static float mist;
    private static float mistO;

    public static void init(IEventBus modBus) {
        modBus.addListener(VadswoudClient::particles);
        NeoForge.EVENT_BUS.addListener(VadswoudClient::onTick);
        NeoForge.EVENT_BUS.addListener(VadswoudClient::onFog);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VadswoudFeature.VADSPLUISJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Pluisje(level, x, y, z, sprites));
        event.registerSpriteSet(VadswoudFeature.VADSBLAADJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Blaadje(level, x, y, z, sprites));
        event.registerSpriteSet(VadswoudFeature.GUH_ZZZ.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Zzz(level, x, y, z, sprites));
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mistO = mist;
        boolean inWood = mc.level != null && mc.gameRenderer.getMainCamera().isInitialized()
                && mc.level.getBiome(mc.gameRenderer.getMainCamera().getBlockPosition()).is(VadswoudFeature.VADSWOUD);
        mist = Mth.approach(mist, inWood ? 1f : 0f, 0.012f);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        float m = Mth.lerp((float) event.getPartialTick(), mistO, mist);
        if (m <= 0.001f || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float newFar = Mth.lerp(m, far, Math.min(far, MIST_FAR));
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), Math.min(MIST_NEAR, newFar * 0.4f)));
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.CYLINDER);   // only the horizontal distance counts: looking down from high up stays clear
        event.setCanceled(true);
    }

    /** Glowing fluff: drifts slowly up and sideways, fades in and out, shines in the dark. */
    static class Pluisje extends TextureSheetParticle {
        Pluisje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 100 + random.nextInt(100);
            quadSize = 0.04f + random.nextFloat() * 0.05f;
            gravity = -0.002f;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = random.nextDouble() * 0.01;
            zd = (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
            alpha = 0f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = Math.min(1f, Math.min(t * 5f, (1f - t) * 4f)) * 0.9f;
            xd += Math.sin(age * 0.07) * 0.0006;
            zd += Math.cos(age * 0.05) * 0.0006;
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

    /** A little mint leaf, falling and spinning like a cherry petal. */
    static class Blaadje extends CherryParticle {
        Blaadje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites);
            quadSize *= 1.2f;
        }
    }

    /** The Zzz of a sleeping guh: floats up and fades away. */
    static class Zzz extends TextureSheetParticle {
        Zzz(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 40;
            quadSize = 0.14f;
            gravity = 0f;
            xd = (random.nextDouble() - 0.5) * 0.01;
            yd = 0.022;
            zd = (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = 1f - age / (float) lifetime;
            quadSize = 0.14f + age * 0.003f;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private VadswoudClient() {
    }
}
