package nl.juiced.guhs.feature.vadsrijk.zone2.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.world.GuhTime;

/**
 * Zone 2 "De Splinterpieken", slice "wereld", client: the zone-2 sky ({@link Z2Lucht}), a pale icy fog colour by day, and
 * ambient sounds/particles per area ({@link Z2Sfeer}). Called from zone 1's {@code WereldClient.init} (one marked line).
 */
public final class Z2WereldClient {
    private Z2WereldClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && Vadsrijk.in(mc.level)) {
                Z2Lucht.teken(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> Z2Sfeer.tick());
        NeoForge.EVENT_BUS.addListener(Z2WereldClient::mistKleur);
    }

    /** The fog (and so the far land) turns pale icy blue by day, deep blue-violet at night. */
    private static void mistKleur(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Vadsrijk.in(mc.level)) {
            return;
        }
        float s = Z2Lucht.sterkte(mc.player.getX(), mc.player.getZ());
        if (s <= 0.01f) {
            return;
        }
        float nacht = Z2Lucht.nachtFactor(GuhTime.timeOfDay(mc.level));
        float m = 0.65f * s;
        e.setRed(Mth.lerp(m, e.getRed(), Mth.lerp(nacht, 0.84f, 0.12f)));
        e.setGreen(Mth.lerp(m, e.getGreen(), Mth.lerp(nacht, 0.91f, 0.14f)));
        e.setBlue(Mth.lerp(m, e.getBlue(), Mth.lerp(nacht, 0.99f, 0.26f)));
    }
}
