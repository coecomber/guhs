package nl.juiced.guhs.feature.samen;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.EmotePayload;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speeltje;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of samen (2.10): cheering at the minigames (goed / mis / record, resting in between, only your own guhs),
 * hearts from playing and travelling together, the rewards of the hartjes levels (clothes, emotes) and the emote lock,
 * riding along in the race kart (the second seat, a real race start and end), skating along, swimming along in the
 * Knuffelbad, the reactions (welcome back, respawn comfort, thunder cuddle, goodnight wave, the bff-knuffel) and the
 * friendships (points, becoming friends once, cuddling, the wip together, sleeping together in the huisje).
 * Templates: samen_test_wei (16 x 6 x 16 of air: the floor and the pool are laid in code), guh_racebaan (the race, own batch).
 */
public class SamenGameTests {
    private static final String WEI = "samen_test_wei";
    private static final String BATCH = "samen";

    /** The floor of the test room (grass at y 0, the whole 16 x 16), made in code like the emote tests do. */
    static void vloer(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos at = new BlockPos(x, 0, z);
                if (helper.getBlockState(at).isAir()) {
                    helper.setBlock(at, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
                }
            }
        }
    }

    /** A little pool (the "Knuffelbad"): water 8 x 8 and 2 deep in the middle, grass all round. */
    static void bad(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean water = x >= 4 && x < 12 && z >= 4 && z < 12;
                helper.setBlock(new BlockPos(x, 0, z), water ? net.minecraft.world.level.block.Blocks.SMOOTH_STONE : net.minecraft.world.level.block.Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 1, z), water ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 2, z), water ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }
    }

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        vloer(helper);
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** A tamed guh of this player (a band guh) that stays put (no wandering, no teleporting). */
    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        guh.setWandering(false);
        guh.setTeleportEnabled(false);
        return guh;
    }

    static void nieuweDag(Mob guh) {
        BandData.Rec r = BandData.get(guh.level().getServer()).vind(Band.eigenaar(guh), Band.id(guh));
        if (r != null) {
            r.dag = -1;
        }
    }

    static boolean quest(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // =====================================================================================================================
    // cheering and hearts
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenJuichtBijGoedEnIsLiefVerdrietigBijMis(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        ServerPlayer ander = speler(helper, new BlockPos(12, 1, 12));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity vreemd = guh(helper, ander, new BlockPos(7, 1, 5));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 1, 5));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(GuhEmotes.canStart(guh), "can start: ground=" + guh.onGround() + " noAi=" + guh.isNoAi() + " target=" + guh.getTarget()
                    + " water=" + guh.isInWater() + " vehicle=" + guh.isVehicle() + " passenger=" + guh.isPassenger() + " hidden=" + guh.getHiddenBy()
                    + " launch=" + guh.getLaunchState() + " y=" + guh.getY() + " type=" + guh.getType() + " below=" + helper.getLevel().getBlockState(guh.blockPosition().below())
                    + " at=" + helper.getLevel().getBlockState(guh.blockPosition()) + " origin=" + helper.absolutePos(BlockPos.ZERO) + " noPhys=" + guh.noPhysics
                    + " grav=" + guh.isNoGravity() + " dm=" + guh.getDeltaMovement() + " sit=" + guh.isOrderedToSit());
            helper.assertTrue(SamenSpel.goed(p, "sjoelen") == 1, "only your own guh cheers");
            helper.assertTrue(guh.emotes.current() == Emote.VAHOEG && guh.emotes.lookTarget() != null, "a VAHOEG jump towards you");
            helper.assertTrue(vreemd.emotes.current() == null && wild.emotes.current() == null, "not someone else's or a wild one");
            helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/samen_juichen"), "the advancement");
            helper.assertTrue(SamenSpel.mis(p, "sjoelen") == 0, "it rests a moment between two reactions");
            guh.getPersistentData().remove(SamenSpel.RUST_TOT);
            helper.assertTrue(SamenSpel.mis(p, "golf") == 1 && guh.emotes.current() == Emote.VERDRIETJE, "a miss: lovingly sad");
            guh.getPersistentData().remove(SamenSpel.RUST_TOT);
            helper.assertTrue(SamenSpel.record(p, "golf") == 1 && guh.emotes.current() == Emote.KNUFFELDANSJE, "a record: the knuffeldansje");
            guh.getPersistentData().remove(SamenSpel.RUST_TOT);
            GuhHooks.bezig(guh, 100);
            helper.assertTrue(SamenSpel.goed(p, "golf") == 0, "a busy guh (a chore, a toy) doesn't look up");
            GuhHooks.bezig(guh, 0);
            helper.assertTrue(SamenSpel.uitslag(null, "golf", true) == 0, "no player: nothing (and no crash)");
            helper.assertTrue(Band.hartjes(guh) <= Reden.SAMEN_TIJD.standaard(), "cheering itself gives no hearts (playing together does)");
            weg(helper, p, ander);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenSpelenEnReizenGevenHartjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.runAfterDelay(5, () -> {
            UUID id = Band.id(guh);
            int begin = Band.hartjes(guh);
            Band.moment(guh, p, Moment.MINIGAME_START, "sjoelen");
            helper.assertTrue(guh.emotes.current() == Emote.ZWAAIEN, "at the start: a wave, succes!");
            Band.moment(guh, p, Moment.MINIGAME_EINDE, "sjoelen");
            helper.assertTrue(Band.hartjes(guh) == begin + Reden.MINIGAME.standaard(), "playing together: " + Band.hartjes(guh));
            helper.assertTrue(Dagboek.stat(p.level().getServer(), p.getUUID(), id, DagboekStat.MINIGAMES_SAMEN) == 1, "the dagboek counts it");
            helper.assertTrue(Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "eerste_minigame"), "the first time");
            helper.assertTrue(quest(p, "samen_gespeeld") && GidsFeature.heeft(p, "lieve_vadsjes/samen_gespeeld"), "the advancements");
            int voor = Band.hartjes(guh);
            Band.moment(guh, p, Moment.RECORD, "sjoelen");
            helper.assertTrue(Band.hartjes(guh) == voor + Reden.RECORD.standaard() && guh.emotes.current() == Emote.KNUFFELDANSJE,
                    "a record: hearts and a dance");
            voor = Band.hartjes(guh);
            Band.moment(guh, p, Moment.REIS, "64");
            helper.assertTrue(Band.hartjes(guh) == voor + Reden.REIZEN.standaard(), "travelling together: a heart per 64 blocks");
            for (int i = 0; i < 20; i++) {
                Band.moment(guh, p, Moment.MINIGAME_EINDE, "golf");
            }
            helper.assertTrue(Band.hartjes(guh) <= voor + Reden.REIZEN.standaard() + Reden.MINIGAME.dagMax(), "capped per day");
            int alles = Band.hartjes(guh);
            SamenSpel.mis(p, "golf");
            helper.assertTrue(Band.hartjes(guh) == alles, "a miss never takes hearts away");
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the rewards of the levels and the emote lock
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenNiveausOntgrendelenKledingEnEmotes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.runAfterDelay(5, () -> {
            for (GuhClothes c : SamenBeloning.alleKleding()) {
                helper.assertTrue("band".equals(KledingBronnen.bron(c)), c + " comes from the hartjes");
            }
            helper.assertTrue(KledingBronnen.van("band").size() == 4, "four hartjes pieces");
            helper.assertTrue(!SamenBeloning.heeft(p, Emote.HARTJES) && SamenBeloning.heeft(p, Emote.VERDRIETJE), "locked, but not the verdrietje");
            // the real path: hearts up to "lieve vadsjes van elkaar" (the level listener)
            for (int dag = 0; dag < 5 && Band.niveau(guh) == BandNiveau.GEEN; dag++) {
                nieuweDag(guh);
                for (Reden r : Reden.values()) {
                    if (Band.hartjes(guh) < BandNiveau.LIEF.drempel()) {
                        Band.geefHartjes(guh, p, Math.min(r.dagMax(), BandNiveau.LIEF.drempel() - Band.hartjes(guh)), r);
                    }
                }
            }
            helper.assertTrue(Band.niveau(guh) == BandNiveau.LIEF, "lieve vadsjes: " + Band.hartjes(guh));
            helper.assertTrue(KledingUnlocks.heeft(p, GuhClothes.SAMEN_HARTJESSPELDJE) && SamenBeloning.heeft(p, Emote.HARTJES),
                    "level 1: the hartjesspeldje and the emote Hartjes");
            helper.assertTrue(!KledingUnlocks.heeft(p, GuhClothes.SAMEN_KNUFFELTRUITJE) && !SamenBeloning.heeft(p, Emote.KNUFFELDANSJE),
                    "not the next level's yet");
            helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/samen_beloning_lief"), "the advancement");
            helper.assertTrue(SamenBeloning.geef(p, BandNiveau.LIEF, guh) == 0, "nothing twice");
            helper.assertTrue(SamenBeloning.geef(p, BandNiveau.ZIELSGUH, guh) == 5, "zielsguh: truitje, knuffeldansje, kroontje, halsbandje, bff");
            helper.assertTrue(KledingUnlocks.heeft(p, GuhClothes.GOUDEN_HARTJESHALSBANDJE) && KledingUnlocks.heeft(p, GuhClothes.SAMEN_ZIELSKROONTJE)
                    && SamenBeloning.heeft(p, Emote.BFF_KNUFFEL) && GidsFeature.heeft(p, "lieve_vadsjes/samen_halsbandje"),
                    "the gouden hartjes-halsbandje, the kroontje and the bff-knuffel");
            // the emote picker's lock (server side)
            SamenBeloning.wisEmotes(p);
            helper.assertTrue(!EmotePayload.apply(p, new EmotePayload(guh.getId(), EmotePayload.NOW, Emote.BFF_KNUFFEL.ordinal())),
                    "a locked emote can't be picked");
            helper.assertTrue(!EmotePayload.apply(p, new EmotePayload(guh.getId(), EmotePayload.FAVORITE, Emote.HARTJES.ordinal()))
                    && guh.emotes.favorite() == null, "nor made the favourite");
            SamenBeloning.inhalen(p);
            helper.assertTrue(SamenBeloning.heeft(p, Emote.HARTJES) && !SamenBeloning.heeft(p, Emote.BFF_KNUFFEL),
                    "the login catch-up gives what your guhs already reached (lief)");
            helper.assertTrue(EmotePayload.apply(p, new EmotePayload(guh.getId(), EmotePayload.NOW, Emote.HARTJES.ordinal()))
                    && guh.emotes.current() == Emote.HARTJES, "unlocked: blowing hearts");
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // really joining in
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenKartHeeftEenTweedeZitje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        ServerPlayer ander = speler(helper, new BlockPos(3, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        RaceGuhEntity kart = helper.spawn(RaceFeature.RACE_GUH.get(), new BlockPos(8, 1, 8));
        kart.setUpForRace();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!guh.startRiding(kart), "without a racer in front: no seat for a guh");
            helper.assertTrue(p.startRiding(kart, true, true), "the racer gets on");
            SamenMee.testKart(p, guh);
            helper.assertTrue(guh.startRiding(kart), "the guh hops on behind");
            helper.assertTrue(kart.getPassengers().size() == 2 && kart.getPassengers().get(0) == p && kart.getControllingPassenger() == p,
                    "the racer stays in front and steers");
            helper.assertTrue(!ander.startRiding(kart), "no third one");
        });
        helper.runAfterDelay(5, () -> {
            Vec3 zit = guh.position(), racer = p.position();
            helper.assertTrue(guh.getVehicle() == kart && zit.distanceTo(racer) > 0.3, "the guh sits behind the racer, not in them: " + zit + " / " + racer);
            SamenMee.testKart(p, null);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!guh.isPassenger(), "a guh in a kart without a race (of samen) hops off again");
            p.stopRiding();
            kart.discard();
            weg(helper, p, ander);
            helper.succeed();
        });
    }

    @GuhTest(template = "guh_racebaan", batch = "samen_kart", timeoutTicks = 200)
    public static void samenRijdtMeeInDeRace(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.RACEGUH);
        helper.assertTrue(npcs.size() == 1, "the Raceguh is there");
        GuhNpcEntity npc = npcs.get(0);
        @SuppressWarnings("removal")
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.snapTo(npc.getX(), npc.getY(), npc.getZ() - 2);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), helper.relativePos(p.blockPosition()));
        guh.tame(p);
        guh.setWandering(false);
        guh.snapTo(p.getX() + 1, p.getY(), p.getZ());
        Vec3 van = guh.position();
        RaceGame.start(npc, p, RaceBaan.RACEBAAN, Niveau.MEDIUM);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && p.getVehicle() instanceof RaceGuhEntity, "the race is on");
        RaceGuhEntity mount = (RaceGuhEntity) p.getVehicle();
        helper.assertTrue(guh.getVehicle() == mount && guh.getUUID().equals(SamenMee.inKart(p)), "your guh hopped on behind you");
        helper.assertTrue(mount.getControllingPassenger() == p, "you still steer");
        helper.assertTrue(quest(p, "samen_kart") && GidsFeature.heeft(p, "lieve_vadsjes/samen_kart"), "the advancements");
        game.end(helper.getLevel(), RaceGame.Ending.STOPPED);
        helper.assertTrue(!guh.isPassenger() && SamenMee.inKart(p) == null, "after the race it hops off");
        helper.assertTrue(guh.position().distanceTo(van) < 1.5, "back where it was");
        helper.assertTrue(guh.isAlive() && !guh.isRemoved(), "and it's still there (never goes with the race guh)");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenSchaatstMeeOpDeElftocht(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity zit = guh(helper, p, new BlockPos(7, 1, 5));
        zit.setOrderedToSit(true);
        helper.runAfterDelay(5, () -> {
            try {
                PinguhMeeglijden.start(p);
                helper.assertTrue(!PinguhMeeglijden.glijdtMee(guh), "not skating: your guh just follows as usual");
                PinguhMeeglijden.stop(p);
                SamenMee.TEST_SCHAATSERS.add(p.getUUID());
                List<GuhEntity> mee = PinguhMeeglijden.start(p);
                helper.assertTrue(mee.contains(guh) && PinguhMeeglijden.glijdtMee(guh), "on the ice: your guh skates along: " + mee);
                helper.assertTrue(!PinguhMeeglijden.glijdtMee(zit), "a sitting guh stays sitting");
                PinguhMeeglijden.stop(p);
                helper.assertTrue(!PinguhMeeglijden.glijdtMee(guh), "the tour is over");
            } finally {
                SamenMee.TEST_SCHAATSERS.remove(p.getUUID());
                PinguhMeeglijden.stop(p);
            }
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 400)
    public static void samenZwemtMeeInHetKnuffelbad(GameTestHelper helper) {
        bad(helper);
        ServerPlayer p = speler(helper, new BlockPos(8, 1, 8));
        GuhEntity guh = guh(helper, p, new BlockPos(1, 3, 1));
        guh.setWandering(true);
        SamenMee.TEST_ZWEMMERS.add(p.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.isInWater() && guh.distanceTo(p) < 4, "the guh jumps in and swims along: " + guh.position() + " " + guh.distanceTo(p));
            SamenMee.TEST_ZWEMMERS.remove(p.getUUID());
            weg(helper, p);
        });
    }

    // =====================================================================================================================
    // reactions
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenWelkomTerugNaLangWeg(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(5, 1, 5));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!SamenReacties.welkomCheck(guh, p), "the first time: just together");
            helper.assertTrue(!SamenReacties.welkomCheck(guh, p), "still together: no dance");
            guh.getPersistentData().putLong(SamenReacties.LAATST_TIJD, helper.getLevel().getGameTime() - SamenReacties.WEG_TICKS - 5);
            helper.assertTrue(SamenReacties.welkomCheck(guh, p) && guh.emotes.current() == Emote.KNUFFELDANSJE, "back after long: a welcome dance");
            helper.assertTrue(quest(p, "samen_welkom") && Band.hartjes(guh) > 0, "hearts and the advancement");
            guh.emotes.stop();
            guh.getPersistentData().putLong(SamenReacties.LAATST_TIJD, helper.getLevel().getGameTime() - 300);
            guh.getPersistentData().putLong(SamenReacties.LAATST_MS, System.currentTimeMillis() - SamenReacties.WEG_MS - 1000);
            helper.assertTrue(SamenReacties.welkomCheck(guh, p), "or a long time away in real time (the world was closed)");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenTroostNaHetDoodgaan(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 12));
        GuhEntity guh = guh(helper, p, new BlockPos(13, 1, 2));
        GuhEntity minder = guh(helper, p, new BlockPos(13, 1, 4));
        helper.runAfterDelay(5, () -> {
            Band.geefHartjes(guh, p, 20, Reden.OVERIG);
            SamenReacties.gestorven(p);
            GuhEntity troost = SamenReacties.troost(p);
            helper.assertTrue(troost == guh, "the guh with the most hearts waits for you: " + troost);
            helper.assertTrue(guh.distanceTo(p) < 3 && guh.emotes.current() == Emote.KNUFFELEN, "right by you, with a comforting hug");
            helper.assertTrue(minder.distanceTo(p) > 5, "(just one)");
            helper.assertTrue(quest(p, "samen_troost"), "the advancement");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 300)
    public static void samenKnuffeltTegenJeAanBijOnweer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(10, 1, 10));
        guh.setWandering(true);
        SamenReacties.TEST_ONWEER.add(guh.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.distanceTo(p) < 3 && guh.emotes.current() == Emote.KNUFFELEN,
                    "it comes and cuddles against you: " + guh.distanceTo(p) + " " + guh.emotes.current());
            helper.assertTrue(quest(p, "samen_onweer"), "the advancement");
            SamenReacties.TEST_ONWEER.remove(guh.getUUID());
            weg(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenZwaaitJeWelterusten(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity a = guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 5));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(SamenReacties.welterusten(p) == 2, "both wave goodnight");
            helper.assertTrue(a.emotes.current() == Emote.ZWAAIEN && b.emotes.current() == Emote.ZWAAIEN, "a wave each");
            helper.assertTrue(quest(p, "samen_welterusten"), "the advancement");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenBffKnuffelMetEenGrootHart(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(6, 1, 6));
        helper.runAfterDelay(5, () -> {
            double voor = guh.distanceTo(p);
            int begin = Band.hartjes(guh);
            helper.assertTrue(guh.emotes.start(Emote.BFF_KNUFFEL, false, GuhEmotes.Source.OWNER), "the bff-knuffel");
            Vec3 plek = SamenReacties.voor(p, 0.6 + guh.getBbWidth() * 0.5);
            helper.assertTrue(guh.distanceTo(p) < voor && guh.distanceTo(p) < 2.2, "it hops right in front of you: " + guh.distanceTo(p)
                    + " w=" + guh.getBbWidth() + " vrij=" + SamenReacties.vrij(helper.getLevel(), guh, plek) + " plek=" + plek + " p=" + p.position()
                    + " owner=" + Band.eigenaarOnline(guh) + " guh=" + guh.position() + " cur=" + guh.emotes.current());
            helper.assertTrue(Band.hartjes(guh) == begin + Reden.KNUFFELEN.standaard(), "a big cuddle's hearts: " + Band.hartjes(guh));
            helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/samen_bff_knuffel"), "the advancement");
            guh.emotes.stop();
            guh.emotes.start(Emote.BFF_KNUFFEL, false, GuhEmotes.Source.OWNER);
            helper.assertTrue(Band.hartjes(guh) == begin + Reden.KNUFFELEN.standaard(), "the hearts rest a while (no spamming)");
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // friendships
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void samenGuhsWordenVriendjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        GuhEntity a = guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity b = guh(helper, p, new BlockPos(7, 1, 5));
        GuhEntity ver = guh(helper, p, new BlockPos(14, 1, 14));
        List<String> momenten = new CopyOnWriteArrayList<>();
        Band.opMoment((guh, speler, m, waarde) -> {
            if (m == Moment.VRIENDJE && (guh == a || guh == b)) {
                momenten.add(waarde);
            }
        });
        helper.runAfterDelay(50, () -> {
            var s = p.level().getServer();
            int punten = Vriendjes.punten(s, Band.id(a), Band.id(b));
            helper.assertTrue(punten >= 1 && punten <= 3, "a point per second together: " + punten);
            helper.assertTrue(Vriendjes.punten(s, Band.id(a), Band.id(ver)) == 0, "not with the one far away");
            Vriendjes.samen(a, b, Vriendjes.VRIENDJES);
            helper.assertTrue(Vriendjes.vrienden(s, Band.id(a), Band.id(b)), "friends");
            helper.assertTrue(momenten.contains(Band.id(b).toString()) && momenten.contains(Band.id(a).toString()), "the VRIENDJE moment for both: " + momenten);
            helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), Band.id(a), "eerste_vriendje"), "the first friend in the dagboek");
            helper.assertTrue(GidsFeature.heeft(p, "lieve_vadsjes/samen_vriendjes"), "the advancement");
            helper.assertTrue(Band.hartjes(a) >= Reden.VRIENDJE.dagMax(), "hearts: " + Band.hartjes(a));
            int voor = Vriendjes.punten(s, Band.id(a), Band.id(b));
            SamenVriendjes.knuffelSamen(a, b, true);
            helper.assertTrue(Vriendjes.punten(s, Band.id(a), Band.id(b)) == voor + SamenVriendjes.KNUFFEL_PUNTEN, "a cuddle together counts");
            helper.assertTrue(a.emotes.current() == Emote.KNUFFELEN && quest(p, "samen_vriendjes_knuffel"), "they hug");
            Vriendjes.samen(a, b, Vriendjes.BESTIES);
            helper.assertTrue(Vriendjes.besties(s, Band.id(a), Band.id(b)) && GidsFeature.heeft(p, "lieve_vadsjes/samen_besties"), "besties!");
            helper.assertTrue(Dagboek.heeftEersteKeer(s, p.getUUID(), Band.id(b), "samen_bestie"), "a bestie in the dagboek");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 300)
    public static void samenVriendjesSpelenOpDeWip(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity a = guh(helper, p, new BlockPos(5, 1, 5));
        GuhEntity b = guh(helper, p, new BlockPos(8, 1, 5));
        a.setWandering(true);
        b.setWandering(true);
        List<Mob> spelers = new CopyOnWriteArrayList<>();
        BlockPos wip = helper.absolutePos(new BlockPos(7, 1, 10));
        SamenVriendjes.TEST_WIP = new Speeltje() {
            @Override
            public String id() {
                return SamenVriendjes.WIP;
            }

            @Override
            public KlusTaak zoek(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
                return new KlusTaak() {
                    int t;

                    @Override
                    public boolean tick() {
                        wie.getNavigation().moveTo(wip.getX() + 0.5, wip.getY(), wip.getZ() + 0.5, 1.0);
                        if (wie.blockPosition().distManhattan(wip) <= 2 && !spelers.contains(wie)) {
                            spelers.add(wie);
                        }
                        return ++t < 160;
                    }

                    @Override
                    public int maxTicks() {
                        return 200;
                    }
                };
            }
        };
        helper.runAfterDelay(2, () -> {
            Vriendjes.samen(a, b, Vriendjes.VRIENDJES);
            SamenVriendjes.TEST_NU.put(a.getUUID(), SamenVriendjes.Wat.SPELEN);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(spelers.contains(a) && spelers.contains(b), "both friends go and play on the wip: " + spelers + " testNu=" + SamenVriendjes.TEST_NU.containsKey(a.getUUID())
                    + " uitgenodigd=" + SamenVriendjes.uitgenodigd(b) + " bezigA=" + GuhHooks.isBezig(a) + " bezigB=" + GuhHooks.isBezig(b) + " vrijA=" + SamenVriendjes.vrij(a)
                    + " vrijB=" + SamenVriendjes.vrij(b) + " vriend=" + SamenVriendjes.vriendInDeBuurt(a) + " goals=" + a.goalSelector.getAvailableGoals().stream()
                    .filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList());
            SamenVriendjes.TEST_WIP = null;
            weg(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void samenVriendjesSlapenSamenInHunHuisje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity a = guh(helper, p, new BlockPos(3, 1, 12));
        GuhEntity b = guh(helper, p, new BlockPos(5, 1, 12));
        helper.runAfterDelay(3, () -> {
            Huisje h = HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(new BlockPos(8, 1, 6)), Direction.SOUTH, HuisjeMaat.MEDIUM, p.getUUID());
            helper.assertTrue(Huisjes.trekIn(h, a) && Huisjes.trekIn(h, b), "both live in the huisje");
            Vriendjes.samen(a, b, Vriendjes.VRIENDJES);
            int voor = Vriendjes.punten(p.level().getServer(), Band.id(a), Band.id(b));
            Huisjes.naarBinnen(a, h);
            helper.assertTrue(Vriendjes.punten(p.level().getServer(), Band.id(a), Band.id(b)) == voor, "alone inside: nothing yet");
            Huisjes.naarBinnen(b, h);
            helper.assertTrue(Vriendjes.punten(p.level().getServer(), Band.id(a), Band.id(b)) == voor + SamenVriendjes.SLAAP_PUNTEN,
                    "friends asleep side by side: " + Vriendjes.punten(p.level().getServer(), Band.id(a), Band.id(b)));
            Huisjes.naarBuiten(b, h, false);
            Huisjes.naarBinnen(b, h);
            helper.assertTrue(Vriendjes.punten(p.level().getServer(), Band.id(a), Band.id(b)) == voor + SamenVriendjes.SLAAP_PUNTEN, "once per night");
            Huisjes.naarBuiten(a, h, false);
            Huisjes.naarBuiten(b, h, false);
            weg(helper, p);
            helper.succeed();
        });
    }
}
