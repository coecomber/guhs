package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.4.2: the data side of the Reisguh menu (ReisVolgorde): own order per player, favourites on top, moving rows (also over
 * a page border), sort mode, new Reisguhs at the end, Reisguhs that are gone, saving and old saves. On a loose
 * Reisguh.Data (the test server has no Guhmension, and the real list must stay untouched).
 */
public class ReisVolgordeGameTests {
    private static final String EMPTY = "empty";

    /** A fresh list with Reisguhs named A, B, C... at x = 100, 200, 300... */
    private static List<UUID> punten(Reisguh.Data data, String... namen) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < namen.length; i++) {
            UUID id = UUID.randomUUID();
            data.points.put(id, new Reisguh.Point(id, Component.literal(namen[i]), new BlockPos(100 * (i + 1), 70, 0), 0));
            ids.add(id);
        }
        return ids;
    }

    private static String namen(List<Reisguh.Point> rijen) {
        StringBuilder s = new StringBuilder();
        rijen.forEach(p -> s.append(p.name().getString()));
        return s.toString();
    }

    private static void gelijk(GameTestHelper helper, Object actual, Object expected, String what) {
        helper.assertTrue(expected.equals(actual), what + ": " + actual + " (expected " + expected + ")");
    }

    private static String menu(Reisguh.Data data, UUID speler, UUID hier) {
        return namen(ReisVolgorde.rijen(data, speler, hier));
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeNieuweAchteraanEnPerSpeler(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        List<UUID> p = punten(data, "A", "B", "C", "D", "E");
        UUID jan = UUID.randomUUID(), piet = UUID.randomUUID(), hier = p.get(4);
        for (int i : new int[]{2, 0, 4}) {
            helper.assertTrue(ReisVolgorde.ontdek(data, jan, p.get(i)), "discovering " + i);
        }
        helper.assertFalse(ReisVolgorde.ontdek(data, jan, p.get(0)), "twice is not new");
        gelijk(helper, menu(data, jan, hier), "CA", "the order you discovered them in, without the Reisguh you stand at");
        ReisVolgorde.ontdek(data, jan, p.get(1));
        gelijk(helper, menu(data, jan, hier), "CAB", "a new one goes at the end");
        ReisVolgorde.ontdek(data, piet, p.get(3));
        ReisVolgorde.ontdek(data, piet, p.get(0));
        gelijk(helper, menu(data, piet, hier), "DA", "every player has an own order");
        gelijk(helper, menu(data, jan, p.get(0)), "CEB", "standing at A");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeFavorietenBovenaan(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        List<UUID> p = punten(data, "A", "B", "C", "D", "Z");
        UUID jan = UUID.randomUUID(), hier = p.get(4);
        p.forEach(id -> ReisVolgorde.ontdek(data, jan, id));
        helper.assertTrue(ReisVolgorde.wisselFavoriet(data, jan, p.get(2)), "star C");
        helper.assertTrue(ReisVolgorde.wisselFavoriet(data, jan, p.get(3)), "star D");
        gelijk(helper, menu(data, jan, hier), "CDAB", "favourites on top, in the own order");
        helper.assertFalse(ReisVolgorde.wisselFavoriet(data, UUID.randomUUID(), p.get(0)), "nobody stars a Reisguh they don't know");
        helper.assertFalse(ReisVolgorde.wisselFavoriet(data, jan, UUID.randomUUID()), "or one that doesn't exist");
        ReisVolgorde.wisselFavoriet(data, jan, p.get(2));
        gelijk(helper, menu(data, jan, hier), "DABC", "unstarred: back to its own place");
        helper.assertFalse(ReisVolgorde.favoriet(data, jan, p.get(2)), "C is no favourite any more");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeSchuivenOokOverEenPagina(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        List<UUID> p = punten(data, "A", "B", "C", "D", "E", "F", "G", "H", "I", "Z");
        UUID jan = UUID.randomUUID(), hier = p.get(9);
        p.forEach(id -> ReisVolgorde.ontdek(data, jan, id));
        // a page of 7 rows: G is the last of page 1, H the first of page 2
        helper.assertTrue(ReisVolgorde.schuif(data, jan, hier, p.get(7), -1), "H up");
        gelijk(helper, menu(data, jan, hier), "ABCDEFHGI", "H moved onto page 1");
        helper.assertTrue(ReisVolgorde.schuif(data, jan, hier, p.get(7), 1), "H down again");
        helper.assertTrue(ReisVolgorde.schuif(data, jan, hier, p.get(0), 1), "A down");
        gelijk(helper, menu(data, jan, hier), "BACDEFGHI", "A one down");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(1), -1), "the top row can't go up");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(8), 1), "the last row can't go down");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(9), -1), "the Reisguh you stand at isn't in the menu");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(2), 2), "one step at a time");
        helper.assertFalse(ReisVolgorde.schuif(data, UUID.randomUUID(), hier, p.get(2), 1), "only your own list");
        // with a favourite: rows never cross the border between favourites and the rest
        ReisVolgorde.wisselFavoriet(data, jan, p.get(4));
        gelijk(helper, menu(data, jan, hier), "EBACDFGHI", "E starred");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(4), 1), "the favourite stays on top");
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(1), -1), "and B can't go above it");
        helper.assertTrue(ReisVolgorde.schuif(data, jan, hier, p.get(3), -1), "D up: over the spot where E was in the own order");
        gelijk(helper, menu(data, jan, hier), "EBADCFGHI", "D swapped with C");
        // only in the own-order mode
        ReisVolgorde.zetSortering(data, jan, ReisVolgorde.NAAM);
        helper.assertFalse(ReisVolgorde.schuif(data, jan, hier, p.get(3), -1), "no moving when sorted by name");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeSorteerModus(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        UUID jan = UUID.randomUUID(), piet = UUID.randomUUID();
        helper.assertTrue(ReisVolgorde.sortering(data, jan) == ReisVolgorde.EIGEN, "own order by default");
        helper.assertTrue(ReisVolgorde.zetSortering(data, jan, ReisVolgorde.AFSTAND), "distance");
        helper.assertFalse(ReisVolgorde.zetSortering(data, jan, 3) || ReisVolgorde.zetSortering(data, jan, -1), "no unknown modes");
        helper.assertTrue(ReisVolgorde.sortering(data, jan) == ReisVolgorde.AFSTAND && ReisVolgorde.sortering(data, piet) == ReisVolgorde.EIGEN, "per player");
        // the screen's sort: favourites on top, then distance / name
        record R(String naam, int afstand, boolean fav) {
        }
        List<R> rows = new ArrayList<>(List.of(new R("bram", 500, false), new R("Anna", 900, false), new R("Cees", 100, true), new R("dirk", 50, false)));
        ReisVolgorde.sorteer(rows, ReisVolgorde.AFSTAND, R::fav, R::afstand, R::naam);
        gelijk(helper, rows.stream().map(R::naam).toList(), List.of("Cees", "dirk", "bram", "Anna"), "by distance");
        ReisVolgorde.sorteer(rows, ReisVolgorde.NAAM, R::fav, R::afstand, R::naam);
        gelijk(helper, rows.stream().map(R::naam).toList(), List.of("Cees", "Anna", "bram", "dirk"), "by name, no case");
        helper.assertTrue(ReisVolgorde.kanSchuiven(rows, 1, 1, R::fav) && !ReisVolgorde.kanSchuiven(rows, 1, -1, R::fav)
                && !ReisVolgorde.kanSchuiven(rows, 3, 1, R::fav), "the arrows");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeVerdwenenEnTerug(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        List<UUID> p = punten(data, "A", "B", "C", "Z");
        UUID jan = UUID.randomUUID(), hier = p.get(3);
        p.forEach(id -> ReisVolgorde.ontdek(data, jan, id));
        ReisVolgorde.wisselFavoriet(data, jan, p.get(1));
        Reisguh.Point b = data.points.remove(p.get(1));               // (picked up)
        gelijk(helper, menu(data, jan, hier), "AC", "a Reisguh that is gone is not in the menu");
        helper.assertFalse(ReisVolgorde.wisselFavoriet(data, jan, p.get(1)), "and can't be starred");
        helper.assertTrue(ReisVolgorde.schuif(data, jan, hier, p.get(2), -1), "moving over the gap");
        gelijk(helper, menu(data, jan, hier), "CA", "C up");
        data.points.put(b.id(), b);                                   // (put down again)
        gelijk(helper, menu(data, jan, hier), "BCA", "back: still a favourite, on top");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "reisvolgorde")
    public static void reisvolgordeOpgeslagenEnOudeWerelden(GameTestHelper helper) {
        Reisguh.Data data = new Reisguh.Data();
        List<UUID> p = punten(data, "A", "B", "C", "D", "Z");
        UUID jan = UUID.randomUUID(), oud = UUID.randomUUID(), hier = p.get(4);
        p.forEach(id -> ReisVolgorde.ontdek(data, jan, id));
        ReisVolgorde.schuif(data, jan, hier, p.get(3), -1);
        ReisVolgorde.wisselFavoriet(data, jan, p.get(2));
        ReisVolgorde.zetSortering(data, jan, ReisVolgorde.NAAM);
        // a player from before 1.4.2: only "Discovered"
        data.of(oud).add(p.get(3));
        data.of(oud).add(p.get(0));
        CompoundTag tag = data.save(new CompoundTag(), null);
        helper.assertTrue(tag.getCompoundOrEmpty("Order").contains(jan.toString()), "the order is saved");
        Reisguh.Data terug = Reisguh.Data.load(tag, null);
        gelijk(helper, menu(terug, jan, hier), "CABD", "order and favourites survive a restart");
        helper.assertTrue(ReisVolgorde.sortering(terug, jan) == ReisVolgorde.NAAM, "and the sort mode");
        gelijk(helper, menu(terug, oud, hier), "AD", "an old player starts in the order the Reisguhs came into the world");
        // an old save without any of it loads fine
        tag.remove("Order");
        tag.remove("Fav");
        tag.remove("Sort");
        Reisguh.Data heelOud = Reisguh.Data.load(tag, null);
        gelijk(helper, menu(heelOud, jan, hier), "ABCD", "a save from 1.4.1");
        helper.assertTrue(ReisVolgorde.sortering(heelOud, jan) == ReisVolgorde.EIGEN, "own order");
        helper.succeed();
    }

    private ReisVolgordeGameTests() {
    }
}
