package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * The rules of the one Guhmon battle (no Minecraft world in here: a pure state machine, so the game tests can play it).
 * Two guhs with a SLEEP bar (0..{@link #VOL}); nothing hurts; whoever falls asleep FIRST wins. Every round the player
 * moves first, then the guh of Gymleider Dutjes.
 * <ul>
 *   <li>{@link Zet#VADSEN}: +{@value #VADSEN} own sleep;</li>
 *   <li>{@link Zet#NJEG}: -{@value #NJEG} sleep of the other ("het is niet erg effectief...") and the other's next Dutje fails;</li>
 *   <li>{@link Zet#KNABBEL}: +{@value #KNABBEL} now and +{@value #KNABBEL} at the start of the own next two moves (volle maag);</li>
 *   <li>{@link Zet#DUTJE}: +{@value #DUTJE}, but it fails when the last thing the other did was Njeg.</li>
 * </ul>
 * A guh that reaches {@link #VOL} through its full belly falls asleep before its move. The guh of Gymleider Dutjes is a
 * little drowsy already: it starts at {@value #START_TEGEN}. Balance (4000 simulated battles): the obvious tactic (Nap,
 * or Chonk when the Nap would fail) wins about 9 in 10, only ever napping about half, only ever chonking 1 in 10.
 */
public final class GuhmonGevecht {
    public enum Zet {
        VADSEN, NJEG, KNABBEL, DUTJE;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Zet op(int i) {
            return values()[Math.floorMod(i, values().length)];
        }
    }

    public enum Uitkomst { BEZIG, GEWONNEN, VERLOREN }

    /** One line of the text box: lang key gui.guhs.guhmon.tekst.&lt;sleutel&gt;, who it is about (0 player's guh, 1 the other), the bars after it. */
    public record Regel(String sleutel, int wie, int slaapSpeler, int slaapTegen) {
    }

    public static final int VOL = 100, VADSEN = 25, NJEG = 15, KNABBEL = 10, DUTJE = 40, MAAG_BEURTEN = 2, START_TEGEN = 30;
    public static final int SPELER = 0, TEGEN = 1;

    private final int[] slaap = new int[2];
    private final int[] maag = new int[2];
    /** The last move of each side (null: none yet). */
    private final Zet[] laatste = new Zet[2];
    private int ronde;
    private Uitkomst uitkomst = Uitkomst.BEZIG;
    /** A Dutje of the other guh failed because of the player's Njeg (the Njegbadge). */
    private boolean njegBlokte;
    /** The player's guh fell asleep with a full belly: through it, or by a move while it was full (the Knabbelbadge). */
    private boolean volBuikje;

    public GuhmonGevecht() {
        slaap[TEGEN] = START_TEGEN;
    }

    public int slaap(int wie) {
        return slaap[wie];
    }

    public int maag(int wie) {
        return maag[wie];
    }

    public int ronde() {
        return ronde;
    }

    public Uitkomst uitkomst() {
        return uitkomst;
    }

    public boolean njegBlokte() {
        return njegBlokte;
    }

    public boolean volBuikje() {
        return volBuikje;
    }

    /** Would a Dutje of this side fail right now? */
    public boolean dutjeMislukt(int wie) {
        return laatste[1 - wie] == Zet.NJEG;
    }

    /** One round: the player's move, then (when nobody sleeps yet) the other guh's. Returns the lines of the text box. */
    public List<Regel> speel(Zet zet, RandomSource random) {
        List<Regel> uit = new ArrayList<>();
        if (uitkomst != Uitkomst.BEZIG) {
            return uit;
        }
        ronde++;
        if (!beurt(SPELER, zet, uit)) {
            beurt(TEGEN, kiesTegen(random), uit);
        }
        return uit;
    }

    /** (Tests) one round in which the other guh's move is given. */
    List<Regel> speel(Zet zet, Zet tegenZet) {
        List<Regel> uit = new ArrayList<>();
        if (uitkomst != Uitkomst.BEZIG) {
            return uit;
        }
        ronde++;
        if (!beurt(SPELER, zet, uit)) {
            beurt(TEGEN, tegenZet, uit);
        }
        return uit;
    }

    /** True when the battle is over after this move. */
    private boolean beurt(int wie, Zet zet, List<Regel> uit) {
        if (maag[wie] > 0) {
            maag[wie]--;
            erbij(wie, KNABBEL);
            uit.add(regel("buikje", wie));
            if (slaap[wie] >= VOL) {
                if (wie == SPELER) {
                    volBuikje = true;
                }
                return klaar(wie, uit);
            }
        }
        switch (zet) {
            case VADSEN -> {
                erbij(wie, VADSEN);
                uit.add(regel("vadsen", wie));
            }
            case NJEG -> {
                slaap[1 - wie] = Math.max(0, slaap[1 - wie] - NJEG);
                uit.add(regel("njeg", wie));
            }
            case KNABBEL -> {
                erbij(wie, KNABBEL);
                uit.add(regel(maag[wie] > 0 ? "knabbel_vol" : "knabbel", wie));
                maag[wie] = MAAG_BEURTEN;
            }
            case DUTJE -> {
                if (dutjeMislukt(wie)) {
                    if (wie == TEGEN) {
                        njegBlokte = true;
                    }
                    uit.add(regel("dutje_mislukt", wie));
                } else {
                    erbij(wie, DUTJE);
                    uit.add(regel("dutje", wie));
                }
            }
        }
        laatste[wie] = zet;
        if (slaap[wie] >= VOL && wie == SPELER && maag[wie] > 0) {
            volBuikje = true;
        }
        return slaap[wie] >= VOL && klaar(wie, uit);
    }

    private void erbij(int wie, int n) {
        slaap[wie] = Math.min(VOL, slaap[wie] + n);
    }

    private boolean klaar(int wie, List<Regel> uit) {
        uitkomst = wie == SPELER ? Uitkomst.GEWONNEN : Uitkomst.VERLOREN;
        uit.add(regel("slaapt", wie));
        uit.add(regel(wie == SPELER ? "gewonnen" : "verloren", wie));
        return true;
    }

    private Regel regel(String sleutel, int wie) {
        return new Regel(sleutel, wie, slaap[SPELER], slaap[TEGEN]);
    }

    /**
     * What the guh of Gymleider Dutjes does: a snack first; a Njeg now and then once the player's guh gets drowsy (less often
     * when one more Nap would win it the battle); otherwise mostly naps. It is sleepy, so half the time it forgets that a
     * Njeg is still echoing and tries a Nap that fails. A player who pays attention wins.
     */
    Zet kiesTegen(RandomSource random) {
        boolean mislukt = dutjeMislukt(TEGEN);
        if (ronde == 1 && !mislukt) {
            return Zet.KNABBEL;
        }
        int njeg = slaap[SPELER] >= 70 ? 70 : slaap[SPELER] >= 40 ? 45 : 0;
        if (slaap[TEGEN] >= VOL - DUTJE && !mislukt) {
            njeg /= 2;
        }
        if (random.nextInt(100) < njeg) {
            return Zet.NJEG;
        }
        int r = random.nextInt(100);
        if (mislukt) {
            if (r < 50) {
                return Zet.DUTJE;
            }
            return maag[TEGEN] == 0 && r < 80 ? Zet.KNABBEL : Zet.VADSEN;
        }
        if (r < 55) {
            return Zet.DUTJE;
        }
        return r < 87 || maag[TEGEN] > 0 ? Zet.VADSEN : Zet.KNABBEL;
    }
}
