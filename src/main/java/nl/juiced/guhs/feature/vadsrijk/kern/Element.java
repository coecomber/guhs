package nl.juiced.guhs.feature.vadsrijk.kern;

import net.minecraft.ChatFormatting;

/**
 * Het Vadsrijk: the elements (ronde 13). Triangle Vuur > Natuur (Zwam) > Water (Slijm) > Vuur: x1.5 on a weakness, x0.7 the
 * other way. Roze (vads energy) is neutral, but x1.5 against "ontvadst" creatures. GEEN = no element (x1).
 */
public enum Element {
    GEEN("-", ChatFormatting.GRAY, 0xAAAAAA),
    ROZE("R", ChatFormatting.LIGHT_PURPLE, 0xFF7AC8),
    VUUR("V", ChatFormatting.RED, 0xFF5A2A),
    NATUUR("N", ChatFormatting.GREEN, 0x5BD14A),
    WATER("W", ChatFormatting.AQUA, 0x3FA9FF);

    public static final double STERK = 1.5, ZWAK = 0.7, ROZE_TEGEN_ONTVADST = 1.5;

    /** The letter on nameplates and tooltips, its chat colour, its RGB. */
    public final String letter;
    public final ChatFormatting kleur;
    public final int rgb;

    Element(String letter, ChatFormatting kleur, int rgb) {
        this.letter = letter;
        this.kleur = kleur;
        this.rgb = rgb;
    }

    /** What this element beats in the triangle (null for GEEN/ROZE). */
    public Element verslaat() {
        return switch (this) {
            case VUUR -> NATUUR;
            case NATUUR -> WATER;
            case WATER -> VUUR;
            default -> null;
        };
    }

    /** The damage factor of an attack with element {@code aanval} on a target with element {@code verdediger}. */
    public static double factor(Element aanval, Element verdediger, boolean ontvadst) {
        if (aanval == null || aanval == GEEN) {
            return 1.0;
        }
        if (aanval == ROZE) {
            return ontvadst ? ROZE_TEGEN_ONTVADST : 1.0;
        }
        if (verdediger == null) {
            return 1.0;
        }
        if (aanval.verslaat() == verdediger) {
            return STERK;
        }
        if (verdediger.verslaat() == aanval) {
            return ZWAK;
        }
        return 1.0;
    }

    public String key() {
        return "vadsrijk.kern.element." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Element van(String naam) {
        try {
            return valueOf(naam.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GEEN;
        }
    }
}
