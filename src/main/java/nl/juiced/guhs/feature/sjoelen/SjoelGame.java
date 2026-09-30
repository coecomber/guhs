package nl.juiced.guhs.feature.sjoelen;

import java.util.HashMap;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Guh-sjoelen with Opoe Njegschuif in the Sjoelhuisje: one player at a time (the others watch from the benches). Opoe
 * lends you 20 sjoelschijven; you stand at the head of the big sjoelbak and slide them one by one (hold right-click for
 * the power bar, look where it should go, let go). The pucks really slide ({@link SjoelBak}): they bounce off the rims,
 * bump into each other, stop short or slip through one of the four gates 2-3-4-1. When all 20 lie still, Opoe counts:
 * every complete set (one in each gate) is 20 points, the rest counts per gate, at most 100. Sjoelschijfjes: 1 for a
 * whole turn, +1 for every complete set, +1 for beating your own record ({@link #munten}); the world's top 3 floats
 * above Opoe and above the gates (board {@value #BOARD}). While you play you can't get hurt or hungry.
 * <p>
 * Games live in memory (a restart ends them). The bak is found once by scanning around Opoe for its head blocks
 * ({@code sjoelen_kop}) and saved in her roleData.
 */
public final class SjoelGame {
    public static final int PUCKS = 20;
    /** Actions from Opoe's screen. */
    public static final int START = 0, SHOP = 1, STOP = 2;
    public static final int COUNTDOWN = 61, SETTLE_LIMIT = 20 * 15, IDLE_LIMIT = 20 * 90, SLIDE_COOLDOWN = 8, TALLY_TICKS = 70;
    /** How far around Opoe the bak may be, and when you've walked away. */
    public static final int SCAN_RADIUS = 24, LEAVE_RADIUS = 30;
    /** Where you have to stand to slide (in the bak's frame): behind or at the head, not beside the bak. */
    public static final double STAND_U_MIN = -3.5, STAND_U_MAX = 1.3, STAND_V_MARGIN = 1.6;
    public static final String BOARD = "sjoelen";
    public static final int SCORES_TICKS = 100;
    static final String BEST = "guhs_sjoelen_best", GAMES_KEY = "guhs_sjoelen_potjes", FIRST = "guhs_sjoelen_first";

    public enum Phase { IDLE, COUNTDOWN, PLAYING, SETTLING, TALLY }

    private static final Map<UUID, SjoelGame> GAMES = new ConcurrentHashMap<>();
    /** Everyone playing right now: player -> their Opoe. */
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();
    /** A game whose Opoe hasn't ticked for this long is over. */
    private static final int STALE_TICKS = 60;

    private final UUID npcId;
    @Nullable
    private UUID player;
    private String playerName = "";
    private Phase phase = Phase.IDLE;
    private int timer, thrown;
    private long lastSlide, lastTick, quietUntil;
    private final SjoelBak bak = new SjoelBak();
    /** puck id -> its entity. */
    private final Map<Integer, UUID> entities = new HashMap<>();
    /** puck id -> the gate it was in last tick (-1 none), for the "klak!" when it goes in. */
    private final Map<Integer, Integer> inGate = new HashMap<>();
    private int[] result = new int[4];

    // the bak, found once
    private boolean scanned;
    @Nullable
    private BlockPos kop;
    private Direction facing = Direction.NORTH;
    @Nullable
    private BlockPos home;

    private SjoelGame(UUID npcId) {
        this.npcId = npcId;
    }

    public static SjoelGame of(GuhNpcEntity npc) {
        SjoelGame game = GAMES.computeIfAbsent(npc.getUUID(), SjoelGame::new);
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

    public int thrown() {
        return thrown;
    }

    public SjoelBak bak() {
        return bak;
    }

    @Nullable
    public BlockPos kop() {
        return kop;
    }

    public Direction facing() {
        return facing;
    }

    /** Does a running game own this puck entity? */
    boolean owns(SjoelSchijfEntity e) {
        return isRunning() && entities.containsValue(e.getUUID());
    }

    // --- the bak's frame -----------------------------------------------------------------------------------------------

    private Direction right() {
        return facing.getClockWise();
    }

    /** The world point of (u, v) on the bak's surface. */
    public Vec3 world(double u, double v) {
        Direction r = right();
        double ox = kop.getX() + 0.5 - facing.getStepX() * 0.5 - r.getStepX() * 0.5;
        double oz = kop.getZ() + 0.5 - facing.getStepZ() * 0.5 - r.getStepZ() * 0.5;
        return new Vec3(ox + facing.getStepX() * u + r.getStepX() * v, kop.getY() + 1.0, oz + facing.getStepZ() * u + r.getStepZ() * v);
    }

    /** (u, v) of a world point. */
    public double[] local(Vec3 pos) {
        Vec3 o = world(0, 0);
        Direction r = right();
        double dx = pos.x - o.x, dz = pos.z - o.z;
        return new double[] {dx * facing.getStepX() + dz * facing.getStepZ(), dx * r.getStepX() + dz * r.getStepZ()};
    }

    // --- talking to Opoe -----------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        SjoelGame game = of(npc);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.9f);
        String line = game.isPlayedBy(player) ? "playing" : game.isRunning() ? "busy" : "hello" + (1 + npc.getRandom().nextInt(4));
        GuhQuests.say(player, npc, "quest.guhs.sjoelen." + line, game.playerName);
        SjoelenFeature.checkKleding(player);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        data.putBoolean("Mine", game.isPlayedBy(player));
        data.putString("Player", game.playerName);
        data.putInt("Left", PUCKS - game.thrown);
        data.putInt("Best", best(player));
        data.putInt("Games", GuhQuests.saved(player).getIntOr(GAMES_KEY, 0));
        List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), BOARD);
        data.putString("RecordName", top.isEmpty() ? "" : top.get(0).name());
        data.putInt("RecordScore", top.isEmpty() ? -1 : top.get(0).score());
        ModNetworking.sendTo(player, new SjoelenPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.SJOELGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        SjoelGame game = of(npc);
        if (action == SHOP) {
            npc.openShop(player);
        } else if (action == STOP && game.isPlayedBy(player)) {
            game.end(npc, player, "quest.guhs.sjoelen.stopped", false);
        } else if (action == START) {
            game.start(npc, player);
        }
    }

    // --- a turn ------------------------------------------------------------------------------------------------------------

    private void start(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.sjoelen.busy", playerName);
            return;
        }
        if (isPlaying(p)) {
            GuhQuests.say(p, npc, "quest.guhs.sjoelen.elsewhere");
            return;
        }
        if (Minigames.refuse(p, npc, Minigames.SJOELEN)) {
            return;
        }
        if (scanned && !intact(world)) {
            scanned = false;
        }
        if (!scan(npc, true) || !intact(world)) {
            GuhQuests.say(p, npc, "quest.guhs.sjoelen.broken");
            return;
        }
        if (p.getInventory().getFreeSlot() < 0 && !p.getMainHandItem().isEmpty()) {
            GuhQuests.say(p, npc, "quest.guhs.sjoelen.full");
            return;
        }
        player = p.getUUID();
        playerName = p.getGameProfile().name();
        PLAYERS.put(player, npcId);
        clearPucks(world);
        thrown = 0;
        result = new int[4];
        lastSlide = world.getGameTime();
        lastTick = world.getGameTime();
        giveStack(p, PUCKS);
        Minigames.startKeeping(p);
        p.clearFire();
        Vec3 spot = world(-1.6, SjoelBak.WIDTH / 2);
        p.teleportTo(world, spot.x, spot.y - 1.0, spot.z, java.util.Set.of(), facing.toYRot(), 12f, true);
        phase = Phase.COUNTDOWN;
        timer = COUNTDOWN;
        GuhQuests.say(p, npc, "quest.guhs.sjoelen.start");
        p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.how").withStyle(ChatFormatting.GRAY));
    }

    /** Let go of the pucks: a slide (if it's your turn, you stand at the head and the last puck is on its way). */
    public static void slide(ServerPlayer p, float power) {
        UUID npc = PLAYERS.get(p.getUUID());
        SjoelGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.doSlide(p, power);
        }
    }

    /** As {@link #slide}, with the aim given (radians off the bak's axis, + = right) instead of the look (tests). */
    public boolean doSlide(ServerPlayer p, float power) {
        Vec3 look = p.getLookAngle();
        Direction r = right();
        double along = look.x * facing.getStepX() + look.z * facing.getStepZ(), side = look.x * r.getStepX() + look.z * r.getStepZ();
        if (along < 0.15) {
            flash(p, Component.translatable("gui.guhs.sjoelen.look").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        return slideWith(p, power, Math.atan2(side, along));
    }

    public boolean slideWith(ServerPlayer p, float power, double aim) {
        ServerLevel world = p.level();
        if (phase != Phase.PLAYING) {
            flash(p, Component.translatable(phase == Phase.COUNTDOWN ? "gui.guhs.sjoelen.wait_countdown" : "gui.guhs.sjoelen.all_gone")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (thrown >= PUCKS) {
            return false;
        }
        double[] at = local(p.position());
        if (at[0] < STAND_U_MIN || at[0] > STAND_U_MAX || at[1] < -STAND_V_MARGIN || at[1] > SjoelBak.WIDTH + STAND_V_MARGIN) {
            flash(p, Component.translatable("gui.guhs.sjoelen.stand").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (world.getGameTime() - lastSlide < SLIDE_COOLDOWN || blocked()) {
            flash(p, Component.translatable("gui.guhs.sjoelen.wait_puck").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        SjoelBak.Puck puck = bak.slide(at[1], SjoelBak.speed(power), SjoelBak.angle(aim));
        Vec3 pos = world(puck.u, puck.v);
        thrown++;
        SjoelSchijfEntity e = SjoelSchijfEntity.create(world, this, puck.id, pos.x, pos.y, pos.z, thrown == PUCKS ? 1 : 0);
        entities.put(puck.id, e.getUUID());
        inGate.put(puck.id, -1);
        lastSlide = world.getGameTime();
        setStack(p, PUCKS - thrown);
        world.playSound(null, BlockPos.containing(pos), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.8f, 1.4f);
        world.playSound(null, BlockPos.containing(pos), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f + power * 0.4f, 0.7f);
        if (power >= SjoelSchijvenItem.GOED_VAN && power <= SjoelSchijvenItem.GOED_TOT) {
            flash(p, Component.translatable("gui.guhs.sjoelen.good").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** The last puck still lies (or crawls) right at the head: wait for it. */
    private boolean blocked() {
        for (SjoelBak.Puck q : bak.pucks()) {
            if (q.moving && q.u < 3) {
                return true;
            }
        }
        return false;
    }

    /** Every tick of Opoe: the countdown, the sliding pucks, counting, and players who wandered off. */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        if (npc.tickCount % SCORES_TICKS == 1) {
            if (!scanned && world.getNearestPlayer(npc, 32) != null) {
                scan(npc, false);
            }
            showScores(npc);
        }
        if (npc.tickCount % 40 == 0) {
            for (ServerPlayer near : world.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(12))) {
                SjoelenFeature.checkKleding(near);
            }
        }
        if (player == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || horizontal(p, npc) > LEAVE_RADIUS) {
            if (p != null && p.isAlive() && p.level() == world) {
                end(npc, p, "quest.guhs.sjoelen.walked_away", false);
            } else {
                if (p != null) {
                    removeStacks(p);
                }
                end(npc, null, null, false);
            }
            return;
        }
        if (npc.tickCount % 20 == 0) {
            Minigames.keep(p);
            p.clearFire();
            int left = PUCKS - thrown;
            if ((phase == Phase.PLAYING || phase == Phase.COUNTDOWN) && left > 0 && !p.getInventory().hasAnyMatching(s -> s.is(SjoelenFeature.SCHIJVEN.get()))
                    && p.getInventory().getFreeSlot() >= 0) {
                giveStack(p, left);                                // (lost them somehow? here they are again)
            }
        }
        physics(world);
        switch (phase) {
            case COUNTDOWN -> {
                timer--;
                if (timer == 60 || timer == 40 || timer == 20) {
                    title(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.sjoelen.countdown_sub"), 0, 22, 0);
                    notifySound(p,SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.9f, 1.0f);
                }
                if (timer <= 0) {
                    phase = Phase.PLAYING;
                    title(p, Component.translatable("gui.guhs.sjoelen.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.sjoelen.go_sub"), 0, 30, 10);
                    notifySound(p,SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 2.0f);
                    npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 0.9f);
                }
            }
            case PLAYING -> {
                if (thrown >= PUCKS) {
                    phase = Phase.SETTLING;
                    timer = SETTLE_LIMIT;
                } else if (world.getGameTime() - lastSlide > IDLE_LIMIT) {
                    end(npc, p, "quest.guhs.sjoelen.idle", false);
                    return;
                }
            }
            case SETTLING -> {
                if (bak.allStill() || --timer <= 0) {
                    tally(npc, p);
                }
            }
            case TALLY -> {
                if (--timer <= 0) {
                    end(npc, p, null, true);
                    return;
                }
            }
            default -> {
            }
        }
        if (isRunning() && npc.tickCount % 10 == 0 && phase != Phase.COUNTDOWN && phase != Phase.TALLY && world.getGameTime() >= quietUntil) {
            int[] c = bak.counts();
            p.sendOverlayMessage(Component.translatable("gui.guhs.sjoelen.bar", PUCKS - thrown, c[0], c[1], c[2], c[3], SjoelBak.score(c))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Moves the pucks one tick and puts their entities where they are. */
    private void physics(ServerLevel world) {
        if (bak.allStill() || kop == null) {
            return;
        }
        int knocks = bak.tick();
        for (SjoelBak.Puck q : bak.pucks()) {
            UUID id = entities.get(q.id);
            if (id != null && world.getEntity(id) instanceof SjoelSchijfEntity e) {
                Vec3 pos = world(q.u, q.v);
                e.setPos(pos.x, pos.y, pos.z);
                e.draai(q.spin);
                if (q.moving && world.getGameTime() % 5 == 0) {
                    world.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.05, pos.z, 1, 0.05, 0, 0.05, 0.0);
                }
            }
            int gate = SjoelBak.gate(q);
            Integer before = inGate.put(q.id, gate);
            if (gate >= 0 && (before == null || before != gate)) {
                Vec3 pos = world(q.u, q.v);
                world.playSound(null, BlockPos.containing(pos), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1f, 1.6f);
                world.playSound(null, BlockPos.containing(pos), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f,
                        0.8f + 0.2f * SjoelBak.VALUES[gate]);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.3, pos.z, 4, 0.15, 0.1, 0.15, 0);
            }
        }
        if (knocks > 0) {
            Vec3 mid = world(SjoelBak.BAR - 2, SjoelBak.WIDTH / 2);
            world.playSound(null, BlockPos.containing(mid), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.7f, 1.7f);
        }
    }

    /** All pucks lie still: Opoe counts. */
    private void tally(GuhNpcEntity npc, ServerPlayer p) {
        nl.juiced.guhs.feature.samen.SamenSpel.goed(p, "sjoelen"); // samen
        ServerLevel world = (ServerLevel) npc.level();
        for (SjoelBak.Puck q : bak.pucks()) {
            q.du = q.dv = 0;
            q.moving = false;
        }
        result = bak.counts();
        int sets = SjoelBak.sets(result), score = SjoelBak.score(result);
        CompoundTag saved = GuhQuests.saved(p);
        int best = best(p);
        boolean record = best >= 0 && score > best;
        if (best < 0 || score > best) {
            saved.putInt(BEST, score);
        }
        saved.putInt(GAMES_KEY, saved.getIntOr(GAMES_KEY, 0) + 1);
        int munten = munten(sets, record);
        MutableComponent line = Component.translatable("quest.guhs.sjoelen.count").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        p.sendSystemMessage(line);
        for (int k = 0; k < 4; k++) {
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.count_gate", SjoelBak.VALUES[k], result[k]).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.total", sets, sets * 20, score - sets * 20, score).withStyle(ChatFormatting.YELLOW));
        if (record) {
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.record", score, best).withStyle(ChatFormatting.GOLD));
        } else if (best < 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.first_record", score).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.no_record", best).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.munten", munten).withStyle(ChatFormatting.LIGHT_PURPLE));
        Minigames.give(p, new ItemStack(SjoelenFeature.SJOELSCHIJFJE.get(), munten));
        if (!saved.getBooleanOr(FIRST, false)) {                                  // the very first turn: a present from Opoe
            saved.putBoolean(FIRST, true);
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            Minigames.give(p, new ItemStack(SjoelenFeature.STAPEL_ITEM.get(), 1));
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.first").withStyle(ChatFormatting.GOLD));
        }
        SjoelenFeature.advancement(p, "sjoelen_gespeeld");
        if (score >= 60) {
            SjoelenFeature.advancement(p, "sjoelen_zestig");
        }
        if (score >= 100) {
            SjoelenFeature.advancement(p, "sjoelen_honderd");
        }
        if (Scorebord.submit(p, BOARD, score, false) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.sjoelen.huis_record").withStyle(ChatFormatting.GOLD));
        }
        showScores(npc);
        String titleKey = score >= 100 ? "gui.guhs.sjoelen.done_100" : score >= 60 ? "gui.guhs.sjoelen.done_good" : "gui.guhs.sjoelen.done";
        title(p, Component.translatable(titleKey, score).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.sjoelen.done_sub", sets, munten), 5, 60, 15);
        notifySound(p,score >= 60 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.1f);
        npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, score >= 60 ? 1.3f : 1.0f);
        Vec3 gates = world(SjoelBak.BAR + 1.5, SjoelBak.WIDTH / 2);
        world.sendParticles(score >= 60 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER, gates.x, gates.y + 1, gates.z, 30, 1.5, 0.6, 1, 0.1);
        GuhQuests.say(p, npc, "quest.guhs.sjoelen.opoe_" + (score >= 100 ? "100" : score >= 60 ? "goed" : score >= 30 ? "aardig" : "oefenen"));
        removeStacks(p);
        phase = Phase.TALLY;
        timer = TALLY_TICKS;
    }

    /** Sjoelschijfjes for a turn: 1 for playing it, +1 for every complete set (0-5), +1 for beating your own record. */
    public static int munten(int sets, boolean record) {
        return 1 + Math.max(0, Math.min(5, sets)) + (record ? 1 : 0);
    }

    /** The game ends: pucks back into Opoe's box. After a whole turn you're back next to her. */
    private void end(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String message, boolean backHome) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null) {
            removeStacks(p);
            if (message != null) {
                p.sendSystemMessage(Component.translatable(message).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (backHome && home != null && p.level() == world && p.isAlive()) {
                p.teleportTo(world, home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), p.getYRot(), 0, true);
            }
        }
        clearPucks(world);
        if (player != null) {
            PLAYERS.remove(player, npcId);
        }
        player = null;
        phase = Phase.IDLE;
    }

    private void clearPucks(ServerLevel world) {
        for (UUID id : entities.values()) {
            if (world.getEntity(id) instanceof SjoelSchijfEntity e) {
                e.discard();
            }
        }
        entities.clear();
        inGate.clear();
        bak.clear();
    }

    /** A player logged out, died or went to another dimension: the turn is over (the pucks go by themselves). */
    public static void leave(ServerPlayer p) {
        UUID npc = PLAYERS.remove(p.getUUID());
        removeStacks(p);
        SjoelGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.player = null;
            game.phase = Phase.IDLE;
            game.entities.clear();
            game.inGate.clear();
            game.bak.clear();
        }
    }

    /** Is this player's Opoe still there, and still ticking? */
    public static boolean hasOpoeNearby(ServerPlayer p) {
        UUID npc = PLAYERS.get(p.getUUID());
        SjoelGame game = npc == null ? null : GAMES.get(npc);
        return game != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - game.lastTick <= STALE_TICKS;
    }

    // --- the bak ------------------------------------------------------------------------------------------------------------

    /** Finds the head of the sjoelbak (the sjoelen_kop with deel 0) and a spot next to Opoe. */
    public boolean scan(GuhNpcEntity npc, boolean force) {
        if (scanned) {
            return true;
        }
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos c = npc.blockPosition();
        BlockPos min = c.offset(-SCAN_RADIUS, -6, -SCAN_RADIUS), max = c.offset(SCAN_RADIUS, 6, SCAN_RADIUS);
        if (!force && !world.hasChunksAt(min, max)) {
            return false;
        }
        kop = null;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = world.getBlockState(pos);
            if (state.is(SjoelenFeature.KOP.get()) && state.getValue(SjoelenBlocks.DEEL) == 0) {
                kop = pos.immutable();
                facing = state.getValue(SjoelenBlocks.Deel.FACING);
                break;
            }
        }
        if (kop == null) {
            return false;
        }
        home = null;
        for (int[] d : new int[][] {{0, 2}, {2, 0}, {0, -2}, {-2, 0}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {0, 3}, {3, 0}, {0, -3}, {-3, 0}}) {
            BlockPos pos = c.offset(d[0], 0, d[1]);
            var shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if ((shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.25) && world.isEmptyBlock(pos.above()) && world.getBlockState(pos.below()).isSolid()) {
                home = pos;
                break;
            }
        }
        if (home == null) {
            home = c;
        }
        scanned = true;
        CompoundTag tag = new CompoundTag();
        tag.putLong("Kop", kop.asLong());
        tag.putString("Facing", facing.getName());
        tag.putLong("Home", home.asLong());
        npc.roleData.put("Bak", tag);
        return true;
    }

    private void load(CompoundTag data) {
        CompoundTag tag = data.getCompoundOrEmpty("Bak");
        if (!tag.contains("Kop")) {
            return;
        }
        kop = BlockPos.of(tag.getLongOr("Kop", 0L));
        Direction f = Direction.byName(tag.getStringOr("Facing", ""));
        facing = f == null || f.getAxis().isVertical() ? Direction.NORTH : f;
        home = tag.contains("Home") ? BlockPos.of(tag.getLongOr("Home", 0L)) : null;
        scanned = true;
    }

    /** Is the head of the bak still there? */
    private boolean intact(ServerLevel world) {
        return kop != null && world.getBlockState(kop).is(SjoelenFeature.KOP.get());
    }

    /** The world's top 3 floats above Opoe and above the gates of the bak. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Component text = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.sjoelen"), List.of(BOARD),
                List.of(Component.translatable("gui.guhs.sjoelen.scorebord_heading")), s -> s + " pt");
        Scorebord.show(world, npc.position().add(0, 2.5, 0), "sjoelen", text);
        SjoelGame game = GAMES.get(npc.getUUID());
        if (game != null && game.kop != null) {
            Vec3 above = game.world(SjoelBak.BACK + 0.2, SjoelBak.WIDTH / 2);
            Scorebord.show(world, above.add(0, 2.6, 0), "sjoelen_poort", text);
        }
    }

    // --- helpers -----------------------------------------------------------------------------------------------------------

    public static int best(Player p) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(BEST) ? saved.getIntOr(BEST, 0) : -1;
    }

    private static double horizontal(ServerPlayer p, GuhNpcEntity npc) {
        return Math.max(Math.abs(p.getX() - npc.getX()), Math.abs(p.getZ() - npc.getZ()));
    }

    /** The pucks into your hand (an empty hotbar slot becomes the selected one). */
    private static void giveStack(ServerPlayer p, int count) {
        removeStacks(p);
        Inventory inv = p.getInventory();
        ItemStack stack = new ItemStack(SjoelenFeature.SCHIJVEN.get(), count);
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

    /** How many pucks the stack in your hand shows. */
    private static void setStack(ServerPlayer p, int count) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(SjoelenFeature.SCHIJVEN.get())) {
                if (count <= 0) {
                    inv.setItem(i, ItemStack.EMPTY);
                } else {
                    s.setCount(count);
                }
                return;
            }
        }
    }

    /** Takes back every loaned puck (inventory, crafting grid, the mouse cursor). */
    public static void removeStacks(Player p) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(SjoelenFeature.SCHIJVEN.get()), -1, p.inventoryMenu.getCraftSlots());
        if (p.containerMenu.getCarried().is(SjoelenFeature.SCHIJVEN.get())) {
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

    // --- for the GameTests ---------------------------------------------------------------------------------------------------

    public void testSkipCountdown() {
        if (phase == Phase.COUNTDOWN) {
            timer = 1;
        }
    }

    /** Puts the rest of the pucks straight into the gates (counts per gate, left to right) and lets Opoe count. */
    public void testLand(GuhNpcEntity npc, int[] counts) {
        ServerLevel world = (ServerLevel) npc.level();
        for (int k = 0; k < counts.length; k++) {
            double v = (SjoelBak.OPENINGS[k][0] + SjoelBak.OPENINGS[k][1]) / 2;
            for (int i = 0; i < counts[k] && thrown < PUCKS; i++) {
                SjoelBak.Puck q = bak.place(SjoelBak.BAR + 0.5 + i * (2 * SjoelBak.R + 0.01) % (SjoelBak.BACK - SjoelBak.BAR - 0.8), v);
                Vec3 pos = world(q.u, q.v);
                SjoelSchijfEntity e = SjoelSchijfEntity.create(world, this, q.id, pos.x, pos.y, pos.z, 0);
                entities.put(q.id, e.getUUID());
                thrown++;
            }
        }
        thrown = PUCKS;
        phase = Phase.SETTLING;
        timer = SETTLE_LIMIT;
    }

    /** The entities of the pucks on the bak now. */
    public List<SjoelSchijfEntity> testEntities(ServerLevel world) {
        return entities.values().stream().map(world::getEntity).filter(e -> e instanceof SjoelSchijfEntity).map(e -> (SjoelSchijfEntity) e).toList();
    }
    /** 26.1: ServerPlayer#playNotifySound is gone (same packet as 1.21.1's). */
    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        notifySound(p, net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch);
    }

    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, source, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }
}
