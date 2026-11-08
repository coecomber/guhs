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
        if (KernPayloads.stuur(p, new KernPayloads.Scene(s))) {
            SceneLaden.begin(p, s);   // W-SPEL: a far camera gets its chunks (server sends them; the client waits for them)
        } else {
            SceneLaden.stop(p);
        }
    }

    public static boolean bezig(ServerPlayer p) {
        Long t = BEZIG_TOT.get(p.getUUID());
        return t != null && p.level().getGameTime() < t;
    }

    /** U-QUESTROBOT: the scene is over for this player (the robot plays a whole quest chain in one tick: "a quiet moment later"). */
    public static void vergeet(ServerPlayer p) {
        BEZIG_TOT.remove(p.getUUID());
        SceneLaden.stop(p);   // W-SPEL
    }

    /** A big title card (Souls/Dungeons style) for this many ticks; kleur = the accent line's RGB. */
    public static void titel(ServerPlayer p, Component titel, Component onder, int ticks, int kleur) {
        KernPayloads.stuur(p, new KernPayloads.Titel(titel, onder, ticks, kleur));
    }
}
