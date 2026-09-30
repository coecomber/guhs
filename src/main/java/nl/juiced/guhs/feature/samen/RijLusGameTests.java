package nl.juiced.guhs.feature.samen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.RijGuard;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.landdiertjes.PluiseekhoorntjeEntity;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhmensionSpawner;

/**
 * 1.1.1: riding can never make a loop (an entity that indirectly rides itself: the server's entity tracker walks the
 * passengers of every entity for every player step and would never finish). Tries the known ride combos, also forced
 * ones and the ones around the 1.1.0 server freeze (two players, a muisje and an eekhoorntje on the shoulder, taming
 * and picking up a guh while something rides you, the kart with a guh on the second seat), and checks that no entity
 * ends up in a loop. Also: wild guhs outside the Guhmension and tamed guhs are still saved.
 */
public class RijLusGameTests {
    private static final String WEI = "samen_test_wei";
    private static final String BATCH = "rijlus";

    /** No entity in the test room rides itself, and walking every passenger list ends (like the entity tracker does). */
    static void geenLus(GameTestHelper helper, String wanneer) {
        for (Entity e : helper.getLevel().getEntities((Entity) null, helper.getBounds().inflate(8), x -> true)) {
            helper.assertTrue(!RijGuard.inLus(e), wanneer + ": " + e + " is in a riding loop");
            int n = 0;
            for (Entity p : e.getIndirectPassengers()) {
                helper.assertTrue(p != e && ++n < 64, wanneer + ": the passengers of " + e + " never end");
            }
        }
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void rijLusNooitOpJezelf(GameTestHelper helper) {
        ServerPlayer p = SamenGameTests.speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = SamenGameTests.guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity ander = SamenGameTests.guh(helper, p, new BlockPos(7, 1, 5));
        GuhEntity derde = SamenGameTests.guh(helper, p, new BlockPos(9, 1, 5));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!guh.startRiding(guh, true, true) && !guh.isPassenger(), "a guh can't ride itself (forced)");
            helper.assertTrue(!p.startRiding(p, true, true) && !p.isPassenger(), "a player can't ride themselves (forced)");
            helper.assertTrue(p.startRiding(guh, true, true), "you get on your guh");
            helper.assertTrue(!guh.startRiding(p, true, true) && guh.getVehicle() == null, "your guh can't get on you while you ride it");
            // (26.1: on the server nothing rides a player at all, not even forced: a player's type is never saved)
            helper.assertTrue(!ander.startRiding(p, true, true) && !ander.isPassenger(), "no guh on your head");
            helper.assertTrue(!RijGuard.mag(guh, p) && RijGuard.mag(ander, guh), "the guard sees the loop (and nothing else)");
            geenLus(helper, "you on your guh");
            p.stopRiding();
            helper.assertTrue(guh.startRiding(ander, true, true) && ander.startRiding(derde, true, true), "a three-high stack of guhs");
            helper.assertTrue(!derde.startRiding(guh, true, true) && !derde.startRiding(ander, true, true) && !derde.isPassenger(),
                    "it can't close into a ring (forced)");
            helper.assertTrue(!RijGuard.mag(derde, guh) && !RijGuard.mag(ander, guh) && RijGuard.mag(guh, derde), "the guard");
            geenLus(helper, "ring");
            guh.stopRiding();
            ander.stopRiding();
            SamenGameTests.weg(helper, p);
            helper.succeed();
        });
    }

    /** The combo of the 1.1.0 freeze: two players, shoulder critters, taming and picking up a guh while something rides you. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void rijLusSchouderEnOppakken(GameTestHelper helper) {
        ServerPlayer luc = SamenGameTests.speler(helper, new BlockPos(2, 1, 2));
        ServerPlayer coe = SamenGameTests.speler(helper, new BlockPos(4, 1, 2));
        PieppiepmuisjeEntity muis = PiepFeature.PIEPPIEPMUISJE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        muis.tame(coe);
        PluiseekhoorntjeEntity eekhoorn = helper.spawn(LanddiertjesFeature.PLUISEEKHOORNTJE.get(), new BlockPos(5, 1, 2));
        eekhoorn.tame(coe);
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 1, 8));
        wild.setWandering(false);
        GuhEntity rijdt = SamenGameTests.guh(helper, luc, new BlockPos(9, 1, 9));
        helper.runAfterDelay(2, () -> {
            Schouder.zet(coe, muis);
            eekhoorn.speciaal(coe);                                           // the shoulder is full: it stays down
            helper.assertTrue(Schouder.heeft(coe) && !eekhoorn.isRemoved(), "one maatje on the shoulder at a time");
            helper.assertTrue(!eekhoorn.startRiding(coe, true, true) && !eekhoorn.isPassenger(), "the eekhoorntje can't ride Coe (not even forced)");
            helper.assertTrue(!eekhoorn.startRiding(eekhoorn, true, true), "nor itself");
            helper.assertTrue(!rijdt.startRiding(luc, true, true), "a guh can't ride Luc");
            helper.assertTrue(luc.startRiding(rijdt, true, true), "Luc rides his own guh");
            wild.tame(luc);                                                   // Luc tames a guh ...
            luc.setShiftKeyDown(true);
            wild.onOwnerTap(luc);                                             // ... and picks it up
            luc.setShiftKeyDown(false);
            helper.assertTrue(wild.isRemoved(), "picked up");
            ItemStack stack = ItemStack.EMPTY;
            for (ItemStack s : luc.getInventory().getNonEquipmentItems()) {
                if (PickedUpGuhItem.guhData(s).contains("id")) {
                    stack = s;
                }
            }
            helper.assertTrue(!stack.isEmpty(), "the guh is in Luc's pockets");
            Vec3 at = helper.absoluteVec(new Vec3(8.5, 1, 8.5));
            Entity terug = PickedUpGuhItem.release(helper.getLevel(), PickedUpGuhItem.guhData(stack), at.x, at.y, at.z, 0);
            helper.assertTrue(terug instanceof GuhEntity && !terug.isPassenger() && !terug.isVehicle(), "put down again, riding nothing");
            helper.assertTrue(!rijdt.startRiding(luc, true, true) && !terug.startRiding(terug, true, true), "no loops after the pick-up");
            terug.startRiding(rijdt, true, true);                            // (the put-down guh hops on behind Luc, if there's room)
            geenLus(helper, "shoulder + pick up");
            rijdt.ejectPassengers();
            Schouder.eraf(coe, coe.position());
            geenLus(helper, "after");
            SamenGameTests.weg(helper, luc, coe);
            helper.succeed();
        });
    }

    /** The race kart with the racer's guh on the second seat, and a second player around. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void rijLusKartTweedeZitje(GameTestHelper helper) {
        ServerPlayer p = SamenGameTests.speler(helper, new BlockPos(2, 1, 2));
        ServerPlayer ander = SamenGameTests.speler(helper, new BlockPos(3, 1, 2));
        GuhEntity guh = SamenGameTests.guh(helper, p, new BlockPos(5, 1, 5));
        RaceGuhEntity kart = helper.spawn(RaceFeature.RACE_GUH.get(), new BlockPos(8, 1, 8));
        kart.setUpForRace();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(p.startRiding(kart, true, true), "the racer gets on");
            SamenMee.testKart(p, guh);
            helper.assertTrue(guh.startRiding(kart), "the guh hops on behind");
            helper.assertTrue(!kart.startRiding(guh, true, true) && !kart.startRiding(p, true, true) && !kart.startRiding(kart, true, true),
                    "the kart can't ride its own riders (forced)");
            p.startRiding(guh, true, true);                                   // (switching seats: allowed, but no loop)
            geenLus(helper, "seat switch");
            ander.startRiding(guh, true, true);
            helper.assertTrue(!guh.startRiding(ander, true, true), "the guh can't ride the player that rides it");
            geenLus(helper, "kart");
            SamenMee.testKart(p, null);
            List<Entity> alle = new ArrayList<>(List.of(p, ander, guh));
            alle.forEach(Entity::stopRiding);
            kart.discard();
            SamenGameTests.weg(helper, p, ander);
            helper.succeed();
        });
    }

    /** Tamed guhs and wild guhs outside the Guhmension are saved as before; only the Guhmension's come-and-go guhs aren't. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 40)
    public static void rijLusWildeGuhsBewaard(GameTestHelper helper) {
        ServerPlayer p = SamenGameTests.speler(helper, new BlockPos(2, 1, 2));
        GuhEntity tam = SamenGameTests.guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 1, 8));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!tam.isKomEnGaGuh() && tam.shouldBeSaved(), "a tamed guh is saved");
            helper.assertTrue(!wild.isKomEnGaGuh() && wild.shouldBeSaved(), "a wild guh in the overworld is saved (it never despawns)");
            helper.assertTrue(GuhmensionSpawner.tidyUp(helper.getLevel()) == 0 && !wild.isRemoved() && !tam.isRemoved(),
                    "the tidy-up leaves guhs that stay alone");
            helper.assertTrue(GuhmensionSpawner.maxWild(helper.getLevel()) >= GuhmensionSpawner.MAX_WILD_BASE, "the cap");
            SamenGameTests.weg(helper, p);
            helper.succeed();
        });
    }
}
