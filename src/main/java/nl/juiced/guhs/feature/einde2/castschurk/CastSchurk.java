package nl.juiced.guhs.feature.einde2.castschurk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;

/**
 * einde2, slice cast_schurk: the table of its {@link GuhNpcEntity.Kind}s (CONTRACT_E2 11.2 H, 11.6), one row per kind, in the
 * order of the Kind block: the character a look belongs to, its body ("lijf": {@code mika} or the tall {@code lang}), how many
 * mix sets, the two render flags, and its animations besides {@code idle} with their length in ticks (0: a pose that lasts).
 * tools/features/einde2_cast_schurk_modellen.py holds the same table as {@code CAST} and its check compares the two.
 * Models: {@code geckolib/models/entity/guh_npc_<kind>.geo.json}, textures {@code textures/entity/npc_<kind>.png}, one
 * animation file per character {@code geckolib/animations/entity/guh_npc_<basis>.animation.json}.
 */
public enum CastSchurk {
    VUURHEER_MIKAI(Kind.VUURHEER_MIKAI, "lang", 0, false, false, "praat:24", "brul:32", "vuurstoot:28", "troon:0", "komeet:0"),
    VUURHEER_MIKAI_KOMEET(Kind.VUURHEER_MIKAI, "lang", 0, false, true, "praat:24", "brul:32", "vuurstoot:28", "troon:0", "komeet:0"),
    VUURHEER_MIKAI_VADSIG(Kind.VUURHEER_MIKAI, "mika", 0, false, false, "praat:24", "smul:40", "boer:24", "zwaai:32"),
    AZULA_MIKA(Kind.AZULA_MIKA, "mika", 0, false, true, "praat:24", "bliksem:40", "grijns:32", "lok:48"),
    TY_LEE_MIKA(Kind.TY_LEE_MIKA, "mika", 0, false, false, "praat:24", "radslag:28", "prik:20", "handstand:0"),
    ZHAO_MIKA(Kind.ZHAO_MIKA, "mika", 0, false, false, "praat:24", "opschep:40", "woede:36"),
    LONG_FENG_MIKA(Kind.LONG_FENG_MIKA, "mika", 0, false, false, "praat:24", "buig:48", "fluister:40"),
    DAI_LI_MIKA(Kind.DAI_LI_MIKA, "mika", 0, false, false, "praat:24", "stenen_hand:24", "verschijn:0"),
    VUURNATIE_MIKA(Kind.VUURNATIE_MIKA, "mika", 3, false, false, "praat:24", "wacht:80", "duw:24", "salueer:32"),
    GEVANGENIS_DIRECTEUR_MIKA(Kind.GEVANGENIS_DIRECTEUR_MIKA, "mika", 0, false, false, "praat:24", "tier:40", "sleutels:48"),
    MIKA_KOK(Kind.MIKA_KOK, "mika", 0, false, false, "praat:24", "roer:40", "spijt:48"),
    VESTINGKAPITEIN_MIKA(Kind.VESTINGKAPITEIN_MIKA, "mika", 0, false, false, "praat:24", "tuur:60", "brul:32");

    /** The bones of each mix set of the crowd kind (set i: every bone named mix_&lt;i&gt;_...): the soldier, the guard, the marine. */
    private static final List<List<String>> MIX_VUURNATIE = List.of(List.of("mix_0_helm", "mix_0_masker"), List.of("mix_1_helm", "mix_1_wangen"),
            List.of("mix_2_helm", "mix_2_sjaal"));

    public final Kind kind;
    private final Kind basis;
    private final String lijf;
    private final int mix;
    private final boolean doorzichtig, gloed;
    private final List<String> animaties;
    private final int[] duren;

    CastSchurk(Kind basis, String lijf, int mix, boolean doorzichtig, boolean gloed, String... animaties) {
        this.kind = Kind.valueOf(name());
        this.basis = basis;
        this.lijf = lijf;
        this.mix = mix;
        this.doorzichtig = doorzichtig;
        this.gloed = gloed;
        this.animaties = Arrays.stream(animaties).map(a -> a.substring(0, a.indexOf(':'))).toList();
        this.duren = Arrays.stream(animaties).mapToInt(a -> Integer.parseInt(a.substring(a.indexOf(':') + 1))).toArray();
    }

    @Nullable
    public static CastSchurk rij(Kind kind) {
        for (CastSchurk r : values()) {
            if (r.kind == kind) {
                return r;
            }
        }
        return null;
    }

    private static CastSchurk moet(Kind kind) {
        CastSchurk r = rij(kind);
        if (r == null) {
            throw new IllegalArgumentException(kind + " is not a kind of cast_schurk");
        }
        return r;
    }

    /** Every Kind of the slice, in the order of the Kind block. */
    public static List<Kind> kinds() {
        List<Kind> out = new ArrayList<>();
        for (CastSchurk r : values()) {
            out.add(r.kind);
        }
        return out;
    }

    /** The character a look belongs to (itself for a base look). */
    public static Kind basis(Kind kind) {
        return moet(kind).basis;
    }

    /** "zit", "mika" or "lang". */
    public static String lijf(Kind kind) {
        return moet(kind).lijf;
    }

    /** How many mix sets (0: none). */
    public static int mix(Kind kind) {
        return moet(kind).mix;
    }

    /** The bones mix_&lt;i&gt;_... of set i. */
    public static List<String> mixBotten(Kind kind, int i) {
        CastSchurk r = moet(kind);
        if (i < 0 || i >= r.mix) {
            return List.of();
        }
        return r == VUURNATIE_MIKA ? MIX_VUURNATIE.get(i) : List.of();
    }

    public static boolean doorzichtig(Kind kind) {
        return moet(kind).doorzichtig;
    }

    /** Then npc_&lt;kind&gt;_glowmask.png exists. */
    public static boolean gloed(Kind kind) {
        return moet(kind).gloed;
    }

    /** The names besides idle; the first is always "praat". */
    public static List<String> animaties(Kind kind) {
        return moet(kind).animaties;
    }

    /** Ticks; 0 = a pose that lasts (houding); -1: the kind has no such animation. */
    public static int duur(Kind kind, String animatie) {
        CastSchurk r = moet(kind);
        int i = r.animaties.indexOf(animatie);
        return i < 0 ? -1 : r.duren[i];
    }
}
