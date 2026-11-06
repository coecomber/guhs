package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import nl.juiced.guhs.feature.vadsrijk.klassen.Balans;

/**
 * U-SPECS: the numbers of the spec effects in one place ({@link SpecEffecten} and the existing advancement code read them), and
 * the sustained single-target DPS model per spec and advancement step that the gametest compares (the two specs of a class must
 * stay within 10 % of each other). Units like {@link Balans#eenheden}: 1 = only basic attacks; ults once a minute; the
 * "kans/uptime" numbers are the assumptions of the model (how often a counter, a long shot or a frozen target happens).
 */
public final class SpecBalans {
    // ---- Schildridder ----------------------------------------------------------------------------------------------------------
    /** Dubbelklap (old step-1 effect, RidderMetBoom): the second swing of Kussenklap does this much. */
    public static final double DUBBELKLAP = 0.6;
    /** Tegenstoot: after a hit you caught with Knuffelschild, your next Kussenklap within {@link #TEGEN_TICKS} does +50 %. */
    public static final double TEGENSTOOT = 0.5, TEGEN_KANS = 0.5;
    public static final int TEGEN_TICKS = 80;
    /** Kristalwal (step 2, Advancement3): Kussenklap +40 % on frozen foes; model: frozen half the time. */
    public static final double KRISTALWAL = 0.4, BEVROREN_UPTIME = 0.5;
    /** Bolwerkplof (step 3): Kussenklap +15 %, Vahoege Plof +30 % and stuns 2 s. */
    public static final double BOLWERK_KK = 0.15, BOLWERK_PLOF = 0.3;
    // ---- Reuzenkussen ----------------------------------------------------------------------------------------------------------
    /** Reuzenzwaai (step 1): basic melee hits +30 % and sweep 50 % to foes within 3 blocks; Kussenklap +60 % but 1 s slower. */
    public static final double REUS_BASIS = 0.3, REUS_ZWAAI = 0.5, REUS_ZWAAI_STRAAL = 3, REUS_KK = 0.6;
    public static final int REUS_KK_TRAGER = 20;
    /** Kussenkoprol (step 2): Stormsprong +60 %, 2 s faster, stuns 1 s. */
    public static final double KOPROL = 0.6;
    public static final int KOPROL_SNELLER = 40;
    /** Nestvorm (step 3, Advancement4): Vahoege Plof +30 % + the pull; and basic hits another +10 %. */
    public static final double NESTVORM = 0.3, REUS_BASIS3 = 0.1;
    // ---- Scherpschutter --------------------------------------------------------------------------------------------------------
    /** Valkenblik (step 1): +8 % crit chance, +15 % crit damage (in the model: +9 % on everything). */
    public static final double VALK_KRIT = 8, VALK_KRITSCHADE = 15, VALK_MODEL = 0.09;
    /** Verre schoten (step 1): basic shots on foes further than {@link #VER_AFSTAND} blocks +30 %; model: 60 % of the shots. */
    public static final double VER = 0.3, VER_KANS = 0.6, VER_AFSTAND = 10;
    /** Opgeladen pijl (step 2): Knuffelpijl +80 % on foes. */
    public static final double OPGELADEN = 0.8;
    /** Kruinvalk (step 3, Advancement4): Sterrenschot +40 % and a quarter again as true damage; basic shots +8 %. */
    public static final double KRUINVALK = 0.4, KRUINVALK_WAAR = 0.25, SCHERP_BASIS3 = 0.08;
    // ---- Strikkenzetter --------------------------------------------------------------------------------------------------------
    /** Saltosalvo (step 1, Pluisjager): 3 arrows of {@link Balans#SALVO} per Rolsalto; quick bow +8 % attack speed. */
    public static final double STRIK_SNEL = 8;
    /** Strikje (step 1): Rolsalto leaves a trap (6 s, radius 2.5): Plakkerig 3 s + Gesporen 4 s at 15 % weapon damage a second. */
    public static final double STRIK_DOT = 0.15, STRIK_STRAAL = 2.5;
    public static final int STRIK_DUUR = 120, STRIK_DOT_TICKS = 80;
    /** IJsster (step 2, Advancement3): every 5th basic shot an extra 1x shot. */
    public static final int IJSSTER_ELKE = 5;
    /** Strikkenregen (step 3): Sterrenschot leaves a trap field (5 s, radius 4, 40 % a second); +8 % more attack speed. */
    public static final double STRIKREGEN = 0.4, STRIKREGEN_STRAAL = 4, STRIK_SNEL3 = 8;
    // ---- Sterrenwever ----------------------------------------------------------------------------------------------------------
    /** The element puddle (step 1, Vadsmagier): 3 ticks of 17.5 % of the Vuurbal; Vadsoverloop: +10 % CDR, +8 % element damage. */
    public static final double PLAS = 3 * 0.175, OVERLOOP_CDR = 10, OVERLOOP_ELEMENT = 8;
    /** Spiegelecho (step 2, Advancement3): 1.2x when the Vuurbal hits a frozen foe; model: 30 % of the casts. */
    public static final double ECHO = 1.2, ECHO_KANS = 0.3;
    /** Schemerwijze (step 3, Advancement4): the fire field (6 x 30 %) after Meteoorguh; +8 % element damage. */
    public static final double VUURVELD = 6 * 0.3, STERREN_ELEMENT3 = 8;
    // ---- Zegenzanger -----------------------------------------------------------------------------------------------------------
    /** Strijdlied (step 1): you +15 % damage, your party within {@link #LIED_STRAAL} +8 %; a song pulse every 2 s: 50 % in 6 blocks. */
    public static final double LIED_ZELF = 0.15, LIED_GROEP = 0.08, LIED_STRAAL = 10, PULS = 0.5, PULS_STRAAL = 6;
    public static final int PULS_TICKS = 40;
    /** Zangkring (step 2): for 6 s after Vadsbron, foes in it take 40 % a second. */
    public static final double ZANGKRING = 0.4;
    public static final int ZANGKRING_SEC = 6, VADSBRON_CD_SEC = 14;
    /** Slotakkoord (step 3): Meteoorguh +30 % and your party 8 s Vahoeg (+20 %). */
    public static final double SLOTAKKOORD = 0.3, SLOT_VAHOEG = 0.2;
    public static final int SLOT_TICKS = 160;
    /** The tree keystone Slotlied: the song +5 % and the pulse 2 blocks further. */
    public static final double SLOTLIED = 0.05;

