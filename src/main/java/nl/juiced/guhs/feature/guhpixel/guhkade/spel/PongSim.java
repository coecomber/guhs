package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * Mika-Pong: the ball is a rolling guh, your paddle is a kaasknabbel stick on the left, the Mika shoves the guh back from
 * the right. Every time you roll the guh back is 1 point, rolling it past the Mika is 5. The guh rolls a little faster
 * with every tap, and the Mika gets quicker with every goal against it. Three times past you and the game is over (the
 * rolling guh thinks it is all great fun: nothing hurts).
 * <p>
 * Input: 0 = stay, 1 = up, 2 = down.
 */
public final class PongSim extends Sim {
    public static final int OMHOOG = 1, OMLAAG = 2;
    /** The field: under the score strip, between the two paddles. */
    static final int BOVEN = 14, ONDER = H - 2;
    static final int BAT_H = 22, BAT_B = 4, BAT_X = 8, MIKA_X = B - 8 - BAT_B;
    static final float BAL_R = 4f, BAT_SNEL = 2.7f;
    static final float SNEL_MIN = 2.0f, SNEL_MAX = 4.3f;
    public static final int LEVENS = 3, PUNT_TIK = 1, PUNT_GOAL = 5;

    private float bx, by, vx, vy;
    /** The middles of the two paddles. */
    private float py = (BOVEN + ONDER) / 2f, my = py;
    private int levens = LEVENS, goals, tikken;
    /** Steps until the guh rolls off again (after a goal and at the start), and who it rolls to (-1 = you). */
    private int pauze = 30, opslagen;
    /** Where the Mika thinks the guh will arrive (it is not very good at guessing). */
    private float mikaFout;
    private int mikaBoos, rol;

    public PongSim(long seed) {
        super(seed);
        leg();
    }

    @Override
    public int bits() {
        return 2;
    }

    public int levens() {
        return levens;
    }

    private void leg() {
        bx = B / 2f;
        by = (BOVEN + ONDER) / 2f;
        vx = vy = 0;
    }

    private float basis() {
        return Math.min(3.0f, SNEL_MIN + 0.12f * goals);
    }

    private void opslag() {
        float snel = basis();
        float hoek = (kans(1000 + opslagen) * 2f - 1f) * 0.55f;     // (a gentle angle, up or down)
        vy = snel * hoek;
        vx = -(float) Math.sqrt(snel * snel - vy * vy);              // (always towards you first)
        mikaFout = 0;
        opslagen++;
    }

    @Override
    protected void doeStap(int invoer) {
        if (invoer == OMHOOG) {
            py -= BAT_SNEL;
        } else if (invoer == OMLAAG) {
            py += BAT_SNEL;
        }
        py = klem(py);
        if (mikaBoos > 0) {
            mikaBoos--;
        }
        if (pauze > 0) {
            if (--pauze == 0) {
                opslag();
            }
            mika(by);
            return;
        }
        bx += vx;
        by += vy;
        rol++;
        if (by - BAL_R < BOVEN) {
            by = BOVEN + BAL_R;
            vy = Math.abs(vy);
            geluid |= GELUID_BONS;
        } else if (by + BAL_R > ONDER) {
            by = ONDER - BAL_R;
            vy = -Math.abs(vy);
            geluid |= GELUID_BONS;
        }
        // your paddle
        if (vx < 0 && bx - BAL_R <= BAT_X + BAT_B && bx - BAL_R >= BAT_X - 3 && Math.abs(by - py) <= BAT_H / 2f + BAL_R) {
            kaats(1, (by - py) / (BAT_H / 2f + BAL_R));
            bx = BAT_X + BAT_B + BAL_R;
            tikken++;
            score += PUNT_TIK;
            geluid |= GELUID_TIK;
            // the Mika guesses where the guh will arrive: worse the faster it rolls, a little better every goal
            float ruimte = Math.max(5f, 17f - goals * 1.5f) + Math.max(0f, snelheid() - 2.6f) * 7f;
            mikaFout = (kans(5000 + tikken) * 2f - 1f) * ruimte;
        }
        // the Mika's paddle
        if (vx > 0 && bx + BAL_R >= MIKA_X && bx + BAL_R <= MIKA_X + BAT_B + 3 && Math.abs(by - my) <= BAT_H / 2f + BAL_R) {
            kaats(-1, (by - my) / (BAT_H / 2f + BAL_R));
            bx = MIKA_X - BAL_R;
            geluid |= GELUID_BONS;
        }
        mika(by + mikaFout);
        if (bx < -BAL_R) {                   // past you
            levens--;
            geluid |= GELUID_AF;
            if (levens <= 0) {
                af = true;
                return;
            }
            leg();
            pauze = 36;
        } else if (bx > B + BAL_R) {         // past the Mika: goal!
            goals++;
            score += PUNT_GOAL;
            mikaBoos = 40;
            geluid |= GELUID_PUNT;
            leg();
            pauze = 36;
        }
    }

