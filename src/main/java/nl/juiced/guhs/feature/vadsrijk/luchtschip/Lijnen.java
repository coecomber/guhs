package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * T-LUCHTSCHIP: ALL scheduled airship lines of Het Vadsrijk in one place (WERELDINDELING.md §4, ZONE4_PLAN W-1..W-3).
 * <p>
 * A line flies between two harbours ({@link Haven}); each harbour is a POI {@code haven_<van>_<naar>_vertrek} (zone tokens
 * {@code kamp z1 z2 z3 z4 z5}). The harbour terrain is t-wereld's / t-z4wereld's work: the POI is looked up at runtime in the
 * generated POI classes ({@link #ZOEKERS}); until it exists the placeholder from the table below is used (the planned
 * position of WERELDINDELING §4 / {@code tools/vadsrijk/indeling.py HAVENS}, y = where you stand on the pier).
 * <p>
 * The ship docks next to the pier: its middle {@link Haven#UIT} blocks out from the POI in the harbour's open-air direction
 * {@link Haven#richting} (degrees, 0 = south/+z, 90 = west/-x, 180 = north/-z, 270 = east/+x, Minecraft yaw), its deck at
 * the POI's height, bow outward. The route between the two is a list of world control points ({@link Lijn#via}); the way
 * back is the same list reversed, shifted sideways in the middle so the two ships of a line pass each other.
 */
public final class Lijnen {
    /** Ticks between two departures from the same harbour (~3 min). */
    public static final int PERIODE = 20 * 180;
    /** Cruise speed (blocks per tick, 8 blocks/s); a flight lasts length / speed, clamped to 60..90 s. */
    public static final double VAART = 0.4;
    public static final int MIN_DUUR = 20 * 60, MAX_DUUR = 20 * 90;
    /** Price of the luxury flight with the travel map (bronze; the airships are free). The first luxury flight to a place is free. */
    public static final long LUXE_PRIJS = 400;

    /** One airship harbour: POI id, fallback position (stand spot on the pier), open-air direction (yaw) and its zone token. */
    public record Haven(String id, BlockPos reserve, float richting, String zone) {
        /** How far out the middle of the docked ship lies (blocks, from the POI). */
        public static final double UIT = 9;

        /** The stand spot on the pier: the generated POI when it exists, else the placeholder. */
        public BlockPos plek() {
            BlockPos b = zoek(id);
            return b != null ? b : reserve;
        }

        /** Where the docked ship floats (its deck at the pier's height). */
        public Vec3 dok() {
            BlockPos p = plek();
            return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5).add(uit().scale(UIT));
        }

        /** Unit vector into the open air. */
        public Vec3 uit() {
            double r = Math.toRadians(richting);
            return new Vec3(-Math.sin(r), 0, Math.cos(r));
        }

        /** Lang key of the harbour's name. */
        public String naam() {
            return "vadsrijk.luchtschip.haven." + id;
        }
    }

    /**
     * One line: {@code a} &lt;-&gt; {@code b}, control points {@code via} from a to b (world), its own phase in the timetable, and
     * (tests) an optional fixed duration / period. {@code letter} is the name on the boards (0, A, B, ...).
     */
    public record Lijn(String id, String letter, Haven a, Haven b, List<Vec3> via, int fase, int vasteDuur, int periode, boolean test) {
        public Lijn(String id, String letter, Haven a, Haven b, List<Vec3> via, int fase) {
            this(id, letter, a, b, via, fase, 0, PERIODE, false);
        }

        /** The harbour on the other side. */
        public Haven ander(Haven h) {
            return h == a ? b : a;
        }

        public String naam() {
            return "vadsrijk.luchtschip.lijn." + id;
        }

        /**
         * Does it fly in the world? Line 0 only once its two harbour POIs exist: Kampsteiger and De Haven already have the old
         * anchored block airship of the scene flight, so the real ship waits until t-wereld places its own piers (dev
         * {@code rij 0} works anyway). All other lines fly at their planned harbours.
         */
        public boolean actief() {
            return !id.equals("kamp_z1") || zoek(a.id()) != null && zoek(b.id()) != null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------ harbours

    public static final Haven KAMPSTEIGER = new Haven("haven_kamp_z1_vertrek", new BlockPos(-235, 87, 1008), 270f, "kamp");
    public static final Haven HAVEN_ZWAMZOET = new Haven("haven_z1_kamp_vertrek", new BlockPos(92, 65, 500), 0f, "z1");
    public static final Haven NOORDKLIFHAVEN = new Haven("haven_z1_z2_vertrek", new BlockPos(150, 121, -600), 180f, "z1");
    public static final Haven KANDIJKOM_DOK = new Haven("haven_z2_z1_vertrek", new BlockPos(704, 91, -900), 0f, "z2");
    public static final Haven GALMDOK = new Haven("haven_z2_z3_vertrek", new BlockPos(955, 111, -835), 270f, "z2");
    public static final Haven WESTSTEIGER = new Haven("haven_z3_z2_vertrek", new BlockPos(1190, 171, -590), 90f, "z3");
    public static final Haven OOSTTAKSTEIGER = new Haven("haven_z3_z4_vertrek", new BlockPos(1630, 171, -500), 270f, "z3");
    public static final Haven LAMPIONHAVEN = new Haven("haven_z4_z3_vertrek", new BlockPos(1800, 71, -470), 90f, "z4");
    // (T-INT: t-z4wereld's ship anchor haven_4_5_luchtschip lies EAST of the pier: the bow points east)
    public static final Haven OPSTIJGPIER = new Haven("haven_z4_z5_vertrek", new BlockPos(2530, 73, -565), 270f, "z4");
    // (T-INT: t-z5wereld's real dock, the whale hangs 20 lower)
    public static final Haven WALVISRUGHAVEN = new Haven("haven_z5_z4_vertrek", new BlockPos(2630, 217, -360), 0f, "z5");

    // ------------------------------------------------------------------------------------------------------------ the lines

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    /** Line 0 (existed as a scene flight): Kampsteiger -> Haven Zwamzoet, over the Zoete Zee. */
    public static final Lijn L0 = new Lijn("kamp_z1", "0", KAMPSTEIGER, HAVEN_ZWAMZOET, List.of(
            v(-150, 100, 990), v(-60, 118, 900), v(20, 128, 790), v(80, 118, 680), v(100, 98, 590)), 0);
    /** Line A: Noordklifhaven -> Kandijkom-dok, climbing along the Splinterwand and round the spires. */
    public static final Lijn LA = new Lijn("z1_z2", "A", NOORDKLIFHAVEN, KANDIJKOM_DOK, List.of(
            v(160, 132, -660), v(250, 158, -730), v(390, 176, -800), v(520, 168, -870), v(640, 140, -820), v(700, 112, -862)), 700);
    /** Line B: Galmdok -> Weststeiger, diving into the Wolkenkloof and climbing along the hanging roots. */
    public static final Lijn LB = new Lijn("z2_z3", "B", GALMDOK, WESTSTEIGER, List.of(
            v(1010, 104, -836), v(1062, 74, -790), v(1100, 88, -716), v(1124, 128, -650), v(1150, 168, -598)), 1400);
    /** Line C: Oosttaksteiger -> Lampionhaven, a round past the crown and the werf, then down through the Wolkenval. */
    public static final Lijn LC = new Lijn("z3_z4", "C", OOSTTAKSTEIGER, LAMPIONHAVEN, List.of(
            v(1690, 190, -540), v(1712, 208, -680), v(1640, 222, -830), v(1730, 200, -850), v(1800, 160, -700), v(1770, 112, -560),
            v(1752, 82, -478)), 2100);
    /** Line D: De Opstijgpier -> Walvisrughaven, a spiral up along the head and the eye of Moeder Wolkwal (after the zone-4 finale). */
    public static final Lijn LD = new Lijn("z4_z5", "D", OPSTIJGPIER, WALVISRUGHAVEN, List.of(
            v(2560, 96, -610), v(2500, 126, -680), v(2420, 160, -600), v(2420, 186, -470), v(2530, 205, -330), v(2630, 218, -300)), 2800);   // (T-INT: end lowered ~45 to the real Walvisrughaven, approach from the south)

    /** All lines (tests may add a test line). */
    public static final List<Lijn> ALLE = new CopyOnWriteArrayList<>(List.of(L0, LA, LB, LC, LD));

    /** Flag set by the zone-4 story (t-z4verhaal) when the finale is done: line D (up to Hoogvads) opens. */
    public static final String VLAG_Z4_FINALE = "zone4_klaar";

    // ------------------------------------------------------------------------------------------------------------ POIs

    /**
     * Where harbour POIs are looked up (first hit wins). Zone 1-3's generated POI classes are here; t-z4wereld / integration may
     * add the zone-4/5 class with one line: {@code Lijnen.ZOEKERS.add(Z4WereldPlekken::plek)}.
     */
    public static final List<Function<String, BlockPos>> ZOEKERS = new CopyOnWriteArrayList<>(List.of(
            id -> veilig(nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.ALLE, id),
            id -> veilig(nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ALLE, id),
            id -> veilig(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ALLE, id),
            // T-INT: zone 4 publishes haven_3_4_* / haven_4_5_* (stand spot + ship anchor): the pier spot is the anchor minus UIT
            Lijnen::zone4Haven,
            id -> veilig(nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE, id),
            id -> veilig(nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.ALLE, id)));

    /** T-INT: the zone-4 harbours from t-z4wereld's anchors, so the docked ship floats exactly at haven_*_luchtschip. */
    @Nullable
    private static BlockPos zone4Haven(String id) {
        String anker = id.equals("haven_z4_z3_vertrek") ? "haven_3_4" : id.equals("haven_z4_z5_vertrek") ? "haven_4_5" : null;
        if (anker == null) {
            return null;
        }
        BlockPos schip = nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE.get(anker + "_luchtschip");
        BlockPos land = nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE.get(anker + "_landing");
        if (schip == null || land == null) {
            return null;
        }
        Haven h = id.equals("haven_z4_z3_vertrek") ? LAMPIONHAVEN : OPSTIJGPIER;
        Vec3 u = h.uit().scale(Haven.UIT);
        return BlockPos.containing(schip.getX() - u.x, land.getY(), schip.getZ() - u.z);
    }

    @Nullable
    private static BlockPos veilig(Map<String, BlockPos> m, String id) {
        return m.get(id);
    }

    @Nullable
    public static BlockPos zoek(String id) {
        for (Function<String, BlockPos> z : ZOEKERS) {
            try {
                BlockPos b = z.apply(id);
                if (b != null) {
                    return b;
                }
            } catch (RuntimeException ignored) {
                // a POI class that is not there in this build
            }
        }
        return null;
    }

    @Nullable
    public static Lijn lijn(String id) {
        for (Lijn l : ALLE) {
            if (l.id().equals(id) || l.letter().equalsIgnoreCase(id)) {
                return l;
            }
        }
        return null;
    }

    /** Exactly this id (the synced id of a ship). */
    @Nullable
    public static Lijn vanId(String id) {
        for (Lijn l : ALLE) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /** All harbours (each line's two). */
    public static List<Haven> havens() {
        List<Haven> l = new ArrayList<>();
        for (Lijn lijn : ALLE) {
            if (!lijn.test() && lijn.actief()) {
                l.add(lijn.a());
                l.add(lijn.b());
            }
        }
        return l;
    }

    @Nullable
    public static Haven haven(String id) {
        for (Lijn l : ALLE) {
            if (l.a().id().equals(id)) {
                return l.a();
            }
            if (l.b().id().equals(id)) {
                return l.b();
            }
        }
        return null;
    }

    /** The line that departs from this harbour. */
    @Nullable
    public static Lijn lijnVan(Haven h) {
        for (Lijn l : ALLE) {
            if (l.a() == h || l.b() == h) {
                return l;
            }
        }
        return null;
    }

    private Lijnen() {
    }
}
