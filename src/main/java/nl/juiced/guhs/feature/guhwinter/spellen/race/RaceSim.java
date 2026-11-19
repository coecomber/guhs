package nl.juiced.guhs.feature.guhwinter.spellen.race;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Doek;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Opname;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.PixelFont;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;

/**
 * De Pepernotenrace (Guhwinter 1.10.10): a Pakjesguh runs over the snowy rooftops of the winter plaza, seen from above, on
 * three snow paths. Chimneys only let you pass in another lane; a satellite dish or a snowman you can also jump over; a
 * washing line full of socks hangs across all three lanes, so you duck under it. Pepernoten lie on the paths; a
 * chocoladeletter pulls every pepernoot near you into your pocket for a while, and Ozovads (Sinterklaas' horse) carries
 * you for a few seconds: fast, and over everything.
 * <p>
 * Like the Guhkade games ({@link Sim}) it is an exact little simulation, 30 steps a second, that only follows from the seed
 * and the input of every step: the client plays it and sends the input, the server plays that again ({@link #speelAf})
 * and knows the real score. No Minecraft classes here.
 * <p>
 * Input per step (3 bits): 0 nothing, {@link #LINKS}, {@link #RECHTS}, {@link #SPRING}, {@link #BUK}.
 */
public final class RaceSim extends Sim {
    /** The phone screen in game pixels (portrait). */
    public static final int BREED = 120, HOOG = 200;
    public static final int LINKS = 1, RECHTS = 2, SPRING = 3, BUK = 4;
    /** The middle of the three lanes, and the line the runner's feet are on. */
    public static final int[] BAAN_X = {30, 60, 90};
    public static final int SPELER_Y = 170;
    /** One row of obstacles every RIJ pixels of the run; the first one after a little warming up. */
    static final int RIJ = 72, EERSTE_RIJ = 300;
    static final int SPRONG_DUUR = 20, BUK_DUUR = 20, MAGNEET_DUUR = 180, OZO_DUUR = 120, GENADE = 24;
    /** Pixels of the run per meter, and what a pepernoot adds to the score. */
    public static final int PX_PER_METER = 16, PEPERNOOT_PUNTEN = 5;

    public static final int GEEN = 0, SCHOORSTEEN = 1, SCHOTEL = 2, SNEEUWPOP = 3;
    public static final int PICK_GEEN = 0, PICK_MAGNEET = 1, PICK_OZOVADS = 2;
    /** How deep an obstacle is (along the run). */
    static final int[] DIEPTE = {0, 14, 10, 10};
    static final int DIEPTE_LIJN = 3;

    private static final int[] HOOGTE = new int[SPRONG_DUUR + 1];

    static {
        for (int i = 0; i <= SPRONG_DUUR; i++) {
            HOOGTE[i] = (int) Math.round(18 * StrictMath.sin(StrictMath.PI * i / SPRONG_DUUR));
        }
    }

    private float afstand, x = BAAN_X[1];
    private int baan = 1, sprong, buk, magneet, ozovads, genade, pepernoten;
    /** Taken things per row: bits 0..3 the pepernoten, bit 4 the pick-up. */
    private final Map<Integer, Integer> genomen = new HashMap<>();
    /** Row cache (rows are pure functions of the seed; this only saves the dice). */
    private final Map<Integer, Rij> rijen = new HashMap<>();

    public RaceSim(long seed) {
        super(seed);
    }

    @Override
    public int bits() {
        return 3;
    }

    @Override
    public int bot(int doel) {
        return 0;
    }

    public int pepernoten() {
        return pepernoten;
    }

    public int meters() {
        return (int) (afstand / PX_PER_METER);
    }

    public int magneet() {
        return magneet;
    }

    public int ozovads() {
        return ozovads;
    }

    /** Pixels a step: 2.4 at first, 5.2 after about two and a half minutes; Ozovads gallops 2 faster. */
    float snelheid() {
        return Math.min(5.2f, 2.4f + afstand * 0.00014f) + (ozovads > 0 ? 2f : 0f);
    }

