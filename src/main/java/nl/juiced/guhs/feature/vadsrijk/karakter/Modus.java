package nl.juiced.guhs.feature.vadsrijk.karakter;

import net.minecraft.ChatFormatting;

/**
 * T-KARAKTER: the game mode of a character (decision R1; no IJzerguh). Chosen at creation, only ever lowered (Eénvads -> Normaal ->
 * Knuffelmodus), never raised.
 * <ul>
 *   <li>{@link #EENVADS}: one life. Fainting outside a town = the knight retires to the Rustende Ridders-zaal and plays on as
 *   Normaal with a badge. While alive: a gold heart by the name and +10 % xp.</li>
 *   <li>{@link #NORMAAL}: the normal game.</li>
 *   <li>{@link #KNUFFEL}: relaxed story mode: monsters do half damage to you, and you get 10 % less xp.</li>
 * </ul>
 */
public enum Modus {
    KNUFFEL("knuffel", 0, "☁", ChatFormatting.LIGHT_PURPLE, 0.5, 0.9),
    NORMAAL("normaal", 1, "", ChatFormatting.WHITE, 1.0, 1.0),
    EENVADS("eenvads", 2, "❤", ChatFormatting.GOLD, 1.0, 1.1);

    public final String id;
    /** Higher = harder. You may only go to a lower one. */
    public final int zwaarte;
    /** The badge by the name ("" = none). */
    public final String badge;
    public final ChatFormatting kleur;
    /** Factor on the damage monsters do to you. */
    public final double schadeFactor;
    /** Factor on all xp you get. */
    public final double xpFactor;

    Modus(String id, int zwaarte, String badge, ChatFormatting kleur, double schadeFactor, double xpFactor) {
        this.id = id;
        this.zwaarte = zwaarte;
        this.badge = badge;
        this.kleur = kleur;
        this.schadeFactor = schadeFactor;
        this.xpFactor = xpFactor;
    }

    public String naamKey() {
        return "vadsrijk.karakter.modus." + id;
    }

    public String uitlegKey() {
        return "vadsrijk.karakter.modus." + id + ".uitleg";
    }

    /** May a character in this mode switch to {@code naar}? (only lower) */
    public boolean magNaar(Modus naar) {
        return naar.zwaarte < zwaarte;
    }

    public static Modus van(String id) {
        for (Modus m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return NORMAAL;
    }

    /** The badge of a retired Eénvads knight (Rustende Ridder). */
    public static final String PENSIOEN_BADGE = "✦";
}
