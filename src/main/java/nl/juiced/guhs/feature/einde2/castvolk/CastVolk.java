package nl.juiced.guhs.feature.einde2.castvolk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;

/**
 * einde2, slice cast_volk: the table of the slice's characters (CONTRACT_E2 11.2 H), one row per {@link GuhNpcEntity.Kind} of
 * the block {@code <einde2_cast_volk>}, in that order. A row says the character a look belongs to ({@code basis}), the
 * animation file it plays ({@code bestand}: the character's, except a statue, whose file holds still), the body ("zit" = the
 * sitting guh, "mika" = the Mika), the see-through and glow flags (drawn by the merge step's M2), its animations besides
 * {@code idle} as {@code naam:ticks} (0 ticks = a pose that lasts) and the bones of its mix sets ({@code |} between sets).
 * The python table {@code CAST} of tools/features/einde2_cast_volk_modellen.py holds the same rows, its check() compares.
 */
public enum CastVolk {
    GUHATSO(Kind.GUHATSO, Kind.GUHATSO, Kind.GUHATSO, "zit", true, true,
            "praat:24,gooi_taart:40,lach:30",
            ""),
    PAKGUH(Kind.PAKGUH, Kind.PAKGUH, Kind.PAKGUH, "zit", false, false,
            "praat:24,snor:40,waterzweep:30,neus_op:0",
            ""),
    BUMI_GUH(Kind.BUMI_GUH, Kind.BUMI_GUH, Kind.BUMI_GUH, "zit", false, false,
            "praat:24,giechel:30,spierbal:40,kristalsnoep:40",
            ""),
    AARDEKONING_GUH(Kind.AARDEKONING_GUH, Kind.AARDEKONING_GUH, Kind.AARDEKONING_GUH, "zit", false, false,
            "praat:24,bril:30,klap:30",
            ""),
    ARNOOK_GUH(Kind.ARNOOK_GUH, Kind.ARNOOK_GUH, Kind.ARNOOK_GUH, "zit", false, false,
            "praat:24,knik:24",
            ""),
    PIANDAO_GUH(Kind.PIANDAO_GUH, Kind.PIANDAO_GUH, Kind.PIANDAO_GUH, "zit", false, false,
            "praat:24,penseel:40,buig:30",
            ""),
    WERKTUIGGUH(Kind.WERKTUIGGUH, Kind.WERKTUIGGUH, Kind.WERKTUIGGUH, "zit", false, false,
            "praat:24,knutsel:40,eureka:30,nog_niet_af:0",
            ""),
    ROKU_GUH(Kind.ROKU_GUH, Kind.ROKU_GUH, Kind.ROKU_GUH, "zit", true, true,
            "praat:24,raad:40,verschijn:0",
            ""),
    ROKU_GUH_BEELD(Kind.ROKU_GUH_BEELD, Kind.ROKU_GUH, Kind.ROKU_GUH_BEELD, "zit", false, false,
            "praat:24,raad:40,verschijn:0",
            ""),
    KYOSHI_GUH(Kind.KYOSHI_GUH, Kind.KYOSHI_GUH, Kind.KYOSHI_GUH, "zit", true, true,
            "praat:24,waaier:30,verschijn:0",
            ""),
    KYOSHI_GUH_BEELD(Kind.KYOSHI_GUH_BEELD, Kind.KYOSHI_GUH, Kind.KYOSHI_GUH_BEELD, "zit", false, false,
            "praat:24,waaier:30,verschijn:0",
            ""),
    JOO_DEE_GUH(Kind.JOO_DEE_GUH, Kind.JOO_DEE_GUH, Kind.JOO_DEE_GUH, "zit", false, false,
            "praat:24,glimlach:30,herstart:40",
            ""),
    KOOLMAN(Kind.KOOLMAN, Kind.KOOLMAN, Kind.KOOLMAN, "zit", false, false,
            "praat:24,mijn_kolen:40,poets_kool:40",
            ""),
    LOKETGUH(Kind.LOKETGUH, Kind.LOKETGUH, Kind.LOKETGUH, "zit", false, false,
            "praat:24,stempel:30,nee:30",
            ""),
    KEI_GUH(Kind.KEI_GUH, Kind.KEI_GUH, Kind.KEI_GUH, "zit", false, false,
            "praat:24,spierbal:40,brul:40",
            ""),
    AARDROMMEL_OMROEPER(Kind.AARDROMMEL_OMROEPER, Kind.AARDROMMEL_OMROEPER, Kind.AARDROMMEL_OMROEPER, "zit", false, false,
            "praat:24,roep:40",
            ""),
    FANGUH(Kind.FANGUH, Kind.FANGUH, Kind.FANGUH, "zit", false, false,
            "praat:24,schuim:50,flauw:0",
            ""),
    LUCHTNON_GUH(Kind.LUCHTNON_GUH, Kind.LUCHTNON_GUH, Kind.LUCHTNON_GUH, "zit", false, false,
            "praat:24,fluit:60",
            ""),
    POSTGUH(Kind.POSTGUH, Kind.POSTGUH, Kind.POSTGUH, "zit", false, false,
            "praat:24,sorteer:40",
            ""),
    PROFESSOR_ZEI_GUH(Kind.PROFESSOR_ZEI_GUH, Kind.PROFESSOR_ZEI_GUH, Kind.PROFESSOR_ZEI_GUH, "zit", false, false,
            "praat:24,meet:40",
            ""),
    KYOSHI_DORPSOUDSTE(Kind.KYOSHI_DORPSOUDSTE, Kind.KYOSHI_DORPSOUDSTE, Kind.KYOSHI_DORPSOUDSTE, "zit", false, false,
            "praat:24,knik:24",
            ""),
    KYOSHI_KRIJGSTERGUH(Kind.KYOSHI_KRIJGSTERGUH, Kind.KYOSHI_KRIJGSTERGUH, Kind.KYOSHI_KRIJGSTERGUH, "zit", false, false,
            "praat:24,waaier:30,wacht:0",
            "mix_0_kroon mix_0_haar|mix_1_kroon mix_1_haar|mix_2_kroon mix_2_haar"),
    WATERSTAM_GUH(Kind.WATERSTAM_GUH, Kind.WATERSTAM_GUH, Kind.WATERSTAM_GUH, "zit", false, false,
            "praat:24,zwaai:30",
            "mix_0_jas mix_0_mouw_links mix_0_mouw_rechts mix_0_kap|mix_1_jas mix_1_mouw_links mix_1_mouw_rechts mix_1_kap mix_1_haar|mix_2_jas mix_2_mouw_links mix_2_mouw_rechts mix_2_haar mix_2_hengel|mix_3_jas mix_3_mouw_links mix_3_mouw_rechts mix_3_muts mix_3_want_links mix_3_want_rechts mix_3_koord"),
    WATERSTAM_WACHTER(Kind.WATERSTAM_WACHTER, Kind.WATERSTAM_WACHTER, Kind.WATERSTAM_WACHTER, "zit", false, false,
            "praat:24,halt:30",
            ""),
    BA_SING_GUH_BURGER(Kind.BA_SING_GUH_BURGER, Kind.BA_SING_GUH_BURGER, Kind.BA_SING_GUH_BURGER, "zit", false, false,
            "praat:24,zwaai:30,fluister:40",
            "mix_0_jas mix_0_mouw_links mix_0_mouw_rechts mix_0_hoed mix_0_mand|mix_1_jas mix_1_mouw_links mix_1_mouw_rechts mix_1_pet|mix_2_jas mix_2_mouw_links mix_2_mouw_rechts mix_2_haar|mix_3_jas mix_3_mouw_links mix_3_mouw_rechts mix_3_hoed mix_3_snor|mix_4_jas mix_4_mouw_links mix_4_mouw_rechts mix_4_haar mix_4_paraplu"),
    OMASHGUH_BURGER(Kind.OMASHGUH_BURGER, Kind.OMASHGUH_BURGER, Kind.OMASHGUH_BURGER, "zit", false, false,
            "praat:24,zwaai:30",
            "mix_0_jas mix_0_mouw_links mix_0_mouw_rechts mix_0_muts|mix_1_jas mix_1_mouw_links mix_1_mouw_rechts mix_1_muts mix_1_pakket|mix_2_jas mix_2_mouw_links mix_2_mouw_rechts mix_2_muts mix_2_baard"),
    KYOSHI_DORPSGUH(Kind.KYOSHI_DORPSGUH, Kind.KYOSHI_DORPSGUH, Kind.KYOSHI_DORPSGUH, "zit", false, false,
            "praat:24,zwaai:30",
            "mix_0_jas mix_0_mouw_links mix_0_mouw_rechts mix_0_hoed|mix_1_jas mix_1_mouw_links mix_1_mouw_rechts mix_1_haar mix_1_net|mix_2_jas mix_2_mouw_links mix_2_mouw_rechts mix_2_band mix_2_schort"),
    GEVANGENE_GUH(Kind.GEVANGENE_GUH, Kind.GEVANGENE_GUH, Kind.GEVANGENE_GUH, "zit", false, false,
            "praat:24,zucht:30,juich:30",
            "mix_0_haar|mix_1_haar|mix_2_doek mix_2_baard"),
    ZONNEKRIJGER_GUH(Kind.ZONNEKRIJGER_GUH, Kind.ZONNEKRIJGER_GUH, Kind.ZONNEKRIJGER_GUH, "zit", false, false,
            "praat:24,zonnegroet:40",
            ""),
    VUURWIJZE_MIKA(Kind.VUURWIJZE_MIKA, Kind.VUURWIJZE_MIKA, Kind.VUURWIJZE_MIKA, "mika", false, false,
            "praat:24,plechtig:30,betrapt:30",
            "mix_0_hoed mix_0_sik|mix_1_hoed mix_1_ketting"),
    VUURWIJZE_SHYU(Kind.VUURWIJZE_SHYU, Kind.VUURWIJZE_SHYU, Kind.VUURWIJZE_SHYU, "mika", false, false,
            "praat:24,wenk:30",
            ""),
    VUURNATIE_BURGER_MIKA(Kind.VUURNATIE_BURGER_MIKA, Kind.VUURNATIE_BURGER_MIKA, Kind.VUURNATIE_BURGER_MIKA, "mika", false, false,
            "praat:24,groet:24,vuurvlokken:50",
            "mix_0_muts mix_0_jas|mix_1_knot mix_1_jas mix_1_hamer|mix_2_pet mix_2_tas|mix_3_doek mix_3_lepel|mix_4_knot mix_4_sjaal"),
    VUURNATIE_JUF(Kind.VUURNATIE_JUF, Kind.VUURNATIE_JUF, Kind.VUURNATIE_JUF, "mika", false, false,
            "praat:24,tik:30",
            ""),
    VUURNATIE_LEERLING(Kind.VUURNATIE_LEERLING, Kind.VUURNATIE_LEERLING, Kind.VUURNATIE_LEERLING, "mika", false, false,
            "praat:24,hand_op:24,dans:40",
            "mix_0_knot|mix_1_knot mix_1_hoorn|mix_2_knotjes mix_2_trommel"),
    TONEELSPELER_GUH(Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, "zit", false, false,
            "praat:24,drama:40,buig:30",
            ""),
    TONEELSPELER_GUH_KATARA(Kind.TONEELSPELER_GUH_KATARA, Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, "zit", false, false,
            "praat:24,drama:40,buig:30",
            ""),
    TONEELSPELER_GUH_SOKKA(Kind.TONEELSPELER_GUH_SOKKA, Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, "zit", false, false,
            "praat:24,drama:40,buig:30",
            ""),
    TONEELSPELER_GUH_TOPH(Kind.TONEELSPELER_GUH_TOPH, Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, "zit", false, false,
            "praat:24,drama:40,buig:30",
            ""),
    TONEELSPELER_GUH_ZUKA(Kind.TONEELSPELER_GUH_ZUKA, Kind.TONEELSPELER_GUH, Kind.TONEELSPELER_GUH, "zit", false, false,
            "praat:24,drama:40,buig:30",
            "");

