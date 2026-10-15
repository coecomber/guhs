package nl.juiced.guhs.feature.verhaal;

import java.util.Arrays;

/**
 * 1.4.4 (klik om verder te gaan): the clock of a scene that waits for the viewer after every line. The scene's time runs
 * by itself through the parts without text (flying, action, establishing shots); at every pause point (the last tick a
 * subtitle is on the screen, {@link Cutscene#pauzes()}) it stands still until the viewer clicks once (left or right mouse
 * button, or space). A scene the viewer has seen before can be skipped as a whole by holding the button for
 * {@link #HOUD_TICKS} ticks. Pure logic without Minecraft classes, so both the client players of scenes and cards and the
 * game tests use the very same clock; one clock per viewer (in multiplayer everyone clicks for their own scene).
 */
public final class Klikklok {
    /** Ticks the button has to be held to skip a seen scene (1.5 seconds). */
    public static final int HOUD_TICKS = 30;

    /** What one tick of the clock did. */
    public enum Stap {
        /** The time went on by itself (when it reached a pause point, {@link #wacht()} is true from now on). */
        LOOPT,
        /** The clock stood still at a line this tick (the time did not go on) and waits for a click. */
        WACHT,
        /** A click: the clock goes on from the line it waited at (the time went on this tick). */
        VERDER,
        /** The button was held long enough: the scene is skipped. */
        OVERGESLAGEN
    }

    private final int[] pauzes;
    private final boolean magOverslaan;
    private int t, volgende, houd;
    private boolean wacht, overgeslagen;

    /** pauzes: the ticks at which the clock waits (any order; doubles count once); magOverslaan: a seen scene. */
    public Klikklok(int[] pauzes, boolean magOverslaan) {
        this.pauzes = Arrays.stream(pauzes).filter(p -> p > 0).distinct().sorted().toArray();
        this.magOverslaan = magOverslaan;
    }

    /**
     * One tick. klik: the viewer pressed the button since the last tick (a fresh press); ingedrukt: the button is down
     * now (for skipping). A click while the time still runs does nothing (the line is still being read).
     */
    public Stap tik(boolean klik, boolean ingedrukt) {
        if (overgeslagen) {
            return Stap.OVERGESLAGEN;
        }
        if (magOverslaan && ingedrukt) {
            if (++houd >= HOUD_TICKS) {
                overgeslagen = true;
                wacht = false;
                return Stap.OVERGESLAGEN;
            }
        } else {
            houd = 0;
        }
        if (wacht) {
            if (!klik) {
                return Stap.WACHT;
            }
            wacht = false;
            volgende++;
            loop();
            return Stap.VERDER;
        }
        loop();
        return Stap.LOOPT;   // (the time went on; when it reached a pause point, wacht() is true from now on)
    }

    private void loop() {
        t++;
        while (volgende < pauzes.length && pauzes[volgende] < t) {
            volgende++;   // (a pause point that was jumped over: never wait for it afterwards)
        }
        if (volgende < pauzes.length && pauzes[volgende] == t) {
            wacht = true;
        }
    }

    /** The scene's time (ticks since the start, not counting the waits). */
    public int tijd() {
        return t;
    }

    /** Does the clock stand still at a line now? */
    public boolean wacht() {
        return wacht;
    }

    public boolean magOverslaan() {
        return magOverslaan;
    }

    public boolean overgeslagen() {
        return overgeslagen;
    }

    /** How far the hold to skip is (0..1). */
    public float houdVoortgang() {
        return magOverslaan ? Math.min(1f, houd / (float) HOUD_TICKS) : 0f;
    }

    /** The pause points it knows (sorted). */
    public int[] pauzes() {
        return pauzes.clone();
    }
}
