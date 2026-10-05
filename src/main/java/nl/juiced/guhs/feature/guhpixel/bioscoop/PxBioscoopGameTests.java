package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.Films;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Guhbioscoop (batch px_bioscoop; run with {@code -Pgt=px_bioscoop}): finding the screen (the biggest
 * full rectangle, at most 7 x 4), starting and stopping a film with the server's checks (per player: only YOUR films), a
 * film that runs to its end (watched, the advancements), a guh that comes to sit (seat taken, popcorn, a fright, tidy
 * afterwards), the nine films' data, the popcornmachine and the shop offers.
 */
public class PxBioscoopGameTests {
    private static final String BATCH = "px_bioscoop";
    private static final String KAMER = "px_test_48";

    /** A cinema in the test room, looking east: the projector's position (absolute). */
    private static BlockPos zaal(GameTestHelper helper) {
        return BioscoopCommando.bouwZaal(helper.getLevel(), helper.absolutePos(new BlockPos(8, 2, 24)), Direction.EAST);
    }

    private static ProjectorBlockEntity projector(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getLevel().getBlockEntity(pos) instanceof ProjectorBlockEntity be)) {
            throw new IllegalStateException("no projector at " + pos);
        }
        return be;
    }

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private static boolean heeftAdv(ServerPlayer p, String naam) {
        AdvancementHolder a = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return a != null && p.getAdvancements().getOrStartProgress(a).isDone();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void doekIsDeGrootsteRechthoek(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos p = zaal(helper);
        ProjectorBlockEntity be = projector(helper, p);
        Doek doek = be.zoekDoek();
        helper.assertTrue(doek != null && doek.breedte() == 7 && doek.hoogte() == 4 && doek.kant() == Direction.WEST && doek.heel(level),
                "the whole 7 x 4 screen is found: " + doek);
        Vec3 m = doek.midden();
        helper.assertTrue(Math.abs(m.y - (p.getY() - 1 + 2)) < 0.01 && Math.abs(m.z - (p.getZ() + 0.5)) < 0.01, "its middle lies in front of the projector: " + m);
        // a hole in the upper right corner: the biggest picture that still fits is 6 wide and 4 high... no: 7 x 3 shows the film bigger
        BlockPos hoek = doek.blok(6, 3);
        level.setBlock(hoek, Blocks.AIR.defaultBlockState(), 3);
        Doek kleiner = be.zoekDoek();
        helper.assertTrue(kleiner != null && kleiner.heel(level) && !kleiner.bevat(hoek), "a full rectangle without the hole: " + kleiner);
        helper.assertTrue(kleiner.breedte() == 7 && kleiner.hoogte() == 3 || kleiner.breedte() == 6 && kleiner.hoogte() == 4, "the biggest one: " + kleiner);
        helper.assertTrue(!doek.heel(level), "the old rectangle is not whole any more");
        // cloth that looks the other way does not count; no cloth: no screen
        for (int a = 0; a < 7; a++) {
            for (int b = 0; b < 4; b++) {
                level.setBlock(doek.blok(a, b), BioscoopSlice.DOEK.get().defaultBlockState().setValue(DoekBlock.FACING, Direction.EAST), 3);
            }
        }
        helper.assertTrue(be.zoekDoek() == null, "a screen with its back to the projector is no screen");
        level.setBlock(doek.blok(3, 1), BioscoopSlice.DOEK.get().defaultBlockState().setValue(DoekBlock.FACING, Direction.WEST), 3);
        Doek een = be.zoekDoek();
        helper.assertTrue(een != null && een.breedte() == 1 && een.hoogte() == 1, "one block of cloth is a (small) screen: " + een);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void alleenJeEigenFilms(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = zaal(helper);
        ProjectorBlockEntity be = projector(helper, pos);
        ServerPlayer a = PxTest.speler(helper), b = PxTest.speler(helper);
        a.snapTo(pos.getX() + 0.5, pos.getY() - 2, pos.getZ() + 1.5);
        b.snapTo(pos.getX() + 0.5, pos.getY() - 2, pos.getZ() - 0.5);
        Films.ontgrendel(a, "skyblok");
        helper.assertTrue(Films.heeft(a, "skyblok") && !Films.heeft(b, "skyblok"), "films are per player");
        helper.assertTrue(Bioscoop.speel(b, pos, "skyblok") == Bioscoop.Uitkomst.OP_SLOT && !be.speelt(), "b does not have the film");
        helper.assertTrue(Bioscoop.speel(a, pos, "bestaat_niet") == Bioscoop.Uitkomst.ONBEKEND, "an unknown film");
        helper.assertTrue(Bioscoop.speel(a, pos.above(5), "skyblok") == Bioscoop.Uitkomst.GEEN_PROJECTOR, "no projector there");
        CompoundTag stand = Bioscoop.stand(b, pos, null);
        helper.assertTrue(stand.getIntOr("DoekB", 0) == 7 && stand.getIntOr("DoekH", 0) == 4 && stand.getListOrEmpty("Films").size() == Films.IDS.size(),
                "the screen shows the cloth it found and all nine films");
        helper.assertTrue(stand.getListOrEmpty("Films").stream().noneMatch(f -> ((CompoundTag) f).getBooleanOr("Heeft", false)), "none of them is b's");
        helper.assertTrue(Bioscoop.stand(a, pos, null).getListOrEmpty("Films").stream().filter(f -> ((CompoundTag) f).getBooleanOr("Heeft", false)).count() == 1,
                "one of them is a's");
        helper.assertTrue(Bioscoop.speel(a, pos, "skyblok") == Bioscoop.Uitkomst.OK && be.speelt() && be.film().equals("skyblok") && be.doek() != null,
                "a starts the film");
        helper.assertTrue(level.getBlockState(pos).getValue(ProjectorBlock.LIT) && Voorstelling.van(level, pos) != null, "the lens is lit, the film is on");
        helper.assertTrue(Bioscoop.stand(b, pos, null).getStringOr("Speelt", "").equals("skyblok"), "everybody sees what runs");
        a.snapTo(pos.getX() + 30, pos.getY(), pos.getZ());
        helper.assertTrue(Bioscoop.stop(a, pos) == Bioscoop.Uitkomst.TE_VER && be.speelt(), "too far away to switch it off");
        helper.assertTrue(Bioscoop.stop(b, pos) == Bioscoop.Uitkomst.OK && !be.speelt() && Voorstelling.van(level, pos) == null
                && !level.getBlockState(pos).getValue(ProjectorBlock.LIT), "anybody at the projector may switch it off");
        // no cloth: no film
        Doek doek = be.zoekDoek();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 4; y++) {
                level.setBlock(doek.blok(x, y), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        a.snapTo(pos.getX() + 0.5, pos.getY() - 2, pos.getZ() + 1.5);
        helper.assertTrue(Bioscoop.speel(a, pos, "skyblok") == Bioscoop.Uitkomst.GEEN_DOEK && !be.speelt(), "without a screen nothing plays");
        PxTest.klaar(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void filmLooptAfEnTeltAlsGezien(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = zaal(helper);
        ProjectorBlockEntity be = projector(helper, pos);
        ServerPlayer kijker = PxTest.speler(helper), ver = PxTest.speler(helper);
        kijker.snapTo(pos.getX() + 3.5, pos.getY() - 2, pos.getZ() + 0.5);
        ver.snapTo(pos.getX() + 0.5, pos.getY() + 60, pos.getZ() + 0.5);
        FilmInfo info = FilmInfo.van(level.getServer(), "bedwars");
        helper.assertTrue(info != null && Bioscoop.start(level, be, info), "the film starts");
        helper.assertTrue(Bioscoop.gezien(kijker, "bedwars") == 0 && !heeftAdv(kijker, Bioscoop.ADV_EERSTE), "nothing watched yet");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.speelt(), "still running after ten ticks");
            be.spoel(info.duur());
        });
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(!be.speelt() && Voorstelling.van(level, pos) == null && !level.getBlockState(pos).getValue(ProjectorBlock.LIT),
                    "the film is over, the projector is off");
            helper.assertTrue(Bioscoop.gezien(kijker, "bedwars") == 1 && Bioscoop.aantalGezien(kijker) == 1 && heeftAdv(kijker, Bioscoop.ADV_EERSTE),
                    "who was there has watched it");
            helper.assertTrue(Bioscoop.gezien(ver, "bedwars") == 0, "who was far away has not");
            helper.assertTrue(!heeftAdv(kijker, Bioscoop.ADV_ALLES), "not all nine yet");
            for (String id : Films.IDS) {
                Bioscoop.markeerGezien(kijker, id);
            }
            helper.assertTrue(Bioscoop.aantalGezien(kijker) == Films.IDS.size() && heeftAdv(kijker, Bioscoop.ADV_ALLES), "all nine watched");
            Bioscoop.wisGezien(kijker);
            PxTest.klaar(helper, kijker, ver);
            helper.succeed();
        });
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void guhKomtZittenMetPopcorn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = zaal(helper);
        ProjectorBlockEntity be = projector(helper, pos);
        ServerPlayer p = PxTest.speler(helper);
        p.snapTo(pos.getX() + 0.5, pos.getY() - 2, pos.getZ() + 2.5);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(11, 2, 24));
        guh.tame(p);
        guh.setWandering(true);
        FilmInfo info = FilmInfo.van(level.getServer(), "vadsnite");
        helper.assertTrue(info != null && Bioscoop.start(level, be, info), "the film starts");
        Voorstelling v = Voorstelling.van(level, pos);
        helper.assertTrue(v != null && v.stoelen().size() == 4 && v.popcorn(), "the cinema knows its four seats and its popcornmachine");
        boolean[] gezeten = {false};
        helper.onEachTick(() -> {
            BioscoopGoal doel = BioscoopGoal.van(guh);
            if (gezeten[0] || doel == null || !doel.zit()) {
                return;
            }
            gezeten[0] = true;
            BlockPos stoel = doel.stoel();
            BlockState state = level.getBlockState(stoel);
            helper.assertTrue(state.getBlock() instanceof StoeltjeBlock && guh.position().distanceTo(StoeltjeBlock.guhPlek(stoel, state)) < 0.3,
                    "the guh is on its seat");
            helper.assertTrue(guh.isInSittingPose() && !guh.isOrderedToSit() && GuhHooks.heeft(guh, PxVlaggen.POPCORN), "sitting, with popcorn");
            helper.assertTrue(GuhKiezer.geclaimd(guh).equals(BioscoopGoal.NS) && GuhHooks.isBezig(guh) && v.publiek() == 1
                    && guh.getUUID().equals(Voorstelling.opStoel(level, stoel)), "the seat is the guh's");
            // a player cannot take that seat
            state.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(stoel), Direction.UP, stoel, false));
            helper.assertTrue(p.getVehicle() == null, "the player does not sit on a guh");
            doel.reageer(level, "schrik");
            helper.assertTrue(GuhHooks.heeft(guh, PxVlaggen.SCHRIK), "startled by the film");
            doel.reageer(level, "slaap");
            helper.assertTrue(GuhHooks.heeft(guh, nl.juiced.guhs.feature.vadswoud.SleepInNestGoal.OOGJES_DICHT), "dozed off");
            be.stop(level, false);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(!BioscoopGoal.bezig(guh) && !GuhHooks.heeft(guh, PxVlaggen.POPCORN | PxVlaggen.SCHRIK) && !guh.isInSittingPose()
                        && GuhKiezer.geclaimd(guh).isEmpty() && !GuhHooks.heeft(guh, nl.juiced.guhs.feature.vadswoud.SleepInNestGoal.OOGJES_DICHT), "after the film everything is tidy again");
                helper.assertTrue(Voorstelling.opStoel(level, stoel) == null, "the seat is free");
                guh.discard();
                PxTest.klaar(helper, p);
                helper.succeed();
            });
        });
    }

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void negenFilms(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Set<String> gezien = new HashSet<>();
        for (String id : Films.IDS) {
            FilmInfo info = FilmInfo.van(server, id);
            helper.assertTrue(info != null, "film " + id + " is in data/guhs/guhbioscoop/films.json");
            helper.assertTrue(info.duur() >= 600 && info.duur() <= 1200, id + " lasts 30 to 60 seconds: " + info.duur());
            helper.assertTrue(info.cues().size() >= 3, id + " has cues for the audience");
            int vorige = 0;
            for (FilmInfo.Cue c : info.cues()) {
                helper.assertTrue(FilmInfo.CUE_SOORTEN.contains(c.soort()) && c.t() >= vorige && c.t() < info.duur(), id + ": cue " + c);
                vorige = c.t();
            }
            for (String deel : new String[]{"naam", "uitleg", "slot", "o1", "t1"}) {
                helper.assertTrue(Language.getInstance().has("gui.guhs.guhbioscoop.film." + id + "." + deel), id + " has its text " + deel);
            }
            gezien.add(id);
        }
        helper.assertTrue(gezien.size() == 9 && FilmInfo.alle(server).keySet().equals(gezien), "nine films, the same nine as Films.IDS");
        for (String g : BioscoopSlice.FILM_GELUIDEN) {
            helper.assertTrue(Language.getInstance().has("subtitles.guhs.guhbioscoop.film." + g), "sound " + g + " has a subtitle");
        }
        // the three story films follow the stories (nobody finished them here)
        ServerPlayer p = PxTest.speler(helper);
        helper.assertTrue(!Films.heeft(p, "balto") && !Films.heeft(p, "mewtwo") && !Films.heeft(p, "stitch626"), "a new player has no story films");
        nl.juiced.guhs.quest.GuhQuests.saved(p).putBoolean(nl.juiced.guhs.feature.balto.BaltoVerhaal.HELD, true);
        helper.assertTrue(Films.heeft(p, "balto") && !Films.heeft(p, "mewtwo"), "the Held van Nomguh has the Balto film");
        nl.juiced.guhs.quest.GuhQuests.saved(p).remove(nl.juiced.guhs.feature.balto.BaltoVerhaal.HELD);
        PxTest.klaar(helper, p);
        helper.succeed();
    }

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void winkelEnPopcorn(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        Muntjes.zet(p, 500);
        helper.assertTrue(Winkel.prijs(p, Winkel.van("guhbioscoop_set")) == BioscoopSlice.PRIJS_SET, "the set costs 250");
        helper.assertTrue(Winkel.koop(p, "guhbioscoop_set") == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 250, "bought");
        helper.assertTrue(tel(p, BioscoopSlice.PROJECTOR_ITEM.get()) == 1 && tel(p, BioscoopSlice.DOEK_ITEM.get()) == 28
                && tel(p, BioscoopSlice.STOELTJE_ITEM.get()) == 4, "a projector, 28 cloth and 4 seats");
        helper.assertTrue(Winkel.prijs(p, Winkel.van("guhbioscoop_set")) == BioscoopSlice.PRIJS_SET_NOG, "a second set is cheaper");
        helper.assertTrue(Winkel.koop(p, "guhbioscoop_stoeltje") == Winkel.Uitkomst.OK && Winkel.koop(p, "guhbioscoop_popcornmachine") == Winkel.Uitkomst.OK
                && Winkel.koop(p, "guhbioscoop_doek") == Winkel.Uitkomst.OK, "the extras");
        helper.assertTrue(Muntjes.saldo(p) == 250 - 15 - 60 - 8 && tel(p, BioscoopSlice.STOELTJE_ITEM.get()) == 5 && tel(p, BioscoopSlice.DOEK_ITEM.get()) == 32
                && tel(p, BioscoopSlice.POPCORNMACHINE_ITEM.get()) == 1, "paid and delivered");
        // the popcornmachine: one bakje a minute per player
        BlockPos machine = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(machine, BioscoopSlice.POPCORNMACHINE.get().defaultBlockState(), 3);
        ServerPlayer q = PxTest.speler(helper);
        helper.assertTrue(PopcornmachineBlock.neem(level, machine, p) && tel(p, BioscoopSlice.POPCORN.get()) == 1, "a bakje popcorn");
        helper.assertTrue(!PopcornmachineBlock.neem(level, machine, p) && tel(p, BioscoopSlice.POPCORN.get()) == 1, "not another one at once");
        helper.assertTrue(PopcornmachineBlock.neem(level, machine, q), "another player gets their own");
        nl.juiced.guhs.quest.GuhQuests.saved(p).putLong(PopcornmachineBlock.LAATST, level.getGameTime() - PopcornmachineBlock.WACHT);
        helper.assertTrue(PopcornmachineBlock.neem(level, machine, p) && tel(p, BioscoopSlice.POPCORN.get()) == 2, "a minute later: the next one");
        nl.juiced.guhs.quest.GuhQuests.saved(p).remove(PopcornmachineBlock.LAATST);
        nl.juiced.guhs.quest.GuhQuests.saved(q).remove(PopcornmachineBlock.LAATST);
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }
}
