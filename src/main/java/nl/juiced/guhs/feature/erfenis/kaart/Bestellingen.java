package nl.juiced.guhs.feature.erfenis.kaart;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// STUB-kaart: the kaart slice owns order generation and this class; the lead replaces it at the merge.
public final class Bestellingen {
    private Bestellingen() {
    }

    public static void open(ServerPlayer p) {
        p.sendOverlayMessage(Component.literal("De bestelkarretjes rijden nog niet... (bestellingen komen bij de merge)"));
    }
}
