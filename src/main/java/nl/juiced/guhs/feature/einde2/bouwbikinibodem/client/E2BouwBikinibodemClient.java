package nl.juiced.guhs.feature.einde2.bouwbikinibodem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.Leegtemist;

/**
 * einde2, slice bouw_bikinibodem, client side: the leegtemist under the dome of Bikinibodem. While the server says the
 * player is inside ({@link Leegtemist#inMist}) the fog turns purple and comes a little nearer (it eases in and out), and
 * slow purple motes drift around the player: the "sea water" of the town. The blocks name their render type in their models.
 */
public final class E2BouwBikinibodemClient {
    /** The mist (blocks): clear up to NABIJ, fully purple at VER. */
    public static final float VER = 120f, NABIJ = 28f;
    private static final float ROOD = 0.36f, GROEN = 0.17f, BLAUW = 0.55f;
    private static float mist;
    private static float mistO;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(E2BouwBikinibodemClient::onTick);
        NeoForge.EVENT_BUS.addListener(E2BouwBikinibodemClient::onFog);
        NeoForge.EVENT_BUS.addListener(E2BouwBikinibodemClient::onFogColour);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
            Leegtemist.wisClient();
            mist = mistO = 0f;
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mistO = mist;
        boolean in = mc.level != null && mc.player != null && Leegtemist.inMist(mc.player);
        mist = Mth.approach(mist, in ? 1f : 0f, 0.02f);
        if (!in || mc.isPaused()) {
            return;
        }
        // slow motes around the player
        RandomSource random = mc.level.getRandom();
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(ParticleTypes.REVERSE_PORTAL, mc.player.getX() + (random.nextDouble() - 0.5) * 24,
                    mc.player.getY() + 1.5 + (random.nextDouble() - 0.4) * 12, mc.player.getZ() + (random.nextDouble() - 0.5) * 24,
                    (random.nextDouble() - 0.5) * 0.01, 0.004, (random.nextDouble() - 0.5) * 0.01);
        }
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        float m = Mth.lerp((float) event.getPartialTick(), mistO, mist);
        if (m <= 0.001f || !(event.getEnvironment() instanceof net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment)
                || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float newFar = Mth.lerp(m, far, Math.min(far, VER));
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), Math.min(NABIJ, newFar * 0.4f)));
    }

    private static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float m = mist;
        if (m <= 0.001f) {
            return;
        }
        event.setRed(Mth.lerp(m, event.getRed(), ROOD));
        event.setGreen(Mth.lerp(m, event.getGreen(), GROEN));
        event.setBlue(Mth.lerp(m, event.getBlue(), BLAUW));
    }

    private E2BouwBikinibodemClient() {
    }
}
