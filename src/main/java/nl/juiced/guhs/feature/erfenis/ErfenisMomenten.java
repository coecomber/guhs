package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// STUB-kern: the fixed story moments of the village (CONTRACT_OPKNAPPER: brief_gelezen, aangekomen, plukje_opgeruimd:<n>,
// vondst:<id>, gebouw_gebouwd:<id>, gebouw_level:<id>:<lvl>, hoeve_level:<n>, bestelling_geleverd:<totaal>, bewoners:<totaal>,
// knusheid:<totaal>). The kern slice owns the real one (it fires them); the sherlock slice listens. This stub only passes a
// moment on to the listeners when the player is online (the real one may do the same); the lead replaces this file at merge.
public final class ErfenisMomenten {
    private static final List<BiConsumer<ServerPlayer, String>> LUISTERAARS = new CopyOnWriteArrayList<>();

    private ErfenisMomenten() {
    }

    public static void melding(MinecraftServer server, UUID speler, String moment) {
        ServerPlayer p = server.getPlayerList().getPlayer(speler);
        if (p != null) {
            for (BiConsumer<ServerPlayer, String> l : LUISTERAARS) {
                l.accept(p, moment);
            }
        }
    }

    public static void luister(BiConsumer<ServerPlayer, String> luisteraar) {
        LUISTERAARS.add(luisteraar);
    }
}
