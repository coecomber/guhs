package nl.juiced.guhs.feature.vadsrijk.karakter;

import net.minecraft.ChatFormatting;

/**
 * T-KARAKTER / U-KARAKTER: the game mode of a character (decision R1; no IJzerguh). U-KARAKTER (user decision 2026-11-06): the
 * three character slots each have their OWN fixed mode, and a mode can never be changed:
 * <ul>
 *   <li>slot 0 (LEFT) = {@link #NORMAAL}: the normal game.</li>
 *   <li>slot 1 (MIDDLE) = {@link #KNUFFEL}: relaxed story mode: monsters do half damage to you, and you get 10 % less xp.</li>
 *   <li>slot 2 (RIGHT) = {@link #EENVADS}: one life. Fainting outside a town = the knight retires to the Rustende Ridders-zaal (a
 *   statue; he is not played any more) and the right slot is free for a new Eénvads knight. While alive: a gold heart by the
 *   name and +10 % xp.</li>
 * </ul>
 */
public enum Modus {
    NORMAAL("normaal", 0, "", ChatFormatting.WHITE, 1.0, 1.0),
    KNUFFEL("knuffel", 1, "☁", ChatFormatting.LIGHT_PURPLE, 0.5, 0.9),
    EENVADS("eenvads", 2, "❤", ChatFormatting.GOLD, 1.0, 1.1);

    public final String id;
    /** The character slot this mode belongs to (0 left, 1 middle, 2 right). */
    public final int slot;
    /** The badge by the name ("" = none). */
    public final String badge;
    public final ChatFormatting kleur;
    /** Factor on the damage monsters do to you. */
    public final double schadeFactor;
    /** Factor on all xp you get. */
    public final double xpFactor;

    Modus(String id, int slot, String badge, ChatFormatting kleur, double schadeFactor, double xpFactor) {
        this.id = id;
        this.slot = slot;
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

    public String kortKey() {
        return "vadsrijk.karakter.modus." + id + ".kort";
    }

    /** The fixed mode of a slot (0 Normaal, 1 Knuffelmodus, 2 Eénvads). */
    public static Modus vanSlot(int slot) {
        for (Modus m : values()) {
            if (m.slot == slot) {
                return m;
            }
        }
        return NORMAAL;
    }

    public static Modus van(String id) {
        for (Modus m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return NORMAAL;
    }

    /** "normaal" / "knuffel" / "eenvads" or a slot number "0".."2" -> the slot, else -1 (for the dev commands). */
    public static int slotVan(String s) {
        for (Modus m : values()) {
            if (m.id.equalsIgnoreCase(s)) {
                return m.slot;
            }
        }
        try {
            int i = Integer.parseInt(s.trim());
            return i >= 0 && i < Karakter.SLOTS ? i : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The badge of a retired Eénvads knight (Rustende Ridder). */
    public static final String PENSIOEN_BADGE = "✦";
}
