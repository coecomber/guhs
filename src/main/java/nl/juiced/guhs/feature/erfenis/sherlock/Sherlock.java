package nl.juiced.guhs.feature.erfenis.sherlock;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// STUB-sherlock: the sherlock slice owns this class; the lead replaces it at the merge.
public final class Sherlock {
    private Sherlock() {
    }

    public static void start(ServerPlayer p, String zaakId) {
    }

    public static void openBureau(ServerPlayer p) {
        p.sendOverlayMessage(Component.literal("Sherlock Guh doet een dutje... (zaakjes komen bij de merge)"));
    }
}
