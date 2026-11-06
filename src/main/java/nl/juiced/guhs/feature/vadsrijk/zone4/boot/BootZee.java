package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * T-Z4BOOT: the sea of zone 4 "De Lampionkust": De Kringstroom (the current round the island chain) and the aanlegsteigers (docks).
 * <p><b>Kringstroom:</b> an ellipse round the islands of WERELDINDELING.md (zone box x 1740..2540, z -716..-266, with the
 * Mysterie-eiland (2480, -330) and the Opstijgpier (2525, -565) still inside): centre (2140, -491), radii 470 x 290; the band runs
 * from 0.93 to 1.0 of it. t-z4wereld can move it: {@link #zetKringstroom}.
 * <p><b>Docks:</b> taken from the POI list of zone 4 when it is there ({@code Z4WereldPlekken.ALLE}, every id starting with
 * {@code boot_steiger_}; t-z4wereld, POI_Z4.md); until then the PLACEHOLDERS below (by each island's middle, y 64). The {@code plek}
 * of a dock is where you stand on its planks after mooring.
 */
public final class BootZee {
    // (T-INT: = t-z4wereld's real current, tools/vadsrijk/zone4/plan4.py STROOM (2150, -470, 470, 255) + STROOM_D 0.93)
    public static final double CX = 2150, CZ = -470, A = 470, B = 255, BINNEN = 0.93, BUITEN = 1.0;
    /** Beyond this fraction of the ellipse there is no current at all (a dev test boat elsewhere in the dimension sails freely). */
    public static final double STROOM_TOT = 1.4;
    /** How close (blocks, horizontally) the boat must be to a dock's spot to moor there. */
    public static final double AANLEG_AFSTAND = 10;
    /** The POI id prefix of a boat dock in the zone-4 POI list. */
    public static final String POI_PREFIX = "boot_dok_";   // (T-INT: t-z4wereld publishes boot_dok_<island> + _water/_wal helper spots)
    /** The dock you come back to when no other is known (and where the story hands you the boat). */
    public static final String THUIS = "lampionhaven";

    private static volatile BootFysica.Kringstroom kring = new BootFysica.Kringstroom(CX, CZ, A, B, BINNEN, BUITEN);

    /** One dock: id (without the prefix), lang key of its name, the standing spot on the planks. */
    public record Steiger(String id, BlockPos plek, boolean placeholder) {
        public String naamKey() {
            return "vadsrijk.z4boot.steiger." + id;
        }
    }

    /** PLACEHOLDER docks (t-z4wereld replaces them with POI ids {@code boot_steiger_<id>}). */
    static final List<Steiger> PLACEHOLDERS = List.of(
            new Steiger("lampionhaven", new BlockPos(1880, 64, -470), true),
            new Steiger("knuffeltij", new BlockPos(1935, 64, -322), true),
            new Steiger("bloemeneiland", new BlockPos(2085, 64, -380), true),
            new Steiger("strandfeest", new BlockPos(1995, 64, -580), true),
            new Steiger("koraalbaai", new BlockPos(2160, 64, -540), true),
            new Steiger("vliegereiland", new BlockPos(2300, 64, -640), true),
            new Steiger("vuurtoren", new BlockPos(2375, 64, -500), true),
            new Steiger("mysterie", new BlockPos(2455, 64, -340), true),
            new Steiger("opstijgpier", new BlockPos(2465, 64, -545), true));

    private static volatile List<Steiger> steigers;

    private BootZee() {
    }

    public static BootFysica.Kringstroom kring() {
        return kring;
    }

    /** API (t-z4wereld): move the current (centre, radii, band start as a fraction, e.g. 0.93). */
    public static void zetKringstroom(double cx, double cz, double a, double b, double binnen) {
        kring = new BootFysica.Kringstroom(cx, cz, a, b, Math.min(0.99, binnen), BUITEN);
    }

    /** Is this spot on the zone-4 sea (inside the outer line of the current)? */
    public static boolean opZee(double x, double z) {
        return kring.binnen(x, z);
    }

    /** All docks: from the zone-4 POIs when there are any, else the placeholders. */
    public static List<Steiger> steigers() {
        List<Steiger> s = steigers;
        if (s == null) {
            s = uitPois();
            steigers = s;
        }
        return s;
    }

    /** (tests / dev) forget the cached list, or set one. */
    static void zetSteigers(@Nullable List<Steiger> lijst) {
        steigers = lijst;
    }

    @Nullable
    public static Steiger steiger(String id) {
        for (Steiger s : steigers()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** The nearest dock within {@code max} blocks (horizontal), or null. */
    @Nullable
    public static Steiger dichtbij(double x, double z, double max) {
        Steiger beste = null;
        double b = max * max;
        for (Steiger s : steigers()) {
            double dx = s.plek().getX() + 0.5 - x, dz = s.plek().getZ() + 0.5 - z;
            double d = dx * dx + dz * dz;
            if (d <= b) {
                b = d;
                beste = s;
            }
        }
        return beste;
    }

    /** The home dock (Lampionhaven), or the first one. */
    public static Steiger thuis() {
        Steiger s = steiger(THUIS);
        return s != null ? s : steigers().get(0);
    }

    /** Reads {@code nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ALLE} (or {@code plek(id)}) when that class exists. */
    @SuppressWarnings("unchecked")
    private static List<Steiger> uitPois() {
        List<Steiger> l = new ArrayList<>();
        for (String naam : new String[] {"nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken", "nl.juiced.guhs.feature.vadsrijk.zone4.Z4WereldPlekken"}) {
            try {
                Class<?> c = Class.forName(naam);
                Field f = c.getField("ALLE");
                Object o = f.get(null);
                if (o instanceof Map<?, ?> m) {
                    for (Map.Entry<?, ?> e : ((Map<Object, Object>) m).entrySet()) {
                        if (e.getKey() instanceof String id && id.startsWith(POI_PREFIX) && !id.endsWith("_water") && !id.endsWith("_wal")
                                && e.getValue() instanceof BlockPos p) {
                            l.add(new Steiger(id.substring(POI_PREFIX.length()), p, false));
                        }
                    }
                }
                if (l.isEmpty()) {   // (no ALLE map: ask plek(id) for the placeholder ids)
                    Method plek = c.getMethod("plek", String.class);
                    for (Steiger s : PLACEHOLDERS) {
                        if (plek.invoke(null, POI_PREFIX + s.id()) instanceof BlockPos p) {
                            l.add(new Steiger(s.id(), p, false));
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // (zone 4 POIs not there yet)
            }
            if (!l.isEmpty()) {
                l.sort((a, b) -> a.id().equals(THUIS) ? -1 : b.id().equals(THUIS) ? 1 : a.id().compareTo(b.id()));
                return List.copyOf(l);
            }
        }
        return PLACEHOLDERS;
    }
}
