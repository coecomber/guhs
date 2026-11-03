package nl.juiced.guhs.feature.erfenis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;

// STUB-kern: the kaart slice needs these to compile and test (orders, Handje helpen). The kern slice owns the real one
// (village storage in the Opslagschuur, Guhlden in the Hoeve); the lead replaces this file at merge. This stub keeps the
// stock in memory only (lost on restart), which is enough for the gametests and a quick look in the dev client.
public final class ErfenisEconomie {
    private static final Map<UUID, Map<String, Integer>> VOORRAAD = new HashMap<>();

    private ErfenisEconomie() {
    }

    public static void geef(MinecraftServer server, UUID eigenaar, String goedOfGuhlden, int n) {
        if (n > 0) {
            VOORRAAD.computeIfAbsent(eigenaar, k -> new HashMap<>()).merge(goedOfGuhlden, n, Integer::sum);
        }
    }

    public static int heeft(MinecraftServer server, UUID eigenaar, String goedOfGuhlden) {
        return VOORRAAD.getOrDefault(eigenaar, Map.of()).getOrDefault(goedOfGuhlden, 0);
    }

    public static boolean neem(MinecraftServer server, UUID eigenaar, String goedOfGuhlden, int n) {
        int h = heeft(server, eigenaar, goedOfGuhlden);
        if (n < 0 || h < n) {
            return false;
        }
        VOORRAAD.computeIfAbsent(eigenaar, k -> new HashMap<>()).put(goedOfGuhlden, h - n);
        return true;
    }
}
