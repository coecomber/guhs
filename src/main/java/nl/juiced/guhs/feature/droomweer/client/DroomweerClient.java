package nl.juiced.guhs.feature.droomweer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.droomsfeer.DroomGeluid;
import nl.juiced.guhs.feature.droomsfeer.client.DroomLucht;
import nl.juiced.guhs.feature.weer.client.WeerClient;

/**
 * Client side of the droomweer (outside Guhdromenland; there the sky is the dimension's own):
 * <ul>
 *   <li>the dream sky ({@link DroomLucht}) laid over the normal sky, as strong as the zone at the camera
 *       ({@link WeerClient#droomSterkte}, which eases in and out at the zone's edge);</li>
 *   <li>the plein's dream music as the situational music while you stand well inside the zone.</li>
 * </ul>
 */
public final class DroomweerClient {
    /** From this strength on the dream music takes over. */
    private static final float MUZIEK_VANAF = 0.5f;

    private DroomweerClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(DroomweerClient::naDeLucht);
        NeoForge.EVENT_BUS.addListener(DroomweerClient::muziek);
    }

    private static void naDeLucht(RenderLevelStageEvent.AfterSky event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() == DromenFeature.DIM) {
            return;
        }
        float s = WeerClient.droomSterkte();
        if (s <= 0.001f) {
            return;
        }
        float a = s * s * (3 - 2 * s);
        float t = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        DroomLucht.teken(t, event.getModelViewMatrix(), a);
    }

    private static void muziek(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() == DromenFeature.DIM || WeerClient.droomSterkte() < MUZIEK_VANAF) {
            return;
        }
        event.setMusic(new Music(DroomGeluid.MUZIEK_PLEIN.holder(), 20, 400, true));
    }
}