    int hoogte() {
        return sprong > 0 ? HOOGTE[SPRONG_DUUR - sprong] : 0;
    }

    // --- the rows ----------------------------------------------------------------------------------------------------------

    /** One row of the rooftops: per lane an obstacle (or none), or a washing line across all three; pepernoten and a pick-up. */
    record Rij(int n, int[] soort, boolean waslijn, int nootBaan, int pickup, int pickBaan) {
        int w() {
            return EERSTE_RIJ + n * RIJ;
        }
    }

    private boolean lijnRauw(int n) {
        return n >= 5 && kans(n * 16L) < 0.12f;
    }

    Rij rij(int n) {
        Rij r = rijen.get(n);
        if (r == null) {
            r = maakRij(n);
            if (rijen.size() > 64) {
                rijen.clear();
            }
            rijen.put(n, r);
        }
        return r;
    }

    private Rij maakRij(int n) {
        int[] soort = new int[3];
        boolean lijn = lijnRauw(n) && !lijnRauw(n - 1);
        if (!lijn) {
            float twee = Math.min(0.55f, 0.15f + n * 0.01f);
            int aantal = kans(n * 16L + 1) < twee ? 2 : 1;
            int eerste = Math.min(2, (int) (kans(n * 16L + 2) * 3));
            int tweede = (eerste + 1 + Math.min(1, (int) (kans(n * 16L + 3) * 2))) % 3;
            for (int i = 0; i < aantal; i++) {
                float t = kans(n * 16L + 4 + i);
                soort[i == 0 ? eerste : tweede] = t < 0.36f ? SCHOORSTEEN : t < 0.68f ? SCHOTEL : SNEEUWPOP;
            }
        }
        int start = Math.min(2, (int) (kans(n * 16L + 8) * 3));
        int vrij = -1, vrij2 = -1;
        for (int i = 0; i < 3; i++) {
            int b = (start + i) % 3;
            if (soort[b] == GEEN) {
                if (vrij < 0) {
                    vrij = b;
                } else if (vrij2 < 0) {
                    vrij2 = b;
                }
            }
        }
        int nootBaan = kans(n * 16L + 6) < 0.45f ? vrij : -1;
        int pickup = PICK_GEEN, pickBaan = -1;
        float p = kans(n * 16L + 7);
        if (n > 3 && p < 0.05f) {
            pickup = PICK_MAGNEET;
        } else if (n > 8 && p < 0.08f) {
            pickup = PICK_OZOVADS;
        }
        if (pickup != PICK_GEEN) {
            pickBaan = vrij2 >= 0 ? vrij2 : vrij;
            if (pickBaan == nootBaan) {
                nootBaan = -1;
            }
        }
        return new Rij(n, soort, lijn, nootBaan, pickup, pickBaan);
    }

    static int nootW(Rij r, int i) {
        return r.w() + 18 + i * 12;
    }

    static int pickW(Rij r) {
        return r.w() + 36;
    }

    private boolean genomen(int n, int bit) {
        return (genomen.getOrDefault(n, 0) >> bit & 1) != 0;
    }

    private void neem(int n, int bit) {
        genomen.merge(n, 1 << bit, (a, b) -> a | b);
    }

    // --- one step ----------------------------------------------------------------------------------------------------------

    @Override
    protected void doeStap(int invoer) {
        switch (invoer) {
            case LINKS -> {
                if (baan > 0) {
                    baan--;
                    geluid |= GELUID_TIK;
                }
            }
            case RECHTS -> {
                if (baan < 2) {
                    baan++;
                    geluid |= GELUID_TIK;
                }
            }
            case SPRING -> {
                if (sprong == 0) {
                    sprong = SPRONG_DUUR;
                    buk = 0;
                    geluid |= GELUID_BONS;
                }
            }
            case BUK -> {
                if (sprong > 4) {
                    sprong = 4;            // (down at once, and duck when you land)
                }
                buk = BUK_DUUR;
            }
            default -> {
            }
        }
        float doelX = BAAN_X[baan];
        x += Math.max(-6f, Math.min(6f, doelX - x));
        if (sprong > 0) {
            sprong--;
        } else if (buk > 0) {
            buk--;
        }
        if (magneet > 0) {
            magneet--;
        }
        if (ozovads > 0) {
            ozovads--;
            if (ozovads == 0) {
                genade = GENADE;
            }
        } else if (genade > 0) {
            genade--;
        }
        afstand += snelheid();
        int n = (int) Math.floor((afstand - EERSTE_RIJ) / RIJ);
        for (int i = n - 1; i <= n + 1; i++) {
            if (i >= 0) {
                botsOfRaap(rij(i));
            }
        }
        if (stappen % 300 == 0) {
            Iterator<Integer> it = genomen.keySet().iterator();
            while (it.hasNext()) {
                if (it.next() < n - 2) {
                    it.remove();
                }
            }
        }
    }

