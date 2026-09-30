package nl.juiced.guhs.feature.verhaal;

/**
 * 3.0 (Guhverhalen): the KnusVlaggen bits of the story guhs (synced to the client, saved; set/read with
 * {@code nl.juiced.guhs.feature.knus.GuhHooks.zet/heeft}). Use these constants, never raw numbers (CONTRACT_30 §2).
 */
public final class VerhaalVlaggen {
    /** mewtwo: the Guhtwo hovers (hover pose, purple glow). */
    public static final int ZWEEFT = 1 << 19;
    /** guhwaii: the 626-guh clings to a wall or the ceiling. */
    public static final int KLIMT = 1 << 20;
    /** balto: the Baltoguh sniffs the way (nose down, a trail). */
    public static final int SNUFFELT = 1 << 21;
    /** hemel: sparkles for a while after coming back from the wolkjes. */
    public static final int GLANS = 1 << 22;
    /** fundament: a story copy (not tameable, name always shown); see VerhaalGuhs. */
    public static final int VERHAAL_NPC = 1 << 23;

    private VerhaalVlaggen() {
    }
}
