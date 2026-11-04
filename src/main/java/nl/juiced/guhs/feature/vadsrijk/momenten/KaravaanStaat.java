package nl.juiced.guhs.feature.vadsrijk.momenten;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;

/**
 * Where the caravan is and what it does (pure, gametested). It always travels, also when nobody is near (the cart only shows up
 * near players: {@link Karavaan}). Phases:
 * <ul>
 *   <li>{@link Fase#STOP}: at a stop for that stop's time, then it leaves (turning round at either end of the road);</li>
 *   <li>{@link Fase#RIJDEN}: slowly along the road ({@link #SNELHEID} blocks a tick) to the next stop;</li>
 *   <li>{@link Fase#AANVAL}: klodders attack. Only while driving, only when players are near and the spot allows it, and only once
 *       the pause since the last attack (20-30 min) is over. The cart stands still until the attack is won
 *       ({@link #aanvalGewonnen}) or the attackers give up after {@link #AANVAL_MAX} ticks.</li>
 * </ul>
 */
public final class KaravaanStaat {
    public enum Fase { RIJDEN, STOP, AANVAL }

    public enum Gebeurtenis { GEEN, AANGEKOMEN, VERTROKKEN, AANVAL_BEGINT, AANVAL_OPGEGEVEN }

    /** Blocks per tick (2 blocks a second: a slow snail). */
    public static final double SNELHEID = 0.1;
    public static final int AANVAL_MAX = 20 * 120;
    public static final int PAUZE_MIN = 20 * 60 * 20, PAUZE_MAX = 20 * 60 * 30;

    private final KaravaanRoute route;
    double s;
    int richting = 1;
    Fase fase = Fase.STOP;
    int timer;
    /** Ticks until an attack may happen again. */
    int aanvalPauze;
    @Nullable
    String stop;
    /** Dev: the next attack may happen anywhere (also in town / at a stop). */
    boolean forceer;

    public KaravaanStaat(KaravaanRoute route, RandomSource r) {
        this.route = route;
        KaravaanRoute.Stop eerste = route.stops().get(0);
        this.s = route.afstandVan(eerste.index());
        this.stop = eerste.id();
        this.timer = eerste.ticks();
        this.aanvalPauze = PAUZE_MIN / 2 + r.nextInt(PAUZE_MIN);   // the first attack after 10-30 min
    }

    public Gebeurtenis tick(boolean spelersDichtbij, boolean magAanvalHier, RandomSource r) {
        if (aanvalPauze > 0) {
            aanvalPauze--;
        }
        switch (fase) {
            case STOP -> {
                if (--timer > 0) {
                    return Gebeurtenis.GEEN;
                }
                if (s >= route.lengte() - 1e-6) {
                    richting = -1;
                } else if (s <= 1e-6) {
                    richting = 1;
                }
                fase = Fase.RIJDEN;
                stop = null;
                return Gebeurtenis.VERTROKKEN;
            }
            case AANVAL -> {
                if (--timer > 0) {
                    return Gebeurtenis.GEEN;
                }
                fase = Fase.RIJDEN;
                aanvalPauze = pauze(r);
                return Gebeurtenis.AANVAL_OPGEGEVEN;
            }
            default -> {
                if (aanvalPauze == 0 && spelersDichtbij && (magAanvalHier || forceer)) {
                    forceer = false;
                    fase = Fase.AANVAL;
                    timer = AANVAL_MAX;
                    return Gebeurtenis.AANVAL_BEGINT;
                }
                KaravaanRoute.Stop volgende = route.volgendeStop(s, richting);
                double doel = volgende != null ? route.afstandVan(volgende.index()) : (richting > 0 ? route.lengte() : 0);
                s += SNELHEID * richting;
                if ((s - doel) * richting >= 0) {
                    s = doel;
                    fase = Fase.STOP;
                    timer = volgende != null ? volgende.ticks() : 20 * 30;
                    stop = volgende != null ? volgende.id() : null;
                    return Gebeurtenis.AANGEKOMEN;
                }
                return Gebeurtenis.GEEN;
            }
        }
    }

    /** The players chased the attackers away: drive on, next attack after a new pause. */
    public void aanvalGewonnen(RandomSource r) {
        if (fase == Fase.AANVAL) {
            fase = Fase.RIJDEN;
        }
        aanvalPauze = pauze(r);
    }

    /** Dev: an attack may come right away. */
    public void aanvalNu() {
        aanvalPauze = 0;
    }

    /** Dev: the next attack ignores where the cart is. */
    public void forceerAanval() {
        forceer = true;
    }

    /** Dev: put the caravan at this distance along the road, driving. */
    public void zet(double s, int richting) {
        this.s = Math.max(0, Math.min(route.lengte(), s));
        this.richting = richting >= 0 ? 1 : -1;
        this.fase = Fase.RIJDEN;
        this.stop = null;
    }

    static int pauze(RandomSource r) {
        return PAUZE_MIN + r.nextInt(PAUZE_MAX - PAUZE_MIN + 1);
    }

    public double s() {
        return s;
    }

    public int richting() {
        return richting;
    }

    public Fase fase() {
        return fase;
    }

    public int timer() {
        return timer;
    }

    public int aanvalPauze() {
        return aanvalPauze;
    }

    @Nullable
    public String stop() {
        return stop;
    }

    public KaravaanRoute route() {
        return route;
    }
}
