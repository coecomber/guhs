package nl.juiced.guhs.feature.erfenis.kaart;

import javax.annotation.Nullable;

/**
 * De Erfenis, wereldkaart: the buildings an NPC village is made of, with the 1.9 ids and footprints of the contract
 * (CONTRACT_OPKNAPPER.md) and the counts per Hoeve level of balans.md 2.3. Levels follow the lead's override: every building
 * has 10 levels (the Mammoetwei 3) and may never be higher than the Hoeve. Knusheid: 2 per building level, the Hoeve 10 per
 * level (deco.md). The ids are the core's {@code GebouwType} ids, so a plan maps 1:1 onto {@code Base.Plan}
 * ({@link KaartBronErfenis#plan}). Arrays: index = Hoeve level (0 unused).
 */
public enum KaartGebouwType {
    HOEVE("hoeve", 11, 11, a(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
    MARKTPLEIN("marktplein", 9, 9, a(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
    GUHHUTJE("guhhutje", 5, 5, a(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
    KIPPENHOK("kippenhok", 8, 10, a(1, 1, 1, 2, 2, 2, 3, 3, 3, 3)),
    AKKER("akker", 8, 8, a(1, 2, 2, 3, 3, 4, 4, 5, 5, 6)),
    SCHAAPSKOOI("schaapskooi", 10, 12, a(1, 1, 1, 2, 2, 2, 2, 3, 3, 3)),
    OPSLAGSCHUUR("opslagschuur", 7, 7, a(1, 1, 1, 2, 2, 2, 3, 3, 3, 3)),
    BOSJE("bosje", 10, 10, a(1, 1, 1, 1, 2, 2, 2, 2, 2, 2)),
    HOUTZAGERIJ("houtzagerij", 7, 7, a(1, 1, 1, 1, 1, 2, 2, 2, 2, 2)),
    TIMMERSCHUUR("timmerschuur", 6, 6, a(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)),
    MOLEN("molen", 7, 7, a(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)),
    BAKKERIJ("bakkerij", 7, 7, a(0, 0, 1, 1, 1, 1, 1, 2, 2, 2)),
    KOEIENSTAL("koeienstal", 12, 12, a(0, 0, 1, 1, 1, 2, 2, 2, 2, 2)),
    KONIJNENREN("konijnenren", 8, 8, a(0, 0, 1, 1, 1, 2, 2, 2, 2, 2)),
    EENDENVIJVER("eendenvijver", 10, 10, a(0, 0, 1, 1, 1, 1, 2, 2, 2, 2)),
    WEVERIJ("weverij", 7, 7, a(0, 0, 1, 1, 1, 1, 2, 2, 2, 2)),
    GROEVE("groeve", 10, 10, a(0, 0, 1, 1, 1, 2, 2, 2, 2, 2)),
    STEENBAKKERIJ("steenbakkerij", 7, 7, a(0, 0, 1, 1, 1, 1, 2, 2, 2, 2)),
    SPEELTUIN("speeltuin", 8, 8, a(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)),
    GUHPENSION("guhpension", 8, 8, a(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)),
    DETECTIVEBUREAU("detectivebureau", 6, 6, a(0, 0, 1, 1, 1, 1, 1, 1, 1, 1)),
    BIJENHOF("bijenhof", 8, 8, a(0, 0, 0, 0, 1, 1, 1, 2, 2, 2)),
    BOOMGAARD("boomgaard", 10, 10, a(0, 0, 0, 0, 1, 1, 1, 2, 2, 2)),
    KAASMAKERIJ("kaasmakerij", 7, 7, a(0, 0, 0, 0, 1, 1, 1, 1, 1, 1)),
    ZANDKUIL("zandkuil", 8, 8, a(0, 0, 0, 0, 1, 1, 1, 2, 2, 2)),
    GLASBLAZERIJ("glasblazerij", 7, 7, a(0, 0, 0, 0, 1, 1, 1, 1, 2, 2)),
    BOUWBLOKKENWERKPLAATS("bouwblokkenwerkplaats", 9, 9, a(0, 0, 0, 0, 1, 1, 1, 1, 1, 2)),
    DORPSCAFE("dorpscafe", 8, 8, a(0, 0, 0, 0, 1, 1, 1, 1, 1, 1)),
    MUZIEKTENT("muziektent", 9, 9, a(0, 0, 0, 0, 1, 1, 1, 1, 1, 1)),
    LOKET("loket", 5, 5, a(0, 0, 0, 0, 0, 1, 1, 1, 1, 1)),
    VADSMAST("vadsmast", 4, 4, a(0, 0, 0, 0, 0, 0, 1, 1, 1, 1)),
    KAARSENMAKERIJ("kaarsenmakerij", 6, 6, a(0, 0, 0, 0, 0, 0, 1, 1, 1, 1)),
    MAMMOETWEI("mammoetwei", 16, 16, a(0, 0, 0, 0, 0, 0, 0, 0, 1, 1));

    public static final int MAX_HOEVE = 10;

    public final String id;
    /** Footprint before rotation (x by z). */
    public final int breedte, diepte;
    private final int[] aantal;

    KaartGebouwType(String id, int breedte, int diepte, int[] aantal) {
        this.id = id;
        this.breedte = breedte;
        this.diepte = diepte;
        this.aantal = aantal;
    }

    private static int[] a(int... perHoeve) {
        int[] r = new int[MAX_HOEVE + 1];
        System.arraycopy(perHoeve, 0, r, 1, MAX_HOEVE);
        return r;
    }

    public boolean vierkant() {
        return breedte == diepte;
    }

    /** The highest level this type has at all (10; the Mammoetwei 3). */
    public int topLevel() {
        return this == MAMMOETWEI ? 3 : 10;
    }

    public int maxAantal(int hoeve) {
        return aantal[clamp(hoeve)];
    }

    /** Max level at this Hoeve level: the Hoeve level itself (the Hoeve: its top level). */
    public int maxLevel(int hoeve) {
        return this == HOEVE ? topLevel() : Math.min(topLevel(), clamp(hoeve));
    }

    /** Knusheid at this level: 2 per level, the Hoeve 10 per level. */
    public int knus(int level) {
        return level <= 0 ? 0 : (this == HOEVE ? 10 : 2) * level;
    }

    public String naamKey() {
        return "erfenis.kaart.gebouw." + id;
    }

    @Nullable
    public static KaartGebouwType van(String id) {
        for (KaartGebouwType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    static int clamp(int h) {
        return Math.max(1, Math.min(MAX_HOEVE, h));
    }
}
