package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * 1.4.2: the Reisguh menu per player: an own order of the Reisguhs you know, favourites (always on top) and how the list is
 * sorted (own order, distance, name). Stored in {@link Reisguh.Data} (keys "Order", "Fav", "Sort"; worlds from before 1.4.2
 * have none: the own order then starts as the order the Reisguhs came into the world). New Reisguhs you discover go at the
 * end of your own order. A Reisguh that is gone (picked up) is only hidden: when it is put down again it is back at its
 * old spot. Everything the screen asks for is checked here (only Reisguhs you know, only moving in your own order).
 * The screen: client/screen/ReisguhScreen.
 */
public final class ReisVolgorde {
    /** The sort modes, in the order the button cycles through them. */
    public static final int EIGEN = 0, AFSTAND = 1, NAAM = 2, MODI = 3;
    public static final String[] MODUS_SLEUTEL = {"eigen", "afstand", "naam"};

    /**
     * The player's own order (the stored list itself): every Reisguh they discovered is in it; the first time (or for one
     * discovered elsewhere) the missing ones are added at the end, in the order they came into the world.
     */
    static List<UUID> eigen(Reisguh.Data data, UUID player) {
        Set<UUID> mine = data.of(player);
        List<UUID> list = data.volgorde.computeIfAbsent(player, k -> new ArrayList<>());
        if (list.size() < mine.size() || !list.containsAll(mine)) {
            Set<UUID> al = new HashSet<>(list);
            for (UUID id : data.points.keySet()) {
                if (mine.contains(id) && al.add(id)) {
                    list.add(id);
                }
            }
            for (UUID id : mine) {                                  // (known but not in the world right now)
                if (al.add(id)) {
                    list.add(id);
                }
            }
            data.setDirty();
        }
        return list;
    }

    /** Discovers a Reisguh for a player: it goes at the end of their own order. True when it was new. */
    static boolean ontdek(Reisguh.Data data, UUID player, UUID point) {
        List<UUID> list = eigen(data, player);
        if (!data.of(player).add(point)) {
            return false;
        }
        if (!list.contains(point)) {
            list.add(point);
        }
        data.setDirty();
        return true;
    }

    public static boolean favoriet(Reisguh.Data data, UUID player, UUID point) {
        Set<UUID> fav = data.favorieten.get(player);
        return fav != null && fav.contains(point);
    }

    /** Star / unstar a Reisguh the player knows. False (nothing changes) for one they don't know or that isn't there. */
    public static boolean wisselFavoriet(Reisguh.Data data, UUID player, UUID point) {
        if (!data.of(player).contains(point) || !data.points.containsKey(point)) {
            return false;
        }
        Set<UUID> fav = data.favorieten.computeIfAbsent(player, k -> new HashSet<>());
        if (!fav.remove(point)) {
            fav.add(point);
        }
        data.setDirty();
        return true;
    }

    public static int sortering(Reisguh.Data data, UUID player) {
        return data.sortering.getOrDefault(player, EIGEN);
    }

    /** Remembers how the player sorts the list; anything but a known mode is ignored. */
    public static boolean zetSortering(Reisguh.Data data, UUID player, int modus) {
        if (modus < 0 || modus >= MODI) {
            return false;
        }
        if (modus == EIGEN) {
            data.sortering.remove(player);
        } else {
            data.sortering.put(player, modus);
        }
        data.setDirty();
        return true;
    }

    /**
     * The menu at Reisguh {@code hier} in the own order: favourites first (in their own order), then the rest. Only Reisguhs
     * that are there, never {@code hier} itself.
     */
    public static List<Reisguh.Point> rijen(Reisguh.Data data, UUID player, UUID hier) {
        List<Reisguh.Point> out = new ArrayList<>();
        for (UUID id : eigen(data, player)) {
            Reisguh.Point p = data.points.get(id);
            if (p != null && !id.equals(hier)) {
                out.add(p);
            }
        }
        sorteer(out, EIGEN, p -> favoriet(data, player, p.id()), p -> 0, p -> "");
        return out;
    }

