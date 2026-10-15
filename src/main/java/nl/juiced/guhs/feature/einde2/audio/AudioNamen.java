package nl.juiced.guhs.feature.einde2.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.einde2.Einde2Ids;

/**
 * einde2, slice audio: every sound name of CONTRACT_E2_B 6.1 (without the {@code einde2_audio.} prefix), grouped by kind.
 * tools/features/einde2_audio.py reads these lists from this file (its self-check fails when the two differ) and writes one
 * {@code sounds.json} entry and one subtitle per name; the files come from tools/features/einde2_audio_geluid.py.
 */
public final class AudioNamen {
    /** Background pieces (music category, 2-4 min): two per area. */
    public static final List<String> MUZIEK = List.of("lucht_1", "lucht_2", "water_1", "water_2", "aarde_1", "aarde_2", "vuur_1", "vuur_2",
            "geest_1", "geest_2");
    /** The epic moments under the big scenes ({@link E2Audio#moment} ids). */
    public static final List<String> MOMENTEN = List.of("intro", "appa", "appa_achtervolging", "golf", "yue_maan", "appa_verloren", "lu_ten",
            "val", "zwarte_zon", "tent", "luchtvloot", "agni_kai", "duel", "duel_koor", "kroning", "aftiteling");
    /** The generated tunes of the minigames and the tea house. */
    public static final List<String> DEUNTJES = List.of("theehuis", "thee_ting", "pai_sho", "luchtscooter", "momo", "slee", "monorail", "glijbaan",
            "dansfeest", "waaierdans", "ritmeduel");
    /** The generated joke and puzzle stingers. */
    public static final List<String> STINGERS = List.of("kolen", "toneel_fanfare", "feestje", "mijn_eer", "opgelost", "tegel");
    /** The generated sounds of the Geestenwereld. */
    public static final List<String> GEEST = List.of("koi_zang", "koh_kraak", "koh_stilte", "lantaarn_tinkel", "klok", "geestentij", "maannacht",
            "reiger");

    /** The five areas with music of their own (the value the server sends; "" = none). */
    public static final String LUCHT = "lucht", WATER = "water", AARDE = "aarde", VUUR = "vuur", GEESTENWERELD = "geest";
    public static final List<String> GEBIEDEN = List.of(LUCHT, WATER, AARDE, VUUR, GEESTENWERELD);

    /** The area of each story structure (its own element). */
    public static final Map<String, String> GEBIED_VAN_VERHAAL = Map.of(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, LUCHT,
            Einde2Ids.NOORDELIJKE_WATERSTAM, WATER, Einde2Ids.BA_SING_GUH, AARDE, Einde2Ids.VUURNATIE_HOOFDSTAD, VUUR);

    private AudioNamen() {
    }

    /** Every name of 6.1, with its kind: {@code muziek.lucht_1}, {@code moment.golf}, ... in table order. */
    public static List<String> alle() {
        List<String> alle = new ArrayList<>();
        MUZIEK.forEach(n -> alle.add("muziek." + n));
        MOMENTEN.forEach(n -> alle.add("moment." + n));
        DEUNTJES.forEach(n -> alle.add("deuntje." + n));
        STINGERS.forEach(n -> alle.add("stinger." + n));
        GEEST.forEach(n -> alle.add("geest." + n));
        return Collections.unmodifiableList(alle);
    }

    /** The area of an element (side places: {@code Einde2Ids.ELEMENT_VAN}). */
    public static String gebied(Einde2Ids.Element e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    /** The two background pieces of an area ({@code muziek.<gebied>_1}, {@code _2}); empty for "" or an unknown area. */
    public static List<String> muziekVan(@Nullable String gebied) {
        if (gebied == null || !GEBIEDEN.contains(gebied)) {
            return List.of();
        }
        return List.of("muziek." + gebied + "_1", "muziek." + gebied + "_2");
    }

    /** Which category a name plays in when this slice plays it (music and moments: the music slider). */
    public static SoundSource categorie(String naam) {
        if (naam.startsWith("muziek.") || naam.startsWith("moment.")) {
            return SoundSource.MUSIC;
        }
        if (naam.startsWith("geest.")) {
            return SoundSource.AMBIENT;
        }
        return SoundSource.RECORDS;
    }

    /** Is this a long piece the client plays as a moment (music category, fades, loops)? */
    public static boolean lang(String naam) {
        return naam.startsWith("muziek.") || naam.startsWith("moment.");
    }
}
