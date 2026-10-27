package nl.juiced.guhs.feature.weer;

import javax.annotation.Nullable;

/**
 * 1.8.0: the weathers of the Weer vadser ({@link WeerVadserBlock}). {@link #NORMAAL} is "off" (the world's own weather);
 * zon, regen and onweer are always there; the eight guh-weertjes are unlocked per player with a weerpotje
 * ({@link WeerPotjeItem}, {@link WeerUnlocks}). {@code regent}: it rains in the zone (puddles: {@link WeerZones#regentOp}).
 */
public enum Weertje {
    NORMAAL("normaal", true, false, 0xFFB8B8C8),
    ZON("zon", true, false, 0xFFFFD866),
    REGEN("regen", true, true, 0xFF7FA8E0),
    ONWEER("onweer", true, true, 0xFF6A6F96),
    PLUISJES("pluisjes", false, false, 0xFFFFC2DE),
    BELLEN("bellen", false, false, 0xFFA8E6FF),
    KNABBEL("knabbel", false, false, 0xFFF2C24A),
    REGENBOOG("regenboog", false, true, 0xFFB98CFF),
    HARTJES("hartjes", false, false, 0xFFFF7AA8),
    STERREN("sterren", false, false, 0xFF4E4A9A),
    KNUSMIST("knusmist", false, false, 0xFFE8C6DA),
    BLOESEM("bloesem", false, false, 0xFFFFB0C8);

    private final String id;
    /** Always available (no weerpotje needed). */
    public final boolean altijd;
    /** It rains in the zone (rain or snow, by the biome's temperature; the rainbow weather rains softly). */
    public final boolean regent;
    /** The colour of its button. */
    public final int kleur;

    Weertje(String id, boolean altijd, boolean regent, int kleur) {
        this.id = id;
        this.altijd = altijd;
        this.regent = regent;
        this.kleur = kleur;
    }

    public String id() {
        return id;
    }

    /** "gui.guhs.weer.weertje.&lt;id&gt;" */
    public String key() {
        return "gui.guhs.weer.weertje." + id;
    }

    /** Does this weather need a weerpotje? */
    public boolean ontgrendelbaar() {
        return !altijd;
    }

    @Nullable
    public static Weertje byId(String id) {
        for (Weertje w : values()) {
            if (w.id.equals(id)) {
                return w;
            }
        }
        return null;
    }

    public static Weertje byIndex(int i) {
        Weertje[] v = values();
        return i >= 0 && i < v.length ? v[i] : NORMAAL;
    }
}
