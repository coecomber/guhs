package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The mounts of zone 1. Speed (blocks per tick factor) and jump grow with the mount's level (1..{@link #MAX_LEVEL}, xp from riding).
 * <ul>
 *   <li>{@link #HOEDHUPPER}: the first mount, a hopping mushroom-pony (quest at lv ~8: {@code Rijdieren.geefEerste}).</li>
 *   <li>{@link #KLODDERROS}: a rare wobbly slime horse with a little crown, the saddle {@code guhs:vr_item_zadel_klodderros} is
 *       loot of Koning Klodder (bazen slice).</li>
 * </ul>
 */
public enum RijdierSoort {
    HOEDHUPPER(0.26, 0.62),
    KLODDERROS(0.30, 0.80);

    public static final int MAX_LEVEL = 10;

    public final double snelheid, sprong;

    RijdierSoort(double snelheid, double sprong) {
        this.snelheid = snelheid;
        this.sprong = sprong;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String naamKey() {
        return "vadsrijk.economie.rijdier." + id();
    }

    /** Movement speed at this level (+3 % per level). */
    public double snelheid(int level) {
        return snelheid * (1 + 0.03 * (Math.max(1, level) - 1));
    }

    /** Jump strength at this level (+0.025 per level). */
    public double sprong(int level) {
        return sprong + 0.025 * (Math.max(1, level) - 1);
    }

    /** Xp (blocks ridden) from this level to the next. */
    public static long xpNodig(int level) {
        return level >= MAX_LEVEL ? 0 : 150L * level;
    }

    @Nullable
    public static RijdierSoort van(String id) {
        for (RijdierSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