    /**
     * Moves a row one place up ({@code richting} -1) or down (+1) in the own order as the menu at {@code hier} shows it
     * (so also from the last row of a page to the first of the next). Only in the own-order mode, only within the
     * favourites or within the rest. False when nothing moved.
     */
    public static boolean schuif(Reisguh.Data data, UUID player, UUID hier, UUID point, int richting) {
        if ((richting != -1 && richting != 1) || sortering(data, player) != EIGEN) {
            return false;
        }
        List<Reisguh.Point> rijen = rijen(data, player, hier);
        int i = -1;
        for (int k = 0; k < rijen.size(); k++) {
            if (rijen.get(k).id().equals(point)) {
                i = k;
            }
        }
        int j = i + richting;
        if (i < 0 || j < 0 || j >= rijen.size()) {
            return false;
        }
        UUID ander = rijen.get(j).id();
        if (favoriet(data, player, point) != favoriet(data, player, ander)) {
            return false;
        }
        List<UUID> list = eigen(data, player);
        int a = list.indexOf(point), b = list.indexOf(ander);
        Collections.swap(list, a, b);
        data.setDirty();
        return true;
    }

    /**
     * Sorts rows the way the menu shows them (stable): favourites on top, then by mode (own order = as they are, distance,
     * name without case). Common code: the server for the own order, the screen for all three (names are read in the
     * player's language there).
     */
    public static <T> void sorteer(List<T> rows, int modus, Predicate<T> fav, ToIntFunction<T> afstand, Function<T, String> naam) {
        Comparator<T> c = Comparator.comparing((T t) -> !fav.test(t));
        if (modus == AFSTAND) {
            c = c.thenComparingInt(afstand);
        } else if (modus == NAAM) {
            c = c.thenComparing(naam, String.CASE_INSENSITIVE_ORDER);
        }
        rows.sort(c);
    }

    /** Can this row move that way in the shown list (same group of favourites on both sides)? For the arrows. */
    public static <T> boolean kanSchuiven(List<T> rows, int i, int richting, Predicate<T> fav) {
        int j = i + richting;
        return i >= 0 && i < rows.size() && j >= 0 && j < rows.size() && fav.test(rows.get(i)) == fav.test(rows.get(j));
    }

    // --- saving (called from Reisguh.Data) ------------------------------------------------------------------------------

    static void save(Reisguh.Data data, CompoundTag tag) {
        tag.put("Order", lijsten(data.volgorde));
        tag.put("Fav", lijsten(data.favorieten));
        CompoundTag sort = new CompoundTag();
        data.sortering.forEach((player, modus) -> sort.putInt(player.toString(), modus));
        tag.put("Sort", sort);
    }

    static void load(Reisguh.Data data, CompoundTag tag) {
        CompoundTag order = tag.getCompoundOrEmpty("Order");
        for (String key : order.keySet()) {
            data.volgorde.put(UUID.fromString(key), new ArrayList<>(ids(order.getListOrEmpty(key))));
        }
        CompoundTag fav = tag.getCompoundOrEmpty("Fav");
        for (String key : fav.keySet()) {
            data.favorieten.put(UUID.fromString(key), new HashSet<>(ids(fav.getListOrEmpty(key))));
        }
        CompoundTag sort = tag.getCompoundOrEmpty("Sort");
        for (String key : sort.keySet()) {
            int modus = sort.getIntOr(key, EIGEN);
            if (modus > EIGEN && modus < MODI) {
                data.sortering.put(UUID.fromString(key), modus);
            }
        }
    }

    private static CompoundTag lijsten(Map<UUID, ? extends java.util.Collection<UUID>> map) {
        CompoundTag out = new CompoundTag();
        map.forEach((player, set) -> {
            ListTag ids = new ListTag();
            set.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
            out.put(player.toString(), ids);
        });
        return out;
    }

    private static List<UUID> ids(ListTag list) {
        List<UUID> out = new ArrayList<>();
        for (Tag t : list) {
            try {
                UUID id = UUID.fromString(t.asString().orElse(""));
                if (!out.contains(id)) {
                    out.add(id);
                }
            } catch (IllegalArgumentException e) {
                // (a broken entry: skipped)
            }
        }
        return out;
    }

    private ReisVolgorde() {
    }
}
