package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

import java.util.ArrayList;
import java.util.List;

/**
 * Flappy Guh: a guh with tiny wings between kaasknabbel pillars. One button: flap. Every pillar you pass is a point; the
 * gaps get a little smaller and the pillars come a little faster until about pillar 28. Touching a pillar or the floor ends the
 * game (the guh just plops down and looks dizzy: nothing hurts).
 * <p>
 * Input: 1 = flap in this step.
 */
public final class FlappySim extends Sim {
    /** The floor (the guh's feet may not get there). */
    public static final int VLOER = 110;
    /** The guh: fixed x, a hit box of 9 x 8 around its middle. */
    static final int GUH_X = 44, GUH_L = 4, GUH_R = 5, GUH_B = 4;
    static final int PILAAR_B = 22, EERSTE = 170, AFSTAND = 74;
    static final float ZWAARTE = 0.22f, FLAP = -2.9f, MAX_VAL = 4.2f;

    private float y = 52, vy;
    /** How far the world has scrolled. */
    private float afstand;
    private final List<Integer> gaten = new ArrayList<>();
    private int vleugel;

    public FlappySim(long seed) {
        super(seed);
    }

    @Override
    public int bits() {
        return 1;
    }

    /** The middle of the gap of pillar i (each at most 28 pixels higher or lower than the one before). */
    int gat(int i) {
        while (gaten.size() <= i) {
            int n = gaten.size();
            int vorig = n == 0 ? 55 : gaten.get(n - 1);
            int nieuw = vorig + Math.round((kans(n) * 2f - 1f) * 28f);
            gaten.add(Math.max(30, Math.min(80, nieuw)));
        }
        return gaten.get(i);
    }

    /** The height of the gap of pillar i: 52 pixels at first, 42 from pillar 25 on. */
    static int opening(int i) {
        return Math.max(42, 52 - (i * 2) / 5);
    }

    /** Pixels a step: 1.5 at first, 2.2 from pillar 28 on. */
    float snelheid() {
        return Math.min(2.2f, 1.5f + score * 0.025f);
    }

    /** The screen x of the left side of pillar i. */
    private float pilaarX(int i) {
        return EERSTE + i * AFSTAND - afstand;
    }

    @Override
    protected void doeStap(int invoer) {
        boolean flap = (invoer & 1) != 0;
        if (flap) {
            vleugel = 6;
            geluid |= GELUID_TIK;
        } else if (vleugel > 0) {
            vleugel--;
        }
        int voor = score;
        Toestand t = new Toestand(y, vy, afstand, score);
        boolean heel = t.stap(flap);
        y = t.y;
        vy = t.vy;
        afstand = t.afstand;
        score = t.score;
        if (score > voor) {
            geluid |= GELUID_PUNT;
        }
        if (!heel) {
            af = true;
            geluid |= GELUID_AF;
        }
    }

    /** The moving part of the game, apart, so the guh that plays can think a second ahead ({@link #bot}). */
    private final class Toestand {
        float y, vy, afstand;
        int score;

        Toestand(float y, float vy, float afstand, int score) {
            this.y = y;
            this.vy = vy;
            this.afstand = afstand;
            this.score = score;
        }

        /** One step: false = bumped into a pillar or the floor. */
        boolean stap(boolean flap) {
            if (flap) {
                vy = FLAP;
            }
            vy = Math.min(MAX_VAL, vy + ZWAARTE);
            y += vy;
            if (y < GUH_B) {            // (the top of the screen is a soft ceiling)
                y = GUH_B;
                vy = 0;
            }
            afstand += Math.min(2.2f, 1.5f + score * 0.025f);
            boolean heel = true;
            float px = EERSTE + score * AFSTAND - afstand;
            if (px + PILAAR_B < GUH_X - GUH_L) {          // passed it
                score++;
            } else if (px < GUH_X + GUH_R && px + PILAAR_B > GUH_X - GUH_L) {
                int midden = gat(score), half = opening(score) / 2;
                if (y - GUH_B < midden - half || y + GUH_B > midden + half) {
                    heel = false;
                }
            }
            if (y + GUH_B >= VLOER) {
                y = VLOER - GUH_B;
                heel = false;
            }
            return heel;
        }
    }

    /** How far the playing guh thinks ahead (steps), and how much thinking it does at most. */
    private static final int VOORUIT = 48, DENKWERK = 20000;
    private int denkwerk;
    /** Dead ends found while thinking about this step (step ahead, height and speed, rounded). */
    private final java.util.HashSet<Long> doodlopend = new java.util.HashSet<>();

