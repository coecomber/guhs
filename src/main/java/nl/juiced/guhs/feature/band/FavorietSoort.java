package nl.juiced.guhs.feature.band;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/** The kinds of secret favourites every band guh has (lang {@code gui.guhs.favoriet.soort.<id>}). Append-only. */
public enum FavorietSoort {
    ETEN, PLEK, KNUFFEL, LIEDJE, SPEELTJE, EMOTE, KLEUR, VRIEND;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("gui.guhs.favoriet.soort." + id());
    }

    @Nullable
    public static FavorietSoort byId(String id) {
        for (FavorietSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
