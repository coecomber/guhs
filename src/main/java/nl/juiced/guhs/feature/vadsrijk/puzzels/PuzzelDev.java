package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * T-PUZZELS: solving a puzzle the honest way, through the same input the players use (clicks, plates, pushes, chat). Used by
 * {@code /guhs vadsrijk puzzel los <id>} and by the gametests (so a broken solution in the data or a broken engine shows up).
 */
public final class PuzzelDev {
    private PuzzelDev() {
    }

    /** Solves this instance as player p. Returns true when the puzzle (or, per-player kinds, p's try) ended solved. */
    public static boolean los(Puzzels.Inst in, ServerPlayer p) {
        PuzzelDef d = in.def;
        if (in.opgelost() && !d.soort().equals("tijdrit") && !d.soort().equals("wachtwoord")) {
            Puzzels.herstel(in);
        }
        switch (d.soort()) {
            case "volgorde" -> {
                in.voortgang = 0;
                in.demoStap = -1;
                Puzzels.alleUit(in);
                for (int i : d.oplossing()) {
                    if (d.elementen().get(i).blok().equals("drukplaat")) {
                        Puzzels.stap(in.level, in.element(i), p);
                    } else {
                        Puzzels.klik(in.level, in.element(i), p);
                    }
                }
            }
            case "symbool" -> {
                List<Integer> stenen = d.van("symboolsteen");
                for (int k = 0; k < stenen.size(); k++) {
                    int i = stenen.get(k);
                    for (int t = 0; t < 6 && Puzzels.stand(in, i) != d.oplossing().get(k); t++) {
                        Puzzels.klik(in.level, in.element(i), p);
                    }
                }
                Puzzels.klik(in.level, in.element(d.van("probeerknop").get(0)), p);
            }
            case "patroon" -> {
                boolean was = Puzzels.nachtForceren;
                Puzzels.nachtForceren = true;
                try {
                    for (int i : d.van("sterrensteen")) {
                        boolean moet = d.oplossing().contains(i);
                        if (Puzzels.aan(in, i) != moet) {
                            Puzzels.klik(in.level, in.element(i), p);
                        }
                    }
                    Puzzels.klik(in.level, in.element(d.van("probeerknop").get(0)), p);
                } finally {
                    Puzzels.nachtForceren = was;
                }
            }
            case "licht" -> {
                List<Integer> sp = d.van("spiegel");
                for (int k = 0; k < sp.size() && !in.opgelost(); k++) {
                    int i = sp.get(k);
                    if ((Puzzels.stand(in, i) & 1) != d.oplossing().get(k)) {
                        Puzzels.klik(in.level, in.element(i), p);
                    }
                }
            }
            case "schuif" -> {
                Puzzels.herstel(in);
                for (int[] z : d.zetten()) {
                    BlockPos b = in.origin.offset(z[0], 0, z[1]);
                    Puzzels.Ref r = Puzzels.ref(in.level, b);
                    if (r == null || !Puzzels.duw(in, r.idx(), z[2], p)) {
                        return false;
                    }
                }
            }
            case "tijdrit" -> {
                Puzzels.klik(in.level, in.element(d.van("startlantaarn").get(0)), p);
                Puzzels.stap(in.level, in.element(d.van("eindplaat").get(0)), p);
                return PuzzelSpeler.opgelost(p, d.id());
            }
            case "wachtwoord" -> {
                Puzzels.klik(in.level, in.element(d.van("fluistersteen").get(0)), p);
                Puzzels.zeg(p, d.woorden().get(0));
                return PuzzelSpeler.opgelost(p, d.id());
            }
            case "geheim" -> {
                List<Integer> t = d.van("hendel");
                if (t.isEmpty()) {
                    t = d.van("scheur");
                }
                Puzzels.klik(in.level, in.element(t.get(0)), p);
            }
            default -> {
                return false;
            }
        }
        return in.opgelost();
    }
}