    private boolean inBaan(int b, float marge) {
        return Math.abs(x - BAAN_X[b]) < marge;
    }

    private void botsOfRaap(Rij r) {
        float voor = afstand + 3, achter = afstand - 3;
        boolean veilig = ozovads > 0 || genade > 0;
        if (r.waslijn()) {
            if (r.w() <= voor && r.w() + DIEPTE_LIJN >= achter && !(buk > 0 && sprong == 0) && !veilig) {
                botst();
                return;
            }
        } else {
            for (int b = 0; b < 3; b++) {
                int s = r.soort()[b];
                if (s == GEEN || !(r.w() <= voor && r.w() + DIEPTE[s] >= achter) || !inBaan(b, 13f)) {
                    continue;
                }
                boolean over = s != SCHOORSTEEN && hoogte() >= 6;
                if (!over && !veilig) {
                    botst();
                    return;
                }
            }
        }
        if (r.nootBaan() >= 0) {
            for (int i = 0; i < 4; i++) {
                int w = nootW(r, i);
                if (genomen(r.n(), i)) {
                    continue;
                }
                boolean dichtbij = Math.abs(w - afstand) < 6;
                if (dichtbij && (inBaan(r.nootBaan(), 16f) || magneet > 0) || magneet > 0 && Math.abs(w - afstand) < 12) {
                    neem(r.n(), i);
                    pepernoten++;
                    score = punten();
                    geluid |= GELUID_PUNT;
                }
            }
        }
        if (r.pickup() != PICK_GEEN && !genomen(r.n(), 4) && Math.abs(pickW(r) - afstand) < 7 && inBaan(r.pickBaan(), 16f)) {
            neem(r.n(), 4);
            if (r.pickup() == PICK_MAGNEET) {
                magneet = MAGNEET_DUUR;
            } else {
                ozovads = OZO_DUUR;
                genade = 0;
                sprong = 0;
                buk = 0;
            }
            geluid |= GELUID_PUNT;
        }
        score = punten();
    }

    private int punten() {
        return meters() + pepernoten * PEPERNOOT_PUNTEN;
    }

    private void botst() {
        af = true;
        geluid |= GELUID_AF;
        score = punten();
    }

    // --- whole games -------------------------------------------------------------------------------------------------------

    /** How a run ended. */
    public record Uitkomst(int score, int stappen, int pepernoten, int meters) {
        public int seconden() {
            return stappen / HZ;
        }
    }

    /** Plays a recorded run again (the server's check): what it really gives. A recording cut short counts as far as it got. */
    public static Uitkomst speelAf(long seed, byte[] opname, int stappen) {
        RaceSim sim = new RaceSim(seed);
        int n = Math.min(Math.max(0, stappen), Math.min(MAX_STAPPEN, Opname.stappen(opname, sim.bits())));
        for (int i = 0; i < n && !sim.af(); i++) {
            sim.stap(Opname.lees(opname, sim.bits(), i));
        }
        return new Uitkomst(sim.score(), sim.stappen(), sim.pepernoten, sim.meters());
    }

    /**
     * Could this many steps really have been played in this much time (ms between the server hearing "start" and "klaar")?
     * 10% and two seconds of room for a slow connection; slower is always fine (a hiccup on the client).
     */
    public static boolean geloofwaardig(int stappen, long ms) {
        return stappen >= 0 && ms >= 0 && stappen <= ms * HZ / 1000.0 * 1.10 + HZ * 2;
    }

