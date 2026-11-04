package nl.juiced.guhs.feature.vadsrijk.klassen.boom;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;

/**
 * The skill tree rules: buying a node (points, requirements, the either/or R variants) and the free respec with a cooldown of
 * one hour real time (ronde 12). The pure methods take the data so the gametests can check the maths without a world.
 */
public final class Boom {
    /** One hour of real time between two free respecs. */
    public static final long RESPEC_MS = 60L * 60L * 1000L;
    /** z2-systemen: node id -> the class level it needs (the zone-2 rows 4/5 open at lv 20/26). Reason "level". */
    public static final java.util.Map<String, Integer> MIN_LEVEL = new java.util.concurrent.ConcurrentHashMap<>();

    private Boom() {
    }

    @Nullable
    public static BoomKnoop knoop(Klasse k, String id) {
        for (BoomKnoop b : k.boom()) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** Points spent on the visible tree (advancement nodes cost nothing). */
    public static int besteed(Klasse k, Set<String> heeft) {
        int n = 0;
        for (BoomKnoop b : k.boom()) {
            if (heeft.contains(b.id())) {
                n += b.kosten();
            }
        }
        return n;
    }

    public static int vrij(Klasse k, Set<String> heeft, int level) {
        return Levels.skillpunten(level) - besteed(k, heeft);
    }

    /** Why this node can't be bought now (a lang key suffix), or null when it can. */
    @Nullable
    public static String waaromNiet(Klasse k, Set<String> heeft, int level, String id) {
        BoomKnoop b = knoop(k, id);
        if (b == null || b.rij() < 0) {
            return "onbekend";
        }
        if (heeft.contains(id)) {
            return "al";
        }
        for (String uit : Bomen.uitgesloten(id)) {
            if (heeft.contains(uit)) {
                return "uitgesloten";
            }
        }
        for (String v : b.vereist()) {
            boolean een = false;
            for (String alt : v.split("\\|")) {
                een |= heeft.contains(alt);
            }
            if (!een) {
                return "vereist";
            }
        }
        if (level < MIN_LEVEL.getOrDefault(id, 0)) {
            return "level";   // z2-systemen
        }
        if (vrij(k, heeft, level) < b.kosten()) {
            return "punten";
        }
        return null;
    }

    public static Set<String> knopen(SpelerData d, String klasse) {
        return d.boom.computeIfAbsent(klasse, x -> new LinkedHashSet<>());
    }

    /** Buys a node of the current class. Returns null when it worked, else the reason. */
    @Nullable
    public static String koop(ServerPlayer p, String id) {
        SpelerData d = Spelers.data(p);
        Klasse k = Spelers.klasse(p);
        Set<String> heeft = knopen(d, k.id());
        String nee = waaromNiet(k, heeft, d.level(k.id()), id);
        if (nee == null) {
            heeft.add(id);
            Spelers.sync(p);
        }
        return nee;
    }

    /** Respec maths: clears the bought nodes of {@code klasse} (advancement nodes stay) when the cooldown is over. Returns
     *  null when it worked, "cooldown" or "leeg". */
    @Nullable
    public static String respec(SpelerData d, String klasse, long nuMs) {
        if (nuMs < d.respecKlaar) {
            return "cooldown";
        }
        Klasse k = Klassen.van(klasse);
        Set<String> heeft = knopen(d, klasse);
        if (k == null || besteed(k, heeft) == 0) {
            return "leeg";
        }
        heeft.removeIf(id -> {
            BoomKnoop b = knoop(k, id);
            return b == null || b.rij() >= 0;
        });
        d.respecKlaar = nuMs + RESPEC_MS;
        return null;
    }

    /** Seconds until the next free respec (0 = now). */
    public static long respecOver(SpelerData d, long nuMs) {
        return Math.max(0, (d.respecKlaar - nuMs + 999) / 1000);
    }

    /** Does this player have this node (current class)? Server or client data alike. */
    public static boolean heeft(net.minecraft.world.entity.player.Player p, String id) {
        return Spelers.heeftKnoop(p, id);
    }
}
