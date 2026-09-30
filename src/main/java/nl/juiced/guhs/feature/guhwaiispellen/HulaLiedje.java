package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The songs of the hula dance at the surf beach of Guhwai'i (3.0): the song IS the level, like at the Guhdisco. Three
 * original songs of a fifties guh crooner on a Hawaiian beach (tools/remix/make_hula.py; sounds/guhwaiispellen/*.ogg),
 * beat 0 at t = 0 (checked by tools/remix/check_songs.py). The dance steps ({@link HulaKaart}) sit exactly on their beat
 * grid, section by section (the same bars as the song's SECTIONS in make_hula.py).
 */
public enum HulaLiedje {
    /** Makkelijk: a slow moonlight croon with a sighing steel guitar (88 BPM); only the hips: left, right. */
    ALOHA_NJEG("aloha_njeg", Niveau.MAKKELIJK, 88.0, 32, 92.27, 0xFF7EB8, 0x7ED8FF),
    /** Medium: rock'n'roll on the beach, slap bass and slapback (132 BPM); hips, arms up and down the knees. */
    GUHLA_HULA_ROCK("guhla_hula_rock", Niveau.MEDIUM, 132.0, 48, 91.27, 0xFFC93C, 0xFF5E7E),
    /** Lastig: surf-rock and rockabilly with rolling toms (160 BPM); eighths, and a VAHOEG! (space) on every shout. */
    VAHOEG_HULA_HOP("vahoeg_hula_hop", Niveau.LASTIG, 160.0, 60, 94.0, 0x3CE0C8, 0xFF8A1E);

    /** The sound event guhs:guhwaiispellen.hula_&lt;id&gt; and the file sounds/guhwaiispellen/&lt;id&gt;.ogg. */
    public final String id;
    public final Niveau niveau;
    public final double bpm;
    /** The bars of the song (4/4): its last beat (the final chord) is at bars * 4. */
    public final int maten;
    /** The whole track with its ringing end, in seconds. */
    public final double seconds;
    /** Its two colours (the screen, the flowers). */
    public final int kleur, kleur2;

    HulaLiedje(String id, Niveau niveau, double bpm, int maten, double seconds, int kleur, int kleur2) {
        this.id = id;
        this.niveau = niveau;
        this.bpm = bpm;
        this.maten = maten;
        this.seconds = seconds;
        this.kleur = kleur;
        this.kleur2 = kleur2;
    }

    /** Server ticks per beat (88 BPM = 13.64 ticks, never rounded along the way). */
    public double ticksPerBeat() {
        return 1200.0 / bpm;
    }

    /** Milliseconds per beat. */
    public double msPerBeat() {
        return 60000.0 / bpm;
    }

    /** The game time (ticks after the start) of a beat. */
    public double tick(double beat) {
        return beat * ticksPerBeat();
    }

    /** The time of a beat in ms after the start. */
    public double ms(double beat) {
        return beat * msPerBeat();
    }

    /** The world top 3 and the Highscores row: hula_makkelijk / hula_medium / hula_lastig. */
    public String board() {
        return "hula_" + niveau.id();
    }

    public String geluid() {
        return "guhwaiispellen.hula_" + id;
    }

    public Component naam() {
        return Component.translatable("gui.guhs.guhwaiispellen.lied." + id);
    }

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HulaLiedje of(Niveau niveau) {
        for (HulaLiedje l : values()) {
            if (l.niveau == niveau) {
                return l;
            }
        }
        return GUHLA_HULA_ROCK;
    }

    public static HulaLiedje of(int ordinal) {
        HulaLiedje[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