    // --- drawing -----------------------------------------------------------------------------------------------------------

    /** The roofs per house: tiles, the tiles' lines, and the gutter colour. */
    private static final int[][] DAKEN = {{0xFFB4553F, 0xFF8C3C2E}, {0xFF5B6478, 0xFF434A5C}, {0xFF9A6A4A, 0xFF744D35}, {0xFFA34A62, 0xFF7C3549}};
    private static final int SNEEUW = 0xFFF4F8FF, SNEEUW_SCHADUW = 0xFFC9D8F0, GOOT = 0xFF1C2238;
    private static final int HUIS = 168;

    private int sy(float w) {
        return Math.round(SPELER_Y - (w - afstand));
    }

    @Override
    public void teken(Doek d) {
        tekenDaken(d);
        int n0 = (int) Math.floor((afstand - EERSTE_RIJ - 40) / RIJ);
        int n1 = (int) Math.floor((afstand + SPELER_Y + 40 - EERSTE_RIJ) / RIJ);
        // far rows first (they are higher on the screen), so nearer things overlap them
        for (int n = n1; n >= Math.max(0, n0); n--) {
            Rij r = rij(n);
            if (r.nootBaan() >= 0) {
                for (int i = 0; i < 4; i++) {
                    if (!genomen(r.n(), i)) {
                        int nx = BAAN_X[r.nootBaan()], ny = sy(nootW(r, i));
                        if (magneet > 0 && Math.abs(nootW(r, i) - afstand) < 40) {
                            float t = 1f - Math.abs(nootW(r, i) - afstand) / 40f;   // (pulled towards you)
                            nx = Math.round(nx + (x - nx) * t * 0.6f);
                        }
                        Pix.teken(d, Pix.PEPERNOOT, nx - 2, ny - 4);
                    }
                }
            }
            if (r.pickup() != PICK_GEEN && !genomen(r.n(), 4)) {
                int px = BAAN_X[r.pickBaan()], py = sy(pickW(r));
                int dobber = (stappen / 6) % 2;
                if (r.pickup() == PICK_MAGNEET) {
                    Pix.teken(d, Pix.LETTER, px - 4, py - 11 - dobber);
                } else {
                    Pix.teken(d, Pix.HOEFIJZER, px - 4, py - 10 - dobber);
                }
                if ((stappen / 4) % 3 == 0) {
                    d.rect(px + 4, py - 13, 1, 1, 0xFFFFFFFF);
                    d.rect(px - 5, py - 6, 1, 1, 0xFFFFFFFF);
                }
            }
            if (r.waslijn()) {
                tekenWaslijn(d, sy(r.w()));
            } else {
                for (int b = 0; b < 3; b++) {
                    int s = r.soort()[b];
                    if (s != GEEN) {
                        tekenObstakel(d, s, BAAN_X[b], sy(r.w()));
                    }
                }
            }
        }
        tekenSpeler(d);
        tekenSneeuw(d);
        // HUD: score left, pepernoten right, the pick-ups' time under it
        PixelFont.teken(d, Integer.toString(score), 5, 6, 2, 0xFF2A1430);
        PixelFont.teken(d, Integer.toString(score), 4, 5, 2, 0xFFFFFFFF);
        String noten = Integer.toString(pepernoten);
        int nb = PixelFont.breedte(noten, 1);
        Pix.teken(d, Pix.PEPERNOOT, BREED - 10 - nb, 5);
        PixelFont.teken(d, noten, BREED - 3 - nb, 6, 1, 0xFF2A1430);
        PixelFont.teken(d, noten, BREED - 4 - nb, 5, 1, 0xFFFFE08A);
        int by = 18;
        if (magneet > 0) {
            Pix.teken(d, Pix.LETTER, 4, by);
            d.rect(14, by + 4, 30, 3, 0xFF3A2416);
            d.rect(14, by + 4, Math.round(30f * magneet / MAGNEET_DUUR), 3, 0xFFE8B04A);
            by += 12;
        }
        if (ozovads > 0) {
            Pix.teken(d, Pix.HOEFIJZER, 4, by);
            d.rect(14, by + 4, 30, 3, 0xFF3A2416);
            d.rect(14, by + 4, Math.round(30f * ozovads / OZO_DUUR), 3, 0xFFFFFFFF);
        }
    }

