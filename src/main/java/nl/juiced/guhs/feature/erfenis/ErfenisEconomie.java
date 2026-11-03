package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

// STUB-kern: minimal stand-in so the koppeling slice compiles (CONTRACT_OPKNAPPER: kern owns ErfenisEconomie). Replace at merge.
public final class ErfenisEconomie {
    private ErfenisEconomie() {
    }

    public static void geef(MinecraftServer server, UUID eigenaar, String goedOfGuhlden, int n) {
    }

    public static int heeft(MinecraftServer server, UUID eigenaar, String goedOfGuhlden) {
        return 0;
    }

    public static boolean neem(MinecraftServer server, UUID eigenaar, String goedOfGuhlden, int n) {
        return false;
    }
}
