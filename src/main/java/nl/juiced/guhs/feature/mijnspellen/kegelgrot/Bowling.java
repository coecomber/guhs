package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import java.util.ArrayList;
import java.util.List;

/**
 * A bowling score sheet, exactly like real ten-pin bowling: 10 frames; a strike (all 10 with the first ball) scores 10 +
 * the next two balls, a spare (all 10 with two balls) 10 + the next ball; in the 10th frame a strike or a spare gives
 * extra balls (three at most), so a perfect game is 300. Pure (no world): the Kegelgrot's game feeds it the pins of every
 * ball ({@link #gooi}) and asks what comes next ({@link #nieuweKegels}, {@link #klaar}).
 */
public final class Bowling {
    public static final int FRAMES = 10, KEGELS = 10;
    private final List<Integer> worpen = new ArrayList<>();

    /** The pins of one ball (how many fell with it: 0..the pins still standing). */
    public void gooi(int kegels) {
        if (klaar()) {
            throw new IllegalStateException("the game is over");
        }
        int max = staand();
        worpen.add(Math.max(0, Math.min(max, kegels)));
    }

    public List<Integer> worpen() {
        return List.copyOf(worpen);
    }

    /** The frame the next ball is in (0..9; 10 = over), and which ball of it (0, 1 or 2 in the 10th). */
    public int[] positie() {
        int i = 0;
        for (int f = 0; f < FRAMES; f++) {
            if (f < FRAMES - 1) {
                if (i >= worpen.size()) {
                    return new int[] {f, 0};
                }
                if (worpen.get(i) == KEGELS) {
                    i++;
                    continue;
                }
                if (i + 1 >= worpen.size()) {
                    return new int[] {f, 1};
                }
                i += 2;
            } else {
                int n = worpen.size() - i;
                int toegestaan = 2;
                if (n >= 1 && worpen.get(i) == KEGELS || n >= 2 && worpen.get(i) + worpen.get(i + 1) == KEGELS) {
                    toegestaan = 3;
                }
                if (n >= 2 && worpen.get(i) == KEGELS) {
                    toegestaan = 3;
                }
                return n >= toegestaan ? new int[] {FRAMES, 0} : new int[] {f, n};
            }
        }
        return new int[] {FRAMES, 0};
    }

    public boolean klaar() {
        return positie()[0] >= FRAMES;
    }

    /** How many pins stand for the next ball (10 at the start of a frame, after a strike or spare in the 10th). */
    public int staand() {
        int[] pos = positie();
        if (pos[0] >= FRAMES) {
            return 0;
        }
        int start = frameStart(pos[0]);
        if (pos[0] < FRAMES - 1) {
            return pos[1] == 0 ? KEGELS : KEGELS - worpen.get(start);
        }
        // the 10th: after a strike the pins are set up again; after a spare too
        int ball = pos[1];
        if (ball == 0) {
            return KEGELS;
        }
        int a = worpen.get(start);
        if (ball == 1) {
            return a == KEGELS ? KEGELS : KEGELS - a;
        }
        int b = worpen.get(start + 1);
        if (a == KEGELS) {
            return b == KEGELS ? KEGELS : KEGELS - b;
        }
        return KEGELS;   // (a spare: a fresh rack for the bonus ball)
    }

    /** Should the next ball get a fresh rack of 10 (the start of a frame, or a fresh rack in the 10th)? */
    public boolean nieuweKegels() {
        return !klaar() && staand() == KEGELS;
    }

    /** The index of the first ball of a frame. */
    private int frameStart(int frame) {
        int i = 0;
        for (int f = 0; f < frame; f++) {
            if (i < worpen.size() && worpen.get(i) == KEGELS) {
                i++;
            } else {
                i += 2;
            }
        }
        return i;
    }

    /** The running total after every frame (null where it can't be counted yet: a strike or spare waiting for its bonus). */
    public Integer[] stand() {
        Integer[] out = new Integer[FRAMES];
        int totaal = 0, i = 0;
        for (int f = 0; f < FRAMES; f++) {
            if (i >= worpen.size()) {
                break;
            }
            int a = worpen.get(i);
            if (f == FRAMES - 1) {
                int n = worpen.size() - i;
                boolean bonus = a == KEGELS || n >= 2 && a + worpen.get(i + 1) == KEGELS;
                int nodig = bonus ? 3 : 2;
                if (n < nodig) {
                    break;
                }
                for (int k = 0; k < nodig; k++) {
                    totaal += worpen.get(i + k);
                }
                out[f] = totaal;
                break;
            }
            if (a == KEGELS) {
                if (i + 2 >= worpen.size()) {
                    break;
                }
                totaal += KEGELS + worpen.get(i + 1) + worpen.get(i + 2);
                i++;
            } else {
                if (i + 1 >= worpen.size()) {
                    break;
                }
                int b = worpen.get(i + 1);
                if (a + b == KEGELS) {
                    if (i + 2 >= worpen.size()) {
                        break;
                    }
                    totaal += KEGELS + worpen.get(i + 2);
                } else {
                    totaal += a + b;
                }
                i += 2;
            }
            out[f] = totaal;
        }
        return out;
    }

    /** The score so far: the last frame that can be counted (0 at the start). */
    public int score() {
        int s = 0;
        for (Integer t : stand()) {
            if (t != null) {
                s = t;
            }
        }
        return s;
    }

    /** How many strikes so far. */
    public int strikes() {
        return (int) symbolen().chars().filter(c -> c == 'X').count();
    }

    /** The sheet as text, frame by frame: "X | 7/ | 9- | 81 | ... | X X 9" (X strike, / spare, - a miss). */
    public String symbolen() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int f = 0; f < FRAMES && i < worpen.size(); f++) {
            if (f > 0) {
                sb.append(" | ");
            }
            if (f < FRAMES - 1) {
                int a = worpen.get(i);
                if (a == KEGELS) {
                    sb.append('X');
                    i++;
                    continue;
                }
                sb.append(teken(a));
                if (i + 1 < worpen.size()) {
                    int b = worpen.get(i + 1);
                    sb.append(a + b == KEGELS ? "/" : teken(b));
                }
                i += 2;
            } else {
                List<Integer> t = worpen.subList(i, worpen.size());
                // a ball "starts a rack" when the rack before it was cleared (first ball, after a strike, after a spare)
                boolean vers = true;
                for (int k = 0; k < t.size(); k++) {
                    if (k > 0) {
                        sb.append(' ');
                    }
                    int w = t.get(k);
                    if (vers) {
                        sb.append(w == KEGELS ? "X" : teken(w));
                        vers = w == KEGELS;
                    } else {
                        sb.append(t.get(k - 1) + w == KEGELS ? "/" : teken(w));
                        vers = t.get(k - 1) + w == KEGELS;
                    }
                }
                i = worpen.size();
            }
        }
        return sb.toString();
    }

    private static String teken(int n) {
        return n == 0 ? "-" : String.valueOf(n);
    }
}