    private void tekenDaken(Doek d) {
        // the houses scroll by: each its own roof colour, a dark gutter between two houses
        int eersteHuis = (int) Math.floor((afstand - 40) / HUIS);
        for (int h = eersteHuis; h <= eersteHuis + 3; h++) {
            int onder = sy(h * (float) HUIS), boven = sy((h + 1) * (float) HUIS);
            int[] dak = DAKEN[Math.floorMod(h * 7 + (int) (seed & 3), DAKEN.length)];
            int y0 = Math.max(0, boven), y1 = Math.min(HOOG, onder);
            if (y1 <= y0) {
                continue;
            }
            d.rect(0, y0, BREED, y1 - y0, dak[0]);
            for (int yy = onder - 6; yy > boven; yy -= 6) {        // rows of roof tiles
                if (yy >= 0 && yy < HOOG) {
                    d.rect(0, yy, BREED, 1, dak[1]);
                    int schuif = ((onder - yy) / 6) % 2 == 0 ? 0 : 4;
                    for (int xx = schuif; xx < BREED; xx += 8) {
                        if (yy + 1 < HOOG) {
                            d.rect(xx, yy + 1, 1, Math.min(5, HOOG - yy - 1), dak[1]);
                        }
                    }
                }
            }
            if (onder - 4 >= 0 && onder - 4 < HOOG) {
                d.rect(0, Math.max(0, onder - 4), BREED, Math.min(4, HOOG - onder + 4), GOOT);
            }
        }
        // the three snow paths, with their soft blue edges and footprints
        for (int b = 0; b < 3; b++) {
            int bx = BAAN_X[b] - 12;
            d.rect(bx - 1, 0, 1, HOOG, SNEEUW_SCHADUW);
            d.rect(bx, 0, 24, HOOG, SNEEUW);
            d.rect(bx + 24, 0, 1, HOOG, SNEEUW_SCHADUW);
            int schuif = Math.floorMod(Math.round(afstand), 23);
            for (int yy = -23 + schuif; yy < HOOG; yy += 23) {
                d.rect(bx + 5 + (b * 5 % 9), yy, 2, 1, SNEEUW_SCHADUW);
                d.rect(bx + 15 - (b * 3 % 5), yy + 11, 2, 1, SNEEUW_SCHADUW);
            }
        }
        // the gutters cross the paths too (only the dark line)
        int eerste = (int) Math.floor((afstand - 40) / HUIS);
        for (int h = eerste; h <= eerste + 3; h++) {
            int onder = sy(h * (float) HUIS);
            if (onder - 4 >= 0 && onder < HOOG) {
                for (int b = 0; b < 3; b++) {
                    d.rect(BAAN_X[b] - 12, onder - 3, 24, 2, SNEEUW_SCHADUW);
                }
            }
        }
    }

