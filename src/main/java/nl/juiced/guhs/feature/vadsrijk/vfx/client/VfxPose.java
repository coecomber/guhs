package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Poses for boss renderers WITHOUT GeckoLib (the Grauwe Klodderreus uses the slime renderer): the server says "windup for 30
 * ticks", "treffer", "geraakt", "fase", "juich"; a renderer asks {@link #squish} (slime-style: negative = flat and wide, positive =
 * tall and thin) and adds it. GeckoLib bosses get the same moments as real animations (BaasAnims), this is only the fallback.
 */
public final class VfxPose {
    private record Pose(String soort, long start, int ticks) {
    }

    private static final Map<Integer, Pose> POSES = new HashMap<>();

    private VfxPose() {
    }

    static void zet(int id, String soort, int ticks, long klok) {
        POSES.put(id, new Pose(soort, klok, Math.max(2, ticks)));
    }

    static void tick(long klok) {
        POSES.values().removeIf(p -> klok - p.start() > p.ticks() + 12);
    }

    static void wis() {
        POSES.clear();
    }

    /** The extra squish for this entity now (0 = none). */
    public static float squish(int entityId, float pt) {
        Pose p = POSES.get(entityId);
        if (p == null) {
            return 0;
        }
        float t = VfxClient.klok() - p.start() + pt;
        float f = Math.min(1f, t / p.ticks());
        return switch (p.soort()) {
            // crouch down over the wind-up, trembling more and more, held until the hit
            case "windup" -> -0.55f * (float) Math.sqrt(f) + (f > 0.5f ? 0.06f * (float) Math.sin(t * 2.6f) * f : 0f);
            // the hit: stretch up, settle
            case "treffer" -> 0.6f * (1 - f) * (float) Math.cos(f * 3.0f);
            case "geraakt" -> 0.22f * (1 - f) * (float) Math.sin(t * 1.7f);
            case "fase", "woede" -> 0.45f * (1 - f) * (float) Math.sin(t * 1.1f);
            case "juich" -> 0.3f * (float) Math.abs(Math.sin(t * 0.45f)) * (1 - f * 0.5f);
            default -> 0;
        };
    }

    /** Is this entity winding up right now (renderers may add a glow)? */
    public static boolean windup(int entityId) {
        Pose p = POSES.get(entityId);
        return p != null && p.soort().equals("windup") && VfxClient.klok() - p.start() <= p.ticks();
    }
}
