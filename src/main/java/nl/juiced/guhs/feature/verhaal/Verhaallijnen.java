package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;

/**
 * bbq2 (verhaal engine): every registered {@link Verhaallijn}, in the order the Guhdex shows them (group by group, the
 * groups in {@link #GROEPEN} order, other groups after them in the order they first registered).
 */
public final class Verhaallijnen {
    /** The headings of this update, in display order (lang {@code gui.guhs.verhalen.kop.<groep>}). */
    public static final List<String> GROEPEN = List.of("knabbelring", "guhrio", "techniek", "barbecue");

    private static final Map<String, Verhaallijn> ALLE = new LinkedHashMap<>();

    static void voegToe(Verhaallijn l) {
        synchronized (ALLE) {
            if (ALLE.containsKey(l.id()) || VerhalenVoortgang.STAPPEN.containsKey(l.id())) {
                throw new IllegalStateException("Verhaallijn " + l.id() + " is registered twice");
            }
            ALLE.put(l.id(), l);
            VerhalenVoortgang.STAPPEN.put(l.id(), l.stappen());
        }
    }

    @Nullable
    public static Verhaallijn van(@Nullable String id) {
        synchronized (ALLE) {
            return id == null ? null : ALLE.get(id);
        }
    }

    /** Every line, in display order. */
    public static List<Verhaallijn> alle() {
        List<Verhaallijn> out = new ArrayList<>();
        for (String g : groepen()) {
            out.addAll(vanGroep(g));
        }
        return out;
    }

    public static List<Verhaallijn> vanGroep(String groep) {
        synchronized (ALLE) {
            return ALLE.values().stream().filter(l -> l.groep().equals(groep)).toList();
        }
    }

    /** The groups that have a line, in display order. */
    public static List<String> groepen() {
        List<String> out = new ArrayList<>();
        synchronized (ALLE) {
            for (String g : GROEPEN) {
                if (ALLE.values().stream().anyMatch(l -> l.groep().equals(g))) {
                    out.add(g);
                }
            }
            for (Verhaallijn l : ALLE.values()) {
                if (!out.contains(l.groep())) {
                    out.add(l.groep());
                }
            }
        }
        return out;
    }

    /**
     * The line this player follows (the objective line, the Superkompas entry "Mijn verhaal", a companion): the one picked
     * in the Guhdex while it isn't done; else the line with {@link Verhaallijn#doelregel} that is open, not done and was
     * touched last (a line that just opened because the one before it was finished counts as touched then). Null: none.
     */
    @Nullable
    public static Verhaallijn gevolgd(ServerPlayer p) {
        Verhaallijn gekozen = van(VerhaalSync.gekozen(p));
        if (gekozen != null && !gekozen.klaar(p)) {
            return gekozen;
        }
        Verhaallijn beste = null;
        long besteTijd = 0;
        for (Verhaallijn l : alle()) {
            if (!l.doelregel() || l.klaar(p) || !l.aanDeBeurt(p)) {
                continue;
            }
            Verhaallijn voor = van(l.na());
            long tijd = Math.max(l.laatst(p), voor == null ? 0 : voor.laatst(p));
            if (tijd > besteTijd && (l.begonnen(p) || voor != null)) {
                beste = l;
                besteTijd = tijd;
            }
        }
        return beste;
    }

    private Verhaallijnen() {
    }
}
