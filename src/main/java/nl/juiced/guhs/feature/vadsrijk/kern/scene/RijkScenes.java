package nl.juiced.guhs.feature.vadsrijk.kern.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;

/** Plays {@link RijkScene}s and title cards; knows who is in a scene right now (they take no damage and can't act). */
public final class RijkScenes {
    private static final Map<UUID, Long> BEZIG_TOT = new HashMap<>();

    private RijkScenes() {
    }

    public static void speel(ServerPlayer p, RijkScene s) {
        BEZIG_TOT.put(p.getUUID(), p.level().getGameTime() + s.duur());
        KernPayloads.stuur(p, new KernPayloads.Scene(s));
    }

    public static boolean bezig(ServerPlayer p) {
        Long t = BEZIG_TOT.get(p.getUUID());
        return t != null && p.level().getGameTime() < t;
    }

    /** A big title card (Souls/Dungeons style) for this many ticks; kleur = the accent line's RGB. */
    public static void titel(ServerPlayer p, Component titel, Component onder, int ticks, int kleur) {
        KernPayloads.stuur(p, new KernPayloads.Titel(titel, onder, ticks, kleur));
    }
}
