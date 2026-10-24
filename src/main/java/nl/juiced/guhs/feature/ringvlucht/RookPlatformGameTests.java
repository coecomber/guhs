package nl.juiced.guhs.feature.ringvlucht;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.7.0 (ronde 37-40): the two new places of the Rookguh-vlucht ({@link RookPlatform}): the gift of the platform (once: at
 * the end of the story, or later at login), one platform per player that moves along, the rows of the screen (the castle
 * greyed until world 1-1, the platform not on its own screen), where the Rookguh lands, and a whole flight onto the platform.
 * Like RingVluchtGameTests: the test level stands in for every dimension, the field ringvlucht_test_veld (floor at y 1).
 */
public final class RookPlatformGameTests {
    private static final String VELD = "ringvlucht_test_veld", BATCH = "rookplatform";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos lokaal) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Ring.wis(p);
        GuhQuests.saved(p).remove(RookPlatform.GEGEVEN);
        RookPlatform.vergeet(p);
        Vec3 daar = Vec3.atBottomCenterOf(helper.absolutePos(lokaal));
        p.snapTo(daar.x, daar.y, daar.z);
        p.setOnGround(true);
        return p;
    }

    private static void klaarTot(ServerPlayer p, int n) {
        Ring.lijn(1).begin(p);
        for (int i = 1; i <= n; i++) {
            Verhaallijn l = Ring.lijn(i);
            l.zet(p, l.stappen());
        }
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Vlucht.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        Vlucht.testLevel = null;
        Vlucht.TEST_DOEL.clear();
        RookPlatform.TEST_DOEL.clear();
    }

    private static int platforms(ServerPlayer p) {
        return p.getInventory().countItem(RingVluchtFeature.PLATFORM_ITEM.get());
    }

    private static List<CompoundTag> rijen(CompoundTag data) {
        List<CompoundTag> uit = new ArrayList<>();
        for (Tag t : data.getListOrEmpty("Haltes")) {
            uit.add((CompoundTag) t);
        }
        return uit;
    }

    private static CompoundTag rij(CompoundTag data, String id) {
        for (CompoundTag c : rijen(data)) {
            if (c.getStringOr("Id", "").equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** The platform comes once: at the end of the story; who finished before 1.7.0 gets it at login. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void rookplatformGeschenkEenKeer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            klaarTot(p, 5);
            helper.assertTrue(platforms(p) == 0 && !RookPlatform.mag(p), "not before the end of the story");
            klaarTot(p, 6);
            helper.assertTrue(Ring.klaar(p) && platforms(p) == 1 && RookPlatform.mag(p), "the end of the story: the platform");
            helper.assertTrue(!RookPlatform.geschenk(p, true) && platforms(p) == 1, "only once (also at the next login)");
            // somebody who finished before 1.7.0: no flag yet
            GuhQuests.saved(p).remove(RookPlatform.GEGEVEN);
            helper.assertTrue(RookPlatform.geschenk(p, true) && platforms(p) == 2 && GuhQuests.saved(p).getBooleanOr(RookPlatform.GEGEVEN, false),
                    "the first login after the update: given, flag set");
            weg(helper, p);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }

    /**
     * One platform per player (the newest counts, gone when its block is gone); the screen: the castle greyed until world
     * 1-1 is done, the platform on every Rookguh's screen but not on its own; the Rookguh lands on top of it.
     */
    @GuhTest(template = VELD, batch = BATCH)
    public static void rookplatformBestemmingen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        try {
            klaarTot(p, 6);
            Vlucht.testLevel = level;
            CompoundTag zonder = Vlucht.menu(p, null, false);
            helper.assertTrue(rij(zonder, RookPlatform.PLATFORM) == null, "no platform yet: no row");
            CompoundTag guhrio = rij(zonder, RookPlatform.GUHRIO);
            helper.assertTrue(guhrio != null && guhrio.getBooleanOr("Grijs", false) && guhrio.getStringOr("Waarom", "").equals("gui.guhs.ringvlucht.guhrio_eerst"),
                    "the castle: shown, greyed, 'Speel eerst wereld 1-1 uit!'");
            helper.assertTrue(!RookPlatform.vlieg(p, RookPlatform.GUHRIO, null, false) && !Vlucht.onderweg(p), "not before world 1-1 (also when a client asks)");
            // the platform: put down, moved
            BlockPos een = helper.absolutePos(new BlockPos(10, 1, 10)), twee = helper.absolutePos(new BlockPos(14, 1, 6));
            level.setBlockAndUpdate(een, RingVluchtFeature.PLATFORM.get().defaultBlockState());
            RookPlatformBlok.neem(p, level, een);
            helper.assertTrue(RookPlatform.isVan(p, level, een), "put down: it is theirs");
            level.setBlockAndUpdate(twee, RingVluchtFeature.PLATFORM.get().defaultBlockState());
            RookPlatformBlok.neem(p, level, twee);
            helper.assertTrue(RookPlatform.isVan(p, level, twee) && !RookPlatform.isVan(p, level, een), "one per player: the destination moved");
            CompoundTag met = Vlucht.menu(p, null, false);
            CompoundTag rij = rij(met, RookPlatform.PLATFORM);
            helper.assertTrue(rij != null && !rij.getBooleanOr("Grijs", false) && rij.getStringOr("Wereld", "").equals("gui.guhs.ringvlucht.wereld.overworld"),
                    "the own platform on the screen, with its world");
            helper.assertTrue(rij(Vlucht.menu(p, null, true), RookPlatform.PLATFORM) == null, "not on the platform's own screen");
            // where it lands: on top of it
            Vlucht.Doelplek d = RookPlatform.doelplek(p, RookPlatform.PLATFORM);
            helper.assertTrue(d != null && d.level() == level, "a destination in its level");
            BlockPos landing = Landing.zoek(level, d.ingangen(), d.afstand(), d.doos(), c -> true);
            helper.assertTrue(twee.above().equals(landing), "on top of the platform: " + landing);
            // world 1-1 done: the castle can be picked
            CompoundTag gehaald = new CompoundTag();
            gehaald.putBoolean(GuhrioKasteel.LEVELS.get(0), true);
            GuhrioSpel.spaar(p).put("Gehaald", gehaald);
            helper.assertTrue(RookPlatform.guhrioOpen(p) && !rij(Vlucht.menu(p, null, false), RookPlatform.GUHRIO).getBooleanOr("Grijs", false),
                    "world 1-1 done: the castle is a destination");
            // nothing to pick (only greyed rows): the Rookguh says so
            CompoundTag grijs = new CompoundTag();
            ListTag l = new ListTag();
            CompoundTag g = new CompoundTag();
            g.putBoolean("Grijs", true);
            l.add(g);
            grijs.put("Haltes", l);
            helper.assertTrue(!RookPlatform.heeftKeuze(grijs) && RookPlatform.heeftKeuze(met), "only greyed rows: nothing to pick");
            // the block gone: no more platform
            level.removeBlock(twee, false);
            helper.assertTrue(RookPlatform.doelplek(p, RookPlatform.PLATFORM) == null && RookPlatform.van(p) == null, "its block gone: forgotten");
            weg(helper, p);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }

    /** A player who may not have one yet: placing it doesn't make it theirs. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void rookplatformTeVroeg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        try {
            klaarTot(p, 2);
            BlockPos pos = helper.absolutePos(new BlockPos(10, 1, 10));
            level.setBlockAndUpdate(pos, RingVluchtFeature.PLATFORM.get().defaultBlockState());
            RookPlatformBlok.neem(p, level, pos);
            helper.assertTrue(RookPlatform.van(p) == null, "the story isn't done: not theirs");
            helper.assertTrue(!RookPlatform.kiesVanafPlatform(p, "gouw"), "and no flight from it");
            weg(helper, p);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }

    /** A whole flight from a Reis-Rookguh onto the own platform: the scene, then the player stands on top of it. */
    @GuhTest(template = VELD, batch = BATCH + "_vlucht", timeoutTicks = 400)
    public static void rookplatformVliegtErheen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        klaarTot(p, 6);
        Vlucht.testLevel = level;
        BlockPos platform = helper.absolutePos(new BlockPos(16, 1, 4));
        level.setBlockAndUpdate(platform, RingVluchtFeature.PLATFORM.get().defaultBlockState());
        RookPlatformBlok.neem(p, level, platform);
        VluchtRookguhEntity r = VluchtRookguhEntity.maak(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 10))), Haltes.van("guhvendel"), 0f);
        level.addFreshEntity(r);
        helper.assertTrue(Vlucht.kies(p, r, RookPlatform.PLATFORM), "the flight to the platform starts");
        helper.onEachTick(() -> NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p)));
        helper.succeedWhen(() -> {
            helper.assertTrue(!Vlucht.onderweg(p) && !Cutscenes.bezig(p), "the scene is over");
            helper.assertTrue(p.blockPosition().equals(platform.above()), "on top of the platform: " + p.blockPosition());
            // from the platform itself: its own screen, to a stop of the story
            Vlucht.TEST_DOEL.put("knabbelmoria", new Vlucht.Doelplek(level, List.of(new Landing.Ingang(helper.absolutePos(new BlockPos(3, 2, 3)), 0, 1)), null, 0));
            helper.assertTrue(!RookPlatform.kiesVanafPlatform(p, RookPlatform.PLATFORM), "not to the platform you stand at");
            helper.assertTrue(RookPlatform.kiesVanafPlatform(p, "knabbelmoria") && Vlucht.onderweg(p), "from the platform to a stop of the story");
            r.discard();
            weg(helper, p);
        });
    }
}
