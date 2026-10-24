package nl.juiced.guhs.feature.golf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A round of guh golf with the Golfguh: one player at a time (others can watch and wait). She lends a club, puts a guh
 * golf ball on tee 1 and counts down; then it's 9 holes, at most {@link #MAX_STROKES} strokes each. The kaassaus and
 * going off the course cost a penalty stroke (the ball goes back to where you hit it). Every hole gives golfballetjes by
 * the result against par (a hole-in-one the most); a whole round a bonus, and the lowest total is your record (it
 * survives dying, see GuhQuests.saved; it's in chat after every round). The best three rounds of the whole world float
 * above the Golfguh ({@link Scorebord}, board {@link #BOARD}). While playing you can't get hurt or hungry. The club and the ball go away when
 * the round ends, you stop, walk away, log out, die or change dimension.
 * <p>
 * Games live in memory (a restart ends them; the club then disappears by itself). The course markers (tees, cups, the
 * windmill hubs) are found once by scanning around the Golfguh and saved in her roleData.
 * <p>
 * 2.10: every hole has three tees ({@link GolfBlocks#NIVEAU}): makkelijk close to the cup, medium where it always was,
 * lastig further away or behind the obstacles, each with its own par ({@link GolfBanen#PARS}). A lastig round also turns
 * the windmill twice as fast and gets extra pink slime bumpers ({@link GolfBanen#lastigBumpers}, placed at the start and
 * taken away after the round). The wind of 2.9 is gone. A course built before 2.10 only has medium tees: then every level
 * plays from those (with the medium par).
 * <p>
 * 1.7.0 "Rondje Guhmension": a course generated since 1.7.0 has 18 holes (the guh_golfbaan18 template: the old 9 plus the
 * back 9, holes 10-18, each a mini Guhmension). The back 9 open per player once you play the front 9 at or under par
 * ({@link #OPEN}); then the Golfguh asks: front 9, back 9 or all 18 ({@link Ronde}). The back 9 give more golfballetjes
 * ({@link GolfBanen#balletjesAchter}), a round of 18 its own bonus, record and scoreboard, and the first round of 18 at or
 * under par the golf trophy. Its moving obstacles (sliding guh blocks, turning arms, snapping teeth) are turned by the
 * Golfguh like her windmill. Older courses stay 9 holes.
 */
public final class GolfGame {
    /** MAX_STROKES is medium's; see {@link #maxSlagen(Niveau)}. */
    public static final int HOLES = 9, MAX_STROKES = 8;
    /** The welcome present of your very first round. */
    public static final int FIRST_BALLS = 6;
    /** The par of the medium tees (see {@link GolfBanen#PARS} for every level). */
    public static final int[] PAR = GolfBanen.PARS[Niveau.MEDIUM.ordinal()];
    public static final int TOTAL_PAR = Arrays.stream(PAR).sum();
    /** Lang keys of the holes: gui.guhs.golf.hole.&lt;name&gt; (also on the signs of the course). */
    public static final String[] HOLE_NAMES = {"guhpaadje", "vadsbocht", "guhmolen", "kaassausmoeras", "vahoegschans", "guhmond",
            "bumperbal", "kaasknabbelheuvel", "grote_vads",
            "blubberslalom", "kristalschacht", "schuifvadsjes", "guhpiek", "gatenkaasgokje", "kruimeldraaimolen", "kokosflipper",
            "wolkenhop", "grote_guhmond"};
    /** 1.7.0: the most holes a course can have (an 18-hole course). */
    public static final int MAX_HOLES = 18;
    /** Actions from the Golfguh's screen (START plays the front 9; 1.7.0: START_ACHTER the back 9, START_ALLE all 18). */
    public static final int START = 0, SHOP = 1, STOP = 2, START_ACHTER = 3, START_ALLE = 4;

    /** 1.7.0: which holes a round has. */
    public enum Ronde {
        VOOR(0, 9), ACHTER(9, 18), ALLE(0, 18);

        public final int first, end;

        Ronde(int first, int end) {
            this.first = first;
            this.end = end;
        }

        public int holes() {
            return end - first;
        }

        public static Ronde of(int action) {
            return action == START_ACHTER ? ACHTER : action == START_ALLE ? ALLE : VOOR;
        }
    }
    public static final int COUNTDOWN = 61, NEXT_HOLE_DELAY = 50, HAZARD_DELAY = 30, IDLE_LIMIT = 20 * 180, SAIL_TICKS = 20;
    /**
     * 1.2.7 (one golfer per course): while somebody else is waiting for the course (they asked the Golfguh in the last
     * {@link #WAIT_TICKS}), the golfer has to keep going: a minute without anything happening, in whatever phase, ends the
     * round, and so does a round of more than {@link #ROUND_LIMIT}. Alone, the round is as relaxed as before (only
     * {@link #IDLE_LIMIT}, now in every phase too, so a round that hangs always ends).
     */
    public static final int IDLE_LIMIT_WAITING = 20 * 60, ROUND_LIMIT = 20 * 60 * 15, WAIT_TICKS = 20 * 60 * 5;
    /** 2.10: on a lastig round the windmill turns twice as fast. */
    public static final int SAIL_TICKS_LASTIG = 10;
    /** The version of the saved course (2: with the tees of all three levels); an older one is scanned again. */
    private static final int COURSE_VERSION = 3;
    /**
     * How far the course reaches around the Golfguh (she sits in the middle of it), and when you've walked away (a square
     * just a bit bigger than the course: golfers can't get hurt, so they don't get to take that out for a walk).
     */
    public static final int COURSE_RADIUS = 52, COURSE_DOWN = 4, COURSE_UP = 22, LEAVE_RADIUS = 58;
    /** You can hit your ball from this far (horizontally); the slowest and the fastest swing. */
    public static final double REACH = 3.2, MIN_SPEED = 0.06, MAX_SPEED = 1.45;

    public enum Phase { IDLE, COUNTDOWN, AIM, ROLLING, HAZARD, HOLED }

    private static final Map<UUID, GolfGame> GAMES = new ConcurrentHashMap<>();
    /** A round whose Golfguh hasn't ticked for this long is over (see {@link #hasGolfguhNearby}). */
    private static final int STALE_TICKS = 60;
    /** The game time the Golfguh last ticked. */
    private long lastTick;
    /** Everyone playing right now: player -> their Golfguh. */
    private static final Map<UUID, UUID> GOLFERS = new ConcurrentHashMap<>();

    /** The world's top 3 (fewest strokes for a whole round). */
    public static final String BOARD = "golf_rondje";
    /** How often the floating top 3 is checked (in Golfguh ticks). */
    public static final int SCORES_TICKS = 100;

    private static final String BEST = "guhs_golf_best", ROUNDS = "guhs_golf_rounds", ACES = "guhs_golf_aces", FIRST = "guhs_golf_first";
    /** 1.7.0: the back 9 are open for you; you got the golf trophy; your best back 9 and your best 18 (per level). */
    public static final String OPEN = "guhs_golf_achter_open", TROFEE = "guhs_golf_trofee", BEST_ACHTER = "guhs_golf_best_achter",
            BEST18 = "guhs_golf_best18";
    /** 1.7.0: the world's top 3 of the back 9 and of a whole round of 18. */
    public static final String BOARD_ACHTER = "golf_achter", BOARD_18 = "golf_18";

    private final UUID npcId;
    /** The level of the round (2.9): makkelijk 10 strokes and no penalty strokes, medium 8, lastig 6 (2.10: far tees). */
    private Niveau niveau = Niveau.MEDIUM;
    /** 1.7.0: the holes of this round. */
    private Ronde ronde = Ronde.VOOR;
    /** 2.10: the par of every hole of this round (the level's, or medium's where that level has no tee on this course). */
    private final int[] pars = new int[MAX_HOLES];
    @Nullable
    private UUID player;
    private String playerName = "";
    private Phase phase = Phase.IDLE;
    private int timer, hole, strokes;
    private final int[] scores = new int[MAX_HOLES];
    @Nullable
    private UUID ball;
    private Vec3 lastShot = Vec3.ZERO;
    private long lastSwing;
    /** 1.2.7: when the round started, until when somebody is waiting for the course, and the phase the idle clock last saw. */
    private long roundStart, waitingUntil;
    private Phase idlePhase = Phase.IDLE;
    /** A short message is on the action bar: the status bar waits until then. */
    private long quietUntil;

    // the course, found once
    private boolean scanned;
    /** The tees per level (makkelijk, medium, lastig) and hole; only medium ones on a course from before 2.10. */
    private final BlockPos[][] tees = new BlockPos[3][MAX_HOLES];
    private final BlockPos[] cups = new BlockPos[MAX_HOLES];
    private final List<BlockPos> hubs = new ArrayList<>();
    /**
     * 1.7.0, the back 9: 0 = not looked yet, 1 = none (a 9-hole course), 2 = there (scanned). Its moving obstacles: where the
     * sliding guh blocks stand in position A and B, the hubs of the turning arms, the jaws of the snapping teeth; and the
     * whole course (for walking away), null on a 9-hole course.
     */
    private int achter;
    private final List<BlockPos> schuifA = new ArrayList<>(), schuifB = new ArrayList<>(), assen = new ArrayList<>(), tanden = new ArrayList<>();
    @Nullable
    private BlockPos baanMin, baanMax, npcPos;
    private boolean obstakelStand;
    /** 2.10: the extra slime bumpers of a lastig round that are on the course right now (saved: they go after a restart too). */
    private final List<BlockPos> bumpers = new ArrayList<>();
    @Nullable
    private BlockPos home;
    private boolean sailsPlus = true;

    private GolfGame(UUID npcId) {
        this.npcId = npcId;
    }

    public static GolfGame of(GuhNpcEntity npc) {
        GolfGame game = GAMES.computeIfAbsent(npc.getUUID(), GolfGame::new);
        if (!game.scanned) {
            game.load(npc.roleData);
        }
        return game;
    }

    public static boolean isGolfing(Player player) {
        return GOLFERS.containsKey(player.getUUID());
    }

    public boolean isRunning() {
        return player != null;
    }

    public boolean isPlayedBy(Player p) {
        return p.getUUID().equals(player);
    }

    public Phase phase() {
        return phase;
    }

    public int hole() {
        return hole;
    }

    public int strokes() {
        return strokes;
    }

    public Niveau niveau() {
        return niveau;
    }

    public Ronde ronde() {
        return ronde;
    }

    /** 1.7.0: does this course have a whole back 9 (holes 10-18 with all their medium tees and cups)? */
    public boolean heeftAchter() {
        if (achter != 2) {
            return false;
        }
        for (int i = HOLES; i < MAX_HOLES; i++) {
            if (tees[Niveau.MEDIUM.ordinal()][i] == null || cups[i] == null) {
                return false;
            }
        }
        return true;
    }

    /** 1.7.0: are the back 9 open for this player (front 9 at or under par once)? */
    public static boolean achterOpen(Player p) {
        return GuhQuests.saved(p).getBooleanOr(OPEN, false);
    }

    /** The most strokes a hole may take on this level: makkelijk 10, medium 8, lastig 6. */
    public static int maxSlagen(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 10;
            case MEDIUM -> MAX_STROKES;
            case LASTIG -> 6;
        };
    }

    /** Does the kaassaus / off the course cost a stroke on this level? (Not on makkelijk: the ball just goes back.) */
    public static boolean strafslag(Niveau niveau) {
        return niveau != Niveau.MAKKELIJK;
    }

    private int maxSlagen() {
        return maxSlagen(niveau);
    }

    public int score(int hole) {
        return scores[hole];
    }

    /** The tee the current round plays hole {@code hole} from (its level's, or the medium one). */
    @Nullable
    public BlockPos tee(int hole) {
        BlockPos own = tees[niveau.ordinal()][hole];
        return own != null ? own : tees[Niveau.MEDIUM.ordinal()][hole];
    }

    /** The tee of a level (null when this course has none for that level). */
    @Nullable
    public BlockPos tee(Niveau level, int hole) {
        return tees[level.ordinal()][hole];
    }

    /** The par of a hole in this round. */
    public int par(int hole) {
        return pars[hole];
    }

    /** The par of the whole round. */
    public int totalPar() {
        return Arrays.stream(pars, ronde.first, ronde.end).sum();
    }

    /** The extra lastig bumpers on the course right now. */
    public List<BlockPos> bumpers() {
        return bumpers;
    }

    /** The par per hole on a level on this course: the level's where it has its own tee, medium's elsewhere. */
    private int[] parsFor(ServerLevel world, Niveau level) {
        int n = heeftAchter() ? MAX_HOLES : HOLES;
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            BlockPos own = tees[level.ordinal()][i];
            boolean eigen = level == Niveau.MEDIUM || (own != null && world.getBlockState(own).is(GolfFeature.AFSLAG.get()));
            p[i] = GolfBanen.par(eigen ? level : Niveau.MEDIUM, i);
        }
        return p;
    }

    @Nullable
    public BlockPos cup(int hole) {
        return cups[hole];
    }

    public List<BlockPos> hubs() {
        return hubs;
    }

    @Nullable
    public GolfBallEntity ball(ServerLevel world) {
        return ball != null && world.getEntity(ball) instanceof GolfBallEntity b && !b.isRemoved() ? b : null;
    }

    // --- talking to the Golfguh ------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        GolfGame game = of(npc);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.1f);
        String line = game.isPlayedBy(player) ? "playing" : game.isRunning() ? "busy" : "hello" + (1 + npc.getRandom().nextInt(3));
        GuhQuests.say(player, npc, "quest.guhs.golf." + line, game.playerName);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        data.putBoolean("Mine", game.isPlayedBy(player));
        data.putString("Player", game.playerName);
        data.putInt("Hole", game.hole + 1);
        data.putInt("Best", best(player));
        Klassiekers.records(data, n -> best(player, n));
        data.putString("Niveau", game.niveau.id());
        for (Niveau n : Niveau.values()) {                             // (2.10: the par of every level on this course)
            data.putIntArray("Par_" + n.id(), game.scanned ? game.parsFor((ServerLevel) npc.level(), n) : GolfBanen.PARS[n.ordinal()]);
        }
        data.putInt("Rounds", GuhQuests.saved(player).getIntOr(ROUNDS, 0));
        data.putBoolean("Achttien", game.heeftAchter());                // (1.7.0: the back 9, and whether they're open for you)
        data.putBoolean("AchterOpen", achterOpen(player));
        data.putString("Ronde", game.ronde.name());
        for (Niveau n : Niveau.values()) {
            data.putInt("BestAchter_" + n.id(), best(player, BEST_ACHTER, n));
            data.putInt("Best18_" + n.id(), best(player, BEST18, n));
        }
        data.putInt("Aces", GuhQuests.saved(player).getIntOr(ACES, 0));
        List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), BOARD);
        data.putString("RecordName", top.isEmpty() ? "" : top.get(0).name());
        data.putInt("RecordScore", top.isEmpty() ? -1 : top.get(0).score());
        ModNetworking.sendTo(player, new GolfPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.GOLFGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        GolfGame game = of(npc);
        int a = Klassiekers.actie(action);
        if (a == SHOP) {
            npc.openShop(player);
        } else if (a == STOP && game.isPlayedBy(player)) {
            game.end(npc, player, "quest.guhs.golf.stopped", false);
        } else if (a == START || a == START_ACHTER || a == START_ALLE) {
            game.start(npc, player, Klassiekers.niveau(action), Ronde.of(a));
        }
    }

    // --- a round ----------------------------------------------------------------------------------------------------------

    private void start(GuhNpcEntity npc, ServerPlayer p, Niveau level, Ronde route) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.golf.busy", playerName);
            ServerPlayer golfer = player == null ? null : world.getServer().getPlayerList().getPlayer(player);
            if (golfer != null && world.getGameTime() > waitingUntil) {
                golfer.sendSystemMessage(Component.translatable("quest.guhs.golf.wachtende", p.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            waitingUntil = world.getGameTime() + WAIT_TICKS;        // 1.2.7: somebody is waiting: no dawdling now
            return;
        }
        if (isGolfing(p)) {
            GuhQuests.say(p, npc, "quest.guhs.golf.elsewhere");
            return;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(p, npc, nl.juiced.guhs.feature.Minigames.GOLF)) {
            return;
        }
        if (scanned && !intact(world)) {
            scanned = false;                                           // (the course changed: look again)
        }
        if (!scan(npc, true) || !complete() || !intact(world)) {
            GuhQuests.say(p, npc, "quest.guhs.golf.broken");
            return;
        }
        if (route != Ronde.VOOR) {                                      // (1.7.0: the back 9)
            if (achter == 0) {
                scanAchter(world, true);
                save(npc.roleData);
            }
            if (!heeftAchter()) {
                GuhQuests.say(p, npc, "quest.guhs.golf.geen_achter");
                return;
            }
            if (!achterOpen(p)) {
                GuhQuests.say(p, npc, "quest.guhs.golf.achter_dicht");
                return;
            }
        }
        if (p.getInventory().getFreeSlot() < 0) {
            GuhQuests.say(p, npc, "quest.guhs.golf.full");
            return;
        }
        player = p.getUUID();
        playerName = p.getGameProfile().name();
        niveau = level;
        ronde = route;
        int[] levelPars = parsFor(world, level);
        Arrays.fill(pars, 0);
        System.arraycopy(levelPars, 0, pars, 0, levelPars.length);
        GOLFERS.put(player, npcId);
        Arrays.fill(scores, 0);
        hole = route.first;
        strokes = 0;
        lastSwing = world.getGameTime();
        lastTick = world.getGameTime();
        roundStart = world.getGameTime();
        waitingUntil = 0;
        giveClub(p);
        nl.juiced.guhs.feature.Minigames.startKeeping(p);
        p.clearFire();
        removeBumpers(npc, world);                                     // (left over? never twice)
        if (level == Niveau.LASTIG) {
            placeBumpers(npc, world);
        }
        toTee(npc, p);
        phase = Phase.COUNTDOWN;
        timer = COUNTDOWN;
        GuhQuests.say(p, npc, route == Ronde.VOOR ? "quest.guhs.golf.start" : route == Ronde.ACHTER ? "quest.guhs.golf.start_achter"
                : "quest.guhs.golf.start_alle");
        if (level != Niveau.MEDIUM) {
            GuhQuests.say(p, npc, "quest.guhs.klassiekers.golf." + level.id(), maxSlagen());
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.golf.how").withStyle(ChatFormatting.GRAY));
    }

    /** The player and a fresh ball to the tee of the current hole (the tee of this round's level). */
    private void toTee(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos tee = tee(hole);
        if (!world.getBlockState(tee).is(GolfFeature.AFSLAG.get())) {
            tee = tees[Niveau.MEDIUM.ordinal()][hole];                  // (that level's tee is gone: the medium one)
        }
        Direction facing = world.getBlockState(tee).getOptionalValue(GolfBlocks.Afslag.FACING).orElse(Direction.NORTH);
        int back = 2;                                                  // (1.7.0: one step when there's a wall two behind the tee)
        BlockPos stand = tee.relative(facing, -2).above();
        if (!world.isEmptyBlock(stand) || !world.isEmptyBlock(stand.above())) {
            back = 1;
        }
        p.teleportTo(world, tee.getX() + 0.5 - facing.getStepX() * back, tee.getY() + 1, tee.getZ() + 0.5 - facing.getStepZ() * back, java.util.Set.of(), facing.toYRot(), 35f, true);
        GolfBallEntity old = ball(world);
        if (old != null) {
            old.discard();
        }
        lastShot = new Vec3(tee.getX() + 0.5, tee.getY() + 1, tee.getZ() + 0.5);
        newBall(world, lastShot);
        strokes = 0;
        world.sendParticles(ParticleTypes.CHERRY_LEAVES, lastShot.x, lastShot.y + 0.5, lastShot.z, 12, 0.4, 0.3, 0.4, 0.02);
    }

    /** A fresh ball at pos for the current player. */
    private void newBall(ServerLevel world, Vec3 pos) {
        GolfBallEntity b = GolfBallEntity.create(world, pos, player, npcId);
        ball = b.getUUID();
    }

    private void startHole(GuhNpcEntity npc, ServerPlayer p) {
        toTee(npc, p);
        phase = Phase.AIM;
        title(p, Component.translatable("gui.guhs.golf.hole_title", hole + 1).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("gui.guhs.golf.hole_sub", holeName(hole), pars[hole]).withStyle(ChatFormatting.WHITE), 5, 40, 10);
        notifySound(p, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // --- 2.10: the extra slime bumpers of a lastig round -----------------------------------------------------------------

    /** Puts the lastig bumpers on the course (only on free spots right above the course; never on someone). */
    private void placeBumpers(GuhNpcEntity npc, ServerLevel world) {
        for (int i = 0; i < HOLES; i++) {
            BlockPos cup = cups[i], tee = tees[Niveau.MEDIUM.ordinal()][i];
            if (cup == null || tee == null || !world.getBlockState(tee).is(GolfFeature.AFSLAG.get())) {
                continue;
            }
            Direction facing = world.getBlockState(tee).getValue(GolfBlocks.Afslag.FACING);
            for (BlockPos pos : GolfBanen.lastigBumpers(i, cup, facing)) {
                if (world.isLoaded(pos) && world.isEmptyBlock(pos) && GolfBallEntity.isCourse(world.getBlockState(pos.below()))
                        && world.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos),
                        e -> e instanceof GolfBallEntity || e instanceof net.minecraft.world.entity.LivingEntity).isEmpty()) {
                    world.setBlock(pos, nl.juiced.guhs.registry.ModBlocks.ROZE_SLIJMBLOK.get().defaultBlockState(), 3);
                    world.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.05);
                    bumpers.add(pos.immutable());
                }
            }
        }
        if (!bumpers.isEmpty()) {
            world.playSound(null, npc.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.NEUTRAL, 1f, 0.8f);
        }
        saveBumpers(npc);
    }

    /** Takes the lastig bumpers off the course again (only what is still pink slime). */
    private void removeBumpers(GuhNpcEntity npc, ServerLevel world) {
        if (bumpers.isEmpty()) {
            return;
        }
        for (var it = bumpers.iterator(); it.hasNext(); ) {
            BlockPos pos = it.next();
            if (!world.isLoaded(pos)) {
                continue;                                              // (later, when it's there again)
            }
            if (world.getBlockState(pos).is(nl.juiced.guhs.registry.ModBlocks.ROZE_SLIJMBLOK.get())) {
                world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                world.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.05);
            }
            it.remove();
        }
        saveBumpers(npc);
    }

    private void saveBumpers(GuhNpcEntity npc) {
        npc.roleData.put("LastigBumpers", new LongArrayTag(bumpers.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    /** Released the club: hit the ball (if it's lying still and within reach) in the direction you look. */
    public static void swing(ServerPlayer p, float power) {
        UUID npc = GOLFERS.get(p.getUUID());
        GolfGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.doSwing(p, power);
        }
    }

    private void doSwing(ServerPlayer p, float power) {
        ServerLevel world = p.level();
        GolfBallEntity b = ball(world);
        if (phase != Phase.AIM || b == null || b.isMoving()) {
            flash(p, Component.translatable(phase == Phase.COUNTDOWN ? "gui.guhs.golf.wait_countdown" : "gui.guhs.golf.wait_ball")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        Vec3 toBall = b.position().subtract(p.position());
        double reach = toBall.horizontalDistance();
        if (reach > REACH || Math.abs(toBall.y) > 2.5) {
            flash(p, Component.translatable("gui.guhs.golf.too_far").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (dir.lengthSqr() < 1e-4) {
            return;
        }
        dir = dir.normalize();
        strokes++;
        lastShot = b.position();
        lastSwing = world.getGameTime();
        double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * Math.pow(Math.max(0, Math.min(1, power)), 1.5);
        b.hit(dir.scale(speed));
        phase = Phase.ROLLING;
        world.playSound(null, b.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1f, 1.8f);
        world.playSound(null, b.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.4f + power * 0.6f, 1.6f);
        world.sendParticles(ParticleTypes.CRIT, b.getX(), b.getY() + 0.2, b.getZ(), 4 + (int) (power * 10), 0.1, 0.1, 0.1, 0.2);
        if (power > 0.97f) {
            flash(p, Component.translatable("gui.guhs.golf.vahoeg").withStyle(ChatFormatting.GOLD));
        }
    }

    // --- what the ball tells us ------------------------------------------------------------------------------------------

    /** Is this ball still part of a game (or a free test ball)? Old balls roll away into nothing. */
    public static boolean ballAlive(GolfBallEntity b) {
        if (b.getNpc() == null) {
            return true;
        }
        GolfGame game = GAMES.get(b.getNpc());
        return game != null && game.isRunning() && b.getUUID().equals(game.ball);
    }

    public static void ballEvent(GolfBallEntity b, GolfBallEntity.BallEvent event) {
        GolfGame game = b.getNpc() == null ? null : GAMES.get(b.getNpc());
        if (game == null || !b.getUUID().equals(game.ball) || !(b.level() instanceof ServerLevel world)
                || !(world.getEntity(game.npcId) instanceof GuhNpcEntity npc) || game.player == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(game.player);
        if (p != null) {
            game.onBall(npc, p, b, event);
        }
    }

    private void onBall(GuhNpcEntity npc, ServerPlayer p, GolfBallEntity b, GolfBallEntity.BallEvent event) {
        if (phase != Phase.ROLLING) {
            return;
        }
        switch (event) {
            case HOLED -> {
                BlockPos cup = cups[hole];
                if (cup != null && b.blockPosition().distManhattan(cup.above()) <= 1) {
                    holeOver(npc, p, strokes, true);
                } else {
                    penalty(p, "wrong_cup");                           // (over a wall into the cup of another hole: nice try)
                }
            }
            case STOPPED -> {
                if (strokes >= maxSlagen()) {
                    holeOver(npc, p, maxSlagen(), false);
                } else {
                    phase = Phase.AIM;
                    notifySound(p, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
                }
            }
            case SAUS, OUT -> penalty(p, event == GolfBallEntity.BallEvent.SAUS ? "saus" : "out");
            case LIP -> flash(p, Component.translatable("gui.guhs.golf.lip").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Kaassaus, off the course or the wrong cup: a penalty stroke, and in a moment the ball goes back to where you hit it. */
    private void penalty(ServerPlayer p, String what) {
        if (strafslag(niveau)) {
            strokes++;
        }
        phase = Phase.HAZARD;
        timer = HAZARD_DELAY;
        title(p, Component.translatable("gui.guhs.golf." + what).withStyle(ChatFormatting.GOLD),
                Component.translatable(strafslag(niveau) ? "gui.guhs.golf.penalty" : "gui.guhs.klassiekers.golf.geen_strafslag")
                        .withStyle(ChatFormatting.WHITE), 3, 30, 8);
        notifySound(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    /** A hole is done: golfballetjes by the result against par. */
    private void holeOver(GuhNpcEntity npc, ServerPlayer p, int score, boolean holed) {
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "golf", holed); // samen
        ServerLevel world = (ServerLevel) npc.level();
        score = Math.min(score, maxSlagen());
        scores[hole] = score;
        int diff = score - pars[hole];
        String result = !holed ? "max" : score == 1 ? "ace" : diff <= -3 ? "albatros" : diff == -2 ? "eagle" : diff == -1 ? "birdie"
                : diff == 0 ? "par" : diff == 1 ? "bogey" : diff == 2 ? "double_bogey" : "more";
        int balls = hole < HOLES ? balletjesPar(score, pars[hole], holed) : GolfBanen.balletjesAchter(score, pars[hole], holed);
        balls = balls > 0 ? niveau.munten(balls) : 0;
        if (balls > 0) {
            give(p, new ItemStack(GolfFeature.GOLFBALLETJE.get(), balls));
        }
        title(p, Component.translatable("gui.guhs.golf.result." + result).withStyle(diff < 0 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE),
                Component.translatable("gui.guhs.golf.result_sub", score, balls).withStyle(ChatFormatting.WHITE), 3, 40, 10);
        p.sendSystemMessage(Component.translatable("quest.guhs.golf.hole_done", hole + 1, holeName(hole), score, pars[hole],
                Component.translatable("gui.guhs.golf.result." + result), balls).withStyle(ChatFormatting.LIGHT_PURPLE));
        BlockPos cup = cups[hole];
        if (holed && cup != null) {
            world.sendParticles(diff < 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER, cup.getX() + 0.5, cup.getY() + 1.3,
                    cup.getZ() + 0.5, diff < 0 ? 40 : 15, 0.4, 0.5, 0.4, 0.2);
            world.sendParticles(ParticleTypes.FIREWORK, cup.getX() + 0.5, cup.getY() + 1.5, cup.getZ() + 0.5, 20, 0.2, 0.4, 0.2, 0.15);
        }
        SoundEvent sound = score == 1 && holed ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : diff < 0 && holed ? SoundEvents.PLAYER_LEVELUP
                : SoundEvents.NOTE_BLOCK_CHIME.value();
        notifySound(p, sound, SoundSource.PLAYERS, 0.9f, 1.2f);
        npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, diff < 0 ? 1.4f : 1.0f);
        if (score == 1 && holed) {
            GuhQuests.saved(p).putInt(ACES, GuhQuests.saved(p).getIntOr(ACES, 0) + 1);
            GuhAdvancements.grant(p, "golf_hole_in_one");
        }
        phase = Phase.HOLED;
        timer = NEXT_HOLE_DELAY;
    }

    /** Golfballetjes for a hole: hole-in-one 5, eagle (or better) 4, birdie 3, par 2, bogey 1, worse (or not holed) 0. */
    /** The bonus after all 9 holes: 3, plus 4 at par or under, plus 3 for beating your own record. */
    public static int roundBonus(int diff, boolean beatRecord) {
        return 3 + (diff <= 0 ? 4 : 0) + (beatRecord ? 3 : 0);
    }

    public static int balletjes(int score, int hole, boolean holed) {
        return balletjesPar(score, PAR[hole], holed);
    }

    /** The same against any par (2.10: every level has its own). */
    public static int balletjesPar(int score, int par, boolean holed) {
        if (!holed) {
            return 0;
        }
        int diff = score - par;
        return score == 1 ? 6 : diff <= -2 ? 5 : diff == -1 ? 4 : diff == 0 ? 3 : diff == 1 ? 2 : 1;
    }

    private void nextHole(GuhNpcEntity npc, ServerPlayer p) {
        hole++;
        if (hole >= ronde.end) {
            finish(npc, p);
        } else {
            startHole(npc, p);
        }
    }

    /** All holes of the round played: the scorecard, the bonus, records (1.7.0: per route; the back 9 opening, the trophy). */
    private void finish(GuhNpcEntity npc, ServerPlayer p) {
        int total = Arrays.stream(scores).sum(), diff = total - totalPar();
        CompoundTag saved = GuhQuests.saved(p);
        String bestKey = ronde == Ronde.VOOR ? BEST : ronde == Ronde.ACHTER ? BEST_ACHTER : BEST18;
        int best = best(p, bestKey, niveau);
        boolean record = best < 0 || total < best;
        int bonus = niveau.munten(ronde == Ronde.ALLE ? GolfBanen.bonus18(diff, record && best >= 0) : roundBonus(diff, record && best >= 0));
        if (record) {
            saved.putInt(Klassiekers.sleutel(bestKey, niveau), total);
        }
        saved.putInt(ROUNDS, saved.getIntOr(ROUNDS, 0) + 1);
        p.sendSystemMessage(Component.translatable("quest.guhs.golf.card").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int from = ronde.first; from < ronde.end; from += HOLES) {        // (18 holes: two lines of 9)
            p.sendSystemMessage(cardLine("gui.guhs.golf.card_par", pars, null, from, from + HOLES));
            p.sendSystemMessage(cardLine("gui.guhs.golf.card_you", scores, pars, from, from + HOLES));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.golf.total", total, rel(diff), bonus).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (record) {
            p.sendSystemMessage(Component.translatable(best < 0 ? "quest.guhs.golf.first_record" : "quest.guhs.golf.record", total, best)
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.golf.no_record", best).withStyle(ChatFormatting.GRAY));
        }
        give(p, new ItemStack(GolfFeature.GOLFBALLETJE.get(), bonus));
        if (!saved.getBooleanOr(FIRST, false)) {                                  // the very first round: a welcome present
            saved.putBoolean(FIRST, true);
            give(p, new ItemStack(GolfFeature.GOLFBALLETJE.get(), FIRST_BALLS));
            give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            give(p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 4));
            p.sendSystemMessage(Component.translatable("quest.guhs.golf.first").withStyle(ChatFormatting.GOLD));
        }
        GuhAdvancements.grant(p, "golf_ronde");
        if (ronde == Ronde.VOOR && diff <= 0 && niveau != Niveau.MAKKELIJK) {
            GuhAdvancements.grant(p, "golf_par");
        }
        if (ronde == Ronde.VOOR && diff <= 0 && niveau == Niveau.LASTIG) {
            GuhAdvancements.grant(p, "golf_lastig_par");                // (from the far tees, past all the bumpers)
        }
        int frontTotal = 0, frontPar = 0;                                // (1.7.0: the front 9 at or under par opens the back 9)
        for (int i = 0; i < HOLES; i++) {
            frontTotal += scores[i];
            frontPar += pars[i];
        }
        if (ronde != Ronde.ACHTER && frontTotal <= frontPar && !saved.getBooleanOr(OPEN, false)) {
            saved.putBoolean(OPEN, true);
            p.sendSystemMessage(Component.translatable(heeftAchter() ? "quest.guhs.golf.achter_open" : "quest.guhs.golf.achter_open_elders")
                    .withStyle(ChatFormatting.GOLD));
            GuhAdvancements.grant(p, "golf_achter_open");
        }
        if (ronde == Ronde.ALLE) {
            GuhAdvancements.grant(p, "golf_18");
            if (diff <= 0 && !saved.getBooleanOr(TROFEE, false)) {
                saved.putBoolean(TROFEE, true);
                give(p, trofee(p.getGameProfile().name(), total, totalPar(), niveau));
                p.sendSystemMessage(Component.translatable("quest.guhs.golf.trofee").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                GuhAdvancements.grant(p, "golf_trofee");
            }
        }
        String board = ronde == Ronde.VOOR ? BOARD : ronde == Ronde.ACHTER ? BOARD_ACHTER : BOARD_18;
        if (Scorebord.submit(p, niveau.board(board), total, true) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.golf.club_record").withStyle(ChatFormatting.GOLD));
        }
        Klassiekers.gespeeld(p, "golf", niveau);
        showScores(npc);
        title(p, Component.translatable("gui.guhs.golf.done_title").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.golf.done_sub", total, rel(diff)).withStyle(ChatFormatting.WHITE), 5, 60, 20);
        notifySound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        end(npc, p, null, true);
    }

    /** 1.7.0: the golf trophy, with the winner's name and the record on it. */
    public static ItemStack trofee(String name, int total, int par, Niveau level) {
        ItemStack stack = new ItemStack(Golf18.GOLFTROFEE_ITEM.get());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.translatable("item.guhs.golftrofee.van", name).withStyle(ChatFormatting.GOLD));
        stack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("item.guhs.golftrofee.record", total, par, rel(total - par)),
                Component.translatable("item.guhs.golftrofee.niveau", Klassiekers.naam(level)))));
        return stack;
    }

    private MutableComponent cardLine(String label, int[] values, @Nullable int[] par, int from, int to) {
        MutableComponent line = Component.translatable(label).withStyle(ChatFormatting.GRAY);
        int total = 0;
        for (int i = from; i < to; i++) {
            total += values[i];
            ChatFormatting colour = par == null ? ChatFormatting.WHITE : values[i] == 1 ? ChatFormatting.GOLD
                    : values[i] < par[i] ? ChatFormatting.GREEN : values[i] == par[i] ? ChatFormatting.WHITE : ChatFormatting.RED;
            line.append(Component.literal(i == from ? " " : " | ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(Component.literal(String.valueOf(values[i])).withStyle(colour));
        }
        return line.append(Component.literal("  = " + total).withStyle(ChatFormatting.YELLOW));
    }

    /** Ends the game: the club goes back, the ball goes away. After a whole round you're back at the Golfguh. */
    private void end(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String message, boolean backHome) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null) {
            removeClubs(p);
            if (message != null) {
                p.sendSystemMessage(Component.translatable(message).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (backHome && home != null && p.level() == world && p.isAlive()) {
                p.teleportTo(world, home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), p.getYRot(), 0, true);
            }
        }
        GolfBallEntity b = ball(world);
        if (b != null) {
            b.discard();
        }
        if (player != null) {
            GOLFERS.remove(player, npcId);
        }
        player = null;
        ball = null;
        phase = Phase.IDLE;
        removeBumpers(npc, world);
    }

    /** (Tests) the clocks of the round: so many ticks ago was the last swing / the start, and is somebody waiting? */
    public void testClocks(ServerLevel world, int idleTicks, int roundTicks, boolean waiting) {
        lastSwing = world.getGameTime() - idleTicks;
        roundStart = world.getGameTime() - roundTicks;
        waitingUntil = waiting ? world.getGameTime() + WAIT_TICKS : 0;
        idlePhase = phase;
    }

    /** A golfer logged out, died or went to another dimension: the round is over (the ball goes by itself). */
    public static void leave(ServerPlayer p) {
        UUID npc = GOLFERS.remove(p.getUUID());
        removeClubs(p);
        GolfGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.player = null;
            game.ball = null;
            game.phase = Phase.IDLE;
        }
    }

    /** Is this golfer's Golfguh still there (loaded, in the same world)? */
    /** Is the golfer's Golfguh still there, and still ticking (her chunk may be loaded but asleep)? */
    public static boolean hasGolfguhNearby(ServerPlayer p) {
        UUID npc = GOLFERS.get(p.getUUID());
        GolfGame game = npc == null ? null : GAMES.get(npc);
        return game != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - game.lastTick <= STALE_TICKS;
    }

    /** Every tick of the Golfguh: the windmill, the countdown, the timers, and golfers who wandered off. */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        if (world.getGameTime() == lastTick) {
            return;                                                    // (1.7.0: ticked already, by her or by the golfer)
        }
        lastTick = world.getGameTime();
        npcPos = npc.blockPosition();
        long clock = lastTick;                                         // (the game clock: the golfer may tick her from afar)
        if (clock % SCORES_TICKS == 1) {
            showScores(npc);
        }
        if (clock % (isRunning() && niveau == Niveau.LASTIG ? SAIL_TICKS_LASTIG : SAIL_TICKS) == 0) {
            if (!scanned && world.getNearestPlayer(npc, 40) != null) {
                scan(npc);
            }
            if (scanned && achter == 0 && world.getNearestPlayer(npc, 40) != null) {
                scanAchter(world, false);                              // (1.7.0: a back 9? once its chunks are there)
                if (achter != 0) {
                    save(npc.roleData);
                }
            }
            if (scanned && !hubs.isEmpty() && world.getNearestPlayer(npc, 72) != null) {
                turnSails(world);
            }
            if (achter == 2 && (isRunning() || world.getNearestPlayer(npc, 170) != null)) {
                moveObstacles(world);
            }
        }
        if (player == null) {
            if (!bumpers.isEmpty() && clock % 20 == 0) {
                removeBumpers(npc, world);                             // (the round ended while she wasn't looking)
            }
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || walkedAway(p, npc)) {
            if (p != null && p.isAlive() && p.level() == world) {
                end(npc, p, "quest.guhs.golf.walked_away", false);
            } else {
                if (p != null) {
                    removeClubs(p);
                }
                end(npc, null, null, false);
            }
            return;
        }
        long now = world.getGameTime();
        if (phase != idlePhase) {                                      // (something happened: the ball came to rest, the next hole...)
            idlePhase = phase;
            lastSwing = Math.max(lastSwing, now);
        }
        boolean waiting = now <= waitingUntil;
        if (now - lastSwing > (waiting ? IDLE_LIMIT_WAITING : IDLE_LIMIT)) {
            end(npc, p, "quest.guhs.golf.idle", false);
            return;
        }
        if (waiting && now - roundStart > ROUND_LIMIT) {
            end(npc, p, "quest.guhs.golf.te_lang", false);
            return;
        }
        if (clock % 20 == 0) {
            refresh(p);
            if (!p.getInventory().hasAnyMatching(s -> s.is(GolfFeature.GOLFCLUB.get())) && p.getInventory().getFreeSlot() >= 0) {
                giveClub(p);                                            // (lost it somehow? here's another one)
            }
        }
        switch (phase) {
            case COUNTDOWN -> {
                timer--;
                if (timer == 60 || timer == 40 || timer == 20) {
                    title(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.golf.hole_sub", holeName(ronde.first), pars[ronde.first]), 0, 22, 0);
                    notifySound(p, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.9f, 1.0f);
                }
                if (timer <= 0) {
                    phase = Phase.AIM;
                    title(p, Component.translatable("gui.guhs.golf.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.golf.go_sub"), 0, 30, 10);
                    notifySound(p, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 2.0f);
                    npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.2f);
                }
            }
            case HAZARD -> {
                if (--timer <= 0) {
                    if (strokes >= maxSlagen()) {
                        holeOver(npc, p, maxSlagen(), false);
                    } else {
                        GolfBallEntity b = ball(world);
                        if (b != null) {
                            b.resetTo(lastShot);
                        }
                        phase = Phase.AIM;
                        world.sendParticles(ParticleTypes.CHERRY_LEAVES, lastShot.x, lastShot.y + 0.4, lastShot.z, 8, 0.3, 0.2, 0.3, 0.02);
                    }
                }
            }
            case HOLED -> {
                if (--timer <= 0) {
                    nextHole(npc, p);
                }
            }
            default -> {
            }
        }
        if (isRunning() && (phase == Phase.AIM || phase == Phase.ROLLING) && ball(world) == null && world.isLoaded(BlockPos.containing(lastShot))) {
            newBall(world, lastShot);                                  // (the ball got lost: a new one where you hit it)
            phase = Phase.AIM;
        }
        if (isRunning() && clock % 10 == 0 && phase != Phase.COUNTDOWN && world.getGameTime() >= quietUntil) {
            int done = 0, par = 0;
            for (int i = ronde.first; i < hole; i++) {
                done += scores[i];
                par += pars[i];
            }
            GolfBallEntity b = ball(world);
            int left = b == null || cups[hole] == null ? 0 : (int) Math.round(Math.hypot(b.getX() - cups[hole].getX() - 0.5, b.getZ() - cups[hole].getZ() - 0.5));
            Component bar = Component.translatable("gui.guhs.golf.bar_n", hole + 1, ronde.end, holeName(hole), pars[hole], strokes, maxSlagen(),
                    left, rel(done - par)).withStyle(ChatFormatting.LIGHT_PURPLE);
            p.sendOverlayMessage(bar);
        }
    }

    // --- the windmill ----------------------------------------------------------------------------------------------------

    /** The sails of every windmill: a "+" (the lower sail closes the tunnel) and an "x" (the tunnel is open), in turn. */
    private void turnSails(ServerLevel world) {
        sailsPlus = !sailsPlus;
        for (BlockPos hub : hubs) {
            BlockState state = world.getBlockState(hub);
            if (!state.is(GolfFeature.MOLENAS.get()) || !world.isLoaded(hub)) {
                continue;
            }
            Direction face = state.getValue(GolfBlocks.Molenas.FACING);
            List<BlockPos> on = sails(hub, face, sailsPlus), off = sails(hub, face, !sailsPlus);
            for (BlockPos pos : off) {
                if (world.getBlockState(pos).is(GolfFeature.WIEK.get())) {
                    world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
            for (BlockPos pos : on) {
                if (world.isEmptyBlock(pos) && world.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos),
                        e -> e instanceof GolfBallEntity || e instanceof net.minecraft.world.entity.LivingEntity).isEmpty()) {
                    world.setBlock(pos, GolfFeature.WIEK.get().defaultBlockState(), 2);
                }
            }
        }
    }

    // --- 1.7.0: the moving obstacles of the back 9 ---------------------------------------------------------------------------

    /**
     * One step of every moving obstacle of the back 9: the sliding guh blocks go to their other position, the arms of the
     * Kruimeldraaimolen turn an eighth (a "+" and an "x" in turn), the teeth of the Grote Guhmond snap shut or open. A block
     * never appears on the ball or on somebody (that spot waits for the next step).
     */
    void moveObstacles(ServerLevel world) {
        obstakelStand = !obstakelStand;
        BlockState guh = Golf18.SCHUIFGUH.get().defaultBlockState();
        swap(world, obstakelStand ? schuifB : schuifA, obstakelStand ? schuifA : schuifB, guh);
        BlockState wiek = GolfFeature.WIEK.get().defaultBlockState();
        for (BlockPos as : assen) {
            if (world.isLoaded(as) && world.getBlockState(as).is(Golf18.DRAAIAS.get())) {
                swap(world, arms(as, !obstakelStand), arms(as, obstakelStand), wiek);
            }
        }
        BlockState tand = nl.juiced.guhs.registry.ModBlocks.TAND.get().defaultBlockState();
        List<BlockPos> dicht = new ArrayList<>();
        for (BlockPos kaak : tanden) {
            if (world.isLoaded(kaak) && world.getBlockState(kaak).is(Golf18.HAPTAND.get())) {
                dicht.add(kaak.below(2));
                dicht.add(kaak.below(3));
            }
        }
        if (obstakelStand) {
            swap(world, dicht, List.of(), tand);                       // hap!
            if (!dicht.isEmpty()) {
                world.playSound(null, dicht.get(0), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.BLOCKS, 0.5f, 1.6f);
            }
        } else {
            swap(world, List.of(), dicht, tand);                       // and open wide
        }
    }

    /** Takes {@code block} away from {@code off} (only where it is that block) and puts it on {@code on} (only on free spots). */
    private static void swap(ServerLevel world, List<BlockPos> off, List<BlockPos> on, BlockState block) {
        for (BlockPos pos : off) {
            if (world.isLoaded(pos) && world.getBlockState(pos).is(block.getBlock())) {
                world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        for (BlockPos pos : on) {
            if (world.isLoaded(pos) && world.isEmptyBlock(pos) && world.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos),
                    e -> e instanceof GolfBallEntity || e instanceof net.minecraft.world.entity.LivingEntity).isEmpty()) {
                world.setBlock(pos, block, 2);
            }
        }
    }

    /** The arms of a turning hub (one block below it): a "+" of 4 x 6 blocks, or an "x" of 4 x 4. */
    public static List<BlockPos> arms(BlockPos hub, boolean plus) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos c = hub.below();
        for (int k = 1; k <= (plus ? 6 : 4); k++) {
            if (plus) {
                list.add(c.offset(k, 0, 0));
                list.add(c.offset(-k, 0, 0));
                list.add(c.offset(0, 0, k));
                list.add(c.offset(0, 0, -k));
            } else {
                list.add(c.offset(k, 0, k));
                list.add(c.offset(-k, 0, k));
                list.add(c.offset(k, 0, -k));
                list.add(c.offset(-k, 0, -k));
            }
        }
        return list;
    }

    public static List<BlockPos> sails(BlockPos hub, Direction face, boolean plus) {
        Direction side = face.getClockWise();
        List<BlockPos> list = new ArrayList<>();
        for (int k = 1; k <= 3; k++) {
            if (plus) {
                list.add(hub.above(k));
                list.add(hub.below(k));
                list.add(hub.relative(side, k));
                list.add(hub.relative(side, -k));
            } else {
                list.add(hub.above(k).relative(side, k));
                list.add(hub.above(k).relative(side, -k));
                list.add(hub.below(k).relative(side, k));
                list.add(hub.below(k).relative(side, -k));
            }
        }
        return list;
    }

    // --- the course markers ------------------------------------------------------------------------------------------------

    /** Finds the tees, cups, windmills and a spot next to the Golfguh (once; when the area is loaded). */
    public boolean scan(GuhNpcEntity npc) {
        return scan(npc, false);
    }

    /** As {@link #scan(GuhNpcEntity)}; with force (someone wants to play now) it loads the chunks it still needs. */
    public boolean scan(GuhNpcEntity npc, boolean force) {
        if (scanned) {
            return true;
        }
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos c = npc.blockPosition();
        BlockPos min = c.offset(-COURSE_RADIUS, -COURSE_DOWN, -COURSE_RADIUS), max = c.offset(COURSE_RADIUS, COURSE_UP, COURSE_RADIUS);
        if (!force && !world.hasChunksAt(min, max)) {
            return false;
        }
        for (BlockPos[] t : tees) {
            Arrays.fill(t, null);
        }
        Arrays.fill(cups, null);
        hubs.clear();
        achter = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = world.getBlockState(pos);
            if (state.is(GolfFeature.AFSLAG.get()) && state.getValue(GolfBlocks.HOLE) <= HOLES) {
                tees[state.getValue(GolfBlocks.NIVEAU).ordinal()][state.getValue(GolfBlocks.HOLE) - 1] = pos.immutable();
            } else if (state.is(GolfFeature.HOLE.get()) && state.getValue(GolfBlocks.HOLE) <= HOLES) {
                cups[state.getValue(GolfBlocks.HOLE) - 1] = pos.immutable();
            } else if (state.is(GolfFeature.MOLENAS.get())) {
                hubs.add(pos.immutable());
            }
        }
        home = null;
        for (int[] d : new int[][]{{0, 2}, {2, 0}, {0, -2}, {-2, 0}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {0, 3}, {3, 0}, {0, -3}, {-3, 0}}) {
            BlockPos pos = c.offset(d[0], 0, d[1]);
            var shape = world.getBlockState(pos).getCollisionShape(world, pos);   // (a carpet is fine)
            if ((shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.25) && world.isEmptyBlock(pos.above())
                    && world.getBlockState(pos.below()).isSolid()) {
                home = pos;
                break;
            }
        }
        if (home == null) {
            home = c;
        }
        scanned = true;
        if (complete()) {
            scanAchter(world, force);
        }
        save(npc.roleData);
        return true;
    }

    /**
     * 1.7.0: is there a back 9, and where are its tees, cups and moving obstacles? An 18-hole course is the guh_golfbaan18
     * template, so the medium tee of hole 10 is at a known spot from the cup and tee of hole 1 ({@link GolfBanen#inWereld18}):
     * no tee there, a 9-hole course. Without force it waits until those chunks are loaded.
     */
    void scanAchter(ServerLevel world, boolean force) {
        BlockPos cup1 = cups[0], tee1 = tees[Niveau.MEDIUM.ordinal()][0];
        if (cup1 == null || tee1 == null || !world.getBlockState(tee1).is(GolfFeature.AFSLAG.get())) {
            return;
        }
        Direction facing = world.getBlockState(tee1).getValue(GolfBlocks.Afslag.FACING);
        BlockPos tee10 = GolfBanen.inWereld18(GolfBanen.TEE10_18, cup1, facing);
        if (!force && !world.isLoaded(tee10)) {
            return;
        }
        for (int i = HOLES; i < MAX_HOLES; i++) {
            for (BlockPos[] t : tees) {
                t[i] = null;
            }
            cups[i] = null;
        }
        schuifA.clear();
        schuifB.clear();
        assen.clear();
        tanden.clear();
        baanMin = baanMax = null;
        BlockState t10 = world.getBlockState(tee10);
        if (!t10.is(GolfFeature.AFSLAG.get()) || t10.getValue(GolfBlocks.HOLE) != 10) {
            achter = 1;                                                // (a 9-hole course)
            return;
        }
        BlockPos[] box = GolfBanen.doos18(GolfBanen.ACHTER_18, cup1, facing);
        if (!force && !world.hasChunksAt(box[0], box[1])) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(box[0], box[1])) {
            BlockState state = world.getBlockState(pos);
            if (state.is(GolfFeature.AFSLAG.get()) && state.getValue(GolfBlocks.HOLE) > HOLES) {
                tees[state.getValue(GolfBlocks.NIVEAU).ordinal()][state.getValue(GolfBlocks.HOLE) - 1] = pos.immutable();
            } else if (state.is(GolfFeature.HOLE.get()) && state.getValue(GolfBlocks.HOLE) > HOLES) {
                cups[state.getValue(GolfBlocks.HOLE) - 1] = pos.immutable();
            } else if (state.is(Golf18.SCHUIFRAIL.get()) && state.getValue(Golf18Blocks.STAND) != Golf18Blocks.Stand.GEEN) {
                (state.getValue(Golf18Blocks.STAND) == Golf18Blocks.Stand.A ? schuifA : schuifB).add(pos.above());
            } else if (state.is(Golf18.DRAAIAS.get())) {
                assen.add(pos.immutable());
            } else if (state.is(Golf18.HAPTAND.get())) {
                tanden.add(pos.immutable());
            }
        }
        BlockPos[] all = GolfBanen.doos18(GolfBanen.BAAN_18, cup1, facing);
        baanMin = all[0];
        baanMax = all[1];
        achter = 2;
    }

    /** Has the golfer left the course? (A 9-hole course: a square round the Golfguh; 1.7.0, 18 holes: the whole course.) */
    private boolean walkedAway(ServerPlayer p, GuhNpcEntity npc) {
        if (achter == 2 && baanMin != null && baanMax != null) {
            int m = LEAVE_RADIUS - COURSE_RADIUS;
            return p.getX() < baanMin.getX() - m || p.getX() > baanMax.getX() + 1 + m || p.getZ() < baanMin.getZ() - m
                    || p.getZ() > baanMax.getZ() + 1 + m;
        }
        return horizontalDistance(p, npc) > LEAVE_RADIUS;
    }

    /**
     * 1.7.0, every tick of a golfer: keeps the Golfguh's chunk going while they play an 18-hole course (the back 9 are far
     * from her), and if she isn't ticking by herself (her chunk asleep) the golfer ticks her game.
     */
    public static void golferTick(ServerPlayer p) {
        UUID npc = GOLFERS.get(p.getUUID());
        GolfGame game = npc == null ? null : GAMES.get(npc);
        if (game == null || game.npcPos == null || !game.isPlayedBy(p)) {
            return;
        }
        ServerLevel world = p.level();
        if (game.achter == 2 && p.tickCount % 20 == 0) {
            world.getChunkSource().addTicketWithRadius(Golf18.TICKET.get(), new net.minecraft.world.level.ChunkPos(game.npcPos.getX() >> 4, game.npcPos.getZ() >> 4), 2);
        }
        if (world.getGameTime() - game.lastTick >= 3 && world.getEntity(npc) instanceof GuhNpcEntity g) {
            game.tick(g);
        }
    }

    /** All 9 medium tees and cups are there (the makkelijk and lastig tees are a bonus: without them, medium's are used). */
    public boolean complete() {
        return Arrays.stream(tees[Niveau.MEDIUM.ordinal()], 0, HOLES).allMatch(java.util.Objects::nonNull)
                && Arrays.stream(cups, 0, HOLES).allMatch(java.util.Objects::nonNull);
    }

    /** Are the tees and cups found earlier still there? (Someone in creative mode may have rebuilt the course.) */
    private boolean intact(ServerLevel world) {
        BlockPos[] medium = tees[Niveau.MEDIUM.ordinal()];
        for (int i = 0; i < HOLES; i++) {
            if (medium[i] == null || cups[i] == null || !world.getBlockState(medium[i]).is(GolfFeature.AFSLAG.get())
                    || !world.getBlockState(cups[i]).is(GolfFeature.HOLE.get())) {
                return false;
            }
        }
        return true;
    }

    private void save(CompoundTag data) {
        if (!complete()) {
            return;                                                    // (look again next time)
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Versie", COURSE_VERSION);
        tag.put("Tees", new LongArrayTag(Arrays.stream(tees[Niveau.MEDIUM.ordinal()], 0, HOLES).mapToLong(BlockPos::asLong).toArray()));
        for (Niveau n : new Niveau[]{Niveau.MAKKELIJK, Niveau.LASTIG}) {
            tag.put("Tees_" + n.id(), new LongArrayTag(Arrays.stream(tees[n.ordinal()], 0, HOLES).mapToLong(t -> t == null ? Long.MIN_VALUE : t.asLong()).toArray()));
        }
        tag.put("Cups", new LongArrayTag(Arrays.stream(cups, 0, HOLES).mapToLong(BlockPos::asLong).toArray()));
        tag.put("Hubs", new LongArrayTag(hubs.stream().mapToLong(BlockPos::asLong).toArray()));
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
        if (achter != 0) {                                             // (1.7.0: the back 9, or "none")
            CompoundTag a = new CompoundTag();
            a.putInt("Status", achter);
            if (achter == 2) {
                for (Niveau n : Niveau.values()) {
                    a.put("Tees_" + n.id(), new LongArrayTag(Arrays.stream(tees[n.ordinal()], HOLES, MAX_HOLES)
                            .mapToLong(t -> t == null ? Long.MIN_VALUE : t.asLong()).toArray()));
                }
                a.put("Cups", new LongArrayTag(Arrays.stream(cups, HOLES, MAX_HOLES).mapToLong(t -> t == null ? Long.MIN_VALUE : t.asLong()).toArray()));
                a.put("SchuifA", longs(schuifA));
                a.put("SchuifB", longs(schuifB));
                a.put("Assen", longs(assen));
                a.put("Tanden", longs(tanden));
                if (baanMin != null && baanMax != null) {
                    a.putLong("Min", baanMin.asLong());
                    a.putLong("Max", baanMax.asLong());
                }
            }
            tag.put("Achter", a);
        }
        data.put("Course", tag);
    }

    private static LongArrayTag longs(List<BlockPos> list) {
        return new LongArrayTag(list.stream().mapToLong(BlockPos::asLong).toArray());
    }

    private static void unlongs(CompoundTag tag, String key, List<BlockPos> into) {
        into.clear();
        for (long l : tag.getLongArray(key).orElse(new long[0])) {
            into.add(BlockPos.of(l));
        }
    }

    private void loadAchter(CompoundTag a) {
        achter = a.getIntOr("Status", 0);
        if (achter != 2) {
            return;
        }
        for (Niveau n : Niveau.values()) {
            long[] l = a.getLongArray("Tees_" + n.id()).orElse(new long[0]);
            for (int i = 0; i < HOLES; i++) {
                tees[n.ordinal()][HOLES + i] = i < l.length && l[i] != Long.MIN_VALUE ? BlockPos.of(l[i]) : null;
            }
        }
        long[] c = a.getLongArray("Cups").orElse(new long[0]);
        for (int i = 0; i < HOLES; i++) {
            cups[HOLES + i] = i < c.length && c[i] != Long.MIN_VALUE ? BlockPos.of(c[i]) : null;
        }
        unlongs(a, "SchuifA", schuifA);
        unlongs(a, "SchuifB", schuifB);
        unlongs(a, "Assen", assen);
        unlongs(a, "Tanden", tanden);
        baanMin = a.contains("Min") ? BlockPos.of(a.getLongOr("Min", 0L)) : null;
        baanMax = a.contains("Max") ? BlockPos.of(a.getLongOr("Max", 0L)) : null;
    }

    private void load(CompoundTag data) {
        bumpers.clear();
        for (long l : data.getLongArray("LastigBumpers").orElse(new long[0])) {
            bumpers.add(BlockPos.of(l));                               // (still on the course from a round before a restart)
        }
        CompoundTag tag = data.getCompoundOrEmpty("Course");
        long[] t = tag.getLongArray("Tees").orElse(new long[0]), c = tag.getLongArray("Cups").orElse(new long[0]);
        if (t.length != HOLES || c.length != HOLES || tag.getIntOr("Versie", 0) < COURSE_VERSION) {
            return;                                                    // (saved before 2.10: look again, for the new tees)
        }
        for (int i = 0; i < HOLES; i++) {
            tees[Niveau.MEDIUM.ordinal()][i] = BlockPos.of(t[i]);
            cups[i] = BlockPos.of(c[i]);
        }
        for (Niveau n : new Niveau[]{Niveau.MAKKELIJK, Niveau.LASTIG}) {
            long[] l = tag.getLongArray("Tees_" + n.id()).orElse(new long[0]);
            for (int i = 0; i < HOLES; i++) {
                tees[n.ordinal()][i] = i < l.length && l[i] != Long.MIN_VALUE ? BlockPos.of(l[i]) : null;
            }
        }
        hubs.clear();
        for (long l : tag.getLongArray("Hubs").orElse(new long[0])) {
            hubs.add(BlockPos.of(l));
        }
        home = tag.contains("Home") ? BlockPos.of(tag.getLongOr("Home", 0L)) : null;
        loadAchter(tag.getCompoundOrEmpty("Achter"));
        scanned = true;
    }

    /** The world's top 3 floats above the Golfguh, in her clubhouse. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Scorebord.show(world, npc.position().add(0, 2.4, 0), "golf", Klassiekers.bord(world.getServer(),
                Component.translatable("gui.guhs.scorebord.golf"), BOARD,
                n -> Component.translatable("gui.guhs.golf.bord_par", maxSlagen(Niveau.of(n)), GolfBanen.totalPar(Niveau.of(n))),
                total -> String.valueOf(total)));
        GolfGame game = GAMES.get(npc.getUUID());
        if (game != null && game.heeftAchter()) {                     // (1.7.0: and the top 3 of 18 holes, a bit to the side)
            Scorebord.show(world, npc.position().add(2.5, 2.4, 0), "golf18", Klassiekers.bord(world.getServer(),
                    Component.translatable("gui.guhs.scorebord.golf18"), BOARD_18,
                    n -> Component.translatable("gui.guhs.golf.bord_par", maxSlagen(Niveau.of(n)), totalPar18(Niveau.of(n))),
                    total -> String.valueOf(total)));
        }
    }

    /** 1.7.0: the par of all 18 holes on a level (43 / 65 / 83). */
    public static int totalPar18(Niveau niveau) {
        int total = 0;
        for (int i = 0; i < MAX_HOLES; i++) {
            total += GolfBanen.par(niveau, i);
        }
        return total;
    }

    // --- helpers -----------------------------------------------------------------------------------------------------------

    /** Your best round on medium (the old record), -1 when you never played a whole round. */
    public static int best(Player p) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(BEST) ? saved.getIntOr(BEST, 0) : -1;
    }

    /** Your best round on this level, -1 when none yet. */
    public static int best(Player p, Niveau niveau) {
        return best(p, BEST, niveau);
    }

    /** 1.7.0: your best round of a route (BEST, BEST_ACHTER, BEST18) on this level, -1 when none yet. */
    public static int best(Player p, String base, Niveau niveau) {
        CompoundTag saved = GuhQuests.saved(p);
        String key = Klassiekers.sleutel(base, niveau);
        return saved.contains(key) ? saved.getIntOr(key, 0) : -1;
    }

    public static Component holeName(int hole) {
        return Component.translatable("gui.guhs.golf.hole." + HOLE_NAMES[hole]);
    }

    /** Against par: "-2", "par", "+3". */
    public static String rel(int diff) {
        return diff == 0 ? "par" : diff > 0 ? "+" + diff : String.valueOf(diff);
    }

    /** How far from the Golfguh along x or z (whichever is more): the course is a square round her. */
    private static double horizontalDistance(ServerPlayer p, GuhNpcEntity npc) {
        return Math.max(Math.abs(p.getX() - npc.getX()), Math.abs(p.getZ() - npc.getZ()));
    }

    /** Golfers don't get hungrier or weaker than at the start, don't burn and never run out of air. */
    private static void refresh(ServerPlayer p) {
        nl.juiced.guhs.feature.Minigames.keep(p);                    // (no hungrier or weaker than at the start: no free healing)
        p.clearFire();
    }

    /** The club goes in your hand (an empty hotbar slot becomes the selected one). */
    private static void giveClub(ServerPlayer p) {
        Inventory inv = p.getInventory();
        ItemStack club = new ItemStack(GolfFeature.GOLFCLUB.get());
        if (inv.getSelectedItem().isEmpty()) {
            inv.setItem(inv.getSelectedSlot(), club);
            return;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, club);
                inv.setSelectedSlot(i);
                if (p.connection != null) {
                    p.connection.send(new ClientboundSetHeldSlotPacket(i));
                }
                return;
            }
        }
        int free = inv.getFreeSlot();                                  // (hotbar full: your own item moves to the backpack)
        if (free >= 0) {
            inv.setItem(free, inv.getSelectedItem());
            inv.setItem(inv.getSelectedSlot(), club);
        } else {
            inv.add(club);
        }
    }

    /** Takes back every golf club (inventory, crafting grid, the mouse cursor). */
    public static void removeClubs(Player p) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(GolfFeature.GOLFCLUB.get()), -1, p.inventoryMenu.getCraftSlots());
        if (p.containerMenu.getCarried().is(GolfFeature.GOLFCLUB.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(p, stack);   // (what doesn't fit drops in front of you)
    }

    /** A short message on the action bar (the status bar keeps quiet for a moment). */
    private void flash(ServerPlayer p, Component message) {
        p.sendOverlayMessage(message);
        quietUntil = p.level().getGameTime() + 30;
    }

    /** Someone left-clicked their ball: tell them how it works. */
    public static void poked(ServerPlayer p) {
        UUID npc = GOLFERS.get(p.getUUID());
        GolfGame game = npc == null ? null : GAMES.get(npc);
        if (game != null) {
            game.flash(p, Component.translatable("gui.guhs.golf.poke").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void title(ServerPlayer p, Component title, Component sub, int in, int stay, int out) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // --- for the GameTests --------------------------------------------------------------------------------------------------

    /** (Tests) starts a round of a route, as the Golfguh's screen would. */
    public void testStart(GuhNpcEntity npc, ServerPlayer p, Niveau level, Ronde route) {
        start(npc, p, level, route);
    }

    /** (Tests) the moving obstacles that were found: sliding blocks A and B, turning hubs, snapping jaws. */
    public List<List<BlockPos>> testObstakels() {
        return List.of(schuifA, schuifB, assen, tanden);
    }

    /** Skips the countdown. */
    public void testSkipCountdown() {
        if (phase == Phase.COUNTDOWN) {
            timer = 1;
        }
    }

    /** Finishes the current hole with this many strokes (as if the ball dropped in), and goes on at once. */
    public void testHoleOut(GuhNpcEntity npc, ServerPlayer p, int score) {
        if (phase == Phase.HOLED) {
            nextHole(npc, p);                                          // (the hole before is still waiting)
        }
        phase = Phase.ROLLING;
        holeOver(npc, p, score, true);
        nextHole(npc, p);
    }

    /** A sound only this player hears (1.1.0: ServerPlayer#playNotifySound is gone; the same packet by hand). */
    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.sounds.SoundEvent sound,
                                    net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        notifySound(p, net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch);
    }

    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,
                                    net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        if (p.connection != null) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, source, p.getX(), p.getY(), p.getZ(),
                    volume, pitch, p.getRandom().nextLong()));
        }
    }
}
