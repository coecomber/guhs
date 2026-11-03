package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// STUB-kern: minimal stand-in so the koppeling slice compiles (CONTRACT_OPKNAPPER: kern owns ErfenisMomenten). Replace at merge.
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