    @Override
    public int bot(int doel) {
        if (score >= doel) {
            return 0;               // (oops, forgot to flap: njeg)
        }
        boolean wens = wens(y, vy, score);
        denkwerk = 0;
        doodlopend.clear();
        if (haalbaar(new Toestand(y, vy, afstand, score), wens, VOORUIT)) {
            return wens ? 1 : 0;
        }
        if (haalbaar(new Toestand(y, vy, afstand, score), !wens, VOORUIT)) {
            return wens ? 0 : 1;
        }
        return wens ? 1 : 0;
    }

    /** The simple plan: stay in the lower part of the next gap. */
    private boolean wens(float y, float vy, int score) {
        float bodem = gat(score) + opening(score) / 2f - 9f;
        return y + vy > bodem && vy > -1.2f;
    }

    /** Does the guh get through the next {@code diepte} steps when it does this now (and then whatever works)? */
    private boolean haalbaar(Toestand t, boolean flap, int diepte) {
        if (!t.stap(flap)) {
            return false;
        }
        if (diepte <= 0 || ++denkwerk > DENKWERK) {
            return true;
        }
        long sleutel = ((long) diepte << 32) | ((long) Math.round(t.y * 4f) << 8) | Math.round((t.vy + 3f) * 8f);
        if (doodlopend.contains(sleutel)) {
            return false;
        }
        float y = t.y, vy = t.vy, afstand = t.afstand;
        int score = t.score;
        boolean wens = wens(y, vy, score);
        if (haalbaar(t, wens, diepte - 1)) {
            return true;
        }
        t.y = y;
        t.vy = vy;
        t.afstand = afstand;
        t.score = score;
        if (denkwerk <= DENKWERK && haalbaar(t, !wens, diepte - 1)) {
            return true;
        }
        doodlopend.add(sleutel);
        return false;
    }

    @Override
    public void teken(Doek d) {
        // the sky, lighter towards the floor
        d.rect(0, 0, B, 44, 0xFF8FD0FF);
        d.rect(0, 44, B, 36, 0xFFA9DCFF);
        d.rect(0, 80, B, VLOER - 80, 0xFFC6E9FF);
        for (int n = 0; n < 3; n++) {       // clouds drifting by slowly
            int x = (int) (((n * 67 + 30) - afstand * 0.25f) % (B + 24));
            if (x < -24) {
                x += B + 24;
            }
            d.sprite(Sprite.WOLK, x - 4, 10 + n * 17);
        }
        // the pillars: kaasknabbel slices stacked from the ceiling and from the floor, a cap at each end
        int eerste = Math.max(0, (int) ((afstand - EERSTE - PILAAR_B) / AFSTAND));
        for (int i = eerste; i < eerste + 4; i++) {
            int px = Math.round(pilaarX(i));
            if (px > B || px + PILAAR_B < 0) {
                continue;
            }
            int midden = gat(i), half = opening(i) / 2;
            int boven = midden - half, onder = midden + half;
            for (int yy = boven - 6 - 8; yy > -8; yy -= 8) {
                d.sprite(Sprite.PILAAR, px, yy);
            }
            d.sprite(Sprite.KAP, px - 2, boven - 6);
            for (int yy = onder + 6; yy < VLOER; yy += 8) {
                d.sprite(Sprite.PILAAR, px, yy);
            }
            d.sprite(Sprite.KAP, px - 2, onder);
        }
        // the floor: a cheese-yellow strip with crumbs sliding by
        d.rect(0, VLOER, B, H - VLOER, 0xFFF2C14E);
        d.rect(0, VLOER, B, 2, 0xFFD99A2B);
        int schuif = (int) (afstand % 16);
        for (int x = -schuif; x < B; x += 16) {
            d.rect(x + 3, VLOER + 4, 4, 2, 0xFFFFE08A);
            d.rect(x + 11, VLOER + 7, 3, 2, 0xFFD99A2B);
        }
        Sprite guh = af ? Sprite.GUH_AF : vleugel > 2 ? Sprite.GUH_NEER : Sprite.GUH_OP;
        d.sprite(guh, GUH_X - 7, Math.round(y) - 6);
        PixelFont.midden(d, Integer.toString(score), B / 2, 6, 2, 0xFFFFFFFF);
    }
}
