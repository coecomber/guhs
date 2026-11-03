package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

// STUB-kern: the kaart slice fires "bestelling_geleverd:<totaal>" through this. The kern slice owns the real one (with
// luister(...) for the sherlock slice); the lead replaces this file at merge.
public final class ErfenisMomenten {
    private ErfenisMomenten() {
    }

    public static void melding(MinecraftServer server, UUID speler, String moment) {
    }
}
