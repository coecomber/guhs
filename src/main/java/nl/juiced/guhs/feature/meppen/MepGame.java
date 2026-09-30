package nl.juiced.guhs.feature.meppen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.meppen.MepBlocks.Kop;
import nl.juiced.guhs.feature.meppen.MepBlocks.MepKop;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * One whack-a-Mika board and its game (one per Mepguh; one player at a time, others can watch). The board is every
 * mika_mep_gat block around the Mepguh; the player stands in the middle of it. After a 3-2-1 countdown, for 60 seconds
 * heads pop up out of the holes (a head is a mika_mep_kop block on top of a hole), faster and faster and more at once.
 * Left-click one with the loaned Mika-mephamer: a Mika is 10 points, a golden Mika 50, both times the combo multiplier
 * (+1 every 5 hits in a row, at most x5). A guh costs 25 points and your combo; so does a Mika you let get away or a
 * whack in an empty hole (the combo only). At the end: mepmunten by score, your personal best in the chat (saved with
 * you, survives death), the world's top 3 ({@link Scorebord}, board {@value #BOARD}) floating at the scoreboard wall,
 * and advancements.
 * <p>
 * While playing you can't get hurt or hungry. The mallet (and the item it replaced in a full hotbar) is given back when
 * the game ends in any way: time up, walking away, stopping, dying, logging out, changing dimension.
 * The game isn't saved: after a restart the board is simply cleared.
 */
public final class MepGame {
    /** Actions from the Mepguh's screen. */
    public static final int START = 0, SHOP = 1, STOP = 2, HELP = 3;
    public static final int COUNTDOWN = 60, LENGTH = 20 * 60;
    /** How far from the Mepguh the board may be; walking further than LEAVE_RADIUS from its middle ends the game. */
    public static final int BOARD_RADIUS = 20, LEAVE_RADIUS = 14;
    public static final int MIKA_POINTS = 10, GOUD_POINTS = 50, GUH_PENALTY = 25, GUH_SPARED = 5;
    public static final int COMBO_STEP = 5, MAX_MULTIPLIER = 5, HIT_COOLDOWN = 3, BONK_TICKS = 7;
    /** One mepmunt per this many points (plus two for playing), at most MAX_COINS; the first game ever gives FIRST_BONUS extra. */
    public static final int COIN_POINTS = 300, MAX_COINS = 11, FIRST_BONUS = 4;
    /** Scores for the advancements. */
    public static final int GOOD = 500, GREAT = 1500, LEGEND = 2500;
    /** TAG_TOP marks the (invisible) spot for the top 3 in the hall; TAG_LIVE the live score board. */
    public static final String TAG_TOP = "guhs_mep_top", TAG_LIVE = "guhs_mep_live";
    /** The world's top 3 of Mika meppen (in {@link Scorebord}): medium; the other levels have _makkelijk / _lastig ({@link Niveau#board}). */
    public static final String BOARD = "meppen_score";

    /**
     * How a level plays (2.9): how fast new heads pop up (ticks between two, from the start to the end of the minute), how
     * long a head stays up, how often a guh decoy pops up instead of a Mika (it grows during the game), how often a golden
     * Mika, and how many heads can be up at once. Medium is the game as it always was.
     */
    public record Tempo(float spawnFrom, float spawnTo, float upFrom, float upTo, float guhFrom, float guhExtra, float gold, int maxFrom, int maxExtra) {
        public static Tempo of(Niveau niveau) {
            return switch (niveau) {
                case MAKKELIJK -> new Tempo(26f, 11f, 46f, 24f, 0.05f, 0.05f, 0.08f, 2, 2);
                case MEDIUM -> new Tempo(20f, 6f, 36f, 15f, 0.10f, 0.08f, 0.06f, 2, 4);
                case LASTIG -> new Tempo(15f, 4f, 28f, 11f, 0.16f, 0.12f, 0.03f, 3, 4);
            };
        }
    }
    private static final String BEST = "guhs_mep_best", FIRST = "guhs_mep_first", STASH = "guhs_mep_stash", STASH_SLOT = "guhs_mep_stash_slot";

    /** The games, by Mepguh. */
    private static final Map<UUID, MepGame> GAMES = new ConcurrentHashMap<>();
    /** Who is playing where: player -> Mepguh. */
    private static final Map<UUID, UUID> PLAYING = new ConcurrentHashMap<>();

    public static boolean isPlaying(Player player) {
        return PLAYING.containsKey(player.getUUID());
    }

