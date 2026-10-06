package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.List;

/** One participant of a round: a real player or a guh NPC. */
public final class Deelnemer {
    public enum Rol { CREW, MIKA }

    public final int idx;
    public final boolean npc;
    public Kleur kleur;
    public Rol rol = Rol.CREW;
    /** Awake = still in the game; not awake = a droomguh. */
    public boolean wakker = true;
    /** A real player who left: counts for nothing any more. */
    public boolean weg;
    public double x, z;
    public float yaw;
    public int zone = -1;
    public final List<TaakStand> taken = new ArrayList<>();
    public boolean knopGebruikt;
    public int duwAfkoel;
    public int luikAfkoel;
    /** The sleeping body lies somewhere to be found (until the next meeting). */
    public boolean lichaam;
    public double lichaamX, lichaamZ;
    public int lichaamZone = -1;
    public int lichaamTick;
    /** The panel a real player is working at (-1: none) and since when. */
    public int werkPaneel = -1;
    public int werkSinds;
    /** An NPC is busy at a panel (the entity looks at it). */
    public boolean werkt;
    public Brein brein;
    public int takenKlaar;
    public int duwen;
    public boolean weggestemd;
    public int uitspraken;

    public Deelnemer(int idx, boolean npc) {
        this.idx = idx;
        this.npc = npc;
    }

    public boolean mika() {
        return rol == Rol.MIKA;
    }

    public boolean alleTakenKlaar() {
        for (TaakStand t : taken) {
            if (!t.klaar()) {
                return false;
            }
        }
        return true;
    }

    public double afstand(double px, double pz) {
        return Math.hypot(x - px, z - pz);
    }

    public double afstand(Deelnemer d) {
        return Math.hypot(x - d.x, z - d.z);
    }
}
