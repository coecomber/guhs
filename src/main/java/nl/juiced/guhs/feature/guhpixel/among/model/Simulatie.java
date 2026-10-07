package nl.juiced.guhs.feature.guhpixel.among.model;

import java.io.FileReader;
import java.io.Reader;
import java.util.Locale;

/**
 * Headless rounds with NPCs only: used by the game test that guards the balance (the Mika should win 35-65% of the
 * rounds on Normaal) and, from a plain {@code java} command line, to tune {@link Balans}:
 * <pre>java -cp build/classes/java/main nl.juiced.guhs.feature.guhpixel.among.model.Simulatie src/main/resources/data/guhs/guhpixel/among_schip.txt 2000</pre>
 */
public final class Simulatie {
    /** A round that is still going after this many ticks (40 minutes) counts as undecided. */
    public static final int MAX_TICKS = 20 * 60 * 40;

    public record Uitkomst(int rondes, int mikaWinst, int crewWinst, int onbeslist, int doorTaken, int doorStemmen, int doorOvermacht, int doorAlarm,
                           double gemTicks, double gemVergaderingen, double gemDuwen, int kortste, int langste, int mikaWeggestemd, int crewWeggestemd,
                           int niemandWeggestemd) {
        public double mikaDeel() {
            return rondes == 0 ? 0 : mikaWinst / (double) rondes;
        }

        public String tekst(String naam) {
            return String.format(Locale.ROOT,
                    "%s: %d rounds, Mika wins %.1f%% (outnumbered %d, alarm %d), crew wins %.1f%% (tasks %d, voted out %d), undecided %d; "
                            + "average %.1f min (shortest %.1f, longest %.1f), %.1f meetings, %.1f pushes; "
                            + "votes: Mika out %d, crew out %d, nobody %d",
                    naam, rondes, 100.0 * mikaDeel(), doorOvermacht, doorAlarm, rondes == 0 ? 0 : 100.0 * crewWinst / rondes, doorTaken, doorStemmen,
                    onbeslist, gemTicks / 1200.0, kortste / 1200.0, langste / 1200.0, gemVergaderingen, gemDuwen, mikaWeggestemd, crewWeggestemd,
                    niemandWeggestemd);
        }
    }

    /** Plays one round to the end (or MAX_TICKS); returns {Mikas voted out, crew voted out, meetings without anybody voted out}. */
    public static int[] speel(Ronde r) {
        int[] stem = new int[3];
        while (r.fase != Ronde.Fase.KLAAR && r.tick < MAX_TICKS) {
            r.tick();
            for (Gebeurtenis g : r.gebeurtenissen) {
                if (g.soort() == Gebeurtenis.Soort.UITSLAG) {
                    stem[g.a() < 0 ? 2 : g.b() == 1 ? 0 : 1]++;
                }
            }
            r.gebeurtenissen.clear();
        }
        return stem;
    }

    public static Uitkomst draai(Schip schip, Balans balans, int rondes, long seed) {
        int mika = 0, crew = 0, onbeslist = 0, taken = 0, stemmen = 0, overmacht = 0, alarm = 0, kortste = Integer.MAX_VALUE, langste = 0;
        long ticks = 0, vergaderingen = 0, duwen = 0;
        int[] stem = new int[3];
        for (int i = 0; i < rondes; i++) {
            Ronde r = new Ronde(schip, balans, seed * 1000003L + i, 0, null);
            int[] st = speel(r);
            for (int k = 0; k < 3; k++) {
                stem[k] += st[k];
            }
            ticks += r.tick;
            vergaderingen += r.aantalVergaderingen;
            duwen += r.aantalDuwen;
            kortste = Math.min(kortste, r.tick);
            langste = Math.max(langste, r.tick);
            if (r.winnaar == null) {
                onbeslist++;
            } else if (r.winnaar == Deelnemer.Rol.MIKA) {
                mika++;
                if (r.einde == Ronde.Einde.ALARM) {
                    alarm++;
                } else {
                    overmacht++;
                }
            } else {
                crew++;
                if (r.einde == Ronde.Einde.TAKEN) {
                    taken++;
                } else {
                    stemmen++;
                }
            }
        }
        double n = Math.max(1, rondes);
        return new Uitkomst(rondes, mika, crew, onbeslist, taken, stemmen, overmacht, alarm, ticks / n, vergaderingen / n, duwen / n,
                rondes == 0 ? 0 : kortste, langste, stem[0], stem[1], stem[2]);
    }

    public static void main(String[] args) throws Exception {
        Schip schip;
        try (Reader in = new FileReader(args[0], java.nio.charset.StandardCharsets.UTF_8)) {
            schip = Schip.lees(in);
        }
        int rondes = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
        long t = System.nanoTime();
        System.out.println(draai(schip, Balans.normaal(), rondes, 1).tekst("Normaal"));
        System.out.println(draai(schip, Balans.lastig(), rondes, 2).tekst("Lastig"));
        System.out.printf(Locale.ROOT, "(%.1f s)%n", (System.nanoTime() - t) / 1e9);
    }

    private Simulatie() {
    }
}