    public static MepGame of(GuhNpcEntity npc) {
        MepGame game = GAMES.computeIfAbsent(npc.getUUID(), id -> new MepGame());
        if (game.npc != npc) {           // (a new entity: loaded again, or a fresh world in the same game session)
            ServerPlayer p = game.running && npc.level() instanceof ServerLevel world ? game.online(world) : null;
            if (p != null) {
                game.abort(p, "gone");         // (the mallet goes back, the item it replaced too)
            }
            if (game.npc != null && game.npc.level() != npc.level()) {
                game.holes = null;             // (the old board is in another (closed) world: don't touch it)
            }
            game.stopQuietly();
            game.npc = npc;
            game.holes = null;
            game.topPos = null;
        }
        return game;
    }

    /** A head on the board: what it is, when it goes down again, and (after a whack) when the squashed head disappears. */
    private static final class Head {
        final Kop kop;
        final int down;
        int bonkUntil = -1;

        Head(Kop kop, int down) {
            this.kop = kop;
            this.down = down;
        }
    }

    @Nullable
    private GuhNpcEntity npc;
    @Nullable
    private List<BlockPos> holes;
    @Nullable
    private BlockPos stand;
    /** Where the top 3 floats (the TAG_TOP marker in the hall; found again when the Mepguh is loaded again). */
    @Nullable
    private Vec3 topPos;
    private boolean running;
    @Nullable
    private UUID player;
    private String playerName = "";
    private int tick, nextSpawn, lastHit = -100;
    private int score, combo, bestCombo, mikas, golds, guhs, missed;
    /** The level of the game that runs (or ran last). */
    private Niveau niveau = Niveau.MEDIUM;
    private long lastTicked;
    private final Map<BlockPos, Head> heads = new LinkedHashMap<>();
    @Nullable
    private ServerBossEvent bar;
    /** Tests switch this off to pop heads themselves. */
    boolean autoSpawn = true;

    public boolean isRunning() {
        return running;
    }

    public int score() {
        return score;
    }

    public int combo() {
        return combo;
    }

    public int multiplier() {
        return Math.min(MAX_MULTIPLIER, 1 + combo / COMBO_STEP);
    }

    public int tickCount() {
        return tick;
    }

    public Niveau niveau() {
        return niveau;
    }

    public List<BlockPos> holes() {
        return holes == null ? List.of() : holes;
    }

    @Nullable
    public BlockPos stand() {
        return stand;
    }

    public int headsUp() {
        return heads.size();
    }

