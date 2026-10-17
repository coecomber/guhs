package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.network.ModNetworking;
import org.slf4j.Logger;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3, 6, 7.9), slice sp_audio: the sound API of the Sponsguh story. Every name of
 * {@link #NAMEN} is the sound event {@code guhs:einde2_sp_audio.<naam>} (subtitle {@code subtitles.guhs.einde2_sp_audio.<naam>};
 * registered in {@link E2SpAudioFeature}).
 * <ul>
 *   <li>{@link #moment}: the music under a big scene (call it right before {@code Cutscenes.speel}, {@link #stopMoment} in the
 *       scene's {@code daarna}). It plays in the music category, fades in, loops while the player waits at a click and fades
 *       out on {@link #stopMoment}; the background music is silent meanwhile. The three SONGS are moments too
 *       ({@code moment(p, "zoete_zege")}): the client starts a song exactly on the tick its scene put it with {@link #lied},
 *       so words and music stay together whatever the clicks before it took;</li>
 *   <li>{@link #stinger}: a short sound for one player (or at a spot). A TUNE ({@link #DEUNTJES}) given to
 *       {@link #stinger(ServerPlayer, String)} becomes that player's minigame tune: it loops from its first beat (the BPM
 *       games count from there) until {@link #stopDeuntje}, a cutscene, death, a change of music area or dimension, or
 *       six minutes; starting it again starts it from beat 0;</li>
 *   <li>{@link #lied}: the SUNG captions of one of the three songs ({@link #LIEDEREN}) on the song's own timing, and the
 *       tick the song starts in that scene; returns the song's length in ticks. Rule for the scene owner: no {@code zeg}
 *       (a line that waits for a click) between {@code vanaf} and {@code vanaf + length}, or the clicks would hold the
 *       captions while the music runs on;</li>
 *   <li>{@link #BPM}: the beat of the rhythm games, from tick 0 of the tune (the generator puts beat 1 on sample 0).</li>
 * </ul>
 */
public final class SpAudio {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Background music (music category, 2-4 min, silence between: the areas bikinibodem, schelpenstad, loopgraaf). */
    public static final List<String> MUZIEK = List.of("muziek.bikinibodem_1", "muziek.bikinibodem_2", "muziek.schelpenstad", "muziek.loopgraaf");
    /** The big moments (moment ids = the part after "moment."). */
    public static final List<String> MOMENTEN = List.of("moment.zoete_zege", "moment.guhfy_guhber", "moment.monsters", "moment.plan_z",
            "moment.bevroren", "moment.cycloop", "moment.hasselguh", "moment.dennis");
    /** Tunes of the minigames and places. */
    public static final List<String> DEUNTJES = List.of("deuntje.bakken", "deuntje.kwallen", "deuntje.examen", "deuntje.sluipen",
            "deuntje.trommel", "deuntje.kroeg", "deuntje.ijssalon", "deuntje.emmers");
    /** Short stings. */
    public static final List<String> STINGERS = List.of("stinger.verteller", "stinger.klarinet", "stinger.karen", "stinger.bevries",
            "stinger.snor", "stinger.opgelost", "stinger.mislukt", "stinger.lach");
    public static final List<String> NAMEN = java.util.stream.Stream.of(MUZIEK, MOMENTEN, DEUNTJES, STINGERS).flatMap(List::stream).toList();
    /** The three songs: the scene each plays in. */
    public static final Map<String, String> LIEDEREN = Map.of("zoete_zege", "einde2_sp_d1_recept_zoete_zege",
            "guhfy_guhber", "einde2_sp_d2_finale_guhfy_guhber", "monsters", "einde2_sp_d2_weg_monsters");
    /** The beat of the rhythm games (beats per minute from tick 0 of the tune). */
    public static final Map<String, Integer> BPM = Map.of("deuntje.trommel", 112, "deuntje.kroeg", 96);
    /** The default fade of a moment, in and out (ticks). */
    public static final int FADE = 40;
    /** A moment or a tune that nobody stops ends after this long (6 minutes; the client keeps the same limit). */
    public static final int MAX_TICKS = 6 * 60 * 20;
    /** Stingers at a spot: heard within this many blocks. */
    public static final double BEREIK = 32;

    /** Server: the moment each player hears now (full name), and the tune with the music area it began in. */
    private static final Map<UUID, String> MOMENT = new ConcurrentHashMap<>();
    record Deuntje(String naam, String gebied) {
    }
    private static final Map<UUID, Deuntje> DEUNTJE = new ConcurrentHashMap<>();
    private static final Set<String> GEMELD = ConcurrentHashMap.newKeySet();

    private SpAudio() {
    }

    /** The sound event of a name of {@link #NAMEN} (an unknown name: a vanilla chime, logged once). */
    public static Supplier<SoundEvent> geluid(String naam) {
        Holder<SoundEvent> h = holder(naam);
        return h == null ? () -> SoundEvents.NOTE_BLOCK_CHIME.value() : h::value;
    }

    /** The registered holder of a name of {@link #NAMEN} (null: no such name). */
    @Nullable
    public static Holder<SoundEvent> holder(String naam) {
        Holder<SoundEvent> h = E2SpAudioFeature.SOUNDS_PER_NAAM.get(naam);
        if (h == null && GEMELD.add(naam)) {
            LOGGER.warn("Guhs einde2 sp_audio: no sound named {} (CONTRACT_E2_C 6.1)", naam);
        }
        return h;
    }

    /** Which category a name plays in (music and moments: the music slider; tunes and stingers: jukebox/note blocks). */
    public static SoundSource categorie(String naam) {
        return naam.startsWith("muziek.") || naam.startsWith("moment.") ? SoundSource.MUSIC : SoundSource.RECORDS;
    }

    /** Starts the music of a big moment for this player ({@code id} without "moment."; the three songs too). */
    public static void moment(ServerPlayer p, String id) {
        String naam = id.startsWith("moment.") ? id : "moment." + id;
        if (!MOMENTEN.contains(naam) || holder(naam) == null) {
            return;
        }
        MOMENT.put(p.getUUID(), naam);
        ModNetworking.sendTo(p, new SpAudioPayloads.Moment(naam, FADE, LIEDEREN.containsKey(naam.substring("moment.".length()))));
    }

    /** (Dev command) any long piece ({@code muziek.*} or {@code moment.*}) at once, as a moment. */
    static void speelLang(ServerPlayer p, String naam) {
        MOMENT.put(p.getUUID(), naam);
        ModNetworking.sendTo(p, new SpAudioPayloads.Moment(naam, FADE, false));
    }

    /** Stops the moment's music with a fade. */
    public static void stopMoment(ServerPlayer p, int fadeTicks) {
        MOMENT.remove(p.getUUID());
        ModNetworking.sendTo(p, new SpAudioPayloads.Moment("", Math.max(0, fadeTicks), false));
    }

    /** The moment this player hears now (full name), or null. */
    @Nullable
    public static String moment(ServerPlayer p) {
        return MOMENT.get(p.getUUID());
    }

    /**
     * A short sound for this player ({@code naam} of {@link #DEUNTJES} / {@link #STINGERS}). A tune becomes the player's
     * looping minigame tune ({@link #deuntje}).
     */
    public static void stinger(ServerPlayer p, String naam) {
        if (DEUNTJES.contains(naam)) {
            deuntje(p, naam, true);
            return;
        }
        Holder<SoundEvent> h = holder(naam);
        if (h == null) {
            return;
        }
        p.connection.send(new ClientboundSoundPacket(h, categorie(naam), p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, p.getRandom().nextLong()));
    }

    /** A short sound at a spot, for the players within 32 blocks (a tune here plays once). */
    public static void stinger(ServerLevel level, Vec3 plek, String naam) {
        Holder<SoundEvent> h = holder(naam);
        if (h == null) {
            return;
        }
        // a variable-range event carries volume * 16 blocks: 2.0 = 32 blocks
        level.playSound(null, plek.x, plek.y, plek.z, h, categorie(naam), (float) (BEREIK / 16.0), 1.0F);
    }

    /** This player's minigame tune from its first beat (loop: until {@link #stopDeuntje} or one of the stops of the class comment). */
    public static void deuntje(ServerPlayer p, String naam, boolean herhaal) {
        if (!DEUNTJES.contains(naam) || holder(naam) == null) {
            return;
        }
        DEUNTJE.put(p.getUUID(), new Deuntje(naam, SpAudioPlek.gebiedNu(p)));
        ModNetworking.sendTo(p, new SpAudioPayloads.Deuntje(naam, herhaal));
    }

    /** Stops this player's minigame tune (the stand-still game of De Ruige Kroeg: while the Mikas look). */
    public static void stopDeuntje(ServerPlayer p) {
        if (DEUNTJE.remove(p.getUUID()) != null) {
            ModNetworking.sendTo(p, new SpAudioPayloads.Deuntje("", false));
        }
    }

    /** The tune this player hears now (full name), or null. */
    @Nullable
    public static String deuntje(ServerPlayer p) {
        Deuntje d = DEUNTJE.get(p.getUUID());
        return d == null ? null : d.naam();
    }

    /** (The area tracker) the music area changed: a tune begun in another area ends. */
    static void gebiedVeranderd(ServerPlayer p, String nu) {
        Deuntje d = DEUNTJE.get(p.getUUID());
        if (d != null && !d.gebied().equals(nu)) {
            stopDeuntje(p);
        }
    }

    /**
     * Puts the sung captions of a song ({@link #LIEDEREN} key) into the builder of its scene from tick {@code vanaf} on, and
     * remembers that the song starts there (the client starts the recording on that tick of the scene); returns how long
     * the song runs (ticks). The scene id of the builder must be the song's scene ({@link #LIEDEREN}): the captions are
     * {@code scene.guhs.<that scene>.lied.<key>}. No {@code zeg} inside the song (see the class comment).
     */
    public static int lied(Cutscene.Builder b, String lied, int vanaf) {
        SpLiederen.Lied l = SpLiederen.van(lied);
        if (l == null) {
            LOGGER.warn("Guhs einde2 sp_audio: no song named {}", lied);
            return 0;
        }
        for (SpLiederen.Regel r : l.regels()) {
            b.lied(vanaf + r.tick(), "lied." + r.key(), r.ticks());
        }
        SpLiederen.IN_SCENE.put(l.scene(), new SpLiederen.Start(lied, vanaf));
        return l.duur();
    }

    /** (Logout) forget the player. */
    static void vergeet(UUID speler) {
        MOMENT.remove(speler);
        DEUNTJE.remove(speler);
    }

    /** (Death, another dimension) the tune ends; the moment is the scene's business. */
    static void stilVoor(ServerPlayer p) {
        stopDeuntje(p);
    }
}
