package nl.juiced.guhs.feature.vadsrijk.zone4.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken;

/**
 * Zone 4 "De Lampionkust", slice "t-z4wereld", client: the Grauwe Mistbank. Around the Mysterie-eiland (south-east, towards the
 * empire) the fog turns grey and closes in ({@link Z4WereldPlekken#mist}: 0 = clear .. 1 = the core, ~14 blocks of sight),
 * with drifting grey wisps. Smoothed per tick so sailing in and out fades. Called from zone 1's {@code WereldClient.init}
 * (one marked line).
 */
public final class Z4WereldClient {
    /** Smoothed mist strength 0..1 at the camera. */
    private static float mist;

    private Z4WereldClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(Z4WereldClient::kleur);
        NeoForge.EVENT_BUS.addListener(Z4WereldClient::afstand);
    }

    public static float mist() {
        return mist;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Vadsrijk.in(mc.level)) {
            mist = 0;
            return;
        }
        float doel = (float) Z4WereldPlekken.mist(mc.player.getX(), mc.player.getZ());
        mist += (doel - mist) * 0.05f;
        if (mist > 0.2f && mc.level.getRandom().nextFloat() < mist * 0.8f) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2, r = 4 + mc.level.getRandom().nextDouble() * 14;
            mc.level.addParticle(ParticleTypes.CLOUD, mc.player.getX() + Math.cos(a) * r, mc.player.getY() + mc.level.getRandom().nextDouble() * 4 - 1,
                    mc.player.getZ() + Math.sin(a) * r, 0.02, 0, 0.01);
        }
    }

    static void kleur(ViewportEvent.ComputeFogColor e) {
        if (mist <= 0.01f) {
            return;
        }
        float m = Math.min(1f, mist) * 0.85f;
        e.setRed(Mth.lerp(m, e.getRed(), 0.62f));
        e.setGreen(Mth.lerp(m, e.getGreen(), 0.63f));
        e.setBlue(Mth.lerp(m, e.getBlue(), 0.66f));
    }

    static void afstand(ViewportEvent.RenderFog e) {
        if (mist <= 0.01f || e.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float ver = Mth.lerp(Math.min(1f, mist), e.getFarPlaneDistance(), 14f);
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), ver));
        e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), ver * 0.1f));
    }
}
