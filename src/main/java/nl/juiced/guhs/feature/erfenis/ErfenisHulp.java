package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

// STUB-kern: "Handje helpen" (kaart slice) speeds up a running build or production of another player's village. The kern
// slice owns the builders' and the productions' timers and implements this for real (rule: -minuten, but never more than
// 25% of that job's total time); the lead replaces this file at merge.
// This stub works on the POC's one builder (Base.bouw) so the button does something in the test client.
public final class ErfenisHulp {
    private ErfenisHulp() {
    }

    /**
     * Speeds up a running build (or else a production) of {@code eigenaar}'s village by up to {@code minuten} minutes, at most
     * 25% of that job's total time. Returns the whole minutes actually saved (rounded up), 0 when nothing was running.
     */
    public static int versnel(MinecraftServer server, UUID eigenaar, int minuten) {
        ErfenisData data = ErfenisData.get(server);
        Base b = data.van(eigenaar);
        long nu = System.currentTimeMillis();
        if (b == null || b.bouw == null || b.bouw.eind() <= nu) {
            return 0;
        }
        long totaal = b.bouw.eind() - b.bouw.start();
        long scheelt = Math.min(minuten * 60_000L, totaal / 4);
        scheelt = Math.min(scheelt, b.bouw.eind() - nu);
        if (scheelt <= 0) {
            return 0;
        }
        b.bouw = new Base.Bouw(b.bouw.gebouw(), b.bouw.naar(), b.bouw.start(), b.bouw.eind() - scheelt);
        data.setDirty();
        return (int) ((scheelt + 59_999) / 60_000);
    }
}
