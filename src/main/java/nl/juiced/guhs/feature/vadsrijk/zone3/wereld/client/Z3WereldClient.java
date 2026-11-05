package nl.juiced.guhs.feature.vadsrijk.zone3.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.world.GuhTime;

/**
 * Zone 3 "De Schemerwortel", slice "wereld", client: the zone-3 sky ({@link Z3Lucht}: honey-gold veil, Hoogvads and the far
 * east), a fog colour per LAYER (honey gold in the crown, amber in the hollow trunk, dusk under the crown, near black with a
 * violet glow in the roots) and the ambient ({@link Z3Sfeer}). Called from zone 1's {@code WereldClient.init} (one marked line).
 */
public final class Z3WereldClient {
    private Z3WereldClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && Vadsrijk.in(mc.level)) {
                Z3Lucht.teken(e);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> Z3Sfeer.tick());
        NeoForge.EVENT_BUS.addListener(Z3WereldClient::mistKleur);
    }

    /** The colour of the layer at this point: {r, g, b} by day and at night. */
    static float[][] laagKleur(double x, double y, double z) {
        double r = Math.hypot(x - Z3WereldPlekken.STAM_X, z - Z3WereldPlekken.STAM_Z);
        // [p-wereld] revamp layers: roots 4-78, trunk 80-160 (+ the hollow up to the nest), branches 160-216, crown 216-312
        if (y < 62 && r < 200) {
            return new float[][] {{0.12f, 0.07f, 0.17f}, {0.06f, 0.03f, 0.1f}};          // Wortelduister
        }
        if (r < 25 && y >= 56 && y < 294) {
            return new float[][] {{0.62f, 0.42f, 0.24f}, {0.22f, 0.14f, 0.1f}};          // the hollow trunk (amber)
        }
        if (y >= 216) {
            return new float[][] {{1.0f, 0.84f, 0.56f}, {0.3f, 0.2f, 0.36f}};            // the crown (honey gold)
        }
        if (r < 140 && y < 150) {
            return new float[][] {{0.5f, 0.46f, 0.52f}, {0.14f, 0.12f, 0.2f}};           // dusk under the crown
        }
        return new float[][] {{0.98f, 0.82f, 0.74f}, {0.2f, 0.16f, 0.3f}};              // sunny edge / branches
    }

    private static void mistKleur(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Vadsrijk.in(mc.level)) {
            return;
        }
        float s = Z3Lucht.sterkte(mc.player.getX(), mc.player.getZ());
        if (s <= 0.01f) {
            return;
        }
        float nacht = Z3Lucht.nachtFactor(GuhTime.timeOfDay(mc.level));
        float[][] k = laagKleur(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        float m = 0.6f * s;
        e.setRed(Mth.lerp(m, e.getRed(), Mth.lerp(nacht, k[0][0], k[1][0])));
        e.setGreen(Mth.lerp(m, e.getGreen(), Mth.lerp(nacht, k[0][1], k[1][1])));
        e.setBlue(Mth.lerp(m, e.getBlue(), Mth.lerp(nacht, k[0][2], k[1][2])));
    }
}
