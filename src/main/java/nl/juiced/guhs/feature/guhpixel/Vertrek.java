package nl.juiced.guhs.feature.guhpixel;

/** Why a player leaves a {@link Sessie}. */
public enum Vertrek {
    /** Done ({@link Sessie#klaar}). */
    KLAAR,
    /** Walked out: /lobby, the dev command. */
    VERLATEN,
    UITGELOGD,
    /** Left the dimension another way (/tp, a command). */
    DIMENSIE,
    DOOD,
    SERVER_STOP,
    /** The whole session was stopped ({@link Sessie#stop}). */
    GESTOPT;

    /** Does the player walk back to the lobby anchor (they are still here and alive)? */
    public boolean naarLobby() {
        return this == KLAAR || this == VERLATEN || this == GESTOPT;
    }
}