    /** The model: one ult per this many seconds. */
    static final double ULT_PER = 60;

    private SpecBalans() {
    }

    /** Extra DPS units of a spec at advancement step 1..3 (cumulative), on top of {@link Balans#eenheden}. */
    public static double extra(Spec s, int stap) {
        String k = s.klasse;
        double e = Balans.eenheden(k);
        double kk = Balans.KUSSENKLAP / 3.0;   // Kussenklap: 1.5x every 3 s
        double vb = Balans.VUURBAL / (Balans.VUURBAL_CD / 20.0);
        double x = 0;
        switch (s) {
            case SCHILDRIDDER -> {
                x += DUBBELKLAP * kk + TEGENSTOOT * kk * TEGEN_KANS;
                if (stap >= 2) {
                    x += KRISTALWAL * kk * (1 + DUBBELKLAP) * BEVROREN_UPTIME;
                }
                if (stap >= 3) {
                    x += BOLWERK_KK * kk * (1 + DUBBELKLAP) + Balans.VAHOEGE_PLOF * BOLWERK_PLOF / ULT_PER;
                }
            }
            case REUZENKUSSEN -> {
                x += REUS_BASIS + Balans.KUSSENKLAP * (1 + REUS_KK) / (3 + REUS_KK_TRAGER / 20.0) - kk;
                if (stap >= 2) {
                    x += Balans.STORMSPRONG * (1 + KOPROL) / (8 - KOPROL_SNELLER / 20.0) - Balans.STORMSPRONG / 8.0;
                }
                if (stap >= 3) {
                    x += REUS_BASIS3 + Balans.VAHOEGE_PLOF * NESTVORM / ULT_PER;
                }
            }
            case SCHERPSCHUTTER -> {
                x += VALK_MODEL * e + VER * VER_KANS;
                if (stap >= 2) {
                    x += Balans.KNUFFELPIJL * OPGELADEN / 7.0;
                }
                if (stap >= 3) {
                    x += Balans.STERRENSCHOT * ((1 + KRUINVALK) * (1 + KRUINVALK_WAAR) - 1) / ULT_PER + SCHERP_BASIS3;
                }
            }
            case STRIKKENZETTER -> {
                x += 3 * Balans.SALVO / 8.0 + STRIK_SNEL / 100.0 + STRIK_DOT * (STRIK_DOT_TICKS / 20.0) / 8.0;
                if (stap >= 2) {
                    x += (1.0 / IJSSTER_ELKE) * (1 + STRIK_SNEL / 100.0);
                }
                if (stap >= 3) {
                    x += STRIKREGEN * 5 / ULT_PER + STRIK_SNEL3 / 100.0 * (1 + STRIK_SNEL / 100.0);
                }
            }
            case STERRENWEVER -> {
                x += PLAS * vb + vb * (1 / (1 - OVERLOOP_CDR / 100.0) - 1) + OVERLOOP_ELEMENT / 100.0 * e;
                if (stap >= 2) {
                    x += ECHO * ECHO_KANS / (Balans.VUURBAL_CD / 20.0);
                }
                if (stap >= 3) {
                    x += VUURVELD / ULT_PER + STERREN_ELEMENT3 / 100.0 * e;
                }
            }
            case ZEGENZANGER -> {
                x += LIED_ZELF * e + PULS / (PULS_TICKS / 20.0);
                if (stap >= 2) {
                    x += ZANGKRING * ZANGKRING_SEC / VADSBRON_CD_SEC;
                }
                if (stap >= 3) {
                    x += SLOT_VAHOEG * (SLOT_TICKS / 20.0) / ULT_PER * e + Balans.METEOOR * SLOTAKKOORD / ULT_PER;
                }
            }
        }
        return x;
    }

    /** The DPS factor of a spec at a step: (base units + extra) / base units. */
    public static double factor(Spec s, int stap) {
        double e = Balans.eenheden(s.klasse);
        return stap <= 0 ? 1 : (e + extra(s, stap)) / e;
    }
}
