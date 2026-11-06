package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * T-RIJDIER: Vahoegje, the baby dragon from zone 3, grows with the story (WERELDVERHAAL §4.3: "groeit per deel"):
 * {@link #KLEIN} (hatched at the end of zone 3) and {@link #MIDDEL} after zone 5 (Hoogvads). Bigger = a bigger model (synced to the
 * pet entity) and a stronger {@link Maatjes#DRAKENWARMTE} (heals more, adds a shield, shorter cooldown).
 * <p>When does it grow: the story flag {@value #VLAG_ZONE5} (the zone-5 story should set it at its end, like zone2_klaar /
 * zone3_klaar), or any {@link #GROEI} check (other slices), or the dev override {@code /guhs vadsrijk huisdier vahoegje <0|1|auto>}.
 */
public final class Vahoegje {
    public static final int KLEIN = 0, MIDDEL = 1;
    /** The story flag after zone 5 (Questlog.vlag). */
    public static final String VLAG_ZONE5 = "zone5_klaar";
    public static final String ID = "vahoegje";
    /** Extra "it has grown" checks (other slices). */
    public static final List<Predicate<ServerPlayer>> GROEI = new CopyOnWriteArrayList<>();
    /** Render scale per stage (times the pet's own scale). */
    public static final float[] SCHAAL = {1.0f, 1.55f};

    private Vahoegje() {
    }

    /** The stage for this player (dev override first). */
    public static int stadium(ServerPlayer p) {
        int dev = Spelers.data(p).extra("huisdieren").getIntOr("vahoegje_dev", -1);
        if (dev >= 0) {
            return Math.min(MIDDEL, dev);
        }
        try {
            if (Questlog.vlag(p, VLAG_ZONE5)) {
                return MIDDEL;
            }
        } catch (RuntimeException e) {
            // (no quest data yet)
        }
        for (Predicate<ServerPlayer> g : GROEI) {
            if (g.test(p)) {
                return MIDDEL;
            }
        }
        return KLEIN;
    }

    /** Dev: force a stage (-1 = automatic again). */
    public static void zetDev(ServerPlayer p, int stadium) {
        if (stadium < 0) {
            Spelers.data(p).extra("huisdieren").remove("vahoegje_dev");
        } else {
            Spelers.data(p).extra("huisdieren").putInt("vahoegje_dev", stadium);
        }
    }
}
