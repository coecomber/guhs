package nl.juiced.guhs.feature.snuffel;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/** The four kinds of scent, each with the colour the scent meter shows for it. */
public enum GeurSoort {
    /** Something to eat: orange. */
    ETEN(0xFFF2A23C),
    /** A thing somebody lost: blue. */
    VOORWERP(0xFF58B4F0),
    /** An animal (or a dog): green. */
    DIER(0xFF7CCB5A),
    /** Something strange: purple. */
    VREEMD(0xFFB57BEA);

    private final int kleur;

    GeurSoort(int kleur) {
        this.kleur = kleur;
    }

    /** ARGB. */
    public int kleur() {
        return kleur;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("gui.guhs.snuffel.soort." + id());
    }

    @Nullable
    public static GeurSoort vanId(String id) {
        for (GeurSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static GeurSoort vanNummer(int n) {
        return n < 0 || n >= values().length ? null : values()[n];
    }
}
