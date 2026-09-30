package nl.juiced.guhs.feature.band;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * The statistics in a guh's dagboekje (lang {@code gui.guhs.dagboek.stat.<id>}). Append-only. Producers: BLOKKEN_SAMEN,
 * KNUFFELS, KNABBELS_GEGETEN, DAGEN_SAMEN (fundament), MINIGAMES_SAMEN (samen), KNABBELS_OPGEGRAVEN + KLUSJES (klusjes),
 * SPEELTJES (speelgoed).
 */
public enum DagboekStat {
    BLOKKEN_SAMEN, KNUFFELS, KNABBELS_GEGETEN, MINIGAMES_SAMEN, KNABBELS_OPGEGRAVEN, DAGEN_SAMEN, KLUSJES, SPEELTJES;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("gui.guhs.dagboek.stat." + id());
    }

    @Nullable
    public static DagboekStat byId(String id) {
        for (DagboekStat s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