    private float snelheid() {
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    /** Off a paddle: a little faster, and the angle follows where on the paddle the guh was tapped (waar = -1..1). */
    private void kaats(int richting, float waar) {
        float snel = Math.min(SNEL_MAX, snelheid() * 1.045f);
        waar = Math.max(-1f, Math.min(1f, waar));
        vy = snel * 0.78f * waar;
        vx = richting * (float) Math.sqrt(snel * snel - vy * vy);
    }

    /** The Mika walks its paddle towards doel (when the guh comes its way), or back to the middle. */
    private void mika(float doel) {
        float snel = Math.min(3.0f, 1.55f + 0.13f * goals);
        float naar = vx > 0 ? doel : (BOVEN + ONDER) / 2f;
        float d = naar - my;
        if (Math.abs(d) > 1.5f) {
            my += Math.signum(d) * Math.min(snel, Math.abs(d));
        }
        my = klem(my);
    }

    private static float klem(float y) {
        return Math.max(BOVEN + BAT_H / 2f, Math.min(ONDER - BAT_H / 2f, y));
    }

    /** Where the guh will be when it gets to x (bouncing off the top and bottom on the way). */
    private float voorspelY(float x) {
        if (Math.abs(vx) < 0.01f) {
            return by;
        }
        float t = (x - bx) / vx;
        if (t < 0) {
            return by;
        }
        float laag = BOVEN + BAL_R, hoog = ONDER - BAL_R, bereik = hoog - laag;
        float yy = (by + vy * t - laag) % (2 * bereik);
        if (yy < 0) {
            yy += 2 * bereik;
        }
        return laag + (yy > bereik ? 2 * bereik - yy : yy);
    }

    @Override
    public int bot(int doel) {
        float naar;
        if (pauze > 0 || vx > 0) {
            naar = (BOVEN + ONDER) / 2f;
        } else {
            naar = voorspelY(BAT_X + BAT_B + BAL_R);
            // tap the guh with the edge of the paddle (that side changes with every tap), so it rolls off at an angle
            naar += ((tikken & 1) == 0 ? 1 : -1) * (BAT_H / 2f - 3f) * (kans(9000 + tikken) * 0.9f);
            if (score >= doel) {
                // good enough for today: looks the other way (njeg)
                naar = naar > (BOVEN + ONDER) / 2f ? naar - 44 : naar + 44;
            }
        }
        float d = naar - py;
        return Math.abs(d) < 1.6f ? 0 : d < 0 ? OMHOOG : OMLAAG;
    }

    @Override
    public void teken(Doek d) {
        d.rect(0, 0, B, H, 0xFF2A1430);
        d.rect(0, 0, B, BOVEN - 2, 0xFF3D1F47);
        d.rect(0, BOVEN - 2, B, 2, 0xFFFF9AC8);
        d.rect(0, ONDER, B, 2, 0xFFFF9AC8);
        for (int y = BOVEN + 3; y < ONDER - 3; y += 10) {       // the dotted middle line
            d.rect(B / 2 - 1, y, 2, 5, 0xFF6B3A78);
        }
        for (int i = 0; i < LEVENS; i++) {
            d.sprite(i < levens ? Sprite.HARTJE : Sprite.HARTJE_LEEG, 3 + i * 8, 3);
        }
        PixelFont.teken(d, Integer.toString(score), B / 2 - PixelFont.breedte(Integer.toString(score), 1) / 2, 4, 1, 0xFFFFFFFF);
        d.sprite(mikaBoos > 0 ? Sprite.MIKA_BOOS : Sprite.MIKA, B - 14, 0);
        d.sprite(Sprite.BATJE, BAT_X, Math.round(py) - BAT_H / 2);
        d.sprite(Sprite.BATJE_MIKA, MIKA_X, Math.round(my) - BAT_H / 2);
        Sprite bal = switch ((rol / 4) & 3) {
            case 0 -> Sprite.BAL_0;
            case 1 -> Sprite.BAL_1;
            case 2 -> Sprite.BAL_2;
            default -> Sprite.BAL_3;
        };
        d.sprite(bal, Math.round(bx) - 5, Math.round(by) - 5);
    }
}
