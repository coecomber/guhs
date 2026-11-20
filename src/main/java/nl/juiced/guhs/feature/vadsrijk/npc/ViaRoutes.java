package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.10.10 "via" routes for the quest tracker. A player could not find Ordemeester Wasja: the Nestkapel hangs UNDER the ring deck
 * of Honingtak, the only way in is the kapellift, and the tracker pointed straight through the floor.
 * <ul>
 *   <li>An entry is an area whose spots are reached only through an access point (a lift, a hatch, a gate).</li>
 *   <li>While the tracked spot lies in that area and the player is not inside, the step points at the access point instead. Its
 *       "where" line becomes the entry's own phrase ("eerst de kapellift omlaag: ..."), with the beam and the distance.</li>
 *   <li>Once the player is inside (the bottom stop counts), the real target shows again.</li>
 *   <li>An access point inside another entry's area is routed again (at most 3 hops), so chained lifts work.</li>
 * </ul>
 * Hooked in last in {@link QuestStap#ROUTES} ({@link #register}, after the zones' airship hops): a target across the sea first
 * becomes the airship, and that pilot is never inside a via area.
 * Zones add their entries with {@link #voegToe}; zone 3 adds its honeycomb cells in {@code zone3.wereld.Z3Liften#viaRoutes}.
 * Which spots need one: {@code python tools/vadsrijk/bereikcheck.py --via} (targets reached only through a lift or hatch).
 */
public final class ViaRoutes {
    /** Where a route applies. Block spots are tested at their middle ({@code x + 0.5, y, z + 0.5}); players at their feet. */
    @FunctionalInterface
    public interface Gebied {
        boolean in(double x, double y, double z);

        default boolean in(BlockPos b) {
            return in(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        }

        default boolean in(ServerPlayer p) {
            return in(p.getX(), p.getY(), p.getZ());
        }

        default Gebied of(Gebied ander) {
            return (x, y, z) -> in(x, y, z) || ander.in(x, y, z);
        }
    }

    /** An upright cylinder: horizontal radius r round (cx, cz), feet height y0..y1 (both included). */
    public static Gebied cilinder(double cx, double cz, double r, double y0, double y1) {
        return (x, y, z) -> y >= y0 && y <= y1 + 0.999 && (x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r;
    }

    /** An upright elliptic cylinder: radii rx, rz round (cx, cz), feet height y0..y1. */
    public static Gebied ellips(double cx, double cz, double rx, double rz, double y0, double y1) {
        return (x, y, z) -> y >= y0 && y <= y1 + 0.999 && sq((x - cx) / rx) + sq((z - cz) / rz) <= 1;
    }

    /** A box of whole blocks (both corners included). */
    public static Gebied doos(int x0, int y0, int z0, int x1, int y1, int z1) {
        return (x, y, z) -> x >= Math.min(x0, x1) && x < Math.max(x0, x1) + 1 && y >= Math.min(y0, y1) && y < Math.max(y0, y1) + 1
                && z >= Math.min(z0, z1) && z < Math.max(z0, z1) + 1;
    }

    private static double sq(double v) {
        return v * v;
    }

    /**
     * One area reached only through an access point.
     *
     * @param id      a name (logs, tests, {@link #voegToe} replaces the same id)
     * @param gebied  where the targets lie that need the route
     * @param binnen  where the player counts as inside (the area plus the bottom stop / landing): no route then
     * @param van     only from here (null = from anywhere outside); for areas you can also walk into from elsewhere, e.g. a
     *                honeycomb cell that a branch path reaches too: the lift is only the way from the deck
     * @param toegang the access point the tracker points at first (the lift's stand spot)
     * @param waarKey the "where" phrase shown instead ({@code vadsrijk.npc.stap.via.<id>}: "eerst de kapellift omlaag: ...")
     */
    public record Via(String id, Gebied gebied, Gebied binnen, @Nullable Gebied van, BlockPos toegang, String waarKey) {
    }

    private static final List<Via> LIJST = new CopyOnWriteArrayList<>();
    private static boolean geregistreerd;

    private ViaRoutes() {
    }

    /** Hooks the routes into the tracker; call once, after the zones added their own {@link QuestStap#ROUTES}. */
    public static synchronized void register() {
        if (!geregistreerd) {
            geregistreerd = true;
            QuestStap.ROUTES.add(ViaRoutes::route);
        }
    }

    /** Adds an entry (an entry with the same id is replaced). */
    public static void voegToe(Via v) {
        LIJST.removeIf(o -> o.id().equals(v.id()));
        LIJST.add(v);
    }

    public static List<Via> alle() {
        return List.copyOf(LIJST);
    }

    @Nullable
    public static Via van(String id) {
        for (Via v : LIJST) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return null;
    }

    /** The via entry this player needs first for a spot (null = walk straight there). */
    @Nullable
    public static Via nodig(ServerPlayer p, BlockPos doel) {
        for (Via v : LIJST) {
            if (v.gebied().in(doel) && !v.binnen().in(p) && (v.van() == null || v.van().in(p))) {
                return v;
            }
        }
        return null;
    }

    /** The {@link QuestStap#ROUTES} hook: the access point first while the target is inside an area the player is not in. */
    static QuestStap route(ServerPlayer p, QuestStap s) {
        BlockPos d = s.doel();
        if (d == null || LIJST.isEmpty()) {   // (no dimension check, like the airship hop: only Vadsrijk players have quest steps)
            return s;
        }
        Via laatste = null;
        for (int hop = 0; hop < 3; hop++) {
            Via v = nodig(p, d);
            if (v == null || v == laatste) {
                break;
            }
            laatste = v;
            d = v.toegang();
        }
        if (laatste == null) {
            return s;
        }
        return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), Component.translatable(laatste.waarKey()), d, s.extra());
    }
}
