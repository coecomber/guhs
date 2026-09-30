package nl.juiced.guhs.feature.spelen;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * The difficulty levels of the minigames (2.9, De Grote Guhspelen): makkelijk, medium, lastig. The game as it was before
 * 2.9 is MEDIUM, so its board keeps its old id ({@link #board}); lastig gives half as many coins more ({@link #munten}).
 * Lang: gui.guhs.niveau.&lt;id&gt; (Makkelijk / Medium / Lastig).
 */
public enum Niveau {
    MAKKELIJK, MEDIUM, LASTIG;

    /** "makkelijk", "medium", "lastig". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Makkelijk / Medium / Lastig (gui.guhs.niveau.&lt;id&gt;). */
    public Component naam() {
        return Component.translatable("gui.guhs.niveau." + id());
    }

    /** The Scorebord board of this level: MEDIUM keeps the base id (the existing boards), the others get _makkelijk / _lastig. */
    public String board(String base) {
        return this == MEDIUM ? base : base + "_" + id();
    }

    /** The coins for a game on this level: lastig gives 50 % more (rounded up), the others the base amount. */
    public int munten(int basis) {
        return this == LASTIG ? (int) Math.ceil(basis * 1.5) : basis;
    }

    /** The level with this ordinal (clamped: below 0 is MAKKELIJK, above 2 is LASTIG). */
    public static Niveau of(int ordinal) {
        return values()[Math.max(0, Math.min(values().length - 1, ordinal))];
    }

    /** The level with this id (MEDIUM when unknown). */
    public static Niveau byId(String id) {
        for (Niveau n : values()) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return MEDIUM;
    }
}