    /** The kind of this row. */
    public final Kind kind;
    /** The character this kind is a look of (itself for a base look). */
    public final Kind basis;
    /** The kind whose animation file this kind plays: geckolib/animations/entity/guh_npc_&lt;bestand&gt;.animation.json. */
    public final Kind bestand;
    private final String lijf;
    private final boolean doorzichtig;
    private final boolean gloed;
    private final List<String> animaties;
    private final Map<String, Integer> duur = new java.util.LinkedHashMap<>();
    private final List<List<String>> mix = new ArrayList<>();

    CastVolk(Kind kind, Kind basis, Kind bestand, String lijf, boolean doorzichtig, boolean gloed, String animaties, String mix) {
        this.kind = kind;
        this.basis = basis;
        this.bestand = bestand;
        this.lijf = lijf;
        this.doorzichtig = doorzichtig;
        this.gloed = gloed;
        List<String> namen = new ArrayList<>();
        for (String a : animaties.split(",")) {
            String[] d = a.split(":");
            namen.add(d[0]);
            this.duur.put(d[0], Integer.parseInt(d[1]));
        }
        this.animaties = List.copyOf(namen);
        if (!mix.isEmpty()) {
            for (String set : mix.split("\\|")) {
                this.mix.add(List.of(set.split(" ")));
            }
        }
    }