    private void tekenObstakel(Doek d, int soort, int cx, int onder) {
        switch (soort) {
            case SCHOORSTEEN -> {
                int x0 = cx - 10, y0 = onder - 18;
                d.rect(x0, y0 + 4, 20, 14, 0xFFB5523B);
                for (int yy = y0 + 8; yy < onder; yy += 4) {
                    d.rect(x0, yy, 20, 1, 0xFF8A3A2A);
                    int s = ((yy - y0) / 4) % 2 == 0 ? 3 : 7;
                    for (int xx = x0 + s; xx < x0 + 20; xx += 8) {
                        d.rect(xx, yy + 1, 1, Math.min(3, onder - yy - 1), 0xFF8A3A2A);
                    }
                }
                d.rect(x0 - 1, y0 + 2, 22, 4, SNEEUW);
                d.rect(x0 + 3, y0 + 3, 14, 3, 0xFF2A1A1A);
                d.rect(x0 - 1, y0 + 1, 22, 1, SNEEUW);
                int puf = (stappen / 5) % 4;               // smoke puffs going up
                d.rect(cx - 3 + puf % 2, y0 - 4 - puf, 5, 4, 0xFFD6D6DE);
                d.rect(cx + 1 - puf % 2, y0 - 10 - puf, 4, 3, 0xFFE6E6EE);
            }
            case SCHOTEL -> {
                int y0 = onder - 13;
                d.rect(cx - 2, onder - 4, 4, 4, 0xFF59606C);
                d.rect(cx - 8, y0 + 2, 16, 7, 0xFFD8DEE6);
                d.rect(cx - 6, y0, 12, 11, 0xFFD8DEE6);
                d.rect(cx - 8, y0 + 2, 1, 7, 0xFF8C95A3);
                d.rect(cx + 7, y0 + 2, 1, 7, 0xFF8C95A3);
                d.rect(cx - 6, y0, 12, 1, 0xFF8C95A3);
                d.rect(cx - 4, y0 + 3, 8, 5, 0xFFEDF1F6);
                d.rect(cx, y0 - 3, 1, 8, 0xFF404652);
                d.rect(cx - 1, y0 - 4, 3, 2, 0xFFE0384C);
                d.rect(cx - 6, y0 - 1, 12, 1, SNEEUW);
            }
            case SNEEUWPOP -> {
                int y0 = onder - 22;
                d.rect(cx - 7, onder - 11, 14, 10, SNEEUW);
                d.rect(cx - 6, onder - 12, 12, 12, SNEEUW);
                d.rect(cx - 7, onder - 4, 14, 2, SNEEUW_SCHADUW);
                d.rect(cx - 5, y0 + 4, 10, 8, SNEEUW);
                d.rect(cx - 4, y0 + 3, 8, 10, SNEEUW);
                d.rect(cx - 5, y0 + 12, 10, 2, 0xFFE0384C);       // scarf
                d.rect(cx + 2, y0 + 13, 2, 4, 0xFFE0384C);
                d.rect(cx - 2, y0 + 6, 1, 1, 0xFF20202A);
                d.rect(cx + 2, y0 + 6, 1, 1, 0xFF20202A);
                d.rect(cx, y0 + 8, 1, 3, 0xFFF08A2A);              // carrot
                d.rect(cx - 4, y0 + 1, 8, 2, 0xFF20202A);          // hat
                d.rect(cx - 3, y0 - 3, 6, 4, 0xFF20202A);
                d.rect(cx - 1, onder - 9, 1, 1, 0xFF20202A);
                d.rect(cx - 1, onder - 6, 1, 1, 0xFF20202A);
            }
            default -> {
            }
        }
    }

    private static final int[] WAS = {0xFFE0384C, 0xFF4A7BE0, 0xFFFFD34E, 0xFF5CC46A, 0xFFFF9EC4, 0xFFFFFFFF};

    private void tekenWaslijn(Doek d, int onder) {
        int ly = onder - 12;
        d.rect(2, ly - 6, 3, 14, 0xFF7A5232);
        d.rect(BREED - 5, ly - 6, 3, 14, 0xFF7A5232);
        d.rect(4, ly, BREED - 8, 1, 0xFF3A3A44);
        int i = (int) (seed & 7);
        for (int xx = 10; xx < BREED - 12; xx += 13) {
            int c = WAS[i++ % WAS.length];
            int k = i % 3;
            if (k == 0) {                   // a sock
                d.rect(xx, ly + 1, 4, 6, c);
                d.rect(xx, ly + 6, 6, 2, c);
                d.rect(xx, ly + 1, 4, 1, 0xFFFFFFFF);
            } else if (k == 1) {            // a little shirt
                d.rect(xx - 1, ly + 1, 10, 3, c);
                d.rect(xx + 1, ly + 1, 6, 8, c);
            } else {                        // a pair of pyjama trousers
                d.rect(xx, ly + 1, 8, 3, c);
                d.rect(xx, ly + 3, 3, 6, c);
                d.rect(xx + 5, ly + 3, 3, 6, c);
                d.rect(xx, ly + 5, 8, 1, 0xFFFFFFFF);
            }
            d.rect(xx + 1, ly - 1, 1, 2, 0xFFB08A5A);       // the pegs
        }
    }

