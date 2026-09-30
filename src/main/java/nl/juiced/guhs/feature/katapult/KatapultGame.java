package nl.juiced.guhs.feature.katapult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * De Knabbelkatapult with Kapitein Floepguh: one player at a time (the others watch from the castle wall). The Mika's
 * have stolen the kaasknabbels and hide them in forts on the cliff ledge across the gorge. A run is 12 hand-made forts in
 * a row ({@link KatapultFort}); each is built up before your eyes, and you get pluisballen to shoot at it with the
 * catapult: makkelijk 5 per fort with a dotted aiming line, medium 4, lastig 3 and the wind blows. A pluisbal knocks
 * blocks out of the fort and whatever loses its hold falls ({@link #impact}); falling blocks can knock others. Mika's
 * that fall run off giggling (they never hurt anyone), crates of kaasknabbels burst open: free knabbels!
 * <p>
 * Per fort: 10 points a block, 500 a Mika, 300 a crate, and when all Mika's are gone 1000 for every pluisbal left.
 * Stars: 1 = every Mika gone, 2 = and every crate open, 3 = and a pluisbal to spare. The run's total goes on the board of
 * its level ({@code katapult_<level>}). Katapultsterren: 1 for a whole run, +1 per 9 stars, +1 for beating your record on
 * that level (lastig +50%, {@link #munten}).
 */
public final class KatapultGame {
    /** Actions from the Kapitein's screen: START + the level's ordinal, the shop, stop. */
    public static final int START = 0, SHOP = 3, STOP = 4;
    public static final int COUNTDOWN = 61, BUILD_STEP = 2, SETTLE_MIN = 20, SETTLE_MAX = 20 * 5, FORT_DONE_TICKS = 60, IDLE_LIMIT = 20 * 120;
    /** How far around the Kapitein the catapult and the fort may be, and when you've walked away. */
    public static final int SCAN_RADIUS = 56, LEAVE_RADIUS = 44;
    /** You fire from this close to the catapult's bucket. */
    public static final double REACH = 5.0;
    public static final double MIN_SPEED = 0.35, MAX_SPEED = 1.75;
    public static final double WIND_STEP = 0.0035;
    public static final int SCORES_TICKS = 100;
    static final String BEST = "guhs_katapult_best_", RUNS = "guhs_katapult_runs", FIRST = "guhs_katapult_first";

    public enum Phase { IDLE, COUNTDOWN, BUILD, AIM, FLYING, SETTLE, FORT_DONE }

    private static final Map<UUID, KatapultGame> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();
    private static final int STALE_TICKS = 60;

    private final UUID npcId;
    @Nullable
    private UUID player;
    private String playerName = "";
    private Niveau niveau = Niveau.MEDIUM;
    private Phase phase = Phase.IDLE;
    private int timer;
    private long lastShot, lastTick, quietUntil, lastKnock;
    private final RandomSource random = RandomSource.create();

    // the run
    private int fort, ballsLeft, runScore, runStars, runKisten;
    private final int[] fortStars = new int[KatapultFort.FORTS];
    // the fort being played
    private int fortMikas, fortKisten, mikasOut, kistenFree, blocksKnocked, fortScore, shotKnocks;
    private final Set<BlockPos> fortBlocks = new HashSet<>();
    private Vec3 wind = Vec3.ZERO;
    private int windForce;
    @Nullable
    private UUID ball;
    private List<KatapultFort.Stuk> building = List.of();
    private int layer;

    // the catapult and the plot, found once
    private boolean scanned;
    @Nullable
    private BlockPos werper, plek, home;
    private Direction werperFacing = Direction.NORTH, plekFacing = Direction.SOUTH;

    private KatapultGame(UUID npcId) {
        this.npcId = npcId;
    }

    public static KatapultGame of(GuhNpcEntity npc) {
        KatapultGame game = GAMES.computeIfAbsent(npc.getUUID(), KatapultGame::new);
        if (!game.scanned) {
            game.load(npc.roleData);
        }
        return game;
    }

    public static boolean isPlaying(Player p) {
        return PLAYERS.containsKey(p.getUUID());
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

    public Niveau niveau() {
        return niveau;
    }

    public int fort() {
        return fort;
    }

    public int ballsLeft() {
        return ballsLeft;
    }

    public int fortMikas() {
        return fortMikas;
    }

    public int mikasOut() {
        return mikasOut;
    }

    public int fortKisten() {
        return fortKisten;
    }

    public int kistenFree() {
        return kistenFree;
    }

    public int runScore() {
        return runScore;
    }

    public int runStars() {
        return runStars;
    }

    public int fortScore() {
        return fortScore;
    }

    public Vec3 wind() {
        return wind;
    }

    public Set<BlockPos> fortBlocks() {
        return fortBlocks;
    }

    @Nullable
    public BlockPos werper() {
        return werper;
    }

    @Nullable
    public BlockPos plek() {
        return plek;
    }

    public Direction plekFacing() {
        return plekFacing;
    }

    /** Pluisballen per fort on each level: makkelijk 5, medium 4, lastig 3. */
    public static int balls(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 5;
            case MEDIUM -> 4;
            case LASTIG -> 3;
        };
    }

    /** Stars for a fort: 1 every Mika gone, 2 and every crate open, 3 and a pluisbal to spare. */
    public static int stars(boolean cleared, boolean allKisten, int ballsLeft) {
        if (!cleared) {
            return 0;
        }
        return 1 + (allKisten ? 1 : 0) + (allKisten && ballsLeft > 0 ? 1 : 0);
    }

    /** Katapultsterren for a run: 1, +1 per 9 stars, +1 for beating your record; lastig +50%. */
    public static int munten(Niveau niveau, int stars, boolean record) {
        return niveau.munten(1 + Math.max(0, stars) / 9 + (record ? 1 : 0));
    }

    public static String board(Niveau niveau) {
        return "katapult_" + niveau.id();
    }

    // --- talking to the Kapitein -------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        KatapultGame game = of(npc);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.05f);
        String line = game.isPlayedBy(player) ? "playing" : game.isRunning() ? "busy" : "hello" + (1 + npc.getRandom().nextInt(4));
        GuhQuests.say(player, npc, "quest.guhs.katapult." + line, game.playerName);
        KatapultFeature.checkKleding(player);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        data.putBoolean("Mine", game.isPlayedBy(player));
        data.putString("Player", game.playerName);
        data.putInt("Fort", game.fort + 1);
        data.putInt("Runs", GuhQuests.saved(player).getIntOr(RUNS, 0));
        for (Niveau n : Niveau.values()) {
            data.putInt("Best_" + n.id(), best(player, n));
            List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), board(n));
            data.putString("RecordName_" + n.id(), top.isEmpty() ? "" : top.get(0).name());
            data.putInt("RecordScore_" + n.id(), top.isEmpty() ? -1 : top.get(0).score());
        }
        ModNetworking.sendTo(player, new KatapultPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.KATAPULTGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        KatapultGame game = of(npc);
        if (action == SHOP) {
            npc.openShop(player);
        } else if (action == STOP && game.isPlayedBy(player)) {
            game.end(npc, player, "quest.guhs.katapult.stopped");
        } else if (action >= START && action < START + Niveau.values().length) {
            game.start(npc, player, Niveau.of(action - START));
        }
    }

    // --- a run -----------------------------------------------------------------------------------------------------------------

    private void start(GuhNpcEntity npc, ServerPlayer p, Niveau level) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.katapult.busy", playerName);
            return;
        }
        if (isPlaying(p)) {
            GuhQuests.say(p, npc, "quest.guhs.katapult.elsewhere");
            return;
        }
        if (Minigames.refuse(p, npc, Minigames.KATAPULT)) {
            return;
        }
        if (scanned && !intact(world)) {
            scanned = false;
        }
        if (!scan(npc, true) || !intact(world) || KatapultFort.load(world, 0).isEmpty()) {
            GuhQuests.say(p, npc, "quest.guhs.katapult.broken");
            return;
        }
        if (p.getInventory().getFreeSlot() < 0 && !p.getMainHandItem().isEmpty()) {
            GuhQuests.say(p, npc, "quest.guhs.katapult.full");
            return;
        }
        player = p.getUUID();
        playerName = p.getGameProfile().name();
        PLAYERS.put(player, npcId);
        niveau = level;
        fort = 0;
        runScore = runStars = runKisten = 0;
        java.util.Arrays.fill(fortStars, 0);
        lastShot = world.getGameTime();
        lastTick = world.getGameTime();
        Minigames.startKeeping(p);
        p.clearFire();
        Vec3 spot = behindCatapult();
        p.teleportTo(world, spot.x, spot.y, spot.z, java.util.Set.of(), werperFacing.toYRot(), -10f, true);
        phase = Phase.COUNTDOWN;
        timer = COUNTDOWN;
        GuhQuests.say(p, npc, "quest.guhs.katapult.start", niveau.naam());
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.how." + niveau.id()).withStyle(ChatFormatting.GRAY));
    }

    /** Where you stand to shoot: two blocks behind the catapult's bucket. */
    private Vec3 behindCatapult() {
        return new Vec3(werper.getX() + 0.5 - werperFacing.getStepX() * 2.2, werper.getY(), werper.getZ() + 0.5 - werperFacing.getStepZ() * 2.2);
    }

    /** A new fort: the plot is cleared and the fort is built up, layer by layer. */
    private void startFort(ServerLevel world) {
        clearPlot(world);
        building = KatapultFort.load(world, fort);
        layer = 0;
        fortMikas = fortKisten = mikasOut = kistenFree = blocksKnocked = fortScore = 0;
        fortBlocks.clear();
        phase = Phase.BUILD;
        timer = BUILD_STEP;
        if (niveau == Niveau.LASTIG) {
            windForce = random.nextInt(4);
            double a = random.nextDouble() * Math.PI * 2;
            wind = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(windForce * WIND_STEP);
        } else {
            windForce = 0;
            wind = Vec3.ZERO;
        }
    }

    /** Builds the next layer of the fort; true when the whole fort stands. */
    private boolean buildLayer(ServerLevel world) {
        net.minecraft.world.level.block.Rotation rot = KatapultFort.rotation(plekFacing);
        boolean any = false;
        for (KatapultFort.Stuk s : building) {
            if (s.local().getY() != layer) {
                continue;
            }
            BlockPos pos = KatapultFort.world(plek, plekFacing, s.local().getX(), s.local().getY(), s.local().getZ());
            BlockState state = s.state().rotate(rot);
            world.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            fortBlocks.add(pos);
            if (KatapultFort.isMika(state)) {
                fortMikas++;
            } else if (KatapultFort.isKist(state)) {
                fortKisten++;
            }
            if (!any) {
                world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0.05);
            }
            any = true;
        }
        if (any) {
            BlockPos mid = KatapultFort.world(plek, plekFacing, KatapultFort.MIDDEN, layer, KatapultFort.D / 2);
            world.playSound(null, mid, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 0.8f + layer * 0.05f);
        }
        layer++;
        return layer >= KatapultFort.H;
    }

    /** Places a whole fort at once (after a run: the first fort waits for the next player). */
    private void placeNow(ServerLevel world, int index) {
        clearPlot(world);
        building = KatapultFort.load(world, index);
        fortBlocks.clear();
        fortMikas = fortKisten = 0;
        for (layer = 0; layer < KatapultFort.H; ) {
            buildLayer(world);
        }
    }

    /** Clears the fort's box and takes away loose brokjes and pluisballen of this game. */
    private void clearPlot(ServerLevel world) {
        for (int x = 0; x < KatapultFort.W; x++) {
            for (int y = 0; y < KatapultFort.H; y++) {
                for (int z = 0; z < KatapultFort.D; z++) {
                    BlockPos pos = KatapultFort.world(plek, plekFacing, x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
        fortBlocks.clear();
        AABB area = new AABB(plek).inflate(40);
        for (KatapultBrokjeEntity b : world.getEntitiesOfClass(KatapultBrokjeEntity.class, area, b -> npcId.equals(b.getNpc()))) {
            b.discard();
        }
        for (PluisbalEntity b : world.getEntitiesOfClass(PluisbalEntity.class, area.inflate(20), b -> npcId.equals(b.getNpc()))) {
            b.discard();
        }
        ball = null;
    }

    /** Let go of the elastic: FLOEP! (when it's your turn, you stand at the catapult and look towards the fort). */
    public static void fire(ServerPlayer p, float power) {
        UUID npc = PLAYERS.get(p.getUUID());
        KatapultGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.fireWith(p, power, p.getLookAngle());
        }
    }

    public boolean fireWith(ServerPlayer p, float power, Vec3 look) {
        ServerLevel world = p.level();
        if (phase != Phase.AIM || ballsLeft <= 0) {
            flash(p, Component.translatable(phase == Phase.FLYING || phase == Phase.SETTLE ? "gui.guhs.katapult.wait_ball"
                    : "gui.guhs.katapult.wait_fort").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Vec3 bucket = Vec3.atBottomCenterOf(werper);
        if (p.position().distanceTo(bucket) > REACH) {
            flash(p, Component.translatable("gui.guhs.katapult.too_far").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Vec3 dir = look.normalize();
        if (dir.x * werperFacing.getStepX() + dir.z * werperFacing.getStepZ() < 0.1) {
            flash(p, Component.translatable("gui.guhs.katapult.look").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * Math.max(0, Math.min(1, power));
        Vec3 from = launchPoint(werper, werperFacing);
        PluisbalEntity b = PluisbalEntity.create(world, from, dir.scale(speed), npcId, wind, plek.getY() - 30);
        ball = b.getUUID();
        ballsLeft--;
        shotKnocks = 0;
        lastShot = world.getGameTime();
        setStack(p, ballsLeft);
        phase = Phase.FLYING;
        world.playSound(null, werper, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.6f);
        world.playSound(null, werper, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
        world.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 8, 0.2, 0.2, 0.2, 0.05);
        if (power >= 0.99f) {
            flash(p, Component.translatable("gui.guhs.katapult.vahoeg").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** Where a pluisbal leaves the catapult: just above its bucket. */
    public static Vec3 launchPoint(BlockPos werper, Direction facing) {
        return new Vec3(werper.getX() + 0.5 + facing.getStepX() * 0.4, werper.getY() + 1.2, werper.getZ() + 0.5 + facing.getStepZ() * 0.4);
    }

    // --- hits and falls ----------------------------------------------------------------------------------------------------------

    static boolean ballAlive(PluisbalEntity b) {
        KatapultGame game = b.getNpc() == null ? null : GAMES.get(b.getNpc());
        return game != null && game.isRunning() && b.getUUID().equals(game.ball);
    }

    /** A pluisbal hit a block: part of the fort? Then knock (returns true). */
    static boolean hitFort(PluisbalEntity b, BlockPos pos, Vec3 at, Vec3 velocity, double energy) {
        KatapultGame game = b.getNpc() == null ? null : GAMES.get(b.getNpc());
        if (game == null || !b.getUUID().equals(game.ball) || !(b.level() instanceof ServerLevel world)) {
            return false;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(world.getServer(), game.player, "katapult", true); // samen
        return game.impact(world, pos, at, velocity, energy);
    }

    static void ballDone(PluisbalEntity b) {
        KatapultGame game = b.getNpc() == null ? null : GAMES.get(b.getNpc());
        if (game == null || !b.getUUID().equals(game.ball)) {
            return;
        }
        game.ball = null;
        if (game.phase == Phase.FLYING) {
            game.phase = Phase.SETTLE;
            game.timer = SETTLE_MAX;
            game.lastKnock = b.level().getGameTime();
            if (game.shotKnocks == 0 && b.level() instanceof ServerLevel world && game.player != null) {
                ServerPlayer p = world.getServer().getPlayerList().getPlayer(game.player);
                if (p != null) {
                    nl.juiced.guhs.feature.samen.SamenSpel.mis(p, "katapult"); // samen
                    game.flash(p, Component.translatable("gui.guhs.katapult.mis").withStyle(ChatFormatting.LIGHT_PURPLE));
                    BlockPos mid = KatapultFort.world(game.plek, game.plekFacing, KatapultFort.MIDDEN, 3, 2);
                    world.playSound(null, mid, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.8f);
                }
            }
        }
    }

    /** A falling brokje landed hard on something: another fort block may go too. */
    static boolean crash(KatapultBrokjeEntity b, BlockPos pos, Vec3 velocity, double energy) {
        KatapultGame game = b.getNpc() == null ? null : GAMES.get(b.getNpc());
        if (game == null || !game.isRunning() || !(b.level() instanceof ServerLevel world) || !game.fortBlocks.contains(pos)) {
            return false;
        }
        return game.knockAround(world, pos, Vec3.atCenterOf(pos), velocity, energy, 1.0);
    }

    /** Is this spot part of the fort (still standing)? */
    public boolean inFort(BlockPos pos) {
        return fortBlocks.contains(pos);
    }

    /**
     * A pluisbal hit the fort at `at` (block pos) with this much energy: blocks around the hit (and a bit behind it, the
     * way the ball flew) are knocked out, the nearest first, as long as the energy lasts; then whatever lost its hold falls.
     */
    public boolean impact(ServerLevel world, BlockPos pos, Vec3 at, Vec3 velocity, double energy) {
        if (!fortBlocks.contains(pos)) {
            return false;
        }
        Vec3 dir = velocity.lengthSqr() < 1e-8 ? Vec3.ZERO : velocity.normalize();
        Vec3 centre = at.add(dir.scale(0.6));
        double radius = 1.1 + Math.min(1.2, energy / 10);
        knockAround(world, pos, centre, velocity, energy, radius);
        return true;
    }

    private boolean knockAround(ServerLevel world, BlockPos first, Vec3 centre, Vec3 velocity, double energy, double radius) {
        List<BlockPos> near = new ArrayList<>();
        for (BlockPos b : fortBlocks) {
            if (!b.equals(first) && Vec3.atCenterOf(b).distanceTo(centre) <= radius) {
                near.add(b);
            }
        }
        near.sort(Comparator.comparingDouble(b -> Vec3.atCenterOf(b).distanceToSqr(centre)));
        near.add(0, first);
        double left = energy;
        boolean any = false;
        double speed = velocity.length();
        for (BlockPos b : near) {
            BlockState state = world.getBlockState(b);
            int cost = KatapultFort.strength(state);
            if (left >= cost || (!any && left >= cost * 0.5)) {
                left -= cost;
                any = true;
                Vec3 push = velocity.lengthSqr() < 1e-8 ? Vec3.ZERO : velocity.normalize().scale(0.2 + Math.min(0.6, speed * 0.35));
                knock(world, b, push.add((random.nextDouble() - 0.5) * 0.15, 0.15 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.15));
            } else if (!any) {
                world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                world.playSound(null, b, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1f, 0.8f);
                break;
            }
        }
        if (any) {
            settle(world);
        }
        return any;
    }

    /** One fort block out: it becomes a tumbling brokje, and it counts. */
    private void knock(ServerLevel world, BlockPos pos, Vec3 velocity) {
        BlockState state = world.getBlockState(pos);
        fortBlocks.remove(pos);
        if (state.isAir()) {
            return;
        }
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        KatapultBrokjeEntity.create(world, pos, state, velocity, npcId);
        world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
        count(world, pos, state);
    }

    private void count(ServerLevel world, BlockPos pos, BlockState state) {
        shotKnocks++;
        lastKnock = world.getGameTime();
        fortScore += KatapultFort.points(state);
        if (KatapultFort.isMika(state)) {
            mikasOut++;
            ServerPlayer p = player == null ? null : world.getServer().getPlayerList().getPlayer(player);
            if (p != null) {
                flash(p, Component.translatable(mikasOut >= fortMikas ? "gui.guhs.katapult.mika_laatste" : "gui.guhs.katapult.mika_weg",
                        mikasOut, fortMikas).withStyle(ChatFormatting.GOLD));
            }
            world.playSound(null, pos, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.6f);
        } else if (KatapultFort.isKist(state)) {
            kistenFree++;
            runKisten++;
            world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.7f, 1.4f);
        } else {
            blocksKnocked++;
        }
    }

    /** Whatever doesn't hold any more falls (see KatapultFort.unstable). */
    private void settle(ServerLevel world) {
        Set<BlockPos> local = new HashSet<>();
        Map<BlockPos, BlockPos> back = new java.util.HashMap<>();
        for (BlockPos b : fortBlocks) {
            BlockPos l = KatapultFort.local(plek, plekFacing, b);
            if (l != null) {
                local.add(l);
                back.put(l, b);
            }
        }
        for (BlockPos l : KatapultFort.unstable(local)) {
            BlockPos b = back.get(l);
            if (b != null && fortBlocks.contains(b)) {
                knock(world, b, new Vec3((random.nextDouble() - 0.5) * 0.08, 0, (random.nextDouble() - 0.5) * 0.08));
            }
        }
    }

    /** Are there still brokjes of this game in the air? */
    private boolean tumbling(ServerLevel world) {
        return !world.getEntitiesOfClass(KatapultBrokjeEntity.class, new AABB(plek).inflate(30), b -> npcId.equals(b.getNpc())
                && b.getDeltaMovement().lengthSqr() > 0.01).isEmpty();
    }

    // --- the game's clock ------------------------------------------------------------------------------------------------------------

    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        if (npc.tickCount % SCORES_TICKS == 1) {
            if (!scanned && world.getNearestPlayer(npc, 40) != null) {
                scan(npc, false);
            }
            showScores(npc);
        }
        if (npc.tickCount % 40 == 0) {
            for (ServerPlayer near : world.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(12))) {
                KatapultFeature.checkKleding(near);
            }
        }
        if (player == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || horizontal(p, npc) > LEAVE_RADIUS) {
            if (p != null && p.isAlive() && p.level() == world) {
                end(npc, p, "quest.guhs.katapult.walked_away");
            } else {
                if (p != null) {
                    removeStacks(p);
                }
                end(npc, null, null);
            }
            return;
        }
        if (npc.tickCount % 20 == 0) {
            Minigames.keep(p);
            p.clearFire();
            if (phase == Phase.AIM && ballsLeft > 0 && !p.getInventory().hasAnyMatching(s -> s.is(KatapultFeature.PLUISBALLEN.get()))
                    && p.getInventory().getFreeSlot() >= 0) {
                giveStack(p, ballsLeft);
            }
        }
        if (niveau == Niveau.LASTIG && windForce > 0 && npc.tickCount % 4 == 0 && (phase == Phase.AIM || phase == Phase.FLYING)) {
            Vec3 from = Vec3.atCenterOf(werper).add(0, 3, 0);
            Vec3 w = wind.normalize().scale(0.25 * windForce);
            world.sendParticles(ParticleTypes.CLOUD, from.x + random.nextGaussian() * 3, from.y + random.nextGaussian(), from.z + random.nextGaussian() * 3,
                    0, w.x, 0.0, w.z, 1.0);
        }
        switch (phase) {
            case COUNTDOWN -> {
                timer--;
                if (timer == 60 || timer == 40 || timer == 20) {
                    title(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.katapult.countdown_sub", niveau.naam()), 0, 22, 0);
                    notifySound(p, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.9f, 1.0f);
                }
                if (timer <= 0) {
                    startFort(world);
                }
            }
            case BUILD -> {
                if (--timer <= 0) {
                    timer = BUILD_STEP;
                    if (buildLayer(world)) {
                        fortReady(world, npc, p);
                    }
                }
            }
            case AIM -> {
                if (world.getGameTime() - lastShot > IDLE_LIMIT) {
                    end(npc, p, "quest.guhs.katapult.idle");
                    return;
                }
            }
            case FLYING -> {
                if (ball == null || !(world.getEntity(ball) instanceof PluisbalEntity)) {
                    ball = null;
                    phase = Phase.SETTLE;
                    timer = SETTLE_MAX;
                }
            }
            case SETTLE -> {
                timer--;
                boolean calm = world.getGameTime() - lastKnock >= SETTLE_MIN && !tumbling(world);
                if (calm || timer <= 0) {
                    afterShot(world, npc, p);
                }
            }
            case FORT_DONE -> {
                if (--timer <= 0) {
                    fort++;
                    if (fort >= KatapultFort.FORTS) {
                        finishRun(world, npc, p);
                        return;
                    }
                    startFort(world);
                }
            }
            default -> {
            }
        }
        if (isRunning() && npc.tickCount % 10 == 0 && (phase == Phase.AIM || phase == Phase.FLYING || phase == Phase.SETTLE)
                && world.getGameTime() >= quietUntil) {
            p.sendOverlayMessage(status());
        }
    }

    /** The whole fort stands: the pluisballen, the fort's name, the wind. */
    private void fortReady(ServerLevel world, GuhNpcEntity npc, ServerPlayer p) {
        ballsLeft = balls(niveau);
        giveStack(p, ballsLeft);
        phase = Phase.AIM;
        lastShot = world.getGameTime();
        Component sub = niveau == Niveau.LASTIG
                ? Component.translatable("gui.guhs.katapult.fort_sub_wind", fortMikas, fortKisten, windText())
                : Component.translatable("gui.guhs.katapult.fort_sub", fortMikas, fortKisten);
        title(p, Component.translatable("gui.guhs.katapult.fort_title", fort + 1, Component.translatable("gui.guhs.katapult.fort." + (fort + 1)))
                .withStyle(ChatFormatting.GOLD), sub, 5, 50, 10);
        BlockPos mid = KatapultFort.world(plek, plekFacing, KatapultFort.MIDDEN, 4, 3);
        world.playSound(null, mid, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1.2f, 1.5f);
        if (fort == 0 || random.nextInt(3) == 0) {
            GuhQuests.say(p, npc, "quest.guhs.katapult.mika" + (1 + random.nextInt(5)));
        }
    }

    /** The ball is done and the fort has calmed down: next shot, or this fort is over. */
    private void afterShot(ServerLevel world, GuhNpcEntity npc, ServerPlayer p) {
        if (mikasOut >= fortMikas) {
            fortDone(world, npc, p, true);
        } else if (ballsLeft > 0) {
            phase = Phase.AIM;
        } else {
            fortDone(world, npc, p, false);
        }
    }

    private void fortDone(ServerLevel world, GuhNpcEntity npc, ServerPlayer p, boolean cleared) {
        int bonus = cleared ? ballsLeft * 1000 : 0;
        fortScore += bonus;
        int stars = stars(cleared, kistenFree >= fortKisten, ballsLeft);
        fortStars[fort] = stars;
        runScore += fortScore;
        runStars += stars;
        removeStacks(p);
        String sterren = "★".repeat(stars) + "☆".repeat(3 - stars);
        title(p, Component.literal(sterren).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable(cleared ? "gui.guhs.katapult.fort_gevallen" : "gui.guhs.katapult.fort_staat", fortScore), 5, 45, 10);
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.fort_klaar", fort + 1, Component.translatable("gui.guhs.katapult.fort." + (fort + 1)),
                sterren, fortScore, mikasOut, fortMikas, kistenFree, fortKisten, bonus).withStyle(cleared ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        notifySound(p, stars == 3 ? SoundEvents.PLAYER_LEVELUP : cleared ? SoundEvents.NOTE_BLOCK_CHIME.value() : SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),
                SoundSource.PLAYERS, 0.9f, 1.2f);
        if (cleared) {
            BlockPos mid = KatapultFort.world(plek, plekFacing, KatapultFort.MIDDEN, 2, 3);
            world.sendParticles(ParticleTypes.FIREWORK, mid.getX() + 0.5, mid.getY() + 2, mid.getZ() + 0.5, 30, 2, 1.5, 2, 0.1);
            npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.2f);
        } else {
            world.playSound(null, KatapultFort.world(plek, plekFacing, KatapultFort.MIDDEN, 3, 2), ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.9f);
        }
        if (stars == 3) {
            KatapultFeature.advancement(p, "katapult_drie_sterren");
        }
        phase = Phase.FORT_DONE;
        timer = FORT_DONE_TICKS;
    }

    /** All 12 forts: the stars, the points, the katapultsterren and the free knabbels. */
    private void finishRun(ServerLevel world, GuhNpcEntity npc, ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        int best = best(p, niveau);
        boolean record = best >= 0 && runScore > best;
        if (best < 0 || runScore > best) {
            saved.putInt(BEST + niveau.id(), runScore);
        }
        saved.putInt(RUNS, saved.getIntOr(RUNS, 0) + 1);
        int munten = munten(niveau, runStars, record);
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.run_klaar", niveau.naam()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < KatapultFort.FORTS; i++) {
            line.append(i == 0 ? "" : " ").append(fortStars[i]);
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.run_sterren", line.toString(), runStars, KatapultFort.FORTS * 3)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.run_punten", runScore).withStyle(ChatFormatting.YELLOW));
        if (record) {
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.record", runScore, best).withStyle(ChatFormatting.GOLD));
        } else if (best < 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.first_record", runScore).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.no_record", best).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.katapult.munten", munten).withStyle(ChatFormatting.LIGHT_PURPLE));
        Minigames.give(p, new ItemStack(KatapultFeature.KATAPULTSTER.get(), munten));
        int knabbels = Math.min(32, runKisten);
        if (knabbels > 0) {
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), knabbels));
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.knabbels", knabbels).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (!saved.getBooleanOr(FIRST, false)) {
            saved.putBoolean(FIRST, true);
            Minigames.give(p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 4));
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.first").withStyle(ChatFormatting.GOLD));
        }
        KatapultFeature.advancement(p, "katapult_gespeeld");
        if (niveau == Niveau.LASTIG) {
            KatapultFeature.advancement(p, "katapult_lastig");
        }
        if (runStars >= KatapultFort.FORTS * 3) {
            KatapultFeature.advancement(p, "katapult_alle_sterren");
        }
        if (Scorebord.submit(p, board(niveau), runScore, false) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.katapult.kasteel_record").withStyle(ChatFormatting.GOLD));
        }
        showScores(npc);
        title(p, Component.translatable("gui.guhs.katapult.run_title", runStars).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.katapult.run_sub", runScore, munten), 5, 70, 20);
        notifySound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        GuhQuests.say(p, npc, "quest.guhs.katapult.einde_" + (runStars >= 30 ? "top" : runStars >= 15 ? "goed" : "oefenen"));
        end(npc, p, null);
    }

    /** The run ends: pluisballen back, the first fort is rebuilt for the next one. */
    private void end(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String message) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null) {
            removeStacks(p);
            if (message != null) {
                p.sendSystemMessage(Component.translatable(message).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (player != null) {
            PLAYERS.remove(player, npcId);
        }
        player = null;
        phase = Phase.IDLE;
        if (plek != null && world.isLoaded(plek)) {
            placeNow(world, 0);
        }
        ball = null;
    }

    public static void leave(ServerPlayer p) {
        UUID npc = PLAYERS.remove(p.getUUID());
        removeStacks(p);
        KatapultGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.player = null;
            game.phase = Phase.IDLE;
            game.ball = null;
        }
    }

    public static boolean hasKapiteinNearby(ServerPlayer p) {
        UUID npc = PLAYERS.get(p.getUUID());
        KatapultGame game = npc == null ? null : GAMES.get(npc);
        return game != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - game.lastTick <= STALE_TICKS;
    }

    // --- the catapult and the plot -------------------------------------------------------------------------------------------------

    public boolean scan(GuhNpcEntity npc, boolean force) {
        if (scanned) {
            return true;
        }
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos c = npc.blockPosition();
        BlockPos min = c.offset(-SCAN_RADIUS, -28, -SCAN_RADIUS), max = c.offset(SCAN_RADIUS, 20, SCAN_RADIUS);
        if (!force && !world.hasChunksAt(min, max)) {
            return false;
        }
        werper = plek = null;
        double wBest = Double.MAX_VALUE, pBest = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = world.getBlockState(pos);
            if (state.is(KatapultFeature.WERPER.get()) && pos.distSqr(c) < wBest) {
                wBest = pos.distSqr(c);
                werper = pos.immutable();
                werperFacing = state.getValue(KatapultBlocks.Gericht.FACING);
            } else if (state.is(KatapultFeature.FORTPLEK.get()) && pos.distSqr(c) < pBest) {
                pBest = pos.distSqr(c);
                plek = pos.immutable();
                plekFacing = state.getValue(KatapultBlocks.Gericht.FACING);
            }
        }
        if (werper == null || plek == null) {
            return false;
        }
        home = c;
        scanned = true;
        CompoundTag tag = new CompoundTag();
        tag.putLong("Werper", werper.asLong());
        tag.putString("WerperFacing", werperFacing.getName());
        tag.putLong("Plek", plek.asLong());
        tag.putString("PlekFacing", plekFacing.getName());
        npc.roleData.put("Katapult", tag);
        // the fort as it was built with the castle: remember its blocks (they're the first fort)
        if (fortBlocks.isEmpty() && !isRunning()) {
            placeNow(world, 0);
        }
        return true;
    }

    private void load(CompoundTag data) {
        CompoundTag tag = data.getCompoundOrEmpty("Katapult");
        if (!tag.contains("Werper") || !tag.contains("Plek")) {
            return;
        }
        werper = BlockPos.of(tag.getLongOr("Werper", 0L));
        plek = BlockPos.of(tag.getLongOr("Plek", 0L));
        Direction w = Direction.byName(tag.getStringOr("WerperFacing", "")), f = Direction.byName(tag.getStringOr("PlekFacing", ""));
        werperFacing = w == null || w.getAxis().isVertical() ? Direction.NORTH : w;
        plekFacing = f == null || f.getAxis().isVertical() ? Direction.SOUTH : f;
        scanned = true;
    }

    private boolean intact(ServerLevel world) {
        return werper != null && plek != null && world.getBlockState(werper).is(KatapultFeature.WERPER.get())
                && world.getBlockState(plek).is(KatapultFeature.FORTPLEK.get());
    }

    /** The world's top 3 of every level floats above the Kapitein, and over the Mika fort. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<String> boards = new ArrayList<>();
        List<Component> headings = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add(board(n));
            headings.add(Component.translatable("gui.guhs.katapult.scorebord_niveau", n.naam(), balls(n)));
        }
        Component text = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.katapult"), boards, headings, s -> s + " pt");
        Scorebord.show(world, npc.position().add(0, 2.6, 0), "katapult", text);
        KatapultGame game = GAMES.get(npc.getUUID());
        if (game != null && game.plek != null) {
            BlockPos top = KatapultFort.world(game.plek, game.plekFacing, KatapultFort.MIDDEN, KatapultFort.H + 1, KatapultFort.D / 2);
            Scorebord.show(world, Vec3.atCenterOf(top), "katapult_fort", text);
        }
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    public static int best(Player p, Niveau niveau) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(BEST + niveau.id()) ? saved.getIntOr(BEST + niveau.id(), 0) : -1;
    }

    /** The wind as the player sees it from the catapult: arrows (→ pushes to the right) and a force 1-3. */
    public Component windText() {
        if (windForce == 0) {
            return Component.translatable("gui.guhs.katapult.wind_stil");
        }
        Direction right = werperFacing.getClockWise();
        double along = wind.x * werperFacing.getStepX() + wind.z * werperFacing.getStepZ(), side = wind.x * right.getStepX() + wind.z * right.getStepZ();
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        double a = Math.atan2(side, along);
        int i = Math.floorMod((int) Math.round(a / (Math.PI / 4)), 8);
        return Component.translatable("gui.guhs.katapult.wind", arrows[i], windForce);
    }

    private Component status() {
        StringBuilder balls = new StringBuilder();
        for (int i = 0; i < balls(niveau); i++) {
            balls.append(i < ballsLeft ? "●" : "○");
        }
        Component w = niveau == Niveau.LASTIG ? windText() : Component.empty();
        return Component.translatable("gui.guhs.katapult.bar", fort + 1, KatapultFort.FORTS, balls.toString(), mikasOut, fortMikas, kistenFree,
                fortKisten, runScore + fortScore, w).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    private static double horizontal(ServerPlayer p, GuhNpcEntity npc) {
        return Math.max(Math.abs(p.getX() - npc.getX()), Math.abs(p.getZ() - npc.getZ()));
    }

    private void giveStack(ServerPlayer p, int count) {
        removeStacks(p);
        if (count <= 0) {
            return;
        }
        Inventory inv = p.getInventory();
        ItemStack stack = PluisballenItem.stack(count, niveau == Niveau.MAKKELIJK);
        if (inv.getSelectedItem().isEmpty()) {
            inv.setItem(inv.getSelectedSlot(), stack);
            return;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                inv.setSelectedSlot(i);
                if (p.connection != null) {
                    p.connection.send(new ClientboundSetHeldSlotPacket(i));
                }
                return;
            }
        }
        int free = inv.getFreeSlot();
        if (free >= 0) {
            inv.setItem(free, inv.getSelectedItem());
            inv.setItem(inv.getSelectedSlot(), stack);
        } else {
            inv.add(stack);
        }
    }

    private static void setStack(ServerPlayer p, int count) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(KatapultFeature.PLUISBALLEN.get())) {
                if (count <= 0) {
                    inv.setItem(i, ItemStack.EMPTY);
                } else {
                    s.setCount(count);
                }
                return;
            }
        }
    }

    public static void removeStacks(Player p) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(KatapultFeature.PLUISBALLEN.get()), -1, p.inventoryMenu.getCraftSlots());
        if (p.containerMenu.getCarried().is(KatapultFeature.PLUISBALLEN.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private void flash(ServerPlayer p, Component message) {
        p.sendOverlayMessage(message);
        quietUntil = p.level().getGameTime() + 30;
    }

    private static void title(ServerPlayer p, Component title, Component sub, int in, int stay, int out) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // --- for the GameTests -----------------------------------------------------------------------------------------------------

    public void testSkipCountdown() {
        if (phase == Phase.COUNTDOWN) {
            timer = 1;
        }
    }

    /** Builds the current fort at once (skips the countdown and the building). */
    public void testBuildNow(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        if (phase == Phase.COUNTDOWN) {
            startFort(world);
        }
        while (phase == Phase.BUILD && !buildLayer(world)) {
            // (layer by layer)
        }
        if (phase == Phase.BUILD) {
            fortReady(world, npc, p);
        }
    }

    /** Knocks every Mika of the fort out (as if well hit) and lets the fort end right away. */
    public void testClearFort(GuhNpcEntity npc, ServerPlayer p, boolean kisten) {
        ServerLevel world = (ServerLevel) npc.level();
        for (BlockPos b : List.copyOf(fortBlocks)) {
            BlockState state = world.getBlockState(b);
            if (KatapultFort.isMika(state) || (kisten && KatapultFort.isKist(state))) {
                if (fortBlocks.contains(b)) {
                    knock(world, b, Vec3.ZERO);
                }
            }
        }
        afterShot(world, npc, p);
    }

    /** Goes on to the next fort at once (after FORT_DONE). */
    public void testNextFort(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        if (phase == Phase.FORT_DONE) {
            fort++;
            if (fort >= KatapultFort.FORTS) {
                finishRun(world, npc, p);
                return;
            }
            startFort(world);
            testBuildNow(npc, p);
        }
    }

    public int[] fortStars() {
        return fortStars;
    }

    /** 1.1.0: ServerPlayer#playNotifySound is gone - the same packet: a sound only this player hears, at the player. */
    private static void notifySound(ServerPlayer p, net.minecraft.sounds.SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, p.getX(), p.getY(), p.getZ(),
                volume, pitch, p.getRandom().nextLong()));
    }
}