    // --- talking to the Mepguh -------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        MepGame game = of(npc);
        CompoundTag saved = GuhQuests.saved(player);
        boolean first = !saved.getBoolean(FIRST);
        String line = game.running ? (player.getUUID().equals(game.player) ? "running_you" : "running") : first ? "hello_first" : "hello";
        if (game.running && !player.getUUID().equals(game.player)) {
            GuhQuests.say(player, npc, "quest.guhs.mika_mep." + line, game.playerName, game.secondsLeft());
        } else {
            GuhQuests.say(player, npc, "quest.guhs.mika_mep." + line);
        }
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.running);
        data.putBoolean("You", game.running && player.getUUID().equals(game.player));
        data.putString("Player", game.playerName);
        data.putInt("Left", game.secondsLeft());
        data.putInt("Best", best(player));
        data.putInt("HallBest", hallBest(player.server));
        Klassiekers.records(data, n -> best(player, n));
        data.putString("Niveau", game.niveau.id());
        data.putInt("Coins", GuhQuests.count(player, MeppenFeature.MEPMUNT.get()));
        data.putBoolean("First", first);
        ModNetworking.sendTo(player, new MepPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.MEPGUH || player.distanceToSqr(npc) > 100) {
            return;
        }
        MepGame game = of(npc);
        Niveau niveau = Klassiekers.niveau(action);
        switch (Klassiekers.actie(action)) {
            case SHOP -> npc.openShop(player);
            case HELP -> {
                for (int i = 1; i <= 4; i++) {
                    GuhQuests.say(player, npc, "quest.guhs.mika_mep.help" + i);
                }
            }
            case STOP -> {
                if (game.running && player.getUUID().equals(game.player)) {
                    game.abort(player, "stopped");
                }
            }
            case START -> game.start(npc, player, niveau);
            default -> {
            }
        }
    }

    // --- playing -----------------------------------------------------------------------------------------------------------

    private void start(GuhNpcEntity npc, ServerPlayer p, Niveau level) {
        if (running) {
            GuhQuests.say(p, npc, p.getUUID().equals(player) ? "quest.guhs.mika_mep.running_you" : "quest.guhs.mika_mep.running",
                    playerName, secondsLeft());
            return;
        }
        if (isPlaying(p)) {                    // (still in a game at another board somehow)
            return;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(p, npc, nl.juiced.guhs.feature.Minigames.MEPPEN)) {
            return;
        }
        ServerLevel world = (ServerLevel) npc.level();
        findBoard(npc);
        if (holes.size() < 4 || stand == null) {
            GuhQuests.say(p, npc, "quest.guhs.mika_mep.broken");
            return;
        }
        clearBoard(world);
        running = true;
        niveau = level;
        player = p.getUUID();
        playerName = p.getGameProfile().getName();
        tick = 0;
        nextSpawn = COUNTDOWN + 8;
        lastHit = -100;
        score = combo = bestCombo = mikas = golds = guhs = missed = 0;
        lastTicked = world.getGameTime();
        PLAYING.put(p.getUUID(), npc.getUUID());
        takeBack(p);                                  // (leftovers of an earlier game, if any)
        giveHammer(p);
        nl.juiced.guhs.feature.Minigames.startKeeping(p);
        // on the middle of the board, looking down at it (away from the Mepguh)
        Vec3 c = Vec3.atBottomCenterOf(stand);
        float yaw = (float) (Mth.atan2(c.z - npc.getZ(), c.x - npc.getX()) * Mth.RAD_TO_DEG) - 90f;
        if (p instanceof FakePlayer) {
            p.moveTo(c.x, c.y, c.z, yaw, 40f);
        } else {
            p.teleportTo(world, c.x, c.y, c.z, yaw, 40f);
        }
        bar = new ServerBossEvent(Component.translatable("gui.guhs.mika_mep.bar.ready"), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setProgress(1f);
        bar.addPlayer(p);
        GuhQuests.say(p, npc, "quest.guhs.mika_mep.go");
        if (level != Niveau.MEDIUM) {
            GuhQuests.say(p, npc, "quest.guhs.klassiekers.meppen." + level.id());
        }
        updateLive(world);
    }

    /** Every tick (from the Mepguh). */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTicked = world.getGameTime();
        if (holes == null) {
            findBoard(npc);
            if (!running) {
                clearBoard(world);
                updateLive(world);
            }
        }
        if (npc.tickCount % 100 == 1) {
            showScores(npc);
        }
        if (running) {
            step(npc);
        } else if (npc.tickCount % 200 == 0) {
            updateLive(world);
        }
    }

    /** One tick of a running game. */
    void step(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        ServerPlayer p = online(world);
        if (p == null || !p.isAlive() || p.level() != world) {
            abort(p, "gone");
            return;
        }
        if (stand != null && horizontalDistSqr(p.position(), Vec3.atBottomCenterOf(stand)) > LEAVE_RADIUS * LEAVE_RADIUS) {
            abort(p, "left");
            return;
        }
        tick++;
        if (tick % 20 == 0) {
            refresh(p);
        }
        // no chests, chest boats or other containers while playing: the mallet can't be put away (the shop is fine)
        if (p.containerMenu != p.inventoryMenu && !(p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)) {
            p.closeContainer();
        }
        if (tick <= COUNTDOWN) {
            if (tick % 20 == 1) {
                int n = 3 - tick / 20;
                title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.mika_mep.ready").withStyle(ChatFormatting.WHITE), 18);
                sound(world, stand, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 0.9f + 0.15f * (3 - n));
            }
            if (tick == COUNTDOWN) {
                title(p, Component.translatable("gui.guhs.mika_mep.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.mika_mep.go.sub").withStyle(ChatFormatting.WHITE), 20);
                sound(world, stand, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
                sound(world, stand, SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.5f);
            }
            updateBar();
            return;
        }
        // heads going down: squashed ones, and the ones that got away
        var it = heads.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Head h = e.getValue();
            if (h.bonkUntil >= 0 ? tick >= h.bonkUntil : tick >= h.down) {
                if (h.bonkUntil < 0) {
                    escaped(world, p, e.getKey(), h);
                }
                removeHead(world, e.getKey());
                it.remove();
            }
        }
        int t = tick - COUNTDOWN;
        if (t >= LENGTH) {
            finish(npc, p);
            return;
        }
        float progress = (float) t / LENGTH;
        if (autoSpawn && tick >= nextSpawn) {
            spawn(world, p, progress);
            Tempo tempo = Tempo.of(niveau);
            nextSpawn = tick + Math.max(3, Math.round(Mth.lerp(progress, tempo.spawnFrom(), tempo.spawnTo())) + world.getRandom().nextInt(5) - 2);
        }
        if (t == LENGTH - 200) {
            title(p, Component.empty(), Component.translatable("gui.guhs.mika_mep.ten_left").withStyle(ChatFormatting.YELLOW), 20);
        }
        if (t > LENGTH - 100 && t % 20 == 0) {
            sound(world, stand, SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.6f);
        }
        if (tick % 5 == 0) {
            updateBar();
        }
        if (tick % 10 == 0) {
            updateLive(world);
        }
    }

    /** A head that wasn't whacked goes back down: a Mika got away (combo gone), a guh is safe (a few points). */
    private void escaped(ServerLevel world, ServerPlayer p, BlockPos pos, Head h) {
        if (h.kop == Kop.GUH) {
            score += GUH_SPARED;
            p.displayClientMessage(Component.translatable("gui.guhs.mika_mep.spared", GUH_SPARED).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            missed++;
            if (combo >= COMBO_STEP) {
                p.displayClientMessage(Component.translatable("gui.guhs.mika_mep.escaped").withStyle(ChatFormatting.GRAY), true);
            }
            combo = 0;
            sound(world, pos, ModSounds.MIKA_AMBIENT.get(), 0.5f, 1.6f);
        }
    }

    /** Pops a new head up out of a free hole (random: mostly Mikas, sometimes a golden one, sometimes a guh). */
    private void spawn(ServerLevel world, ServerPlayer p, float progress) {
        Tempo tempo = Tempo.of(niveau);
        int max = tempo.maxFrom() + (int) (progress * tempo.maxExtra());
        if (heads.size() >= max) {
            return;
        }
        List<BlockPos> free = new ArrayList<>();
        for (BlockPos hole : holes) {
            BlockPos pos = hole.above();
            if (!heads.containsKey(pos) && world.getBlockState(hole).is(MeppenFeature.MEP_GAT.get()) && world.getBlockState(pos).isAir()
                    && world.getEntitiesOfClass(Player.class, new AABB(pos)).isEmpty()) {
                free.add(hole);
            }
        }
        if (free.isEmpty()) {
            return;
        }
        float r = world.getRandom().nextFloat();
        Kop kop = r < tempo.gold() ? Kop.GOUD
                : progress > 0.08f && r < tempo.gold() + tempo.guhFrom() + tempo.guhExtra() * progress ? Kop.GUH : Kop.MIKA;
        int up = Math.round(Mth.lerp(progress, tempo.upFrom(), tempo.upTo())) + (kop == Kop.GUH ? 8 : kop == Kop.GOUD ? -3 : 0);
        pop(world, p, free.get(world.getRandom().nextInt(free.size())), kop, up);
    }

    /** Puts a head on a hole for this many ticks, looking at the player. */
    void pop(ServerLevel world, @Nullable ServerPlayer p, BlockPos hole, Kop kop, int up) {
        BlockPos pos = hole.above();
        Direction facing = Direction.NORTH;
        if (p != null) {
            double dx = p.getX() - (pos.getX() + 0.5), dz = p.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dz * dz > 0.01) {
                facing = Direction.getNearest(dx, 0, dz);
            }
        }
        world.setBlock(pos, MeppenFeature.MEP_KOP.get().defaultBlockState().setValue(MepKop.FACING, facing).setValue(MepKop.KOP, kop), 3);
        heads.put(pos, new Head(kop, tick + up));
        world.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.01);
        if (kop == Kop.GOUD) {
            world.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1f, 0.85f, 0.2f), 1.2f), pos.getX() + 0.5, pos.getY() + 0.6,
                    pos.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0);
            sound(world, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        } else if (kop == Kop.GUH) {
            sound(world, pos, ModSounds.GUH_AMBIENT.get(), 0.7f, 1.3f);
        } else {
            sound(world, pos, SoundEvents.CHICKEN_EGG, 0.6f, 0.8f + world.getRandom().nextFloat() * 0.4f);
        }
    }

    /** The player left-clicked a head or a hole of this board. */
    void hit(ServerPlayer p, BlockPos pos) {
        ServerLevel world = p.serverLevel();
        if (!running || !p.getUUID().equals(player)) {
            p.displayClientMessage(Component.translatable("gui.guhs.mika_mep.not_playing").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        if (tick <= COUNTDOWN) {
            return;
        }
        if (!p.getMainHandItem().is(MeppenFeature.MEP_HAMER.get())) {
            p.displayClientMessage(Component.translatable("gui.guhs.mika_mep.take_hammer").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (tick - lastHit < HIT_COOLDOWN) {
            return;
        }
        lastHit = tick;
        Head h = heads.get(pos);
        BlockPos headPos = pos;
        if (h == null) {                                          // the hole under a head counts as the head
            h = heads.get(pos.above());
            headPos = pos.above();
        }
        if (h == null) {                                          // an empty hole: BONK on wood, combo gone
            nl.juiced.guhs.feature.samen.SamenSpel.mis(p, "meppen"); // samen
            if (combo > 0) {
                p.displayClientMessage(Component.translatable("gui.guhs.mika_mep.miss").withStyle(ChatFormatting.GRAY), true);
            }
            combo = 0;
            sound(world, pos, SoundEvents.WOOD_HIT, 1f, 0.7f);
            return;
        }
        if (h.bonkUntil >= 0) {
            return;                                               // (already squashed)
        }
        h.bonkUntil = tick + BONK_TICKS;
        BlockState state = world.getBlockState(headPos);
        if (state.is(MeppenFeature.MEP_KOP.get())) {
            world.setBlock(headPos, state.setValue(MepKop.BONK, true), 3);
        }
        double x = headPos.getX() + 0.5, y = headPos.getY() + 0.5, z = headPos.getZ() + 0.5;
        if (h.kop == Kop.GUH) {
            nl.juiced.guhs.feature.samen.SamenSpel.mis(p, "meppen"); // samen
            guhs++;
            score = Math.max(0, score - GUH_PENALTY);
            combo = 0;
            world.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 0.3, z, 3, 0.3, 0.2, 0.3, 0);
            sound(world, headPos, ModSounds.GUH_HURT.get(), 1f, 1.2f);
            sound(world, headPos, SoundEvents.VILLAGER_NO, 0.6f, 1.4f);
            title(p, Component.translatable("gui.guhs.mika_mep.njeg").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.mika_mep.not_the_guh", GUH_PENALTY).withStyle(ChatFormatting.RED), 16);
        } else {
            nl.juiced.guhs.feature.samen.SamenSpel.goed(p, "meppen"); // samen
            int before = multiplier();
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            int mult = multiplier();
            int points = (h.kop == Kop.GOUD ? GOUD_POINTS : MIKA_POINTS) * mult;
            score += points;
            sound(world, headPos, SoundEvents.SLIME_SQUISH, 1f, 1.3f);
            sound(world, headPos, ModSounds.MIKA_HURT.get(), 0.8f, 1.2f + world.getRandom().nextFloat() * 0.3f);
            world.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.3, 0.2, 0.3, 0.2);
            if (h.kop == Kop.GOUD) {
                golds++;
                GuhAdvancements.grant(p, "mika_meppen_goud");
                world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.3, z, 25, 0.3, 0.3, 0.3, 0.3);
                sound(world, headPos, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.8f);
                title(p, Component.translatable("gui.guhs.mika_mep.gold").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.literal("+" + points).withStyle(ChatFormatting.YELLOW), 14);
            } else {
                mikas++;
            }
            if (mult > before) {
                sound(world, headPos, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 0.6f + 0.2f * mult);
                world.sendParticles(ParticleTypes.HEART, x, y + 0.6, z, 3, 0.3, 0.2, 0.3, 0);
            }
            if (mult >= MAX_MULTIPLIER) {
                GuhAdvancements.grant(p, "mika_meppen_combo");
            }
            p.displayClientMessage(Component.translatable(mult > before ? "gui.guhs.mika_mep.combo_up" : "gui.guhs.mika_mep.hit",
                    points, combo, mult).withStyle(mult > before ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE), true);
        }
        updateBar();
    }

    /** Time's up: mepmunten, the record, the scoreboard, advancements. */
    private void finish(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        int endScore = score;
        stopQuietly();
        takeBack(p);
        CompoundTag saved = GuhQuests.saved(p);
        // no mepmunten for just standing there (letting guhs go): you have to whack at least one Mika
        int coins = niveau.munten(coins(endScore, mikas + golds));
        boolean first = !saved.getBoolean(FIRST);
        title(p, Component.translatable("gui.guhs.mika_mep.time").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.mika_mep.score", endScore).withStyle(ChatFormatting.WHITE), 50);
        p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.end", endScore, mikas, golds, guhs, bestCombo).withStyle(ChatFormatting.GOLD));
        if (first) {
            saved.putBoolean(FIRST, true);
            coins += FIRST_BONUS;
            give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 8));
            p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.first", FIRST_BONUS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (coins > 0) {
            give(p, new ItemStack(MeppenFeature.MEPMUNT.get(), coins));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.coins", coins).withStyle(ChatFormatting.YELLOW));
        int best = best(p, niveau);
        if (endScore > best) {
            saved.putInt(Klassiekers.sleutel(BEST, niveau), endScore);
            p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.record", endScore).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            if (best > 0) {
                sound(world, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            }
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.best", best).withStyle(ChatFormatting.GRAY));
        }
        GuhAdvancements.grant(p, "mika_meppen_gespeeld");
        if (endScore >= GOOD) {
            GuhAdvancements.grant(p, "mika_meppen_500");
        }
        if (endScore >= GREAT) {
            GuhAdvancements.grant(p, "mika_meppen_1500");
        }
        if (endScore >= LEGEND) {
            GuhAdvancements.grant(p, "mika_meppen_2500");
        }
        // the world's top 3: only real games (at least one Mika whacked) count
        if (mikas + golds > 0) {
            Scorebord.submit(p, niveau.board(BOARD), endScore, false);
            Klassiekers.gespeeld(p, "meppen", niveau);
        }
        showScores(npc);
        updateLive(world);
        sound(world, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, 0.9f, 1.2f);
        sound(world, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        world.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 2.2, p.getZ(), 40, 0.6, 0.4, 0.6, 0.08);
        score = endScore;
    }

    /** Ends the game without a reward (walked away, stopped, died, logged out...). */
    void abort(@Nullable ServerPlayer p, String why) {
        stopQuietly();
        if (p != null) {
            takeBack(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.mika_mep.stop." + why).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (p.connection != null) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            }
        }
        if (npc != null && npc.level() instanceof ServerLevel world) {
            updateLive(world);
        }
    }

    /** Stops the game: board cleared, bar gone, nobody playing (nothing given or taken). */
    private void stopQuietly() {
        if (npc != null && npc.level() instanceof ServerLevel world && holes != null) {
            clearBoard(world);
        }
        heads.clear();
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
        if (player != null) {
            PLAYING.remove(player);
        }
        running = false;
    }

    /** For tests (and the end of time): the game ends now as if the time was up. */
    void finishNow(GuhNpcEntity npc) {
        ServerPlayer p = online((ServerLevel) npc.level());
        if (running && p != null) {
            finish(npc, p);
        }
    }

    private int secondsLeft() {
        return running ? Math.max(0, (COUNTDOWN + LENGTH - tick + 19) / 20) : 0;
    }

    private void updateBar() {
        if (bar == null) {
            return;
        }
        if (tick <= COUNTDOWN) {
            bar.setName(Component.translatable("gui.guhs.mika_mep.bar.ready"));
            bar.setProgress(1f);
            return;
        }
        int t = tick - COUNTDOWN;
        bar.setProgress(Mth.clamp(1f - (float) t / LENGTH, 0f, 1f));
        bar.setName(Component.translatable("gui.guhs.mika_mep.bar", score, multiplier(), secondsLeft()).append("  ").append(Klassiekers.naam(niveau)));
        bar.setColor(multiplier() >= MAX_MULTIPLIER ? BossEvent.BossBarColor.YELLOW : multiplier() > 1 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.PINK);
    }

    // --- the board -----------------------------------------------------------------------------------------------------

    /** Finds the holes around the Mepguh; the player stands in the middle of them. */
    private void findBoard(GuhNpcEntity npc) {
        holes = new ArrayList<>();
        stand = null;
        BlockPos c = npc.blockPosition();
        List<BlockPos> all = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-BOARD_RADIUS, -6, -BOARD_RADIUS), c.offset(BOARD_RADIUS, 6, BOARD_RADIUS))) {
            if (npc.level().getBlockState(pos).is(MeppenFeature.MEP_GAT.get())) {
                all.add(pos.immutable());
            }
        }
        if (all.isEmpty()) {
            return;
        }
        // the board is the group of holes (at most 2 apart) closest to the Mepguh: another board nearby isn't ours
        all.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(c)));
        holes.add(all.remove(0));
        for (int i = 0; i < holes.size(); i++) {
            BlockPos h = holes.get(i);
            for (var it = all.iterator(); it.hasNext(); ) {
                BlockPos p = it.next();
                if (p.getY() == h.getY() && Math.abs(p.getX() - h.getX()) <= 2 && Math.abs(p.getZ() - h.getZ()) <= 2) {
                    holes.add(p);
                    it.remove();
                }
            }
        }
        double x = 0, z = 0;
        int y = Integer.MIN_VALUE;
        for (BlockPos h : holes) {
            x += h.getX();
            z += h.getZ();
            y = Math.max(y, h.getY());
        }
        stand = BlockPos.containing(x / holes.size(), y + 1, z / holes.size());
    }

    /** No heads left on the board (heads we don't know of too: from before a restart). */
    private void clearBoard(ServerLevel world) {
        for (BlockPos hole : holes()) {
            removeHead(world, hole.above());
        }
    }

    private static void removeHead(ServerLevel world, BlockPos pos) {
        if (world.isLoaded(pos) && world.getBlockState(pos).is(MeppenFeature.MEP_KOP.get())) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // --- the scoreboard wall (the live board and the top 3) ---------------------------------------------------------------

    private List<Display.TextDisplay> displays(ServerLevel world, String tag) {
        if (npc == null) {
            return List.of();
        }
        return world.getEntitiesOfClass(Display.TextDisplay.class, npc.getBoundingBox().inflate(48, 20, 48), d -> d.getTags().contains(tag));
    }

    private static void setText(Display.TextDisplay display, Component text) {
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        tag.putString("text", Component.Serializer.toJson(text, display.registryAccess()));
        display.load(tag);
    }

    /** The live board above the stage: the score while playing, an invitation otherwise. */
    private void updateLive(ServerLevel world) {
        MutableComponent text;
        if (running) {
            text = Component.literal(playerName).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(" (").append(niveau.naam()).append(")").withStyle(Klassiekers.kleur(niveau))).append("\n")
                    .append(Component.translatable("gui.guhs.mika_mep.live.score", score).withStyle(ChatFormatting.WHITE)).append("\n")
                    .append(Component.translatable("gui.guhs.mika_mep.live.combo", multiplier(), secondsLeft()).withStyle(ChatFormatting.YELLOW));
        } else {
            text = Component.translatable("gui.guhs.mika_mep.live.title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD).append("\n")
                    .append(Component.translatable("gui.guhs.mika_mep.live.idle").withStyle(ChatFormatting.WHITE));
        }
        for (Display.TextDisplay d : displays(world, TAG_LIVE)) {
            setText(d, text);
        }
    }

    /** The world's top 3 floats at the scoreboard wall (or above the Mepguh when the hall has no spot for it). */
    public static void showScores(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel world)) {
            return;
        }
        MepGame game = of(npc);
        if (game.topPos == null) {
            game.topPos = game.displays(world, TAG_TOP).stream().min(java.util.Comparator.comparingDouble(d -> d.distanceToSqr(npc)))
                    .map(Display.TextDisplay::position).orElse(npc.position().add(0, 2.6, 0));
        }
        Scorebord.show(world, game.topPos, "meppen", Klassiekers.bord(world.getServer(), Component.translatable("gui.guhs.scorebord.meppen"),
                BOARD, MepGame::points));
    }

    /** Mepmunten for a game: none without whacking a single Mika (no AFK farming), else 2 + one per COIN_POINTS points. */
    public static int coins(int score, int whacked) {
        return whacked <= 0 ? 0 : Math.min(MAX_COINS, 2 + Math.max(0, score) / COIN_POINTS);
    }

    /** A score on the board: "230 pt". */
    public static String points(int score) {
        return score + " pt";
    }

    /** The best score of the whole world (the top of the scoreboard), 0 when nobody played yet. */
    public static int hallBest(net.minecraft.server.MinecraftServer server) {
        List<Scorebord.Entry> top = Scorebord.top(server, BOARD);
        return top.isEmpty() ? 0 : top.get(0).score();
    }

    public static int best(Player player) {
        return GuhQuests.saved(player).getInt(BEST);
    }

    /** Your best score on this level (medium = the old record). */
    public static int best(Player player, Niveau niveau) {
        return GuhQuests.saved(player).getInt(Klassiekers.sleutel(BEST, niveau));
    }

    // --- the loaned mallet ---------------------------------------------------------------------------------------------

    /** The mallet goes in your hand: in the selected slot if it's free, else a free hotbar slot, else the item there is kept safe. */
    private static void giveHammer(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int slot = inv.getItem(inv.selected).isEmpty() ? inv.selected : -1;
        for (int i = 0; i < 9 && slot < 0; i++) {
            if (inv.getItem(i).isEmpty()) {
                slot = i;
            }
        }
        if (slot < 0) {
            slot = inv.selected;
            CompoundTag saved = GuhQuests.saved(p);
            saved.put(STASH, inv.getItem(slot).save(p.registryAccess()));
            saved.putInt(STASH_SLOT, slot);
        }
        inv.setItem(slot, new ItemStack(MeppenFeature.MEP_HAMER.get()));
        inv.selected = slot;
        if (p.connection != null && !(p instanceof FakePlayer)) {
            p.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
    }

    /** Takes the mallet back (everywhere in the inventory) and gives back what it replaced. */
    static void takeBack(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(MeppenFeature.MEP_HAMER.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (p.containerMenu.getCarried().is(MeppenFeature.MEP_HAMER.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.contains(STASH)) {
            ItemStack stack = ItemStack.parseOptional(p.registryAccess(), saved.getCompound(STASH));
            int slot = saved.getInt(STASH_SLOT);
            saved.remove(STASH);
            saved.remove(STASH_SLOT);
            if (!stack.isEmpty()) {
                if (inv.getItem(slot).isEmpty()) {
                    inv.setItem(slot, stack);
                } else {
                    give(p, stack);
                }
            }
        }
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    /** Whacking: a left-click on a head or a hole. The board itself never breaks (except for builders in creative mode). */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        boolean head = state.is(MeppenFeature.MEP_KOP.get());
        if (!head && !state.is(MeppenFeature.MEP_GAT.get())) {
            return;
        }
        if (p.getAbilities().instabuild && !isPlaying(p) && !head) {
            return;                                                  // builders may move holes around
        }
        event.setCanceled(true);
        MepGame game = gameAt(event.getPos());
        if (game != null) {
            game.hit(p, event.getPos());
        } else if (head) {
            removeHead(p.serverLevel(), event.getPos());             // (a leftover head nobody knows)
        }
    }

    @Nullable
    private static MepGame gameAt(BlockPos pos) {
        for (MepGame game : GAMES.values()) {
            if (game.holes != null && (game.holes.contains(pos) || game.holes.contains(pos.below()))) {
                return game;
            }
        }
        return null;
    }

    /** Players can't get hurt while playing. */
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isPlaying(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** The mallet can't be thrown away: while playing it jumps back into your hand, else it's just gone. */
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!stack.is(MeppenFeature.MEP_HAMER.get())) {
            return;
        }
        event.setCanceled(true);
        if (isPlaying(event.getPlayer()) && event.getPlayer() instanceof ServerPlayer p) {
            if (!p.getInventory().add(stack.copy())) {
                p.getInventory().setItem(p.getInventory().selected, stack.copy());
            }
        }
    }

    /** The mallet can't be handed to entities (item frames, armor stands, allays, pets...); talking to the Mepguh is fine. */
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().is(MeppenFeature.MEP_HAMER.get()) && !(event.getTarget() instanceof GuhNpcEntity)) {
            event.setCanceled(true);
        }
    }

    public static void onInteractEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getItemStack().is(MeppenFeature.MEP_HAMER.get()) && !(event.getTarget() instanceof GuhNpcEntity)) {
            event.setCanceled(true);
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> item.getItem().is(MeppenFeature.MEP_HAMER.get()));
    }

    /** Dying ends the game (the mallet goes back, the item it replaced drops with the rest). */
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p, "gone");
        }
    }

    /** The server stops (singleplayer: back to the title screen): forget every game, and the worlds they were in. */
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        PLAYING.clear();
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            if (isPlaying(p)) {
                stopFor(p, "gone");
            } else {
                takeBack(p);
            }
        }
    }

    /** Leftovers of a game that ended while you were gone (a crash...): the mallet goes, your item comes back. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !isPlaying(p)) {
            takeBack(p);
        }
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p, "gone");
        }
    }

    /** Every second: players in a game that doesn't tick any more (its Mepguh unloaded) are let go. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && isPlaying(p)) {
            MepGame game = GAMES.get(PLAYING.get(p.getUUID()));
            if (game == null || !game.running || p.serverLevel().getGameTime() - game.lastTicked > 60) {
                if (game != null && game.running) {
                    game.abort(p, "gone");
                } else {
                    PLAYING.remove(p.getUUID());
                    takeBack(p);
                }
            }
        }
    }

    private static void stopFor(ServerPlayer p, String why) {
        UUID npcId = PLAYING.get(p.getUUID());
        MepGame game = npcId == null ? null : GAMES.get(npcId);
        if (game != null && game.running) {
            game.abort(p, why);
        } else {
            PLAYING.remove(p.getUUID());
            takeBack(p);
        }
    }

    // --- helpers -----------------------------------------------------------------------------------------------------------

    @Nullable
    private ServerPlayer online(ServerLevel world) {
        return player == null ? null : world.getServer().getPlayerList().getPlayer(player);
    }

    /** The player can't get hungry or hurt while playing. */
    private static void refresh(ServerPlayer p) {
        nl.juiced.guhs.feature.Minigames.keep(p);    // (no hungrier or weaker than at the start: no free healing)
    }

    private static double horizontalDistSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static void sound(ServerLevel world, @Nullable BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (pos != null) {
            world.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    private static void title(ServerPlayer p, Component title, Component subtitle, int stay) {
        if (p.connection == null || p instanceof FakePlayer) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, stay, 6));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }

    private MepGame() {
    }
}
