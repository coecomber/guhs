package nl.juiced.guhs.feature.race;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A guhrace on one track (one race at a time per race guh NPC; it belongs to the NPC who lends out the race guh: the
 * Raceguh of the old racebaan, or Coach Vahoegvroem of the Guh-Circuit with its three tracks).
 * <ol>
 *     <li>Start: the racer gets a rental race guh on the start marker (and their best race as a ghost next to them, and
 *     the world's track record as a golden ghost). A countdown of 3 seconds: 3, 2, 1, VAHOEG!</li>
 *     <li>The laps through all the checkpoint rings in order (a ring you skip doesn't count: go back!), with the VAHOEG
 *     pads and boost rings for a boost. Fall off, land in the kaas saus or get stuck: back to the last checkpoint
 *     (makkelijk: back a little way, where you were two seconds ago).</li>
 *     <li>Finish: coins for the time (more for faster, lastig +50 %), a personal record per track and level (total and
 *     best lap, and a new ghost), a place on the world's top 3 (Scorebord); first place is the track record and its race
 *     becomes the golden ghost.</li>
 * </ol>
 * Getting off gives you 5 seconds to hop back on; logging out, dying, leaving or changing dimension ends the race. The
 * race guh and the ghosts disappear with the race. While racing you can't get hurt or hungry.
 * <p>
 * 2.9: the track ({@link RaceBaan}) and the level ({@link Niveau}) set the laps, medal times, coin, boards, records and
 * the race guh's run; a track's {@link RaceBaan.Extra} adds its own things (Mika-pikkers, rolling knabbels, looping...).
 */
public final class RaceGame {
    /** Laps of the old racebaan. */
    public static final int LAPS = 3;
    /** Extra coins for beating your own record, and in the welcome bag of your first race (on a track). */
    public static final int RECORD_PRIZES = 2, FIRST_PRIZES = 4;
    public static final int COUNTDOWN = 60, OFF_GRACE = 100, STUCK_TICKS = 200, OFF_TRACK_TICKS = 25, MAX_TICKS = 20 * 60 * 6;
    /** Makkelijk ("wide rails"): off the road for this long before it counts, and a reset only goes back a little way. */
    public static final int OFF_TRACK_TICKS_MAKKELIJK = 70, TERUG_TICKS = 40;
    /**
     * 2.10 (no more "Oepsie!" loops): after a reset the rider's game gets a moment to catch up (its guh is put back with a
     * vehicle packet; late positions from before it may still arrive), so for {@link #GRACE_TICKS} nothing sends it back again.
     */
    public static final int GRACE_TICKS = 30;
    /** A reset message in the chat at most this often (more resets in between only show above the hotbar). */
    public static final int RESET_MESSAGE_TICKS = 100;
    /** Falling this far below the last height the guh ran on the road (or the last ring, if that's lower) is falling off. */
    public static final double FALL = 10;
    /** What the race guh may run on: the road, the kerbs, the finish line, the tongue in the guh head, the pads. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> TRACK_BLOCKS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, nl.juiced.guhs.Guhs.id("race_track"));
    /** 2.9: blocks that give a race guh a boost when it runs through them (the Regenboogbaan's rainbow rings). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BOOST_BLOCKS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, nl.juiced.guhs.Guhs.id("race_boost"));
    /** The ghost is recorded every this many ticks. */
    public static final int SAMPLE = 2;
    /** How far from the Raceguh the race may go before it counts as having left. */
    public static final int FAR = 150;
    /** How far from Coach Vahoegvroem a circuit race may go (the circuit is bigger). */
    public static final int FAR_CIRCUIT = 190;

    /** Medals by total time (ticks, the old racebaan at medium): what you get for them. */
    public enum Medal {
        GOUD(20 * 72, 6, ChatFormatting.GOLD),
        ZILVER(20 * 85, 4, ChatFormatting.WHITE),
        BRONS(20 * 105, 3, ChatFormatting.RED),
        FINISH(Integer.MAX_VALUE, 2, ChatFormatting.LIGHT_PURPLE);

        public final int ticks;
        public final int prizes;
        public final ChatFormatting colour;

        Medal(int ticks, int prizes, ChatFormatting colour) {
            this.ticks = ticks;
            this.prizes = prizes;
            this.colour = colour;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Medal of(int ticks) {
            for (Medal m : values()) {
                if (ticks <= m.ticks) {
                    return m;
                }
            }
            return FINISH;
        }

        /** The medal of a race time on a track and level. */
        public static Medal of(int ticks, RaceBaan baan, Niveau niveau) {
            for (int i = 0; i < 3; i++) {
                if (ticks <= baan.medalTicks(i, niveau)) {
                    return values()[i];
                }
            }
            return FINISH;
        }
    }

    /** Why a race ends early. */
    public enum Ending { GAVE_UP, GONE, TOO_LATE, STOPPED }

    private static final Map<UUID, RaceGame> GAMES = new ConcurrentHashMap<>();       // by race guh NPC
    private static final Map<UUID, RaceGame> RACERS = new ConcurrentHashMap<>();      // by racer
    private static final String[] GUH_NAMES = {"Bliksemvads", "Turbo Njeg", "Vahoeg 3000", "Roze Donder", "Kaasknabbel Express", "Vadsraket",
            "Snelle Gerrit", "Pluizige Pijl", "Knabbelknaller", "Wervelguh"};
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1f, 0.55f, 0.8f), 1.4f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.4f);

    private final UUID npcId;
    private final Vec3 npcPos;
    private final ResourceKey<Level> dimension;
    private final RaceBaan baan;
    private final Niveau niveau;
    /** Game time of the last race tick (the race guh drives the race; the NPC ends a race that stopped ticking). */
    private long lastTick;
    private final RaceTrack track;
    private final UUID racer;
    private UUID guh;
    @Nullable
    private UUID ghost;
    @Nullable
    private UUID goudGhost;
    private int[] ghostSamples = new int[0];
    private int[] goudSamples = new int[0];
    private int goudTicks = -1;
    private int[] pbSplits = new int[0];

    private boolean racing;          // false = the countdown
    private int countdown;
    private int ticks;               // race time
    private int lap;                 // laps done
    private int lapStart;
    private final List<Integer> lapTimes = new ArrayList<>();
    private int next = 1;            // the ring to go through next (0 = the finish)
    private int lastGate = 0;
    private int lastGateTicks = 0;
    private int insideGate = 0;
    private final List<Integer> splits = new ArrayList<>();
    private final List<Integer> samples = new ArrayList<>();
    private Vec3 lastPos;
    private Vec3 respawn;
    private float respawnYaw;
    /** Makkelijk: where the guh was on the road, now and then (ticks, x, y, z, yaw). */
    private final Deque<double[]> onRoad = new ArrayDeque<>();
    private int offTicks, stuckTicks, offTrack, missedCooldown, resets, pads;
    private boolean wasInRit;
    private Vec3 stuckFrom;
    /** The speed check: where the guh was {@link #paceTicks} ticks ago. */
    private Vec3 paceFrom;
    private int paceTicks;
    private boolean paceChecks = true;
    /** 2.10: the height the guh last ran on the road (falls are measured from here, not from the ring). */
    private double roadY;
    /** 2.10: ticks left in which nothing sends the guh back (just after a reset). */
    private int grace;
    /** 2.10: the ring's own spot wasn't a safe place to come back to: the next spot on the road becomes the respawn. */
    private boolean respawnPending;
    /** 2.10: when the last reset message went to the chat (race ticks), and how many did (tests). */
    private int lastResetChat = -RESET_MESSAGE_TICKS;
    private int resetChats;
    /** What the track's extras keep during the race (Mika-pikkers, rolling knabbels...). */
    public final Map<String, Object> extraState = new HashMap<>();
    /**
     * A boosted race guh runs about 0.84 blocks a tick (RaceGuhEntity.BOOST_SPEED on normal ground): more than this in
     * one tick, or more than {@link #MAX_PACE} in {@link #PACE_TICKS} ticks, can't be real.
     */
    static final double MAX_STEP = 3.0, MAX_PACE = 45.0;
    static final int PACE_TICKS = 40;
    private int lastDelta = Integer.MIN_VALUE;

    private RaceGame(GuhNpcEntity npc, RaceTrack track, ServerPlayer racer, RaceBaan baan, Niveau niveau) {
        this.npcId = npc.getUUID();
        this.npcPos = npc.position();
        this.dimension = npc.level().dimension();
        this.lastTick = npc.level().getGameTime();
        this.track = track;
        this.racer = racer.getUUID();
        this.baan = baan;
        this.niveau = niveau;
        this.lastPos = this.respawn = this.stuckFrom = this.paceFrom = track.startPos();
        this.respawnYaw = track.startYaw();
        this.roadY = track.startPos().y;
    }

    // --- who is who ---------------------------------------------------------------------------------------------------

    @Nullable
    public static RaceGame of(GuhNpcEntity npc) {
        return GAMES.get(npc.getUUID());
    }

    @Nullable
    public static RaceGame of(Player player) {
        return RACERS.get(player.getUUID());
    }

    /** Racing on the old guh racebaan right now (Minigames.RACE)? The circuit's races count as Minigames.CIRCUIT. */
    public static boolean isRacing(Player player) {
        RaceGame game = RACERS.get(player.getUUID());
        return game != null && game.baan.isRacebaan();
    }

    /** Racing anywhere (old racebaan or circuit): no damage, no hunger. */
    public static boolean isRacingAny(Player player) {
        return RACERS.containsKey(player.getUUID());
    }

    /** Racing on one of the circuit's tracks (or another track of that minigame id)? */
    public static boolean isRacingIn(Player player, String minigame) {
        RaceGame game = RACERS.get(player.getUUID());
        return game != null && game.baan.minigame.equals(minigame);
    }

    public static boolean isRacerOf(ServerPlayer player, RaceGuhEntity guh) {
        RaceGame game = RACERS.get(player.getUUID());
        return game != null && guh.getUUID().equals(game.guh);
    }

    static boolean isRaceGuh(Entity entity) {
        return GAMES.values().stream().anyMatch(g -> entity.getUUID().equals(g.guh));
    }

    static boolean isGhost(Entity entity) {
        return GAMES.values().stream().anyMatch(g -> entity.getUUID().equals(g.ghost) || entity.getUUID().equals(g.goudGhost));
    }

    public UUID racer() {
        return racer;
    }

    public RaceBaan baan() {
        return baan;
    }

    public Niveau niveau() {
        return niveau;
    }

    public boolean isRacing() {
        return racing;
    }

    public int lap() {
        return lap;
    }

    public int laps() {
        return baan.laps;
    }

    public int next() {
        return next;
    }

    public int ticks() {
        return ticks;
    }

    public int resets() {
        return resets;
    }

    public RaceTrack track() {
        return track;
    }

    @Nullable
    public UUID guh() {
        return guh;
    }

    @Nullable
    public UUID ghost() {
        return ghost;
    }

    @Nullable
    public UUID goudGhost() {
        return goudGhost;
    }

    /** The golden ghost's time (the track record when the race started), or -1 without one. */
    public int goudTicks() {
        return goudTicks;
    }

    @Nullable
    public RaceGuhEntity mount(ServerLevel level) {
        return level.getEntity(guh) instanceof RaceGuhEntity g ? g : null;
    }

    // --- starting -------------------------------------------------------------------------------------------------------

    /** The racer asked the Raceguh for a race (the old racebaan, medium). */
    public static void start(GuhNpcEntity npc, ServerPlayer player) {
        start(npc, player, RaceBaan.RACEBAAN, Niveau.MEDIUM);
    }

    /** The racer asked for a race on a track and level. */
    public static void start(GuhNpcEntity npc, ServerPlayer player, RaceBaan baan, Niveau niveau) {
        ServerLevel level = (ServerLevel) npc.level();
        RaceGame running = GAMES.get(npc.getUUID());
        if (running != null) {
            ServerPlayer other = level.getServer().getPlayerList().getPlayer(running.racer);
            GuhQuests.say(player, npc, "quest.guhs.race.busy", other == null ? "?" : other.getDisplayName(), Math.min(running.lap + 1, running.laps()),
                    running.laps());
            return;
        }
        if (isRacingAny(player)) {
            return;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(player, npc, baan.minigame)) {
            return;
        }
        RaceTrack track = RaceTrack.of(npc, baan);
        if (track == null) {
            GuhQuests.say(player, npc, "quest.guhs.race.broken");
            return;
        }
        RaceGame game = new RaceGame(npc, track, player, baan, niveau);
        RaceGuhEntity guh = RaceFeature.RACE_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return;
        }
        Vec3 pos = track.startPos();
        guh.snapTo(pos.x, pos.y, pos.z, track.startYaw(), 0);
        guh.setYHeadRot(track.startYaw());
        guh.setUpForRace();
        guh.setNiveau(niveau);
        guh.wear(GuhClothes.JOCKEY_PET);
        guh.wear(GuhClothes.JOCKEY_JASJE);
        guh.wear(GuhClothes.RACEBRIL);
        guh.setCustomName(Component.literal(GUH_NAMES[level.getRandom().nextInt(GUH_NAMES.length)]));
        guh.setFrozen(true);
        level.addFreshEntity(guh);
        game.guh = guh.getUUID();
        GAMES.put(npc.getUUID(), game);
        RACERS.put(player.getUUID(), game);

        player.stopRiding();
        player.closeContainer();
        player.teleportTo(level, pos.x, pos.y, pos.z, track.startYaw(), 0);
        player.startRiding(guh, true);
        nl.juiced.guhs.feature.Minigames.startKeeping(player);

        // your best race drives along as a ghost
        String rec = baan.records(niveau);
        game.pbSplits = RaceRecords.splits(player, rec);
        int[] recording = RaceRecords.ghost(player, rec);
        if (recording.length >= 6 && RaceRecords.ghostOn(player)) {
            RaceGhostEntity ghost = game.spawnGhost(level, false,
                    Component.translatable("entity.guhs.race_ghost.name", RaceRecords.time(RaceRecords.best(player, rec))));
            if (ghost != null) {
                game.ghost = ghost.getUUID();
                game.ghostSamples = recording;
            }
        }
        // and the world's track record in gold (when it's someone else's)
        RaceGeesten.Geest record = RaceGeesten.geest(level.getServer(), baan.boardTotal(niveau));
        if (record != null && !record.player().equals(player.getUUID()) && RaceRecords.goudOn(player) && record.samples().length >= 6) {
            RaceGhostEntity goud = game.spawnGhost(level, true,
                    Component.translatable("entity.guhs.race_ghost.goud", record.name(), RaceRecords.time(record.ticks())));
            if (goud != null) {
                game.goudGhost = goud.getUUID();
                game.goudSamples = record.samples();
                game.goudTicks = record.ticks();
            }
        }
        GuhQuests.say(player, npc, baan.isRacebaan() ? "quest.guhs.race.go" : "quest.guhs.circuit.go." + baan.id);
        player.sendSystemMessage(Component.translatable("quest.guhs.race.controls").withStyle(ChatFormatting.GRAY));
        if (niveau != Niveau.MEDIUM) {
            player.sendSystemMessage(Component.translatable("quest.guhs.race.niveau." + niveau.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (baan.extra() != null) {
            baan.extra().start(game, level, player);
        }
        game.hud(player);
    }

    @Nullable
    private RaceGhostEntity spawnGhost(ServerLevel level, boolean goud, Component name) {
        RaceGhostEntity ghost = RaceFeature.RACE_GHOST.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ghost == null) {
            return null;
        }
        Vec3 pos = track.startPos();
        ghost.snapTo(pos.x, pos.y, pos.z, track.startYaw(), 0);
        ghost.setGuhScale(RaceGuhEntity.SCALE);
        ghost.setVariant(GuhVariant.NORMAL);
        ghost.equipSaddle(new ItemStack(net.minecraft.world.item.Items.SADDLE), null);
        ghost.setGoud(goud);
        ghost.setCustomName(name);
        level.addFreshEntity(ghost);
        return ghost;
    }

    // --- every tick ------------------------------------------------------------------------------------------------------

    /**
     * The race guh drives its race every tick (it is always where the racer is, so the race keeps going even when the
     * NPC's pit stop is too far away to tick). The NPC only ends a race that stopped ticking (its guh is gone).
     */
    static void tickFrom(RaceGuhEntity mount) {
        for (RaceGame game : GAMES.values()) {
            if (mount.getUUID().equals(game.guh)) {
                game.tick((ServerLevel) mount.level());
                return;
            }
        }
    }

    /** From the NPC: a race whose guh stopped ticking (unloaded, gone) is over. */
    public void checkAlive(GuhNpcEntity npc) {
        if (npc.level().getGameTime() - lastTick > 40) {
            end((ServerLevel) npc.level(), Ending.GONE);
        }
    }

    /** One race tick (for tests: {@link #tick(ServerLevel)} from the NPC's level). */
    public void tick(GuhNpcEntity npc) {
        tick((ServerLevel) npc.level());
    }

    private int far() {
        return baan.isRacebaan() ? FAR : FAR_CIRCUIT;
    }

    void tick(ServerLevel level) {
        lastTick = level.getGameTime();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(racer);
        RaceGuhEntity mount = level.getEntity(guh) instanceof RaceGuhEntity g ? g : null;
        if (player == null || !player.isAlive() || player.level() != level || mount == null || !mount.isAlive()
                || player.distanceToSqr(npcPos) > far() * far()) {
            end(level, Ending.GONE);
            return;
        }
        refresh(player);
        if (!racing) {
            tickCountdown(level, player, mount);
            return;
        }
        ticks++;
        // off the guh: 5 seconds to hop back on
        if (player.getVehicle() != mount) {
            if (offTicks++ == 0) {
                player.sendSystemMessage(Component.translatable("quest.guhs.race.off", OFF_GRACE / 20).withStyle(ChatFormatting.YELLOW));
            }
            if (offTicks > OFF_GRACE) {
                end(level, Ending.GAVE_UP);
                return;
            }
        } else {
            offTicks = 0;
        }
        Vec3 now = mount.position();
        boolean inRit = mount.inRit();
        boolean graced = grace > 0;
        if (graced) {
            // just put back: a late position from the rider's game (from before the reset) is no cheat and no new fall
            grace--;
            paceTicks = 0;
            paceFrom = now;
            stuckFrom = now;
            stuckTicks = 0;
            offTrack = 0;
            if (horizontalDistSqr(now, lastPos) > MAX_STEP * MAX_STEP) {
                lastPos = now;          // (no rings "driven through" by that jump)
            }
        } else if (inRit || wasInRit) {
            // a scripted ride (the looping) moves the guh itself: no speed checks, and start them afresh after it
            paceTicks = 0;
            paceFrom = now;
            stuckFrom = now;
            stuckTicks = 0;
            offTrack = 0;
        } else if (tooFast(now)) {                        // (the rider's game moves the guh: a cheat could make it fly)
            backToCheckpoint(level, player, mount);
            return;
        }
        wasInRit = inRit;
        int gate = track.gateBetween(lastPos, now);
        lastPos = now;
        if (gate < 0) {
            insideGate = -1;
        } else if (gate != insideGate) {
            insideGate = gate;
            throughGate(level, player, mount, gate);
            if (!GAMES.containsValue(this)) {
                return; // that was the finish
            }
        }
        if (missedCooldown > 0) {
            missedCooldown--;
        }
        if (ticks % SAMPLE == 0) {
            Vec3 local = track.toLocal(now);
            samples.add(Math.round((float) local.x * 16));
            samples.add(Math.round((float) local.y * 16));
            samples.add(Math.round((float) local.z * 16));
        }
        moveGhosts(level);
        if (ticks % 20 == 0) {
            shooWildGuhs(level, mount);
        }
        if (baan.extra() != null) {
            baan.extra().tick(this, level, player, mount);
            if (!GAMES.containsValue(this)) {
                return;
            }
        }
        if (inRit) {
            roadY = Math.min(roadY, now.y);  // (the looping comes out lower than it went in, never higher)
            return;
        }
        // fell off the track, into the kaas saus, or stuck somewhere: back to the last checkpoint
        if (ticks % 20 == 0) {
            if (now.distanceToSqr(stuckFrom) > 1.5 * 1.5 || player.getVehicle() != mount) {
                stuckFrom = now;
                stuckTicks = 0;
            } else {
                stuckTicks += 20;
            }
        }
        // no shortcuts over the grass: the race guh has to stay on the racebaan
        boolean onTrack = level.getBlockState(mount.getOnPos()).is(TRACK_BLOCKS) || level.getBlockState(mount.blockPosition()).is(TRACK_BLOCKS);
        if (mount.onGround() && !onTrack) {
            offTrack = graced ? 0 : offTrack + 1;
        } else {
            offTrack = 0;
            if (mount.onGround() && onTrack && !mount.isInFluidType()) {
                roadY = now.y;
                boolean safe = (respawnPending || ticks % 10 == 0) && safeSpot(level, now) != null;
                if (respawnPending && safe) {        // the ring's spot wasn't safe: come back here instead (just past the ring)
                    respawnPending = false;
                    respawn = now;
                    respawnYaw = mount.getYRot();
                }
                if (ticks % 10 == 0 && safe) {
                    onRoad.addLast(new double[]{ticks, now.x, now.y, now.z, mount.getYRot()});
                    while (onRoad.size() > 10) {
                        onRoad.removeFirst();
                    }
                }
            }
        }
        int offLimit = niveau == Niveau.MAKKELIJK ? OFF_TRACK_TICKS_MAKKELIJK : OFF_TRACK_TICKS;
        if (graced) {
            // (nothing sends it back during the grace)
        } else if (player.getVehicle() == mount && offTrack > offLimit) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.race.off_track").withStyle(ChatFormatting.YELLOW));
            backToCheckpoint(level, player, mount);
        } else if (player.getVehicle() == mount && (mount.isInFluidType() || fellOff(now.y) || !track.area.contains(now) || stuckTicks >= STUCK_TICKS)) {
            backToCheckpoint(level, player, mount);
        }
        if (ticks >= MAX_TICKS) {
            end(level, Ending.TOO_LATE);
        }
    }

    private void tickCountdown(ServerLevel level, ServerPlayer player, RaceGuhEntity mount) {
        if (player.getVehicle() != mount) {
            player.startRiding(mount, true); // (no getting off before the start)
        }
        Vec3 start = track.startPos();
        if (horizontalDistSqr(mount.position(), start) > 0.25) {
            mount.setDeltaMovement(Vec3.ZERO);   // (no false start: the guh stays on the line, whatever the rider's game says)
            mount.teleportTo(start.x, start.y, start.z);
            lastPos = stuckFrom = paceFrom = start;
        }
        if (countdown % 20 == 0 && countdown < COUNTDOWN) {
            int n = 3 - countdown / 20;
            title(player, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.race.ready"), 0, 18, 2);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 1f);
        }
        if (++countdown >= COUNTDOWN) {
            racing = true;
            mount.setFrozen(false);
            title(player, Component.translatable("quest.guhs.race.vahoeg").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 0, 20, 10);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 2f);
            level.playSound(null, mount.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1.3f);
            level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.3, mount.getZ(), 20, 0.6, 0.2, 0.6, 0.05);
            hud(player);
        }
    }

    /** The race guh went through ring {@code gate}. */
    void throughGate(ServerLevel level, ServerPlayer player, RaceGuhEntity mount, int gate) {
        if (gate != next) {
            if (gate != lastGate && missedCooldown == 0) {
                missedCooldown = 40;
                player.sendSystemMessage(Component.translatable("quest.guhs.race.missed", gateName(next)).withStyle(ChatFormatting.RED));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1f, 0.6f);
                title(player, Component.empty(), Component.translatable("quest.guhs.race.missed.short", gateName(next)).withStyle(ChatFormatting.RED), 0, 30, 10);
            }
            return;
        }
        lastGate = gate;
        lastGateTicks = ticks;
        // come back here after a fall, but only to a safe spot: on the road (not in the air over a jump, not in the saus)
        Vec3 safe = safeSpot(level, mount.position());
        if (safe != null) {
            respawn = safe;
            respawnYaw = mount.getYRot();
            respawnPending = false;
        } else {
            double[] last = onRoad.peekLast();
            if (last != null) {                // (until the next spot on the road: the last one before the ring)
                respawn = new Vec3(last[1], last[2], last[3]);
                respawnYaw = (float) last[4];
            }
            respawnPending = true;
        }
        splits.add(ticks);
        int index = splits.size() - 1;
        lastDelta = index < pbSplits.length ? ticks - pbSplits[index] : Integer.MIN_VALUE;
        Vec3 c = track.centre(gate);
        level.sendParticles(gate == 0 ? GOLD : PINK, c.x, c.y, c.z, 40, 2, 1.5, 2, 0.02);
        if (gate == 0) {
            int lapTime = ticks - lapStart;
            lapTimes.add(lapTime);
            lapStart = ticks;
            lap++;
            if (lap >= laps()) {
                finish(level, player);
                return;
            }
            next = 1 % track.gates.size();
            boolean last = lap == laps() - 1;
            title(player, Component.translatable(last ? "quest.guhs.race.last_lap" : "quest.guhs.race.lap", lap + 1, laps()).withStyle(ChatFormatting.GOLD),
                    Component.translatable("quest.guhs.race.lap_time", RaceRecords.time(lapTime)).append(deltaText()), 0, 30, 10);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        } else {
            next = (gate + 1) % track.gates.size();
            player.sendOverlayMessage(Component.translatable("quest.guhs.race.checkpoint", gate, track.gates.size() - 1).append(deltaText())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.9f, 1.2f + 0.1f * gate);
        }
        hud(player);
    }

    private Component deltaText() {
        if (lastDelta == Integer.MIN_VALUE) {
            return Component.empty();
        }
        return Component.literal("  " + RaceRecords.delta(lastDelta)).withStyle(lastDelta <= 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component gateName(int gate) {
        return gate == 0 ? Component.translatable("quest.guhs.race.finish_line") : Component.translatable("quest.guhs.race.gate", gate);
    }

    /** A VAHOEG pad (or boost ring) under / around a race guh (server side; the boost itself happens where the guh is steered). */
    static void onPad(RaceGuhEntity mount) {
        ServerLevel level = (ServerLevel) mount.level();
        level.playSound(null, mount.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        level.playSound(null, mount.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.5f);
        level.sendParticles(GOLD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 25, 0.6, 0.1, 0.6, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 8, 0.4, 0.1, 0.4, 0.08);
        if (mount.getFirstPassenger() instanceof ServerPlayer rider) {
            RaceGame game = RACERS.get(rider.getUUID());
            if (game != null) {
                game.pads++;
            }
            rider.sendOverlayMessage(Component.translatable("quest.guhs.race.pad").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /**
     * Faster than a race guh can run? One big jump in a tick, or too far over {@link #PACE_TICKS} (with room for a lagging
     * connection that sends a few steps at once).
     */
    private boolean tooFast(Vec3 now) {
        if (!paceChecks) {
            return false;
        }
        if (horizontalDistSqr(now, lastPos) > MAX_STEP * MAX_STEP) {
            return true;
        }
        if (++paceTicks >= PACE_TICKS) {
            boolean fast = horizontalDistSqr(now, paceFrom) > MAX_PACE * MAX_PACE;
            paceTicks = 0;
            paceFrom = now;
            return fast;
        }
        return false;
    }

    private static double horizontalDistSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /** Fell off: this far below the last height on the road (or the last ring, when that's lower). */
    boolean fellOff(double y) {
        return y < Math.min(roadY, respawn.y) - FALL;
    }

    /**
     * A safe spot to put a race guh back on near {@code at}: the top of a road block at most 4 blocks under it, with room
     * above and no fluid; null when there isn't one (in the air over a jump, in the kaas saus...).
     */
    @Nullable
    static Vec3 safeSpot(ServerLevel level, Vec3 at) {
        net.minecraft.core.BlockPos top = net.minecraft.core.BlockPos.containing(at);
        for (int dy = 0; dy <= 4; dy++) {
            net.minecraft.core.BlockPos p = top.below(dy);
            var state = level.getBlockState(p);
            if (!state.getFluidState().isEmpty()) {
                return null;
            }
            var shape = state.getCollisionShape(level, p);
            if (shape.isEmpty()) {
                continue;                       // (air, a ring's glow, a boost ring: look further down)
            }
            if (!state.is(TRACK_BLOCKS) || NOT_SAFE.contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath())) {
                return null;                    // (off the road, in the kaas saus or on a bouncy mushroom)
            }
            double y = p.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
            net.minecraft.core.BlockPos above = net.minecraft.core.BlockPos.containing(at.x, y + 0.01, at.z);
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                    || !level.getBlockState(above.above()).getCollisionShape(level, above.above()).isEmpty()
                    || !level.getFluidState(above).isEmpty()) {
                return null;
            }
            return new Vec3(at.x, y, at.z);
        }
        return null;
    }

    /** Road blocks that are no place to be put back on. */
    private static final java.util.Set<String> NOT_SAFE = java.util.Set.of("circuit_kaassaus", "circuit_stuiterpaddenstoel");

    /** Back to the last ring (makkelijk: to where the guh was on the road about two seconds ago, if that's after the ring). */
    private void backToCheckpoint(ServerLevel level, ServerPlayer player, RaceGuhEntity mount) {
        resets++;
        Vec3 to = respawn;
        float yaw = respawnYaw;
        if (niveau == Niveau.MAKKELIJK) {
            double[] best = null;
            for (double[] spot : onRoad) {
                if (spot[0] > lastGateTicks && spot[0] <= ticks - TERUG_TICKS) {
                    best = spot;
                }
            }
            if (best != null) {
                to = new Vec3(best[1], best[2], best[3]);
                yaw = (float) best[4];
                onRoad.removeIf(spot -> spot[0] > ticks - TERUG_TICKS);
            }
        }
        paceTicks = 0;
        paceFrom = to;
        stuckTicks = 0;
        offTrack = 0;
        grace = GRACE_TICKS;
        roadY = to.y;
        player.stopRiding();
        mount.setDeltaMovement(Vec3.ZERO);
        mount.resetFallDistance();
        player.resetFallDistance();
        mount.teleportTo(to.x, to.y + 0.1, to.z);
        mount.setYRot(yaw);
        mount.setYHeadRot(yaw);
        player.teleportTo(level, to.x, to.y + 0.1, to.z, yaw, 0);
        player.startRiding(mount, true);
        // the rider's game moves the race guh (like a boat): tell it where its guh is now, or it keeps sending the old spot
        // (down in the gap) and the guh falls "again" at once
        if (player.connection != null) {
            player.connection.send(new ClientboundSetEntityMotionPacket(mount));
            player.connection.send(new ClientboundMoveVehiclePacket(mount));
        }
        lastPos = stuckFrom = mount.position();
        insideGate = lastGate;
        level.sendParticles(ParticleTypes.POOF, to.x, to.y + 0.5, to.z, 20, 0.5, 0.5, 0.5, 0.05);
        Component message = Component.translatable(to == respawn ? "quest.guhs.race.reset" : "quest.guhs.race.reset_makkelijk")
                .withStyle(ChatFormatting.YELLOW);
        if (ticks - lastResetChat >= RESET_MESSAGE_TICKS) {
            lastResetChat = ticks;
            resetChats++;
            player.sendSystemMessage(message);
        } else {
            player.sendOverlayMessage(message);   // (again so soon: just above the hotbar, no chat spam)
        }
        player.playNotifySound(ModSounds.GUH_HURT.get(), SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /**
     * Wild Guhmension guhs wander everywhere (the Guhmension spawner doesn't know the racebaan): the ones standing on the
     * road ahead of a racer poof away, like wild guhs do anyway when nobody is looking. Tame, named or kept guhs stay.
     */
    private void shooWildGuhs(ServerLevel level, RaceGuhEntity mount) {
        for (GuhEntity wild : level.getEntitiesOfClass(GuhEntity.class, mount.getBoundingBox().inflate(24, 6, 24),
                g -> g.getType() == ModEntities.GUH.get() && !g.isTame() && !g.hasCustomName() && !g.isPersistenceRequired()
                        && !g.isVehicle() && !g.isPassenger() && !g.isLeashed() && track.area.contains(g.position())
                        && (level.getBlockState(g.getOnPos()).is(TRACK_BLOCKS) || level.getBlockState(g.blockPosition()).is(TRACK_BLOCKS)))) {
            level.sendParticles(ParticleTypes.POOF, wild.getX(), wild.getY() + 0.5, wild.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
            wild.discard();
        }
    }

    /** The ghosts drive their race: at race time t each is where its racer was at time t back then. */
    private void moveGhosts(ServerLevel level) {
        if (ghost != null && !moveGhost(level, ghost, ghostSamples, ParticleTypes.END_ROD)) {
            ghost = null;
        }
        if (goudGhost != null && !moveGhost(level, goudGhost, goudSamples, GOLD)) {
            goudGhost = null;
        }
    }

    /** Moves one ghost; false when it's gone (its race is over: it poofs). */
    private boolean moveGhost(ServerLevel level, UUID id, int[] recording, net.minecraft.core.particles.ParticleOptions trail) {
        if (!(level.getEntity(id) instanceof RaceGhostEntity g)) {
            return false;
        }
        int count = recording.length / 3;
        float at = (float) ticks / SAMPLE;
        int i = Mth.floor(at);
        if (i >= count - 1) {
            level.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 0.5, g.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
            g.discard();
            return false;
        }
        Vec3 a = sample(recording, i), b = sample(recording, i + 1);
        Vec3 pos = track.toWorld(a.lerp(b, at - i));
        Vec3 along = track.toWorld(b).subtract(track.toWorld(a));
        float yaw = along.horizontalDistanceSqr() > 1e-4 ? (float) (Mth.atan2(-along.x, along.z) * Mth.RAD_TO_DEG) : g.getYRot();
        g.glideTo(pos, yaw);
        if (ticks % 3 == 0) {
            level.sendParticles(trail, pos.x, pos.y + 0.6, pos.z, 1, 0.2, 0.2, 0.2, 0.01);
        }
        return true;
    }

    private static Vec3 sample(int[] recording, int i) {
        return new Vec3(recording[i * 3] / 16.0, recording[i * 3 + 1] / 16.0, recording[i * 3 + 2] / 16.0);
    }

    // --- the end --------------------------------------------------------------------------------------------------------

    /** The coins for a race: the medal's (on lastig 50 % more), +2 for a new personal record. */
    public static int prizes(Medal medal, Niveau niveau, boolean record) {
        return niveau.munten(medal.prizes) + (record ? RECORD_PRIZES : 0);
    }

    private void finish(ServerLevel level, ServerPlayer player) {
        int total = ticks;
        int bestLap = lapTimes.stream().mapToInt(Integer::intValue).min().orElse(total);
        String rec = baan.records(niveau);
        int oldBest = RaceRecords.best(player, rec);
        int oldLap = RaceRecords.bestLap(player, rec);
        boolean hadGhost = ghost != null || ghostSamples.length > 0;
        int[] recording = samples.stream().mapToInt(Integer::intValue).toArray();
        boolean record = RaceRecords.save(player, rec, total, bestLap, splits.stream().mapToInt(Integer::intValue).toArray(), recording);
        Medal medal = Medal.of(total, baan, niveau);
        int prizes = prizes(medal, niveau, record && oldBest >= 0);
        String coinKeys = baan.isRacebaan() ? "quest.guhs.race." : "quest.guhs.circuit.";
        give(player, new ItemStack(baan.coin(), prizes));

        title(player, Component.translatable("quest.guhs.race.finish").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(RaceRecords.time(total)).withStyle(medal.colour).append(record
                        ? Component.translatable("quest.guhs.race.new_record").withStyle(ChatFormatting.YELLOW) : Component.empty()), 5, 60, 20);
        StringBuilder laps = new StringBuilder();
        for (int t : lapTimes) {
            laps.append(laps.length() > 0 ? " / " : "").append(RaceRecords.time(t));
        }
        player.sendSystemMessage(Component.translatable(coinKeys + "result", RaceRecords.time(total), laps.toString(),
                Component.translatable("quest.guhs.race.medal." + medal.id()).withStyle(medal.colour), prizes).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (record) {
            player.sendSystemMessage(Component.translatable(oldBest < 0 ? "quest.guhs.race.first_record" : coinKeys + "record",
                    RaceRecords.time(total), RaceRecords.time(oldBest)).withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.translatable("quest.guhs.race.no_record", RaceRecords.time(oldBest),
                    RaceRecords.delta(total - oldBest)).withStyle(ChatFormatting.GRAY));
        }
        if (oldLap < 0 || bestLap < oldLap) {
            player.sendSystemMessage(Component.translatable("quest.guhs.race.lap_record", RaceRecords.time(bestLap)).withStyle(ChatFormatting.YELLOW));
        }
        // the first race ever (on this track): a welcome bag
        if (RaceRecords.firstFinish(player, baan.isRacebaan() ? "" : "_" + baan.id)) {
            give(player, new ItemStack(baan.coin(), FIRST_PRIZES));
            give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            player.sendSystemMessage(Component.translatable(coinKeys + "first").withStyle(ChatFormatting.GOLD));
        }
        if (baan.isRacebaan()) {
            GuhAdvancements.grant(player, "race_eerste");
            switch (medal) {
                case GOUD -> {
                    GuhAdvancements.grant(player, "race_goud");
                    GuhAdvancements.grant(player, "race_zilver");
                    GuhAdvancements.grant(player, "race_brons");
                }
                case ZILVER -> {
                    GuhAdvancements.grant(player, "race_zilver");
                    GuhAdvancements.grant(player, "race_brons");
                }
                case BRONS -> GuhAdvancements.grant(player, "race_brons");
                default -> {
                }
            }
            if (record && hadGhost && oldBest >= 0) {
                GuhAdvancements.grant(player, "race_geest");
            }
            if (niveau == Niveau.LASTIG) {
                grant(player, "grote_guhspelen/race_lastig");
            }
        }
        // the world's top 3 (race time and fastest lap); first place is the track record (and becomes the golden ghost)
        String boardTotal = baan.boardTotal(niveau);
        int trackRecord = trackRecord(level.getServer(), boardTotal);
        Scorebord.submit(player, boardTotal, total, true);
        Scorebord.submit(player, baan.boardLap(niveau), bestLap, true);
        if (trackRecord < 0 || total < trackRecord) {
            RaceGeesten.offer(player, boardTotal, total, recording);
            Component waar = baan.isRacebaan() ? Component.translatable("quest.guhs.race.track_record", player.getDisplayName(), RaceRecords.time(total))
                    : Component.translatable("quest.guhs.circuit.track_record", player.getDisplayName(), baan.naam(), niveau.naam(), RaceRecords.time(total));
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, track.area.inflate(32))) {
                p.sendSystemMessage(waar.copy().withStyle(ChatFormatting.GOLD));
            }
        }
        if (baan.extra() != null) {
            baan.extra().finish(this, level, player, total, medal, record);
        }
        if (level.getEntity(npcId) instanceof GuhNpcEntity npc && npc.getKind() == GuhNpcEntity.Kind.RACEGUH) {
            RaceRole.showScores(npc);
        }
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        Vec3 c = track.centre(0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z, 80, 2, 1.5, 2, 0.4);
        level.sendParticles(GOLD, c.x, c.y + 1, c.z, 60, 3, 2, 3, 0.05);
        cleanup(level, true);
    }

    /** The track record (fastest race of the world) on a board, or -1. */
    public static int trackRecord(net.minecraft.server.MinecraftServer server, String board) {
        List<Scorebord.Entry> top = Scorebord.top(server, board);
        return top.isEmpty() ? -1 : top.get(0).score();
    }

    /** Grants a shown advancement (guhs:&lt;path&gt;) that has an impossible criterion "done". */
    public static void grant(ServerPlayer player, String path) {
        var holder = player.level().getServer().getAdvancements().get(nl.juiced.guhs.Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    /** Ends the race early. */
    public void end(ServerLevel level, Ending ending) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(racer);
        if (player != null && ending != Ending.STOPPED) {
            player.sendSystemMessage(Component.translatable("quest.guhs.race.ended." + ending.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        cleanup(level, ending != Ending.GONE);
    }

    /** The race guh and the ghosts go away (poof), the racer is no longer protected and (if still here) back at the NPC. */
    private void cleanup(ServerLevel level, boolean backToNpc) {
        if (!GAMES.values().remove(this)) {
            RACERS.remove(racer, this);
            return;
        }
        RACERS.remove(racer);
        ServerLevel home = level.getServer().getLevel(dimension);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(racer);
        if (baan.extra() != null) {
            baan.extra().end(this, home != null ? home : level);
        }
        for (UUID id : new UUID[]{guh, ghost, goudGhost}) {
            Entity e = id == null ? null : (home != null ? home : level).getEntity(id);
            if (e != null) {
                e.ejectPassengers();
                ((ServerLevel) e.level()).sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 25, 0.5, 0.5, 0.5, 0.03);
                e.discard();
            }
        }
        if (player != null) {
            hudOff(player);
            if (backToNpc && home != null && player.level() == home && player.isAlive() && home.getEntity(npcId) instanceof GuhNpcEntity npc) {
                Vec3 look = Vec3.directionFromRotation(0, npc.getYRot());
                player.teleportTo(home, npc.getX() + look.x * 2.5, npc.getY(), npc.getZ() + look.z * 2.5, npc.getYRot() + 180, 0);
            }
        }
    }

    // --- events ------------------------------------------------------------------------------------------------------------

    /** Logging out, dying or changing dimension ends the race (before the player is saved, so the guh is never saved with them). */
    public static void playerGone(ServerPlayer player) {
        RaceGame game = RACERS.get(player.getUUID());
        if (game != null) {
            ServerLevel home = player.level().getServer().getLevel(game.dimension);
            if (player.getVehicle() instanceof RaceGuhEntity) {
                player.stopRiding();
            }
            game.cleanup(home != null ? home : player.level(), false);
        }
    }

    /**
     * Every racer tick: a race whose race guh stopped ticking (the racer was teleported far away, so the race guh and the
     * NPC are both unloaded) is over, so nobody stays unhurtable with a race panel forever.
     */
    public static void checkStale(ServerPlayer player) {
        RaceGame game = RACERS.get(player.getUUID());
        if (game == null) {
            return;
        }
        ServerLevel home = player.level().getServer().getLevel(game.dimension);
        ServerLevel level = home != null ? home : player.level();
        if (level.getGameTime() - game.lastTick > 60) {
            game.end(level, Ending.GONE);
        }
    }

    /** No hurting racers (falling off a bridge, bumping into things...). */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isRacingAny(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** Racers don't get hungrier or weaker than at the start, and never run out of air. */
    static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);    // (no hungrier or weaker than at the start: no free healing)
    }

    // --- feedback ------------------------------------------------------------------------------------------------------

    public static void title(ServerPlayer player, Component title, @Nullable Component subtitle, int in, int stay, int out) {
        if (player.connection == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** The race panel on the racer's screen (see RaceClient): sent when something changes, the timer runs by itself. */
    void hud(ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        String rec = baan.records(niveau);
        data.putBoolean("Active", true);
        data.putBoolean("Racing", racing);
        data.putInt("Ticks", ticks);
        data.putInt("Lap", Math.min(lap + 1, laps()));
        data.putInt("Laps", laps());
        data.putInt("LapStart", lapStart);
        data.putInt("Next", next);
        data.putInt("Gates", track.gates.size());
        data.putInt("Best", RaceRecords.best(player, rec));
        data.putInt("BestLap", RaceRecords.bestLap(player, rec));
        data.putInt("Delta", lastDelta);
        data.putBoolean("Ghost", ghost != null);
        data.putString("Baan", baan.id);
        data.putString("Niveau", niveau.id());
        data.putInt("Goud", goudTicks);
        RacePayloads.send(player, new RacePayloads.Hud(data));
    }

    static void hudOff(ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("Active", false);
        RacePayloads.send(player, new RacePayloads.Hud(data));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }

    /** The server stopped: no race survives that (the race guhs and ghosts are never saved). */
    static void forgetAll() {
        GAMES.clear();
        RACERS.clear();
    }

    // --- for tests -------------------------------------------------------------------------------------------------------

    /** (Tests) the test moves the race guh by teleporting it around: no speed check. */
    public void trustTeleports() {
        paceChecks = false;
    }

    /** Skips the countdown. */
    public void skipCountdown(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(racer);
        while (!racing && player != null && level.getEntity(guh) instanceof RaceGuhEntity mount) {
            tickCountdown(level, player, mount);
        }
    }

    public int pads() {
        return pads;
    }

    /** (Tests) how many reset messages went to the chat. */
    public int resetChats() {
        return resetChats;
    }

    /** (Tests) ticks left of the grace after a reset. */
    public int grace() {
        return grace;
    }

    /** Pretends the race guh last drove {@code ticks} ticks ago. */
    void stallForTest(int ticks) {
        lastTick -= ticks;
    }
}
