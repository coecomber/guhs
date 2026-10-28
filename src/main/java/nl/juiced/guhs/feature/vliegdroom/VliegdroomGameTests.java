package nl.juiced.guhs.feature.vliegdroom;

import java.util.List;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/** 1.8.0 uitbreiding: the Vliegdroom (the island's spots, the parcours' bookkeeping, the diploma and the record). */
public class VliegdroomGameTests {
    private static final String BATCH = "vliegdroom", KAMER = "vadskracht_test_kamer";

    static int tel(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** The island hangs above the plein, has a template and enough rings, and every spot belongs to it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void vliegdroomPlekken(GameTestHelper helper) {
        var o = DroomEilanden.opzet(DroomEilanden.VLIEG);
        helper.assertTrue(o.versie() >= 1 && o.template() != null, "the Vliegdroom has its own template");
        helper.assertTrue(DroomEilanden.midden(DroomEilanden.VLIEG).getY() >= DroomEilanden.VLIEG_VANAF, "high above the plein");
        helper.assertTrue(DroomEilanden.dichtstbij(Vec3.atCenterOf(DroomEilanden.midden(DroomEilanden.VLIEG))).equals(DroomEilanden.VLIEG),
                "up there you are at the Vliegdroom");
        var p = VliegPlekken.get();
        helper.assertTrue(p.ringen().size() >= 8, "a parcours of at least eight rings: " + p.ringen().size());
        helper.assertTrue(p.stal().size() >= 2, "Rookguhs to ride in the stal");
        helper.assertTrue(VliegdroomFeature.plekken().size() == 2 + p.stal().size(), "a Rookguh spot each");
        helper.succeed();
    }

    /** Rings in order; one skipped waits; the clock runs from the first to the last. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void parcoursTeltRingen(GameTestHelper helper) {
        UUID wie = UUID.randomUUID();
        List<Vec3> m = List.of(new Vec3(0, 0, 0), new Vec3(10, 0, 0), new Vec3(20, 0, 0));
        List<Double> r = List.of(3.0, 3.0, 3.0);
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(20, 0, 0), 0, m, r) == VliegParcours.Stap.NIETS, "the last ring first does nothing");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(0.5, 0, 0), 10, m, r) == VliegParcours.Stap.BEGIN, "the first ring starts the clock");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(20, 0, 0), 20, m, r) == VliegParcours.Stap.NIETS, "skipping waits");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(10, 1, 0), 30, m, r) == VliegParcours.Stap.RING, "the second ring");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(19, 0, 1), 70, m, r) == VliegParcours.Stap.KLAAR, "the last ring: done");
        helper.assertTrue(VliegParcours.tijd(wie) == 60, "sixty ticks from the first to the last ring");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(0, 0, 0), 80, m, r) == VliegParcours.Stap.BEGIN, "a new round");
        helper.assertTrue(VliegParcours.stap(wie, new Vec3(50, 0, 0), 80 + VliegParcours.KWIJT + 1, m, r) == VliegParcours.Stap.KWIJT,
                "too long without a ring: off");
        helper.succeed();
    }

    /** The first round: the diploma and droomzand; a slower one: nothing; a record the same day: once. */
    @SuppressWarnings("removal")
    @GuhTest(template = KAMER, batch = BATCH)
    public static void diplomaEnRecord(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        GuhQuests.saved(p).remove(VliegParcours.BESTE);
        GuhQuests.saved(p).remove(VliegParcours.DIPLOMA);
        GuhQuests.saved(p).remove(VliegParcours.DAG);
        var level = helper.getLevel();
        helper.assertTrue(VliegParcours.klaar(p, level, 900) == VliegParcours.Uitkomst.DIPLOMA, "the first round: the diploma");
        helper.assertTrue(tel(p.getInventory(), VliegdroomFeature.DIPLOMA.get()) == 1, "the Vlieg-diploma");
        helper.assertTrue(VliegParcours.klaar(p, level, 1000) == VliegParcours.Uitkomst.KLAAR, "slower: just the time");
        helper.assertTrue(VliegParcours.klaar(p, level, 800) == VliegParcours.Uitkomst.RECORD_BELONING, "a record: droomzand");
        helper.assertTrue(VliegParcours.klaar(p, level, 700) == VliegParcours.Uitkomst.RECORD, "another record the same day: no more");
        helper.assertTrue(GuhQuests.saved(p).getIntOr(VliegParcours.BESTE, 0) == 700, "the record is kept");
        helper.assertTrue(tel(p.getInventory(), VliegdroomFeature.DIPLOMA.get()) == 1, "one diploma only");
        helper.succeed();
    }

    /** (1.8.0 fix) Two Rookguhs on one spot (an older world, or a refill before the entities loaded): one stays. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void rookguhNooitDubbel(GameTestHelper helper) {
        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)));
        DroomRookguhEntity a = DroomRookguhEntity.maak(helper.getLevel(), DroomRookguhEntity.Rol.STEIGER, plek, 0f);
        DroomRookguhEntity b = DroomRookguhEntity.maak(helper.getLevel(), DroomRookguhEntity.Rol.STEIGER, plek, 0f);
        helper.assertTrue(a != null && b != null, "two Rookguhs made");
        helper.getLevel().addFreshEntity(a);
        helper.getLevel().addFreshEntity(b);
        List<DroomRookguhEntity> er = new java.util.ArrayList<>(List.of(a, b));
        helper.assertTrue(VliegdroomFeature.ruimDubbelOp(er) == 1, "one double cleared");
        helper.assertTrue(a.isAlive() != b.isAlive(), "exactly one Rookguh stays");
        (a.isAlive() ? a : b).discard();
        helper.succeed();
    }
}
