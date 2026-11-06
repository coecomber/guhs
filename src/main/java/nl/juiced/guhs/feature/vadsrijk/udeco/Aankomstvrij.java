package nl.juiced.guhs.feature.vadsrijk.udeco;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;

/**
 * U-DECO (feedback round 4, "you arrive between attacking mobs at the gondola"): no mob spawns within {@link #STRAAL} blocks
 * (flat, {@link #HOOGTE} up/down) of an arrival spot, a campfire or a town edge. Hooked into the kern spawners via
 * {@link Spawners#VERBODEN_VOOR}, so it knows the spawner:
 * <ul>
 * <li>arrival spots (both gondola stations, every airship harbour/landing/bell/departure board, every boat dock, the airport
 * arrival, return portals): always (a spawner centred on a station is a mistake);</li>
 * <li>campfires and town edges (a town area within {@link #STRAAL} of the spot): only for spawners whose own centre lies
 * farther than {@link #STRAAL} from it, so a dungeon spawner placed on purpose next to a campfire still works (the kern's own
 * 12-block campfire rule keeps applying there).</li>
 * </ul>
 * The spots come from the zone 1-5 map tables (WereldPlekken, Z2-Z5WereldPlekken) by id pattern, collected once.
 */
public final class Aankomstvrij {
    public static final int STRAAL = 24;
    public static final int HOOGTE = 16;

    /** strikt = an arrival spot (no exemption); id for the dev command. */
    public record Punt(String id, BlockPos pos, boolean strikt) {
    }

    private static volatile List<Punt> punten;

    private Aankomstvrij() {
    }

    public static void registreer() {
        Spawners.VERBODEN_VOOR.add(Aankomstvrij::verboden);
    }

    public static List<Punt> punten() {
        List<Punt> l = punten;
        if (l == null) {
            l = new ArrayList<>();
            voeg(l, nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.ALLE);
            voeg(l, nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ALLE);
            voeg(l, nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ALLE);
            voeg(l, nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE);
            voeg(l, nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.ALLE);
            punten = l = List.copyOf(l);
        }
        return l;
    }

    private static void voeg(List<Punt> l, Map<String, BlockPos> m) {
        m.forEach((id, p) -> {
            if (aankomst(id)) {
                l.add(new Punt(id, p, true));
            } else if (id.startsWith("kampvuur_") && !id.endsWith("_trap")) {
                l.add(new Punt(id, p, false));
            }
        });
    }

    /** Is this map id an arrival spot (gondola, airship harbour, boat dock, airport, return portal)? */
    static boolean aankomst(String id) {
        if (id.equals("gondel_dal") || id.equals("gondel_berg") || id.equals("npc_gondel") || id.equals("npcplek.z2_gondel_boven")) {
            return true;
        }
        if (id.startsWith("boot_dok_")) {
            return !id.endsWith("_water");
        }
        if (id.startsWith("haven_")) {
            return id.endsWith("_landing") || id.endsWith("_vertrek") || id.endsWith("_bel") || id.endsWith("_plein")
                    || id.endsWith("_eind") || id.endsWith("_vertrekbord") || id.endsWith("_bord");
        }
        return id.endsWith("_landing") || id.equals("luchthaven_aankomst") || id.startsWith("luchthaven_pier")
                || id.startsWith("terugportaal") || id.startsWith("dok_landing") || id.equals("dok_welkom");
    }

    static boolean binnen(BlockPos a, BlockPos b, int r) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz < (double) r * r && Math.abs(a.getY() - b.getY()) <= HOOGTE;
    }

    /** The spawner hook: true = no spawn of spawner s at plek. */
    static boolean verboden(Spawners.Spawner s, BlockPos plek) {
        for (Punt p : punten()) {
            if (!binnen(plek, p.pos(), STRAAL)) {
                continue;
            }
            if (p.strikt() || !binnen(s.centrum(), p.pos(), STRAAL + 2)) {
                return true;
            }
        }
        return stadsrand(plek) && !stadsrand(s.centrum());
    }

    /** A town area within {@link #STRAAL} blocks of this spot (8 directions + the spot itself). */
    static boolean stadsrand(BlockPos p) {
        for (int k = 0; k < 9; k++) {
            double a = k * Math.PI / 4, d = k == 8 ? 0 : STRAAL;
            Gebieden.Gebied g = Gebieden.op(p.getX() + 0.5 + Math.cos(a) * d, p.getY(), p.getZ() + 0.5 + Math.sin(a) * d);
            if (g != null && g.stad()) {
                return true;
            }
        }
        return false;
    }
}
