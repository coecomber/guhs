package nl.juiced.guhs.feature.knus;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The parts of a guh day (2.8), from the day time (dayTime % 24000):
 * OCHTEND [23000, 24000) + [0, 1500) · DAG [1500, 6000) + [8000, 11500) · DUTJE [6000, 8000) · AVOND [11500, 13500) ·
 * NACHT [13500, 23000). The Guhmensie shares the overworld's clock, so this works in every dimension (and on the client).
 */
public enum Dagdeel {
    OCHTEND, DAG, DUTJE, AVOND, NACHT;

    public static Dagdeel van(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        if (t >= 23000 || t < 1500) {
            return OCHTEND;
        }
        if (t < 6000) {
            return DAG;
        }
        if (t < 8000) {
            return DUTJE;
        }
        if (t < 11500) {
            return DAG;
        }
        if (t < 13500) {
            return AVOND;
        }
        return NACHT;
    }

    public static Dagdeel huidig(Level level) {
        return van(level.getDayTime());
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** lang gui.guhs.dagdeel.&lt;id&gt;. */
    public Component naam() {
        return Component.translatable("gui.guhs.dagdeel." + id());
    }
}
