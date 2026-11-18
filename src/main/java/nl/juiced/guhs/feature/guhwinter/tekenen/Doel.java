package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * What a drawing is for. Each purpose carries its own listeners: {@link #opOpslaan} is called (server side, main thread) every
 * time a player saves a drawing for it, and {@link #magOpslaan} may refuse a save (a deadline, a full Grote Boek...).
 * <ul>
 *   <li>{@link #LOOTJE}: the drawing for your lootje (slice lootjes: it keeps the id and gives the item after pakjesavond);</li>
 *   <li>{@link #KERSTBOOM}: your one drawing in the kerstboom (this slice: {@link KerstboomTekeningen}).</li>
 * </ul>
 */
public enum Doel {
    LOOTJE("lootje"),
    KERSTBOOM("kerstboom");

    /** May this player save a drawing for this purpose now? {@code bestaande} is the drawing being edited (or null). */
    @FunctionalInterface
    public interface Regel {
        /** Null = yes; else the lang key of the reason (shown in the editor). */
        @Nullable
        String weiger(ServerPlayer speler, @Nullable UUID bestaande);
    }

    private final String id;
    private final List<BiConsumer<ServerPlayer, Tekening>> luisteraars = new CopyOnWriteArrayList<>();
    private final List<Regel> regels = new CopyOnWriteArrayList<>();

    Doel(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Called after every save of a drawing with this purpose (new or edited), on the server thread. */
    public void opOpslaan(BiConsumer<ServerPlayer, Tekening> luisteraar) {
        luisteraars.add(luisteraar);
    }

    /** Adds a rule that may refuse a save (see {@link Regel}). */
    public void magOpslaan(Regel regel) {
        regels.add(regel);
    }

    @Nullable
    String weiger(ServerPlayer speler, @Nullable UUID bestaande) {
        for (Regel r : regels) {
            String w = r.weiger(speler, bestaande);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    void opgeslagen(ServerPlayer speler, Tekening t) {
        for (BiConsumer<ServerPlayer, Tekening> l : luisteraars) {
            l.accept(speler, t);
        }
    }

    public static Doel van(String id) {
        for (Doel d : values()) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        return LOOTJE;
    }
}
