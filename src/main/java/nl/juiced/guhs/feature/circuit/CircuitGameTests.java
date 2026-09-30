package nl.juiced.guhs.feature.circuit;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceGeesten;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.feature.race.RaceProtection;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.race.RaceRit;
import nl.juiced.guhs.feature.race.RaceTrack;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Tests of the Guh-Circuit: whole races on the real circuit (the three tracks found from Coach Vahoegvroem, coins,
 * records and boards per track and level, the golden ghost, the Mika-pikkers per level, the rolling kaasknabbels, the
 * Vadslooping, cleanup, protection), and the small things on their own (boost rings, kaassaus, stuiterpaddenstoel, the
 * looping's path, a pinching Mika, a rolling knabbel, the shop, the prizes and medal times).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class CircuitGameTests {
    private static final String CIRCUIT = "guh_circuit";
    private static final String EMPTY = "empty";

    private static GuhNpcEntity coach(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.CIRCUITGUH);
        helper.assertTrue(npcs.size() == 1, "Coach Vahoegvroem is in the Pitpaleis: " + npcs.size());
        return npcs.get(0);
    }

    private static ServerPlayer racer(GameTestHelper helper, GuhNpcEntity npc) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        return player;
    }

    private static RaceGuhEntity mount(GameTestHelper helper, Player player) {
        helper.assertTrue(player.getVehicle() instanceof RaceGuhEntity, "the racer rides a race guh: " + player.getVehicle());
        return (RaceGuhEntity) player.getVehicle();
    }

    /** Drives the race guh through a ring: from just above it, down into it (one race tick each). */
    private static void through(RaceGame game, GuhNpcEntity npc, RaceGuhEntity mount, int gate) {
        Vec3 c = game.track().centre(gate);
        mount.teleportTo(c.x, c.y + 6, c.z);
        game.tick(npc);
        mount.teleportTo(c.x, c.y, c.z);
        game.tick(npc);
    }

    private static int count(GameTestHelper helper, Class<? extends Entity> type) {
        return helper.getLevel().getEntitiesOfClass(type, helper.getBounds().inflate(8), Entity::isAlive).size();
    }

    private static void leave(GameTestHelper helper, Player... players) {
        for (Player p : players) {
            helper.getLevel().removePlayerImmediately((ServerPlayer) p, Entity.RemovalReason.DISCARDED);
        }
    }

    // --- whole races on the real circuit ----------------------------------------------------------------------------------

    @GameTest(batch = "circuit_regenboog", template = CIRCUIT, timeoutTicks = 400)
    public static void circuitRegenboogRaceForCircuitbekers(GameTestHelper helper) {
        GuhNpcEntity npc = coach(helper);
        ServerPlayer player = racer(helper, npc);
        CircuitBanen.Frame frame = CircuitBanen.frame(helper.getLevel(), npc);
        helper.assertTrue(frame != null && frame.facing() == Direction.EAST, "the coach finds her circuit: " + frame);
        RaceGeesten.forget(player.server, CircuitBanen.REGENBOOG.boardTotal(Niveau.MEDIUM));
        CircuitRole.action(npc, player, CircuitRole.START, "regenboog", Niveau.MEDIUM.ordinal());
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.baan() == CircuitBanen.REGENBOOG && game.niveau() == Niveau.MEDIUM, "a race on the Regenboogbaan");
        game.trustTeleports();
        helper.assertTrue(Minigames.CIRCUIT.equals(Minigames.playing(player)) && !RaceGame.isRacing(player) && RaceGame.isRacingAny(player),
                "it counts as the circuit, not the old racebaan: " + Minigames.playing(player));
        RaceGuhEntity mount = mount(helper, player);
        helper.assertTrue(mount.niveau() == Niveau.MEDIUM, "a medium race guh");
        helper.assertTrue(game.track().gates.size() == 5, "5 rings on the Regenboogbaan: " + game.track().gates.size());
        helper.assertTrue(game.laps() == 3, "3 laps");
        helper.assertTrue(count(helper, MikaPikkerEntity.class) == 4, "medium: 4 Mika-pikkers along the track: " + count(helper, MikaPikkerEntity.class));
        // one at a time: somebody else has to wait (on any track)
        ServerPlayer other = racer(helper, npc);
        CircuitRole.action(npc, other, CircuitRole.START, "vads", Niveau.LASTIG.ordinal());
        helper.assertTrue(RaceGame.of(npc) == game && !RaceGame.isRacingAny(other), "one race at a time on the circuit");
        game.skipCountdown(npc);
        for (int lap = 0; lap < game.laps(); lap++) {
            for (int gate : new int[]{1, 2, 3, 4, 0}) {
                through(game, npc, mount, gate);
                if (lap == 0 && gate == 1) {
                    helper.assertTrue(mount.sprongen(), "2.10.1: on the Regenboogbaan the race guh makes the rainbow jump over the gaps");
                }
            }
        }
        helper.assertTrue(RaceGame.of(npc) == null && !RaceGame.isRacingAny(player), "3 laps: finished");
        helper.assertTrue(mount.isRemoved() && count(helper, MikaPikkerEntity.class) == 0, "the race guh and the Mika-pikkers are gone");
        int bekers = GuhQuests.count(player, CircuitFeature.CIRCUITBEKER.get());
        helper.assertTrue(bekers == RaceGame.Medal.GOUD.prizes + RaceGame.FIRST_PRIZES, "gold (6) + the first race bag (4) circuitbekers: " + bekers);
        helper.assertTrue(GuhQuests.count(player, RaceFeature.RACEPRIJSJE.get()) == 0, "no raceprijsjes on the circuit");
        helper.assertTrue(GuhQuests.count(player, ModItems.KAAS_KNABBELS.get()) == 16, "and kaasknabbels for the first race");
        String rec = CircuitBanen.REGENBOOG.records(Niveau.MEDIUM);
        int best = RaceRecords.best(player, rec);
        helper.assertTrue(best > 0 && RaceRecords.best(player) < 0 && RaceRecords.finishedOnce(player, "regenboog"),
                "a record of its own (not the old racebaan's): " + best);
        helper.assertTrue(RaceRecords.splits(player, rec).length == 3 * 5, "a time for every ring");
        helper.assertTrue(Scorebord.top(player.server, "circuit_regenboog_medium").stream().anyMatch(e -> e.player().equals(player.getUUID())),
                "on the board circuit_regenboog_medium");
        helper.assertTrue(Scorebord.top(player.server, "circuit_regenboog_medium_ronde").stream().anyMatch(e -> e.player().equals(player.getUUID())),
                "and on circuit_regenboog_medium_ronde");
        RaceGeesten.Geest goud = RaceGeesten.geest(player.server, "circuit_regenboog_medium");
        helper.assertTrue(goud != null && goud.player().equals(player.getUUID()) && goud.ticks() == best, "the track record is the golden ghost now");
        // the other two tracks are there too
        RaceTrack vads = RaceTrack.of(npc, CircuitBanen.VADS), berg = RaceTrack.of(npc, CircuitBanen.KAASBERG);
        helper.assertTrue(vads != null && vads.gates.size() == 6 && vads.markers("circuit_looping").size() == 1, "the Vadsbaan: 6 rings and the looping");
        helper.assertTrue(berg != null && berg.gates.size() == 6 && berg.markers("circuit_rolplek").size() == 3, "the Kaasbergbaan: 6 rings, 3 rolling spots");
        // the circuit can't be broken
        Player walker = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(RaceProtection.denied(walker, BlockPos.containing(game.track().startPos()).below()), "no digging in the circuit's road");
        // the scoreboards float at the tracks
        CircuitRole.showScores(npc);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, helper.getBounds(),
                d -> d.getTags().contains(Scorebord.TAG)).isEmpty(), "the boards float at the tracks");
        leave(helper, player, other);
        helper.succeed();
    }

    @GameTest(batch = "circuit_kaasberg", template = CIRCUIT, timeoutTicks = 400)
    public static void circuitKaasbergLastigPikkersAndRollingKnabbels(GameTestHelper helper) {
        GuhNpcEntity npc = coach(helper);
        ServerPlayer player = racer(helper, npc);
        RaceGeesten.forget(player.server, CircuitBanen.KAASBERG.boardTotal(Niveau.LASTIG));
        CircuitRole.action(npc, player, CircuitRole.START, "kaasberg", Niveau.LASTIG.ordinal());
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.baan() == CircuitBanen.KAASBERG && game.niveau() == Niveau.LASTIG, "a lastig race on the Kaasberg");
        game.trustTeleports();
        RaceGuhEntity mount = mount(helper, player);
        helper.assertTrue(mount.niveau() == Niveau.LASTIG, "a lastig race guh");
        List<MikaPikkerEntity> mikas = helper.getLevel().getEntitiesOfClass(MikaPikkerEntity.class, helper.getBounds().inflate(8));
        long pikkers = mikas.stream().filter(m -> !m.isDuwer()).count(), duwers = mikas.stream().filter(MikaPikkerEntity::isDuwer).count();
        helper.assertTrue(pikkers == 6 && duwers == 3, "lastig: all 6 Mika-pikkers and 3 pushers: " + pikkers + " / " + duwers);
        game.skipCountdown(npc);
        for (int i = 0; i < CircuitExtra.ROL_TICKS[Niveau.LASTIG.ordinal()] + 2; i++) {
            game.tick(npc);
        }
        helper.assertTrue(count(helper, RolknabbelEntity.class) >= 1, "the Mika's push a kaasknabbel down the Knabbelhelling");
        helper.assertTrue(!mount.sprongen(), "no rainbow jump on the Kaasbergbaan (it has no gaps)");
        // run past a Mika-pikker: it pinches the VAHOEG
        MikaPikkerEntity pikker = mikas.stream().filter(m -> !m.isDuwer()).findFirst().orElseThrow();
        Vec3 home = pikker.home();
        mount.teleportTo(home.x + 1.2, home.y, home.z);
        game.tick(npc);
        helper.assertTrue(pikker.pinches() == 1 && mount.lastSchok() == RaceGuhEntity.SCHOK_PIK, "the Mika-pikker pinched the race guh's VAHOEG");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "a Mika-pikker never hurts");
        RaceGame.playerGone(player);
        helper.assertTrue(RaceGame.of(npc) == null && mount.isRemoved(), "logging out ends the race");
        helper.assertTrue(count(helper, MikaPikkerEntity.class) == 0 && count(helper, RolknabbelEntity.class) == 0,
                "the Mika's and the knabbels go away with the race");
        helper.assertTrue(RaceRecords.best(player, CircuitBanen.KAASBERG.records(Niveau.LASTIG)) < 0, "an unfinished race changes no records");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(batch = "circuit_vads", template = CIRCUIT, timeoutTicks = 400)
    public static void circuitVadsloopingGoesRoundAndOut(GameTestHelper helper) {
        GuhNpcEntity npc = coach(helper);
        ServerPlayer player = racer(helper, npc);
        CircuitRole.action(npc, player, CircuitRole.START, "vads", Niveau.MAKKELIJK.ordinal());
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.baan() == CircuitBanen.VADS, "a race on the Vadsbaan");
        game.trustTeleports();
        RaceGuhEntity mount = mount(helper, player);
        helper.assertTrue(count(helper, MikaPikkerEntity.class) == 2, "makkelijk: only 2 Mika-pikkers: " + count(helper, MikaPikkerEntity.class));
        game.skipCountdown(npc);
        RaceTrack.Marker loop = game.track().markers("circuit_looping").get(0);
        RaceRit ride = CircuitExtra.looping(loop);
        Vec3 in = loop.centre();
        mount.teleportTo(in.x, in.y, in.z);
        mount.setYRot(loop.direction().toYRot());
        game.tick(npc);
        helper.assertTrue(mount.inRit(), "running into the Vadslooping starts the ride");
        Vec3 exit = ride.exit();
        // the ride's way out lies on the Vadsbaan's road (the leg after the looping)
        BlockPos below = BlockPos.containing(exit).below();
        helper.assertTrue(helper.getLevel().getBlockState(below).is(RaceGame.TRACK_BLOCKS), "the way out is on the road: " + helper.getLevel().getBlockState(below));
        helper.succeedWhen(() -> {
            helper.assertTrue(!mount.inRit(), "still going round");
            helper.assertTrue(mount.position().distanceTo(exit) < 1.5, "out at the other end: " + mount.position() + " / " + exit);
            helper.assertTrue(game.resets() == 0 && RaceGame.of(npc) == game, "no reset for going round the loop");
            RaceGame.playerGone(player);
            leave(helper, player);
        });
    }

    /** The top of the road in template column (x, z): the lowest road block with room above it (null: no road there). */
    private static Vec3 roadTop(GameTestHelper helper, CircuitBanen.Frame frame, int x, int z) {
        for (int y = 0; y < CircuitBanen.H - 2; y++) {
            BlockPos p = frame.toWorld(new BlockPos(x, y, z));
            var state = helper.getLevel().getBlockState(p);
            if (state.is(RaceGame.TRACK_BLOCKS) && !state.getCollisionShape(helper.getLevel(), p).isEmpty()
                    && helper.getLevel().getBlockState(p.above()).getCollisionShape(helper.getLevel(), p.above()).isEmpty()
                    && helper.getLevel().getBlockState(p.above(2)).getCollisionShape(helper.getLevel(), p.above(2)).isEmpty()) {
                return new Vec3(p.getX() + 0.5, p.getY() + state.getCollisionShape(helper.getLevel(), p).max(Direction.Axis.Y), p.getZ() + 0.5);
            }
        }
        return null;
    }

    /**
     * 2.10: the Regenboogbaan's last leg runs down from ring 4 (high in the sky) to the ground: more than 10 blocks lower
     * than the ring. That is driving, not falling (falls are measured from the last height on the road), so no "Oepsie!".
     */
    @GameTest(batch = "circuit_regenboog_af", template = CIRCUIT, timeoutTicks = 400)
    public static void circuitRegenboogDownhillIsNoFall(GameTestHelper helper) {
        GuhNpcEntity npc = coach(helper);
        ServerPlayer player = racer(helper, npc);
        CircuitBanen.Frame frame = CircuitBanen.frame(helper.getLevel(), npc);
        helper.assertTrue(frame != null, "the coach finds her circuit");
        CircuitRole.action(npc, player, CircuitRole.START, "regenboog", Niveau.MAKKELIJK.ordinal());
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.baan() == CircuitBanen.REGENBOOG, "a race on the Regenboogbaan");
        game.trustTeleports();
        RaceGuhEntity mount = mount(helper, player);
        game.skipCountdown(npc);
        for (int gate : new int[]{1, 2, 3, 4}) {
            through(game, npc, mount, gate);
        }
        helper.assertTrue(game.next() == 0 && game.resets() == 0, "through ring 4: " + game.next());
        double ring = game.track().centre(4).y, lowest = ring;
        // down the rainbow road: south along x 22, then east towards the finish
        List<int[]> way = new java.util.ArrayList<>();
        for (int z = 42; z <= 61; z++) {
            way.add(new int[]{22, z});
        }
        for (int x = 23; x <= 48; x++) {
            way.add(new int[]{x, 61});
        }
        int driven = 0;
        for (int[] c : way) {
            Vec3 top = roadTop(helper, frame, c[0], c[1]);
            if (top == null) {
                continue;
            }
            mount.teleportTo(top.x, top.y, top.z);
            mount.setOnGround(true);
            game.tick(npc);
            lowest = Math.min(lowest, top.y);
            driven++;
        }
        helper.assertTrue(driven > 30, "drove the last leg on the road: " + driven);
        helper.assertTrue(ring - lowest > RaceGame.FALL, "the road really goes down more than a fall from the ring: " + (ring - lowest));
        helper.assertTrue(game.resets() == 0 && RaceGame.of(npc) == game, "driving down is no fall: " + game.resets() + " resets");
        // really falling off (from the ground-level road into a pit) still counts
        Vec3 now = mount.position();
        mount.teleportTo(now.x, now.y - 12, now.z);
        mount.setOnGround(false);                                     // (falling, not running on some road down there)
        game.tick(npc);
        helper.assertTrue(game.resets() == 1 && mount.position().y > now.y - 3, "a real fall still goes back: " + mount.position() + " from " + now + " resets " + game.resets() + " riding " + (player.getVehicle() == mount));
        RaceGame.playerGone(player);
        leave(helper, player);
        helper.succeed();
    }

    /**
     * 2.10.1: the Regenboogbaan's jumps (circuit_banen.py regenboog() gaps, template: the last road block before the gap,
     * its walking height, the first road block after it and its walking height; both run west). Keep in sync.
     */
    static final int[][] REGENBOOG_SPRONGEN = {{141, 18, 136, 17}, {58, 17, 53, 16}};
    static final int SPRONG_Z = 20;

    /**
     * 2.10.1 (the Regenboogbaan was impossible: at every gap the race guh fell down): drives a race guh the way the rider's
     * game does it (tick by tick, holding W, the real template's blocks) at every jump of the Regenboogbaan, on every level,
     * in the left, middle and right lane, from a standing start (worst case, after a reset) and at cruising speed: it has to
     * land on the road on the other side, never lower than it. And without the rainbow jump the gap really is too wide.
     */
    @GameTest(batch = "circuit_sprong", template = CIRCUIT, timeoutTicks = 100)
    public static void circuitRegenboogJumpsAreClearable(GameTestHelper helper) {
        GuhNpcEntity npc = coach(helper);
        CircuitBanen.Frame frame = CircuitBanen.frame(helper.getLevel(), npc);
        helper.assertTrue(frame != null, "the coach finds her circuit");
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.zza = 1;
        StringBuilder report = new StringBuilder();
        for (int[] gap : REGENBOOG_SPRONGEN) {
            int edge = gap[0], walk = gap[1], land = gap[2], landWalk = gap[3];
            // the gap is there: road, air, road (one block lower)
            for (int x = land + 1; x < edge; x++) {
                for (int y = landWalk - 6; y < walk + 3; y++) {
                    helper.assertTrue(helper.getLevel().getBlockState(frame.toWorld(new BlockPos(x, y, SPRONG_Z))).isAir(),
                            "the gap at " + x + "," + y + " is open");
                }
            }
            helper.assertTrue(!helper.getLevel().getBlockState(frame.toWorld(new BlockPos(edge, walk - 1, SPRONG_Z))).isAir()
                    && !helper.getLevel().getBlockState(frame.toWorld(new BlockPos(land, landWalk - 1, SPRONG_Z))).isAir(), "road on both sides of the gap");
            for (Niveau niveau : Niveau.values()) {
                for (double lane : new double[]{-2.4, 0, 2.4}) {
                    for (int runUp : new int[]{10, 2}) {
                        for (boolean cruising : new boolean[]{false, true}) {
                            String result = jump(helper, frame, rider, gap, niveau, lane, runUp, cruising, true);
                            report.append(edge).append('/').append(niveau.ordinal()).append('/').append(lane).append('/').append(runUp)
                                    .append(cruising ? "/cruise: " : "/stand: ").append(result).append("; ");
                            helper.assertTrue(result.startsWith("ok"), "jump " + edge + " " + niveau + " lane " + lane + ", " + runUp + " blocks run-up"
                                    + (cruising ? " cruising" : " from standstill") + ": " + result);
                        }
                    }
                }
            }
            // (the bug: without the rainbow jump a guh that isn't flat out and boosted drops into the gap)
            for (Niveau niveau : Niveau.values()) {
                for (int runUp : new int[]{10, 2}) {
                    String without = jump(helper, frame, rider, gap, niveau, 0, runUp, true, false);
                    report.append(edge).append('/').append(niveau.ordinal()).append('/').append(runUp).append(" without the jump: ").append(without).append("; ");
                    helper.assertTrue(runUp == 10 || !without.startsWith("ok"), "without the rainbow jump the gap at " + edge + " is too wide: " + without);
                }
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("[circuit_sprong] {}", report);
        helper.succeed();
    }

    /** One run at a jump: "ok ..." when it landed on the road past the gap (and never went lower than that road). */
    private static String jump(GameTestHelper helper, CircuitBanen.Frame frame, Player rider, int[] gap, Niveau niveau, double lane,
                               int runUp, boolean cruising, boolean sprongen) {
        int edge = gap[0], walk = gap[1], land = gap[2], landWalk = gap[3];
        Vec3 o = Vec3.atLowerCornerOf(frame.toWorld(BlockPos.ZERO));
        Vec3 west = o.subtract(Vec3.atLowerCornerOf(frame.toWorld(new BlockPos(1, 0, 0))));
        Vec3 south = Vec3.atLowerCornerOf(frame.toWorld(new BlockPos(0, 0, 1))).subtract(o);
        // runUp blocks before the edge (10: over the VAHOEG pad 3-4 blocks before each gap; 2: just past it, no boost), in a lane
        Vec3 start = Vec3.atBottomCenterOf(frame.toWorld(new BlockPos(edge + runUp, walk, SPRONG_Z))).add(south.scale(lane));
        Vec3 landing = Vec3.atBottomCenterOf(frame.toWorld(new BlockPos(land, landWalk, SPRONG_Z)));
        float yaw = (float) (Mth.atan2(west.z, west.x) * Mth.RAD_TO_DEG) - 90f;
        RaceGuhEntity guh = RaceFeature.RACE_GUH.get().create(helper.getLevel());
        guh.setUpForRace();
        guh.setNiveau(niveau);
        guh.setSprongen(sprongen);
        guh.moveTo(start.x, start.y, start.z, yaw, 0);
        guh.setYHeadRot(yaw);
        guh.setOnGround(true);
        if (cruising) {
            float top = RaceGuhEntity.TOP[niveau.ordinal()];
            guh.setRaceSpeedForTest(top);
            guh.setDeltaMovement(west.scale(top / (1 - 0.546) * 0.546));      // (the pace on the road: speed / (1 - 0.546) a tick)
        }
        double highest = -Double.MAX_VALUE;
        boolean glided = false, over = false;
        String out = null;
        for (int t = 0; t < 120 && out == null; t++) {
            guh.rideTickForTest(rider);
            glided |= guh.zweeft();
            double gone = guh.position().subtract(start).dot(west) - runUp - 0.5; // past the edge
            highest = Math.max(highest, guh.getY() - start.y);
            over |= gone > 0 && !guh.onGround();
            BlockPos under = guh.getOnPos();
            double underPast = Vec3.atCenterOf(under).subtract(landing).dot(west);   // >= 0: standing on the first road block after the gap or further
            if (guh.getY() < landing.y - 1.5) {
                out = "fell at t" + t + " (" + String.format("%.2f", gone) + " past the edge, y " + String.format("%.2f", guh.getY() - landing.y) + ")";
            } else if (over && guh.onGround()) {
                // the first landing after the edge: on the road past the gap, no lower than one block under its first road block
                boolean road = helper.getLevel().getBlockState(under).is(RaceGame.TRACK_BLOCKS);
                out = (underPast > -0.01 && road && guh.getY() > landing.y - 1.01 ? "ok" : "bad landing") + " t" + t + " flew to " + String.format("%.1f", gone)
                        + " up " + String.format("%.2f", highest) + (glided ? " glided" : "") + (road ? "" : " off the road");
            }
        }
        guh.discard();
        return out == null ? "neither landed nor fell: " + guh.position() : out;
    }

    // --- the small things -----------------------------------------------------------------------------------------------

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_QUARTZ);
            }
        }
    }

    private static RaceGuhEntity raceGuh(GameTestHelper helper, double x, double y, double z) {
        RaceGuhEntity guh = RaceFeature.RACE_GUH.get().create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        guh.moveTo(at.x, at.y, at.z, 0, 0);
        guh.setUpForRace();
        helper.getLevel().addFreshEntity(guh);
        return guh;
    }

    @GameTest(batch = "circuit_klein", template = EMPTY)
    public static void circuitBoostRingKaassausAndStuiterpaddenstoel(GameTestHelper helper) {
        floor(helper);
        RaceGuhEntity guh = raceGuh(helper, 2.5, 1, 2.5);
        helper.assertTrue(!guh.boostHere(), "no boost on plain quartz");
        helper.setBlock(new BlockPos(2, 1, 2), CircuitFeature.BOOSTRING.get().defaultBlockState());
        helper.assertTrue(guh.inBoostRing() && guh.boostHere(), "the race guh runs through a rainbow boost ring");
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.zza = 1;
        guh.setFrozen(false);
        guh.riddenInputForTest(rider);
        helper.assertTrue(guh.getRaceSpeed() >= RaceGuhEntity.BOOST_SPEED, "VAHOEG: the ring gives a boost");
        helper.assertTrue(CircuitFeature.BOOSTRING.get().defaultBlockState().getCollisionShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))).isEmpty(),
                "you run right through the ring");
        helper.assertTrue(CircuitFeature.KAASSAUS.get().getFriction() > 0.98f && CircuitFeature.BERGIJS.get().getFriction() > 0.97f,
                "kaassaus and bergijs are slippery");
        helper.assertTrue(CircuitFeature.KAASWEG.get().getFriction() < 0.7f, "the cheese road is not");
        // a stuiterpaddenstoel throws you up
        helper.setBlock(new BlockPos(1, 0, 1), CircuitFeature.STUITERPADDENSTOEL.get());
        RaceGuhEntity jumper = raceGuh(helper, 1.5, 1, 1.5);
        jumper.setOnGround(true);
        CircuitBlocks.Stuiterpaddenstoel.bounce(helper.getLevel(), helper.absolutePos(new BlockPos(1, 0, 1)), jumper);
        helper.assertTrue(jumper.getDeltaMovement().y >= CircuitBlocks.Stuiterpaddenstoel.STUITER - 1e-6, "boing: " + jumper.getDeltaMovement());
        guh.discard();
        jumper.discard();
        helper.succeed();
    }

    @GameTest(batch = "circuit_klein", template = EMPTY)
    public static void circuitLoopingPathGoesRoundAndOut(GameTestHelper helper) {
        Vec3 in = new Vec3(10.5, 64, 10.5);
        RaceRit rit = new RaceRit(in, Direction.SOUTH, CircuitBanen.LOOP_R, CircuitBanen.LOOP_L, CircuitBanen.LOOP_W, CircuitBanen.LOOP_TICKS);
        helper.assertTrue(rit.pos(0).distanceTo(in) < 1e-3, "it starts at the entrance");
        Vec3 out = rit.exit();
        helper.assertTrue(out.distanceTo(in.add(-CircuitBanen.LOOP_W, 0, CircuitBanen.LOOP_L)) < 1e-3, "12 forward (south) and 8 to the right (west): " + out);
        Vec3 top = rit.pos(0.5);
        helper.assertTrue(top.y - in.y > CircuitBanen.LOOP_R && top.y - in.y + 3.2 < 2 * CircuitBanen.LOOP_R + 0.6,
                "at the top the guh and its rider hang under the road of the loop: " + (top.y - in.y));
        // a live ride: a race guh goes round by itself and comes out with a VAHOEG boost
        floor(helper);
        RaceGuhEntity guh = raceGuh(helper, 2.5, 1, 2.5);
        Vec3 start = guh.position();
        RaceRit kort = new RaceRit(start, Direction.EAST, 3, 2, 1, 16);
        guh.startRit(kort);
        helper.assertTrue(guh.inRit() && guh.isNoGravity(), "riding the loop");
        helper.succeedWhen(() -> {
            helper.assertTrue(!guh.inRit(), "still riding");
            helper.assertTrue(guh.position().distanceTo(kort.exit()) < 0.6, "out at the end: " + guh.position());
            helper.assertTrue(guh.boostTicks() > 0 && guh.getRaceSpeed() >= RaceGuhEntity.BOOST_SPEED, "with a VAHOEG boost");
            guh.discard();
        });
    }

    @GameTest(batch = "circuit_klein", template = EMPTY, timeoutTicks = 100)
    public static void circuitMikaPikkerPinchesAndPopsBack(GameTestHelper helper) {
        floor(helper);
        RaceGuhEntity guh = raceGuh(helper, 1.5, 1, 1.5);
        MikaPikkerEntity mika = CircuitFeature.MIKAPIKKER.get().create(helper.getLevel());
        Vec3 home = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
        mika.setHome(home, 90);
        CircuitExtra.markLive(mika);
        helper.getLevel().addFreshEntity(mika);
        helper.assertTrue(mika.kanPikken(), "ready to pinch");
        helper.assertTrue(mika.isInvulnerableTo(helper.getLevel().damageSources().generic()), "a Mika-pikker can't be hurt (it's part of the game)");
        helper.assertTrue(!mika.doHurtTarget(guh), "and never hurts anyone");
        mika.pik(guh, null);
        helper.assertTrue(guh.lastSchok() == RaceGuhEntity.SCHOK_PIK && !mika.kanPikken() && mika.pinches() == 1, "it pinched the VAHOEG and giggles off");
        helper.succeedWhen(() -> {
            helper.assertTrue(mika.position().distanceTo(home) < 0.3, "back on its spot (poof)");
            mika.discard();
            guh.discard();
        });
    }

    @GameTest(batch = "circuit_klein", template = EMPTY, timeoutTicks = 100)
    public static void circuitRolknabbelBumpsARaceGuh(GameTestHelper helper) {
        floor(helper);
        RaceGuhEntity guh = raceGuh(helper, 4.2, 1, 2.5);
        RolknabbelEntity knabbel = CircuitFeature.ROLKNABBEL.get().create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(0.8, 1, 2.5));
        knabbel.moveTo(at.x, at.y, at.z, 0, 0);
        knabbel.rol(new Vec3(1, 0, 0));
        CircuitExtra.markLive(knabbel);
        helper.getLevel().addFreshEntity(knabbel);
        helper.succeedWhen(() -> {
            helper.assertTrue(knabbel.isRemoved() && knabbel.bumped(), "the knabbel rolled into the race guh and crumbled");
            helper.assertTrue(guh.lastSchok() == RaceGuhEntity.SCHOK_BOTS, "the race guh got a bump (no damage)");
            guh.discard();
        });
    }

    @GameTest(batch = "circuit_klein", template = EMPTY)
    public static void circuitCoachSellsTheOutfitOnlyHere(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 1, 2));
        npc.setKind(GuhNpcEntity.Kind.CIRCUITGUH);
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == 3, "three pieces: " + offers.size());
        List<GuhClothes> sold = offers.stream().map(o -> ((nl.juiced.guhs.item.GuhClothingItem) o.getResult().getItem()).getClothes()).toList();
        helper.assertTrue(sold.containsAll(List.of(GuhClothes.CIRCUIT_HELMPJE, GuhClothes.CIRCUIT_RACEPAK, GuhClothes.CIRCUIT_VLAGCAPE)), "the circuit outfit: " + sold);
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(CircuitFeature.CIRCUITBEKER.get()) && o.getCostB().isEmpty()
                && o.getMaxUses() == Integer.MAX_VALUE), "for circuitbekers only, never sold out");
        helper.assertTrue(GuhClothes.CIRCUIT_HELMPJE.slot == GuhClothes.Slot.HEAD && GuhClothes.CIRCUIT_RACEPAK.slot == GuhClothes.Slot.BODY
                && GuhClothes.CIRCUIT_VLAGCAPE.slot == GuhClothes.Slot.BACK, "a helmet, a racing suit and a cape on the back");
        for (GuhClothes c : sold) {
            helper.assertTrue("circuit".equals(KledingBronnen.bron(c)) && KledingBronnen.prijs(c) != null, "one source: the circuit (" + c + ")");
        }
        helper.assertTrue(CircuitRole.PRICE_HELMPJE < CircuitRole.PRICE_VLAGCAPE && CircuitRole.PRICE_VLAGCAPE < CircuitRole.PRICE_RACEPAK,
                "a hat is cheapest, the suit dearest");
        npc.discard();
        helper.succeed();
    }

    @GameTest(batch = "circuit_klein", template = EMPTY)
    public static void circuitPrizesLevelsAndMedalTimes(GameTestHelper helper) {
        // every reward rule is one more than its base (the 2.7 rule): medals 5/3/2/1 + 1, record 1 + 1, first race 3 + 1
        int[] base = {5, 3, 2, 1};
        for (RaceGame.Medal m : RaceGame.Medal.values()) {
            helper.assertTrue(m.prizes == base[m.ordinal()] + 1, "medal " + m);
        }
        helper.assertTrue(RaceGame.RECORD_PRIZES == 1 + 1 && RaceGame.FIRST_PRIZES == 3 + 1, "record bonus and first bag");
        helper.assertTrue(RaceGame.prizes(RaceGame.Medal.GOUD, Niveau.MEDIUM, false) == 6 && RaceGame.prizes(RaceGame.Medal.GOUD, Niveau.LASTIG, false) == 9
                && RaceGame.prizes(RaceGame.Medal.BRONS, Niveau.MAKKELIJK, true) == 5, "lastig +50 %, +2 for a record");
        for (RaceBaan baan : List.of(RaceBaan.RACEBAAN, CircuitBanen.REGENBOOG, CircuitBanen.VADS, CircuitBanen.KAASBERG)) {
            for (int m = 0; m < 3; m++) {
                helper.assertTrue(baan.medalTicks(m, Niveau.MAKKELIJK) > baan.medalTicks(m, Niveau.MEDIUM)
                        && baan.medalTicks(m, Niveau.MEDIUM) > baan.medalTicks(m, Niveau.LASTIG), "more time on makkelijk (" + baan + ")");
            }
            helper.assertTrue(baan.medalTicks(0, Niveau.MEDIUM) < baan.medalTicks(1, Niveau.MEDIUM) && baan.medalTicks(1, Niveau.MEDIUM) < baan.medalTicks(2, Niveau.MEDIUM),
                    "gold is fastest (" + baan + ")");
            helper.assertTrue(RaceGame.Medal.of(baan.medalTicks(0, Niveau.LASTIG), baan, Niveau.LASTIG) == RaceGame.Medal.GOUD
                    && RaceGame.Medal.of(baan.medalTicks(2, Niveau.LASTIG) + 1, baan, Niveau.LASTIG) == RaceGame.Medal.FINISH, "medals by level (" + baan + ")");
        }
        helper.assertTrue(RaceBaan.RACEBAAN.medalTicks(0, Niveau.MEDIUM) == RaceGame.Medal.GOUD.ticks, "the old racebaan at medium keeps its 2.4 medals");
        // boards and records per track and level (the old racebaan's medium keeps its ids)
        helper.assertTrue(RaceBaan.RACEBAAN.boardTotal(Niveau.MEDIUM).equals("race_total") && RaceBaan.RACEBAAN.boardLap(Niveau.LASTIG).equals("race_lap_lastig")
                && RaceBaan.RACEBAAN.records(Niveau.MEDIUM).isEmpty(), "the old racebaan's boards");
        helper.assertTrue(CircuitBanen.VADS.boardTotal(Niveau.MEDIUM).equals("circuit_vads_medium") && CircuitBanen.KAASBERG.boardLap(Niveau.MAKKELIJK)
                .equals("circuit_kaasberg_makkelijk_ronde"), "the circuit's boards");
        for (RaceBaan baan : CircuitBanen.BANEN) {
            for (Niveau n : Niveau.values()) {
                helper.assertTrue(nl.juiced.guhs.quest.Highscores.game(baan.boardTotal(n)) != null
                        && nl.juiced.guhs.quest.Highscores.game(baan.boardLap(n)) != null, "a Highscores row for " + baan.boardTotal(n));
            }
        }
        // the race guh per level: calmer, as it was, faster (but it turns after your mouse)
        helper.assertTrue(RaceGuhEntity.TOP[0] < RaceGuhEntity.TOP[1] && RaceGuhEntity.TOP[1] < RaceGuhEntity.TOP[2]
                && RaceGuhEntity.TOP[1] == RaceGuhEntity.TOP_SPEED && RaceGuhEntity.BOOST[1] == RaceGuhEntity.BOOST_SPEED, "speeds per level");
        helper.assertTrue(CircuitExtra.REACH[0] < CircuitExtra.REACH[1] && CircuitExtra.REACH[1] < CircuitExtra.REACH[2]
                && CircuitExtra.ROL_TICKS[0] > CircuitExtra.ROL_TICKS[1] && CircuitExtra.ROL_TICKS[1] > CircuitExtra.ROL_TICKS[2],
                "more pikkers and knabbels the harder it gets");
        helper.succeed();
    }
}
