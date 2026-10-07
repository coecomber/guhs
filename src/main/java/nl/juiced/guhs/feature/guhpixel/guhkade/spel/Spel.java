package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

import java.util.function.LongFunction;

/** The two games of the Guhkade. */
public enum Spel {
    FLAPPY("flappy", FlappySim::new),
    PONG("pong", PongSim::new);

    public final String id;
    private final LongFunction<Sim> maker;

    Spel(String id, LongFunction<Sim> maker) {
        this.id = id;
        this.maker = maker;
    }

    public Sim nieuw(long seed) {
        return maker.apply(seed);
    }

    public static Spel op(String id) {
        for (Spel s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return FLAPPY;
    }
}