    private static final Map<Kind, CastVolk> RIJEN = new EnumMap<>(Kind.class);
    private static final List<Kind> KINDS;

    static {
        List<Kind> kinds = new ArrayList<>();
        for (CastVolk rij : values()) {
            RIJEN.put(rij.kind, rij);
            kinds.add(rij.kind);
        }
        KINDS = List.copyOf(kinds);
    }

    /** Every Kind of the slice, in the order of the Kind block. */
    public static List<Kind> kinds() {
        return KINDS;
    }

    /** The character a look belongs to (itself for a base look). */
    public static Kind basis(Kind kind) {
        return RIJEN.get(kind).basis;
    }

    /** "zit" or "mika" ("lang" is cast_schurk's). */
    public static String lijf(Kind kind) {
        return RIJEN.get(kind).lijf;
    }

    /** How many mix sets (0: none). */
    public static int mix(Kind kind) {
        return RIJEN.get(kind).mix.size();
    }

    /** The bones mix_&lt;i&gt;_... of set i. */
    public static List<String> mixBotten(Kind kind, int i) {
        return RIJEN.get(kind).mix.get(i);
    }

    public static boolean doorzichtig(Kind kind) {
        return RIJEN.get(kind).doorzichtig;
    }

    /** Then textures/entity/npc_&lt;kind&gt;_glowmask.png exists. */
    public static boolean gloed(Kind kind) {
        return RIJEN.get(kind).gloed;
    }

    /** The names besides idle; the first is always "praat". */
    public static List<String> animaties(Kind kind) {
        return RIJEN.get(kind).animaties;
    }

    /** Ticks; 0 = a pose that lasts (houding). An unknown name: -1. */
    public static int duur(Kind kind, String animatie) {
        return RIJEN.get(kind).duur.getOrDefault(animatie, -1);
    }
}
