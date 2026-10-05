package nl.juiced.guhs.feature.guhpixel.among.model;

/** One task of one participant and how far it is. */
public final class TaakStand {
    public final Schip.Taak taak;
    public int stap;

    public TaakStand(Schip.Taak taak) {
        this.taak = taak;
    }

    public boolean klaar() {
        return stap >= taak.panelen().length;
    }

    /** The panel of the next step (-1 when the task is done). */
    public int paneel() {
        return klaar() ? -1 : taak.panelen()[stap];
    }
}
