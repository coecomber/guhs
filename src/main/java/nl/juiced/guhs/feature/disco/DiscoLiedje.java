package nl.juiced.guhs.feature.disco;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The songs of the Guhdisco (2.9): the song IS the level. Every song is a real track (assets/guhs/sounds/disco/*.ogg,
 * made by tools/remix/) and the game follows its beat: the colours flash exactly on the beat (a fractional tick
 * accumulator, {@link #ticksPerBeat()}), the tempo never speeds up; a song gets harder by starting with a longer row,
 * by playing the colours on "double counts" (two per beat) from a certain row length, and by less time per step.
 * <p>
 * Beat 0 of every track is at t = 0 and the game loops a song after {@link #loopBeats} beats (whole bars; checked by
 * tools/remix/check_songs.py, keep in sync).
 */
public enum DiscoLiedje {
    /** Makkelijk: a slow, dramatic little tango with a bandoneon and singing guhs (~100 BPM). */
    TANGO("vadsige_tango", Niveau.MAKKELIJK, 100.0, 144, 89.9, 1, 0, 10,
            new int[]{0xE0303C, 0xF2C14E, 0xFFE8E0, 0x8C1C3C}),
    /** Medium (the standard song, also the idle music): the 70's disco version of "Ze hangen aan me vet" (110 BPM). */
    DISCO70("ze_hangen_disco70", Niveau.MEDIUM, 110.0, 200, 111.6, 1, 9, 8,
            new int[]{0xFF6EC7, 0x4FB6FF, 0xFFE14D, 0x5CF07A}),
    /** Lastig: the cheeky Mika-Mambo (~140 BPM), the Mika's sing the coro. */
    MAMBO("mika_mambo", Niveau.LASTIG, 140.0, 208, 92.1, 3, 6, 8,
            new int[]{0xFF8A1E, 0xFF2E9A, 0xB6F03C, 0x2EE6D6}),
    /** The bonus song with its own board: Njeg-Njeg Boogie (~128 BPM). Played like medium (coins), a bit harder. */
    BOOGIE("njeg_njeg_boogie", null, 128.0, 208, 100.5, 2, 7, 8,
            new int[]{0xA25CFF, 0xFFC93C, 0x3CE0FF, 0xFFFFFF});

    /** The sound event (guhs:disco.&lt;id&gt;) and file (sounds/disco/&lt;id&gt;.ogg). */
    public final String id;
    /** The level this song is (null: the bonus song). */
    @Nullable
    public final Niveau niveau;
    public final double bpm;
    /** After this many beats the DJ puts the song on again (a game keeps going as long as you keep dancing). */
    public final int loopBeats;
    /** The whole track (with its last reverb tail), in seconds: the idle music starts again after it. */
    public final double seconds;
    /** How many colours the first round has. */
    public final int startLengte;
    /** From this row length the DJ plays two colours per beat ("double counts"); 0 = never. */
    public final int dubbelVanaf;
    /** How many beats you may think about every next step. */
    public final int stapBeats;
    /** The song's own colours (light show, sparkles, the screen). */
    private final int[] palet;

    DiscoLiedje(String id, @Nullable Niveau niveau, double bpm, int loopBeats, double seconds, int startLengte, int dubbelVanaf,
                int stapBeats, int[] palet) {
        this.id = id;
        this.niveau = niveau;
        this.bpm = bpm;
        this.loopBeats = loopBeats;
        this.seconds = seconds;
        this.startLengte = startLengte;
        this.dubbelVanaf = dubbelVanaf;
        this.stapBeats = stapBeats;
        this.palet = palet;
    }

    /** Server ticks per beat (not a whole number: 110 BPM = 10.909 ticks). */
    public double ticksPerBeat() {
        return 1200.0 / bpm;
    }

    /** The world top 3 of this song: the medium song keeps the old board (disco_kleuren). */
    public String board() {
        return niveau == null ? DiscoGame.BOARD + "_boogie" : niveau.board(DiscoGame.BOARD);
    }

    /** The Highscores row of this song. */
    public String highscore() {
        return this == DISCO70 ? "disco" : this == BOOGIE ? "disco_boogie" : "disco_" + niveau.id();
    }

    /** Is a row of this length played on double counts (two colours per beat)? */
    public boolean dubbel(int lengte) {
        return dubbelVanaf > 0 && lengte >= dubbelVanaf;
    }

    /** Discomunten for a game on this song: lastig +50 %. */
    public int munten(int score) {
        int basis = DiscoGame.coins(score);
        return niveau == null ? basis : niveau.munten(basis);
    }

    public int kleur(int i) {
        return palet[Math.floorMod(i, palet.length)];
    }

    public Component naam() {
        return Component.translatable("gui.guhs.disco.lied." + id);
    }

    /** "Makkelijk" / "Medium" / "Lastig" / "Bonus". */
    public Component niveauNaam() {
        return niveau == null ? Component.translatable("gui.guhs.disco.bonus") : niveau.naam();
    }

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DiscoLiedje of(int ordinal) {
        DiscoLiedje[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }

    /** The song that belongs to a level (the bonus song has none). */
    public static DiscoLiedje of(Niveau niveau) {
        for (DiscoLiedje l : values()) {
            if (l.niveau == niveau) {
                return l;
            }
        }
        return DISCO70;
    }
}