    private void tekenSpeler(Doek d) {
        int px = Math.round(x), h = hoogte();
        // the shadow stays on the roof
        d.rect(px - 6, SPELER_Y - 2, 12, 3, 0xFF9AAED0);
        if (ozovads > 0) {
            int galop = (stappen / 3) % 2;
            Pix.teken(d, Pix.OZOVADS, px - 8, SPELER_Y - 16 - galop);
            Pix.teken(d, Pix.GUH_A, px - 6, SPELER_Y - 26 - galop);
            return;
        }
        if (genade > 0 && (stappen / 2) % 2 == 0) {
            return;                         // (blinking: you can't be hit just yet)
        }
        if (af) {
            Pix.teken(d, Pix.GUH_PLOF, px - 6, SPELER_Y - 12);
        } else if (buk > 0 && sprong == 0) {
            Pix.teken(d, Pix.GUH_BUK, px - 6, SPELER_Y - 10);
        } else {
            boolean stapA = sprong > 0 || (stappen / 4) % 2 == 0;
            Pix.teken(d, stapA ? Pix.GUH_A : Pix.GUH_B, px - 6, SPELER_Y - 15 - h);
        }
    }

    private void tekenSneeuw(Doek d) {
        for (int i = 0; i < 18; i++) {
            float kx = kans(1_000_000L + i), ky = kans(2_000_000L + i);
            int sx = Math.floorMod((int) (kx * BREED + Math.sin(stappen * 0.05 + i) * 3), BREED);
            int sy = Math.floorMod((int) (ky * HOOG + stappen * (0.6f + (i % 3) * 0.3f)), HOOG);
            d.rect(sx, sy, i % 4 == 0 ? 2 : 1, i % 4 == 0 ? 2 : 1, 0xFFFFFFFF);
        }
    }

    /** The title picture: the roofs standing still, the Pakjesguh, the name and what to press. */
    public void tekenTitel(Doek d, String naam, String start, String regel2, boolean knipper) {
        teken(d);
        d.rect(8, 40, BREED - 16, 62, 0xF02A1430);
        d.rect(8, 40, BREED - 16, 1, 0xFFFFD27A);
        d.rect(8, 101, BREED - 16, 1, 0xFFFFD27A);
        PixelFont.midden(d, naam, BREED / 2, 48, 1, 0xFFFFD27A);
        Pix.teken(d, Pix.PEPERNOOT, BREED / 2 - 18, 62);
        Pix.teken(d, Pix.GUH_A, BREED / 2 - 6, 58);
        Pix.teken(d, Pix.PEPERNOOT, BREED / 2 + 13, 62);
        PixelFont.midden(d, regel2, BREED / 2, 78, 1, 0xFFFFE6EE);
        if (knipper) {
            PixelFont.midden(d, start, BREED / 2, 90, 1, 0xFFFFFFFF);
        }
    }

    /** Over the stopped game: AF (or RECORD) and the score; extra = a line under it (the pepernoten you got). */
    public static void tekenAf(Doek d, String kop, int score, String extra, boolean goud) {
        d.rect(10, 56, BREED - 20, 66, 0xF02A1430);
        d.rect(10, 56, BREED - 20, 1, goud ? 0xFFFFD27A : 0xFFFF9AC8);
        d.rect(10, 121, BREED - 20, 1, goud ? 0xFFFFD27A : 0xFFFF9AC8);
        PixelFont.midden(d, kop, BREED / 2, 63, 2, goud ? 0xFFFFD27A : 0xFFFF9AC8);
        PixelFont.midden(d, Integer.toString(score), BREED / 2, 82, 3, 0xFFFFFFFF);
        if (!extra.isEmpty()) {
            PixelFont.midden(d, extra, BREED / 2, 106, 1, 0xFFFFE08A);
        }
    }
}
