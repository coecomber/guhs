package nl.juiced.guhs.feature.bio.wereld;

/**
 * biomes3 wereld, the Bloesemmeertje: the day rhythm of its falling petals, as a rule both sides can read (the client
 * draws the petals: client/MeerClient). More petals in a gust of the breeze that comes and goes by itself (about every
 * 75 seconds), in the minute after dusk and after dawn, in rain and most of all in thunder; half as many in a quiet night.
 */
public final class MeerRitme {
    /** Extra petals per tick around the camera in calm daylight. */
    public static final float BASIS = 0.3f;
    /** A breeze after dusk or dawn lasts this long (ticks). */
    public static final int BRIES = 1200;

    /**
     * How many extra petals a tick. regen, onweer: 0..1 (the level's rain and thunder level); donker: night;
     * briesTicks: what is left of the dusk / dawn breeze; tijd: the game time.
     */
    public static float sterkte(float regen, float onweer, boolean donker, int briesTicks, long tijd) {
        // the breeze that comes and goes: mostly still, a gust now and then
        double golf = 0.5 + 0.5 * Math.sin(tijd * (2 * Math.PI / 1500.0));
        float s = BASIS * (float) (1 + 2.5 * golf * golf * golf);
        if (donker) {
            s *= 0.5f;
        }
        if (briesTicks > 0) {
            s += BASIS * 2.5f * Math.min(1f, briesTicks / 200f);
        }
        return s + BASIS * (2.5f * regen + 2f * onweer);
    }

    private MeerRitme() {
    }
}
