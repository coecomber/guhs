package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

// STUB-kern: the kaart slice hands out special deco pieces (order thanks, the gold-star order, the Burenhulp-bloempot). The
// kern slice owns the deco storage ("decoschuurtje"-list) and implements this; the lead replaces this file at merge.
// Deco ids used by the kaart slice: knabbelbessenstruik, kaassausfonteintje, de_eerste_knabbel, mika_schrik,
// mammoetje_van_heg, burenhulp_bloempot, burenhulp_bankje, burenhulp_vlaggetjes (see NOTITIES_KAART.md).
public final class ErfenisDeco {
    private ErfenisDeco() {
    }

    /** Puts one deco piece {@code decoId} in {@code eigenaar}'s deco storage (placed later from the overview). */
    public static void geef(MinecraftServer server, UUID eigenaar, String decoId) {
    }
}
